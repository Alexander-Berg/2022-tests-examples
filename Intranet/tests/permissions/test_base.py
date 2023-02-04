# coding: utf-8


import pytest
from idm.core.models import System
from idm.utils import reverse
from idm.tests.utils import create_user, add_perms_by_role, remove_perms_by_role


# разрешаем использование базы в тестах
pytestmark = [
    pytest.mark.django_db,
]


def test_global_and_system_permissions(simple_system, generic_system):
    """Проверяем работу пермишенов"""
    frodo = create_user('frodo')
    sam = create_user('sam')

    # проверяем что пермишен работает глобально
    add_perms_by_role('security', frodo)

    assert frodo.has_internal_role('security')
    assert frodo.has_perm('core.request_role')
    assert not frodo.has_perm('core.test_perm')
    assert simple_system.is_permitted_for(frodo, 'core.request_role')
    assert generic_system.is_permitted_for(frodo, 'core.request_role')
    assert set(System.objects.permitted_for(frodo, 'core.request_role')) == {simple_system, generic_system}

    # проверяем что пермишен работает только в системе
    add_perms_by_role('roles_manage', sam, simple_system)

    assert sam.has_internal_role('roles_manage', simple_system)
    assert not sam.has_perm('core.request_role')
    assert simple_system.is_permitted_for(sam, 'core.request_role')
    assert not generic_system.is_permitted_for(sam, 'core.request_role')
    assert set(System.objects.permitted_for(sam, 'core.request_role')) == {simple_system}

    assert set(simple_system.get_users_with_permissions('core.request_role')) == {frodo, sam}
    assert set(generic_system.get_users_with_permissions('core.request_role')) == {frodo}

    john = create_user('john')
    for role in ('superuser', 'developer'):
        add_perms_by_role(role, john)
        assert generic_system.is_permitted_for(john, 'core.edit_system')
        assert generic_system.is_permitted_for(john, 'core.edit_system_extended')
        remove_perms_by_role(role, john)

    bob = create_user('bob')
    assert not generic_system.is_permitted_for(bob, 'core.edit_system')
    assert not generic_system.is_permitted_for(bob, 'core.edit_system_extended')


def test_check_roles_for_self_developers(client, generic_system):
    frodo = create_user('frodo')
    add_perms_by_role('developer', frodo)
    url = reverse(
        'api_dispatch_detail',
        api_name='frontend',
        resource_name='systems',
        slug=generic_system.slug
    )
    data = {
        'operation': 'sync_roles',
        'dry_run': 'true',
    }
    client.login('frodo')
    response = client.json.post(url, data)
    assert response.status_code == 200
