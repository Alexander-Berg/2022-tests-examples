import aiohttp.pytest_plugin
import pytest
import pytz
import random
import tvmauth

from aiohttp.pytest_plugin import TestServer
from aiohttp.test_utils import TestClient
from datetime import datetime
from faker import Faker
from faker.providers import BaseProvider
from tvmauth.mock import MockedTvmClient
from unittest.mock import Mock, MagicMock

from maps_adv.billing_proxy.lib import Application
from maps_adv.billing_proxy.lib.core.async_xmlrpc_client import XmlRpcClient
from maps_adv.billing_proxy.lib.db.engine import DB
from maps_adv.billing_proxy.lib.db.enums import BillingType
from maps_adv.billing_proxy.proto.common_pb2 import Error
from maps_adv.common.geoproduct import GeoproductClient

from .factory import Factory
from .helpers import coro_mock

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.aiotvm.pytest.plugin",
    "maps_adv.common.pgswim.pytest_plugin_deprecated",
    "maps_adv.billing_proxy.tests.dms",
    "maps_adv.billing_proxy.tests.domains",
]
del aiohttp.pytest_plugin.loop  # DEVTOOLS-5496


def pytest_configure(config):
    config.addinivalue_line(
        "addopts", "--database-url=postgresql://billing:billing@localhost:13000/billing"
    )
    config.addinivalue_line("markers", "config")
    config.addinivalue_line("markers", "geoproduct_product")
    config.addinivalue_line("markers", "mock_dm")
    config.addinivalue_line("markers", "vip")


@pytest.fixture
def loop(event_loop):
    return event_loop


_config = {
    "DATABASE_URL": "postgresql://billing:billing@localhost:13000/billing",
    "BALANCE_XMLRPC_API_HOST": "billing-xmlrpc.localhost",
    "BALANCE_XMLRPC_API_PORT": 80,
    "BALANCE_XMLRPC_API_PATH": "xmlrpc",
    "BALANCE_SERVICE_ID": 100,
    "BALANCE_SERVICE_TOKEN": "service_token",
    "BALANCE_OPERATOR_UID": 111,
    "GEOPRODUCT_DEFAULT_UID": 222,
    "GEOPRODUCT_URL": "geoproduct.localhost",
    "SKIP_BALANCE_API_CALL_ON_ORDERS_CHARGE": False,
    "TVMTOOL_PORT": 1,
    "TVMTOOL_LOCAL_AUTHTOKEN": "TVM_TOKEN",
    "WARDEN_URL": None,
    "USE_RECALCULATE_STATISTIC_MODE": False,
    "GEOPROD_SERVICE_ID": 200,
    "YT_CLUSTER": "hahn",
    "YT_TOKEN": "token",
    "YT_RECOINCILIATION_REPORT_DIR": "//home/geoprod/billing",
    "SEASONAL_COEFS_SINCE": pytz.timezone("Europe/Moscow").localize(
        datetime(year=2000, month=1, day=1, hour=0, minute=0, second=0)
    ),
}


class EnumFakerProvider(BaseProvider):
    @staticmethod
    def enum(enum_class):
        return random.choice(list(enum_class))


@pytest.fixture(scope="session")
def faker():
    fake = Faker("ru_RU")
    fake.add_provider(EnumFakerProvider)
    return fake


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
async def app(mocker, db, config, balance_client_mock_class, tvmclient):
    mocker.patch(
        "maps_adv.billing_proxy.lib.application.BalanceClient",
        balance_client_mock_class,
    )
    mocker.patch.object(tvmauth.TvmClient, "__new__", Mock(return_value=tvmclient))

    app = Application(config)
    await app.setup(db)
    return app


@pytest.fixture
async def api_client(app, aiohttp_client):
    return await aiohttp_client(app.api)


class ProtoTestClient(TestClient):
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

    async def request(
        self,
        method,
        path,
        pb=None,
        allowed_status_codes=None,
        decode_as=None,
        expected_error=None,
        **kwargs,
    ):
        if "data" in kwargs:
            raise Exception("Cannot use pb_message and data arguments together")

        if pb is not None:
            kwargs["data"] = pb.SerializeToString()

        response = await super().request(method, path, **kwargs)

        if allowed_status_codes is not None:
            assert response.status in allowed_status_codes

        if expected_error is not None:
            body = await response.read()
            error = Error.FromString(body)
            assert error.code == expected_error[0]
            assert error.description == expected_error[1]
        elif decode_as:
            body = await response.read()
            return decode_as.FromString(body)
        else:
            return response


@pytest.fixture
async def api(app, loop):
    server = TestServer(app.api, loop=loop)
    client = ProtoTestClient(server, loop=loop)
    await client.start_server()

    yield client

    await client.close()


@pytest.fixture
async def common_api(app, aiohttp_client):
    return await aiohttp_client(app.api)


@pytest.fixture
async def client(factory):
    return await factory.create_client()


@pytest.fixture
async def agency(factory):
    return await factory.create_agency()


@pytest.fixture
async def contract(factory, client):
    return await factory.create_contract(client_id=client["id"])


@pytest.fixture
async def agency_contract(factory, agency):
    return await factory.create_contract(client_id=agency["id"])


@pytest.fixture
def factory(con, faker):
    return Factory(con, faker)


@pytest.fixture
async def product(request, factory):
    service_id = 37 if request.node.get_closest_marker("geoproduct_product") else 110
    return await factory.create_product(
        service_id=service_id,
        billing_type=BillingType.CPM,
        billing_data={"base_cpm": "25"},
    )


@pytest.fixture
async def product_with_fix_billing(factory):
    return await factory.create_product(
        billing_type=BillingType.FIX,
        billing_data={"cost": "25", "time_interval": "MONTHLY"},
    )


@pytest.fixture
def balance_client_mock_class():
    class BalanceClientMock:
        def __init__(self, *args, **kwargs):
            pass

        find_client = coro_mock()
        find_client_by_uid = coro_mock()
        find_clients = coro_mock()
        create_client = coro_mock()
        list_client_contracts = coro_mock()
        create_order = coro_mock()
        create_deposit_request = coro_mock()
        update_orders = coro_mock()
        create_user_client_association = coro_mock()
        list_client_passports = coro_mock()

    return BalanceClientMock


@pytest.fixture
def balance_client(balance_client_mock_class):
    return balance_client_mock_class()


# needed for mock async calls
class AsyncMock(MagicMock):
    async def __call__(self, *args, **kwargs):
        return super(AsyncMock, self).__call__(*args, **kwargs)


@pytest.fixture
async def geoproduct_client(config, aiotvm, mocker):
    mocker.patch(
        "maps_adv.common.geoproduct.GeoproductClient.create_order_for_media_platform",
        new_callable=AsyncMock,
    )
    async with GeoproductClient(
        url=config["GEOPRODUCT_URL"],
        default_uid=config["GEOPRODUCT_DEFAULT_UID"],
        tvm_client=aiotvm,
        tvm_destination="geoproduct",
    ) as client:
        yield client


@pytest.fixture
def xmlrpc_client(mocker, aiotvm):
    client = XmlRpcClient(
        "xmlrpc.localhost",
        tvm_client=aiotvm,
        tvm_destination="balance",
    )
    mocker.patch.object(client, "request", coro_mock())

    return client


@pytest.fixture
def tvmclient():
    return MockedTvmClient()
