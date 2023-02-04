from unittest.mock import call, patch

import pytest

from tasha.lib.mail import notify_about_not_kicked_users

from tasha.tests.mock_objects import coro_patch, CoroMock
from tasha.tests.utils import MockTelethonClient

pytestmark = [pytest.mark.django_db(transaction=True), pytest.mark.asyncio]


@pytest.fixture
def not_kicked_users():
    return [
        {
            'first_name': 'first_name',
            'last_name': 'last_name',
            'username': 'username'
        },
        {
            'username': 'other_username'
        }
    ]


@pytest.fixture
def telethon_client():
    client = MockTelethonClient()
    setattr(client, 'send_message', CoroMock())
    return client


async def test_notify_about_not_kicked_users_no_emails(not_kicked_users, db_proxy, telethon_client):
    setattr(db_proxy, 'register_email', CoroMock())
    # вызовем два раза и проверим, что отправили одно сообщение
    with coro_patch('tasha.lib.mail.bot.send_message') as send_message_mock:
        for _ in range(2):
            await notify_about_not_kicked_users(
                'bot_username', [], not_kicked_users, 'Title', 12345, db_proxy, client=telethon_client
            )

    message_text = (
        'Внимание! В чате имеются пользователи, чьи telegram-аккаунты не являются аккаунтами действующих '
        'сотрудников Яндекса\nfirst_name last_name (@username)\n(@other_username)'
    )
    assert send_message_mock.call_args_list == [call(chat_id=12345, text=message_text)]
    assert db_proxy.register_email.call_args_list == [call(not_kicked_users, 12345)]


async def test_notify_about_not_kicked_users_email(not_kicked_users, db_proxy, telethon_client):
    admin_emails = ['x@yandex-team.ru']
    setattr(db_proxy, 'register_email', CoroMock())
    with patch('tasha.lib.mail.send_mail') as send_mail:
        await notify_about_not_kicked_users(
            'bot_username', admin_emails, not_kicked_users, 'Title', 12345, db_proxy, client=telethon_client
        )

    assert send_mail.call_args_list == [call(
        admin_emails,
        '[@bot_username] Unknown users have been detected in your chat Title',
        'telegram_notify',
        {
            'bot_username': 'bot_username',
            'print_users': ['first_name last_name (@username)', '(@other_username)'],
            'group_title': 'Title',
        }
    )]
    assert db_proxy.register_email.call_args_list == [call(not_kicked_users, 12345)]


async def test_notify_about_not_kicked_users_no_users(not_kicked_users, db_proxy):
    setattr(db_proxy, 'register_email', CoroMock())
    await notify_about_not_kicked_users('bot_username', ['x@yandex-team.ru'], [], 'Title', 12345, db_proxy)
    assert db_proxy.register_email.call_args_list == []
