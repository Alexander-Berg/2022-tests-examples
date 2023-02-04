# -*- coding: utf-8 -*-

import datetime
import decimal
import contextlib

import pytest
import mock

from balance import mapper
from balance import muzzle_util as ut
from balance.actions import taxes_update
from balance.actions.process_completions.log_tariff import ProcessLogTariff
from balance import constants as cst

from tests import object_builder as ob

from tests.balance_tests.process_completions.common import (
    BaseProcessCompletionsTest,
    process_completions_plsql,
    process_completions,
    create_order,
    migrate_client,
    create_order_task,
    assert_task_consumes,
    NOW,
)

pytestmark = [
    pytest.mark.taxes_update
]

D = decimal.Decimal
PAST = datetime.datetime(2000, 1, 1)
TODAY = ut.trunc_date(NOW)
NEAR_PAST = NOW - datetime.timedelta(1)
FUTURE = NOW + datetime.timedelta(1)
FAR_FUTURE = NOW + datetime.timedelta(2)


@contextlib.contextmanager
def mock_policy(update_policy):

    def mock_property(self):
        if self.old_tax_policy_pct == self.adjusted_tax_policy_pct:
            return None
        else:
            return update_policy

    patcher = mock.patch('balance.actions.taxes_update.TaxUpdater.policy', property(mock_property))
    patcher.start()
    yield
    patcher.stop()


def create_direct_fish_product(session):
    tax_policy = ob.TaxPolicyBuilder(
        tax_pcts=[(PAST, 18), (NOW, 20)]
    ).build(session).obj
    tpp1, tpp2 = tax_policy.taxes

    product = ob.ProductBuilder(
        taxes=tax_policy,
        prices=[
            (PAST, 'RUR', 30, tpp1),
            (NOW, 'RUR', 30, tpp2),
        ]
    ).build(session).obj

    session.expire_all()
    return product


def create_direct_currency_product(session):
    product = ob.ProductBuilder(
        unit=ob.Getter(mapper.ProductUnit, cst.DIRECT_RUB_UNIT_ID),
        taxes=ob.TaxPolicyBuilder(
            tax_pcts=[(PAST, 18), (NOW, 20)]
        ),
        create_price=False,
    ).build(session).obj
    session.expire_all()
    return product


def create_product_price_wo_tax(session):
    product = ob.ProductBuilder(
        taxes=ob.TaxPolicyBuilder(
            tax_pcts=[(PAST, 18), (NOW, 20)]
        ),
        prices=[
            (PAST, 'RUR', 100),
            (NOW, 'RUR', 100),
        ]
    ).build(session).obj

    session.expire_all()
    return product


class TestShipment(BaseProcessCompletionsTest):

    @pytest.mark.parametrize(
        'update_policy', [
            taxes_update.TaxUpdatePolicy.UPDATE_QTY,
            taxes_update.TaxUpdatePolicy.UPDATE_SUM
        ]
    )
    def test_price_w_tax_fish(self, session, update_policy):
        # инициализация продукта
        product = create_direct_fish_product(session)
        price1, price2 = product.prices
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        # заказ и открутки до налога
        order = create_order(session, product.id, [100, 666], dt=NEAR_PAST)
        process_completions(order, {'Bucks': 10}, dt=NEAR_PAST)
        c1, c2 = order.consumes

        # открутки после смены налога
        with mock_policy(update_policy):
            process_completions(order, {'Bucks': 15})

        assert order.completion_qty == 15
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 10, 300, 0, 0, tpp1.id, price1.id),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp1.id, price1.id),
                (c1.invoice_id, 90, 2700, 5, 150, 0, 0, tpp2.id, price1.id),
                (c2.invoice_id, 666, 19980, 0, 0, 0, 0, tpp2.id, price1.id),
            ],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert c1.invoice.consume_sum == 100 * 30
        assert c2.invoice.consume_sum == 666 * 30

    def test_price_w_tax_markups(self, session):
        # инициализация продукта
        product = create_direct_fish_product(session)
        price1, price2 = product.prices
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        with mock.patch('balance.mapper.orders.Order.product_markups_by_date', return_value=[ut.Struct(pct=66)]):
            # заказ и открутки до налога
            order = create_order(session, product.id, [100, 666], dt=NEAR_PAST)
            process_completions(order, {'Bucks': 10}, dt=NEAR_PAST)
            c1, c2 = order.consumes

            # открутки после смены налога
            process_completions(order, {'Bucks': 15})

        assert order.completion_qty == 15
        price_coeff = 30 * D('1.66')
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 10 * price_coeff, 10, 10 * price_coeff, 0, 0, tpp1.id, price1.id),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp1.id, price1.id),
                (c1.invoice_id, 90, 90 * price_coeff, 5, 5 * price_coeff, 0, 0, tpp2.id, price1.id),
                (c2.invoice_id, 666, 666 * price_coeff, 0, 0, 0, 0, tpp2.id, price1.id),
            ],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert c1.invoice.consume_sum == 100 * price_coeff
        assert c2.invoice.consume_sum == 666 * price_coeff

    @pytest.mark.parametrize(
        'update_policy', [
            taxes_update.TaxUpdatePolicy.UPDATE_QTY,
            taxes_update.TaxUpdatePolicy.UPDATE_SUM
        ]
    )
    def test_currency(self, session, update_policy):
        # инициализация продукта
        product = create_direct_currency_product(session)
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        # заказ и открутки до налога
        order = create_order(session, product.id, [100, 666], dt=NEAR_PAST)
        process_completions(order, {'Money': 66}, dt=NEAR_PAST)
        c1, c2 = order.consumes

        # открутки после смены налога
        with mock_policy(update_policy):
            process_completions(order, {'Money': 70})

        assert order.completion_qty == 70
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 66, 66, 66, 66, 0, 0, tpp1.id, None),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp1.id, None),
                (c1.invoice_id, 34, 34, 4, 4, 0, 0, tpp2.id, None),
                (c2.invoice_id, 666, 666, 0, 0, 0, 0, tpp2.id, None),
            ],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert c1.invoice.consume_sum == 100
        assert c2.invoice.consume_sum == 666

    def test_price_wo_tax_update_sum(self, session):
        # инициализация продукта
        product = create_product_price_wo_tax(session)
        price1, price2 = product.prices
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        # заказ и открутки до налога
        order = create_order(session, product.id, [15, 20], dt=NEAR_PAST)
        process_completions(order, {'Bucks': 10}, dt=NEAR_PAST)
        c1, c2 = order.consumes

        # открутки после смены налога
        with mock_policy(taxes_update.TaxUpdatePolicy.UPDATE_SUM):
            process_completions(order, {'Bucks': 21})

        assert order.completion_qty == 21
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 1180, 10, 1180, 0, 0, tpp1.id, price1.id),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp1.id, price1.id),
                (c1.invoice_id, 5, 600, 5, 600, 0, 0, tpp2.id, price1.id),
                (c2.invoice_id, 20, 2400, 6, 720, 0, 0, tpp2.id, price1.id),
            ],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert c1.invoice.consume_sum == 10 * 118 + 5 * 120
        assert c2.invoice.consume_sum == 20 * 120

    def test_price_wo_tax_update_sum_discount(self, session):
        # инициализация продукта
        product = create_product_price_wo_tax(session)
        price1, price2 = product.prices
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        # заказ и открутки до налога
        order = create_order(session, product.id, [15, 20], discount_pct=16, dt=NEAR_PAST)
        process_completions(order, {'Bucks': 10}, dt=NEAR_PAST)
        c1, c2 = order.consumes

        # открутки после смены налога
        with mock_policy(taxes_update.TaxUpdatePolicy.UPDATE_SUM):
            process_completions(order, {'Bucks': 21})

        assert order.completion_qty == 21
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, D('991.2'), 10, D('991.2'), 0, 0, tpp1.id, price1.id, 16),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp1.id, price1.id, 16),
                (c1.invoice_id, 5, 504, 5, 504, 0, 0, tpp2.id, price1.id, 16),
                (c2.invoice_id, 20, 2016, 6, D('604.8'), 0, 0, tpp2.id, price1.id, 16),
            ],
            extra_params=['tax_policy_pct_id', 'price_id', 'discount_pct']
        )
        assert c1.invoice.consume_sum == (10 * 118 + 5 * 120) * D('0.84')
        assert c2.invoice.consume_sum == 20 * 120 * D('0.84')

    def test_price_wo_tax_update_qty(self, session):
        # инициализация продукта
        product = create_product_price_wo_tax(session)
        price1, price2 = product.prices
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        # заказ и открутки до налога
        order = create_order(session, product.id, [15, 20], dt=NEAR_PAST)
        process_completions(order, {'Bucks': 10}, dt=NEAR_PAST)
        c1, c2 = order.consumes

        # открутки после смены налога
        with mock_policy(taxes_update.TaxUpdatePolicy.UPDATE_QTY):
            process_completions(order, {'Bucks': 21})

        assert order.completion_qty == 21
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 1180, 10, 1180, 0, 0, tpp1.id, price1.id),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp1.id, price1.id),
                (c1.invoice_id, D('4.916667'), 590, D('4.916667'), 590, 0, 0, tpp2.id, price1.id),
                (c2.invoice_id, D('19.666667'), 2360, D('6.083333'), 730, 0, 0, tpp2.id, price1.id),
            ],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert c1.invoice.consume_sum == 15 * 118
        assert c2.invoice.consume_sum == 20 * 118

    def test_price_wo_tax_update_qty_discount(self, session):
        # инициализация продукта
        product = create_product_price_wo_tax(session)
        price1, price2 = product.prices
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        # заказ и открутки до налога
        order = create_order(session, product.id, [15, 20], discount_pct=16, dt=NEAR_PAST)
        process_completions(order, {'Bucks': 10}, dt=NEAR_PAST)
        c1, c2 = order.consumes

        # открутки после смены налога
        with mock_policy(taxes_update.TaxUpdatePolicy.UPDATE_QTY):
            process_completions(order, {'Bucks': 21})

        assert order.completion_qty == 21
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, D('991.2'), 10, D('991.2'), 0, 0, tpp1.id, price1.id, 16),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp1.id, price1.id, 16),
                (c1.invoice_id, D('4.916667'), D('495.6'), D('4.916667'), D('495.6'), 0, 0, tpp2.id, price1.id, 16),
                (c2.invoice_id, D('19.666667'), D('1982.4'), D('6.083333'), D('613.2'), 0, 0, tpp2.id, price1.id, 16),
            ],
            extra_params=['tax_policy_pct_id', 'price_id', 'discount_pct']
        )
        assert c1.invoice.consume_sum == 15 * 118 * D('0.84')
        assert c2.invoice.consume_sum == 20 * 118 * D('0.84')

    def test_price_wo_tax_markups(self, session):
        # инициализация продукта
        product = create_product_price_wo_tax(session)
        price1, price2 = product.prices
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        with mock.patch('balance.mapper.orders.Order.product_markups_by_date', return_value=[ut.Struct(pct=66)]):
            # заказ и открутки до налога
            order = create_order(session, product.id, [15, 20], dt=NEAR_PAST)
            process_completions(order, {'Bucks': 10}, dt=NEAR_PAST)
            c1, c2 = order.consumes

            # открутки после смены налога
            process_completions(order, {'Bucks': 21})

        assert order.completion_qty == 21
        old_price_coeff = 100 * D('1.18') * D('1.66')
        new_price_coeff = 100 * D('1.20') * D('1.66')
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 10 * old_price_coeff, 10, 10 * old_price_coeff, 0, 0, tpp1.id, price1.id),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp1.id, price1.id),
                (c1.invoice_id, 5, 5 * new_price_coeff, 5, 5 * new_price_coeff, 0, 0, tpp2.id, price1.id),
                (c2.invoice_id, 20, 20 * new_price_coeff, 6, 6 * new_price_coeff, 0, 0, tpp2.id, price1.id),
            ],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert c1.invoice.consume_sum == 10 * old_price_coeff + 5 * new_price_coeff
        assert c2.invoice.consume_sum == 20 * new_price_coeff

    def test_price_change(self, session):
        # продукт
        tax_policy = ob.TaxPolicyBuilder(
            tax_pcts=[(PAST, 18)]
        ).build(session).obj
        tpp, = tax_policy.taxes

        product = ob.ProductBuilder(
            taxes=tax_policy,
            prices=[
                (PAST, 'RUR', 100, tpp),
                (TODAY, 'RUR', 200, tpp)
            ]
        ).build(session).obj
        price, _ = product.prices

        # заказ
        order = create_order(session, product.id, [10], dt=NEAR_PAST)
        process_completions(order, {'Bucks': 2}, dt=NEAR_PAST)
        c1, = order.consumes

        # новые открутки
        process_completions(order, {'Bucks': 5})

        assert order.completion_qty == 5
        self._assert_consumes(
            order,
            [(c1.invoice_id, 10, 1000, 5, 500, 0, 0, tpp.id, price.id)],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert c1.invoice.consume_sum == 1000

    def test_price_tax_change(self, session):
        # продукт
        old_policy = ob.TaxPolicyBuilder(
            tax_pcts=[(PAST, 18), (TODAY, 20)]
        ).build(session).obj
        tpp1, _ = old_policy.taxes

        new_policy = ob.TaxPolicyBuilder(
            tax_pcts=[(PAST, 10)]
        ).build(session).obj
        tpp2, = new_policy.taxes

        product = ob.ProductBuilder(
            taxes=[
                (PAST, old_policy),
                (TODAY, new_policy)
            ],
            prices=[
                (PAST, 'RUR', 100, tpp1),
                (TODAY, 'RUR', 100, tpp2),
            ]
        ).build(session).obj
        price, _ = product.prices

        # заказ
        order = create_order(session, product.id, [10], dt=NEAR_PAST)
        process_completions(order, {'Bucks': 2}, dt=NEAR_PAST)
        c1, = order.consumes

        # новые открутки
        process_completions(order, {'Bucks': 5})

        assert order.completion_qty == 5
        self._assert_consumes(
            order,
            [(c1.invoice_id, 10, 1000, 5, 500, 0, 0, tpp1.id, price.id)],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert c1.invoice.consume_sum == 1000

    def test_plsql(self, session):
        product = create_direct_fish_product(session)
        order = create_order(session, product.id, [10], dt=NEAR_PAST)
        consume, = order.consumes
        process_completions(order, {'Bucks': 1}, dt=NEAR_PAST)

        process_completions_plsql(order, {'Bucks': 2})

        assert order.completion_qty == 1
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 10, 300, 1, 30, 0, 0),
            ]
        )
        self._assert_enqueued(order, reason='ORA-20666: Tax change for active consume')

    def test_refresh_tax_policy_pct(self, session):
        product = ob.ProductBuilder(
            unit=ob.Getter(mapper.ProductUnit, cst.DIRECT_RUB_UNIT_ID),
            taxes=ob.TaxPolicyBuilder(
                tax_pcts=[(PAST, 18), (NOW, 20)]
            ),
            create_price=False,
        ).build(session).obj
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        order = create_order(session, product.id, [10], dt=NEAR_PAST)
        order.completion_qty = 1
        consume, = order.consumes
        consume.tax_policy_pct_id = None
        consume.dt = NEAR_PAST
        consume.completion_qty = 1
        consume.completion_sum = 1
        session.flush()
        session.expire_all()

        process_completions(order, {'Money': 3}, dt=NEAR_PAST)

        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 1, 1, 1, 1, 0, 0, None, None),
                (consume.invoice_id, 9, 9, 2, 2, 0, 0, tpp1.id, None)
            ],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert consume.invoice.consume_sum == 10

    def test_from_after_on_before(self, session):
        # инициализация продукта
        product = create_direct_fish_product(session)
        price1, price2 = product.prices
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        # зачисления после налога
        order = create_order(session, product.id, [100, 666], dt=NOW)
        c1, c2 = order.consumes

        # открутки до налога
        process_completions(order, {'Bucks': 10}, dt=NEAR_PAST)

        # открутки после налога
        process_completions(order, {'Bucks': 66}, dt=NOW)

        assert order.completion_qty == 66
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 0, 0, 0, 0, 0, 0, tpp2.id, price2.id),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp2.id, price2.id),
                (c1.invoice_id, 10, 10 * 30, 10, 10 * 30, 0, 0, tpp1.id, price2.id),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp1.id, price2.id),
                (c1.invoice_id, 90, 90 * 30, 56, 56 * 30, 0, 0, tpp2.id, price2.id),
                (c2.invoice_id, 666, 666 * 30, 0, 0, 0, 0, tpp2.id, price2.id),
            ],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert c1.invoice.consume_sum == 100 * 30
        assert c2.invoice.consume_sum == 666 * 30

    @pytest.mark.dont_mock_mnclose
    def test_shipment_closed_month(self, session):
        # инициализация продукта
        product = create_direct_currency_product(session)
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        # заказ
        order = create_order(session, product.id, [666], dt=NEAR_PAST)
        c, = order.consumes

        # Открутки до смены налога
        with mock.patch('balance.mncloselib.is_month_closed', return_value=False):
            process_completions(order, {'Money': 66}, dt=NEAR_PAST)

        session.clear_cache()

        # Переразбор откруток после смены налога
        with mock.patch('balance.mncloselib.is_month_closed', return_value=True):
            process_completions(order, {'Money': 70}, dt=NEAR_PAST)

        assert order.completion_qty == 70
        self._assert_consumes(
            order,
            [
                (c.invoice_id, 66, 66, 66, 66, 0, 0, tpp1.id, None),
                (c.invoice_id, 600, 600, 4, 4, 0, 0, tpp2.id, None),
            ],
            extra_params=['tax_policy_pct_id', 'price_id']
        )


@pytest.mark.log_tariff
class TestLogTariff(BaseProcessCompletionsTest):
    @pytest.mark.parametrize(
        'update_policy', [
            taxes_update.TaxUpdatePolicy.UPDATE_QTY,
            taxes_update.TaxUpdatePolicy.UPDATE_SUM
        ]
    )
    def test_direct(self, session, update_policy):
        product = create_direct_currency_product(session)
        tax, = product.taxes
        tpp1, tpp2 = tax.policy.taxes

        # заказ и открутки до налога
        order = create_order(session, product.id, [100, 666], dt=NEAR_PAST)
        migrate_client(order.client)
        process_completions(order, {'Money': 66}, dt=NEAR_PAST)
        c1, c2 = order.consumes

        task = create_order_task(order, 666)

        # открутки после смены налога
        with mock_policy(update_policy):
            ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == 732
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 66, 66, 66, 66, 0, 0, tpp1.id, None),
                (c2.invoice_id, 0, 0, 0, 0, 0, 0, tpp1.id, None),
                (c1.invoice_id, 34, 34, 34, 34, 0, 0, tpp2.id, None),
                (c2.invoice_id, 666, 666, 632, 632, 0, 0, tpp2.id, None),
            ],
            extra_params=['tax_policy_pct_id', 'price_id']
        )
        assert c1.invoice.consume_sum == 100
        assert c2.invoice.consume_sum == 666

        assert_task_consumes(
            task,
            [
                (order.consumes[2].id, 34, 34, 34, 34),
                (order.consumes[3].id, 632, 632, 666, 666),
            ]
        )
