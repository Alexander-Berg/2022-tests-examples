# -*- coding: utf-8 -*-

import decimal
import pytest
import datetime
from balance.actions.invoice_update.common import get_parameters_update_calculator
from balance import mapper, core
from balance.actions import acts as a_a
from tests import object_builder as ob

D = decimal.Decimal

now = datetime.datetime.now()


def create_y_invoice(session, person, qty, paysys_id=1003, product_id=503162, invoice_dt=now):
    contract = ob.ContractBuilder(
        client=person.client,
        person=person,
        commission=0,
        firm=1,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        is_signed=session.now(),
    ).build(session).obj
    contract.client.is_agency = 1
    subclient = ob.ClientBuilder.construct(session)
    order = ob.OrderBuilder(product=ob.Getter(mapper.Product, product_id),
                            service=ob.Getter(mapper.Service, 7),
                            client=subclient,
                            agency=contract.client
                            ).build(session).obj
    basket = ob.BasketBuilder(
        client=contract.client,
        rows=[
            ob.BasketItemBuilder(order=o, quantity=qty)
            for o, qty in [(order, qty)]
        ]
    )
    request = ob.RequestBuilder(basket=basket).build(session).obj
    coreobj = core.Core(request.session)
    pa, = coreobj.pay_on_credit(
        request_id=request.id,
        paysys_id=paysys_id,
        person_id=contract.person.id,
        contract_id=contract.id
    )
    request.session.flush()

    order.calculate_consumption(invoice_dt, {order.shipment_type: qty})
    act, = a_a.ActAccounter(
        pa.client,
        mapper.ActMonth(for_month=invoice_dt),
        invoices=[pa.id], dps=[],
        force=1
    ).do()
    invoice = act.invoice
    pa.session.flush()
    return invoice


@pytest.mark.parametrize('w_nds', [
    True,
    False
])
def test_get_invoice_orders(session, w_nds):
    if w_nds:
        person_type = 'ur'
        paysys_id = 1003
        product_id = 503162
    else:
        person_type = 'yt'
        paysys_id = 1014
        product_id = 503162
    person = ob.PersonBuilder.construct(session, type=person_type)
    invoice = create_y_invoice(session, person, qty=D('19.3434'), paysys_id=paysys_id, product_id=product_id)
    invoice._invoice_orders_ = None
    prev_total_sum = invoice.total_sum
    invoice.update_from_calculator(get_parameters_update_calculator(invoice))
    assert prev_total_sum == invoice.total_sum


@pytest.mark.parametrize('invoice_dt', [
    datetime.datetime.now(),
    datetime.datetime(2018, 01, 01)
])
def test_get_invoice_orders_nds_18_20(session, invoice_dt):
    person = ob.PersonBuilder.construct(session, type='ur')
    invoice = create_y_invoice(session, person, qty=D('19.3434'), paysys_id=1003, product_id=503162,
                               invoice_dt=invoice_dt)
    invoice._invoice_orders_ = None
    prev_total_sum = invoice.total_sum
    invoice.update_from_calculator(get_parameters_update_calculator(invoice))
    assert prev_total_sum == invoice.total_sum
