import copy
import datetime
import json

import mock
import pytest
from bson import ObjectId
from django.core.exceptions import PermissionDenied
from django.core.urlresolvers import reverse
from waffle.models import Switch

from staff.budget_position.const import WORKFLOW_STATUS
from staff.budget_position.workflow_service.gateways.workflow_repository import WorkflowRepository
from staff.departments.edit.constants import nearest_hr_dates
from staff.departments.edit.proposal_mongo import get_mongo_object, MONGO_COLLECTION_NAME
from staff.departments.models import Department, DepartmentKind, ProposalMetadata, DepartmentRoles
from staff.departments.tests.factories import VacancyMemberFactory
from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.lib.testing import OfficeFactory, OrganizationFactory, StaffFactory, DepartmentStaffFactory
from staff.person.models import Staff

from staff.proposal import views
from staff.proposal.models import CityAttrs
from staff.proposal.proposal_builder import ProposalBuilder


@pytest.fixture
def proposal_draft(tester):
    return {
        'apply_at': datetime.date.today().strftime('%Y-%m-%d'),
        'link_to_ticket': None,
        'description': None,
        'persons': {
            'actions': [{
                'action_id': 'act_97905',
                'comment': '',
                'login': tester.staff.login,
                'organization': {'organization': OrganizationFactory().id},
                'sections': ['organization'],
            }],
        },
    }


@pytest.fixture
def dep_action_draft(company):
    return {
        'action_id': 'act_86134',
        'administration': None,
        'fake_id': 'dep_42061',
        'hierarchy': {
            'fake_parent': '',
            'parent': company.yandex.url,
        },
        'name': {
            'hr_type': True,
            'name_en': 'asd',
            'name': 'asd',
        },
        'technical': {
            'category': 'technical',
            'code': 'dep42061',
            'department_type': DepartmentKind.objects.first().id,
            'order': None,
        },
        'url': None,
        'sections': [
            'name',
            'hierarchy',
            'administration',
            'technical',
        ],
    }


@pytest.fixture
def create_request(post_request, proposal_draft, mocked_mongo, robot_staff_user):
    def inner(data=()):
        with mock.patch('staff.proposal.tasks.create_proposal_tickets'):
            proposal = copy.deepcopy(proposal_draft)
            proposal.update(data)
            return views.add_proposal(post_request('proposal-api:add-proposal', proposal))
    return inner


@pytest.mark.django_db
def test_add_proposal(create_request, deadlines):
    res = create_request()
    assert res.status_code == 200, res.content
    proposal_uid = json.loads(res.content)['proposal_uid']
    assert get_mongo_object(proposal_uid)


@pytest.mark.django_db
def test_add_proposal_400_on_conflict_proposal(create_request, deadlines):
    create_request()
    res = create_request()
    assert res.status_code == 400, res.content
    err = json.loads(res.content)['errors'][''][0]
    assert err['code'] == 'bp_conflict_error' and err['params']['meta'], err


@pytest.mark.django_db
def test_update_proposal_404_on_conflict_proposal(
    create_request,
    dep_action_draft,
    post_request,
    proposal_draft,
    deadlines,
):
    Switch.objects.get_or_create(name='proposal_skip_ticket_creating', active=True)
    create_request()  # Создаём заявку с иземнением сотрудника tester

    create_resp = create_request({  # Создаём заявку с созданием подразделения внутри Яндекса
        'departments': {'actions': [dep_action_draft]},
        'persons': {'actions': []}
    })
    proposal_id = json.loads(create_resp.content)['proposal_uid']
    proposal_draft.update({'departments': {'actions': [dep_action_draft]}})

    res = views.edit_proposal(
        post_request('proposal-api:edit-proposal', proposal_draft, proposal_id=proposal_id),
        proposal_id,
    )

    assert res.status_code == 400, res.content
    err = json.loads(res.content)['errors'][''][0]
    assert err['code'] == 'bp_conflict_error' and err['params']['meta'], err


@pytest.mark.django_db
def test_can_edit_proposal_permission(create_request, proposal_draft, post_request, deadlines, mocked_mongo, rf):
    create_resp = create_request()
    proposal_id = json.loads(create_resp.content)['proposal_uid']
    mongo_obj = next(mocked_mongo.db[MONGO_COLLECTION_NAME].find(
        {'_id': ObjectId(proposal_id)}
    ))
    proposal_author = Staff.objects.get(id=mongo_obj['author'])
    not_author = StaffFactory(login='notauthor')

    edit_url = reverse('proposal-api:edit-proposal', kwargs={'proposal_id': proposal_id})
    request = rf.get(edit_url)

    request.user = proposal_author.user
    response = views.edit_proposal(request, proposal_id)
    assert response.status_code == 200

    request.user = not_author.user
    with pytest.raises(PermissionDenied):
        assert views.edit_proposal(request, proposal_id)


@pytest.mark.django_db
def test_delete_proposal(create_request, post_request, get_request, deadlines):
    CityAttrs.objects.create(city=None, component=None, hr=None)
    res = create_request()
    proposal_uid = json.loads(res.content)['proposal_uid']

    res = views.delete(
        post_request('proposal-api:delete-proposal', {}, proposal_id=proposal_uid),
        proposal_uid,
    )
    assert res.status_code == 202, res.content


@pytest.mark.django_db
def test_get_flow_context_proposal(create_request, post_request, get_request, deadlines):
    res = create_request()
    proposal_uid = json.loads(res.content)['proposal_uid']

    res = views.debug_flow_context(
        get_request('proposal-api:debug-flow-context', proposal_id=proposal_uid),
        proposal_uid,
    )

    assert res.status_code == 200, res.content
    assert json.loads(res.content)['params'].get('person_actions')


@pytest.mark.django_db
def test_create_with_dep_existing_in_deleted_proposal(create_request, post_request, company, deadlines):
    dep11 = Department.objects.get(url='yandex_dep1_dep11')
    with_dep_data = {
        'departments': {
            'actions': [
                {
                    'url': dep11.url,
                    'fake_id': '',
                    'delete': False,
                    'sections': ['name'],
                    'action_id': 'act_23759',
                    'name': {
                         'name_en': 'HR Department',
                         'name': 'HR-департамент 1',
                         'hr_type': True,
                     },
                },
            ],
        },
    }
    with mock.patch('staff.proposal.views.views.tasks.create_proposal_tickets', mock.MagicMock(return_value=None)):
        res = create_request(with_dep_data)
    proposal_uid = json.loads(res.content)['proposal_uid']
    views.delete(post_request('proposal-api:delete-proposal', {}, proposal_id=proposal_uid), proposal_uid)

    with mock.patch('staff.proposal.views.views.tasks.create_proposal_tickets', mock.MagicMock(return_value=None)):
        res = create_request(with_dep_data)
    assert res.status_code == 200, res.content


@pytest.mark.django_db
def test_add_person_action(create_request, proposal_draft, post_request, deadlines):
    create_resp = create_request()
    proposal_uid = json.loads(create_resp.content)['proposal_uid']

    new_staff = StaffFactory()
    new_action = {
        'action_id': 'act_97942',
        'comment': '',
        'login': new_staff.login,
        'organization': {'organization': OrganizationFactory().id},
        'sections': ['organization'],
    }
    draft_actions = proposal_draft['persons']['actions']
    draft_actions.append(new_action)

    with mock.patch('staff.proposal.tasks.SyncProposalTickets', mock.MagicMock()) as m:
        m.is_running.return_value = False

        res = views.edit_proposal(
            post_request('proposal-api:edit-proposal', proposal_draft, proposal_id=proposal_uid),
            proposal_uid,
        )
    assert res.status_code == 200, res.content

    mongo_object = get_mongo_object(proposal_uid)
    db_action_ids = {act['action_id'] for act in mongo_object['persons']['actions']}
    draft_action_ids = {act['action_id'] for act in draft_actions}
    assert db_action_ids == draft_action_ids

    postgres_id = ProposalMetadata.objects.get(proposal_id=proposal_uid)
    workflows = WorkflowRepository().get_workflows_by_proposal_id(postgres_id)
    assert workflows

    budget_positions = set()
    for workflow in workflows:
        assert len(workflow.changes) == 1
        change = workflow.changes[0]
        budget_positions.add(change.budget_position.id)

    staff_bps = set(
        Staff.objects
        .filter(login__in=[it['login'] for it in draft_actions])
        .values_list('budget_position_id', flat=True)
    )

    assert budget_positions == staff_bps


@mock.patch('staff.proposal.tasks.SyncProposalTickets')
@pytest.mark.django_db
def test_change_person_action(sync_mock, create_request, proposal_draft, post_request, deadlines):
    sync_mock.is_running = mock.Mock(return_value=False)
    create_resp = create_request()
    proposal_uid = json.loads(create_resp.content)['proposal_uid']

    action = proposal_draft['persons']['actions'][0]
    action['sections'].append('office')
    action['office'] = {'office': OfficeFactory().id}
    tester_person = Staff.objects.get(login='tester')

    action['__department_chain__'] = [tester_person.department.url]

    res = views.edit_proposal(
        post_request('proposal-api:edit-proposal', proposal_draft, proposal_id=proposal_uid),
        proposal_uid,
    )
    assert res.status_code == 200, res.content

    mongo_object = get_mongo_object(proposal_uid)
    assert mongo_object['persons']['actions'][0] == action


@pytest.mark.django_db
def test_remove_person_action(
    create_request,
    proposal_draft,
    dep_action_draft,
    post_request,
    deadlines,
):
    # need to add dep action - proposal have to have at least one any action after editing
    proposal_draft.update({'departments': {'actions': [dep_action_draft]}})
    create_resp = create_request(proposal_draft)
    proposal_uid = json.loads(create_resp.content)['proposal_uid']
    proposal_draft['persons']['actions'] = []

    with mock.patch('staff.proposal.tasks.SyncProposalTickets', mock.MagicMock()) as m:
        m.is_running.return_value = False
        res = views.edit_proposal(
            post_request('proposal-api:edit-proposal', proposal_draft, proposal_id=proposal_uid),
            proposal_uid,
        )
    assert res.status_code == 200, res.content

    mongo_object = get_mongo_object(proposal_uid)
    assert not mongo_object['persons']['actions']

    postgres_id = ProposalMetadata.objects.get(proposal_id=proposal_uid)
    assert all(
        workflow.status != WORKFLOW_STATUS.PENDING
        for workflow in WorkflowRepository().get_workflows_by_proposal_id(postgres_id)
    )


@mock.patch('staff.departments.controllers.tickets.restructurisation.RestructurisationTicket.get_components')
def test_save_vacancies_data_to_storage(_, company, vacancies, mocked_mongo, create_request, deadlines, tester):
    _.return_value = [], set()

    vacancy_id, vacancy = next(iter(vacancies.items()))
    VacancyMemberFactory(vacancy=vacancy, person=tester.get_profile())
    move_vacansion_data = {
        'actions': [],
        'vacancies': {
            'actions': [
                {
                    'vacancy_id': vacancy_id,
                    'department': 'yandex_dep1_dep11',
                    'fake_department': '',
                    'action_id': 'act_69869',
                },
            ],
        },
        'apply_at': datetime.date.today().strftime('%Y-%m-%d'),
        'apply_at_hr': nearest_hr_dates()[1][0],
    }
    with mock.patch('staff.departments.controllers.tickets.restructurisation.create_issue') as m:
        m.return_value = mock.MagicMock(key='TICKET-12345')
        response = create_request(move_vacansion_data)
    assert response.status_code == 200
    content = json.loads(response.content)
    assert 'proposal_uid' in content
    proposal_uid = content['proposal_uid']
    mongo_obj = next(mocked_mongo.db[MONGO_COLLECTION_NAME].find(
        {'_id': ObjectId(proposal_uid)}
    ))

    assert len(mongo_obj['vacancies']['actions']) == 1
    assert mongo_obj['vacancies']['actions'][0] == {
        'department': 'yandex_dep1_dep11',
        'vacancy_id': vacancy_id,
        'action_id': 'act_69869',
        'fake_department': '',
        '__department_chain__': ['yandex'],
        'value_stream': None,
        'force_recalculate_schemes': None,
    }


@mock.patch('staff.departments.controllers.tickets.restructurisation.RestructurisationTicket.get_components')
def test_save_headcounts_data_to_storage(_, company, mocked_mongo, create_request, deadlines, tester):
    _.return_value = [], set()

    # given
    DepartmentStaffFactory(
        department=company.yandex,
        staff=tester.get_profile(),
        role_id=DepartmentRoles.HR_ANALYST.value
    )
    hp = HeadcountPositionFactory(department=company.dep1)
    move_headcount_data = {
        'actions': [],
        'headcounts': {
            'actions': [
                {
                    'headcount_code': hp.code,
                    'department': company.dep2.url,
                    'fake_department': '',
                    'action_id': 'act_69869',
                },
            ],
        },
        'apply_at': datetime.date.today().strftime('%Y-%m-%d'),
        'apply_at_hr': nearest_hr_dates()[1][0],
    }

    # when
    with mock.patch('staff.departments.controllers.tickets.restructurisation.create_issue') as m:
        m.return_value = mock.MagicMock(key='TICKET-12345')
        response = create_request(move_headcount_data)

    # then
    assert response.status_code == 200
    content = json.loads(response.content)
    assert 'proposal_uid' in content
    proposal_uid = content['proposal_uid']
    mongo_obj = next(mocked_mongo.db[MONGO_COLLECTION_NAME].find(
        {'_id': ObjectId(proposal_uid)}
    ))

    assert len(mongo_obj['headcounts']['actions']) == 1
    assert mongo_obj['headcounts']['actions'][0] == {
        'department': company.dep2.url,
        'headcount_code': hp.code,
        'action_id': 'act_69869',
        'fake_department': '',
        '__department_chain__': [company.yandex.url, company.dep1.url],
        'value_stream': None,
        'force_recalculate_schemes': None,
    }


def test_load_vacancies_data_from_storage(company, vacancies, mocked_mongo, get_request, tester):
    vacancy_id = next(iter(vacancies.keys()))
    proposal_uid = mocked_mongo.db[MONGO_COLLECTION_NAME].insert_one(
        {
            'tickets': {
                'department_ticket': '',
                'department_linked_ticket': '',
                'deleted_persons': {},
                'restructurisation': '',
                'persons': {},
            },
            'description': '',
            'author': tester.get_profile().id,
            'created_at': '2019-02-28T11:54:49.437',
            'pushed_to_oebs': None,
            'updated_at': '2019-02-28T11:54:49.437',
            'actions': [],
            'persons': {
                'actions': [],
            },
            'root_departments': [],
            'apply_at_hr': '2018-05-02',
            'apply_at': datetime.datetime(2019, 2, 28, 0, 0),
            'vacancies': {
                'actions': [
                    {
                        'department': 'yandex_dep1_dep11',
                        'vacancy_id': vacancy_id,
                        'action_id': 'act_69869',
                        'fake_department': '',
                    },
                ],
            },
            '_prev_actions_state': {},
        }
    ).inserted_id
    proposal_uid = str(proposal_uid)
    ProposalMetadata.objects.create(proposal_id=proposal_uid)
    request = get_request(to_reverse='proposal-api:edit-proposal', proposal_id=proposal_uid)
    response = views.edit_proposal(request, proposal_uid)

    assert response.status_code == 200
    content = json.loads(response.content)
    vacancy_proposal_data = content['data']['vacancies']['value']['actions']['value'][0]['value']  # SForm format
    assert vacancy_proposal_data['vacancy_id']['value'] == vacancy_id
    assert vacancy_proposal_data['department']['value'] == 'yandex_dep1_dep11'
    assert vacancy_proposal_data['fake_department']['value'] == ''


def test_load_headcounts_data_from_storage(company, mocked_mongo, get_request, tester):
    # given
    DepartmentStaffFactory(
        department=company.yandex,
        staff=tester.get_profile(),
        role_id=DepartmentRoles.HR_ANALYST.value
    )
    hp = HeadcountPositionFactory(department=company.dep1)

    proposal_id = (
        ProposalBuilder()
        .for_existing_department(
            company.dep11.url,
            lambda department: department.use_for_headcount(hp.code, lambda headcount: headcount)
        )
        .build(company.persons['yandex-chief'].login)
    )

    # when
    request = get_request(to_reverse='proposal-api:edit-proposal', proposal_id=proposal_id)
    response = views.edit_proposal(request, proposal_id)

    # then
    assert response.status_code == 200
    content = json.loads(response.content)
    headcount_data = content['data']['headcounts']['value']['actions']['value'][0]['value']  # SForm format
    assert headcount_data['headcount_code']['value'] == hp.code
    assert headcount_data['department']['value'] == company.dep11.url
    assert headcount_data['fake_department']['value'] == ''
