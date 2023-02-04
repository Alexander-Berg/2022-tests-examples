# -*- coding: utf-8 -*-

import datetime
import time
import json
import contextlib
import decimal
import uuid
from functools import partial

import pytest
import mock
import hamcrest

from balance import mapper
from balance import muzzle_util as ut
from balance import exc
from balance.constants import (
    OrderLogTariffState,
    ExportState,
    ServiceId,
    CONVERT_TYPE_MODIFY,
    DIRECT_PRODUCT_RUB_ID,
    DIRECT_PRODUCT_ID,
    NirvanaProcessingAttemptType,
    NirvanaProcessingTaskState,
    UAChildType,
)
from balance.actions.nirvana.operations.log_processing.log_tariff import (
    process,
)

from tests import object_builder as ob
from tests.tutils import has_exact_entries
from tests.balance_tests.invoices.invoice_common import create_invoice

from tests.balance_tests.nirvana.log_processing.common import (
    add_block_input as common_add_block_input,
)

# TODO: тесты на парсинг интервалов

ON_DT = datetime.datetime.now().replace(microsecond=0)
TODAY = datetime.datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
YESTERDAY = datetime.datetime.now().replace(hour=0, minute=0, second=0, microsecond=0) - datetime.timedelta(days=1)


@pytest.fixture(autouse=True)
def mock_get_log_interval_from_metadata():
    with mock.patch('balance.utils.log_tariff.get_log_interval_from_metadata'):
        yield


@pytest.fixture
def nirvana_block(session):
    block = ob.NirvanaBlockBuilder.construct(session, operation='log_tariff', request={})
    with mock.patch.object(block, 'upload'), mock.patch.object(block, 'download'):
        yield block


@pytest.fixture(autouse=True)
def mock_batch_processor():
    patch_path = 'balance.util.ParallelBatchProcessor.process_batches'
    calls = []

    def _process_batches(_s, func, batches, **kw):
        calls.append(batches)
        return map(partial(func, **kw), batches)

    with mock.patch(patch_path, _process_batches):
        yield calls


@pytest.fixture(autouse=True)
def mock_skip_missing_orders(session):
    session.config.__dict__['SKIP_UNTARIFFING_MISSING_ORDERS'] = 0


@contextlib.contextmanager
def mock_intervals_comparison(cmp_res):
    patch_path = 'balance.actions.nirvana.operations.log_processing.log_tariff.compare_intervals'
    with mock.patch(patch_path, side_effect=lambda p, c: cmp_res):
        yield


@contextlib.contextmanager
def mock_patch_type(type_id):
    with mock.patch('balance.actions.nirvana.operations.log_processing.log_tariff.Context.type', type_id):
        yield


@pytest.fixture
def task_type(session):
    res = ob.LogTariffTypeBuilder.construct(session)
    with mock_patch_type(res.id):
        yield res


@contextlib.contextmanager
def mock_new_metadata(metadata):
    get_input_metadata_patcher = mock.patch(
        'balance.actions.nirvana.operations.log_processing.log_tariff.Context.metadata',
        metadata
    )
    with get_input_metadata_patcher:
        yield


def add_block_input(nirvana_block, values):
    common_add_block_input(
        nirvana_block,
        log_tariff_meta=mock.Mock(),
        orders=json.dumps(values)
    )


def test_new(session, nirvana_block, task_type):
    prev_task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=NirvanaProcessingTaskState.FINISHED,
    )

    invoice = create_invoice(session)
    invoice.turn_on_rows()
    consume = invoice.consumes[0]
    prev_order = ob.LogTariffOrderBuilder.construct(
        session,
        task=prev_task,
        order=consume.order,
        tariff_dt=datetime.datetime.now(),
        completion_qty_delta=666
    )
    ob.LogTariffConsumeBuilder.construct(
        session,
        tariff_order=prev_order,
        consume=consume,
        qty=1,
        sum=1,
    )

    orders_qty_states = [
        (123.456, 'abc'),
        (666.666, 'cba'),
        (6661244614.835437, 'acb'),
    ]
    orders = [
        ob.OrderBuilder.construct(session, _is_log_tariff=OrderLogTariffState.MIGRATED)
        for _ in orders_qty_states
    ]

    add_block_input(
        nirvana_block,
        [
            {
                'ServiceID': order.service_id,
                'EffectiveServiceOrderID': order.service_order_id,
                'tariff_dt': time.mktime((ON_DT + datetime.timedelta(seconds=idx)).timetuple()),
                'completion_qty_delta': qty,
                'state': state,
                'CurrencyID': 100 + idx,
                'group_dt': time.mktime(TODAY.timetuple()),
            }
            for idx, (order, (qty, state)) in enumerate(zip(orders, orders_qty_states))
        ]
    )

    metadata = 'mock_interval_new'
    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.NEW):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert nirvana_block.upload.call_args_list == []

    hamcrest.assert_that(
        prev_task,
        hamcrest.has_properties(
            state=NirvanaProcessingTaskState.ARCHIVED,
            orders=[]
        )
    )

    new_task = (
        session.query(mapper.LogTariffTask)
            .filter_by(type_id=task_type.id,
                       state=NirvanaProcessingTaskState.IN_PROGRESS)
            .one()
    )
    assert new_task.metadata == metadata

    orders_matchers = [
        hamcrest.has_properties(
            service_id=order.service_id,
            service_order_id=order.service_order_id,
            tariff_dt=ON_DT + datetime.timedelta(seconds=idx),
            completion_qty_delta=ut.float2decimal6(qty),
            state=state,
            currency_id=100 + idx,
            group_dt=TODAY
        )
        for idx, (order, (qty, state)) in enumerate(zip(orders, orders_qty_states))
    ]
    hamcrest.assert_that(
        new_task.orders,
        hamcrest.contains_inanyorder(*orders_matchers)
    )
    hamcrest.assert_that(
        orders,
        hamcrest.only_contains(
            hamcrest.has_properties(
                exports=hamcrest.has_entries(
                    PROCESS_COMPLETION=hamcrest.has_properties(
                        state=ExportState.enqueued,
                        input={'log_tariff_task_id': new_task.id}
                    )
                )
            )
        )
    )


def test_new_multiple_currencies(session, nirvana_block, task_type):
    order = ob.OrderBuilder.construct(session, _is_log_tariff=OrderLogTariffState.MIGRATED)

    add_block_input(
        nirvana_block,
        [
            {
                'ServiceID': order.service_id,
                'EffectiveServiceOrderID': order.service_order_id,
                'tariff_dt': time.mktime(ON_DT.timetuple()),
                'completion_qty_delta': 6.66,
                'CurrencyID': currency_id,
                'state': 'state',
                'group_dt': time.mktime(TODAY.timetuple())
            }
            for currency_id in [0, 666]
        ]
    )

    with mock_new_metadata('mock_interval_new'), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.INVALID):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert nirvana_block.upload.call_args_list == []

    new_task = (
        session.query(mapper.LogTariffTask)
            .filter_by(type_id=task_type.id,
                       state=NirvanaProcessingTaskState.IN_PROGRESS)
            .one()
    )

    order_matchers = [
        hamcrest.has_properties(
            service_id=order.service_id,
            service_order_id=order.service_order_id,
            completion_qty_delta=decimal.Decimal('6.66'),
            currency_id=currency_id,
            res_tariff_dt=None,
            res_state=None,
            group_dt=TODAY
        )
        for currency_id in [0, 666]
    ]
    hamcrest.assert_that(
        new_task.orders,
        hamcrest.contains_inanyorder(*order_matchers)
    )

    hamcrest.assert_that(
        order,
        hamcrest.has_properties(
            exports=hamcrest.has_entries(
                PROCESS_COMPLETION=hamcrest.has_properties(
                    state=ExportState.enqueued,
                    input={'log_tariff_task_id': new_task.id}
                )
            )
        )
    )


def test_new_multiple_group_dt(session, nirvana_block, task_type):
    order = ob.OrderBuilder.construct(session, _is_log_tariff=OrderLogTariffState.MIGRATED)

    add_block_input(
        nirvana_block,
        [
            {
                'ServiceID': order.service_id,
                'EffectiveServiceOrderID': order.service_order_id,
                'tariff_dt': time.mktime(ON_DT.timetuple()),
                'completion_qty_delta': 6.66,
                'CurrencyID': 1,
                'state': 'state',
                'group_dt': group_dt
            }
            for group_dt in [time.mktime(YESTERDAY.timetuple()), time.mktime(TODAY.timetuple())]
        ]
    )

    with mock_new_metadata('mock_interval_new'), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.INVALID):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert nirvana_block.upload.call_args_list == []

    new_task = (
        session.query(mapper.LogTariffTask)
            .filter_by(type_id=task_type.id,
                       state=NirvanaProcessingTaskState.IN_PROGRESS)
            .one()
    )

    order_matchers = [
        hamcrest.has_properties(
            service_id=order.service_id,
            service_order_id=order.service_order_id,
            completion_qty_delta=decimal.Decimal('6.66'),
            currency_id=1,
            res_tariff_dt=None,
            res_state=None,
            group_dt=group_dt
        )
        for group_dt in [YESTERDAY, TODAY]
    ]
    hamcrest.assert_that(
        new_task.orders,
        hamcrest.contains_inanyorder(*order_matchers)
    )

    hamcrest.assert_that(
        order,
        hamcrest.has_properties(
            exports=hamcrest.has_entries(
                PROCESS_COMPLETION=hamcrest.has_properties(
                    state=ExportState.enqueued,
                    input={'log_tariff_task_id': new_task.id}
                )
            )
        )
    )


def test_new_skipped_orders(session, nirvana_block, task_type):
    orders = [
        ob.OrderBuilder.construct(session, _is_log_tariff=OrderLogTariffState.MIGRATED),
        ob.OrderBuilder.construct(session, _is_log_tariff=None),
        ut.Struct(
            service_id=ServiceId.DIRECT,
            service_order_id=ob.OrderBuilder().generate_unique_id(session, 'service_order_id')
        ),
        ob.OrderBuilder.construct(session, _is_log_tariff=None, child_ua_type=UAChildType.LOG_TARIFF),
    ]
    o1, o2, o3, o4 = orders
    order_currencies = [
        (o1, 666),
        (o1, 667),
        (o2, 665),
        (o2, 666),
        (o3, 668),
        (o4, 666),
    ]

    add_block_input(
        nirvana_block,
        [
            {
                'ServiceID': o.service_id,
                'EffectiveServiceOrderID': o.service_order_id,
                'tariff_dt': time.mktime(ON_DT.timetuple()),
                'completion_qty_delta': 6.66,
                'state': 'state',
                'CurrencyID': currency_id,
                'group_dt': time.mktime(TODAY.timetuple())
            }
            for o, currency_id in order_currencies
        ]
    )

    metadata = 'mock_interval_new'
    with mock_new_metadata(metadata), \
            mock_intervals_comparison(NirvanaProcessingAttemptType.INVALID):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert nirvana_block.upload.call_args_list == []

    new_task = (
        session.query(mapper.LogTariffTask)
            .filter_by(type_id=task_type.id,
                       state=NirvanaProcessingTaskState.IN_PROGRESS)
            .one()
    )
    order_matchers = [
        hamcrest.has_properties(
            service_id=o.service_id,
            service_order_id=o.service_order_id,
            completion_qty_delta=decimal.Decimal('6.66'),
            currency_id=currency_id,
            res_tariff_dt=ON_DT if is_processed else None,
            res_state='state' if is_processed else None,
            skip=1 if is_skipped else None,
            group_dt=TODAY,
        )
        for (o, currency_id), is_processed, is_skipped in zip(order_currencies, [0, 0, 1, 1, 1, 0], [0, 0, 1, 1, 0, 0])
    ]
    hamcrest.assert_that(
        new_task,
        hamcrest.has_properties(
            metadata=metadata,
            orders=hamcrest.contains_inanyorder(*order_matchers)
        )
    )

    hamcrest.assert_that(
        o1,
        hamcrest.has_properties(
            exports=hamcrest.has_entries(
                PROCESS_COMPLETION=hamcrest.has_properties(
                    state=ExportState.enqueued,
                    input={'log_tariff_task_id': new_task.id}
                )
            )
        )
    )
    hamcrest.assert_that(
        o4,
        hamcrest.has_properties(
            exports=hamcrest.has_entries(
                PROCESS_COMPLETION=hamcrest.has_properties(
                    state=ExportState.enqueued,
                    input={'log_tariff_task_id': new_task.id}
                )
            )
        )
    )

    assert 'PROCESS_COMPLETION' not in o2.exports


def test_migration_in_process(session, nirvana_block, task_type):
    """Для заказов, упавших при миграции, надо сохранить открутки в нетарифицированном.
    """
    o1 = ob.OrderBuilder.construct(session, _is_log_tariff=OrderLogTariffState.INIT)
    o2 = ob.OrderBuilder.construct(session, _is_log_tariff=OrderLogTariffState.MIGRATED)
    add_block_input(
        nirvana_block,
        [
            {
                'ServiceID': o.service_id,
                'EffectiveServiceOrderID': o.service_order_id,
                'tariff_dt': time.mktime(ON_DT.timetuple()),
                'completion_qty_delta': 6.66,
                'state': 'state',
                'CurrencyID': 666,
                'group_dt': time.mktime(TODAY.timetuple())
            }
            for o in [o1, o2]
        ]
    )

    metadata = 'mock_interval_new'
    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.INVALID):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert nirvana_block.upload.call_args_list == []

    new_task = (
        session.query(mapper.LogTariffTask)
            .filter_by(type_id=task_type.id,
                       state=NirvanaProcessingTaskState.IN_PROGRESS)
            .one()
    )
    order_matchers = [
        hamcrest.has_properties(
            service_id=o.service_id,
            service_order_id=o.service_order_id,
            completion_qty_delta=decimal.Decimal('6.66'),
            currency_id=666,
            res_tariff_dt=None,
            res_state=None,
            skip=None,
            group_dt=TODAY,
        )
        for o in [o1, o2]
    ]
    hamcrest.assert_that(
        new_task,
        hamcrest.has_properties(
            metadata=metadata,
            orders=hamcrest.contains_inanyorder(*order_matchers)
        )
    )
    for o in [o1, o2]:
        hamcrest.assert_that(
            o,
            hamcrest.has_properties(
                exports=hamcrest.has_entries(
                    PROCESS_COMPLETION=hamcrest.has_properties(
                        state=ExportState.enqueued,
                        input={'log_tariff_task_id': new_task.id}
                    )
                )
            )
        )


def test_new_first_task(session, nirvana_block, task_type):
    order = ob.OrderBuilder.construct(session, _is_log_tariff=OrderLogTariffState.MIGRATED)

    add_block_input(
        nirvana_block,
        [
            {
                'ServiceID': order.service_id,
                'EffectiveServiceOrderID': order.service_order_id,
                'tariff_dt': time.mktime(ON_DT.timetuple()),
                'completion_qty_delta': 6.66,
                'CurrencyID': 666,
                'state': 'state',
                'group_dt': time.mktime(TODAY.timetuple())
            }
        ]
    )

    metadata = 'mock_interval_new'
    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.INVALID):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert nirvana_block.upload.call_args_list == []

    new_task = (
        session.query(mapper.LogTariffTask)
            .filter_by(type_id=task_type.id,
                       state=NirvanaProcessingTaskState.IN_PROGRESS)
            .one()
    )
    hamcrest.assert_that(
        new_task,
        hamcrest.has_properties(
            metadata=metadata,
            orders=hamcrest.contains(
                hamcrest.has_properties(
                    order=order,
                    completion_qty_delta=decimal.Decimal('6.66')
                )
            )
        )
    )


@pytest.mark.parametrize(
    'task_state',
    [
        NirvanaProcessingTaskState.IN_PROGRESS,
        NirvanaProcessingTaskState.FINISHED,
    ]
)
def test_finished(session, nirvana_block, task_type, task_state):
    orders = []
    for idx in range(3):
        order = ob.OrderBuilder.construct(session, product_id=DIRECT_PRODUCT_RUB_ID)
        orders.append(order)
        for c_idx in range(idx + 1):
            invoice = create_invoice(session, client=order.client, orders=[(order, 666)])
            invoice.turn_on_rows()

    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=task_state,
    )
    for idx, order in enumerate(orders):
        tariff_order = ob.LogTariffOrderBuilder.construct(
            session,
            task=task,
            order=order,
            tariff_dt=ON_DT,
            completion_qty_delta=idx,
            state='state',
            currency_id=idx,
            res_tariff_dt=ON_DT + datetime.timedelta(seconds=idx),
            res_state='state%s' % idx,
            group_dt=TODAY
        )
        for c_idx, consume in enumerate(order.consumes):
            ob.LogTariffConsumeBuilder.construct(
                session,
                tariff_order=tariff_order,
                consume=consume,
                qty=c_idx,
                sum=c_idx,
                consume_qty=42 + c_idx,
                consume_sum=42 + c_idx
            )

    session.expire_all()

    metadata = 'mock_interval_new'
    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_finished()
    assert task.state == NirvanaProcessingTaskState.FINISHED
    hamcrest.assert_that(
        nirvana_block.upload.call_args_list,
        hamcrest.contains(
            hamcrest.has_properties(
                args=hamcrest.contains(
                    'orders',
                    hamcrest.anything()
                ),
                kwargs={}
            )
        )
    )

    req_res = {
        'log_tariff_meta': metadata,
        'orders': [
            {
                'ServiceID': o.service_id,
                'EffectiveServiceOrderID': o.service_order_id,
                'state': 'state%s' % idx,
                'tariff_dt': time.mktime((ON_DT + datetime.timedelta(seconds=idx)).timetuple()),
                'completion_qty_delta': idx,
                'CurrencyID': idx,
                'group_dt': time.mktime(TODAY.timetuple()),
                'consumes': [
                    {
                        'id': co.id,
                        'k_qty': 42 + c_idx,
                        'k_sum': 42 + c_idx,
                        'qty': c_idx,
                        'sum': c_idx,
                    }
                    for c_idx, co in sorted(enumerate(o.consumes), key=lambda e: e[1].id)
                ]
            }
            for idx, o in sorted(enumerate(orders), key=lambda e: e[1].service_order_id)
        ],
        'errors': [],
    }
    res = json.loads(nirvana_block.upload.call_args.args[1])
    res['orders'].sort(key=lambda o: o['EffectiveServiceOrderID'])
    for o in res['orders']:
        o['consumes'].sort(key=lambda c: c['id'])
    assert res == req_res


@pytest.mark.parametrize('is_migrated', [True, False])
def test_finished_multicurrency_qty(session, nirvana_block, task_type, is_migrated):
    order = ob.OrderBuilder.construct(session, product_id=DIRECT_PRODUCT_ID)
    if is_migrated:
        order.client.set_currency(ServiceId.DIRECT, 'RUB', ut.trunc_date(ON_DT), CONVERT_TYPE_MODIFY, force=1)

    invoice = create_invoice(session, client=order.client, orders=[(order, decimal.Decimal('1.5'))])
    invoice.turn_on_rows()
    consume, = order.consumes

    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=NirvanaProcessingTaskState.FINISHED,
    )
    tariff_order = ob.LogTariffOrderBuilder.construct(
        session,
        task=task,
        order=order,
        tariff_dt=ON_DT,
        completion_qty_delta=666,
        state='state',
        currency_id=0,
        res_tariff_dt=ON_DT,
        res_state='res_state',
        group_dt=TODAY
    )
    ob.LogTariffConsumeBuilder.construct(
        session,
        tariff_order=tariff_order,
        consume=consume,
        qty=30,
        sum=30,
        consume_qty=45,
        consume_sum=45
    )

    session.expire_all()

    metadata = 'mock_interval_new'
    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_finished()
    assert task.state == NirvanaProcessingTaskState.FINISHED
    req_res = {
        'log_tariff_meta': metadata,
        'orders': [
            {
                'ServiceID': order.service_id,
                'EffectiveServiceOrderID': order.service_order_id,
                'state': 'res_state',
                'tariff_dt': time.mktime(ON_DT.timetuple()),
                'completion_qty_delta': 666,
                'CurrencyID': 0,
                'group_dt': time.mktime(TODAY.timetuple()),
                'consumes': [
                    {
                        'id': consume.id,
                        'k_qty': 45,
                        'k_sum': 45,
                        'qty': 30,
                        'sum': 30,
                    }
                ]
            }
        ],
        'errors': [],
    }
    assert json.loads(nirvana_block.upload.call_args.args[1]) == req_res


def test_finished_multiple_currencies(session, nirvana_block, task_type):
    order = ob.OrderBuilder.construct(session, product_id=DIRECT_PRODUCT_RUB_ID)

    for idx in range(2):
        invoice = create_invoice(session, client=order.client, orders=[(order, decimal.Decimal('1.5'))])
        invoice.turn_on_rows()

    co1, co2 = order.consumes

    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=NirvanaProcessingTaskState.FINISHED,
    )
    tariff_order1 = ob.LogTariffOrderBuilder.construct(
        session,
        task=task,
        order=order,
        tariff_dt=ON_DT,
        completion_qty_delta=666,
        state='state',
        currency_id=666,
        res_tariff_dt=ON_DT,
        res_state='res_state',
        group_dt=TODAY
    )
    ob.LogTariffConsumeBuilder.construct(
        session,
        tariff_order=tariff_order1,
        consume=co1,
        qty=30,
        sum=30,
        consume_qty=decimal.Decimal('1.5'),
        consume_sum=decimal.Decimal('1.5'),
    )
    tariff_order2 = ob.LogTariffOrderBuilder.construct(
        session,
        task=task,
        order=order,
        tariff_dt=ON_DT,
        completion_qty_delta=666,
        state='state',
        currency_id=667,
        res_tariff_dt=ON_DT,
        res_state='res_state',
        group_dt=TODAY
    )
    for idx, co in enumerate([co1, co2]):
        ob.LogTariffConsumeBuilder.construct(
            session,
            tariff_order=tariff_order2,
            consume=co,
            qty=idx + 40,
            sum=idx + 40,
            consume_qty=decimal.Decimal('1.5'),
            consume_sum=decimal.Decimal('1.5'),
        )

    session.expire_all()

    metadata = 'mock_interval_new'
    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_finished()
    assert task.state == NirvanaProcessingTaskState.FINISHED
    req_res = {
        'log_tariff_meta': metadata,
        'orders': [
            {
                'ServiceID': order.service_id,
                'EffectiveServiceOrderID': order.service_order_id,
                'state': 'res_state',
                'tariff_dt': time.mktime(ON_DT.timetuple()),
                'completion_qty_delta': 666,
                'CurrencyID': currency_id,
                'group_dt': time.mktime(TODAY.timetuple()),
                'consumes': [
                    {
                        'id': co.id,
                        'k_qty': 1.5,
                        'k_sum': 1.5,
                        'qty': idx + base_qty,
                        'sum': idx + base_qty,
                    }
                    for idx, co in enumerate(sorted(consumes, key=lambda co: co.id))
                ]
            }
            for currency_id, base_qty, consumes in [(666, 30, [co1]), (667, 40, [co1, co2])]
        ],
        'errors': [],
    }

    res = json.loads(nirvana_block.upload.call_args.args[1])
    for o in res['orders']:
        o['consumes'].sort(key=lambda c: c['id'])

    assert res == req_res


def test_finished_no_consumes(session, nirvana_block, task_type):
    order = ob.OrderBuilder.construct(session, product_id=DIRECT_PRODUCT_RUB_ID)

    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=NirvanaProcessingTaskState.FINISHED,
    )

    ob.LogTariffOrderBuilder.construct(
        session,
        task=task,
        order=order,
        tariff_dt=ON_DT,
        completion_qty_delta=666,
        state='state',
        currency_id=None,
        res_tariff_dt=ON_DT,
        res_state='state666',
        group_dt=TODAY
    )

    session.expire_all()

    metadata = 'mock_interval_new'
    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_finished()
    assert task.state == NirvanaProcessingTaskState.FINISHED
    hamcrest.assert_that(
        nirvana_block.upload.call_args_list,
        hamcrest.contains(
            hamcrest.has_properties(
                args=hamcrest.contains(
                    'orders',
                    hamcrest.anything()
                ),
                kwargs={}
            )
        )
    )

    req_res = {
        'log_tariff_meta': metadata,
        'orders': [
            {
                'ServiceID': order.service_id,
                'EffectiveServiceOrderID': order.service_order_id,
                'state': 'state666',
                'tariff_dt': time.mktime(ON_DT.timetuple()),
                'completion_qty_delta': 666,
                'CurrencyID': None,
                'group_dt': time.mktime(TODAY.timetuple()),
                'consumes': []
            }
        ],
        'errors': [],
    }
    res = json.loads(nirvana_block.upload.call_args.args[1])
    assert res == req_res


def test_in_progress(session, nirvana_block, task_type):
    order1, order2 = [
        ob.OrderBuilder.construct(session)
        for _ in range(2)
    ]

    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=NirvanaProcessingTaskState.IN_PROGRESS,
    )

    tariff_order_1, tariff_order_2 = [
        ob.LogTariffOrderBuilder.construct(
            session,
            task=task,
            order=order,
            tariff_dt=ON_DT,
            completion_qty_delta=1,
            state='state',
            group_dt=TODAY
        )
        for order in [order1, order2]
    ]

    invoice = create_invoice(session, client=order1.client, orders=[(order1, 666)])
    invoice.turn_on_rows()
    tariff_order_1.res_state = 'state'
    tariff_order_1.res_tariff_dt = ON_DT
    ob.LogTariffConsumeBuilder.construct(
        session,
        tariff_order=tariff_order_1,
        consume=order1.consumes[0],
        qty=666,
        sum=666
    )

    session.expire_all()

    with mock_new_metadata('mock_interval_new'), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert task.state == NirvanaProcessingTaskState.IN_PROGRESS
    assert nirvana_block.upload.call_args_list == []


def test_in_progress_multiple_currencies(session, nirvana_block, task_type):
    order = ob.OrderBuilder.construct(session)

    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=NirvanaProcessingTaskState.IN_PROGRESS,
    )

    tariff_order_1, tariff_order_2 = [
        ob.LogTariffOrderBuilder.construct(
            session,
            task=task,
            order=order,
            tariff_dt=ON_DT,
            completion_qty_delta=1,
            state='state',
            currency_id=currency_id,
            group_dt=TODAY
        )
        for currency_id in [665, 666]
    ]

    invoice = create_invoice(session, client=order.client, orders=[(order, 666)])
    invoice.turn_on_rows()
    consume, = order.consumes

    tariff_order_1.res_state = 'state'
    tariff_order_1.res_tariff_dt = ON_DT
    ob.LogTariffConsumeBuilder.construct(
        session,
        tariff_order=tariff_order_1,
        consume=consume,
        qty=666,
        sum=666
    )

    session.expire_all()

    with mock_new_metadata('mock_interval_new'), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert task.state == NirvanaProcessingTaskState.IN_PROGRESS
    assert nirvana_block.upload.call_args_list == []


def test_enqueue_initialized(session, nirvana_block, task_type):
    orders = [
        ob.OrderBuilder.construct(
            session,
            product_id=DIRECT_PRODUCT_RUB_ID,
            _is_log_tariff=OrderLogTariffState.MIGRATED
        )
        for _ in range(3)
    ]

    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=NirvanaProcessingTaskState.NEW,
    )
    log_tariff_orders = [
        ob.LogTariffOrderBuilder.construct(
            session,
            task=task,
            order=order,
            tariff_dt=ON_DT,
            completion_qty_delta=666,
            state='state',
            group_dt=TODAY
        )
        for order in orders
    ]

    session.expire_all()

    add_block_input(
        nirvana_block,
        [
            {
                'ServiceID': item.service_id,
                'EffectiveServiceOrderID': item.service_order_id,
            }
            for item in log_tariff_orders
        ]
    )

    with mock_new_metadata('mock_interval_new'), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert task.state == NirvanaProcessingTaskState.IN_PROGRESS

    hamcrest.assert_that(
        orders,
        hamcrest.only_contains(
            hamcrest.has_properties(
                exports=hamcrest.has_entries(
                    PROCESS_COMPLETION=hamcrest.has_properties(
                        state=ExportState.enqueued,
                        input={'log_tariff_task_id': task.id}
                    )
                )
            )
        )
    )


@pytest.mark.parametrize(
    'prev_state, cmp_res, exc_cls, exc_message',
    [
        pytest.param(
            NirvanaProcessingTaskState.ARCHIVED,
            NirvanaProcessingAttemptType.PREV,
            exc.LOG_TASK_STATE_INVALID,
            'archived task'
        ),
        pytest.param(
            NirvanaProcessingTaskState.FINISHED,
            NirvanaProcessingAttemptType.INVALID,
            exc.LOG_TASK_ATTEMPT_INVALID,
            'broken sequence of tasks'
        ),
        pytest.param(
            NirvanaProcessingTaskState.IN_PROGRESS,
            NirvanaProcessingAttemptType.NEW,
            exc.LOG_TASK_STATE_INVALID,
            'previous task is not finished'
        ),
    ]
)
def test_mismatched_states(session, nirvana_block, task_type, prev_state, cmp_res, exc_cls, exc_message):
    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=prev_state,
    )

    with pytest.raises(exc_cls) as exc_info:
        with mock_new_metadata('mock_interval_new'), \
             mock_intervals_comparison(cmp_res):
            process(nirvana_block)

    exc_message = '%s: %s' % (task.id, exc_message)
    assert exc_message in exc_info.value.msg


def test_locked_type(app, session, nirvana_block):
    real_session = app.real_new_session()

    type_id = uuid.uuid5(uuid.NAMESPACE_OID, 'test_locked_type').hex
    with real_session.begin():
        task_type = real_session.query(mapper.LogTariffType).get(type_id)
        if task_type is None:
            task_type = ob.LogTariffTypeBuilder.construct(real_session, id=type_id)

    with real_session.begin(), mock_patch_type(task_type.id), mock_new_metadata('mock_interval_new'):
        task_type.lock()

        with pytest.raises(exc.LOG_TASK_IS_LOCKED):
            process(nirvana_block)


def test_no_orders(session, nirvana_block, task_type):
    add_block_input(nirvana_block, [])

    metadata = 'mock_interval_new'
    with mock_new_metadata(metadata), \
        mock_intervals_comparison(NirvanaProcessingAttemptType.NEW):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()

    task = (
        session.query(mapper.LogTariffTask)
            .filter_by(type_id=task_type.id,
                       state=NirvanaProcessingTaskState.IN_PROGRESS)
            .one()
    )
    assert task.metadata == metadata
    assert len(task.orders) == 0, task.orders

    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_finished()
    assert task.state == NirvanaProcessingTaskState.FINISHED

    hamcrest.assert_that(
        nirvana_block.upload.call_args_list,
        hamcrest.contains(
            hamcrest.has_properties(
                args=hamcrest.contains(
                    'orders',
                    hamcrest.anything()
                ),
                kwargs={}
            )
        )
    )

    res = json.loads(nirvana_block.upload.call_args.args[1])
    req_res = {
        'log_tariff_meta': metadata,
        'orders': [],
        'errors': [],
    }
    assert res == req_res


def test_new_multiple_services(session, nirvana_block, task_type, mock_batch_processor):
    session.config.__dict__['LOG_TARIFF_ENQUEUE_BATCH_SIZE'] = 2

    orders_qty_states = [
        (ServiceId.DIRECT, 123.456, 'abc'),
        (ServiceId.MARKET, 666.666, 'cba'),
        (ServiceId.DIRECT, 321.456, 'bac'),
        (ServiceId.DIRECT, 456.456, 'bca'),
    ]
    orders = [
        ob.OrderBuilder.construct(session, service_id=s_id, _is_log_tariff=OrderLogTariffState.MIGRATED)
        for s_id, _, _ in orders_qty_states
    ]
    sorted_orders = sorted(orders, key=lambda o: (o.service_id, o.service_order_id))

    add_block_input(
        nirvana_block,
        [
            {
                'ServiceID': order.service_id,
                'EffectiveServiceOrderID': order.service_order_id,
                'tariff_dt': time.mktime((ON_DT + datetime.timedelta(seconds=idx)).timetuple()),
                'completion_qty_delta': qty,
                'state': state,
                'CurrencyID': 100 + idx,
                'group_dt': time.mktime(TODAY.timetuple())
            }
            for idx, (order, (_, qty, state)) in enumerate(zip(orders, orders_qty_states))
        ]
    )

    metadata = 'mock_interval_new'
    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.NEW):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert nirvana_block.upload.call_args_list == []
    hamcrest.assert_that(
        mock_batch_processor,
        hamcrest.contains(
            hamcrest.contains(
                hamcrest.has_entries({'service_id': ServiceId.DIRECT, 'service_order_id': hamcrest.contains(sorted_orders[0].service_order_id, sorted_orders[1].service_order_id)}),
                hamcrest.has_entries({'service_id': ServiceId.DIRECT, 'service_order_id': hamcrest.contains(sorted_orders[2].service_order_id, sorted_orders[2].service_order_id)}),
                hamcrest.has_entries({'service_id': ServiceId.MARKET, 'service_order_id': hamcrest.contains(sorted_orders[3].service_order_id, sorted_orders[3].service_order_id)}),
            ),
        ),
    )

    new_task = (
        session.query(mapper.LogTariffTask)
            .filter_by(type_id=task_type.id,
                       state=NirvanaProcessingTaskState.IN_PROGRESS)
            .one()
    )
    assert new_task.metadata == metadata

    orders_matchers = [
        hamcrest.has_properties(
            service_id=order.service_id,
            service_order_id=order.service_order_id,
            tariff_dt=ON_DT + datetime.timedelta(seconds=idx),
            completion_qty_delta=ut.float2decimal6(qty),
            state=state,
            currency_id=100 + idx,
            group_dt=TODAY
        )
        for idx, (order, (_, qty, state)) in enumerate(zip(orders, orders_qty_states))
    ]
    hamcrest.assert_that(
        new_task.orders,
        hamcrest.contains_inanyorder(*orders_matchers)
    )
    hamcrest.assert_that(
        orders,
        hamcrest.only_contains(
            hamcrest.has_properties(
                exports=hamcrest.has_entries(
                    PROCESS_COMPLETION=hamcrest.has_properties(
                        state=ExportState.enqueued,
                        input={'log_tariff_task_id': new_task.id}
                    )
                )
            )
        )
    )


def test_errors(session, nirvana_block, task_type):
    orders = []
    for idx in range(3):
        order = ob.OrderBuilder.construct(session, product_id=DIRECT_PRODUCT_RUB_ID)
        orders.append(order)

    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=NirvanaProcessingTaskState.FINISHED,
    )
    for idx, order in enumerate(orders):
        tariff_order = ob.LogTariffOrderBuilder.construct(
            session,
            task=task,
            order=order,
            tariff_dt=ON_DT,
            completion_qty_delta=idx,
            state='state',
            currency_id=idx,
            res_tariff_dt=ON_DT + datetime.timedelta(seconds=idx),
            res_state='state%s' % idx,
            error='failed %s' % idx,
            group_dt=TODAY
        )
        for c_idx, consume in enumerate(order.consumes):
            ob.LogTariffConsumeBuilder.construct(
                session,
                tariff_order=tariff_order,
                consume=consume,
                qty=c_idx,
                sum=c_idx,
                consume_qty=42 + c_idx,
                consume_sum=42 + c_idx
            )

    session.expire_all()

    metadata = {'run_id': 'test_666'}
    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_finished()
    assert task.state == NirvanaProcessingTaskState.FINISHED

    res, res_data = nirvana_block.upload.call_args.args
    assert res == 'orders'
    res_data = json.loads(res_data)

    orders = sorted(enumerate(orders), key=lambda e: e[1].service_order_id)
    req_res = {
        'log_tariff_meta': has_exact_entries(metadata),
        'orders': hamcrest.contains(*[
            has_exact_entries({
                'ServiceID': o.service_id,
                'EffectiveServiceOrderID': o.service_order_id,
                'state': 'state%s' % idx,
                'tariff_dt': time.mktime((ON_DT + datetime.timedelta(seconds=idx)).timetuple()),
                'completion_qty_delta': idx,
                'CurrencyID': idx,
                'group_dt': time.mktime(TODAY.timetuple()),
                'consumes': hamcrest.contains(*[
                    has_exact_entries({
                        'id': co.id,
                        'k_qty': 42 + c_idx,
                        'k_sum': 42 + c_idx,
                        'qty': c_idx,
                        'sum': c_idx,
                    })
                    for c_idx, co in sorted(enumerate(o.consumes), key=lambda e: e[1].id)
                ]),
            })
            for idx, o in orders
        ]),
        'errors': hamcrest.contains(*[
            has_exact_entries({
                'ServiceID': o.service_id,
                'EffectiveServiceOrderID': o.service_order_id,
                'error': 'failed %s' % idx,
            })
            for idx, o in orders
        ]),
    }
    res_data['orders'].sort(key=lambda o: o['EffectiveServiceOrderID'])
    res_data['errors'].sort(key=lambda o: o['EffectiveServiceOrderID'])
    for o in res_data['orders']:
        o['consumes'].sort(key=lambda c: c['id'])
    hamcrest.assert_that(
        res_data,
        hamcrest.has_entries(req_res),
    )


def test_result_skip(session, nirvana_block, task_type):
    orders = [
        ob.OrderBuilder.construct(session, product_id=DIRECT_PRODUCT_RUB_ID)
        for _ in range(3)
    ]
    order_states = zip(orders, [0, 1, None])

    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=NirvanaProcessingTaskState.FINISHED,
    )
    for order, skip in order_states:
        ob.LogTariffOrderBuilder.construct(
            session,
            task=task,
            order=order,
            tariff_dt=ON_DT - datetime.timedelta(1),
            completion_qty_delta=1,
            state='state',
            currency_id=123,
            res_tariff_dt=ON_DT,
            res_state='res_state',
            skip=skip,
            group_dt=TODAY
        )

    metadata = {'run_id': 'test_666'}
    with mock_new_metadata(metadata), \
         mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_finished()
    assert task.state == NirvanaProcessingTaskState.FINISHED

    res, res_data = nirvana_block.upload.call_args.args
    assert res == 'orders'
    res_data = json.loads(res_data)

    req_res = {
        'log_tariff_meta': has_exact_entries(metadata),
        'orders': hamcrest.contains_inanyorder(*[
            has_exact_entries({
                'ServiceID': o.service_id,
                'EffectiveServiceOrderID': o.service_order_id,
                'state': 'res_state',
                'tariff_dt': time.mktime(ON_DT.timetuple()),
                'completion_qty_delta': 1,
                'CurrencyID': 123,
                'consumes': [],
                'group_dt': time.mktime(TODAY.timetuple())
            })
            for o, skip in order_states if not skip
        ]),
        'errors': [],
    }
    hamcrest.assert_that(
        res_data,
        hamcrest.has_entries(req_res),
    )
