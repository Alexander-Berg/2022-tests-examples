# coding: utf-8


import pytest
import requests

from django.core.management import call_command

from idm.core.constants.passport_login import PASSPORT_LOGIN_STATE
from idm.core.models import UserPassportLogin, Action, Role
from idm.tests.utils import create_fake_response, raw_make_role, refresh
from mock import patch

pytestmark = [pytest.mark.django_db]


@pytest.fixture(autouse=True)
def no_passport():
    """Это нужно для того чтобы отменить глобальный мок и протестировать внутренности работы с Паспортом
    """
    pass


def should_not_be_called(*args, **kwargs):
    raise Exception('This function should not be called!')


def test_idm_update_passport_logins(simple_system, users_for_test, monkeypatch):
    monkeypatch.setattr('idm.sync.passport.get_uid', lambda login: 1)

    art, fantom, terran, admin = users_for_test
    role = raw_make_role(
        art, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-art'},
        system_specific={'passport-login': 'yndx-art'},
        state='requested'
    )
    assert UserPassportLogin.objects.count() == 1
    login = UserPassportLogin.objects.get()
    assert login.is_fully_registered

    # здесь не будет вызова, т.к. роль неактивна, данный логин не имеет ни одной активной роли, и не будет подписан
    with patch.object(requests.sessions.Session, 'request') as post:
        post.side_effect = Exception
        call_command('idm_subscribe_passport_logins')
    assert len(post.call_args_list) == 0
    login = refresh(login)
    assert login.state == PASSPORT_LOGIN_STATE.CREATED

    role.set_state('approved')
    role = refresh(role)

    UserPassportLogin.objects.all().update(state=PASSPORT_LOGIN_STATE.CREATED)
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        assert role.state == 'granted'
        call_command('idm_subscribe_passport_logins')
        login = refresh(login)
        assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED

    UserPassportLogin.objects.all().update(state=PASSPORT_LOGIN_STATE.CREATED)
    with patch.object(requests.sessions.Session, 'request') as post:
        answer = '''{
            "status": "error",
            "errors": ["subscriptionnotallowed"]
        }'''
        post.return_value = create_fake_response(answer, status_code=400)

        call_command('idm_subscribe_passport_logins')
        login = refresh(login)
        assert login.state == PASSPORT_LOGIN_STATE.CREATED

    UserPassportLogin.objects.all().update(state=PASSPORT_LOGIN_STATE.CREATED)
    with patch.object(requests.sessions.Session, 'request') as post:
        answer = '<html>403 forbidden</html>'
        post.return_value = create_fake_response(answer, status_code=403)

        call_command('idm_subscribe_passport_logins')
        login = refresh(login)
        assert login.state == PASSPORT_LOGIN_STATE.CREATED


@pytest.mark.parametrize('threshold', [1, 3, None])
def test_threshold(simple_system, arda_users, monkeypatch, settings, threshold):
    monkeypatch.setattr('idm.sync.passport.get_uid', lambda login: 1)

    if threshold is not None:
        settings.IDM_SID67_THRESHOLD = threshold
    role1 = raw_make_role(
        arda_users.frodo, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-frodo'},
        system_specific={'passport-login': 'yndx-frodo'},
    )
    role2 = raw_make_role(
        arda_users.legolas, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-legolas'},
        system_specific={'passport-login': 'yndx-legolas'},
    )
    login1 = role1.passport_logins.get()
    login2 = role2.passport_logins.get()
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        call_command('idm_subscribe_passport_logins')
        login1.refresh_from_db()
        login2.refresh_from_db()
        if threshold in (None, 3):
            expected = PASSPORT_LOGIN_STATE.SUBSCRIBED
        else:
            expected = PASSPORT_LOGIN_STATE.CREATED
        assert login1.state == expected
        assert login2.state == expected


def test_idm_update_passport_logins_force(client, simple_system, users_for_test, monkeypatch):
    monkeypatch.setattr('idm.sync.passport.get_uid', lambda login: 1)

    art, fantom, terran, admin = users_for_test
    raw_make_role(
        art, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-art'},
        system_specific={'passport-login': 'yndx-art'},
        state='granted'
    )

    assert UserPassportLogin.objects.count() == 1
    login = UserPassportLogin.objects.get()

    # Проверяем, что подписывает
    UserPassportLogin.objects.all().update(state=PASSPORT_LOGIN_STATE.CREATED)
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')

        call_command('idm_subscribe_passport_logins')
        login = refresh(login)
        assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED

        action = login.actions.all().order_by('-id')[0]
        assert action.action == 'subscribed'

    # Проверяем, что второй раз ничего не происходит
    Action.objects.all().delete()
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')

        call_command('idm_subscribe_passport_logins')

        assert login.actions.count() == 0

    # А если вызвать насильственно, то подпишет еще раз
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')

        call_command('idm_subscribe_passport_logins', force=True)

        action = login.actions.all().order_by('-id')[0]
        assert action.action == 'subscribed'


def test_active_roles_in_inactive_systems_do_not_subscribe_login(simple_system, arda_users, monkeypatch):
    """Проверим, что активные роли в неактивных системах не подписывают логин"""
    monkeypatch.setattr('idm.sync.passport.get_uid', lambda login: 1)

    frodo = arda_users.frodo
    role = raw_make_role(
        frodo, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-art'},
        system_specific={'passport-login': 'yndx-art'},
        state='granted'
    )
    assert UserPassportLogin.objects.count() == 1
    simple_system.is_active = False
    simple_system.save()

    # здесь не будет вызова, т.к. роль в неактивной системе,
    # данный логин не имеет ни одной другой активной роли, и не будет подписан
    with patch.object(requests.sessions.Session, 'request') as post:
        post.side_effect = Exception
        call_command('idm_subscribe_passport_logins')
    assert len(post.call_args_list) == 0


def test_active_roles_in_inactive_systems_do_not_prevent_logins_from_unsubscribe(simple_system, arda_users,
                                                                                 monkeypatch):
    """Проверим, что активные роли в неактивных системах не препятствуют отписи логина"""
    monkeypatch.setattr('idm.sync.passport.get_uid', lambda login: 1)

    frodo = arda_users.frodo
    role = raw_make_role(
        frodo, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-art'},
        system_specific={'passport-login': 'yndx-art'},
        state='granted'
    )
    assert UserPassportLogin.objects.count() == 1
    simple_system.is_active = False
    simple_system.save()

    # здесь будет вызов, т.к. роль в неактивной системе, и поэтому данный логин будет отписан
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('')
        call_command('idm_unsubscribe_passport_logins')

    assert len(post.call_args_list) == 1


def test_excluded_systems(simple_system, complex_system, arda_users, monkeypatch, settings):
    """Проверим, что роли в исключённых системах не приводят к подписи сидом"""

    monkeypatch.setattr('idm.sync.passport.get_uid', lambda login: 1)
    settings.IDM_SID67_EXCLUDED_SYSTEMS = [simple_system.slug]

    frodo = arda_users.frodo
    role1 = raw_make_role(frodo, simple_system, {'role': 'manager'}, fields_data={'passport-login': 'yndx-frodo'})
    assert UserPassportLogin.objects.count() == 1
    login = UserPassportLogin.objects.get()

    # вызова не случится, так как simple система находится в списке исключений
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        call_command('idm_subscribe_passport_logins')

    login.refresh_from_db()
    assert login.state == PASSPORT_LOGIN_STATE.CREATED
    role2 = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'developer'},
                          fields_data={'passport-login': 'yndx-frodo'})

    # complex системы в списке исключений нет, поэтому здесь запрос пройдёт
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        call_command('idm_subscribe_passport_logins')

    login.refresh_from_db()
    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED

    # чтобы отозвать подписку, достаточно отозвать все роли в РАЗРЕШЁННЫХ системах
    role2.deprive_or_decline(frodo)

    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        call_command('idm_unsubscribe_passport_logins')

    login.refresh_from_db()
    # подписка снялась
    assert login.state == PASSPORT_LOGIN_STATE.UNSUBSCRIBED

    role1.deprive_or_decline(frodo)
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        call_command('idm_unsubscribe_passport_logins')

    login.refresh_from_db()
    # особо ничего не поменялось
    assert login.state == PASSPORT_LOGIN_STATE.UNSUBSCRIBED


def test_excluded_nodes(simple_system, other_system, arda_users, monkeypatch, settings):
    """Проверим, что роли на исключённые узлы не приводят к подписи сидом"""

    monkeypatch.setattr('idm.sync.passport.get_uid', lambda login: 1)
    settings.IDM_SID67_EXCLUDED_NODES = {
        simple_system.slug: [
            '/role/manager/',
            '/role/superuser/',
        ],
        other_system.slug: [
            '/role/superuser/',
        ]
    }

    frodo = arda_users.frodo
    role1 = raw_make_role(frodo, simple_system, {'role': 'manager'}, fields_data={'passport-login': 'yndx-frodo'})
    assert UserPassportLogin.objects.count() == 1
    login = UserPassportLogin.objects.get()

    # вызова не случится, так как узел находится в списке исключений
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        call_command('idm_subscribe_passport_logins')
    login.refresh_from_db()
    assert login.state == PASSPORT_LOGIN_STATE.CREATED

    role2 = raw_make_role(frodo, other_system, {'role': 'manager'}, fields_data={'passport-login': 'yndx-frodo'})
    # тот же узел другой системы, поэтому здесь запрос пройдёт
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        call_command('idm_subscribe_passport_logins')
    login.refresh_from_db()
    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED

    # отписываются логины по обратному условию - если нет ни одной роли, на которую нужно вешать сид
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        call_command('idm_unsubscribe_passport_logins')
    login.refresh_from_db()
    # ничего не случилось
    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED

    role1.deprive_or_decline(frodo)
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        call_command('idm_unsubscribe_passport_logins')
    login.refresh_from_db()
    # опять ничего не случилось, потому что узел в исключениях
    assert login.state == PASSPORT_LOGIN_STATE.SUBSCRIBED

    role2.deprive_or_decline(frodo)
    with patch.object(requests.sessions.Session, 'request') as post:
        post.return_value = create_fake_response('{"status": "ok"}')
        call_command('idm_unsubscribe_passport_logins')
    login.refresh_from_db()
    # теперь подписка снялась
    assert login.state == PASSPORT_LOGIN_STATE.UNSUBSCRIBED
