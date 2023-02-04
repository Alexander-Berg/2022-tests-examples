import pytest

from staff.lib.testing import StaffFactory

from staff.budget_position.workflow_service import BudgetPositionsRepository


@pytest.mark.django_db
def test_headcount_position_data_by_persons_ids_no_linked_budget_positions():
    person = StaffFactory(budget_position=None)
    target = BudgetPositionsRepository()

    result = target.headcount_position_data_by_persons_ids([person.id])

    assert result[person.id] is None
