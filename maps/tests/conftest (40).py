import asyncio
import shutil

import freezegun
import motor.motor_asyncio as motor_io
import pytest
from smb.common.multiruntime.lib.io import setup_filesystem

from maps_adv.common.helpers import AsyncContextManagerMock, coro_mock
from maps_adv.common.mds import MDSClient, MDSInstallation
from maps_adv.geosmb.harmonist.server.lib import Application
from maps_adv.geosmb.harmonist.server.lib.data_manager import (
    BaseDataManager,
    DataManager,
)
from maps_adv.geosmb.harmonist.server.lib.domain import Domain
from maps_adv.geosmb.harmonist.server.lib.engine import codec_options

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.geosmb.doorman.client.pytest.plugin",
    "maps_adv.geosmb.harmonist.server.tests.factory",
]

freezegun.configure(extend_ignore_list=["asyncio", "concurrent"])

_config = dict(
    TVM_DAEMON_URL="http://tvm.daemon",
    TVM_TOKEN="tvm-token",
    DATABASE_URL="mongodb://harmonist:harmonist@localhost:27017/?authSource=admin",
    DATABASE_NAME="harmonist",
    MDS_INSTALLATION="debug",
    MDS_STORE_NAMESPACE="mds_store",
    REDIS_URL="redis://localhost:6379",
    DOORMAN_URL="http://doorman.service",
    WARDEN_URL=None,
    WARDEN_TASKS=[],
)


class UploadFileResultMock:
    def __init__(self, download_link: str):
        self.download_link = download_link


@pytest.fixture(autouse=True)
async def mds(mocker, aiotvm):
    mocker.patch.dict(
        "maps_adv.common.mds.mds_installations",
        {
            "debug": MDSInstallation(
                outer_read_url="http://mds-outer-read.server",
                inner_read_url="http://mds-inner-read.server",
                write_url="http://mds-write.server",
            )
        },
    )

    upload_file = mocker.patch("maps_adv.common.mds.MDSClient.upload_file", coro_mock())
    upload_file.coro.return_value = UploadFileResultMock(
        download_link="http://mds-inner-read.server/get-business/603/errors_file.csv?disposition=1"
    )

    async with MDSClient(
        installation="testing",
        namespace="business",
        tvm_client=aiotvm,
        tvm_destination="mds",
    ) as client:
        yield client


@pytest.fixture
def config():
    return _config.copy()


@pytest.fixture(autouse=True)
async def motor_client(config):
    client = motor_io.AsyncIOMotorClient(config["DATABASE_URL"])

    yield client

    await client.drop_database(config["DATABASE_NAME"])


@pytest.fixture
def motor_db(config, motor_client):
    return motor_client.get_database(
        name=config["DATABASE_NAME"], codec_options=codec_options
    )


@pytest.fixture
def lock_manager():
    class LockManagerMock:
        try_lock_creation_entry = AsyncContextManagerMock()

    return LockManagerMock()


@pytest.fixture
def dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_dm")
    return request.getfixturevalue("_dm")


@pytest.fixture
def _mock_dm():
    class MockDM(BaseDataManager):
        submit_data = coro_mock()
        show_preview = coro_mock()
        submit_markup = coro_mock()
        fetch_clients_creation_log = coro_mock()
        update_creation_log = coro_mock()
        submit_validated_clients = coro_mock()
        submit_error_file = coro_mock()
        update_log_status = coro_mock()
        submit_import_result = coro_mock()
        list_unvalidated_creation_entries = coro_mock()
        list_unimported_creation_entries = coro_mock()

    return MockDM()


@pytest.fixture
def _dm(config, motor_client):
    return DataManager(motor_client=motor_client, db_name=config["DATABASE_NAME"])


@pytest.fixture
def domain(config, dm, mds, doorman, lock_manager):
    return Domain(dm, mds_client=mds, doorman_client=doorman, lock_manager=lock_manager)


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture
def setup_xlsx_fixture_files():
    setup_filesystem("maps_adv/geosmb/harmonist/server/tests", "tests")
    yield
    shutil.rmtree("tests/fixtures")


@pytest.fixture
async def wait_for_background_tasks_to_finish(event_loop):
    """This lets to get rid of useless warnings in logs"""

    yield

    await asyncio.sleep(0.5)
