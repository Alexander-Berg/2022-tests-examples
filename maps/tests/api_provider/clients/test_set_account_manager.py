import pytest

from maps_adv.manul.lib.api_providers import ClientsApiProvider
from maps_adv.manul.proto import clients_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def provider(clients_dm):
    return ClientsApiProvider(clients_dm)


async def test_account_manager_passed_into_data_manager(clients_dm, provider):
    input_pb = clients_pb2.ClientSetAccountManagerInput(
        client_id=1, account_manager_id=100500
    )

    await provider.set_account_manager_for_client(input_pb.SerializeToString())

    clients_dm.set_account_manager_for_client.assert_called_with(
        client_id=1, account_manager_id=100500
    )
