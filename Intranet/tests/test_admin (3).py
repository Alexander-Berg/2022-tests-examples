import json
from unittest.mock import patch, call

import pytest
from django.conf import settings
from django.test import override_settings

from idm.core.constants.role import ROLE_STATE
from idm.core.models import RoleNode, Role
from idm.tests.utils import (
    add_perms_by_role,
    clear_mailbox,
    create_user,
    enable_intrasearch_pushes,
    raw_make_role,
    refresh,
    run_commit_hooks,
    set_workflow,
)
from idm.users.constants.group import GROUP_TYPES
from idm.users.models import Group
from idm.utils import reverse

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('remove_by', ['is_public', 'is_active'])
def test_admin_rolenode_push(client, arda_users, simple_system, remove_by):
    add_perms_by_role('superuser', arda_users.frodo)
    client.login('frodo')
    url = reverse('admin:core_rolenode_add')
    detail_url = reverse(
        'api_dispatch_detail',
        api_name='v1',
        resource_name='rolenodes',
        slug_path='simple/role/test_role'
    )

    data = {
        'parent': simple_system.root_role_node.children.get().pk,
        'slug': 'test_role',
        'state': 'active',
        'system': simple_system.pk,
        'name': 'Тестовая роль',
        'name_en': 'Role for test',
        'is_public': True,
    }

    for form in ('responsibilities', 'aliases', 'fields'):
        data.update({
            '-'.join([form, 'TOTAL_FORMS']): '0',
            '-'.join([form, 'INITIAL_FORMS']): '0',
        })

    expected_args = (settings.IDM_INTRASEARCH_PUSH_URL.format(source_type='rolenodes'), )

    with enable_intrasearch_pushes() as pusher:
        # добавление
        with run_commit_hooks():
            client.post(url, data)
        node = RoleNode.objects.get(slug='test_role')
        args, kwargs = pusher.http_post.call_args
        assert args == expected_args
        pushed_data = json.loads(kwargs['data'])
        assert pushed_data.get('updated_at', None)
        response = client.json.get(detail_url).json()
        del pushed_data['updated_at']
        del response['updated_at']
        assert pushed_data == response
        pusher.http_post.reset_mock()

        data.update({key: value for key, value in pushed_data.items() if key in (
            'parent',
            'is_key',
            'slug_path',
            'value_path',
        )})

        modify_url = reverse('admin:core_rolenode_change', RoleNode.objects.last().id)

        if remove_by == 'is_public':
            data['state'] = 'deprived'
        else:
            data['is_public'] = False
        with run_commit_hooks():
            client.post(modify_url, data)
        args, kwargs = pusher.http_delete.call_args
        assert args == expected_args
        pushed_data = json.loads(kwargs['data'])
        assert pushed_data.keys() == {'slug_path', 'system', 'updated_at', 'id'}
        expected = {
            'slug_path': '/role/test_role/',
            'system': {
                'slug': simple_system.slug
            },
            'id': node.id,
        }
        assert {key: value for key, value in pushed_data.items() if key in ('slug_path', 'system', 'id')} == expected
        pusher.http_post.reset_mock()


@pytest.mark.parametrize('remove_by', ['is_public', 'is_active'])
def test_admin_rolenode_change(client, arda_users, simple_system, remove_by):
    add_perms_by_role('superuser', arda_users.frodo)
    client.login('frodo')
    url = reverse('admin:core_rolenode_add')
    detail_url = reverse(
        'api_dispatch_detail',
        api_name='v1',
        resource_name='rolenodes',
        slug_path='simple/role/test'
    )

    data = {
        'parent': simple_system.root_role_node.children.get().pk,
        'slug': 'test',
        'state': 'active',
        'system': simple_system.pk,
        'name': 'Тест',
        'name_en': 'Test',
        'is_public': True,
    }

    for form in ('responsibilities', 'aliases', 'fields'):
        data.update({
            '-'.join([form, 'TOTAL_FORMS']): '0',
            '-'.join([form, 'INITIAL_FORMS']): '0',
        })

    # Установи workflow с условием any_from(system.all_users_with_role())
    legolas = arda_users.legolas
    raw_make_role(legolas, simple_system, {'role': 'admin'}, state='approved')
    workflow = "approvers = any_from(system.all_users_with_role({'role': 'admin'}, among_states='almost_active'))"
    set_workflow(simple_system, workflow)

    expected_args = (settings.IDM_INTRASEARCH_PUSH_URL.format(source_type='rolenodes'), )
    with enable_intrasearch_pushes() as pusher:

        # Добавим новый узел
        with run_commit_hooks():
            client.post(url, data)
        args, kwargs = pusher.http_post.call_args
        assert args == expected_args
        pushed_data = json.loads(kwargs['data'])
        assert pushed_data.get('updated_at', None)
        response = client.json.get(detail_url).json()
        del pushed_data['updated_at']
        del response['updated_at']
        assert pushed_data == response
        pusher.http_post.reset_mock()

        # Проверим, что узел создался
        node = RoleNode.objects.get(slug_path='/role/test/')
        assert node.name_en == 'Test'

        # Запросим роль на новый узел и проверим запрос
        aragorn = arda_users.aragorn
        role = Role.objects.request_role(aragorn, aragorn, simple_system, '', {'role': 'test'}, None)
        role = refresh(role)
        assert role.state == 'requested'
        clear_mailbox()
        assert Role.objects.count() == 2
        approvers = role.requests.values_list('approves__requests__approver', flat=True)
        assert list(approvers) == [legolas.pk]

        # Изменим данные в новом узле
        data.update({key: value for key, value in pushed_data.items() if key in (
            'parent',
            'is_key',
            'slug_path',
            'value_path',
        )})
        modify_url = reverse('admin:core_rolenode_change', RoleNode.objects.get(slug_path='/role/test/').id)
        data.update({'name_en': 'Test_new', 'data': json.dumps(node.data), 'fullname': json.dumps(node.fullname)})
        response = client.post(modify_url, data)
        pusher.http_post.reset_mock()

        # Проверим изменения в узле
        node = RoleNode.objects.get(slug_path='/role/test/')
        assert node.name_en == 'Test_new'

        # Запросим роль на измененный узел и проверим запрос
        varda = arda_users.varda
        role = Role.objects.request_role(varda, varda, simple_system, '', {'role': 'test'}, None)
        role = refresh(role)
        assert role.state == 'requested'
        approvers = role.requests.values_list('approves__requests__approver', flat=True)
        assert list(approvers) == [legolas.pk]
        clear_mailbox()
        assert Role.objects.count() == 3


@pytest.mark.parametrize('role_state, is_confirmed, is_deprived', (
    (ROLE_STATE.GRANTED, True, False),
    (ROLE_STATE.DEPRIVING_VALIDATION, False, False),
    (ROLE_STATE.DEPRIVING_VALIDATION, True, True),
))
@patch('idm.core.admin.DepriveDeprivingRoles.delay')
def test_admin_role_deprive_depriving(mocked_deprive_task, client, simple_system,
                                      role_state, is_confirmed, is_deprived):
    """
    Проверяет действие отзыва ролей в админке
    """
    role = raw_make_role(
        subject=create_user('user'),
        system=simple_system,
        data={'role': 'manager'},
        state=role_state,
    )
    superuser = create_user('superuser', superuser=True)
    client.login(superuser.username)

    url = reverse('admin:core_role_changelist')
    data = {
        'action': 'deprive_depriving_roles',
        '_selected_action': role.id,
    }
    if is_confirmed:
        data['is_confirmed'] = True
    client.post(url, data)

    assert mocked_deprive_task.called is is_deprived
    if is_deprived:
        mocked_deprive_task.assert_called_once_with(
            depriver_id=None,
            roles_ids=[role.id],
            block=True,
        )


@pytest.mark.parametrize('group_type', (
    GROUP_TYPES.DEPARTMENT,
    GROUP_TYPES.WIKI,
    GROUP_TYPES.SERVICE,
))
@patch('idm.core.depriving.registry.get_repository')
@patch('idm.core.admin.DepriveDeprivingRoles.delay')
@override_settings(IDM_STAFF_OAUTH_TOKEN='xxx')
def test_admin_role_deprive_depriving_check_group(mocked_deprive_task, mocked_get_repo, client,
                                                  simple_system, group_roots, group_type):
    """
    Проверяет, что срабатывает проверка на вхождение в группу при отзыве ролей через админку
    """
    role = raw_make_role(
        subject=create_user('user'),
        system=simple_system,
        data={'role': 'manager'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
    )
    root = Group.objects.get_root(group_type)
    group = Group.objects.create(
        type=group_type,
        slug='epic-group',
        parent=root,
        external_id=100500,
    )
    superuser = create_user('superuser', superuser=True)

    client.login(superuser.username)
    client.post(reverse('admin:core_role_changelist'), {
        'action': 'deprive_depriving_roles',
        '_selected_action': role.id,
        'check': 'group',
        'slug': group.slug,
    })

    assert not mocked_deprive_task.called
    expected_params = {
        '_limit': 1000,
        '_sort': 'id',
        '_fields': 'person.login',
        'person.login': 'user',
        'group.url': 'epic-group',
    }
    mocked_get_repo().getiter.assert_has_calls([call(expected_params)])
    if group_type == GROUP_TYPES.DEPARTMENT:
        expected_params['group.ancestors.url'] = expected_params.pop('group.url')
        mocked_get_repo().getiter.assert_has_calls([call(expected_params)])
