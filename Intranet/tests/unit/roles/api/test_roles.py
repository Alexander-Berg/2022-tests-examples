from functools import partial

import pretend
import pytest
from django.conf import settings
from django.core.urlresolvers import reverse
from mock import patch

from plan.roles.models import Role, RoleScope
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, staff_factory):
    person = staff_factory()
    metaservice = factories.ServiceFactory(
        slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG,
        name='Метасервис',
        name_en='MetaService',
    )
    service1 = factories.ServiceFactory(parent=metaservice, owner=person, slug='slug')
    service2 = factories.ServiceFactory()

    role_scope = factories.RoleScopeFactory(slug='scope', name='Скоуп')
    protected_scope = factories.RoleScopeFactory(protected=True)

    role1 = factories.RoleFactory(code=Role.EXCLUSIVE_OWNER, scope=role_scope)
    role2 = factories.RoleFactory(service=service1)
    role3 = factories.RoleFactory()
    non_exportable_role = factories.RoleFactory(is_exportable=False)

    fixture = pretend.stub(
        person=person,
        role1=role1,
        role2=role2,
        role3=role3,
        non_exportable_role=non_exportable_role,
        role_scope=role_scope,
        protected_scope=protected_scope,
        metaservice=metaservice,
        service1=service1,
        service2=service2,
    )
    return fixture


@pytest.fixture
def get_roles(client):
    return partial(
        client.json.get,
        reverse('roles-api:role-list')
    )


def test_get_roles(get_roles, client, data):
    client.login(data.person.login)

    response = get_roles()

    assert response.status_code == 200

    result = response.json()
    assert result['count'] == Role.objects.all().count()

    assert (
        set(
            (role_data['name']['ru'], role_data['name']['en'])
            for role_data in result['results']
        ) ==
        set(
            (role.name, role.name_en)
            for role in Role.objects.all()
        )
    )


def test_filter_by_service(get_roles, data):
    response = get_roles({'service': data.service1.pk})
    assert len(response.json()['results']) == 1

    response = get_roles({'service': [data.service1.pk, data.service2.pk]})
    assert len(response.json()['results']) == 1

    response = get_roles({'service__in': '%s,%s' % (data.service1.pk, data.service2.pk)})
    assert len(response.json()['results']) == 1


def test_filter_by_service_slug(get_roles, data):
    response = get_roles({'service__slug': data.service1.slug})
    assert len(response.json()['results']) == 1

    response = get_roles({'service__slug__in': '%s,%s' % (data.service1.slug, data.service2.slug)})
    assert len(response.json()['results']) == 1


@pytest.mark.skip
def test_filter_by_service_slug_is_case_insensitive(get_roles, data):
    response = get_roles({'service__slug': data.service1.slug.upper()})
    assert len(response.json()['results']) == 1


def test_filter_by_service__with_descendants(get_roles, data):
    response = get_roles({'service__with_descendants': data.metaservice.id})
    assert len(response.json()['results']) == 1

    response = get_roles({'service__with_descendants': data.service2.id})
    assert len(response.json()['results']) == 0


def test_filter_by_service__with_descendants_slug(get_roles, data):
    response = get_roles({'service__with_descendants__slug': data.metaservice.slug})
    assert len(response.json()['results']) == 1

    response = get_roles({'service__with_descendants__slug': data.service2.slug})
    assert len(response.json()['results']) == 0


def test_filter_by_service_include_global(get_roles, data):
    service = factories.ServiceFactory()
    factories.RoleFactory(service=service)

    response = get_roles({'service__slug': data.service1.slug, 'include_global': True})
    assert len(response.json()['results']) == 7
    assert [role['service'] is None or role['service'] == data.service1 for role in response.json()['results']]

    response = get_roles({'service': data.service1.id, 'include_global': False})
    assert len(response.json()['results']) == 1

    response = get_roles({'include_global': True})
    assert len(response.json()['results']) == 8

    response = get_roles({'service': [data.service1.id, service.id], 'include_global': True})
    assert len(response.json()['results']) == 8


def test_filter_by_scope(get_roles, data):
    response = get_roles({'scope': data.role_scope.id})
    assert len(response.json()['results']) == 1

    response = get_roles({'scope__in': data.role_scope.id})
    assert len(response.json()['results']) == 1


def test_filter_by_code(get_roles, data):
    response = get_roles({'code': Role.EXCLUSIVE_OWNER})
    assert len(response.json()['results']) == 1

    response = get_roles({'code__in': Role.EXCLUSIVE_OWNER})
    assert len(response.json()['results']) == 1

    response = get_roles({'code__in': '%s,xxx' % Role.EXCLUSIVE_OWNER})
    assert len(response.json()['results']) == 1

    response = get_roles({'code': 'xxx'})
    assert len(response.json()['results']) == 0


@pytest.mark.parametrize('filter_key', [
    'scope__can_issue_at_duty_time',
    'can_issue_at_duty_time',
])
def test_filter_by_can_issue_at_duty_time(get_roles, data, filter_key):
    """get_roles возвращает роли, доступные для выдачи дежурным"""
    data.role2.scope.can_issue_at_duty_time = False
    data.role2.scope.save()

    response = get_roles({filter_key: True})
    results = response.json()['results']
    role_ids = [r['id'] for r in results]
    assert data.role1.id not in role_ids
    assert data.role2.id not in role_ids
    assert data.role3.id in role_ids
    assert data.non_exportable_role.id in role_ids


@patch('plan.api.idm.actions.add_role')
def test_create_role(add_role, client, data):
    client.login(data.person.login)

    response = client.json.post(
        reverse('roles-api:role-list'),
        {
            'service': data.service1.pk,
            'scope': data.role_scope.pk,
            'name': {'ru': 'Админ', 'en': 'Admin'},
            'code': 'admin',
        }
    )

    assert response.status_code == 200

    role = Role.objects.get(code='admin')
    assert role.service == data.service1
    assert role.scope == data.role_scope
    assert role.name == 'Админ'
    assert role.name_en == 'Admin'
    assert role.native_lang == 'ru'

    assert response.json()['id'] == role.id

    add_role.assert_called_once_with(role)


def test_create_role_without_required_fields(client, data):
    client.login(data.person.login)

    response = client.json.post(
        reverse('roles-api:role-list'),
        {
            'scope': data.role_scope.pk,
            'name': {'ru': 'Админ', 'en': 'Admin'},
            'code': 'admin',
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['extra'] == {'service': ['This field is required.']}

    response = client.json.post(
        reverse('roles-api:role-list'),
        {
            'service': data.service1.pk,
            'name': {'ru': 'Админ', 'en': 'Admin'},
            'code': 'admin',
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['extra'] == {'scope': ['This field is required.']}


def test_unique_name_in_one_service(client, data):
    client.login(data.person.login)

    role = factories.RoleFactory(service=data.service1)

    response = client.json.post(
        reverse('roles-api:role-list'),
        {
            'service': data.service1.pk,
            'scope': data.role_scope.pk,
            'name': {'ru': role.name, 'en': 'xxx'},
            'code': 'role 2',
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['extra'] == {
        'non_field_errors': ['В этом сервисе уже есть роль с таким именем.']
    }

    response = client.json.post(
        reverse('roles-api:role-list'),
        {
            'service': data.service1.pk,
            'scope': data.role_scope.pk,
            'name': {'ru': 'xxx', 'en': role.name_en},
            'code': 'role 2',
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['extra'] == {
        'non_field_errors': ['В этом сервисе уже есть роль с таким английским именем.']
    }


@patch('plan.api.idm.actions.add_role')
def test_unique_name_in_different_services(add_role, client, data):
    client.login(data.person.login)

    role2 = factories.RoleFactory(scope=data.role_scope, name='Роль 2', service=data.service1)
    role2.save()
    service2 = factories.ServiceFactory(parent=data.metaservice, owner=data.person)
    service2.save()

    response = client.json.post(
        reverse('roles-api:role-list'),
        {
            'service': service2.pk,
            'scope': data.role_scope.pk,
            'name': {'ru': 'Роль 2', 'en': 'Role 2'},
            'code': 'role 2',
        }
    )

    assert response.status_code == 200

    role = Role.objects.get(code='role 2')
    assert role.service == service2
    assert role.scope == data.role_scope
    assert role.name == 'Роль 2'


def test_names_must_be_unique_within_scope(client, data):
    client.login(data.person.login)

    factories.RoleFactory(
        scope=data.role_scope,
        name='Админ'
    )

    response = client.json.post(
        reverse('roles-api:role-list'),
        {
            'service': data.service1.pk,
            'scope': data.role_scope.pk,
            'name': {'ru': 'Админ', 'en': 'Admin'},
            'code': 'admin',
        }
    )

    assert response.status_code == 400

    error = response.json()['error']
    assert error['code'] == 'validation_error'
    assert error['extra']['non_field_errors'] == [
        'Роль с таким названием уже существует в этом скоупе ролей, выберите другое.'
    ]


def test_en_names_must_be_unique_within_scope(client, data):
    client.login(data.person.login)

    factories.RoleFactory(
        scope=data.role_scope,
        name_en='Admin'
    )

    response = client.json.post(
        reverse('roles-api:role-list'),
        {
            'service': data.service1.pk,
            'scope': data.role_scope.pk,
            'name': {'ru': 'Админ', 'en': 'Admin'},
            'code': 'admin',
        }
    )

    assert response.status_code == 400

    error = response.json()['error']
    assert error['code'] == 'validation_error'
    assert error['extra']['non_field_errors'] == [
        'Роль с таким английским названием уже существует в этом скоупе ролей, выберите другое.'
    ]


@patch('plan.api.idm.actions.add_role')
def test_cannot_create_local_roles_in_protected_scope(add_role, client, data):
    client.login(data.person.login)

    response = client.json.post(
        reverse('roles-api:role-list'),
        {
            'service': data.service1.pk,
            'scope': data.protected_scope.pk,
            'name': {'ru': 'Админ', 'en': 'Admin'},
            'code': 'admin',
        }
    )

    assert response.status_code == 400

    result = response.json()
    assert result['error']['code'] == 'validation_error'
    assert result['error']['extra']['scope'] == [
        'В этом скоупе ролей нельзя создавать локальные роли.'
    ]

    assert not add_role.called


def test_filter_by_protected(client, data):
    client.login(data.person.login)

    response = client.json.get(reverse('api-v3:role-scope-list'))
    assert response.status_code == 200

    result = response.json()
    assert result['count'] == RoleScope.objects.count()

    response = client.json.get(
        reverse('api-v3:role-scope-list'),
        {'protected': True}
    )
    assert response.status_code == 200

    result = response.json()
    assert result['count'] == RoleScope.objects.filter(protected=True).count()
    assert result['results'][0]['id'] == data.protected_scope.id
    assert result['results'][0]['utility_scope'] == data.protected_scope.utility_scope


@pytest.mark.parametrize('filter_by', ('id', 'slug'))
@pytest.mark.parametrize('with_filter', (True, False))
def test_filter_by_available(client, filter_by, with_filter):
    Role.objects.all().delete()
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    service_role = factories.RoleFactory(service=service)
    factories.RoleFactory(service=factories.ServiceFactory())
    params = {}
    if with_filter:
        if filter_by == 'id':
            params = {'available_for': service.id}
        else:
            params = {'available_for': service.slug}

    response = client.json.get(reverse('api-v4:role-list'), params)
    assert response.status_code == 200, response.content
    data = response.json()['results']

    if with_filter:
        assert len(data) == 2
        assert {role['id'] for role in data} == {role.id, service_role.id}
    else:
        assert len(data) == 3


@pytest.mark.parametrize('filter_mode', ['exportable', 'nonexportable', 'all'])
def test_filter_by_exportable(client, data, filter_mode):
    client.login(data.person.login)
    target_role = data.non_exportable_role

    if filter_mode == 'all':
        response = client.json.get(reverse('api-v3:role-list'))
    elif filter_mode == 'exportable':
        response = client.json.get(
            reverse('api-v3:role-list'),
            {'is_exportable': True},
        )
    else:
        response = client.json.get(
            reverse('api-v3:role-list'),
            {'is_exportable': False},
        )

    assert response.status_code == 200
    results = response.json()['results']

    if filter_mode == 'all':
        assert target_role.id in {js['id'] for js in results}
        assert len(results) == Role.objects.count()
    elif filter_mode == 'exportable':
        assert target_role.id not in {js['id'] for js in response.json()['results']}
        assert len(results) == Role.objects.count() - 1
    else:
        assert target_role.id in {js['id'] for js in response.json()['results']}
        assert len(results) == 1
