import pytest

from staff.departments.tests.factories import HRProductFactory

from staff.budget_position.controllers import check_if_hr_product_changed_controller
from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory


@pytest.mark.django_db
def test_check_if_hr_product_changed_controller_new_hr_product():
    budget_position_assignment = BudgetPositionAssignmentFactory()
    HRProductFactory(value_stream=budget_position_assignment.value_stream)

    result = check_if_hr_product_changed_controller(budget_position_assignment.budget_position, HRProductFactory())

    assert result is True


@pytest.mark.django_db
def test_check_if_hr_product_changed_controller_old_hr_product():
    budget_position_assignment = BudgetPositionAssignmentFactory()
    old_hr_product = HRProductFactory(value_stream=budget_position_assignment.value_stream)

    result = check_if_hr_product_changed_controller(budget_position_assignment.budget_position, old_hr_product)

    assert result is False


@pytest.mark.django_db
def test_check_if_hr_product_changed_controller_no_current_hr_product():
    budget_position_assignment = BudgetPositionAssignmentFactory()

    result = check_if_hr_product_changed_controller(budget_position_assignment.budget_position, HRProductFactory())

    assert result is True


@pytest.mark.django_db
def test_check_if_hr_product_changed_controller_no_current_hr_product_and_no_new():
    budget_position_assignment = BudgetPositionAssignmentFactory()

    result = check_if_hr_product_changed_controller(budget_position_assignment.budget_position, None)

    assert result is False


@pytest.mark.django_db
def test_check_if_hr_product_changed_controller_many_hr_products():
    budget_position_assignment = BudgetPositionAssignmentFactory()
    old_hr_product = HRProductFactory(value_stream=budget_position_assignment.value_stream)
    other_budget_position_assignment = BudgetPositionAssignmentFactory(
        budget_position=budget_position_assignment.budget_position,
    )
    HRProductFactory(value_stream=other_budget_position_assignment.value_stream)

    result = check_if_hr_product_changed_controller(budget_position_assignment.budget_position, old_hr_product)

    assert result is True
