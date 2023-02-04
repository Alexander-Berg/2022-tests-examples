# -*- coding: utf-8 -*


from datetime import timedelta

import pytest
from django.core import mail
from django.core import management
from django.utils import timezone
from freezegun import freeze_time

from idm.core.models import Role
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import (
    refresh, assert_contains, raw_make_role, mock_all_roles, assert_inconsistency,
    make_inconsistency)

# разрешаем использование базы в тестах
pytestmark = [pytest.mark.django_db]


@pytest.fixture()
def spec_system(simple_system):
    simple_system.role_grant_policy = 'system'
    simple_system.save()
    return simple_system


def test_request_role(spec_system, arda_users):
    frodo = arda_users.frodo

    assert frodo.roles.count() == 0
    assert len(mail.outbox) == 0

    role = Role.objects.request_role(frodo, frodo, spec_system, '', {'role': 'admin'}, None)

    assert frodo.roles.count() == 1
    assert len(mail.outbox) == 0

    role = refresh(role)
    assert role.is_active is False
    assert role.state == 'sent'


def test_request_role_with_passport_login(pt1_system, arda_users):
    pt1_system.role_grant_policy = 'system'
    pt1_system.save()
    frodo = arda_users.frodo

    assert frodo.roles.count() == 0
    assert frodo.passport_logins.count() == 0
    assert len(mail.outbox) == 0

    Role.objects.request_role(frodo, frodo, pt1_system, '', {'project': 'proj1', 'role': 'admin'},
                              {'passport-login': 'yndx.frodo'})

    frodo = refresh(frodo)
    assert frodo.roles.count() == 1
    assert frodo.passport_logins.count() == 1
    assert frodo.passport_logins.get().login == 'yndx.frodo'

    role = frodo.roles.get()
    assert role.is_active is False
    assert role.state == 'awaiting'
    assert len(mail.outbox) == 0

    frodo.passport_logins.update(is_fully_registered=True)
    Role.objects.poke_awaiting_roles()
    role = frodo.roles.get()
    assert role.is_active is False
    assert role.state == 'sent'

    assert len(mail.outbox) == 1
    assert_contains(
        (
            'Ваша роль отправлена в систему',
            'В Паспорте был заведен новый логин',
            'yndx.frodo',
        ),
        mail.outbox[0].body
    )


def test_create_inconsistency(spec_system, arda_users):
    frodo = arda_users.frodo
    raw_make_role(frodo, spec_system, {'role': 'admin'}, state='sent')

    user_roles = [{
        'login': 'frodo',
        'roles': [
            {'role': 'admin'}
        ]
    }]

    with mock_all_roles(spec_system, user_roles=user_roles):
        Inconsistency.objects.check_roles()

    # должна появиться одна неконсистентность, про пользователя
    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(
        inconsistency,
        system=spec_system,
        user=arda_users.frodo,
        path='/role/admin/'
    )


def test_grant_role(spec_system, arda_users):
    frodo = arda_users.frodo
    role = raw_make_role(frodo, spec_system, {'role': 'manager'}, state='sent', system_specific=None)
    assert len(mail.outbox) == 0

    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=spec_system,
        path='/role/manager/',
    )

    management.call_command('idm_check_and_resolve', resolve_only=True)

    role = refresh(role)
    assert role.state == 'granted'

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'obsolete'

    assert len(mail.outbox) == 1
    assert_contains(
        (
            'Вы получили новую роль',
        ),
        mail.outbox[0].body
    )


def test_grant_role_with_fields(spec_system, arda_users):
    frodo = arda_users.frodo
    role = raw_make_role(frodo, spec_system, {'role': 'manager'}, state='sent', system_specific={'login': 'baggins'})
    make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=spec_system,
        path='/role/manager/',
        remote_fields={'login': 'hello'}
    )
    management.call_command('idm_check_and_resolve', resolve_only=True)
    role = refresh(role)
    assert role.state == 'sent'

    make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=spec_system,
        path='/role/manager/',
        remote_fields={'login': 'baggins'}
    )
    management.call_command('idm_check_and_resolve', resolve_only=True)
    role = refresh(role)
    assert role.state == 'granted'


def test_expire_role(spec_system, arda_users):
    frodo = arda_users.frodo

    role = Role.objects.request_role(frodo, frodo, spec_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.state == 'sent'

    with freeze_time(timezone.now() + timedelta(days=7)):
        management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.state == 'sent'

    with freeze_time(timezone.now() + timedelta(days=14)):
        management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.state == 'expired'
