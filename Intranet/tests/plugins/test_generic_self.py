
# coding: utf-8


import pytest

from idm.core.models import InternalRole, RoleNode, NodeResponsibility
from idm.tests.permissions.test_hooks import get_user_roles
from idm.tests.utils import create_user, add_perms_by_role


pytestmark = [pytest.mark.django_db]


def test_generic_self_get_all_roles(simple_system, client):
    """Проверяем выдачу списка ролей
    """
    frodo = create_user('frodo')
    sam = create_user('sam')
    the_ring = create_user('the-ring', type='tvm_app')
    client.login('frodo')
    simple_system.plugin_type = 'idm.core.plugins.generic_self.Plugin'
    simple_system.audit_method = 'get_roles'
    simple_system._plugin = None
    simple_system.save(update_fields=['plugin_type', 'audit_method'])

    roles = list(simple_system.plugin.get_roles())

    assert not roles

    add_perms_by_role('developer', frodo)
    add_perms_by_role('auditor', the_ring)
    add_perms_by_role('roles_manage', frodo, simple_system)
    add_perms_by_role('roles_manage', sam, simple_system, '/admin/')

    # и просто создаем internal роль, привязанную к системе, но без юзеров
    InternalRole.objects.create(
        role='some_role',
        node=RoleNode.objects.get_node_by_value_path(simple_system, '/manager/'),
    )

    roles = list(simple_system.plugin.get_roles())

    assert {(role['login'], role['subject_type']) for role in roles} == {
        ('frodo', 'user'),
        ('sam', 'user'),
        ('the-ring', 'tvm_app'),
    }

    frodo_roles = get_user_roles(roles, 'frodo', 'user')
    assert sorted(frodo_roles, key=lambda x: x['role']) == sorted([
        {'role': '/group/common/role/developer/'},
        {'scope': '/', 'role': '/group/system/system_on/simple/role/roles_manage/'},
    ], key=lambda x: x['role'])

    sam_roles = get_user_roles(roles, 'sam', 'user')
    assert sorted(sam_roles) == sorted([
        {'scope': '/admin/', 'role': '/group/system/system_on/simple/role/roles_manage/'},
    ])

    ring_roles = get_user_roles(roles, 'the-ring', 'tvm_app')
    assert sorted(ring_roles, key=lambda x: x['role']) == [
        {'role': '/group/common/role/auditor/'},
    ]


def test_get_all_roles_excludes_responsibilities(complex_system_with_responsibilities, arda_users):
    """Проверяем, что права, выданные через ответственных отфильтровываются из выдачи get-all-roles"""

    complex_system_with_responsibilities.plugin_type = 'idm.core.plugins.generic_self.Plugin'
    complex_system_with_responsibilities.audit_method = 'get_roles'
    complex_system_with_responsibilities._plugin = None
    complex_system_with_responsibilities.save(update_fields=['plugin_type', 'audit_method'])

    roles = list(complex_system_with_responsibilities.plugin.get_roles())
    assert not roles

    # но если пометить их неактивными, то выгрузка работает
    NodeResponsibility.objects.update(is_active=False)

    roles = list(complex_system_with_responsibilities.plugin.get_roles())
    assert {(role['login'], role['subject_type']) for role in roles} == {('frodo', 'user'), ('sam', 'user')}
