import pytest

from maps_adv.geosmb.doorman.proto.clients_pb2 import (
    SearchClientsForGdprInput,
    SearchClientsForGdprOutput,
)

pytestmark = [
    pytest.mark.asyncio,
]

url = "/internal/v1/search_clients_for_gdpr/"


async def test_returns_true_if_matched_by_passport(factory, api):
    await factory.create_client(passport_uid=12345)

    got = await api.post(
        url,
        proto=SearchClientsForGdprInput(passport_uid=12345),
        decode_as=SearchClientsForGdprOutput,
        expected_status=200,
    )

    assert got == SearchClientsForGdprOutput(clients_exist=True)


async def test_returns_false_if_is_not_matched_by_passport(factory, api):
    await factory.create_client(passport_uid=111)

    got = await api.post(
        url,
        proto=SearchClientsForGdprInput(passport_uid=222),
        decode_as=SearchClientsForGdprOutput,
        expected_status=200,
    )

    assert got == SearchClientsForGdprOutput(clients_exist=False)
