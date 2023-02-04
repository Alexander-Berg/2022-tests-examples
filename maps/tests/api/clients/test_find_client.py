import pytest

from maps_adv.billing_proxy.proto import clients_pb2
from maps_adv.billing_proxy.tests.helpers import mock_find_by_uid_clients

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/clients/find-client-by-uid/"


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.find_client_by_uid.coro.side_effect = mock_find_by_uid_clients


async def test_returns_client_data(api):
    input_pb = clients_pb2.ClientFindByUidSchema(uid=10001)
    result = await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Client, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Client(
        id=55,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        partner_agency_id=1,
        has_accepted_offer=False,
    )


@pytest.mark.parametrize("uid", [10002, 10003])
async def test_not_returns_client_data(api, uid):
    input_pb = clients_pb2.ClientFindByUidSchema(uid=uid)
    await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Client, allowed_status_codes=[200, 404]
    )
