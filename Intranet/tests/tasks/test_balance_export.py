# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from decimal import Decimal
import freezegun
import mock
import pytest

from django.core.management import call_command
from django.utils import timezone

from core.models.reward import Reward
from core.models.user import NewPaymentInfo
from core.tests.factory import ReporterFactory, RewardFactory


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('table_exists', [False, True])
@freezegun.freeze_time('2019-09-03 21:19:00')
def test_export_rewards_to_yt(table_exists):

    def create_reward(idx, status, date):
        reporter = ReporterFactory(uid=idx, balance_client_id=idx)
        with mock.patch('core.models.user.datasync.update_or_create_payment_info_entry'):
            NewPaymentInfo.objects.create(reporter_id=reporter.id)
        reward = RewardFactory(reporter=reporter, payment_amount_usd=100*(idx+1), payment_currency=Reward.C_USD,
                               status=status, balance_contract_created=date)

    yesterday = timezone.now() - timezone.timedelta(days=1)
    # Новый день наступает в 21:00 UTC
    still_yesterday = timezone.now() - timezone.timedelta(hours=1)
    today = timezone.now() - timezone.timedelta(minutes=5)
    for idx in range(2):
        create_reward(idx, 1, yesterday)
    create_reward(2, 1, still_yesterday)
    for idx in range(3, 6):
        create_reward(idx, 1, today)
    for idx in range(6, 9):
        create_reward(idx, 0, yesterday)
    with mock.patch('app.tasks.balance_export.yt') as mocked_yt:
        mocked_yt.exists.return_value = table_exists
        call_command('bugbounty_export_rewards_to_yt')
    if not table_exists:
        clients = [line['client_id'] for line in mocked_yt.write_table.call_args[0][1]]
        assert clients == ['0', '1', '2']
        amounts = {line['money'] for line in mocked_yt.write_table.call_args[0][1]}
        assert amounts == {'100.00', '200.00', '300.00'}
        assert mocked_yt.write_table.call_args[0][0] == '//home/bugbounty/export_payments_to_balance/production/2019-09-03'
    else:
        assert mocked_yt.write_table.call_count == 0


@pytest.mark.parametrize('table_exists', [False, True])
@freezegun.freeze_time('2019-09-03 20:19:00')
def test_export_rewards_to_yt_with_respect_to_currency(table_exists):

    def create_reward(idx, **kwargs):
        reporter = ReporterFactory(uid=idx, balance_client_id=idx)
        with mock.patch('core.models.user.datasync.update_or_create_payment_info_entry'):
            NewPaymentInfo.objects.create(reporter_id=reporter.id)
        data = {
            'reporter': reporter,
            'payment_amount_usd': 10*idx,
            'payment_amount_rur': 300*idx,
            'payment_currency': Reward.C_RUR if idx == 1 else Reward.C_USD,
            'status': Reward.ST_PAYABLE,  # payable
            'balance_contract_created': timezone.now() - timezone.timedelta(days=1),
        }
        data.update(kwargs)
        reward = RewardFactory(**data)

    create_reward(1)
    create_reward(2)

    with mock.patch('app.tasks.balance_export.yt') as mocked_yt:
        mocked_yt.exists.return_value = table_exists
        call_command('bugbounty_export_rewards_to_yt')
    if not table_exists:
        clients = [line['client_id'] for line in mocked_yt.write_table.call_args[0][1]]
        assert set(clients) == {'1', '2'}
        amounts = {line['money'] for line in mocked_yt.write_table.call_args[0][1]}
        assert amounts == {
            '300.00',   # uid=1, RUB
            '20.00',    # uid=2, USD
        }
        assert mocked_yt.write_table.call_args[0][0] == '//home/bugbounty/export_payments_to_balance/production/2019-09-02'
    else:
        assert mocked_yt.write_table.call_count == 0
