from typing import List

import aiohttp.pytest_plugin
import pytest
from asyncpg import Connection

from maps_adv.manul.lib.application import Application
from maps_adv.manul.lib.data_managers import (
    BaseClientsDataManager,
    BaseOrdersDataManager,
    ClientsDataManager,
    OrdersDataManager,
)
from maps_adv.manul.lib.db.engine import DB
from maps_adv.manul.lib.db.enums import CurrencyType, RateType

from . import coro_mock

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.common.pgswim.pytest_plugin_deprecated",
]
del aiohttp.pytest_plugin.loop


_config = {
    "DATABASE_URL": "postgresql://manul:manul@localhost:5433/manul",
}


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture(scope="session")
def config():
    return _config.copy()


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB


@pytest.fixture
def clients_dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_clients_dm")
    return request.getfixturevalue("_clients_dm")


@pytest.fixture
def orders_dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_orders_dm")
    return request.getfixturevalue("_orders_dm")


@pytest.fixture
def _clients_dm(db):
    return ClientsDataManager(db)


@pytest.fixture
def _orders_dm(db):
    return OrdersDataManager(db)


@pytest.fixture
def _mock_clients_dm():
    class MockDm(BaseClientsDataManager):
        create_client = coro_mock()
        update_client = coro_mock()
        set_account_manager_for_client = coro_mock()
        retrieve_client = coro_mock()
        list_clients = coro_mock()

    return MockDm()


@pytest.fixture
def _mock_orders_dm():
    class MockDm(BaseOrdersDataManager):
        create_order = coro_mock()
        retrieve_order = coro_mock()
        list_orders = coro_mock()
        retrieve_order_ids_for_account = coro_mock()

    return MockDm()


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

        if response.headers.get("content_type") == "application/json":
            return await response.json()
        else:
            return await response.read()


@pytest.fixture
async def api(loop, config, db, aiohttp_client):
    app = Application(config)
    api = app.setup(db)

    server = aiohttp.test_utils.TestServer(api, loop=loop)
    client = TestClient(server, loop=loop)

    await client.start_server()
    yield client
    await client.close()


class Factory:
    def __init__(
        self,
        clients_dm: ClientsDataManager,
        orders_dm: OrdersDataManager,
        con: Connection,
    ):
        self.clients_dm = clients_dm
        self.orders_dm = orders_dm
        self.con = con

    async def create_client(self, name: str, account_manager_id: int = None) -> dict:
        return await self.clients_dm.create_client(name, account_manager_id)

    async def retrieve_client(self, client_id: int) -> dict:
        return await self.clients_dm.retrieve_client(client_id)

    async def create_order(
        self,
        title: str,
        client_id: int,
        product_id: int = 1,
        currency: CurrencyType = CurrencyType.RUB,
        comment: str = "",
        rate: RateType = RateType.PAID,
    ) -> dict:
        return await self.orders_dm.create_order(
            title, client_id, product_id, currency, comment, rate
        )

    async def retrieve_order(self, order_id: int) -> dict:
        return await self.orders_dm.retrieve_order(order_id)

    async def list_orders(self, *args) -> List[dict]:
        if args:
            return await self.orders_dm.list_orders(*args)
        else:
            return await self.orders_dm.list_orders([])


@pytest.fixture
async def factory(clients_dm, orders_dm, con):
    return Factory(clients_dm, orders_dm, con)
