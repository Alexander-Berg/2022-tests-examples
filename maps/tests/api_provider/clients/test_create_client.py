import pytest
from marshmallow import ValidationError

from maps_adv.manul.lib.api_providers import ClientsApiProvider
from maps_adv.manul.proto import clients_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def provider(clients_dm):
    return ClientsApiProvider(clients_dm)


@pytest.mark.parametrize("manager_id", (None, 100500))
async def test_returns_client_data(clients_dm, provider, manager_id):
    clients_dm.create_client.coro.return_value = dict(id=1, name="client0")
    input_pb = clients_pb2.ClientInput(name="client0")
    if manager_id is not None:
        input_pb.account_manager_id = manager_id

    raw_got = await provider.create_client(input_pb.SerializeToString())
    got = clients_pb2.ClientOutput.FromString(raw_got)

    assert got == clients_pb2.ClientOutput(id=1, name="client0")


@pytest.mark.parametrize("name", ("", "N" * 257))
async def test_raises_for_wrong_length_name(name, provider):
    input_pb = clients_pb2.ClientInput(name=name)

    with pytest.raises(ValidationError) as exc:
        await provider.create_client(input_pb.SerializeToString())

    assert exc.value.messages == {"name": ["Length must be between 1 and 256."]}


async def test_data_manager_called_ok(clients_dm, provider):
    clients_dm.create_client.coro.return_value = dict(id=1, name="client0")
    input_pb = clients_pb2.ClientInput(name="client0")

    await provider.create_client(input_pb.SerializeToString())

    clients_dm.create_client.assert_called_with(name="client0")
