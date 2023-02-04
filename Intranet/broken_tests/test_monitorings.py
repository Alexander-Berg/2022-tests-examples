from collections import Counter
from operator import itemgetter
import time

import freezegun
import pytest

from django.utils import timezone

from tasha.lib import cache_storage
from tasha.models import SyncTime
from tasha.monitorings.metrics import DismissedNotKicked, TimeToKickFromChats
from tasha.monitorings.not_kicked import get_failed_memberships
from tasha.tests import factories

pytestmark = pytest.mark.django_db


URL = '/internal/uhura/staff_monitoring/'
HEADERS = {
    'HTTP_HOST': 'localhost',
    'content_type': 'application/json'
}


def test_bad_request_no_sync(client):
    response = client.get(URL, **HEADERS)
    assert response.status_code == 500


def test_bad_request_long_sync(client):
    SyncTime.objects.create(name='import_staff', last_success_start=timezone.now() - timezone.timedelta(hours=2))
    response = client.get(URL, **HEADERS)
    assert response.status_code == 500
    assert response.content.decode() == 'Last sync was finished 2.0 hours ago'


def test_bad_request_short_sync(client):
    SyncTime.objects.create(name='import_staff', last_success_start=timezone.now())
    response = client.get(URL, **HEADERS)
    assert response.status_code == 200
    assert response.content.decode() == 'Last sync was finished 0.0 hours ago'


@freezegun.freeze_time('2019-01-01')
def test_get_failed_memberships():
    fired_user = factories.UserFactory(quit_at='2018-12-01', is_active=False)
    username = factories.TelegramAccountFactory(user=fired_user)
    inactive_membership = factories.TgMembershipFactory(account=username, is_active=False)  # noqa F841
    membership_with_email = factories.TgMembershipFactory(account=username)
    membership_with_email.actions.create(action='email', added='2018-12-02')
    failed_membership = factories.TgMembershipFactory(account=username)
    failed_memberships = get_failed_memberships()
    assert failed_memberships == [failed_membership.pk]


def test_hanging_monitoring(client):
    for username in ('u1', 'u2', 'u3'):
        factories.TelegramAccountFactory(is_tasha=True, username=username)

    cache_storage.set_key_value('last_finish_u1', time.time() - 1000)
    cache_storage.set_key_value('last_finish_u2', time.time() - 1000000)

    response = client.get('/hanging_monitoring/')
    assert response.status_code == 500
    assert response.content.decode() == 'u2 is hanging\nu3 is not in cache'


@freezegun.freeze_time('2020-01-01')
def test_metrics():

    now = timezone.now()

    account = factories.TelegramAccountFactory(user=None)
    membership = factories.TgMembershipFactory(account=account)
    enter = membership.actions.create(action='user_joined')
    enter.added = timezone.datetime(2019, 11, 1, tzinfo=now.tzinfo)
    enter.save()
    changed_username = membership.actions.create(action='user_changed_username')
    changed_username.added = timezone.datetime(2019, 11, 13, tzinfo=now.tzinfo)
    changed_username.save()

    old_fired_user = factories.UserFactory(
        quit_at=timezone.datetime(2019, 10, 10, tzinfo=now.tzinfo).date(),
        leave_at=timezone.datetime(2019, 10, 10, tzinfo=now.tzinfo),
        is_active=False,
    )
    old_fired_account = factories.TelegramAccountFactory(user=old_fired_user)

    # Старое активное членство, о котором отправили два письма,
    # в результат не должно попасть ни одно
    old_membership_with_email = factories.TgMembershipFactory(
        account=old_fired_account,
        first_notified_from_email=timezone.datetime(2018, 11, 15, tzinfo=now.tzinfo)
    )
    old_mail1 = old_membership_with_email.actions.create(action='email')
    old_mail1.added = timezone.datetime(2018, 11, 15, tzinfo=now.tzinfo)
    old_mail1.save()
    old_mail2 = old_membership_with_email.actions.create(action='email')
    old_mail2.added = timezone.datetime(2019, 12, 20, tzinfo=now.tzinfo)
    old_mail2.save()

    fired_user = factories.UserFactory(
        quit_at=timezone.datetime(2019, 12, 10, tzinfo=now.tzinfo).date(),
        leave_at=timezone.datetime(2019, 12, 10, tzinfo=now.tzinfo),
        is_active=False,
    )
    fired_account = factories.TelegramAccountFactory(user=fired_user)

    another_fired_user = factories.UserFactory(
        quit_at=timezone.datetime(2019, 12, 20, tzinfo=now.tzinfo).date(),
        leave_at=timezone.datetime(2019, 12, 20, tzinfo=now.tzinfo),
        is_active=False
    )
    another_fired_account = factories.TelegramAccountFactory(user=another_fired_user)

    # Вообще не влияет на метрику, так как вышел из чата раньше увольнения
    old_membership = factories.TgMembershipFactory(account=another_fired_account, is_active=False)
    enter = old_membership.actions.create(action='user_joined')
    enter.added = timezone.datetime(2019, 12, 1, tzinfo=now.tzinfo)
    enter.save()
    kick = old_membership.actions.create(action='user_banned_user')
    kick.added = timezone.datetime(2019, 12, 10, tzinfo=now.tzinfo)
    kick.save()

    # Членство неактивное, информации о кике/выходе нет, в результат не попадет
    inactive_membership = factories.TgMembershipFactory(account=fired_account, is_active=False)  # noqa F841

    # Активное членство, о котором отправили два письма,
    # в результат ничего не попадет (TASHA-29)
    membership_with_email = factories.TgMembershipFactory(
        account=fired_account,
        first_notified_from_email=timezone.datetime(2019, 12, 15, tzinfo=now.tzinfo)
    )
    mail1 = membership_with_email.actions.create(action='email')
    mail1.added = timezone.datetime(2019, 12, 15, tzinfo=now.tzinfo)
    mail1.save()
    mail2 = membership_with_email.actions.create(action='email')
    mail2.added = timezone.datetime(2019, 12, 20, tzinfo=now.tzinfo)
    mail2.save()

    # Активное членство, из которого мы один раз удалили пользователя после его увольнения, но он опять вошел в чат
    membership_with_kick = factories.TgMembershipFactory(account=fired_account)
    kick = membership_with_kick.actions.create(action='user_deleted_bot')
    kick.added = timezone.datetime(2019, 12, 16, tzinfo=now.tzinfo)
    kick.save()
    now_join = membership_with_kick.actions.create(action='user_joined')
    now_join.added = timezone.datetime(2019, 12, 20, tzinfo=now.tzinfo)
    now_join.save()

    # Неактивное членство, письма/кика нет, в результат попадет разница между датой увольнения и текущей датой
    failed_membership = factories.TgMembershipFactory(account=fired_account)

    # Пользователь вернулся в штат, установлена дата первого увольнения leave_at
    # В отчет не попадет, отчет только по уволенным
    valid_user = factories.UserFactory(
        quit_at=timezone.datetime(2019, 12, 10, tzinfo=now.tzinfo).date(),
        leave_at=timezone.datetime(2019, 12, 10, tzinfo=now.tzinfo),
        is_active=True,
    )
    valid_username = factories.TelegramAccountFactory(user=valid_user)
    valid_membership_with_kick = factories.TgMembershipFactory(account=valid_username)
    valid_user_old_kick = valid_membership_with_kick.actions.create(action='user_deleted_bot')
    valid_user_old_kick.added = timezone.datetime(2019, 12, 5, tzinfo=now.tzinfo)
    valid_user_old_kick.save()
    self_now_join = valid_membership_with_kick.actions.create(action='user_joined')
    self_now_join.added = timezone.datetime(2019, 12, 8, tzinfo=now.tzinfo)
    self_now_join.save()

    valid_user_kick = valid_membership_with_kick.actions.create(action='user_deleted_bot')
    valid_user_kick.added = timezone.datetime(2019, 12, 20, tzinfo=now.tzinfo)
    valid_user_kick.save()
    self_now_join = valid_membership_with_kick.actions.create(action='user_joined')
    self_now_join.added = timezone.datetime(2019, 12, 21, tzinfo=now.tzinfo)
    self_now_join.save()
    self_kick = valid_membership_with_kick.actions.create(action='user_leaved')
    self_kick.added = timezone.datetime(2019, 12, 22, tzinfo=now.tzinfo)
    self_kick.save()
    self_now_join = valid_membership_with_kick.actions.create(action='user_joined')
    self_now_join.added = timezone.datetime(2019, 12, 26, tzinfo=now.tzinfo)
    self_now_join.save()

    task = TimeToKickFromChats()
    time_kicked = task.get_time_delta_kicked()

    expected_times = sorted([
        (mail1.added - fired_user.leave_at).total_seconds(),
        (kick.added - fired_user.leave_at).total_seconds(),
        (timezone.now() - failed_membership.account.user.leave_at).total_seconds(),
        (timezone.now() - now_join.added).total_seconds(),
        (timezone.now() - changed_username.added).total_seconds(),
    ])

    assert sorted(time_kicked) == expected_times
    assert task.calculate() == [('percentile_90', expected_times[3] / 60 / 60)]

    task = DismissedNotKicked()
    assert task.calculate() == [('not_kicked', 2)]

    task = TimeToKickFromChats()
    time_kicked = task.get_time_delta_kicked(with_memberships=True)
    assert Counter(map(itemgetter(0), time_kicked)) == Counter([
        membership.id,
        membership_with_email.id,
        membership_with_kick.id,
        membership_with_kick.id,
        failed_membership.id,
    ])
