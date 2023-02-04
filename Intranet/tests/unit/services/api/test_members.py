from collections import Counter
from datetime import date, timedelta
from functools import partial

import pytest
from django.core.urlresolvers import reverse
from django.utils import timezone
from django.conf import settings
from mock import patch, Mock

from plan.roles.models import Role
from plan.idm import exceptions
from plan.services.models import Service, ServiceMember, ServiceTag
from plan.services.state import SERVICEMEMBER_STATE
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def get_members(client):
    return partial(client.json.get, reverse('services-api:member-list'))


def test_get_members(get_members, data):
    response = get_members()

    assert response.status_code == 200

    json = response.json()['results']
    assert len(json) == ServiceMember.objects.team().count()

    assert {member['id'] for member in json} == set(ServiceMember.objects.team().values_list('id', flat=True))


@pytest.mark.parametrize(
    'endpoint_path', ['services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list']
)
@pytest.mark.parametrize(
    ('role', 'restricted'), [('own_only_viewer', True), ('services_viewer', False), ('full_access', False)]
)
def test_get_members_restricted(data, client, staff_factory, endpoint_path, role, restricted):
    viewer = staff_factory(role)
    factories.ServiceMemberFactory(service=data.service, staff=viewer)
    client.login(viewer.login)

    response = client.json.get(reverse(endpoint_path), {})
    assert response.status_code == 200

    real_ids = Counter([m['id'] for m in response.json()['results']])
    assert len(real_ids) > 0

    if restricted:
        services_ids = ServiceMember.objects.filter(staff=viewer).values_list('service_id', flat=True)
        expected_ids = Counter([m.id for m in ServiceMember.objects.team().filter(service__in=services_ids)])
    else:
        expected_ids = Counter([m.id for m in ServiceMember.objects.team()])

    assert real_ids == expected_ids


def test_get_members_with_department_member(get_members, django_assert_num_queries):
    role = factories.RoleFactory()
    department_role = factories.RoleFactory()
    department = factories.DepartmentFactory()
    service = factories.ServiceFactory()
    users = [factories.StaffFactory(department=department) for _ in range(100)]
    dep_member = factories.ServiceMemberDepartmentFactory(service=service, department=department, role=department_role)
    for user in users:
        factories.ServiceMemberFactory(service=service, staff=user, role=role, from_department=dep_member)

    with django_assert_num_queries(6):
        response = get_members()

    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == 20  # Размер страницы по умолчанию - 20


def test_get_members_with_service_ids_filtering(client, django_assert_num_queries):
    role = factories.RoleFactory()
    services = [factories.ServiceFactory() for _ in range(100)]
    user = factories.StaffFactory()

    for service in services:
        factories.ServiceMemberFactory(service=service, staff=user, role=role)

    url = reverse('api-v3:service-member-list')
    service_ids = {service.id for service in services[:50]}
    with django_assert_num_queries(6):
        response = client.json.get(
            url,
            data={'page_size': 1000, 'service__in': ','.join(map(str, service_ids)) + ',invalid_slug'},
        )
    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == 50
    assert {item['service']['id'] for item in data} == service_ids


def test_get_members_is_robot_and_affiliation(get_members, data):
    response = get_members({'person': data.staff.login})
    staff = response.json()['results'][0]['staff']
    assert staff['is_robot'] == data.staff.is_robot
    assert staff['affiliation'] == data.staff.affiliation


def test_filter_by_service(get_members, data):
    response = get_members({'service': data.service.id})
    assert len(response.json()['results']) == data.service.members.team().count()

    response = get_members({'service': [data.service.id, data.other_service.id]})
    assert len(response.json()['results']) == (
        data.service.members.team().count() + data.other_service.members.team().count()
    )

    response = get_members({'service__in': ','.join(str(id_) for id_ in (data.service.id, data.other_service.id))})
    assert len(response.json()['results']) == (
        data.service.members.team().count() + data.other_service.members.team().count()
    )


def test_filter_by_service_slug(get_members, data):
    response = get_members({'service__slug': data.service.slug})
    assert response.json()['count'] == data.service.members.team().count()

    response = get_members({'service__slug__in': ','.join((data.service.slug, data.other_service.slug))})
    assert response.json()['count'] == (data.service.members.team().count() + data.other_service.members.team().count())


@pytest.mark.skip
def test_filter_by_service_slug_is_case_insensitive(get_members, data):
    response = get_members({'service__slug': data.service.slug.upper()})
    assert response.json()['count'] == data.service.members.team().count()


def test_filter_by_service_state(get_members, data):
    response = get_members({'service__state': Service.states.IN_DEVELOP})
    assert response.json()['count'] == (
        ServiceMember.objects.team().filter(service__state=Service.states.IN_DEVELOP).count()
    )

    response = get_members({'service__state': Service.states.CLOSED})
    assert response.json()['count'] == 0

    response = get_members({'service__state__in': ','.join([Service.states.IN_DEVELOP, Service.states.CLOSED])})
    assert response.json()['count'] == (
        ServiceMember.objects.team()
        .filter(service__state__in=[Service.states.IN_DEVELOP, Service.states.CLOSED])
        .count()
    )


def test_filter_by_service__with_descendants(get_members, data):
    response = get_members({'service__with_descendants': data.metaservice.id})
    assert response.json()['count'] == (
        ServiceMember.objects.filter(service__in=data.metaservice.get_descendants(include_self=True)).team().count()
    )

    response = get_members({'service__with_descendants': data.service.id})
    assert response.json()['count'] == (
        ServiceMember.objects.filter(service__in=data.service.get_descendants(include_self=True)).team().count()
    )


def test_filter_by_service__with_descendants_slug(get_members, data):
    response = get_members({'service__with_descendants__slug': data.metaservice.slug})
    assert response.json()['count'] == (
        ServiceMember.objects.filter(service__in=data.metaservice.get_descendants(include_self=True)).team().count()
    )

    response = get_members({'service__with_descendants__slug': data.service.slug})
    assert response.json()['count'] == (
        ServiceMember.objects.filter(service__in=data.service.get_descendants(include_self=True)).team().count()
    )


def test_filter_by_person(get_members, data):
    response = get_members({'person': data.staff.login})
    assert response.json()['count'] == ServiceMember.objects.filter(staff=data.staff).team().count()

    response = get_members({'person': data.stranger.login})
    assert response.json()['count'] == 0


def test_filter_by_person_login(get_members, data):
    factories.ServiceMemberFactory()

    response = get_members({'person': data.staff.login})
    assert response.json()['count'] == (ServiceMember.objects.team().filter(staff=data.staff).count())

    response = get_members({'person': data.stranger.login})
    assert response.json()['count'] == 0

    response = get_members({'person__in': ','.join((data.staff.login, data.other_staff.login))})
    assert response.json()['count'] == (
        ServiceMember.objects.team().filter(staff__in=[data.staff, data.other_staff]).count()
    )

    response = get_members({'person__login__in': ','.join((data.staff.login, data.other_staff.login))})
    assert response.json()['count'] == (
        ServiceMember.objects.team().filter(staff__in=[data.staff, data.other_staff]).count()
    )


def test_filter_by_person_uid(get_members, data):
    factories.ServiceMemberFactory()

    response = get_members({'person__uid': data.staff.uid})
    assert len(response.json()['results']) == (ServiceMember.objects.team().filter(staff=data.staff).count())

    response = get_members({'person__uid': data.stranger.uid})
    assert len(response.json()['results']) == 0

    response = get_members({'person__uid__in': ','.join((data.staff.uid, data.other_staff.uid))})
    assert len(response.json()['results']) == (
        ServiceMember.objects.team().filter(staff__in=[data.staff, data.other_staff]).count()
    )


def test_filter_by_role(get_members, data):
    response = get_members({'role': data.team_member.role.id})
    assert len(response.json()['results']) == (ServiceMember.objects.team().filter(role=data.team_member.role).count())

    role = factories.RoleFactory()
    response = get_members({'role': role.id})
    assert len(response.json()['results']) == 0


def test_filter_by_role_code(get_members, data):

    response = get_members({'role__code': data.owner_of_service.role.code})
    assert response.json()['count'] == (ServiceMember.objects.filter(role=data.owner_of_service.role).count())

    response = get_members({'role__code': data.team_member.role.code})
    assert response.json()['count'] == (ServiceMember.objects.filter(role=data.team_member.role).count())


@pytest.mark.parametrize('existing_roles', [True, False])
def test_filter_by_role_in(get_members, data, existing_roles):
    role = factories.RoleFactory()

    if existing_roles:
        request_roles = ','.join(str(role_id) for role_id in (data.team_member.role.id, role.id))
        check_roles = [data.team_member.role, role] if existing_roles else []
    else:
        request_roles = ['11111', '22222']
        check_roles = []

    request = partial(
        get_members, {'role__in': request_roles}
    )

    result = request().json()
    assert result['count'] == (ServiceMember.objects.filter(role__in=check_roles).count())

    assert role.id not in {member_data['role']['id'] for member_data in result['results']}

    factories.ServiceMemberFactory(role=role)

    result = request().json()
    assert result['count'] == ServiceMember.objects.filter(role__in=check_roles).count()

    if existing_roles:
        assert role.id in {member_data['role']['id'] for member_data in result['results']}


def test_filter_by_role_code_in(get_members, data):
    role = factories.RoleFactory()

    request = partial(get_members, {'role__code__in': ','.join((data.team_member.role.code, role.code))})

    result = request().json()
    assert result['count'] == (ServiceMember.objects.filter(role__in=[data.team_member.role, role]).count())
    assert role.id not in {member_data['role']['id'] for member_data in result['results']}

    factories.ServiceMemberFactory(role=role)

    result = request().json()
    assert result['count'] == (ServiceMember.objects.filter(role__in=[data.team_member.role, role]).count())
    assert role.id in {member_data['role']['id'] for member_data in result['results']}


def test_filter_by_role_scope(get_members, service_with_owner):
    admin_scope = factories.RoleScopeFactory()
    dev_scope = factories.RoleScopeFactory()
    admin_role = factories.RoleFactory(scope=admin_scope)
    dev_role = factories.RoleFactory(scope=dev_scope)
    admin = factories.ServiceMemberFactory(
        service=service_with_owner,
        role=admin_role,
        staff=factories.StaffFactory(),
    )
    dev = factories.ServiceMemberFactory(
        service=service_with_owner,
        role=dev_role,
        staff=factories.StaffFactory(),
    )

    response = get_members({'role__scope__slug': admin_scope.slug})
    json = response.json()['results']
    assert len(json) == 1
    assert json[0]['id'] == admin.id

    response = get_members({'role__scope__id': admin_scope.id})
    json = response.json()['results']
    assert len(json) == 1
    assert json[0]['id'] == admin.id

    response = get_members(
        {'role__scope__id__in': ','.join(str(role_id) for role_id in (admin_scope.id, dev_scope.id, 100000278028))}
    )

    json = response.json()['results']
    assert len(json) == 2
    assert {item['id'] for item in json} == {admin.id, dev.id}

    response = get_members({'role__scope__slug__in': ','.join([admin_scope.slug, dev_scope.slug, 'cats'])})
    json = response.json()['results']
    assert len(json) == 2
    assert {item['id'] for item in json} == {admin.id, dev.id}


def test_filter_by_exportable(get_members, data):
    result = get_members().json()
    assert result['count'] == (ServiceMember.objects.team().filter(service__is_exportable=True).count())
    assert data.meta_other.slug not in {member_data['service']['slug'] for member_data in result['results']}

    result = get_members({'service__is_exportable': 'true'}).json()
    assert result['count'] == (ServiceMember.objects.team().filter(service__is_exportable=True).count())
    assert data.meta_other.slug not in {member_data['service']['slug'] for member_data in result['results']}

    result = get_members({'service__is_exportable': 'false'}).json()
    assert result['count'] == (ServiceMember.objects.team().filter(service__is_exportable=False).count())
    assert {member_data['id'] for member_data in result['results']} == set(
        ServiceMember.objects.team().filter(service__is_exportable=False).values_list('id', flat=True)
    )

    result = get_members({'service__is_exportable__in': 'true,false'}).json()
    assert result['count'] == ServiceMember.objects.team().count()


@pytest.mark.parametrize('api', ['api-v4', 'api-frontend'])
def test_filter_by_exportable_exclude(client, data, api):
    data.service.is_exportable = False
    data.service.save()

    response = client.json.get(
        reverse(f'{api}:service-member-list'),
        {
            'service__slug': data.service.slug,
        }
    )

    result = response.json()
    assert len(result['results']) == (4 if api == 'api-frontend' else 0)


def test_filter_by_id(get_members, data):
    members_ids = list(ServiceMember.objects.team().values_list('id', flat=True).order_by('id'))
    mid_id = members_ids[len(members_ids) // 2]
    response1 = get_members({'id__lt': mid_id})

    assert response1.json()['count'] == (ServiceMember.objects.team().filter(id__lt=mid_id).count())

    response = get_members({'id__gt': mid_id})

    assert response.json()['count'] == (ServiceMember.objects.team().filter(id__gt=mid_id).count())

    response1 = get_members({'id__in': ','.join(map(str, members_ids[: len(members_ids) // 2]))})

    assert response1.json()['count'] == (
        ServiceMember.objects.team().filter(id__in=members_ids[: len(members_ids) // 2]).count()
    )

    response1 = get_members({'id': mid_id})

    assert response1.json()['count'] == (ServiceMember.objects.team().filter(id__exact=mid_id).count())


def test_filter_by_use_inheritance_settings_true(data, get_members, owner_role):
    """
    Фильтр ролей с учётом прав наследования у вложенных сервисов
    """

    all_members = (
        ServiceMember.objects.filter(service__in=data.metaservice.get_descendants(include_self=True)).team().count()
    )

    service_members = (
        ServiceMember.objects.filter(service__in=data.service.get_descendants(include_self=True)).team().count()
    )

    data.service.membership_inheritance = False
    data.service.save()

    response = get_members({'service__with_descendants': data.metaservice.id, 'use_inheritance_settings': 'True'})

    assert response.json()['count'] == all_members - service_members

    # добавим ещё несколько вложенных сервисов

    new_service = factories.ServiceFactory(
        parent=data.metaservice,
        owner=data.other_staff,
        membership_inheritance=True,
    )
    factories.ServiceMemberFactory(service=new_service, role=owner_role, staff=data.other_staff)

    child_new_service = factories.ServiceFactory(
        parent=new_service, owner=data.other_staff, membership_inheritance=False
    )
    factories.ServiceMemberFactory(service=child_new_service, role=owner_role, staff=data.other_staff)

    all_members = (
        ServiceMember.objects.filter(service__in=data.metaservice.get_descendants(include_self=True)).team().count()
    )

    child_new_service_members = (
        ServiceMember.objects.filter(service__in=child_new_service.get_descendants(include_self=True)).team().count()
    )

    response = get_members({'service__with_descendants': data.metaservice.id, 'use_inheritance_settings': 'True'})

    assert response.json()['count'] == all_members - service_members - child_new_service_members


def test_filter_by_use_inheritance_settings_true_two_value(data, get_members, owner_role):
    """
    Фильтр ролей с учётом прав наследования у вложенных сервисов.
    Если в value передаём два значения, одно из которых является потомком другого (например, Metaservice и Child),
    но при этом обрезается каким-либо родительским с параметром membership_inheritance=False,
    например:

        Metaservice
        |
        |_ Service (membership_inheritance=False)
        |   |
        |   |_ Child
        |       |
        |       |_ New_service
        |
        |_ Other_service

    то на выходе ожидаем роли для Metaservice и Other_service, а также для Child и New_service
    """
    new_service = factories.ServiceFactory(
        parent=data.child,
        owner=data.other_staff,
        membership_inheritance=True,
    )
    factories.ServiceMemberFactory(service=new_service, role=owner_role, staff=data.other_staff)

    all_members = (
        ServiceMember.objects.filter(service__in=data.metaservice.get_descendants(include_self=True)).team().count()
    )

    service_members = ServiceMember.objects.filter(service=data.service).team().count()

    data.service.membership_inheritance = False
    data.service.save()

    response = get_members(
        {'service__with_descendants': [data.metaservice.id, data.child.id], 'use_inheritance_settings': 'True'}
    )

    assert response.json()['count'] == all_members - service_members


def test_filter_by_use_inheritance_settings_false(data, get_members):
    """
    Фильтр ролей без учёта прав наследования у вложенных сервисов
    """

    all_members = get_members(
        {
            'service__with_descendants': data.metaservice.id,
        }
    ).json()['count']

    data.service.membership_inheritance = False
    data.service.save()

    response = get_members({'service__with_descendants': data.metaservice.id, 'use_inheritance_settings': 'False'})

    assert response.json()['count'] == all_members


@pytest.mark.parametrize(
    ('endpoint_path', 'queries_count'),
    [('services-api:member-list', 5), ('api-v3:service-member-list', 5), ('api-v4:service-member-list', 4)],
)
@pytest.mark.parametrize(('tags_count', 'exclude_tags_count'), [(1, 1), (2, 1), (2, 2), (3, 3)])
def test_filter_by_tags_exclude(
    client, endpoint_path, queries_count, tags_count, exclude_tags_count, django_assert_num_queries
):
    """
    Фильтр ролей с исключением сервисов по тегам.
    Всего 3 сервиса, в каждом есть по одному member.
    Каждый сервис либо имеет свой тег, либо не имеет тегов совсем.
    """

    all_services_count = 3
    for _ in range(all_services_count):
        service = factories.ServiceFactory(is_exportable=True)
        factories.ServiceMemberFactory(service=service)

    services = list(Service.objects.all())
    for i in range(tags_count):
        tag = factories.ServiceTagFactory()
        service = services[i]
        service.tags.add(tag)

    data = dict()
    tags = list(ServiceTag.objects.all())
    if exclude_tags_count == 1:
        data['service__with__tags__exclude'] = tags[0].slug
    else:
        value = []
        for i in range(exclude_tags_count):
            value.append(tags[i].slug)

        data['service__with__tags__exclude__in'] = ','.join(value)

    # Для старых ручек (services-api и api-v3):
    # если в ответе ничего не должно быть, то не делаем запрос в services_servicemember:
    member_count = all_services_count - exclude_tags_count
    if not member_count and (
        endpoint_path == 'services-api:member-list' or endpoint_path == 'api-v3:service-member-list'
    ):
        queries_count -= 1

    waffle_query_count = 1
    with django_assert_num_queries(queries_count + waffle_query_count):
        # Количество запросов не имеет зависимостей:
        # 2 selects middleware
        # 1 select в services_servicemember + для старых ручек 1 SELECT COUNT
        # 1 pg_is_in_recovery
        # 1 запрос for waffle readonly switch
        response = client.json.get(reverse(endpoint_path), data)
        results = response.json()['results']

    assert len(results) == member_count

    if member_count:
        results_services = {member['service']['id'] for member in results}
        # теги проставляем последовательно, поэтому ожидаем сервисы с того порядка в списке,
        # которым заканчивается количество исключаемых тегов
        expected_services = {service.id for service in services[exclude_tags_count:]}
        assert results_services == expected_services


@pytest.mark.parametrize(
    'endpoint_path',
    (
        'services-api:member-list',
        'api-v3:service-member-list',
        'api-v4:service-member-list',
        'api-frontend:service-member-list',
    )
)
@pytest.mark.parametrize('is_frozen', (True, False))
def test_is_frozen(client, endpoint_path, is_frozen):
    member = factories.ServiceMemberFactory()
    if is_frozen:
        member.staff.is_frozen = True
        member.staff.save()

    response = client.json.get(reverse(endpoint_path))
    assert response.status_code == 200
    results = response.json()['results']
    expected_count = 1
    if is_frozen and 'frontend' not in endpoint_path:
        expected_count = 0
    assert len(results) == expected_count


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list')
)
def test_request_member_incorrect(client, data, endpoint_path):
    # Тест запроса роли с недостоющими полями

    client.login(data.staff.login)

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        response = client.json.post(
            reverse(endpoint_path), {'service': data.metaservice.id, 'person': data.staff.login}
        )

        assert response.status_code == 400
        assert request_membership.call_count == 0

    assert response.json()['error']['code'] == 'validation_error'
    assert response.json()['error']['extra']['role'] == ['This field is required.']


@pytest.mark.parametrize(
    'endpoint_path', ['services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list']
)
def test_request_member(client, owner_role, data, endpoint_path):
    # Тест корректного запроса без доп полей

    client.login(data.staff.login)
    with patch('plan.api.idm.actions.request_membership') as request_membership:
        request_membership.return_value = {'id': 1}
        response = client.json.post(
            reverse(endpoint_path),
            {'service': data.other_service.id, 'person': data.staff.login, 'role': data.owner_of_other_service.role.id},
        )

        assert response.status_code == 201

        assert request_membership.call_count == 1
        request_membership.assert_called_with(
            data.other_service,
            data.staff,
            data.owner_of_other_service.role,
            deprive_after=None,
            deprive_at=None,
            comment='',
            requester=data.staff,
            silent=False,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )
    member = ServiceMember.all_states.get(service=data.other_service, staff=data.staff, role=owner_role)
    assert member.state == SERVICEMEMBER_STATE.REQUESTED
    assert member.idm_role_id == 1


@pytest.mark.parametrize('requested_role_code', [Role.DUTY, Role.RESPONSIBLE_FOR_DUTY])
@pytest.mark.parametrize(
    'endpoint_path', ['services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list']
)
def test_request_duty_roles(client, data, endpoint_path, patch_tvm, requested_role_code):
    """
    Роли DUTY и RESPONSIBLE_FOR_DUTY разрешено запрашивать.
    """
    role = factories.RoleFactory(code=requested_role_code)

    client.login(data.staff.login)
    with patch('plan.idm.manager.Manager._run_request') as run_request:
        response = Mock()
        response.ok = True
        response.text = 'x'
        response.json.return_value = {'id': 1}
        run_request.return_value = response
        response = client.json.post(
            reverse(endpoint_path), {'service': data.other_service.id, 'person': data.staff.login, 'role': role.id}
        )

    assert response.status_code == 201


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list')
)
def test_request_member_deprive(client, data, endpoint_path):
    # Тест запроса роли со взаимоисключающими полями

    role = factories.RoleFactory()
    client.login(data.staff.login)

    with patch('plan.api.idm.actions.request_membership') as request_membership:
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.metaservice.id,
                'person': data.staff.login,
                'role': role.id,
                'deprive_after': 10,
                'deprive_at': date(2020, 12, 31).isoformat(),
            },
        )

        assert response.status_code == 400
        assert request_membership.call_count == 0

    assert response.json()['error']['code'] == 'validation_error'
    assert response.json()['error']['detail'] == "Got mutually exclusive fields: ('deprive_at', 'deprive_after')"


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list')
)
def test_request_member_deprive_after(client, data, endpoint_path):
    # Тест запроса роли со сроком на n дней

    client.login(data.staff.login)

    with patch('plan.api.idm.actions.request_membership', Mock(return_value={'id': 1})) as request_membership:
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.other_service.id,
                'person': data.staff.login,
                'role': data.owner_of_other_service.role.id,
                'deprive_after': 10,
            },
        )

        assert response.status_code == 201
        assert request_membership.call_count == 1

        request_membership.assert_called_with(
            data.other_service,
            data.staff,
            data.owner_of_other_service.role,
            deprive_after=10,
            deprive_at=None,
            comment='',
            requester=data.staff,
            silent=False,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )
        member = ServiceMember.all_states.get(
            role_id=data.owner_of_other_service.role.id,
            service_id=data.other_service.id,
            staff_id=data.staff.id,
        )
        assert member.expires_at == date.today() + timedelta(days=10)


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list')
)
def test_request_member_deprive_at(client, data, endpoint_path):
    # Тест запроса роли до определенной даты

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.request_membership', Mock(return_value={'id': 1})) as request_membership:
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.other_service.id,
                'person': data.staff.login,
                'role': data.owner_of_other_service.role.id,
                'deprive_at': '2020-12-31',
            },
        )

        assert response.status_code == 201
        assert request_membership.call_count == 1

        request_membership.assert_called_with(
            data.other_service,
            data.staff,
            data.owner_of_other_service.role,
            deprive_after=None,
            deprive_at=date(2020, 12, 31),
            comment='',
            requester=data.staff,
            silent=False,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )
        member = ServiceMember.all_states.get(
            role_id=data.owner_of_other_service.role.id,
            service_id=data.other_service.id,
            staff_id=data.staff.id,
        )
        assert member.expires_at == date(2020, 12, 31)


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list')
)
def test_request_member_dublicate(client, data, endpoint_path):
    # Тест запроса уже существующей роли

    client.login(data.staff.login)

    response = client.json.post(
        reverse(endpoint_path),
        {
            'service': data.other_service.id,
            'person': data.owner_of_other_service.staff.login,
            'role': data.owner_of_other_service.role.id,
        },
    )

    assert response.status_code == 409

    assert response.json()['error']['code'] == 'conflict'
    assert response.json()['error']['detail'] == 'Role already exists'


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list',)
)
def test_request_member_idm_race_condition_conflict(client, data, endpoint_path):
    # Тест запроса уже существующей роли
    client.login(data.staff.login)

    def create_obj_and_conflict(*_, **__):
        factories.ServiceMemberFactory(
            service=data.service,
            staff=data.other_staff,
            role=data.deputy.role,
        )
        raise exceptions.Conflict(detail='Role already exists', extra={})

    with patch('plan.api.idm.actions.request_membership', side_effect=create_obj_and_conflict) as request_membership:
        response = client.json.post(
            reverse(endpoint_path),
            {
                'service': data.service.id,
                'person': data.other_staff.login,
                'role': data.deputy.role.id,
            },
        )

    assert response.status_code == 201
    request_membership.assert_called_once()


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list')
)
def test_request_member_for_deleted_service(client, data, endpoint_path):
    # Тест запроса роли к удалённому сервису

    client.login(data.staff.login)

    data.other_service.state = Service.states.DELETED
    data.other_service.save()

    response = client.json.post(
        reverse(endpoint_path),
        {
            'service': data.other_service.id,
            'person': data.owner_of_other_service.staff.login,
            'role': data.owner_of_other_service.role.id,
        },
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'validation_error'
    assert response.json()['error']['extra'] == {
        'service': ['Invalid pk "{}" - object does not exist.'.format(data.other_service.id)]
    }


@pytest.mark.urls('plan.urls')
@pytest.mark.usefixtures('crowdtest_urls')
@pytest.mark.parametrize(
    'endpoint_path', ['services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list']
)
def test_request_assessor_member(client, owner_role, data, endpoint_path):
    """В окружении с CROWDTEST=1 выдает пользователю роль в сервисе без подтверждения"""
    client.login(data.staff.login)
    with patch('plan.api.idm.actions.request_membership') as request_membership:
        response = client.json.post(
            reverse(endpoint_path),
            {'service': data.other_service.id, 'person': data.staff.login, 'role': data.owner_of_other_service.role.id},
        )

    assert response.status_code == 201
    assert not request_membership.called
    assert ServiceMember.all_states.get(
        service=data.other_service, staff=data.staff, role=owner_role,
    ).state == SERVICEMEMBER_STATE.ACTIVE


@pytest.mark.urls('plan.urls')
@pytest.mark.usefixtures('crowdtest_urls')
@pytest.mark.parametrize(
    'endpoint_path', ['services-api:member-detail', 'api-v3:service-member-detail', 'api-v4:service-member-detail']
)
def test_destroy_assessor_member(client, data, endpoint_path):
    """В окружении с CROWDTEST=1 удаляет роль пользователя без подтверждения"""
    client.login(data.team_member.staff.login)
    with patch('plan.api.idm.actions.deprive_role') as deprive_role:
        response = client.json.delete(reverse(endpoint_path, args=[data.team_member.id]))

    assert response.status_code == 204
    assert not deprive_role.called
    assert ServiceMember.all_states.get(id=data.team_member.id).state == SERVICEMEMBER_STATE.DEPRIVED


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-approve', 'api-v3:service-member-approve', 'api-v4:service-member-approve')
)
def test_approve_member(client, data, endpoint_path):
    # Тест апрува

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.approve_role') as approve_role:
        response = client.json.post(reverse(endpoint_path), {'idm_role': 1})

        assert response.status_code == 204

        assert approve_role.call_count == 1
        approve_role.assert_called_with(
            role_id=1,
            requester=data.staff,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-decline', 'api-v3:service-member-decline', 'api-v4:service-member-decline')
)
def test_decline_member(client, data, endpoint_path):
    # Тест отклонения

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.decline_role') as decline_role:
        response = client.json.post(reverse(endpoint_path), {'idm_role': 1})

        assert response.status_code == 204

        assert decline_role.call_count == 1
        decline_role.assert_called_with(
            role_id=1,
            requester=data.owner_of_service.staff,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-detail', 'api-v3:service-member-detail', 'api-v4:service-member-detail'),
)
@pytest.mark.parametrize(
    'depriving', (True, False),
)
def test_member_view_destroy(client, data, endpoint_path, depriving):
    # Тест отзыва роли

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.deprive_role') as deprive_role_mock:
        if depriving:
            data.team_member.set_depriving_state()
        response = client.json.delete(reverse(endpoint_path, args=[data.team_member.id]))

        if depriving:
            assert response.status_code == 400
            deprive_role_mock.assert_not_called()
        else:
            assert response.status_code == 204

            deprive_role_mock.assert_called_once_with(
                member=data.team_member,
                requester=data.staff,
                timeout=settings.IDM_DELETE_ROLES_TIMEOUT,
                retry=settings.ABC_IDM_FROM_API_RETRY,
            )
            assert ServiceMember.objects.get(pk=data.team_member.pk).state == SERVICEMEMBER_STATE.DEPRIVING


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-detail', 'api-v3:service-member-detail', 'api-v4:service-member-detail')
)
def test_deprive_owner_membership(client, data, endpoint_path):
    # Тест отзыва роли руководителя

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.deprive_role'):
        response = client.json.delete(
            reverse(endpoint_path, args=[data.owner_of_service.id]),
        )

        assert response.status_code == 403
        assert response.json()['error']['code'] == 'permission_denied'


@pytest.mark.parametrize(
    'endpoint_path',
    ('services-api:member-rerequest', 'api-v3:service-member-rerequest', 'api-v4:service-member-rerequest'),
)
def test_rerequest_member(client, data, endpoint_path):
    # Тест перезапроса

    client.login(data.owner_of_service.staff.login)

    with patch('plan.api.idm.actions.rerequest_role') as rerequest_role:
        response = client.json.post(
            reverse(endpoint_path, args=[data.team_member.id]),
        )

        assert response.status_code == 204

        assert rerequest_role.call_count == 1
        rerequest_role.assert_called_with(
            service_member=data.team_member,
            requester=data.owner_of_service.staff,
            timeout=settings.IDM_POST_ROLES_TIMEOUT,
            retry=settings.ABC_IDM_FROM_API_RETRY,
        )


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list')
)
def test_request_member_service_in_readonly(client, data, endpoint_path):
    # Тест запроса роли, когда сервис находится в состоянии read-only

    client.login(data.staff.login)
    data.other_service.readonly_state = 'creating'
    data.other_service.save()

    with patch('plan.api.idm.actions.request_membership'):
        response = client.json.post(
            reverse(endpoint_path),
            {'service': data.other_service.id, 'person': data.staff.login, 'role': data.owner_of_other_service.role.id},
        )

        assert response.status_code == 409

        assert response.json()['error']['code'] == 'service_readonly'
        assert response.json()['error']['detail'] == 'service is read-only'


def test_querystring_typos(client, data):
    response = client.json.get(reverse('services-api:member-list'), {'service__шв': data.metaservice.id})
    assert response.status_code == 200


def test_inactive_services(get_members, data):
    """
    У метасервиса 3 подсервиса: активный, закрытый и удаленный
    Запрос с параметром with_descendants не должен учитывать закрытый и удаленный
    """
    metaservice = data.metaservice
    descendats = metaservice.get_descendants()
    assert len(descendats) == 3
    closed, deleted, active = descendats
    closed.state = 'closed'
    closed.save()
    deleted.state = 'deleted'
    deleted.save()

    assert (
        metaservice.members.count() + active.members.count()
        == get_members({'service__with_descendants__slug': data.metaservice.slug}).json()['count']
    )


def test_members_of_inactive_service(get_members, data):
    service = data.child
    service.state = Service.states.CLOSED
    service.save()
    assert get_members({'service__with_descendants__slug': service.slug}).json()['count'] == 1


def test_members_invalid_service_slug(get_members):
    """Возвращает пустой результат, если передан слаг несуществующего сервиса."""
    assert get_members({'service__with_descendants__slug': 'noservice'}).json()['count'] == 0


def test_exportable(client):
    service = factories.ServiceFactory()
    exportable_role = factories.RoleFactory()
    non_exportable_role = factories.RoleFactory(is_exportable=False)
    factories.ServiceMemberFactory(service=service, role=exportable_role)
    factories.ServiceMemberFactory(service=service, role=non_exportable_role)
    members = client.json.get(reverse('services-api:member-list')).json()['results']
    assert len(members) == 2
    members = client.json.get(reverse('services-api:member-list'), {'is_exportable': True}).json()['results']
    assert len(members) == 1
    assert members[0]['role']['id'] == exportable_role.id


def test_v4_pagination(client, person):
    service = factories.ServiceFactory()
    expected = {factories.ServiceMemberFactory(service=service) for i in range(100)}
    reversed_url = reverse('api-v4:service-member-list')
    response = client.json.get(reversed_url)
    actual = [service for service in response.json()['results']]
    while response.json()['next'] is not None:
        assert len(response.json()['results']) == 20
        assert reversed_url + '?cursor=' in response.json()['next']
        response = client.json.get(response.json()['next'])
        actual.extend([service for service in response.json()['results']])
    actual = {member for member in ServiceMember.objects.filter(id__in=[member['id'] for member in actual])}
    assert actual == expected


def test_created_modified_filter(client):
    old_created_old_modified = factories.ServiceMemberFactory()
    old_created_new_modified = factories.ServiceMemberFactory()
    new = factories.ServiceMemberFactory()
    (
        ServiceMember.objects.filter(id=old_created_old_modified.id).update(
            created_at=timezone.datetime(2019, 1, 1), modified_at=timezone.datetime(2019, 1, 1)
        )
    )
    (
        ServiceMember.objects.filter(id=old_created_new_modified.id).update(
            created_at=timezone.datetime(2019, 1, 1), modified_at=timezone.datetime(2020, 1, 1)
        )
    )
    (
        ServiceMember.objects.filter(id=new.id).update(
            created_at=timezone.datetime(2020, 1, 1), modified_at=timezone.datetime(2020, 1, 1)
        )
    )
    response = client.json.get(reverse('services-api:member-list'))
    members = response.json()['results']
    assert {member['id'] for member in members} == {old_created_old_modified.id, old_created_new_modified.id, new.id}
    response = client.json.get(reverse('services-api:member-list'), {'created_at__gt': '2019-05-01T00:00:00Z'})
    members = response.json()['results']
    assert {member['id'] for member in members} == {new.id}
    response = client.json.get(reverse('services-api:member-list'), {'modified_at__gt': '2019-05-01T00:00:00Z'})
    members = response.json()['results']
    assert {member['id'] for member in members} == {old_created_new_modified.id, new.id}


@pytest.mark.parametrize(
    'api',
    (
        'api-v3',
        'api-v4',
    ),
)
def test_get_members_with_fields_num_queries(client, django_assert_num_queries_lte, api):
    """
    GET /api/v3(v4)/services/members/?fields=...

    Проверим, что при запросе любого из доступных полей количество запросов к базе не увеличивается
    """
    factories.ServiceMemberFactory.create_batch(100)
    url = reverse('%s:service-member-list' % api)
    response = client.json.get(url)
    assert response.status_code == 200
    data = response.json()
    all_fields = []
    stack = [(data['results'][0], '')]
    while stack:
        element, prefix = stack.pop()
        for item, value in element.items():
            new_prefix = f'{prefix}.{item}' if prefix else item
            if isinstance(value, dict):
                stack.append((value, new_prefix))
            all_fields.append(new_prefix)

    waffle_query_count = 1
    total_query_limit = 5 + waffle_query_count  # function and module run counts differ

    for field in all_fields:
        with django_assert_num_queries_lte(total_query_limit):
            response = client.json.get(url, data={'fields': field})
        assert response.status_code == 200


def test_get_members_staff_id(client, data):
    url = reverse('api-v3:service-member-list')
    response = client.json.get(url)
    assert response.status_code == 200

    json = response.json()['results']
    assert len(json) == ServiceMember.objects.team().count()

    assert {member['id'] for member in json} == set(ServiceMember.objects.team().values_list('id', flat=True))

    assert {member['person']['id'] for member in json} == set(
        ServiceMember.objects.team().values_list('staff__staff_id', flat=True)
    )

    assert {member['person']['id'] for member in json} != set(
        ServiceMember.objects.team().values_list('staff__id', flat=True)
    )


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list')
)
def test_members_list_excludes_deleted_services(client, endpoint_path):
    service = factories.ServiceFactory()
    deleted_service = factories.ServiceFactory(state=Service.states.DELETED)

    sm = factories.ServiceMemberFactory(service=service)
    factories.ServiceMemberFactory(service=deleted_service)

    response = client.json.get(reverse(endpoint_path))
    assert response.status_code == 200
    result = response.json()['results']
    assert len(result) == 1
    assert result[0]['id'] == sm.id


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list')
)
@pytest.mark.parametrize('is_robot', (None, False, True))
def test_members_filter_is_robot(client, endpoint_path, is_robot):
    """
    Тест фильтра is_robot (отфильтровать в выдаче только роботов)
    """
    service = factories.ServiceFactory()

    human_staff = factories.StaffFactory()
    human_service_member = factories.ServiceMemberFactory(service=service, staff=human_staff)

    robot_staff = factories.StaffFactory(is_robot=True)
    robot_service_member = factories.ServiceMemberFactory(service=service, staff=robot_staff)

    params = {} if is_robot is None else {'is_robot': is_robot}
    response = client.json.get(reverse(endpoint_path), params)
    assert response.status_code == 200
    result = response.json()['results']
    assert len(result) == 2 if is_robot is None else 1
    if is_robot is None:
        expected = {human_service_member.id, robot_service_member.id}
    else:
        expected = {robot_service_member.id if is_robot else human_service_member.id}
    assert {r['id'] for r in result} == expected


@pytest.mark.parametrize(
    'endpoint_path', ('services-api:member-list', 'api-v3:service-member-list', 'api-v4:service-member-list')
)
@pytest.mark.parametrize('unique', (None, False, True))
def test_members_filter_unique(client, endpoint_path, unique):
    """
    Тест фильтра уникальных членств (для каждого участника отдается только одно, самое старое членство)
    """
    other_service = factories.ServiceFactory()

    service = factories.ServiceFactory()

    staff1 = factories.StaffFactory()
    staff2 = factories.StaffFactory()

    factories.ServiceMemberFactory(service=other_service, staff=staff1)

    service_member_1_old = factories.ServiceMemberFactory(service=service, staff=staff1)
    service_member_1_new = factories.ServiceMemberFactory(service=service, staff=staff1)

    service_member_2 = factories.ServiceMemberFactory(service=service, staff=staff2)

    params = {'service': service.id} if unique is None else {'service': service.id, 'unique': unique}
    response = client.json.get(reverse(endpoint_path), params)
    assert response.status_code == 200
    result = response.json()['results']
    assert len(result) == 2 if unique else 3
    expected = {service_member_1_old.id, service_member_2.id}
    if not unique:
        expected.add(service_member_1_new.id)
    assert {r['id'] for r in result} == expected
