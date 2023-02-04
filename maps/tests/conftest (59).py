import asyncio

import pytest

from smb.common.multiruntime.lib.basics import is_arcadia_python

from maps_adv.common.helpers import coro_mock
from maps_adv.common.helpers.enums import CampaignTypeEnum
from maps_adv.statistics.dashboard.server.lib import Application
from maps_adv.statistics.dashboard.server.lib.api_provider import ApiProvider
from maps_adv.statistics.dashboard.server.lib.ch_query_log import ClickHouseQueryLog
from maps_adv.statistics.dashboard.server.lib.data_manager import (
    AbstractDataManager,
    DataManager,
)
from maps_adv.statistics.dashboard.server.lib.db import DB
from maps_adv.statistics.dashboard.server.lib.domain import Domain

if is_arcadia_python:
    import aiohttp.pytest_plugin

    del aiohttp.pytest_plugin.loop

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.common.pgswim.pytest_plugin_deprecated",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.statistics.dashboard.server.tests.clickhouse_conftest",
    "maps_adv.statistics.dashboard.server.tests.factory",
]


_config = {
    "CH_STORAGE_PASSWORD": "",
    "CH_STORAGE_TABLE": "accepted_sample",
    "CH_STORAGE_AGGREGATED_TABLE": "aggregated_sample",
    "CH_STAT_DATABASE_URL": "clickhouse://default@localhost:9001/stat?secure=False",
    "CH_SYSTEM_DATABASE_URL": "clickhouse://default:@localhost:9001/sys?secure=False",
    "SSL_CERT_FILE": None,
    "DATABASE_URL": (
        "postgresql://dashboard:dashboard@localhost:5433/dashboard"
        "?master_as_replica=true"
    ),
    "YQL_TOKEN": "",
    "YQL_CLUSTER": "cluster",
    "YQL_CATEGORY_SEARCH_REPORT_TABLE": "yt/path/to/category/search/report/table",
    "YT_POOL": "",
    "WARDEN_URL": None,
    "MONKEY_PATCH_CAMPAIGNS_ONLY_FOR_V2": [],
    "MONKEY_PATCH_ALL_CAMPAIGNS_USE_V2": False,
    "ADV_STORE_URL": "",
    "JUGGLER_EVENTS_URL": "http://localhost:31579",
    "NANNY_SERVICE_ID": "",
}


def pytest_configure(config):
    config.addinivalue_line("markers", "mock_adv_store_client")
    config.addinivalue_line("markers", "mock_ch_query_log")
    config.addinivalue_line("markers", "mock_dm")
    config.addinivalue_line("markers", "mock_domain")
    config.addinivalue_line("markers", "no_setup_ch")


@pytest.fixture(scope="session")
def event_loop(request):
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture
def config():
    return _config.copy()


@pytest.fixture()
def ch_config(config):
    return {
        "hosts": [
            {"host": "localhost", "port": 9001},
            {"host": "localhost", "port": 9002},
        ],
        "database": "stat",
    }


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB


@pytest.fixture
def dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_dm")
    return request.getfixturevalue("_dm")


@pytest.fixture
def _dm(db, config, ch_config):
    return DataManager(
        ch_config=ch_config,
        table=config["CH_STORAGE_TABLE"],
        aggregated_table=config["CH_STORAGE_AGGREGATED_TABLE"],
        postgres_db=db,
        yql_config=dict(
            token=config["YQL_TOKEN"],
            cluster=config["YQL_CLUSTER"],
            category_search_report_table=config["YQL_CATEGORY_SEARCH_REPORT_TABLE"],
            yt_pool=config["YT_POOL"],
        ),
        campaigns_only_for_v2=config["MONKEY_PATCH_CAMPAIGNS_ONLY_FOR_V2"],
    )


@pytest.fixture
def _mock_dm():
    class MockDM(AbstractDataManager):
        calculate_by_campaigns_and_period = coro_mock()
        calculate_campaigns_charged_sum = coro_mock()
        fetch_search_icons_statistics = coro_mock()
        calculate_metrics = coro_mock()
        retrieve_tables_metrics = coro_mock()
        get_campaign_ids_for_period = coro_mock()
        sync_category_search_reports = coro_mock()
        calculate_campaigns_displays_for_period = coro_mock()
        calculate_campaigns_events_for_period = coro_mock()
        get_aggregated_normalized_events_by_campaign = coro_mock()
        get_aggregated_processed_events_by_campaign = coro_mock()
        get_aggregated_mapkit_events_by_campaign = coro_mock()

    return MockDM()


@pytest.fixture
def domain(request):
    if request.node.get_closest_marker("mock_domain"):
        return request.getfixturevalue("_mock_domain")
    return request.getfixturevalue("_domain")


@pytest.fixture
def _domain(dm, adv_store_client, juggler_client_mock):
    return Domain(dm, adv_store_client, juggler_client_mock)


@pytest.fixture
def _mock_domain():
    class MockDomain:
        calculate_monitoring_data = coro_mock()
        check_not_spending_budget = coro_mock()

    return MockDomain()


@pytest.fixture
def api_provider(dm, domain, ch_query_log):
    return ApiProvider(dm, domain, ch_query_log)


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture
def adv_store_client(request):
    if request.node.get_closest_marker("real_adv_store_client"):
        return request.getfixturevalue("_adv_store_client")
    return request.getfixturevalue("_mock_adv_store_client")


@pytest.fixture
def _mock_adv_store_client():
    class AdvStoreClientMock:
        __aenter__ = coro_mock()
        __aexit__ = coro_mock()
        retrieve_campaign_data_for_monitorings = coro_mock()
        list_campaigns_for_budget_analysis = coro_mock()
        create_campaign_not_spending_budget_events = coro_mock()

    mock = AdvStoreClientMock()
    mock.__aenter__.coro.side_effect = lambda: mock
    mock.__aexit__.coro.side_effect = lambda *args, **kwargs: None
    mock.retrieve_campaign_data_for_monitorings.coro.return_value = [
        {"id": 111, "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER},
        {"id": 222, "campaign_type": CampaignTypeEnum.PIN_ON_ROUTE},
        {"id": 333, "campaign_type": CampaignTypeEnum.BILLBOARD},
        {"id": 444, "campaign_type": CampaignTypeEnum.OVERVIEW_BANNER},
        {"id": 555, "campaign_type": CampaignTypeEnum.ROUTE_BANNER},
    ]

    yield mock


@pytest.fixture
def juggler_client_mock():
    class JugglerClientMock:
        __aenter__ = coro_mock()
        __aexit__ = coro_mock()
        __call__ = coro_mock()

    return JugglerClientMock()


@pytest.fixture
def warden_client_mock():
    class WardenClientMock:
        executor_id = "someuuidhere"
        update_status = coro_mock()

    return WardenClientMock()


@pytest.fixture
def ch_query_log(request, config):
    if request.node.get_closest_marker("mock_ch_query_log"):
        return request.getfixturevalue("_mock_ch_query_log")
    return request.getfixturevalue("_ch_query_log")


@pytest.fixture
def _mock_ch_query_log(config):
    class MockClickHouseQueryLog:
        retrieve_metrics_for_queries = coro_mock()

    return MockClickHouseQueryLog()


@pytest.fixture
def _ch_query_log(config):
    return ClickHouseQueryLog(
        database_url=config["CH_SYSTEM_DATABASE_URL"],
        ssl_cert_file=config["SSL_CERT_FILE"],
    )
