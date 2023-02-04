import pytest

pytestmark = [pytest.mark.asyncio]


async def test_checks_if_clients_have_agencies(factory, clients_dm):
    client1 = await factory.create_client()
    client2 = await factory.create_client()
    agency = await factory.create_agency()

    assert (
        await clients_dm.list_clients_with_orders_with_agency(
            [client1["id"], client2["id"]], agency["id"]
        )
    ) == []

    await factory.create_order(client_id=client1["id"], agency_id=agency["id"])

    assert await clients_dm.list_clients_with_orders_with_agency(
        [client1["id"], client2["id"]], agency["id"]
    ) == [client1["id"]]
