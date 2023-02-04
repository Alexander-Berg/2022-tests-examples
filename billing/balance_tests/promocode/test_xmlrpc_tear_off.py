# -*- coding: utf-8 -*-

import pytest
import hamcrest

from balance import mapper
from billing.contract_iface import ContractTypeId
from balance import constants as cst
from balance.providers.personal_acc_manager import PersonalAccountManager
from balance.actions import promocodes as a_pmc
from balance.actions.invoice_turnon import InvoiceTurnOn

from tests import object_builder as ob
from tests.balance_tests.promocode.common import (
    create_order,
    create_invoice,
    create_promocode,
)

pytestmark = [
    pytest.mark.promo_code,
]


def build_invoice(order, promocode=None):
    session = order.session
    if promocode:
        a_pmc.reserve_promo_code(order.client, promocode)

    invoice = create_invoice(session, 100, order.client, order)
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(apply_promocode=True)
    session.flush()
    return invoice


def assert_call_res(res, *invoices):
    hamcrest.assert_that(
        res,
        hamcrest.contains(
            0,
            'SUCCESS',
            hamcrest.contains_inanyorder(*[
                {'InvoiceID': i.id, 'InvoiceEID': i.external_id}
                for i in invoices
            ])
        )
    )


def test_invoice_id(session, xmlrpcserver, order, promocode):
    other_invoice = build_invoice(order)
    invoice = build_invoice(order, promocode)

    res = xmlrpcserver.TearOffPromocode(
        session.oper_id,
        {'InvoiceID': invoice.id}
    )

    assert_call_res(res, invoice)
    assert invoice.promo_code == promocode
    assert not any(co.discount_pct for co in invoice.consumes if co.current_qty > 0)
    assert bool(invoice.reverses) is True
    assert bool(other_invoice.reverses) is False


def test_invoice_eid(session, xmlrpcserver, order, promocode):
    other_invoice = build_invoice(order)
    invoice = build_invoice(order, promocode)

    res = xmlrpcserver.TearOffPromocode(
        session.oper_id,
        {'InvoiceEID': invoice.external_id}
    )

    assert_call_res(res, invoice)
    assert invoice.promo_code == promocode
    assert not any(co.discount_pct for co in invoice.consumes if co.current_qty > 0)
    assert bool(invoice.reverses) is True
    assert bool(other_invoice.reverses) is False


def test_order_promocode_id(session, xmlrpcserver, order, promocode):
    other_invoice = build_invoice(order, create_promocode(session, dict(skip_reservation_check=True)))
    invoice = build_invoice(order, promocode)

    res = xmlrpcserver.TearOffPromocode(
        session.oper_id,
        {
            'ServiceID': order.service_id,
            'ServiceOrderID': order.service_order_id,
            'PromocodeID': promocode.id,
        }
    )

    assert_call_res(res, invoice)
    assert invoice.promo_code == promocode
    assert not any(co.discount_pct for co in invoice.consumes if co.current_qty > 0)
    assert bool(invoice.reverses) is True
    assert bool(other_invoice.reverses) is False


def test_order_promocode_code(session, xmlrpcserver, order, promocode):
    other_invoice = build_invoice(order, create_promocode(session, dict(skip_reservation_check=True)))
    invoice = build_invoice(order, promocode)

    res = xmlrpcserver.TearOffPromocode(
        session.oper_id,
        {
            'ServiceID': order.service_id,
            'ServiceOrderID': order.service_order_id,
            'Promocode': promocode.code,
        }
    )

    assert_call_res(res, invoice)
    assert invoice.promo_code == promocode
    assert not any(co.discount_pct for co in invoice.consumes if co.current_qty > 0)
    assert bool(invoice.reverses) is True
    assert bool(other_invoice.reverses) is False


def test_client_promocode_code(session, xmlrpcserver, client, order, promocode):
    other_invoice = build_invoice(order, create_promocode(session, dict(skip_reservation_check=True)))
    invoice = build_invoice(order, promocode)

    res = xmlrpcserver.TearOffPromocode(
        session.oper_id,
        {
            'ClientID': client.id,
            'Promocode': promocode.code,
        }
    )

    assert_call_res(res, invoice)
    assert invoice.promo_code == promocode
    assert not any(co.discount_pct for co in invoice.consumes if co.current_qty > 0)
    assert bool(invoice.reverses) is True
    assert bool(other_invoice.reverses) is False


def test_client(session, xmlrpcserver, client):
    invoice_1 = build_invoice(
        create_order(session, client),
        create_promocode(session, dict(skip_reservation_check=True))
    )
    invoice_2 = build_invoice(
        create_order(session, client),
        create_promocode(session, dict(skip_reservation_check=True))
    )

    res = xmlrpcserver.TearOffPromocode(
        session.oper_id,
        {
            'ClientID': client.id
        }
    )

    assert_call_res(res, invoice_1, invoice_2)
    assert invoice_1.promo_code is not None
    assert not any(co.discount_pct for co in invoice_1.consumes if co.current_qty > 0)
    assert invoice_2.promo_code is not None
    assert not any(co.discount_pct for co in invoice_2.consumes if co.current_qty > 0)
    assert bool(invoice_1.reverses) is True
    assert bool(invoice_2.reverses) is True


def test_client_promocode_charge_note(session, xmlrpcserver, client):
    paysys = ob.Getter(mapper.Paysys, 1003).build(session).obj
    contract = ob.create_credit_contract(
        session,
        client,
        commission=ContractTypeId.NON_AGENCY,
        commission_type=None,
        personal_account_fictive=0,
        services={cst.ServiceId.TAXI_CASH}
    )
    pa = PersonalAccountManager(session).for_contract(contract).for_paysys(paysys).get(auto_create=True)
    invoice = ob.InvoiceBuilder.construct(
        session,
        contract=contract,
        person=contract.person,
        type='charge_note',
        request=ob.RequestBuilder(
            turn_on_rows=True,
            basket=ob.BasketBuilder(
                client=contract.client,
                rows=[ob.BasketItemBuilder(
                    quantity=100,
                    order=ob.OrderBuilder(
                        service_id=cst.ServiceId.TAXI_CASH,
                        product_id=cst.DIRECT_PRODUCT_RUB_ID,
                        client=contract.client,
                    )
                )]
            )
        )
    )

    promocode = create_promocode(session, dict(service_ids=[cst.ServiceId.TAXI_CASH]))
    a_pmc.reserve_promo_code(contract.client, promocode)
    session.flush()
    InvoiceTurnOn(invoice).do()

    res = xmlrpcserver.TearOffPromocode(
        session.oper_id,
        {
            'ClientID': client.id,
            'PromocodeID': promocode.id,
        }
    )

    assert_call_res(res, pa)
    assert not any(co.discount_pct for co in pa.consumes if co.current_qty > 0)
    assert bool(pa.reverses) is True
