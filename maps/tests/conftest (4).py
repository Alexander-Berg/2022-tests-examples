import asyncio
import time
from collections.abc import Generator
from concurrent.futures import ProcessPoolExecutor
from unittest import mock

import aiohttp
import aiohttp.test_utils
import alembic.command
import alembic.config
import psycopg2
import pytest

from maps_adv.adv_store.v2.lib import Application
from maps_adv.adv_store.v2.lib.core.direct_moderation import (
    Client as DirectModerationClient,
)
from smb.common.multiruntime.lib.basics import is_arcadia_python

from maps_adv.adv_store.v2.lib.core.logbroker import LogbrokerWrapper
from maps_adv.adv_store.v2.lib.db import DB
from maps_adv.adv_store.v2.lib.domains.campaigns import CampaignsDomain
from maps_adv.adv_store.v2.lib.domains.events import EventsDomain
from maps_adv.adv_store.v2.lib.domains.moderation import ModerationDomain
from maps_adv.adv_store.v2.tests import coro_mock

if is_arcadia_python:
    import aiohttp.pytest_plugin

    del aiohttp.pytest_plugin.loop


def pytest_configure(config):
    config.addinivalue_line("markers", "mock_direct_moderation_client")
    config.addinivalue_line("markers", "mock_dm")
    config.addinivalue_line("markers", "real_db")
    config.addinivalue_line("markers", "wip")


pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.adv_store.v2.tests.factory",
    "maps_adv.adv_store.v2.tests.dms",
]


_config = dict(
    DATABASE_URL="postgresql://adv_store:adv_store@localhost:5433/adv_store",
    DASHBOARD_API_URL="http://dashboard.server",
    WARDEN_URL=None,
    BILLING_API_URL="http://billing_proxy.ru",
    YT_CLUSTER="hahn",
    YT_TOKEN="yt_token",
    YT_CHANGE_LOGS_TABLE="//home/change_logs_table",
    USE_DIRECT_MODERATION=True,
    LOGBROKER_CLUSTER="sas.logbroker.yandex.net",
    LOGBROKER_PORT=2135,
    LOGBROKER_AUTH_TOKEN="",
    LOGBROKER_SOURCE_ID=b"maps_adv",
    LOGBROKER_DIRECT_MODERATION_TOPIC_OUT="/maps-adv/direct-moderation/testing/moderations",  # noqa: E501
    LOGBROKER_DIRECT_MODERATION_TOPIC_IN="/maps-adv/direct-moderation/testing/results",
    LOGBROKER_DIRECT_MODERATION_CONSUMER_ID=(
        "/maps-adv/direct-moderation/testing/process-direct-moderations-consumer"
    ),
    AVATAR_MDS_BASE_URL="https://avatars.mds.yandex.net",
    PING_V1_URL="http://127.0.0.1:33333/ping",
    # Experiments
    # TODO(megadiablo) Выкинуть флаг после завершения эксперемента
    #                  GEOADVDEV-2493
    EXPERIMENT_CALCULATE_DISPLAY_CHANCE_FROM_CPM={"default": True, "converter": bool},
)


@pytest.fixture
def config():
    return _config


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture(scope="session")
def wait_for_db():
    waited = 0

    while waited < 100:
        try:
            con = psycopg2.connect(_config["DATABASE_URL"])
        except psycopg2.OperationalError:
            time.sleep(1)
            waited += 1
        else:
            con.close()
            break
    else:
        raise ConnectionRefusedError()


def _db_upgrade(db_url):
    cfg = alembic.config.Config("alembic.ini")
    cfg.set_main_option("sqlalchemy.url", db_url)
    alembic.command.upgrade(cfg, "head")


def _db_downgrade(db_url):
    cfg = alembic.config.Config("alembic.ini")
    cfg.set_main_option("sqlalchemy.url", db_url)
    alembic.command.downgrade(cfg, "base")


@pytest.fixture(scope="session")
def migrate_db(wait_for_db):
    # alembic commands should be executed in a separate process
    # to prevent alembic overwriting logging config
    with ProcessPoolExecutor(1) as pool:
        pool.submit(_db_upgrade, _config["DATABASE_URL"]).result()
        yield
        pool.submit(_db_downgrade, _config["DATABASE_URL"]).result()


@pytest.fixture
def db(request, migrate_db):
    if request.node.get_closest_marker("real_db"):
        return request.getfixturevalue("_real_db")
    return request.getfixturevalue("_transaction_db")


@pytest.fixture
async def _transaction_db(config):
    _db = await DB.create(config["DATABASE_URL"], use_single_connection=True)

    con = await _db.acquire()
    tr = con.transaction()
    await tr.start()

    yield _db

    await tr.rollback()
    await _db.release(con, force=True)

    await _db.close()


@pytest.fixture
async def _real_db(config):
    _db = await DB.create(config["DATABASE_URL"])

    yield _db

    await _db.close()

    with ProcessPoolExecutor(1) as pool:
        await asyncio.wrap_future(pool.submit(_db_downgrade, config["DATABASE_URL"]))
        await asyncio.wrap_future(pool.submit(_db_upgrade, config["DATABASE_URL"]))


@pytest.fixture
async def con(db):
    async with db.acquire() as con:
        yield con


class TestClient(aiohttp.test_utils.TestClient):
    async def get(self, path, *args, **kwargs):
        return await self.request("GET", path, *args, **kwargs)

    async def post(self, path, *args, **kwargs):
        return await self.request("POST", path, *args, **kwargs)

    async def put(self, path, *args, **kwargs):
        return await self.request("PUT", path, *args, **kwargs)

    async def patch(self, path, *args, **kwargs):
        return await self.request("PATCH", path, *args, **kwargs)

    async def delete(self, path, *args, **kwargs):
        return await self.request("DELETE", path, *args, **kwargs)

    async def request(self, method, path, *args, **kwargs):
        expected_status = kwargs.pop("expected_status", None)
        decode_as = kwargs.pop("decode_as", None)

        if "proto" in kwargs:
            proto = kwargs.pop("proto")
            kwargs["data"] = proto.SerializeToString()

        response = await super().request(method, path, *args, **kwargs)

        if expected_status:
            assert response.status == expected_status

        if decode_as:
            raw = await response.read()
            return decode_as.FromString(raw)

        if response.headers.get("Content-Type").startswith("application/json"):
            return await response.json()
        else:
            return await response.read()


@pytest.fixture
async def campaigns_domain(
    campaigns_dm, events_dm, moderation_dm, config, billing_proxy_client
):
    return CampaignsDomain(
        campaigns_dm,
        events_dm,
        moderation_dm,
        config["DASHBOARD_API_URL"],
        billing_proxy_client,
    )


@pytest.fixture
async def events_domain(events_dm, campaigns_dm):
    return EventsDomain(events_dm, campaigns_dm)


@pytest.fixture
async def moderation_domain(
    moderation_dm, campaigns_dm, direct_moderation_client, billing_proxy_client
):
    return ModerationDomain(
        moderation_dm, campaigns_dm, direct_moderation_client, billing_proxy_client
    )


@pytest.fixture
async def api(loop, config, db, aiohttp_client, logbroker_client, billing_proxy_client):
    app = Application(config)
    api = app.setup(db, logbroker_client, billing_proxy_client)

    server = aiohttp.test_utils.TestServer(api, loop=loop)
    client = TestClient(server, loop=loop)

    await client.start_server()
    yield client
    await client.close()


@pytest.fixture
def charged_sum_mock(mocker):
    return mocker.patch(
        "maps_adv.statistics.dashboard.client.Client.campaigns_charged_sum", coro_mock()
    ).coro


@pytest.fixture
async def logbroker_client():
    class MockLogbrokerClient(LogbrokerWrapper):
        start = coro_mock()
        stop = coro_mock()

    return MockLogbrokerClient()


@pytest.fixture
def direct_moderation_client(request):
    if request.node.get_closest_marker("mock_direct_moderation_client"):
        return request.getfixturevalue("_mock_direct_moderation_client")
    return request.getfixturevalue("_direct_moderation_client")


@pytest.fixture
def _direct_moderation_client(config, logbroker_client):
    return DirectModerationClient(
        out_topic=config["LOGBROKER_DIRECT_MODERATION_TOPIC_OUT"],
        in_topic=config["LOGBROKER_DIRECT_MODERATION_TOPIC_IN"],
        consumer_id=config["LOGBROKER_DIRECT_MODERATION_CONSUMER_ID"],
        avatar_mds_base_url=config["AVATAR_MDS_BASE_URL"],
        logbroker_client=logbroker_client,
    )


@pytest.fixture
def _mock_direct_moderation_client(config, logbroker_client):
    class MockDirectModerationClient(Generator):
        def __aenter__(self):
            return self

        __aexit__ = coro_mock()

        # crazy staff to make "async with ... as" work in test
        def __await__(self):
            return self

        def send(self, ignored_arg):
            raise StopIteration(self)

        def throw(self, type=None, value=None, traceback=None):
            raise StopIteration

        send_campaign_moderation = coro_mock()
        retrieve_direct_responses = mock.MagicMock()

    return MockDirectModerationClient()


@pytest.fixture
def billing_proxy_client():
    class BillingClientMock:
        __aenter__ = coro_mock()
        __aexit__ = coro_mock()
        fetch_order = coro_mock()
        calculate_product_cpm = coro_mock()

    mock = BillingClientMock()
    mock.__aenter__.coro.side_effect = lambda: mock
    mock.__aexit__.coro.side_effect = lambda *args, **kwargs: None

    return mock
