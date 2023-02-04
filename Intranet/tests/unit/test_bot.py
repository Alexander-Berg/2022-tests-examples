import pytest
import responses
from mock import Mock

from stackbot.main import BotHandler
from stackbot.config import settings
from stackbot.db import BotUser
from stackbot.logic.utils import create_bot_user

from unit.utils import (
    staff_api_result,
    get_staff_api_auth_url,
)


@pytest.fixture
def update():
    update = Mock()
    update.effective_chat = Mock()
    update.effective_chat.id = 1
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
        get_staff_api_auth_url([update.effective_user.username]),
        status=200,
        json={'result': []}
    )
    bot._start(update=update, context=context)

    context.bot.send_message.assert_called_once_with(
        chat_id=update.effective_chat.id,
        text=settings.NO_AUTH_MESSAGE,
        parse_mode='Markdown',
    )


@responses.activate
def test_bot_new(update, scope_session, staff_info):
    staff = staff_info(telegram_account=update.effective_user.username)
    bot = BotHandler()
    context = Mock()
    responses.add(
        responses.GET,
        get_staff_api_auth_url([update.effective_user.username]),
        json={'result': [staff_api_result(staff)]}
    )
    responses.add(
        responses.GET,
        f'{settings.STACK_API_HOSTS[0]}api/2.3/users/',
        json={'items': [{'display_name': staff.login}]}
    )
    assert not scope_session.query(BotUser).count()
    bot._start(update=update, context=context)

    context.bot.send_message.assert_called_once_with(
        chat_id=update.effective_chat.id,
        parse_mode='Markdown',
        text=f'Hello, `@{staff.login}`',
    )
    bot_user = scope_session.query(BotUser).filter(
        BotUser.telegram_id == str(update.effective_user.id)
    ).first()
    assert bot_user.staff_uid == staff.uid
    assert bot_user.username == update.effective_user.username
    assert bot_user.telegram_id == str(update.effective_user.id)


@responses.activate
def test_bot_existed(update, scope_session, staff_info):
    staff = staff_info(telegram_account=update.effective_user.username)
    bot = BotHandler()
    context = Mock()
    create_bot_user(
        db=scope_session,
        staff_info=staff,
        username=update.effective_user.username,
        telegram_id=update.effective_user.id
    )

    bot._start(update=update, context=context)

    context.bot.send_message.assert_called_once_with(
        chat_id=update.effective_chat.id,
        parse_mode='Markdown',
        text=f'Already authorized: `@{staff.login}`',
    )
