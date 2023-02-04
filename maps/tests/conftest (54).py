import pytest

from maps_adv.config_loader import Config
from maps_adv.stat_controller.client.lib.charger import Client as ChargerClient
from maps_adv.stat_controller.client.lib.collector import Client as CollectorClient
from maps_adv.stat_controller.client.lib.normalizer import Client as NormalizerClient

from .aresponses import ResponsesMockServer


@pytest.fixture
async def aresponses(event_loop):
    async with ResponsesMockServer(loop=event_loop) as server:
        yield server


@pytest.fixture
async def rmock(aresponses):
    return lambda *a: aresponses.add("example.com", *a)


@pytest.fixture(autouse=True)
def config():
    config = Config(
        {
            "RETRY_MAX_ATTEMPTS": {"default": 1},
            "RETRY_WAIT_MULTIPLIER": {"default": 0.1},
        }
    )
    config.init()
    return config


async def _make_client(client_cls, config):
    client = client_cls(
        "https://example.com/",
        retry_settings={
            "max_attempts": config.RETRY_MAX_ATTEMPTS,
            "wait_multiplier": config.RETRY_WAIT_MULTIPLIER,
        },
    )

    yield client

    await client.close()


@pytest.fixture
async def normalizer_client(config):
    async for client in _make_client(NormalizerClient, config):
        yield client


@pytest.fixture
async def collector_client(config):
    async for client in _make_client(CollectorClient, config):
        yield client


@pytest.fixture
async def charger_client(config):
    async for client in _make_client(ChargerClient, config):
        yield client
