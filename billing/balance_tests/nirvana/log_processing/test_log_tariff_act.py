# -*- coding: utf-8 -*-

import itertools
import functools
import json
import uuid
import time
import decimal
import contextlib

import pytest
import mock
import hamcrest
import sqlalchemy as sa

from balance import mapper
from balance import exc
from balance import scheme
from balance.constants import (
    NirvanaProcessingTaskState,
    NirvanaProcessingAttemptType,
    ExportState,
)
from balance.actions.nirvana.operations.log_processing.log_tariff_act import (
    process,
)


from tests import object_builder as ob

from tests.balance_tests.nirvana.log_processing.common import (
    add_block_input,
    create_act,
    create_invoice,
    gen_act_external_id,
)

D = decimal.Decimal


@pytest.fixture(autouse=True)
def mock_get_log_interval_from_metadata():
    with mock.patch('balance.utils.log_tariff.get_log_interval_from_metadata'):
        yield


@pytest.fixture
def nirvana_block(session):
    block = ob.NirvanaBlockBuilder.construct(session, operation='act_tariff_log', request={})
    with mock.patch.object(block, 'upload'), mock.patch.object(block, 'download'):
        yield block


@pytest.fixture
def task_type(session):
    return ob.LogTariffTypeBuilder.construct(session)


@pytest.fixture
def load_task_type(session, task_type):
    return ob.YTLogLoadTypeBuilder.construct(session, cc=task_type.id)


@pytest.fixture(autouse=True)
def mock_batch_processor():
    patch_path = 'balance.util.ParallelBatchProcessor.process_batches'
    with mock.patch(patch_path, lambda s, f, b, **k: map(functools.partial(f, **k), b)):
        yield


@contextlib.contextmanager
def mock_intervals_comparison(cmp_res):
    patch_path = 'balance.actions.nirvana.operations.log_processing.log_tariff_act.compare_intervals'
    with mock.patch(patch_path, side_effect=lambda p, c: cmp_res):
        yield


def create_task(task_type, state, metadata=None):
    return ob.LogTariffTaskBuilder.construct(
        task_type.session,
        task_type=task_type,
        state=state,
        metadata=metadata or {'some': 'meta'}
    )


def create_load_task(task_type, state=NirvanaProcessingTaskState.IN_PROGRESS, metadata=None):
    return ob.YTLogLoadTaskBuilder.construct(
        task_type.session,
        task_type=task_type,
        state=state,
        metadata=metadata or {'some': 'meta'},
    )


def create_act_rows(task_type, consume_rows):
    rows = [
        {
            'log_type_id': task_type.id,
            'act_eid': eid,
            'act_dt': int(time.time()),
            'consume_id': co.id,
            'tax_policy_pct_id': co.tax_policy_pct_id,
            'invoice_id': co.invoice_id,
            'acted_qty': q,
            'acted_sum': s,
            'service_order_id': ssid,
            'hidden': hidden
        }
        for eid, co, q, s, ssid, hidden in consume_rows
    ]
    task_type.session.execute(scheme.log_tariff_act_row.insert(), rows)


def assert_last_task(session, task_type, **kwargs):
    task = (
        session.query(mapper.LogTariffTask)
            .filter_by(type_id=task_type.id)
            .order_by(sa.desc(mapper.LogTariffTask.id))
            .first()
    )
    hamcrest.assert_that(
        task,
        hamcrest.has_properties(
            **kwargs
        )
    )


def assert_invoices_export(state, task_type, *invoices):
    if task_type:
        input_ = {'log_type_id': task_type.id}
    else:
        input_ = None

    hamcrest.assert_that(
        invoices,
        hamcrest.only_contains(
            hamcrest.has_properties(
                exports=hamcrest.has_entry(
                    'LOG_TARIFF_ACT',
                    hamcrest.has_properties(
                        state=state,
                        input=input_
                    )
                )
            )
        )
    )


@pytest.mark.parametrize(
    'prev_task_state',
    [
        NirvanaProcessingTaskState.FINISHED,
        NirvanaProcessingTaskState.ARCHIVED,
        None,
    ]
)
def test_new(session, nirvana_block, task_type, load_task_type, prev_task_state):
    if prev_task_state:
        prev_task = create_task(task_type, prev_task_state)

    load_task = create_load_task(load_task_type, NirvanaProcessingTaskState.IN_PROGRESS)

    add_block_input(
        nirvana_block,
        metadata=json.dumps(load_task.metadata),
        params=json.dumps({'type': task_type.id})
    )

    invoices = [
        create_invoice(session, 3, exported=True),
        create_invoice(session, 2, exported=False)
    ]
    co1, co2, co3, co4, co5 = itertools.chain.from_iterable(i.consumes for i in invoices)
    create_act_rows(
        load_task_type,
        [
            ('Y-666', co1, 1, 2, None, 0),
            ('Y-666', co2, 3, 4, None, 0),
            ('Y-667', co3, D('666.666'), D('13'), None, 0),
            ('Y-668', co4, 18, 9, None, 0),
            ('Y-669', co5, 587, 777, None, 0),
        ]
    )

    with mock_intervals_comparison(NirvanaProcessingAttemptType.NEW):
        proc_res = process(nirvana_block)

    session.expire_all()

    assert proc_res.is_running()
    if prev_task_state:
        assert prev_task.state == NirvanaProcessingTaskState.ARCHIVED
    assert load_task.state == NirvanaProcessingTaskState.IN_PROGRESS

    assert_last_task(
        session,
        task_type,
        state=NirvanaProcessingTaskState.IN_PROGRESS,
        metadata={'some': 'meta'},
    )
    assert_invoices_export(ExportState.enqueued, load_task_type, *invoices)


@pytest.mark.parametrize(
    'prev_task_state',
    [
        NirvanaProcessingTaskState.NEW,
        NirvanaProcessingTaskState.IN_PROGRESS,
    ]
)
def test_new_prev_unfinished(session, nirvana_block, task_type, load_task_type, prev_task_state):
    create_task(task_type, prev_task_state)
    load_task = create_load_task(load_task_type)

    add_block_input(
        nirvana_block,
        metadata=json.dumps(load_task.metadata),
        params=json.dumps({'type': task_type.id})
    )

    invoice = create_invoice(session)
    create_act_rows(load_task_type, [('Y-666', invoice.consumes[0], 1, 2, None, 0)])

    with pytest.raises(exc.LOG_TASK_STATE_INVALID) as exc_info:
        with mock_intervals_comparison(NirvanaProcessingAttemptType.NEW):
            process(nirvana_block)

    assert 'previous task is not finished' in exc_info.value.msg


def test_retry_new_task(session, nirvana_block, task_type, load_task_type):
    task = create_task(task_type, NirvanaProcessingTaskState.NEW)
    load_task = create_load_task(load_task_type)

    add_block_input(
        nirvana_block,
        metadata=json.dumps(task.metadata),
        params=json.dumps({'type': task_type.id})
    )

    invoice = create_invoice(session)
    create_act_rows(load_task_type, [('Y-666', invoice.consumes[0], 1, 2, None, 0)])

    with mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)

    session.expire_all()

    assert proc_res.is_running()
    assert load_task.state == NirvanaProcessingTaskState.IN_PROGRESS
    assert_last_task(
        session,
        task_type,
        id=task.id,
        state=NirvanaProcessingTaskState.IN_PROGRESS,
        metadata={'some': 'meta'},
    )
    assert_invoices_export(ExportState.enqueued, load_task_type, invoice)


@pytest.mark.parametrize(
    'act_exists, act_hidden, act_rows_hidden, res_state',
    [
        pytest.param(False, 0, 0, NirvanaProcessingTaskState.IN_PROGRESS, id='wait'),
        pytest.param(True, 0, 1, NirvanaProcessingTaskState.IN_PROGRESS, id='act_not_hidden_act_row_hidden_wait'),
        pytest.param(True, 1, 1, NirvanaProcessingTaskState.FINISHED, id='act_hidden_act_row_hidden_processed'),
        pytest.param(True, 0, 0, NirvanaProcessingTaskState.FINISHED, id='act_not_hidden_act_row_not_hidden_processed'),
    ]
)
def test_in_progress(session, nirvana_block, task_type, load_task_type, act_exists, act_hidden, act_rows_hidden, res_state):
    task = create_task(task_type, NirvanaProcessingTaskState.IN_PROGRESS)
    load_task = create_load_task(load_task_type)

    add_block_input(
        nirvana_block,
        metadata=json.dumps(task.metadata),
        params=json.dumps({'type': task_type.id})
    )

    invoice = create_invoice(session)
    if act_exists:
        act = create_act(invoice)
        if act_hidden:
            act.hide('666')
        act_eid = act.external_id
    else:
        act_eid = gen_act_external_id(session)

    create_act_rows(load_task_type, [(act_eid, invoice.consumes[0], 1, 2, None, act_rows_hidden)])

    with mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)

    session.expire_all()

    if act_exists and act_hidden == act_rows_hidden:
        assert proc_res.is_finished()
    else:
        assert proc_res.is_running()

    assert load_task.state == res_state
    assert_last_task(
        session,
        task_type,
        id=task.id,
        state=res_state,
        metadata={'some': 'meta'},
    )
    assert_invoices_export(ExportState.exported, None, invoice)


def test_finished(session, nirvana_block, task_type, load_task_type):
    task = create_task(task_type, NirvanaProcessingTaskState.FINISHED)
    load_task = create_load_task(load_task_type)

    add_block_input(
        nirvana_block,
        metadata=json.dumps(task.metadata),
        params=json.dumps({'type': task_type.id})
    )

    invoice = create_invoice(session)
    create_act_rows(load_task_type, [('YYY----666', invoice.consumes[0], 1, 2, None, 0)])

    with mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
        proc_res = process(nirvana_block)

    session.expire_all()

    assert proc_res.is_finished()

    assert load_task.state == NirvanaProcessingTaskState.IN_PROGRESS
    assert_last_task(
        session,
        task_type,
        id=task.id,
        state=NirvanaProcessingTaskState.FINISHED,
        metadata={'some': 'meta'},
    )


def test_archived(session, nirvana_block, task_type, load_task_type):
    task = create_task(task_type, NirvanaProcessingTaskState.ARCHIVED)
    create_load_task(load_task_type)

    add_block_input(
        nirvana_block,
        metadata=json.dumps(task.metadata),
        params=json.dumps({'type': task_type.id})
    )

    invoice = create_invoice(session)
    create_act_rows(load_task_type, [('42', invoice.consumes[0], 1, 2, None, 0)])

    with pytest.raises(exc.LOG_TASK_STATE_INVALID) as exc_info:
        with mock_intervals_comparison(NirvanaProcessingAttemptType.PREV):
            process(nirvana_block)

    assert 'archived task' in exc_info.value.msg


@pytest.mark.parametrize(
    'load_task_state',
    [
        NirvanaProcessingTaskState.NEW,
        NirvanaProcessingTaskState.FINISHED,
        NirvanaProcessingTaskState.ARCHIVED,
    ]
)
def test_wrong_state_load_task(session, nirvana_block, task_type, load_task_type, load_task_state):
    task = create_task(task_type, NirvanaProcessingTaskState.FINISHED)
    create_load_task(load_task_type, load_task_state)

    add_block_input(
        nirvana_block,
        metadata=json.dumps(task.metadata),
        params=json.dumps({'type': task_type.id})
    )

    invoice = create_invoice(session)
    create_act_rows(load_task_type, [('42', invoice.consumes[0], 1, 2, None, 0)])

    with pytest.raises(exc.LOG_TASK_ATTEMPT_INVALID):
        with mock_intervals_comparison(NirvanaProcessingAttemptType.NEW):
            process(nirvana_block)


def test_wrong_meta_load_task(session, nirvana_block, task_type, load_task_type):
    create_load_task(load_task_type, NirvanaProcessingTaskState.IN_PROGRESS, {'some': 'meta'})

    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'other meta'}),
        params=json.dumps({'type': task_type.id})
    )

    invoice = create_invoice(session)
    create_act_rows(load_task_type, [('42', invoice.consumes[0], 1, 2, None, 0)])

    with pytest.raises(exc.LOG_TASK_ATTEMPT_INVALID):
        process(nirvana_block)


def test_no_load_task(session, nirvana_block, task_type, load_task_type):
    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some': 'meta'}),
        params=json.dumps({'type': task_type.id})
    )

    invoice = create_invoice(session)
    create_act_rows(load_task_type, [('42', invoice.consumes[0], 1, 2, None, 0)])

    with pytest.raises(exc.LOG_TASK_ATTEMPT_INVALID):
        process(nirvana_block)


def test_invalid_interval(session, nirvana_block, task_type, load_task_type):
    create_task(task_type, NirvanaProcessingTaskState.FINISHED, {'some': 'meta'})
    create_load_task(load_task_type, NirvanaProcessingTaskState.IN_PROGRESS, {'some': 'meta'})

    add_block_input(
        nirvana_block,
        metadata=json.dumps({'some_random': 'meta'}),
        params=json.dumps({'type': task_type.id})
    )

    invoice = create_invoice(session)
    create_act_rows(load_task_type, [('42', invoice.consumes[0], 1, 2, None, 0)])

    with pytest.raises(exc.LOG_TASK_ATTEMPT_INVALID):
        with mock_intervals_comparison(NirvanaProcessingAttemptType.INVALID):
            process(nirvana_block)


@pytest.mark.parametrize(
    'lock_type, cc',
    [
        ('base', 'log_tariff_act_test_locked_type_base'),
        ('load', 'log_tariff_act_test_locked_type_load'),
    ]
)
def test_locked_type(app, session, nirvana_block, lock_type, cc):
    real_session = app.real_new_session()

    type_id = uuid.uuid5(uuid.NAMESPACE_OID, cc).hex
    with real_session.begin():
        task_type = real_session.query(mapper.LogTariffType).filter_by(id=type_id).one_or_none()
        if task_type is None:
            task_type = ob.LogTariffTypeBuilder.construct(real_session, id=type_id)

        load_task_type = real_session.query(mapper.YtLogLoadType).filter_by(cc=type_id).one_or_none()
        if load_task_type is None:
            load_task_type = ob.YTLogLoadTypeBuilder.construct(real_session, cc=type_id)

    add_block_input(
        nirvana_block,
        metadata=json.dumps({'a': 'b'}),
        params=json.dumps({'type': type_id})
    )

    with real_session.begin():
        if lock_type == 'base':
            task_type.lock()
        else:
            load_task_type.lock()

        with pytest.raises(exc.LOG_TASK_IS_LOCKED):
            process(nirvana_block)


def test_client_priority(session, nirvana_block, task_type, load_task_type):
    load_task = create_load_task(load_task_type, NirvanaProcessingTaskState.IN_PROGRESS)

    add_block_input(
        nirvana_block,
        metadata=json.dumps(load_task.metadata),
        params=json.dumps({'type': task_type.id})
    )

    invoices = [
        create_invoice(session, 1),
        create_invoice(session, 1)
    ]
    invoices[0].client.acts_most_valuable_priority = 1
    co1, co2, = itertools.chain.from_iterable(i.consumes for i in invoices)
    create_act_rows(
        load_task_type,
        [
            ('Y-666', co1, 1, 2, None, 0),
            ('Y-667', co2, 3, 4, None, 0),
        ]
    )

    with mock_intervals_comparison(NirvanaProcessingAttemptType.NEW):
        process(nirvana_block)

    session.expire_all()

    hamcrest.assert_that(
        invoices[0],
        hamcrest.has_properties(
            exports=hamcrest.has_entry(
                'LOG_TARIFF_ACT',
                hamcrest.has_properties(
                    state=ExportState.enqueued,
                    priority=-1
                )
            )
        )
    )
    hamcrest.assert_that(
        invoices[1],
        hamcrest.has_properties(
            exports=hamcrest.has_entry(
                'LOG_TARIFF_ACT',
                hamcrest.has_properties(
                    state=ExportState.enqueued,
                    priority=0
                )
            )
        )
    )
