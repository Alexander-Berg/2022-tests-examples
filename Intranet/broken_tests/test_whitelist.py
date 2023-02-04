import mock
import nest_asyncio
import pytest

from aiotg import Bot, Chat
from django.test.utils import override_settings

from tasha.constants import CHAT_TYPE
from tasha.models import TgGroupInfo, TgMembership, TelegramAccount
from tasha.true_telegram_bot.bot_watcher import Watcher

from tasha.tests import factories
from tasha.tests.mock_objects import coro_patch

nest_asyncio.apply()
pytestmark = [pytest.mark.asyncio, pytest.mark.django_db(transaction=True)]


@pytest.fixture
def bot():
    return Bot('api_token')


@pytest.fixture
def mock_bot():
    responses = {
        'getchat': {
            'id': -100987654321,
            'title': 'go-chaos',
            'type': CHAT_TYPE.SUPERGROUP,
            'description': 'Тестируем фолбэки Яндекс.Go',
            'invite_link': 'https://t.me/joinchat/FY-X99NGdsNzBi',
            'permissions': {},
            'photo': {},
            'pinned_message': {},
        },
        'getchatmember': {
            'user': {
                'id': 110921999,
                'is_bot': False,
                'first_name': 'Olga',
                'last_name': 'Ramblerova',
                'username': 'olgaram',
            },
            'status': 'member',
        },
        'getchatmemberscount': 22,
    }

    return coro_patch(
        'aiotg.Bot.api_call',
        side_effect=lambda method_name, *args, **kwargs: {'ok': True, 'result': responses[method_name]},
    )


async def test_whitelist_enter(bot):
    invited_user_id = 1001
    invited_user_name = 'whl_account'

    # Author user
    author_user = factories.UserFactory(username='author')
    whl_account = factories.TelegramAccountFactory(user=None, telegram_id=invited_user_id, username=invited_user_name)
    group = factories.TgGroupInfoFactory(telegram_id=1234)

    factories.TgWhitelistEntryFactory(
        user_account=whl_account,
        group=group,
        author=author_user,
    )

    with coro_patch('aiotg.chat.Chat.kick_chat_member', return_value={'ok': True}) as kick_chat_member, \
            coro_patch('aiotg.Bot.get_me', return_value={'username': 'bot_username', 'id': 100000}):
        watcher = Watcher()
        chat = Chat(bot=bot, chat_id=1234, src_message={'from': {'id': 123}})
        await watcher.handle_enter(
            chat=chat, invited_user={'id': invited_user_id, 'username': invited_user_name, 'is_bot': False}
        )

    assert not kick_chat_member.called
    await watcher.db_proxy.close()


async def test_kick_not_in_whitelist(bot, monkeypatch):
    invited_user_id = 1001
    invited_user_name = 'whl_account'

    kick_user_id = 1002
    kick_user_name = 'kick_account'

    # Author user
    author_user = factories.UserFactory(username='author')
    whl_account = factories.TelegramAccountFactory(user=None, telegram_id=invited_user_id, username=invited_user_name)
    group = factories.TgGroupInfoFactory(telegram_id=1234)

    factories.TgWhitelistEntryFactory(
        user_account=whl_account,
        group=group,
        author=author_user,
    )

    with coro_patch('aiotg.chat.Chat.kick_chat_member', return_value={'ok': True}) as kick_chat_member, \
            coro_patch('aiotg.Bot.get_me', return_value={'username': 'bot_username', 'id': 100000}):
        watcher = Watcher()
        chat = Chat(bot=bot, chat_id=1234, src_message={'from': {'id': 123}})
        await watcher.handle_enter(
            chat=chat, invited_user={'id': invited_user_id, 'username': invited_user_name, 'is_bot': False}
        )
        await watcher.handle_enter(
            chat=chat, invited_user={'id': kick_user_id, 'username': kick_user_name, 'is_bot': False}
        )

    assert kick_chat_member.call_args_list == [mock.call(kick_user_id)]
    await watcher.db_proxy.close()
