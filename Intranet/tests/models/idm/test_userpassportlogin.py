# coding: utf-8


import pytest
from django.test.utils import override_settings

from idm.core.models import UserPassportLogin
from idm.users.models import GroupMembership
from idm.tests.utils import (raw_make_role)

pytestmark = pytest.mark.django_db


def test_empty_login_is_not_subscribable(arda_users, simple_system):
    login = UserPassportLogin.objects.create(login='yndx-777-vasya-nagibator-777')
    assert not login.is_subscribable()


def test_login_with_good_roles_is_subscribable(arda_users, simple_system):
    # логин не привязан к пользователю, роли через create_role можно создавать на любых пользователей
    login = UserPassportLogin.objects.create(login='yndx-777-vasya-nagibator-777')
    assert not login.is_subscribable()

    roles = []
    for user in arda_users.values():
        new_role = raw_make_role(
            user, simple_system, {'role': 'admin'},
            state='granted', fields_data={'passport-login': 'yndx-777-vasya-nagibator-777'},
        )
        roles.append(new_role)
        assert login.is_subscribable()
    for i, role in enumerate(roles):
        role.set_raw_state('deprived')
        assert login.is_subscribable() == (i < len(roles) - 1)


def test_login_with_new_role_is_subscribable(arda_users, simple_system):
    frodo = arda_users.frodo
    login = UserPassportLogin.objects.create(login='yndx-777-vasya-nagibator-777')
    raw_make_role(
        frodo, simple_system, {'role': 'admin'},
        state='deprived', fields_data={'passport-login': 'yndx-777-vasya-nagibator-777'},
    )

    assert not login.is_subscribable()
    new_role = raw_make_role(
        frodo, simple_system, {'role': 'manager'},
        state='deprived', fields_data={'passport-login': 'yndx-777-vasya-nagibator-777'},
    )
    assert login.is_subscribable(new_role=new_role)


def test_login_with_role_for_bad_system_is_not_subscribable(arda_users, simple_system, other_system):
    login = UserPassportLogin.objects.create(login='yndx-777-vasya-nagibator-777')
    assert not login.is_subscribable()

    raw_make_role(
        arda_users.frodo, simple_system, {'role': 'admin'},
        state='granted', fields_data={'passport-login': 'yndx-777-vasya-nagibator-777'},
    )
    raw_make_role(
        arda_users.frodo, simple_system, {'role': 'manager'},
        state='granted', fields_data={'passport-login': 'yndx-777-vasya-nagibator-777'},
    )

    assert login.is_subscribable()
    with override_settings(IDM_SID67_EXCLUDED_SYSTEMS=['simple']):
        assert not login.is_subscribable()

    raw_make_role(
        arda_users.frodo, other_system, {'role': 'admin'},
        state='granted', fields_data={'passport-login': 'yndx-777-vasya-nagibator-777'},
    )

    with override_settings(IDM_SID67_EXCLUDED_SYSTEMS=['simple']):
        assert login.is_subscribable()
    with override_settings(IDM_SID67_EXCLUDED_SYSTEMS=['simple', 'other']):
        assert not login.is_subscribable()


def test_login_with_role_for_bad_node_is_not_subscribable(arda_users, simple_system, other_system):
    login = UserPassportLogin.objects.create(login='yndx-777-vasya-nagibator-777')
    assert not login.is_subscribable()
    excluded_nodes = {
        'simple': [
            '/role/admin/',
            '/role/poweruser/',
        ]
    }

    raw_make_role(
        arda_users.frodo, simple_system, {'role': 'admin'},
        state='granted', fields_data={'passport-login': 'yndx-777-vasya-nagibator-777'},
    )
    assert login.is_subscribable()
    with override_settings(IDM_SID67_EXCLUDED_NODES=excluded_nodes):
        assert not login.is_subscribable()

    good_role = raw_make_role(
        arda_users.frodo, simple_system, {'role': 'manager'},
        state='granted', fields_data={'passport-login': 'yndx-777-vasya-nagibator-777'},
    )
    with override_settings(IDM_SID67_EXCLUDED_NODES=excluded_nodes):
        assert login.is_subscribable()

    good_role.set_raw_state('deprived')
    new_good_role = raw_make_role(
        arda_users.frodo, other_system, {'role': 'admin'},
        state='granted', fields_data={'passport-login': 'yndx-777-vasya-nagibator-777'},
    )
    with override_settings(IDM_SID67_EXCLUDED_NODES=excluded_nodes):
        assert login.is_subscribable()

    new_good_role.set_raw_state('deprived')
    with override_settings(IDM_SID67_EXCLUDED_NODES=excluded_nodes):
        assert not login.is_subscribable()


def test_get_passport_login(arda_users, simple_system, department_structure):
    
    passport_login = simple_system.root_role_node.get_children().get().fields.get(slug='passport-login')
    passport_login.is_required = False
    passport_login.save()
    
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    mb_passport_login = UserPassportLogin.objects.create(login='yndx-frodo-fellowship', user=frodo)

    GroupMembership.objects.create(
        group=fellowship, user=arda_users.frodo, is_direct=False, state='inactive',
        passport_login=mb_passport_login,
    )
    
    parent_role = raw_make_role(
        fellowship, simple_system, {'role': 'manager'},
        state='granted',
    )

    role = raw_make_role(
        arda_users.frodo, simple_system, {'role': 'manager'},
        state='granted', fields_data={'passport-login': 'yndx-smth'},
        parent=parent_role,
    )
    
    login = role.get_passport_login()
    assert login is None
    fellowship.memberships.filter(user=frodo).update(passport_login=mb_passport_login)
    login = role.get_passport_login()
    assert login == 'yndx-frodo-fellowship'
