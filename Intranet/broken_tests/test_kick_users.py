# coding: utf-8
import mock
import pytest
from django.test.utils import override_settings
from django.utils import timezone

from tasha.models import TgGroupInfo
from tasha.core.old_chat_processor import kick_users, chat_processor
from tasha.tests import factories
from tasha.tests.mock_objects import coro_patch
from tasha.tests.utils import MockTelethonClient, MockTelethonDialog, MockTelethonParticipant

pytestmark = [pytest.mark.asyncio, pytest.mark.django_db(transaction=True)]

m_client = MockTelethonClient(admins=['true_bot'])

async def _mock_external_staff_get_person_data(username_or_phone):
    return None


# Don't remove argument, used in runtime
async def _mock_get_client(*args):
    return m_client


@pytest.fixture(autouse=True)
def patch_bot_get_me(monkeypatch):
    async def fake_get_me(*args):
        return {'username': 'true_bot', 'id': 100000000000000}

    monkeypatch.setattr('tasha.old_chat_processor.kick_users.Bot.get_me', fake_get_me)


@pytest.mark.parametrize('kick_ratio', [0.0, 1.0])
async def test_kick_ratio(monkeypatch, telegram_test_users, kick_ratio):
    # Bot account
    bot_account = factories.TelegramAccountFactory(username='true_bot', telegram_id=901, is_bot=True)

    user1 = factories.UserFactory(is_active=False)
    user2 = factories.UserFactory(is_active=False)
    account1 = factories.TelegramAccountFactory(username='user1', telegram_id=1001, user=user1)
    account2 = factories.TelegramAccountFactory(username='user2', telegram_id=1002, user=user2)

    group1 = factories.TgGroupInfoFactory(telegram_id=-101, title='group1', god_bot_number=10)

    factories.TgMembershipFactory(account_id=account1.id, group_id=group1.id)
    factories.TgMembershipFactory(account_id=account2.id, group_id=group1.id)
    factories.TgMembershipFactory(account_id=bot_account.id, group_id=group1.id)

    kick_users.DRY_RUN = False
    chat_processor.SMALL_CHAT_KICK_RATIO_THRESHOLD = kick_ratio

    MockTelethonDialog(
        dialog_id=group1.telegram_id,
        title=group1.title,
        client=m_client,
        admins=[
            MockTelethonParticipant(
                username=bot_account.username, telegram_id=bot_account.telegram_id, client=m_client, bot=True
            )
        ],
        participants=[
            MockTelethonParticipant(username=account1.username, telegram_id=account1.telegram_id, client=m_client),
            MockTelethonParticipant(username=account2.username, telegram_id=account2.telegram_id, client=m_client)
        ]
    )

    with override_settings(GOD_BOT_NUMBER=10):
        with coro_patch('aiotg.Chat.kick_chat_member', return_value={'result': True}) as ban_patch:
            with coro_patch('tasha.lib.mail.notify_about_not_kicked_users'):
                with mock.patch('tasha.external.telethon.api.get_client', _mock_get_client):
                    await kick_users.kick_users()

    if kick_ratio > 0:
        # frodo, saruman
        assert len(ban_patch.call_args_list) == 2
    else:
        assert len(ban_patch.call_args_list) == 0

    # TODO: тут надо нормально замокать CURRENT_TIMESTAMP у Connection, но пока так
    now = timezone.now()
    group = TgGroupInfo.objects.get(telegram_id=-101)
    assert group.last_successful_scan is not None
    assert now - timezone.timedelta(minutes=1) < group.last_successful_scan < now + timezone.timedelta(minutes=1)
