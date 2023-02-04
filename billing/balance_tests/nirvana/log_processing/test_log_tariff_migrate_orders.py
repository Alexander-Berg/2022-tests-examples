# -*- coding: utf-8 -*-
import time
import datetime
import json
import pytest
import mock
import uuid
import hamcrest as hm
import contextlib
from decimal import Decimal as D
from functools import partial

from balance import (
    exc,
    constants as cst,
    mapper,
)
from balance import muzzle_util as ut
from balance.actions.nirvana.operations.util.process_factory import get_last_task
from balance.actions.nirvana.operations.log_processing.log_tariff_migrate_orders import process

from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import create_invoice

from .common import (
    add_block_input as common_add_block_input,
    link_orders,
    create_order,
    create_client,
)

ON_DT = datetime.datetime.now().replace(microsecond=0)

TASK_TYPE = 'test_migration'
RUB_CURRENCY_ID = 643


@pytest.fixture(autouse=True)
def mock_get_log_interval_from_metadata():
    with mock.patch('balance.utils.log_tariff.get_log_interval_from_metadata'):
        yield


@contextlib.contextmanager
def mock_intervals_comparison(cmp_res):
    patch_path = 'balance.actions.nirvana.operations.log_processing.log_tariff_migrate_orders.compare_intervals'
    with mock.patch(patch_path, side_effect=lambda p, c: cmp_res):
        yield


@pytest.fixture
def nirvana_block(session):
    block = ob.NirvanaBlockBuilder.construct(
        session,
        operation='log_tariff_migrate_orders',
        request={'data': {'options': {}}},
    )
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


@pytest.fixture(name='task_type')
def create_task_type(session, id_=TASK_TYPE):
    return ob.LogTariffTypeBuilder.construct(session, id=id_)


@pytest.fixture(name='task')
def create_task(session, task_type, state=cst.NirvanaProcessingTaskState.NEW):
    task = ob.LogTariffTaskBuilder.construct(
        session,
        task_type=task_type,
        state=state,
        metadata={'some': 'metadata'},
    )
    return task


@pytest.fixture(name='main_order')
def create_main_order(session, client=None, ch_count=0):
    client = client or create_client(session)

    main_order = create_order(session, client=client, main_order=True)
    children = [create_order(session, client=client) for _i in range(ch_count)]
    link_orders(main_order, children)

    main_order.turn_on_log_tariff()
    return main_order


def add_block_input(nirvana_block, orders, params=None):
    params = params or {'type': TASK_TYPE}
    common_add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'metadata'}),
        params=json.dumps(params),
        orders=json.dumps(orders),
    )


@pytest.fixture
def block_input(session, nirvana_block):
    main_order = create_main_order(session, ch_count=1)
    children = main_order.group_children
    qtys = ['6.666']
    add_block_input(
        nirvana_block,
        orders=[
            {
                'ServiceID': order.service_id,
                'ServiceOrderID': order.service_order_id,
                'EffectiveServiceOrderID': main_order.service_order_id,
                'EventTime': time.mktime((ON_DT + datetime.timedelta(seconds=idx)).timetuple()),
                'BillableEventCostCur': qty,
                'CurrencyID': 643,
            }
            for idx, (order, qty) in enumerate(zip(children, qtys))
        ],
    )


@pytest.mark.usefixtures('task_type')
def test_new(session, nirvana_block):
    main_order = create_main_order(session, ch_count=3)
    children = main_order.group_children
    qtys = [3.333333, 6.666, 1244614.835437]

    add_block_input(
        nirvana_block,
        orders=[
            {
                'ServiceID': order.service_id,
                'ServiceOrderID': order.service_order_id,
                'EffectiveServiceOrderID': main_order.service_order_id,
                'EventTime': int(time.mktime((ON_DT + datetime.timedelta(seconds=idx)).timetuple())),
                'BillableEventCostCur': qty,
                'CurrencyID': 100 + idx,
            }
            for idx, (order, qty) in enumerate(zip(children, qtys))
        ],
    )

    with mock_intervals_comparison(cst.NirvanaProcessingAttemptType.NEW):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert nirvana_block.upload.call_args_list == []

    new_task = (
        session.query(mapper.LogTariffTask)
        .filter_by(
            type_id=TASK_TYPE,
            state=cst.NirvanaProcessingTaskState.IN_PROGRESS,
        )
        .one()
    )
    assert new_task.metadata == {'some': 'metadata'}
    assert len(new_task.migration_orders) == 1
    migration_order, = new_task.migration_orders

    hm.assert_that(
        new_task.migration_orders,
        hm.contains(
            hm.has_properties(
                service_id=main_order.service_id,
                service_order_id=main_order.service_order_id,
                state=None,
            ),
        )
    )
    hm.assert_that(
        new_task.migration_orders[0].tariff_inputs,
        hm.contains_inanyorder(*[
            hm.has_properties(
                task_id=new_task.id,
                service_id=order.service_id,
                service_order_id=order.service_order_id,
                tariff_dt=ON_DT + datetime.timedelta(seconds=idx),
                completion_qty_delta=ut.float2decimal6(qty),
                currency_id=100 + idx,
            )
            for idx, (order, qty) in enumerate(zip(children, qtys))
        ])
    )

    hm.assert_that(
        main_order,
        hm.has_properties(
            exports=hm.has_entries(
                TARIFF_MIGRATE=hm.has_properties(
                    state=cst.ExportState.enqueued,
                    input=hm.has_entries({
                        'log_tariff_task_id': new_task.id,
                        'migration_order_id': migration_order.id,
                    }),
                )
            )
        )
    )
    hm.assert_that(
        children,
        hm.only_contains(
            hm.has_properties(
                exports=hm.not_(hm.has_key('TARIFF_MIGRATE')),
            )
        )
    )


@pytest.mark.usefixtures('task_type')
def test_null_input(session, nirvana_block):
    """Если у ОС нет откруток в historical.
    """
    main_order = create_main_order(session, ch_count=1)
    order, = main_order.group_children

    add_block_input(
        nirvana_block,
        orders=[
            {
                'ServiceID': order.service_id,
                'ServiceOrderID': order.service_order_id,
                'EffectiveServiceOrderID': main_order.service_order_id,
                'EventTime': 0,
                'BillableEventCostCur': 0,
                'CurrencyID': None,
            }
        ],
    )

    with mock_intervals_comparison(cst.NirvanaProcessingAttemptType.NEW):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert nirvana_block.upload.call_args_list == []

    new_task = (
        session.query(mapper.LogTariffTask)
        .filter_by(
            type_id=TASK_TYPE,
            state=cst.NirvanaProcessingTaskState.IN_PROGRESS,
        )
        .one()
    )
    assert new_task.metadata == {'some': 'metadata'}
    assert len(new_task.migration_orders) == 1
    migration_order, = new_task.migration_orders

    hm.assert_that(
        migration_order,
        hm.has_properties(
            service_id=main_order.service_id,
            service_order_id=main_order.service_order_id,
            state=None,
        ),
    )
    hm.assert_that(
        migration_order.tariff_inputs,
        hm.contains(
            hm.has_properties(
                task_id=new_task.id,
                service_id=order.service_id,
                service_order_id=order.service_order_id,
                tariff_dt=datetime.datetime(1970, 1, 1, 3, 0),
                completion_qty_delta=D(0),
                currency_id=None,
            )
        )
    )
    hm.assert_that(
        main_order,
        hm.has_properties(
            exports=hm.has_entries(
                TARIFF_MIGRATE=hm.has_properties(
                    state=cst.ExportState.enqueued,
                    input=hm.has_entries({
                        'log_tariff_task_id': new_task.id,
                        'migration_order_id': migration_order.id,
                    }),
                )
            )
        )
    )


def test_upload_results(session, client, task, nirvana_block):
    main_orders = [create_main_order(session, client, ch_count=3) for _i in range(3)]
    add_block_input(
        nirvana_block,
        orders=[
            {
                'ServiceID': order.service_id,
                'ServiceOrderID': order.service_order_id,
                'EffectiveServiceOrderID': main_order.service_order_id,
                'EventTime': time.mktime((ON_DT + datetime.timedelta(seconds=idx)).timetuple()),
                'BillableEventCostCur': 666,
                'CurrencyID': RUB_CURRENCY_ID,
            }
            for main_order in main_orders
            for idx, order in enumerate(main_order.group_children)
        ],
    )
    task.state = cst.NirvanaProcessingTaskState.IN_PROGRESS
    session.flush()

    migration_orders = []
    migration_untariffed = []
    migration_consumes = []

    for idx, main_order in enumerate(main_orders):
        children = main_order.group_children
        inv = create_invoice(session, client=client, orders=[(main_order, 666)])
        inv.turn_on_rows()
        co, = inv.consumes

        migration_order = ob.LogTariffMigrationOrderBuilder.construct(
            session,
            order=main_order,
            task=task,
            state='%s:%s' % (co.id, ob.get_big_number()),
        )
        migration_orders.append(migration_order)
        migration_untariffed.extend([
            ob.LogTariffMigrationUntariffedBuilder.construct(
                session,
                task=task,
                order=order,
                migration_order=migration_order,
                overcompletion_qty=100 + idx,
                currency_id=RUB_CURRENCY_ID,
            )
            for order in [main_order] + children
        ])
        migration_consumes.append(
            ob.LogTariffMigrationConsumeBuilder.construct(
                session,
                task=task,
                consume=co,
                product_id=cst.DIRECT_PRODUCT_RUB_ID,
                migration_order=migration_order,
                qty=10 + idx,
                sum=200 + idx,
                consume_qty=300 + idx,
                consume_sum=400 + idx,
            )
        )

    with mock_intervals_comparison(cst.NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_finished()
    assert task.state == cst.NirvanaProcessingTaskState.FINISHED
    assert len(nirvana_block.upload.call_args_list) == 1
    data, = nirvana_block.upload.call_args_list
    data_tag, data = data[0]
    assert data_tag == 'res_data'

    data = json.loads(data)
    assert data['log_tariff_meta'] == {'some': 'metadata'}

    dyntable_data = data['dyntable']
    hm.assert_that(
        dyntable_data,
        hm.contains(*[
            hm.has_entries({
                'ServiceID': row.service_id,
                'EffectiveServiceOrderID': row.service_order_id,
                'state': row.state,
            })
            for row in sorted(migration_orders, key=lambda x: (x.service_id, x.service_order_id))
        ])
    )

    untariffed_data = data['untariffed']
    hm.assert_that(
        untariffed_data,
        hm.contains_inanyorder(*[
            hm.has_entries({
                'BillableEventCostCur': row.overcompletion_qty,
                'CurrencyID': RUB_CURRENCY_ID,
                'EffectiveServiceOrderID': row.migration_order.service_order_id,
                'EventTime': time.mktime((row.tariff_dt).timetuple()),
                'ProductID': row.product_id,
                'ServiceID': row.service_id,
                'ServiceOrderID': row.service_order_id,
            })
            for row in sorted(migration_untariffed, key=lambda x: (x.service_id, x.service_order_id, x.tariff_dt))
        ])
    )

    unprocessed_events_data = data['unprocessed_events']
    hm.assert_that(
        unprocessed_events_data,
        hm.contains_inanyorder(*[
            hm.has_entries({
                'EffectiveServiceOrderID': row.migration_order.service_order_id,
                'ServiceID': row.service_id,
                'ServiceOrderID': row.service_order_id,
                'ProductID': row.product_id,
                'coeff_qty': row.consume_qty,
                'coeff_sum': row.consume_sum,
                'consume_id': row.consume_id,
                'tariff_dt': time.mktime((row.tariff_dt).timetuple()),
                'tariffed_qty': row.qty,
                'tariffed_sum': row.sum,
            })
            for row in sorted(migration_consumes, key=lambda x: (x.service_id, x.service_order_id, x.tariff_dt))
        ])
    )


def test_upload_empty_results(session, client, task, nirvana_block):
    main_order = create_main_order(session, client, ch_count=1)
    add_block_input(
        nirvana_block,
        orders=[
            {
                'ServiceID': order.service_id,
                'ServiceOrderID': order.service_order_id,
                'EffectiveServiceOrderID': main_order.service_order_id,
                'EventTime': time.mktime((ON_DT + datetime.timedelta(seconds=idx)).timetuple()),
                'BillableEventCostCur': 666,
                'CurrencyID': RUB_CURRENCY_ID,
            }
            for idx, order in enumerate(main_order.group_children)
        ],
    )
    task.state = cst.NirvanaProcessingTaskState.IN_PROGRESS
    session.flush()

    migration_order = ob.LogTariffMigrationOrderBuilder.construct(
        session,
        order=main_order,
        task=task,
        state='%s:%s' % (0, ob.get_big_number()),
        error='Some migration error',
    )

    with mock_intervals_comparison(cst.NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_finished()
    assert task.state == cst.NirvanaProcessingTaskState.FINISHED

    data, error_data = nirvana_block.upload.call_args_list
    data_tag, data = data[0]
    data = json.loads(data)
    assert data['log_tariff_meta'] == {'some': 'metadata'}

    dyntable_data = data['dyntable']
    hm.assert_that(
        dyntable_data,
        hm.empty(),
    )
    untariffed_data = data['untariffed']
    hm.assert_that(
        untariffed_data,
        hm.empty(),
    )
    unprocessed_events_data = data['unprocessed_events']
    hm.assert_that(
        unprocessed_events_data,
        hm.empty(),
    )

    error_tag, error_data = error_data[0]
    assert error_tag == 'res_migration_errors'
    error_data = json.loads(error_data)
    hm.assert_that(
        error_data,
        hm.contains(
            hm.has_entries({
                'service_id': migration_order.service_id,
                'service_order_id': migration_order.service_order_id,
                'error': migration_order.error,
            })
        ),
    )


def test_archived_task(session, task_type, nirvana_block, client):
    finished_task = create_task(session, task_type, cst.NirvanaProcessingTaskState.FINISHED)

    main_order = create_main_order(session, client)
    inv = create_invoice(session, client=client, orders=[(main_order, 666)])
    inv.turn_on_rows()
    co, = inv.consumes

    migration_order = ob.LogTariffMigrationOrderBuilder.construct(
        session,
        order=main_order,
        task=finished_task,
        state='%s:%s' % (co.id, ob.get_big_number()),
    )
    ob.LogTariffMigrationInputBuilder.construct(
        session,
        task_id=finished_task.id,
        order=main_order,
        migration_order_id=migration_order.id,
        tariff_dt=ON_DT,
        completion_qty_delta=666,
        currency_id=RUB_CURRENCY_ID,
    )
    ob.LogTariffMigrationUntariffedBuilder.construct(
        session,
        task=finished_task,
        order=main_order,
        migration_order=migration_order,
        overcompletion_qty=666,
        currency_id=RUB_CURRENCY_ID,
    )
    ob.LogTariffMigrationConsumeBuilder.construct(
        session,
        task=finished_task,
        consume=co,
        product_id=cst.DIRECT_PRODUCT_RUB_ID,
        migration_order=migration_order,
        qty=333,
        sum=123,
        consume_qty=456,
        consume_sum=789,
    )

    main_order_new = create_main_order(session, ch_count=2)
    add_block_input(
        nirvana_block,
        orders=[
            {
                'ServiceID': order.service_id,
                'ServiceOrderID': order.service_order_id,
                'EffectiveServiceOrderID': main_order_new.service_order_id,
                'EventTime': time.mktime((ON_DT + datetime.timedelta(seconds=idx)).timetuple()),
                'BillableEventCostCur': 666,
                'CurrencyID': RUB_CURRENCY_ID,
            }
            for idx, order in enumerate(sorted(main_order_new.group_children, key=lambda x: x.id))
        ],
    )

    with mock_intervals_comparison(cst.NirvanaProcessingAttemptType.NEW):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert finished_task.state == cst.NirvanaProcessingTaskState.ARCHIVED
    assert session.query(mapper.LogTariffMigrationConsume).filter_by(task_id=finished_task.id).count() == 0
    assert session.query(mapper.LogTariffMigrationUntariffed).filter_by(task_id=finished_task.id).count() == 0
    assert session.query(mapper.LogTariffMigrationInput).filter_by(task_id=finished_task.id).count() == 0
    assert session.query(mapper.LogTariffMigrationOrder).filter_by(task_id=finished_task.id).count() == 0

    new_task = get_last_task(session, mapper.LogTariffTask, TASK_TYPE)
    assert new_task.state == cst.NirvanaProcessingTaskState.IN_PROGRESS
    migration_order = session.query(mapper.LogTariffMigrationOrder).filter_by(task_id=new_task.id).all()
    assert len(migration_order) == 1
    migration_order, = migration_order
    hm.assert_that(
        migration_order,
        hm.has_properties(
            service_id=main_order_new.service_id,
            service_order_id=main_order_new.service_order_id,
            state=None,
        ),
    )
    hm.assert_that(
        migration_order.tariff_inputs,
        hm.contains_inanyorder(*[
            hm.has_properties(
                task_id=new_task.id,
                service_id=o.service_id,
                service_order_id=o.service_order_id,
                tariff_dt=(ON_DT + datetime.timedelta(seconds=idx)),
                completion_qty_delta=666,
                currency_id=RUB_CURRENCY_ID,
            )
            for idx, o in enumerate(sorted(main_order_new.group_children, key=lambda x: x.id))
        ]),
    )


@pytest.mark.usefixtures('block_input')
def test_prev_task(session, task, nirvana_block):
    task.state = cst.NirvanaProcessingTaskState.IN_PROGRESS
    session.flush()

    main_order = create_main_order(session, ch_count=1)
    _migration_order = ob.LogTariffMigrationOrderBuilder.construct(
        session,
        order=main_order,
        task=task,
        state=None,
    )

    with mock_intervals_comparison(cst.NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)
    session.expire_all()

    assert proc_res.is_running()
    assert task.state == cst.NirvanaProcessingTaskState.IN_PROGRESS


@pytest.mark.parametrize(
    'prev_state, interval_res, exc_cls, exc_message',
    [
        pytest.param(
            cst.NirvanaProcessingTaskState.ARCHIVED,
            cst.NirvanaProcessingAttemptType.PREV,
            exc.LOG_TASK_STATE_INVALID,
            'archived task'
        ),
        pytest.param(
            cst.NirvanaProcessingTaskState.FINISHED,
            cst.NirvanaProcessingAttemptType.INVALID,
            exc.LOG_TASK_ATTEMPT_INVALID,
            'broken sequence of tasks'
        ),
        pytest.param(
            cst.NirvanaProcessingTaskState.IN_PROGRESS,
            cst.NirvanaProcessingAttemptType.NEW,
            exc.LOG_TASK_STATE_INVALID,
            'previous task is not finished'
        ),
    ]
)
def test_mismatched_states(session, task_type, nirvana_block, prev_state, interval_res, exc_cls, exc_message):
    task = create_task(session, task_type, prev_state)

    main_order = create_main_order(session, ch_count=1)
    add_block_input(
        nirvana_block,
        orders=[
            {
                'ServiceID': order.service_id,
                'ServiceOrderID': order.service_order_id,
                'EffectiveServiceOrderID': main_order.service_order_id,
                'EventTime': time.mktime((ON_DT + datetime.timedelta(seconds=idx)).timetuple()),
                'BillableEventCostCur': 666,
                'CurrencyID': RUB_CURRENCY_ID,
            }
            for idx, order in enumerate(main_order.group_children)
        ],
    )

    with pytest.raises(exc_cls) as exc_info:
        with mock_intervals_comparison(interval_res):
            process(nirvana_block)
    assert exc_message in exc_info.value.msg


def test_locked_type(app, session, nirvana_block):
    real_session = app.real_new_session()

    type_id = uuid.uuid5(uuid.NAMESPACE_OID, 'test_locked_type').hex
    with real_session.begin():
        task_type = real_session.query(mapper.LogTariffType).get(type_id)
        if task_type is None:
            task_type = create_task_type(real_session, id_=type_id)

    add_block_input(nirvana_block, orders=[], params={'type': task_type.id})

    with real_session.begin():
        task_type.lock()

        with pytest.raises(exc.LOG_TASK_IS_LOCKED):
            process(nirvana_block)


@pytest.mark.usefixtures('task_type')
def test_no_orders(session, nirvana_block):
    add_block_input(nirvana_block, orders=[])

    with pytest.raises(exc.LOG_TARIFF_NO_INPUT_DATA):
        process(nirvana_block)
