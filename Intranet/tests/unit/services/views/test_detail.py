import json
import operator

import pretend
import pytest
from django.conf import settings
from django.urls import reverse
from mock import patch

from plan.denormalization.check import check_obj_with_denormalized_fields
from plan.idm import exceptions as idm_exceptions
from plan.services.models import Service
from plan.services.views.catalog.serializers import IDMSerializer
from plan.services.views.detail import ServiceDetailView
from plan.services.constants.action import MEMBER_FIELDS
from common import factories
from plan.common.internal_roles import get_internal_roles
from utils import source_path

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, staff_factory):
    user1 = factories.UserFactory()
    staff1 = staff_factory(
        user=user1,
        first_name='Фродо',
        first_name_en='Frodo',
        last_name='Бэггинс',
        last_name_en='Beggins',
    )
    user2 = factories.UserFactory()
    staff2 = staff_factory(user=user2, first_name='Сэм', last_name='Гэмджи')
    user3 = factories.UserFactory()
    staff3 = staff_factory(user=user3, first_name='User', last_name='3', affiliation='external')
    user4 = factories.UserFactory()
    staff4 = staff_factory(user=user4, first_name='User', last_name='4', is_robot=True)

    service = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    role1 = factories.RoleFactory(service=service)
    role2 = factories.RoleFactory(service=service)
    service_member1 = factories.ServiceMemberFactory(
        service=service,
        staff=staff1,
        role=role1,
    )
    department = factories.DepartmentFactory()
    department_sm = factories.ServiceMemberDepartmentFactory(
        service=service,
        department=department,
    )
    service_member2 = factories.ServiceMemberFactory(
        service=service,
        staff=staff2,
        role=role1,
        from_department=department_sm,
    )

    fixture = pretend.stub(
        service=service,
        department=department,
        role1=role1,
        role2=role2,
        staff1=staff1,
        staff2=staff2,
        staff3=staff3,
        staff4=staff4,
        service_member1=service_member1,
        service_member2=service_member2,
        department_sm=department_sm,
    )

    check_obj_with_denormalized_fields(service, Service.DENORMALIZED_FIELDS, fix=True)
    return fixture


@pytest.mark.parametrize('staff_role', (
    'own_only_viewer',
    'services_viewer',
    'full_access',
))
def test_get_service_new_roles(client, staff_role, staff_factory):
    service = factories.ServiceFactory(slug='visible')
    other_service = factories.ServiceFactory(slug='invisible')

    staff = staff_factory(staff_role)
    role = factories.RoleFactory()
    factories.ServiceMemberFactory(staff=staff, role=role, service=service)

    def substitute(fields):
        substitution = {
            'available_states': 'availableStates',
            'incoming_move_requests': 'incomingMoveRequests',
            'outgoing_move_request': 'outgoingMoveRequest',
            'chown_request': 'chownRequest',
            'subservice_count': 'subserviceCount',
            'unique_members_count': 'teamCount',
            'unique_immediate_members_count': 'teamImmediateCount',
            'unique_immediate_robots_count': 'teamImmediateRobotsCount',
            'unique_immediate_external_members_count': 'immediateExternalCount',
            'is_important': 'isImportant',
        }
        return {
            substitution[field] if substitution.get(field) else field
            for field in fields
        }

    permissions = get_internal_roles(staff.user)
    all_fields = {
        'id', 'name', 'level', 'type', 'slug', 'ancestors', 'readonly_state', 'is_exportable', 'is_suspicious',
        'state', 'available_states', 'incoming_move_requests', 'outgoing_move_request', 'chown_request', 'owner',
        'team', 'departments', 'responsible', 'team_statuses', 'description', 'activity', 'contacts', 'url', 'actions',
        'subservice_count', 'unique_members_count', 'unique_immediate_members_count', 'unique_immediate_robots_count',
        'unique_immediate_external_members_count', 'kpi', 'is_important', 'tags', 'has_external_members'
    }
    visible_fields = {
        f
        for p in permissions
        for f in ServiceDetailView.PERMISSION_CODENAME_MAPPER.get(p) or ()
    }
    idm_fields = {'team_statuses', 'chown_request'}

    def get_service(service, fields):
        return client.json.get('/services/{}/'.format(service.id), {'fields': ','.join(fields)})

    client.login(staff.login)

    response = get_service(other_service, all_fields - idm_fields)
    assert response.status_code == (404 if staff_role == 'own_only_viewer' else 200)

    response = get_service(service, {'owner'})
    assert response.status_code == 200
    assert set(response.json()['content']['service']) == substitute({'owner'})

    response = get_service(service, {'kpi'})
    assert response.status_code == 200
    assert set(response.json()['content']['service']) == substitute(
        {'kpi'}
        if staff_role == 'full_access'
        else {}
    )

    response = get_service(service, all_fields - idm_fields)
    assert response.status_code == 200
    assert set(response.json()['content']['service']) == substitute(visible_fields - idm_fields - {'empty'})


@pytest.mark.postgresql
def test_case_insensitive(client, data):
    client.login(data.staff1.login)

    url = '/services/%s/' % data.service.slug.upper()
    response = client.json.get(url, {'fields': 'name,slug'})
    assert response.status_code == 200

    assert response.json()['content']['service']['slug'] == data.service.slug


def test_some_fields(client, data):
    client.login(data.staff1.login)

    url = '/services/%s/' % data.service.slug
    response = client.json.get(url, {'fields': 'slug,is_exportable,is_suspicious'})
    assert response.status_code == 200

    result = response.json()['content']['service']
    assert result['slug'] == data.service.slug
    assert result['is_exportable'] == data.service.is_exportable
    assert result['is_suspicious'] == data.service.is_suspicious


def test_actions(client, data):
    client.login(data.staff1.login)
    url = '/services/%s/' % data.service.slug
    response = client.json.get(url, {'fields': 'actions'})
    assert response.status_code == 200

    action_keys = {list(item.keys())[0] for item in response.json()['content']['service']['actions']}
    expected = {
        'edit_state',
        'edit_name',
        'member_rerequest',
        'department_remove',
        'contact_add',
        'department_rerequest',
        'chown',
        'chown_cancel',
        'contacts_replace',
        'contact_remove',
        'member_add_many',
        'member_remove',
        'contact_edit',
        'request_resource'
    }
    assert action_keys == expected


def test_kpi(client, data):
    data.service.kpi_bugs_count = 1
    data.service.kpi_release_count = 2
    data.service.kpi_lsr_count = 3
    data.service.save()

    client.login(data.staff1.login)

    url = '/services/%s/' % data.service.slug
    response = client.json.get(url, {'fields': 'kpi'})
    assert response.status_code == 200

    result = response.json()['content']['service']

    assert result['kpi']['bugs_count'] == 1
    assert result['kpi']['releases_count'] == 2
    assert result['kpi']['lsr_count'] == 3


def test_service_has_external_members(client, data):
    url = '/services/%s/' % data.service.slug
    response = client.json.get(url, {'fields': 'has_external_members'})
    assert response.status_code == 200

    assert response.json()['content']['service']['has_external_members'] == data.service.has_external_members

    factories.ServiceMemberFactory(
        service=data.service,
        staff=data.staff3,
        role=data.role1,
    )

    check_obj_with_denormalized_fields(data.service, Service.DENORMALIZED_FIELDS, fix=True)
    response = client.json.get(url, {'fields': 'has_external_members'})
    assert response.json()['content']['service']['has_external_members'] == data.service.has_external_members


def test_service_count_robot(client, data):
    url = '/services/%s/' % data.service.slug

    def up(service, url):
        check_obj_with_denormalized_fields(service, Service.DENORMALIZED_FIELDS, fix=True)
        response = client.json.get(url, {'fields': ','.join([
            'unique_immediate_robots_count',
            'unique_immediate_members_count',
            'unique_immediate_external_members_count'
        ])})
        assert response.status_code == 200
        return response

    # запомним количество людей и роботов в сервисе до добавления новых
    content_service = up(data.service, url).json()['content']['service']
    robots_count = content_service['teamImmediateRobotsCount']
    members_count = content_service['teamImmediateCount']
    external_members_count = content_service['immediateExternalCount']
    assert robots_count == data.service.unique_immediate_robots_count
    assert members_count == data.service.unique_immediate_members_count
    assert external_members_count == data.service.unique_immediate_external_members_count
    assert robots_count + members_count == data.service.members.team().values('staff').distinct().count()

    # добавляем одного робота
    # ожидаем, что увеличится только исходное количество роботов на единицу
    factories.ServiceMemberFactory(
        service=data.service,
        staff=data.staff4,
        role=data.role1,
    )

    content_service = up(data.service, url).json()['content']['service']
    assert content_service['teamImmediateRobotsCount'] == robots_count + 1
    assert content_service['teamImmediateCount'] == members_count

    # добавляем неробота
    # ожидаем, что увеличится только исходное количество людей на единицу
    factories.ServiceMemberFactory(
        service=data.service,
        staff=data.staff3,
        role=data.role1,
    )

    content_service = up(data.service, url).json()['content']['service']
    assert content_service['teamImmediateRobotsCount'] == robots_count + 1
    assert content_service['teamImmediateCount'] == members_count + 1

    # добавляем еще раз неробота с новой ролью
    # не ожидаем изменений
    factories.ServiceMemberFactory(
        service=data.service,
        staff=data.staff3,
        role=data.role2,
    )

    content_service = up(data.service, url).json()['content']['service']
    assert content_service['teamImmediateRobotsCount'] == robots_count + 1
    assert content_service['teamImmediateCount'] == members_count + 1

    # добавляем робота с другой ролью
    # не ожидаем изменений
    factories.ServiceMemberFactory(
        service=data.service,
        staff=data.staff3,
        role=data.role2,
    )

    content_service = up(data.service, url).json()['content']['service']
    assert content_service['teamImmediateRobotsCount'] == robots_count + 1
    assert content_service['teamImmediateCount'] == members_count + 1


def test_service_team(client, data):
    client.login(data.staff1.login)

    url = '/services/%s/' % data.service.slug
    response = client.json.get(url, {'fields': 'team'})
    assert response.status_code == 200

    team = response.json()['content']['service']['team']
    # пока в team нет никакой сортировки
    team = sorted(team, key=lambda member: member['person']['login'])

    assert team[0]['person']['login'] == data.staff1.login
    assert team[0]['person']['firstName'] == 'Frodo'
    assert team[0]['person']['lastName'] == 'Beggins'


def test_service_team_ru(client, data):
    client.login(data.staff1.login)

    url = '/services/%s/' % data.service.slug
    response = client.json.get(url, {'fields': 'team'}, HTTP_ACCEPT_LANGUAGE='ru')
    assert response.status_code == 200

    team = response.json()['content']['service']['team']
    # пока в team нет никакой сортировки
    team = sorted(team, key=lambda member: member['person']['login'])

    assert team[0]['person']['login'] == data.staff1.login
    assert team[0]['person']['firstName'] == 'Фродо'
    assert team[0]['person']['lastName'] == 'Бэггинс'

    assert team[0]['role']['id'] == data.role1.pk
    assert team[0]['role']['service'] == data.role1.service.pk
    assert team[0]['role_url'] == 'https://idm.test.yandex-team.ru/system/abc/roles#f-role=abc/services/' \
                                  'meta_other/*/%s,f-user=%s,f-ownership=personal' % (data.service_member1.role.id, data.staff1.login)
    assert team[0]['person']['is_robot'] == data.staff1.is_robot
    assert team[0]['person']['affiliation'] == data.staff1.affiliation

    assert team[1]['person']['login'] == data.staff2.login
    assert team[1]['person']['firstName'] == 'Сэм'
    assert team[1]['person']['lastName'] == 'Гэмджи'
    assert team[1]['serviceMemberDepartmentId'] == data.department_sm.id
    assert team[1]['role_url'] == 'https://idm.test.yandex-team.ru/system/abc/roles#f-role=abc/services/' \
                                  'meta_other/*/%s,f-user=%s,f-ownership=group' % (data.service_member2.role.id, data.staff2.login)
    assert team[1]['person']['is_robot'] == data.staff2.is_robot
    assert team[1]['person']['affiliation'] == data.staff2.affiliation


def test_duplicate_roles(client, data):
    # PLAN-4136
    factories.ServiceMemberFactory(
        service=data.service,
        staff=data.staff2,
        role=data.role1,
        from_department=None,
    )

    url = '/services/%s/' % data.service.slug
    response = client.json.get(url, data={'fields': 'team'})
    assert response.status_code == 200

    result = response.json()['content']['service']
    assert list(result.keys()) == ['team']
    assert len(result['team']) == 3
    assert {member['person']['login'] for member in result['team']} == {data.staff1.login, data.staff2.login}


def test_department_roles(client, data):
    department_sm = factories.ServiceMemberDepartmentFactory(
        service=data.service,
        department=data.department,
        role=data.role1
    )
    sm = factories.ServiceMemberFactory(
        service=data.service,
        staff=data.staff1,
        role=data.role1,
        from_department=department_sm,
    )

    department2 = factories.DepartmentFactory()
    role2 = factories.RoleFactory()
    department_sm2 = factories.ServiceMemberDepartmentFactory(
        service=data.service,
        department=department2,
        role=role2
    )
    sm2 = factories.ServiceMemberFactory(
        service=data.service,
        staff=data.staff2,
        role=role2,
        from_department=department_sm2,
    )

    url = '/services/%s/' % data.service.slug
    response = client.json.get(url, data={'fields': 'team'})
    assert response.status_code == 200

    result = response.json()['content']['service']
    assert list(result.keys()) == ['team']
    team = [member for member in result['team'] if member['fromDepartment'] is not None]
    team.sort(key=lambda member: member['id'])

    assert len(team) == 3

    assert team[0]['id'] == data.service_member2.pk
    assert team[0]['role']['id'] == data.role1.pk
    assert team[0]['fromDepartment']['id'] == data.department.pk
    assert team[0]['serviceMemberDepartmentId'] == data.department_sm.pk

    assert team[1]['id'] == sm.pk
    assert team[1]['role']['id'] == data.role1.pk
    assert team[1]['fromDepartment']['id'] == data.department.pk
    assert team[1]['serviceMemberDepartmentId'] == department_sm.pk

    assert team[2]['id'] == sm2.pk
    assert team[2]['role']['id'] == role2.pk
    assert team[2]['fromDepartment']['id'] == department2.pk
    assert team[2]['serviceMemberDepartmentId'] == department_sm2.pk


def test_unknown_service(client, data):
    client.login(data.staff1.login)

    url = '/services/unexistent_service/'
    response = client.json.get(url, {'fields': 'team'})

    assert response.status_code == 404
    assert response.json() == {
        'content': {},
        'error': {
            'code': 'NOT_FOUND',
            'message': 'Service unexistent_service not found',
            'params': {},
        },
    }


def test_requested_roles(client, data, owner_role):
    requester = factories.StaffFactory()
    client.login(data.staff1.login)

    url = '/services/%s/' % data.service.slug
    path = source_path(
        'intranet/plan/tests/test_data/idm_api/role_with_approvers.json'
    )
    idm_answer = json.loads(open(path).read())
    idm_answer[0]['require_approve_from'][0][0]['username'] = data.staff1.login
    idm_answer[0]['require_approve_from'][0][1]['username'] = data.staff2.login
    idm_answer[0]['user']['username'] = requester.login
    idm_answer[0]['node']['data']['role'] = data.role1.pk

    with patch('plan.api.idm.actions.get_roles') as get_roles:
        get_roles.side_effect = [idm_answer, idm_exceptions.BadRequest()]

        response = client.json.get(url, {'fields': 'team_statuses'})

    assert response.status_code == 200

    result = response.json()
    role_data = result['content']['service']['team_statuses']['inactive']['persons'][0]
    assert role_data['state'] == 'waiting_approval'
    assert role_data['expires'] is None
    assert role_data['role_url'] == 'https://idm.test.yandex-team.ru/system/abc/roles#role=10658601'
    assert role_data['person']['is_robot'] == requester.is_robot

    approvers = sorted(role_data['approvers'], key=operator.itemgetter('login'))
    expected_approvers = sorted([data.staff1, data.staff2], key=operator.attrgetter('login'))
    assert list(u['login'] for u in approvers) == list(u.login for u in expected_approvers)
    assert list(u['is_robot'] for u in approvers) == list(u.is_robot for u in expected_approvers)


def test_action_field_in_service(client, data):
    client.login(data.staff1.login)

    data.service.readonly_state = 'creating'
    data.service.save()

    url = '/services/%s/' % data.service.slug
    response = client.json.get(url, {'fields': 'actions,readonly_state'})
    assert response.status_code == 200

    result = response.json()['content']['service']
    assert result['readonly_state']['id'] == 'creating'

    for action in result['actions']:
        assert action not in MEMBER_FIELDS


@pytest.mark.parametrize('state, actions', (
    ('requested', ['approve', 'decline', 'rerequest']),
    ('depriving_validation', ['remove']),
))
def test_action_guess(state, actions):
    role = {
        'state': state,
    }
    guessed = set(IDMSerializer.guess_actions(role))
    assert guessed == set(actions)


def test_get_service_dehydrate_export(client, data):
    service = data.service

    with patch('plan.services.views.catalog.serializers.idm_actions') as idm_actions:
        idm_actions.get_chown_requests.return_value = []
        idm_actions.get_roles_with_permissions.return_value = []
        response = client.json.get(reverse('services:service', args=[service.pk]))

    required_keys = [
        'id', 'name', 'level', 'type', 'slug', 'ancestors',
        'readonly_state', 'is_exportable', 'is_suspicious', 'state',
        'availableStates', 'incomingMoveRequests', 'outgoingMoveRequest',
        'chownRequest', 'owner', 'team', 'departments', 'responsible', 'team_statuses',
        'description', 'activity', 'contacts', 'url', 'actions', 'subserviceCount',
        'teamCount', 'teamImmediateCount', 'teamImmediateRobotsCount', 'kpi',
        'isImportant', 'tags', 'has_external_members'
    ]
    data_keys = response.json()['content']['service'].keys()
    assert all([key in data_keys for key in required_keys])
