import logging
import time
from datetime import timedelta
from decimal import Decimal
from operator import methodcaller
from unittest.mock import Mock

import clickhouse_driver
import pytest

from smb.common.multiruntime import lib as multiruntime

from maps_adv.common.helpers import coro_mock
from maps_adv.statistics.beekeeper.lib import Application

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.statistics.beekeeper.tests.factory",
]

_config = {
    "CH_STORAGE_PASSWORD": None,
    "CH_STAT_DATABASE_URL": "clickhouse://@localhost:9001/?secure=False",
    "CH_MAX_MEMORY_USAGE": 1234567890,
    "CH_SYNC_REQUEST_TIMEOUT": 60,
    "CH_NORMALIZER_QUERY_ID": "maps_adv_beekeeper_normalizer_query",
    "CH_PROCESSOR_QUERY_ID": "maps_adv_beekeeper_processor_query",
    "ADV_STORE_URL": "http://adv_srore.server",
    "BILLING_URL": "http://billing.server",
    "TIME_THRESHOLD_FREE_EVENTS": timedelta(seconds=45),
    "LAG_PACKET_SIZE": timedelta(seconds=90),
    "DEDUPLICATION_WINDOW": timedelta(seconds=240),
    "MIN_PACKET_SIZE": timedelta(seconds=180),
    "MAX_PACKET_SIZE": timedelta(seconds=360),
    "MONKEY_PATCH_CAMPAIGNS_FOR_BEEKEEPER": None,  # GEOPROD-4108
    "MAPKIT_SOURCE_APP_FILTER": {"ios_maps_build": 201},
    "MAPKIT_NORMALIZER_TABLE_NAME": "kek-table-name",
    "MAPKIT_RECOGNISED_APPS": {
        "ru.yandex.yandexnavi": "NAVIGATOR",
        "ru.yandex.yandexmaps": "MOBILE_MAPS",
        "ru.yandex.mobile.navigator": "NAVIGATOR",
        "ru.yandex.traffic": "MOBILE_MAPS",
    },
    "EXPERIMENTAL_CHARGE_FIX_CAMPAIGNS": True,
}


def pytest_configure(config):
    config.addinivalue_line("markers", "mapkit")


@pytest.fixture()
def config():
    return _config.copy()


@pytest.fixture()
def ch_config(config):
    return {
        "host": "localhost",
        "port": 9001,
        "sync_request_timeout": config["CH_SYNC_REQUEST_TIMEOUT"],
    }


@pytest.fixture(scope="session")
def ch_client():
    return clickhouse_driver.Client(
        host="localhost",
        port=9001,
        sync_request_timeout=_config["CH_SYNC_REQUEST_TIMEOUT"],
    )


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture
def db():
    pass


@pytest.fixture(scope="session")
def clickhouse_fixture_sqls():
    fixture_names = [
        "aggregated_processed_events_by_campaigns_and_days.sql",
        "maps_adv_statistics_raw_metrika_log.sql",
        "mapkit_events.sql",
        "normalized_events.sql",
        "processed_events.sql",
        "processed_event_group_ids.sql",
    ]
    sql_fixtures = []

    for fixture_name in fixture_names:
        text = multiruntime.io.read_file(f"tests/fixtures/{fixture_name}")
        setup, teardown = map(
            methodcaller("strip"), text.split("-- BACKWARD --", maxsplit=1)
        )
        sql_fixtures.append(
            {
                "setup": list(map(methodcaller("strip"), setup.split(";\n"))),
                "teardown": list(map(methodcaller("strip"), teardown.split(";\n"))),
            }
        )

    return sql_fixtures


@pytest.fixture(autouse=True)
def setup_clickhouses(wait_for_chs, ch_client, clickhouse_fixture_sqls):
    for fixture in clickhouse_fixture_sqls:
        for sql in fixture["setup"]:
            ch_client.execute(sql)

    yield

    for fixture in reversed(clickhouse_fixture_sqls):
        for sql in fixture["teardown"]:
            ch_client.execute(sql)


@pytest.fixture(scope="session")
def wait_for_chs(ch_client):
    waited = 0

    while waited < 100:
        try:
            ch_client.connection.connect()
        except clickhouse_driver.errors.NetworkError:
            time.sleep(1)
            waited += 1
        else:
            ch_client.disconnect()
            break
    else:
        ConnectionRefusedError()


@pytest.fixture
def warden_client_mock():
    class WardenClientMock:
        executor_id = "someuuidhere"
        update_status = coro_mock()

    return WardenClientMock()


@pytest.fixture
def caplog_set_level_error(caplog):
    caplog.set_level(logging.ERROR)


@pytest.fixture
def adv_store_client_mock():
    class AdvStoreClientMock:
        __aenter__ = coro_mock()
        __aexit__ = coro_mock()
        list_active_cpm_campaigns = coro_mock()
        list_active_cpa_campaigns = coro_mock()
        list_active_fix_campaigns = coro_mock()
        stop_campaigns = coro_mock()
        update_paid_till = coro_mock()

    mock = AdvStoreClientMock()
    mock.__aenter__.coro.side_effect = lambda: mock
    mock.__aexit__.coro.side_effect = lambda *args, **kwargs: None
    mock.list_active_cpm_campaigns.coro.return_value = [
        {
            "campaign_id": 11,
            "order_id": 101,
            "cost": Decimal("1.1"),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "timezone": "UTC",
        }
    ]
    mock.list_active_cpa_campaigns.coro.return_value = [
        {
            "campaign_id": 22,
            "order_id": 202,
            "cost": Decimal("1.1"),
            "budget": Decimal("31"),
            "daily_budget": Decimal("21"),
            "timezone": "UTC",
            "paid_events_names": [
                "ACTION_CALL",
                "ACTION_MAKE_ROUTE",
                "ACTION_SEARCH",
                "ACTION_OPEN_SITE",
                "ACTION_OPEN_APP",
                "ACTION_SAVE_OFFER",
            ],
        }
    ]

    return mock


@pytest.fixture
def billing_client_mock():
    class BillingClientMock:
        __aenter__ = coro_mock()
        __aexit__ = coro_mock()
        fetch_orders_balance = coro_mock()
        fetch_orders_discounts = coro_mock()
        fetch_orders_debits = coro_mock()
        submit_orders_charges = coro_mock()

    mock = BillingClientMock()
    mock.__aenter__.coro.side_effect = lambda: mock
    mock.__aexit__.coro.side_effect = lambda *args, **kwargs: None
    mock.fetch_orders_balance.coro.return_value = {
        101: Decimal("100"),
        202: Decimal("100"),
    }
    mock.fetch_orders_discounts.coro.return_value = {
        101: Decimal("1.0"),
        202: Decimal("1.0"),
    }
    mock.submit_orders_charges.coro.return_value = (False, {101: True})

    return mock


@pytest.fixture
def warden_context_mock():
    context_mock = Mock()
    context_mock.client = Mock()
    context_mock.client.executor_id = "executor_id_uuid"

    return context_mock
