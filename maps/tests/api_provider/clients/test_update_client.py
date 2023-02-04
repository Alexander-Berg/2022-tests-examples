import pytest

from maps_adv.manul.lib.api_providers import ClientsApiProvider
from maps_adv.manul.proto import clients_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def provider(clients_dm):
    return ClientsApiProvider(clients_dm)


async def test_returns_client_data(clients_dm, provider):
    clients_dm.update_client.coro.return_value = dict(
        id=1, name="client0", orders_count=4
    )
    input_pb = clients_pb2.ClientInput(name="client0")

    raw_got = await provider.update_client(input_pb.SerializeToString(), client_id=1)
    got = clients_pb2.ClientOutput.FromString(raw_got)

    assert got == clients_pb2.ClientOutput(id=1, name="client0", orders_count=4)


async def test_client_id_passed_into_data_manager(clients_dm, provider):
    clients_dm.update_client.coro.return_value = dict(
        id=1, name="client_name", orders_count=4
    )
    input_pb = clients_pb2.ClientInput(name="new_client_name")

    await provider.update_client(input_pb.SerializeToString(), client_id=1)

    clients_dm.update_client.assert_called_with(client_id=1, name="new_client_name")
