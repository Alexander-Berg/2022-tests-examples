# -*- coding: utf-8 -*-
# https://wiki.yandex-team.ru/passport/python/api/bundle/registration/#registracijapolzovateljabezpodtverzhdenija


import pytest
from django.core.management import call_command
from mock import MagicMock, call, patch

from idm.core.constants.passport_login import PASSPORT_LOGIN_STATE
from idm.core.models import Role, UserPassportLogin
from idm.sync import passport
from idm.tests.utils import create_fake_response, refresh, raw_make_role

pytestmark = [pytest.mark.django_db]


@pytest.fixture(autouse=True)
def no_passport():
    """Это нужно для того чтобы отменить глобальный мок и протестировать внутренности работы с Паспортом
    """
    pass


def test_register_login_db_error(monkeypatch):
    monkeypatch.setattr(
        'requests.sessions.Session.request',
        lambda *args, **kwargs: create_fake_response('{"status": "error", "errors": ["backend.database_failed"]}')
    )

    with pytest.raises(passport.PassportError):
        passport.register_login('yndx-username-login', 'username', 'first', 'last', True)


def test_register_login_no_access(monkeypatch):
    monkeypatch.setattr(
        'requests.sessions.Session.request',
        lambda *args, **kwargs: create_fake_response('', status_code=403)
    )

    with pytest.raises(passport.NoGrantsError):
        passport.register_login('yndx-username-login', 'username', 'first', 'last', True)


@pytest.fixture
def system_answer(monkeypatch):
    monkeypatch.setattr(
        'idm.utils.http.post',
        lambda *args, **kwargs: create_fake_response('{"code": 0}')
    )


def test_login_process(arda_users, generic_system, system_answer):
    frodo = arda_users['frodo']

    with patch('idm.sync.passport.exists') as p_exists, \
            patch('idm.sync.passport.register_login') as p_register_login, \
            patch('idm.sync.passport.set_strongpwd') as p_set_strongpwd:
        p_exists.return_value = False
        p_register_login.return_value = 'pass'
        p_set_strongpwd.return_value = True

        role = Role.objects.request_role(
            frodo, frodo, generic_system, '', {'role': 'admin'}, {'passport-login': 'yndx.frodo'}
        )

    role = refresh(role)
    assert role.state == 'awaiting'
    login = UserPassportLogin.objects.get()
    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED


def test_existing_login_process(arda_users, generic_system, system_answer, monkeypatch):
    monkeypatch.setattr('idm.sync.passport.exists', lambda *args, **kwargs: True)

    fake_subscriber = MagicMock()
    fake_subscriber.return_value = True
    monkeypatch.setattr('idm.sync.passport.set_strongpwd', fake_subscriber)

    frodo = arda_users['frodo']

    raw_make_role(frodo, generic_system, {'role': 'admin'}, fields_data={'passport-login': 'yndx.frodo'})

    login = UserPassportLogin.objects.get()
    assert login.login == 'yndx.frodo'
    assert login.state == PASSPORT_LOGIN_STATE.CREATED  # старый логин

    role = Role.objects.request_role(
        frodo, frodo, generic_system, '', {'role': 'manager'}, {'passport-login': 'yndx.frodo'}
    )

    role = refresh(role)
    assert role.state == 'granted'
    login = refresh(login)
    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED
    assert fake_subscriber.mock_calls == [call('yndx.frodo')]


def test_deprive_roles_with_logins(arda_users, generic_system, system_answer, monkeypatch):
    blackbox_logins = set()

    def exists(login):
        return login in blackbox_logins

    def register_login(login, *agrs, **kwargs):
        blackbox_logins.add(login)
    monkeypatch.setattr('idm.sync.passport.exists', exists)
    monkeypatch.setattr('idm.sync.passport.register_login', register_login)
    monkeypatch.setattr('idm.sync.passport.set_strongpwd', lambda *args, **kwargs: True)

    fake_remover = MagicMock()
    fake_remover.return_value = True
    monkeypatch.setattr('idm.sync.passport.remove_strongpwd', fake_remover)

    assert UserPassportLogin.objects.exists() is False
    frodo = arda_users['frodo']
    role = Role.objects.request_role(
        frodo, frodo, generic_system, '', {'role': 'admin'}, {'passport-login': 'yndx.frodo'}
    )
    role2 = Role.objects.request_role(
        frodo, frodo, generic_system, '', {'role': 'manager'}, {'passport-login': 'yndx.frodo'}
    )

    frodo.passport_logins.update(is_fully_registered=True)
    Role.objects.poke_awaiting_roles()
    role = refresh(role)
    role2 = refresh(role2)

    login = UserPassportLogin.objects.get()
    assert login.login == 'yndx.frodo'
    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED

    role = refresh(role)
    role.set_state('depriving')
    role = refresh(role)
    assert role.state == 'deprived'
    login = refresh(login)
    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED
    assert fake_remover.call_args_list == []

    fake_remover.reset_mock()
    role2 = refresh(role2)
    role2.set_state('depriving')
    role2 = refresh(role2)
    assert role2.state == 'deprived'
    login = refresh(login)
    assert login.state == PASSPORT_LOGIN_STATE.UNSUBSCRIBED
    assert fake_remover.mock_calls == [call('yndx.frodo')]


def test_resubscribe_login(arda_users, generic_system, system_answer, monkeypatch):
    blackbox_logins = set()

    def exists(login):
        return login in blackbox_logins

    def register_login(login, *agrs, **kwargs):
        blackbox_logins.add(login)
    monkeypatch.setattr('idm.sync.passport.exists', exists)
    monkeypatch.setattr('idm.sync.passport.register_login', register_login)

    fake_subscriber = MagicMock()
    fake_subscriber.return_value = True
    monkeypatch.setattr('idm.sync.passport.set_strongpwd', fake_subscriber)

    fake_remover = MagicMock()
    fake_remover.return_value = True
    monkeypatch.setattr('idm.sync.passport.remove_strongpwd', fake_remover)

    assert UserPassportLogin.objects.exists() is False
    frodo = arda_users['frodo']
    role = Role.objects.request_role(
        frodo, frodo, generic_system, '', {'role': 'admin'}, {'passport-login': 'yndx.frodo'}
    )

    frodo.passport_logins.update(is_fully_registered=True)
    Role.objects.poke_awaiting_roles()
    role = refresh(role)

    login = UserPassportLogin.objects.get()
    assert login.login == 'yndx.frodo'
    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED

    role = refresh(role)
    role.set_state('depriving')
    role = refresh(role)
    assert role.state == 'deprived'
    login = refresh(login)
    assert login.state == PASSPORT_LOGIN_STATE.UNSUBSCRIBED
    assert fake_remover.mock_calls == [call('yndx.frodo')]

    role2 = Role.objects.request_role(
        frodo, frodo, generic_system, '', {'role': 'admin'}, {'passport-login': 'yndx.frodo'}
    )

    role2 = refresh(role2)
    assert role2.state == 'granted'
    login = refresh(login)
    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED
    assert fake_subscriber.mock_calls == [call('yndx.frodo')]


def test_unsubscribe(arda_users, generic_system):
    frodo = arda_users['frodo']

    raw_make_role(frodo, generic_system, {'role': 'admin'},
                  fields_data={'passport-login': 'yndx.frodo'}, state='deprived')

    login = UserPassportLogin.objects.get()
    assert login.login == 'yndx.frodo'
    assert login.state == PASSPORT_LOGIN_STATE.CREATED

    with patch.object(passport, 'remove_strongpwd') as remove_strongpwd:
        remove_strongpwd.return_value = True
        call_command('idm_unsubscribe_passport_logins')

    login = refresh(login)
    assert login.state == PASSPORT_LOGIN_STATE.UNSUBSCRIBED


def test_unknown_login(arda_users, generic_system):
    frodo = arda_users['frodo']
    raw_make_role(
        frodo, generic_system, {'role': 'manager'}, fields_data={'passport-login': 'yndx.frodo'}, state='granted',
    )

    login = UserPassportLogin.objects.get()
    assert login.login == 'yndx.frodo'
    assert login.state == PASSPORT_LOGIN_STATE.CREATED

    with patch.object(passport, 'get_uid') as get_uid:
        get_uid.return_value = None
        call_command('idm_subscribe_passport_logins')

    login = refresh(login)
    assert login.state == PASSPORT_LOGIN_STATE.UNKNOWN
