import datetime
import json
from textwrap import dedent

import freezegun
import pytest
import pytz
import waffle.testutils
from django.conf import settings
from django.core.management import call_command
from django.utils import timezone
from django.utils.timezone import make_aware

from idm.core.canonical import CanonicalNode
from idm.core.constants.instrasearch import INTRASEARCH_METHOD
from idm.core.constants.rolenode import ROLENODE_STATE
from idm.core.models import RoleNode, Action, Role, RoleField
from idm.core.queues import RoleNodeQueue
from idm.tests.utils import (
    add_perms_by_role,
    assert_num_queries_lte,
    assert_stable_hash,
    ctt_data_is_consistent,
    enable_intrasearch_pushes,
    mock_tree,
    raw_make_role,
    refresh,
    run_commit_hooks,
    set_workflow,
    sync_role_nodes, random_slug,
)
from idm.utils import reverse


pytestmark = [
    pytest.mark.django_db,
    pytest.mark.parametrize('api_name', ('frontend', 'v1'))
]


def assert_is_parent(node, start_path):
    if not node['slug_path'].startswith(start_path):
        return False
    return node['slug_path'][len(start_path):] == node['slug'] + '/'


def assert_except_updated(sent_to_intrasearch, got_from_api):
    sent_to_intrasearch = {k: v for k, v in json.loads(sent_to_intrasearch).items() if k != 'updated_at'}
    got_from_api = {k: v for k, v in got_from_api.items() if k != 'updated_at'}
    assert sent_to_intrasearch == got_from_api


@pytest.mark.parametrize('method', ['get', 'delete', 'put'])
def test_get_node_by_unique_id_system_does_not_exist(arda_users, api_name, client, method):
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes', slug_path='system_slug/unique_id/id')
    response = getattr(client.json, method)(url)
    assert response.status_code == 400
    assert response.json()['message'] == 'Cannot identify valid system from given node path'


@pytest.mark.parametrize('method', ['get', 'delete', 'put'])
def test_get_node_by_unique_id_node_does_not_exist(arda_users, api_name, client, simple_system, method):
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes', slug_path='simple/unique_id/id')
    response = getattr(client.json, method)(url)
    assert response.status_code == 404
    assert response.json()['message'] == 'Cannot find node with unique_id id'


def test_get_node_by_unique_id(arda_users, api_name, client, simple_system):
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes', slug_path='simple/unique_id/id')
    node = simple_system.nodes.get(slug='manager')
    node.unique_id = 'id'
    node.save(update_fields=['unique_id'])

    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json()['slug_path'] == node.slug_path


def test_delete_node_by_unique_id(arda_users, api_name, client, simple_system):
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=simple_system)
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes', slug_path='simple/unique_id/id')
    node = simple_system.nodes.get(slug='manager')
    node.unique_id = 'id'
    node.save(update_fields=['unique_id'])

    response = client.json.delete(url)
    assert response.status_code == 204

    node.refresh_from_db()
    assert node.state == 'deprived'


def test_put_node_by_unique_id(arda_users, api_name, client, simple_system):
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=simple_system)
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes', slug_path='simple/unique_id/id')
    node = simple_system.nodes.get(slug='manager')
    node.unique_id = 'id'
    node.save(update_fields=['unique_id'])

    response = client.json.put(url, {'slug': 'new_slug'})
    assert response.status_code == 204

    node.refresh_from_db()
    assert node.slug == 'new_slug'


def test_intrasearch_push_rolenode_on(client, api_name, complex_system, arda_users):
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)

    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    detail_url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/archer'
    )

    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/',
        'slug': 'archer',
        'name': {
            'ru': 'Лучник',
            'en': 'Archer',
        }
    }

    expected_args = (settings.IDM_INTRASEARCH_PUSH_URL.format(source_type='rolenodes'), )

    with enable_intrasearch_pushes() as pusher:
        with run_commit_hooks():
            response = client.json.post(url, node_data)
        assert response.status_code == 201
        args, kwargs = pusher.http_post.call_args
        assert args == expected_args
        if api_name == 'v1':
            assert_except_updated(kwargs['data'], client.json.get(detail_url).json())
        pusher.http_post.reset_mock()

        node_data['name']['ru'] = 'Другое имя'
        with run_commit_hooks():
            client.json.put(detail_url, node_data)
        args, kwargs = pusher.http_post.call_args
        assert args == expected_args
        if api_name == 'v1':
            assert_except_updated(kwargs['data'], client.json.get(detail_url).json())
        pusher.http_post.reset_mock()

        node_data['visibility'] = False
        with run_commit_hooks():
            client.json.put(detail_url, node_data)
        args, kwargs = pusher.http_delete.call_args
        assert args == expected_args
        pusher.http_delete.reset_mock()

        node_data['visibility'] = True
        with run_commit_hooks():
            client.json.put(detail_url, node_data)
        args, kwargs = pusher.http_post.call_args
        assert args == expected_args
        pusher.http_post.reset_mock()

        with run_commit_hooks():
            client.json.delete(detail_url, node_data)
        args, kwargs = pusher.http_delete.call_args
        assert args == expected_args
        pusher.http_delete.reset_mock()


def test_intrapush_on_movement(client, api_name, complex_system, arda_users):
    add_perms_by_role('tree_edit', arda_users.frodo, system=complex_system)
    client.login('frodo')

    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes', slug_path='complex/project/subs')
    with enable_intrasearch_pushes() as pusher:
        with run_commit_hooks():
            client.json.put(url, data={'parent': '/project/rules/role/'})
        assert len(pusher.http_post.call_args_list) == 3
        assert len(pusher.http_delete.call_args_list) == 3
        added_paths = [json.loads(call[1]['data'])['slug_path'] for call in pusher.http_post.call_args_list]
        removed_paths = [json.loads(call[1]['data'])['slug_path'] for call in pusher.http_delete.call_args_list]
        assert sorted(removed_paths) == [
            '/project/subs/',
            '/project/subs/role/developer/',
            '/project/subs/role/manager/',
        ]
        assert sorted(added_paths) == [
            '/project/rules/role/subs/',
            '/project/rules/role/subs/role/developer/',
            '/project/rules/role/subs/role/manager/',
        ]


def test_intrapush_on_slug_change(client, api_name, complex_system, arda_users):
    add_perms_by_role('tree_edit', arda_users.frodo, system=complex_system)
    client.login('frodo')
    slug_path_to_id = dict(complex_system.nodes.values_list('slug_path', 'id'))

    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/rules')
    with enable_intrasearch_pushes() as pusher:
        with run_commit_hooks():
            client.json.put(url, data={'slug': 'idm'})
        # В этом случае вся пострабротка идёт уже в рамках запроса
        assert len(pusher.http_post.call_args_list) == 3
        assert len(pusher.http_delete.call_args_list) == 4
        added_paths = [json.loads(call[1]['data'])['slug_path'] for call in pusher.http_post.call_args_list]
        removed_ids = [json.loads(call[1]['data'])['id'] for call in pusher.http_delete.call_args_list]

        # При удалении Поиск использует ID узлов,
        # поэтому это единственное, что имеет смысл проверять
        expected_removed_paths = [
            '/project/rules/',
            '/project/rules/role/admin/',
            '/project/rules/role/auditor/',
            '/project/rules/role/invisic/',
        ]
        expected_removed_ids = [slug_path_to_id.get(p) for p in expected_removed_paths]
        assert sorted(removed_ids) == expected_removed_ids

        assert sorted(added_paths) == [
            '/project/idm/',
            '/project/idm/role/admin/',
            '/project/idm/role/auditor/',
        ]


def test_intrasearch_push_rolenode_off(client, api_name, complex_system, arda_users):
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)

    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    detail_url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/manager'
    )

    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
    }

    with enable_intrasearch_pushes(enable=False) as pusher:
        response = client.json.post(url, node_data)
        assert response.status_code == 201
        assert pusher.http_post.called is False
        pusher.http_post.reset_mock()
        node = RoleNode.objects.get(slug='division')
        node.is_key = False
        node.save(update_fields=['is_key'])

        response = client.json.delete(detail_url, node_data)
        assert response.status_code == 204
        assert pusher.http_post.called is False
        pusher.http_post.reset_mock()
    node.refresh_from_db()
    assert node.state == ROLENODE_STATE.DEPRIVED
    assert node.need_isearch_push_method == INTRASEARCH_METHOD.REMOVE


def test_connections_count(client, complex_system, simple_system, arda_users,
                           api_name, django_assert_num_queries):
    expected_counts = {'frontend': 10, 'v1': 10}
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    with django_assert_num_queries(expected_counts[api_name]):
        data = client.json.get(url)


def test_get_nodes(client, api_name, complex_system, simple_system, arda_users):
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')

    # common
    data = client.json.get(url, {
        'system': 'complex'
    }).json()

    if api_name == 'frontend':
        ids = [obj['id'] for obj in data['objects']]
        assert ids == sorted(ids)
        assert set(data['objects'][0]) == {
            'data',
            'description',
            'human',
            'human_short',
            'id',
            'is_auto_updated',
            'is_key',
            'is_public',
            'review_required',
            'comment_required',
            'name',
            'set',
            'slug',
            'slug_path',
            'state',
            'system',
            'unique_id',
            'value_path'
        }
    elif api_name == 'v1':
        assert set(data['objects'][0]) == {
            'id',
            'state',
            'aliases',
            'fields',
            'help',
            'human',
            'human_short',
            'is_key',
            'name',
            'parent_path',
            'responsibilities',
            'review_required',
            'comment_required',
            'slug',
            'slug_path',
            'system',
            'value_path',
            'visibility',
            'unique_id',
            'created_at',
            'updated_at',
        }
        assert set(data['objects'][0]['system']) == {
            'description',
            'endpoint_timeout',
            'endpoint_long_timeout',
            'group_policy',
            'is_active',
            'is_broken',
            'is_sox',
            'name',
            'slug',
            'state',
            'use_mini_form',
            'use_webauth',
        }

    assert len(data['objects']) == complex_system.nodes.count()

    # slug_path
    data = client.json.get(url, {
        'slug_path': '/project/rules/role/',
        'system': 'complex',
    }).json()

    assert len(data['objects']) == complex_system.nodes.filter(slug_path__startswith='/project/rules/role/').count()
    assert all(node['slug_path'].startswith('/project/rules/role/') for node in data['objects'])
    if api_name == 'v1':
        assert all(
            node['parent_path'] == '/complex/project/rules/' or node['is_key'] == True
            for node in data['objects']
        )

    # is_key == False
    data = client.json.get(url, {
        'is_key': False,
        'system': 'complex',
    }).json()

    assert len(data['objects']) == complex_system.nodes.filter(is_key=False).count()
    assert all(node['is_key'] == False for node in data['objects'])

    # is_key == True
    data = client.json.get(url, {
        'is_key': True,
        'system': 'complex',
    }).json()

    assert len(data['objects']) == complex_system.nodes.filter(is_key=True).count()
    assert all(node['is_key'] == True for node in data['objects'])

    # is_public == False
    data = client.json.get(url, {
        'is_public': False,
        'system': 'complex',
    }).json()

    assert len(data['objects']) == complex_system.nodes.filter(is_public=False).count()
    if api_name == 'frontend':
        assert all(node['is_public'] == False for node in data['objects'])

    # is_public == True
    data = client.json.get(url, {
        'is_public': True,
        'system': 'complex',
    }).json()

    assert len(data['objects']) == complex_system.nodes.public().count()
    if api_name == 'frontend':
        assert all(node['is_public'] == True for node in data['objects'])


    # parent
    parent = '/project/subs/'
    data = client.json.get(url, {
        'system': 'complex',
        'parent': parent
    }).json()

    assert all(assert_is_parent(node, parent) for node in data['objects'])
    true_slugs = complex_system.nodes.filter(slug_path__startswith=parent).values_list('slug_path', 'slug')
    true_slugs = [node[0] for node in true_slugs if node[0][len(parent):] == node[1] + '/']
    response_slug_paths = [node['slug_path'] for node in data['objects']]
    assert all(path in response_slug_paths for path in true_slugs)

    # all systems
    data = client.json.get(url).json()

    assert len(data['objects']) == RoleNode.objects.count()
    if api_name == 'frontend':
        assert {obj['system']['slug'] for obj in data['objects']} == {'complex', 'simple'}


def test_get_nodes_updated(client, api_name, complex_system, arda_users):
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')

    for i, node in enumerate(complex_system.nodes.all()):
        dt = make_aware(datetime.datetime.fromtimestamp(i*3600))
        complex_system.nodes.filter(id=node.id).update(updated_at=dt)

    response = client.json.get(url, {
        'updated__since': datetime.datetime.fromtimestamp(3*3600),
        'updated__until': datetime.datetime.fromtimestamp(7*3600),
        'system': 'complex',
    })

    assert response.status_code == 200
    assert len(response.json()['objects']) == 5


def test_get_nodes_updated_tz(client, api_name, complex_system, arda_users):
    """Проверим передачу TZ-aware времени в API"""

    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')

    for i, node in enumerate(complex_system.nodes.all()):
        dt = make_aware(datetime.datetime.fromtimestamp(i*3600), timezone=timezone.utc)
        complex_system.nodes.filter(id=node.id).update(updated_at=dt)

    since = make_aware(datetime.datetime.fromtimestamp(3 * 3600), timezone=timezone.utc)
    until = make_aware(datetime.datetime.fromtimestamp(7 * 3600), timezone=timezone.utc)
    response = client.json.get(url, {
        'updated__since': since.isoformat(),
        'updated__until': until.isoformat(),
        'system': 'complex',
    })

    assert response.status_code == 200
    assert len(response.json()['objects']) == 5

    msk = pytz.timezone('Europe/Moscow')
    since = make_aware(datetime.datetime.fromtimestamp(3 * 3600), timezone=msk)
    until = make_aware(datetime.datetime.fromtimestamp(7 * 3600), timezone=msk)
    response = client.json.get(url, {
        'updated__since': since.isoformat(),
        'updated__until': until.isoformat(),
        'system': 'complex',
    })

    assert response.status_code == 200
    assert len(response.json()['objects']) == 3


def test_get_node(client, api_name, complex_system, arda_users):
    # GET /api/v1/rolenodes/project/rules/
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/rules')
    response = client.json.get(url)
    data = response.json()

    assert data['slug_path'] == '/project/rules/'
    assert data['human'] == 'Проект: IDM'
    assert data['human_short'] == 'IDM'


@pytest.mark.parametrize('create', [False, True])
def test_edit_node(client, api_name, complex_system, arda_users, create):
    # PUT /api/v1/rolenodes/project/rules/
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/rules')

    new_data = {
        'create': create,
        'name': {
            'ru': 'rulezz',
            'en': 'IDM'
        },
        'review_required': False,
        'comment_required': True,
    }

    response = client.json.put(url, data={})
    assert response.status_code == 403

    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    response = client.json.put(url, data=new_data)
    assert response.wsgi_request.node_modification_info == {
        'method': 'PUT',
        'can_upsert': create,
        'was_renamed': False,
        'was_moved': False,
        'was_created': False,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }
    assert response.status_code == (201 if create else 204)

    call_command('idm_recalc_pipeline')

    response = client.json.get(url)
    data = response.json()
    assert data['slug'] == 'rules'
    assert data['review_required'] is False
    assert data['comment_required'] is True
    if api_name == 'frontend':
        assert data['name'] == 'rulezz'
    else:
        assert data['name'] == {
            'ru': 'rulezz',
            'en': 'IDM'
        }
    rules_node = RoleNode.objects.get(slug='rules')
    assert rules_node.name == 'rulezz'
    assert rules_node.name_en == 'IDM'
    assert rules_node.review_required is False
    assert rules_node.comment_required is True

    admin = RoleNode.objects.get(slug='admin')
    assert admin.humanize() == 'Проект: rulezz, Роль: Админ'

    assert not Action.objects.filter(action='role_node_created').exists()
    action = Action.objects.get(action='role_node_changed')
    assert action.data['from_api'] is True


def test_edit_node_name(client, api_name, complex_system, arda_users):
    # PUT /api/v1/rolenodes/project/rules/
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/rules')

    response = client.json.put(url, data={'name': 'new_name'})
    assert response.wsgi_request.node_modification_info == {
        'method': 'PUT',
        'can_upsert': False,
        'was_renamed': False,
        'was_moved': False,
        'was_created': False,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }
    assert response.status_code == 204

    call_command('idm_recalc_pipeline')

    response = client.json.get(url)
    data = response.json()
    assert data['slug'] == 'rules'
    if api_name == 'frontend':
        assert data['name'] == 'new_name'
        assert data['is_public'] is True
    else:
        assert data['visibility'] is True
        assert data['name']['ru'] == 'new_name'
        assert data['name']['en'] == 'new_name'


@pytest.mark.parametrize('create', [False, True])
def test_edit_parent_node(client, api_name, complex_system, arda_users, create):
    # PUT /api/v1/rolenodes/project/rules/
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project')

    new_data = {
        'create': create,
        'name': 'proekt'
    }

    response = client.json.put(url, data={})
    assert response.status_code == 403

    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    response = client.json.put(url, data=new_data)
    assert response.status_code == (201 if create else 204)

    node = RoleNode.objects.get(slug_path='/project/rules/')
    assert node.pushed_at is None
    assert node.moved_at is None
    call_command('idm_recalc_pipeline')

    response = client.json.get(url)
    data = response.json()
    assert data['slug'] == 'project'
    if api_name == 'frontend':
        assert data['name'] == 'proekt'
    else:
        assert data['name'] == {
            'ru': 'proekt',
            'en': 'proekt'
        }

    admin = RoleNode.objects.get(slug='admin')
    assert admin.humanize() == 'Proekt: IDM, Роль: Админ'

    assert not Action.objects.filter(action='role_node_created').exists()
    action = Action.objects.get(action='role_node_changed')
    assert action.data['from_api'] is True


@freezegun.freeze_time('2019-01-01')
def test_edit_node_parent(client, api_name, complex_system, arda_users):
    # PUT /api/v1/rolenodes/project/rules/
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    old_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/rules/role/auditor/')

    complex_system.nodes.filter(slug_path='/project/rules/role/auditor/').update(unique_id='hey')

    with run_commit_hooks():
        response = client.json.put(old_url, data={'parent': '/project/subs/role/'})
    assert response.wsgi_request.node_modification_info == {
        'method': 'PUT',
        'can_upsert': False,
        'was_renamed': False,
        'was_moved': True,
        'was_created': False,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }
    assert response.status_code == 204

    assert not complex_system.nodes.filter(slug_path='/project/rules/role/auditor/').exists()
    node = complex_system.nodes.get(slug_path='/project/subs/role/auditor/')
    assert node.parent == complex_system.nodes.get(slug_path='/project/subs/role/')
    assert node.unique_id == 'hey'
    assert node.pushed_at is None
    assert node.moved_at == timezone.now()


@pytest.mark.parametrize('create', [False, True])
def test_edit_node_is_exclusive(client, api_name, complex_system, arda_users, create):
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)

    node = RoleNode.objects.get(slug_path='/project/rules/')
    node.fetch_system()
    assert not node.is_exclusive

    url = reverse(
        'api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
        slug_path=f'{node.system.slug}{node.slug_path}'
    )
    response = client.json.put(url, data={'is_exclusive': True, 'create': create})
    assert response.status_code == (201 if create else 204)

    node.refresh_from_db()
    assert node.is_exclusive


def test_create_node_is_exclusive(client, api_name, complex_system, arda_users):
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)

    url = reverse(
        'api_dispatch_list', api_name=api_name, resource_name='rolenodes'
    )

    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
        'is_exclusive': True,
    }

    response = client.json.post(url, node_data)
    assert response.status_code == 201

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.is_exclusive


@pytest.mark.parametrize('create', [False, True])
def test_cannot_create_conflict_on_move(client, api_name, complex_system, arda_users, create):
    """При применении parent и slug из тела запроса конфликты приводят к 400"""
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)

    url = reverse(
        'api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
        slug_path='complex/project/rules/role/',
    )
    response = client.json.put(url, data={'parent': '/project/subs/', 'create': create})
    assert response.status_code == 400
    assert response.json()['message'] == 'Узел "/project/subs/role/" уже есть на этом уровне дерева'

    for path in ['complex/project/rules/role/auditor/', 'complex/project/rules/role/randomguy/']:
        url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes', slug_path=path)
        response = client.json.put(url, data={'slug': 'invisic', 'create': create})
        assert response.status_code == 400
        assert response.json()['message'] == 'Узел "/project/rules/role/invisic/" уже есть на этом уровне дерева'

        response = client.json.put(url, data={'parent': '/project/subs/role/', 'slug': 'developer', 'create': create})
        assert response.status_code == 400
        assert response.json()['message'] == 'Узел "/project/subs/role/developer/" уже есть на этом уровне дерева'

    for slug in ['auditor', 'randomguy']:
        if slug != 'randomguy':
            assert RoleNode.objects.filter(parent__slug_path='/project/rules/role/', slug=slug).exists()
        path = 'complex/project/rules/role/{}/'.format(slug)
        new_slug = slug + '_new'
        url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes', slug_path=path)
        response = client.json.put(url, data={'slug': new_slug, 'parent': '/project/subs/role/', 'create': create})
        if slug == 'randomguy' and not create:
            assert response.status_code == 400
        else:
            assert response.status_code == (201 if create else 204)
            assert RoleNode.objects.filter(parent__slug_path='/project/subs/role/', slug=new_slug).exists()
            assert not RoleNode.objects.filter(parent__slug_path='/project/rules/role/', slug=slug).exists()


def test_change_node_visibility(client, api_name, complex_system, arda_users):
    # PUT /api/v1/rolenodes/project/rules/
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/rules')

    node = RoleNode.objects.get(slug_path='/project/rules/')
    assert node.is_public is True

    response = client.json.put(url, data={'visibility': 'false'})
    assert response.status_code == 204

    node = refresh(node)
    assert node.is_public is False

    response = client.json.put(url, data={'visibility': '1'})
    assert response.status_code == 204

    node = refresh(node)
    assert node.is_public is True


@freezegun.freeze_time('2019-01-01')
def test_add_node(client, api_name, complex_system, arda_users):
    # POST /api/v1/rolenodes/
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
        'unique_id': 'bla',
        'review_required': True,
    }
    node_data_division = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/division/',
        'slug': 'nord',
        'name': {
            'ru': 'Дивизион Север',
            'en': 'Division Nord',
        },
        'help': 'Справка по северу',
        'visibility': True,
        'set': 'manager_id',
        'aliases': [{
            'name': {
                'ru': 'Алиас',
                'en': 'Alias',
            }
        }],
        'fields': [{
            'is_required': False,
            'type': 'choicefield',
            'name': {
                'ru': 'Поле',
                'en': 'Field'
            },
            'slug': 'division_nick',
            'options': {
                'choices': [{
                    'name': {
                        'en': 'Choice 1',
                        'ru': 'Вариант 1'
                    },
                    'value': 1
                }, {
                    'name': 'Choice 2',
                    'value': 2,
                }]
            }
        }],
        'responsibilities': [{
            'username': 'sauron',
            'notify': True,
        }],
    }

    response = client.json.post(url, node_data)
    assert response.status_code == 403

    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    response = client.json.post(url, node_data)
    assert response.status_code == 201
    assert response.wsgi_request.node_modification_info == {
        'method': 'POST',
        'was_moved': False,
        'has_unique_id': True,
        'was_restored': False,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }
    action = Action.objects.get(action='role_node_created')
    assert action.requester_id == arda_users.frodo.id
    assert action.data['from_api'] is True

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.slug == 'division'
    assert node.is_key is True
    assert node.name == 'Дивизион'
    assert node.name_en == 'Division'
    assert node.is_public is True
    assert node.unique_id == 'bla'
    assert node.review_required is True

    response = client.json.post(url, node_data_division)
    assert response.status_code == 201
    assert response.wsgi_request.node_modification_info == {
        'method': 'POST',
        'was_moved': False,
        'has_unique_id': False,
        'was_restored': False,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/nord/')
    assert node.slug == 'nord'
    assert node.is_key is False
    assert node.is_public is True
    assert node.name == 'Дивизион Север'
    assert node.name_en == 'Division Nord'
    assert node.description == 'Справка по северу'
    assert node.description_en == 'Справка по северу'
    node.fetch_nodeset()
    assert node.nodeset.set_id == 'manager_id'
    assert node.unique_id == ''
    assert node.review_required is None
    assert node.pushed_at == timezone.now()
    assert node.moved_at is None

    assert node.aliases.count() == 1
    alias = node.aliases.get()
    assert alias.name == 'Алиас'
    assert alias.name_en == 'Alias'

    assert node.fields.count() == 1
    field = node.fields.get()
    assert field.is_required is False
    assert field.slug == 'division_nick'
    assert field.name == 'Поле'
    assert field.name_en == 'Field'
    assert field.options == {
        'choices': [{
            'name': {
                'en': 'Choice 1',
                'ru': 'Вариант 1'
            },
            'value': 1
        }, {
            'name': {
                'en': 'Choice 2',
                'ru': 'Choice 2',
            },
            'value': 2,
        }]
    }

    assert node.responsibilities.count() == 1
    responsibility = node.responsibilities.get()
    assert responsibility.user_id == arda_users.sauron.id
    assert responsibility.notify is True

    role = raw_make_role(arda_users.legolas, complex_system, {'project': 'subs', 'role': 'manager', 'division': 'nord'})
    assert role.state == 'granted'

    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/subs/role/manager/division/nord')
    response = client.json.get(url)
    data = response.json()
    assert response.status_code == 200
    assert data['slug_path'] == '/project/subs/role/manager/division/nord/'
    if api_name == 'v1':
        assert data['responsibilities'] == node_data_division['responsibilities']


@pytest.mark.parametrize('method', ['POST', 'PUT'])
def test_add_node_for_parent_with_roles(client, api_name, complex_system, arda_users, method):
    # PUT /api/v1/rolenodes/
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)

    role = raw_make_role(arda_users.legolas, complex_system, {'project': 'subs', 'role': 'manager'})
    assert role.state == 'granted'

    if method == 'PUT':
        url = reverse(
            'api_dispatch_detail',
            api_name=api_name,
            resource_name='rolenodes',
            slug_path='complex/project/subs/role/manager/division/',
        )
        data = {
            'create': True,
            'name': 'Division',
        }
        response = client.json.put(url, data)
    else:
        url = reverse(
            'api_dispatch_list',
            api_name=api_name,
            resource_name='rolenodes',
        )
        data = {
            'system': 'complex',
            'parent': '/project/subs/role/manager/',
            'slug': 'division',
            'name': 'Division',
        }
        response = client.json.post(url, data)

    assert response.status_code == 400
    assert response.json()['message'] == 'Parent node already has active or soon-to-be active roles'

    role.set_raw_state('deprived')

    if method == 'PUT':
        response = client.json.put(url, data)
    else:
        response = client.json.post(url, data)

    assert response.status_code == 201


@freezegun.freeze_time('2019-01-01')
def test_add_node_via_put(client, api_name, complex_system, arda_users):
    # PUT /api/v1/rolenodes/
    client.login('frodo')

    key_url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/manager/division/',
    )
    bad_key_node_data = {
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        }
    }
    key_node_data = {
        'create': True,
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
        'unique_id': 'bla',
    }
    division_url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/manager/division/nord/',
    )
    division_node_data = {
        'create': True,
        'name': {
            'ru': 'Дивизион Север',
            'en': 'Division Nord',
        },
        'help': 'Справка по северу',
        'visibility': True,
        'set': 'manager_id',
        'aliases': [{
            'name': {
                'ru': 'Алиас',
                'en': 'Alias',
            }
        }],
        'fields': [{
            'is_required': False,
            'type': 'choicefield',
            'name': {
                'ru': 'Поле',
                'en': 'Field'
            },
            'slug': 'division_nick',
            'options': {
                'choices': [{
                    'name': {
                        'en': 'Choice 1',
                        'ru': 'Вариант 1'
                    },
                    'value': 1
                }, {
                    'name': 'Choice 2',
                    'value': 2,
                }]
            }
        }],
        'responsibilities': [{
            'username': 'sauron',
            'notify': True,
        }],
    }

    response = client.json.put(key_url, key_node_data)
    assert response.status_code == 403

    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)

    response = client.json.put(key_url, bad_key_node_data)
    assert response.status_code == 400
    assert response.json()['message'] == 'Role node with given slug_path does not exist'

    response = client.json.put(key_url, key_node_data)
    assert response.status_code == 201
    assert response.wsgi_request.node_modification_info == {
        'method': 'PUT',
        'can_upsert': True,
        'was_moved': False,
        'was_renamed': False,
        'was_created': True,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }
    action = Action.objects.get(action='role_node_created')
    assert action.requester_id == arda_users.frodo.id
    assert action.data['from_api'] is True

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.slug == 'division'
    assert node.is_key is True
    assert node.name == 'Дивизион'
    assert node.name_en == 'Division'
    assert node.is_public is True
    assert node.unique_id == 'bla'
    assert node.pushed_at == timezone.now()
    assert node.moved_at is None

    response = client.json.put(division_url, division_node_data)
    assert response.status_code == 201
    assert response.wsgi_request.node_modification_info == {
        'method': 'PUT',
        'can_upsert': True,
        'was_moved': False,
        'was_renamed': False,
        'was_created': True,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/nord/')
    assert node.slug == 'nord'
    assert node.is_key is False
    assert node.is_public is True
    assert node.name == 'Дивизион Север'
    assert node.name_en == 'Division Nord'
    assert node.description == 'Справка по северу'
    assert node.description_en == 'Справка по северу'
    node.fetch_nodeset()
    assert node.nodeset.set_id == 'manager_id'
    assert node.unique_id == ''

    assert node.aliases.count() == 1
    alias = node.aliases.get()
    assert alias.name == 'Алиас'
    assert alias.name_en == 'Alias'

    assert node.fields.count() == 1
    field = node.fields.get()
    assert field.is_required is False
    assert field.slug == 'division_nick'
    assert field.name == 'Поле'
    assert field.name_en == 'Field'
    assert field.options == {
        'choices': [{
            'name': {
                'en': 'Choice 1',
                'ru': 'Вариант 1'
            },
            'value': 1
        }, {
            'name': {
                'en': 'Choice 2',
                'ru': 'Choice 2',
            },
            'value': 2,
        }]
    }

    assert node.responsibilities.count() == 1
    responsibility = node.responsibilities.get()
    assert responsibility.user_id == arda_users.sauron.id
    assert responsibility.notify is True

    role = raw_make_role(arda_users.legolas, complex_system, {'project': 'subs', 'role': 'manager', 'division': 'nord'})
    assert role.state == 'granted'

    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/subs/role/manager/division/nord')
    response = client.json.get(url)
    data = response.json()
    assert response.status_code == 200
    assert data['slug_path'] == '/project/subs/role/manager/division/nord/'
    if api_name == 'v1':
        assert data['responsibilities'] == division_node_data['responsibilities']


# Механизм обновления алиасов, полей и ответственных на стороне API и на стороне NodeAdditionItem'ов совпадает
@pytest.mark.parametrize('create', [False, True])
def test_change_aliases_via_put(client, api_name, complex_system, arda_users, create):
    # PUT /api/v1/rolenodes/
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)

    key_url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/manager/division/',
    )
    key_node_data = {
        'create': True,
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        }
    }
    division_url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/manager/division/nord/',
    )
    division_node_data = {
        'create': True,
        'name': {
            'ru': 'Дивизион Север',
            'en': 'Division Nord',
        },
        'visibility': True,
        'aliases': [
            {
                'name': {
                    'ru': 'Алиас',
                    'en': 'Alias',
                }
            }
        ],
    }
    new_data = {
        'create': create,
        'aliases': [
            {
                'name': {
                    'ru': 'Новый алиас',
                    'en': 'New alias',
                }
            },
        ]
    }

    response = client.json.put(key_url, key_node_data)
    assert response.status_code == 201
    response = client.json.put(division_url, division_node_data)
    assert response.status_code == 201

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/nord/')
    assert node.aliases.count() == 1
    alias = node.aliases.get()
    assert alias.name == 'Алиас'
    assert alias.name_en == 'Alias'

    response = client.json.put(division_url, new_data)
    assert response.status_code == (201 if create else 204)

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/nord/')
    assert node.aliases.count() == 2
    assert node.aliases.filter(is_active=True).count() == 1
    alias = node.aliases.get(is_active=True)
    assert alias.name == 'Новый алиас'
    assert alias.name_en == 'New alias'


@pytest.mark.parametrize('slug', ['division', None, 'group'])
@pytest.mark.parametrize('parent', ['/project/subs/role/manager/', None, '/project/subs/'])
def test_add_existing_node_via_put_with_slug(client, api_name, complex_system, arda_users, slug, parent):
    # PUT /api/v1/rolenodes/ с create=True и изменением slug'а и parent'а
    # Теперь это валидно и работает аналогично create=False
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)


    key_url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/manager/division/',
    )
    key_node_data = {
        'create': True,
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
    }
    if slug is not None:
        key_node_data['slug'] = slug
    if parent is not None:
        key_node_data['parent'] = parent

    initial_slug = 'division'
    initial_parent = RoleNode.objects.get(slug_path='/project/subs/role/manager/')
    assert not RoleNode.objects.filter(slug_path='/project/subs/role/manager/{}/'.format(initial_slug)).exists()
    new_parent = RoleNode.objects.get(slug_path=parent) if parent else None

    response = client.json.put(key_url, key_node_data)
    assert response.status_code == 201
    assert response.wsgi_request.node_modification_info == {
        'method': 'PUT',
        'can_upsert': True,
        'was_moved': False,
        'was_renamed': False,
        'was_created': True,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }
    action = Action.objects.get(action='role_node_created')
    assert action.requester_id == arda_users.frodo.id
    assert action.data['from_api'] is True

    if slug is not None and slug != 'division':
        assert not RoleNode.objects.filter(slug_path='/project/subs/role/manager/{}/'.format(initial_slug)).exists()
    node = RoleNode.objects.get(slug_path='{}{}/'.format((new_parent or initial_parent).slug_path, slug or initial_slug))
    assert node.slug == (slug or initial_slug)
    assert node.is_key is True
    assert node.name == 'Дивизион'
    assert node.name_en == 'Division'
    assert node.is_public is True
    assert node.parent == (new_parent if parent else initial_parent)


def test_add_and_modify_via_put_with_create(client, api_name, complex_system, arda_users):
    # PUT /api/v1/rolenodes/
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)

    key_url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/manager/division/',
    )
    key_node_data = {
        'create': True,
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
    }
    new_key_node_data = {
        'create': True,
        'name': {
            'ru': 'Группа',
            'en': 'Group',
        },
    }

    response = client.json.put(key_url, key_node_data)
    assert response.status_code == 201
    assert response.wsgi_request.node_modification_info == {
        'method': 'PUT',
        'can_upsert': True,
        'was_moved': False,
        'was_renamed': False,
        'was_created': True,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }
    action = Action.objects.get(action='role_node_created')
    assert action.requester_id == arda_users.frodo.id
    assert action.data['from_api'] is True
    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.slug == 'division'
    assert node.is_key is True
    assert node.name == 'Дивизион'
    assert node.name_en == 'Division'
    assert node.is_public is True

    response = client.json.put(key_url, new_key_node_data)
    assert response.status_code == 201
    assert response.wsgi_request.node_modification_info == {
        'method': 'PUT',
        'can_upsert': True,
        'was_moved': False,
        'was_renamed': False,
        'was_created': False,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }
    assert Action.objects.filter(action='role_node_created').count() == 1
    assert Action.objects.filter(action='role_node_changed').count() == 1
    role_node_changed = Action.objects.get(action='role_node_changed')
    assert role_node_changed.requester_id == arda_users.frodo.id
    assert role_node_changed.data['from_api'] is True
    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.slug == 'division'
    assert node.is_key is True
    assert node.name == 'Группа'
    assert node.name_en == 'Group'
    assert node.is_public is True


def test_idempotent_put(client, api_name, complex_system, arda_users):
    # PUT /api/v1/rolenodes/
    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)

    key_url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/manager/division/',
    )
    key_node_data = {
        'create': True,
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
    }

    response = client.json.put(key_url, key_node_data)
    assert response.status_code == 201
    assert response.wsgi_request.node_modification_info['was_created']
    action = Action.objects.get(action='role_node_created')
    assert action.requester_id == arda_users.frodo.id
    assert action.data['from_api'] is True
    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.slug == 'division'
    assert node.is_key is True
    assert node.name == 'Дивизион'
    assert node.name_en == 'Division'
    assert node.is_public is True

    response = client.json.put(key_url, key_node_data)
    assert response.status_code == 201
    assert not response.wsgi_request.node_modification_info['was_created']
    assert Action.objects.filter(action='role_node_created').count() == 1
    assert Action.objects.filter(action='role_node_changed').count() == 0  # Фактических изменений не было
    create_action = Action.objects.filter(action='role_node_created').order_by('pk').last()
    assert create_action.requester_id == arda_users.frodo.id
    assert create_action.data['from_api'] is True
    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.slug == 'division'
    assert node.is_key is True
    assert node.name == 'Дивизион'
    assert node.name_en == 'Division'
    assert node.is_public is True

    response = client.json.put(key_url, key_node_data)
    assert response.status_code == 201
    assert not response.wsgi_request.node_modification_info['was_created']
    assert Action.objects.filter(action='role_node_created').count() == 1
    assert Action.objects.filter(action='role_node_changed').count() == 0  # Фактических изменений не было
    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.slug == 'division'
    assert node.is_key is True
    assert node.name == 'Дивизион'
    assert node.name_en == 'Division'
    assert node.is_public is True


def test_add_node_simple_strings(client, api_name, complex_system, arda_users):
    # POST /api/v1/rolenodes/
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': 'Division'
    }
    node_data_division = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/division/',
        'slug': 'nord',
        'name': 'North division',
        'help': 'Help division',
        'visibility': True,
        'set': 'manager_id',
        'aliases': [{
            'name': 'Alias'
        }],
        'fields': [{
            'is_required': False,
            'name': 'Field',
            'slug': 'division_nick',
        }],
        'responsibilities': [{
            'username': 'sauron',
            'notify': True,
        }]
    }

    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    response = client.json.post(url, node_data)
    assert response.status_code == 201

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.name == node.name_en == 'Division'

    response = client.json.post(url, node_data_division)
    assert response.status_code == 201
    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/nord/')
    assert node.name == node.name_en == 'North division'
    field = node.fields.get()
    assert field.name == field.name_en == 'Field'
    assert field.options is None
    alias = node.aliases.get()
    assert alias.name == alias.name_en == 'Alias'
    complex_system.root_role_node.rehash()
    assert_stable_hash(complex_system)


def test_node_alias(client, api_name, complex_system, arda_users):
    # POST /api/v1/rolenodes/
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
        'aliases': [{
            'name': 'Алиас',
        }],
    }
    response = client.json.post(url, data=node_data)
    assert response.status_code == 201

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.aliases.all().count() == 1
    alias = node.aliases.get()
    assert alias.name == 'Алиас'
    assert alias.name_en == 'Алиас'


@pytest.mark.parametrize('comment_required', (True, False))
def test_create__set_comment_required(client, api_name, complex_system, arda_users, comment_required):
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    client.login('frodo')
    response = client.json.post(reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes'), data={
        'system': complex_system.slug,
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
        'comment_required': comment_required,
    })
    assert response.status_code == 201

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert node.comment_required == comment_required


def test_node_bad_alias(client, api_name, complex_system, arda_users):
    # POST /api/v1/rolenodes/
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
        'aliases': [{
            'name': ['Алиас', 'Alias'],
        }],
    }
    response = client.json.post(url, node_data)
    assert response.status_code == 400
    expected = ["Название: Неверный формат поля"]
    assert response.json()['errors']['node_aliases'] == expected


def test_add_same_node_different_systems(client, api_name, simple_system, other_system, arda_users):
    # POST /api/v1/rolenodes/
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    node_data = {
        'parent': '/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        }
    }

    add_perms_by_role('tree_edit', arda_users['frodo'], system=simple_system)
    simple_node_data = node_data.copy()
    simple_node_data['system'] = 'simple'
    response = client.json.post(url, simple_node_data)
    assert response.status_code == 201

    add_perms_by_role('tree_edit', arda_users['frodo'], system=other_system)
    other_node_data = node_data.copy()
    other_node_data['system'] = 'other'
    response = client.json.post(url, other_node_data)
    assert response.status_code == 201


def test_add_node_ctt_hook(client, api_name, complex_system, arda_users):
    # POST /api/v1/rolenodes/
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        }
    }

    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    response = client.json.post(url, node_data)
    assert response.status_code == 201

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    assert ctt_data_is_consistent(node)


def test_add_node_field(client, api_name, complex_system, arda_users):
    # POST /api/v1/rolenodes/
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
        'fields': [{
            'options': 'sudo',
            'slug': 'sudo',
            'required': True,
            'name': {
                'ru': 'Поле',
                'en': 'Field'
            },
        }],
    }

    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    response = client.json.post(url, node_data)
    assert response.status_code == 400
    assert response.json()['errors']['node_fields'] == ['Опции поля должны быть словарем']

    node_data['fields'][0]['options'] = {'validators': 'sudo'}
    response = client.json.post(url, node_data)
    assert response.status_code == 400
    assert response.json()['errors']['node_fields'] == ['Валидаторы поля должны быть списком строк']

    node_data['fields'][0]['options'] = {'validators': ['sudo']}
    response = client.json.post(url, node_data)
    assert response.status_code == 400
    assert response.json()['errors']['node_fields'] == ['Валидатор <sudo> неизвестен']

    node_data['fields'][0]['options'] = {'validators': ['sudoers_entry']}
    response = client.json.post(url, node_data)
    assert response.status_code == 201

    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    field = node.fields.get()
    assert field.is_required is True
    with assert_num_queries_lte(20):
        complex_system.root_role_node.rehash()
    assert_stable_hash(complex_system)


def test_add_node_responsibilities(client, api_name, complex_system, arda_users):
    """POST /api/v1/rolenodes/"""

    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    responsibilities = [{
        'username': 'sam',
        'notify': True,
    }, {
        'username': 'legolas',
        'notify': False,
    }]
    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        },
        'responsibilities': 'hello',
    }

    add_perms_by_role('tree_edit', arda_users.frodo, system=complex_system)

    response = client.json.post(url, node_data)
    assert response.status_code == 400
    assert response.json()['errors']['node_responsibilities'] == ['Ожидается список словарей']

    node_data['responsibilities'] = [{'x': 'y'}]
    response = client.json.post(url, node_data)
    assert response.status_code == 400
    assert response.json()['errors']['node_responsibilities'] == ["Сотрудник: Обязательное поле."]

    node_data['responsibilities'] = responsibilities
    response = client.json.post(url, node_data)
    assert response.status_code == 201
    node = RoleNode.objects.get(slug_path='/project/subs/role/manager/division/')
    legolas_resp, sam_resp = node.responsibilities.order_by('user__username')
    assert legolas_resp.is_active is True
    assert legolas_resp.notify is False
    assert legolas_resp.user_id == arda_users.legolas.id
    assert sam_resp.is_active is True
    assert sam_resp.notify is True
    assert sam_resp.user_id == arda_users.sam.id


def test_change_unique_id(client, api_name, complex_system, arda_users):
    # POST /api/v1/rolenodes/
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/subs/role/developer')
    node_data = {
        'unique_id': 'developer',
    }
    add_perms_by_role('tree_edit', arda_users.frodo, system=complex_system)
    response = client.json.put(url, node_data)
    assert response.status_code == 204
    node = RoleNode.objects.get_node_by_value_path(complex_system, '/subs/developer/')
    assert node.unique_id == 'developer'
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/subs/role/manager')
    response = client.json.put(url, node_data)
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'unique_id': ['В данной системе уже есть такой unique_id: developer.']
        }
    }


def test_recover_depriving_node(client, simple_system, arda_users, department_structure, api_name):
    # POST /api/v1/rolenodes/
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    frodo = arda_users.frodo
    node = simple_system.nodes.get(slug='admin')
    node.mark_depriving()
    node.refresh_from_db()
    assert node.state == 'depriving'
    client.login('frodo')
    add_perms_by_role('tree_edit', frodo)
    node_data = {
        'system': 'simple',
        'parent': '/role/',
        'slug': 'admin',
        'name': {
            'ru': 'admin',
            'en': 'admin',
        },
    }

    # Легального способа сдеать дубль быть не должно, но если вдруг, то мы оживим последнюю ноду
    queue = RoleNodeQueue(system=simple_system)
    canonical_node = CanonicalNode(hash='', slug='admin', name='admin', name_en='admin')
    queue.push_addition(node=node.parent, child_data=canonical_node, in_auto_mode=True)
    queue.apply(user=frodo, from_api=True)
    clone = simple_system.nodes.active().get(slug_path=node.slug_path)
    clone.mark_depriving()

    nodes_count = simple_system.nodes.count()
    response = client.json.post(url, node_data)
    assert response.wsgi_request.node_modification_info == {
        'method': 'POST',
        'has_unique_id': False,
        'was_restored': True,
        'was_moved': False,
        'impersonator': '<UNDEFINED>',
        'impersonated': 'frodo',
    }
    assert simple_system.nodes.count() == nodes_count
    node.refresh_from_db()
    assert clone.state == 'active'
    assert node.state == 'depriving'


@waffle.testutils.override_switch('idm.move_node_via_post_denied', active=True)
def test_move_node_via_api(client, api_name, complex_system, arda_users):
    """Попробуем переместить узел через API"""
    role = raw_make_role(arda_users.frodo, complex_system, {'project': 'subs', 'role': 'developer'})
    assert role.node.slug_path == '/project/subs/role/developer/'
    client.login('frodo')
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/subs/role/developer')
    add_perms_by_role('tree_edit', arda_users.frodo, system=complex_system)
    response = client.json.put(url, {
        'unique_id': 'developer',
    })
    assert response.status_code == 204
    listurl = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    response = client.json.post(listurl, {
        'system': 'complex',
        'parent': '/project/rules/role/',
        'slug': 'dev',
        'name': 'Developeur',
        'unique_id': 'developer'
    })
    assert response.status_code == 400
    assert response.json()['message'] == 'Found active node with same unique_id: developer, system: complex'

    role = refresh(role)
    assert role.node.slug_path == '/project/subs/role/developer/'
    assert role.state == 'granted'


@pytest.mark.parametrize('with_approvers', [True, False])
def test_move_node_via_api_reruns_workflow(client, api_name, complex_system, simple_system, arda_users,
                                           with_approvers, idm_robot, ad_system):
    """Попробуем переместить узел через API и убедимся, что воркфлоу роли пересматривается"""

    frodo = arda_users.frodo

    RoleField.objects.filter(node__system=complex_system).delete()

    set_workflow(complex_system, dedent('''
    if role['role'] == 'dev':
        no_email = False
        approvers = [approver('legolas')]
        email_cc = ['sauron@yandex-team.ru']
        ad_groups = ["OU=group1"]
        ref_roles = [{'system': '%(system)s', 'role_data': {'role': 'manager'}, 'role_fields': {'login': scope}}]
    else:
        approvers = [%(user)s]
        no_email = True
    ''' % {
        'system': simple_system.slug,
        'user': '"legolas"' if with_approvers else ''
    }))

    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, None)
    assert role.node.slug_path == '/project/subs/role/developer/'
    assert role.no_email is True
    assert role.refs.count() == 0
    assert role.ref_roles == []

    client.login('frodo')

    tree = ad_system.plugin.get_info()
    with mock_tree(ad_system, tree):
        sync_role_nodes(ad_system, requester=frodo)

    add_perms_by_role('tree_edit', arda_users.frodo, system=complex_system)
    url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/developer'
    )
    with run_commit_hooks():
        response = client.json.put(url, {
            'system': 'complex',
            'parent': '/project/rules/role/',
            'slug': 'dev',
            'name': 'Developeur',
        })
    assert response.status_code == 204
    role = refresh(role)
    assert role.node.slug_path == '/project/rules/role/dev/'
    assert role.no_email is False
    assert role.email_cc == {
        'granted': [{'lang': 'ru', 'email': 'sauron@yandex-team.ru', 'pass_to_personal': False}]
    }
    assert role.ad_groups == []
    expected_ref_roles = [
        {
            'system': 'simple',
            'role_data': {'role': 'manager'},
            'role_fields': {'login': '/rules/'}
        },
        {
            'system': 'ad_system',
            'role_data': {
                'type': 'roles_in_groups',
                'ad_group': 'OU=group1',
                'group_roles': 'member',
            },
        },
    ]
    assert role.ref_roles == expected_ref_roles
    assert role.actions.filter(action='rerun_workflow').count() == 1
    action = role.actions.get(action='rerun_workflow')
    assert action.user_id == frodo.id
    assert action.requester_id == idm_robot.id
    assert action.error == ''
    assert action.system_id == complex_system.id
    assert action.data == {
        'diff': {
            'email_cc': [
                {},
                {
                    'granted': [{'email': 'sauron@yandex-team.ru', 'lang': 'ru', 'pass_to_personal': False}]
                }
            ],
            'no_email': [
                True, False
            ],
            'ref_roles': [
                [],
                expected_ref_roles,
            ]
        }
    }

    if with_approvers:
        assert role.state == 'requested'
        assert role.refs.count() == 0
    else:
        assert role.state == 'granted'
        assert role.refs.count() == 2
        ref = list(role.refs.filter())[1]
        assert ref.state == 'granted'
        assert ref.fields_data == {'login': '/rules/'}


def test_move_node_via_api_reruns_parentful_workflow(client, api_name, pt1_system, arda_users, idm_robot):
    """Попробуем переместить ветку узлов через API и убедимся, что воркфлоу пересматривается даже для ролей
    с parent-ом"""

    frodo = arda_users.frodo

    RoleField.objects.filter(node__system=pt1_system).delete()

    set_workflow(pt1_system, dedent('''
    approvers = []
    if role['role'] == 'admin':
        ref_data = role.copy()
        ref_data['role'] = 'manager'
        ref_roles = [{'system': '%(system)s', 'role_data': ref_data}]
    elif role['role'] == 'manager':
        ref_data = role.copy()
        ref_data['role'] = 'developer'
        ref_roles = [{'system': '%(system)s', 'role_data': ref_data}]
        no_email = True
    ''' % {
        'system': pt1_system.slug,
    }))

    role = Role.objects.request_role(
        frodo, frodo, pt1_system, '',
        {'project': 'proj3', 'a_subproject': 'subproj1', 'role': 'admin'}
    )
    role.refresh_from_db()
    assert role.state == 'granted'
    assert role.refs.count() == 1
    manager_ref = role.refs.get()
    assert manager_ref.state == 'granted'
    assert manager_ref.refs.count() == 1
    dev_ref = manager_ref.refs.get()
    assert dev_ref.state == 'granted'
    dev_ref.fetch_node()
    assert dev_ref.node.slug_path == '/project/proj3/a_subproject/subproj1/role/developer/'

    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users.frodo, system=pt1_system)
    url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='%s/project/proj3/a_subproject/subproj1/' % pt1_system.slug,
    )
    with run_commit_hooks():
        response = client.json.put(url, {
            'system': pt1_system.slug,
            'parent': '/project/',
        })
    assert response.status_code == 204
    role = refresh(role)
    assert role.node.slug_path == '/project/subproj1/role/admin/'
    assert role.ref_roles == [{
        'system': pt1_system.slug,
        'role_data': {'role': 'manager', 'project': 'subproj1'},
    }]
    assert role.actions.filter(action='rerun_workflow').count() == 1
    action = role.actions.get(action='rerun_workflow')
    assert action.user_id == frodo.id
    assert action.requester_id == idm_robot.id
    assert action.error == ''
    assert action.system_id == pt1_system.id
    assert role.refs.count() == 1
    manager_ref.refresh_from_db()
    assert manager_ref.ref_roles == [{
        'system': pt1_system.slug,
        'role_data': {'role': 'developer', 'project': 'subproj1'},
    }]
    assert manager_ref.refs.count() == 1
    dev_ref.refresh_from_db()
    assert dev_ref.state == 'granted'


def test_rerun_workflow_with_no_changes(client, api_name, complex_system, arda_users, idm_robot):
    """Попробуем переместить узел через API и убедимся, что воркфлоу роли пересматривается"""

    frodo = arda_users.frodo

    RoleField.objects.filter(node__system=complex_system).delete()
    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, None)
    assert role.node.slug_path == '/project/subs/role/developer/'

    client.login('frodo')
    add_perms_by_role('tree_edit', arda_users.frodo, system=complex_system)
    url = reverse(
        'api_dispatch_detail',
        api_name=api_name,
        resource_name='rolenodes',
        slug_path='complex/project/subs/role/developer'
    )
    with run_commit_hooks():
        response = client.json.put(url, {
            'system': 'complex',
            'parent': '/project/rules/role/',
            'slug': 'dev',
            'name': 'Developeur',
        })
    assert response.status_code == 204
    role = refresh(role)
    assert role.node.slug_path == '/project/rules/role/dev/'
    assert role.actions.filter(action='rerun_workflow').count() == 1
    action = role.actions.get(action='rerun_workflow')
    assert action.user_id == frodo.id
    assert action.requester_id == idm_robot.id
    assert action.error == ''
    assert action.system_id == complex_system.id
    assert action.data == {
        'diff': {}
    }


def test_add_node_same_slug(client, api_name, complex_system, arda_users):
    # POST /api/v1/rolenodes/
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        }
    }

    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    response = client.json.post(url, data=node_data)
    assert response.status_code == 201

    response = client.json.post(url, data=node_data)
    assert response.status_code == 400
    assert response.json()['message'] == 'Узел "/project/subs/role/manager/division/" уже есть на этом уровне дерева'


def test_add_key_node(client, api_name, complex_system, arda_users):
    # POST /api/v1/rolenodes/
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        }
    }

    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    response = client.json.post(url, data=node_data)
    assert response.status_code == 201

    node_data['slug'] = 'division2'
    response = client.json.post(url, data=node_data)
    assert response.status_code == 400
    assert response.json()['message'] == 'У родительского узла уже есть потомок, больше добавлять нельзя'


def test_move_subtree(client, api_name, complex_system, arda_users, idm_robot):
    node = RoleNode.objects.get(slug_path='/project/subs/')
    role = raw_make_role(arda_users.frodo, complex_system, {'project': 'subs', 'role': 'developer'})
    assert role.node.slug_path == '/project/subs/role/developer/'

    add_perms_by_role('tree_edit', arda_users.frodo, system=complex_system)
    client.login('frodo')

    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/subs')
    with run_commit_hooks():
        response = client.json.put(
            url,
            data={'parent': '/project/rules/role/'},
        )

    assert response.status_code == 204

    action = Action.objects.get(action='role_node_moved')
    assert action.role_node_id == node.id
    assert action.data == {'from': '/project/', 'to': '/project/rules/role/'}

    node = refresh(node)
    assert node.parent.slug_path == '/project/rules/role/'
    assert node.slug_path == '/project/rules/role/subs/'
    assert node.value_path == '/rules/subs/'
    assert node.fullname == [{
        'description': '',
        'description_en': '',
        'name': 'Проект',
        'name_en': 'Проект'
    }, {
        'description': '',
        'description_en': '',
        'name': 'IDM',
        'name_en': 'IDM'
    }, {
        'description': '',
        'description_en': '',
        'name': 'роль',
        'name_en': 'роль'
    }, {
        'description': '',
        'description_en': '',
        'name': 'Подписки',
        'name_en': 'Подписки'
    }]

    leaf_node = refresh(role.node)
    assert leaf_node.slug_path == '/project/rules/role/subs/role/developer/'
    assert leaf_node.value_path == '/rules/subs/developer/'
    assert leaf_node.fullname == [{
        'description': '',
        'description_en': '',
        'name': 'Проект',
        'name_en': 'Проект'
    }, {
        'description': '',
        'description_en': '',
        'name': 'IDM',
        'name_en': 'IDM'
    }, {
        'description': '',
        'description_en': '',
        'name': 'роль',
        'name_en': 'роль'
    }, {
        'description': '',
        'description_en': '',
        'name': 'Подписки',
        'name_en': 'Подписки'
    }, {
        'description': '',
        'description_en': '',
        'name': 'роль',
        'name_en': 'роль'
    }, {
        'description': '',
        'description_en': '',
        'name': 'Разработчик',
        'name_en': 'Разработчик'
    }]

    role = refresh(role)
    assert role.node.slug_path == '/project/rules/role/subs/role/developer/'
    expected_ancestors = [
        '/',
        '/project/',
        '/project/rules/',
        '/project/rules/role/',
        '/project/rules/role/subs/',
        '/project/rules/role/subs/role/',
        '/project/rules/role/subs/role/developer/',
    ]
    assert list(leaf_node.get_ancestors(include_self=True).values_list('slug_path', flat=True)) == expected_ancestors
    expected_levels = list(range(len(expected_ancestors)))
    assert list(leaf_node.get_ancestors(include_self=True).values_list('level', flat=True)) == expected_levels
    complex_system.root_role_node.rehash()
    assert_stable_hash(complex_system)


def test_move_subtree_fail(client, api_name, complex_system, arda_users):
    add_perms_by_role('tree_edit', arda_users.frodo, system=complex_system)
    client.login('frodo')

    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/subs')

    # Несуществующий родитель
    response = client.json.put(
        url,
        data={'parent': '/project/rules/xxx/'},
    )

    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'errors': {'parent': ['Узел в дереве ролей не найден']},
        'message': 'Invalid data sent',
    }

    # Чередование нод
    response = client.json.put(
        url,
        data={'parent': '/project/rules/'},
    )

    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'errors': {'__all__': [
            'Невозможно сменить родителя узла на указанного. Ключевые и неключевые узлы должны чередоваться.'
        ]},
        'message': 'Невозможно сменить родителя узла на указанного. Ключевые и неключевые узлы должны чередоваться.',
    }

    # Перенос в свое же поддерево
    response = client.json.put(
        url,
        data={'parent': '/project/subs/role/'},
    )

    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'errors': {
            '__all__': ['Невозможно перенести узел в свое же поддерево.']},
        'message': 'Невозможно перенести узел в свое же поддерево.',
    }


def test_delete_node(client, api_name, complex_system, arda_users):
    # DELETE /api/v1/rolenodes/project/rules/
    client.login('gandalf')
    add_perms_by_role('impersonator', arda_users.gandalf, complex_system)

    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                  slug_path='complex/project/rules')

    response = client.json.get(url)
    data = response.json()
    assert data['slug_path'] == '/project/rules/'

    role = raw_make_role(arda_users['legolas'], complex_system, {'project': 'rules', 'role': 'admin'})

    response = client.json.delete(url, {'_requester': 'frodo'})
    assert response.status_code == 403

    if api_name == 'frontend':
        add_perms_by_role('tree_edit', arda_users.gandalf, system=complex_system)
    else:
        add_perms_by_role('tree_edit', arda_users.frodo, system=complex_system)
    response = client.json.delete(url, {'_requester': 'frodo'})
    assert response.status_code == 204

    response = client.json.get(url)
    assert response.status_code == 404

    role = refresh(role)
    assert role.state == 'deprived'
    action = role.node.actions.get(action='role_node_deleted')
    assert action.data['from_api'] is True
    if api_name == 'v1':
        assert action.impersonator_id == arda_users.gandalf.id
        assert action.requester_id == arda_users.frodo.id
    elif api_name == 'frontend':
        assert action.requester_id == arda_users.gandalf.id


def test_hide_deleted_nodes(client, api_name, complex_system, arda_users):
    # DELETE /api/v1/rolenodes/project/rules/
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    client.login('frodo')

    response = client.json.get(
        reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes'),
        {'system': 'complex'},
    )
    assert len(response.json()['objects']) == 11

    response = client.json.delete(
        reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                slug_path='complex/project/rules/role/auditor'),
    )
    assert response.status_code == 204

    response = client.json.get(
        reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes'),
        {'system': 'complex'},
    )
    nodes = response.json()['objects']
    assert len(nodes) == 10
    assert '/project/rules/role/auditor/' not in {node['slug_path'] for node in nodes}


def test_noneditable_system(client, api_name, complex_system, arda_users):
    complex_system.roletree_policy = 'noneditable'
    complex_system.save()
    add_perms_by_role('tree_edit', arda_users['frodo'], system=complex_system)
    client.login('frodo')

    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    node_data = {
        'system': 'complex',
        'parent': '/project/subs/role/manager/',
        'slug': 'division',
        'name': {
            'ru': 'Дивизион',
            'en': 'Division',
        }
    }
    response = client.json.post(url, data=node_data)
    assert response.status_code == 403

    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes', slug_path='complex/project/rules')
    response = client.json.put(url, data={'name': 'aaa'})
    assert response.status_code == 403

    response = client.json.delete(
        reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes',
                slug_path='complex/project/rules/role/auditor'),
    )
    assert response.status_code == 403

    response = client.json.get(
        reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes'),
        {'system': 'complex'},
    )
    assert len(response.json()['objects']) == 11


def test_put_request(client, users_for_test, simple_system, arda_users, api_name):
    # Проверим что при изменении полей, не вызывающих перепрогон workflow, не создаются actions
    client.login('admin')
    data = {
            'aliases': [],
            'fields': [],
            'name': 'Админ',
            'responsibilities': [{'notify': True, 'username': 'frodo'},
                                 {'notify': True, 'username': 'sam'},
                                 ],
            'set': None,
            'visibility': True,
    }
    url = reverse('api_dispatch_detail', api_name=api_name, resource_name='rolenodes', slug_path='/simple/role/admin')
    role = raw_make_role(arda_users['frodo'], simple_system, {'role': 'admin'}, state='approved')
    response = client.json.put(url, data=data)
    assert response.status_code == 204
    node = RoleNode.objects.filter(system=simple_system, name='Админ').get()
    responsibilities_username = [resp.user.username for resp in node.responsibilities.select_related('user')]
    assert sorted(responsibilities_username) == ['frodo', 'sam']
    assert Action.objects.filter(action='rerun_workflow').count() == 0


def test_state_filter(client, complex_system, simple_system, arda_users, api_name):
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes')
    simple_system.nodes.update(state='deprived')
    data = client.json.get(url).json()['objects']
    assert len(data) == complex_system.nodes.count()
    assert {node['id'] for node in data} == set(complex_system.nodes.values_list('id', flat=True))

    data = client.json.get(url, {'state': 'deprived'}).json()['objects']
    assert len(data) == simple_system.nodes.count()
    assert {node['id'] for node in data} == set(simple_system.nodes.values_list('id', flat=True))

    data = client.json.get(url, {'state': 'all'}).json()['objects']
    assert len(data) == RoleNode.objects.count()
    assert {node['id'] for node in data} == set(RoleNode.objects.values_list('id', flat=True))


@pytest.mark.parametrize('fields', (set(), {'slug', 'state', 'is_key'}))
def test_list__specify_fields(client, simple_system, arda_users, api_name: str, fields: set):
    client.login('frodo')
    response = client.json.get(
        reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes'),
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


def test_list__specify_unknown_fields(client, simple_system, arda_users, api_name):
    client.login('frodo')
    unknown_field = 'unknown_field'
    response = client.json.get(
        reverse('api_dispatch_list', api_name=api_name, resource_name='rolenodes'),
        data={'fields': f'slug,{unknown_field}'},
    )
    assert response.status_code == 400, response.json()
    error = response.json()
    assert error['message'] == f'Unknown fields passed in query: {unknown_field}'


@pytest.mark.parametrize('fields', (set(), {'slug', 'state', 'is_key'}))
def test_get__specify_fields(client, simple_system, arda_users, api_name: str, fields: set):
    client.login('frodo')

    node = simple_system.nodes.get(slug='manager')
    node.unique_id = random_slug()
    node.save(update_fields=['unique_id'])
    response = client.json.get(
        reverse(
            'api_dispatch_detail',
            api_name=api_name,
            resource_name='rolenodes',
            slug_path=f'{simple_system.slug}/unique_id/{node.unique_id}',
        ),
        data={'fields': ','.join(fields)},
    )
    assert response.status_code == 200, response.json()
    node_data = response.json()
    if fields:
        assert set(node_data.keys()) == fields
    else:
        # при пустом значении возвращаем все поля
        assert node_data != {}


def test_get__specify_unknown_fields(client, simple_system, arda_users, api_name):
    client.login('frodo')
    unknown_field = 'unknown_field'
    node = simple_system.nodes.get(slug='manager')
    node.unique_id = random_slug()
    node.save(update_fields=['unique_id'])
    response = client.json.get(
        reverse(
            'api_dispatch_detail',
            api_name=api_name,
            resource_name='rolenodes',
            slug_path=f'{simple_system.slug}/unique_id/{node.unique_id}',
        ),
        data={'fields': f'slug,{unknown_field}'},
    )
    assert response.status_code == 400, response.json()
    error = response.json()
    assert error['message'] == f'Unknown fields passed in query: {unknown_field}'
