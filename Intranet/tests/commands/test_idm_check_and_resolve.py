# coding: utf-8


import socket
import mock
from collections import defaultdict

import pytest
from django.conf import settings
from django.core import mail
from django.core.management import call_command
from django.utils import timezone
from django.utils.timezone import make_aware
from django.utils.timezone import datetime, timedelta
from freezegun import freeze_time

from idm.sync import everysync
from idm.core.models import Role, System, RoleNode
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import make_inconsistency, mock_all_roles, refresh, assert_contains, raw_make_role

pytestmark = [pytest.mark.django_db, pytest.mark.robot]


def test_cannot_resolve_inconsistencies_in_inactive_or_broken_system(simple_system, arda_users):
    """Проверим, что нельзя разрешить неконсистентности в сломанной или неактивной системе"""

    make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=arda_users.frodo,
        system=simple_system,
        path='/role/admin/',
    )

    System.objects.filter(pk=simple_system.pk).update(is_broken=True, is_active=True)
    call_command('idm_check_and_resolve')
    assert Role.objects.count() == 0
    call_command('idm_check_and_resolve', '--system', 'simple')
    assert Role.objects.count() == 0

    System.objects.filter(pk=simple_system.pk).update(is_broken=False, is_active=False)
    call_command('idm_check_and_resolve')
    assert Role.objects.count() == 0
    call_command('idm_check_and_resolve', '--system', 'simple')
    assert Role.objects.count() == 0

    System.objects.filter(pk=simple_system.pk).update(is_broken=True, is_active=False)
    call_command('idm_check_and_resolve')
    assert Role.objects.count() == 0
    call_command('idm_check_and_resolve', '--system', 'simple')
    assert Role.objects.count() == 0

    System.objects.filter(pk=simple_system.pk).update(is_broken=False, is_active=True)
    call_command('idm_check_and_resolve')
    assert Role.objects.count() == 1


@pytest.mark.parametrize('sync_success', [True, False])
@pytest.mark.parametrize('log_check_time', [True, False])
def test_check_timestamp_is_set(simple_system, arda_users, log_check_time, sync_success):
    """Проверим, что metainfo.last_check_inconsistencies_finish записывается только если запустить команду с параметром --log-time
    и только в том случае, если сверка прошла успешно"""

    simple_system.metainfo.last_check_inconsistencies_start = None
    simple_system.metainfo.last_check_inconsistencies_finish = None
    simple_system.metainfo.save()

    make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=arda_users.frodo,
        system=simple_system,
        path='/role/admin/',
    )
    if sync_success:
        kwargs = {
            'user_roles': [{
                'login': 'frodo',
                'roles': [{'role': 'admin'}]
            }]
        }
    else:
        kwargs = {
            'side_effect': socket.error
        }
    args = ['idm_check_and_resolve']
    if log_check_time:
        args.append('--log-time')
    with mock_all_roles(simple_system, **kwargs):
        call_command(*args)

    simple_system = refresh(simple_system)
    if log_check_time:
        assert simple_system.metainfo.last_check_inconsistencies_start is not None
    else:
        assert simple_system.metainfo.last_check_inconsistencies_start is None
    if log_check_time and sync_success:
        assert simple_system.metainfo.last_check_inconsistencies_finish is not None
    else:
        assert simple_system.metainfo.last_check_inconsistencies_finish is None


@pytest.mark.parametrize('report_unchecked', [True, False])
def test_check_failure_email(simple_system, complex_system, arda_users, report_unchecked):
    """Проверим отправку письма, если проверка не проходила больше n дней"""

    last_success = datetime(1970, 1, 1)
    make_aware(last_success, timezone.utc)
    simple_system.metainfo.last_check_inconsistencies_finish = last_success
    simple_system.metainfo.save()

    complex_system.metainfo.last_check_inconsistencies_finish = timezone.now()
    complex_system.metainfo.save()

    threshold = settings.IDM_CHECK_AND_RESOLVE_ALERT_THERSHOLD_DAYS
    # через n+1 дней фейлов команда фейлится, должны отправить письмо
    with freeze_time(last_success + timedelta(days=threshold + 1)):
        with mock_all_roles(simple_system, side_effect=socket.error):
            call_command('idm_check_and_resolve', log_check_time=True, report_unchecked=report_unchecked)

    if report_unchecked:
        assert len(mail.outbox) == 3
        message = mail.outbox[0]
        assert message.subject == 'Проблемы со сверкой ролей в системе Simple система.'
        assert_contains([
            'В системе Simple система сверка ролей не проводилась уже больше {} дней.'.format(threshold),
            'Последняя сверка была 1 января 1970 г. 0:00.',
        ], message.body)
    else:
        assert len(mail.outbox) == 0


def test_sync_dumb_system(dumb_system, arda_users, settings):
    """Убедимся, что для dumb-систем аккуратно доудаляются узлы"""

    assert RoleNode.objects.count() == 6

    depriving_node = RoleNode.objects.get(slug='manager')
    role = raw_make_role(arda_users.frodo, dumb_system, {'role': 'manager'}, state='granted')

    depriving_node.state = 'depriving'
    depriving_node.depriving_at = timezone.now() - timezone.timedelta(1)
    depriving_node.save(update_fields=['state', 'depriving_at'])

    call_command('idm_check_and_resolve')

    depriving_node = refresh(depriving_node)
    role = refresh(role)

    assert role.state == 'deprived'
    assert depriving_node.state == 'deprived'
    assert RoleNode.objects.count() == 6
    assert RoleNode.objects.filter(state='active').count() == 5


def set_weekday(now: datetime, weekday: int):
    while now.weekday() != weekday:
        now -= timedelta(days=1)
    return now


def test_system_sync_spread_by_time(simple_system, complex_system, dumb_system):
    synced_systems = defaultdict(int)

    def sync_roles_mock(system, *args, **kwargs):
        synced_systems[system.id] += 1

    with mock.patch.object(everysync, 'sync_roles_and_nodes', side_effect=sync_roles_mock):
        now = set_weekday(datetime.now(), 1)  # вторник
        synced_systems_count = 0
        for hour in range(0, 23):
            with freeze_time(now.replace(second=0, minute=0, hour=hour)):
                call_command('idm_check_and_resolve', daily_schedule_interval=3600)

            # В каждый интервал либо никакая система не синкается, либо синкается одна
            current_synced_systems_count = sum(synced_systems.values())
            if synced_systems_count + 1 == current_synced_systems_count:
                synced_systems_count += 1
            else:
                assert synced_systems_count == current_synced_systems_count

        # В итоге, за весь день все системы должны посинкаться
        assert synced_systems_count == 3


def test_system_sync_on_weekend(simple_system, complex_system, dumb_system):
    synced_systems = defaultdict(int)

    def sync_roles_mock(system, *args, **kwargs):
        synced_systems[system.id] += 1

    now = set_weekday(datetime.now().replace(second=0, minute=0, hour=10), 6)  # воскресенье

    with mock.patch.object(everysync, 'sync_roles_and_nodes', side_effect=sync_roles_mock):
        with freeze_time(now.replace(second=0, minute=0, hour=10)):  # 10:00
            call_command('idm_check_and_resolve', daily_schedule_interval=3600)

        for system in (simple_system, complex_system, dumb_system):
            assert synced_systems[system.id] == 0, 'sync on sunday'
        now -= timedelta(days=1)

        with freeze_time(now.replace(second=0, minute=0, hour=10)):  # 10:00
            call_command('idm_check_and_resolve', daily_schedule_interval=3600)

        for system in (simple_system, complex_system, dumb_system):
            assert synced_systems[system.id] == 0, 'sync on saturday'
