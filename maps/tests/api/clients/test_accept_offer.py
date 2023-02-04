import pytest

from maps_adv.billing_proxy.proto import clients_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

CREATE_CLIENT_URL = "/clients/"
RETRIEVE_CLIENT_URL = "/clients/{}/"
CLIENT_ACCEPT_OFFER_URL = "/clients/{}/accept_offer/"
AGENCY_ACCEPT_OFFER_URL = "/agencies/{}/accept_offer/"
LIST_AGENCIES_URL = "/agencies/"


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.create_client.coro.return_value = 55


async def test_defaults_to_false(api):
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="",
    )
    result = await api.post(
        CREATE_CLIENT_URL,
        input_pb,
        decode_as=clients_pb2.Client,
        allowed_status_codes=[201],
    )

    assert not result.has_accepted_offer


async def test_sets_has_accepted_offer(api, agency, client):
    assert not client["has_accepted_offer"]
    assert not agency["has_accepted_offer"]

    await api.post(
        CLIENT_ACCEPT_OFFER_URL.format(client["id"]),
        allowed_status_codes=[200],
    )

    result = await api.get(
        RETRIEVE_CLIENT_URL.format(client["id"]),
        decode_as=clients_pb2.Client,
        allowed_status_codes=[200],
    )

    await api.post(
        AGENCY_ACCEPT_OFFER_URL.format(agency["id"]),
        allowed_status_codes=[200],
    )

    result = await api.get(
        LIST_AGENCIES_URL,
        decode_as=clients_pb2.Agencies,
        allowed_status_codes=[200],
    )

    assert result.agencies[0].has_accepted_offer


async def test_accepts_already_accepted(agency, api):
    input_pb = clients_pb2.ClientCreationInput(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        domain="",
        has_accepted_offer=True,
    )
    result = await api.post(
        CREATE_CLIENT_URL,
        input_pb,
        decode_as=clients_pb2.Client,
        allowed_status_codes=[201],
    )
    assert result.has_accepted_offer

    await api.post(
        CLIENT_ACCEPT_OFFER_URL.format(result.id),
        allowed_status_codes=[200],
    )
    result = await api.get(
        RETRIEVE_CLIENT_URL.format(result.id),
        decode_as=clients_pb2.Client,
        allowed_status_codes=[200],
    )
    assert result.has_accepted_offer

    await api.post(
        AGENCY_ACCEPT_OFFER_URL.format(agency["id"]),
        allowed_status_codes=[200],
    )
    result = await api.get(
        LIST_AGENCIES_URL,
        decode_as=clients_pb2.Agencies,
        allowed_status_codes=[200],
    )
    assert result.agencies[0].has_accepted_offer

    await api.post(
        AGENCY_ACCEPT_OFFER_URL.format(agency["id"]),
        allowed_status_codes=[200],
    )
    result = await api.get(
        LIST_AGENCIES_URL,
        decode_as=clients_pb2.Agencies,
        allowed_status_codes=[200],
    )
    assert result.agencies[0].has_accepted_offer


async def test_fails_non_existing_client(api):
    api.post(
        CLIENT_ACCEPT_OFFER_URL.format(53),
        allowed_status_codes=[404],
    )

    api.post(
        AGENCY_ACCEPT_OFFER_URL.format(53),
        allowed_status_codes=[404],
    )
