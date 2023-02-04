import uuid

from staff.budget_position.workflow_service import entities


def test_get_next_change_will_return_nothing_on_absent_budget_position():
    # given
    change = entities.Change()
    workflow = entities.workflows.Workflow1_1_1(uuid.uuid1(), [change])

    # when
    result = workflow.get_next_change_for_sending()

    # then
    assert result is None


def test_get_next_change_will_return_change_on_present_budget_position():
    # given
    change = entities.Change(budget_position=entities.BudgetPosition(1, 100500))
    workflow = entities.workflows.Workflow1_1_1(uuid.uuid1(), [change])

    # when
    result = workflow.get_next_change_for_sending()

    # then
    assert result is not None
