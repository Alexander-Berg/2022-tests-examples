from datetime import date
from decimal import Decimal
from mock import MagicMock, patch
import uuid

import attr
import pytest

from staff.departments.models import DepartmentRoles
from staff.departments.tests.factories import VacancyFactory, HRProductFactory
from staff.headcounts.tests.factories import CreditManagementApplicationFactory
from staff.lib.testing import PlacementFactory, StaffFactory, DepartmentStaffFactory, ValueStreamFactory

from staff.budget_position.const import WORKFLOW_STATUS, PUSH_STATUS, PositionType
from staff.budget_position.models import BudgetPosition, BudgetPositionAssignmentStatus
from staff.budget_position.tasks import update_push_status
from staff.budget_position.tests.utils import BudgetPositionFactory, BudgetPositionAssignmentFactory
from staff.budget_position.tests.workflow_tests.utils import OEBSServiceMock
from staff.budget_position.workflow_service import (
    FemidaData,
    WorkflowRegistryService,
    CreditRepaymentData,
    MoveToMaternityData,
    container,
)
from staff.budget_position.workflow_service import gateways, entities


@pytest.mark.django_db
def test_workflow_1_1_scenario(company_with_module_scope):
    repository = gateways.WorkflowRepository()
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
        department=company_with_module_scope.yandex,
        value_stream=vs,
    )

    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        ticket='TJOB-1',
        department_id=company_with_module_scope.yandex.id,
        organization_id=company_with_module_scope['organizations']['yandex'].id,
        office_id=company_with_module_scope['offices']['KR'].id,
        dismissal_date=None,
        vacancy_name='Developer',
        is_vacancy=True,
    )

    workflow_service = WorkflowRegistryService()
    workflow_service.oebs_service = OEBSServiceMock()
    workflow_service.oebs_service.set_position_as_change(
        assignment.budget_position.code,
        entities.Change(grade_id=None),
    )
    workflow_id = workflow_service.try_create_workflow_from_femida(data)

    workflow = repository.get_by_id(workflow_id)
    assert workflow.status == WORKFLOW_STATUS.CONFIRMED
    assert workflow.code == '1.1'

    workflow_service = WorkflowRegistryService()
    workflow_service.push_workflow_to_oebs(workflow_id, StaffFactory().id)

    workflow = repository.get_by_id(workflow_id)
    assert workflow.status == WORKFLOW_STATUS.SENDING_NOTIFICATION


@pytest.mark.django_db
def test_workflow_1_2_scenario(company_with_module_scope):
    new_budget_position_code = 23562457
    gateways.OEBSService.get_transaction_status = MagicMock(
        return_value=('UPLOADED', new_budget_position_code, None, None),
    )
    repository = gateways.WorkflowRepository()
    BudgetPositionFactory()
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=None,
        ticket='TJOB-123',
        department_id=company_with_module_scope.yandex.id,
        organization_id=company_with_module_scope['organizations']['yandex'].id,
        office_id=company_with_module_scope['offices']['KR'].id,
        is_vacancy=True,
    )
    catalyst = StaffFactory()
    placement = PlacementFactory(office_id=data.office_id, organization_id=data.organization_id)

    workflow_service = WorkflowRegistryService()
    workflow_id = workflow_service.try_create_workflow_from_femida(data)

    workflow = repository.get_by_id(workflow_id)
    assert workflow.status == WORKFLOW_STATUS.CONFIRMED
    assert workflow.code == '1.2'

    # To avoid recursion instead of celery tasks
    with patch('staff.budget_position.workflow_service.container.update_push_status'):
        workflow_service.push_workflow_to_oebs(workflow_id, catalyst.id)
        update_push_status(workflow_id)
        update_push_status(workflow_id)

    workflow = repository.get_by_id(workflow_id)
    assert workflow.status == WORKFLOW_STATUS.SENDING_NOTIFICATION
    assert workflow.changes[0].new_budget_position.id == BudgetPosition.objects.get(code=new_budget_position_code).id
    assert workflow.changes[0].push_status == PUSH_STATUS.FINISHED
    assert workflow.changes[1].linked_budget_position_id == workflow.changes[0].new_budget_position.id
    assert workflow.changes[1].push_status == PUSH_STATUS.FINISHED

    assert gateways.OEBSService.send_request.call_count == 2

    assert gateways.OEBSService.send_request.call_args_list[0][0][1] == {
        'apiUniqueKey': str(workflow.changes[0].oebs_idempotence_key),
        'login': catalyst.login,
        'loginDate': date.today().isoformat(),
        'positionName': f'Кредит для TJOB-123',
        'ticket': 'JOB-1230000',
        'forRecruitment': {
            'hireDate': date.today().isoformat(),
            'positionPermission': PositionType.NEGATIVE_RESERVE.name,
        },
        'positionAnalitics': [{
            'effectiveDate': date.today().isoformat(),
            'organizationID': data.department_id,
            'taxUnitID': company_with_module_scope['organizations']['yandex'].oebs_organization.org_id,
            'officeID': placement.id,
            'headCount': 1,
            'unitManager': 'N',
            'fte': 1.0,
        }],
        'positionAnalytics': [{
            'effectiveDate': date.today().isoformat(),
            'organizationID': data.department_id,
            'taxUnitID': company_with_module_scope['organizations']['yandex'].oebs_organization.org_id,
            'officeID': placement.id,
            'headCount': 1,
            'unitManager': 'N',
            'fte': 1.0,
        }],
    }

    assert gateways.OEBSService.send_request.call_args_list[1][0][1] == {
        'apiUniqueKey': str(workflow.changes[1].oebs_idempotence_key),
        'login': catalyst.login,
        'loginDate': date.today().isoformat(),
        'positionName': f'Вакансия',
        'ticket': 'JOB-1230000',
        'forRecruitment': {
            'hireDate': date.today().isoformat(),
            'positionPermission':  PositionType.VACANCY.name,
        },
        'positionAnalitics': [{
            'effectiveDate': date.today().isoformat(),
            'organizationID': data.department_id,
            'linkedPos': new_budget_position_code,
            'taxUnitID': company_with_module_scope['organizations']['yandex'].oebs_organization.org_id,
            'officeID': placement.id,
            'headCount': 1,
            'unitManager': 'N',
            'fte': 1.0,
        }],
        'positionAnalytics': [{
            'effectiveDate': date.today().isoformat(),
            'organizationID': data.department_id,
            'linkedPos': new_budget_position_code,
            'taxUnitID': company_with_module_scope['organizations']['yandex'].oebs_organization.org_id,
            'officeID': placement.id,
            'headCount': 1,
            'unitManager': 'N',
            'fte': 1.0,
        }],
    }


@pytest.mark.django_db
def test_workflow_5_2_scenario(company_with_module_scope):
    DepartmentStaffFactory(
        staff=company_with_module_scope['yandex-person'],
        role_id=DepartmentRoles.HR_ANALYST_TEMP.value,
        department=company_with_module_scope.yandex,
    )
    repository = gateways.WorkflowRepository()
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.OFFER.value,
        department=company_with_module_scope.yandex,
        value_stream=vs,
    )
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        ticket='TJOB-1',
        department_id=company_with_module_scope.yandex.id,
        organization_id=company_with_module_scope['organizations']['yandex'].id,
        office_id=company_with_module_scope['offices']['KR'].id,
        vacancy_name='Developer',
        is_offer_rejection=True,
    )

    workflow_service = WorkflowRegistryService()
    workflow_id = workflow_service.try_create_workflow_from_femida(data)

    # To avoid recursion instead of celery tasks
    with patch('staff.budget_position.workflow_service.container.update_push_status'):
        update_push_status(workflow_id)
        update_push_status(workflow_id)

    workflow = repository.get_by_id(workflow_id)
    assert workflow.code == '5.2'
    assert workflow.status == WORKFLOW_STATUS.SENDING_NOTIFICATION


@pytest.mark.django_db
def test_workflow_5_3_scenario(company_with_module_scope):
    repository = gateways.WorkflowRepository()
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
        department=company_with_module_scope.yandex,
        value_stream=vs,
    )
    person_office = company_with_module_scope['offices']['KR']
    person = StaffFactory(office=person_office, organization=company_with_module_scope['organizations']['yandex_tech'])
    data = FemidaData(
        vacancy_id=100500,
        budget_position_code=assignment.budget_position.code,
        ticket='TJOB-123',
        department_id=company_with_module_scope.yandex.id,
        organization_id=company_with_module_scope['organizations']['yandex'].id,
        is_internal_offer=True,
        person_id=person.id,
    )
    placement = PlacementFactory(office_id=person_office.id, organization_id=data.organization_id)

    workflow_service = WorkflowRegistryService()
    workflow_id = workflow_service.try_create_workflow_from_femida(data)

    workflow = repository.get_by_id(workflow_id)
    assert workflow.status == WORKFLOW_STATUS.CONFIRMED
    assert workflow.code == '5.3'

    assert len(workflow.changes) == 1
    change = workflow.changes[0]

    assert change.salary == Decimal(100500)
    assert change.rate == Decimal(0.5)
    assert change.organization_id == data.organization_id
    assert change.office_id == person_office.id
    assert change.placement_id == placement.id


@pytest.mark.django_db
def test_workflow_credit_management_with_vacancy_scenario(company_with_module_scope):
    repository = gateways.WorkflowRepository()
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    credit = BudgetPositionAssignmentFactory(status=BudgetPositionAssignmentStatus.RESERVE.value, value_stream=vs)
    repayment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
        value_stream=vs,
    )
    vacancy = VacancyFactory(budget_position=repayment.budget_position)

    data = CreditRepaymentData(
        credit_management_id=CreditManagementApplicationFactory().id,
        credit_budget_position=credit.budget_position.code,
        repayment_budget_position=repayment.budget_position.code,
        vacancy_id=vacancy.id,
    )

    workflow_service = WorkflowRegistryService()
    workflow_id = workflow_service.try_create_workflow_for_credit_repayment(data)

    workflow = repository.get_by_id(workflow_id)
    assert workflow.status == WORKFLOW_STATUS.PENDING
    assert workflow.code == '8.0'

    with patch('staff.budget_position.workflow_service.gateways.femida_service.FemidaService.close_vacancy') as mock:
        workflow_service.confirm_workflow(workflow_id, StaffFactory().id)
        mock.assert_called_once_with(workflow_id)

    # To avoid recursion instead of celery tasks
    with patch('staff.budget_position.workflow_service.container.update_push_status'):
        update_push_status(workflow_id)

    workflow = repository.get_by_id(workflow_id)
    assert workflow.status == WORKFLOW_STATUS.SENDING_NOTIFICATION


@pytest.mark.django_db
def test_workflow_credit_management_scenario(company_with_module_scope):
    repository = gateways.WorkflowRepository()
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    credit = BudgetPositionAssignmentFactory(status=BudgetPositionAssignmentStatus.RESERVE.value, value_stream=vs)
    repayment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
        value_stream=vs,
    )

    data = CreditRepaymentData(
        credit_management_id=CreditManagementApplicationFactory().id,
        credit_budget_position=credit.budget_position.code,
        repayment_budget_position=repayment.budget_position.code,
    )

    workflow_service = WorkflowRegistryService()
    workflow_id = workflow_service.try_create_workflow_for_credit_repayment(data)

    workflow = repository.get_by_id(workflow_id)
    assert workflow.status == WORKFLOW_STATUS.PENDING
    assert workflow.code == '8.1'

    workflow_service.confirm_workflow(workflow_id, StaffFactory().id)

    # To avoid recursion instead of celery tasks
    with patch('staff.budget_position.workflow_service.container.update_push_status'):
        update_push_status(workflow_id)

    workflow = repository.get_by_id(workflow_id)
    assert workflow.status == WORKFLOW_STATUS.SENDING_NOTIFICATION


@pytest.mark.django_db
def test_workflow_execution_schedules_push_to_femida():
    # given
    test_container = container.build_container().clone()

    workflow_mock = MagicMock()
    workflow_mock.id = uuid.uuid1()
    workflow_mock.should_push_new_department_to_femida = True
    workflow_mock.should_close_vacancy = False  # bool(mock.attribute) is True by default
    workflow_mock.should_be_pushed_automatically = False

    repo_mock = MagicMock()
    repo_mock.get_workflows_by_proposal_id = MagicMock(return_value=[workflow_mock])
    repo_mock.get_by_id = MagicMock(return_value=workflow_mock)
    repo_mock.save = MagicMock()
    test_container[entities.WorkflowRepositoryInterface] = repo_mock

    femida_mock = MagicMock()
    femida_mock.schedule_department_push = MagicMock()
    test_container[entities.FemidaService] = femida_mock

    workflow_service = WorkflowRegistryService(test_container)

    # when
    workflow_service.confirm_workflows_for_proposal(100500)

    # then
    femida_mock.schedule_department_push.assert_called_once_with(workflow_mock.id)


@pytest.mark.django_db
def test_maternity_workflow_scenario():
    # given
    test_container = container.build_container().clone()
    responsible = StaffFactory()
    bp = BudgetPositionFactory()
    person = StaffFactory(budget_position=bp)
    oebs_mock = MagicMock()
    oebs_mock.get_position_as_change = MagicMock(return_value=entities.Change())
    oebs_mock.get_crossing_position_info_as_change = MagicMock(return_value=entities.Change())
    test_container[entities.OEBSService] = oebs_mock

    data = MoveToMaternityData(
        login=person.login,
        department_id=person.department_id,
        responsible_id=responsible.id,
        budget_position=bp.code,
        ticket='TSALARY-100',
    )
    workflow_service = WorkflowRegistryService(test_container)

    # when
    with patch('staff.budget_position.workflow_service.container.update_push_status'):
        workflow_id = workflow_service.try_create_workflow_for_maternity(data)

    # then
    workflow = gateways.WorkflowRepository().get_by_id(workflow_id)
    assert workflow.status == WORKFLOW_STATUS.PUSHED
    assert workflow.code == '9'


@pytest.mark.django_db
def test_workflow_71_changing_only_position_should_be_pushed_automatically():
    # given
    fields_without_values = {
        'new_budget_position',
        'linked_budget_position_id',
        'pushed_to_femida',
        'oebs_transaction_id',
        'last_oebs_error',
        'correction_id',
        'push_status',
        'sent_to_oebs',
        'assignment_id',
        'currency',
        'department_id',
        'dismissal_date',
        'geography_url',
        'grade_id',
        'headcount',
        'hr_product_id',
        'office_id',
        'organization_id',
        'pay_system',
        'wage_system',
        'placement_id',
        'position_name',
        'position_type',
        'rate',
        'remove_budget_position',
        'salary',
        'optional_ticket',
        'unit_manager',
        'review_scheme_id',
        'bonus_scheme_id',
        'reward_scheme_id',
        'employment_type',
        'physical_entity_id',
        'other_payments',
        'join_at',
        'probation_period_code',
        'is_replacement',
        'instead_of_login',
        'contract_term_date',
        'contract_period',
    }

    change = entities.Change(
        id=1,
        workflow_id=uuid.uuid1(),
        budget_position=entities.BudgetPosition(),
        effective_date=date.today(),
        person_id=1,
        ticket='1',
        position_id=1,
        **{field_name: None for field_name in fields_without_values},
    )
    fields_not_described_above = (
        {field.name for field in attr.fields(entities.Change)}
        - entities.workflows.Workflow7_1.fields_not_changing_budget_position
        - fields_without_values
        - {'position_id'}
    )
    workflow = entities.workflows.Workflow7_1(change.workflow_id, [change])

    # then
    assert workflow.should_be_pushed_automatically
    assert workflow.should_be_marked_manually_processed_automatically
    assert not fields_not_described_above, 'you should explicitly describe whether new field affects send condition'


@pytest.mark.django_db
def test_workflow_71_changing_not_only_position_should_not_be_pushed_automatically():
    # given
    change = entities.Change(position_id=100500, position_name='Test')
    workflow = entities.workflows.Workflow7_1(uuid.uuid1(), [change])

    # then
    assert not workflow.should_be_pushed_automatically
    assert not workflow.should_be_marked_manually_processed_automatically
