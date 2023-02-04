# coding: utf-8
"""Тесты бизнес-логики, относящейся к запросу и выдаче роли.
В данных тестах не тестируются вьюхи, только модели"""



import pytest

from idm.core.workflow.exceptions import PassportLoginPolicyError
from idm.core.models import Role
from idm.tests.utils import add_perms_by_role, set_roles_tree, raw_make_role

pytestmark = pytest.mark.django_db


def test_request_role_for_system_with_unique_for_user_passport_policy(users_for_test, simple_system):
    """При попытке запросить в системе с политикой "один паспортный логин на все роли" для пользователя,
     у которого уже есть роль, привязанная к логину, второй роли на другой логин, она не должна быть выдана"""
    art, fantom, terran, admin = users_for_test
    set_roles_tree(simple_system, {
        'code': 0,
        'roles': {
            'values': {
                'superuser': 'суперпользователь',
                'manager': 'менеджер'
            },
            'name': 'роль',
            'slug': 'role'
        },
        'fields': [
            {
                'slug': 'passport-login',
                'required': True,
                'name': 'Паспортный логин',
            }
        ]
    })
    add_perms_by_role('responsible', art, simple_system)
    simple_system.passport_policy = 'unique_for_user'
    simple_system.save()
    raw_make_role(fantom, simple_system, {'role': 'manager'}, fields_data={'passport-login': 'fantom'})
    assert Role.objects.count() == 1
    with pytest.raises(PassportLoginPolicyError):
        Role.objects.request_role(
            art, fantom, simple_system, comment='',
            data={'role': 'superuser'}, fields_data={'passport-login': 'yndx-fantom'}
        )


def test_request_role_for_system_with_unique_for_user_passport_policy_in_generic_system(users_for_test, generic_system):
    """При попытке запросить в системе с политикой "один паспортный логин на все роли" для пользователя,
     у которого уже есть роль, привязанная к логину, второй роли на другой логин, она не должна быть выдана"""
    art, fantom, terran, admin = users_for_test
    set_roles_tree(generic_system, {
        'code': 0,
        'roles': {
            'values': {
                'superuser': 'суперпользователь',
                'manager': 'менеджер'
            },
            'name': 'роль',
            'slug': 'role'
        },
        'fields': [
            {
                'slug': 'passport-login',
                'required': True,
                'name': 'Паспортный логин',
            }
        ]
    })
    add_perms_by_role('responsible', art, generic_system)
    generic_system.passport_policy = 'unique_for_user'
    generic_system.save()
    raw_make_role(fantom, generic_system, {'role': 'manager'}, fields_data={'passport-login': 'fantom'})
    assert Role.objects.count() == 1
    with pytest.raises(PassportLoginPolicyError):
        Role.objects.request_role(
            art, fantom, generic_system, comment='',
            data={'role': 'superuser'}, fields_data={'passport-login': 'yndx-fantom'}
        )
