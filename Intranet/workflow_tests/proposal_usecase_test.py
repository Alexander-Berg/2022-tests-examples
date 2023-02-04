from datetime import date
from typing import Any, Optional

import attr
from lagom import Container, Singleton
import pytest
from waffle.models import Switch

from staff.departments.models import HeadcountPosition
from staff.departments.tests.factories import ProposalMetadataFactory, VacancyFactory
from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.lib.testing import (
    BudgetPositionFactory,
    OccupationFactory,
    OfficeFactory,
    OrganizationFactory,
    PlacementFactory,
    StaffFactory,
)
from staff.lib.tests.pytest_fixtures import AttrDict
from staff.map.models import Office, Placement
from staff.oebs.constants import PERSON_POSITION_STATUS
from staff.person.models import Organization, Staff

from staff.budget_position.tests.utils import ChangeFactory, GradeFactory
from staff.budget_position.tests.workflow_tests.utils import (
    FemidaServiceMock,
    OEBSServiceMock,
    StaffServiceMock,
    TableflowMock,
    WorkflowModelFactory,
)
from staff.budget_position.workflow_service import entities, gateways, use_cases


@pytest.fixture
def oebs_service_mock() -> OEBSServiceMock:
    return OEBSServiceMock()


@pytest.fixture
def table_flow_service_mock() -> TableflowMock:
    return TableflowMock()


@pytest.fixture
def container(oebs_service_mock, table_flow_service_mock) -> Container:
    workflow_service_container = Container()
    workflow_service_container[entities.AbstractProposalWorkflowFactory] = Singleton(entities.ProposalWorkflowFactory)
    workflow_service_container[entities.WorkflowRepositoryInterface] = Singleton(gateways.WorkflowRepository)
    workflow_service_container[entities.BudgetPositionsRepository] = Singleton(gateways.BudgetPositionsRepository)
    workflow_service_container[entities.StaffService] = Singleton(gateways.StaffService)
    workflow_service_container[entities.OEBSService] = oebs_service_mock
    workflow_service_container[use_cases.OebsHireService] = Singleton(gateways.OebsHireService)
    workflow_service_container[use_cases.CreditManagementServiceInterface] = Singleton(gateways.CreditManagementService)
    workflow_service_container[entities.FemidaService] = Singleton(FemidaServiceMock)
    workflow_service_container[entities.TableflowService] = table_flow_service_mock
    return workflow_service_container


@pytest.mark.django_db
def test_usecase_returns_workflows_conflict_for_proposal(company: AttrDict, container: Container) -> None:
    # given
    budget_position = BudgetPositionFactory()
    workflow = WorkflowModelFactory(proposal=ProposalMetadataFactory())
    ChangeFactory(workflow=workflow, budget_position=budget_position)
    HeadcountPositionFactory(
        status=PERSON_POSITION_STATUS.OCCUPIED,
        department=company.yandex,
        code=budget_position.code,
    )
    data = entities.ProposalData(
        proposal_id=1,
        ticket='TJOB-1',
        is_move_with_budget_position=True,
        person_id=StaffFactory(budget_position=budget_position).id,
        proposal_changes=[entities.ProposalChange()],
    )
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    _, conflicting_workflows = use_case.create([data])

    # then
    assert len(conflicting_workflows) == 1
    assert conflicting_workflows[0].code == entities.workflows.Workflow1_1.code


@pytest.mark.django_db
def test_factory_creates_appropriate_workflow_for_workflow_7_1(company: AttrDict, container: Container) -> None:
    # given
    budget_position_model = BudgetPositionFactory()
    budget_position = entities.BudgetPosition(id=budget_position_model.id, code=budget_position_model.code)
    HeadcountPositionFactory(
        status=PERSON_POSITION_STATUS.OCCUPIED,
        department=company.yandex,
        code=budget_position_model.code,
    )
    data = entities.ProposalData(
        proposal_id=1,
        ticket='TJOB-1',
        is_move_with_budget_position=True,
        person_id=StaffFactory().id,
        proposal_changes=[entities.ProposalChange()],
    )
    proposal_workflow_factory = container[entities.ProposalWorkflowFactory]

    # when
    workflow = proposal_workflow_factory.create_workflow(
        data,
        entities.BudgetPositionMove(budget_position, budget_position),
    )

    # then
    assert workflow.code == entities.workflows.Workflow7_1.code


@pytest.mark.django_db
def test_factory_creates_appropriate_workflow_for_workflow_1_1_1(company: AttrDict, container: Container) -> None:
    # given
    budget_position_model = BudgetPositionFactory()
    budget_position = entities.BudgetPosition(id=budget_position_model.id, code=budget_position_model.code)
    HeadcountPositionFactory(
        status=PERSON_POSITION_STATUS.VACANCY_OPEN,
        department=company.yandex,
        code=budget_position_model.code,
    )
    vacancy = VacancyFactory(budget_position=budget_position_model)
    data = entities.ProposalData(
        proposal_id=1,
        ticket='TJOB-1',
        vacancy_id=vacancy.id,
        is_move_with_budget_position=True,
        proposal_changes=[entities.ProposalChange()],
    )
    proposal_workflow_factory = container[entities.ProposalWorkflowFactory]

    # when
    workflow = proposal_workflow_factory.create_workflow(
        data,
        entities.BudgetPositionMove(budget_position, budget_position),
    )

    # then
    assert workflow.code == entities.workflows.Workflow1_1_1.code


@pytest.mark.django_db
def test_factory_appropriate_workflow_for_moving_headcount(company: AttrDict, container: Container) -> None:
    # given
    budget_position_model = BudgetPositionFactory()
    budget_position = entities.BudgetPosition(id=budget_position_model.id, code=budget_position_model.code)

    HeadcountPositionFactory(
        status=PERSON_POSITION_STATUS.VACANCY_PLAN,
        department=company.yandex,
        code=budget_position_model.code,
    )

    data = entities.ProposalData(
        proposal_id=1,
        ticket='TJOB-1',
        vacancy_id=None,
        is_move_with_budget_position=True,
        headcount_position_code=budget_position_model.code,
        proposal_changes=[entities.ProposalChange(department_id=company.dep1.id)],
    )
    proposal_workflow_factory = container[entities.ProposalWorkflowFactory]

    # when
    workflow = proposal_workflow_factory.create_workflow(
        data,
        entities.BudgetPositionMove(budget_position, budget_position),
    )

    # then
    assert workflow.code == entities.workflows.HeadcountChangeFromProposalWorkflow.code


@pytest.mark.django_db
def test_workflow_71_backed_by_workflow_7_100500(company: AttrDict, container: Container) -> None:
    # given
    data = entities.ProposalData(
        proposal_id=1,
        ticket='TJOB-1',
        person_id=StaffFactory().id,
        proposal_changes=[entities.ProposalChange()],
        is_move_with_budget_position=True,
    )
    proposal_workflow_factory = container[entities.ProposalWorkflowFactory]

    # when
    workflow = proposal_workflow_factory.create_workflow(data, entities.BudgetPositionMove(None, None))

    # then
    assert workflow.code == entities.workflows.Workflow7_100500.code


def prepare_data_for_placement_tests(company: AttrDict) -> Any:
    class Data:
        proposal_data: Optional[entities.ProposalData] = None
        staff: Optional[Staff] = None
        prev_office: Optional[Office] = None
        new_office: Optional[Office] = None
        prev_organization: Optional[Organization] = None
        new_organization: Optional[Organization] = None
        placement_prev_prev: Optional[Placement] = None
        placement_prev_new: Optional[Placement] = None
        placement_new_prev: Optional[Placement] = None
        placement_new_new: Optional[Placement] = None
        occupied_headcount: Optional[HeadcountPosition] = None
        budget_position_move: Optional[entities.BudgetPositionMove] = None

    data = Data()

    data.prev_office = OfficeFactory()
    data.new_office = OfficeFactory()
    data.prev_organization = OrganizationFactory()
    data.new_organization = OrganizationFactory()

    data.placement_prev_prev = PlacementFactory(office=data.prev_office, organization=data.prev_organization)
    data.placement_prev_new = PlacementFactory(office=data.prev_office, organization=data.new_organization)
    data.placement_new_prev = PlacementFactory(office=data.new_office, organization=data.prev_organization)
    data.placement_new_new = PlacementFactory(office=data.new_office, organization=data.new_organization)

    data.staff = StaffFactory(office=data.prev_office, organization=data.prev_organization)
    data.occupied_headcount = HeadcountPositionFactory(
        status=PERSON_POSITION_STATUS.OCCUPIED,
        code=data.staff.budget_position.code,
        department=company.yandex,
    )

    data.proposal_data = entities.ProposalData(
        proposal_id=1,
        ticket='TJOB-1',
        person_id=data.staff.id,
        is_move_with_budget_position=True,
    )

    data.budget_position_move = entities.BudgetPositionMove(
        old_budget_position=entities.BudgetPosition(data.staff.budget_position.id, data.staff.budget_position.code),
        new_budget_position=entities.BudgetPosition(data.staff.budget_position.id, data.staff.budget_position.code),
    )

    return data


@pytest.mark.django_db
def test_factory_adds_appropriate_placement_when_office_changing(company: AttrDict, container: Container) -> None:
    # given
    data = prepare_data_for_placement_tests(company)
    proposal_data = attr.evolve(
        data.proposal_data,
        proposal_changes=[entities.ProposalChange(
            office_id=data.new_office.id,
            oebs_date=date(2020, 2, 10),
        )]
    )
    proposal_workflow_factory = container[entities.ProposalWorkflowFactory]

    # when
    workflow = proposal_workflow_factory.create_workflow(proposal_data, data.budget_position_move)

    # then
    assert len(workflow.changes_by_effective_date) == 1
    assert workflow.changes_by_effective_date[0].placement_id == data.placement_new_prev.id
    assert workflow.changes_by_effective_date[0].office_id == data.new_office.id
    assert workflow.changes_by_effective_date[0].organization_id is None


@pytest.mark.django_db
def test_factory_adds_appropriate_placement_when_organization_changing(company: AttrDict, container: Container) -> None:
    # given
    data = prepare_data_for_placement_tests(company)
    proposal_data = attr.evolve(
        data.proposal_data,
        proposal_changes=[entities.ProposalChange(
            organization_id=data.new_organization.id,
            oebs_date=date(2020, 2, 10),
        )],
    )
    proposal_workflow_factory = container[entities.ProposalWorkflowFactory]

    # when
    workflow = proposal_workflow_factory.create_workflow(proposal_data, data.budget_position_move)

    # then
    assert len(workflow.changes_by_effective_date) == 1
    assert workflow.changes_by_effective_date[0].placement_id == data.placement_prev_new.id
    assert workflow.changes_by_effective_date[0].office_id is None
    assert workflow.changes_by_effective_date[0].organization_id == data.new_organization.id


@pytest.mark.django_db
def test_factory_adds_appropriate_placement_when_organization_and_office_changing_at_the_same_date(
    company: AttrDict,
    container: Container,
) -> None:
    # given
    data = prepare_data_for_placement_tests(company)
    proposal_data = attr.evolve(
        data.proposal_data,
        proposal_changes=[entities.ProposalChange(
            office_id=data.new_office.id,
            organization_id=data.new_organization.id,
            oebs_date=date(2020, 2, 10),
        )],
    )
    proposal_workflow_factory = container[entities.ProposalWorkflowFactory]

    # when
    workflow = proposal_workflow_factory.create_workflow(proposal_data, data.budget_position_move)

    # then
    assert len(workflow.changes_by_effective_date) == 1
    assert workflow.changes_by_effective_date[0].placement_id == data.placement_new_new.id
    assert workflow.changes_by_effective_date[0].office_id == data.new_office.id
    assert workflow.changes_by_effective_date[0].organization_id == data.new_organization.id


@pytest.mark.django_db
def test_factory_adds_appropriate_placement_when_office_changing_before_organization(
    company: AttrDict,
    container: Container,
) -> None:
    # given
    data = prepare_data_for_placement_tests(company)
    proposal_data = attr.evolve(
        data.proposal_data,
        proposal_changes=[
            entities.ProposalChange(
                organization_id=data.new_organization.id,
                oebs_date=date(2020, 2, 10),
            ),
            entities.ProposalChange(
                office_id=data.new_office.id,
                oebs_date=date(2020, 2, 11),
            ),
        ],
    )
    proposal_workflow_factory = container[entities.ProposalWorkflowFactory]

    # when
    workflow = proposal_workflow_factory.create_workflow(proposal_data, data.budget_position_move)

    # then
    assert len(workflow.changes_by_effective_date) == 2

    assert workflow.changes_by_effective_date[0].placement_id == data.placement_prev_new.id
    assert workflow.changes_by_effective_date[0].office_id is None
    assert workflow.changes_by_effective_date[0].organization_id == data.new_organization.id

    assert workflow.changes_by_effective_date[1].placement_id == data.placement_new_new.id
    assert workflow.changes_by_effective_date[1].office_id == data.new_office.id
    assert workflow.changes_by_effective_date[1].organization_id is None


@pytest.mark.django_db
def test_factory_adds_appropriate_placement_when_organization_changing_before_office(
    company: AttrDict,
    container: Container,
) -> None:
    # given
    data = prepare_data_for_placement_tests(company)
    proposal_data = attr.evolve(
        data.proposal_data,
        proposal_changes=[
            entities.ProposalChange(office_id=data.new_office.id, oebs_date=date(2020, 2, 10)),
            entities.ProposalChange(organization_id=data.new_organization.id, oebs_date=date(2020, 2, 11)),
        ],
    )
    proposal_workflow_factory = container[entities.ProposalWorkflowFactory]

    # when
    workflow = proposal_workflow_factory.create_workflow(proposal_data, data.budget_position_move)

    # then
    assert len(workflow.changes_by_effective_date) == 2

    assert workflow.changes_by_effective_date[0].placement_id == data.placement_new_prev.id
    assert workflow.changes_by_effective_date[0].office_id == data.new_office.id
    assert workflow.changes_by_effective_date[0].organization_id is None

    assert workflow.changes_by_effective_date[1].placement_id == data.placement_new_new.id
    assert workflow.changes_by_effective_date[1].office_id is None
    assert workflow.changes_by_effective_date[1].organization_id == data.new_organization.id


@pytest.mark.django_db
def test_factory_adds_appropriate_placement_when_more_than_two_changes(
    company: AttrDict,
    container: Container,
) -> None:
    # given
    data = prepare_data_for_placement_tests(company)
    proposal_data = attr.evolve(data.proposal_data, proposal_changes=[
        entities.ProposalChange(office_id=data.new_office.id, oebs_date=date(2020, 2, 10)),
        entities.ProposalChange(currency='USD', oebs_date=date(2020, 2, 11)),
        entities.ProposalChange(organization_id=data.new_organization.id, oebs_date=date(2020, 2, 12)),
    ])
    proposal_workflow_factory = container[entities.ProposalWorkflowFactory]

    # when
    workflow = proposal_workflow_factory.create_workflow(proposal_data, data.budget_position_move)

    # then
    assert len(workflow.changes_by_effective_date) == 3

    assert workflow.changes_by_effective_date[0].placement_id == data.placement_new_prev.id
    assert workflow.changes_by_effective_date[0].office_id == data.new_office.id
    assert workflow.changes_by_effective_date[0].organization_id is None

    assert workflow.changes_by_effective_date[1].placement_id is None
    assert workflow.changes_by_effective_date[1].office_id is None
    assert workflow.changes_by_effective_date[1].organization_id is None

    assert workflow.changes_by_effective_date[2].placement_id == data.placement_new_new.id
    assert workflow.changes_by_effective_date[2].office_id is None
    assert workflow.changes_by_effective_date[2].organization_id == data.new_organization.id


@pytest.mark.django_db
def test_will_recalculate_schemes_on_force(
    company: AttrDict,
    container: Container,
    oebs_service_mock: OEBSServiceMock,
    table_flow_service_mock: TableflowMock,
) -> None:
    # given
    Switch.objects.get_or_create(name='enable_oebs_requests_for_registry', active=True)
    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    occupation = OccupationFactory(name='SMM', group_reward='StandartReward')
    expected_review_scheme_id = 1
    expected_bonus_scheme_id = 2
    expected_reward_category = 'Prof'
    expected_reward_scheme_id = 3
    person_grade = 16

    dep111_person = company.persons['dep111-person']
    data = entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        ticket='TJOB-1',
        is_move_with_budget_position=True,
        person_id=dep111_person.id,
        proposal_changes=[entities.ProposalChange(force_recalculate_schemes=True)],
    )

    oebs_service_mock.set_grades_data_response(dep111_person.login, entities.GradeData(occupation.name, person_grade))

    table_flow_service_mock.add_response_for_review_scheme_id(
        entities.ReviewSchemeIdRequest(occupation.name, dep111_person.department.id, person_grade),
        expected_review_scheme_id,
    )
    table_flow_service_mock.add_response_for_bonus_scheme_id(
        entities.BonusSchemeIdRequest(dep111_person.department.id, occupation.name, person_grade),
        expected_bonus_scheme_id,
    )
    table_flow_service_mock.add_response_for_reward_category(
        entities.RewardCategoryRequest(occupation.group_reward, person_grade),
        expected_reward_category,
    )
    table_flow_service_mock.add_response_for_reward_scheme_id(
        entities.RewardSchemeIdRequest(dep111_person.department.id, expected_reward_category, is_internship=False),
        expected_reward_scheme_id,
    )
    workflow_repository = gateways.WorkflowRepository()
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    workflow_id = use_case.create([data])[0][0]

    # then
    workflow = workflow_repository.get_by_id(workflow_id)
    assert len(workflow.changes) == 1
    change = workflow.changes[0]
    assert change.review_scheme_id == expected_review_scheme_id
    assert change.bonus_scheme_id == expected_bonus_scheme_id
    assert change.reward_scheme_id == expected_reward_scheme_id


@pytest.mark.django_db
def test_will_recalculate_grade_id_on_grade_change(
    company: AttrDict,
    container: Container,
    oebs_service_mock: OEBSServiceMock,
    table_flow_service_mock: TableflowMock,
) -> None:
    # given
    Switch.objects.get_or_create(name='enable_oebs_requests_for_registry', active=True)
    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    occupation = OccupationFactory(name='SMM', group_reward='StandartReward')
    current_grade_data = entities.GradeData('SMM', 16)
    new_grade = GradeFactory(occupation=occupation, level=17)
    expected_review_scheme_id = 1
    expected_bonus_scheme_id = 2
    expected_reward_category = 'Prof'
    expected_reward_scheme_id = 3

    dep111_person = company.persons['dep111-person']
    data = entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        ticket='TJOB-1',
        is_move_with_budget_position=True,
        person_id=dep111_person.id,
        proposal_changes=[entities.ProposalChange(grade_change='+1')],
    )

    oebs_service_mock.set_grades_data_response(dep111_person.login, current_grade_data)
    oebs_service_mock.add_grade_id(occupation.pk, new_grade.level, new_grade.grade_id)

    table_flow_service_mock.add_response_for_review_scheme_id(
        entities.ReviewSchemeIdRequest(occupation.name, dep111_person.department.id, new_grade.level),
        expected_review_scheme_id,
    )
    table_flow_service_mock.add_response_for_bonus_scheme_id(
        entities.BonusSchemeIdRequest(dep111_person.department.id, occupation.name, new_grade.level),
        expected_bonus_scheme_id,
    )
    table_flow_service_mock.add_response_for_reward_category(
        entities.RewardCategoryRequest(occupation.group_reward, new_grade.level),
        expected_reward_category,
    )
    table_flow_service_mock.add_response_for_reward_scheme_id(
        entities.RewardSchemeIdRequest(dep111_person.department.id, expected_reward_category, is_internship=False),
        expected_reward_scheme_id,
    )

    workflow_repository = gateways.WorkflowRepository()
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    workflow_id = use_case.create([data])[0][0]

    # then
    workflow = workflow_repository.get_by_id(workflow_id)
    assert len(workflow.changes) == 1
    change = workflow.changes[0]
    assert change.grade_id == new_grade.grade_id
    assert change.review_scheme_id == expected_review_scheme_id
    assert change.bonus_scheme_id == expected_bonus_scheme_id
    assert change.reward_scheme_id == expected_reward_scheme_id


@pytest.mark.django_db
def test_will_recalculate_schemes_on_department_change(
    company: AttrDict,
    container: Container,
    oebs_service_mock: OEBSServiceMock,
    table_flow_service_mock: TableflowMock,
) -> None:
    # given
    Switch.objects.get_or_create(name='enable_oebs_requests_for_registry', active=True)
    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    occupation = OccupationFactory(name='SMM', group_reward='StandartReward')
    expected_review_scheme_id = 1
    expected_bonus_scheme_id = 2
    expected_reward_category = 'Prof'
    expected_reward_scheme_id = 3

    dep111_person = company.persons['dep111-person']
    person_grade = 16
    data = entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        ticket='TJOB-1',
        is_move_with_budget_position=True,
        person_id=dep111_person.id,
        proposal_changes=[entities.ProposalChange(department_id=company.dep1.id)]
    )

    oebs_service_mock.set_grades_data_response(dep111_person.login, entities.GradeData(occupation.name, person_grade))

    table_flow_service_mock.add_response_for_review_scheme_id(
        entities.ReviewSchemeIdRequest(occupation.name, company.dep1.id, person_grade),
        expected_review_scheme_id,
    )
    table_flow_service_mock.add_response_for_bonus_scheme_id(
        entities.BonusSchemeIdRequest(company.dep1.id, occupation.name, person_grade),
        expected_bonus_scheme_id,
    )
    table_flow_service_mock.add_response_for_reward_category(
        entities.RewardCategoryRequest(occupation.group_reward, person_grade),
        expected_reward_category,
    )
    table_flow_service_mock.add_response_for_reward_scheme_id(
        entities.RewardSchemeIdRequest(company.dep1.id, expected_reward_category, is_internship=False),
        expected_reward_scheme_id,
    )

    workflow_repository = gateways.WorkflowRepository()
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    workflow_id = use_case.create([data])[0][0]

    # then
    workflow = workflow_repository.get_by_id(workflow_id)
    assert len(workflow.changes) == 1
    change = workflow.changes[0]
    assert change.review_scheme_id == expected_review_scheme_id
    assert change.bonus_scheme_id == expected_bonus_scheme_id
    assert change.reward_scheme_id == expected_reward_scheme_id


@pytest.mark.django_db
def test_will_recalculate_schemes_on_department_change_using_occupation_groups(
    company: AttrDict,
    container: Container,
    oebs_service_mock: OEBSServiceMock,
    table_flow_service_mock: TableflowMock,
) -> None:
    # given
    Switch.objects.get_or_create(name='enable_oebs_requests_for_registry', active=True)
    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    occupation = OccupationFactory(name='SMM', group_reward='StandartReward')
    expected_review_scheme_id = 1
    expected_bonus_scheme_id = 2
    expected_reward_category = 'Prof'
    expected_reward_scheme_id = 3

    dep111_person = company.persons['dep111-person']
    person_grade = 16
    data = entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        ticket='TJOB-1',
        is_move_with_budget_position=True,
        person_id=dep111_person.id,
        proposal_changes=[entities.ProposalChange(department_id=company.dep1.id)],
    )

    oebs_service_mock.set_grades_data_response(dep111_person.login, entities.GradeData(occupation.name, person_grade))

    table_flow_service_mock.add_response_for_review_scheme_id(
        entities.ReviewSchemeIdRequest(occupation.name, company.dep1.id, person_grade),
        None,
    )
    table_flow_service_mock.add_response_for_review_scheme_id_by_group(
        entities.ReviewSchemeIdByGroupRequest(occupation.group_review, company.dep1.id, person_grade),
        expected_review_scheme_id,
    )
    table_flow_service_mock.add_response_for_bonus_scheme_id(
        entities.BonusSchemeIdRequest(company.dep1.id, occupation.name, person_grade),
        None,
    )
    table_flow_service_mock.add_response_for_bonus_scheme_id_by_group(
        entities.BonusSchemeIdByGroupRequest(occupation.group_bonus, company.dep1.id, person_grade),
        expected_bonus_scheme_id,
    )
    table_flow_service_mock.add_response_for_reward_category(
        entities.RewardCategoryRequest(occupation.group_reward, person_grade),
        expected_reward_category,
    )
    table_flow_service_mock.add_response_for_reward_scheme_id(
        entities.RewardSchemeIdRequest(company.dep1.id, expected_reward_category, is_internship=False),
        expected_reward_scheme_id,
    )

    workflow_repository = gateways.WorkflowRepository()
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    workflow_id = use_case.create([data])[0][0]

    # then
    workflow = workflow_repository.get_by_id(workflow_id)
    assert len(workflow.changes) == 1
    change = workflow.changes[0]
    assert change.review_scheme_id == expected_review_scheme_id
    assert change.bonus_scheme_id == expected_bonus_scheme_id
    assert change.reward_scheme_id == expected_reward_scheme_id


@pytest.mark.django_db
def test_grade_changes_calculated_on_grade_change(container: Container) -> None:
    # given
    person = StaffFactory()

    data = [entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        person_id=person.id,
        proposal_changes=[entities.ProposalChange(grade_change='+1')],
    )]
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    result = use_case._grade_changes(data, {person.id})

    # when
    assert person.id in result


@pytest.mark.django_db
def test_grade_changes_calculated_on_department_change(container: Container) -> None:
    # given
    person = StaffFactory()

    data = [entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        person_id=person.id,
        proposal_changes=[entities.ProposalChange(department_id=1)],
    )]
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    result = use_case._grade_changes(data, {person.id})

    # when
    assert person.id in result


@pytest.mark.django_db
def test_grade_changes_calculated_on_occupation_change(container: Container) -> None:
    # given
    person = StaffFactory()

    data = [entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        person_id=person.id,
        proposal_changes=[entities.ProposalChange(occupation_id='SOME_OCCUPATION')],
    )]
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    result = use_case._grade_changes(data, {person.id})

    # when
    assert person.id in result


@pytest.mark.django_db
def test_grade_changes_not_calculated_on_missing_budget_position(container: Container) -> None:
    # given
    person = StaffFactory(budget_position=None)

    data = [entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        person_id=person.id,
        proposal_changes=[entities.ProposalChange(occupation_id='SOME_OCCUPATION')],
    )]
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    result = use_case._grade_changes(data, set())

    # when
    assert not result


@pytest.mark.django_db
def test_grade_changes_not_calculated_on_missing_grade_related_changes(container: Container) -> None:
    # given
    person = StaffFactory()

    data = [entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        person_id=person.id,
        proposal_changes=[entities.ProposalChange()],
    )]
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    result = use_case._grade_changes(data, {person.id})

    # when
    assert not result


@pytest.mark.django_db
def test_will_not_recalculate_schemes_on_department_change_with_missing_grade(
    company: AttrDict,
    container: Container,
    oebs_service_mock: OEBSServiceMock,
    table_flow_service_mock: TableflowMock,
) -> None:
    # given
    Switch.objects.get_or_create(name='enable_oebs_requests_for_registry', active=True)
    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    occupation = OccupationFactory(name='SMM', group_reward='StandartReward')
    expected_review_scheme_id = 1
    expected_bonus_scheme_id = 2
    expected_reward_category = 'Prof'
    expected_reward_scheme_id = 3

    dep111_person = company.persons['dep111-person']
    person_grade = 16
    data = entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        ticket='TJOB-1',
        is_move_with_budget_position=True,
        person_id=dep111_person.id,
        proposal_changes=[entities.ProposalChange(department_id=company.dep1.id)],
    )

    oebs_service_mock.set_grades_data_response(dep111_person.login, None)

    table_flow_service_mock.add_response_for_review_scheme_id(
        entities.ReviewSchemeIdRequest(occupation.name, company.dep1.id, person_grade),
        None,
    )
    table_flow_service_mock.add_response_for_review_scheme_id_by_group(
        entities.ReviewSchemeIdByGroupRequest(occupation.group_review, company.dep1.id, person_grade),
        expected_review_scheme_id,
    )
    table_flow_service_mock.add_response_for_bonus_scheme_id(
        entities.BonusSchemeIdRequest(company.dep1.id, occupation.name, person_grade),
        None,
    )
    table_flow_service_mock.add_response_for_bonus_scheme_id_by_group(
        entities.BonusSchemeIdByGroupRequest(occupation.group_bonus, company.dep1.id, person_grade),
        expected_bonus_scheme_id,
    )
    table_flow_service_mock.add_response_for_reward_category(
        entities.RewardCategoryRequest(occupation.group_reward, person_grade),
        expected_reward_category,
    )
    table_flow_service_mock.add_response_for_reward_scheme_id(
        entities.RewardSchemeIdRequest(company.dep1.id, expected_reward_category, is_internship=False),
        expected_reward_scheme_id,
    )

    workflow_repository = gateways.WorkflowRepository()
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    workflow_id = use_case.create([data])[0][0]

    # then
    workflow = workflow_repository.get_by_id(workflow_id)
    assert len(workflow.changes) == 1
    change = workflow.changes[0]
    assert change.grade_id is None
    assert change.review_scheme_id is None
    assert change.bonus_scheme_id is None
    assert change.reward_scheme_id is None


@pytest.mark.django_db
def test_will_recalculate_grade_id_on_occupation_change_and_current_empty_level(
    company: AttrDict,
    container: Container,
    oebs_service_mock: OEBSServiceMock,
    table_flow_service_mock: TableflowMock,
) -> None:
    # given
    Switch.objects.get_or_create(name='enable_oebs_requests_for_registry', active=True)
    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    OccupationFactory(name='SMM')
    new_occupation = OccupationFactory(name='BackendDeveloper', group_reward='StandartReward')
    current_grade_data = entities.GradeData('SMM', None)
    new_grade = GradeFactory(occupation=new_occupation, level=None)
    expected_review_scheme_id = 1
    expected_bonus_scheme_id = 2
    expected_reward_category = 'Prof'
    expected_reward_scheme_id = 3

    dep111_person = company.persons['dep111-person']
    data = entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        ticket='TJOB-1',
        is_move_with_budget_position=True,
        person_id=dep111_person.id,
        proposal_changes=[entities.ProposalChange(occupation_id=new_occupation.pk)],
    )

    oebs_service_mock.set_grades_data_response(dep111_person.login, current_grade_data)
    oebs_service_mock.add_grade_id(new_occupation.pk, new_grade.level, new_grade.grade_id)

    table_flow_service_mock.add_response_for_review_scheme_id(
        entities.ReviewSchemeIdRequest(new_occupation.name, dep111_person.department.id, new_grade.level),
        expected_review_scheme_id,
    )
    table_flow_service_mock.add_response_for_bonus_scheme_id(
        entities.BonusSchemeIdRequest(dep111_person.department.id, new_occupation.name, new_grade.level),
        expected_bonus_scheme_id,
    )
    table_flow_service_mock.add_response_for_reward_category(
        entities.RewardCategoryRequest(new_occupation.group_reward, new_grade.level),
        expected_reward_category,
    )
    table_flow_service_mock.add_response_for_reward_scheme_id(
        entities.RewardSchemeIdRequest(dep111_person.department.id, expected_reward_category, is_internship=False),
        expected_reward_scheme_id,
    )

    workflow_repository = gateways.WorkflowRepository()
    use_case = container[use_cases.CreateWorkflowFromProposal]

    # when
    workflow_id = use_case.create([data])[0][0]

    # then
    workflow = workflow_repository.get_by_id(workflow_id)
    assert len(workflow.changes) == 1
    change = workflow.changes[0]
    assert change.grade_id == new_grade.grade_id
    assert change.review_scheme_id == expected_review_scheme_id
    assert change.bonus_scheme_id == expected_bonus_scheme_id
    assert change.reward_scheme_id == expected_reward_scheme_id


@pytest.mark.django_db
def test_scheme_exceptions_works(
    company: AttrDict,
    container: Container,
    oebs_service_mock: OEBSServiceMock,
    table_flow_service_mock: TableflowMock,
) -> None:
    # given
    test_container = container.clone()
    Switch.objects.get_or_create(name='enable_oebs_requests_for_registry', active=True)
    Switch.objects.get_or_create(name='enable_table_flow_for_registry', active=True)

    occupation = OccupationFactory(name='SMM', group_reward='StandartReward')
    unexpected_review_scheme_id = 4
    unexpected_bonus_scheme_id = 5
    unexpected_reward_category = 'Mass'
    unexpected_reward_scheme_id = 6
    expected_review_scheme_id = 1
    expected_bonus_scheme_id = 2
    expected_reward_scheme_id = 3

    dep111_person = company.persons['dep111-person']
    person_grade = 16
    data = entities.ProposalData(
        proposal_id=ProposalMetadataFactory().id,
        ticket='TJOB-1',
        is_move_with_budget_position=True,
        person_id=dep111_person.id,
        proposal_changes=[entities.ProposalChange(department_id=company.dep1.id)]
    )

    oebs_service_mock.set_grades_data_response(dep111_person.login, entities.GradeData(occupation.name, person_grade))

    table_flow_service_mock.add_response_for_review_scheme_id(
        entities.ReviewSchemeIdRequest(occupation.name, company.dep1.id, person_grade),
        unexpected_review_scheme_id,
    )
    table_flow_service_mock.add_response_for_bonus_scheme_id(
        entities.BonusSchemeIdRequest(company.dep1.id, occupation.name, person_grade),
        unexpected_bonus_scheme_id,
    )
    table_flow_service_mock.add_response_for_reward_category(
        entities.RewardCategoryRequest(occupation.group_reward, person_grade),
        unexpected_reward_category,
    )
    table_flow_service_mock.add_response_for_reward_scheme_id(
        entities.RewardSchemeIdRequest(company.dep1.id, unexpected_reward_category, is_internship=False),
        unexpected_reward_scheme_id,
    )

    staff_service_mock = StaffServiceMock()
    staff_service_mock.set_person(entities.Person(dep111_person.id, dep111_person.login, 0, None))
    staff_service_mock.set_person_department(dep111_person.id, dep111_person.department_id)
    staff_service_mock.set_occupation_details(
        occupation_id=occupation.name,
        details=entities.OccupationDetails(occupation.group_review, occupation.group_bonus, occupation.group_reward),
    )
    staff_service_mock.set_person_scheme_exception(entities.PersonSchemeException(
        person_id=dep111_person.id,
        bonus_scheme_id=expected_bonus_scheme_id,
        reward_scheme_id=expected_reward_scheme_id,
        review_scheme_id=expected_review_scheme_id,
    ))
    test_container[entities.StaffService] = staff_service_mock

    use_case = test_container[use_cases.CreateWorkflowFromProposal]

    # when
    workflow_id = use_case.create([data])[0][0]

    # then
    workflow = gateways.WorkflowRepository().get_by_id(workflow_id)
    assert len(workflow.changes) == 1
    change = workflow.changes[0]
    assert change.review_scheme_id == expected_review_scheme_id
    assert change.bonus_scheme_id == expected_bonus_scheme_id
    assert change.reward_scheme_id == expected_reward_scheme_id
