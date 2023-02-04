import pytest

from balance.actions.nirvana.operations.run_mnclose_task import process
from balance.constants import NirvanaTaskStatus


@pytest.fixture(autouse=True)
def setup_run_no_wait(nirvana_block):
    nirvana_block.options['resolve_immediately'] = False
    nirvana_block.options['wait_resolving'] = False


def test_opens_task_if_not_opened_yet(session, nirvana_block, db_task):
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
def test_does_not_change_status_if_it_is_not_new_unopenable(
        session, nirvana_block, db_task, db_task_status,
):
    db_task.status = db_task_status
    session.flush()

    with session.begin():
        process(nirvana_block)

    session.refresh(db_task)
    assert db_task.status == db_task_status


@pytest.mark.parametrize('db_task_status', NirvanaTaskStatus.__all__)
def test_returns_correct_status(
        session, nirvana_block, db_task, db_task_status,
):
    db_task.status = NirvanaTaskStatus.TASK_STATUS_NEW_UNOPENABLE
    session.flush()

    with session.begin():
        result = process(nirvana_block)

    assert result.is_finished()
