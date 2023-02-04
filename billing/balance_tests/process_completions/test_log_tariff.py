# -*- coding: utf-8 -*-

import datetime
import decimal
from itertools import groupby
from operator import attrgetter

import pytest
import hamcrest
import mock
import sqlalchemy as sa

from balance import constants as cst
from balance import mapper
from balance import muzzle_util as ut
from balance.actions.consumption import reverse_consume
from balance import exc
from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
    DIRECT_PRODUCT_ID,
    ISO_NUM_CODE_RUB,
    ISO_NUM_CODE_UAH,
    NirvanaProcessingTaskState,
    OrderLogTariffState,
    GenerateAutoOverdraftState,
    TransferMode,
    PersonCategoryCodes,
)
from balance.actions.act_create import ActFactory
from balance.actions.acts.row import ActRow
from balance.actions.process_completions.log_tariff import (
    ProcessLogTariff,
)
from balance.processors.process_completions import find_log_tariff_task_id, process_order

from tests import object_builder as ob

from tests.balance_tests.process_completions.common import (
    LogExceptionInterceptHandler,
    create_order,
    create_orders,
    mk_shipment,
    consume_order,
    migrate_client,
    create_order_task,
    calc_log_tariff_state,
    assert_task_consumes,
    assert_consumes,
    task_assert_from_params,
    NOW,
)
from tests.balance_tests.test_auto_overdraft import (
    get_client,
    get_person,
    get_overdraft_params
)

pytestmark = [
    pytest.mark.log_tariff
]

D = decimal.Decimal

TODAY = ut.trunc_date(NOW)
FUTURE = NOW + datetime.timedelta(1)
PAST = NOW - datetime.timedelta(1)
VERY_PAST = NOW - datetime.timedelta(1)


def create_act_for_consumes(session, cos, backdate=PAST):
    for invoice, invoice_cos in groupby(sorted(cos, key=attrgetter('invoice')), key=attrgetter('invoice')):
        act_rows = list(
            ActRow(co, co.completion_qty, co.completion_sum)
            for co in invoice_cos
        )
        ActFactory.create_from_external(
            invoice,
            act_rows,
            backdate,
            from_log_tariff=True
        )

    session.flush()
    session.expire_all()


class TestCompletions(object):
    @pytest.mark.parametrize(
        'archive_consume',
        [
            pytest.param(False, id='wo_archive'),
            pytest.param(True, id='w_archive'),
        ]
    )
    def test_completions(self, session, archive_consume):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10])
        migrate_client(order.client)
        mk_shipment(order, 33)
        task = create_order_task(order, 21)

        co1, co2, co3 = order.consumes
        if archive_consume:
            create_act_for_consumes(session, [co1])

        ProcessLogTariff(order, task.task_id, obj_lock=True, shared_lock=True).do()

        assert order.completion_qty == 54
        assert order.shipment.money == 54
        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 4, 4),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co2.id, 17, 17, 20, 20),
                (co3.id, 4, 4, 10, 10),
            ]
        )

    @pytest.mark.parametrize(
        'archive_consume',
        [
            pytest.param(False, id='wo_archive'),
            pytest.param(True, id='w_archive'),
        ]
    )
    def test_rollback(self, session, archive_consume):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10])
        migrate_client(order.client)
        mk_shipment(order, 33)
        task = create_order_task(order, -21)

        co1, co2, co3 = order.consumes
        if archive_consume:
            create_act_for_consumes(session, [co1])

        ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == 12
        assert order.shipment.money == 12
        assert_consumes(
            order,
            [
                (30, 30, 12, 12),
                (20, 20, 0, 0),
                (10, 10, 0, 0),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co1.id, -18, -18, 30, 30),
                (co2.id, -3, -3, 20, 20),
            ]
        )

    def test_overcompletion(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10])
        migrate_client(order.client)
        mk_shipment(order, 33)
        task = create_order_task(order, 633)

        co1, co2, co3 = order.consumes

        ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == 666
        assert order.shipment.money == 666
        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co2.id, 17, 17, 20, 20),
                (co3.id, 10, 10, 10, 10),
            ]
        )

    def test_add_overcompletion(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10])
        migrate_client(order.client)
        mk_shipment(order, 666, PAST)
        task = create_order_task(order, 42, tariff_dt=FUTURE)

        ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == 102
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                money=102,
                dt=FUTURE
            )
        )
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=FUTURE,
                res_state=hamcrest.is_not(None)
            )
        )

    def test_overcompletion_consume(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [20])
        migrate_client(order.client)
        mk_shipment(order, 25)
        consume_order(order, [10])
        task = create_order_task(order, 1)

        co1, co2 = order.consumes
        ProcessLogTariff(order, task.task_id, obj_lock=True, shared_lock=True).do()

        assert order.completion_qty == 21
        assert order.shipment.money == 21
        assert_consumes(
            order,
            [
                (20, 20, 20, 20),
                (10, 10, 1, 1),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co2.id, 1, 1, 10, 10),
            ]
        )

    @pytest.mark.parametrize(
        'init_qty, delta_qty, res_qty, res_sum, delta_sum',
        [
            pytest.param(1, 1, 2, D('0.29'), D('0.15'), id='completion'),
            pytest.param(2, -1, 1, D('0.14'), -D('0.15'), id='rollback'),
        ]
    )
    def test_price(self, session, init_qty, delta_qty, res_qty, res_sum, delta_sum):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [7], discount_pct=D('85.71'))
        migrate_client(order.client)
        mk_shipment(order, init_qty)
        task = create_order_task(order, delta_qty)

        consume, = order.consumes

        ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == res_qty
        assert order.shipment.money == res_qty
        assert_consumes(
            order,
            [
                (7, 1, res_qty, res_sum),
            ]
        )
        assert_task_consumes(
            task,
            [
                (consume.id, delta_qty, delta_sum, 7, 1),
            ]
        )

    def test_price_multiple(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [7], discount_pct=D('85.71'))
        migrate_client(order.client)
        mk_shipment(order, 0)

        tasks = []
        for idx in range(7):
            tasks.append(create_order_task(order, 1))
            ProcessLogTariff(order, tasks[-1].task_id).do()

        assert order.completion_qty == 7
        assert order.shipment.money == 7
        assert_consumes(
            order,
            [
                (7, 1, 7, 1),
            ]
        )
        assert [co.qty for t in tasks for co in t.consumes] == [1] * 7

        req_sums = ([D('0.14'), D('0.15')] + [D('0.14')] * 3 + [D('0.15'), D('0.14')])
        assert [co.sum for t in tasks for co in t.consumes] == req_sums

    def test_zero_consume(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [1, 1, 1])
        migrate_client(order.client)
        task = create_order_task(order, 2)

        co1, co2, co3 = order.consumes
        reverse_consume(co2, None, D('0.9999'))
        co2.consume_sum = 0
        co2.consume_qty = co2.current_qty
        session.flush()

        ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == 2
        assert order.shipment.money == 2
        assert_consumes(
            order,
            [
                (1, 1, 1, 1),
                (D('0.0001'), 0, D('0.0001'), 0),
                (1, 1, D('0.9999'), 1),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co1.id, 1, 1, 1, 1),
                (co2.id, D('0.0001'), 0, D('0.0001'), 0),
                (co3.id, D('0.9999'), 1, 1, 1),
            ]
        )

    def test_new_order(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [42])
        migrate_client(order.client)
        task = create_order_task(order, 2, NOW)

        ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == 2
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                bucks=None,
                money=2,
                dt=NOW
            )
        )

    @pytest.mark.parametrize(
        'shipment_dt, tariff_dt, res_tariff_dt',
        [
            pytest.param(TODAY, NOW, NOW, id='present'),
            pytest.param(TODAY, FUTURE, FUTURE, id='future'),
            pytest.param(NOW, PAST, NOW, id='past'),
        ]
    )
    def test_tariff_dt(self, session, shipment_dt, tariff_dt, res_tariff_dt):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [666])
        migrate_client(order.client)
        mk_shipment(order, 30, shipment_dt)
        task = create_order_task(order, 666, tariff_dt)

        start_proc_dt = datetime.datetime.now().replace(microsecond=0)
        ProcessLogTariff(order, task.task_id).do()
        end_proc_dt = datetime.datetime.now()

        assert task.res_tariff_dt == res_tariff_dt
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                dt=res_tariff_dt,
                update_dt=hamcrest.all_of(
                    hamcrest.greater_than_or_equal_to(start_proc_dt),
                    hamcrest.less_than_or_equal_to(end_proc_dt),
                )
            )
        )
        assert order.shipment.dt == res_tariff_dt
        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                shipment_dt=res_tariff_dt,
                shipment_update_dt=order.shipment.update_dt
            )
        )
        hamcrest.assert_that(
            order.shipment_accepted,
            hamcrest.has_properties(
                dt=res_tariff_dt,
                update_dt=order.shipment.update_dt
            )
        )

    @pytest.mark.dont_mock_mnclose
    @pytest.mark.parametrize('is_month_closed', [False, True])
    def test_closed_month_tariff_dt(self, session, is_month_closed):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [666])
        migrate_client(order.client)
        mk_shipment(order, 30, VERY_PAST)
        task = create_order_task(order, 666, PAST)

        cur_dt = datetime.datetime.now().replace(microsecond=0)
        with mock.patch('balance.mncloselib.is_month_closed', return_value=is_month_closed):
            ProcessLogTariff(order, task.task_id).do()

        if is_month_closed:
            assert task.res_tariff_dt >= cur_dt > PAST
            assert task.res_tariff_dt == order.shipment.dt
        else:
            assert task.res_tariff_dt == PAST
            assert task.res_tariff_dt == PAST

    def test_group_dt(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10])
        migrate_client(order.client)
        task1 = create_order_task(order, 33, tariff_dt=PAST, currency_id=ISO_NUM_CODE_RUB, group_dt=PAST)
        task2 = create_order_task(order, 21, tariff_dt=TODAY, currency_id=ISO_NUM_CODE_RUB, group_dt=TODAY)
        task2.task_id = task1.task_id
        session.flush()

        co1, co2, co3 = order.consumes

        ProcessLogTariff(order, task1.task_id).do()

        assert order.completion_qty == 54
        assert order.shipment.money == 54
        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 4, 4),
            ]
        )

        assert_task_consumes(
            task1,
            [
                (co1.id, 30, 30, 30, 30),
                (co2.id, 3, 3, 20, 20),
            ]
        )
        assert_task_consumes(
            task2,
            [
                (co2.id, 17, 17, 20, 20),
                (co3.id, 4, 4, 10, 10),
            ]
        )

        expected_res_state = calc_log_tariff_state(co1.id, [(co1, 30), (co2, 20), (co3, 4)])
        hamcrest.assert_that(
            task1,
            hamcrest.has_properties(
                res_tariff_dt=PAST,
                res_state=expected_res_state
            )
        )
        hamcrest.assert_that(
            task2,
            hamcrest.has_properties(
                res_tariff_dt=TODAY,
                res_state=expected_res_state
            )
        )

    def test_state_completion(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [40, 30, 20, 10])
        migrate_client(order.client)
        mk_shipment(order, 74)
        create_act_for_consumes(session, [order.consumes[0]])
        task = create_order_task(order, D('5.3'))

        ProcessLogTariff(order, task.task_id).do()

        state = calc_log_tariff_state(order.consumes[1].id, zip(order.consumes[1:-1], [30, D('9.3')]))
        assert task.res_state == state

    def test_state_rollback(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [40, 30, 20, 10])
        migrate_client(order.client)
        mk_shipment(order, 94)
        create_act_for_consumes(session, list(order.consumes)[:3])
        task = create_order_task(order, -D('5.5'))

        ProcessLogTariff(order, task.task_id).do()

        state = calc_log_tariff_state(order.consumes[1].id, zip(order.consumes[1:3], [D('30'), D('18.5')]))
        assert task.res_state == state

    def test_state_rollback_to_zero(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [40, 30, 20])
        migrate_client(order.client)
        mk_shipment(order, 74)
        create_act_for_consumes(session, list(order.consumes)[:2])
        task = create_order_task(order, -74)

        ProcessLogTariff(order, task.task_id).do()

        state = calc_log_tariff_state(order.consumes[0].id, [])
        assert task.res_state == state

    def test_state_no_consumes(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [])
        migrate_client(order.client)
        task = create_order_task(order, 7, tariff_dt=FUTURE)

        ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == 7
        assert order.shipment.money == 7
        assert order.consumes == []

        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=FUTURE,
                res_state=calc_log_tariff_state(0, [])
            )
        )

    def test_state_no_completions(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [321, 345])
        migrate_client(order.client)
        mk_shipment(order, 400)

        c1, c2 = order.consumes
        create_act_for_consumes(session, [c1])
        assert c1.archive

        task = create_order_task(
            order, 0,
            state=calc_log_tariff_state(c1.id, [(c1, 321), (c2, 79)]),
            tariff_dt=FUTURE
        )

        ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == 400
        assert order.shipment.money == 400
        assert_consumes(
            order,
            [
                (321, 321, 321, 321),
                (345, 345, 79, 79),
            ]
        )

        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=FUTURE,
                res_state=calc_log_tariff_state(c2.id, [(c2, 79)])
            )
        )

    def test_state_archive_consumes(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [10, 20])
        migrate_client(order.client)
        mk_shipment(order, 30)

        c1, c2 = order.consumes
        create_act_for_consumes(session, [c1, c2])

        task = create_order_task(
            order, 12,
            state=calc_log_tariff_state(c1.id, [(c1, 10), (c2, 20)]),
        )

        ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == 42
        assert order.shipment.money == 42
        assert_consumes(
            order,
            [
                (10, 10, 10, 10),
                (20, 20, 20, 20),
            ]
        )

        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=hamcrest.is_not(None),
                res_state=calc_log_tariff_state(0, [(c1, 10), (c2, 20)])
            )
        )

    def test_state_check_ok(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [100, 90, 80])
        migrate_client(order.client)
        mk_shipment(order, 180)
        c1, c2, c3 = order.consumes

        task = create_order_task(order, 40, state=calc_log_tariff_state(c2.id, [(c2, 80)]))

        ProcessLogTariff(order, task.task_id).do()

    def test_state_check_fail(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [10, 20, 30])
        migrate_client(order.client)
        mk_shipment(order, 15)
        c1, c2, c3 = order.consumes

        base_state = calc_log_tariff_state(c1.id, [(c1, 10), (c2, 4)])
        task = create_order_task(order, 40, state=base_state)

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, 'state mismatch')
        assert 'state mismatch' in task.error

        assert order.completion_qty == 15
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(money=15)
        )
        assert_consumes(
            order,
            [
                (10, 10, 10, 10),
                (20, 20, 5, 5),
                (30, 30, 0, 0),
            ]
        )
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=task.tariff_dt,
                res_state=base_state
            )
        )

    @pytest.mark.parametrize(
        'is_log_tariff, has_state, msg',
        [
            (OrderLogTariffState.OFF, True, 'log tariff is not enable'),  # and child_ua_type != 3
            (OrderLogTariffState.INIT, True, 'order is not migrated'),
            (OrderLogTariffState.INIT, False, 'order is not migrated'),
            (OrderLogTariffState.MIGRATED, False, 'task without state for order with completions'),
        ]
    )
    def test_state_check_init(self, session, is_log_tariff, has_state, msg):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [11])
        order._is_log_tariff = is_log_tariff
        session.flush()

        co, = order.consumes
        migrate_client(order.client)
        mk_shipment(order, 1)

        if has_state:
            base_state = calc_log_tariff_state(co.id, [(co, 1)])
        else:
            base_state = None
        task = create_order_task(order, 40, state=base_state, skip_update_is_log_tariff=True)

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, msg)
        assert msg in task.error

        assert order.completion_qty == 1
        assert_consumes(order, [(11, 11, 1, 1)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=task.tariff_dt,
                res_state=base_state
            )
        )

    def test_state_check_completions_for_child(self, session, client):
        migrate_client(client)
        main_order, (child_order,) = create_orders(client, DIRECT_PRODUCT_RUB_ID, [DIRECT_PRODUCT_RUB_ID])
        consume_order(child_order, [666])
        mk_shipment(child_order, 1)
        co, = child_order.consumes

        task = create_order_task(child_order, 665, state=None, skip_update_is_log_tariff=True)

        ProcessLogTariff(child_order, task.task_id).do()

        assert child_order.completion_qty == 666
        assert_consumes(child_order, [(666, 666, 666, 666)])
        assert_task_consumes(task, [(co.id, 665, 665, 666, 666)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                res_tariff_dt=task.tariff_dt,
                res_state=calc_log_tariff_state(co.id, [(co, 666)])
            )
        )

    def test_no_state(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [11])
        order._is_log_tariff = OrderLogTariffState.MIGRATED
        co, = order.consumes
        migrate_client(order.client)

        task = create_order_task(order, 7, state=None)
        ProcessLogTariff(order, task.task_id).do()

        assert order.completion_qty == 7
        assert_consumes(order, [(11, 11, 7, 7)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                res_tariff_dt=task.tariff_dt,
                res_state=calc_log_tariff_state(co.id, [(co, 7)])
            )
        )

    def test_rollback_not_completed(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [666])
        migrate_client(order.client)
        mk_shipment(order, 42, VERY_PAST)
        consume, = order.consumes
        task = create_order_task(order, -43, PAST, calc_log_tariff_state(consume.id, [(consume, 42)]))

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        msg = 'not enough completions for rollback'
        log_intercept.assert_exc(exc.LOG_TARIFF_CANT_PROCESS, msg)
        assert msg in task.error

        assert order.completion_qty == 42
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                money=42,
                dt=VERY_PAST
            )
        )
        assert_consumes(order, [(666, 666, 42, 42)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=PAST,
                res_state=calc_log_tariff_state(consume.id, [(consume, 42)])
            )
        )

    def test_rollback_no_consumes(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [])
        migrate_client(order.client)
        task = create_order_task(order, -1, PAST, calc_log_tariff_state(666, []))

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        msg = 'not enough completions for rollback'
        log_intercept.assert_exc(exc.LOG_TARIFF_CANT_PROCESS, msg)
        assert msg in task.error

        assert order.completion_qty == 0
        assert order.shipment.money is None
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=PAST,
                res_state=calc_log_tariff_state(666, [])
            )
        )

    def test_check_multiple_rows(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [])
        migrate_client(order.client)
        state = calc_log_tariff_state(666, [])
        task1 = create_order_task(order, 1, tariff_dt=PAST, state=state, currency_id=ISO_NUM_CODE_RUB)
        task2 = create_order_task(order, 1, tariff_dt=PAST, state=state, currency_id=0)
        task2.task_id = task1.task_id
        session.flush()

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task1.task_id).do()

        msg = 'invalid number of order tasks for processor'
        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, msg)
        assert msg in task1.error
        assert msg in task2.error

        assert order.completion_qty == 0
        assert order.shipment.money is None
        hamcrest.assert_that(
            [task1, task2],
            hamcrest.only_contains(
                hamcrest.has_properties(
                    consumes=[],
                    res_tariff_dt=PAST,
                    res_state=state
                )
            )
        )

    @pytest.mark.parametrize(
        'task_currencies',
        [
            pytest.param([0], id='no currencies'),
            pytest.param([ISO_NUM_CODE_UAH], id='wrong currency'),
            pytest.param([ISO_NUM_CODE_RUB, ISO_NUM_CODE_UAH], id='multiple currencies'),
        ]
    )
    def test_check_wrong_currency(self, session, task_currencies):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [11])
        migrate_client(order.client)
        mk_shipment(order, 1)

        task_id = None
        for currency in task_currencies:
            task = create_order_task(order, 40, currency_id=currency)
            if task_id is None:
                task_id = task.task_id
            else:
                task.task_id = task_id
        session.flush()

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        msg = "task currencies doesn't match client's"
        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, msg)
        assert msg in task.error

        assert order.completion_qty == 1
        assert_consumes(order, [(11, 11, 1, 1)])

    def test_check_is_log_tariff(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [11])
        migrate_client(order.client)
        mk_shipment(order, 1)

        task = create_order_task(order, 40)
        order._is_log_tariff = None
        session.flush()

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        msg = 'log tariff is not enabled'
        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, msg)
        assert msg in task.error

        assert order.completion_qty == 1
        assert_consumes(order, [(11, 11, 1, 1)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=task.tariff_dt,
                res_state=task.state
            )
        )

    def test_check_no_tasks(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [11])
        task = ob.LogTariffTaskBuilder.construct(session)

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.id).do()

        assert log_intercept.exceptions == []

    @pytest.mark.parametrize('currency_id', [0, ISO_NUM_CODE_RUB])
    def test_multiple_tasks(self, session, currency_id):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID)
        task = ob.LogTariffTaskBuilder.construct(session)
        ob.LogTariffOrderBuilder.construct(
            session,
            task=task,
            order=order,
            currency_id=currency_id,
            completion_qty_delta=666,
            tariff_dt=datetime.datetime.now()
        )
        with pytest.raises(sa.exc.DatabaseError) as exc_info:
            ob.LogTariffOrderBuilder.construct(
                session,
                task=task,
                order=order,
                currency_id=currency_id,
                completion_qty_delta=667,
                tariff_dt=datetime.datetime.now()
            )
        assert 'ORA-00001: unique constraint (BO.U_LOG_TARIFF_ORDER) violated' in exc_info.value.message

    def test_already_processed(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [666])
        migrate_client(order.client)
        task = create_order_task(
            order, 1,
            tariff_dt=VERY_PAST,
            state=calc_log_tariff_state(666, [])
        )
        task.res_tariff_dt = FUTURE
        session.flush()

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        assert log_intercept.exceptions == []
        assert order.completion_qty == 0
        assert order.shipment.money is None
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=FUTURE,
                res_state=None
            )
        )

    def test_already_processed_multiple(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [14])
        migrate_client(order.client)
        task1 = create_order_task(order, 1, tariff_dt=PAST, state=None, currency_id=ISO_NUM_CODE_RUB)
        task2 = create_order_task(order, 1, tariff_dt=PAST, currency_id=0)
        task2.task_id = task1.task_id
        task1.res_tariff_dt = FUTURE
        session.flush()

        with pytest.raises(exc.LOG_TARIFF_CANT_PROCESS) as exc_info:
            ProcessLogTariff(order, task1.task_id).do()

        assert 'Partially completed tasks!' in exc_info.value.msg

    @pytest.mark.parametrize(
        'case',
        [
            pytest.param(
                dict(
                    consume_qtys=[10, 20, 30],
                    shipment_qty=16,
                    delta_idx=1,
                    delta_qty=20,
                    task_qty=17,
                    res_consumes=[
                        (10, 10, 10, 10),
                        (0, 0, 0, 0),
                        (6, 6, 6, 6),
                        (20, 20, 17, 17),
                        (24, 24, 0, 0),
                    ],
                    res_task=[(3, 17, 17)]
                ),
                id='completions_full'
            ),
            pytest.param(
                dict(
                    consume_qtys=[10, 20, 30],
                    shipment_qty=16,
                    delta_idx=1,
                    delta_qty=19,
                    task_qty=17,
                    res_consumes=[
                        (10, 10, 10, 10),
                        (1, 1, 1, 1),
                        (5, 5, 5, 5),
                        (19, 19, 17, 17),
                        (25, 25, 0, 0),
                    ],
                    res_task=[(3, 17, 17)]
                ),
                id='completions_part'
            ),
            pytest.param(
                dict(
                    consume_qtys=[10, 20, 30],
                    shipment_qty=11,
                    delta_idx=1,
                    delta_qty=20,
                    task_qty=1,
                    res_consumes=[
                        (10, 10, 10, 10),
                        (0, 0, 0, 0),
                        (1, 1, 1, 1),
                        (20, 20, 1, 1),
                        (29, 29, 0, 0),
                    ],
                    res_task=[(3, 1, 1)]
                ),
                id='completions_small'
            ),
            pytest.param(
                dict(
                    consume_qtys=[10, 20, 30],
                    shipment_qty=35,
                    delta_idx=1,
                    delta_qty=20,
                    task_qty=2,
                    res_consumes=[
                        (10, 10, 10, 10),
                        (0, 0, 0, 0),
                        (25, 25, 25, 25),
                        (20, 20, 2, 2),
                        (5, 5, 0, 0),
                    ],
                    res_task=[(3, 2, 2)]
                ),
                id='completions_big'
            ),
            pytest.param(
                dict(
                    consume_qtys=[10, 20, 30],
                    shipment_qty=31,
                    delta_idx=1,
                    delta_qty=20,
                    task_qty=-2,
                    res_consumes=[
                        (10, 10, 10, 10),
                        (0, 0, 0, 0),
                        (21, 21, 19, 19),
                        (20, 20, 0, 0),
                        (9, 9, 0, 0),
                    ],
                    res_task=[(2, -2, -2)]
                ),
                id='rollback_full'
            ),
            pytest.param(
                dict(
                    consume_qtys=[10, 20, 30],
                    shipment_qty=31,
                    delta_idx=1,
                    delta_qty=19,
                    task_qty=-2,
                    res_consumes=[
                        (10, 10, 10, 10),
                        (1, 1, 1, 1),
                        (20, 20, 18, 18),
                        (19, 19, 0, 0),
                        (10, 10, 0, 0),
                    ],
                    res_task=[(2, -2, -2)]
                ),
                id='rollback_part'
            ),
            pytest.param(
                dict(
                    consume_qtys=[10, 20, 30],
                    shipment_qty=13,
                    delta_idx=1,
                    delta_qty=20,
                    task_qty=-2,
                    res_consumes=[
                        (10, 10, 10, 10),
                        (0, 0, 0, 0),
                        (3, 3, 1, 1),
                        (20, 20, 0, 0),
                        (27, 27, 0, 0),
                    ],
                    res_task=[(2, -2, -2)]
                ),
                id='rollback_single_consume'
            ),
            pytest.param(
                dict(
                    consume_qtys=[10, 20, 30],
                    shipment_qty=40,
                    delta_idx=1,
                    delta_qty=20,
                    task_qty=-2,
                    res_consumes=[
                        (10, 10, 10, 10),
                        (0, 0, 0, 0),
                        (30, 30, 28, 28),
                        (20, 20, 0, 0),
                    ],
                    res_task=[(2, -2, -2)]
                ),
                id='rollback_big'
            ),
        ]
    )
    def test_consume_sequence_break(self, session, case):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, case['consume_qtys'])

        reverse_consume(order.consumes[case['delta_idx']], None, case['delta_qty'])
        migrate_client(order.client)
        mk_shipment(order, case['shipment_qty'])
        reverse_consume(order.consumes[case['delta_idx']], None, -case['delta_qty'])

        task = create_order_task(order, case['task_qty'])
        ProcessLogTariff(order, task.task_id).do()

        assert_consumes(order, case['res_consumes'])
        assert_task_consumes(task, task_assert_from_params(order.consumes, case['res_task']))

    def test_group_dt_rollback_completions(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10])
        migrate_client(order.client)
        mk_shipment(order, 50, VERY_PAST)
        co1, co2, co3 = order.consumes

        create_act_for_consumes(session, [co2], backdate=FUTURE)

        task1 = create_order_task(order, -8, tariff_dt=PAST, currency_id=ISO_NUM_CODE_RUB, group_dt=PAST)
        task2 = create_order_task(order, 2, tariff_dt=TODAY, currency_id=ISO_NUM_CODE_RUB, group_dt=TODAY)
        task2.task_id = task1.task_id
        session.flush()
        session.expire_all()

        ProcessLogTariff(order, task1.task_id).do()
        session.flush()
        session.expire_all()

        assert order.completion_qty == 44
        assert order.shipment.money == 44
        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 14, 14),
                (10, 10, 0, 0),
            ]
        )

        assert_task_consumes(
            task1,
            [
                (co2.id, -8, -8, 20, 20),
            ]
        )
        assert_task_consumes(
            task2,
            [
                (co2.id, 2, 2, 20, 20),
            ]
        )


class TestMulticurrencyMigratedOrders(object):
    @pytest.mark.parametrize(
        'archive_consume, init_consume_qty, bucks, money, task_qty, res_qty, res_money, res_consumes, res_task, res_shipment_money',
        [
            pytest.param(
                True,
                [30, 20, 10],
                30, 432, 250,
                D('52.733333'), 682,
                [
                    (30, 900, 30, 900),
                    (20, 600, 20, 600),
                    (10, 300, D('2.733333'), 82),
                ],
                [
                    (1, 168, 168),
                    (2, 82, 82),
                ],
                682,
                id='base'
            ),
            pytest.param(
                False,
                [50],
                None, 0, 11,
                D('0.366667'), 11,
                [(50, 1500, 0, 0)],
                [(0, 11, 0)],
                11,
                id='null shipment bucks'
            ),
            pytest.param(
                False,
                [50],
                D('42.424242'), 0, 11,
                D('42.790909'), 11,
                [(50, 1500, D('42.790909'), D('1283.73'))],
                [(0, 11, 11)],
                11,
                id='precision_completion'
            ),
            pytest.param(
                False,
                [50],
                D('49.633333'), 0, 11,
                50, 11,
                [(50, 1500, 50, 1500)],
                [(0, 11, 11)],
                11,
                id='precision_equal_bucks'
            ),
            pytest.param(
                False,
                [50],
                0, 1489, 11,
                50, 1500,
                [(50, 1500, 50, 1500)],
                [(0, 11, 11)],
                1500,
                id='precision_equal_money'
            ),
            pytest.param(
                False,
                [D('1.724671'), D('0.60066')],
                0, D('29.123456'), D('35.987654'),
                D('2.170370'), D('65.11111'),
                [
                    (D('1.724671'), D('51.74'), D('1.724671'), D('51.74')),
                    (D('0.60066'), D('18.02'), D('0.445699'), D('13.37')),
                ],
                [
                    (0, D('22.616674'), D('22.62')),
                    (1, D('13.37098'), D('13.37')),
                ],
                D('65.11111'),
                id='precision_task'
            ),
            pytest.param(
                False,
                [D('1.000006')],
                0, 30, D('0.0002'),
                D('1.000007'), D('30.00018'),
                [(D('1.000006'), 30, D('1.000006'), 30)],
                [(0, D('0.00018'), 0)],
                D('30.0002'),
                id='overcompletion_2step'
            ),
            pytest.param(
                False,
                [D('1.000006')],
                0, D('30.0001'), D('0.0001'),
                D('1.000007'), D('30.00018'),
                [(D('1.000006'), 30, D('1.000006'), 30)],
                [(0, D('0.00008'), 0)],
                D('30.0002'),
                id='overcompletion_1step'
            ),
            pytest.param(
                False,
                [D('1.000006'), D('0.001003')],
                0, D('29.999'), D('0.0354'),
                D('1.001147'), D('30.03027'),
                [
                    (D('1.000006'), 30, D('1.000006'), 30),
                    (D('0.001003'), D('0.03'), D('0.001003'), D('0.03')),
                ],
                [
                    (0, D('0.001180'), 0),
                    (1, D('0.030090'), D('0.03')),
                ],
                D('30.0344'),
                id='overcompletion_multiple_consumes'
            ),
            pytest.param(
                False,
                [D('6.060606'), D('0.404040'), D('0.202020')],
                0, D('29.999999'), D('666.666666'),
                D('23.222222'), D('199.99998'),
                [
                    (D('6.060606'), D('181.82'), D('6.060606'), D('181.82')),
                    (D('0.404040'), D('12.12'), D('0.404040'), D('12.12')),
                    (D('0.202020'), D('6.06'), D('0.202020'), D('6.06')),
                ],
                [
                    (0, D('151.818181'), D('151.82')),
                    (1, D('12.1212'), D('12.12')),
                    (2, D('6.0606'), D('6.06')),
                ],
                D('696.666665'),
                id='overcompletion_precision'
            ),
            pytest.param(
                False,
                [42],
                10, 600, 666,
                D('52.2'), 960,
                [(42, 42 * 30, 42, 42 * 30)],
                [(0, 360, 360)],
                1266,
                id='overcompletion'
            ),
            pytest.param(
                False,
                [10, 20, 30],
                10, 660, -250,
                D('23.666667'), 410,
                [
                    (10, 300, 10, 300),
                    (20, 600, D('13.666667'), 410),
                    (30, 900, 0, 0),
                ],
                [
                    (1, -190, -190),
                    (2, -60, -60),
                ],
                410,
                id='rollback'
            ),
        ]
    )
    def test_common(self, session, archive_consume, init_consume_qty, bucks, money, task_qty,
                         res_qty, res_money, res_consumes, res_task, res_shipment_money):
        order = create_order(session, DIRECT_PRODUCT_ID, init_consume_qty)
        migrate_client(order.client)
        if bucks is not None:
            mk_shipment(order, bucks, money=money)
        order.completion_consumed_money = money
        task = create_order_task(order, task_qty)

        if archive_consume:
            create_act_for_consumes(session, [order.consumes[0]])

        ProcessLogTariff(order, task.task_id).do()
        assert task.error is None

        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                completion_qty=res_qty,
                completion_consumed_money=res_money
            )
        )
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                bucks=bucks or 0,
                money=res_shipment_money
            )
        )
        assert_consumes(order, res_consumes)
        assert_task_consumes(task, task_assert_from_params(order.consumes, res_task, qty_coeff=30))

    def test_rounding_multiple_tasks(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [50])
        migrate_client(order.client)
        mk_shipment(order, 0, money=0)
        order.completion_consumed_money = 0

        tasks = []
        for idx in range(10):
            tasks.append(create_order_task(order, D('0.001')))
            ProcessLogTariff(order, tasks[-1].task_id).do()

        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                completion_qty=D('0.000333'),
                completion_consumed_money=D('0.01')
            )
        )
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                bucks=0,
                money=D('0.01')
            )
        )
        assert_consumes(
            order,
            [
                (50, 1500, D('0.000333'), D('0.01')),
            ]
        )

        assert [co.qty for t in tasks for co in t.consumes] == [D('0.001')] * 10
        assert [co.sum for t in tasks for co in t.consumes] == [0] * 4 + [D('0.01')] + [0] * 5

    def test_completions_discount(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [42], discount_pct=D('66.6'))
        migrate_client(order.client)
        mk_shipment(order, 12, money=0)
        order.completion_consumed_money = 0
        task = create_order_task(order, D('66.654321'))

        ProcessLogTariff(order, task.task_id).do()

        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                completion_qty=D('14.221811'),
                completion_consumed_money=D('66.654321')
            )
        )
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                bucks=12,
                money=D('66.654321')
            )
        )
        assert_consumes(
            order,
            [
                (42, D('420.84'), D('14.221811'), D('142.50')),
            ]
        )
        assert_task_consumes(
            task,
            [
                (order.consumes[0].id, D('66.654321'), D('22.26'), 1260, D('420.84')),
            ]
        )

    def test_add_overcompletion(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [10])
        migrate_client(order.client)
        mk_shipment(order, 0, money=0)
        order.completion_consumed_money = 0

        co, = order.consumes

        prev_task = create_order_task(order, 330)
        ProcessLogTariff(order, prev_task.task_id).do()

        task = create_order_task(order, 66)
        ProcessLogTariff(order, task.task_id).do()

        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                completion_qty=D('12.2'),
                completion_consumed_money=300
            )
        )
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                bucks=0,
                money=366
            )
        )
        assert_consumes(order, [(10, 300, 10, 300)])
        assert_task_consumes(prev_task, [(co.id, 300, 300, 300, 300)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=hamcrest.is_not(None),
                res_state=hamcrest.is_not(None)
            )
        )

    def test_overcompletion_consume(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [20])
        migrate_client(order.client)
        mk_shipment(order, 0, money=0)
        order.completion_consumed_money = 0
        session.flush()

        prev_task = create_order_task(order, 750)
        ProcessLogTariff(order, prev_task.task_id).do()

        consume_order(order, [10])
        task = create_order_task(order, 35)

        co1, co2 = order.consumes
        ProcessLogTariff(order, task.task_id, obj_lock=True, shared_lock=True).do()

        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                completion_qty=D('21.166667'),
                completion_consumed_money=635
            )
        )
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                bucks=0,
                money=635
            )
        )
        assert_consumes(
            order,
            [
                (20, 600, 20, 600),
                (10, 300, D('1.166667'), 35),
            ]
        )
        assert_task_consumes(prev_task, [(co1.id, 600, 600, 600, 600)])
        assert_task_consumes(task, [(co2.id, 35, 35, 300, 300)])

    def test_rollback_bucks(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [666])
        migrate_client(order.client)
        mk_shipment(order, 10, money=660)
        order.completion_consumed_money = 660
        task = create_order_task(order, -700)

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, 'rollback bucks completion')

        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                completion_qty=32,
                completion_consumed_money=660
            )
        )
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                bucks=10,
                money=660
            )
        )
        assert_consumes(order, [(666, 666 * 30, 32, 32 * 30)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=task.tariff_dt,
                res_state=task.state
            )
        )

    def test_rollback_bucks_several_tasks(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [666])
        migrate_client(order.client)
        mk_shipment(order, 10, money=660)
        order.completion_consumed_money = 660
        task_1 = create_order_task(order, 10, group_dt=PAST)
        task_2 = create_order_task(order, -700, group_dt=TODAY)
        task_2.task_id = task_1.task_id
        session.flush()

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task_1.task_id).do()

        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, 'rollback bucks completion')

        hamcrest.assert_that(
            order,
            hamcrest.has_properties(
                completion_qty=32,
                completion_consumed_money=660
            )
        )
        hamcrest.assert_that(
            order.shipment,
            hamcrest.has_properties(
                bucks=10,
                money=660
            )
        )
        assert_consumes(order, [(666, 666 * 30, 32, 32 * 30)])
        hamcrest.assert_that(
            task_1,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=task_1.tariff_dt,
                res_state=task_1.state
            )
        )
        hamcrest.assert_that(
            task_2,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=task_2.tariff_dt,
                res_state=task_2.state
            )
        )

    def test_check_not_migrated(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [666])
        task = create_order_task(order, 666, tariff_dt=PAST)

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, 'client must be migrated to currency')

        assert order.completion_qty == 0
        assert_consumes(order, [(666, 666 * 30, 0, 0)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=PAST,
                res_state=task.state
            )
        )

    @pytest.mark.parametrize(
        'product_id, price_coeff',
        [
            (DIRECT_PRODUCT_ID, 30),
            (DIRECT_PRODUCT_RUB_ID, 1),
        ])
    def test_check_wrong_product(self, session, product_id, price_coeff):
        order = create_order(session, product_id, [666])
        migrate_client(order.client, 'USD', None)
        mk_shipment(order, 0, money=0)
        order.completion_consumed_money = 0
        task = create_order_task(order, 666)

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, "client currency doesn't match order")

        assert order.completion_qty == 0
        assert_consumes(order, [(666, 666 * price_coeff, 0, 0)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=task.tariff_dt,
                res_state=task.state
            )
        )

    @pytest.mark.parametrize(
        'product_id, currency_id, price_coeff',
        [
            (DIRECT_PRODUCT_ID, 0, 30),
            (DIRECT_PRODUCT_RUB_ID, ISO_NUM_CODE_UAH, 1)
        ]
    )
    def test_check_wrong_currency(self, session, product_id, currency_id, price_coeff):
        order = create_order(session, product_id, [666])
        migrate_client(order.client)
        mk_shipment(order, 0, money=0)
        order.completion_consumed_money = 0
        task = create_order_task(order, 666, currency_id=currency_id)

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, "task currencies doesn't match client's")

        assert order.completion_qty == 0
        assert_consumes(order, [(666, 666 * price_coeff, 0, 0)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=task.tariff_dt,
                res_state=task.state
            )
        )

    def test_check_migration_not_finished(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [666])
        migrate_client(order.client)
        task = create_order_task(order, 666)

        # ломаем фишки
        order.completion_fixed_qty = 1
        session.flush()

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        msg = 'completion_fixed_qty != t_shipment.bucks'
        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, msg)
        assert msg in task.error

        assert order.completion_qty == 0
        assert_consumes(order, [(666, 666 * 30, 0, 0)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=task.tariff_dt,
                res_state=task.state
            )
        )

    def test_check_completion_money(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [666])
        migrate_client(order.client)
        mk_shipment(order, 0, money=30)
        order.completion_consumed_money = None
        task = create_order_task(order, 666)

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task.task_id).do()

        msg = 'invalid completion money for migrated order'
        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, msg)
        assert msg in task.error

        assert order.completion_qty == 1
        assert_consumes(order, [(666, 666 * 30, 1, 30)])
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=task.tariff_dt,
                res_state=task.state
            )
        )

    @pytest.mark.parametrize(
        'case',
        [
            pytest.param(
                dict(
                    consume_qtys=[10, 20, 30],
                    bucks_qty=11,
                    money_qty=150,
                    delta_idx=1,
                    delta_qty=19,
                    task_qty=350,
                    res_consumes=[
                        (10, 10 * 30, 10, 10 * 30),
                        (1, 1 * 30, 1, 1 * 30),
                        (5, 5 * 30, 5, 5 * 30),
                        (19, 19 * 30, D('11.666667'), 350),
                        (25, 25 * 30, 0, 0),
                    ],
                    res_task=[(3, 350, 350)]
                ),
                id='completions'
            ),
            pytest.param(
                dict(
                    consume_qtys=[10, 20, 30],
                    bucks_qty=11,
                    money_qty=350,
                    delta_idx=1,
                    delta_qty=20,
                    task_qty=-77,
                    res_consumes=[
                        (10, 10 * 30, 10, 10 * 30),
                        (0, 0, 0, 0),
                        (D('12.666667'), 380, D('10.1'), 303),
                        (20, 20 * 30, 0, 0),
                        (D('17.333333'), 520, 0, 0),
                    ],
                    res_task=[(2, -77, -77)]
                ),
                id='rollback'
            ),
        ]
    )
    def test_consume_sequence_break(self, session, case):
        order = create_order(session, DIRECT_PRODUCT_ID, case['consume_qtys'])

        reverse_consume(order.consumes[case['delta_idx']], None, case['delta_qty'])
        migrate_client(order.client)
        mk_shipment(order, case['bucks_qty'], money=case['money_qty'])
        order.completion_consumed_money = case['money_qty']
        reverse_consume(order.consumes[case['delta_idx']], None, -case['delta_qty'])

        task = create_order_task(order, case['task_qty'])
        ProcessLogTariff(order, task.task_id).do()

        assert_consumes(order, case['res_consumes'])
        assert_task_consumes(task, task_assert_from_params(order.consumes, case['res_task'], qty_coeff=30))


class TestReconsume(object):
    def test_manager(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [50, 40, 30])
        migrate_client(order.client)
        mk_shipment(order, 42, VERY_PAST)
        order.manager = ob.SingleManagerBuilder.construct(session)

        task = create_order_task(order, 11)
        ProcessLogTariff(order, task.task_id).do()

        assert_consumes(
            order,
            [
                (42, 42, 42, 42),
                (0, 0, 0, 0),
                (0, 0, 0, 0),
                (8, 8, 8, 8),
                (40, 40, 3, 3),
                (30, 30, 0, 0),
            ]
        )
        assert_task_consumes(
            task,
            [
                (order.consumes[3].id, 8, 8, 8, 8),
                (order.consumes[4].id, 3, 3, 40, 40),
            ]
        )

    def test_rollback(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [321, 345])
        migrate_client(order.client)
        mk_shipment(order, 42, VERY_PAST)
        order.manager = ob.SingleManagerBuilder.construct(session)

        task = create_order_task(order, -33)
        ProcessLogTariff(order, task.task_id).do()

        assert_consumes(
            order,
            [
                (321, 321, 9, 9),
                (345, 345, 0, 0),
            ]
        )
        assert_task_consumes(
            task,
            [
                (order.consumes[0].id, -33, -33, 321, 321),
            ]
        )

    def test_reconsume_failure(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [50, 40, 30])
        migrate_client(order.client)
        mk_shipment(order, 42, FUTURE)
        task = create_order_task(order, 11, tariff_dt=PAST)

        patch_path = 'balance.actions.process_completions.log_tariff._IncrementalProcessor'
        exception = exc.LOG_TARIFF_MUST_RECONSUME(task, order.consumes[0].id, 'because i can')
        with mock.patch(patch_path, side_effect=exception):
            with LogExceptionInterceptHandler() as log_intercept:
                ProcessLogTariff(order, task.task_id).do()

        log_intercept.assert_exc(exc.LOG_TARIFF_MUST_RECONSUME)

        assert_consumes(
            order,
            [
                (50, 50, 42, 42),
                (40, 40, 0, 0),
                (30, 30, 0, 0),
            ]
        )

        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=PAST,
                res_state=task.state
            )
        )

    def test_overacted(self, session):
        client = ob.ClientBuilder.construct(session)
        order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)
        person = ob.PersonBuilder.construct(session, client=client, type='ur')
        paysys = ob.Getter(mapper.Paysys, 1003).build(session).obj

        invoice = ob.InvoiceBuilder(
            person=person,
            paysys=paysys,
            request=ob.RequestBuilder(
                basket=ob.BasketBuilder(
                    client=client,
                    rows=[ob.BasketItemBuilder(order=order, quantity=666)]
                )
            )
        ).build(session).obj
        invoice.create_receipt(invoice.effective_sum)
        invoice.transfer(order, TransferMode.dst, 50)

        migrate_client(order.client)
        mk_shipment(order, 42, VERY_PAST)

        create_act_for_consumes(session, invoice.consumes)

        mk_shipment(order, 41, VERY_PAST)

        session.expire_all()

        order.manager = ob.SingleManagerBuilder.construct(session)

        task = create_order_task(order, 3)
        ProcessLogTariff(order, task.task_id).do()

        assert_consumes(
            order,
            [
                (42, 42, 42, 42),
                (8, 8, 2, 2),
            ]
        )
        assert_task_consumes(
            task,
            [
                (order.consumes[0].id, 1, 1, 50, 50),
                (order.consumes[1].id, 2, 2, 8, 8),
            ]
        )

    def test_dynamic_discount(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [10, 20, 30])
        migrate_client(order.client)
        mk_shipment(order, 10, money=0)
        order.completion_consumed_money = 0
        ob.add_dynamic_discount(order.consumes[0], 42)

        task = create_order_task(order, 13)

        ProcessLogTariff(order, task.task_id).do()

        assert_consumes(
            order,
            [
                (10, 174, 10, 174),
                (0, 0, 0, 0),
                (0, 0, 0, 0),
                (D('4.2'), 126, D('0.433333'), 13),
                (20, 600, 0, 0),
                (30, 900, 0, 0),
            ]
        )
        assert_task_consumes(
            task,
            [
                (order.consumes[3].id, 13, 13, 126, 126),
            ]
        )

    def test_dynamic_discount_acted(self, session):
        order = create_order(session, DIRECT_PRODUCT_ID, [10, 20, 30])
        migrate_client(order.client)
        mk_shipment(order, 0, money=300)
        consume = order.consumes[0]
        create_act_for_consumes(session, [order.consumes[0]], backdate=NOW)
        consume.invoice.person.type = PersonCategoryCodes.russia_resident_legal_entity
        mk_shipment(order, 0, money=0)

        order.completion_consumed_money = D('0')
        ob.add_dynamic_discount(consume, 42)

        task = create_order_task(order, D('13'))

        ProcessLogTariff(order, task.task_id).do()

        assert_consumes(
            order,
            [
                (10, 174, D('0.433333'), D('7.54')),
                (0, 0, 0, 0),
                (0, 0, 0, 0),
                (D('4.2'), 126, 0, 0),
                (20, 600, 0, 0),
                (30, 900, 0, 0),
            ]
        )
        assert_task_consumes(
            task,
            [
                (consume.id, 13, D('7.54'), D('517.24137'), D('300')),
            ]
        )


class TestFindLogTariff(object):
    def test_find_log_tariff_task_id(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID)
        task = create_order_task(order, 1)

        log_tariff_task_id = find_log_tariff_task_id(session, order)
        assert log_tariff_task_id == task.task_id

    def test_find_log_tariff_task_id_several_not_archived_tasks(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID)
        task_1 = create_order_task(order, 1)
        task_2 = create_order_task(order, 1)

        with pytest.raises(exc.SEVERAL_NOT_ARCHIVED_TASKS) as exc_info:
            find_log_tariff_task_id(session, order)
        msg = 'process completions for order {order} failed: several tasks {task_list} aren\'t archived'.format(
            order=order.id, task_list=[task_1.task_id, task_2.task_id])
        assert exc_info.value.msg == msg

    def test_find_log_tariff_task_id_several_not_archived_tasks_process_order(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID)
        task_1 = create_order_task(order, 1)
        task_2 = create_order_task(order, 1)

        with pytest.raises(exc.SEVERAL_NOT_ARCHIVED_TASKS) as exc_info:
            process_order(order, None)
        msg = 'process completions for order {order} failed: several tasks {task_list} aren\'t archived'.format(
            order=order.id, task_list=[task_1.task_id, task_2.task_id])
        assert exc_info.value.msg == msg

    def test_several_not_archived_tasks_process_order_with_input(self, session):
        '''
        Проверяем, что если в input есть log_tariff_task_id, то find_log_tariff_task_id не зовется, и
        исключение SEVERAL_NOT_ARCHIVED_TASKS не вызывается
        '''
        order = create_order(session, DIRECT_PRODUCT_RUB_ID)
        migrate_client(order.client)
        mk_shipment(order, 33)
        task_1 = create_order_task(order, 21)
        task_2 = create_order_task(order, 1)

        process_order(order, {'log_tariff_task_id': task_1.task_id})

        assert order.completion_qty == 54
        assert order.shipment.money == 54

        process_order(order, {'log_tariff_task_id': task_2.task_id})

        assert order.completion_qty == 55
        assert order.shipment.money == 55

    def test_find_log_tariff_task_cant_find_task(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID)

        with pytest.raises(exc.CANT_FIND_TASK_FOR_ORDER) as exc_info:
            find_log_tariff_task_id(session, order)
        msg = 'process completions for order {order} failed: can\'t find task for order'.format(order=order.id)
        assert exc_info.value.msg == msg

    def test_find_log_tariff_task_cant_find_task_process_order(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID)
        order._is_log_tariff = OrderLogTariffState.MIGRATED

        assert process_order(order, None) is None

    def test_find_log_tariff_task_id_one_archived_task(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID)
        not_archived_task = create_order_task(order, 1)
        archived_task = create_order_task(order, 1)
        archived_task.task.state = NirvanaProcessingTaskState.ARCHIVED
        session.flush()

        log_tariff_task_id = find_log_tariff_task_id(session, order)
        assert log_tariff_task_id == not_archived_task.task_id

    def test_find_log_tariff_task_id_two_archived_tasks(self, session):
        order = create_order(session, DIRECT_PRODUCT_RUB_ID)
        first_archived_task = create_order_task(order, 1)
        first_archived_task.task.state = NirvanaProcessingTaskState.ARCHIVED
        first_archived_task.task.dt -= datetime.timedelta(seconds=1)
        second_archived_task = create_order_task(order, 1)
        second_archived_task.task.state = NirvanaProcessingTaskState.ARCHIVED
        session.flush()

        log_tariff_task_id = find_log_tariff_task_id(session, order)
        assert log_tariff_task_id == second_archived_task.task_id


class TestAutoOverdraft(object):
    def create_order(
        self,
        session,
        product_id=cst.DIRECT_PRODUCT_ID,
        consumes_qtys=None,
        discount_pct=0,
        dt=None,
        client_limit=D('20'),
        auto_overdraft=True
    ):
        client = get_client(session, convert_type=cst.CONVERT_TYPE_MODIFY)
        person = get_person(session, client)

        if auto_overdraft:
            get_overdraft_params(session, client, person, client_limit=client_limit)

        order = ob.OrderBuilder(
            client=client,
            product=ob.Getter(mapper.Product, product_id),
        ).build(session).obj

        consumes_qtys = consumes_qtys if consumes_qtys is not None else [666]
        consume_order(order, consumes_qtys, discount_pct, dt)
        for invoice_order in order.invoice_orders:
            if dt:
                invoice_order.invoice.turn_on_dt = dt
        return order

    def test_simple_auto_overdraft(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order, 60, dt=dt)
        task = create_order_task(order, 11, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 60
        assert order.shipment.money == 60

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        co1, co2, co3, co4 = order.consumes

        assert order.consume_qty == 71
        assert order.completion_qty == 71
        assert order.shipment.money == 71

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (11, 11, 11, 11),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co4.id, 11, 11, 11, 11),
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 1
        overdraft_invoice = overdraft_invoices[0]
        assert overdraft_invoice.is_auto_overdraft is True
        assert overdraft_invoice.total_sum == 11
        assert overdraft_invoice.total_act_sum == 0
        assert len(overdraft_invoice.acts) == 0
        operation, = overdraft_invoice.operations
        assert 'is_truncated' in operation.memo

    def test_no_auto_overdraft_last_day_completions(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [60, ], dt=dt)
        task = create_order_task(order, 50, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        co, = order.consumes

        assert order.consume_qty == 60
        assert order.completion_qty == 50
        assert order.shipment.money == 50

        assert_consumes(
            order,
            [
                (60, 60, 50, 50),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co.id, 50, 50, 60, 60),
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 0

    def test_last_day_completions(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order, 45, dt=dt)
        task = create_order_task(order, 25, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 45
        assert order.shipment.money == 45

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        co1, co2, co3, co4 = order.consumes

        assert order.consume_qty == 70
        assert order.completion_qty == 70
        assert order.shipment.money == 70

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (10, 10, 10, 10),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co2.id, 5, 5, 20, 20),
                (co3.id, 10, 10, 10, 10),
                (co4.id, 10, 10, 10, 10),
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 1
        overdraft_invoice = overdraft_invoices[0]
        assert overdraft_invoice.is_auto_overdraft is True
        assert overdraft_invoice.total_sum == 10
        assert overdraft_invoice.total_act_sum == 0
        assert len(overdraft_invoice.acts) == 0
        operation, = overdraft_invoice.operations
        assert 'is_truncated' in operation.memo

    def test_last_day_rollback(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order, 45, dt=dt)
        task = create_order_task(order, -3, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 45
        assert order.shipment.money == 45

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        co1, co2, co3 = order.consumes

        assert order.consume_qty == 60
        assert order.completion_qty == 42
        assert order.shipment.money == 42

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 12, 12),
                (10, 10, 0, 0),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co2.id, -3, -3, 20, 20),
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 0

    def test_fish_last_day_completions(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        prev_dt = dt - datetime.timedelta(days=1)
        order = self.create_order(session, DIRECT_PRODUCT_ID, [20], dt=dt, client_limit=9000)
        migrate_client(order.client)
        mk_shipment(order, 0, money=0, dt=dt)
        order.completion_consumed_money = 0
        session.flush()

        prev_task = create_order_task(order, 510, tariff_dt=prev_dt, group_dt=prev_dt)
        ProcessLogTariff(order, prev_task.task_id).do()

        task = create_order_task(order, 120, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        ProcessLogTariff(order, task.task_id, obj_lock=True, shared_lock=True).do()
        co1, co2 = order.consumes

        assert order.consume_qty == 21
        assert order.completion_qty == 21
        assert order.shipment.money == 630

        assert_consumes(
            order,
            [
                (20, 600, 20, 600),
                (1, 30, 1, 30),
            ]
        )
        assert_task_consumes(
            prev_task,
            [
                (co1.id, 510, 510, 600, 600),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co1.id, 90, 90, 600, 600),
                (co2.id, 30, 30, 30, 30),
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 1
        overdraft_invoice = overdraft_invoices[0]
        assert overdraft_invoice.is_auto_overdraft is True
        assert overdraft_invoice.total_sum == 30
        assert overdraft_invoice.total_act_sum == 0
        assert len(overdraft_invoice.acts) == 0
        operation, = overdraft_invoice.operations
        assert 'is_truncated' in operation.memo

    def test_completion_up_to_limit(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order, 60, dt=dt)
        task = create_order_task(order, 20, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 60
        assert order.shipment.money == 60

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        co1, co2, co3, co4 = order.consumes

        assert order.consume_qty == 80
        assert order.completion_qty == 80
        assert order.shipment.money == 80

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (20, 20, 20, 20),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co4.id, 20, 20, 20, 20),
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 1
        overdraft_invoice = overdraft_invoices[0]
        assert overdraft_invoice.is_auto_overdraft is True
        assert overdraft_invoice.total_sum == 20
        assert overdraft_invoice.total_act_sum == 0
        assert len(overdraft_invoice.acts) == 0
        operation, = overdraft_invoice.operations
        assert 'is_truncated' in operation.memo

    def test_completion_over_limit(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order, 60, dt=dt)
        task = create_order_task(order, 30, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        co1, co2, co3, co4 = order.consumes

        assert order.consume_qty == 80
        assert order.completion_qty == 90
        assert order.shipment.money == 90

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (20, 20, 20, 20),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co4.id, 20, 20, 20, 20),
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 1
        overdraft_invoice = overdraft_invoices[0]
        assert overdraft_invoice.is_auto_overdraft is True
        assert overdraft_invoice.total_sum == 20
        assert overdraft_invoice.total_act_sum == 0
        assert len(overdraft_invoice.acts) == 0
        operation, = overdraft_invoice.operations
        assert 'is_truncated' in operation.memo

    def test_process_order(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        order._is_log_tariff = OrderLogTariffState.MIGRATED
        mk_shipment(order, 60, dt=dt)
        task = create_order_task(order, 20, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 60
        assert order.shipment.money == 60

        process_order(order, {'log_tariff_task_id': task.task_id})
        co1, co2, co3, co4 = order.consumes

        assert order.consume_qty == 80
        assert order.completion_qty == 80
        assert order.shipment.money == 80

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (20, 20, 20, 20),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co4.id, 20, 20, 20, 20),
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 1
        overdraft_invoice = overdraft_invoices[0]
        assert overdraft_invoice.is_auto_overdraft is True
        assert overdraft_invoice.total_sum == 20
        assert overdraft_invoice.total_act_sum == 0
        assert len(overdraft_invoice.acts) == 0
        operation, = overdraft_invoice.operations
        assert 'is_truncated' in operation.memo

    def test_current_month_completions(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        consume_order(order, [15, 10, 5])
        mk_shipment(order, 80)
        task = create_order_task(order, 20, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 90
        assert order.completion_qty == 80
        assert order.shipment.money == 80

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        co1, co2, co3, co4, co5, co6, co7, co8, co9 = order.consumes

        assert order.consume_qty == 110
        assert order.completion_qty == 100
        assert order.shipment.money == 100

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (15, 15, 15, 15),
                (5, 5, 5, 5),
                (0, 0, 0, 0),
                (20, 20, 20, 20),
                (5, 5, 0, 0),
                (5, 5, 0, 0),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co7.id, 20, 20, 20, 20)
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 1
        overdraft_invoice = overdraft_invoices[0]
        assert overdraft_invoice.is_auto_overdraft is True
        assert overdraft_invoice.total_sum == 20
        assert overdraft_invoice.total_act_sum == 0
        assert len(overdraft_invoice.acts) == 0
        operation, = overdraft_invoice.operations
        assert 'is_truncated' in operation.memo

    def test_current_month_group_dt(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        current_month_dt = dt + datetime.timedelta(1)
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order, 60, dt=dt)
        task_1 = create_order_task(order, 20, tariff_dt=dt, group_dt=dt)
        task_2 = create_order_task(order, 30, tariff_dt=current_month_dt, group_dt=current_month_dt)

        task_2.task_id = task_1.task_id
        task_1.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 60
        assert order.shipment.money == 60

        ProcessLogTariff(
            order,
            task_1.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        co1, co2, co3, co4 = order.consumes

        assert order.consume_qty == 80
        assert order.completion_qty == 110
        assert order.shipment.money == 110

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (20, 20, 20, 20),
            ]
        )
        assert_task_consumes(
            task_1,
            [
                (co4.id, 20, 20, 20, 20),
            ]
        )
        assert_task_consumes(task_2, [])

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 1
        overdraft_invoice = overdraft_invoices[0]
        assert overdraft_invoice.is_auto_overdraft is True
        assert overdraft_invoice.total_sum == 20
        assert overdraft_invoice.total_act_sum == 0
        assert len(overdraft_invoice.acts) == 0
        operation, = overdraft_invoice.operations
        assert 'is_truncated' in operation.memo

    def test_current_month_completions_and_group_dt(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        current_month_dt = dt + datetime.timedelta(1)
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        consume_order(order, [15, 10, 5])
        mk_shipment(order, 80)
        task_1 = create_order_task(order, 20, tariff_dt=dt, group_dt=dt)
        task_2 = create_order_task(order, 30, tariff_dt=current_month_dt, group_dt=current_month_dt)

        task_2.task_id = task_1.task_id
        task_1.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 90
        assert order.completion_qty == 80
        assert order.shipment.money == 80

        ProcessLogTariff(
            order,
            task_1.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        co1, co2, co3, co4, co5, co6, co7, co8, co9 = order.consumes

        assert order.consume_qty == 110
        assert order.completion_qty == 130
        assert order.shipment.money == 130

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (15, 15, 15, 15),
                (5, 5, 5, 5),
                (0, 0, 0, 0),
                (20, 20, 20, 20),
                (5, 5, 5, 5),
                (5, 5, 5, 5),
            ]
        )
        assert_task_consumes(
            task_1,
            [
                (co7.id, 20, 20, 20, 20)
            ]
        )
        assert_task_consumes(
            task_2,
            [
                (co8.id, 5, 5, 5, 5),
                (co9.id, 5, 5, 5, 5),
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 1
        overdraft_invoice = overdraft_invoices[0]
        assert overdraft_invoice.is_auto_overdraft is True
        assert overdraft_invoice.total_sum == 20
        assert overdraft_invoice.total_act_sum == 0
        assert len(overdraft_invoice.acts) == 0
        operation, = overdraft_invoice.operations
        assert 'is_truncated' in operation.memo

    def test_two_orders_overcompleted(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt

        order_1 = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order_1, 60, dt=dt)
        task_1 = create_order_task(order_1, 15, tariff_dt=dt, group_dt=dt)

        order_2 = ob.OrderBuilder(
            client=order_1.client,
            product=ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID),
        ).build(session).obj
        consumes_qtys = [30, 20, 10]
        consume_order(order_2, consumes_qtys, dt=dt)
        for invoice_order in order_2.invoice_orders:
            invoice_order.invoice.turn_on_dt = dt
        mk_shipment(order_2, 60, dt=dt)
        task_2 = create_order_task(order_2, 10, tariff_dt=dt, group_dt=dt)

        task_2.task_id = task_1.task_id
        task_1.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        ProcessLogTariff(
            order_1,
            task_1.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        ProcessLogTariff(
            order_2,
            task_1.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()

        co1_1, co1_2, co1_3, co1_4 = order_1.consumes
        assert order_1.consume_qty == 75
        assert order_1.completion_qty == 75
        assert order_1.shipment.money == 75
        assert_consumes(
            order_1,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (15, 15, 15, 15),
            ]
        )
        assert_task_consumes(
            task_1,
            [
                (co1_4.id, 15, 15, 15, 15),
            ]
        )

        co2_1, co2_2, co2_3, co2_4 = order_2.consumes
        assert order_2.consume_qty == 65
        assert order_2.completion_qty == 70
        assert order_2.shipment.money == 70
        assert_consumes(
            order_2,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (5, 5, 5, 5),
            ]
        )
        assert_task_consumes(
            task_2,
            [
                (co2_4.id, 5, 5, 5, 5),
            ]
        )

        overdraft_invoices_1 = [io.invoice for io in order_1.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices_1) == 1
        overdraft_invoice_1 = overdraft_invoices_1[0]
        assert overdraft_invoice_1.is_auto_overdraft is True
        assert overdraft_invoice_1.total_sum == 15
        assert overdraft_invoice_1.total_act_sum == 0
        assert len(overdraft_invoice_1.acts) == 0
        operation, = overdraft_invoice_1.operations
        assert 'is_truncated' in operation.memo

        overdraft_invoices_2 = [io.invoice for io in order_2.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices_2) == 1
        overdraft_invoice_2 = overdraft_invoices_2[0]
        assert overdraft_invoice_2.is_auto_overdraft is True
        assert overdraft_invoice_2.total_sum == 5
        assert overdraft_invoice_2.total_act_sum == 0
        assert len(overdraft_invoice_2.acts) == 0
        operation, = overdraft_invoice_2.operations
        assert 'is_truncated' in operation.memo

    def test_two_orders_one_overcompleted(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt

        order_1 = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order_1, 60, dt=dt)
        task_1 = create_order_task(order_1, 15, tariff_dt=dt, group_dt=dt)

        order_2 = ob.OrderBuilder(
            client=order_1.client,
            product=ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID),
        ).build(session).obj
        consumes_qtys = [60, ]
        consume_order(order_2, consumes_qtys, dt=dt)
        for invoice_order in order_2.invoice_orders:
            invoice_order.invoice.turn_on_dt = dt
        task_2 = create_order_task(order_2, 50, tariff_dt=dt, group_dt=dt)

        task_2.task_id = task_1.task_id
        task_1.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        ProcessLogTariff(
            order_1,
            task_1.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        ProcessLogTariff(
            order_2,
            task_1.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()

        co1_1, co1_2, co1_3, co1_4 = order_1.consumes
        assert order_1.consume_qty == 75
        assert order_1.completion_qty == 75
        assert order_1.shipment.money == 75
        assert_consumes(
            order_1,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (15, 15, 15, 15),
            ]
        )
        assert_task_consumes(
            task_1,
            [
                (co1_4.id, 15, 15, 15, 15),
            ]
        )

        co2, = order_2.consumes
        assert order_2.consume_qty == 60
        assert order_2.completion_qty == 50
        assert order_2.shipment.money == 50
        assert_consumes(
            order_2,
            [
                (60, 60, 50, 50),
            ]
        )
        assert_task_consumes(
            task_2,
            [
                (co2.id, 50, 50, 60, 60),
            ]
        )

        overdraft_invoices_1 = [io.invoice for io in order_1.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices_1) == 1
        overdraft_invoice_1 = overdraft_invoices_1[0]
        assert overdraft_invoice_1.is_auto_overdraft is True
        assert overdraft_invoice_1.total_sum == 15
        assert overdraft_invoice_1.total_act_sum == 0
        assert len(overdraft_invoice_1.acts) == 0
        operation, = overdraft_invoice_1.operations
        assert 'is_truncated' in operation.memo

        overdraft_invoices_2 = [io.invoice for io in order_2.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices_2) == 0

    def test_two_orders_one_completed_over_limit(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt

        order_1 = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order_1, 60, dt=dt)
        task_1 = create_order_task(order_1, 25, tariff_dt=dt, group_dt=dt)

        order_2 = ob.OrderBuilder(
            client=order_1.client,
            product=ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID),
        ).build(session).obj
        consumes_qtys = [60, ]
        consume_order(order_2, consumes_qtys, dt=dt)
        for invoice_order in order_2.invoice_orders:
            invoice_order.invoice.turn_on_dt = dt
        task_2 = create_order_task(order_2, 50, tariff_dt=dt, group_dt=dt)

        task_2.task_id = task_1.task_id
        task_1.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        ProcessLogTariff(
            order_1,
            task_1.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        ProcessLogTariff(
            order_2,
            task_1.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()

        co1_1, co1_2, co1_3, co1_4 = order_1.consumes
        assert order_1.consume_qty == 80
        assert order_1.completion_qty == 85
        assert order_1.shipment.money == 85
        assert_consumes(
            order_1,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
                (20, 20, 20, 20),
            ]
        )
        assert_task_consumes(
            task_1,
            [
                (co1_4.id, 20, 20, 20, 20),
            ]
        )

        co2, = order_2.consumes
        assert order_2.consume_qty == 60
        assert order_2.completion_qty == 50
        assert order_2.shipment.money == 50
        assert_consumes(
            order_2,
            [
                (60, 60, 50, 50),
            ]
        )
        assert_task_consumes(
            task_2,
            [
                (co2.id, 50, 50, 60, 60),
            ]
        )

        overdraft_invoices_1 = [io.invoice for io in order_1.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices_1) == 1
        overdraft_invoice_1 = overdraft_invoices_1[0]
        assert overdraft_invoice_1.is_auto_overdraft is True
        assert overdraft_invoice_1.total_sum == 20
        assert overdraft_invoice_1.total_act_sum == 0
        assert len(overdraft_invoice_1.acts) == 0
        operation, = overdraft_invoice_1.operations
        assert 'is_truncated' in operation.memo

        overdraft_invoices_2 = [io.invoice for io in order_2.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices_2) == 0

    def test_incremental_validator_error(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        last_tariffed_dt = mapper.ActMonth(current_month=datetime.datetime.now()).begin_dt - datetime.timedelta(days=1)
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10])
        task = create_order_task(order, 23, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.NOT_STARTED,
            'auto_overdraft_dt': last_tariffed_dt.strftime('%Y-%m-%d')
        }
        session.flush()

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(
                order,
                task.task_id,
                obj_lock=True,
                shared_lock=True
            ).do()

        msg = "can't tariff due to auto_overdraft"
        log_intercept.assert_exc(exc.LOG_TARIFF_CANT_PROCESS, msg)
        assert msg in task.error

        assert order.consume_qty == 60
        assert order.completion_qty == 0
        assert order.shipment.money is None
        assert_consumes(
            order,
            [
                (30, 30, 0, 0),
                (20, 20, 0, 0),
                (10, 10, 0, 0),
            ]
        )
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                consumes=[],
                res_tariff_dt=dt,
                res_state=task.state
            )
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 0

    def test_incremental_validator_no_overdraft(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        past_dt = mapper.ActMonth(current_month=dt).document_dt
        order = create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        migrate_client(order.client)
        mk_shipment(order, 20, dt=dt)
        task = create_order_task(order, 23, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()

        co1, co2, co3 = order.consumes

        assert order.consume_qty == 60
        assert order.completion_qty == 43
        assert order.shipment.money == 43
        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 13, 13),
                (10, 10, 0, 0),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co1.id, 10, 10, 30, 30),
                (co2.id, 13, 13, 20, 20),
            ]
        )
        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 0

    def test_incremental_validator_no_current_month_invoice(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        past_dt = mapper.ActMonth(current_month=dt).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order, 20, dt=dt)
        task = create_order_task(order, 23, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': past_dt.strftime('%Y-%m-%d')
        }
        session.flush()

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()

        co1, co2, co3 = order.consumes

        assert order.consume_qty == 60
        assert order.completion_qty == 43
        assert order.shipment.money == 43
        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 13, 13),
                (10, 10, 0, 0),
            ]
        )
        assert_task_consumes(
            task,
            [
                (co1.id, 10, 10, 30, 30),
                (co2.id, 13, 13, 20, 20),
            ]
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 0

    def test_current_month_act(self, session):
        now = datetime.datetime.now()
        dt = mapper.ActMonth(current_month=now).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order, 45, dt=dt)
        for co in order.consumes:
            co.invoice.turn_on_dt = now
        create_act_for_consumes(session, order.consumes)
        task = create_order_task(order, 20, tariff_dt=dt, group_dt=dt)
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()
        session.expire_all()

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(
                order,
                task.task_id,
                obj_lock=True,
                shared_lock=True
            ).do()

        msg = "Invoice %s that is to be withdrawn has acts" % order.consumes[1].invoice_id
        log_intercept.assert_exc(exc.LOG_TARIFF_CANT_PROCESS, msg)
        assert msg in task.error

    def test_check_multiple_rows(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [333], dt=dt)
        state = calc_log_tariff_state(333, [])
        task_1 = create_order_task(order, 20, tariff_dt=dt, group_dt=dt, state=state, currency_id=ISO_NUM_CODE_RUB)
        task_2 = create_order_task(order, 20, tariff_dt=dt, group_dt=dt, state=state, currency_id=0)
        task_2.task_id = task_1.task_id
        task_1.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(order, task_1.task_id).do()

        msg = 'invalid number of order tasks for processor'
        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, msg)
        assert msg in task_1.error
        assert msg in task_2.error

        assert order.completion_qty == 0
        assert order.shipment.money is None
        hamcrest.assert_that(
            [task_1, task_2],
            hamcrest.only_contains(
                hamcrest.has_properties(
                    consumes=[],
                    res_tariff_dt=dt,
                    res_state=state
                )
            )
        )

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 0

    @pytest.mark.parametrize(
        'generate_auto_overdraft',
        [
            GenerateAutoOverdraftState.NOT_STARTED,
            GenerateAutoOverdraftState.IN_PROGRESS,
            GenerateAutoOverdraftState.FINISHED
        ]
    )
    def test_no_overdraft_params(self, session, generate_auto_overdraft):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt, auto_overdraft=False)
        mk_shipment(order, 60, dt=dt)
        task = create_order_task(order, 20, tariff_dt=dt, group_dt=dt)
        if generate_auto_overdraft == GenerateAutoOverdraftState.NOT_STARTED:
            prev_month_first_day = mapper.ActMonth(current_month=datetime.datetime.now()).begin_dt
            auto_overdraft_dt = prev_month_first_day - datetime.timedelta(days=1)
        else:
            auto_overdraft_dt = dt
        task.task.metadata = {
            'generate_auto_overdraft': generate_auto_overdraft,
            'auto_overdraft_dt': auto_overdraft_dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 60
        assert order.shipment.money == 60

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()

        assert order.consume_qty == 60
        assert order.completion_qty == 80
        assert order.shipment.money == 80

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
            ]
        )
        assert_task_consumes(task, [])

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 0

    def test_no_last_day_row(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order, 60, dt=dt)
        task = create_order_task(order, 20, tariff_dt=dt, group_dt=dt + datetime.timedelta(1))
        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 60
        assert order.shipment.money == 60

        ProcessLogTariff(
            order,
            task.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()

        assert order.consume_qty == 60
        assert order.completion_qty == 80
        assert order.shipment.money == 80

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
            ]
        )
        assert_task_consumes(task, [])

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 0

    def test_too_many_auto_overdraft_rows(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        mk_shipment(order, 60, dt=dt)
        task_1 = create_order_task(order, 20, tariff_dt=dt, group_dt=dt)
        task_2 = create_order_task(order, 20, tariff_dt=dt, group_dt=dt - datetime.timedelta(1))
        task_2.task_id = task_1.task_id
        task_1.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.IN_PROGRESS,
            'auto_overdraft_dt': dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 60
        assert order.shipment.money == 60

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(
                order,
                task_1.task_id,
                obj_lock=True,
                shared_lock=True
            ).do()

        msg = "Too many rows with group_dt <= {}".format(dt)
        log_intercept.assert_exc(exc.LOG_TARIFF_TASK_INVALID, msg)
        assert msg in task_1.error

        assert order.consume_qty == 60
        assert order.completion_qty == 60
        assert order.shipment.money == 60

        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 20, 20),
                (10, 10, 10, 10),
            ]
        )
        assert_task_consumes(task_1, [])
        assert_task_consumes(task_2, [])

        overdraft_invoices = [io.invoice for io in order.invoice_orders if io.invoice.type == 'overdraft']
        assert len(overdraft_invoices) == 0

    def test_skip_auto_overdraft_tasks(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        current_month_dt = dt + datetime.timedelta(1)
        last_tariffed_dt = mapper.ActMonth(current_month=datetime.datetime.now()).begin_dt - datetime.timedelta(days=1)
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        task_1 = create_order_task(order, 33, tariff_dt=dt, group_dt=dt)
        task_2 = create_order_task(order, 21, tariff_dt=current_month_dt, group_dt=current_month_dt)

        task_2.task_id = task_1.task_id
        task_1.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.NOT_STARTED,
            'auto_overdraft_dt': last_tariffed_dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 0
        assert order.shipment.money is None

        ProcessLogTariff(
            order,
            task_1.task_id,
            obj_lock=True,
            shared_lock=True
        ).do()
        co1, co2, co3 = order.consumes

        assert order.shipment.dt == dt
        assert order.completion_qty == 33
        assert order.shipment.money == 33
        assert_consumes(
            order,
            [
                (30, 30, 30, 30),
                (20, 20, 3, 3),
                (10, 10, 0, 0),
            ]
        )

        assert_task_consumes(
            task_1,
            [
                (co1.id, 30, 30, 30, 30),
                (co2.id, 3, 3, 20, 20),
            ]
        )
        assert_task_consumes(
            task_2,
            []
        )
        expected_res_state = calc_log_tariff_state(co1.id, [(co1, 30), (co2, 3)])
        hamcrest.assert_that(
            task_1,
            hamcrest.has_properties(
                res_tariff_dt=dt,
                res_state=expected_res_state,
                error=None,
            )
        )
        hamcrest.assert_that(
            task_2,
            hamcrest.has_properties(
                res_tariff_dt=current_month_dt,
                res_state=expected_res_state,
                error='Skipping auto_overdraft tasks',
            )
        )

    def test_skip_auto_overdraft_tasks_skip_all(self, session):
        dt = mapper.ActMonth(current_month=datetime.datetime.now()).document_dt
        current_month_dt = dt + datetime.timedelta(1)
        last_tariffed_dt = mapper.ActMonth(current_month=datetime.datetime.now()).begin_dt - datetime.timedelta(days=1)
        order = self.create_order(session, DIRECT_PRODUCT_RUB_ID, [30, 20, 10], dt=dt)
        state = calc_log_tariff_state(333, [])
        task = create_order_task(order, 21, tariff_dt=current_month_dt, group_dt=current_month_dt, state=state)

        task.task.metadata = {
            'generate_auto_overdraft': GenerateAutoOverdraftState.NOT_STARTED,
            'auto_overdraft_dt': last_tariffed_dt.strftime('%Y-%m-%d')
        }
        session.flush()

        assert order.consume_qty == 60
        assert order.completion_qty == 0
        assert order.shipment.money is None

        with LogExceptionInterceptHandler() as log_intercept:
            ProcessLogTariff(
                order,
                task.task_id,
                obj_lock=True,
                shared_lock=True
            ).do()

        msg = "all tasks is skipped due to auto_overdraft"
        log_intercept.assert_exc(exc.LOG_TARIFF_CANT_PROCESS, msg)
        assert msg in task.error

        assert order.completion_qty == 0
        assert order.shipment.money is None
        assert order.shipment.dt is None
        assert_consumes(
            order,
            [
                (30, 30, 0, 0),
                (20, 20, 0, 0),
                (10, 10, 0, 0),
            ]
        )
        assert_task_consumes(
            task,
            []
        )
        hamcrest.assert_that(
            task,
            hamcrest.has_properties(
                res_tariff_dt=current_month_dt,
                res_state=state,
            )
        )


class TestChildOrder(object):
    @pytest.mark.parametrize(
        'params',
        [
            pytest.param(
                {
                    'consume_qty': (10, 0), 'shipments': (5, 0),
                    'task_qtys': (4, 1), 'res_completion': (9, 1), 'res_consume': (9, 1),
                    'main_order_consumes': [(9, 9, 9, 9)], 'ch_order_consumes': [(1, 1, 1, 1)],
                    'main_task_res': [(4, 4, 10, 10)], 'child_task_res': [(1, 1, 1, 1)],
                },
                # если сначала обрабатываем открутки для ОС, то на нём ещё нет перевода средств с дочернего
                id='completion main -> child',
            ),
            pytest.param(
                {
                    'consume_qty': (10, 0), 'shipments': (5, 0),
                    'reverse': True,
                    'task_qtys': (4, 1), 'res_completion': (9, 1), 'res_consume': (9, 1),
                    'main_order_consumes': [(9, 9, 9, 9)], 'ch_order_consumes': [(1, 1, 1, 1)],
                    'main_task_res': [(4, 4, 10, 10)], 'child_task_res': [(1, 1, 1, 1)],
                },
                # если же сначала обрабатываем дочерний, то на ОС уже нет денег, которые на дочерний перевели
                id='completion child -> main',
            ),
            pytest.param(
                {
                    'consume_qty': (10, 1), 'shipments': (5, 0),
                    'task_qtys': (4, 1), 'res_completion': (9, 1), 'res_consume': (10, 1),
                    'main_order_consumes': [(10, 10, 9, 9)], 'ch_order_consumes': [(1,) * 4],
                    'main_task_res': [(4, 4, 10, 10)], 'child_task_res': [(1, 1, 1, 1)],
                },
                id='complete existing consume_qty main -> child',
            ),
            pytest.param(
                {
                    'consume_qty': (10, 2), 'shipments': (5, 2),
                    'task_qtys': (6, -1), 'res_completion': (11, 1), 'res_consume': (10, 2),
                    'main_order_consumes': [(10,) * 4], 'ch_order_consumes': [(2, 2, 1, 1)],
                    'main_task_res': [(5, 5, 10, 10)], 'child_task_res': [(-1, -1, 2, 2)],
                },
                # если сначала разбираем открутки на главном, то получаем на нём перекрутку,
                # которая не попала на конзюм, потому что он был сформирован позже
                # по идее она попадет в нетарифицированное и будет обработана на след шаге
                id='rollback main -> child',
            ),
            pytest.param(
                {
                    'child_product_id': cst.DIRECT_PRODUCT_ID,
                    'consume_qty': (40, 0), 'shipments': (5, 0),
                    'task_qtys': (5, 30), 'res_completion': (10, 1), 'res_consume': (10, 1),
                    'main_order_consumes': [(10,) * 4], 'ch_order_consumes': [(1, 30, 1, 30)],
                    'main_task_res': [(5, 5, 40, 40)], 'child_task_res': [(30,) * 4],
                },
                id='fish child completion main -> child',
            ),
            pytest.param(
                {
                    'child_product_id': cst.DIRECT_PRODUCT_ID,
                    'consume_qty': (300, 0), 'shipments': (150, 0),
                    'reverse': True,
                    'task_qtys': (120, 30), 'res_completion': (270, 1), 'res_consume': (270, 1),
                    'main_order_consumes': [(270,) * 4], 'ch_order_consumes': [(1, 30, 1, 30)],
                    'main_task_res': [(120, 120, 300, 300)], 'child_task_res': [(30,) * 4],
                },
                id='fish child completion child -> main',
            ),
            pytest.param(
                {
                    'child_product_id': cst.DIRECT_PRODUCT_ID,
                    'consume_qty': (40, 1), 'shipments': (5, 0),
                    'task_qtys': (5, 30), 'res_completion': (10, 1), 'res_consume': (40, 1),
                    'main_order_consumes': [(40, 40, 10, 10)], 'ch_order_consumes': [(1, 30, 1, 30)],
                    'main_task_res': [(5, 5, 40, 40)], 'child_task_res': [(30,) * 4],
                },
                id='fish child complete existing consume_qty main -> child',
            ),
            pytest.param(
                {
                    'child_product_id': cst.DIRECT_PRODUCT_ID,
                    'consume_qty': (300, 1), 'shipments': (150, 2),
                    'reverse': True,
                    'error': (exc.LOG_TARIFF_MUST_RECONSUME, 'broken consumes sequence'),
                    'task_qtys': (120, 30), 'res_completion': (270, 2), 'res_consume': (300, 1),
                    'main_order_consumes': [(300, 300, 270, 270)], 'ch_order_consumes': [(1, 30, 1, 30)],
                    'main_task_res': [(120, 120, 300, 300)], 'child_task_res': [],
                },
                # получается эта та же самая перекрутка, которая уже есть
                id='fish child overcompletion child -> main',
            ),
            pytest.param(
                {
                    'main_product_id': cst.DIRECT_PRODUCT_ID,
                    'consume_qty': (10, 0), 'shipments': (5, 0),
                    'task_qtys': (120, 30), 'res_completion': (9, 30), 'res_consume': (9, 30),
                    'main_order_consumes': [(9, 270, 9, 270)], 'ch_order_consumes': [(30,) * 4],
                    'main_task_res': [(120, 120, 300, 300)], 'child_task_res': [(30,) * 4],
                },
                id='fish main completion main -> child',
            ),
            pytest.param(
                {
                    'main_product_id': cst.DIRECT_PRODUCT_ID,
                    'consume_qty': (10, 0), 'shipments': (5, 0),
                    'reverse': True,
                    'task_qtys': (120, 30), 'res_completion': (9, 30), 'res_consume': (9, 30),
                    'main_order_consumes': [(9, 270, 9, 270)], 'ch_order_consumes': [(30,) * 4],
                    'main_task_res': [(120, 120, 300, 300)], 'child_task_res': [(30,) * 4],
                },
                id='fish main completion child -> main',
            ),
            pytest.param(
                {
                    'main_product_id': cst.DIRECT_PRODUCT_ID,
                    'consume_qty': (10, 60), 'shipments': (5, 60),
                    'task_qtys': (180, -30), 'res_completion': (11, 30), 'res_consume': (10, 60),
                    'main_order_consumes': [(10, 300, 10, 300)], 'ch_order_consumes': [(60, 60, 30, 30)],
                    'main_task_res': [(150, 150, 300, 300)], 'child_task_res': [(-30, -30, 60, 60)],
                },
                id='fish main rollback main -> child',
            ),
            pytest.param(
                {
                    'consume_qty': (10, 0), 'shipments': (5, 0),
                    'task_qtys': (5, 1), 'res_completion': (10, 1), 'res_consume': (10, 0),
                    'main_order_consumes': [(10,) * 4], 'ch_order_consumes': [],
                    'main_task_res': [(5, 5, 10, 10)], 'child_task_res': [],
                },
                # сначала разбираем ОС, свободных средств не остается и открутка дочернего попадет в нетарифицируемое
                id='overcompletion main -> child',
            ),
            pytest.param(
                {
                    'consume_qty': (10, 0), 'shipments': (5, 0),
                    'reverse': True,
                    'task_qtys': (5, 1), 'res_completion': (10, 1), 'res_consume': (9, 1),
                    'main_order_consumes': [(9, 9, 9, 9)], 'ch_order_consumes': [(1, 1, 1, 1)],
                    'main_task_res': [(4, 4, 10, 10)], 'child_task_res': [(1, 1, 1, 1)],
                },
                # если же сначала обрабатываем дочерний, то открутка образуется на ОС
                id='overcompletion child -> main',
            ),
            pytest.param(
                {
                    'consume_qty': (10, 1), 'shipments': (5, 1),
                    'task_qtys': (5, -1), 'res_completion': (10, 0), 'res_consume': (10, 1),
                    'main_order_consumes': [(10,) * 4], 'ch_order_consumes': [(1, 1, 0, 0)],
                    'main_task_res': [(5, 5, 10, 10)], 'child_task_res': [(-1, -1, 1, 1)],
                },
                id='available rollback main -> child',
            ),
            pytest.param(
                {
                    'consume_qty': (10, 1), 'shipments': (5, 2),
                    'task_qtys': (5, -1), 'res_completion': (10, 0), 'res_consume': (10, 1),
                    'main_order_consumes': [(10,) * 5], 'ch_order_consumes': [(1, 1, 0, 0)],
                    'main_task_res': [(5, 5, 10, 10)], 'child_task_res': [(-1, -1, 1, 1)],
                },
                # мы получает то, что должны разложить на конзюмы
                # т.е. -1 это уже новые открутки, включая более раннюю перекрутку на 1
                id='available rollback w overcompletion main -> child',
            ),
            pytest.param(
                {
                    'consume_qty': (10, 0), 'shipments': (5, 2),
                    'task_qtys': (5, 3), 'res_completion': (10, 3), 'res_consume': (10, 0),
                    'main_order_consumes': [(10,) * 4], 'ch_order_consumes': [],
                    'main_task_res': [(5, 5, 10, 10)], 'child_task_res': [],
                },
                id='existing + overcompletion main -> child',
            ),
            pytest.param(
                {
                    'consume_qty': (10, 0), 'shipments': (5, 2),
                    'reverse': True,
                    'task_qtys': (5, 3), 'res_completion': (10, 3), 'res_consume': (7, 3),
                    'main_order_consumes': [(7,) * 4], 'ch_order_consumes': [(3,) * 4],
                    'main_task_res': [(2, 2, 10, 10)], 'child_task_res': [(3,) * 4],
                },
                id='existing + overcompletion child -> main',
            ),
            pytest.param(
                {
                    'child_product_id': cst.DIRECT_PRODUCT_ID,
                    'reverse': True,
                    'consume_qty': (10, 0), 'shipments': (5, 0),
                    'task_qtys': (5, D('0.01')), 'res_completion': (10, D('0.000333')), 'res_consume': (D('9.99'), D('0.000333')),
                    'main_order_consumes': [(D('9.99'),) * 4], 'ch_order_consumes': [(D('0.000333'), D('0.01'), D('0.000333'), D('0.01'))],
                    'main_task_res': [(D('4.99'), D('4.99'), 10, 10)], 'child_task_res': [(D('0.00999'), D('0.01'), D('0.00999'), D('0.01'))],
                },
                id='rounding completion',
            ),
            pytest.param(
                {
                    'main_product_id': cst.DIRECT_PRODUCT_ID,
                    'consume_qty': (10, D('0.01')), 'shipments': (5, D('0.01')),
                    'reverse': True,
                    'task_qtys': (180, -D('0.01')), 'res_completion': (11, 0), 'res_consume': (10, D('0.01')),
                    'main_order_consumes': [(10, 300, 10, 300)], 'ch_order_consumes': [(D('0.01'), D('0.01'), 0, 0)],
                    'main_task_res': [(150, 150, 300, 300)],
                    'child_task_res': [(-D('0.01'), -D('0.01'), D('0.01'), D('0.01'))],
                },
                id='fish main rollback child -> main',
            ),
            pytest.param(
                {
                    'main_product_id': cst.DIRECT_PRODUCT_ID,
                    'child_product_id': cst.DIRECT_PRODUCT_ID,
                    'consume_qty': (10, 0), 'shipments': (0, 0),
                    'task_qtys': (0, D('0.000001')),
                    'res_completion': (0, 0),
                    'res_consume': (D('9.999999'), D('0.000001')),
                    'main_order_consumes': [(D('9.999999'), 300, 0, 0)],
                    'ch_order_consumes': [(D('0.000001'), 0, 0, 0)],
                    'main_task_res': [],
                    'child_task_res': [(D('0.000001'), 0, D('0.00003'), 0)],
                },
                id='fish rounding to zero',
            ),
        ],
    )
    def test_completion(self, session, client, params):
        main_order, (ch_order,) = create_orders(
            client,
            main_product_id=params.get('main_product_id', cst.DIRECT_PRODUCT_RUB_ID),
            children_product_ids=[params.get('child_product_id', cst.DIRECT_PRODUCT_RUB_ID)],
            force_log_tariff=True,
        )
        main_sh, ch_sh = params['consume_qty']
        consume_order(main_order, [main_sh])
        consume_order(ch_order, [ch_sh])

        main_shipment, child_shipment = params['shipments']
        mk_shipment(main_order, main_shipment)
        mk_shipment(ch_order, child_shipment)
        ch_order.completion_fixed_qty = ch_order.shipment.bucks
        ch_order.completion_consumed_money = child_shipment

        main_task_qty, ch_task_qty = params['task_qtys']
        main_task = create_order_task(main_order, main_task_qty)
        ch_task = create_order_task(ch_order, ch_task_qty, currency_id=params.get('ch_cur_id', ISO_NUM_CODE_RUB))

        processors = [
            ProcessLogTariff(main_order, main_task.task_id, obj_lock=True, shared_lock=True),
            ProcessLogTariff(ch_order, ch_task.task_id, obj_lock=True, shared_lock=True),
        ]
        if params.get('reverse', False):
            processors = processors[::-1]

        with LogExceptionInterceptHandler() as log_intercept:
            for processor in processors:
                processor.do()
                session.flush()

        if 'error' in params:
            err, msg = params['error']
            log_intercept.assert_exc(err, msg)
            assert msg in ch_task.error
        else:
            assert log_intercept.exceptions == []
            assert ch_task.error is None
        assert main_task.error is None

        res_main_qty, res_ch_qty = params['res_completion']
        assert main_order.completion_qty == res_main_qty
        assert ch_order.completion_qty == res_ch_qty

        res_main_qty, res_ch_qty = params['res_consume']
        assert main_order.consume_qty == res_main_qty
        assert ch_order.consume_qty == res_ch_qty

        assert_consumes(main_order, params['main_order_consumes'])
        assert_consumes(ch_order, params['ch_order_consumes'])

        assert_task_consumes(main_task, [((co.id,) + t_res) for co, t_res in zip(main_order.consumes, params['main_task_res'])])
        assert_task_consumes(ch_task, [((co.id,) + t_res) for co, t_res in zip(ch_order.consumes, params['child_task_res'])])

    @pytest.mark.parametrize(
        'person_type',
        [
            pytest.param('ph', id='person_type_ph'),
            pytest.param('ur', id='person_type_ur'),
        ]
    )
    def test_complete_archived(self, session, client, person_type):
        # На ОС все деньги заакчены, но потом произошел откат.
        # Переносим деньги на дочерний, а на ОС получаем переакт.
        ob.PersonBuilder(client=client, type=person_type).build(session)
        main_order, (ch_order,) = create_orders(client, force_log_tariff=True)

        consume_order(main_order, [10])
        mk_shipment(main_order, 10)

        main_co, = main_order.consumes
        create_act_for_consumes(session, [main_co])
        mk_shipment(main_order, 9)

        ch_task = create_order_task(ch_order, 1)
        ProcessLogTariff(ch_order, ch_task.task_id, obj_lock=True, shared_lock=True).do()

        assert main_order.completion_qty == 9
        assert ch_order.completion_qty == 1

        assert main_order.consume_qty == 9
        assert ch_order.consume_qty == 1

        assert_consumes(main_order, [(9,) * 4])
        assert_consumes(ch_order, [(1,) * 4])

        co, = ch_order.consumes
        assert_task_consumes(ch_task, [(co.id, 1, 1, 1, 1)])

    def test_rollback_archived(self, session, client):
        # На дочернем всё заакчено, но тут получаем откат
        # получаем на дочернем переакт
        main_order, (ch_order,) = create_orders(client, force_log_tariff=True)
        consume_order(main_order, [10])
        consume_order(ch_order, [2])

        mk_shipment(ch_order, 2)
        ch_order.completion_consumed_money = 2

        co, = ch_order.consumes
        create_act_for_consumes(session, [co])

        ch_task = create_order_task(ch_order, -1)
        ProcessLogTariff(ch_order, ch_task.task_id, obj_lock=True, shared_lock=True).do()

        assert main_order.completion_qty == 0
        assert ch_order.completion_qty == 1
        assert ch_order.completion_consumed_money == 2

        assert main_order.consume_qty == 10
        assert ch_order.consume_qty == 2

        assert_consumes(main_order, [(10, 10, 0, 0)])
        assert_consumes(ch_order, [(2, 2, 1, 1)])

        # здесь 2 потому что сначала записываем ответ в task.consumes, а потом уже переносим свободные деньги на ОС
        assert_task_consumes(ch_task, [(co.id, -1, -1, 2, 2)])

    @pytest.mark.parametrize(
        'params',
        [
            pytest.param(
                {
                    'task_qtys': (2, 2),
                    'completion_qty': 6,
                    'consume_qty': (6, 6),
                    'consumes': [(2,) * 4, (4,) * 4],
                    'res_task_1': ([1], [(2, 2, 4, 4)]),
                    'res_task_2': ([1], [(2, 2, 4, 4)]),
                },
                id='positive completions',
            ),
            pytest.param(
                {
                    'task_qtys': (2, 8),
                    'completion_qty': 12,
                    'consume_qty': (0, 12),
                    'consumes': [(2,) * 4, (10,) * 4],
                    'res_task_1': ([1], [(2, 2, 10, 10)]),
                    'res_task_2': ([1], [(8, 8, 10, 10)]),
                },
                id='all completed',
            ),
            pytest.param(
                {
                    'task_qtys': (2, 10),
                    'completion_qty': 14,
                    'consume_qty': (0, 12),
                    'consumes': [(2,) * 4, (10,) * 4],
                    'res_task_1': ([1], [(2, 2, 10, 10)]),
                    'res_task_2': ([1], [(8, 8, 10, 10)]),
                },
                id='overcompleted',
            ),
            pytest.param(
                {
                    'task_qtys': (10, 1),
                    'completion_qty': 13,
                    'consume_qty': (0, 12),
                    'consumes': [(2,) * 4, (10,) * 4],
                    'res_task_1': ([1], [(10,) * 4]),
                    'res_task_2': ([], [(0,) * 4]),
                },
                id='all completed w first task',
            ),
            pytest.param(
                {
                    'task_qtys': (10, -10),
                    'completion_qty': 2,
                    'consume_qty': (10, 2),
                    'consumes': [(2,) * 4],
                    'res_task_1': ([], []),
                    'res_task_2': ([], []),
                },
                id='compensated',
            ),
            pytest.param(
                {
                    'task_qtys': (-1, -1),
                    'completion_qty': 0,
                    'consume_qty': (10, 2),
                    'consumes': [(2, 2, 0, 0)],
                    'res_task_1': ([0], [(-1, -1, 2, 2)]),
                    'res_task_2': ([0], [(-1, -1, 2, 2)]),
                },
                id='both negative',
            ),
            pytest.param(
                {
                    'task_qtys': (D('0.00008'), D('0.0002')),
                    'completion_qty': D('2.00028'),
                    'consume_qty': (D('9.9997'), D('2.0003')),
                    'consumes': [(2,) * 4, (D('0.0003'), 0, D('0.00028'), 0)],
                    'res_task_1': ([1], [(D('0.00008'), 0, D('0.0003'), 0)]),
                    'res_task_2': ([1], [(D('0.0002'), 0, D('0.0003'), 0)]),
                },
                id='rounding',
            ),
        ],
    )
    def test_multitasks(self, session, client, params):
        main_order, (ch_order,) = create_orders(client, force_log_tariff=True)
        consume_order(main_order, [10])
        consume_order(ch_order, [2])

        mk_shipment(ch_order, 2)
        ch_order.completion_consumed_money = 2

        base_task = ob.LogTariffTaskBuilder.construct(session)
        qty1, qty2 = params['task_qtys']
        task_1 = create_order_task(ch_order, qty1, task=base_task, tariff_dt=PAST, group_dt=PAST)
        task_2 = create_order_task(ch_order, qty2, task=base_task, tariff_dt=TODAY, group_dt=TODAY)
        ProcessLogTariff(ch_order, base_task.id, obj_lock=True, shared_lock=True).do()

        assert main_order.completion_qty == 0
        assert ch_order.completion_qty == params['completion_qty']

        main_consume_qty, ch_consume_qty = params['consume_qty']
        assert main_order.consume_qty == main_consume_qty
        assert ch_order.consume_qty == ch_consume_qty

        assert_consumes(ch_order, params['consumes'])

        consumes = ch_order.consumes
        assert_task_consumes(task_1, [((consumes[co_idx].id,) + t_res) for co_idx, t_res in zip(*params['res_task_1'])])
        assert_task_consumes(task_2, [((consumes[co_idx].id,) + t_res) for co_idx, t_res in zip(*params['res_task_2'])])
