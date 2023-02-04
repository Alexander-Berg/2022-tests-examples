from mock import MagicMock

import pytest

from staff.departments.tests.factories import HRProductFactory
from staff.lib.testing import StaffFactory, ValueStreamFactory

from staff.budget_position.models import BudgetPositionAssignmentStatus
from staff.budget_position.tests.utils import ChangeFactory, BudgetPositionAssignmentFactory
from staff.budget_position.tests.workflow_tests.utils import WorkflowModelFactory
from staff.budget_position.workflow_service import FemidaData
from staff.budget_position.workflow_service import entities, gateways


@pytest.fixture
def create_workflow_factory():
    staff_service_mock = MagicMock(spec=gateways.StaffService)
    oebs_service_mock = MagicMock(spec=entities.OEBSService)
    budget_positions_repo = gateways.BudgetPositionsRepository()

    def create_factory(without_checks: bool):
        if without_checks:
            return entities.FemidaWorkflowFactoryWithoutChecks(
                staff_service_mock,
                oebs_service_mock,
                budget_positions_repo,
            )

        return entities.FemidaWorkflowFactory(staff_service_mock, oebs_service_mock, budget_positions_repo)

    return create_factory


@pytest.mark.django_db
@pytest.mark.parametrize('without_checks', [True, False])
def test_workflow_factory_creates_workflow_11_on_new_vacancy_from_femida(
    company,
    create_workflow_factory,
    without_checks,
):
    # given
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
        department=company.yandex,
        value_stream=vs,
    )
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        is_vacancy=True,
    )

    # when
    workflow = create_workflow_factory(without_checks).create(data)

    # then
    assert workflow.code == entities.workflows.Workflow1_1.code


@pytest.mark.django_db
@pytest.mark.parametrize('without_checks', [True, False])
def test_workflow_factory_creates_appropriate_workflow_for_workflow_53(
    company,
    create_workflow_factory,
    without_checks,
):
    # given
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
        department=company.yandex,
        value_stream=vs,
    )
    person = StaffFactory()
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        is_internal_offer=True,
        person_id=person.id,
    )

    # when
    workflow = create_workflow_factory(without_checks).create(data)

    # then
    assert workflow.code == entities.workflows.Workflow5_3.code


@pytest.mark.django_db
@pytest.mark.parametrize('without_checks', [True, False])
def test_workflow_factory_creates_workflow_12_on_new_vacancy_from_femida_wo_budget_position(
    company,
    create_workflow_factory,
    without_checks,
):
    # given
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=None,
        department_id=company.dep1.id,
        is_vacancy=True,
    )
    # when
    workflow = create_workflow_factory(without_checks).create(data)

    # then
    assert workflow.code == entities.workflows.Workflow1_2.code


@pytest.mark.django_db
@pytest.mark.parametrize('without_checks', [True, False])
def test_workflow_factory_creates_workflow_11_on_new_vacancy_from_femida_with_changed_department(
    company,
    create_workflow_factory,
    without_checks,
):
    # given
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
        department=company.yandex,
        value_stream=vs,
    )
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        department_id=company.dep1.id,
        is_vacancy=True,
    )

    # when
    workflow = create_workflow_factory(without_checks).create(data)

    # then
    assert workflow.code == entities.workflows.Workflow1_1.code


@pytest.mark.django_db
@pytest.mark.parametrize('without_checks', [True, False])
def test_workflow_factory_creates_appropriate_workflow_for_workflow_13(
    company,
    create_workflow_factory,
    without_checks,
):
    # given
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
        department=company.yandex,
        value_stream=vs,
    )
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        is_vacancy_cancellation=True,
    )

    # when
    workflow = create_workflow_factory(without_checks).create(data)

    # then
    assert workflow.code == entities.workflows.Workflow1_3.code


@pytest.mark.django_db
@pytest.mark.parametrize('without_checks', [True, False])
def test_workflow_factory_creates_appropriate_workflow_for_workflow_21(
    company,
    create_workflow_factory,
    without_checks,
):
    # given
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        department=company.yandex,
        value_stream=vs,
    )
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        is_vacancy=True,
    )

    # when
    workflow = create_workflow_factory(without_checks).create(data)

    # then
    assert workflow.code == entities.workflows.Workflow2_1.code


@pytest.mark.django_db
@pytest.mark.parametrize('without_checks', [True, False])
def test_workflow_factory_creates_appropriate_workflow_for_workflow_52(
    company,
    create_workflow_factory,
    without_checks,
):
    # given
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.OFFER.value,
        department=company.yandex,
        value_stream=vs,
    )
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        is_offer_rejection=True,
    )

    # when
    workflow = create_workflow_factory(without_checks).create(data)

    # then
    assert workflow.code == entities.workflows.Workflow5_2.code


@pytest.mark.django_db
@pytest.mark.parametrize('without_checks', [True, False])
def test_workflow_factory_creates_appropriate_workflow_for_workflow_51(
    company,
    create_workflow_factory,
    without_checks,
):
    # given
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
        department=company.yandex,
        value_stream=vs,
    )
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        is_offer=True,
    )

    # when
    workflow = create_workflow_factory(without_checks).create(data)

    # then
    assert workflow.code == entities.workflows.Workflow5_1.code


@pytest.mark.django_db
@pytest.mark.parametrize('without_checks', [True, False])
def test_workflow_factory_creates_appropriate_workflow_for_workflow_511(create_workflow_factory, without_checks):
    # given
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
        value_stream=vs,
    )
    data = FemidaData(budget_position_code=assignment.budget_position.code, is_assignment_creation=True)

    # when
    workflow = create_workflow_factory(without_checks).create(data)

    # then
    assert workflow.code == entities.workflows.Workflow5_1_1.code


@pytest.mark.django_db
@pytest.mark.parametrize('without_checks', [True, False])
def test_workflow_factory_doesnt_raise_error_on_workflows_conflict_for_femida(
    company,
    create_workflow_factory,
    without_checks,
):
    # given
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        department=company.yandex,
        value_stream=vs,
    )
    existing_workflow = WorkflowModelFactory()
    ChangeFactory(workflow=existing_workflow, budget_position=assignment.budget_position)
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        is_vacancy=True,
    )

    # when
    assert create_workflow_factory(without_checks).create(data)
