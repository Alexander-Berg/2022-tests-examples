import factory
import pytest

from watcher.db import BotUser, Chat
from .base import BOT_USER_SEQUENCE, CHAT_SEQUENCE
from watcher import enums

@pytest.fixture(scope='function')
def bot_user_factory(meta_base, staff_factory):
    class BotUserFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = BotUser

        id = factory.Sequence(lambda n: n + BOT_USER_SEQUENCE)
        staff = factory.SubFactory(staff_factory)
        username = factory.Sequence(lambda n: f'bot_user_{n}')
        telegram_id = factory.Sequence(lambda n: n + BOT_USER_SEQUENCE + 500)

    return BotUserFactory


@pytest.fixture(scope='function')
def chat_factory(meta_base, bot_user_factory):
    class ChatFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Chat

        id = factory.Sequence(lambda n: n + CHAT_SEQUENCE)
        author = factory.SubFactory(bot_user_factory)
        title = factory.Sequence(lambda n: f'chat_title_{n}')
        chat_type = enums.ChatType.private

    return ChatFactory
