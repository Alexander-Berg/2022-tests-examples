import os

import pytest

from tasha.core.old_chat_processor import TELEGRAM_TRUE_BOT_USERNAME
from tasha.core.dbproxy import DBProxy
from tasha.tests import factories

pytestmark = [pytest.mark.asyncio, pytest.mark.django_db(transaction=True)]


async def test_get_chats_with_true_bot():
    true_bot = factories.UserFactory()
    tg_username = factories.TelegramAccountFactory(username=os.environ['TELEGRAM_TRUE_BOT_USERNAME'], user=true_bot)
    factories.TgMembershipFactory()
    group_with_bot = factories.TgGroupInfoFactory()
    group_without_bot = factories.TgGroupInfoFactory()  # noqa
    another_group_without_bot = factories.TgGroupInfoFactory()
    factories.TgMembershipFactory(group=group_with_bot, account=tg_username)
    factories.TgMembershipFactory(group=another_group_without_bot, account=tg_username, is_active=False)

    db_proxy = await DBProxy.create()
    db_chats_with_truebot = {
        row['telegram_id'] for row in await db_proxy.get_chats_with_true_bot(TELEGRAM_TRUE_BOT_USERNAME)
    }
    assert db_chats_with_truebot == {group_with_bot.telegram_id}
    await db_proxy.close()


async def test_get_active_chats_for():
    account1 = factories.TelegramAccountFactory(username='user1', telegram_id=1)
    account2 = factories.TelegramAccountFactory(username='user2', telegram_id=2)

    group1 = factories.TgGroupInfoFactory(telegram_id=101, title='group1', god_bot_number=None)
    group2 = factories.TgGroupInfoFactory(telegram_id=102, title='group2', god_bot_number=10)
    group3 = factories.TgGroupInfoFactory(telegram_id=103, title='group3', god_bot_number=10)

    factories.TgMembershipFactory(account_id=account1.id, group_id=group1.id)
    factories.TgMembershipFactory(account_id=account1.id, group_id=group2.id)
    factories.TgMembershipFactory(account_id=account1.id, group_id=group3.id)
    factories.TgMembershipFactory(account_id=account2.id, group_id=group1.id)
    factories.TgMembershipFactory(account_id=account2.id, group_id=group2.id)

    db_proxy = await DBProxy.create()
    assert {r['telegram_id'] for r in await db_proxy.get_active_chats_for('user1')} == {
        group1.telegram_id,
        group2.telegram_id,
        group3.telegram_id,
    }
    assert {r['telegram_id'] for r in await db_proxy.get_active_chats_for('user1', god_bot_number=10)} == {
        group2.telegram_id,
        group3.telegram_id,
    }
    assert {r['telegram_id'] for r in await db_proxy.get_active_chats_for('user2')} == {
        group1.telegram_id,
        group2.telegram_id,
    }
    assert {r['telegram_id'] for r in await db_proxy.get_active_chats_for('user2', god_bot_number=10)} == {
        group2.telegram_id,
    }
