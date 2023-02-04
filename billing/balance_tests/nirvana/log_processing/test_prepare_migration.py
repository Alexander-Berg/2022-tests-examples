# -*- coding: utf-8 -*-
import datetime
import json
import pytest
import mock
import contextlib
import hamcrest as hm
from functools import partial

from balance import (
    constants as cst,
    exc,
)
from balance.actions.nirvana.operations.log_processing.prepare_migration import process

from tests import object_builder as ob
from tests.tutils import has_exact_entries

from .common import (
    add_block_input,
    link_orders,
    create_client,
    create_order,
)

RUN_ID = '2020-12-10T23:23:12'


class MockYtClient(object):
    def __init__(self, *a, **kw):
        self.data = {}

    def __call__(self, *a, **kw):
        return self

    def write_table(self, table, rows):
        self.data['table'] = table
        self.data['rows'] = rows

    def set(self, meta_table, metadata):
        self.data['meta_table'] = meta_table
        self.data['metadata'] = metadata

    def TablePath(self, table, schema):
        return table

    @contextlib.contextmanager
    def Transaction(self):
        yield


@pytest.fixture
def mock_yt_client():
    with mock.patch('balance.actions.nirvana.operations.log_processing.prepare_migration.YtClient', MockYtClient()) as yt_client:
        yield yt_client


@pytest.fixture
def nirvana_block(session):
    block = ob.NirvanaBlockBuilder.construct(
        session,
        operation='prepare_migration',
        request={'data': {'options': {'res_table_dir': '//tmp/trash'}}},
    )
    with mock.patch.object(block, 'upload'), mock.patch.object(block, 'download'):
        yield block


@pytest.fixture
def task_type(session):
    return ob.YTLogLoadTypeBuilder.construct(session, table_name='t_log_tariff_migration_order_load')


@pytest.fixture(name='task')
def create_task(session, task_type):
    return ob.YTLogLoadTaskBuilder.construct(
        session,
        external_id='1',
        task_type=task_type,
        cluster_name='hahn',
        table_path='//tmp/import/2020-11-12',
        state=cst.NirvanaProcessingTaskState.IN_PROGRESS,
        metadata={'some': 'meta'},
    )


@pytest.fixture
def block_input(nirvana_block, task_type):
    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'meta'}),
        params=json.dumps({
            'run_id': RUN_ID,
            'type': task_type.cc,
            'cluster': 'hahn',
            'table': '//tmp/import/2020-11-12',
        })
    )


@pytest.fixture(autouse=True)
def mock_batch_processor():
    patch_path = 'balance.util.ParallelBatchProcessor.process_batches'
    calls = []

    def _process_batches(_s, func, batches, **kw):
        calls.append(batches)
        return map(partial(func, **kw), batches)

    with mock.patch(patch_path, _process_batches):
        yield calls


def do_complete(order, qty):
    order.calculate_consumption(datetime.datetime.now(), {order.shipment_type: qty})
    order.session.flush()


@pytest.fixture(name='main_order')
def create_main_order(session, task_type, client=None, ch_count=0):
    client = client or create_client(session)

    main_order = create_order(session, client=client, main_order=True)
    ob.LogTariffMigrationOrderLoadBuilder.construct(session, log_type_id=task_type.id, order=main_order)

    children = [create_order(session, client=client) for _i in range(ch_count)]
    link_orders(main_order, children)

    return main_order


@pytest.mark.usefixtures('block_input')
def test_no_load_task(session, nirvana_block):
    with pytest.raises(exc.LOG_TASK_ATTEMPT_INVALID) as exc_info:
        process(nirvana_block)
    assert exc_info.value.msg == 'Invalid attempt with last task None: broken sequence of tasks'


@pytest.mark.parametrize(
    'task_attr, val',
    [
        pytest.param('state', cst.NirvanaProcessingTaskState.NEW, id='state'),
        pytest.param('metadata', {'different': 'meta'}, id='metadata'),
    ],
)
@pytest.mark.usefixtures('block_input')
def test_invalid_task(session, nirvana_block, task, task_attr, val):
    setattr(task, task_attr, val)
    session.flush()

    with pytest.raises(exc.LOG_TASK_ATTEMPT_INVALID) as exc_info:
        process(nirvana_block)
    assert exc_info.value.msg == 'Invalid attempt with last task %s: broken sequence of tasks' % task.id


@pytest.mark.usefixtures('block_input')
def test_upload_orders_w_children(session, nirvana_block, task_type, task, mock_yt_client):
    main_orders = [create_main_order(session, task_type, ch_count=2) for _i in range(3)]
    process(nirvana_block)

    session.refresh(task)
    assert task.state == cst.NirvanaProcessingTaskState.FINISHED

    for main_order in main_orders:
        assert main_order._is_log_tariff == cst.OrderLogTariffState.INIT
        for ch_order in main_order.group_children:
            assert ch_order.child_ua_type == cst.UAChildType.LOG_TARIFF

    hm.assert_that(
        mock_yt_client.data,
        hm.has_entries({
            'table': '//tmp/trash/%s' % RUN_ID,
            'rows': hm.contains_inanyorder(*[
                has_exact_entries({
                    'ServiceID': o.service_id,
                    'ServiceOrderID': o.service_order_id,
                    'EffectiveServiceOrderID': main_o.service_order_id,
                })
                for main_o in main_orders
                for o in main_o.group_children
            ]),
            'meta_table': '//tmp/trash/%s/@log_tariff_meta' % RUN_ID,
            'metadata': hm.has_entries({'some': 'meta'}),
        }),
    )


@pytest.mark.usefixtures('block_input')
def test_upload_orders_w_2_level_children(session, nirvana_block, client, task_type, task, mock_yt_client):
    main_order = create_order(session, client)
    ch_order_1 = create_order(session, client)
    ch_order_2 = create_order(session, client)

    link_orders(ch_order_1, [ch_order_2])
    link_orders(main_order, [ch_order_1])
    ob.LogTariffMigrationOrderLoadBuilder.construct(session, log_type_id=task_type.id, order=main_order)

    process(nirvana_block)
    session.expire_all()

    assert task.state == cst.NirvanaProcessingTaskState.FINISHED

    assert main_order._is_log_tariff == cst.OrderLogTariffState.INIT
    assert ch_order_1._is_log_tariff == cst.OrderLogTariffState.OFF
    assert ch_order_2._is_log_tariff == cst.OrderLogTariffState.OFF

    assert ch_order_1.child_ua_type == cst.UAChildType.LOG_TARIFF
    assert ch_order_2.child_ua_type == cst.UAChildType.LOG_TARIFF

    hm.assert_that(
        mock_yt_client.data,
        hm.has_entries({
            'table': '//tmp/trash/%s' % RUN_ID,
            'rows': hm.contains_inanyorder(*[
                has_exact_entries({
                    'ServiceID': o.service_id,
                    'ServiceOrderID': o.service_order_id,
                    'EffectiveServiceOrderID': main_order.service_order_id,
                })
                for o in [ch_order_1, ch_order_2]
            ]),
            'meta_table': '//tmp/trash/%s/@log_tariff_meta' % RUN_ID,
            'metadata': hm.has_entries({'some': 'meta'}),
        }),
    )


@pytest.mark.usefixtures('block_input')
def test_upload_wo_children_orders(session, nirvana_block, task_type, task, mock_yt_client):
    client = ob.ClientBuilder.construct(session)
    client.set_currency(cst.ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), cst.CONVERT_TYPE_COPY)

    cur_order = create_order(session, client, cst.DIRECT_PRODUCT_RUB_ID)
    parent_cur_order = create_order(session, client, cst.DIRECT_PRODUCT_RUB_ID)
    fish_order = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=cst.DIRECT_PRODUCT_ID,
        group_order_id=parent_cur_order.id
    )

    ob.LogTariffMigrationOrderLoadBuilder.construct(session, log_type_id=task_type.id, order=cur_order)
    ob.LogTariffMigrationOrderLoadBuilder.construct(session, log_type_id=task_type.id, order=parent_cur_order)
    session.expire_all()

    process(nirvana_block)
    session.expire_all()

    assert task.state == cst.NirvanaProcessingTaskState.FINISHED
    assert cur_order._is_log_tariff == cst.OrderLogTariffState.INIT
    assert parent_cur_order._is_log_tariff == cst.OrderLogTariffState.INIT

    hm.assert_that(
        mock_yt_client.data,
        hm.has_entries({
            'table': '//tmp/trash/%s' % RUN_ID,
            'rows': hm.contains_inanyorder(
                has_exact_entries(
                    ServiceID=cur_order.service_id,
                    ServiceOrderID=cur_order.service_order_id,
                    EffectiveServiceOrderID=cur_order.service_order_id,
                ),
                has_exact_entries(
                    ServiceID=parent_cur_order.service_id,
                    ServiceOrderID=parent_cur_order.service_order_id,
                    EffectiveServiceOrderID=parent_cur_order.service_order_id,
                ),
                has_exact_entries(
                    ServiceID=fish_order.service_id,
                    ServiceOrderID=fish_order.service_order_id,
                    EffectiveServiceOrderID=parent_cur_order.service_order_id,
                )
            ),
            'meta_table': '//tmp/trash/%s/@log_tariff_meta' % RUN_ID,
            'metadata': hm.has_entries({'some': 'meta'}),
        }),
    )


def test_2_yt_load_task(session, nirvana_block, client, mock_yt_client):
    task_type_1 = task_type(session)
    task_type_2 = task_type(session)

    task_1 = create_task(session, task_type_1)
    _task_2 = create_task(session, task_type_2)

    main_order_1 = create_main_order(session, task_type_1, client, ch_count=1)
    _main_order_2 = create_main_order(session, task_type_2, client, ch_count=1)

    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'meta'}),
        params=json.dumps({
            'run_id': RUN_ID,
            'type': task_type_1.cc,
            'cluster': 'hahn',
            'table_path': '//tmp/trash',
            'table': '//tmp/import/%s' % RUN_ID,
        })
    )

    process(nirvana_block)
    session.expire_all()

    assert task_1.state == cst.NirvanaProcessingTaskState.FINISHED
    assert main_order_1._is_log_tariff == cst.OrderLogTariffState.INIT

    hm.assert_that(
        mock_yt_client.data,
        hm.has_entries({
            'table': '//tmp/trash/%s' % RUN_ID,
            'rows': hm.contains_inanyorder(*[
                has_exact_entries({
                    'ServiceID': o.service_id,
                    'ServiceOrderID': o.service_order_id,
                    'EffectiveServiceOrderID': main_order_1.service_order_id,
                })
                for o in main_order_1.group_children
            ]),
            'meta_table': '//tmp/trash/%s/@log_tariff_meta' % RUN_ID,
            'metadata': hm.has_entries({'some': 'meta'}),
        }),
    )


@pytest.mark.parametrize(
    'task_state',
    [cst.NirvanaProcessingTaskState.FINISHED, cst.NirvanaProcessingTaskState.ARCHIVED],
)
@pytest.mark.usefixtures('block_input')
def test_restart(session, nirvana_block, client, task_type, task, main_order, task_state, mock_yt_client):
    task.state = task_state
    session.flush()

    res = process(nirvana_block)

    assert str(res) == 'finished'
    assert mock_yt_client.data == {}
