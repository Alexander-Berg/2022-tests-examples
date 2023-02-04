# -*- coding: utf-8 -*-
import pytest
from balance import mapper
from balance import constants as cst

from tests import object_builder as ob

PERMISSION = 'Permission'
WRONG_PERMISSION = 'WrongPermission'
INVOICE_FIRM_ID = cst.FirmId.YANDEX_OOO


class CheckType(cst.Enum):
    object = 'object'
    query = 'query'


def create_passport(session, roles, client=None, patch_session=True, **kwargs):
    passport = ob.create_passport(session, *roles, patch_session=patch_session, client=client, **kwargs)
    return passport


def create_role(session, *permissions):
    return ob.create_role(session, *permissions)


@pytest.fixture(name='wrong_role')
def create_wrong_role(session):
    return ob.create_role(session, (WRONG_PERMISSION, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='right_role')
def create_right_role(session):
    return ob.create_role(session, (PERMISSION, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='view_invoices_role')
def create_view_invoices_role(session):
    return ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='view_invoices_role_2')
def create_view_invoices_role_2(session):
    return ob.create_role(
        session,
        (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None, cst.ConstraintTypes.manager: 1}),
    )


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    )


@pytest.fixture(name='yamoney_role')
def create_yamoney_role(session):
    return ob.create_role(session, (cst.PermissionCode.YAMONEY_SUPPORT_ACCESS, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='manager')
def create_manager(session, passport=None):
    return ob.SingleManagerBuilder(
        passport_id=passport and passport.passport_id,
        domain_login=passport and passport.login).build(session).obj


@pytest.fixture(name='request_')
def create_request(session, client=None, firm_id=INVOICE_FIRM_ID, manager=None):
    client = client or ob.ClientBuilder().build(session).obj
    return ob.RequestBuilder(
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(
                order=ob.OrderBuilder(client=client, manager=manager),
                quantity=666,
            )]
        )
    ).build(session).obj


@pytest.fixture(name='invoice')
def create_invoice(session, client=None, firm_id=INVOICE_FIRM_ID, manager=None):
    client = client or create_client(session)
    request = create_request(session, client, firm_id, manager)
    person = ob.PersonBuilder(client=request.client).build(session).obj
    paysys = session.query(mapper.Paysys).filter_by(firm_id=firm_id, cc='ph').one()

    invoice = ob.InvoiceBuilder(
        person=person,
        paysys=paysys,
        request=request,
    ).build(session).obj
    return invoice


@pytest.fixture(name='act')
def create_act(session, firm_id=INVOICE_FIRM_ID, manager=None, client=None):
    invoice = create_invoice(session, firm_id=firm_id, manager=manager, client=client)
    invoice.turn_on_rows()
    order = invoice.invoice_orders[0].order
    order.calculate_consumption(dt=session.now(),
                                shipment_info={order.shipment_type: 333})
    return invoice.generate_act(force=True)[0]


def _get_query(obj):
    return obj.session.query(mapper.Act).join(mapper.Invoice)


def is_allowed(obj, passport, check_type):
    if check_type == CheckType.object:
        return obj.check_perm_constraints(passport, PERMISSION)
    else:
        filters = mapper.Act.get_perm_constraints_query_filter(passport, PERMISSION)
        return (
            _get_query(obj)
            .filter(mapper.Act.id == obj.id, filters)
            .exists()
        )
