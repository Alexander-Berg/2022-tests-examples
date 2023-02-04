# -*- coding: utf-8 -*-

import datetime
import itertools

import pytest
import mock
import hamcrest

from balance import core
from balance import exc
from balance import muzzle_util as ut
from balance import mapper
from balance.constants import (
    ServiceId,
    DIRECT_PRODUCT_ID,
)

from tests import object_builder as ob

BANK_UR_PAYSYS_ID = 1003
CARD_UR_PAYSYS_ID = 1033


@pytest.fixture
def coreobj(session):
    return core.Core(session)


def _create_request(session, client, product_id, qty):
    return _create_request_rows(session, client, product_rows=[(product_id, qty)])


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


def _create_contract(session, client, person, **kwargs):
    params = dict(
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
        is_signed=datetime.datetime.now()
    )
    params.update(kwargs)

    contract = ob.ContractBuilder(
        client=client,
        person=person,
        **params
    ).build(session).obj
    return contract


@pytest.fixture
def contract(request, session, client, person):
    params = getattr(request, 'param', {})
    return _create_contract(session, client, person, **params)


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


class TestPayOnCredit(object):
    @pytest.mark.parametrize(
        'contract',
        [
            {'personal_account_fictive': 0},
            {'personal_account_fictive': 1},
        ],
        True,
        ['old_pa', 'new_pa']
    )
    def test_personal_account(self, coreobj, request_obj, contract):
        pa, = coreobj.pay_on_credit(
            request_id=request_obj.id,
            paysys_id=BANK_UR_PAYSYS_ID,
            person_id=contract.person_id,
            contract_id=contract.id
        )
        assert pa.consume_sum == 300
        assert isinstance(pa, mapper.PersonalAccount)

        with pytest.raises(exc.CREDIT_WAS_ALREADY_GIVEN):
            coreobj.pay_on_credit(
                request_id=request_obj.id,
                paysys_id=BANK_UR_PAYSYS_ID,
                person_id=contract.person_id,
                contract_id=contract.id
            )

    def test_fictive_invoice(self, session, coreobj, client, person, request_obj):
        contract = _create_contract(
            session, client, person,
            personal_account=None,
            personal_account_fictive=None
        )

        invoice, = coreobj.pay_on_credit(
            request_id=request_obj.id,
            paysys_id=BANK_UR_PAYSYS_ID,
            person_id=contract.person_id,
            contract_id=contract.id
        )
        assert invoice.consume_sum == 300
        assert isinstance(invoice, mapper.FictiveInvoice)

        with pytest.raises(exc.CREDIT_WAS_ALREADY_GIVEN):
            coreobj.pay_on_credit(
                request_id=request_obj.id,
                paysys_id=BANK_UR_PAYSYS_ID,
                person_id=contract.person_id,
                contract_id=contract.id
            )

    def test_multiple_requests(self, session, coreobj, contract):
        # BALANCE-20323
        request1 = _create_request(session, contract.client, DIRECT_PRODUCT_ID, 10)
        request2 = _create_request(session, contract.client, DIRECT_PRODUCT_ID, 10)

        coreobj.pay_on_credit(
            request_id=request1.id,
            paysys_id=BANK_UR_PAYSYS_ID,
            person_id=contract.person_id,
            contract_id=contract.id
        )
        pa, = coreobj.pay_on_credit(
            request_id=request2.id,
            paysys_id=BANK_UR_PAYSYS_ID,
            person_id=contract.person_id,
            contract_id=contract.id
        )
        assert pa.consume_sum == 600

        deferpays = (
            session.query(mapper.Deferpay)
                .filter(mapper.Deferpay.orig_request_id.in_([request1.id, request2.id]))
                .all()
        )
        assert all(dp.invoice_id == pa.id for dp in deferpays)

    def test_different_paysyses_fictive(self, session, coreobj, contract):
        request1 = _create_request(session, contract.client, DIRECT_PRODUCT_ID, 7)
        request2 = _create_request(session, contract.client, DIRECT_PRODUCT_ID, 3)

        pa1, = coreobj.pay_on_credit(
            request_id=request1.id,
            paysys_id=BANK_UR_PAYSYS_ID,
            person_id=contract.person_id,
            contract_id=contract.id
        )
        pa2, = coreobj.pay_on_credit(
            request_id=request2.id,
            paysys_id=CARD_UR_PAYSYS_ID,
            person_id=contract.person_id,
            contract_id=contract.id
        )

        hamcrest.assert_that(
            pa1,
            hamcrest.has_properties(
                consume_sum=210,
                paysys_id=BANK_UR_PAYSYS_ID,
            )
        )
        hamcrest.assert_that(
            pa2,
            hamcrest.has_properties(
                consume_sum=90,
                paysys_id=CARD_UR_PAYSYS_ID,
            )
        )

    def test_different_paysyses_old(self, session, coreobj, contract):
        contract.col0.personal_account_fictive = 0
        request1 = _create_request(session, contract.client, DIRECT_PRODUCT_ID, 7)
        request2 = _create_request(session, contract.client, DIRECT_PRODUCT_ID, 3)

        coreobj.pay_on_credit(
            request_id=request1.id,
            paysys_id=BANK_UR_PAYSYS_ID,
            person_id=contract.person_id,
            contract_id=contract.id
        )
        with pytest.raises(exc.MULTIPLE_PERSONAL_ACCOUNTS):
            coreobj.pay_on_credit(
                request_id=request2.id,
                paysys_id=CARD_UR_PAYSYS_ID,
                person_id=contract.person_id,
                contract_id=contract.id
            )


class TestShop(object):

    @pytest.fixture
    def shop_product(self, request, session):
        price = getattr(request, 'param', 10)
        return ob.ProductBuilder(
            price=price,
            engine_id=ServiceId.ONE_TIME_SALE
        ).build(session).obj

    @pytest.mark.parametrize(
        'contract',
        [
            {'personal_account_fictive': 0},
            {'personal_account_fictive': 1},
        ],
        True,
        ['old_pa', 'new_pa']
    )
    def test_base(self, session, coreobj, request_obj, contract, shop_product):
        # зачисляем на Директ
        pa, = coreobj.pay_on_credit(
            request_id=request_obj.id,
            paysys_id=BANK_UR_PAYSYS_ID,
            person_id=contract.person_id,
            contract_id=contract.id
        )

        # откручиваем Директ
        request_order, = request_obj.rows
        direct_order = request_order.order
        with mock.patch('balance.actions.taxes_update.TaxUpdater.policy', None):
            direct_order.calculate_consumption(
                datetime.datetime.now() - datetime.timedelta(666),
                {direct_order.shipment_type: direct_order.consume_qty}
            )
        session.flush()

        # создаём реквест с магазином
        shop_request_obj = _create_request_rows(
            session, contract.client,
            product_rows=[
                (shop_product.id, 6),
                (shop_product.id, 66),
            ]
        )
        shop_order1, shop_order2 = [ro.order for ro in shop_request_obj.rows]

        # зачисляем на магазин
        coreobj.pay_on_credit(
            request_id=shop_request_obj.id,
            paysys_id=BANK_UR_PAYSYS_ID,
            person_id=contract.person_id,
            contract_id=contract.id
        )

        assert pa.consume_sum == 300 + 6 * 10 + 66 * 10

        req_consumes_state = [
            (direct_order, 10, 10, 0),
            (shop_order1, 6, 6, 6),
            (shop_order2, 66, 66, 66),
        ]
        consumes_state = [(co.order, co.current_qty, co.completion_qty, co.act_qty) for co in pa.consumes]
        assert consumes_state == req_consumes_state

    def test_only_monthly_actable(self, session, coreobj, client, shop_product):
        # Турция - единственная фирма с only_monthly_actable
        person = ob.PersonBuilder(client=client, type='tru').build(session).obj
        contract = _create_contract(session, client, person, firm=8)

        # создаём реквест с магазином
        shop_request_obj = _create_request_rows(
            session, contract.client,
            product_rows=[(shop_product.id, 666)]
        )
        on_dt = ut.trunc_date(datetime.datetime.now()).replace(day=1)
        shop_request_obj.desired_invoice_dt = on_dt
        shop_order, = [ro.order for ro in shop_request_obj.rows]

        # зачисляем на магазин
        pa, = coreobj.pay_on_credit(
            request_id=shop_request_obj.id,
            paysys_id=1050,
            person_id=contract.person_id,
            contract_id=contract.id
        )

        act, = itertools.chain.from_iterable(ri.acts for ri in pa.repayments)

        req_consumes_state = [
            (shop_order, 666, 666, 666),
        ]
        consumes_state = [(co.order, co.current_qty, co.completion_qty, co.act_qty) for co in pa.consumes]
        assert consumes_state == req_consumes_state
        assert act.dt == ut.add_months_to_date(on_dt, 1) - datetime.timedelta(1)
