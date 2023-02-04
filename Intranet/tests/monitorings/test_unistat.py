# coding: utf-8
from __future__ import absolute_import

from freezegun import freeze_time
import json
from mock import patch
import pytest
import pytz

from constance.test import override_config
from django.utils import timezone
from django.utils.timezone import datetime, timedelta

from idm.core.models import CommandTimestamp, GroupMembershipSystemRelation
from idm.users.models import Group, GroupMembership
from idm.monitorings.tasks import CalculateUnistatMetrics
from idm.monitorings.views.unistat import UnistatView
from idm.tests.utils import create_user, raw_make_role
from idm.inconsistencies.models import Inconsistency

pytestmark = [pytest.mark.django_db]


def test_unistat(client, arda_users):
    with patch(
            'idm.monitorings.views.unistat.UnistatView.get_queues_size',
            return_value=[('metric_1', 0)]
    ) as queues_metric, \
            patch(
                'idm.monitorings.views.unistat.UnistatView.get_periodic_timestamps',
                return_value=[('metric_2', 0), ('metric_3', 1)]
            ) as timestamp_metric, \
            patch('idm.monitorings.views.unistat.UnistatView.get_role_states', return_value=[]) as role_states_metric, \
            patch(
                'idm.monitorings.views.unistat.UnistatView.get_depriving_roles_states',
                return_value=[],
            ) as depriving_roles_metric, \
            patch(
                'idm.monitorings.views.unistat.UnistatView.get_groupmembership_system_states',
                  return_value=[],
            ) as gms_states_metric:
        response = client.get('/monitorings/unistat/')

    assert response.status_code == 200
    response_json = json.loads(response.content)
    assert sorted(response_json) == sorted([
        ['metric_1', 0],
        ['metric_2', 0],
        ['metric_3', 1],
    ])
    queues_metric.assert_called_once()
    timestamp_metric.assert_called_once()
    depriving_roles_metric.assert_called_once()
    role_states_metric.assert_called_once()
    gms_states_metric.assert_called_once()


def test_get_periodic_timestamps():
    assert not CommandTimestamp.objects.exists()
    metrics = UnistatView().get_periodic_timestamps()
    assert metrics == []

    CommandTimestamp.objects.create(
        command='tasks.SomeTask',
        last_success_start=timezone.datetime(2019, 1, 1, 12, 0, tzinfo=pytz.UTC),
        last_success_finish=timezone.datetime(2019, 1, 1, 12, 20, tzinfo=pytz.UTC),
    )
    with freeze_time(timezone.datetime(2019, 1, 1, 13, 20)):
        metrics = UnistatView().get_periodic_timestamps()
    assert metrics == [
        ['tasks.SomeTask_freshness_max', 1.0]
    ]

    CommandTimestamp.objects.create(
        command='tasks.SystemTask:simple',
        last_success_start=timezone.datetime(2019, 1, 1, 13, 0, tzinfo=pytz.UTC),
        last_success_finish=timezone.datetime(2019, 1, 1, 13, 20, tzinfo=pytz.UTC),
    )
    CommandTimestamp.objects.create(
        command='tasks.SystemTask:complex',
        last_success_start=timezone.datetime(2019, 1, 1, 14, 0, tzinfo=pytz.UTC),
        last_success_finish=timezone.datetime(2019, 1, 1, 14, 20, tzinfo=pytz.UTC),
    )
    with freeze_time(timezone.datetime(2019, 1, 1, 15, 20)):
        metrics = UnistatView().get_periodic_timestamps()
    assert sorted(metrics) == sorted([
        ['tasks.SomeTask_freshness_max', 3.0],
        ['tasks.SystemTask_freshness_max', 2.0],
    ])


def test_get_role_states(simple_system, other_system):
    CalculateUnistatMetrics.delay()
    metrics = UnistatView().get_role_states()
    assert len(metrics) == 0

    user1 = create_user('user1')
    raw_make_role(user1, other_system, {'role': 'admin'}, state='granted')
    raw_make_role(user1, simple_system, {'role': 'admin'}, state='deprived')
    raw_make_role(user1, simple_system, {'role': 'superuser'}, state='deprived')
    raw_make_role(user1, simple_system, {'role': 'manager'}, state='granted')

    CalculateUnistatMetrics.delay()
    metrics = UnistatView().get_role_states()
    assert sorted(metrics) == [
        ['role_deprived_max', 2],
        ['role_granted_max', 2],
    ]


def test_get_groupmembership_system_states(simple_system, other_system):
    CalculateUnistatMetrics.delay()
    metrics = UnistatView().get_groupmembership_system_states()
    assert len(metrics) == 0

    user1 = create_user('user1')
    group1 = Group.objects.create(type='wiki', name='group1')
    membership_1 = GroupMembership.objects.create(group=group1, user=user1, is_direct=True, state='active')
    GroupMembershipSystemRelation.objects.create(membership=membership_1, system=other_system, state='activating')
    GroupMembershipSystemRelation.objects.create(membership=membership_1, system=simple_system, state='depriving')

    CalculateUnistatMetrics.delay()
    metrics = UnistatView().get_groupmembership_system_states()
    assert sorted(metrics) == [
        [f'ctype={other_system.slug};groupmembership_system_activated_max', 0],
        [f'ctype={other_system.slug};groupmembership_system_activating_max', 1],
        [f'ctype={other_system.slug};groupmembership_system_deprived_max', 0],
        [f'ctype={other_system.slug};groupmembership_system_depriving_max', 0],
        [f'ctype={other_system.slug};groupmembership_system_failed_max', 0],
        [f'ctype={other_system.slug};groupmembership_system_hold_max', 0],

        [f'ctype={simple_system.slug};groupmembership_system_activated_max', 0],
        [f'ctype={simple_system.slug};groupmembership_system_activating_max', 0],
        [f'ctype={simple_system.slug};groupmembership_system_deprived_max', 0],
        [f'ctype={simple_system.slug};groupmembership_system_depriving_max', 1],
        [f'ctype={simple_system.slug};groupmembership_system_failed_max', 0],
        [f'ctype={simple_system.slug};groupmembership_system_hold_max', 0],
    ]


@override_config(SYSTEM_SLUGS_FOR_INCONSISTENCY_UNISTAT='simple,other')
def test_get_inconsistency_system_states(simple_system, other_system):
    for _ in range(4):
        Inconsistency.objects.create(system=other_system, state='active')
        Inconsistency.objects.create(system=other_system, state='deprived')

    for _ in range(6):
        Inconsistency.objects.create(system=simple_system, state='active')

    CalculateUnistatMetrics.delay()
    metrics = UnistatView().get_inconsistency_states()
    assert sorted(metrics) == [['inconsistencies_in_other_max', 4], ['inconsistencies_in_simple_max', 6]]


@override_config(SYSTEM_SLUGS_FOR_LAST_SYNC_UNISTAT='simple,other')
def test_get_last_sync_system_states(simple_system, other_system):
    simple_system.metainfo.last_check_inconsistencies_finish = datetime.now() - timedelta(days=2)
    other_system.metainfo.last_check_inconsistencies_finish = datetime.now() - timedelta(minutes=10)
    simple_system.metainfo.save()
    other_system.metainfo.save()
    CalculateUnistatMetrics.delay()
    metrics = UnistatView().get_last_sync_states()
    assert sorted(metrics) == [['last_sync_in_other_max', 0.17], ['last_sync_in_simple_max', 48.0]]
