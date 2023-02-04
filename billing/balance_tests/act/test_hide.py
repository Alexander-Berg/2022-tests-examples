# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest

from balance import mapper
from balance import core
import balance.muzzle_util as ut
import balance.actions.acts as a_a
from balance.constants import *

from tests import object_builder as ob


PRESENT = ut.trunc_date(datetime.datetime.now())
CUR_MONTH = mapper.ActMonth(for_month=PRESENT)


@pytest.fixture
def agency(session):
    return ob.ClientBuilder(is_agency=1).build(session).obj


@pytest.fixture
def subclient(session, agency):
    return ob.ClientBuilder(agency=agency).build(session).obj


@pytest.fixture
def credit_contract(session, agency):
    contract = ob.ContractBuilder(
        dt=datetime.datetime.now() - datetime.timedelta(days=66),
        client=agency,
        person=ob.PersonBuilder(client=agency, type='ur'),
        commission=1,
        payment_type=3,
        credit_type=1,
        payment_term=30,
        payment_term_max=60,
        personal_account=1,
        personal_account_fictive=1,
        currency=810,
        lift_credit_on_payment=1,
        commission_type=57,
        repayment_on_consume=1,
        credit_limit_single=1666666,
        services={7},
        is_signed=datetime.datetime.now(),
        firm=1,
    ).build(session).obj
    session.flush()
    return contract


@pytest.fixture
def paysys(session):
    return session.query(mapper.Paysys).filter_by(firm_id=1).getone(cc='ur')


class TestActHide(object):
    def test_prepay(self, session):
        client = ob.ClientBuilder().build(session).obj
        orders = [
            ob.OrderBuilder(
                product=ob.Getter(mapper.Product, DIRECT_PRODUCT_ID),
                client=client,
            ).build(session).obj
            for _ in range(10)
        ]

        invoice = ob.InvoiceBuilder(
            person=ob.PersonBuilder(client=client),
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=client,
                    rows=[
                        ob.BasketItemBuilder(order=o, quantity=10)
                        for o in orders
                    ]
                )
            )
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows(on_dt=invoice.dt)

        for order in orders:
            order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 6})
        act1, = invoice.generate_act(backdate=PRESENT, force=1)
        act1.exports['OEBS'].state = 1

        for order in orders:
            order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 10})
        act2, = invoice.generate_act(backdate=PRESENT, force=1)

        act1.hide('test')

        hamcrest.assert_that(
            act1,
            hamcrest.has_properties(
                hidden=4,
                jira_id='test',
                invoice=hamcrest.has_properties(
                    hidden=0,
                    total_act_sum=1200
                ),
                exports=hamcrest.has_entry(
                    'OEBS',
                    hamcrest.has_properties(state=0)
                )
            )
        )
        hamcrest.assert_that(
            orders,
            hamcrest.only_contains(
                hamcrest.has_properties(
                    consumes=hamcrest.contains(
                        hamcrest.has_properties(
                            act_qty=4,
                            act_sum=120
                        )
                    )
                )
            )
        )

    def test_y_invoice(self, session, credit_contract, subclient, paysys):
        orders = [
            ob.OrderBuilder(
                product=ob.Getter(mapper.Product, DIRECT_PRODUCT_ID),
                client=subclient, agency=credit_contract.client,
            ).build(session).obj
            for _ in range(10)
        ]

        request = ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=credit_contract.client,
                rows=[
                    ob.BasketItemBuilder(quantity=10, order=o)
                    for o in orders
                ]
            )
        ).build(session).obj
        pa, = core.Core(session).pay_on_credit(request.id, paysys.id, credit_contract.person_id, credit_contract.id)

        for order in orders:
            order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 6})
        act1, = a_a.ActAccounter(pa.client, CUR_MONTH, force=1).do()
        act1.exports['OEBS'].state = 1

        for order in orders:
            order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 10})
        act2, = a_a.ActAccounter(pa.client, CUR_MONTH, force=1).do()

        act1.hide('test')

        hamcrest.assert_that(
            act1,
            hamcrest.has_properties(
                hidden=4,
                jira_id='test',
                invoice=hamcrest.has_properties(
                    hidden=2,
                    total_act_sum=0
                ),
                exports=hamcrest.has_entry(
                    'OEBS',
                    hamcrest.has_properties(state=0)
                )
            )
        )
        hamcrest.assert_that(
            orders,
            hamcrest.only_contains(
                hamcrest.has_properties(
                    consumes=hamcrest.contains(
                        hamcrest.has_properties(
                            act_qty=4,
                            act_sum=120
                        )
                    )
                )
            )
        )
