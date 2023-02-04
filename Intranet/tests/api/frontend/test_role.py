import io
import operator
import random
import re
import zipfile

import pytest
import waffle.testutils
from django.conf import settings
from django.core import mail
from django.db import connection
from django.db.models import Max
from django.test.utils import CaptureQueriesContext
from django.utils.dateparse import parse_datetime
from django.utils.timezone import now, timedelta
from mock import patch, call

from idm.api.exceptions import Forbidden
from idm.core.constants.role import ROLE_STATE
from idm.core.constants.rolefield import FIELD_STATE, FIELD_TYPE
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.core.models import Role, RoleField, RoleNode, RoleAlias, ApproveRequest
from idm.core.models.metrikacounter import MetrikaCounter
from idm.core.models.appmetrica import AppMetrica
from idm.permissions.utils import add_perms_by_role, remove_perms_by_role
from idm.tests import utils
from idm.tests.models.test_metrikacounter import generate_counter_record
from idm.tests.models.test_appmetrica import generate_app_record
from idm.tests.utils import set_workflow, DEFAULT_WORKFLOW, raw_make_role, create_system, create_user, create_group, \
    random_slug
from idm.users.constants.group import GROUP_TYPES
from idm.users.models import Group, GroupMembership, GroupResponsibility
from idm.utils import reverse, json

pytestmark = pytest.mark.django_db


@pytest.fixture
def roles_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='roles')


def get_role_url(role_id, api_name='frontend'):
    return reverse('api_dispatch_detail', api_name=api_name, resource_name='roles', pk=role_id)


def assert_approvers_deprecated(data, expected):
    usernames = [
        [approver['username'] for approver in approve]
        for approve in data['require_approve_from']
    ]
    assert usernames == expected


def test_dont_estimate_count_on_page_of_single_user(simple_system, arda_users, client):
    client.login('frodo')

    with CaptureQueriesContext(connection) as context:
        client.json.get(reverse('api_dispatch_list', api_name='frontend', resource_name='roles'), {'limit': 0})
        assert len([x for x in context if 'count_estimate' in str(x)]) == 1

    with CaptureQueriesContext(connection) as context:
        client.json.get(
            reverse('api_dispatch_list', api_name='frontend', resource_name='roles'), {'user': 'frodo', 'limit': 0}
        )
        assert len([x for x in context if 'count_estimate' in str(x)]) == 0


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_get_parent_parent(simple_system, arda_users, client, api_name):
    client.login('frodo')
    frodo = arda_users.frodo
    utils.set_workflow(simple_system, 'approvers = []')

    role_1 = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'superuser'}, None)
    role_2 = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'manager'}, None)
    role_3 = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'poweruser'}, None)
    role_2.parent = role_1
    role_2.save(update_fields=['parent'])
    role_3.parent = role_2
    role_3.save(update_fields=['parent'])

    response = client.json.get(reverse('api_dispatch_list', api_name=api_name, resource_name='roles'),
                               {'id': role_3.id})
    assert response.status_code == 200

    response = response.json()
    assert response['meta']['total_count'] == 1

    _role_3 = response['objects'][0]
    assert _role_3['id'] == role_3.id
    _role_2 = _role_3['parent']
    assert _role_2['id'] == role_2.id

    # parent.parent - просто id роли
    _role_1_id = _role_2['parent']
    assert _role_1_id == role_1.id


def test_get_roles_filter_by_type(client, simple_system, users_for_test, roles_url):
    """
    GET /frontend/roles/?system=simple
    Проверяем, что роль правильно фильтруется по типу
    """
    (art, fantom, terran, admin) = users_for_test
    utils.set_workflow(simple_system, 'approvers = ["art"]')

    def assert_count(**states):
        responses = {state: client.json.get(roles_url, {'type': state}).json() for state in states.keys()}
        for state, count in states.items():
            response = responses[state]
            assert len(response['objects']) == count

    # role granted to art
    role = Role.objects.request_role(admin, admin, simple_system, None, {'role': 'superuser'}, None)
    client.login('admin')

    # запрошенная роль отображается в запрошенных
    assert_count(active=0, requested=1, inactive=0)

    # созданная роль отображается в неактивных
    role.set_raw_state('created', is_active=False)
    assert_count(active=0, requested=0, inactive=1)

    # одобренная роль отображается в запрошенных
    role.set_raw_state('approved', is_active=False)
    assert_count(active=0, requested=1, inactive=0)

    # отозванная роль отображается в неактивных
    role.set_raw_state('deprived', is_active=False)
    assert_count(active=0, requested=0, inactive=1)

    # роль, которую не получилось выдать, отображается в неактивных
    role.set_raw_state('failed', is_active=False)
    assert_count(active=0, requested=0, inactive=1)

    # просроченная роль отображается в неактивных
    role.set_raw_state('failed', is_active=False)
    assert_count(active=0, requested=0, inactive=1)

    # отклонённая роль отображается в неактивных
    role.set_raw_state('declined', is_active=False)
    assert_count(active=0, requested=0, inactive=1)

    # перезапрошенная роль отображается и в запрошенных, и в активных
    role.set_raw_state('rerequested', is_active=True)
    assert_count(active=1, requested=1, inactive=0)

    # роль в статусе "нужен перезапрос" отображается в активных
    role.set_raw_state('need_request', is_active=True)
    assert_count(active=1, requested=0, inactive=0)

    # выданная роль отображается в активных
    role.set_raw_state('granted', is_active=True)
    assert_count(active=1, requested=0, inactive=0)

    # отзываемая роль отображается в активных (!), потому что в системе она ещё активна
    role.set_raw_state('depriving', is_active=True)
    assert_count(active=1, requested=0, inactive=0)

    # роль в статусе "перезапрос в связи с пересмотром" отображается и в активных, и в запрошенных
    role.set_raw_state('review_request', is_active=True)
    assert_count(active=1, requested=1, inactive=0)


def test_get_roles(client, simple_system, users_for_test, roles_url):
    """
    GET /frontend/roles/?system=simple
    """

    (art, fantom, terran, admin) = users_for_test

    # role granted to art
    role1 = Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)

    utils.set_workflow(simple_system, 'approvers = ["admin"]')

    # role in requested state
    role2 = Role.objects.request_role(art, art, simple_system, None, {'role': 'manager'}, None)

    # role in deprived state
    role_to_deprive = Role.objects.request_role(admin, admin, simple_system, None, {'role': 'superuser'}, None)
    utils.refresh(role_to_deprive).deprive_or_decline(admin)

    # art can't access roles for the system
    client.login('art')
    with utils.assert_num_queries_lte(30, show_queries=False):
        data = client.json.get(roles_url, {'system': 'simple'}).json()
    assert data['meta']['total_count'] == 2
    assert {item['user']['username'] for item in data['objects']} == {'art'}

    # admin will see all system roles
    client.login('admin')
    data = client.json.get(roles_url, {'system': 'simple'}).json()
    assert data['meta']['total_count'] == 3
    assert {item['system']['slug'] for item in data['objects']} == {'simple'}

    # testing role "type" filter

    # inactive role
    data = client.json.get(roles_url, {
        'system': 'simple',
        'type': 'inactive'
    }).json()

    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['data'] == {'role': 'superuser'}
    assert data['objects'][0]['user']['username'] == 'admin'
    assert data['objects'][0]['state'] == 'deprived'
    assert data['objects'][0]['is_active'] is False

    # requested role
    data = client.json.get(roles_url, {
        'system': 'simple',
        'type': 'requested'
    }).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['data'] == {'role': 'manager'}
    assert data['objects'][0]['user']['username'] == 'art'
    assert data['objects'][0]['state'] == 'requested'
    assert data['objects'][0]['is_active'] is False

    # active
    data = client.json.get(roles_url, {
        'system': 'simple',
        'type': 'active'
    }).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['data'] == {'role': 'superuser'}
    assert data['objects'][0]['user']['username'] == 'art'
    assert data['objects'][0]['state'] == 'granted'
    assert data['objects'][0]['is_active'] is True

    # single id
    data = client.json.get(roles_url, {
        'id': role1.id
    }).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['id'] == role1.id

    # many ids
    data = client.json.get(roles_url, {
        'id': '{},{}'.format(role1.id, role2.id)
    }).json()
    assert data['meta']['total_count'] == 2
    assert set(obj['id'] for obj in data['objects']) == {role1.id, role2.id}

    # absent id
    data = client.json.get(roles_url, {
        'id': Role.objects.all().aggregate(Max('id'))['id__max'] + 1
    }).json()
    assert data['meta']['total_count'] == 0
    assert len(data['objects']) == 0


@pytest.mark.parametrize('order_by', ['-updated', 'system', 'role', 'subject', None])
def test_roles_sorting(client, simple_system, arda_users_with_roles, roles_url, order_by):
    """Проверим, что сортировка работает"""

    client.login('frodo')
    kwargs = {
        'system': 'simple',
    }
    if order_by is not None:
        kwargs['order_by'] = order_by
    response = client.json.get(roles_url, kwargs)
    assert response.status_code == 200


def test_get_roles_lang(client, arda_users_with_roles, roles_url):
    client.login('frodo', lang='en')
    data = client.json.get(roles_url).json()
    assert data['objects'][0]['system']['name'] == 'Simple system'


def test_get_roles_for_webauth(client, arda_users_with_roles, roles_url):
    client.login('gandalf')
    role = arda_users_with_roles.gandalf[0]
    role.fields_data = {'key': 'value'}
    role.save(update_fields=['fields_data'])

    data = client.json.get(
        roles_url,
        {'user': 'gandalf', 'for_webauth': True, 'ownership': 'personal', 'parent_type': 'absent'}
    ).json()
    assert data['objects'] == [{
        'group': None,
        'user': {
            'username': 'gandalf',
        },
        'node': {
            'value_path': role.node.value_path,
        },
        'fields_data': {
            'key': 'value',
        },
    }]


def test_get_group_roles(client, simple_system, arda_users, department_structure, roles_url):
    """
    GET /frontend/roles/?system=simple
    """

    fellowship = department_structure.fellowship
    frodo = arda_users.get('frodo')
    utils.set_workflow(simple_system, group_code='approvers=[]')

    # role granted to fellowship
    Role.objects.request_role(frodo, fellowship, simple_system, None, {'role': 'superuser'}, None)

    # role in requested state
    utils.set_workflow(simple_system, group_code='approvers = ["legolas"]')
    Role.objects.request_role(frodo, fellowship, simple_system, None, {'role': 'manager'}, None)

    # role in deprived state
    utils.set_workflow(simple_system, group_code='approvers = []')
    role_to_deprive = Role.objects.request_role(frodo, fellowship, simple_system, None, {'role': 'poweruser'}, None)
    role_to_deprive = utils.refresh(role_to_deprive, select_related=['user'])
    role_to_deprive.deprive_or_decline(frodo)

    client.login('legolas')
    with utils.assert_num_queries_lte(65):
        data = client.json.get(roles_url, {'system': 'simple'}).json()
    # две пользовательских, три групповых
    assert data['meta']['total_count'] == 5
    expected_keys = {'added', 'data', 'expire_at', 'review_at', 'fields_data', 'granted_at', 'group',
                     'human', 'human_short', 'human_state',
                     'id', 'is_active', 'is_public', 'node', 'parent', 'review_at',
                     'review_days', 'review_date', 'state', 'system', 'system_specific',
                     'ttl_date', 'ttl_days', 'updated', 'user',
                     'with_inheritance', 'with_robots', 'with_external', 'without_hold', }
    assert set(data['objects'][0].keys()) == expected_keys
    usernames = {item['user']['username'] for item in data['objects'] if item['user']}
    groupnames = {item['group']['slug'] for item in data['objects'] if item['group']}
    assert usernames == {'legolas'}
    assert groupnames == {'fellowship-of-the-ring'}

    # group responsible will see all roles
    client.login('frodo')
    data = client.json.get(roles_url, {'system': 'simple'}).json()

    # три групповых роли + 2*количество_участников персональных
    assert data['meta']['total_count'] == 3 + 2 * fellowship.members.count()
    assert {item['system']['slug'] for item in data['objects']} == {'simple'}

    # отфильтруем групповые роли
    group_roles = [item for item in data['objects'] if item['group']]
    fellowship_info = group_roles[0]['group']
    expected_keys = ['created_at', 'id', 'name', 'slug', 'state',
                     'type', 'updated_at', 'url']
    assert sorted(fellowship_info.keys()) == expected_keys
    assert fellowship_info == {
        'created_at': fellowship.created_at.isoformat(),
        'id': fellowship.external_id,
        'name': fellowship.name,
        'state': 'active',
        'type': 'department',
        'updated_at': fellowship.updated_at.isoformat(),
        'slug': 'fellowship-of-the-ring',
        'url': 'https://staff.test.yandex-team.ru/departments/fellowship-of-the-ring/',
    }

    # Если группа depriving или даже deprived, её роли мы должны всё равно показывать
    fellowship.mark_depriving()
    response = client.json.get(roles_url, {'system': 'simple', 'group': fellowship.external_id})
    assert response.status_code == 200
    data = response.json()
    assert data['meta']['total_count'] == 3


def test_get_roles_with_refs(client, simple_system_w_refs, arda_users, responsible_gandalf, roles_url):
    """
    GET /frontend/roles/?system=simple&parent={id}
    """

    frodo = arda_users.frodo

    # role granted to frodo
    req_role = Role.objects.request_role(frodo, frodo, simple_system_w_refs, None, {'role': 'admin'}, None)
    ref_role = req_role.refs.get()

    # gandalf will see all system roles
    client.login('gandalf')
    data = client.json.get(roles_url, {'system': 'simple'}).json()

    # две роли: запрошенная и связная
    assert data['meta']['total_count'] == 2

    # проверяем, что родительская роль отдалась полностью в связной
    roles = data['objects']
    assert len(roles) == 2
    ref, parent = roles
    if parent['parent']:
        parent, ref = ref, parent

    assert ref['id'] == ref_role.pk
    assert ref['parent']['id'] == req_role.pk
    assert ref['parent']['system']['slug'] == 'simple'
    assert ref['parent']['user']['username'] == 'frodo'

    # а у родительской роли в parent ничего нет
    role = data['objects'][1]
    assert parent['id'] == req_role.pk
    assert parent['parent'] is None

    # проверяем фильтрацию по parent
    data = client.json.get(roles_url, {'parent': req_role.pk}).json()

    assert data['meta']['total_count'] == 1

    assert len(data['objects']) == 1
    role = data['objects'][0]
    assert role['id'] == ref_role.pk

    role = client.json.get(get_role_url(req_role.id)).json()
    assert role['ref_count'] == 1

    role = client.json.get(get_role_url(ref_role.id)).json()
    assert role['ref_count'] == 0

    ref_role.is_public = False
    ref_role.save(update_fields=['is_public'])
    role = client.json.get(get_role_url(req_role.id)).json()
    assert role['ref_count'] == 0


def test_get_roles_parentless_or_parentful(client, simple_system_w_refs, arda_users, responsible_gandalf, roles_url,
                                           department_structure):
    """
    GET /frontend/roles/?system=simple&parent={absent,present,user,group}
    """

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    client.login('frodo')

    parentless_user_role = Role.objects.request_role(frodo, frodo, simple_system_w_refs, None, {'role': 'admin'}, None)
    parentless_user_role = utils.refresh(parentless_user_role)
    parentful_user_role = parentless_user_role.refs.get()

    parentless_group_role = Role.objects.request_role(frodo, fellowship, simple_system_w_refs, None, {'role': 'admin'},
                                                      None)
    parentful_group_role = parentless_group_role.refs.get(group=fellowship)
    # Итого, имеется 2*members + 4 роли: одна роль на frodo, вторая - связанная по ней.
    # Третья - роль на fellowship, четвёртая - связанная по ней, ещё 2*members - по количество пользователей в группе

    response = client.json.get(roles_url, {'parent_type': 'absent'})
    assert response.status_code == 200
    data = response.json()
    assert data['meta']['total_count'] == 2
    assert {item['id'] for item in data['objects']} == {parentless_user_role.pk, parentless_group_role.pk}

    data = client.json.get(roles_url, {'parent_type': 'present'}).json()
    assert data['meta']['total_count'] == 2 * fellowship.members.count() + 2

    data = client.json.get(roles_url, {'parent_type': 'user'}).json()
    assert data['meta']['total_count'] == 1
    assert {item['id'] for item in data['objects']} == {parentful_user_role.pk}

    data = client.json.get(roles_url, {'parent_type': 'group'}).json()
    assert data['meta']['total_count'] == 2 * fellowship.members.count() + 1

    data = client.json.get(roles_url, {'parent_type': 'group', 'ownership': 'group'}).json()
    assert {item['id'] for item in data['objects']} == {parentful_group_role.pk}


def test_get_filtered_roles(client, pt1_system, users_with_roles, roles_url):
    """
    GET /frontend/roles/?system=simple
    """
    (art, fantom, terran, admin) = users_with_roles

    # filter by users
    client.login('admin')
    data = client.json.get(roles_url, {
        'system': 'test1',
        'users': 'art,terran'
    }).json()

    assert data['meta']['total_count'] == 3
    assert {item['user']['username'] for item in data['objects']} == {'art', 'terran'}

    # filter by role_id_gt
    client.login('admin')
    data = client.json.get(roles_url, {
        'system': 'test1',
        'id__gt': fantom.id
    }).json()
    assert data['meta']['total_count'] == Role.objects.filter(id__gt=fantom.id).count()
    assert {item['user']['username'] for item in data['objects']} == set(
        Role.objects.filter(id__gt=fantom.id).values_list('user__username', flat=True))

    # filter by role_id_lt
    client.login('admin')
    data = client.json.get(roles_url, {
        'system': 'test1',
        'id__lt': fantom.id
    }).json()
    assert data['meta']['total_count'] == Role.objects.filter(id__lt=fantom.id).count()
    assert {item['user']['username'] for item in data['objects']} == set(
        Role.objects.filter(id__lt=fantom.id).values_list('user__username', flat=True))

    # filter by role path
    client.login('admin')
    data = client.json.get(roles_url, {
        'system': 'test1',
        'path': '/proj1/admin/'
    }).json()

    assert data['meta']['total_count'] == 2
    assert data['objects'][0]['data'] == data['objects'][1]['data'] == {'project': 'proj1', 'role': 'admin'}

    # filter by role partial path
    client.login('admin')
    data = client.json.get(roles_url, {
        'system': 'test1',
        'path': '/proj2/'
    }).json()

    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['data'] == {'project': 'proj2', 'role': 'wizard'}

    # filter by state
    client.login('admin')
    data = client.json.get(roles_url, {
        'state': 'deprived'
    }).json()

    assert data['meta']['total_count'] == 1

    data = client.json.get(roles_url, {
        'state': ['granted', 'deprived']
    }).json()

    assert data['meta']['total_count'] == 4

    data = client.json.get(roles_url, {
        'state': 'granted,deprived'
    }).json()

    assert data['meta']['total_count'] == 4


def test_get_filtered_group_roles(client, groups_with_roles, arda_users, simple_system, roles_url):
    """
    GET /frontend/roles/?group=
    """
    # filter by group
    client.login('frodo')

    bilbo = arda_users.bilbo
    Role.objects.request_role(bilbo, bilbo, simple_system, None, {'role': 'superuser'}, None)

    ids = [r.group.external_id for r in groups_with_roles['fellowship'] + groups_with_roles['associations']]

    data = client.json.get(roles_url, {
        'group': ','.join(map(str, ids))
    }).json()

    assert data['meta']['total_count'] == 3
    assert ({item['group']['id'] for item in data['objects']} == set(ids))

    data = client.json.get(roles_url, {
        'group': ','.join(map(str, ids)), 'user': 'bilbo'
    }).json()
    assert data['meta']['total_count'] == 4


def test_get_filtered_group_roles_with_parents(client, groups_with_roles, arda_users, simple_system, roles_url):
    client.login('frodo')

    assoc_id = groups_with_roles['associations'][0].group.external_id
    fellow_id = groups_with_roles['fellowship'][0].group.external_id

    role_id = groups_with_roles['associations'][0].id

    direct = client.json.get(roles_url, {'group': assoc_id, 'system': simple_system.slug, 'path': '/poweruser/'}).json()
    assert direct['meta']['total_count'] == 1
    assert direct['objects'][0]['group']['id'] == assoc_id
    assert direct['objects'][0]['id'] == role_id

    remote = client.json.get(roles_url, {'group': fellow_id, 'system': simple_system.slug,
                                         'path': '/poweruser/'}).json()
    assert remote['meta']['total_count'] == 0

    remote = client.json.get(roles_url, {'group': fellow_id, 'system': simple_system.slug,
                                         'path': '/poweruser/', 'with_parents': False}).json()
    assert remote['meta']['total_count'] == 0

    remote = client.json.get(roles_url, {'group': fellow_id, 'system': simple_system.slug,
                                         'path': '/poweruser/', 'with_parents': True}).json()
    assert remote['meta']['total_count'] == 1
    assert direct['objects'][0]['group']['id'] == assoc_id
    assert direct['objects'][0]['id'] == role_id

    direct = client.json.get(roles_url, {'group': assoc_id, 'system': simple_system.slug,
                                         'path': '/poweruser/', 'with_parents': True}).json()
    assert direct['meta']['total_count'] == 1
    assert direct['objects'][0]['group']['id'] == assoc_id
    assert direct['objects'][0]['id'] == role_id


def test_get_group_vs_personal_roles(client, arda_users, department_structure, simple_system, roles_url):
    """
    GET /frontend/roles/?ownership=personal|group
    """
    utils.set_workflow(simple_system, utils.DEFAULT_WORKFLOW, utils.DEFAULT_WORKFLOW)
    client.login('frodo')

    frodo = arda_users.frodo
    personal_role = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'superuser'}, {})

    shire = Group.objects.get(slug='the-shire')
    group_role = Role.objects.request_role(frodo, shire, simple_system, None, {'role': 'poweruser'}, {})

    data = client.json.get(roles_url).json()
    assert data['meta']['total_count'] == 7

    data = client.json.get(roles_url, {'ownership': 'group'}).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['id'] == group_role.id

    data = client.json.get(roles_url, {'ownership': 'personal'}).json()
    assert data['meta']['total_count'] == 6
    for role in data['objects']:
        if role['parent']:
            # персональные роли от групповой
            assert role['parent']['id'] == group_role.id
        else:
            # отдельная личная роль
            assert role['id'] == personal_role.id


def test_filter_roles_by_lookup_fields_data_nosystem(client, arda_users, simple_system, roles_url):
    """
    GET /frontend/roles/?field_data__login=login
    """
    client.login('frodo')

    response = client.json.get(roles_url, {'field_data__login': 'yndx-frodo1'})
    assert response.status_code == 400
    assert response.json()['message'] == 'Для фильтра по полям нужно указать систему'


def test_filter_roles_by_lookup_fields_data_noindex(client, arda_users, simple_system, roles_url):
    """
    GET /frontend/roles/?field_data__login=login
    """
    client.login('frodo')

    response = client.json.get(
        roles_url,
        {'system': simple_system.slug, 'field_data__login': 'yndx-frodo1'}
    )
    assert response.status_code == 400
    assert response.json()['message'] == "Для полей 'login' не найдено активных индексов"


def test_filter_roles_by_lookup_fields_data_char(client, arda_users, simple_system, roles_url):
    """
    GET /frontend/roles/?field_data__login=login
    """
    client.login('frodo')

    frodo = arda_users.frodo
    fieldful_role = Role.objects.request_role(
        frodo, frodo, simple_system, None, {'role': 'admin'}, {'passport-login': 'yndx-frodo1'}
    )

    simple_system.systemrolefields.create(
        slug='passport-login',
        state=FIELD_STATE.ACTIVE,
    )

    response = client.json.get(
        roles_url,
        {'system': simple_system.slug, 'field_data__passport-login': 'yndx-frodo1'}
    )
    assert response.status_code == 200
    data = response.json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['id'] == fieldful_role.pk


def test_filter_roles_by_lookup_fields_data_int(client, arda_users, simple_system, roles_url):
    """
    GET /frontend/roles/?field_data__resourse_id=resource_id
    """
    client.login('frodo')

    node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'admin'})
    RoleField.objects.create(
        node=node, type=FIELD_TYPE.INTEGER, name='Resource Id', slug='resource_id', is_required=True
    )

    frodo = arda_users.frodo
    fieldful_role = Role.objects.request_role(
        frodo, frodo, simple_system, None, {'role': 'admin'}, {'resource_id': 42}
    )

    simple_system.systemrolefields.create(
        type=FIELD_TYPE.INTEGER,
        slug='resource_id',
        state=FIELD_STATE.ACTIVE,
    )

    response = client.json.get(
        roles_url,
        {'system': simple_system.slug, 'field_data__resource_id': 42}
    )
    assert response.status_code == 200
    data = response.json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['id'] == fieldful_role.pk


def test_filter_roles_by_lookup_fields_data_badint(client, arda_users, simple_system, roles_url):
    """
    GET /frontend/roles/?field_data__resource_id=resource_id
    """
    client.login('frodo')

    node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'admin'})
    RoleField.objects.create(
        node=node, type=FIELD_TYPE.INTEGER, name='Resource Id', slug='resource_id', is_required=True
    )

    simple_system.systemrolefields.create(
        type=FIELD_TYPE.INTEGER,
        slug='resource_id',
        state=FIELD_STATE.ACTIVE,
    )

    response = client.json.get(
        roles_url,
        {'system': simple_system.slug, 'field_data__resource_id': 'forty-two'}
    )
    assert response.status_code == 400
    assert response.json()['message'] == "Ошибка при преобразовании значения 'forty-two' к типу 'int'"


def test_filter_roles_by_lookup_fields_data_bool(client, arda_users, simple_system, roles_url):
    """
    GET /frontend/roles/?field_data__can_read=True
    """
    client.login('frodo')

    node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'admin'})
    RoleField.objects.create(node=node, type=FIELD_TYPE.BOOLEAN, name='IsBoolean', slug='can_read', is_required=False)

    frodo = arda_users.frodo
    fieldful_role = Role.objects.request_role(
        frodo, frodo, simple_system, None, {'role': 'admin'}, {'can_read': False}
    )

    simple_system.systemrolefields.create(
        type=FIELD_TYPE.BOOLEAN,
        slug='can_read',
        state=FIELD_STATE.ACTIVE,
    )

    response = client.json.get(
        roles_url,
        {'system': simple_system.slug, 'field_data__can_read': False}
    )

    assert response.status_code == 200
    data = response.json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['id'] == fieldful_role.pk


def test_filter_roles_by_lookup_fields_data_badbool(client, arda_users, simple_system, roles_url):
    """
    GET /frontend/roles/?field_data__can_read=True
    """
    client.login('frodo')

    node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'admin'})
    RoleField.objects.create(node=node, type=FIELD_TYPE.BOOLEAN, name='IsBoolean', slug='can_read', is_required=False)

    frodo = arda_users.frodo
    fieldful_role = Role.objects.request_role(
        frodo, frodo, simple_system, None, {'role': 'admin'}, {'can_read': False}
    )

    simple_system.systemrolefields.create(
        type=FIELD_TYPE.BOOLEAN,
        slug='can_read',
        state=FIELD_STATE.ACTIVE,
    )

    response = client.json.get(
        roles_url,
        {'system': simple_system.slug, 'field_data__can_read': 'IamBadBool'}
    )

    assert response.status_code == 400
    assert response.json()['message'] == "Ошибка при преобразовании значения 'IamBadBool' к типу 'boolean'"


def test_filter_roles_by_fields_data(client, arda_users, simple_system, responsible_gandalf, roles_url):
    """
    GET /frontend/roles/?fields_data={fields_data}
    """
    client.login('frodo')

    frodo = arda_users.frodo
    fieldless_role = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'admin'}, None)
    fieldful_role1 = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'admin'},
                                               {'login': 'yndx-frodo1'})
    fieldful_role2 = Role.objects.request_role(frodo, frodo, simple_system, None, {'role': 'admin'},
                                               {'login': 'yndx-frodo2'})
    data = client.json.get(roles_url).json()
    assert data['meta']['total_count'] == 3

    data = client.json.get(roles_url, {'fields_data': json.dumps({'login': 'yndx-frodo1'})}).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['id'] == fieldful_role1.pk

    data = client.json.get(roles_url, {'fields_data': json.dumps({'login': 'yndx-frodo2'})}).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['id'] == fieldful_role2.pk

    data = client.json.get(roles_url, {'fields_data': json.dumps(None)}).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['id'] == fieldless_role.pk


def test_roles_for_group_aware_system(client, arda_users, department_structure, aware_simple_system, roles_url,
                                      responsible_gandalf):
    """Тесты для group-aware систем"""

    utils.set_workflow(aware_simple_system, group_code=utils.DEFAULT_WORKFLOW)

    Role.objects.request_role(arda_users.frodo, department_structure.associations, aware_simple_system, '',
                              {'role': 'admin'}, None)
    client.login('frodo')
    data = client.json.get(roles_url).json()
    assert data['meta']['total_count'] == 1
    assert len(data['objects']) == 1

    # фильтр по ownership действует и для group aware систем
    data = client.json.get(roles_url, {'ownership': 'group'}).json()
    assert data['meta']['total_count'] == 1
    assert len(data['objects']) == 1
    data = client.json.get(roles_url, {'ownership': 'personal'}).json()
    assert data['meta']['total_count'] == 0
    assert len(data['objects']) == 0

    # фильтр по пользователю возвращает групповые роли этого пользователя для group-aware систем
    response = client.json.get(roles_url, {'user': 'frodo'})
    assert response.status_code == 200
    data = response.json()
    assert data['meta']['total_count'] == 1
    assert len(data['objects']) == 1

    # ... даже если передано несколько пользователей
    client.login('gandalf')
    data = client.json.get(roles_url, {'user': 'gimli,sam'}).json()
    assert data['meta']['total_count'] == 1
    assert len(data['objects']) == 1

    # но только если текущий пользователь - ответственный или член этих групп:
    client.login('manve')
    data = client.json.get(roles_url, {'user': 'gimli,sam'}).json()
    assert len(data['objects']) == 0
    utils.add_perms_by_role('responsible', arda_users.manve, aware_simple_system)
    data = client.json.get(roles_url, {'user': 'gimli,sam'}).json()
    assert len(data['objects']) == 1


def test_roles_report(client, simple_system, users_for_test, roles_url):
    """
    POST /frontend/roles/
    """
    (art, fantom, terran, admin) = users_for_test
    utils.add_perms_by_role('superuser', art)
    utils.clear_mailbox()
    client.login('art')

    # Проверка отчета по активным ролям
    role1 = utils.raw_make_role(fantom, simple_system, {'role': 'admin'}, state='granted')
    role1.requests.create()
    role2 = utils.raw_make_role(terran, simple_system, {'role': 'admin'}, state='requested')
    role2.requests.create()

    response = client.json.post(roles_url, {'state': 'granted'})
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'format': ['Обязательное поле.']
        }
    }

    response = client.json.post(roles_url, {'state': 'granted', 'format': 'csv',
                                            'comment': 'Хочу увидеть всё, что скрыто'})
    assert response.status_code == 200
    assert response.json() == {'message': 'Отчёт формируется и будет отправлен вам на почту по завершении'}

    assert len(mail.outbox) == 1

    message = mail.outbox[0]
    assert message.to == ['art@example.yandex.ru']
    assert message.subject == 'Отчёт сформирован.'
    utils.assert_contains([
        'Добрый день',
        'Отчёт сформирован и приложен к данному письму.',
        'Хочу увидеть всё, что скрыто',
        'Ваш IDM'
    ], message.body)
    attachments = message.attachments
    assert len(attachments) == 1
    filename, data, content_type = attachments[0]
    assert re.match(r'^report_\d+_\d+\.csv\.zip', filename)
    assert content_type == 'application/zip'
    input_zip = io.BytesIO(data)
    input_zip = zipfile.ZipFile(input_zip)
    assert len(input_zip.namelist()) == 1
    zipped_filename = input_zip.namelist()[0]
    assert re.match(r'^report_\d+_\d+\.csv', zipped_filename)
    report_text = input_zip.read(zipped_filename).decode('utf8')
    utils.assert_contains([
        'Сотрудник;Логин;Тип группы;Должность;Отдел;Система;Роль;Код роли;Доп. данные;Состояние;Выдана;Подтвердили',
        'Легионер Тит;fantom;-;;Арда;Simple система;Роль: Админ;"{""role"": ""admin""}";;Выдана;'
    ], report_text)

    response = client.json.post(roles_url, {'user': 'admin', 'format': 'xls'})
    assert response.status_code == 200
    assert response.json() == {'message': 'Отчёт формируется и будет отправлен вам на почту по завершении'}
    assert len(mail.outbox) == 2

    response = client.json.post(roles_url, {'state': 'granted', 'format': 'doc'})
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'format': ['Выберите корректный вариант. doc нет среди допустимых значений.']
        }
    }

    response = client.json.post(roles_url, {'state': 'non-existent', 'format': 'csv'})
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'state': ['Выберите корректный вариант. non-existent нет среди допустимых значений.']
        }
    }


@patch('xlwt.ExcelMagic.MAX_ROW', 1)
def test_report_lot_of_roles(client, simple_system, users_for_test, roles_url, settings):
    (art, fantom, terran, admin) = users_for_test
    utils.raw_make_role(art, simple_system, {'role': 'admin'}, state='granted')
    utils.raw_make_role(art, simple_system, {'role': 'manager'}, state='requested')

    client.login('art')

    utils.clear_mailbox()
    data = client.json.post(roles_url, {'format': 'xls'}).json()
    assert data == {
        'message': 'Отчёт формируется и будет отправлен вам на почту по завершении'
    }
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['art@example.yandex.ru']
    assert message.subject == 'Ошибка формирования отчёта.'
    utils.assert_contains([
        'Добрый день',
        'Количество строк в отчёте больше ограничения xls файла',
        'Ваш IDM'
    ], message.body)

    utils.clear_mailbox()
    settings.IDM_REPORTS_CSV_MAX_ROWS = 1
    data = client.json.post(roles_url, {'format': 'csv'}).json()
    assert data == {
        'message': 'Отчёт формируется и будет отправлен вам на почту по завершении'
    }
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['art@example.yandex.ru']
    assert message.subject == 'Ошибка формирования отчёта.'
    utils.assert_contains([
        'Добрый день',
        'Количество строк в отчёте больше максимально допустимого',
        'Ваш IDM'
    ], message.body)


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_get_failed_role(arda_users, department_structure, simple_system, client, api_name):
    frodo = arda_users.frodo
    boromir = arda_users.boromir
    group = department_structure.fellowship
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    group_role = Role.objects.request_role(frodo, group, simple_system, None, {'role': 'superuser'}, None)
    personal_role = boromir.roles.get(parent=group_role)
    personal_role.set_raw_state(ROLE_STATE.FAILED)

    def assert_can_be_poked(login, role, value, status_code):
        client.login(login)
        response = client.json.get(get_role_url(role.pk))
        data = response.json()
        assert response.status_code == status_code
        if data.get('permissions') and data.get('permissions').get('can_be_poked_if_failed'):
            assert data['permissions']['can_be_poked_if_failed'] == value

    # пользователь failed роли может ее довыдать
    assert_can_be_poked('boromir', personal_role, True, 200)
    # ответственный группы может довыдать персональную роль, выданную по групповой
    assert_can_be_poked('frodo', personal_role, True, 200)
    # ответственный группы может довыдать все персональные роли, выданные по групповой
    assert_can_be_poked('frodo', group_role, True, 200)
    # не пользователь роли и не ответственный группы не может довыдать роль
    assert_can_be_poked('aragorn', personal_role, False, 404)
    assert_can_be_poked('aragorn', group_role, False, 200)
    assert_can_be_poked('boromir', group_role, False, 200)

    # не failed роль и групповая роль без failed персональных ролей не могут быть довыданы
    granted_group_role = Role.objects.request_role(frodo, group, simple_system, None, {'role': 'manager'}, None)
    granted_personal_role = boromir.roles.get(parent=granted_group_role)
    assert_can_be_poked('boromir', granted_personal_role, False, 200)
    assert_can_be_poked('boromir', granted_group_role, False, 200)
    assert_can_be_poked('frodo', granted_group_role, False, 200)
    assert_can_be_poked('frodo', granted_group_role, False, 200)


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])  # FIXME убрать v1 из тестов frontend
@pytest.mark.parametrize('review_days', [None, 1])
def test_get_role(client, simple_system, more_users_for_test, roles_url, api_name, review_days):
    """
    GET /frontend/roles/<role_id>/
    """
    (art, fantom, terran, zerg, protoss, admin) = more_users_for_test

    # role granted to art
    utils.add_perms_by_role('responsible', fantom, simple_system)
    review_date = now() + timedelta(review_days if review_days else simple_system.roles_review_days)
    superuser_role = Role.objects.request_role(fantom, art, simple_system, None, {'role': 'superuser'},
                                               None, review_date=review_date)

    # role owner can see it
    client.login('art')
    data = client.json.get(get_role_url(superuser_role.pk, api_name=api_name)).json()
    expected_keys = {
        'ref_count', 'permissions',
        'human_state', 'human', 'human_short',
        'id', 'group', 'system',
        'state', 'fields_data', 'ttl_date', 'system_specific', 'is_active', 'is_public', 'data', 'ttl_days',
        'review_date', 'updated', 'added', 'granted_at', 'review_at', 'expire_at', 'review_days',
        'node', 'parent', 'user', 'role_request', 'with_inheritance', 'with_robots', 'with_external',
        'with_inheritance', 'with_robots', 'with_external', 'without_hold'
    }
    if api_name == 'frontend':
        expected_keys.add('personal_granted_at')
    assert set(data) == expected_keys

    assert data['data'] == {'role': 'superuser'}
    assert data['user']['username'] == 'art'
    assert data['state'] == 'granted'
    assert data['is_active'] is True
    assert data['system']['slug'] == 'simple'
    assert data['fields_data'] is None
    assert data['system_specific'] is None
    assert data['permissions'] == {
        'can_be_deprived': True,  # владелец роли может её отозвать
        'can_be_approved': False,  # роль уже выдана
        'can_be_rerequested': False,
        'can_be_poked_if_failed': False,
    }
    if review_days is None:
        review_at = now() + timedelta(simple_system.roles_review_days)
    else:
        review_at = parse_datetime(data['review_at'])
    utils.compare_time(review_at, review_date, epsilon=3)

    # check review for temporary role
    superuser_role = utils.refresh(superuser_role)
    utils.patch_role(superuser_role, expire_at=now() + timedelta(2))
    data = client.json.get(get_role_url(superuser_role.pk, api_name=api_name)).json()
    if review_days is None:
        review_at = now() + timedelta(simple_system.roles_review_days)
    else:
        review_at = parse_datetime(data['review_at'])
    utils.compare_time(review_at, review_date, epsilon=3)

    utils.patch_role(superuser_role, expire_at=now() + timedelta(settings.IDM_DEFAULT_REVIEW_ROLES_DAYS * 2))
    data = client.json.get(get_role_url(superuser_role.pk, api_name=api_name)).json()
    if review_days is None:
        review_at = now() + timedelta(simple_system.roles_review_days)
    else:
        review_at = parse_datetime(data['review_at'])
    utils.compare_time(review_at, review_date, epsilon=3)

    # requester could see the roles they requested
    utils.remove_perms_by_role('responsible', fantom, simple_system)
    client.login('fantom')
    response = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=superuser_role.id),
    )

    assert response.status_code == 200

    # simple user has no rights to see it
    client.login('terran')
    response = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=superuser_role.id),
    )

    assert response.status_code == 404

    # user with "view_roles" permission can see it allright, developer for example
    utils.add_perms_by_role('developer', terran)
    data = client.json.get(get_role_url(superuser_role.pk)).json()

    assert data['data'] == {'role': 'superuser'}
    assert data['user']['username'] == 'art'
    assert data['state'] == 'granted'
    assert data['is_active'] is True
    assert data['system']['slug'] == 'simple'
    assert data['fields_data'] is None
    assert data['system_specific'] is None
    assert data['permissions'] == {
        'can_be_deprived': False,  # user with "view_roles" permission can not deprive role
        'can_be_rerequested': False,  # нельзя перезапросить чужую роль
        'can_be_approved': False,  # нельзя подтвердить чужую роль
        'can_be_poked_if_failed': False,
    }

    # user with "<system>.view_roles" permission can see it allright too, "users_view" role holder for example
    utils.add_perms_by_role('users_view', zerg, system=simple_system)
    client.login('zerg')
    data = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=superuser_role.id)
    ).json()

    assert data['data'] == {'role': 'superuser'}
    assert data['user']['username'] == 'art'
    assert data['state'] == 'granted'
    assert data['is_active'] is True
    assert data['system']['slug'] == 'simple'
    assert data['fields_data'] is None
    assert data['system_specific'] is None
    assert data['permissions'] == {
        'can_be_deprived': True,
        'can_be_rerequested': False,
        'can_be_approved': False,
        'can_be_poked_if_failed': False,
    }

    # проверим, что для роли в состоянии need_request can_be_rerequested=True
    superuser_role = utils.refresh(superuser_role)
    superuser_role.set_state('need_request')
    client.login('art')
    data = client.json.get(get_role_url(superuser_role.pk)).json()
    assert data['permissions'] == {
        'can_be_deprived': True,
        'can_be_rerequested': True,
        'can_be_approved': False,
        'can_be_poked_if_failed': False,
    }

    # проверим, что роль с запросом в состоянии requested отдаётся без ошибок
    client.login('fantom')
    utils.set_workflow(simple_system, 'approvers = ["terran"]')
    manager_role = Role.objects.request_role(fantom, fantom, simple_system, '', {'role': 'manager'}, None)
    data = client.json.get(get_role_url(manager_role.pk)).json()
    assert data['data'] == {'role': 'manager'}
    assert data['require_approve_from'] == [[{'username': 'terran', 'full_name': 'Легат Аврелий'}]]
    request = manager_role.get_last_request()
    assert data['role_request']['id'] == request.id
    assert data['role_request']['requester']['username'] == 'fantom'
    assert len(data['role_request']['approves']) == 1

    expire_at = parse_datetime(data['expire_at'])
    utils.compare_time(expire_at, manager_role.expire_at)
    assert data['state'] == 'requested'
    assert data['permissions'] == {
        'can_be_deprived': True,
        'can_be_rerequested': True,
        'can_be_approved': False,
        'can_be_poked_if_failed': False,
    }
    # если роль запрашивается от лица аппрувера, то can_be_approved=True
    client.login('terran')
    data = client.json.get(get_role_url(manager_role.pk)).json()
    assert data['approve_request']['id'] == manager_role.get_last_request().approves.all()[0].requests.all()[0].id

    assert data['permissions'] == {
        'can_be_deprived': False,
        'can_be_rerequested': False,
        'can_be_approved': True,
        'can_be_poked_if_failed': False,
    }

    # проверим, что роль без запросов в состоянии requested отдаётся без ошибок
    poweruser_role = utils.raw_make_role(fantom, simple_system, {'role': 'poweruser'}, state='requested')
    data = client.json.get(get_role_url(poweruser_role.pk)).json()
    assert data['data'] == {'role': 'poweruser'}
    assert data['permissions'] == {
        'can_be_deprived': False,
        'can_be_rerequested': False,
        'can_be_approved': False,
        'can_be_poked_if_failed': False,
    }

    # у скрытой роли нет review_at
    hidden_role = Role.objects.request_role(fantom, fantom, simple_system, None, {'role': 'superuser'}, None)
    hidden_role.is_public = False
    hidden_role.save(update_fields=['is_public'])
    data = client.json.get(get_role_url(hidden_role.pk)).json()
    assert data['review_at'] is None


def test_get_role_for_tvm_app(client, simple_system, arda_users):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    tvm_app = arda_users.tvm_app
    client.login(legolas.username)

    simple_system.use_tvm_role = True
    simple_system.save(update_fields=['use_tvm_role'])

    tvm_service = Group.objects.create(type=GROUP_TYPES.TVM_SERVICE, external_id=123456)
    GroupMembership.objects.create(group=tvm_service, user=tvm_app, state='active', is_direct=True)
    GroupResponsibility.objects.create(group=tvm_service, user=frodo, is_active=True)
    responsibility = GroupResponsibility.objects.create(group=tvm_service, user=legolas, is_active=True)
    superuser_role = Role.objects.request_role(frodo, tvm_app, simple_system, None, {'role': 'superuser'})

    response = client.json.get(get_role_url(superuser_role.pk))
    assert response.status_code == 200
    assert response.json()['id'] == superuser_role.pk

    responsibility.is_active = False
    responsibility.save(update_fields=['is_active'])
    response = client.json.get(get_role_url(superuser_role.pk))
    assert response.status_code == 404


def test_require_approve_from(client, simple_system, arda_users):
    """Проверим логику поля require_approve_from"""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    utils.set_workflow(simple_system, "approvers = [approver('legolas') | 'gandalf',"
                                      "approver('gimli') | 'sam', "
                                      "approver('saruman') | 'gandalf']")

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = utils.refresh(role)
    assert role.state == 'requested'
    client.login('frodo')
    data = client.json.get(get_role_url(role.pk)).json()
    assert_approvers_deprecated(data, [['legolas', 'gandalf'], ['gimli', 'sam'], ['saruman', 'gandalf']])
    utils.assert_approvers(data['role_request'], [['legolas', 'gandalf'], ['gimli', 'sam'], ['saruman', 'gandalf']],
                           format='full')
    # если gandalf подтвердит свой ApproveRequest, соответствующая OR-группа должна пропасть из ответа ручки
    ApproveRequest.objects.filter(approver=gandalf).select_related_for_set_decided().first().set_approved(gandalf)
    data = client.json.get(get_role_url(role.pk)).json()
    assert_approvers_deprecated(data, [['gimli', 'sam']])
    utils.assert_approvers(data['role_request'], [['legolas', 'gandalf'], ['gimli', 'sam'], ['saruman', 'gandalf']],
                           format='full')
    role = utils.refresh(role)
    assert role.state == 'requested'


def test_get_role_lang(client, simple_system, more_users_for_test, roles_url):
    """
    GET /frontend/roles/<role_id>/
    """
    (art, fantom, terran, zerg, protoss, admin) = more_users_for_test
    superuser_role = Role.objects.request_role(admin, art, simple_system, None, {'role': 'superuser'}, None)

    client.login('art')
    data = client.json.get(get_role_url(superuser_role.pk)).json()
    assert data['state'] == 'granted'
    assert data['human_state'] == 'Выдана'

    with patch('idm.framework.middleware.get_language_from_user'):
        data = client.json.get(get_role_url(superuser_role.pk), HTTP_ACCEPT_LANGUAGE='en').json()
        assert data['human_state'] == 'Granted'

    client.login('art', lang='en')
    data = client.json.get(get_role_url(superuser_role.pk)).json()
    assert data['state'] == 'granted'
    assert data['human_state'] == 'Granted'


def test_get_role_shows_role_can_be_deprived(client, simple_system, users_for_test, depriver_users):
    """
    GET /frontend/roles/<role_id>/
    """
    art, fantom = users_for_test[:2]
    utils.add_perms_by_role('responsible', fantom, simple_system)

    superuser_role = Role.objects.request_role(fantom, art, simple_system, None, {'role': 'superuser'}, None)

    for depriver, can_deprive in depriver_users:
        client.login(depriver.username)
        if not can_deprive:
            utils.add_perms_by_role('viewer', depriver)
        data = client.json.get(get_role_url(superuser_role.pk)).json()
        assert data['data'] == {'role': 'superuser'}
        assert data['user']['username'] == 'art'
        assert data['state'] == 'granted'
        assert data['is_active'] is True
        assert data['system']['slug'] == 'simple'
        assert data['fields_data'] is None
        assert data['system_specific'] is None
        assert data['permissions']['can_be_deprived'] is can_deprive


def test_deprive_role(client, simple_system, users_for_test, depriver_users):
    """
    DELETE /frontend/roles/<role_id>/
    """
    art, fantom = users_for_test[:2]

    utils.add_perms_by_role('responsible', fantom, simple_system)
    for depriver, can_deprive in depriver_users:
        superuser_role = Role.objects.request_role(fantom, art, simple_system, None, {'role': 'superuser'}, None)

        client.login(depriver.username)
        deprive_url = get_role_url(superuser_role.pk)
        if can_deprive:
            response = client.delete(deprive_url)
            superuser_role = utils.refresh(superuser_role)
            assert response.status_code == 204
            assert superuser_role.state == 'deprived'
            assert superuser_role.actions.filter(action='deprive').exists() is True
        else:
            response = client.delete(deprive_url)
            assert response.status_code == 404
            superuser_role = utils.refresh(superuser_role)
            assert superuser_role.state == 'granted'
            assert not superuser_role.actions.filter(action='deprive').exists()
        superuser_role.delete()


def test_deprive_role_with_comment(client, simple_system, users_for_test, depriver_users, idm_robot):
    """
    DELETE /frontend/roles/<role_id>/
    with comment
    """
    art, fantom = users_for_test[:2]

    utils.add_perms_by_role('responsible', fantom, simple_system)
    comment = 'Отзываю ненужную роль'
    for depriver, can_deprive in depriver_users:
        superuser_role = Role.objects.request_role(fantom, art, simple_system, None, {'role': 'superuser'}, None)
        utils.clear_mailbox()

        client.login(depriver.username)
        deprive_url = get_role_url(superuser_role.pk)
        if can_deprive:
            response = client.json.delete(deprive_url, {'comment': comment})
            superuser_role = utils.refresh(superuser_role)
            assert response.status_code == 204
            assert superuser_role.state == 'deprived'
            assert superuser_role.actions.filter(action='deprive').exists()
            assert len(mail.outbox) == 1
            assert mail.outbox[0].to == ['art@example.yandex.ru']
            assert comment in mail.outbox[0].body
            history = client.json.get(
                reverse('api_dispatch_list', api_name='frontend', resource_name='actions'),
                data={
                    'role': superuser_role.pk,
                }
            ).json()
            assert history['objects'][0]['action'] == 'remove'
            assert history['objects'][0]['comment'] == 'Роль удалена из системы.'
            assert history['objects'][2]['action'] == 'deprive'
            assert history['objects'][2]['comment'] == comment
        else:
            response = client.json.delete(deprive_url, {'comment': comment})
            assert response.status_code == 404
            superuser_role = utils.refresh(superuser_role)
            assert superuser_role.state == 'granted'
            assert not superuser_role.actions.filter(action='deprive').exists()
            assert len(mail.outbox) == 0
        superuser_role.delete()


@pytest.mark.parametrize(
    'validate_depriving,target_state',
    ((True, ROLE_STATE.DEPRIVING_VALIDATION), (False, ROLE_STATE.DEPRIVED))
)
def test_deprive_with_validation(client, simple_system, arda_users, idm_robot, validate_depriving, target_state):
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'manager'})
    assert role.state == ROLE_STATE.GRANTED

    client.login(arda_users.gandalf)
    deprive_url = get_role_url(role.pk)
    with waffle.testutils.override_switch('idm.deprive_not_immediately', active=True):
        response = client.json.delete(deprive_url, {'validate_depriving': validate_depriving})

    assert response.status_code == 204

    role.refresh_from_db()
    assert role.state == target_state


def test_mass_deprive(client, simple_system, arda_users, arda_users_with_roles, idm_robot, roles_url):
    """
    DELETE /frontend/roles/
    """

    frodo = arda_users.frodo
    # Попробуем отозвать все роли frodo. Их три: две обычных, одна по групповой
    # Поэтому две должно получиться отозвать, а третью – нет
    role1, role2 = sorted(arda_users_with_roles.frodo, key=operator.attrgetter('pk'))
    ref_role = arda_users_with_roles.fellowship[0].refs.get(user=frodo)
    legolas_role = arda_users_with_roles.legolas[0]
    legolas_ref_role = arda_users_with_roles.fellowship[0].refs.get(user=arda_users.legolas)
    gandalf_role = arda_users_with_roles.gandalf[0]
    gandalf_ref_role = arda_users_with_roles.fellowship[0].refs.get(user=arda_users.gandalf)
    client.login('frodo')
    response = client.json.delete(roles_url, {'user': 'frodo', 'comment': 'depriving frodo role'})
    assert response.status_code == 200

    assert response.json() == {
        'errors': 1,
        'successes': 2,
        'successes_ids': [
            {
                'id': role1.pk
            },
            {
                'id': role2.pk
            }
        ],
        'errors_ids': [{
            'id': ref_role.pk,
            'message': 'Нельзя отозвать роль, имеющую родительскую роль, если она не отложена',
        }]
    }
    assert frodo.roles.filter(state='deprived').count() == 2
    assert frodo.roles.filter(state='granted').count() == 1

    # Теперь перелогинимся под legolas и попробуем отозвать роли gandalf
    # Однако эти роли не видны legolas, поэтому получаем 0 успехов и 0 ошибок
    client.login('legolas')
    response = client.json.delete(roles_url, {'user': 'gandalf'})
    assert response.json() == {'errors': 0, 'successes': 0, 'successes_ids': [], 'errors_ids': []}

    # свои роли при этом отзываются нормально (одна роль - по групповой и отозваться не может)
    response = client.json.delete(roles_url, {'user': 'legolas'})
    assert response.json() == {
        'errors': 1,
        'successes': 1,
        'successes_ids': [
            {
                'id': legolas_role.pk,
            }
        ],
        'errors_ids': [
            {
                'id': legolas_ref_role.pk,
                'message': 'Нельзя отозвать роль, имеющую родительскую роль, если она не отложена',
            }
        ]
    }

    # поменяем политику системы на 'anyone' и убедимся, что даже если роль видно, права помешают её отозвать:
    simple_system.request_policy = 'anyone'
    simple_system.save()

    response = client.json.delete(roles_url, {'user': 'gandalf'})
    expected = {
        'errors': 2,
        'successes': 0,
        'successes_ids': [],
        'errors_ids': sorted([
            {
                'id': gandalf_role.pk,
                'message': 'Роль находится в состоянии Отозвана, из которого отзыв невозможен',
            },
            {
                'id': gandalf_ref_role.pk,
                'message': 'Нельзя отозвать роль, имеющую родительскую роль, если она не отложена',
            }
        ], key=operator.itemgetter('id'))
    }
    assert response.json() == expected


def test_mass_deprive_fails(client, simple_system, arda_users, arda_users_with_roles, idm_robot, roles_url):
    """
    DELETE /frontend/roles/
    """

    client.login('frodo')
    response = client.json.delete(roles_url)
    # хотя бы один фильтр должен быть представлен
    assert response.status_code == 400
    assert response.json() == {
        'message': 'Пожалуйста, укажите хотя бы один фильтрационный параметр',
        'error_code': 'BAD_REQUEST',
    }

    # комментарий фильтром не считается
    response = client.json.delete(roles_url, {'comment': 'А вот что, если?'})
    assert response.status_code == 400
    assert response.json() == {
        'message': 'Пожалуйста, укажите хотя бы один фильтрационный параметр',
        'error_code': 'BAD_REQUEST',
    }

    # можно указывать параметры и в querystring, и в body
    response = client.json.delete(roles_url, data={'comment': 'А вот что, если?'}, query={
        'user': 'frodo',
        'type': 'active',
        'parent_type': 'absent',
    })
    assert response.status_code == 200
    frodo_role1, frodo_role2 = arda_users_with_roles.frodo
    expected = {
        'errors': 0,
        'successes': 2,
        'errors_ids': [],
        'successes_ids': sorted([{'id': frodo_role1.pk}, {'id': frodo_role2.pk}], key=operator.itemgetter('id')),
    }
    assert response.json() == expected

    assert Role.objects.filter(state='deprived').count() == 3  # роль gandalf уже была отозвана
    role1, role2 = arda_users_with_roles.frodo
    role1.refresh_from_db()
    assert role1.actions.get(action='deprive').comment == 'А вот что, если?'

    # если параметр одновременно указан в querystring и в body, то приоритет имеет body
    response = client.json.delete(roles_url, data={
        'type': 'active',
        'ownership': 'group',
        'comment': 'commentbody'
    }, query={
        'comment': 'commentquery'
    })
    assert response.status_code == 200
    expected = {
        'successes': 1,
        'errors': 0,
        'successes_ids': [{'id': arda_users_with_roles.fellowship[0].pk}],
        'errors_ids': [],
    }
    assert response.json() == expected
    group_role = Role.objects.get(group__isnull=False)
    assert group_role.actions.get(action='deprive').comment == 'commentbody'


def test_mass_deprive_with_comment(client, simple_system, arda_users, depriver_users, idm_robot, roles_url):
    """
    DELETE /frontend/roles/
    """
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    comment = 'Отзываю ненужную роль'

    for depriver, can_deprive in depriver_users:
        role = Role.objects.request_role(frodo, legolas, simple_system, None, {'role': 'superuser'}, None)
        utils.clear_mailbox()

        client.login(depriver.username)
        if can_deprive:
            response = client.json.delete(roles_url, {'user': 'legolas', 'comment': comment})
            role = utils.refresh(role)
            assert response.status_code == 200
            expected = {
                'errors': 0,
                'successes': 1,
                'errors_ids': [],
                'successes_ids': [{'id': role.pk}],
            }
            assert response.json() == expected

            assert role.state == 'deprived'
            assert role.actions.filter(action='deprive').exists()
            assert len(mail.outbox) == 1
            # письмо владельцу
            assert mail.outbox[0].to == ['legolas@example.yandex.ru']
            assert comment in mail.outbox[0].body
            history = client.json.get(
                reverse('api_dispatch_list', api_name='frontend', resource_name='actions'),
                data={
                    'role': role.pk,
                }
            ).json()
            assert history['objects'][0]['action'] == 'remove'
            assert history['objects'][0]['comment'] == 'Роль удалена из системы.'
            assert history['objects'][2]['action'] == 'deprive'
            assert history['objects'][2]['comment'] == comment
        else:
            response = client.json.delete(roles_url, {'user': 'legolas', 'comment': comment})
            assert response.status_code == 200
            assert response.json() == {
                'errors': 0,
                'successes': 0,
                'errors_ids': [],
                'successes_ids': [],
            }
            role = utils.refresh(role)
            assert role.state == 'granted'
            assert not role.actions.filter(action='deprive').exists()
            assert len(mail.outbox) == 0
        role.delete()


def test_filter_roles_by_nodeset(client, arda_users, complex_system_with_nodesets, roles_url):
    """Проверим фильтрацию ролей по группам узлов"""

    system = complex_system_with_nodesets
    frodo = arda_users.frodo
    utils.raw_make_role(frodo, system, {'project': 'subs', 'role': 'developer'}, state='granted')
    utils.raw_make_role(frodo, system, {'project': 'rules', 'role': 'developer'}, state='granted')
    utils.raw_make_role(frodo, system, {'project': 'subs', 'role': 'manager'}, state='granted')
    client.login('frodo')

    data = client.json.get(roles_url, {'system': system.slug, 'users': 'frodo', 'nodeset': 'developer_id'}).json()
    assert data['meta']['total_count'] == 2
    expected = {'Подписки / Разработчик', 'IDM / Разработчик'}
    assert {item['human_short'] for item in data['objects']} == expected

    data = client.json.get(roles_url, {'system': system.slug, 'users': 'frodo', 'nodeset': 'manager_id'}).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['human_short'] == 'Подписки / Менеджер'

    data = client.json.get(roles_url, {
        'system': system.slug,
        'users': 'frodo',
        'nodeset': 'developer_id,manager_id'
    }).json()
    assert data['meta']['total_count'] == 3


def test_public_roles(client, arda_users, simple_system, roles_url):
    """Проверим, что скрытые роли не отдаются в списке ролей"""

    frodo = arda_users.frodo
    role = utils.raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')

    client.login('frodo')
    data = client.json.get(roles_url).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['id'] == role.id

    role.node.is_public = False
    role.node.save(update_fields=['is_public'])
    data = client.json.get(roles_url).json()
    assert len(data['objects']) == 0


def test_filter_roles_by_internal_role(client, arda_users, complex_system, roles_url):
    """Проверим фильтрацию ролей по внутренней роли"""

    system = complex_system
    frodo = arda_users.frodo
    utils.raw_make_role(frodo, system, {'project': 'subs', 'role': 'developer'}, state='granted')
    utils.raw_make_role(frodo, system, {'project': 'rules', 'role': 'admin'}, state='granted')
    utils.raw_make_role(frodo, system, {'project': 'subs', 'role': 'manager'}, state='granted')

    utils.add_perms_by_role('roles_manage', frodo, system, '/subs/')

    client.login('frodo')
    data = client.json.get(roles_url, {'system': system.slug, 'users': 'frodo', 'internal_role': 'roles_manage'}).json()

    assert data['meta']['total_count'] == 2
    expected = {'Подписки / Разработчик', 'Подписки / Менеджер'}
    assert {item['human_short'] for item in data['objects']} == expected


def test_filter_roles_by_abc_slug(client, simple_service_groups, arda_users, simple_system, roles_url):
    """
    Проверим фильтрацию ролей по сервису, ожидаем, что
    выводятся роли сервиса и всех его скоупов
    """

    frodo = arda_users.frodo

    simple_system.request_policy = 'anyone'
    simple_system.save(update_fields=['request_policy'])

    utils.set_workflow(simple_system, group_code='approvers=[]')
    service_group = simple_service_groups['svc_group']
    service_scope_group = simple_service_groups['svc_group_scope']
    another_group = Group.objects.create(
        type='service',
        slug='svc_another_group',
        name='svc_another_group',
        external_id=999,
    )

    Role.objects.request_role(frodo, service_group, simple_system, None, {'role': 'superuser'}, None)
    Role.objects.request_role(frodo, service_scope_group, simple_system, None, {'role': 'manager'}, None)
    Role.objects.request_role(frodo, another_group, simple_system, None, {'role': 'manager'}, None)

    client.login('frodo')
    data = client.json.get(roles_url, {'abc_slug': 'group'}).json()
    assert data['meta']['total_count'] == 2
    assert {role['group']['slug'] for role in data['objects']} == {'svc_group', 'svc_group_scope'}


def test_filter_roles_by_keyword_without_suggest(client, arda_users, complex_system, roles_url):
    """Проверим фильтрацию ролей по ключевому слову"""

    system = complex_system
    frodo = arda_users.frodo
    developer = RoleNode.objects.get(system=complex_system, slug='developer')

    RoleAlias.objects.create(node=developer, type=RoleAlias.DEFAULT_ALIAS, name='Синоним', name_en='Synonym')
    RoleAlias.objects.create(node=developer, type=RoleAlias.DEFAULT_ALIAS, name='Псевдоним', name_en='Pseudonym',
                             is_active=False)
    RoleAlias.objects.create(node=developer, type=RoleAlias.DEFAULT_ALIAS, name='Nymph', name_en='Nymph',
                             is_active=True)
    utils.raw_make_role(frodo, system, {'project': 'subs', 'role': 'developer'}, state='granted')
    utils.raw_make_role(frodo, system, {'project': 'rules', 'role': 'admin'}, state='granted')
    utils.raw_make_role(frodo, system, {'project': 'subs', 'role': 'manager'}, state='granted')

    client.login('frodo')

    with utils.use_proxied_suggest(should_use=False):
        # dev
        data = client.json.get(roles_url, {
            'system': system.slug,
            'users': 'frodo',
            'path': '/subs/',
            'role__contains': 'dev'
        }).json()

        assert data['meta']['total_count'] == 1
        expected = {'Подписки / Разработчик'}
        assert {item['human_short'] for item in data['objects']} == expected

        # syn
        response = client.json.get(roles_url,
                                   {'system': system.slug, 'users': 'frodo', 'path': '/subs/', 'role__contains': 'syn'})
        data = response.json()
        assert data['meta']['total_count'] == 1
        assert {item['human_short'] for item in data['objects']} == {'Подписки / Разработчик'}

        # pseudonym – неактивный, 0 результатов
        data = client.json.get(roles_url, {
            'system': system.slug,
            'users': 'frodo',
            'path': '/subs/',
            'role__contains': 'pse'
        }).json()
        assert data['meta']['total_count'] == 0

        # сино – ищем в name
        data = client.json.get(roles_url, {
            'system': system.slug,
            'users': 'frodo',
            'path': '/subs/',
            'role__contains': 'Син'
        }).json()
        assert data['meta']['total_count'] == 1

        # ищем по внукам, но в результат должны попасть и роли, выданные на потомков этих внуков
        data = client.json.get(roles_url, {
            'system': system.slug,
            'users': 'frodo',
            'path': '/',
            'role__contains': 'su'
        }).json()
        assert data['meta']['total_count'] == 2
        assert {item['human_short'] for item in data['objects']} == {'Подписки / Разработчик', 'Подписки / Менеджер'}


def test_filter_roles_by_keyword_with_suggest(pt1_system, client, arda_users, roles_url, intrasearch_objects):
    frodo = arda_users.frodo

    client.login('frodo')

    utils.raw_make_role(frodo, pt1_system, {'project': 'proj1', 'role': 'doc'}, state='granted')
    utils.raw_make_role(frodo, pt1_system, {'project': 'proj1', 'role': 'admin'}, state='granted')
    utils.raw_make_role(frodo, pt1_system, {'project': 'proj1', 'role': 'manager'}, state='granted')

    with utils.use_intrasearch_for_roles(should_use=True):
        with utils.mock_ids_repo('intrasearch', 'idm_rolenodes', intrasearch_objects) as repo:
            data = client.json.get(
                roles_url,
                {
                    'system': pt1_system.slug,
                    'users': 'frodo',
                    'path': '/',
                    'role__contains': 'роект'
                }).json()

            assert data['meta']['total_count'] == 3
            assert {item['human_short'] for item in data['objects']} == {
                'Проект 1 / Менеджер',
                'Проект 1 / Тех писатель',
                'Проект 1 / Админ',
            }

            assert repo.get.call_args_list == [
                call({
                    'layers': 'idm_rolenodes',
                    'language': 'ru',
                    'allow_empty': True,
                    'idm_rolenodes.per_page': 100,
                    'text': 'роект',
                    'idm_rolenodes.page': None,
                    'idm_rolenodes.query': 's_parent_path:"/test1/"'
                })
            ]


@pytest.fixture
def role_for_rerequest(simple_system, arda_users):
    frodo = arda_users.frodo
    gimli = arda_users.gimli
    role = Role.objects.request_role(frodo, gimli, simple_system, '', {'role': 'admin'}, None)
    role = utils.refresh(role)
    return role


API_NAMES = ('frontend', 'v1')


@pytest.mark.parametrize('api_name', API_NAMES)
def test_transitions_url_methods(client, role_for_rerequest, api_name):
    """
    OPTIONS /frontend/roles/<role_id>/
    """
    client.login('gimli')
    transition_url = reverse('api_dispatch_detail', api_name=api_name, pk=role_for_rerequest.pk, resource_name='roles')
    response = client.options(transition_url)
    assert response.status_code == 200
    assert response['Allow'] == 'GET,POST,DELETE'


@pytest.mark.parametrize('api_name', API_NAMES)
def test_transitions_invalid_parameters(client, role_for_rerequest, api_name):
    """
    POST /frontend/roles/<role_id>/
    """
    client.login('gimli')
    max_pk = Role.objects.all().aggregate(Max('id'))['id__max']
    transition_url = reverse('api_dispatch_detail', api_name=api_name, pk=max_pk + 1, resource_name='roles')
    response = client.json.post(transition_url, {'state': 'rerequested'})
    assert response.status_code == 404
    transition_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='roles',
                             pk=role_for_rerequest.pk)
    response = client.json.post(transition_url, {})
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'state': ['Обязательное поле.']
        }
    }
    response = client.json.post(transition_url, {'state': 'deprived'})
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'state': ['Невозможно перевести роль в статус "deprived"']
        }
    }


@pytest.mark.parametrize('api_name', API_NAMES)
def test_rerequest_role(client, role_for_rerequest, api_name):
    """
    POST /frontend/roles/<role_id>/
    """
    client.login('gimli')
    errordict = {
        'error_code': 'FORBIDDEN',
        'message': 'Перезапрос роли невозможен: Нельзя перезапросить роль в состоянии "Выдана"',
    }
    transition_url = reverse('api_dispatch_detail', api_name=api_name, pk=role_for_rerequest.pk, resource_name='roles')
    response = client.json.post(
        transition_url,
        {'state': 'rerequested'}
    )
    assert response.status_code == 403
    assert response.json() == errordict
    role_for_rerequest.system.fetch_actual_workflow()
    utils.set_workflow(role_for_rerequest.system, 'approvers = ["gandalf"]')
    role_for_rerequest.set_state('need_request')
    response = client.json.post(
        transition_url,
        data={'state': 'rerequested'}
    )
    assert response.status_code == 202
    role = utils.refresh(role_for_rerequest)
    assert role.state == 'rerequested'


@pytest.mark.parametrize('api_name', API_NAMES)
def test_rerequest_change_ttl_days(client, role_for_rerequest, api_name):
    """
    POST /frontend/roles/<role_id>/
    """
    client.login('gimli')
    transition_url = reverse('api_dispatch_detail', api_name=api_name, pk=role_for_rerequest.pk, resource_name='roles')
    role_for_rerequest.set_state('expiring')
    response = client.json.post(
        transition_url,
        data={'state': 'rerequested', 'ttl_days': 123}
    )
    assert response.status_code == 202
    role = utils.refresh(role_for_rerequest)
    assert role.ttl_days == 123


@pytest.mark.parametrize('api_name', API_NAMES)
def test_rerequest_workflow_with_error(client, role_for_rerequest, api_name):
    """
    POST /frontend/roles/<role_id>/
    """
    client.login('gimli')
    role_for_rerequest.system.fetch_actual_workflow()
    utils.set_workflow(role_for_rerequest.system, 'approvers = ["unknownperson"]')
    transition_url = reverse('api_dispatch_detail', api_name=api_name, pk=role_for_rerequest.pk, resource_name='roles')
    role_for_rerequest.set_state('need_request')
    response = client.json.post(
        transition_url,
        data={'state': 'rerequested'}
    )
    assert response.status_code == 409
    assert response.json() == {
        'error_code': 'CONFLICT',
        'message': "('Подтверждающие не найдены: unknownperson', 'unknownperson')",
    }


@pytest.mark.parametrize('api_name', API_NAMES)
def test_rerequest_role_with_comment(client, role_for_rerequest, idm_robot, api_name):
    """
    POST /frontend/roles/<role_id>/
    w/comment
    """
    comment = 'Хочу ещё съесть этих мягких французских булок, да выпить чаю'
    client.login('gimli')
    role_for_rerequest.system.fetch_actual_workflow()
    utils.set_workflow(role_for_rerequest.system, 'approvers = ["gandalf"]')
    role_for_rerequest.set_state('need_request')
    transition_url = reverse('api_dispatch_detail', api_name=api_name, pk=role_for_rerequest.pk,
                             resource_name='roles')
    response = client.json.post(
        transition_url,
        data={
            'state': 'rerequested',
            'comment': comment
        }
    )
    assert response.status_code == 202
    role = utils.refresh(role_for_rerequest)
    assert role.state == 'rerequested'
    history = client.json.get(
        reverse('api_dispatch_list', api_name='frontend', resource_name='actions'),
        {'role': role.pk},
    ).json()
    assert history['objects'][1]['action'] == 'rerequest'
    assert history['objects'][1]['comment'] == comment


@pytest.mark.parametrize('api_name', API_NAMES)
def test_rerequest_by_impersonator(client, role_for_rerequest, idm_robot, api_name, arda_users):
    """
    POST /{frontend|v1}/roles/<role_id>/
    """

    role_for_rerequest.system.fetch_actual_workflow()
    utils.set_workflow(role_for_rerequest.system, 'approvers = ["gandalf"]')
    utils.add_perms_by_role('impersonator', arda_users.manve, role_for_rerequest.system)
    client.login('manve')

    role_for_rerequest.set_state('need_request')
    transition_url = reverse('api_dispatch_detail', api_name=api_name, pk=role_for_rerequest.pk, resource_name='roles')
    response = client.json.post(transition_url, {
        '_requester': 'gimli',
        'state': 'rerequested',
    })
    if api_name == 'v1':
        assert response.status_code == 202
        role = utils.refresh(role_for_rerequest)
        assert role.state == 'rerequested'
        action = role.actions.get(action='rerequest')
        assert action.user_id == arda_users.gimli.id
    else:
        assert response.status_code == 404
        assert response.json() == {'message': 'No Role matches the given query.', 'error_code': 'NOT_FOUND'}
        # добавим manve прав на просмотр ролей (но не отзыв) и попробуем ещё раз
        utils.add_perms_by_role('viewer', arda_users.manve, role_for_rerequest.system)
        response = client.json.post(transition_url, {
            '_requester': 'gimli',
            'state': 'rerequested',
        })
        assert response.status_code == 403
        assert response.json() == {
            'error_code': 'FORBIDDEN',
            'message': 'Перезапрос роли невозможен: Недостаточно прав'
        }


def test_role_hold(client, simple_system, arda_users, department_structure, roles_url):
    """
    GET /frontend/roles/<role_id>
    """
    fellowship = department_structure.fellowship
    frodo = arda_users.get('frodo')
    utils.set_workflow(simple_system, group_code='approvers=[]')

    # role granted to fellowship
    Role.objects.request_role(frodo, fellowship, simple_system, None, {'role': 'superuser'}, None)
    role = Role.objects.get(user=frodo)
    role.set_raw_state('onhold')

    client.login('frodo')
    data = client.json.get(get_role_url(role.pk, 'frontend')).json()
    assert data['permissions']['can_be_deprived'] is True


@pytest.mark.parametrize('api_name', API_NAMES)
def test_rerequest_requested(client, api_name, simple_system, arda_users):
    """
    POST /{frontend|v1}/roles/<role_id>/
    """

    utils.set_workflow(simple_system, 'approvers = ["gandalf"]')
    client.login('frodo')
    frodo = arda_users.frodo

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = utils.refresh(role)

    transition_url = reverse('api_dispatch_detail', api_name=api_name, pk=role.pk, resource_name='roles')
    response = client.json.post(transition_url, {
        'state': 'requested',
    })

    assert response.status_code == 202
    role = utils.refresh(role)
    assert role.state == 'requested'
    assert role.requests.count() == 2


def test_sox_system(client, roles_url, simple_system, sox_other_system, arda_users):
    client.login('frodo')
    frodo = arda_users.frodo

    role_sox = utils.raw_make_role(frodo, sox_other_system, {'role': 'admin'}, state='granted')
    role_no_sox = utils.raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')

    # sox
    data = client.json.get(roles_url, {
        'sox': True
    }).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['id'] == role_sox.id

    # no sox
    data = client.json.get(roles_url, {
        'sox': False
    }).json()
    assert data['meta']['total_count'] == 1
    assert data['objects'][0]['id'] == role_no_sox.id

    # any sox
    data = client.json.get(roles_url).json()
    assert data['meta']['total_count'] == 2
    assert {obj['id'] for obj in data['objects']} == {role_sox.id, role_no_sox.id}


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_nested_user(client, simple_system, arda_users, api_name):
    """Проверим, что группы пользователя нет во вложенных объектах роли"""

    frodo = arda_users.frodo
    client.login('frodo')

    roles_url = reverse('api_dispatch_list', api_name=api_name, resource_name='roles')
    utils.raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')
    response = client.json.get(roles_url, {
        'user': 'frodo'
    })
    data = response.json()

    role = data['objects'][0]
    assert role['user']['username'] == 'frodo'
    # важно – в этом списке нет department_group
    expected = {
        'username',
        'fired_at',
        'is_active',
        'sex',
        'full_name',
        'position',
        'email',
        'date_joined',
        'type',
    }
    assert set(role['user'].keys()) == expected


def test_role_perms_for_robot_owners(simple_system, arda_users, robot_gollum, client):
    frodo, gandalf = arda_users.frodo, arda_users.gandalf
    simple_system.request_policy = 'subordinates'
    simple_system.save()
    add_perms_by_role('superuser', gandalf, simple_system)

    client.login('frodo')

    # frodo не может запросить роль для робота
    requests_url = reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequests')
    roles_url = reverse('api_dispatch_list', api_name='frontend', resource_name='roles')
    result = client.json.post(requests_url, {
        'path': '/admin/',
        'system': 'simple',
        'user': 'gollum',
    }).json()
    message = ('У пользователя "Фродо Бэггинс" нет прав на запрос роли для'
               ' пользователя "gollum" в системе "simple": Недостаточно прав')
    assert result['message'] == message
    assert result['error_code'] == Forbidden.error_code

    # пусть некто третий запросит роль
    client.login('gandalf')
    response = client.json.post(requests_url, {
        'path': '/admin/',
        'system': 'simple',
        'user': 'gollum',
    })
    assert response.status_code == 201
    role1 = Role.objects.get(pk=response.json()['id'])
    client.logout()

    # frodo не видит эту роль
    client.login('frodo')
    rolelist = client.json.get(roles_url)
    assert len(rolelist.json()['objects']) == 0

    # frodo не может отозвать роль
    role1_url = get_role_url(role1.pk)
    response = client.delete(role1_url)
    assert response.status_code == 404

    # делаем frodo робовладельцем
    robot_gollum.responsibles.add(frodo)

    # владелец робота успешно запрашивает роль
    response = client.json.post(requests_url, {
        'path': '/manager/',
        'system': 'simple',
        'user': 'gollum',
    })
    assert response.status_code == 201
    role2 = Role.objects.get(pk=response.json()['id'])

    # владелец видит обе роли робота
    rolelist = client.json.get(roles_url)
    assert len(rolelist.json()['objects']) == 2
    assert {role1.pk, role2.pk} == {obj['id'] for obj in rolelist.json()['objects']}

    # успешно отзывает роль
    role1_url = get_role_url(role1.pk)
    response = client.delete(role1_url)
    assert response.status_code == 204

    # одна роль стала неактивной
    rolelist = client.json.get(roles_url)
    assert [False] == [obj['is_active'] for obj in rolelist.json()['objects'] if obj['id'] == role1.pk]

    # на всякий случай отзывает другую роль
    role2_url = get_role_url(role2.pk)
    response = client.delete(role2_url)
    assert response.status_code == 204

    # в итоге видит две отозванных роли
    rolelist = client.json.get(roles_url)
    assert [False, False] == [obj['is_active'] for obj in rolelist.json()['objects']]


@pytest.mark.parametrize('system_state', ['inactive', 'broken'])
def test_deprive_role_in_inactive_or_broken_system(
        client,
        simple_system,
        users_for_test,
        depriver_users,
        system_state,
):
    """
    DELETE /frontend/roles/<role_id>/
    """
    art, fantom = users_for_test[:2]
    utils.add_perms_by_role('responsible', fantom, simple_system)
    role = Role.objects.request_role(fantom, art, simple_system, None, {'role': 'manager'}, None)

    if system_state == 'inactive':
        simple_system.is_active = False
    else:
        simple_system.is_broken = True
    simple_system.save()

    for depriver, can_deprive in depriver_users:
        client.login(depriver.username)
        deprive_url = get_role_url(role.pk)
        response = client.delete(deprive_url)
        if can_deprive:
            assert response.status_code == 403
        else:
            assert response.status_code == 404


def test_deprive_approved_role(client, simple_system, users_for_test, depriver_users):
    """
    DELETE /frontend/roles/<role_id>/
    """
    art = users_for_test[0]

    for depriver, can_deprive in depriver_users:
        superuser_role = utils.raw_make_role(art, simple_system, {'role': 'superuser'}, state='approved')
        client.login(depriver.username)
        deprive_url = get_role_url(superuser_role.pk)
        response = client.delete(deprive_url)
        superuser_role = utils.refresh(superuser_role)
        if can_deprive:
            assert response.status_code == 403
            response = response.json()
            assert response['message'] == 'Роль находится в состоянии Подтверждена, из которого отзыв невозможен'
            assert superuser_role.state == 'approved'
            assert not superuser_role.actions.filter(action='deprive').exists()
        else:
            assert response.status_code == 404
            assert superuser_role.state == 'approved'
            assert not superuser_role.actions.filter(action='deprive').exists()
        superuser_role.delete()


def test_deprive_depriving_role(client, simple_system, users_for_test, depriver_users):
    """
    DELETE /frontend/roles/<role_id>/
    """
    art = users_for_test[0]

    for depriver, can_deprive in depriver_users:
        superuser_role = utils.raw_make_role(art, simple_system, {'role': 'superuser'}, state='depriving')
        client.login(depriver.username)
        deprive_url = get_role_url(superuser_role.pk)
        response = client.delete(deprive_url)
        superuser_role = utils.refresh(superuser_role)
        if can_deprive:
            assert response.status_code == 204
            assert superuser_role.state == 'deprived'
        else:
            assert response.status_code == 404
            assert superuser_role.state == 'depriving'
        superuser_role.delete()


@waffle.testutils.override_switch('idm.enable_ignore_failed_roles_on_poke', active=True)
def test_retry_all_personal_roles_of_specific_group_role(client, arda_users, simple_system, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship

    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    group_role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    for role in Role.objects.filter(parent=group_role):
        role.set_raw_state(ROLE_STATE.FAILED)

    total_roles = Role.objects.filter(parent=group_role).count()
    assert Role.objects.filter(parent=group_role, state=ROLE_STATE.FAILED).count() == total_roles

    transition_url = reverse('api_dispatch_detail', api_name='frontend', pk=group_role.pk, resource_name='roles')

    client.login('bilbo')  # not a member
    response = client.json.post(transition_url, {
        'action': 'retry_failed',
    })

    assert response.status_code == 404

    client.login('meriadoc')  # member
    response = client.json.post(transition_url, {
        'action': 'retry_failed',
    })

    assert response.status_code == 403
    assert response.json()['message'] == 'Довыдача роли невозможна: Недостаточно прав для довыдачи роли'

    client.login('frodo')  # responsible
    response = client.json.post(transition_url, {
        'action': 'retry_failed',
    })

    assert response.status_code == 202
    assert Role.objects.filter(parent=group_role).count() == total_roles
    assert Role.objects.filter(parent=group_role, state=ROLE_STATE.GRANTED).count() == total_roles


@waffle.testutils.override_switch('idm.enable_ignore_failed_roles_on_poke', active=True)
def test_retry_specific_personal_failed_role(client, arda_users, simple_system, department_structure):
    frodo = arda_users.frodo
    bilbo = arda_users.bilbo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'superuser'}, None)
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)

    assert frodo.roles.count() == 2
    role1, role2 = frodo.roles.all()

    for role in frodo.roles.all():
        role.set_raw_state(ROLE_STATE.FAILED)

    role1.refresh_from_db()
    role2.refresh_from_db()
    assert frodo.roles.filter(state=ROLE_STATE.FAILED).count() == 2

    # довыдаем role1
    transition_url = reverse('api_dispatch_detail', api_name='frontend', pk=role1.pk, resource_name='roles')
    client.login('bilbo')
    response = client.json.post(transition_url, {
        'action': 'retry_failed',
    })
    role1.refresh_from_db()
    role2.refresh_from_db()
    assert response.status_code == 404
    assert role1.state == ROLE_STATE.FAILED
    assert role2.state == ROLE_STATE.FAILED

    client.login('frodo')
    response = client.json.post(transition_url, {
        'action': 'retry_failed',
    })
    role1.refresh_from_db()
    role2.refresh_from_db()

    # role1 довыдалась, role2 не довыдавалась и не довыдалась
    assert role1.state == ROLE_STATE.GRANTED
    assert role2.state == ROLE_STATE.FAILED

    # попытка довыдачи ненедовыданной роли
    response = client.json.post(transition_url, {
        'action': 'retry_failed',
    })
    assert response.status_code == 403
    assert response.json()[
               'message'] == 'Довыдача роли невозможна: Довыдать можно только персональную роль или групповую роль, у которой есть персональные со статусом \'Ошибка\''

    # довыдача роли ответственным системы
    for role in frodo.roles.all():
        role.set_raw_state(ROLE_STATE.FAILED)

    add_perms_by_role('responsible', bilbo, simple_system)
    response = client.json.post(transition_url, {
        'action': 'retry_failed',
    })

    role1.refresh_from_db()
    assert response.status_code == 202
    assert role1.state == ROLE_STATE.GRANTED
    remove_perms_by_role('responsible', bilbo, simple_system)

    # довыдача роли с пермишеном
    for role in frodo.roles.all():
        role.set_raw_state(ROLE_STATE.FAILED)

    add_perms_by_role('superuser', bilbo)
    response = client.json.post(transition_url, {
        'action': 'retry_failed',
    })

    role1.refresh_from_db()
    assert response.status_code == 202
    assert role1.state == ROLE_STATE.GRANTED


@pytest.mark.skip('Починить переопределенный механизм листинга ролей')
@pytest.mark.parametrize('fields', (set(), {'user', 'state', 'is_active'}))
def test_get_list__specify_fields(client, simple_system, arda_users, fields: set):
    client.login('frodo')
    raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')

    response = client.json.get(
        reverse('api_dispatch_list', api_name='frontend', resource_name='roles'),
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


@pytest.mark.skip('Починить переопределенный механизм листинга ролей')
def test_get_list__specify_unknown_fields(client, simple_system, arda_users):
    client.login('frodo')
    raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')
    unknown_field = 'unknown_field'

    response = client.json.get(
        reverse('api_dispatch_list', api_name='frontend', resource_name='roles'),
        data={'fields': f'state,{unknown_field}'},
    )
    assert response.status_code == 400, response.json()
    error = response.json()
    assert error['message'] == f'Unknown fields passed in query: {unknown_field}'


@pytest.mark.parametrize('display_name', [True, False, None])
@pytest.mark.parametrize('field_name', ('system_specific', 'fields_data'))
def test_get_list__display_fields_name(client, display_name, field_name):
    role_tree = {
            'code': 0,
            'roles': {
                'slug': 'role',
                'name': 'Роль',
                'values': {
                    'accessor': {
                        'name': 'Ассессор',
                        'fields': [
                            {
                                'slug': 'group',
                                'name': 'Номер счетчика',
                                'type': 'choicefield',
                                'options': {
                                    'choices': [{"value": "5", "name": "Вертикали"}, {"value": "4", "name": "Маркет"}]},
                            },
                        ]
                    }
                },
            },
            'fields': [
                {
                    'slug': 'organization_id',
                    'name': 'Номер счетчика',
                    'type': 'choicefield',
                    'options': {
                        'choices': [{"value": "1", "name": "ООО ЯНДЕКС"}, {"value": "117", "name": "ООО ЯНДЕКС.ЕДА"}],
                    },
                },
            ],
        }
    if display_name is not None:
        role_tree['fields'][0]['options']['display_name'] = display_name
        role_tree['roles']['values']['accessor']['fields'][0]['options']['display_name'] = display_name

    system = create_system(role_tree=role_tree)
    rolenode = system.nodes.last()
    subject = create_user()
    client.login(subject)
    params = {field_name: {'organization_id': '1', 'group': '5'}}
    raw_make_role(subject, system, rolenode.data, **params)

    response = client.get(reverse('api_dispatch_list', api_name='frontend', resource_name='roles'),)
    assert response.status_code == 200, response.json()
    result = response.json()
    assert len(result['objects']) == 1
    expected_fields = {
            'organization_id': '1',
            'group': '5',
        }
    if display_name:
        expected_fields = {
            'organization_id': 'ООО ЯНДЕКС',
            'group': 'Вертикали',
        }

    assert result['objects'][0][field_name] == expected_fields


@pytest.mark.parametrize('display_name', [True, False, None])
@pytest.mark.parametrize('field_name', ('system_specific', 'fields_data'))
def test_get_detail__display_fields_name(client, display_name, field_name):
    role_tree = {
        'code': 0,
        'roles': {
            'slug': 'role',
            'name': 'Роль',
            'values': {
                'accessor': {
                    'name': 'Ассессор',
                    'fields': [
                        {
                            'slug': 'group',
                            'name': 'Номер счетчика',
                            'type': 'choicefield',
                            'options': {
                                'choices': [{"value": "5", "name": "Вертикали"}, {"value": "4", "name": "Маркет"}]},
                        },
                    ]
                }
            },
        },
        'fields': [
            {
                'slug': 'organization_id',
                'name': 'Номер счетчика',
                'type': 'choicefield',
                'options': {
                    'choices': [{"value": "1", "name": "ООО ЯНДЕКС"}, {"value": "117", "name": "ООО ЯНДЕКС.ЕДА"}],
                },
            },
        ],
    }
    if display_name is not None:
        role_tree['fields'][0]['options']['display_name'] = display_name
        role_tree['roles']['values']['accessor']['fields'][0]['options']['display_name'] = display_name

    system = create_system(role_tree=role_tree)
    rolenode = system.nodes.last()
    subject = create_user()
    client.login(subject)

    params = {field_name: {'organization_id': '1', 'group': '5'}}
    role = raw_make_role(subject, system, rolenode.data, **params)
    response = client.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=role.pk),
    )
    assert response.status_code == 200, response.json()
    result = response.json()

    expected_fields = {
            'organization_id': '1',
            'group': '5',
        }
    if display_name:
        expected_fields = {
            'organization_id': 'ООО ЯНДЕКС',
            'group': 'Вертикали',
        }

    assert result[field_name] == expected_fields


@pytest.mark.parametrize('fields', (set(), {'user', 'state', 'is_active'}))
def test_get_detail__specify_fields(client, simple_system, arda_users, fields: set):
    client.login('frodo')
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')

    response = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=role.pk),
        data={'fields': ','.join(fields)},
    )
    assert response.status_code == 200, response.json()
    node_data = response.json()
    if fields:
        assert set(node_data.keys()) == fields
    else:
        # при пустом значении возвращаем все поля
        assert node_data != {}


def test_get_detail__specify_unknown_fields(client, simple_system, arda_users):
    client.login('frodo')
    unknown_field = 'unknown_field'
    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')

    response = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=role.pk),
        data={'fields': f'state,{unknown_field}'},
    )
    assert response.status_code == 400, response.json()
    error = response.json()
    assert error['message'] == f'Unknown fields passed in query: {unknown_field}'


def test_get_list__metrika_counter_fields_data(client, metrika_system):
    subject = create_user()
    client.login(subject)

    counter = MetrikaCounter.objects.create(**generate_counter_record().as_dict)
    rolenode = metrika_system.nodes.last()
    raw_make_role(
        subject,
        metrika_system,
        rolenode.data,
        fields_data={'counter_id': counter.counter_id},
        state='granted',
    )

    response = client.get(reverse('api_dispatch_list', api_name='frontend', resource_name='roles'))
    assert response.status_code == 200, response.json()
    result = response.json()
    assert len(result['objects']) == 1
    assert result['objects'][0]['fields_data'] == {'counter_id': counter.counter_id, 'counter_name': counter.name},\
        result['objects'][0]['fields_data']


def test_get_list__app_metrica_fields_data(client, app_metrica_system):
    subject = create_user()
    client.login(subject)

    app = AppMetrica.objects.create(**generate_app_record().as_dict)
    rolenode = app_metrica_system.nodes.last()
    raw_make_role(
        subject,
        app_metrica_system,
        rolenode.data,
        fields_data={'application_id': app.application_id},
        state='granted',
    )

    response = client.get(reverse('api_dispatch_list', api_name='frontend', resource_name='roles'))
    assert response.status_code == 200, response.json()
    result = response.json()
    assert len(result['objects']) == 1
    assert result['objects'][0]['fields_data'] == {
        'application_id': app.application_id, 'application_name': app.name
    }, result['objects'][0]['fields_data']


def test_get_detail__metrika_counter_fields_data(client, metrika_system):
    subject = create_user()
    client.login(subject)

    counter = MetrikaCounter.objects.create(**generate_counter_record().as_dict)
    rolenode = metrika_system.nodes.last()
    role = raw_make_role(
        subject,
        metrika_system,
        rolenode.data,
        fields_data={'counter_id': counter.counter_id},
        state='granted',
    )

    response = client.get(reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=role.pk))
    assert response.status_code == 200, response.json()
    result = response.json()
    assert result['fields_data'] == {'counter_id': counter.counter_id, 'counter_name': counter.name}, \
        result['fields_data']


def test_get_detail__app_metrica_fields_data(client, app_metrica_system):
    subject = create_user()
    client.login(subject)

    app = AppMetrica.objects.create(**generate_app_record().as_dict)
    rolenode = app_metrica_system.nodes.last()
    role = raw_make_role(
        subject,
        app_metrica_system,
        rolenode.data,
        fields_data={'application_id': app.application_id},
        state='granted',
    )

    response = client.get(reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=role.pk))
    assert response.status_code == 200, response.json()
    result = response.json()
    assert result['fields_data'] == {
        'application_id': app.application_id, 'application_name': app.name}, result['fields_data']


@pytest.mark.parametrize('state', [ROLE_STATE.GRANTED, ROLE_STATE.DEPRIVED])
def test_get_detail__personal_granted_at(client, state: str):
    client.login(create_user(superuser=True))
    aware_system = create_system(group_policy=SYSTEM_GROUP_POLICY.AWARE)
    unaware_system = create_system(group_policy=SYSTEM_GROUP_POLICY.UNAWARE)

    user = create_user(staff_id=random.randint(1, 10*6))
    group = create_group()
    group.add_members([user])

    # персональная роль
    user_role = raw_make_role(user, aware_system, aware_system.nodes.last().data, state=state)
    response = client.get(reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=user_role.pk))
    assert response.status_code == 200
    result = response.json()
    assert state == ROLE_STATE.GRANTED and result['personal_granted_at'] == user_role.granted_at.isoformat() or \
           result['personal_granted_at'] is None

    # групповая роль
    # без параметра user_context
    group_role = raw_make_role(group, aware_system, aware_system.nodes.last().data, state=state, with_inheritance=False)
    group_role.request_group_roles()

    response = client.get(reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=group_role.pk))
    assert response.status_code == 200
    result = response.json()
    assert state == ROLE_STATE.GRANTED and result['personal_granted_at'] == group_role.granted_at.isoformat() or \
           result['personal_granted_at'] is None

    # неизвестный пользователь в user_context
    response = client.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=group_role.pk),
        {'user_context': random_slug()}
    )
    assert response.status_code == 200
    assert response.json()['personal_granted_at'] is None

    # пользователь не является членом группы
    response = client.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=group_role.pk),
        {'user_context': create_user().username}
    )
    assert response.status_code == 200
    assert response.json()['personal_granted_at'] is None

    # пользователь вступил в группу до выдачи роли
    response = client.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=group_role.pk),
        {'user_context': user.username},
    )
    # пользователь вступил в группу до выдачи роли
    response = client.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=group_role.pk),
        {'user_context': user.username},
    )
    assert response.status_code == 200
    result = response.json()
    assert state == ROLE_STATE.GRANTED and \
           result['personal_granted_at'] == group_role.granted_at.isoformat() or \
           result['personal_granted_at'] is None

    # пользователь вступил в группу после выдачи роли
    member = create_user(staff_id=random.randint(1, 10*6))
    group.add_members([member])
    group_role.request_group_roles()

    response = client.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=group_role.pk),
        {'user_context': member.username}
    )
    assert response.status_code == 200
    result = response.json()
    assert state == ROLE_STATE.GRANTED and \
           result['personal_granted_at'] == member.memberships.get().date_joined.isoformat() or \
           result['personal_granted_at'] is None

    # пользователь не является непосредственным участником
    member.memberships.update(is_direct=False)
    response = client.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=group_role.pk),
        {'user_context': member.username}
    )
    assert response.status_code == 200
    assert response.json()['personal_granted_at'] is None

    # роль распространяется на участников подгруппы
    group_role.with_inheritance = True
    group_role.save(update_fields=['with_inheritance'])

    response = client.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=group_role.pk),
        {'user_context': member.username}
    )
    assert response.status_code == 200
    result = response.json()
    assert state == ROLE_STATE.GRANTED and \
           result['personal_granted_at'] == member.memberships.get().date_joined.isoformat() or \
           result['personal_granted_at'] is None

    # раскрытая групповая роль
    group_role_with_personal = raw_make_role(group, unaware_system, unaware_system.nodes.last().data)
    group_role_with_personal.request_group_roles()
    personal_role = group_role_with_personal.refs.first()
    personal_role.set_raw_state(state)

    response = client.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=personal_role.pk),
        {'user_context': member.username}
    )
    assert response.status_code == 200
    result = response.json()
    assert state == ROLE_STATE.GRANTED and \
           result['personal_granted_at'] == personal_role.granted_at.isoformat() or \
           result['personal_granted_at'] is None
