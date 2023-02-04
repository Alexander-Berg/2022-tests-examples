from unittest.mock import patch

import aiohttp.pytest_plugin
import pytest

from maps_adv.stat_tasks_starter.lib import Application
from maps_adv.stat_tasks_starter.lib.config import config as app_config

from .aresponses import ResponsesMockServer

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.stat_tasks_starter.tests.clickhouse_conftest",
]
del aiohttp.pytest_plugin.loop


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
async def aresponses(loop):
    async with ResponsesMockServer(loop=loop) as server:
        yield server


_config = {
    "TASKS_TO_START": {"default": []},
    "STAT_CONTROLLER_URL": {"default": "http://example.com"},
    "RELAUNCH_INTERVAL": {"default": 2},
    "TIMEOUT": {"default": 300},
    "RETRY_MAX_ATTEMPTS": {"default": 1},
    "RETRY_WAIT_MULTIPLIER": {"default": 0.1},
    "CH_STORAGE_DB": {"default": "stat"},
    "CH_STORAGE_SOURCE_TABLE": {"default": "source_sample"},
    "CH_STORAGE_NORMALIZED_TABLE": {"default": "normalized_sample"},
    "CH_STORAGE_ACCEPTED_TABLE": {"default": "accepted_sample"},
    "CH_STORAGE_ACCEPTED_GROUPS_TABLE": {"default": "accepted_sample_event_group_ids"},
    "CH_STORAGE_HOST": {"default": "localhost"},
    "CH_STORAGE_PORT": {"default": 9001},
    "CH_STORAGE_USE_SSL": {"default": False},
    "SENTRY_DSN": {"default": None},
    "BILLING_URL": {"default": "http://somedomain.com"},
    "ADV_STORE_URL": {"default": "http://somedomain.com"},
    "COLLECTOR_MATCHING_LAG": {"default": 60},
    "WARDEN_URL": {"default": "http://somedomain.com"},
}


@pytest.fixture(autouse=True)
def config():
    with patch.dict(app_config.options_map, _config):
        app_config.init()
        yield app_config


@pytest.fixture
async def app(config):
    return Application()
