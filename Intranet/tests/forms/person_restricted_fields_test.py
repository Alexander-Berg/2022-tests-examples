import mock
import pytest

import json

from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.departments.controllers.proposal import ProposalCtl
from staff.departments.models import DepartmentRoles
from staff.lib.testing import OccupationFactory, DepartmentStaffFactory
from staff.oebs.tests.factories import JobFactory

from staff.proposal.proposal_builder import ProposalBuilder
from staff.proposal import views


@pytest.fixture
def get_proposal(rf):
    def inner(person, proposal_id: str):
        edit_url = reverse('proposal-api:edit-proposal', kwargs={'proposal_id': proposal_id})
        request = rf.get(edit_url)
        request.user = person.user
        return views.edit_proposal(request, proposal_id)

    return inner


@pytest.fixture
def post_proposal(rf):
    def inner(person, proposal_id: str, post_data):
        edit_url = reverse('proposal-api:edit-proposal', kwargs={'proposal_id': proposal_id})
        request = rf.post(edit_url, json.dumps(post_data), 'application/json')
        request.user = person.user
        setattr(request, 'service_is_readonly', False)
        setattr(request, 'csrf_processing_done', True)
        return views.edit_proposal(request, proposal_id)

    return inner


def test_user_without_rights_doesnt_see_fields(mocked_mongo, company, get_proposal):
    # given
    occupation = OccupationFactory()
    author = company['persons']['dep1-chief']
    person = company['persons']['dep12-person']

    proposal_id = (
        ProposalBuilder()
        .with_person(person.login, lambda person: person.occupation(occupation.name))
        .build(author.login)
    )

    # when
    response = get_proposal(author, proposal_id)

    # then
    assert response.status_code == 200
    data = json.loads(response.content)

    grade_structure = data['structure']['persons']['value']['actions']['structure']['value']['grade']['value']
    assert 'occupation' not in grade_structure
    assert 'force_recalculate_schemes' not in grade_structure

    grade_data = data['data']['persons']['value']['actions']['value'][0]['value']['grade']['value']
    assert 'occupation' in grade_data
    assert 'force_recalculate_schemes' in grade_data


def test_user_with_rights_see_fields(mocked_mongo, company, get_proposal):
    # given
    occupation = OccupationFactory()
    author = company['persons']['dep1-chief']
    DepartmentStaffFactory(staff=author, department=company.yandex, role_id=DepartmentRoles.HR_ANALYST.value)
    person = company['persons']['dep12-person']

    proposal_id = (
        ProposalBuilder()
        .with_person(person.login, lambda person: person.occupation(occupation.name))
        .build(author.login)
    )

    # when
    response = get_proposal(author, proposal_id)

    # then
    assert response.status_code == 200
    data = json.loads(response.content)

    grade_structure = data['structure']['persons']['value']['actions']['structure']['value']['grade']['value']
    assert 'occupation' in grade_structure
    assert 'force_recalculate_schemes' in grade_structure

    assert grade_structure['new_grade']['value'] == '0'
    assert grade_structure['occupation']['value'] == ''
    assert grade_structure['force_recalculate_schemes']['value'] is False

    grade_data = data['data']['persons']['value']['actions']['value'][0]['value']['grade']['value']
    assert 'occupation' in grade_data
    assert 'force_recalculate_schemes' in grade_data

    assert grade_data['new_grade']['value'] == '0'
    assert grade_data['occupation']['value'] == occupation.name
    assert grade_data['force_recalculate_schemes']['value'] is False


@mock.patch('staff.proposal.tasks.SyncProposalTickets')
def test_user_without_rights_edits_form_wo_fields(sync_mock, mocked_mongo, company, post_proposal):
    # given
    sync_mock.is_running = mock.Mock(return_value=False)
    occupation = OccupationFactory()
    author = company['persons']['dep1-chief']
    person = company['persons']['dep12-person']
    user_wo_rights = company['persons']['dep1-person']
    can_execute_permission = Permission.objects.get(codename='can_manage_department_proposals')
    user_wo_rights.user.user_permissions.add(can_execute_permission)

    proposal_id = (
        ProposalBuilder()
        .with_person(
            person.login,
            lambda builder: builder.occupation(occupation.name).grade('+2').staff_position('lol'),
        )
        .build(author.login)
    )

    proposal = ProposalCtl(proposal_id).proposal_object

    form = {
        'persons': {
            'actions': [
                {
                    'login': person.login,
                    'sections': ['position'],
                    'comment': 'comment',
                    'position': {
                        'new_position': 'lol2',
                        'position_legal': JobFactory().code,
                    },
                    'grade': {
                        'new_grade': '+1',
                    },
                    'action_id': proposal['persons']['actions'][0]['action_id'],
                }
            ]
        },
        'apply_at': '2030-12-01',
    }

    # when
    response = post_proposal(user_wo_rights, proposal_id, form)

    # then
    assert response.status_code == 200

    person_actions = list(ProposalCtl(proposal_id).person_action_objs)
    assert len(person_actions) == 1
    assert person_actions[0].grade.occupation == occupation.name
    assert person_actions[0].grade.new_grade == '+1'
