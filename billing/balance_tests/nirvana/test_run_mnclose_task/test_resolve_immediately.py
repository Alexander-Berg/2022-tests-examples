import pytest

from balance.actions.nirvana.operations.run_mnclose_task import process
from balance.constants import NirvanaTaskStatus


@pytest.fixture(autouse=True)
def setup_resolve_immediately(nirvana_block):
    nirvana_block.options['resolve_immediately'] = True


@pytest.mark.parametrize('db_task_status', NirvanaTaskStatus.__all__)
def test_changes_db_task_status_and_returns_correct_result(session, nirvana_block, db_task, db_task_status):
    db_task.status = db_task_status
    session.flush()

    with session.begin():
        result = process(nirvana_block)

    session.refresh(db_task)
    assert db_task.status == NirvanaTaskStatus.TASK_STATUS_RESOLVED
    assert result.is_finished()
