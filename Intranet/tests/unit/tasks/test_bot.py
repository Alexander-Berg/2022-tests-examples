import pytest
import responses

from watcher.config import settings
from watcher.db import BotUser, Chat
from watcher.tasks.bot import remove_unauthorized_bot_users, STAFF_PAGE_LIMIT


@responses.activate
@pytest.mark.parametrize('correct_telegram', [True, False])
@pytest.mark.parametrize('is_dismissed', [True, False])
def test_remove_unauthorized_bot_users(
    scope_session, correct_telegram, is_dismissed,
    bot_user_factory, chat_factory
):
    bot_user = bot_user_factory()
    chat_factory(author=bot_user)
    telegram = bot_user.username if correct_telegram else 'wrong_telegram'
    if is_dismissed:
        bot_user.staff.is_dismissed = True
    scope_session.commit()
    responses.add(
        responses.GET,
        f'{settings.STAFF_API_HOST}v3/persons/'
        f'?login={bot_user.staff.login}&_fields=telegram_accounts.value%2Clogin&_limit={STAFF_PAGE_LIMIT}',
        status=200,
        json={'result': [
            {
                'login': bot_user.staff.login,
                'telegram_accounts': [{'value': telegram}],
            }
        ]
        }
    )
    responses.add(
        responses.POST,
        f'{settings.JNS_API_HOST}api/telegram/users/delete',
        status=200,
    )
    remove_unauthorized_bot_users()
    bot_users = scope_session.query(BotUser).count()
    chats = scope_session.query(Chat).count()
    if correct_telegram and not is_dismissed:
        assert bot_users and chats
    else:
        assert not (bot_users and chats)
