import pytest

from maps_adv.billing_proxy.proto import clients_pb2

pytestmark = [pytest.mark.asyncio]

CLIENT_URL = "/clients/{}/representatives/"
AGENCY_URL = "/agencies/{}/representatives/"


async def test_returns_representatives(factory, balance_client, api):
    client = await factory.create_client(representatives=[111, 222, 333])
    agency = await factory.create_agency(representatives=[111, 222, 333])

    result = await api.get(CLIENT_URL.format(client["id"]), decode_as=clients_pb2.Uids)
    assert result == clients_pb2.Uids(uids=[111, 222, 333])

    result = await api.get(AGENCY_URL.format(agency["id"]), decode_as=clients_pb2.Uids)
    assert result == clients_pb2.Uids(uids=[111, 222, 333])


async def test_fails_non_existing_client(balance_client, api):
    await api.get(
        CLIENT_URL.format(53), decode_as=clients_pb2.Uids, allowed_status_codes=[404]
    )
    await api.get(
        AGENCY_URL.format(53), decode_as=clients_pb2.Uids, allowed_status_codes=[404]
    )
