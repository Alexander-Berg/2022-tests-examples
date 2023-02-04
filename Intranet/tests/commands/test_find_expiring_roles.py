# -*- coding: utf-8 -*-

import pytest
from django.core.management import call_command
from django.utils import timezone
from waffle.testutils import override_switch

from idm.core.constants.role import ROLE_STATE
from idm.tests.utils import raw_make_role

pytestmark = [pytest.mark.django_db]


def make_expiring_role(user, system, ttl_date=None, ttl_days=None, expiring_now=False):
    return raw_make_role(
        user,
        system,
        {'role': 'manager'},
        ttl_days=ttl_days,
        ttl_date=ttl_date,
        expire_at=timezone.now() + (timezone.timedelta() if expiring_now else timezone.timedelta(days=8)),
        state=ROLE_STATE.GRANTED,
    )


def test_find_expiring_roles_small_ttl_days(simple_system, arda_users, mailoutbox):
    role = make_expiring_role(arda_users.frodo, simple_system, ttl_days=3, expiring_now=True)
    with override_switch('find_expiring_roles', active=True):
        call_command('idm_find_expiring_roles')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.GRANTED
    assert len(mailoutbox) == 0


def test_find_expiring_roles_small_ttl_date(simple_system, arda_users, mailoutbox):
    role = make_expiring_role(
        arda_users.frodo, simple_system, ttl_date=timezone.now() + timezone.timedelta(days=1), expiring_now=True
    )
    with override_switch('find_expiring_roles', active=True):
        call_command('idm_find_expiring_roles')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.GRANTED
    assert len(mailoutbox) == 0


def test_find_expiring_roles_not_expiring_yet(simple_system, arda_users, mailoutbox):
    role = make_expiring_role(
        arda_users.frodo, simple_system, ttl_days=10, expiring_now=False
    )
    with override_switch('find_expiring_roles', active=True):
        call_command('idm_find_expiring_roles')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.GRANTED
    assert len(mailoutbox) == 0


def test_find_expiring_roles_ok_ttl_days(simple_system, arda_users, mailoutbox):
    role = make_expiring_role(
        arda_users.frodo, simple_system, ttl_days=10, expiring_now=True
    )
    with override_switch('find_expiring_roles', active=True):
        call_command('idm_find_expiring_roles')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.EXPIRING
    assert len(mailoutbox) == 1
    assert 'В связи с истечением срока действия некоторые роли требуют повторного подтверждения' in mailoutbox[0].body


def test_find_expiring_roles_ok_ttl_date(simple_system, arda_users, mailoutbox):
    role = make_expiring_role(
        arda_users.frodo, simple_system, ttl_date=timezone.now() + timezone.timedelta(days=10), expiring_now=True
    )
    with override_switch('find_expiring_roles', active=True):
        call_command('idm_find_expiring_roles')

    role.refresh_from_db()
    assert role.state == ROLE_STATE.EXPIRING
    assert len(mailoutbox) == 1
    assert 'В связи с истечением срока действия некоторые роли требуют повторного подтверждения' in mailoutbox[0].body
