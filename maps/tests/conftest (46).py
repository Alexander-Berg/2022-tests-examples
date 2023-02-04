import pytest
from smb.common.multiruntime.lib.basics import is_arcadia_python

from maps_adv.common.helpers import coro_mock
from maps_adv.geosmb.promoter.server.lib import Application
from maps_adv.geosmb.promoter.server.lib.data_manager import (
    BaseDataManager,
    DataManager,
)
from maps_adv.geosmb.promoter.server.lib.db import DB

if is_arcadia_python:
    import aiohttp.pytest_plugin

    del aiohttp.pytest_plugin.loop

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.pgswim.pytest.plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.common.shared_mock.pytest.plugin",
    "maps_adv.geosmb.promoter.server.tests.factory",
]


_config = dict(
    DATABASE_URL=(
        "postgresql://promoter:promoter@localhost:5433/promoter"
        "?master_as_replica=true"
    ),
    EVENTS_DIR_YT_IMPORT_TABLE="//path/to/events-dir",
    LEADS_YT_EXPORT_TABLE="//path/to/table",
    WARDEN_TASKS=[],
    WARDEN_URL=None,
    YQL_TOKEN="yql-token",
    YT_CLUSTER="hahn",
    YT_TOKEN="fake_token",
)


@pytest.fixture
def config(request):
    __config = _config.copy()

    config_mark = request.node.get_closest_marker("config")
    if config_mark:
        __config.update(config_mark.kwargs)

    return __config


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB


@pytest.fixture
def dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_dm")
    return request.getfixturevalue("_dm")


@pytest.fixture
def _dm(db):
    return DataManager(db)


@pytest.fixture
def _mock_dm():
    class MockDM(BaseDataManager):
        list_leads = coro_mock()
        retrieve_lead = coro_mock()
        iter_leads_for_export = coro_mock()
        list_segments = coro_mock()
        list_lead_events = coro_mock()
        import_events_from_generator = coro_mock()
        list_lead_segments = coro_mock()
        check_leads_existence_by_passport = coro_mock()
        delete_leads_data_by_passport = coro_mock()

    return MockDM()


@pytest.fixture
def app(config):
    return Application(config)
