import pytest
from smb.common.testing_utils import dt

from maps_adv.common.helpers import coro_mock
from maps_adv.geosmb.cleaner.server.lib import Application
from maps_adv.geosmb.cleaner.server.lib.data_manager import BaseDataManager, DataManager
from maps_adv.geosmb.cleaner.server.lib.db import DB
from maps_adv.geosmb.cleaner.server.lib.domain import Domain

from .factory import Factory

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.pgswim.pytest.plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.geosmb.doorman.client.pytest.plugin",
]

_config = dict(
    DATABASE_URL="postgresql://cleaner:cleaner@localhost:5433/cleaner",
    TVM_DAEMON_URL="http://tvm.daemon",
    TVM_TOKEN="tvm-token",
    DOORMAN_URL="http://doorman.server",
)


@pytest.fixture
def config():
    return _config.copy()


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB


@pytest.fixture
def aiotvm(aiotvm):
    aiotvm.fetch_user_uid.return_value = 123

    return aiotvm


@pytest.fixture
def dm(request):
    dm_fixture_name = (
        "_mock_dm" if request.node.get_closest_marker("mock_dm") else "_real_dm"
    )

    return request.getfixturevalue(dm_fixture_name)


@pytest.fixture
def _mock_dm(db):
    class MockDm(BaseDataManager):
        register_client_for_delete = coro_mock()
        list_not_processed_requests = coro_mock()
        mark_request_as_processed = coro_mock()
        retrieve_last_request = coro_mock()
        list_processed_services = coro_mock()
        create_operation = coro_mock()
        retrieve_dt_of_last_processed_request = coro_mock()

    _dm = MockDm()
    _dm.list_not_processed_requests.coro.return_value = [
        {"id": 11, "passport_uid": 111}
    ]
    _dm.retrieve_last_request.coro.return_value = {
        "id": 11,
        "processed_at": None,
    }
    _dm.list_processed_services.coro.return_value = set()
    _dm.retrieve_dt_of_last_processed_request.return_value = dt("2020-01-01 00:00:00")

    return _dm


@pytest.fixture
def _real_dm(db):
    return DataManager(db)


@pytest.fixture
def domain(dm, aiotvm, doorman):
    return Domain(
        dm=dm,
        tvm_client=aiotvm,
        doorman_client=doorman,
    )


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture
def factory(con):
    return Factory(con)
