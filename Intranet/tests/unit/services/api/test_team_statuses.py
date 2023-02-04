import json
from unittest.mock import patch

import pytest
from django.urls import reverse

from plan.idm import exceptions

from plan.staff.models import Staff
from common import factories
from utils import source_path

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('language', ('ru', 'en'))
def test_team_statuses(client, django_assert_num_queries, language, staff_factory):
    service = factories.ServiceFactory()
    requester = staff_factory(lang_ui=language)
    client.login(requester.login)

    users = [factories.StaffFactory() for _ in range(6)]
    users[0].first_name = 'Иванушка'
    users[0].first_name_en = 'Ivanushka'
    users[0].last_name = 'Дурачок'
    users[0].last_name_en = 'Fool'
    users[0].save()

    sm0 = factories.ServiceMemberFactory(service=service, staff=users[0])
    sm3 = factories.ServiceMemberFactory(service=service, staff=users[3])
    path = source_path(
        'intranet/plan/tests/test_data/idm_api/idm_roles.json'
    )
    idm_answer = json.loads(open(path).read())
    idm_answer[0]['require_approve_from'][0][0]['username'] = users[1].login
    idm_answer[0]['require_approve_from'][0][1]['username'] = users[2].login
    idm_answer[0]['user']['username'] = users[0].login
    idm_answer[0]['node']['data']['role'] = sm0.role.pk
    idm_answer[1]['user']['username'] = users[3].login
    idm_answer[1]['node']['data']['role'] = sm3.role.pk

    with patch('plan.api.idm.actions.get_roles') as get_roles:
        get_roles.side_effect = [idm_answer, exceptions.BadRequest()]

        # 2: Middleware
        # 6: Role, Staff, Service(2), ServiceMember, ServiceMemberDepartment
        # 2: pg_is_in_recovery + waffle
        with django_assert_num_queries(10):
            response = client.json.get(reverse('api-frontend:service-team-statuses-detail', args=[service.id]))
        assert response.status_code == 200

    result = response.json()

    assert len(result['active']['persons']) == 1
    assert len(result['active']['departments']) == 0
    assert len(result['inactive']['persons']) == 1
    assert len(result['inactive']['departments']) == 0

    person1 = result['inactive']['persons'][0]
    assert person1['id'] == idm_answer[0]['id']
    assert person1['actions'] == ['member_approve', 'member_decline', 'member_remove', 'member_rerequest']
    assert person1['raw_state']['value'] == 'requested'
    assert person1['expires'] is None
    assert person1['fromDepartment'] is None
    assert person1['serviceMemberDepartmentId'] is None
    assert person1['person']['login'] == users[0].login
    assert person1['person']['firstName'] == users[0].i_first_name
    assert person1['person']['lastName'] == users[0].i_last_name
    assert person1['state'] == 'waiting_approval'
    assert len(person1['approvers']) == 2
    assert {a['login'] for a in person1['approvers']} == {users[1].login, users[2].login}
    assert person1['role']['id'] == sm0.role.pk

    person2 = result['active']['persons'][0]
    assert person2['id'] == sm3.id
    assert person2['expires'] == idm_answer[1]['expire_at']
    assert person2['raw_state']['value'] == 'need_request'
    assert person2['actions'] == []
    assert person2['state'] is None


def test_departments_team_statuses(client, django_assert_num_queries, owner_role, staff_factory):
    service = factories.ServiceFactory()
    requester = staff_factory()
    factories.ServiceMemberFactory(service=service, staff=requester, role=owner_role)
    client.login(requester.login)

    users = [factories.StaffFactory() for _ in range(4)]

    role = factories.RoleFactory()

    parent_department = factories.DepartmentFactory()
    users[0].department = parent_department
    users[0].save()
    users[1].department = parent_department
    users[1].save()
    child_department = factories.DepartmentFactory(parent=parent_department)
    users[2].department = child_department
    users[2].save()

    other_department = factories.DepartmentFactory()
    users[3].department = other_department
    users[3].save()

    path = source_path(
        'intranet/plan/tests/test_data/idm_api/idm_group_role.json'
    )
    idm_answer = json.loads(open(path).read())
    idm_answer[0]['group']['id'] = parent_department.staff_id
    idm_answer[0]['node']['data']['role'] = role.pk
    idm_answer[1]['group']['id'] = other_department.staff_id
    idm_answer[1]['node']['data']['role'] = role.pk

    with patch('plan.api.idm.actions.get_roles') as get_roles:
        get_roles.side_effect = [idm_answer, exceptions.BadRequest()]

        # 2: Middleware
        # 9: Role, Service(2), Department, DepartmentsClosure, Staff(2), ServiceMember, ServiceMemberDepartment
        # 2: pg_is_in_recovery + waffle
        with django_assert_num_queries(13):
            response = client.json.get(reverse('api-frontend:service-team-statuses-detail', args=[service.id]))
        assert response.status_code == 200

    result = response.json()

    assert len(result['active']['persons']) == 0
    assert len(result['active']['departments']) == 0
    assert len(result['inactive']['persons']) == 4
    assert len(result['inactive']['departments']) == 2

    first_role_persons = [
        role
        for role in result['inactive']['persons']
        if role['serviceMemberDepartmentId'] == idm_answer[0]['id']
    ]
    second_role_persons = [
        role
        for role in result['inactive']['persons']
        if role['serviceMemberDepartmentId'] == idm_answer[1]['id']
    ]

    assert set(role['person']['login'] for role in first_role_persons) == {u.login for u in users[0:3]}
    assert set(role['person']['login'] for role in second_role_persons) == {users[3].login}

    for role in first_role_persons:
        assert Staff.objects.filter(login=role['person']['login']).exists()
        assert role['fromDepartment']['id'] == parent_department.id
    for role in second_role_persons:
        assert Staff.objects.filter(login=role['person']['login']).exists()
        assert role['fromDepartment']['id'] == other_department.id

    department = result['inactive']['departments'][0]
    assert department['id'] == parent_department.id
    assert department['raw_state']['value'] == 'requested'
    assert department['serviceMemberDepartmentId'] == idm_answer[0]['id']
    assert department['name'] == parent_department.i_name
    assert department['state'] == 'new'
    assert not department['isApproved']
    assert department['approvers'] == []
    assert set(department['actions']) == {'department_approve', 'department_decline', 'department_rerequest'}


@pytest.mark.parametrize('key', [123, 'abracadabra', -1])
def test_404_on_wrong_key(client, key):
    response = client.json.get(reverse('api-frontend:service-team-statuses-detail', args=[key]))
    assert response.status_code == 404
