import copy
from datetime import datetime

import pytest
from waffle.models import Switch

from staff.budget_position.const import WORKFLOW_STATUS
from staff.budget_position.workflow_service.gateways.workflow_repository import WorkflowRepository
from staff.departments.controllers.proposal_execution import ProposalExecution
from staff.departments.models import ProposalMetadata
from staff.lib.testing import DepartmentKindFactory
from staff.oebs.tests.factories import JobFactory
from staff.person.models import Staff

from staff.departments.controllers.exceptions import BpConflict
from staff.departments.controllers.proposal import ProposalCtl
from staff.proposal.proposal_builder import ProposalBuilder


@pytest.fixture
def proposal_form(proposal_skeleton, person_from_ya, company):
    proposal_skeleton['actions'].append({
        'fake_id': '12345678',
        'hierarchy': {
            'fake_parent': '',
            'parent': company.yandex.id,
        },
        'id': None,
        'delete': False,
        'name': {
            'name': 'some name',
            'name_en': 'some name',
        },
        'technical': {
            'code': 'rndnm',
            'position': 0,
            'kind': DepartmentKindFactory().id,
        },
    })
    proposal_skeleton['persons']['actions'].append({
        'sections': ['position', 'department'],
        'login': person_from_ya.login,
        'position': {'new_position': 'pseudo random generator', 'position_legal': JobFactory().code},
        'department': {'fake_department': '12345678', 'department': None},
        'action_id': 'act_2134',
    })
    return proposal_skeleton


def test_check_creating_bp_change(company, person_from_ya, mocked_mongo, deadlines):
    proposal_id = (
        ProposalBuilder().for_new_department(
            'name123',
            lambda department: department.set_parent(company.yandex.url).use_for_person(
                person_from_ya.login,
                lambda person: person.staff_position('pseudo random generator').legal_position(JobFactory().code),
            )
        )
        .build(person_from_ya.login)
    )

    postgres_id = ProposalMetadata.objects.get(proposal_id=proposal_id).id
    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id, status=WORKFLOW_STATUS.PENDING)
    assert len(workflows) == 1
    workflow = workflows[0]

    assert len(workflow.changes) == 1

    change = workflow.changes[0]

    assert change.budget_position.id == person_from_ya.budget_position_id


def test_changing_existing_bp_changes(company, proposal_form, person_from_ya, deadlines):
    p_ctl = ProposalCtl(author=person_from_ya).create_from_cleaned_data(proposal_form)
    proposal_id = p_ctl.save()
    postgres_id = ProposalMetadata.objects.get(proposal_id=proposal_id).id

    workflows_before = WorkflowRepository().get_workflows_by_proposal_id(postgres_id, status=WORKFLOW_STATUS.PENDING)
    assert len(workflows_before) == 1
    workflow_before = workflows_before[0]

    another_person = next(it for it in company.persons.values() if it != person_from_ya)
    new_proposal_state = copy.deepcopy(proposal_form)
    new_proposal_state['persons']['actions'] = [{
        'sections': ['position'],
        'login': another_person.login,
        'position': {'new_position': 'just another position'},
        'action_id': 'act_2135',
    }]
    p_ctl.update(new_proposal_state)
    p_ctl.save()

    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id, status=WORKFLOW_STATUS.PENDING)
    assert len(workflows) == 1
    workflow = workflows[0]

    assert workflow.id != workflow_before.id

    workflow_before = WorkflowRepository().get_by_id(workflow_before.id)

    assert workflow_before.status == WORKFLOW_STATUS.CANCELLED


def test_executing_proposal_confirms_changes(proposal_form, person_from_ya, deadlines):
    proposal_form['apply_at'] = datetime.now()
    p_ctl = ProposalCtl(author=person_from_ya).create_from_cleaned_data(proposal_form)
    proposal_id = p_ctl.save()
    postgres_id = ProposalMetadata.objects.get(proposal_id=proposal_id).id

    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id, status=WORKFLOW_STATUS.PENDING)
    assert len(workflows) == 1

    ProposalExecution(p_ctl).execute()

    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id, status=WORKFLOW_STATUS.CONFIRMED)
    assert len(workflows) == 1


def test_bp_conflict_proposals(proposal_form, vacancies, person_from_ya, deadlines):
    vacancy = list(vacancies.values())[0]
    proposal_form['vacancies']['actions'].append({
        'vacancy_id': vacancy.id,
        'department': 'yandex_dep1_dep11',
        'fake_department': '',
        'action_id': 'act_69869',
    })
    proposal_id = ProposalCtl(author=person_from_ya).create_from_cleaned_data(proposal_form).save()
    person_login = proposal_form['persons']['actions'][0]['login']
    person_bp = Staff.objects.values_list('budget_position__code', flat=True).get(login=person_login)
    expect_data = [
        {
            'vacancy_id': vacancy.id,
            'proposal_id': proposal_id,
            'hrbps': ['dep1-hr-partner'],
            'bp_id': vacancy.budget_position.code,
            'author': person_from_ya.login,
        },
        {
            'author': person_from_ya.login,
            'proposal_id': proposal_id,
            'hrbps': ['dep1-hr-partner'],
            'bp_id': person_bp,
            'bp_login': person_login,
            'bp_conflict_login': person_login,
            'ticket': '',
        }
    ]

    with pytest.raises(BpConflict) as exc:
        ProposalCtl(author=person_from_ya).create_from_cleaned_data(proposal_form).save()

    _, exc, _ = exc._excinfo

    def key(dct):
        return dct['bp_id']

    assert sorted(exc.meta, key=key) == sorted(expect_data, key=key)


def test_ignore_bp_conflict_proposals(proposal_form, vacancies, person_from_ya, deadlines):
    Switch.objects.get_or_create(name='ignore_budget_position_conflicts', active=True)
    vacancy = list(vacancies.values())[0]
    proposal_form['vacancies']['actions'].append({
        'vacancy_id': vacancy.id,
        'department': 'yandex_dep1_dep11',
        'fake_department': '',
        'action_id': 'act_69869',
    })

    ProposalCtl(author=person_from_ya).create_from_cleaned_data(proposal_form).save()
    person_login = proposal_form['persons']['actions'][0]['login']
    Staff.objects.values_list('budget_position__code', flat=True).get(login=person_login)

    ProposalCtl(author=person_from_ya).create_from_cleaned_data(proposal_form).save()
    assert True


def test_correct_changes_states_after_proposal_executing(proposal_form, person_from_ya, deadlines):
    proposal_form['apply_at'] = datetime.now()
    p_ctl = ProposalCtl(author=person_from_ya).create_from_cleaned_data(proposal_form)
    proposal_id = p_ctl.save()
    postgres_id = ProposalMetadata.objects.get(proposal_id=proposal_id).id

    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id, status=WORKFLOW_STATUS.PENDING)
    assert len(workflows) == 1
    changes = workflows[0].changes

    assert len(changes) == 1
    change = changes[0]

    assert change.person_id == person_from_ya.id
    assert change.position_id == proposal_form['persons']['actions'][0]['position']['position_legal']
    assert change.department_id is None

    ProposalExecution(p_ctl).execute()

    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id, status=WORKFLOW_STATUS.CONFIRMED)
    assert len(workflows) == 1
    changes = workflows[0].changes

    assert len(changes) == 1
    change = changes[0]

    assert change.person_id == person_from_ya.id
    assert change.position_id == proposal_form['persons']['actions'][0]['position']['position_legal']
    assert change.department_id == p_ctl.proposal_object['actions'][0]['id']
