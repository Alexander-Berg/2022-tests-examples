import pretend
import pytest
from django.conf import settings
from django.urls import reverse

from plan.denormalization.check import check_obj_with_denormalized_fields
from plan.roles.models import Role
from plan.services.models import Service
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, staff_factory):
    user1 = factories.UserFactory()
    staff1 = staff_factory('full_access', user=user1, first_name='Фродо', last_name='Бэггинс')
    user2 = factories.UserFactory()
    staff2 = staff_factory('full_access', user=user2, first_name='Сэм', last_name='Гэмджи')
    user3 = factories.UserFactory()
    staff3 = staff_factory('full_access', user=user3, first_name='User', last_name='3', affiliation='external')
    user4 = factories.UserFactory()
    staff4 = staff_factory('full_access', user=user4, first_name='User', last_name='4', is_robot=True)

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


def test_team(client, data, django_assert_num_queries):
    client.login(data.staff1.login)
    url = reverse('api-frontend:service-team-detail', args=[data.service.pk])

    factories.ServiceMemberFactory(
        staff=data.staff2,
        role=Role.get_responsible(),
        service=data.service
    )

    with django_assert_num_queries(6):
        # 1 - Staff auth.User
        # 1 - Middleware
        # 1 - get Service or 404
        # 1 - ServiceMember qs
        # 1 - pg_is_in_recovery()
        # 1 - Waffle is_readonly
        response = client.json.get(url, {'fields': 'team'}, HTTP_ACCEPT_LANGUAGE='ru')

    assert response.status_code == 200

    team = response.json()
    team = sorted(team, key=lambda member: member['person']['login'])

    assert {member['id'] for member in team} == {member.id for member in data.service.members.team()}

    assert team[0]['person']['login'] == data.staff1.login
    assert team[0]['person']['firstName'] == 'Фродо'
    assert team[0]['person']['lastName'] == 'Бэггинс'

    assert team[0]['role']['id'] == data.role1.pk
    assert team[0]['role']['scope']['scope_id'] == data.role1.scope.pk
    assert team[0]['role']['scope']['id'] == data.role1.scope.slug
    assert team[0]['role']['is_exportable'] is True
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


def test_team_for_responsible(client, data):
    url = reverse('api-frontend:service-team-detail', args=[data.service.pk])

    factories.ServiceMemberFactory(
        staff=data.staff3,
        role=Role.get_responsible(),
        service=data.service,
    )
    factories.ServiceMemberFactory(
        staff=data.staff4,
        role=data.role1,
        service=data.service,
    )
    client.login(data.staff4.login)

    response = client.json.get(url, {'fields': 'team'})
    assert response.status_code == 200
    assert response.json()[0]['person']['is_frozen'] is False


def test_limit_scope_members_in_team_status_api(client):
    """
    Проверяем, что с параметром max_scope_members выдается
    не более N людей в каждом скоупе
    """
    service = factories.ServiceFactory()
    resp_role = Role.get_responsible()
    role = factories.RoleFactory(scope=resp_role.scope)
    role_1 = factories.RoleFactory()

    for _ in range(4):
        factories.ServiceMemberFactory(
            role=resp_role,
            service=service,
        )

    expected_role = []
    expected_role_1 = set()

    for _ in range(3):
        member = factories.ServiceMemberFactory(
            role=role,
            service=service,
        )
        expected_role.append(member.staff.id)

    for _ in range(10):
        member = factories.ServiceMemberFactory(
            role=role_1,
            service=service,
        )
        expected_role_1.add(member.staff.id)

    response = client.json.get(
        reverse('api-frontend:service-team-detail', args=[service.id]),
        {'max_scope_members': 3},
    )
    assert response.status_code == 200
    result = response.json()

    result_with_role = [
        item['person']['id']
        for item in result
        if item['role']['scope']['id'] == role.scope.slug
    ]
    assert len(result_with_role) == 3
    assert set(result_with_role) == set(expected_role)

    result_with_role_1 = [
        item['person']['id']
        for item in result
        if item['role']['scope']['id'] == role_1.scope.slug
    ]
    assert len(result_with_role_1) == 3
    assert set(result_with_role_1).issubset(expected_role_1)


def test_filter_scope_team_status_api(client, data):
    scope = factories.RoleScopeFactory()
    role = factories.RoleFactory(scope=scope)

    for _ in range(4):
        factories.ServiceMemberFactory(
            role=role,
            service=data.service,
        )

    response = client.json.get(
        reverse('api-frontend:service-team-detail', args=[data.service.id]),
        {'scope__slug': scope.slug},
    )
    assert response.status_code == 200
    result = response.json()
    assert set(item['role']['scope']['id'] for item in result) == {scope.slug}
