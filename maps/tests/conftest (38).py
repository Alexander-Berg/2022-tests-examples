import pytest
from smb.common.multiruntime.lib.basics import is_arcadia_python

from maps_adv.common.helpers import coro_mock
from maps_adv.geosmb.doorman.server.lib import Application
from maps_adv.geosmb.doorman.server.lib.data_managers import (
    BaseDataManager,
    DataManager,
)
from maps_adv.geosmb.doorman.server.lib.db import DB
from maps_adv.geosmb.doorman.server.lib.domain import Domain
from maps_adv.geosmb.doorman.server.tests.shared_mock import SharedCallableMockManager

if is_arcadia_python:
    import aiohttp.pytest_plugin

    del aiohttp.pytest_plugin.loop

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.pgswim.pytest.plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.common.shared_mock.pytest.plugin",
    "maps_adv.geosmb.clients.bvm.pytest.plugin",
    "maps_adv.geosmb.doorman.server.tests.factory",
]


_config = dict(
    DATABASE_URL="postgresql://doorman:doorman@localhost:5433/doorman"
    "?master_as_replica=true",
    YT_CLUSTER="fake_cluster",
    CLIENT_YT_EXPORT_TABLE="yt-export-table",
    CLIENT_YT_EXPORT_TOKEN="yt-export-token",
    CALLS_STAT_YT_TABLE="//fake/calls-stat",
    CALLS_TRACKING_YT_TABLE="//fake/calls-tracking",
    ADVERT_ORGS_YT_TABLE="//fake/advert-orgs",
    ADVERT_YT_TABLE="//fake/advert",
    CAMPAIGN_ORGANIZATION_YT_TABLE="//fake/campaign-org",
    CAMPAIGN_YT_TABLE="//fake/campaign",
    YQL_TOKEN="yql-token",
    BVM_URL="http://bvm-server",
)


@pytest.fixture
def config(request):
    __config = _config.copy()

    config_mark = request.node.get_closest_marker("config")
    if config_mark:
        __config.update(config_mark.kwargs)

    return __config


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB


@pytest.fixture
def dm(request):
    dm_fixture_name = (
        "_mock_dm" if request.node.get_closest_marker("mock_dm") else "_real_dm"
    )
    return request.getfixturevalue(dm_fixture_name)


@pytest.fixture
def _mock_dm():
    class MockDataManager(BaseDataManager):
        create_client = coro_mock()
        create_clients = coro_mock()
        find_clients = coro_mock()
        retrieve_client = coro_mock()
        client_exists = coro_mock()
        list_clients = coro_mock()
        list_suggest_clients = coro_mock()
        iter_clients_for_export = coro_mock()
        list_segments = coro_mock()
        list_contacts = coro_mock()
        segment_statistics = coro_mock()
        update_client = coro_mock()
        merge_client = coro_mock()
        add_order_event = coro_mock()
        add_call_event = coro_mock()
        list_clients_by_segment = coro_mock()
        list_client_segments = coro_mock()
        list_client_events = coro_mock()
        fetch_max_geoproduct_id_for_call_events = coro_mock()
        create_table_import_call_events_tmp = coro_mock()
        store_imported_call_events = coro_mock()
        upload_imported_call_events = coro_mock()
        clear_clients_by_passport = coro_mock()
        check_clients_existence_by_passport = coro_mock()

    return MockDataManager()


@pytest.fixture
def _real_dm(db):
    return DataManager(db)


@pytest.fixture
def domain(dm, bvm):
    return Domain(dm=dm, bvm_client=bvm)


@pytest.fixture
def mock_yt(mocker, shared_proxy_mp_manager):
    methods = "remove", "create", "exists", "write_table", "Transaction"
    return {
        method: mocker.patch(
            f"yt.wrapper.YtClient.{method}", shared_proxy_mp_manager.SharedMock()
        )
        for method in methods
    }


@pytest.fixture
def mp_mock_manager():
    with SharedCallableMockManager() as manager:
        yield manager


@pytest.fixture(autouse=True)
def mock_yql(mocker, mp_mock_manager):
    table_get_iterator = mp_mock_manager.SharedCallableMock()
    table_get_iterator.return_value = []

    results_table = mp_mock_manager.YqlResultTableMock()
    results_table.get_iterator = table_get_iterator

    request_results = mp_mock_manager.YqlRequestResultsMock()
    request_results.status = mp_mock_manager.SharedCallableMock()
    request_results.text = mp_mock_manager.SharedCallableMock()
    request_results.table = results_table

    request = mp_mock_manager.YqlRequestMock()
    request.run = mp_mock_manager.SharedCallableMock()
    request.get_results = request_results

    query = mp_mock_manager.SharedCallableMock(return_value=request)

    return {
        "query": mocker.patch("yql.api.v1.client.YqlClient.query", query),
        "request_run": request.run,
        "request_get_results": request_results,
        "table_get_iterator": table_get_iterator,
    }
