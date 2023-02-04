# coding: utf-8


import freezegun
import pytest

from django.utils import timezone

from idm.core.models import Action
from idm.metrics.granted_roles import (
    compute_granted_roles,
    compute_grant_timings_by_systems,
    compute_remove_timings_by_systems,
    compute_depriving_validation_timings,
)
from idm.core.constants.action import ACTION
from idm.tests.utils import raw_make_role


pytestmark = [pytest.mark.django_db]


@freezegun.freeze_time('2019-01-01')
def test_compute_granted_roles(simple_system, arda_users):
    frodo = arda_users['frodo']
    sam = arda_users['sam']
    now = timezone.now()

    # Нет события о выдаче роли (grant, fail), в метрику не попадёт
    role1 = raw_make_role(frodo, simple_system, {'role': 'manager'})
    action = Action.objects.create(
        role=role1,
        action=ACTION.APPROVE,
    )
    Action.objects.filter(pk=action.pk).update(added=now-timezone.timedelta(seconds=100))

    # Апрув роли был 7 дней назад, считаем что такой ситуации возникнуть не может, время выдачи в метрику не попадёт
    role2 = raw_make_role(frodo, simple_system, {'role': 'admin'})
    action = Action.objects.create(
        role=role2,
        action=ACTION.APPROVE,
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(days=8))
    action = Action.objects.create(
        role=role2,
        action=ACTION.FAIL,
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=10))

    # Есть событие о том, что роль упала в ошибку, в метрику попадёт
    role3 = raw_make_role(sam, simple_system, {'role': 'manager'})
    action = Action.objects.create(
        added=now - timezone.timedelta(seconds=100),
        role=role3,
        action=ACTION.APPROVE,
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=110))
    action = Action.objects.create(
        role=role3,
        action=ACTION.FAIL,
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=10))

    # Есть событие о успешной выдаче роли, в метрику попадёт
    role4 = raw_make_role(sam, simple_system, {'role': 'admin'})
    action = Action.objects.create(
        added=now - timezone.timedelta(seconds=100),
        role=role4,
        action=ACTION.APPROVE,
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=110))
    action = Action.objects.create(
        added=now - timezone.timedelta(seconds=10),
        role=role4,
        action=ACTION.GRANT,
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=100))

    metric_values = compute_granted_roles()
    expected_values = [
        {'slug': 'roles_count',        'value': 3},
        {'slug': 'time_of_granted_90', 'value': 100},
        {'slug': 'time_of_granted_95', 'value': 100},
        {'slug': 'time_of_granted_99', 'value': 100},
    ]
    assert metric_values == expected_values


@freezegun.freeze_time('2019-01-01')
@pytest.mark.parametrize('action_type', ['grant', 'remove'])
def test_compute_grant_timings(simple_system, complex_system, arda_users, action_type):
    start_actions = {
        'grant': 3 * [ACTION.APPROVE],
        'remove': [ACTION.DEPRIVE, ACTION.EXPIRE, ACTION.DEPRIVE],
    }[action_type]
    finish_actions = {
        'grant': [ACTION.FAIL, ACTION.GRANT, ACTION.GRANT],
        'remove': 3 * [ACTION.REMOVE],
    }[action_type]
    frodo = arda_users['frodo']
    sam = arda_users['sam']
    now = timezone.now()
    role1 = raw_make_role(sam, simple_system, {'role': 'manager'})
    action = Action.objects.create(
        added=now - timezone.timedelta(seconds=100),
        role=role1,
        action=start_actions[0],
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=110))
    action = Action.objects.create(
        role=role1,
        action=finish_actions[0],
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=10))

    role2 = raw_make_role(frodo, simple_system, {'role': 'manager'})
    action = Action.objects.create(
        added=now - timezone.timedelta(seconds=100),
        role=role2,
        action=start_actions[1],
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=110))
    action = Action.objects.create(
        role=role2,
        action=finish_actions[1],
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=10))

    role3 = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'})
    action = Action.objects.create(
        added=now - timezone.timedelta(seconds=50),
        role=role3,
        action=start_actions[2],
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=60))
    action = Action.objects.create(
        role=role3,
        action=finish_actions[2],
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=10))
    metric_values = compute_grant_timings_by_systems() if action_type == 'grant' else compute_remove_timings_by_systems()
    assert {measure['context']['system'] for measure in metric_values} == {'simple', 'complex', '_all_systems_'}
    assert {measure['value'] for system_measure in metric_values for measure in system_measure['values']} == {50, 100}


@freezegun.freeze_time('2019-01-01')
def test_compute_depriving_validation_timings(simple_system, complex_system, arda_users):
    frodo = arda_users['frodo']
    sam = arda_users['sam']
    now = timezone.now()
    role1 = raw_make_role(sam, simple_system, {'role': 'manager'})
    action = Action.objects.create(
        added=now - timezone.timedelta(seconds=100),
        role=role1,
        action=ACTION.DEPRIVE,
        data={'force_deprive': False}
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=110))
    action = Action.objects.create(
        role=role1,
        action=ACTION.DEPRIVE,
        data={'force_deprive': True}
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=10))

    role2 = raw_make_role(frodo, simple_system, {'role': 'manager'})
    action = Action.objects.create(
        added=now - timezone.timedelta(seconds=100),
        role=role2,
        action=ACTION.DEPRIVE,
        data={'force_deprive': False}
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=110))
    action = Action.objects.create(
        role=role2,
        action=ACTION.DEPRIVE,
        data={'force_deprive': True}
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=10))

    role3 = raw_make_role(frodo, complex_system, {'project': 'subs', 'role': 'manager'})
    action = Action.objects.create(
        added=now - timezone.timedelta(seconds=50),
        role=role3,
        action=ACTION.DEPRIVE,
        data={'force_deprive': False}
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=60))
    action = Action.objects.create(
        role=role3,
        action=ACTION.DEPRIVE,
        data={'force_deprive': True}
    )
    Action.objects.filter(pk=action.pk).update(added=now - timezone.timedelta(seconds=10))
    metric_values = compute_depriving_validation_timings()
    assert {measure['context']['system'] for measure in metric_values} == {'simple', 'complex', '_all_systems_'}
    assert {measure['value'] for system_measure in metric_values for measure in system_measure['values']} == {50, 100}
