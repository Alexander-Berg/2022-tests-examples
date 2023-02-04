# coding: utf-8


import pytest

from idm.core.workflow.exceptions import Forbidden as Forbidden
from idm.core.models import Role
from idm.permissions import shortcuts as permissions_shortcuts
from idm.tests.utils import add_perms_by_role, refresh, set_workflow
from idm.utils import reverse

pytestmark = [
    pytest.mark.django_db,
]


def test_add_role_for_another(simple_system, users_for_test):
    """Проверяет запрос роли одним пользователем для другого, без разрешения и с ним"""
    (art, fantom, terran, admin) = users_for_test

    # попробуем без роли Управление ролями добавить роль другому пользователю
    admin_node = simple_system.nodes.get(slug='admin')

    assert not permissions_shortcuts.can_request_role(art, fantom, simple_system, admin_node, {})

    # теперь как бы выдадим роль запрашивающему (добавим его в группу Управление ролями для системы)
    add_perms_by_role('roles_manage', art, simple_system)

    fantom = refresh(fantom)

    assert permissions_shortcuts.can_request_role(art, fantom, simple_system, admin_node, {})


def test_role_perms_for_robot_owners(simple_system, arda_users, robot_gollum, client):
    frodo, gandalf = arda_users.frodo, arda_users.gandalf
    simple_system.request_policy = 'subordinates'
    simple_system.save()
    add_perms_by_role('superuser', gandalf, simple_system)
    roles_url = reverse('api_dispatch_list', api_name='frontend', resource_name='roles')

    client.login('frodo')

    # frodo не может запросить роль для робота
    with pytest.raises(Forbidden):
        Role.objects.request_role(frodo, robot_gollum, simple_system, '', {'role': 'admin'}, None)

    # пусть некто третий запросит роль
    role1 = Role.objects.request_role(gandalf, robot_gollum, simple_system, '', {'role': 'admin'}, None)
    role1.refresh_from_db()

    # frodo не видит эту роль
    client.login('frodo')
    rolelist = client.json.get(roles_url)
    assert len(rolelist.json()['objects']) == 0

    # frodo не может отозвать роль
    with pytest.raises(Forbidden):
        role1.deprive_or_decline(frodo)

    # делаем frodo робовладельцем
    robot_gollum.responsibles.add(frodo)

    # владелец робота успешно запрашивает роль
    role2 = Role.objects.request_role(frodo, robot_gollum, simple_system, '', {'role': 'manager'}, None)

    # владелец видит обе роли робота
    rolelist = client.json.get(roles_url)
    assert len(rolelist.json()['objects']) == 2
    assert {role1.pk, role2.pk} == {obj['id'] for obj in rolelist.json()['objects']}

    # успешно отзывает роль
    role1.deprive_or_decline(frodo)

    # Видит, что роль стала неактивной
    rolelist = client.json.get(roles_url)
    assert [False] == [obj['is_active'] for obj in rolelist.json()['objects'] if obj['id'] == role1.pk]


def test_roles_manage_can_deprive_any_role(simple_system, arda_users):
    set_workflow(simple_system, 'approvers = []')
    frodo = arda_users.frodo
    bilbo = arda_users.bilbo

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role.refresh_from_db()
    assert role.state == 'granted'
    assert not permissions_shortcuts.can_deprive_role(bilbo, role)

    add_perms_by_role('roles_manage', bilbo, simple_system)
    assert permissions_shortcuts.can_deprive_role(bilbo, role)
