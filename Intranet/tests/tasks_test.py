import pytest
from uuid import UUID

from django.conf import settings
from django.db.models import F
from mock import patch, call

from staff.lib.testing import BudgetPositionFactory, DepartmentStaffFactory, StaffFactory

from staff.budget_position.workflow_service import FemidaError
from staff.departments import models as dep_models
from staff.departments.models.department import DepartmentRoles
from staff.departments.tests.factories import VacancyFactory
from staff.headcounts.tests.factories import HeadcountPositionFactory

from staff.budget_position.const import PUSH_STATUS, WORKFLOW_STATUS
from staff.budget_position.tasks import PushWorkflow, PushFromStartrekToOEBS
from staff.budget_position.tests.utils import ChangeFactory
from staff.budget_position.tests.workflow_tests.utils import WorkflowModelFactory
from staff.budget_position.models import Workflow


class Issue:
    def __init__(self, *args, **kwargs):
        for name, value in kwargs.items():
            self.__setattr__(name, value)

    def __getitem__(self, key):
        return self.__getattribute__(key)


@pytest.mark.django_db
def test_push_ticket_to_oebs():
    # given
    analyst = StaffFactory()
    workflow_id = UUID('123e4567-e89b-12d3-a456-426614174000')
    WorkflowModelFactory(status=WORKFLOW_STATUS.CONFIRMED, id=workflow_id)
    analyst_field = Issue(login=analyst.login)
    issue = Issue(analyst=analyst_field)
    issue.__setattr__(settings.STARTREK_WORKFLOW_FIELD_ID, '123e4567-e89b-12d3-a456-426614174000')

    # when
    with patch('staff.lib.startrek.issues.get_issue', lambda ticket: issue):
        PushFromStartrekToOEBS('JOB-123')

    # then
    workflow = Workflow.objects.filter(id=workflow_id).first()
    assert workflow.status == WORKFLOW_STATUS.SENDING_NOTIFICATION
    assert workflow.catalyst_id == analyst.id


@pytest.mark.django_db
def test_update_from_oebs(company):
    bp_model = BudgetPositionFactory()

    # create vacancies to update
    VacancyFactory(headcount_position_code=1234)
    VacancyFactory(headcount_position_code=bp_model.code, budget_position=bp_model)
    VacancyFactory(headcount_position_code=12345, budget_position=bp_model)

    # create staff to update
    dep = company.yandex
    staff_no_bp = StaffFactory()
    HeadcountPositionFactory(code=1234, department=dep, current_person=staff_no_bp)
    staff_correct_bp = StaffFactory(budget_position=bp_model)
    HeadcountPositionFactory(code=bp_model.code, department=dep, current_person=staff_correct_bp)
    staff_incorrect_bp = StaffFactory(budget_position=bp_model)
    HeadcountPositionFactory(code=12345, department=dep, current_person=staff_incorrect_bp)

    incorrect_vacancies = dep_models.Vacancy.objects.exclude(
        headcount_position_code=F('budget_position__code')
    ).filter(budget_position__code__isnull=True)
    incorrect_persons = dep_models.HeadcountPosition.objects.exclude(
        code=F('current_person__budget_position__code')
    ).filter(current_person__budget_position__code__isnull=True)
    assert not incorrect_vacancies.exists() and not incorrect_persons.exists()


@pytest.mark.django_db
def test_push_finished_workflow(company):
    workflow = WorkflowModelFactory(status=WORKFLOW_STATUS.FINISHED)
    ChangeFactory(workflow=workflow, push_status=PUSH_STATUS.FINISHED)

    st_patch = patch('staff.budget_position.workflow_service.gateways.startrek_service.add_comment_task.delay')
    with st_patch as st_patch_:
        PushWorkflow()

        st_patch_.assert_not_called()

        workflow.refresh_from_db()
        assert workflow.status == WORKFLOW_STATUS.FINISHED


@pytest.mark.django_db
@pytest.mark.parametrize('workflow_code, femida_called', (
    ('8.0', True),
    ('8.1', False),
))
def test_push_not_finished_workflow(company, workflow_code, femida_called):
    workflow = WorkflowModelFactory(
        status=WORKFLOW_STATUS.QUEUED,
        code=workflow_code,
    )
    change = ChangeFactory(workflow=workflow)
    hc = HeadcountPositionFactory(code=change.budget_position.code)

    hr_analyst = company['dep1-hr-analyst']
    DepartmentStaffFactory(
        department=hc.department,
        staff=hr_analyst,
        role_id=DepartmentRoles.HR_ANALYST_TEMP.value,
    )

    femida_patch = patch('staff.budget_position.workflow_service.gateways.femida_service.FemidaService.close_vacancy')
    oebs_patch = patch('staff.budget_position.workflow_service.use_cases.push_workflow_to_oebs.PushWorkflowToOebs.push')
    with oebs_patch as oebs_patch_:
        with femida_patch as femida_patch_:
            PushWorkflow()

            if femida_called:
                femida_patch_.assert_called_once_with(workflow.id)
            else:
                femida_patch_.assert_not_called()
            oebs_patch_.assert_called_once_with(workflow.id, hr_analyst.id)

            workflow.refresh_from_db()
            assert workflow.status == WORKFLOW_STATUS.QUEUED


@pytest.mark.django_db
def test_push_workflow_femida_fails(company):
    workflow = WorkflowModelFactory(
        status=WORKFLOW_STATUS.QUEUED,
        code='8.0',
    )
    change = ChangeFactory(workflow=workflow)
    hc = HeadcountPositionFactory(code=change.budget_position.code)

    hr_analyst = company['dep1-hr-analyst']
    DepartmentStaffFactory(
        department=hc.department,
        staff=hr_analyst,
        role_id=DepartmentRoles.HR_ANALYST_TEMP.value,
    )

    femida_patch = patch(
        'staff.budget_position.workflow_service.gateways.femida_service.FemidaService.close_vacancy',
        side_effect=FemidaError('По переданной вакансии уже выставлен оффер.'),
    )
    oebs_patch = patch('staff.budget_position.workflow_service.use_cases.push_workflow_to_oebs.PushWorkflowToOebs.push')
    with oebs_patch as oebs_patch_:
        with femida_patch as femida_patch_:
            PushWorkflow()

            femida_patch_.assert_called_once_with(workflow.id)
            oebs_patch_.assert_not_called()

            workflow.refresh_from_db()
            assert workflow.status == WORKFLOW_STATUS.QUEUED


@pytest.mark.django_db
def test_push_workflow_exception(company):
    workflow1 = WorkflowModelFactory(status=WORKFLOW_STATUS.QUEUED)
    workflow2 = WorkflowModelFactory(status=WORKFLOW_STATUS.QUEUED)
    use_case_patch = patch(
        'staff.budget_position.workflow_service.use_cases.PushWorkflow.push_workflow',
        side_effect=Exception(),
    )

    with use_case_patch as use_case_patch_:
        PushWorkflow()

        use_case_patch_.assert_has_calls([
            call.push_workflow(workflow1.id),
            call.push_workflow(workflow2.id),
        ])


@pytest.mark.django_db
def test_try_send_notification_for_workflow_exception(company):
    workflow1 = WorkflowModelFactory(status=WORKFLOW_STATUS.SENDING_NOTIFICATION)
    workflow2 = WorkflowModelFactory(status=WORKFLOW_STATUS.SENDING_NOTIFICATION)
    use_case_patch = patch(
        'staff.budget_position.workflow_service.use_cases.PushWorkflow.try_send_notification_for_workflow',
        side_effect=Exception(),
    )

    with use_case_patch as use_case_patch_:
        PushWorkflow()

        use_case_patch_.assert_has_calls([
            call.try_send_notification_for_workflow(workflow1.id),
            call.try_send_notification_for_workflow(workflow2.id),
        ])
