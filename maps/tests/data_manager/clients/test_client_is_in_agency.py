import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_true_for_agency_client(factory, agency, clients_dm):
    agency_client = await factory.create_client()
    await factory.create_client()  # not_agency_client
    await factory.add_client_to_agency(agency_client["id"], agency["id"])

    assert (
        await clients_dm.client_is_in_agency(agency_client["id"], agency["id"]) is True
    )


async def test_returns_false_for_not_agency_clients(factory, agency, clients_dm):
    agency_client = await factory.create_client()
    not_agency_client = await factory.create_client()
    await factory.add_client_to_agency(agency_client["id"], agency["id"])

    assert (
        await clients_dm.client_is_in_agency(not_agency_client["id"], agency["id"])
        is False
    )


async def test_returns_false_for_other_agency_clients(factory, agency, clients_dm):
    other_agency = await factory.create_agency()
    other_agency_client = await factory.create_client()
    await factory.add_client_to_agency(other_agency_client["id"], other_agency["id"])

    assert (
        await clients_dm.client_is_in_agency(other_agency_client["id"], agency["id"])
        is False
    )


async def test_returns_false_for_inexistent_client(factory, agency, client, clients_dm):
    await factory.add_client_to_agency(client["id"], agency["id"])
    inexistent_id = await factory.get_inexistent_client_id()

    assert await clients_dm.client_is_in_agency(inexistent_id, agency["id"]) is False


async def test_returns_false_for_inexistent_agency(factory, agency, client, clients_dm):
    await factory.add_client_to_agency(client["id"], agency["id"])
    inexistent_id = await factory.get_inexistent_client_id()

    assert await clients_dm.client_is_in_agency(client["id"], inexistent_id) is False
