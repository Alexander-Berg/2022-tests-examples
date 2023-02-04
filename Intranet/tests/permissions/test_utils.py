# coding: utf-8


import contextlib
from copy import deepcopy

import pytest

from django.conf import settings

from idm.permissions.utils import sync_permissions
from idm.tests.utils import create_user, add_perms_by_role, remove_perms_by_role


pytestmark = [
    pytest.mark.django_db,
]


@contextlib.contextmanager
def override_list(list_):
    listcopy = list_[:]
    yield list_
    list_[:] = listcopy


@contextlib.contextmanager
def override_dict(dict_):
    dictcopy = deepcopy(dict_)
    yield dict_
    dict_.clear()
    for key, value in dictcopy.items():
        dict_[key] = value


def test_add_common_role():
    user = create_user('terran')

    add_perms_by_role('developer', user)

    for perm in settings.IDM_COMMON_ROLES_PERMISSIONS['developer']:
        assert user.has_perm('core.%s' % perm)

    assert user.is_staff


def test_remove_common_role():
    user = create_user('terran')

    add_perms_by_role('developer', user)
    add_perms_by_role('auditor', user)

    remove_perms_by_role('developer', user)

    for perm in (set(settings.IDM_COMMON_ROLES_PERMISSIONS['developer'])
                 ^ set(settings.IDM_COMMON_ROLES_PERMISSIONS['auditor'])):
        assert not user.has_perm('core.%s' % perm)

    assert user.is_staff

    remove_perms_by_role('auditor', user)

    assert not user.is_staff


def test_add_system_role(simple_system):
    """Проверяем добавление групп с разрешениями при создании системы"""
    user = create_user('terran')

    add_perms_by_role('roles_manage', user, simple_system)

    for perm in settings.IDM_SYSTEM_ROLES_PERMISSIONS['roles_manage']:
        assert simple_system.is_permitted_for(user, 'core.%s' % perm)


def test_remove_system_role(simple_system):
    user = create_user('terran')

    add_perms_by_role('roles_manage', user, simple_system)
    remove_perms_by_role('roles_manage', user, simple_system)

    for perm in settings.IDM_SYSTEM_ROLES_PERMISSIONS['roles_manage']:
        assert not simple_system.is_permitted_for(user, 'core.%s' % perm)


def test_sync_permissions(arda_users):
    """Проверяем синхронизацию набора прав у уже выданных ролей"""

    frodo = arda_users.frodo

    add_perms_by_role('security', frodo)

    assert frodo.has_perm('core.request_role')
    assert not frodo.has_perm('core.test_perm')

    with override_list(settings.IDM_PERMISSIONS) as perms:
        perms.append(('test_perm', 'Тестовый пермишен'))
        with override_dict(settings.IDM_COMMON_ROLES_PERMISSIONS) as common_perms:
            common_perms['security'].append('test_perm')

            sync_permissions()

            assert frodo.has_perm('core.test_perm')

            for perm in ('request_role',  'test_perm'):
                common_perms['security'].remove(perm)

            sync_permissions()

            assert not frodo.has_perm('core.request_role')
            assert not frodo.has_perm('core.test_perm')


def test_sync_permissions_add_only(arda_users):
    """Проверяем синхронизацию набора прав у уже выданных ролей. Проверка случая, когда мы только добавляем права"""
    frodo = arda_users.frodo

    add_perms_by_role('security', frodo)

    assert frodo.has_perm('core.request_role')
    assert not frodo.has_perm('core.test_perm')

    with override_list(settings.IDM_PERMISSIONS) as perms:
        perms.append(('test_perm', 'Тестовый пермишен'))
        with override_dict(settings.IDM_COMMON_ROLES_PERMISSIONS) as common_perms:
            common_perms['security'].append('test_perm')
            common_perms['security'].remove('request_role')
            sync_permissions(add_only=True)

            assert frodo.has_perm('core.test_perm')
            # т.к. мы только добавляем, то пермишен сохраняется
            assert frodo.has_perm('core.request_role')


def test_sync_permissions_remove_only(arda_users):
    """Проверяем синхронизацию набора прав у уже выданных ролей. Проверка случая, когда мы только удаляем права"""

    frodo = arda_users.frodo

    add_perms_by_role('security', frodo)

    assert frodo.has_perm('core.request_role')
    assert not frodo.has_perm('core.test_perm')

    with override_list(settings.IDM_PERMISSIONS) as perms:
        perms.append(('test_perm', 'Тестовый пермишен'))
        with override_dict(settings.IDM_COMMON_ROLES_PERMISSIONS) as common_perms:
            common_perms['security'].append('test_perm')
            common_perms['security'].remove('request_role')
            sync_permissions(remove_only=True)

            # так как мы только удаляем, то удаляемое право удалится, а новое (testperm) не добавится
            assert not frodo.has_perm('core.test_perm')
            assert not frodo.has_perm('core.request_role')


def test_sync_permissions_global_only(simple_system, complex_system, arda_users):
    """Проверяем синхронизацию набора прав у уже выданных ролей.
    Проверка случая, когда изменение касается только широких полномочий"""

    frodo = arda_users.frodo

    add_perms_by_role('developer', frodo)
    add_perms_by_role('roles_manage', frodo, simple_system)

    assert frodo.has_perm('core.idm_view_roles')
    assert simple_system.is_permitted_for(frodo, 'core.request_role')
    assert not complex_system.is_permitted_for(frodo, 'core.request_role')

    with override_list(settings.IDM_PERMISSIONS) as perms:
        perms.append(('test_perm', 'Тестовый пермишен'))
        perms.append(('test_perm2', 'Тестовый пермишен 2'))
        with override_dict(settings.IDM_COMMON_ROLES_PERMISSIONS) as common_perms:
            with override_dict(settings.IDM_SYSTEM_ROLES_PERMISSIONS) as persystem_perms:
                common_perms['developer'].append('test_perm')
                common_perms['developer'].remove('idm_view_workflow')
                persystem_perms['roles_manage'].append('test_perm2')
                persystem_perms['roles_manage'].remove('request_role')
                sync_permissions(global_only=True)

                # так как мы меняем только права на глобальном уровне, то на глобальном уровне пермишены обновятся
                assert frodo.has_perm('core.test_perm')
                assert not frodo.has_perm('core.idm_view_workflow')
                # а на локальном – нет
                assert not simple_system.is_permitted_for(frodo, 'core.test_perm2')
                assert simple_system.is_permitted_for(frodo, 'core.request_role')


def test_sync_permissions_local_only(simple_system, complex_system, arda_users):
    """Проверяем синхронизацию набора прав у уже выданных ролей. Проверка случая, когда изменение касается
    только полномочий в отношении конкретной системы или узла в этой системе"""

    frodo = arda_users.frodo

    add_perms_by_role('developer', frodo)
    add_perms_by_role('roles_manage', frodo, simple_system)

    assert frodo.has_perm('core.idm_view_roles')
    assert simple_system.is_permitted_for(frodo, 'core.request_role')
    assert not complex_system.is_permitted_for(frodo, 'core.request_role')

    with override_list(settings.IDM_PERMISSIONS) as perms:
        perms.append(('test_perm', 'Тестовый пермишен'))
        perms.append(('test_perm2', 'Тестовый пермишен 2'))
        with override_dict(settings.IDM_COMMON_ROLES_PERMISSIONS) as common_perms:
            with override_dict(settings.IDM_SYSTEM_ROLES_PERMISSIONS) as persystem_perms:
                common_perms['developer'].append('test_perm')
                common_perms['developer'].remove('idm_view_workflow')
                persystem_perms['roles_manage'].append('test_perm2')
                persystem_perms['roles_manage'].remove('request_role')
                sync_permissions(local_only=True)

                # так как мы меняем только права на локальном уровне, то на глобальном уровне всё останется как было
                assert not frodo.has_perm('core.test_perm')
                assert frodo.has_perm('core.idm_view_workflow')
                # а на локальном права обновятся
                assert simple_system.is_permitted_for(frodo, 'core.test_perm2')
                assert not simple_system.is_permitted_for(frodo, 'core.request_role')
