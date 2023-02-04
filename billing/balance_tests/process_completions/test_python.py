# -*- coding: utf-8 -*-

import decimal
import datetime

import hamcrest
import mock
import pytest

from balance import mapper
from balance.actions import acts as a_a
from balance.actions import consumption as a_c
from balance.actions import process_completions as a_pc
from balance import exc
from balance.constants import (
    UAChildType,
    OrderLogTariffState,
)

from tests import object_builder as ob

from tests.balance_tests.process_completions.common import (
    BaseProcessCompletionsTest,
    process_completions,
    create_order,
    migrate_client,
    NOW,
)

D = decimal.Decimal


class TestFair(object):
    def test_ok(self, order):
        process_completions(order, {'Bucks': 1})

        with mock.patch('balance.actions.process_completions.ProcessCompletions.process_completions') as p:
            a_pc.ProcessCompletions(order).calculate_consumption_fair({'Bucks': 6})

        assert p.called is False
        assert order.completion_qty == 6
        assert order.active_consume.completion_qty == 6

    def test_python_fallback(self, session, order):
        process_completions(order, {'Bucks': 1})
        order.active_consume_id = None  # не обрабатывается в plsql
        session.flush()

        with mock.patch('balance.actions.process_completions.ProcessCompletions.process_completions') as p:
            a_pc.ProcessCompletions(order).calculate_consumption_fair({'Bucks': 6})

        assert p.called is True


class TestPython(BaseProcessCompletionsTest):
    @staticmethod
    def _assert_shipment_dts(order):
        assert order.shipment_dt == order.shipment.dt
        assert order.shipment_update_dt == order.shipment.update_dt
        assert order.shipment_accepted.dt == order.shipment.dt
        assert order.shipment_accepted.update_dt == order.shipment.update_dt

    def test_forward(self, session):
        order = create_order(session, consumes_qtys=[10, 10, 10])
        c1, c2, c3 = order.consumes

        process_completions(order, {'Bucks': 17})

        assert order.completion_qty == 17
        self._assert_shipment_dts(order)
        assert order.active_consume_id == order.consumes[1].id
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 10, 300, 0, 0),
                (c2.invoice_id, 10, 300, 7, 210, 0, 0),
                (c3.invoice_id, 10, 300, 0, 0, 0, 0),
            ]
        )

    def test_overshipment(self, session):
        order = create_order(session, consumes_qtys=[10, 10, 10])
        c1, c2, c3 = order.consumes

        process_completions(order, {'Bucks': 666})

        assert order.completion_qty == 666
        self._assert_shipment_dts(order)
        assert order.active_consume_id is None
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 10, 300, 0, 0),
                (c2.invoice_id, 10, 300, 10, 300, 0, 0),
                (c3.invoice_id, 10, 300, 10, 300, 0, 0),
            ]
        )

    def test_rollback(self, session):
        order = create_order(session, consumes_qtys=[10, 10, 10])
        c1, c2, c3 = order.consumes

        # подготовительные открутки
        process_completions(order, {'Bucks': 23})

        # откат
        process_completions(order, {'Bucks': 8})

        assert order.completion_qty == 8
        self._assert_shipment_dts(order)
        assert order.active_consume_id == order.consumes[0].id
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 8, 240, 0, 0),
                (c2.invoice_id, 10, 300, 0, 0, 0, 0),
                (c3.invoice_id, 10, 300, 0, 0, 0, 0),
            ]
        )

        # проверяем t_reverse_completion
        reversed_completions = session.query(mapper.ReverseCompletion).filter_by(order=order).all()
        hamcrest.assert_that(
            reversed_completions,
            hamcrest.contains(hamcrest.has_properties(
                order_id=order.id,
                old_qty=23,
                qty=8
            ))
        )

    def test_rollback_acted(self, session):
        order = create_order(session, consumes_qtys=[10, 10, 10])
        c1, c2, c3 = order.consumes

        # подготовительные открутки
        process_completions(order, {'Bucks': 23})

        # акты
        a_a.ActAccounter(order.client, dps=[], backdate=NOW, force=1).do()
        session.flush()

        # откат
        process_completions(order, {'Bucks': 8})

        assert order.completion_qty == 8
        self._assert_shipment_dts(order)
        assert order.active_consume_id == order.consumes[0].id
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 8, 240, 10, 300),
                (c2.invoice_id, 10, 300, 0, 0, 10, 300),
                (c3.invoice_id, 10, 300, 0, 0, 3, 90),
            ]
        )

        # проверяем t_reverse_completion
        reversed_completions = session.query(mapper.ReverseCompletion).filter_by(order=order).all()
        hamcrest.assert_that(
            reversed_completions,
            hamcrest.contains(hamcrest.has_properties(
                order_id=order.id,
                old_qty=23,
                qty=8
            ))
        )

        # проверяем t_acted_completion
        acted_completions = (
            session.query(mapper.ActedCompletion)
                .join(mapper.Consume)
                .filter(mapper.Consume.order == order)
                .all()
        )
        hamcrest.assert_that(
            acted_completions,
            hamcrest.contains_inanyorder(
                hamcrest.has_properties(consume_id=c1.id, old_qty=10, qty=8, act_qty=10),
                hamcrest.has_properties(consume_id=c2.id, old_qty=10, qty=0, act_qty=10),
                hamcrest.has_properties(consume_id=c3.id, old_qty=3, qty=0, act_qty=3),
            )
        )

    def test_consistency_inconsequential_completions(self, session):
        order = create_order(session, consumes_qtys=[10, 10])
        c1, c2 = order.consumes

        process_completions(order, {'Bucks': 5})
        order.shipment.update(NOW, {'Bucks': 6})
        order.completion_qty = 6
        c2.completion_qty = 1
        c2.completion_sum = 30
        session.flush()

        process_completions(order, {'Bucks': 7})

        assert order.completion_qty == 7
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 7, 210, 0, 0),
                (c2.invoice_id, 10, 300, 0, 0, 0, 0),
            ]
        )

    def test_consistency_inconsequential_completions_multiple(self, session):
        order = create_order(session, consumes_qtys=[10, 10, 10])
        c1, c2, c3 = order.consumes

        process_completions(order, {'Bucks': 5})
        order.shipment.update(NOW, {'Bucks': 6})
        order.completion_qty = 6
        c3.completion_qty = 1
        c3.completion_sum = 30
        session.flush()

        process_completions(order, {'Bucks': 11})

        assert order.completion_qty == 11
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 10, 300, 0, 0),
                (c2.invoice_id, 10, 300, 1, 30, 0, 0),
                (c3.invoice_id, 10, 300, 0, 0, 0, 0),
            ]
        )

    def test_consistency_inconsequential_completions_rollback(self, session):
        order = create_order(session, consumes_qtys=[10, 10, 10])
        c1, c2, c3 = order.consumes

        process_completions(order, {'Bucks': 5})
        order.shipment.update(NOW, {'Bucks': 6})
        order.completion_qty = 6
        c3.completion_qty = 1
        c3.completion_sum = 30
        session.flush()

        process_completions(order, {'Bucks': 4})

        assert order.completion_qty == 4
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 4, 120, 0, 0),
                (c2.invoice_id, 10, 300, 0, 0, 0, 0),
                (c3.invoice_id, 10, 300, 0, 0, 0, 0),
            ]
        )

    def test_consistency_rollback_old(self, session):
        # https://st.yandex-team.ru/BALANCE-22673
        order = create_order(session, consumes_qtys=[10, 10, 10])
        c1, c2, c3 = order.consumes

        # Предварительные открутки
        process_completions(order, {'Bucks': 22})
        a_c.reverse_consume(c2, None, 4)

        process_completions(order, {'Bucks': 23})

        assert order.completion_qty == 23
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 10, 300, 0, 0),
                (c2.invoice_id, 6, 180, 6, 180, 0, 0),
                (c3.invoice_id, 10, 300, 7, 210, 0, 0),
            ]
        )

    def test_change_manager(self, session):
        order = create_order(session, consumes_qtys=[10, 10, 10])
        c1, c2, c3 = order.consumes

        # предварительные открутки
        process_completions(order, {'Bucks': 17})

        # меняем менеджера
        old_manager = order.manager
        order.manager = ob.ManagerWithChiefsBuilder(number=1).build(session).obj
        session.flush()

        # открутки
        process_completions(order, {'Bucks': 22})

        assert order.completion_qty == 22
        self._assert_shipment_dts(order)
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 10, 300, 0, 0, old_manager.manager_code),
                (c2.invoice_id, 7, 210, 7, 210, 0, 0, old_manager.manager_code),
                (c3.invoice_id, 0, 0, 0, 0, 0, 0, old_manager.manager_code),
                (c2.invoice_id, 3, 90, 3, 90, 0, 0, order.manager_code),
                (c3.invoice_id, 10, 300, 2, 60, 0, 0, order.manager_code),
            ],
            extra_params=['manager_code']
        )

    def test_change_manager_rollback(self, session):
        order = create_order(session, consumes_qtys=[10, 10, 10])
        c1, c2, c3 = order.consumes

        # предварительные открутки
        process_completions(order, {'Bucks': 17})

        # меняем менеджера
        old_manager = order.manager
        order.manager = ob.ManagerWithChiefsBuilder(number=1).build(session).obj
        session.flush()

        # открутки
        process_completions(order, {'Bucks': 11})

        assert order.completion_qty == 11
        self._assert_shipment_dts(order)
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 10, 300, 0, 0, old_manager.manager_code),
                (c2.invoice_id, 10, 300, 1, 30, 0, 0, old_manager.manager_code),
                (c3.invoice_id, 10, 300, 0, 0, 0, 0, old_manager.manager_code),
            ],
            extra_params=['manager_code']
        )

    def test_change_manager_overacted(self, session):
        order = create_order(session, consumes_qtys=[10, 10])
        c1, c2 = order.consumes

        # предварительные открутки
        process_completions(order, {'Bucks': 9})

        # акты
        a_a.ActAccounter(order.client, dps=[], backdate=NOW, force=1).do()
        session.flush()

        # меняем менеджера
        old_manager = order.manager
        order.manager = ob.ManagerWithChiefsBuilder(number=1).build(session).obj
        session.flush()

        # открутки
        process_completions(order, {'Bucks': 6})

        assert order.completion_qty == 6
        self._assert_shipment_dts(order)
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 6, 180, 9, 270, old_manager.manager_code),
                (c2.invoice_id, 10, 300, 0, 0, 0, 0, old_manager.manager_code),
            ],
            extra_params=['manager_code']
        )

    def test_dynamic_discount(self, session):
        order = create_order(session, consumes_qtys=[10])
        consume, = order.consumes

        # предварительные открутки
        process_completions(order, {'Bucks': D('3.333333')})

        # рисуем динамическую скидку
        ob.add_dynamic_discount(consume, 10)

        # открутки
        process_completions(order, {'Bucks': 6})
        assert order.completion_qty == 6
        self._assert_shipment_dts(order)
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, D('3.333333'), 90, D('3.333333'), 90, 0, 0, mapper.DiscountObj(0, dynamic_pct=10)),
                (consume.invoice_id, 7, 210, D('2.666667'), 80, 0, 0, mapper.DiscountObj()),
            ],
            extra_params=['discount_obj']
        )

    def test_on_dt(self, session):
        order = create_order(session, consumes_qtys=[10])
        consume, = order.consumes

        # история откруток
        first_dt = (NOW - datetime.timedelta(9)).replace(microsecond=0)
        process_completions(order, {order.shipment_type: 3}, dt=first_dt)
        order.shipment.update(NOW - datetime.timedelta(5), {order.shipment_type: 5})
        order.shipment.update(NOW - datetime.timedelta(1), {order.shipment_type: 7})

        # разбор откруток
        a_pc.ProcessCompletions(order, NOW - datetime.timedelta(3)).process_completions()

        assert order.completion_qty == 7
        assert order.shipment_dt == NOW - datetime.timedelta(1)
        assert order.shipment_accepted.dt == NOW - datetime.timedelta(1)
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 10, 300, 7, 210, 0, 0),
            ],
        )

    @pytest.mark.log_tariff
    @pytest.mark.parametrize(
        'is_log_tariff, child_ua_type',
        [
            pytest.param(OrderLogTariffState.INIT, None, id='main_init'),
            pytest.param(OrderLogTariffState.MIGRATED, None, id='main_migrated'),
            pytest.param(None, UAChildType.LOG_TARIFF, id='child'),
        ]
    )
    def test_log_tariff_completions(self, session, order, is_log_tariff, child_ua_type):
        consume, = order.consumes
        order._is_log_tariff = is_log_tariff
        order.child_ua_type = child_ua_type
        session.flush()

        with pytest.raises(exc.PROCESS_COMPLETION_FORBIDDEN) as exc_info:
            process_completions(order, {'Bucks': 6})

        assert 'completion change in log tariff order' in exc_info.value.msg
        assert order.completion_qty == 0
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 0, 0, 0, 0),
            ],
        )

    @pytest.mark.log_tariff
    def test_log_tariff_reconsume(self, session, order):
        consume, = order.consumes

        process_completions(order, {'Bucks': 6})

        order._is_log_tariff = OrderLogTariffState.INIT
        order.manager = ob.SingleManagerBuilder.construct(session)
        session.flush()

        with pytest.raises(exc.PROCESS_COMPLETION_FORBIDDEN) as exc_info:
            process_completions(order, {'Bucks': 7})

        assert 'update_consumes for log tariff order' in exc_info.value.msg
        assert order.completion_qty == 6
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 6, 6 * 30, 0, 0),
            ],
        )

    @pytest.mark.log_tariff
    def test_log_tariff_rollback_overcompletion(self, session, order):
        consume, = order.consumes

        process_completions(order, {'Bucks': 700})

        order._is_log_tariff = OrderLogTariffState.INIT
        order.manager = ob.SingleManagerBuilder.construct(session)
        session.flush()

        with pytest.raises(exc.PROCESS_COMPLETION_FORBIDDEN) as exc_info:
            process_completions(order, {'Bucks': D('665.99')})

        assert 'completion change in log tariff order' in exc_info.value.msg
        assert order.completion_qty == 700
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 666, 666 * 30, 0, 0),
            ],
        )

    @pytest.mark.log_tariff
    def test_log_tariff_unchanged(self, session, order):
        consume, = order.consumes
        session.flush()

        process_completions(order, {'Bucks': 6})

        order._is_log_tariff = OrderLogTariffState.INIT
        session.flush()

        process_completions(order, {'Bucks': 6})

        assert order.completion_qty == 6
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 6, 6 * 30, 0, 0),
            ]
        )

    @pytest.mark.log_tariff
    @pytest.mark.parametrize(
        'old_qty, new_qty',
        [
            pytest.param(666, 667, id='overcompletion'),
            pytest.param(667, 666, id='rollback'),
        ]
    )
    def test_log_tariff_unchanged_overcompletion(self, session, order, old_qty, new_qty):
        consume, = order.consumes
        session.flush()

        process_completions(order, {'Bucks': old_qty})

        order._is_log_tariff = OrderLogTariffState.INIT
        session.flush()

        process_completions(order, {'Bucks': new_qty})

        assert order.completion_qty == new_qty
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 666, 666 * 30, 0, 0),
            ]
        )


class TestDirectMigration(BaseProcessCompletionsTest):
    def test_base(self, session):
        order = create_order(session, consumes_qtys=[10, 10])
        c1, c2 = order.consumes

        # предварительные открутки
        process_completions(order, {'Bucks': 11})

        # мультивалютность
        migrate_client(order.client)

        # новые открутки
        process_completions(order, {'Bucks': 12, 'Money': 45})

        assert order.completion_qty == D('13.5')
        assert order.completion_fixed_qty == 12
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 10, 300, 0, 0),
                (c2.invoice_id, 10, 300, D('3.5'), 105, 0, 0),
            ]
        )

    def test_reverse_cross_fixed_qty(self, session):
        order = create_order(session)
        process_completions(order, {'Bucks': 2})
        migrate_client(order.client)
        process_completions(order, {'Bucks': 2, 'Money': 30})
        assert order.completion_fixed_qty == 2

        # откат денег, открутка фишек
        process_completions(order, {'Bucks': 3, 'Money': 0})
        assert order.completion_qty == 3
        assert order.completion_fixed_qty == 3

    def test_microfish(self, session):
        order = create_order(session, consumes_qtys=[100])
        process_completions(order, {'Bucks': 3})
        migrate_client(order.client)
        process_completions(order, {'Bucks': 3, 'Money': 60})
        assert order.completion_fixed_qty == 3

        # открутка микрофишки
        process_completions(order, {'Bucks': D('3.000001'), 'Money': 60})
        assert order.completion_fixed_qty == D('3.000001')
        assert order.completion_qty == D('5.000001')

        # нормальная откутка в деньгах
        process_completions(order, {'Bucks': D('3.000001'), 'Money': 120})

        assert order.completion_fixed_qty == D('3.000001')
        assert order.completion_qty == D('7.000001')
        self._assert_consumes(
            order,
            [(order.consumes[0].invoice_id, 100, 3000, D('7.000001'), 210, 0, 0)]
        )

    def test_dynamic_discount_rounding(self, session):
        order = create_order(session, consumes_qtys=[100])
        consume, = order.consumes

        process_completions(order, {'Bucks': D('2.222222')})
        ob.add_dynamic_discount(consume, D('0.999999'))

        migrate_client(order.client)

        # проверка, что сконвертированные фишки равны деньгам
        cons_sum = sum(
            co.current_sum * co.invoice.exact_internal_rate
            for co in order.consumes
        )
        assert order.consume_money_qty == cons_sum

        free_cons_sum = sum(
            (co.current_sum - co.completion_sum) * co.invoice.exact_internal_rate
            for co in order.consumes
        )
        assert order.consume_money_qty - order.completion_money_qty == free_cons_sum

    def test_completions_after_migration_dt(self, session):
        order = create_order(session, consumes_qtys=[10, 10])
        c1, c2 = order.consumes

        process_completions(order, {'Bucks': 7}, dt=NOW - datetime.timedelta(2))
        order.shipment.update(NOW - datetime.timedelta(1), {'Bucks': 8})
        order.shipment.update(NOW + datetime.timedelta(1), {'Bucks': 11})
        session.flush()

        migrate_client(order.client)

        a_pc.ProcessCompletions(order, NOW - datetime.timedelta(1)).process_completions()

        assert order.completion_qty == D('11')
        assert order.shipment_dt == NOW + datetime.timedelta(1)
        assert order.shipment_accepted.dt == NOW + datetime.timedelta(1)
        assert order.completion_fixed_qty == 0
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 10, 300, 10, 300, 0, 0),
                (c2.invoice_id, 10, 300, 1, 30, 0, 0),
            ]
        )
