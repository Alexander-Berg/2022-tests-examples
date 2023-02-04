import pytest

from maps_adv.manul.lib.api_providers import ClientsApiProvider
from maps_adv.manul.proto import clients_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def provider(clients_dm):
    return ClientsApiProvider(clients_dm)


async def test_returns_client_data(clients_dm, provider):
    clients_dm.list_clients.coro.return_value = [
        dict(id=1, name="client1", orders_count=4),
        dict(id=2, name="client2", orders_count=40),
    ]

    raw_got = await provider.list_clients()
    got = clients_pb2.ClientsList.FromString(raw_got)

    assert got == clients_pb2.ClientsList(
        clients=[
            clients_pb2.ClientOutput(id=1, name="client1", orders_count=4),
            clients_pb2.ClientOutput(id=2, name="client2", orders_count=40),
        ]
    )


async def test_data_manager_called_ok(clients_dm, provider):
    clients_dm.list_clients.coro.return_value = []

    await provider.list_clients()

    assert clients_dm.list_clients.called is True
