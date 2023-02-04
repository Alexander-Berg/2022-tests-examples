from datetime import date, timedelta

import pytest
from django.core.urlresolvers import reverse
from django.conf import settings
from mock import Mock, patch
from common import factories

from plan.idm import exceptions
from plan.services.models import ServiceMemberDepartment
from plan.services.state import SERVICEMEMBER_STATE
from plan.services import tasks

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-list',
    'api-v3:service-department-list',
    'api-v4:service-department-list',
    'api-frontend:service-department-list',
])
def test_get_department_members(client, data, endpoint_path):
    response = client.json.get(reverse(endpoint_path))

    assert response.status_code == 200

    json = response.json()['results']
    assert len(json) == 1
    assert json[0]['id'] == data.department_member.id


def test_get_department_members_count(client, data, django_assert_num_queries):
    role = factories.RoleFactory()
    department_member = factories.ServiceMemberDepartmentFactory(
        service=data.service, role=role,
    )

    for i in range(3):
        factories.StaffFactory(
            department=department_member.department
        )

    tasks.update_service_department_members()

    with django_assert_num_queries(6):
        # 1 staff
        # 1 permissions
        # 1 count members
        # 1 services_servicememberdepartment
        # 1 pg_is_in_recovery
        # 1 waffle
        response = client.json.get(
            reverse('api-frontend:service-department-list'),
            {'fields': 'id,members_count'},
        )

    assert response.status_code == 200

    json = response.json()['results']
    assert len(json) == 2
    assert json[0]['id'] == data.department_member.id
    assert {
        dep_obj['id']: dep_obj['members_count']
        for dep_obj in json
    } == {data.department_member.id: 1, department_member.id: 3}


def test_service_filter(client, data):
    response = client.json.get(reverse('services-api:department-list'))
    assert len(response.json()['results']) == 1

    response = client.json.get(reverse('services-api:department-list'), {'service': data.metaservice.id})
    assert len(response.json()['results']) == 0

    response = client.json.get(reverse('services-api:department-list'), {'service__slug': data.metaservice.slug})
    assert len(response.json()['results']) == 0

    response = client.json.get(reverse('services-api:department-list'), {'service': data.service.id})
    assert len(response.json()['results']) == 1

    response = client.json.get(reverse('services-api:department-list'), {'service__slug': data.service.slug})
    assert len(response.json()['results']) == 1


def test_descendants_filter(client, data):
    response = client.json.get(
        reverse('services-api:department-list'),
        {
            'service': data.metaservice.id,
            'descendants': True,
        }
    )

    json = response.json()['results']
    assert len(json) == 1
    assert json[0]['id'] == data.department_member.id


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-list', 'api-v3:service-department-list', 'api-v4:service-department-list',
])
def test_request_departments(client, data, endpoint_path):
    # Тест корректного запроса роли без доп полей

    client.login(data.staff.login)

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        request_membership.return_value = {'id': 1}
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.other_service.id,
                'department': data.department.id,
                'role': data.team_member.role.id
            }
        )

        assert response.status_code == 201

        assert request_membership.call_count == 1
        member_department = ServiceMemberDepartment.all_states.get(service=data.other_service, department=data.department)
        assert member_department.state == SERVICEMEMBER_STATE.REQUESTED
        assert (
            member_department.members(manager='all_states').filter(state=SERVICEMEMBER_STATE.REQUESTED).count() == 1
        )
        assert member_department.idm_role_id == 1
        request_membership.assert_called_with(
            data.other_service,
            data.department,
            data.team_member.role,
            comment='',
            deprive_after=None,
            deprive_at=None,
            requester=data.owner_of_service.staff,
            silent=False,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-list', 'api-v3:service-department-list', 'api-v4:service-department-list',
])
def test_request_departments_deprive(client, data, endpoint_path):
    # Тест запроса роли со взаимоисключающими полями

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.other_service.id,
                'department': data.department.id,
                'role': data.team_member.role.id,
                'deprive_after': 10,
                'deprive_at': date(2020, 12, 31).isoformat()
            }
        )

        assert response.status_code == 400
        assert request_membership.call_count == 0

    assert response.json()['error']['code'] == 'validation_error'
    assert response.json()['error']['detail'] == "Got mutually exclusive fields: ('deprive_at', 'deprive_after')"


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-list', 'api-v3:service-department-list', 'api-v4:service-department-list',
])
def test_request_departments_deprive_after(client, data, endpoint_path):
    # Тест запроса роли со сроком на n дней

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.request_membership', Mock(return_value={'id': 1})) as request_membership:
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.other_service.id,
                'department': data.department.id,
                'role': data.team_member.role.id,
                'deprive_after': 10
            }
        )

        assert response.status_code == 201

        assert request_membership.call_count == 1
        request_membership.assert_called_with(
            data.other_service,
            data.department,
            data.team_member.role,
            comment='',
            deprive_after=10,
            deprive_at=None,
            requester=data.owner_of_service.staff,
            silent=False,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )
        member = ServiceMemberDepartment.all_states.get(role_id=data.team_member.role.id, service_id=data.other_service.id)
        assert member.expires_at == date.today() + timedelta(days=10)


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-list', 'api-v3:service-department-list', 'api-v4:service-department-list',
])
def test_request_departments_deprive_at(client, data, endpoint_path):
    # Тест запроса роли до определенной даты

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.request_membership', Mock(return_value={'id': 1})) as request_membership:
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.other_service.id,
                'department': data.department.id,
                'role': data.team_member.role.id,
                'deprive_at': '2020-12-31'
            }
        )

        assert response.status_code == 201

        assert request_membership.call_count == 1
        request_membership.assert_called_with(
            data.other_service,
            data.department,
            data.team_member.role,
            comment='',
            deprive_after=None,
            deprive_at=date(2020, 12, 31),
            requester=data.owner_of_service.staff,
            silent=False,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )
        member = ServiceMemberDepartment.all_states.get(role_id=data.team_member.role.id, service_id=data.other_service.id)
        assert member.expires_at == date(2020, 12, 31)


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-list', 'api-v3:service-department-list', 'api-v4:service-department-list',
])
def test_request_departments_dublicate(client, data, endpoint_path):
    # Тест запроса уже существующей роли

    client.login(data.staff.login)
    response = client.json.post(
        reverse(endpoint_path),
        {
            'service': data.department_member.service.id,
            'department': data.department_member.department.id,
            'role': data.department_member.role.id,
        }
    )

    assert response.status_code == 409

    assert response.json()['error']['code'] == 'conflict'
    assert response.json()['error']['detail'] == 'Role already exists'


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-list',
    'api-v3:service-department-list', 'api-v4:service-department-list',
])
def test_request_departments_idm_race_condition_conflict(client, data, endpoint_path):
    # Тест запроса на случай когда IDM медленно отвечает, но роль выдает и ретрай запроса падает с конфликтом

    client.login(data.staff.login)

    def create_obj_and_conflict(*_, **__):
        factories.ServiceMemberDepartmentFactory(
            service=data.service,
            department=data.department,
            role=data.deputy.role,
        )
        raise exceptions.Conflict(detail='Role already exists', extra={})

    with patch('plan.api.idm.actions.request_membership', side_effect=create_obj_and_conflict) as request_membership:
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.service.id,
                'department': data.department.id,
                'role': data.deputy.role.id,
            }
        )

    assert response.status_code == 201
    request_membership.assert_called_once()


@pytest.mark.parametrize('endpoint_path', ('services-api:department-list', 'api-v3:service-department-list'))
def test_request_departments_for_deleted_service(client, data, endpoint_path):
    # Тест запроса роли к удалённому сервису

    client.login(data.staff.login)

    data.other_service.state = 'deleted'
    data.other_service.save()

    with patch('plan.api.idm.actions.deprive_role'):
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.other_service.id,
                'department': data.department.id,
                'role': data.team_member.role.id,
            }
        )

        assert response.status_code == 400
        assert response.json()['error']['code'] == 'invalid'
        assert response.json()['error']['extra']['service'] == ['Invalid pk "%s" - object does not exist.' % data.other_service.id]


@pytest.mark.urls('plan.urls')
@pytest.mark.usefixtures('crowdtest_urls')
@pytest.mark.parametrize('endpoint_path', ('services-api:department-list', 'api-v3:service-department-list'))
def test_request_assessor_department(client, data, endpoint_path):
    """В окружении с CROWDTEST=1 выдает департаменту роль в сервисе без подтверждения"""
    client.login(data.staff.login)
    with patch('plan.api.idm.actions.request_membership') as request_membership:
        with patch('plan.services.tasks.offset_notify_staff'):
            response = client.json.post(
                reverse(endpoint_path),
                {
                    'service': data.other_service.id,
                    'department': data.department.id,
                    'role': data.team_member.role.id
                }
            )

    assert response.status_code == 201
    assert not request_membership.called
    member_department = ServiceMemberDepartment.objects.get(
        service=data.other_service,
        department=data.department,
        role=data.team_member.role,
        state=SERVICEMEMBER_STATE.ACTIVE,
    )
    assert member_department.members.count() == 1


@pytest.mark.urls('plan.urls')
@pytest.mark.usefixtures('crowdtest_urls')
@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-detail',
    'api-v3:service-department-detail',
    'api-v4:service-department-detail',
    'api-frontend:service-department-detail',
])
def test_deprive_assessor_department(client, data, endpoint_path):
    """В окружении с CROWDTEST=1 удаляет роль департамента без подтверждения"""
    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        response = client.json.delete(
            reverse(endpoint_path, args=[data.department_member.id])
        )

    assert response.status_code == 204
    assert not deprive_role.called
    assert not data.department_member.members.exists()
    assert not ServiceMemberDepartment.objects.filter(id=data.department_member.id).exists()


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-detail',
    'api-v3:service-department-detail',
    'api-v4:service-department-detail',
    'api-frontend:service-department-detail',
])
def test_destroy_departments(client, data, endpoint_path):
    # Тест отзыва роли

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        response = client.json.delete(
            reverse(endpoint_path, args=[data.department_member.id, ])
        )

        assert response.status_code == 204
        assert deprive_role.call_count == 1

        deprive_role.assert_called_with(
            member=data.department_member,
            requester=data.owner_of_service.staff,
            timeout=settings.IDM_DELETE_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )
    member_department = ServiceMemberDepartment.all_states.get(pk=data.department_member.id, state=SERVICEMEMBER_STATE.DEPRIVING)
    assert member_department.members(manager='all_states').filter(state=SERVICEMEMBER_STATE.DEPRIVING).count() == 1


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-approve', 'api-v3:service-department-approve', 'api-v4:service-department-approve',
])
def test_approve_departments_api_v3(client, data, endpoint_path):
    # Тест апрува

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.approve_role') as approve_role:
        response = client.json.post(
            reverse(endpoint_path),
            {'idm_role': 3}
        )

        assert response.status_code == 204

        assert approve_role.call_count == 1
        approve_role.assert_called_with(
            role_id=3,
            requester=data.owner_of_service.staff,
            timeout=10.0,
            retry=0,
        )


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-decline', 'api-v3:service-department-decline', 'api-v4:service-department-decline',
])
def test_decline_departments(client, data, endpoint_path):
    # Тест отклонения

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.decline_role') as decline_role:
        response = client.json.post(
            reverse(endpoint_path),
            {'idm_role': 4}
        )

        assert response.status_code == 204

        assert decline_role.call_count == 1
        decline_role.assert_called_with(
            role_id=4,
            requester=data.owner_of_service.staff,
            timeout=10.0,
            retry=0,
        )


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-rerequest', 'api-v3:service-department-rerequest', 'api-v4:service-department-rerequest',
])
def test_rerequest_departments(client, data, endpoint_path):
    # Тест перезапроса роли

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.rerequest_role') as rerequest_role:
        response = client.json.post(
            reverse(endpoint_path, args=[data.department_member.id]),
        )

        assert response.status_code == 204

        assert rerequest_role.call_count == 1
        rerequest_role.assert_called_with(
            service_member=data.department_member,
            requester=data.owner_of_service.staff,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-list', 'api-v3:service-department-list', 'api-v4:service-department-list',
])
def test_request_departments_service_in_readonly(client, data, endpoint_path):
    # Тест запроса роли, когда сервис находится в состоянии read-only

    client.login(data.staff.login)
    data.other_service.readonly_state = 'creating'
    data.other_service.save()

    with patch('plan.api.idm.actions.request_membership'):
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.other_service.id,
                'department': data.department.id,
                'role': data.team_member.role.id
            }
        )

        assert response.status_code == 409

        assert response.json()['error']['code'] == 'service_readonly'
        assert response.json()['error']['detail'] == 'service is read-only'


@pytest.mark.parametrize('endpoint_path', [
    'services-api:department-list', 'api-v3:service-department-list', 'api-v4:service-department-list',
])
def test_request_departments_null(client, data, endpoint_path):
    # Тест запроса роли с некорректными данными

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.other_service.id,
                'department': data.department.id,
                'role': data.team_member.role.id,
                'deprive_after': 0,
            }
        )

        assert response.status_code == 400
        assert request_membership.call_count == 0

    assert response.json()['error']['code'] == 'invalid'
    assert response.json()['error']['extra']['deprive_after'] == ['Ensure this value is greater than or equal to 1.']


@pytest.mark.parametrize('endpoint_path', (
    'services-api:department-list', 'api-v3:service-department-list', 'api-v4:service-department-list'
))
def test_request_departments_staff_id(client, data, endpoint_path):
    """
    Проверяем возможность запроса роли на департамент по staff_id департамента
    Также проверяем корректность валидации этого поля
    """

    data.department.staff_id = data.department.id + 1000
    data.department.save()

    client.login(data.staff.login)
    path = reverse(endpoint_path)

    with patch('plan.api.idm.actions.request_membership', Mock(return_value={'id': 1})) as request_membership:
        response = client.json.post(
            path,
            {
                'service': data.other_service.id,
                'staff_department': data.department.staff_id,
                'role': data.team_member.role.id
            }
        )
        assert response.status_code == 201

        assert request_membership.call_count == 1
        request_membership.assert_called_with(
            data.other_service,
            data.department,
            data.team_member.role,
            comment='',
            deprive_after=None,
            deprive_at=None,
            requester=data.owner_of_service.staff,
            silent=False,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )

        invalid_staff_departments = [0, 'tudumpurum', 100500]

        for staff_department in invalid_staff_departments:
            response = client.json.post(
                path,
                {
                    'service': data.other_service.id,
                    'staff_department': staff_department,
                    'role': data.team_member.role.id
                }
            )
            assert response.status_code == 404
