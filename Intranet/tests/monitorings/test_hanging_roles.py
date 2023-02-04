# coding: utf-8


import pytest
from django.utils import timezone
from django.test import override_settings

import constance

from idm.core.models import Role
from idm.tests.utils import assert_contains

pytestmark = pytest.mark.django_db


def _change_role_state(role, state, last_action, action_timedelta=None, delete_prev_actions=True):
    if not action_timedelta:
        action_timedelta = timezone.timedelta(seconds=constance.config.IDM_HANGING_DEPRIVING_OLD)
    role.set_raw_state(state)
    if delete_prev_actions:
        role.actions.all().delete()
    action = role.actions.create()
    action.added = timezone.now() - action_timedelta
    action.action = last_action
    action.save()


@override_settings(IDM_HANGING_DEPRIVING_BY_SYSTEM_THRESHOLD=1)
@pytest.mark.parametrize('status', ['warn', 'crit'])
def test_hanging_roles(arda_users_with_roles, client, status):
    """Проверяем работу мониторинга зависших ролей"""
    roles = Role.objects.all()
    frodo = arda_users_with_roles['frodo'][0].user
    client.login(frodo)

    for url in ['approved', 'depriving']:
        response = client.get('/monitorings/hanging-{}-roles/'.format(url))
        assert response.status_code == 200
        assert response.content == b'ok'

    # Не попадет в выборку, т.к. экшн содержит ошибку
    _change_role_state(roles[0], 'depriving', 'error')
    response = client.get('/monitorings/hanging-depriving-roles/')
    assert response.status_code == 200
    assert response.content == b'ok'

    # 3 approved роли попадут в выборку
    if status == 'warn':
        action_timedelta = None
        expected_status = 412
    else:
        action_timedelta = timezone.timedelta(minutes=constance.config.HANGING_ROLES_WARN_MINUTES + 1)
        expected_status = 400
    current_timedelta = action_timedelta
    for role in roles[:3]:
        _change_role_state(role, 'approved', 'approve', action_timedelta=current_timedelta)
        # Достаточно одной сильно зависшей роли
        current_timedelta = None
    response = client.get('/monitorings/hanging-approved-roles/')
    assert response.status_code == expected_status
    assert_contains([b'IDM has 3 approved hanging roles since'], response.content)

    response = client.get('/monitorings/hanging-approved-roles/', {'systems': 'simple'})
    assert response.status_code == expected_status
    assert_contains([b'IDM has 3 approved hanging roles since'], response.content)

    response = client.get('/monitorings/hanging-approved-roles/', {'systems': 'fake_system'})
    assert response.status_code == 200

    response = client.get('/monitorings/hanging-approved-roles/', {'threshold': 330000})
    assert response.status_code == 200

    # 2 depriving роли попадут в выборку
    current_timedelta = action_timedelta
    for role in roles[:2]:
        _change_role_state(role, 'depriving', 'deprive', action_timedelta=current_timedelta)
        current_timedelta = None
    response = client.get('/monitorings/hanging-depriving-roles/')
    assert response.status_code == expected_status
    assert_contains([b'IDM has 2 depriving hanging roles since'], response.content)
    
    with override_settings(IDM_HANGING_DEPRIVING_BY_SYSTEM_THRESHOLD=3):
        response = client.get('/monitorings/hanging-depriving-roles/')
        assert response.status_code == 200


@override_settings(IDM_HANGING_DEPRIVING_BY_SYSTEM_THRESHOLD=1)
@pytest.mark.parametrize('status', ['warn', 'crit'])
def test_two_systems(arda_users_with_roles, users_with_roles, client, status):
    simple_system_roles = Role.objects.filter(system__slug='simple')[:3]
    test1_system_roles = Role.objects.filter(system__slug='test1')[:2]

    if status == 'warn':
        action_timedelta = None
        expected_status = 412
    else:
        action_timedelta = timezone.timedelta(minutes=constance.config.HANGING_ROLES_WARN_MINUTES + 1)
        expected_status = 400
    # Добавляем по 2 экшена для ролей в simple_system, чтобы проверить, что не считаем их по 2 раза
    for role in simple_system_roles:
        _change_role_state(role, 'depriving', 'deprive', action_timedelta=action_timedelta)
        _change_role_state(role, 'depriving', 'deprive', action_timedelta=action_timedelta, delete_prev_actions=False)
    assert simple_system_roles[0].actions.count() > 1

    for role in test1_system_roles:
        _change_role_state(role, 'depriving', 'deprive', action_timedelta=action_timedelta)

    response = client.get('/monitorings/hanging-depriving-roles/')
    assert response.status_code == expected_status
    assert_contains(
        [b'IDM has 5 depriving hanging roles since', b'System simple has 3', b'System test1 has 2'],
        response.content
    )
