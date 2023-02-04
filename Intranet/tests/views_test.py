import json
from decimal import Decimal

import pytest
from mock import Mock, patch
from waffle.models import Switch

from django.core.urlresolvers import reverse

from staff.departments.tests.factories import VacancyFactory, HRProductFactory, BonusFactory
from staff.groups.models import GROUP_TYPE_CHOICES
from staff.lib.testing import (
    GeographyFactory,
    GroupFactory,
    OfficeFactory,
    OrganizationFactory,
    PlacementFactory,
    StaffFactory,
    ValueStreamFactory,
)
from staff.oebs.tests.factories import (
    JobFactory,
    ReviewFactory,
    RewardFactory,
)

from staff.budget_position import views
from staff.budget_position.const import InitStatus, PositionType, FemidaRequestType, WORKFLOW_STATUS
from staff.budget_position.models import Workflow, ChangeRegistry, BudgetPositionAssignmentStatus
from staff.budget_position.tests import utils
from staff.budget_position.tests.utils import ChangeFactory, BudgetPositionAssignmentFactory
from staff.budget_position.tests.workflow_tests.utils import WorkflowModelFactory
from staff.budget_position.workflow_service import OebsHireError, gateways, entities


@pytest.mark.django_db
@patch.object(gateways.OEBSService, 'get_position_as_change')
def test_attach_bp_to_vacancy_simple(mocked_oebs, femida_post_request, company):
    # given
    mocked_oebs.return_value = entities.Change(grade_id=None)
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_PLAN.value,
        department=company.yandex,
        value_stream=vs,
    )
    vacancy = VacancyFactory()
    form_data = {
        'budget_position_id': assignment.budget_position.code,
        'vacancy': vacancy.id,
        'request_type': FemidaRequestType.VACANCY.value,
    }

    # when
    resp = views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', form_data))

    # then
    assert resp.status_code == 201, resp.content
    resp = json.loads(resp.content)
    assert 'id' in resp
    assert resp['id'] == str(Workflow.objects.first().id)
    assert resp['status'] == InitStatus.CREATED.value


@pytest.mark.django_db
def test_attach_bp_to_vacancy_without_workflow(femida_post_request, company):
    Switch.objects.get_or_create(name='enable_bp_state_checks_in_registry', active=True)

    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.MATERNITY.value,
        department=company.yandex,
        value_stream=vs,
    )

    vacancy = VacancyFactory()

    resp = views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', {
        'budget_position_id': assignment.budget_position.code,
        'vacancy': vacancy.id,
        'request_type': FemidaRequestType.VACANCY.value,
    }))

    assert resp.status_code == 400, resp.content
    resp = json.loads(resp.content)
    assert 'errors' in resp


@pytest.mark.django_db
def test_attach_bp_to_vacancy_without_bp(femida_post_request):
    utils.GradeFactory()
    vacancy = VacancyFactory()
    hr_product_by_translation_id = HRProductFactory()

    resp = views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', {
        'vacancy': vacancy.id,
        'request_type': FemidaRequestType.VACANCY.value,
        'hr_product_translation_id': hr_product_by_translation_id.st_translation_id,
    }))

    assert resp.status_code == 201, resp.content
    resp = json.loads(resp.content)
    assert 'id' in resp
    assert resp['status'] == InitStatus.CREATED.value, resp

    assert resp['id'] == str(Workflow.objects.first().id)
    workflow = Workflow.objects.get(id=resp['id'])
    change = workflow.changeregistry_set.first()
    assert change
    assert change.staff_hr_product_id == hr_product_by_translation_id.id


@pytest.mark.django_db
def test_attach_bp_to_offer_full(femida_post_request, company):
    bp_status = 'VACANCY'

    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
        department=company.yandex,
        value_stream=vs,
    )
    offer_id = 123
    vacancy = VacancyFactory(offer_id=offer_id)
    job_issue_key = 'JOB-666'
    department_id = company.yandex.id
    abc_service = GroupFactory(service_id=666, type=GROUP_TYPE_CHOICES.SERVICE)
    payment_type = 'piecework'
    converted_payment_type = utils.PaysysFactory(name='XXYA_JOBPRICE')
    is_internship = True
    grade = utils.GradeFactory()
    office = OfficeFactory()
    organization = OrganizationFactory()
    salary = '1'
    currency = 'USD'
    dismissal_date = '2019-11-22'
    reward = RewardFactory()
    bonus = BonusFactory()
    review = ReviewFactory()
    geography = GeographyFactory()
    hr_product = HRProductFactory()
    placement = PlacementFactory(organization=organization, office=office)

    post_kw = {
        'budget_position_id': assignment.budget_position.code,
        'budget_position_status': bp_status,
        'vacancy': vacancy.id,

        'abc_services': [abc_service.service_id],
        'department': department_id,
        'is_internship': is_internship,
        'job_issue_key': job_issue_key,
        'payment_type': payment_type,
        'vacancy_name': vacancy.name,

        'bonus_scheme': bonus.scheme_id,
        'reward_scheme': reward.scheme_id,
        'currency': currency,
        'dismissal_date': dismissal_date,
        'grade': grade.level,
        'geography_translation_id': geography.st_translation_id,
        'hr_product_translation_id': hr_product.st_translation_id,
        'offer': offer_id,
        'office': office.id,
        'organization': organization.id,
        'profession_key': grade.occupation.name,
        'review_scheme': review.scheme_id,
        'salary': salary,

        'request_type': FemidaRequestType.OFFER.value,
    }
    resp = views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', post_kw))

    assert resp.status_code == 201
    workflow_id = json.loads(resp.content)['id']

    repo = gateways.WorkflowRepository()
    change = repo.get_by_id(workflow_id).changes[0]

    assert change.bonus_scheme_id == bonus.scheme_id
    assert change.reward_scheme_id == reward.scheme_id
    assert change.currency == currency
    assert change.department_id == department_id
    assert change.dismissal_date is None
    assert change.geography_url == geography.department_instance.url
    assert change.grade_id == grade.grade_id
    assert change.placement_id == placement.id
    assert change.headcount is None
    assert change.office_id == office.id
    assert change.organization_id == organization.id
    assert change.pay_system == converted_payment_type.name
    assert change.position_type == PositionType.OFFER
    assert change.review_scheme_id == review.scheme_id
    assert change.salary == Decimal(salary)
    assert change.ticket == job_issue_key


@pytest.mark.django_db
def test_attach_bp_to_internal_offer(femida_post_request, company):
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
        department=company.yandex,
        value_stream=vs,
    )

    vacancy = VacancyFactory()

    person = StaffFactory()
    views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', {
        'budget_position_id': assignment.budget_position.code,
        'vacancy': vacancy.id,
        'request_type': FemidaRequestType.INTERNAL_OFFER.value,
        'username': person.login,
    }))

    workflow = Workflow.objects.last()
    assert workflow.code == '5.3'


@pytest.fixture(scope='module')
def assignment_create_data_required():
    return {
        'person': 123,  # ID физ лица в Я.Найм (не staff.id)
        'join_at': '2020-03-01',
    }


@pytest.fixture(scope='module')
def assignment_create_data_full(company_with_module_scope, assignment_create_data_required, django_db_blocker):
    with django_db_blocker.unblock():
        vs = ValueStreamFactory()
        HRProductFactory(value_stream=vs)
        assignment = BudgetPositionAssignmentFactory(value_stream=vs)

        yield {
            'budget_position_id': assignment.budget_position.code,
            'department': company_with_module_scope.yandex.id,
            'office': OfficeFactory().id,
            'employment_type': 'full',
            'payment_type': 'monthly',
            'salary': '100500',
            'currency': 'RUB',
            'organization': company_with_module_scope['organizations']['yandex'].id,
            'job': JobFactory().code,
            'other_payments': '100500',
            'probation_period': 3,
            'is_replacement': False,
            'instead_of': StaffFactory().login,
            'contract_term_date': '2021-03-01',
            'contract_term': 12,
            **assignment_create_data_required
        }


@pytest.mark.django_db
def test_create_assignment(femida_post_request, assignment_create_data_full):
    resp = views.create_assignment(
        femida_post_request('budget-position-api:create-assignment', assignment_create_data_full)
    )

    resp_content = json.loads(resp.content)
    assert resp.status_code == 201, resp_content
    assert resp_content['id']


@pytest.mark.django_db
def test_push_to_oebs(startrek_post_request):
    workflow_id = WorkflowModelFactory(status=WORKFLOW_STATUS.CONFIRMED).id
    ChangeFactory(
        ticket='SALARY-123',
        position_name='some',
        workflow_id=workflow_id,
    )
    ticket = {
        'ticket': 'SALARY-123',
    }
    with patch('staff.budget_position.tasks.PushFromStartrekToOEBS', lambda ticket: None):
        resp = views.push_to_oebs(
            startrek_post_request('budget-position-api:push-to-oebs', ticket)
        )
        resp_content = json.loads(resp.content)
        assert resp.status_code == 200, resp_content


@pytest.mark.django_db
def test_bad_push_to_oebs(startrek_post_request):
    workflow_id = WorkflowModelFactory(status=WORKFLOW_STATUS.CONFIRMED).id
    ChangeFactory(
        ticket='SALARY-12',
        position_name='some',
        workflow_id=workflow_id,
    )
    ticket = {
    }
    with patch('staff.budget_position.tasks.PushFromStartrekToOEBS', lambda ticket: None):
        resp = views.push_to_oebs(
            startrek_post_request('budget-position-api:push-to-oebs', ticket)
        )
        assert resp.status_code == 400


@patch(
    'staff.budget_position.workflow_service.gateways.oebs_hire_service.OebsHireService.send_change',
    Mock(side_effect=OebsHireError('some description', ['code_1', 'code_2'])),
)
@pytest.mark.django_db
def test_create_assignment_oebs_hire_error_handling(femida_post_request, assignment_create_data_full):
    resp = views.create_assignment(
        femida_post_request('budget-position-api:create-assignment', assignment_create_data_full)
    )

    resp_content = json.loads(resp.content)
    assert resp.status_code == 500, resp_content
    assert resp_content['oebs_internal_errors']


@patch('staff.budget_position.views.export_views._has_access_to_export_changes', Mock(side_effect=lambda *a, **b: True))
@patch('staff.lib.decorators._is_tvm_request', Mock(side_effect=lambda *a, **b: False))
@pytest.mark.django_db
@pytest.mark.parametrize(
    'workflow_code, optional_ticket, budget_position_changed',
    [
        ('5.1', None, False),
        ('5.3', 'test_ticket', True),
        ('7.3', None, True),
    ],
)
def test_export_changes(fetcher, workflow_code, optional_ticket, budget_position_changed):
    pending_workflow = WorkflowModelFactory(status=WORKFLOW_STATUS.PENDING, code=workflow_code)
    confirmed_workflow = WorkflowModelFactory(status=WORKFLOW_STATUS.FINISHED, code=workflow_code)
    utils.ChangeFactory(workflow=pending_workflow, optional_ticket=optional_ticket)
    confirmed_change = utils.ChangeFactory(workflow=confirmed_workflow, optional_ticket=optional_ticket)

    response = fetcher.get(reverse('budget-position-api:export-changes'))
    assert response.status_code == 200

    expected = [{
        'budget_position': {'code': confirmed_change.budget_position.code, 'id': confirmed_change.budget_position.id},
        'budget_position_changed': budget_position_changed,
        'id': confirmed_change.id,
        'workflow_id': str(confirmed_workflow.id),
        'meta': {
            'bonus_scheme_id': confirmed_change.staff_bonus_scheme_id,
            'compensation_scheme_id': confirmed_change.compensation_scheme_id,
            'currency': confirmed_change.currency,
            'department_id': confirmed_change.department_id,
            'dismissal_date': confirmed_change.dismissal_date,
            'effective_date': confirmed_change.effective_date and str(confirmed_change.effective_date),
            'geography_id': None,
            'grade_id': confirmed_change.oebs_grade_id,
            'headcount': confirmed_change.headcount,
            'hr_product_id': confirmed_change.staff_hr_product_id,
            'login': confirmed_change.staff.login,
            'office_id': confirmed_change.office_id,
            'organization_id': confirmed_change.organization_id,
            'pay_system': confirmed_change.pay_system and confirmed_change.pay_system.name,
            'wage_system': confirmed_change.wage_system,
            'placement_id': confirmed_change.placement_id,
            'position_id': confirmed_change.position_id,
            'position_name': confirmed_change.position_name,
            'position_type': confirmed_change.position_type,
            'rate': confirmed_change.rate,
            'review_scheme_id': confirmed_change.review_scheme_id,
            'salary': confirmed_change.salary,
            'ticket': confirmed_change.ticket,
            'optional_ticket': confirmed_change.optional_ticket,
        },
        'push_status': confirmed_change.push_status,
        'oebs_transaction_id': confirmed_change.oebs_transaction_id,
        'status': confirmed_workflow.status,
        'vacancy_id': confirmed_workflow.vacancy_id,
        'proposal_id': confirmed_workflow.proposal and confirmed_workflow.proposal.proposal_id,
        'staff_id': confirmed_change.staff.id,
        'started_at': confirmed_workflow.created_at and confirmed_workflow.created_at.isoformat()[:-7],
        'resolved_at': confirmed_workflow.confirmed_at and confirmed_workflow.confirmed_at.isoformat()[:-7],
        'sent_to_oebs': confirmed_change.sent_to_oebs and confirmed_change.sent_to_oebs.isoformat()[:-7],
        'pushed_to_femida': confirmed_change.pushed_to_femida,
        'remove_budget_position': confirmed_change.remove_budget_position,
        'contract_term_date': confirmed_change.contract_term_date,
        'contract_period': confirmed_change.contract_period,
        'employment_type': confirmed_change.employment_type,
        'instead_of_login': confirmed_change.instead_of_login,
        'is_replacement': confirmed_change.is_replacement,
        'join_at': confirmed_change.join_at,
        'other_payments': confirmed_change.other_payments,
        'person_id': confirmed_change.person_id,
        'probation_period_code': confirmed_change.probation_period_code,
    }]

    actual = json.loads(response.content)
    assert actual == expected


@pytest.mark.django_db
def test_make_changes_query_no_form_data_filter():
    filter_form_data = {'from_id': None, 'from_date': None, 'from_effective_date': None}

    result = views.export_views._make_changes_query(filter_form_data)

    eligible_codes = (
        entities.workflows.femida_workflows.Workflow5_1.code,
        entities.workflows.femida_workflows.Workflow5_2.code,
        entities.workflows.femida_workflows.Workflow5_3.code,
        entities.workflows.proposal_workflows.Workflow7_1.code,
        entities.workflows.proposal_workflows.Workflow7_100500.code,
        entities.workflows.proposal_workflows.MoveWithoutBudgetPositionWorkflow.code
    )

    for c in eligible_codes:
        for s in WORKFLOW_STATUS._choices:
            utils.ChangeFactory(workflow=WorkflowModelFactory(status=s[1], code=c))
            utils.ChangeFactory(workflow=WorkflowModelFactory(status=s[1], code=c), optional_ticket="test")

    filtered = ChangeRegistry.objects.prefetch_related("workflow").filter(result)

    assert len(filtered) == 22
    assert all(x.workflow.status in (WORKFLOW_STATUS.FINISHED, WORKFLOW_STATUS.SENDING_NOTIFICATION) for x in filtered)
    assert all(x.workflow.code in eligible_codes for x in filtered)
    assert all(x.optional_ticket for x in filtered
               if x.workflow.code == entities.workflows.femida_workflows.Workflow5_3.code)


@pytest.mark.django_db
def test_attach_to_vacancy_hr_product_by_translation_id_defined(femida_post_request, company):
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
        department=company.yandex,
        value_stream=vs,
    )
    vacancy = VacancyFactory()
    hr_product_by_id = HRProductFactory()
    hr_product_by_translation_id = HRProductFactory()
    person = StaffFactory()

    result = views.attach_to_vacancy(femida_post_request(
        'budget-position-api:attach-to-vacancy',
        {
            'budget_position_id': assignment.budget_position.code,
            'vacancy': vacancy.id,
            'request_type': FemidaRequestType.INTERNAL_OFFER.value,
            'username': person.login,
            'hr_product_id': hr_product_by_id.id,
            'hr_product_translation_id': hr_product_by_translation_id.st_translation_id,
        },
    ))

    assert result.status_code == 201
    response = json.loads(result.content)
    workflow = Workflow.objects.get(id=response['id'])
    change = workflow.changeregistry_set.first()
    assert change
    assert change.staff_hr_product_id == hr_product_by_translation_id.id


@pytest.mark.django_db
def test_attach_to_vacancy_hr_product_by_translation_id_not_defined(femida_post_request, company):
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.VACANCY_OPEN.value,
        department=company.yandex,
        value_stream=vs,
    )
    vacancy = VacancyFactory()
    hr_product_by_id = HRProductFactory()
    person = StaffFactory()

    result = views.attach_to_vacancy(femida_post_request(
        'budget-position-api:attach-to-vacancy',
        {
            'budget_position_id': assignment.budget_position.code,
            'vacancy': vacancy.id,
            'request_type': FemidaRequestType.INTERNAL_OFFER.value,
            'username': person.login,
            'hr_product_id': hr_product_by_id.id,
        },
    ))

    assert result.status_code == 201
    response = json.loads(result.content)
    workflow = Workflow.objects.get(id=response['id'])
    change = workflow.changeregistry_set.first()
    assert change
    assert change.staff_hr_product_id == hr_product_by_id.id


@pytest.mark.django_db
def test_attach_to_vacancy_no_vs_defined(femida_post_request, company):
    vacancy = VacancyFactory()

    result = views.attach_to_vacancy(femida_post_request(
        'budget-position-api:attach-to-vacancy',
        {
            'vacancy': vacancy.id,
            'request_type': FemidaRequestType.VACANCY.value,
        },
    ))

    assert result.status_code == 400, result.content
    assert 'errors' in json.loads(result.content)


@pytest.mark.django_db
@patch.object(gateways.OEBSService, 'get_position_as_change')
def test_attach_bp_gives_400_on_unknown_grade_id(mocked_oebs, femida_post_request, company):
    # given
    test_grade_id = 54321
    mocked_oebs.return_value = entities.Change(grade_id=test_grade_id)
    vs = ValueStreamFactory()
    HRProductFactory(value_stream=vs)
    assignment = BudgetPositionAssignmentFactory(
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        department=company.yandex,
        value_stream=vs,
    )
    vacancy = VacancyFactory()
    form_data = {
        'budget_position_id': assignment.budget_position.code,
        'vacancy': vacancy.id,
        'request_type': FemidaRequestType.VACANCY.value,
    }

    # when
    resp = views.attach_to_vacancy(femida_post_request('budget-position-api:attach-to-vacancy', form_data))

    # then
    assert resp.status_code == 400, resp.content
    resp = json.loads(resp.content)
    assert 'errors' in resp
