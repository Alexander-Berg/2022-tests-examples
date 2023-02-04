import pytest
import responses
from mock import Mock

from watcher.bot.core import BotHandler
from watcher.config import settings
from watcher.db import BotUser, Chat
from watcher.enums import ChatType


@pytest.fixture
def update():
    update = Mock()
    update.effective_chat = Mock()
    update.effective_chat.id = 1
    update.effective_chat.username = 'smth'
    update.effective_chat.type = ChatType.private
    update.effective_user = Mock()
    update.effective_user.username = 'smth'
    update.effective_user.id = 100

    return update


@responses.activate
def test_bot_unknown_user(update):
    bot = BotHandler()
    context = Mock()
    responses.add(
        responses.GET,
        f'{settings.STAFF_API_HOST}v3/persons/?telegram_accounts.value=smth&_fields=uid%2Clogin',
        status=200,
        json={'results': []}
    )
    bot._start(update=update, context=context)

    context.bot.send_message.assert_called_once_with(
        chat_id=update.effective_chat.id,
        text='Unknown account',
        parse_mode='Markdown'
    )


@responses.activate
def test_bot_new(update, scope_session, test_request_user):
    bot = BotHandler()
    context = Mock()
    responses.add(
        responses.GET,
        f'{settings.STAFF_API_HOST}v3/persons/?telegram_accounts.value=smth&_fields=uid%2Clogin',
        status=200,
        json={'result': [{'login': test_request_user.login, 'uid': test_request_user.uid}]}
    )
    responses.add(
        responses.POST,
        f'{settings.JNS_API_HOST}api/telegram/users/upsert',
        status=200,
    )
    assert not scope_session.query(BotUser).count()
    assert not scope_session.query(Chat).count()

    bot._start(update=update, context=context)

    context.bot.send_message.assert_called_once_with(
        chat_id=update.effective_chat.id,
        text=f'Successfull authorization: `@{test_request_user.login}`',
        parse_mode='Markdown'
    )
    bot_user = scope_session.query(BotUser).filter(
        BotUser.username == update.effective_user.username
    ).first()
    assert bot_user.staff == test_request_user
    assert bot_user.telegram_id == str(update.effective_user.id)

    chat = scope_session.query(Chat).filter(
        Chat.author_id == bot_user.id
    ).first()

    assert chat.id == update.effective_chat.id
    assert chat.chat_type == ChatType.private
    assert chat.title == update.effective_user.username


@responses.activate
def test_bot_existed(update, bot_user_factory, scope_session, test_request_user):
    bot = BotHandler()
    context = Mock()
    bot_user_factory(
        staff=test_request_user,
        username=update.effective_user.username,
        telegram_id=update.effective_user.id
    )

    bot._start(update=update, context=context)

    context.bot.send_message.assert_called_once_with(
        chat_id=update.effective_chat.id,
        text=f'Already authorized: `@{test_request_user.login}`',
        parse_mode='Markdown'
    )


@responses.activate
@pytest.mark.parametrize(
    'group_type',
    [ChatType.group, ChatType.channel, ChatType.supergroup]
)
def test_bot_group_chat(update, scope_session, test_request_user, group_type):
    bot = BotHandler()
    context = Mock()
    update.effective_chat.type = group_type

    bot._start(update=update, context=context)

    context.bot.send_message.assert_called_once_with(
        chat_id=update.effective_chat.id,
        text='Group chats not supported',
        parse_mode='Markdown'
    )
