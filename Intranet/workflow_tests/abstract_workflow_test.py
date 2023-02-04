from uuid import uuid1

from staff.budget_position.workflow_service.entities.abstract_workflow import AbstractWorkflow
from staff.budget_position.workflow_service.entities.change import Change
from staff.budget_position.const import PUSH_STATUS


def test_is_finished_empty():
    workflow = AbstractWorkflow(uuid1(), [])

    assert workflow.is_finished()


def test_is_finished_not_started_at_all():
    workflow = AbstractWorkflow(
        uuid1(),
        [
            create_change(None),
        ],
    )

    assert not workflow.is_finished()


def test_is_finished_has_not_started_with_errors():
    workflow = AbstractWorkflow(
        uuid1(),
        [
            create_change(PUSH_STATUS.ERROR),
            create_change(PUSH_STATUS.FINISHED),
            create_change(None),
        ],
    )

    assert workflow.is_finished()


def test_is_finished_pushed_with_errors():
    workflow = AbstractWorkflow(
        uuid1(),
        [
            create_change(PUSH_STATUS.ERROR),
            create_change(PUSH_STATUS.FINISHED),
            create_change(PUSH_STATUS.PUSHED),
        ],
    )

    assert workflow.is_finished()


def test_is_finished_error_or_finished():
    workflow = AbstractWorkflow(
        uuid1(),
        [
            create_change(PUSH_STATUS.ERROR),
            create_change(PUSH_STATUS.FINISHED),
        ],
    )

    assert workflow.is_finished()


def test_is_finished_only_finished():
    workflow = AbstractWorkflow(
        uuid1(),
        [
            create_change(PUSH_STATUS.FINISHED),
        ],
    )

    assert workflow.is_finished()


def create_change(push_status):
    return Change(push_status=push_status, effective_date=1)
