import pytest

from telebot.async_telebot import AsyncTeleBot

from maps_adv.common.helpers import coro_mock
from maps_adv.geosmb.notification_bot.server.lib import Application
from maps_adv.geosmb.notification_bot.server.lib.domain import Domain
from maps_adv.geosmb.tuner.client import TunerClient


pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.common.lasagna.pytest.plugin",
]

_config = dict(
    SOURCE_TVM_ID="source-tvm-id",
    TVM_DAEMON_URL="http://tvm.daemon",
    TVM_TOKEN="tvm-token",
    BOT_TOKEN="12345:qwerty",
    CUSTOM_RATE_LIMIT=5,
    BOT_ADMINS=["1234"],
    WEBHOOK_URL_PATH='/webhook',
    WEBHOOK_URL="https://geoadv-api.yandex.ru/webhook",
    TUNER_URL="http://tuner.server",
)


@pytest.fixture
def config():
    return _config.copy()


@pytest.fixture(autouse=True)
def bot(mocker, config):
    mocker.patch("telebot.apihelper.CUSTOM_REQUEST_SENDER", coro_mock())
    mocker.patch("telebot.async_telebot.AsyncTeleBot.delete_webhook", coro_mock())
    mocker.patch("telebot.async_telebot.AsyncTeleBot.set_webhook", coro_mock())
    mocker.patch("telebot.async_telebot.AsyncTeleBot.set_my_commands", coro_mock())
    mocker.patch("telebot.async_telebot.AsyncTeleBot.send_message", coro_mock())
    mocker.patch("telebot.async_telebot.AsyncTeleBot.reply_to", coro_mock())

    return AsyncTeleBot(
        token=config["BOT_TOKEN"],
        parse_mode="HTML"
    )


@pytest.fixture(autouse=True)
async def tuner_client(mocker, aiotvm, config):
    mocker.patch(
        "maps_adv.geosmb.tuner.client.TunerClient.update_telegram_user", coro_mock()
    )
    mocker.patch(
        "maps_adv.geosmb.tuner.client.TunerClient.delete_telegram_user", coro_mock()
    )

    async with TunerClient(
        url=config["TUNER_URL"],
        tvm=aiotvm,
        tvm_destination="tuner",
    ) as client:
        yield client


@pytest.fixture(autouse=True)
def domain(bot, config, tuner_client):
    return Domain(
        config=config,
        bot=bot,
        tuner_client=tuner_client
    )


@pytest.fixture
def app(config):
    return Application(config)
