import pytest
from mock import MagicMock
from waffle.models import Switch

from staff.lib.testing import StaffFactory

from staff.budget_position import const
from staff.budget_position.workflow_service import WorkflowInvalidStateError
from staff.budget_position.workflow_service import gateways, entities, use_cases, FemidaData
from staff.budget_position.tests.workflow_tests.utils import WorkflowModelFactory
from staff.budget_position.tests.utils import ChangeFactory


@pytest.mark.django_db
def test_workflow_raises_exception_on_trying_to_set_manually_processed_on_workflow_in_pushed_state():
    # given
    existing_workflow_id = WorkflowModelFactory(status=const.WORKFLOW_STATUS.PUSHED).id
    ChangeFactory(workflow_id=existing_workflow_id)
    existing_workflow = gateways.WorkflowRepository().get_by_id(existing_workflow_id)

    # when
    with pytest.raises(WorkflowInvalidStateError) as exception_info:
        existing_workflow.mark_manually_processed(StaffFactory())

    # then
    assert exception_info.value.workflow_id == existing_workflow_id


@pytest.mark.django_db
def test_workflow_raises_exception_on_trying_to_set_manually_processed_on_workflow_in_cancelled_state():
    # given
    existing_workflow_id = WorkflowModelFactory(status=const.WORKFLOW_STATUS.CANCELLED).id
    ChangeFactory(workflow_id=existing_workflow_id)
    existing_workflow = gateways.WorkflowRepository().get_by_id(existing_workflow_id)

    # when
    with pytest.raises(WorkflowInvalidStateError) as exception_info:
        existing_workflow.mark_manually_processed(StaffFactory())

    # then
    assert exception_info.value.workflow_id == existing_workflow_id


@pytest.mark.django_db
def test_workflow_11_can_ask_tableflow_service_for_review_scheme(company):
    # given
    table_flow_mock = MagicMock(spec=entities.TableflowService)
    table_flow_mock.review_scheme_id = MagicMock(return_value=[100500])
    workflow_repo_mock = MagicMock(spec=entities.WorkflowRepositoryInterface)

    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    data = FemidaData(
        department_id=company.dep111.id,
        occupation_id='SOMEOCCUPATION',
        grade_level=16,
        is_vacancy=True,
        budget_position_code=1,
    )

    staff_service = MagicMock(spec=entities.StaffService)
    oebs_service = MagicMock(spec=entities.OEBSService)
    budget_position_repo = MagicMock(spec=entities.BudgetPositionsRepository)
    workflow_factory = entities.FemidaWorkflowFactory(staff_service, oebs_service, budget_position_repo)
    grade_calculator = entities.GradeCalculator(oebs_service, staff_service)
    schemes_calculator = entities.SchemesCalculator(table_flow_mock, staff_service, grade_calculator)

    usecase = use_cases.CreateWorkflowFromFemida(
        workflow_repo_mock,
        staff_service,
        MagicMock(spec=use_cases.ConfirmWorkflows),
        grade_calculator,
        schemes_calculator,
        workflow_factory,
        oebs_service,
    )
    workflow_factory._changes_existing_budget_position = MagicMock(return_value=True)
    workflow_factory._changes_vacancy_plan_budget_position = MagicMock(return_value=True)

    # when
    usecase.create(data)

    # then
    workflow = workflow_repo_mock.save.call_args[0][0]
    assert len(workflow.changes) == 1
    assert workflow.changes[0].review_scheme_id == 100500
