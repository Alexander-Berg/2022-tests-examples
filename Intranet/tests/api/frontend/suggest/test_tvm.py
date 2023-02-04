# coding: utf-8
import pytest

from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.users.constants.group import GROUP_TYPES
from idm.users.constants.user import USER_TYPES
from idm.users.models import User, Group, GroupMembership, GroupResponsibility
from idm.tests.utils import add_perms_by_role, remove_perms_by_role
from idm.utils import reverse

pytestmark = pytest.mark.django_db


def test_tvm_managers(users_for_test):
    art, fantom, terran, admin = users_for_test

    tvm_app = User.objects.create(type=USER_TYPES.TVM_APP, username='tvm_app')
    group = Group.objects.create(type=GROUP_TYPES.TVM_SERVICE, name='tvm service')
    Group.objects.create(
        external_id=1234,
        name='tvm_service',
        name_en='tvm_service',
        slug='tvm_service',
        type=GROUP_TYPES.TVM_SERVICE,
    )
    GroupMembership.objects.create(user=tvm_app, group=group, is_direct=True, state=GROUPMEMBERSHIP_STATE.ACTIVE)
    GroupResponsibility.objects.create(user=art, group=group, is_active=True, rank='admin')

    assert tvm_app in art.get_managed_tvm_apps()
    assert art not in art.get_managed_tvm_apps()


def test_tvm_suggest(client, arda_users):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/tvm/all')

    frodo = arda_users['frodo']
    client.login('frodo')

    tvm_apps = [
        User.objects.create(type=USER_TYPES.TVM_APP, username=10*i, first_name=f'tvm_{i}')
        for i in range(1, 4)
    ]

    groups = [
        Group.objects.create(
            external_id=1357*i,
            name='tvm_service_'+str(i),
            name_en='tvm_service_'+str(i),
            slug='tvm_service_'+str(i),
            type=GROUP_TYPES.TVM_SERVICE,
        )
        for i in range(1, 4)
    ]

    for app, group in zip(tvm_apps, groups):
        GroupMembership.objects.create(user=app, group=group, is_direct=True, state=GROUPMEMBERSHIP_STATE.ACTIVE)

    for group in groups[:-1]:
        GroupResponsibility.objects.create(user=frodo, group=group, is_active=True, rank='manager')

    response = client.json.get(suggest_url)

    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': '10', 'name': 'tvm_1 (10)', 'slug': '10'},
            {'id': '20', 'name': 'tvm_2 (20)', 'slug': '20'},
        ],
    }

    response = client.json.get(suggest_url, {'limit': 1})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 1},
        'data': [
            {'id': '10', 'name': 'tvm_1 (10)', 'slug': '10'},
        ]
    }

    response = client.json.get(suggest_url, {'q': 'vm_1'})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': '10', 'name': 'tvm_1 (10)', 'slug': '10'},
        ]
    }

    response = client.json.get(suggest_url, {'q': '1'})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': '10', 'name': 'tvm_1 (10)', 'slug': '10'},
        ]
    }

    response = client.json.get(suggest_url, {'q': '3'})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': []
    }

    response = client.json.get(suggest_url, {'offset': 'a'})
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'offset': ['Ожидается число']
        }
    }

    tvm_app = arda_users.tvm_app
    # superuser и developer видят всё
    for role in ('superuser', 'developer'):
        add_perms_by_role(role, frodo)
        response = client.json.get(suggest_url)
        assert response.status_code == 200
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': [
                {'id': tvm_app.username, 'name': f'{tvm_app.first_name} ({tvm_app.username})', 'slug': tvm_app.username},
                {'id': '10', 'name': 'tvm_1 (10)', 'slug': '10'},
                {'id': '20', 'name': 'tvm_2 (20)', 'slug': '20'},
                {'id': '30', 'name': 'tvm_3 (30)', 'slug': '30'},
            ],
        }
        remove_perms_by_role(role, frodo)
