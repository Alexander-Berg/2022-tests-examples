import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


async def test_returns_nothing_if_nothing_found(orders_dm, factory):
    got = await orders_dm.retrieve_order_ids_for_account(200600)

    assert got == []


async def test_returns_list_of_existing_orders_ids(orders_dm, factory):
    client1_id = (await factory.create_client("client1", 100500))["id"]
    order1_id = (await factory.create_order("order1", client1_id))["id"]
    order2_id = (await factory.create_order("order2", client1_id))["id"]
    client2_id = (await factory.create_client("client2", 100500))["id"]
    order3_id = (await factory.create_order("order1", client2_id))["id"]
    order4_id = (await factory.create_order("order2", client2_id))["id"]

    got = await orders_dm.retrieve_order_ids_for_account(100500)

    assert sorted(got) == [order1_id, order2_id, order3_id, order4_id]


@pytest.mark.parametrize(["other_account"], [[200600], [None]])
async def test_does_not_return_orders_ids_for_other_accounts(
    orders_dm, factory, other_account
):
    client1_id = (await factory.create_client("client1", 100500))["id"]
    order1_id = (await factory.create_order("order1", client1_id))["id"]
    client2_id = (await factory.create_client("client2", other_account))["id"]
    await factory.create_order("order2", client2_id)

    got = await orders_dm.retrieve_order_ids_for_account(100500)

    assert got == [order1_id]
