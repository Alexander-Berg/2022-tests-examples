import mock
import pytest

from aiotg.bot import BotApiError
from django.utils import timezone
from django.test.utils import override_settings

from tasha.constants import ACTION
from tasha.models import Action
from tasha.core.old_chat_processor import unban_users
from tasha.true_telegram_bot import kick_users as kick_users_module
from tasha.true_telegram_bot.kick_users import kick_users, unblock_users

from tasha.tests import factories
from tasha.tests.mock_objects import coro_patch
from tasha.tests.utils import MockAioTgChat

pytestmark = [pytest.mark.asyncio, pytest.mark.django_db(transaction=True)]


@pytest.fixture(autouse=True)
def patch_bot_get_me(monkeypatch):
    async def fake_get_me(*args):
        return {'username': 'true_bot'}

    monkeypatch.setattr('tasha.true_telegram_bot.kick_users.Bot.get_me', fake_get_me)


async def test_dont_kick_active_with_username():
    membership = factories.TgMembershipFactory()
    await kick_users()
    membership.refresh_from_db()
    assert membership.is_active


async def test_kick_inactive_with_username(true_bot):
    group = factories.TgGroupInfoFactory()
    factories.TgMembershipFactory(account=true_bot, group=group)
    membership = factories.TgMembershipFactory(group=group)
    membership.account.user.is_active = False
    membership.account.user.save()
    with coro_patch('aiotg.Chat.kick_chat_member', return_value={'result': 'ok'}) as kick_chat_member, \
            coro_patch(
                'aiotg.Chat.get_chat_member',
                return_value={'ok': True, 'result': {'user': {'username': 'username', 'is_bot': False}}}
            ), \
            mock.patch('tasha.true_telegram_bot.kick_users.kick_threshold_exceeded', return_value=False):
        await kick_users()

    assert kick_chat_member.call_args_list == [mock.call(membership.account.telegram_id)]
    membership.refresh_from_db()
    assert not membership.is_active
    action = membership.actions.get()
    assert action.action == ACTION.USER_BANNED_BY_BOT


async def test_unblock_users_user_left():
    """
    Человек сам вышел из чата => не пытаемся разбанить
    """
    membership = factories.TgMembershipFactory(is_active=False)
    membership.actions.create(action=ACTION.USER_BANNED_BY_BOT)
    membership.actions.create(action=ACTION.USER_JOINED_TO_CHAT)
    membership.actions.create(action=ACTION.USER_LEAVED)

    await unblock_users()
    membership.refresh_from_db()
    assert not membership.is_active


async def test_unblock_users_not_active_user():
    """
    Пользователь все еще не активен => не пытаемся разбанить
    """
    membership = factories.TgMembershipFactory(is_active=False)
    membership.account.user.is_active = False
    membership.account.user.save()
    membership.actions.create(action=ACTION.USER_LEAVED)

    await unblock_users()
    membership.refresh_from_db()
    assert not membership.is_active


async def test_unblock_user(true_bot):
    """
    Забаненный стал активным => разбаниваем
    """
    user1 = factories.UserFactory(is_active=True)
    account1 = factories.TelegramAccountFactory(username='user1', telegram_id=1001, user=user1)

    group1 = factories.TgGroupInfoFactory(telegram_id=-102, title='group1', god_bot_number=10)

    membership = factories.TgMembershipFactory(group=group1, account=account1, is_active=False)
    membership.actions.create(action=ACTION.USER_JOINED_TO_CHAT)
    membership.actions.create(action=ACTION.USER_BANNED_BY_BOT)

    factories.TgMembershipFactory(group=group1, account=true_bot)

    with override_settings(GOD_BOT_NUMBER=10):
        with coro_patch('tasha.lib.telegram.bot.unban_chat_member', return_value={'ok': True}) as unban_chat_member:
            await unban_users.unban_users()

    unban_chat_member.assert_called_with(
        chat_id=group1.telegram_id,
        user_id=account1.telegram_id,
        only_if_banned=True
    )
    membership.refresh_from_db()
    assert not membership.is_active
    assert membership.actions.order_by('added').last().action == ACTION.USER_UNBANNED_BY_BOT


async def test_send_email_to_admins(monkeypatch, true_bot, mailoutbox):
    MockAioTgChat.kick_error = BotApiError
    MockAioTgChat.chat_info = {'id': 100, 'title': 'test_chat'}

    group = factories.TgGroupInfoFactory()
    factories.TgMembershipFactory(account=true_bot, group=group)
    admin = factories.TgMembershipFactory(is_admin=True, group=group)
    admin.account.user.email = 'some_mail@yandex-team.ru'
    admin.account.user.username = 'some-username'
    admin.account.user.save()
    membership = factories.TgMembershipFactory(group=group)
    membership.account.user.is_active = False
    membership.account.user.save()

    assert membership.first_notified_from_email is None

    MockAioTgChat.users_info[admin.account.telegram_id] = {
        'result': {
            'user': {
                'username': admin.account.username
            }
        }
    }
    MockAioTgChat.users_info[membership.account.telegram_id] = {
        'ok': True,
        'result': {
            'user': {
                'id': membership.account.telegram_id,
                'username': membership.account.username,
            },
            'status': 'member',
        }
    }
    MockAioTgChat.chat_info = {'result': {'id': admin.group.telegram_id, 'title': admin.group.title}}

    monkeypatch.setattr(kick_users_module, 'Chat', MockAioTgChat)
    with coro_patch('aiotg.Chat.kick_chat_member', return_value={'result': 'error'}):
        with mock.patch('tasha.true_telegram_bot.kick_users.kick_threshold_exceeded', return_value=False):
            await kick_users()

    membership.refresh_from_db()

    assert membership.is_active
    # Удалить из группы не получилось, отправим письмо
    action = Action.objects.get()
    assert action.action == 'email'
    assert action.membership == membership
    assert membership.first_notified_from_email.date() == timezone.now().date()
    assert len(mailoutbox) == 1
    message = mailoutbox[0]
    assert message.to == ['some_mail@yandex-team.ru']
    assert admin.group.title in message.body
    assert membership.account.username in message.body
    assert admin.account.username not in message.body
