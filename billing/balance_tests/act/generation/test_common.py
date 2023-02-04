# -*- coding: utf-8 -*-

import datetime
import decimal

import pytest
import hamcrest
import mock

from balance.processors.month_proc import handle_client
from balance import mapper
import balance.muzzle_util as ut
from balance import exc
from balance.constants import (
    DIRECT_PRODUCT_ID,
    DIRECT_PRODUCT_RUB_ID,
    SPRAVOCHNIK_PRODUCT_ID,
    OrderLogTariffState,
    UAChildType,
    PaymentMethodIDs,
    FirmId
)

from tests import object_builder as ob

from tests.balance_tests.act.generation.common import (
    create_invoice,
    create_order,
    calculate_consumption,
    create_consume,
    consume_credit,
    generate_act,
    get_act_act_enqueuer
)

D = decimal.Decimal

PRESENT = ut.trunc_date(datetime.datetime.now())
NEAR_PAST = PRESENT - datetime.timedelta(3)
CUR_MONTH = mapper.ActMonth(for_month=PRESENT)


class TestActCommon(object):

    def create_act(self, invoice, order):
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 3})
        act, = invoice.generate_act(backdate=PRESENT, force=1)
        return act

    # Внутренний акт для внутреннего клиента
    def test_internal_act_for_internal_client(self, session):
        client = ob.ClientBuilder(is_agency=0, internal=1).build(session).obj
        order = create_order(session, client)
        invoice = create_invoice(session, client, [order])
        act = self.create_act(invoice, order)
        hamcrest.assert_that(
            act,
            hamcrest.has_properties(
                is_external=False,
                client=hamcrest.has_properties(
                    internal=1
                )
            )
        )

    # Внутренний акт для счетов, оплаченного промокодом
    def test_internal_act_for_paid_promocode(self, session):
        client = ob.ClientBuilder(is_agency=0).build(session).obj
        order = create_order(session, client)
        paysys = session.query(mapper.Paysys).filter_by(
            payment_method_id=PaymentMethodIDs.paid_promocode
        ).getone(firm_id=FirmId.YANDEX_OOO)
        invoice = create_invoice(session, client, [order], paysys=paysys.id)
        act = self.create_act(invoice, order)
        hamcrest.assert_that(
            act,
            hamcrest.has_properties(
                is_external=False,
            )
        )

    # Скидка в заказе
    def test_act_with_discount_in_order(self, session):
        client = ob.ClientBuilder(is_agency=0).build(session).obj
        order = create_order(session, client, SPRAVOCHNIK_PRODUCT_ID)
        invoice = create_invoice(session, client, [order])
        act = self.create_act(invoice, order)
        hamcrest.assert_that(
            act.invoice.invoice_orders[0],
            hamcrest.has_properties(
                discount_pct=20
            )
        )

    # Разные менеджеры в строчках актов
    def test_multimanager_act(self, session):
        client = ob.ClientBuilder(is_agency=0).build(session).obj
        order = create_order(session, client, DIRECT_PRODUCT_ID)
        manager_1 = order.manager.manager_code
        invoice = create_invoice(session, client, [order])
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 3})
        manager_2 = ob.SingleManagerBuilder().build(session).obj
        order.manager = manager_2
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: D('4.5')})
        act, = invoice.generate_act(backdate=PRESENT, force=1)
        managers_fact = [act.rows[0].manager_code, act.rows[1].manager_code]
        managers_expected = [manager_1, manager_2.manager_code]
        hamcrest.assert_that(
            managers_fact.sort(),
            hamcrest.equal_to(
                managers_expected.sort()
            )
        )

    @pytest.mark.log_tariff
    @pytest.mark.parametrize(
        'is_log_tariff, child_ua_type, is_ok',
        [
            pytest.param(OrderLogTariffState.OFF, None, True),
            pytest.param(OrderLogTariffState.INIT, None, False),
            pytest.param(OrderLogTariffState.MIGRATED, None, False),
            pytest.param(OrderLogTariffState.OFF, UAChildType.TRANSFERS, True),
            pytest.param(OrderLogTariffState.OFF, UAChildType.OPTIMIZED, True),
            pytest.param(OrderLogTariffState.OFF, UAChildType.LOG_TARIFF, False),
        ]
    )
    def test_log_tariff(self, session, is_log_tariff, child_ua_type, is_ok):
        client = ob.ClientBuilder.construct(session)
        order = create_order(session, client, DIRECT_PRODUCT_RUB_ID)
        other_order = create_order(session, client, DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(session, client, [order, other_order], qty=66)
        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 666})
        other_order.calculate_consumption(datetime.datetime.now(), {other_order.shipment_type: 666})
        order._is_log_tariff = is_log_tariff
        order.child_ua_type = child_ua_type
        session.flush()

        acts = invoice.generate_act(backdate=PRESENT, force=1)

        hamcrest.assert_that(
            acts,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(act_sum=132 if is_ok else 66)
            )
        )

    def test_only_taxes_act(self, session, client):
        tax_policy = ob.TaxPolicyBuilder.construct(session, tax_pcts=[20])
        product = ob.ProductBuilder.construct(session, taxes=[tax_policy], prices=[(NEAR_PAST, 'RUR', 1)])

        order = create_order(session, client, product=product.id)
        invoice = create_invoice(session, client, [order], qty=100)

        calculate_consumption(order, PRESENT, D('99.9706'))
        generate_act(invoice, PRESENT)

        calculate_consumption(order, PRESENT, D('99.9724'))
        act, = generate_act(invoice, PRESENT)

        hamcrest.assert_that(
            act,
            hamcrest.has_properties(
                amount=D('0.01'),
                amount_nds=D('0.01'),
                type='internal',
            )
        )

    @pytest.mark.log_tariff
    @pytest.mark.parametrize(
        'is_log_tariff, child_ua_type, force',
        [
            pytest.param(OrderLogTariffState.MIGRATED, None, 1),
            pytest.param(OrderLogTariffState.INIT, None, 1),
            pytest.param(OrderLogTariffState.OFF, UAChildType.OPTIMIZED, 1),
            pytest.param(OrderLogTariffState.OFF, UAChildType.LOG_TARIFF, 1),
            pytest.param(OrderLogTariffState.MIGRATED, None, 0),
            pytest.param(OrderLogTariffState.INIT, None, 0),
            pytest.param(OrderLogTariffState.OFF, UAChildType.OPTIMIZED, 0),
            pytest.param(OrderLogTariffState.OFF, UAChildType.LOG_TARIFF, 0),
        ]
    )
    #  если ежемесячная генерация, то не берём в расчет тарификационные заказы
    def test_month_generation_log_tariff_acts_over_completions(self, session, is_log_tariff, child_ua_type, force):
        session.config.__dict__['MONTH_PROC_ENQUEUE_RO_SESSION'] = 0

        client = ob.ClientBuilder.construct(session)
        order = create_order(session, client, DIRECT_PRODUCT_RUB_ID)
        order_log_tariff = create_order(session, client, DIRECT_PRODUCT_RUB_ID)

        invoice = create_invoice(session, client, [order], qty=66)
        invoice2 = create_invoice(session, client, [order, order_log_tariff], qty=66)

        order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: 666})
        order_log_tariff.calculate_consumption(datetime.datetime.now(), {order_log_tariff.shipment_type: 666})

        q = order_log_tariff.consumes[0]
        q.act_sum = 2000

        order_log_tariff._is_log_tariff = is_log_tariff
        order_log_tariff.child_ua_type = child_ua_type

        session.flush()

        act_enq = get_act_act_enqueuer(session, CUR_MONTH, force=force, op_type=8, client_ids=[client.id])
        act_enq.enqueue_acts()

        export_obj = session.query(mapper.Export).getone(object_id=client.id, type='MONTH_PROC', classname='Client')
        input_ = ut.Struct(export_obj.input)
        input_['invoices'] = sorted(input_['invoices'])

        if force and child_ua_type != UAChildType.OPTIMIZED:
            hamcrest.assert_that(
                input_['invoices'],
                hamcrest.has_length(2)
            )

            hamcrest.assert_that(
                input_,
                hamcrest.has_properties(
                    invoices=sorted([invoice.id, invoice2.id])
                )
            )

        else:
            hamcrest.assert_that(
                input_['invoices'],
                hamcrest.has_length(1)
            )

            hamcrest.assert_that(
                input_,
                hamcrest.has_properties(
                    invoices=sorted([invoice.id])
                )
            )


@pytest.mark.linked_clients
class TestSubclientLimit(object):

    def test_brands(self, session, agency, paysys):
        person = ob.PersonBuilder.construct(session, client=agency, type='ur')

        subclient1 = ob.ClientBuilder.construct(session, agency=agency)
        subclient2 = ob.ClientBuilder.construct(session, agency=agency)
        ob.create_brand(session, [(NEAR_PAST, [subclient1, subclient2])], CUR_MONTH.end_dt)

        contract = ob.create_credit_contract(
            session,
            agency,
            person,
            client_limits={
                subclient1.id: {'client_limit': 666},
            }
        )

        order1 = ob.OrderBuilder.construct(session, client=subclient1, agency=agency, product_id=DIRECT_PRODUCT_RUB_ID)
        order2 = ob.OrderBuilder.construct(session, client=subclient2, agency=agency, product_id=DIRECT_PRODUCT_RUB_ID)

        pa = consume_credit(contract, [(order1, 200), (order2, 300)], paysys.id)

        calculate_consumption(order1, PRESENT, 200)
        calculate_consumption(order2, PRESENT, 300)
        act, = generate_act(pa, CUR_MONTH)

        assert act.act_sum == 500
        assert pa.subclient_id == subclient1.id
        assert act.invoice.fictives == [pa]

    def test_brand_finished(self, session, agency, paysys):
        month = mapper.ActMonth()
        person = ob.PersonBuilder.construct(session, client=agency, type='ur')

        subclient1 = ob.ClientBuilder.construct(session, agency=agency)
        subclient2 = ob.ClientBuilder.construct(session, agency=agency)
        ob.create_brand(session, [(month.begin_dt, [subclient1, subclient2])], month.end_dt)

        contract = ob.create_credit_contract(
            session,
            agency,
            person,
            client_limits={
                subclient1.id: {'client_limit': 666},
            }
        )

        order1 = ob.OrderBuilder.construct(session, client=subclient1, agency=agency, product_id=DIRECT_PRODUCT_RUB_ID)
        order2 = ob.OrderBuilder.construct(session, client=subclient2, agency=agency, product_id=DIRECT_PRODUCT_RUB_ID)

        pa = consume_credit(contract, [(order1, 200)], paysys.id, month.document_dt)
        with mock.patch('balance.actions.transfers_qty.interface.check_invoice', return_value=None):
            create_consume(pa, order2, 300)

        calculate_consumption(order1, month.document_dt, 200)
        calculate_consumption(order2, month.document_dt, 300)

        act, = generate_act(pa, month)
        assert act.act_sum == 500

    def test_brand_finished_fail(self, session, agency, paysys):
        person = ob.PersonBuilder.construct(session, client=agency, type='ur')

        subclient1 = ob.ClientBuilder.construct(session, agency=agency)
        subclient2 = ob.ClientBuilder.construct(session, agency=agency)
        ob.create_brand(session, [(NEAR_PAST, [subclient1, subclient2])], PRESENT)

        contract = ob.create_credit_contract(
            session,
            agency,
            person,
            client_limits={
                subclient1.id: {'client_limit': 666},
            }
        )

        order1 = ob.OrderBuilder.construct(session, client=subclient1, agency=agency, product_id=DIRECT_PRODUCT_RUB_ID)
        order2 = ob.OrderBuilder.construct(session, client=subclient2, agency=agency, product_id=DIRECT_PRODUCT_RUB_ID)

        pa = consume_credit(contract, [(order1, 200)], paysys.id, NEAR_PAST)
        with mock.patch('balance.actions.transfers_qty.interface.check_invoice', return_value=None):
            create_consume(pa, order2, 300)

        calculate_consumption(order1, NEAR_PAST, 200)
        calculate_consumption(order2, NEAR_PAST, 300)

        with pytest.raises(exc.MIXED_CLIENTS_FOR_SUBCLIENT_PERSONAL_ACCOUNT):
            generate_act(pa, CUR_MONTH)


class TestActMinQty(object):
    PARAMS = [
        (1003, 0.000033, 0.000367)  # RUR
        , (1043, 0.002381, 0.026189)  # EUR
        , (1028, 0.002439, 0.026829)  # USD
        , (2701101, 0.001063, 0.011688)  # BYN
        , (1045, 0.002265, 0.024911)  # CHF
        , (2501020, 0.000008, 0.000094)  # KZT

    ]

    def create_invoice_with_consumprions_and_act(self, session, paysys, consumption_params):
        client = ob.ClientBuilder(is_agency=0).build(session).obj
        orders = [create_order(session, client) for _ in consumption_params]
        invoice = create_invoice(session, client, orders, paysys)
        for order in orders:
            order.calculate_consumption(datetime.datetime.now(),
                                        {order.shipment_type: consumption_params[orders.index(order)]})
        act, = invoice.generate_act(backdate=PRESENT, force=1)
        return act

    def check_is_external_act(self, act, is_external):
        hamcrest.assert_that(
            act,
            hamcrest.has_properties(
                is_external=is_external
            )
        )

    def check_under_limit(self, act, row, qty_under_limit):
        hamcrest.assert_that(
            act.rows[row],
            hamcrest.has_properties(
                act_qty=D(qty_under_limit).quantize(D("1.000000")),
                act_sum=D('0'),
                amount=D('0')
            )
        )

    def check_above_limit(self, act, row, qty_above_limit):
        hamcrest.assert_that(
            act.rows[row],
            hamcrest.has_properties(
                act_qty=D(qty_above_limit).quantize(D("1.000000")),
                act_sum=D('0.01'),
                amount=D('0.01')
            )
        )

    # Проверяем выставление акта, если в счете 2 открутки – одна из них меньше минимального значения
    # акт внешний, одна строчка нулевая, вторая - нет
    @pytest.mark.parametrize('paysys, qty_under_limit, qty_above_limit', PARAMS)
    def test_several_consumes_above_and_under_limit(self, session, paysys, qty_under_limit, qty_above_limit):
        act = self.create_invoice_with_consumprions_and_act(session, paysys, [qty_under_limit, qty_above_limit])
        self.check_is_external_act(act, is_external=1)
        self.check_under_limit(act, row=0, qty_under_limit=qty_under_limit)
        self.check_above_limit(act, row=1, qty_above_limit=qty_above_limit)

    # Проверяем выставление акта, если в счете 1 открутка и она меньше минимального значения
    #  акт внутренний, строчка нулевая
    @pytest.mark.parametrize('paysys, qty_under_limit, qty_above_limit', PARAMS)
    def test_consume_under_limit(self, session, paysys, qty_under_limit, qty_above_limit):
        act = self.create_invoice_with_consumprions_and_act(session, paysys, [qty_under_limit])
        self.check_is_external_act(act, is_external=0)
        self.check_under_limit(act, row=0, qty_under_limit=qty_under_limit)

    # Проверяем выставление акта, если в счете 1 открутка и она больше минимального значения
    #  акт внешний, строчка не нулевая
    @pytest.mark.parametrize('paysys, qty_under_limit, qty_above_limit', PARAMS)
    def test_consume_above_limit(self, session, paysys, qty_under_limit, qty_above_limit):
        act = self.create_invoice_with_consumprions_and_act(session, paysys, [qty_above_limit])
        self.check_is_external_act(act, is_external=1)
        self.check_above_limit(act, row=0, qty_above_limit=qty_above_limit)
