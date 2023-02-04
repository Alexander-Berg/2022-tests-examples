from unittest.mock import AsyncMock

import pytest

from maps_adv.geosmb.marksman.server.lib import Application
from maps_adv.geosmb.marksman.server.lib.data_manager import (
    BaseDataManager,
    DataManager,
)
from maps_adv.geosmb.marksman.server.lib.db import DB
from maps_adv.geosmb.marksman.server.lib.domain import Domain

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.pgswim.pytest.plugin",
    "maps_adv.common.lasagna.pytest.plugin",
    "maps_adv.geosmb.clients.bvm.pytest.plugin",
    "maps_adv.geosmb.clients.cdp.pytest.plugin",
    "maps_adv.geosmb.clients.geosearch.pytest.plugin",
    "maps_adv.geosmb.marksman.server.tests.factory",
]

_config = dict(
    DATABASE_URL="postgresql://marksman:marksman@localhost:5433/marksman?master_as_replica=true",
    TVM_DAEMON_URL="http://tvm.daemon",
    TVM_TOKEN="tvm-token",
    BVM_URL="http://bvm.url",
    GEOSEARCH_URL="http://geosearch.url",
    CDP_URL="http://cdp.url",
    YQL_TOKEN="some_token",
    YT_CLUSTER="hahn",
    DOORMAN_CLIENTS_TABLE="//path/to/clients",
    PROMOTER_LEADS_TABLE="//path/to/leads",
    WARDEN_URL=None,
    WARDEN_TASKS=[],
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
        add_business = AsyncMock()
        list_business_segments_data = AsyncMock()
        add_business_segment = AsyncMock()
        list_biz_ids = AsyncMock()
        update_segments_sizes = AsyncMock()

    return MockDM()


@pytest.fixture
def domain(dm, bvm, geosearch, cdp):
    return Domain(dm=dm, bvm=bvm, geosearch=geosearch, cdp=cdp)


@pytest.fixture
def app(config):
    return Application(config)


@pytest.fixture(autouse=True)
def geosearch_metrika_counter(geosearch):
    geosearch.resolve_org.coro.return_value.metrika_counter = "444"
