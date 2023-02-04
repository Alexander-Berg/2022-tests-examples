from mock import MagicMock

import pytest
from waffle.models import Switch

from staff.lib.testing import OccupationFactory

from staff.budget_position.const import FemidaProfessionalLevel
from staff.budget_position.models import BudgetPositionAssignmentStatus
from staff.budget_position.tests.utils import GradeFactory, BudgetPositionAssignmentFactory
from staff.budget_position.tests.workflow_tests.utils import TableflowMock, OEBSServiceMock
from staff.budget_position.workflow_service import FemidaData
from staff.budget_position.workflow_service import entities, gateways, use_cases


def build_usecase(table_flow: TableflowMock, oebs_service: OEBSServiceMock) -> use_cases.CreateWorkflowFromFemida:
    repository = gateways.WorkflowRepository()
    staff_service = gateways.StaffService()
    grade_calculator = entities.GradeCalculator(oebs_service, staff_service)
    schemes_calculator = entities.SchemesCalculator(table_flow, staff_service, grade_calculator)
    workflow_factory = entities.FemidaWorkflowFactory(staff_service, oebs_service, gateways.BudgetPositionsRepository())

    return use_cases.CreateWorkflowFromFemida(
        repository,
        staff_service,
        MagicMock(spec=use_cases.ConfirmWorkflows),
        grade_calculator,
        schemes_calculator,
        workflow_factory,
        oebs_service,
    )


@pytest.mark.django_db
def test_creation_workflow_from_femida_saves_workflow_to_repo():
    # given
    data = FemidaData()
    some_workflow = MagicMock()
    repo_mock = MagicMock(spec=entities.WorkflowRepositoryInterface)
    workflow_factory = MagicMock(spec=entities.FemidaWorkflowFactory)
    use_case = use_cases.CreateWorkflowFromFemida(
        repo_mock,
        MagicMock(spec=gateways.StaffService),
        MagicMock(spec=use_cases.ConfirmWorkflows),
        MagicMock(spec=entities.GradeCalculator),
        MagicMock(spec=entities.SchemesCalculator),
        workflow_factory,
        MagicMock(spec=entities.OEBSService),
    )
    workflow_factory.create = MagicMock(return_value=some_workflow)

    # when
    use_case.create(data)

    # then
    repo_mock.save.assert_called_once_with(some_workflow)


@pytest.mark.django_db
def test_usecase_calculates_schemes_on_new_offer(company):
    # given
    Switch.objects.get_or_create(name='enable_oebs_requests_for_registry', active=True)
    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    assignment = BudgetPositionAssignmentFactory(
        department=company.dep1,
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
    )

    oebs_service_mock = OEBSServiceMock()

    occupation = OccupationFactory(name='SMM', group_reward='StandartReward')
    expected_review_scheme_id = 1
    expected_bonus_scheme_id = 2
    expected_reward_category = 'Prof'
    expected_reward_scheme_id = 3
    offer_grade_level = 16

    tableflow_service_mock = TableflowMock()
    tableflow_service_mock.add_response_for_review_scheme_id(
        entities.ReviewSchemeIdRequest(occupation.name, company.dep1.id, offer_grade_level),
        expected_review_scheme_id,
    )
    tableflow_service_mock.add_response_for_bonus_scheme_id(
        entities.BonusSchemeIdRequest(company.dep1.id, occupation.name, offer_grade_level),
        expected_bonus_scheme_id,
    )
    tableflow_service_mock.add_response_for_reward_category(
        entities.RewardCategoryRequest(occupation.group_reward, offer_grade_level),
        expected_reward_category,
    )
    tableflow_service_mock.add_response_for_reward_scheme_id(
        entities.RewardSchemeIdRequest(company.dep1.id, expected_reward_category, is_internship=False),
        expected_reward_scheme_id,
    )

    usecase = build_usecase(tableflow_service_mock, oebs_service_mock)

    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        is_offer=True,
        department_id=company.dep1.id,
        occupation_id=occupation.name,
        grade_level=offer_grade_level,
    )

    # when
    workflow_id = usecase.create(data)

    # then
    workflow = gateways.WorkflowRepository().get_by_id(workflow_id)
    assert workflow.code == entities.workflows.Workflow5_1.code
    assert len(workflow.changes) == 1
    change = workflow.changes[0]
    assert change.reward_scheme_id == expected_reward_scheme_id
    assert change.review_scheme_id == expected_review_scheme_id
    assert change.bonus_scheme_id == expected_bonus_scheme_id


@pytest.mark.django_db
def test_usecase_calculates_schemes_on_new_vacancy(company):
    # given
    Switch.objects.get_or_create(name='enable_oebs_requests_for_registry', active=True)
    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    assignment = BudgetPositionAssignmentFactory(
        department=company.dep1,
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
    )

    occupation = OccupationFactory(name='SMM', group_reward='StandartReward')
    offer_grade_level = 16
    grade = GradeFactory(occupation=occupation, level=offer_grade_level)
    expected_review_scheme_id = 1
    expected_bonus_scheme_id = 2
    expected_reward_category = 'Prof'
    expected_reward_scheme_id = 3

    oebs_service_mock = OEBSServiceMock()
    oebs_service_mock.add_grade_id(occupation.name, grade.level, grade.grade_id)
    oebs_service_mock.add_mapping_for_femida_level(occupation.name, FemidaProfessionalLevel.middle, offer_grade_level)

    tableflow_service_mock = TableflowMock()
    tableflow_service_mock.add_response_for_review_scheme_id(
        entities.ReviewSchemeIdRequest(occupation.name, company.dep1.id, offer_grade_level),
        expected_review_scheme_id,
    )
    tableflow_service_mock.add_response_for_bonus_scheme_id(
        entities.BonusSchemeIdRequest(company.dep1.id, occupation.name, offer_grade_level),
        expected_bonus_scheme_id,
    )
    tableflow_service_mock.add_response_for_reward_category(
        entities.RewardCategoryRequest(occupation.group_reward, offer_grade_level),
        expected_reward_category,
    )
    tableflow_service_mock.add_response_for_reward_scheme_id(
        entities.RewardSchemeIdRequest(company.dep1.id, expected_reward_category, is_internship=False),
        expected_reward_scheme_id,
    )

    usecase = build_usecase(tableflow_service_mock, oebs_service_mock)

    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        department_id=company.dep1.id,
        occupation_id=occupation.name,
        professional_level=FemidaProfessionalLevel.middle,
        is_vacancy=True,
    )

    # when
    workflow_id = usecase.create(data)

    # then
    workflow = gateways.WorkflowRepository().get_by_id(workflow_id)
    assert workflow.code == entities.workflows.Workflow1_1.code
    assert len(workflow.changes) == 1
    change = workflow.changes[0]
    assert change.reward_scheme_id == expected_reward_scheme_id
    assert change.review_scheme_id == expected_review_scheme_id
    assert change.bonus_scheme_id == expected_bonus_scheme_id
    assert change.grade_id == grade.grade_id


@pytest.mark.django_db
def test_usecase_calculates_schemes_on_new_credit_vacancy(company):
    # given
    Switch.objects.get_or_create(name='enable_oebs_requests_for_registry', active=True)
    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    occupation = OccupationFactory(name='SMM', group_reward='StandartReward')
    offer_grade_level = 16
    grade = GradeFactory(occupation=occupation, level=offer_grade_level)
    expected_review_scheme_id = 1
    expected_bonus_scheme_id = 2
    expected_reward_category = 'Prof'
    expected_reward_scheme_id = 3

    oebs_service_mock = OEBSServiceMock()
    oebs_service_mock.add_grade_id(occupation.name, grade.level, grade.grade_id)
    oebs_service_mock.add_mapping_for_femida_level(occupation.name, FemidaProfessionalLevel.middle, offer_grade_level)
    oebs_service_mock.set_grade_data(grade.grade_id, entities.GradeData(occupation.pk, offer_grade_level))

    tableflow_service_mock = TableflowMock()
    tableflow_service_mock.add_response_for_review_scheme_id(
        entities.ReviewSchemeIdRequest(occupation.name, company.dep1.id, offer_grade_level),
        expected_review_scheme_id,
    )
    tableflow_service_mock.add_response_for_bonus_scheme_id(
        entities.BonusSchemeIdRequest(company.dep1.id, occupation.name, offer_grade_level),
        expected_bonus_scheme_id,
    )
    tableflow_service_mock.add_response_for_reward_category(
        entities.RewardCategoryRequest(occupation.group_reward, offer_grade_level),
        expected_reward_category,
    )
    tableflow_service_mock.add_response_for_reward_scheme_id(
        entities.RewardSchemeIdRequest(company.dep1.id, expected_reward_category, is_internship=False),
        expected_reward_scheme_id,
    )

    usecase = build_usecase(tableflow_service_mock, oebs_service_mock)

    data = FemidaData(
        vacancy_id=100500,
        department_id=company.dep1.id,
        occupation_id=occupation.name,
        professional_level=FemidaProfessionalLevel.middle,
        is_vacancy=True,
    )

    # when
    workflow_id = usecase.create(data)

    # then
    workflow = gateways.WorkflowRepository().get_by_id(workflow_id)
    assert workflow.code == entities.workflows.Workflow1_2.code
    assert len(workflow.changes) == 2
    first_change = workflow.changes[0]
    assert first_change.reward_scheme_id == expected_reward_scheme_id
    assert first_change.review_scheme_id == expected_review_scheme_id
    assert first_change.bonus_scheme_id == expected_bonus_scheme_id
    assert first_change.grade_id == grade.grade_id
    last = workflow.changes[1]
    assert last.reward_scheme_id == expected_reward_scheme_id
    assert last.review_scheme_id == expected_review_scheme_id
    assert last.bonus_scheme_id == expected_bonus_scheme_id
    assert last.grade_id == grade.grade_id
