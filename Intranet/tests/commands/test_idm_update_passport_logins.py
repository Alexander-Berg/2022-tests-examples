# coding: utf-8


import pytest
from django.core.management import call_command
from idm.core.models import UserPassportLogin, Role
from idm.tests.utils import raw_make_role

pytestmark = [
    pytest.mark.django_db,
]


def test_idm_update_passport_logins(simple_system, users_for_test):
    art = users_for_test[0]
    role = raw_make_role(
        art, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-art'},
        system_specific={'passport-login': 'yndx-art'},
        state='granted'
    )

    call_command('idm_update_passport_logins')
    assert UserPassportLogin.objects.count() == 1
    assert role.passport_logins.count() == 1
    passport_login = role.passport_logins.get()
    assert passport_login.login == 'yndx-art'
    assert passport_login.roles.count() == 1
    assert passport_login.roles.get() == role
    passport_login.roles.clear()
    assert passport_login.roles.count() == 0

    call_command('idm_update_passport_logins')
    assert passport_login.roles.count() == 1
    assert passport_login.roles.get() == role


def test_one_users_one_login(simple_system, users_for_test):
    art = users_for_test[0]
    raw_make_role(
        art, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-art'},
        system_specific={'passport-login': 'yndx-art'},
        state='granted',
    )

    call_command('idm_update_passport_logins')

    assert UserPassportLogin.objects.count() == 1
    login = UserPassportLogin.objects.get()
    assert Role.objects.filter(passport_logins=login).count() == 1


def test_one_user_one_login_imported_roles(simple_system, users_for_test):
    """ Роли приехали из систем, fields_data не заполнено """
    art = users_for_test[0]
    raw_make_role(art, simple_system, {'role': 'manager'}, system_specific={'passport-login': 'yndx-art'})

    assert UserPassportLogin.objects.count() == 0

    call_command('idm_update_passport_logins')

    assert UserPassportLogin.objects.count() == 1
    login = UserPassportLogin.objects.get()
    assert Role.objects.filter(passport_logins=login).count() == 1
