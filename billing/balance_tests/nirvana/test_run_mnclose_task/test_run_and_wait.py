import pytest

from balance.actions.nirvana.operations.run_mnclose_task import process
from balance.actions.nirvana.status import (
    NirvanaBlockStatusRunning, NirvanaBlockStatusWaiting, NirvanaBlockStatusFinished,
)
from balance.constants import NirvanaTaskStatus


@pytest.fixture(autouse=True)
def setup_run_and_wait(nirvana_block):
    nirvana_block.options['resolve_immediately'] = False
    nirvana_block.options['wait_resolving'] = True


def test_opens_db_task_if_not_opened_yet(session, nirvana_block, db_task):
    db_task.status = NirvanaTaskStatus.TASK_STATUS_NEW_UNOPENABLE
    session.flush()

    with session.begin():
        process(nirvana_block)

    session.refresh(db_task)
    assert db_task.status == NirvanaTaskStatus.TASK_STATUS_NEW_OPENABLE


@pytest.mark.parametrize('db_task_status', [
    NirvanaTaskStatus.TASK_STATUS_NEW_OPENABLE,
    NirvanaTaskStatus.TASK_STATUS_OPENED,
    NirvanaTaskStatus.TASK_STATUS_STALLED,
    NirvanaTaskStatus.TASK_STATUS_RESOLVED,
])
def test_does_not_change_db_task_status_if_it_is_not_new_unopenable(
        session, nirvana_block, db_task, db_task_status,
):
    db_task.status = db_task_status
    session.flush()

    with session.begin():
        process(nirvana_block)

    session.refresh(db_task)
    assert db_task.status == db_task_status


@pytest.mark.parametrize('db_task_status, expected_rv_cls', [
    (NirvanaTaskStatus.TASK_STATUS_NEW_UNOPENABLE, NirvanaBlockStatusRunning),
    (NirvanaTaskStatus.TASK_STATUS_NEW_OPENABLE, NirvanaBlockStatusRunning),
    (NirvanaTaskStatus.TASK_STATUS_OPENED, NirvanaBlockStatusRunning),
    (NirvanaTaskStatus.TASK_STATUS_STALLED, NirvanaBlockStatusWaiting),
    (NirvanaTaskStatus.TASK_STATUS_RESOLVED, NirvanaBlockStatusFinished),
])
def test_returns_correct_status(
        session, nirvana_block, db_task, db_task_status, expected_rv_cls,
):
    db_task.status = db_task_status
    session.flush()

    with session.begin():
        result = process(nirvana_block)

    assert isinstance(result, expected_rv_cls)


def test_uses_delay_minutes_from_block_in_running_status(
        session, nirvana_block, db_task,
):
    nirvana_block.options['check_delay'] = 17
    db_task.status = NirvanaTaskStatus.TASK_STATUS_NEW_UNOPENABLE
    session.flush()

    with session.begin():
        result = process(nirvana_block)

    assert result.is_running()
    assert result.options['delay_minutes'] == 17


def test_uses_delay_minutes_from_block_in_waiting_status(
        session, nirvana_block, db_task,
):
    nirvana_block.options['check_delay'] = 17
    db_task.status = NirvanaTaskStatus.TASK_STATUS_STALLED
    session.flush()

    with session.begin():
        result = process(nirvana_block)

    assert result.is_waiting()
    assert result.options['delay_minutes'] == 17
