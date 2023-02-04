import pytest

from maps_adv.common.helpers import coro_mock
from maps_adv.geosmb.tuner.server.lib import Application
from maps_adv.geosmb.tuner.server.lib.data_manager import BaseDataManager, DataManager
from maps_adv.geosmb.tuner.server.lib.db import DB
from maps_adv.geosmb.tuner.server.lib.domain import Domain

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.pgswim.pytest.plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.geosmb.tuner.server.tests.factory",
]

_config = dict(
    DATABASE_URL="postgresql://tuner:tuner@localhost:5433/tuner?master_as_replica=true",
)


@pytest.fixture
def config():
    return _config.copy()


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB


@pytest.fixture
def dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_dm")
    return request.getfixturevalue("_dm")


@pytest.fixture
def _dm(config, db):
    return DataManager(db=db)


@pytest.fixture
def _mock_dm():
    class MockDM(BaseDataManager):
        create_default_settings = coro_mock()
        fetch_settings = coro_mock()
        fetch_general_schedule_id_setting = coro_mock()
        update_settings = coro_mock()
        check_settings_exist = coro_mock()
        create_default_settings_v2 = coro_mock()
        fetch_settings_v2 = coro_mock()
        update_settings_v2 = coro_mock()
        fetch_permissions = coro_mock()
        check_permission = coro_mock()
        update_permission = coro_mock()
        fetch_telegram_users = coro_mock()
        check_telegram_user = coro_mock()
        update_telegram_user = coro_mock()
        delete_telegram_user = coro_mock()

    return MockDM()


@pytest.fixture
def domain(dm, config):
    return Domain(dm=dm)


@pytest.fixture
def app(config):
    return Application(config)
