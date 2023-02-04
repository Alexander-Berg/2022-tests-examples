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


async def test_handle_enter_saves_non_existent_members(bot):
    invited_user_id = 12
    invited_bot_id = 21
    factories.TgGroupInfoFactory(telegram_id=1234)
    assert TgMembership.objects.count() == 0

    with coro_patch('aiotg.chat.Chat.kick_chat_member', return_value={'ok': True}) as kick_chat_member, \
            coro_patch('aiotg.Bot.get_me', return_value={'username': 'bot_username', 'id': 100000}):
        watcher = Watcher()
        chat = Chat(bot=bot, chat_id=1234, src_message={'from': {'id': 123}})
        await watcher.handle_enter(
            chat=chat, invited_user={'id': invited_user_id, 'username': 'any_username', 'is_bot': False}
        )
        await watcher.handle_enter(
            chat=chat, invited_user={'id': invited_bot_id, 'username': 'username_bot', 'is_bot': True}
        )

    assert TgMembership.objects.count() == 2
    assert kick_chat_member.call_args_list == [mock.call(invited_user_id)]
    await watcher.db_proxy.close()


async def test_dont_kick_from_unknown_chats(bot, monkeypatch):
    with coro_patch('aiotg.Bot.get_me', return_value={'username': 'bot_username', 'id': 100000}):
        watcher = Watcher()

    with coro_patch('aiotg.chat.Chat.kick_chat_member') as kick_chat_memebers:
        chat = Chat(bot=bot, chat_id=1234, src_message={'from': {'id': 12}})

    assert TgGroupInfo.objects.count() == 0
    await watcher.handle_enter(chat=chat, invited_user={'id': 12, 'username': 'any_username', 'is_bot': False})
    assert kick_chat_memebers.call_args_list == []
    await watcher.db_proxy.close()


@pytest.mark.parametrize('shard_numbers', [None, [10], [20, 30, 40]])
async def test_new_chat_gets_shard_number(mock_bot, shard_numbers):
    with coro_patch('aiotg.Bot.get_me', return_value={'username': 'bot_username', 'id': 1000000}):
        watcher = Watcher()

    message = {
        'chat': {
            'id': -100987654321,
            'type': CHAT_TYPE.GROUP,
        },
    }

    tasha = TelegramAccount(username='bot_username', telegram_id=1000000)
    tasha.save()

    assert TgGroupInfo.objects.count() == 0

    with mock_bot, override_settings(SHARD_NUMBERS=shard_numbers):
        await watcher.ensure_chat_is_known(message=message)

    new_group = TgGroupInfo.objects.get()
    assert new_group.telegram_id == -100987654321
    assert new_group.chat_type == CHAT_TYPE.SUPERGROUP
    if shard_numbers:
        assert new_group.god_bot_number in shard_numbers
    else:
        assert new_group.god_bot_number is None
