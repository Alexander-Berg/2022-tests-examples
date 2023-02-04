# -*- coding: utf-8 -*-

import datetime

import pytest
import mock

from balance import core
from balance import mapper
from balance import exc
from balance.constants import (
    DIRECT_PRODUCT_ID,
    InvoiceStatusId,
    ExportState,
    PermissionCode,
    ConstraintTypes,
    FirmId,
)

from tests import object_builder as ob

BANK_UR_PAYSYS_ID = 1003


@pytest.fixture
def coreobj(session):
    return core.Core(session)


@pytest.fixture
def paysys(session):
    return session.query(mapper.Paysys).get(BANK_UR_PAYSYS_ID)


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder(client=client, type='ur').build(session).obj


@pytest.fixture
def request_obj(request, session, client):
    product_id, qty = getattr(request, 'param', (DIRECT_PRODUCT_ID, 10))
    return _create_request(session, client, product_id, qty)


def _create_request(session, client, product_id, qty):
    return _create_request_rows(session, client, product_rows=[(product_id, qty)])


def _create_request_rows(session, client, order_rows=None, product_rows=None):
    if order_rows is None:
        order_rows = []
        for product_id, qty in product_rows:
            product = ob.Getter(mapper.Product, product_id).build(session).obj
            order = ob.OrderBuilder(
                product=product,
                client=client,
                service_id=product.engine_id
            ).build(session).obj
            order_rows.append((order, qty))

    return ob.RequestBuilder(
        basket=ob.BasketBuilder(
            client=client,
            rows=[
                ob.BasketItemBuilder(order=order, quantity=qty)
                for order, qty in order_rows
            ]
        )
    ).build(session).obj


@pytest.fixture
def contract(session, client, person):
    params = dict(
        commission=0,
        firm=1,
        postpay=1,
        personal_account=None,
        personal_account_fictive=None,
        payment_type=3,
        payment_term=30,
        credit=3,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        is_signed=datetime.datetime.now()
    )
    contract = ob.ContractBuilder(
        client=client,
        person=person,
        **params
    ).build(session).obj
    return contract


@pytest.fixture
def preliminary_invoice(session, coreobj, paysys, contract, request_obj):
    fictive_invoice, = coreobj.pay_on_credit(
        request_obj.id,
        BANK_UR_PAYSYS_ID,
        contract.person_id,
        contract.id
    )

    return coreobj.issue_repayment_invoice(
        session.oper_id,
        [fictive_invoice.deferpay.id],
        datetime.datetime.now()
    )


@pytest.fixture
def passport_w_perm(session):
    role = ob.create_role(
        session,
        PermissionCode.ISSUE_INVOICES,   # для честного создания счетов
        PermissionCode.CHANGE_REPAYMENTS_STATUS,
    )
    passport = ob.create_passport(session, role, patch_session=True)
    return passport


def _do_completions(invoice):
    invoice_order, = invoice.invoice_orders
    order = invoice_order.order
    shipment_dt = datetime.datetime.now() - datetime.timedelta(60)
    order.calculate_consumption(shipment_dt, {order.shipment_type: order.consume_qty})
    invoice.session.flush()


@pytest.mark.usefixtures('passport_w_perm')
def test_invoice_confirm(session, coreobj, preliminary_invoice):
    coreobj.preliminary_invoice_action(session.oper_id, preliminary_invoice.id, 'confirm')

    assert preliminary_invoice.hidden == 0
    assert preliminary_invoice.status_id == InvoiceStatusId.CONFIRMED
    assert preliminary_invoice.exports['OEBS'].state == ExportState.enqueued


@pytest.mark.usefixtures('passport_w_perm')
def test_invoice_decline(session, coreobj, preliminary_invoice):
    coreobj.preliminary_invoice_action(session.oper_id, preliminary_invoice.id, 'decline')

    assert preliminary_invoice.hidden == 2
    assert preliminary_invoice.status_id == InvoiceStatusId.PRELIMINARY


@pytest.mark.usefixtures('passport_w_perm')
def test_deferpay_confirm(session, coreobj, preliminary_invoice):
    fictive_invoice, = preliminary_invoice.fictives
    coreobj.deferpays_action(session.oper_id, [fictive_invoice.deferpay.id], 'confirm')

    assert preliminary_invoice.hidden == 0
    assert preliminary_invoice.status_id == InvoiceStatusId.CONFIRMED
    assert preliminary_invoice.exports['OEBS'].state == ExportState.enqueued


@pytest.mark.usefixtures('passport_w_perm')
def test_confirm_act(session, coreobj, preliminary_invoice):
    _do_completions(preliminary_invoice)

    with mock.patch('balance.mapper.invoices.Invoice.actable_dt', return_value=True):
        coreobj.preliminary_invoice_action(session.oper_id, preliminary_invoice.id, 'confirm')

    assert preliminary_invoice.total_act_sum > 0


@pytest.mark.usefixtures('passport_w_perm')
def test_decline_no_act(session, coreobj, preliminary_invoice):
    _do_completions(preliminary_invoice)

    with mock.patch('balance.mapper.invoices.Invoice.actable_dt', return_value=True):
        coreobj.preliminary_invoice_action(session.oper_id, preliminary_invoice.id, 'decline')

    assert preliminary_invoice.total_act_sum == 0


@pytest.mark.permissions
def test_access_owner(session, coreobj, preliminary_invoice):
    passport = ob.create_passport(session, patch_session=True)
    passport.client = preliminary_invoice.client
    _do_completions(preliminary_invoice)

    with mock.patch('balance.mapper.invoices.Invoice.actable_dt', return_value=True):
        coreobj.preliminary_invoice_action(session.oper_id, preliminary_invoice.id, 'confirm')

    assert preliminary_invoice.status_id == InvoiceStatusId.CONFIRMED
    assert preliminary_invoice.total_act_sum > 0


@pytest.mark.permissions
def test_access_wrong_perm(session, coreobj, preliminary_invoice):
    role = ob.create_role(session, PermissionCode.ISSUE_INVOICES)
    ob.create_passport(session, role, patch_session=True)
    _do_completions(preliminary_invoice)

    with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
        with mock.patch('balance.mapper.invoices.Invoice.actable_dt', return_value=True):
            coreobj.preliminary_invoice_action(session.oper_id, preliminary_invoice.id, 'confirm')

    assert 'has no permission ChangeRepaymentsStatus' in str(exc_info.value)
    assert preliminary_invoice.total_act_sum == 0


@pytest.mark.permissions
def test_access_wrong_firm(session, coreobj, preliminary_invoice):
    role = ob.create_role(session, (PermissionCode.CHANGE_REPAYMENTS_STATUS, {ConstraintTypes.firm_id: None}))
    ob.create_passport(session, (role, FirmId.TAXI), patch_session=True)

    with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
        coreobj.preliminary_invoice_action(session.oper_id, preliminary_invoice.id, 'confirm')

    assert 'has no permission ChangeRepaymentsStatus' in str(exc_info.value)
