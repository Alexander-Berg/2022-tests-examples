# -*- coding: utf-8 -*-

import datetime
import json
import contextlib
import decimal
import uuid

import pytest
import mock
import hamcrest
import sqlalchemy as sa

from balance import mapper
from balance import scheme
from balance import exc
from balance.constants import (
    NirvanaProcessingTaskState,
)
from balance.actions.nirvana.operations.log_processing.yt_log_load import (
    process,
    LogLoaderLogic,
)

from tests import object_builder as ob

from tests.balance_tests.nirvana.log_processing.common import (
    add_block_input,
)


@pytest.fixture(autouse=True)
def mock_get_log_interval_from_metadata():
    with mock.patch('balance.utils.log_tariff.get_log_interval_from_metadata'):
        yield


@pytest.fixture
def nirvana_block(session):
    block = ob.NirvanaBlockBuilder.construct(session, operation='load_yt_log', request={})
    with mock.patch.object(block, 'upload'), mock.patch.object(block, 'download'):
        yield block


@pytest.fixture
def task_type(session):
    return ob.YTLogLoadTypeBuilder.construct(session)


@contextlib.contextmanager
def mock_yt_data(cluster, table, rows):
    patch_path = 'balance.actions.nirvana.operations.log_processing.yt_log_load.LogLoaderLogic._read_yt_table'

    def _mock(m_cluster, m_table, context):
        assert m_cluster == cluster
        assert m_table == table
        return iter(rows)

    with mock.patch(patch_path, staticmethod(_mock)):
        yield


@contextlib.contextmanager
def mock_delete_data():
    patch_path = 'balance.actions.nirvana.operations.log_processing.yt_log_load.LogLoaderLogic._delete_data'
    base_func = LogLoaderLogic._delete_data
    session_mock = mock.Mock()

    with mock.patch(patch_path, staticmethod(lambda s, c: base_func(session_mock, c))):
        yield session_mock


def assert_truncate_call(mock_obj, task_type):
    query = 'ALTER TABLE %s TRUNCATE PARTITION %s UPDATE INDEXES' % (task_type.table_name, task_type.cc)
    hamcrest.assert_that(
        mock_obj.execute,
        hamcrest.has_properties(
            call_count=1,
            call_args=hamcrest.has_properties(args=(query,))
        )
    )


def assert_last_task(session, task_type, **kwargs):
    task = (
        session.query(mapper.YtLogLoadTask)
            .filter_by(type_id=task_type.id)
            .order_by(sa.desc(mapper.YtLogLoadTask.id))
            .first()
    )
    hamcrest.assert_that(
        task,
        hamcrest.has_properties(
            **kwargs
        )
    )


def _row_d2f(row):
    return {
        k: float(v) if isinstance(v, decimal.Decimal) else v
        for k, v in row.items()
    }


def assert_rows(session, task_type, rows):
    res_rows_sel = (
        sa.select([scheme.log_tariff_act_row])
        .where(scheme.log_tariff_act_row.c.log_type_id == task_type.id)
    )
    res_rows = map(_row_d2f, session.execute(res_rows_sel))
    hamcrest.assert_that(
        res_rows,
        hamcrest.contains_inanyorder(*[
            hamcrest.has_entries(**row)
            for row in rows
        ])
    )


def format_row(eid, dt_, iid, cid, tpid, qty, sum_):
    return {
        'act_eid': eid,
        'act_dt': dt_,
        'invoice_id': iid,
        'consume_id': cid,
        'tax_policy_pct_id': tpid,
        'acted_qty': qty,
        'acted_sum': sum_,
    }


@pytest.mark.parametrize(
    'prev_task_state',
    [
        NirvanaProcessingTaskState.FINISHED,
        NirvanaProcessingTaskState.ARCHIVED,
        None,
    ]
)
def test_new(session, nirvana_block, task_type, prev_task_state):
    if prev_task_state:
        prev_task = ob.YTLogLoadTaskBuilder.construct(
            session,
            external_id='1',
            task_type=task_type,
            state=prev_task_state,
        )

    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'meta'}),
        params=json.dumps({
            'run_id': '2',
            'type': task_type.cc,
            'cluster': 'nhah',
            'table': '//tmp/trash',
        })
    )

    rows = [
        format_row('Y-1', 666, 1, 1, 1, 666.666, 6.66),
        format_row('Y-2', 666, 1, 2, 1, 123.456, 8),
        format_row('Y-2', 666, 3, 2, 2, 1, 2),
    ]
    with mock_yt_data('nhah', '//tmp/trash', rows), mock_delete_data() as delete_mock:
        proc_res = process(nirvana_block)

    session.expire_all()

    assert proc_res.is_finished()
    if prev_task_state:
        assert prev_task.state == NirvanaProcessingTaskState.ARCHIVED
        assert_truncate_call(delete_mock, task_type)
    else:
        assert delete_mock.execute.call_count == 0

    assert_last_task(
        session,
        task_type,
        state=NirvanaProcessingTaskState.IN_PROGRESS,
        external_id='2',
        cluster_name='nhah',
        table_path='//tmp/trash',
        metadata={'some': 'meta'},
    )
    assert_rows(session, task_type, rows)


@pytest.mark.parametrize(
    'prev_task_state',
    [
        NirvanaProcessingTaskState.NEW,
        NirvanaProcessingTaskState.IN_PROGRESS,
    ]
)
def test_new_prev_unfinished(session, nirvana_block, task_type, prev_task_state):
    prev_task = ob.YTLogLoadTaskBuilder.construct(
        session,
        external_id='1',
        task_type=task_type,
        state=prev_task_state,
    )

    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'meta'}),
        params=json.dumps({'run_id': '2', 'type': task_type.cc})
    )

    rows = [{'act_eid': 'some_act'}]
    with pytest.raises(exc.LOG_TASK_STATE_INVALID) as exc_info:
        with mock_yt_data('nhah', '//tmp/trash', rows), mock_delete_data() as delete_mock:
            process(nirvana_block)

    assert delete_mock.execute.call_count == 0
    assert 'previous task is not finished' in exc_info.value.msg
    assert prev_task.state == prev_task_state


def test_retry_new_task(session, nirvana_block, task_type):
    task = ob.YTLogLoadTaskBuilder.construct(
        session,
        external_id='1',
        task_type=task_type,
        state=NirvanaProcessingTaskState.NEW,
        cluster_name='nhah',
        table_path='//tmp/trash',
        metadata={'some': 'meta'}
    )

    add_block_input(
        nirvana_block,
        metadata=json.dumps({'other': 'beta'}),
        params=json.dumps({
            'run_id': '1',
            'type': task_type.cc,
            'cluster': 'abyr',
            'table': 'valg',
        })
    )

    rows = [
        format_row('Y-2', 666, 3, 2, 5, 1, 2),
    ]
    with mock_yt_data('nhah', '//tmp/trash', rows), mock_delete_data() as delete_mock:
        proc_res = process(nirvana_block)

    session.expire_all()

    assert proc_res.is_finished()
    assert_truncate_call(delete_mock, task_type)
    assert_last_task(
        session,
        task_type,
        id=task.id,
        state=NirvanaProcessingTaskState.IN_PROGRESS,
        external_id='1',
        cluster_name='nhah',
        table_path='//tmp/trash',
        metadata={'some': 'meta'},
    )
    assert_rows(session, task_type, rows)


@pytest.mark.parametrize(
    'task_state',
    [
        NirvanaProcessingTaskState.IN_PROGRESS,
        NirvanaProcessingTaskState.FINISHED,
    ]
)
def test_finished(session, nirvana_block, task_type, task_state):
    task = ob.YTLogLoadTaskBuilder.construct(
        session,
        external_id='1',
        task_type=task_type,
        state=task_state,
    )

    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'meta'}),
        params=json.dumps({'run_id': '1', 'type': task_type.cc})
    )

    rows = [{'act_eid': 'some_act'}]
    with mock_yt_data('nhah', '//tmp/trash', rows), mock_delete_data() as delete_mock:
        proc_res = process(nirvana_block)

    assert proc_res.is_finished()
    assert delete_mock.execute.call_count == 0
    assert task.state == task_state


def test_archived(session, nirvana_block, task_type):
    task = ob.YTLogLoadTaskBuilder.construct(
        session,
        external_id='1',
        task_type=task_type,
        state=NirvanaProcessingTaskState.ARCHIVED,
    )

    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'meta'}),
        params=json.dumps({'run_id': '1', 'type': task_type.cc})
    )

    rows = [{'act_eid': 'some_act'}]
    with pytest.raises(exc.LOG_TASK_STATE_INVALID) as exc_info:
        with mock_yt_data('nhah', '//tmp/trash', rows), mock_delete_data() as delete_mock:
            process(nirvana_block)

    assert delete_mock.execute.call_count == 0
    assert 'archived task' in exc_info.value.msg
    assert task.state == NirvanaProcessingTaskState.ARCHIVED


def test_invalid_attempt(session, nirvana_block, task_type):
    task = ob.YTLogLoadTaskBuilder.construct(
        session,
        external_id='666',
        task_type=task_type,
        state=NirvanaProcessingTaskState.FINISHED,
    )

    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'meta'}),
        params=json.dumps({'run_id': '1', 'type': task_type.cc})
    )

    rows = [{'act_eid': 'some_act'}]
    with pytest.raises(exc.LOG_TASK_ATTEMPT_INVALID):
        with mock_yt_data('nhah', '//tmp/trash', rows), mock_delete_data() as delete_mock:
            process(nirvana_block)

    assert delete_mock.execute.call_count == 0
    assert task.state == NirvanaProcessingTaskState.FINISHED


def test_locked_type(app, session, nirvana_block):
    real_session = app.real_new_session()

    type_id = uuid.uuid5(uuid.NAMESPACE_OID, 'test_locked_type').hex
    with real_session.begin():
        task_type = real_session.query(mapper.YtLogLoadType).filter_by(cc=type_id).one_or_none()
        if task_type is None:
            task_type = ob.YTLogLoadTypeBuilder.construct(real_session, cc=type_id)

    add_block_input(
        nirvana_block,
        params=json.dumps({'run_id': '1', 'type': task_type.cc})
    )

    with real_session.begin(), mock_yt_data('a', 'b', []), mock_delete_data():
        task_type.lock()

        with pytest.raises(exc.LOG_TASK_IS_LOCKED):
            process(nirvana_block)


def test_cols_map(session, nirvana_block):
    task_type = ob.YTLogLoadTypeBuilder.construct(session, cc='postrestore', table_name='bo.t_log_tariff_migration_order_load')
    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'meta'}),
        params=json.dumps({
            'run_id': '2',
            'type': task_type.cc,
            'cluster': 'winter',
            'table': '//tmp/trash',
            'cols_map': {'service_id': 'ServiceID', 'service_order_id': 'EffectiveServiceOrderID'},
        })
    )

    rows = [
        {'ServiceID': 7, 'EffectiveServiceOrderID': 123, 'state': '0:123'},
        {'ServiceID': 7, 'EffectiveServiceOrderID': 666, 'state': '0:666'},
        {'ServiceID': 7, 'EffectiveServiceOrderID': 999, 'state': '0:999'},
    ]
    session.flush()
    with mock_yt_data('winter', '//tmp/trash', rows), mock_delete_data() as delete_mock:
        proc_res = process(nirvana_block)

    session.expire_all()

    assert proc_res.is_finished()
    assert delete_mock.execute.call_count == 0

    assert_last_task(
        session,
        task_type,
        state=NirvanaProcessingTaskState.IN_PROGRESS,
        external_id='2',
        cluster_name='winter',
        table_path='//tmp/trash',
        metadata={'some': 'meta'},
    )
    res_rows_sel = (
        sa.select([scheme.log_tariff_migration_order_loads])
            .where(scheme.log_tariff_migration_order_loads.c.log_type_id == task_type.id)
    )
    res_rows = map(_row_d2f, session.execute(res_rows_sel))
    hamcrest.assert_that(
        res_rows,
        hamcrest.contains_inanyorder(
            hamcrest.has_entries({'service_id': 7, 'service_order_id': 123}),
            hamcrest.has_entries({'service_id': 7, 'service_order_id': 666}),
            hamcrest.has_entries({'service_id': 7, 'service_order_id': 999}),
        )
    )
