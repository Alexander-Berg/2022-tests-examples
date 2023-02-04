# -*- coding: utf-8 -*-

import decimal

import pytest
import hamcrest

from balance import mapper
from balance.constants import (
    ExportState,
    UAChildType,
    OrderLogTariffState,
)

from tests import object_builder as ob

from tests.balance_tests.process_completions.common import (
    BaseProcessCompletionsTest,
    process_completions_plsql,
    process_completions_plsql_batch,
    process_completions,
    create_order,
    migrate_client,
    NOW,
)

D = decimal.Decimal



class TestClusterTools(object):
    def test_ok(self, session, order):
        order.active_consume_id = order.consumes[0].id
        session.flush()

        process_completions_plsql_batch(order, {'Bucks': 1})

        assert order.completion_qty == 1
        assert 'PROCESS_COMPLETION' not in order.exports

    def test_fail(self, session, order):
        process_completions_plsql_batch(order, {'Bucks': 1}, stop=1)
        export = order.exports['PROCESS_COMPLETION']

        assert order.completion_qty == 0
        assert export.state == ExportState.enqueued
        assert export.reason == 'ORA-20666: got stop flag'


class TestPLSQL(BaseProcessCompletionsTest):
    def test_ok(self, session, order):
        consume, = order.consumes
        order.active_consume_id = consume.id
        session.flush()
        process_completions_plsql(order, {'Bucks': 2})

        assert order.completion_qty == 2
        self._assert_consumes(
            order,
            [(consume.invoice_id, 666, 666 * 30, 2, 60, 0, 0)]
        )
        self._assert_enqueued(order, expected_state=None)

    def test_stop_flag(self, session, order):
        consume, = order.consumes
        process_completions_plsql(order, {'Bucks': 1}, stop=1)

        assert order.completion_qty == 0
        self._assert_consumes(
            order,
            [(consume.invoice_id, 666, 666 * 30, 0, 0, 0, 0)]
        )
        self._assert_enqueued(order, reason='ORA-20666: got stop flag')

    def test_need_actual_completions(self, session, order):
        consume, = order.consumes
        order.need_actual_completions = 1
        session.flush()

        process_completions_plsql(order, {'Bucks': 1})

        assert order.completion_qty == 0
        self._assert_consumes(
            order,
            [(consume.invoice_id, 666, 666 * 30, 0, 0, 0, 0)]
        )
        self._assert_enqueued(order, reason='ORA-20666: need_actual_completions flag found')

    def test_null_consume(self, order):
        consume, = order.consumes
        process_completions_plsql(order, {'Bucks': 1})

        assert order.completion_qty == 0
        self._assert_consumes(
            order,
            [(consume.invoice_id, 666, 666 * 30, 0, 0, 0, 0)]
        )
        self._assert_enqueued(order, reason='ORA-20666: p_order_active_consume_id IS NULL')

    def test_change_shipment_type(self, order):
        consume, = order.consumes
        process_completions_plsql(order, {'Bucks': 1, 'Money': 2})

        assert order.completion_qty == 0
        self._assert_consumes(
            order,
            [(consume.invoice_id, 666, 666 * 30, 0, 0, 0, 0)]
        )
        self._assert_enqueued(order, reason='ORA-20666: p_order_active_consume_id IS NULL')

    def test_over_consume(self, session):
        order = create_order(session, consumes_qtys=[6, 6, 6])
        c1, c2, c3 = order.consumes
        process_completions(order, {'Bucks': 4})
        assert order.completion_qty == 4

        process_completions_plsql(order, {'Bucks': 7})

        assert order.completion_qty == 4
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 6, 180, 4, 120, 0, 0),
                (c2.invoice_id, 6, 180, 0, 0, 0, 0),
                (c3.invoice_id, 6, 180, 0, 0, 0, 0),
            ]
        )
        self._assert_enqueued(order, reason='ORA-20666: Operation don`t use one consume')

    def test_rollback(self, session, order):
        consume, = order.consumes
        process_completions(order, {'Bucks': 10})
        process_completions_plsql(order, {'Bucks': 6})

        assert order.completion_qty == 6
        self._assert_consumes(
            order,
            [(consume.invoice_id, 666, 666 * 30, 6, 180, 0, 0)]
        )
        self._assert_enqueued(order, expected_state=None)

        reversed_completions = session.query(mapper.ReverseCompletion).filter_by(order=order).all()
        hamcrest.assert_that(
            reversed_completions,
            hamcrest.contains(hamcrest.has_properties(
                order_id=order.id,
                old_qty=10,
                qty=6
            ))
        )

    def test_rollback_over_consume(self, session):
        order = create_order(session, consumes_qtys=[6, 6])
        c1, c2 = order.consumes
        process_completions(order, {'Bucks': 10})
        process_completions_plsql(order, {'Bucks': 5})

        assert order.completion_qty == 10
        self._assert_consumes(
            order,
            [
                (c1.invoice_id, 6, 180, 6, 180, 0, 0),
                (c2.invoice_id, 6, 180, 4, 120, 0, 0),
            ]
        )
        self._assert_enqueued(order, reason='ORA-20666: Operation don`t use one consume')

    def test_rollback_acted(self, order):
        consume, = order.consumes
        invoice = consume.invoice

        process_completions(order, {'Bucks': 6})
        invoice.generate_act(force=1, backdate=NOW)
        process_completions(order, {'Bucks': 10})
        process_completions_plsql(order, {'Bucks': 6})

        assert order.completion_qty == 6
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 6, 180, 6, 180),
            ]
        )
        self._assert_enqueued(order, expected_state=None)

    def test_rollback_acted_fail(self, order):
        consume, = order.consumes
        invoice = consume.invoice

        process_completions(order, {'Bucks': 6})
        invoice.generate_act(force=1, backdate=NOW)
        process_completions(order, {'Bucks': 10})
        process_completions_plsql(order, {'Bucks': 5})

        assert order.completion_qty == 10
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 10, 300, 6, 180),
            ]
        )
        self._assert_enqueued(order, reason='ORA-20666: descended into act')

    def test_overshipment_over_consume(self, session):
        order = create_order(session, consumes_qtys=[10])
        consume, = order.consumes
        process_completions(order, {'Bucks': 4})
        assert order.completion_qty == 4

        process_completions_plsql(order, {'Bucks': 11})

        assert order.completion_qty == 4
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 10, 300, 4, 120, 0, 0),
            ]
        )
        self._assert_enqueued(order, reason='ORA-20666: Operation don`t use one consume')

    def test_overshipment(self, session):
        order = create_order(session, consumes_qtys=[10])
        consume, = order.consumes
        process_completions(order, {'Bucks': 11})
        assert order.completion_qty == 11

        process_completions_plsql(order, {'Bucks': 12})

        assert order.completion_qty == 12
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 10, 300, 10, 300, 0, 0),
            ]
        )
        self._assert_enqueued(order, expected_state=None)

    def test_overshipment_rollback(self, session):
        order = create_order(session, consumes_qtys=[10])
        consume, = order.consumes
        process_completions(order, {'Bucks': 12})
        assert order.completion_qty == 12

        process_completions_plsql(order, {'Bucks': 11})

        assert order.completion_qty == 11
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 10, 300, 10, 300, 0, 0),
            ]
        )
        self._assert_enqueued(order, expected_state=None)

    def test_change_manager(self, session, order):
        consume, = order.consumes
        process_completions(order, {'Bucks': 1})

        old_manager = order.manager.manager_code
        order.manager = ob.ManagerWithChiefsBuilder(number=1).build(session).obj
        session.flush()
        new_manager = order.manager.manager_code

        process_completions_plsql(order, {'Bucks': 2})
        assert order.completion_qty == 1
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 1, 30, 0, 0),
            ]
        )
        self._assert_enqueued(order, reason=u'ORA-20666: Manager changed %s -> %s' % (old_manager, new_manager))

    def test_change_discount(self, session, order):
        consume, = order.consumes
        process_completions(order, {'Bucks': 10})

        ob.add_dynamic_discount(consume, 10)
        session.flush()

        process_completions_plsql(order, {'Bucks': 20})
        assert order.completion_qty == 10
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 740, 666 * 30, 10, 270, 0, 0),
            ]
        )
        self._assert_enqueued(order, reason=u'ORA-20666: Has dynamic discount 10')

    def test_empty(self, session, order):
        consume, = order.consumes
        order.active_consume_id = consume.id
        session.flush()

        process_completions_plsql(order, None)
        assert order.completion_qty == 0
        self._assert_enqueued(order, expected_state=None)

    @pytest.mark.log_tariff
    @pytest.mark.parametrize(
        'is_log_tariff, child_ua_type',
        [
            pytest.param(OrderLogTariffState.INIT, None, id='main_init'),
            pytest.param(OrderLogTariffState.MIGRATED, None, id='main_migrated'),
            pytest.param(None, UAChildType.LOG_TARIFF, id='child'),
        ]
    )
    def test_log_tariff(self, session, order, is_log_tariff, child_ua_type):
        consume, = order.consumes
        order._is_log_tariff = is_log_tariff
        order.child_ua_type = child_ua_type
        order.active_consume_id = consume.id
        session.flush()

        process_completions_plsql(order, {'Bucks': 6})
        assert order.completion_qty == 0
        self._assert_enqueued(order, reason=u'ORA-20666: changing consume completion for log_tariff order')

    @pytest.mark.log_tariff
    def test_log_tariff_unchanged(self, session, order):
        consume, = order.consumes
        order.active_consume_id = consume.id
        session.flush()

        process_completions_plsql(order, {'Bucks': 6})

        order._is_log_tariff = OrderLogTariffState.INIT
        session.flush()

        process_completions_plsql(order, {'Bucks': 6})

        assert order.completion_qty == 6
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 6, 6 * 30, 0, 0),
            ]
        )
        self._assert_enqueued(order, expected_state=None)

    @pytest.mark.log_tariff
    @pytest.mark.parametrize(
        'new_completion_qty',
        [
            pytest.param(701, id='completion'),
            pytest.param(699, id='rollback'),
        ]
    )
    def test_log_tariff_overcompletion(self, session, order, new_completion_qty):
        consume, = order.consumes
        order.active_consume_id = consume.id
        session.flush()

        process_completions(order, {'Bucks': 700})

        order._is_log_tariff = OrderLogTariffState.INIT
        session.flush()

        process_completions_plsql(order, {'Bucks': new_completion_qty})

        assert order.completion_qty == new_completion_qty
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 666, 666 * 30, 0, 0),
            ]
        )
        self._assert_enqueued(order, expected_state=None)


class TestDirectMigration(BaseProcessCompletionsTest):

    def test_w_dynamic_discount(self, session, order):
        consume, = order.consumes
        process_completions(order, {'Bucks': 1})

        migrate_client(order.client)
        ob.add_dynamic_discount(consume, 10)
        session.flush()

        process_completions_plsql(order, {'Bucks': 1, 'Money': 1})
        assert order.completion_qty == 1
        assert order.completion_fixed_qty == 0
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 740, 666 * 30, 1, 27, 0, 0),
            ]
        )
        self._assert_enqueued(order, reason='ORA-20666: fixed part of money consumption changed 0 -> 1')

    def test_migrated(self, session, order):
        consume, = order.consumes
        process_completions(order, {'Bucks': 1})
        migrate_client(order.client)
        process_completions(order, {'Bucks': 1, 'Money': 30})

        process_completions_plsql(order, {'Bucks': 1, 'Money': 45})
        assert order.completion_qty == D('2.5')
        assert order.completion_fixed_qty == 1
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, D('2.5'), 75, 0, 0),
            ]
        )
        self._assert_enqueued(order, expected_state=None)

    def test_reverse_cross_fixed_qty(self, session, order):
        consume, = order.consumes
        process_completions(order, {'Bucks': 1})
        migrate_client(order.client)
        process_completions(order, {'Bucks': 1, 'Money': 30})
        assert order.completion_qty == 2

        process_completions_plsql(order, {'Bucks': 3, 'Money': 0})
        assert order.completion_qty == 2
        assert order.completion_fixed_qty == 1
        self._assert_consumes(
            order,
            [
                (consume.invoice_id, 666, 666 * 30, 2, 60, 0, 0),
            ]
        )
        self._assert_enqueued(order, reason='ORA-20666: fixed part of money consumption changed 1 -> 3')

    def test_empty(self, session, order):
        consume, = order.consumes
        order.active_consume_id = consume.id
        session.flush()

        migrate_client(order.client)
        process_completions_plsql(order, {'Bucks': 0})
        assert order.completion_qty == 0
        self._assert_enqueued(order, expected_state=None)

    def test_no_consumes(self, session):
        order = create_order(session, consumes_qtys=[])
        migrate_client(order.client)

        process_completions_plsql(order, {'Bucks': 0, 'Money': 660})
        assert order.completion_qty == 22
        self._assert_enqueued(order, expected_state=None)
