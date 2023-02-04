import pytest

pytestmark = [pytest.mark.asyncio]


async def test_checks_if_clients_have_agencies(factory, clients_dm):
    client1 = await factory.create_client()
    client2 = await factory.create_client()
    agency = await factory.create_agency()

    assert (
        await clients_dm.list_clients_with_agencies([client1["id"], client2["id"]])
    ) == []

    await clients_dm.add_clients_to_agency([client1["id"]], agency["id"])

    assert await clients_dm.list_clients_with_agencies(
        [client1["id"], client2["id"]]
    ) == [client1["id"]]


async def test_checks_if_clients_in_internal_agency(factory, clients_dm):
    client1 = await factory.create_client()
    client2 = await factory.create_client()

    assert (
        await clients_dm.list_clients_with_agencies([client1["id"], client2["id"]])
    ) == []

    await clients_dm.add_clients_to_agency([client1["id"]], None)

    assert await clients_dm.list_clients_with_agencies(
        [client1["id"], client2["id"]]
    ) == [client1["id"]]
