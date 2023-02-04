# coding: utf-8


import datetime
import random
from unittest import mock
from urllib.parse import urljoin

import pytest
from constance.test import override_config
from django.core import mail
from django.core.management import call_command
from mock import patch
from requests import ConnectionError

from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.core.constants.role import ROLE_STATE
from idm.core.constants.rolefield import FIELD_TYPE, FIELD_STATE
from idm.core.constants.system import (
    SYSTEM_AUTH_FACTOR,
    SYSTEM_PLUGIN_TYPE,
    SYSTEM_NODE_PLUGIN_TYPE,
    SYSTEM_AUDIT_METHOD,
    SYSTEM_GROUP_POLICY,
    SYSTEM_REQUEST_POLICY,
    SYSTEM_ROLE_GRANT_POLICY,
    SYSTEM_INCONSISTENCY_POLICY,
    SYSTEM_PASSPORT_POLICY,
    SYSTEM_WORKFLOW_APPROVE_POLICY,
    SYSTEM_ROLETREE_POLICY,
    SYSTEM_REVIEW_ON_RELOCATE_POLICY,
)
from idm.core.models import FavoriteSystem, RoleNode, Action, System, SystemRolePush, Role
from idm.inconsistencies.models import Inconsistency
from idm.services.models import Service
from idm.tests.utils import (refresh, add_perms_by_role, remove_perms_by_role, mock_tree, assert_contains, Response,
                             mock_all_roles,
                             models_didnt_change, raw_make_role, create_system, capture_http, assert_http,
                             clear_mailbox, set_workflow, create_user, run_commit_hooks, create_tvm_app, add_members)
from idm.users.constants.group import GROUP_TYPES
from idm.users.models import Group, GroupMembership, GroupResponsibility
from idm.utils import reverse, events

# разрешаем использование базы в  тестах
pytestmark = pytest.mark.django_db
API_NAMES = ('frontend', 'testapi')


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_get_systems(client, simple_system, pt1_system, users_for_test, api_name):
    """
    GET /frontend/systems/
    """
    client.login('art')
    (art, fantom, terran, admin) = users_for_test

    url = reverse('api_dispatch_list', api_name=api_name, resource_name='systems')

    response = client.json.get(url)
    data = response.json()
    assert data['meta']['total_count'] == 2
    assert [system_data['slug'] for system_data in data['objects']] == ['simple', 'test1']
    assert all([system_data['is_active'] for system_data in data['objects']])
    assert 'workflow' not in data['objects'][0]
    expected = {
        'description',
        'emails',
        'endpoint_timeout',
        'endpoint_long_timeout',
        'group_policy',
        'is_active',
        'is_broken',
        'is_favorite',
        'is_sox',
        'name',
        'use_webauth',
        'responsibles',
        'service',
        'slug',
        'state',
        'team',
        'use_mini_form',
        'permissions',
    }
    assert set(data['objects'][0].keys()) == expected

    # state
    simple_system.is_active = True
    simple_system.is_broken = False
    simple_system.save()
    data = client.json.get(url, {'system__contains': 'simple'}).json()
    assert data['objects'][0]['state'] == 'Активна'

    simple_system.is_active = True
    simple_system.is_broken = True
    simple_system.save()
    data = client.json.get(url, {'system__contains': 'simple'}).json()
    assert data['objects'][0]['state'] == 'Сломана'

    simple_system.is_active = True
    simple_system.is_broken = True
    simple_system.save()
    data = client.json.get(url, {'system__contains': '  simple '}).json()
    assert data['objects'][0]['state'] == 'Сломана'

    simple_system.is_active = False
    simple_system.is_broken = False
    simple_system.save()
    data = client.json.get(url, {'system__contains': 'simple', 'state': 'inactive'}).json()
    assert data['objects'][0]['state'] == 'Выключена'

    simple_system.is_active = False
    simple_system.is_broken = True
    simple_system.save()
    data = client.json.get(url, {'system__contains': 'simple', 'state': 'inactive'}).json()
    assert data['objects'][0]['state'] == 'Выключена'

    simple_system.is_active = True
    simple_system.is_broken = False
    simple_system.save()

    # check if only active systems in response
    pt1_system.is_active = False
    pt1_system.save()
    data = client.json.get(url).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'simple'

    # is_broken
    pt1_system.is_active = True
    pt1_system.is_broken = True
    pt1_system.save()

    data = client.json.get(url, {'is_broken': True}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'test1'

    data = client.json.get(url, {'is_broken': False}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'simple'

    # is_sox
    pt1_system.is_sox = True
    pt1_system.save()

    data = client.json.get(url, {'is_sox': True}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'test1'

    data = client.json.get(url, {'is_sox': False}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'simple'

    # use_webauth
    pt1_system.use_webauth = True
    pt1_system.save()

    data = client.json.get(url, {'use_webauth': True}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'test1'

    data = client.json.get(url, {'use_webauth': False}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'simple'

    # is_favorite
    FavoriteSystem.objects.create(system=pt1_system, user=art)

    data = client.json.get(url, {'is_favorite': True}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'test1'
    assert data['objects'][0]['is_favorite']

    # responsibles
    add_perms_by_role('responsible', fantom, simple_system)

    data = client.json.get(url, {'responsibles': 'fantom'}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'simple'

    # team_members
    add_perms_by_role('users_view', terran, pt1_system)

    data = client.json.get(url, {'team_members': 'terran'}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'test1'

    # service
    service = Service(slug='test_service')
    service.save()
    pt1_system.service = service
    pt1_system.save()

    data = client.json.get(url, {'service': pt1_system.service.external_id}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'test1'
    assert set(data['objects'][0]['service'].keys()) == {
        'created_at',
        'description_html',
        'description_wiki',
        'id',
        'is_vteam',
        'name',
        'parent',
        'root',
        'slug',
        'state',
        'updated_at'
    }

    # state == active
    pt1_system.is_active = True
    pt1_system.is_broken = False
    pt1_system.save()

    simple_system.is_active = True
    simple_system.is_broken = True
    simple_system.save()

    data = client.json.get(url, {'state': 'active'}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'test1'

    # state == inactive
    pt1_system.is_active = True
    pt1_system.save()

    simple_system.is_active = False
    simple_system.save()

    data = client.json.get(url, {'state': 'inactive'}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'simple'

    # state == broken
    pt1_system.is_active = True
    pt1_system.is_broken = True
    pt1_system.save()

    simple_system.is_active = False
    simple_system.is_broken = True
    simple_system.save()

    data = client.json.get(url, {'state': 'broken'}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'test1'

    # system__contains
    data = client.json.get(url, {'system__contains': 'est1'}).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'test1'


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_get_systems_security(client, simple_system, pt1_system, users_for_test, api_name):
    """
    GET /frontend/systems/
    """
    art = users_for_test[0]
    add_perms_by_role('security', art)
    client.login('art')

    url = reverse('api_dispatch_list', api_name=api_name, resource_name='systems')

    response = client.json.get(url)
    data = response.json()
    assert data['meta']['total_count'] == 2
    assert [system_data['slug'] for system_data in data['objects']] == ['simple', 'test1']
    assert all([system_data['is_active'] for system_data in data['objects']])
    assert 'workflow' not in data['objects'][0]
    expected = {
        'description',
        'emails',
        'endpoint_timeout',
        'endpoint_long_timeout',
        'group_policy',
        'is_active',
        'is_broken',
        'is_favorite',
        'is_sox',
        'name',
        'responsibles',
        'service',
        'slug',
        'state',
        'team',
        'use_mini_form',
        'permissions',
        'auth_factor',
        'base_url',
        'tvm_id',
        'plugin_type',
        'use_webauth',
    }
    assert set(data['objects'][0].keys()) == expected


def test_get_systems_ordering(client, simple_system, pt1_system, complex_system, arda_users):
    client.login('frodo')

    url = reverse('api_dispatch_list', api_name='frontend', resource_name='systems')

    # service
    root = Service.objects.get_root()
    root1 = Service.objects.create(external_id=1, name='root1', parent=root)
    root2 = Service.objects.create(external_id=2, name='root2', parent=root)

    service_a = Service.objects.create(external_id=3, name='a', parent=root1, root=root1)
    service_b = Service.objects.create(external_id=4, name='b', parent=root2, root=root2)
    service_c = Service.objects.create(external_id=5, name='c', parent=root1, root=root1)

    simple_system.service = service_a
    simple_system.save()
    pt1_system.service = service_b
    pt1_system.save()
    complex_system.service = service_c
    complex_system.save()

    data = client.json.get(url, {'order_by': 'service'}).json()
    assert [system['service']['name'] for system in data['objects']] == ['a', 'c', 'b']  # service.root > service

    # name
    data = client.json.get(url).json()
    assert [system['slug'] for system in data['objects']] == sorted(system.slug for system in System.objects.all())


def test_get_system(client, simple_system, users_for_test):
    """
    GET /frontend/systems/simple/
    """
    client.login('art')
    data = client.json.get(reverse('api_dispatch_detail', api_name='frontend',
                                   resource_name='systems', slug='simple')).json()
    assert data['slug'] == 'simple'
    assert data['emails'] == ['simple@yandex-team.ru', 'simplesystem@yandex-team.ru']
    assert data['is_active'] is True
    assert 'workflow' not in data
    assert 'responsibles' not in data
    assert 'team' not in data


def test_get_system_with_fields(client, simple_system, users_for_test):
    """
    GET /frontend/systems/simple/
    """
    field_data = {
        'name': 'тестовое поле',
        'name_en': 'test field',
        'slug': 'test_field',
        'options': {},
        'state': FIELD_STATE.ACTIVE,
        'type': FIELD_TYPE.CHARFIELD
    }
    simple_system.systemrolefields.create(**field_data)
    field_data['slug'] = 'test_field2'
    simple_system.systemrolefields.create(**field_data)

    client.login('art')
    data = client.json.get(reverse('api_dispatch_detail', api_name='frontend',
                                   resource_name='systems', slug='simple')).json()

    assert data['slug'] == 'simple'
    assert data['rolefields'] == 'test_field,test_field2'


def test_get_system_is_case_insensitive(client, simple_system, complex_system, arda_users):
    """
    GET /frontend/systems/simple/
    """
    client.login(create_user())
    response = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug='SimPlE')
    )
    assert response.status_code == 200
    data = response.json()
    assert data['slug'] == 'simple'
    assert data['emails'] == ['simple@yandex-team.ru', 'simplesystem@yandex-team.ru']
    assert data['is_active'] is True
    assert 'workflow' not in data


def test_get_system_lang(client, simple_system, users_for_test):
    """
    GET /frontend/systems/simple/
    """
    simple_system.description = 'Обычная система'
    simple_system.description_en = 'Usual system'
    simple_system.save()
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug='simple')
    list_url = reverse('api_dispatch_list', api_name='frontend', resource_name='systems')

    client.login('art')
    data = client.json.get(url).json()
    assert data['name'] == {'ru': 'Simple система', 'en': 'Simple system'}
    assert data['description'] == {'ru': 'Обычная система', 'en': 'Usual system'}

    data = client.json.get(list_url).json()
    assert data['objects'][0]['name'] == {'ru': 'Simple система', 'en': 'Simple system'}
    assert data['objects'][0]['description'] == {'ru': 'Обычная система', 'en': 'Usual system'}


def test_edit_system(client, simple_system, arda_users):
    """Пока что редактирование разрешено только ответственным и только нескольких полей"""

    frodo = arda_users.frodo
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug='simple')
    response = client.json.put(url, {'name': {'ru': 'Простая', 'en': 'Simplish'}})
    assert response.status_code == 403
    assert response.json() == {
        'message': 'Вы не можете редактировать данную систему',
        'error_code': 'FORBIDDEN'
    }

    add_perms_by_role('responsible', frodo, simple_system)
    response = client.json.put(url, {'name': {'ru': 'Простая', 'en': 'Simplish'}})
    assert response.status_code == 204
    simple_system = refresh(simple_system)
    assert simple_system.name == 'Простая'
    assert simple_system.name_en == 'Simplish'
    assert simple_system.description == ''

    response = client.json.put(url, {
        'name': {'ru': 'Проще', 'en': 'Simpler'},
        'description': {'ru': 'Описание простое', 'en': 'Simple desc'},
        'group_policy': SYSTEM_GROUP_POLICY.AWARE,
    })
    assert response.status_code == 204
    simple_system = refresh(simple_system)
    assert simple_system.name == 'Проще'
    assert simple_system.name_en == 'Simpler'
    assert simple_system.description == 'Описание простое'
    assert simple_system.description_en == 'Simple desc'
    # кроме name/description, редактировать ничего нельзя
    assert simple_system.group_policy == 'unaware'


@pytest.mark.parametrize('field', ('base_url', 'roles_tree_url'))
@pytest.mark.parametrize(('url', 'expected_status'), (
        ('https://test.yandex.team.ru', 200),
        ('http://test.yandex.team.ru', 200),
        ('ftp://test.yandex.team.ru', 400),
        ('file:///etc/passwd', 400),
        ('test.yandex.team.ru', 400),
))
def test_edit_system__invalid_url(client, field: str, url: str, expected_status: int):
    system = create_system()
    client.login(create_user(superuser=True))

    response = client.json.patch(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug=system.slug),
        {field: url}
    )
    assert response.status_code == expected_status, response.json()
    if expected_status == 200:
        system.refresh_from_db(fields=[field])
        assert getattr(system, field) == url
    else:
        result = response.json()
        assert result['message'] == 'Invalid data sent', result
        assert result['errors'] == {field: ['Введите правильный URL.']}, result


def test_create_system(client, arda_users, department_structure, self_system):
    """
    POST /frontend/systems/
    """
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    valinor = department_structure.valinor
    valinor.state = 'deprived'
    valinor.save()
    client.login('frodo')
    root = Service.objects.get_root()
    root1 = Service.objects.create(external_id=1, name='root1', parent=root)

    url = reverse('api_dispatch_list', api_name='frontend', resource_name='systems')
    data = {
        'slug': 'SiMpLe',
        'name': {'ru': 'Простая', 'en': 'Simple'},
        'team_members_user': 'bilbo,bilbo,sam',
        'responsibles_group': '{},{},{}'.format(fellowship.external_id, shire.external_id, valinor.external_id),
        'description': {'ru': 'Простая система', 'en': 'Simple system'},
        'service': '1',
    }
    response = client.json.post(url, data)
    assert response.status_code == 201
    call_command('request_new_system_responsibles')
    system = System.objects.get(slug='simple')
    for field in ['name', 'description']:
        assert getattr(system, field) == data.get(field, '')['ru']
        assert getattr(system, field + '_en') == data.get(field, '')['en']
    system_node = self_system.nodes.get(slug='simple')
    assert system_node.slug_path == '/group/system/system_on/simple/'
    responsibles_node = system_node.children.get().children.get(slug='responsible')
    team_members_node = system_node.children.get().children.get(slug='users_view')
    assert responsibles_node.roles.exclude(group=None).count() == 2
    assert responsibles_node.roles.exclude(user=None).filter(parent=None).count() == 0
    assert team_members_node.roles.exclude(group=None).count() == 0
    assert team_members_node.roles.count() == 2
    assert SystemRolePush.objects.all().count() == 0

    repeat_response = client.json.post(url, data)
    assert repeat_response.status_code == 400
    assert repeat_response.json()['message'] == 'System with slug "simple" already exists'

    data['slug'] = 'even_more_simple'
    required_fields = ['slug', 'name']
    for field in required_fields:
        modified_data = data.copy()
        modified_data.pop(field)
        response = client.json.post(url, modified_data)
        assert response.status_code == 400
        assert response.json()['message'] == 'Invalid data sent'
        assert set(response.json()['errors'].keys()) == {field}

    modified_data = data.copy()
    modified_data['responsibles_user'] = 'harry_potter'
    response = client.json.post(url, modified_data)
    assert response.status_code == 400
    assert response.json()['message'] == 'Invalid data sent'

    data['team_members_group'] = data['responsibles_group']
    data['responsibles_user'] = data['team_members_user']
    for not_required_field in ['description']:
        modified_data = data.copy()
        modified_data['slug'] = 'simple_system_without_{}'.format(not_required_field)
        modified_data.pop(not_required_field)
        response = client.json.post(url, modified_data)
        assert response.status_code == 201
        assert System.objects.filter(slug=modified_data['slug']).exists()


def test_create_system_no_responsibles(create_system_response, create_system_data):
    del create_system_data['responsibles_group']
    response = create_system_response(create_system_data)
    assert response.status_code == 400
    assert response.json()['errors']['responsibles_user'] == ['Нет ответственных за систему']


def test_create_system_empty_name(create_system_response, create_system_data):
    create_system_data['name'] = {'ru': u' ', 'en': '\t'}
    response = create_system_response(create_system_data)
    assert response.status_code == 400
    assert response.json()['errors']['name'] == ['Имя не может быть пустым']


def test_create_system_zero_service(create_system_response, create_system_data):
    create_system_data['service'] = '0'
    response = create_system_response(create_system_data)
    assert response.status_code == 400
    assert response.json()['errors']['service'] == [
        'Выберите корректный вариант. Вашего варианта нет среди допустимых значений.'
    ]


def test_create_system_no_service(create_system_response, create_system_data):
    del create_system_data['service']
    response = create_system_response(create_system_data)
    assert response.status_code == 400
    assert response.json()['errors']['service'] == ['Обязательное поле.']


@pytest.fixture
def create_system_response(client, arda_users, department_structure, self_system):
    def _create_system_response(data):
        client.login('frodo')
        url = reverse('api_dispatch_list', api_name='frontend', resource_name='systems')
        response = client.json.post(url, data)
        return response

    return _create_system_response


@pytest.fixture
def create_system_data(department_structure):
    fellowship = department_structure.fellowship
    shire = department_structure.shire
    root = Service.objects.get_root()
    Service.objects.create(external_id=1, name='root1', parent=root)
    data = {
        'slug': 'SiMpLe',
        'name': {'ru': u'Простая', 'en': 'Simple'},
        'responsibles_group': '{},{}'.format(fellowship.external_id, shire.external_id),
        'team_members_user': 'bilbo,bilbo,sam',
        'description': {'ru': u'Простая система', 'en': 'Simple system'},
        'service': '1',
    }
    return data


@pytest.mark.parametrize('role', ['responsible', 'users_view', 'superuser'])
def test_patch_sox_system(client, arda_users, simple_system, role):
    """
    PATCH /frontend/systems/
    """
    frodo = arda_users.frodo
    client.login('frodo')
    service = Service.objects.create(external_id=123)
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug='simple')
    sox_data = {
        'name': {'ru': 'Простая обновленная', 'en': 'Simple updated'},
        'description': {'ru': 'Обновленное описание', 'en': 'Updated desription'},
        'group_policy': SYSTEM_GROUP_POLICY.AWARE,
        'passport_policy': SYSTEM_PASSPORT_POLICY.UNIQUE_FOR_USER,
        'request_policy': SYSTEM_REQUEST_POLICY.SUBORDINATES,
        'role_grant_policy': SYSTEM_ROLE_GRANT_POLICY.SYSTEM,
        'roletree_policy': SYSTEM_ROLETREE_POLICY.NONEDITABLE,
        'service': {'id': service.external_id},
        'audit_method': SYSTEM_AUDIT_METHOD.GET_ROLES,
        'plugin_type': SYSTEM_PLUGIN_TYPE.GENERIC,
        'node_plugin_type': None,
        'use_tvm_role': True,
        'emails': ['simple@yandex-team.ru', 'simplesystem@yandex-team.ru', 'polosate@yandex-team.com'],
        'sync_interval': '10',
    }
    data = {
        'is_sox': False,
        'has_review': False,
        'review_on_relocate_policy': SYSTEM_REVIEW_ON_RELOCATE_POLICY.IGNORE,
        'inconsistency_policy': SYSTEM_INCONSISTENCY_POLICY.TRUST,
        'workflow_approve_policy': SYSTEM_WORKFLOW_APPROVE_POLICY.ANOTHER,
    }
    extended_data = {
        'can_be_broken': True,
        'roles_review_days': 1,
        'endpoint_timeout': 1,
        'endpoint_long_timeout': 3,
        'max_approvers': 1,
        'is_broken': True,
    }

    simple_system.is_sox = True
    simple_system.node_plugin_type = SYSTEM_NODE_PLUGIN_TYPE.YAML
    simple_system.save()

    origin_sox = sox_data.copy()
    data.update(extended_data)
    sox_data.update(data)
    patch_data = sox_data
    response = client.json.patch(url, patch_data)
    assert response.status_code == 403
    add_perms_by_role(role, frodo, simple_system)
    response = client.json.patch(url, patch_data)
    assert response.status_code == 200
    system = System.objects.get(slug='simple')
    system.fetch_service()

    for field in origin_sox:
        if field in ['name', 'description']:
            assert getattr(system, field) == origin_sox.get(field, '')['ru']
            assert getattr(system, field + '_en') == origin_sox.get(field, '')['en']
        elif field == 'sync_interval':
            assert getattr(system, field) == datetime.timedelta(0, int(origin_sox.get(field)))
        elif field == 'emails':
            assert getattr(system, field) == ','.join(origin_sox.get(field))
        elif field == 'service':
            assert getattr(system, field).external_id == origin_sox.get(field)['id']
        elif field == 'plugin_type' and not system.is_active:
            assert getattr(system, field) == SYSTEM_PLUGIN_TYPE.DUMB  # При выключении системы меняем ей плагин
        else:
            assert getattr(system, field) == origin_sox.get(field)
    for field in data:
        if role == 'superuser':
            assert getattr(system, field) == data.get(field)
        else:
            assert getattr(system, field) == getattr(simple_system, field)

    # patch_data['tvm_id'] = ''
    # response = client.json.patch(url, patch_data)
    # assert response.status_code == 400
    # assert response.json()['message'] == 'TVM id can not be empty'

    patch_data['auth_factor'] = SYSTEM_AUTH_FACTOR.CERT
    patch_data['check_certificate'] = False
    response = client.json.patch(url, patch_data)
    assert response.status_code == 200
    system.refresh_from_db()
    if role in {'superuser', 'developer'}:
        assert system.auth_factor == SYSTEM_AUTH_FACTOR.CERT
    else:
        # patch не поменял auth_factor, так как у client нет прав
        assert system.auth_factor == SYSTEM_AUTH_FACTOR.TVM
    assert not system.check_certificate

    patch_data['name'] = {'ru': '', 'en': ''}
    response = client.json.patch(url, patch_data)
    assert response.status_code == 400
    assert response.json()['message'] == 'Name can not be empty'

    system.auth_factor = SYSTEM_AUTH_FACTOR.TVM
    system.save()
    response = client.json.patch(url, {
        'base_url': urljoin(system.base_url, 'idm')
    })
    assert response.status_code == 200, response.json()

    system.auth_factor = SYSTEM_AUTH_FACTOR.CERT
    system.save()
    response = client.json.patch(url, {
        'base_url': urljoin(system.base_url, '/idm')
    })
    assert response.status_code == 400
    assert response.json()['message'] == \
           'base url нельзя редактировать, если методом аутентификации выбран клиентский сертификат'

    privileged_system_plugin_choice = (
        next(iter(set(SYSTEM_PLUGIN_TYPE.CHOICES) - set(SYSTEM_PLUGIN_TYPE.UNPRIVILEGED_CHOICES)))
    )[0]

    if role in {'superuser', 'developer'}:
        system.auth_factor = SYSTEM_AUTH_FACTOR.TVM
        system.save()
        response = client.json.patch(url, {
            'base_url': urljoin(system.base_url, 'idm'),
            'auth_factor': SYSTEM_AUTH_FACTOR.CERT
        })
        assert response.status_code == 400
        assert response.json()[
                   'message'] == 'base url нельзя редактировать, если методом аутентификации выбран клиентский сертификат'

        system.auth_factor = SYSTEM_AUTH_FACTOR.CERT
        system.save()
        response = client.json.patch(url, {
            'base_url': urljoin(system.base_url, '/idm'),
            'auth_factor': SYSTEM_AUTH_FACTOR.TVM
        })
        assert response.status_code == 200

        response = client.json.patch(url, {'plugin_type': privileged_system_plugin_choice})
        assert response.status_code == 200
    else:
        response = client.json.patch(url, {'plugin_type': privileged_system_plugin_choice})
        assert response.status_code == 400
        assert response.json()['errors']['plugin_type'][0] == 'Недостаточно прав для выбора этой системы подключения'


@pytest.mark.parametrize('role', ['responsible', 'users_view', 'superuser'])
def test_patch_system_and_set_sox(client, arda_users, simple_system, role):
    """
    PATCH /frontend/systems/
    """
    frodo = arda_users.frodo
    client.login('frodo')
    service = Service.objects.create(external_id=123)
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug='simple')

    valid_data = {
        'is_sox': True,
        'has_review': True,
        'review_on_relocate_policy': SYSTEM_REVIEW_ON_RELOCATE_POLICY.REVIEW,
        'inconsistency_policy': SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX,
        'workflow_approve_policy': SYSTEM_WORKFLOW_APPROVE_POLICY.ANOTHER,
    }
    invalid_data = {
        'has_review': False,
        'review_on_relocate_policy': SYSTEM_REVIEW_ON_RELOCATE_POLICY.IGNORE,
        'inconsistency_policy': SYSTEM_INCONSISTENCY_POLICY.TRUST,
        'workflow_approve_policy': SYSTEM_WORKFLOW_APPROVE_POLICY.ANY,
    }

    add_perms_by_role(role, frodo, simple_system)

    for key in invalid_data.keys():
        data = valid_data.copy()
        data[key] = invalid_data[key]

        # Шлём не-sox значения
        response = client.json.patch(url, data)
        assert response.status_code == 400

        # Нужных значений вообще не шлём
        del data[key]
        response = client.json.patch(url, data)
        assert response.status_code == 400

    # Шлём корректные sox значения
    response = client.json.patch(url, valid_data)
    assert response.status_code == 200

    simple_system.refresh_from_db()
    for field, value in valid_data.items():
        assert getattr(simple_system, field) == value


@pytest.mark.parametrize('role', ['responsible', 'users_view', 'superuser'])
def test_patch_not_sox_system(client, simple_system, role):
    """
    PATCH /frontend/systems/
    """
    user = create_user()
    client.login(user)
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug=simple_system.slug)
    data = {
        'name': {'ru': 'Простая обновленная', 'en': 'Simple updated'},
        'description': {'ru': 'Обновленное описание', 'en': 'Updated desription'},
        'group_policy': SYSTEM_GROUP_POLICY.AWARE,
        'passport_policy': SYSTEM_PASSPORT_POLICY.UNIQUE_FOR_USER,
        'request_policy': SYSTEM_REQUEST_POLICY.SUBORDINATES,
        'role_grant_policy': SYSTEM_ROLE_GRANT_POLICY.SYSTEM,
        'roletree_policy': SYSTEM_ROLETREE_POLICY.NONEDITABLE,
        # 'auth_factor': SYSTEM_AUTH_FACTOR.TVM,
        # 'tvm_id': '123',
        'audit_method': SYSTEM_AUDIT_METHOD.GET_ROLES,
        'plugin_type': SYSTEM_PLUGIN_TYPE.GENERIC,
        'use_tvm_role': True,
        'emails': ['simple@yandex-team.ru', 'simplesystem@yandex-team.ru', 'polosate@yandex-team.com'],
        'is_sox': False,
        'has_review': False,
        'review_on_relocate_policy': SYSTEM_REVIEW_ON_RELOCATE_POLICY.IGNORE,
        'inconsistency_policy': SYSTEM_INCONSISTENCY_POLICY.TRUST,
        'workflow_approve_policy': SYSTEM_WORKFLOW_APPROVE_POLICY.ANOTHER,
        'sync_interval': '10',
        'rolefields': 'login',
        'is_broken': True,
    }
    extended_data = {
        'can_be_broken': True,
        'roles_review_days': 1,
        'endpoint_timeout': 1,
        'endpoint_long_timeout': 3,
        'max_approvers': 1,
    }

    origin_data = data.copy()
    data.update(extended_data)
    patch_data = data
    response = client.json.patch(url, patch_data)
    assert response.status_code == 403
    assert response.json()['message'] == 'Вы не можете редактировать данную систему'

    add_perms_by_role(role, user, simple_system)
    response = client.json.patch(url, patch_data)
    assert response.status_code == 200

    system = System.objects.get(slug=simple_system.slug)

    for field in origin_data:
        if field in ['name', 'description']:
            assert getattr(system, field) == origin_data.get(field, '')['ru']
            assert getattr(system, field + '_en') == origin_data.get(field, '')['en']
        elif field == 'sync_interval':
            assert getattr(system, field) == datetime.timedelta(0, int(origin_data.get(field)))
        elif field == 'emails':
            assert getattr(system, field) == ','.join(origin_data.get(field))
        elif field == 'plugin_type' and not system.is_active:
            assert getattr(system, field) == SYSTEM_PLUGIN_TYPE.DUMB  # При выключении системы меняем ей плагин
        elif field == 'rolefields':
            rolefield = system.systemrolefields.get(slug='login')
            assert rolefield.name == 'Доп поле'
            assert rolefield.name_en == 'Доп поле'
            assert rolefield.state == FIELD_STATE.CREATED
            assert rolefield.type == FIELD_TYPE.CHARFIELD
        else:
            assert getattr(system, field) == origin_data.get(field)

    for field in extended_data:
        if role == 'superuser':
            assert getattr(system, field) == extended_data.get(field)
        else:
            assert getattr(system, field) == getattr(simple_system, field)

    # patch_data['tvm_id'] = ''
    # response = client.json.patch(url, patch_data)
    # assert response.status_code == 400
    # assert response.json()['message'] == 'TVM id can not be empty'

    patch_data['auth_factor'] = SYSTEM_AUTH_FACTOR.CERT
    patch_data['check_certificate'] = False
    response = client.json.patch(url, patch_data)
    assert response.status_code == 200
    system.refresh_from_db()
    if role in {'superuser', 'developer'}:
        assert system.auth_factor == SYSTEM_AUTH_FACTOR.CERT
    else:
        # patch не поменял auth_factor, так как у client нет прав
        assert system.auth_factor == SYSTEM_AUTH_FACTOR.TVM
    assert not system.check_certificate

    patch_data['name'] = {'ru': '', 'en': ''}
    response = client.json.patch(url, patch_data)
    assert response.status_code == 400
    assert response.json()['message'] == 'Name can not be empty'

    system.auth_factor = SYSTEM_AUTH_FACTOR.TVM
    system.save()
    response = client.json.patch(url, {
        'base_url': urljoin(system.base_url, 'idm')
    })
    assert response.status_code == 200


@pytest.mark.parametrize('role', ['responsible', 'users_view', None])
@pytest.mark.parametrize('is_superuser', [True, False])
def test_patch_disable_system(client, arda_users, simple_system, role, is_superuser):
    """
    PATCH /frontend/systems/ disable
    """
    frodo = arda_users.frodo
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug='simple')
    patch_data = {'is_active': False}
    if is_superuser:
        add_perms_by_role('superuser', frodo)
    if role:
        add_perms_by_role(role, frodo, simple_system)
    response = client.json.patch(url, patch_data)
    if is_superuser:
        assert response.status_code == 200
    else:
        assert response.status_code == 403
        if role:
            message = (
                'Выключить систему может только суперпользователь, '
                'обратитесь в поддержку IDM'
            )
        else:
            message = 'Вы не можете редактировать данную систему'
        assert response.json()['message'] == message


@pytest.mark.parametrize('role', ['responsible', 'users_view', 'superuser'])
def test_patch_tvm_field(client, arda_users, generic_system_with_tvm, role):
    frodo = arda_users.frodo
    tvm_app = arda_users.tvm_app
    tvm_group = Group.objects.create(type=GROUP_TYPES.TVM_SERVICE, name='TVM Control')
    GroupMembership.objects.create(user=tvm_app, group=tvm_group, is_direct=True,
                                   state=GROUPMEMBERSHIP_STATE.ACTIVE, )

    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems',
                  slug=generic_system_with_tvm.slug)

    add_perms_by_role(role, frodo, generic_system_with_tvm if role not in {'superuser', 'developer'} else None)

    def assert_tvm_field_error(response):
        assert response.status_code == 400
        assert set(response.json()['errors'].keys()) == {'tvm_id'}
        assert response.json()['errors']['tvm_id'] == [
            'Можно указать только TVM-приложения, для которых вы являетесь управляющим в ABC']

    client.login('frodo')
    if role in {'superuser', 'developer'}:
        response = client.json.patch(url, {'tvm_id': '12345'})
        generic_system_with_tvm.refresh_from_db()
        assert_tvm_field_error(response)

        response = client.json.patch(url, {'tvm_id': 344124125})
        generic_system_with_tvm.refresh_from_db()
        assert_tvm_field_error(response)

        response = client.json.patch(url, {'tvm_id': tvm_app.username})
        generic_system_with_tvm.refresh_from_db()
        assert response.status_code == 200
        assert generic_system_with_tvm.tvm_id == tvm_app.username
    elif role == 'responsible':
        GroupResponsibility.objects.create(user=frodo, group=tvm_group, is_active=True, rank='manager')

        old_tvm_id = generic_system_with_tvm.tvm_id
        response = client.json.patch(url, {'tvm_id': ''})
        generic_system_with_tvm.refresh_from_db()
        assert_tvm_field_error(response)
        assert generic_system_with_tvm.tvm_id == old_tvm_id

        response = client.json.patch(url, {'tvm_id': '12345'})
        assert_tvm_field_error(response)

        response = client.json.patch(url, {'tvm_id': tvm_app.username})
        generic_system_with_tvm.refresh_from_db()
        assert response.status_code == 200
        assert generic_system_with_tvm.tvm_id == tvm_app.username
    else:
        old_tvm_id = generic_system_with_tvm.tvm_id
        response = client.json.patch(url, {'tvm_id': ''})
        generic_system_with_tvm.refresh_from_db()
        assert_tvm_field_error(response)
        assert generic_system_with_tvm.tvm_id == old_tvm_id

        response = client.json.patch(url, {'tvm_id': old_tvm_id})
        assert response.status_code == 200

        response = client.json.patch(url, {'tvm_id': '12345'})
        assert_tvm_field_error(response)

        response = client.json.patch(url, {'tvm_id': tvm_app.username})
        assert_tvm_field_error(response)


@pytest.mark.parametrize('role', ['responsible', 'users_view'])
def test_patch_system_fields(client, arda_users, simple_system, role):
    """
    PATCH /frontend/systems/
    """
    frodo = arda_users.frodo
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug='simple')
    patch_data = {
        'rolefields': 'login,passport-login',
    }

    simple_system.systemrolefields.create(
        slug='login', name='Доп поле', name_en='Additional field', state=FIELD_STATE.DEPRIVED
    )

    response = client.json.patch(url, patch_data)
    assert response.status_code == 403
    add_perms_by_role(role, frodo, simple_system)
    response = client.json.patch(url, patch_data)
    assert response.status_code == 200

    system = System.objects.get(slug='simple')
    rolefields = system.systemrolefields.filter(slug__in=['login', 'passport-login']).order_by('slug')

    assert rolefields[0].name == 'Доп поле'
    assert rolefields[0].name_en == 'Additional field'
    assert rolefields[0].state == FIELD_STATE.CREATED
    assert rolefields[0].type == FIELD_TYPE.CHARFIELD

    assert rolefields[1].name == 'Паспортный логин'
    assert rolefields[1].name_en == 'Паспортный логин'
    assert rolefields[1].state == FIELD_STATE.CREATED
    assert rolefields[1].type == FIELD_TYPE.PASSPORT_LOGIN

    badfield = 'doesnotexistinroles'
    patch_data['rolefields'] = badfield
    response = client.json.patch(url, patch_data)
    assert response.status_code == 400
    assert response.json()['message'] == ('Поля %s нет среди полей в ролях для данной '
                                          'системы' % badfield)

    patch_data = {'rolefields': ''}
    response = client.json.patch(url, patch_data)
    assert response.status_code == 200
    rolefields = system.systemrolefields.filter(slug__in=['login', 'passport-login'])
    assert rolefields[0].state == FIELD_STATE.DEPRIVING
    assert rolefields[1].state == FIELD_STATE.DEPRIVING


def test_responsibles(client, simple_system, users_for_test):
    """
    GET /frontend/systems/simple/
    """
    client.login('art')

    (art, fantom, terran, admin) = users_for_test
    add_perms_by_role('responsible', fantom, simple_system)
    add_perms_by_role('users_view', terran, simple_system)

    data = client.json.get(reverse('api_dispatch_detail', api_name='frontend',
                                   resource_name='systems', slug='simple')).json()
    assert data['slug'] == 'simple'
    assert 'responsibles' not in data
    assert 'team' not in data


@pytest.mark.parametrize('api_name', API_NAMES)
def test_repair_broken_system(client, generic_system, users_for_test, api_name):
    """
    POST /frontend/systems/simple/
    """
    art = users_for_test[0]

    generic_system = refresh(generic_system)
    generic_system.is_broken = True
    generic_system.save()

    client.login('art')

    response = client.json.post(
        reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug),
        data={'operation': 'recover'},
    )
    assert response.status_code == 403

    add_perms_by_role('superuser', art)
    response = client.json.post(
        reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug),
        data={'operation': 'recover'},
    )

    assert response.status_code == 204
    assert refresh(generic_system).is_broken is False
    recover_action = generic_system.actions.get(action='system_marked_recovered')
    recover_action.fetch_requester()
    assert recover_action.requester.username == 'art'


def test_get_favorite_system(client, simple_system, users_for_test):
    """
    GET /frontend/systems/simple/
    """
    art = users_for_test[0]
    client.login('art')

    data = client.json.get(reverse('api_dispatch_detail', api_name='frontend',
                                   resource_name='systems', slug='simple')).json()
    assert data['slug'] == 'simple'
    assert data['is_favorite'] is False

    FavoriteSystem.objects.create(system=simple_system, user=art)

    data = client.json.get(reverse('api_dispatch_detail', api_name='frontend',
                                   resource_name='systems', slug='simple')).json()
    assert data['slug'] == 'simple'
    assert data['is_favorite'] is True


def test_get_permissions_by_systems(client, simple_system, arda_users, settings):
    frodo = arda_users['frodo']
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='systems')

    client.login('frodo')
    data = client.json.get(url).json()

    assert data['objects'][0]['permissions'] == []
    add_perms_by_role('developer', frodo, simple_system)
    data = client.json.get(url).json()
    assert set(data['objects'][0]['permissions']) == set(settings.IDM_COMMON_ROLES_PERMISSIONS['developer'])
    add_perms_by_role('tree_edit', frodo, simple_system)
    data = client.json.get(url).json()
    assert set(data['objects'][0]['permissions']) == (
        set(settings.IDM_SYSTEM_ROLES_PERMISSIONS['tree_edit'] + settings.IDM_COMMON_ROLES_PERMISSIONS['developer'])
    )


@pytest.mark.parametrize('api_name', API_NAMES)
def test_sync_system_nodes(client, generic_system, simple_system, arda_users, api_name):
    """
    POST /frontend/systems/simple/ dry_run=True
    TestpalmID: 3456788-202
    """
    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug)
    add_perms_by_role('responsible', arda_users.gandalf, generic_system)
    client.login('gandalf')
    raw_make_role(arda_users.frodo, generic_system, {'role': 'poweruser'})

    tree = simple_system.plugin.get_info()
    tree['roles']['values']['new_role'] = 'Новая Роль'
    del tree['roles']['values']['poweruser']

    expected_queue = [
        'Добавленные в систему, но пока отсутствующие в БД узлы дерева ролей: (1)',
        'Роль: Новая Роль',
        'Удаленные из системы, но пока существующие в БД узлы дерева ролей: (1)',
        'Роль: Могучий Пользователь (1 ролей к отзыву)',
    ]

    with mock_tree(generic_system, tree):
        with models_didnt_change():
            data = client.json.post(system_url, {'operation': 'sync_nodes', 'dry_run': True}).json()

    assert_contains(expected_queue, data['queue'])
    assert 'Content-Type' not in data['queue']
    assert RoleNode.objects.filter(system=generic_system, slug='new_role').exists() is False

    with mock_tree(generic_system, tree):
        response = client.json.post(system_url, {'operation': 'sync_nodes'})

    assert response.status_code == 200
    assert RoleNode.objects.filter(system=generic_system, slug='new_role').exists() is True

    delete_action_count = Action.objects.filter(action='role_node_deleted').count()
    assert delete_action_count == 0

    create_action = Action.objects.get(action='role_node_created')
    assert create_action.data['from_api'] is False

    with mock_tree(generic_system, tree):
        with models_didnt_change():
            data = client.json.post(system_url, {'operation': 'sync_nodes', 'dry_run': True}).json()

    assert 'Сохраненные и свежеполученные узлы дерева ролей системы совпадают.' in data['queue']


@pytest.mark.parametrize('api_name', API_NAMES)
def test_queue_for_changed_nodes(client, complex_system, arda_users, api_name):
    """
    POST /{api_name}/systems/complex/ dry_run=True
    """
    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=complex_system.slug)
    add_perms_by_role('responsible', arda_users.gandalf, complex_system)

    client.login('gandalf')
    tree = complex_system.plugin.get_info()
    complex_system.plugin_type = 'generic'
    complex_system.save()
    complex_system = refresh(complex_system)
    tree['roles']['values']['rules']['roles']['values']['auditor']['name'] = {'ru': 'Аудитор', 'en': 'Auditor'}
    tree['roles']['values']['rules']['roles']['values']['auditor']['firewall-declaration'] = 'another-one'
    tree['roles']['values']['subs']['roles']['values']['developer'] = {
        'name': 'Разработчик',
        'responsibilities': [{
            'username': 'frodo',
            'notify': False,
        }]
    }
    del tree['fields'][-1]
    tree['fields'][0]['options'] = {'custom': True}
    tree['fields'].append({
        'slug': 'url',
        'name': 'URL',
        'required': True,
        'type': 'charfield',
    })

    expected_queue = [
        'Измененные узлы дерева ролей: (3)',
        'Проект',
        'Добавленные поля узла:',
        'URL (Slug:url, Тип: charfield)',
        'Удалённые поля узла:',
        'Паспортный логин (Slug: passport-login, Тип: passportlogin)',
        'Изменённые поля узла:',
        'options: None ⇾ {&#39;custom&#39;: True}',
        'Проект: IDM, Роль: Аудитор',
        'Изменения свойств узла:',
        'name_en: Аудитор ⇾ Auditor',
        'Добавленные синонимы узла:',
        'another-one (firewall)',
        'Удалённые синонимы узла:',
        'test-rules-auditor (firewall)',
        'Проект: Подписки, Роль: Разработчик',
        'Добавленные ответственности:',
        'frodo (notify: нет)',
    ]

    with mock_tree(complex_system, tree):
        with models_didnt_change():
            data = client.json.post(system_url, {'operation': 'sync_nodes', 'dry_run': True}).json()

    assert_contains(expected_queue, data['queue'])


def test_queue_addition(client, arda_users):
    """
    POST /{api_name}/systems/complex/ dry_run=True
    """
    complex_system = create_system(
        'complex',
        'idm.tests.base.ComplexPlugin',
        name='Complex система',
        name_en='Complex system',
        auth_factor='cert',
        sync_role_tree=False,
    )
    complex_plugin = complex_system.plugin
    complex_system.plugin_type = 'generic'
    complex_system.save()
    complex_system = refresh(complex_system)
    system_url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug=complex_system.slug)
    add_perms_by_role('responsible', arda_users.gandalf, complex_system)

    client.login('gandalf')
    expected_queue = [
        'Добавленные в систему, но пока отсутствующие в БД узлы дерева ролей: (10)',
        'Проект: Подписки, роль: Разработчик',
        'Проект: Подписки, роль: Менеджер',
        'Проект: IDM, роль: невидимка',
    ]

    with mock_tree(complex_system, complex_plugin.get_info()):
        with models_didnt_change():
            data = client.json.post(system_url, {'operation': 'sync_nodes', 'dry_run': True}).json()

    assert_contains(expected_queue, data['queue'])


def test_sync_nodes_dry_run_unique_id(client, arda_users, complex_system, monkeypatch):
    gandalf = arda_users.gandalf
    client.login('gandalf')
    system_url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug=complex_system.slug)
    add_perms_by_role('responsible', arda_users.gandalf, complex_system)

    tree = {
        'roles': {
            'slug': 'project',
            'values': {
                'subs': {
                    'roles': {
                        'slug': 'role',
                        'values': {
                            'developer': 'Разработчик',
                            'manager': {
                                'name': 'Уникальный менеджер',
                                'unique_id': 'unique_manager',
                            }
                        },
                    },
                },
                'rules': {
                    'roles': {
                        'slug': 'role',
                        'values': {
                            'admin': 'Админ',
                        },
                    },
                },
            },
        },
    }

    with mock_tree(complex_system, tree):
        complex_system.synchronize(force_update=True)

    role = Role.objects.request_role(gandalf, gandalf, complex_system, '', {'project': 'subs', 'role': 'manager'})
    role.refresh_from_db()
    assert role.state == ROLE_STATE.GRANTED

    modified_tree = tree.copy()
    modified_tree['roles']['values']['rules']['roles']['values']['manager'] = {
        'unique_id': 'unique_manager',
        'name': 'Тот же уникальный менеджер'
    }
    del modified_tree['roles']['values']['subs']['roles']['values']['manager']

    complex_system.base_url = '%s'
    complex_system._plugin = None
    complex_system.plugin_type = SYSTEM_PLUGIN_TYPE.GENERIC
    complex_system.save()
    with mock_tree(complex_system, modified_tree):
        data = client.json.post(system_url, {'operation': 'sync_nodes', 'dry_run': True}).json()

    assert 'Не будет отозвано ни одной роли в связи с удалением узлов' in data['queue']


@pytest.mark.parametrize('api_name', API_NAMES)
def test_sync_system_nodes_errors(client, generic_system, users_for_test, api_name):
    """
    GET /frontend/systems/simple/
    """
    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug)

    client.login('art')
    response = client.json.post(system_url, {'operation': 'sync_nodes'})
    assert response.status_code == 403
    assert response.json() == {
        'error_code': 'FORBIDDEN',
        'message': 'Вы не можете синхронизировать дерево ролей в данной системе',
    }

    client.login('admin')
    with patch.object(generic_system.plugin.__class__, 'get_info') as get_info:
        get_info.side_effect = ValueError
        response = client.json.post(system_url, {'operation': 'sync_nodes', 'dry_run': True})

    assert response.status_code == 200
    assert response.json() == {
        'queue': 'Система не отвечает или отвечает как-то не так',
    }

    generic_system.is_broken = True
    generic_system.save()

    response = client.json.post(system_url, {'operation': 'sync_nodes'})
    assert response.status_code == 403
    assert response.json() == {
        'error_code': 'FORBIDDEN',
        'message': 'Система сломана, операции невозможны',
    }

    generic_system.is_broken = False
    generic_system.save()

    response = client.json.post(system_url)
    assert response.status_code == 400
    assert response.json()['errors']['operation'] == ['Обязательное поле.']


@pytest.mark.parametrize('api_name', API_NAMES)
def test_sync_system_roles(client, generic_system, arda_users, api_name, superuser_gandalf):
    """
    GET /frontend/systems/simple/
    """
    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug)
    client.login('gandalf')
    Action.objects.all().delete()

    with mock_all_roles(generic_system, []):
        response = client.json.post(system_url, {'operation': 'sync_roles'})
    assert response.status_code == 200

    assert Action.objects.count() == 4
    actions = set(Action.objects.values_list('action', flat=True))
    # случились и сверка, и разрешение
    assert actions == {
        'started_comparison_with_system',
        'compared_with_system',
        'started_sync_with_system',
        'synced_with_system',
    }
    # писем нет
    assert len(mail.outbox) == 0


@pytest.mark.parametrize('api_name', API_NAMES)
@pytest.mark.parametrize(
    'system_max,all_max,possible',
    (
            [3, 100, False],
            [10, 1, True],
            [None, 100, True],
            [None, 3, False],
    )
)
def test_sync_system_roles_check_count(
        client, generic_system, arda_users, api_name,
        superuser_gandalf, system_max, all_max, possible
):
    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug)
    client.login('gandalf')

    for _ in range(7):
        Inconsistency.objects.create(system=generic_system, state='active')
        Inconsistency.objects.create(system=generic_system, state='resolved')

    with mock_all_roles(generic_system, []):
        with override_config(
                SYSTEM_INCONSISTENCY_MAX=all_max,
                SYSTEM_INCONSISTENCY_EXCEPT='{{"test": {}}}'.format(system_max) if system_max else '{}',
        ):
            response = client.json.post(system_url, {'operation': 'sync_roles'})
    assert response.status_code == 200 if possible else 403


@pytest.mark.parametrize('api_name', API_NAMES)
def test_sync_system_roles_in_idm_direct(client, generic_system, arda_users, api_name, superuser_gandalf):
    """
    GET /frontend/systems/simple/
    """
    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug)
    client.login('gandalf')
    role = raw_make_role(arda_users.frodo, generic_system, {'role': 'admin'}, state='granted')
    Action.objects.all().delete()
    clear_mailbox()

    with mock_all_roles(generic_system, []):
        with capture_http(generic_system, {'code': 0, 'data': {}}) as mocked:
            response = client.json.post(system_url, {'operation': 'sync_roles', 'resolve_in_idm_direct': True})
    assert response.status_code == 200

    data = {
        'login': 'frodo',
        'role': '{"role": "admin"}',
        'fields': 'null',
        'path': '/role/admin/'
    }
    assert_http(mocked.http_post, url='http://example.com/add-role/', data=data)

    role = refresh(role)
    assert role.is_active
    assert role.state == 'granted'

    actions = set(Action.objects.values_list('action', flat=True))
    # случились и сверка, и разрешение
    assert actions.issuperset({
        'started_comparison_with_system',
        'compared_with_system',
        'started_sync_with_system',
        'synced_with_system',
    })
    # писем нет
    assert len(mail.outbox) == 0


@pytest.mark.parametrize('api_name', API_NAMES)
def test_check_system_roles(client, generic_system, arda_users, api_name, superuser_gandalf):
    """
    GET /frontend/systems/simple/
    """
    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug)
    client.login('gandalf')
    Action.objects.all().delete()

    with mock_all_roles(generic_system, []):
        response = client.json.post(system_url, {'operation': 'sync_roles', 'dry_run': True})
    assert response.status_code == 200

    assert Action.objects.count() == 2
    actions = set(Action.objects.values_list('action', flat=True))
    # сверка случилась, а разрешение – нет
    assert actions == {
        'started_comparison_with_system',
        'compared_with_system',
    }
    # писем нет
    assert len(mail.outbox) == 0


@pytest.mark.parametrize('api_name', API_NAMES)
def test_sync_system_memberships(client, generic_system, arda_users, api_name, superuser_gandalf):
    "Проверяем запус синка/пуша системочленств через ручку"
    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug)
    client.login('gandalf')
    Action.objects.all().delete()

    with mock_all_roles(generic_system, []):
        response = client.json.post(system_url, {'operation': 'sync_memberships'})
    assert response.status_code == 400
    assert response.json()['message'] == 'Система не поддерживает работу с членствами в группах'
    assert Action.objects.count() == 0

    generic_system.group_policy = 'aware_of_memberships_without_logins'
    generic_system.save()

    with mock_all_roles(generic_system, []):
        response = client.json.post(system_url, {'operation': 'sync_memberships'})
    assert response.status_code == 200

    assert Action.objects.count() == 4
    actions = set(Action.objects.values_list('action', flat=True))
    # случились и сверка, и разрешение
    assert actions == {
        'started_memberships_sync_with_system',
        'finished_memberships_sync_with_system',
        'started_memberships_push_to_system',
        'finished_memberships_push_to_system',
    }
    # писем нет
    assert len(mail.outbox) == 0


@pytest.mark.parametrize('api_name', API_NAMES)
@pytest.mark.parametrize('auth_factor', ('cert', 'tvm'))
@pytest.mark.parametrize('is_broken', (True, False))
@pytest.mark.parametrize('plugin_type', ('generic', 'generic_legacy'))
def test_system_handles(client, generic_system_with_tvm, users_for_test, monkeypatch, api_name, is_broken, plugin_type,
                        auth_factor, ):
    """
    POST /frontend/systems/simple/
    """
    monkeypatch.setattr('idm.utils.tvm.get_tvm_ticket', lambda x: '1')
    request_params = {}
    generic_system_with_tvm.is_broken = is_broken
    generic_system_with_tvm.plugin_type = plugin_type
    generic_system_with_tvm.save()

    def request(*args, **kwargs):
        if auth_factor == 'cert':
            request_params['cert'] = kwargs.get('cert')
        return Response(200, '{"code": 0}', headers='Requests headers')

    monkeypatch.setattr('requests.sessions.Session.request', request)

    system_url = reverse(
        'api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system_with_tvm.slug
    )
    client.login('admin')

    response = client.json.post(
        system_url,
        {'operation': 'pull_handle', 'options': {'handle': 'info', 'library': 'default', 'auth_factor': auth_factor}}
    )
    assert response.status_code == 200
    data = response.json()
    assert data['auth_factor'] == auth_factor
    assert data['library'].startswith('http.py')
    assert data['url'] == 'http://example.com/info/'
    assert data['headers'] == 'Requests headers'
    assert data['text'] == '{\n  "code": 0\n}'
    assert data['response'] == '<Response [200]>'
    if auth_factor == 'cert':
        assert request_params['cert'] == 'yandex_internal_cert'

    def curl(*args, **kwargs):
        request_params['use_client_certificate'] = kwargs.get('use_client_certificate')
        return Response(200, '{"code": 0}', headers='Curl headers')

    monkeypatch.setattr('idm.utils.curl._perform', curl)

    data = client.json.post(
        system_url,
        {'operation': 'pull_handle', 'options': {'handle': 'get-roles', 'library': 'curl', 'auth_factor': 'no'}}
    ).json()
    assert data['auth_factor'] == 'no'
    assert data['library'].startswith('curl.py')
    assert data['url'] == 'http://example.com/get-roles/'
    assert data['headers'] == 'Curl headers'
    assert data['text'] == '{\n  "code": 0\n}'
    assert data['response'] == '<Response [200]>'
    assert request_params['use_client_certificate'] is False


@pytest.mark.parametrize('api_name', API_NAMES)
def test_pull_handle__base_url_not_set(client, generic_system, api_name: str):
    client.login(create_user(superuser=True))
    system = create_system(plugin_type=SYSTEM_PLUGIN_TYPE.GENERIC, sync_role_tree=False)
    response = client.json.post(
        reverse(
            'api_dispatch_detail', api_name=api_name, resource_name='systems', slug=system.slug
        ),
        {
            'operation': 'pull_handle',
            'options': {'handle': 'info', 'library': 'default', 'auth_factor': SYSTEM_AUTH_FACTOR.NO},
        },
    )
    assert response.status_code == 400, response.json()
    assert response.json()['message'] == 'Не установлен адрес ручки в параметрах системы'


@pytest.mark.parametrize('api_name', API_NAMES)
def test_pull_handle__role_tree_url_not_set(client, api_name: str):
    client.login(create_user(superuser=True))
    system = create_system(
        node_plugin_type=SYSTEM_NODE_PLUGIN_TYPE.YAML,
        auth_factor=SYSTEM_AUTH_FACTOR.NO,
        sync_role_tree=False,
    )
    response = client.json.post(
        reverse(
            'api_dispatch_detail', api_name=api_name, resource_name='systems', slug=system.slug
        ),
        {
            'operation': 'sync_nodes',
            'dry_run': True,
            'options': {'handle': 'info', 'library': 'default', 'auth_factor': SYSTEM_AUTH_FACTOR.NO}
        },
    )
    assert response.status_code == 400, response.json()
    assert response.json()['message'] == 'Не установлена ссылка на дерево в параметрах системы'


def test_no_tvm_id(client, generic_system, users_for_test):
    client.login('admin')
    generic_system.auth_factor = 'tvm'
    generic_system.save()
    system_url = reverse(
        'api_dispatch_detail', api_name='frontend', resource_name='systems', slug=generic_system.slug
    )
    response = client.json.post(
        system_url,
        {'operation': 'pull_handle', 'options': {'handle': 'info', 'library': 'default', 'auth_factor': 'tvm'}}
    )
    assert response.status_code == 400
    assert response.json()['message'] == 'У данной системы не указан ID TVM-приложения'


@pytest.mark.parametrize('api_name', API_NAMES)
def test_system_handles_connection_error(client, generic_system, users_for_test, monkeypatch, api_name):
    """
    POST /frontend/systems/simple/
    """
    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug)
    client.login('admin')

    with patch('requests.sessions.Session.request') as request:
        request.side_effect = ConnectionError('connect() timed out!')
        data = client.json.post(system_url, {
            'operation': 'pull_handle',
            'options': {
                'handle': 'info',
                'library': 'default',
                'auth_factor': 'cert'
            }
        }).json()

    assert data['auth_factor'] == 'cert'
    assert data['library'].startswith('http.py')
    assert data['url'] == 'http://example.com/info/'
    assert data['headers'] == '(Нет)'
    assert data['text'] == 'connect() timed out!'
    assert data['response'] == '<Connection error>'


@pytest.mark.parametrize('api_name', API_NAMES)
def test_system_handles_errors(client, generic_system, users_for_test, monkeypatch, api_name):
    """
    POST /frontend/systems/simple/
    """

    def request(*args, **kwargs):
        return Response(200, '{"code": 0}', headers='Requests headers')

    monkeypatch.setattr('requests.sessions.Session.request', request)

    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=generic_system.slug)
    client.login('admin')

    data = client.json.post(system_url, {'operation': 'pull_handle'}).json()
    assert data['message'] == 'Invalid data sent'

    data = client.json.post(
        system_url,
        {'operation': 'pull_handle', 'options': {}}
    ).json()
    assert data['errors']['handle'] == ['Обязательное поле.']
    assert data['errors']['library'] == ['Обязательное поле.']
    assert data['errors']['auth_factor'] == ['Обязательное поле.']

    data = client.json.post(
        system_url,
        {'operation': 'pull_handle', 'options': {'handle': 'xxx'}}
    ).json()
    assert data['errors']['handle'] == ['Выберите корректный вариант. xxx нет среди допустимых значений.']


@pytest.mark.parametrize('api_name', API_NAMES)
def test_system_handles_plugin_error(client, dumb_system, users_for_test, api_name):
    """
    POST /frontend/systems/simple/
    """
    system_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='systems', slug=dumb_system.slug)
    client.login('admin')

    data = client.json.post(system_url, {'operation': 'pull_handle'}).json()
    assert data['message'] == 'Доступны только системы с плагином generic'


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
@pytest.mark.parametrize('role', ['creator', 'responsible', 'users_view'])
def test_get_system_additional_fields(client, simple_system, arda_users, api_name, role):
    """
    GET /frontend/systems/
    """
    frodo = arda_users['frodo']

    system_fields = {
        'description',
        'endpoint_timeout',
        'endpoint_long_timeout',
        'group_policy',
        'is_active',
        'is_broken',
        'is_favorite',
        'is_sox',
        'name',
        'service',
        'slug',
        'state',
        'use_mini_form',
        'emails',
        'rolefields',
        'use_webauth',
    }

    system_additional_fields = {
        'can_be_broken',
        'has_review',
        'passport_policy',
        'request_policy',
        'role_grant_policy',
        'roletree_policy',
        'review_on_relocate_policy',
        'inconsistency_policy',
        'workflow_approve_policy',
        'auth_factor',
        'tvm_id',
        'check_certificate',
        'base_url',
        'plugin_type',
        'sync_interval',
        'node_plugin_type',
        'roles_tree_url',
        'last_sync_start_at',
        'last_sync_at',
        'last_check_at',
        'roles_review_days',
        'max_approvers',
        'use_tvm_role',
        'use_workflow_for_deprive',
        'audit_method',
        'retry_failed_roles',
        'export_to_tirole',
        'tvm_tirole_ids',
    }
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug='simple')
    client.login('frodo')

    response = client.json.get(url)
    data = response.json()
    assert set(data.keys()) == system_fields

    if role == 'creator':
        simple_system.creator = frodo
        simple_system.save(update_fields=['creator'])
    else:
        add_perms_by_role(role, frodo, simple_system)
    response = client.json.get(url)
    data = response.json()
    assert set(data.keys()) == system_fields | system_additional_fields


def test_permissions_for_edit(client, simple_system, users_for_test):
    """
    OPTIONS /frontend/systems/simple/
    """

    url = reverse('api_get_editable_fields', api_name='frontend', resource_name='systems', slug='simple')

    (art, fantom, terran, admin) = users_for_test
    add_perms_by_role('responsible', fantom, simple_system)
    add_perms_by_role('users_view', terran, simple_system)
    add_perms_by_role('auditor', art, simple_system)

    all_fields = {
        'description': {},
        'endpoint_timeout': {},  # exclusive
        'endpoint_long_timeout': {},  # exclusive
        'group_policy': {},
        'is_active': {},
        'is_sox': {},
        'name': {},
        'service': {},
        'can_be_broken': {},
        'has_review': {},
        'passport_policy': {},
        'request_policy': {},
        'role_grant_policy': {},
        'roletree_policy': {},
        'review_on_relocate_policy': {},
        'inconsistency_policy': {},
        'workflow_approve_policy': {},
        'auth_factor': {},  # exclusive
        'tvm_id': {},
        'check_certificate': {},
        'base_url': {},
        'plugin_type': {'allowed_values': ['generic', 'dumb', 'generic_legacy', 'generic_self', 'generic_connect']},
        'sync_interval': {},
        'node_plugin_type': {},
        'roles_tree_url': {},
        'roles_review_days': {},  # exclusive
        'max_approvers': {},  # exclusive
        'use_tvm_role': {},
        'use_workflow_for_deprive': {},
        'audit_method': {},
        'emails': {},
        'rolefields': {},
        'retry_failed_roles': {},
        'use_webauth': {},
        'is_broken': {},
        'export_to_tirole': {},
        'tvm_tirole_ids': {},
    }
    responsibles_fields = {
        'description': {},
        'group_policy': {},
        'is_active': {},
        'is_sox': {},  # exclusive
        'name': {},
        'service': {},
        'has_review': {},  # exclusive
        'passport_policy': {},
        'request_policy': {},
        'role_grant_policy': {},
        'roletree_policy': {},
        'review_on_relocate_policy': {},  # exclusive
        'inconsistency_policy': {},  # exclusive
        'workflow_approve_policy': {},  # exclusive
        'tvm_id': {},
        'base_url': {},
        'check_certificate': {},
        'plugin_type': {'allowed_values': ['generic', 'dumb']},
        'sync_interval': {},
        'node_plugin_type': {},
        'roles_tree_url': {},
        'use_tvm_role': {},
        'use_workflow_for_deprive': {},
        'audit_method': {},
        'emails': {},
        'rolefields': {},
        'retry_failed_roles': {},
        'use_webauth': {},
        'is_broken': {},
        'export_to_tirole': {},
        'tvm_tirole_ids': {},
    }
    sox_fields = {
        'description': {},
        'group_policy': {},
        'is_active': {},
        'name': {},
        'service': {},
        'passport_policy': {},
        'request_policy': {},
        'role_grant_policy': {},
        'roletree_policy': {},
        'tvm_id': {},
        'base_url': {},
        'check_certificate': {},
        'plugin_type': {'allowed_values': ['generic', 'dumb']},
        'sync_interval': {},
        'node_plugin_type': {},
        'roles_tree_url': {},
        'use_tvm_role': {},
        'use_workflow_for_deprive': {},
        'audit_method': {},
        'emails': {},
        'rolefields': {},
        'retry_failed_roles': {},
        'use_webauth': {},
        'is_broken': {},
        'export_to_tirole': {},
        'tvm_tirole_ids': {},
    }

    client.login('art')
    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json() == {'editable_fields': []}

    for user in ['fantom', 'terran']:
        client.login(user)
        response = client.json.get(url)
        assert response.status_code == 200
        assert response.json() == {'editable_fields': responsibles_fields}

    client.login('admin')
    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json()['editable_fields'] == all_fields
    assert response.json() == {'editable_fields': all_fields}

    simple_system.is_sox = True
    simple_system.save()

    client.login('art')
    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json() == {'editable_fields': []}

    for user in ['fantom', 'terran']:
        client.login(user)
        response = client.json.get(url)
        assert response.status_code == 200
        assert response.json() == {'editable_fields': sox_fields}

    client.login('admin')
    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json() == {'editable_fields': all_fields}

    url = reverse('api_get_editable_fields', api_name='frontend', resource_name='systems', slug='sImpLe')
    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json() == {'editable_fields': all_fields}

    client.login('terran')
    for role in ('developer', 'superuser'):
        response = client.json.get(url)
        assert response.status_code == 200
        assert response.json() == {'editable_fields': sox_fields}
        add_perms_by_role(role, terran)
        response = client.json.get(url)
        assert response.status_code == 200
        assert response.json() == {'editable_fields': all_fields}
        remove_perms_by_role(role, terran)


def test_make_system_inactive(client, arda_users, simple_system, complex_system, department_structure):
    """
    PATCH /frontend/systems/simple/
    """
    # Проверим, что роли в системе отзываются, если в PATCH передаётся is_active=False
    frodo = arda_users.frodo
    add_perms_by_role('superuser', frodo)
    add_perms_by_role('responsible', frodo, simple_system)
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug='simple')

    set_workflow(simple_system, group_code='approvers=[]')
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'})
    Role.objects.request_role(frodo, department_structure.fellowship, simple_system, '', {'role': 'manager'})
    complex_role = raw_make_role(frodo, complex_system, {'project': 'rules', 'role': 'admin'}, ROLE_STATE.GRANTED)
    assert Role.objects.filter(state=ROLE_STATE.GRANTED).count() == Role.objects.count()
    simple_system.is_active = False
    simple_system.save(update_fields=['is_active'])

    # При обновлении других полей выключенной системы с ролями ничего не произойдет
    response = client.json.patch(url, {'description': {'ru': 'Новое описание', 'en': 'New desription'}})
    assert response.status_code == 200
    assert Role.objects.filter(state=ROLE_STATE.GRANTED).count() == Role.objects.count()

    # При включении системы с ролями тоже ничего не произойдет
    response = client.json.patch(url, {'is_active': True})
    assert response.status_code == 200
    assert Role.objects.filter(state=ROLE_STATE.GRANTED).count() == Role.objects.count()

    # При выключении системы через api будут отозваны все роли в этой системе
    response = client.json.patch(url, {'is_active': False})
    assert response.status_code == 200
    assert Role.objects.filter(state=ROLE_STATE.DEPRIVED).count() == Role.objects.filter(system=simple_system).count()

    # Роли в другой системе не будут затронуты (если они не были выданы, как связанные к ролям в выключенной системе)
    complex_role.refresh_from_db()
    assert complex_role.state == ROLE_STATE.GRANTED


def test_system_role_field(arda_users, client, simple_system):
    simple_system.systemrolefields.create(
        type=FIELD_TYPE.CHARFIELD,
        slug='f1',
        name='Поле',
        name_en='Field',
        state=FIELD_STATE.ACTIVE
    )
    simple_system.systemrolefields.create(
        type=FIELD_TYPE.CHARFIELD,
        slug='f2',
        name='Неактивное поле',
        name_en='Field',
        state=FIELD_STATE.DEPRIVED
    )

    client.login(arda_users.frodo.username)
    data = client.json.get(reverse(
        'api_get_filter_fields',
        api_name='frontend',
        resource_name='systems',
        slug='simple'
    )).json()

    assert data == [{
        'is_shareable': True,
        'name': 'Поле',
        'required': False,
        'slug': 'f1',
        'type': 'charfield',
    }]


def test_form_empty_field_values(client, simple_system, arda_users):
    add_perms_by_role('responsible', arda_users.frodo, simple_system)
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug='simple')

    client.json.patch(url, {'emails': ['frodo123123@example.yandex.ru']})
    simple_system.refresh_from_db()
    assert simple_system.emails == 'frodo123123@example.yandex.ru'
    client.json.patch(url, {'emails': []})
    simple_system.refresh_from_db()
    assert not simple_system.emails


@pytest.mark.parametrize('fields', (set(), {'slug', 'service', 'is_broken'}))
def test_list__specify_fields(client, simple_system, arda_users, fields: set):
    client.login('frodo')

    response = client.json.get(
        reverse('api_dispatch_list', api_name='frontend', resource_name='systems'),
        data={'fields': ','.join(fields)},
    )
    assert response.status_code == 200, response.json()
    result = response.json()
    assert len(result['objects']) > 0
    for node_data in result['objects']:
        if fields:
            assert set(node_data.keys()) == fields
        else:
            # при пустом значении возвращаем все поля
            assert node_data != {}


def test_list__specify_unknown_fields(client, simple_system, arda_users):
    client.login('frodo')
    unknown_field = 'unknown_field'

    response = client.json.get(
        reverse('api_dispatch_list', api_name='frontend', resource_name='systems'),
        data={'fields': f'slug,{unknown_field}'},
    )
    assert response.status_code == 400, response.json()
    error = response.json()
    assert error['message'] == f'Unknown fields passed in query: {unknown_field}'


@pytest.mark.parametrize('fields', (set(), {'slug', 'service', 'is_broken'}))
def test_get__specify_fields(client, simple_system, arda_users, fields: set):
    client.login('frodo')

    response = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug=simple_system.slug),
        data={'fields': ','.join(fields)},
    )
    assert response.status_code == 200, response.json()
    node_data = response.json()
    if fields:
        assert set(node_data.keys()) == fields
    else:
        # при пустом значении возвращаем все поля
        assert node_data != {}


def test_get__specify_unknown_fields(client, simple_system, arda_users):
    client.login('frodo')
    unknown_field = 'unknown_field'

    response = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug=simple_system.slug),
        data={'fields': f'slug,{unknown_field}'},
    )
    assert response.status_code == 400, response.json()
    error = response.json()
    assert error['message'] == f'Unknown fields passed in query: {unknown_field}'


@pytest.mark.parametrize(('permission', 'allowed'), [
    ('superuser', True),
    ('responsible', True),
    ('users_view', True),
    ('viewer', False),
])
def test_patch__repair_system(client, permission: str, allowed: bool):
    system = create_system(is_broken=True)
    user = create_user()
    add_perms_by_role(permission, user, system)

    client.login(user)

    response = client.json.patch(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug=system.slug),
        data={'is_broken': False},
    )
    if not allowed:
        assert response.status_code == 403, (response.status_code, response.content)
        result = response.json()
        assert result['message'] == 'Вы не можете редактировать данную систему', (response.result)
    else:
        assert response.status_code == 200, (response.status_code, response.content)
        result = response.json()
        assert result['is_broken'] is False

        system.refresh_from_db(fields=['is_broken'])
        assert system.is_broken is False


@pytest.mark.parametrize('export_to_tirole', [False, True])
@pytest.mark.parametrize(('permission', 'allowed'), [
    ('superuser', True),
    ('responsible', True),
    ('users_view', True),
    ('viewer', False),
])
def test_patch__export_to_tirole(client, mongo_mock, permission: str, allowed: bool, export_to_tirole: bool):
    system = create_system()
    user = create_user()
    add_perms_by_role(permission, user, system)

    client.login(user)

    with mock.patch('idm.utils.events.add_event') as add_event_mock, run_commit_hooks():
        response = client.json.patch(
            reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug=system.slug),
            data={'export_to_tirole': export_to_tirole},
        )

    if not allowed:
        assert response.status_code == 403, (response.status_code, response.content)
        result = response.json()
        assert result['message'] == 'Вы не можете редактировать данную систему', (response.result)
        add_event_mock.assert_not_called()
    else:
        assert response.status_code == 200, (response.status_code, response.content)
        result = response.json()
        assert result['export_to_tirole'] is export_to_tirole

        system.refresh_from_db(fields=['export_to_tirole'])
        assert system.export_to_tirole is export_to_tirole

        if export_to_tirole:
            add_event_mock.assert_called_once_with(
                event_type=events.EventType.YT_EXPORT_REQUIRED,
                system_id=system.id,
            )
        else:
            add_event_mock.assert_not_called()

@pytest.mark.parametrize(('permission', 'allowed'), [
    ('superuser', True),
    ('responsible', True),
    ('users_view', True),
    ('viewer', False),
])
def test_patch__tvm_tirole_ids(client, permission: str, allowed: bool):
    system = create_system()
    user = create_user()
    add_perms_by_role(permission, user, system)

    tvm_ids = []
    for _ in range(3):
        tvm_app = create_tvm_app()
        tvm_app.member_of.filter(type=GROUP_TYPES.TVM_SERVICE).get().responsibilities.create(user=user, is_active=True)
        tvm_ids.append(int(tvm_app.username))

    client.login(user)
    with mock.patch('idm.utils.events.add_event') as add_event_mock, run_commit_hooks():
        response = client.json.patch(
            reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug=system.slug),
            data={'tvm_tirole_ids': tvm_ids},
        )

    if not allowed:
        assert response.status_code == 403, (response.status_code, response.content)
        result = response.json()
        assert result['message'] == 'Вы не можете редактировать данную систему', (response.result)
        add_event_mock.assert_not_called()
    else:
        assert response.status_code == 200, (response.status_code, response.content)
        result = response.json()
        assert result['tvm_tirole_ids'] == tvm_ids

        system.refresh_from_db(fields=['tvm_tirole_ids'])
        assert system.tvm_tirole_ids == tvm_ids


@pytest.mark.parametrize('permission', ['superuser', 'responsible', 'users_view'])
@pytest.mark.parametrize('exists', [False, True])
def test_patch__tvm_tirole_ids_forbidden(client, permission: str, exists: bool):
    system = create_system()
    user = create_user()
    add_perms_by_role(permission, user, system)

    client.login(user)

    if exists:
        tvm_ids = [random.randint(1, 10**8) for _ in range(3)]
    else:
        tvm_ids = [int(create_tvm_app().username) for _ in range(3)]

    with mock.patch('idm.utils.events.add_event') as add_event_mock, run_commit_hooks():
        response = client.json.patch(
            reverse('api_dispatch_detail', api_name='frontend', resource_name='systems', slug=system.slug),
            data={'tvm_tirole_ids': tvm_ids},
        )

    assert response.status_code == 400, (response.status_code, response.content)
    result = response.json()
    assert result['message'] == 'Invalid data sent', result
    assert result['errors']['tvm_tirole_ids'] == \
           ['Можно указать только TVM-приложения, для которых вы являетесь управляющим в ABC'], result
    add_event_mock.assert_not_called()
