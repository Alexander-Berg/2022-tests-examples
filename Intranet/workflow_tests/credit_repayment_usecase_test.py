from unittest.mock import Mock

import pytest

from staff.departments.tests.factories import VacancyFactory
from staff.headcounts.tests.factories import CreditManagementApplicationFactory

from staff.budget_position.models import BudgetPositionAssignmentStatus
from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.budget_position.workflow_service import CreditRepaymentData
from staff.budget_position.workflow_service.entities import workflows
from staff.budget_position.workflow_service.gateways import BudgetPositionsRepository
from staff.budget_position.workflow_service.use_cases import CreateWorkflowForCreditRepayment


@pytest.mark.django_db
def test_usecase_factory_creates_appropriate_workflow_for_credit_management_with_vacancy_workflow():
    # given
    credit = BudgetPositionAssignmentFactory(status=BudgetPositionAssignmentStatus.RESERVE.value)
    repayment = BudgetPositionAssignmentFactory(status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value)
    vacancy = VacancyFactory(budget_position=repayment.budget_position)

    data = CreditRepaymentData(
        credit_management_id=CreditManagementApplicationFactory().id,
        credit_budget_position=credit.budget_position.code,
        repayment_budget_position=repayment.budget_position.code,
        vacancy_id=vacancy.id,
    )

    use_case = CreateWorkflowForCreditRepayment(Mock(), BudgetPositionsRepository())

    # when
    workflow = use_case._create_workflow(data)

    # then
    assert workflow.code == workflows.WorkflowCreditManagementWithVacancy.code


@pytest.mark.django_db
def test_usecase_creates_appropriate_workflow_for_credit_management_workflow():
    # given
    credit = BudgetPositionAssignmentFactory(status=BudgetPositionAssignmentStatus.RESERVE.value)
    repayment = BudgetPositionAssignmentFactory(status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value)

    data = CreditRepaymentData(
        credit_management_id=CreditManagementApplicationFactory().id,
        credit_budget_position=credit.budget_position.code,
        repayment_budget_position=repayment.budget_position.code,
    )

    use_case = CreateWorkflowForCreditRepayment(Mock(), BudgetPositionsRepository())

    # when
    workflow = use_case._create_workflow(data)

    # then
    assert workflow.code == workflows.WorkflowCreditManagement.code
