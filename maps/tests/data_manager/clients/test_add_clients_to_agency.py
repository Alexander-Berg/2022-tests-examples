import pytest

from maps_adv.billing_proxy.lib.data_manager.exceptions import (
    AgencyDoesNotExist,
    ClientsDoNotExist,
)

pytestmark = [pytest.mark.asyncio]


async def test_adds_one_client_to_agency(factory, agency, client, clients_dm):
    await clients_dm.add_clients_to_agency([client["id"]], agency["id"])

    assert await factory.get_agency_clients_ids(agency["id"]) == [client["id"]]


async def test_adds_many_clients_to_agency(factory, agency, clients_dm):
    client1 = await factory.create_client()
    client2 = await factory.create_client()
    client3 = await factory.create_client()

    await clients_dm.add_clients_to_agency(
        [client1["id"], client2["id"], client3["id"]], agency["id"]
    )

    agency_clients = await factory.get_agency_clients_ids(agency["id"])
    expected_agency_clients = [client1["id"], client2["id"], client3["id"]]
    assert sorted(agency_clients) == sorted(expected_agency_clients)


async def test_adds_another_client_to_agency(factory, agency, client, clients_dm):
    await factory.add_client_to_agency(client["id"], agency["id"])
    another_client = await factory.create_client()

    await clients_dm.add_clients_to_agency([another_client["id"]], agency["id"])

    agency_clients = await factory.get_agency_clients_ids(agency["id"])
    expected_agency_clients = [client["id"], another_client["id"]]
    assert sorted(agency_clients) == sorted(expected_agency_clients)


async def test_does_nothing_if_client_already_in_agency(
    factory, agency, client, clients_dm
):
    await factory.add_client_to_agency(client["id"], agency["id"])

    await clients_dm.add_clients_to_agency([client["id"]], agency["id"])

    assert await factory.get_agency_clients_ids(agency["id"]) == [client["id"]]


async def test_raises_for_nonexistent_agency(factory, client, clients_dm):
    inexistent_id = await factory.get_inexistent_client_id()

    with pytest.raises(AgencyDoesNotExist) as exc:
        await clients_dm.add_clients_to_agency([client["id"]], inexistent_id)

    assert exc.value.agency_id == inexistent_id


async def test_raises_for_nonexistent_clients(factory, agency, client, clients_dm):
    inexistent_id1 = await factory.get_inexistent_client_id()
    inexistent_id2 = inexistent_id1 + 1

    with pytest.raises(ClientsDoNotExist) as exc:
        await clients_dm.add_clients_to_agency(
            [client["id"], inexistent_id1, inexistent_id2], agency["id"]
        )

    assert sorted(exc.value.client_ids) == sorted([inexistent_id1, inexistent_id2])


async def test_adds_one_client_to_internal(factory, client, clients_dm):
    await clients_dm.add_clients_to_agency([client["id"]], None)

    assert await factory.get_agency_clients_ids(None) == [client["id"]]


async def test_adds_many_clients_to_internal(factory, clients_dm):
    client1 = await factory.create_client()
    client2 = await factory.create_client()
    client3 = await factory.create_client()

    await clients_dm.add_clients_to_agency(
        [client1["id"], client2["id"], client3["id"]], None
    )

    agency_clients = await factory.get_agency_clients_ids(None)
    expected_agency_clients = [client1["id"], client2["id"], client3["id"]]
    assert sorted(agency_clients) == sorted(expected_agency_clients)


async def test_adds_another_client_to_internal(factory, client, clients_dm):
    await factory.add_client_to_agency(client["id"], None)
    another_client = await factory.create_client()

    await clients_dm.add_clients_to_agency([another_client["id"]], None)

    agency_clients = await factory.get_agency_clients_ids(None)
    expected_agency_clients = [client["id"], another_client["id"]]
    assert sorted(agency_clients) == sorted(expected_agency_clients)


async def test_does_nothing_if_client_already_in_internal(factory, client, clients_dm):
    await factory.add_client_to_agency(client["id"], None)

    await clients_dm.add_clients_to_agency([client["id"]], None)

    assert await factory.get_agency_clients_ids(None) == [client["id"]]
