# coding: utf-8


import pytest

from django.core.management import call_command

from idm.core.models import UserPassportLogin, Role
from idm.tests.utils import raw_make_role, refresh, capture_http, set_workflow, DEFAULT_WORKFLOW
from idm.users.models import GroupMembership
from mock import patch

pytestmark = [
    pytest.mark.django_db,
]


def get_bb_with_response(response):
    class Blackbox(object):
        def __init__(self, *args, **kwargs):
            pass

        def userinfo(self, *args, **kwargs):
            return response
    return Blackbox


@pytest.mark.parametrize('state', ['requested', 'approved', 'awaiting'])
def test_idm_check_awaiting_logins(simple_system, users_for_test, state):
    art, fantom, terran, admin = users_for_test
    role = raw_make_role(
        art, simple_system, {'role': 'manager'},
        fields_data={'passport-login': 'yndx-art'},
        system_specific={'passport-login': 'yndx-art'},
        state=state
    )
    assert UserPassportLogin.objects.count() == 1
    login = UserPassportLogin.objects.get()
    login.is_fully_registered = False
    login.save()

    with patch('idm.sync.passport.get_external_blackbox') as bb, \
            capture_http(simple_system, {'code': 0}):
        bb.side_effect = get_bb_with_response({
                    'attributes': {
                        '1005': '0'
                    }
                })
        call_command('idm_check_awaiting_logins')
    login = refresh(login)
    assert not login.is_fully_registered  # логин недореган
    role = refresh(role)
    assert role.state == state  # смысла выдавать роль нет

    with patch('idm.sync.passport.get_external_blackbox') as bb,\
            capture_http(simple_system, {'code': 0}):
        bb.side_effect = get_bb_with_response({
                    'attributes': {
                        '1005': '1'
                    }
                })
        call_command('idm_check_awaiting_logins')
    login = refresh(login)
    assert login.is_fully_registered

    role = refresh(role)
    if state == 'awaiting':
        assert role.state == 'awaiting'
        call_command('idm_check_awaiting_roles')
        role.refresh_from_db()
        assert role.state == 'granted'  # awaiting-роль допиналась, так как она только дорегистрации и ждала
    else:
        assert role.state == state  # все остальные роли не трогаем


def test_check_logins_for_personal_roles(pt1_system, arda_users, department_structure):
    """
    У персональных ролей паспортный логин может приезжать из членства.
    Это копия теста test_role_request_with_passport_login из test_group_role.py,
    только теперь вместо ручной смены статуса и запуска допинывалки ролей мы запускаем синк логинов
    """
    frodo = arda_users.frodo
    for login in ['login1', 'login2']:
        frodo.passport_logins.create(login=login, state='created')
    login = frodo.passport_logins.first()  # пофиг какой

    group = department_structure.fellowship
    set_workflow(pt1_system, group_code=DEFAULT_WORKFLOW)
    members_count = group.members.count()

    group_role = Role.objects.request_role(frodo, group, pt1_system, '', {'project': 'proj1', 'role': 'admin'}, None)
    assert Role.objects.count() == 1 + members_count
    assert Role.objects.filter(user=None).count() == 1
    assert Role.objects.filter(group=None).count() == members_count
    assert group_role.state == 'granted'
    frodo_role = Role.objects.get(user=frodo)
    assert frodo_role.state == 'awaiting'  # У членства не выбран логин.
    assert not frodo_role.passport_logins.exists()

    GroupMembership.objects.filter(user=frodo, group=group).update(passport_login=login)
    Role.objects.poke_awaiting_roles()
    frodo_role = refresh(frodo_role)
    login = refresh(login)
    assert frodo_role.state == 'awaiting'  # Теперь логин определён и прицеплен к роли, но не дореган
    assert frodo_role.fields_data == {'passport-login': 'login1'}  # Теперь тут есть логин
    assert frodo_role.passport_logins.get() == login
    assert group_role.fields_data is None  # А здесь нет
    assert not login.is_fully_registered

    with patch('idm.sync.passport.get_external_blackbox') as bb,\
            capture_http(pt1_system, {'code': 0}):
        bb.side_effect = get_bb_with_response({
                    'attributes': {
                        '1005': '1'
                    }
                })
        call_command('idm_check_awaiting_logins')
    login = refresh(login)
    assert login.is_fully_registered

    # Запуск допинывалки ролей не требуется, синк логинов всё сделал сам
    frodo_role = refresh(frodo_role)
    group_role = refresh(group_role)
    assert group_role.state == 'granted'
    assert frodo_role.state == 'awaiting'
    call_command('idm_check_awaiting_roles')
    frodo_role.refresh_from_db()
    assert frodo_role.state == 'granted'  # Теперь всё ок
    assert frodo_role.node.data == {'project': 'proj1', 'role': 'admin'}
    assert frodo_role.fields_data == {'passport-login': 'login1'}
    assert group_role.fields_data is None
    assert frodo_role.passport_logins.get() == login
