import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]


async def test_removes_client_from_agency(factory, con, agency, client, clients_dm):
    await factory.add_client_to_agency(client["id"], agency["id"])

    await clients_dm.remove_clients_from_agency([client["id"]], agency["id"])

    assert await factory.get_agency_clients_ids(agency["id"]) == []


async def test_does_nothing_if_client_not_in_agency(
    factory, con, agency, client, clients_dm
):
    await clients_dm.remove_clients_from_agency([client["id"]], agency["id"])

    assert await factory.get_agency_clients_ids(agency["id"]) == []


async def test_removes_multiple_clients_from_agency(factory, con, agency, clients_dm):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], agency["id"])
    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], agency["id"])

    await clients_dm.remove_clients_from_agency(
        [client1["id"], client2["id"]], agency["id"]
    )

    assert await factory.get_agency_clients_ids(agency["id"]) == []


async def test_removes_only_provided_clients_from_agency(
    factory, con, agency, clients_dm
):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], agency["id"])
    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], agency["id"])
    client3 = await factory.create_client()
    await factory.add_client_to_agency(client3["id"], agency["id"])

    await clients_dm.remove_clients_from_agency(
        [client1["id"], client2["id"]], agency["id"]
    )

    assert await factory.get_agency_clients_ids(agency["id"]) == [client3["id"]]


async def test_removes_multiple_clients_from_agency_ignoring_those_not_in_agency(
    factory, con, agency, clients_dm
):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], agency["id"])
    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], agency["id"])
    client3 = await factory.create_client()

    await clients_dm.remove_clients_from_agency(
        [client1["id"], client3["id"]], agency["id"]
    )

    assert await factory.get_agency_clients_ids(agency["id"]) == [client2["id"]]


async def test_removes_client_from_internal(factory, con, client, clients_dm):
    await factory.add_client_to_agency(client["id"], None)

    await clients_dm.remove_clients_from_agency([client["id"]], None)

    assert await factory.get_agency_clients_ids(None) == []


async def test_does_nothing_if_client_not_in_internal(factory, con, client, clients_dm):
    await clients_dm.remove_clients_from_agency([client["id"]], None)

    assert await factory.get_agency_clients_ids(None) == []


async def test_removes_multiple_clients_from_internal(factory, con, clients_dm):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], None)
    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], None)

    await clients_dm.remove_clients_from_agency([client1["id"], client2["id"]], None)

    assert await factory.get_agency_clients_ids(None) == []


async def test_removes_only_provided_clients_from_internal(factory, con, clients_dm):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], None)
    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], None)
    client3 = await factory.create_client()
    await factory.add_client_to_agency(client3["id"], None)

    await clients_dm.remove_clients_from_agency([client1["id"], client2["id"]], None)

    assert await factory.get_agency_clients_ids(None) == [client3["id"]]


async def test_removes_multiple_clients_from_internal_ignoring_those_not_in_agency(
    factory, con, clients_dm
):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], None)
    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], None)
    client3 = await factory.create_client()

    await clients_dm.remove_clients_from_agency([client1["id"], client3["id"]], None)

    assert await factory.get_agency_clients_ids(None) == [client2["id"]]
