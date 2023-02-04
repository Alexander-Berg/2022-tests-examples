import pytest
from requests import codes as http_codes
from rest_framework.test import APIRequestFactory

from plan.resources.api.permissions import PermissionView, AssertView, ServiceMemberSerializer
from plan.resources.models import ServiceResource
from plan.services.models import ServiceMember
from common import factories

SOURCE_SLUG = 'test_test'
TYPE_NAME = 'test_type'

pytestmark = [
    pytest.mark.django_db,
]


def user_name(id):
    return 'test_user_{}'.format(id)


def role_name(id):
    return 'test_role_{}'.format(id)


def service_name(id):
    return 'test_service_{}'.format(id)


def resource_name(id):
    return 'test_resource_{}'.format(id)


user_count = 6

roles_data = {
    0: {'scope': 'design'},
    1: {'scope': 'development'},
    2: {'scope': 'testing'},
    3: {'scope': 'analytics'},
}

services_data = {
    0: {
        'for_requests': True,
        'members': [
            {'role': 0, 'user': 0},
            {'role': 1, 'user': 0},
            {'role': 2, 'user': 1},
            {'role': 3, 'user': 2},
        ],
    },
    1: {
        'for_requests': True,
        'members': [
            {'role': 0, 'user': 2},
            {'role': 1, 'user': 2},
            {'role': 2, 'user': 4},
            {'role': 2, 'user': 5},
        ],
    },
    2: {
        'for_requests': False,
        'members': [
            {'role': 2, 'user': 3},
            {'role': 3, 'user': 2},
        ],
    }
}

resources_data = {
    0: {'services': [0, 1]},
    1: {'services': [2]},
}


def create_test_resource(key):
    test_supplier = factories.ServiceFactory(
        slug=SOURCE_SLUG,
        name=SOURCE_SLUG
    )
    test_type = factories.ResourceTypeFactory(
        name=TYPE_NAME,
        supplier=test_supplier,
    )
    test_resource = factories.ResourceFactory(
        external_id=resource_name(key),
        type=test_type,
    )
    test_supplier.save()
    test_type.save()
    test_resource.save()

    return test_resource


# NOTE (lavrukov): Здесь лучше сделать scope='module', так как ручка использует
# только select запросы к базе, но в данный момент сделать это в pytest
# невозможно, так как база инициализируется для каждого теста заново
@pytest.fixture
def data(db):
    test_supplier = factories.ServiceFactory(
        slug=SOURCE_SLUG,
        name=SOURCE_SLUG
    )
    resource_type = factories.ResourceTypeFactory(
        name=TYPE_NAME,
        supplier=test_supplier,
    )
    test_supplier.save()
    resource_type.save()

    users = {}
    for key in range(user_count):
        user = factories.StaffFactory(login=user_name(key))
        user.save()
        users[key] = user

    roles = {}
    for key, role_data in roles_data.items():
        role = factories.RoleFactory(
            name=role_name(key),
            scope=factories.RoleScopeFactory(slug=role_data['scope']),
        )
        role.save()
        roles[key] = role

    services = {}
    members = []
    for key, service_data in services_data.items():
        service = factories.ServiceFactory(slug=service_name(key))
        service.save()
        services[key] = service

        service_for_requests = service_data['for_requests']
        for member_data in service_data['members']:
            member = factories.ServiceMemberFactory(
                service=service,
                role=roles[member_data['role']],
                staff=users[member_data['user']],
            )
            member.save()

            if service_for_requests:
                members.append(member)

    resources = {}
    for key, resource_data in resources_data.items():
        resource = factories.ResourceFactory(
            external_id=resource_name(key),
            type=resource_type,
        )
        resource.save()
        resources[key] = resource

        for service_key in resource_data['services']:
            factories.ServiceResourceFactory(
                service=services[service_key],
                resource=resource,
                state=ServiceResource.GRANTED,
            ).save()

    return {
        'resource': resources[0],
        'services': services,
        'roles': roles,
        'users': users,
        'members': members
    }


def get_response(endpoint, view, resource_id, **params):
    resource_params = {
        'resource_id': resource_name(resource_id),
        'resource_type': TYPE_NAME,
        'resource_source': SOURCE_SLUG,
    }
    resource_params.update(params)
    factory = APIRequestFactory()

    # TODO: переписать на обычного client
    factories.ServiceFactory()
    request = factory.get(endpoint, resource_params)
    request.user = factories.UserFactory()
    return view.as_view()(request)


def get_permission_response(resource_id=0, **params):
    return get_response(
        endpoint='resources/permissions/',
        view=PermissionView,
        resource_id=resource_id,
        **params
    )


def get_assert_response(resource_id=0, **params):
    return get_response(
        endpoint='resources/assert/',
        view=AssertView,
        resource_id=resource_id,
        **params
    )


def get_permissions(**params):
    response = get_permission_response(**params)
    assert response.status_code == http_codes.ok
    return response.data['results']


def get_assertion(status_code=http_codes.ok, **params):
    response = get_assert_response(**params)
    assert response.status_code == status_code
    if status_code == http_codes.ok:
        assert 'assertion' in response.data
        assertion = response.data['assertion']
        assert isinstance(assertion, bool)
    else:
        assertion = False
    return assertion


def sort_key(permission):
    key_names = ['staff', 'service', 'role']
    return tuple(permission[key_name]['id'] for key_name in key_names)


default_queryset = (
    ServiceMember.objects.filter(
        service__serviceresource__resource__external_id=resource_name(0),
        service__serviceresource__type__name=TYPE_NAME,
        service__serviceresource__type__supplier__slug=SOURCE_SLUG,
        service__serviceresource__state__in=ServiceResource.ACTIVE_STATES,
    )
    .select_related('role', 'staff', 'service')
)


def assert_permission(endpoint_query, queryset):
    permissions = get_permissions(**endpoint_query)

    permissions.sort(key=sort_key)
    assert len(permissions) > 0

    db_permissions = ServiceMemberSerializer(queryset, many=True).data
    db_permissions.sort(key=sort_key)

    assert permissions == db_permissions


def assert_assertion(endpoint_query, queryset):
    assertion = get_assertion(**endpoint_query)
    assert assertion and queryset.count() > 0


def test_without_arguments(data):
    queryset = default_queryset.filter()
    endpoint_query = {}
    assert_permission(endpoint_query, queryset)
    assert_assertion(endpoint_query, queryset)


def test_with_user(data):
    queryset = default_queryset.filter(staff=data['users'][0])
    endpoint_query = dict(user=data['users'][0].login)
    assert_permission(endpoint_query, queryset)
    assert_assertion(endpoint_query, queryset)


def test_with_role(data):
    queryset = default_queryset.filter(role=data['roles'][0])
    endpoint_query = dict(role=data['roles'][0].id)
    assert_permission(endpoint_query, queryset)
    assert_assertion(endpoint_query, queryset)


def test_with_role_scope(data):
    queryset = default_queryset.filter(role__scope=data['roles'][1].scope)
    endpoint_query = dict(role_scope=data['roles'][1].scope_id)
    assert_permission(endpoint_query, queryset)
    assert_assertion(endpoint_query, queryset)


def test_with_service(data):
    queryset = default_queryset.filter(service=data['services'][0])
    endpoint_query = dict(service=data['services'][0].slug)
    assert_permission(endpoint_query, queryset)
    assert_assertion(endpoint_query, queryset)


def test_with_user_role(data):
    queryset = default_queryset.filter(
        staff=data['users'][4],
        role=data['roles'][2],
    )
    endpoint_query = dict(
        user=data['users'][4].login,
        role=data['roles'][2].id,
    )
    assert_permission(endpoint_query, queryset)
    assert_assertion(endpoint_query, queryset)


def test_with_service_user_role(data):
    queryset = default_queryset.filter(
        service=data['services'][0],
        staff=data['users'][1],
        role=data['roles'][2],
    )
    endpoint_query = dict(
        service=data['services'][0].slug,
        user=data['users'][1].login,
        role=data['roles'][2].id,
    )
    assert_permission(endpoint_query, queryset)
    assert_assertion(endpoint_query, queryset)


def test_full_query(data):
    queryset = default_queryset.filter(
        service=data['services'][0],
        staff=data['users'][1],
        role=data['roles'][2],
        role__scope=data['roles'][2].scope
    )
    endpoint_query = dict(
        service=data['services'][0].slug,
        user=data['users'][1].login,
        role=data['roles'][2].id,
        role_scope=data['roles'][2].scope_id
    )
    assert_permission(endpoint_query, queryset)
    assert_assertion(endpoint_query, queryset)


def test_empty(data):
    queryset = default_queryset.filter(
        staff=data['users'][3],
        role=data['roles'][2],
    )
    endpoint_query = dict(
        user=data['users'][3].login,
        role=data['roles'][2].id,
    )
    assert queryset.count() == 0

    response = get_permission_response(**endpoint_query)
    assert response.status_code == http_codes.ok
    assert response.data['count'] == 0
    assert response.data['results'] == []
    assert not get_assertion(**endpoint_query)


@pytest.mark.parametrize('params', [
    {'resource_id': 3},
    {'role': role_name(0)},
    {'role_scope': 'invalid_role_scope'},
])
def test_bad_request(data, params):
    status_code = http_codes.bad_request
    response = get_permission_response(**params)
    assert response.status_code == http_codes.bad_request
    assert not get_assertion(status_code=status_code, **params)


@pytest.mark.parametrize('params', [
    {'user': 'invalid_user', },
    {'role': 1337},
    {'service': 'invalid_service'},
])
def test_good_request(data, params):
    """
    проверяем что фильтрация по валидному значению, которого
    нет в базе возвращает пустой результат
    """
    status_code = http_codes.ok
    response = get_permission_response(**params)
    assert response.status_code == http_codes.ok
    assert response.data['count'] == 0
    assert not get_assertion(status_code=status_code, **params)
