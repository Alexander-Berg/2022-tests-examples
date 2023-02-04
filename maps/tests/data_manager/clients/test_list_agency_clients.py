from operator import itemgetter

import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_agency_clients(factory, agency, client, clients_dm):
    await factory.add_client_to_agency(client["id"], agency["id"])

    result = await clients_dm.list_agency_clients(agency["id"])

    assert result == [
        {
            "id": client["id"],
            "name": client["name"],
            "email": client["email"],
            "phone": client["phone"],
            "orders_count": 0,
            "account_manager_id": client["account_manager_id"],
            "partner_agency_id": client["partner_agency_id"],
            "has_accepted_offer": client["has_accepted_offer"],
        }
    ]


async def test_not_returns_not_agency_clients(factory, agency, client, clients_dm):
    result = await clients_dm.list_agency_clients(agency["id"])

    assert result == []


async def test_not_returns_another_agency_clients(factory, agency, client, clients_dm):
    another_agency = await factory.create_agency()
    await factory.add_client_to_agency(client["id"], another_agency["id"])

    result = await clients_dm.list_agency_clients(agency["id"])

    assert result == []


async def test_not_returns_internal_clients(factory, agency, client, clients_dm):
    await factory.add_client_to_agency(client["id"], None)

    result = await clients_dm.list_agency_clients(agency["id"])

    assert result == []


async def test_returns_only_agency_clients(factory, clients_dm):
    agency = await factory.create_agency()
    client_of_this_agency1 = await factory.create_client()
    client_of_this_agency2 = await factory.create_client()
    await factory.add_client_to_agency(client_of_this_agency1["id"], agency["id"])
    await factory.add_client_to_agency(client_of_this_agency2["id"], agency["id"])

    another_agency = await factory.create_agency()
    client_of_another_agency = await factory.create_client()
    await factory.add_client_to_agency(
        client_of_another_agency["id"], another_agency["id"]
    )

    await factory.create_client()  # Client of no agency

    result = await clients_dm.list_agency_clients(agency["id"])

    expected_result = [
        {
            "id": client_of_this_agency1["id"],
            "name": client_of_this_agency1["name"],
            "email": client_of_this_agency1["email"],
            "phone": client_of_this_agency1["phone"],
            "orders_count": 0,
            "account_manager_id": client_of_this_agency1["account_manager_id"],
            "partner_agency_id": client_of_this_agency1["partner_agency_id"],
            "has_accepted_offer": client_of_this_agency1["has_accepted_offer"],
        },
        {
            "id": client_of_this_agency2["id"],
            "name": client_of_this_agency2["name"],
            "email": client_of_this_agency2["email"],
            "phone": client_of_this_agency2["phone"],
            "orders_count": 0,
            "account_manager_id": client_of_this_agency2["account_manager_id"],
            "partner_agency_id": client_of_this_agency2["partner_agency_id"],
            "has_accepted_offer": client_of_this_agency2["has_accepted_offer"],
        },
    ]
    key_func = itemgetter("id")
    assert sorted(result, key=key_func) == sorted(expected_result, key=key_func)


async def test_list_internal_clients(factory, client, clients_dm):
    await factory.add_client_to_agency(client["id"], None)

    result = await clients_dm.list_agency_clients(None)

    assert result == [
        {
            "id": client["id"],
            "name": client["name"],
            "email": client["email"],
            "phone": client["phone"],
            "orders_count": 0,
            "account_manager_id": client["account_manager_id"],
            "partner_agency_id": client["partner_agency_id"],
            "has_accepted_offer": client["has_accepted_offer"],
        }
    ]


async def test_not_returns_agency_clients_for_internal(factory, client, clients_dm):
    another_agency = await factory.create_agency()
    await factory.add_client_to_agency(client["id"], another_agency["id"])

    result = await clients_dm.list_agency_clients(None)

    assert result == []


async def test_returns_only_internal_clients(factory, clients_dm):
    internal_client1 = await factory.create_client()
    internal_client2 = await factory.create_client()
    await factory.add_client_to_agency(internal_client1["id"], None)
    await factory.add_client_to_agency(internal_client2["id"], None)

    another_agency = await factory.create_agency()
    client_of_another_agency = await factory.create_client()
    await factory.add_client_to_agency(
        client_of_another_agency["id"], another_agency["id"]
    )

    await factory.create_client()  # Client of no agency

    result = await clients_dm.list_agency_clients(None)

    expected_result = [
        {
            "id": internal_client1["id"],
            "name": internal_client1["name"],
            "email": internal_client1["email"],
            "phone": internal_client1["phone"],
            "orders_count": 0,
            "account_manager_id": internal_client1["account_manager_id"],
            "partner_agency_id": internal_client1["partner_agency_id"],
            "has_accepted_offer": internal_client1["has_accepted_offer"],
        },
        {
            "id": internal_client2["id"],
            "name": internal_client2["name"],
            "email": internal_client2["email"],
            "phone": internal_client2["phone"],
            "orders_count": 0,
            "account_manager_id": internal_client2["account_manager_id"],
            "partner_agency_id": internal_client2["partner_agency_id"],
            "has_accepted_offer": internal_client2["has_accepted_offer"],
        },
    ]
    key_func = itemgetter("id")
    assert sorted(result, key=key_func) == sorted(expected_result, key=key_func)


async def test_returns_orders_count(factory, agency, client, clients_dm):
    await factory.add_client_to_agency(client["id"], agency["id"])
    await factory.create_order(agency_id=agency["id"], client_id=client["id"])
    await factory.create_order(agency_id=agency["id"], client_id=client["id"])

    result = await clients_dm.list_agency_clients(agency["id"])

    assert result == [
        {
            "id": client["id"],
            "name": client["name"],
            "email": client["email"],
            "phone": client["phone"],
            "orders_count": 2,
            "account_manager_id": client["account_manager_id"],
            "partner_agency_id": client["partner_agency_id"],
            "has_accepted_offer": client["has_accepted_offer"],
        }
    ]


async def test_returns_orders_count_for_internal_clients(
    factory, agency, client, clients_dm
):
    await factory.add_client_to_agency(client["id"], None)
    await factory.create_order(agency_id=None, client_id=client["id"])
    await factory.create_order(agency_id=None, client_id=client["id"])

    result = await clients_dm.list_agency_clients(None)

    assert result == [
        {
            "id": client["id"],
            "name": client["name"],
            "email": client["email"],
            "phone": client["phone"],
            "orders_count": 2,
            "account_manager_id": client["account_manager_id"],
            "partner_agency_id": client["partner_agency_id"],
            "has_accepted_offer": client["has_accepted_offer"],
        }
    ]


async def test_not_returns_other_orders_count(factory, agency, client, clients_dm):
    await factory.add_client_to_agency(client["id"], agency["id"])
    await factory.create_order()

    result = await clients_dm.list_agency_clients(agency["id"])

    assert result == [
        {
            "id": client["id"],
            "name": client["name"],
            "email": client["email"],
            "phone": client["phone"],
            "orders_count": 0,
            "account_manager_id": client["account_manager_id"],
            "partner_agency_id": client["partner_agency_id"],
            "has_accepted_offer": client["has_accepted_offer"],
        }
    ]


async def test_not_returns_client_orders_count_in_another_agency(
    factory, agency, client, clients_dm
):
    await factory.add_client_to_agency(client["id"], agency["id"])
    another_agency = await factory.create_agency()
    await factory.add_client_to_agency(client["id"], another_agency["id"])
    await factory.create_order(agency_id=another_agency["id"], client_id=client["id"])

    result = await clients_dm.list_agency_clients(agency["id"])

    assert result == [
        {
            "id": client["id"],
            "name": client["name"],
            "email": client["email"],
            "phone": client["phone"],
            "orders_count": 0,
            "account_manager_id": client["account_manager_id"],
            "partner_agency_id": None,
            "has_accepted_offer": client["has_accepted_offer"],
        }
    ]


async def test_not_returns_client_orders_count_in_internal_agency(
    factory, agency, client, clients_dm
):
    await factory.add_client_to_agency(client["id"], agency["id"])
    await factory.add_client_to_agency(client["id"], None)
    await factory.create_order(agency_id=None, client_id=client["id"])

    result = await clients_dm.list_agency_clients(agency["id"])

    assert result == [
        {
            "id": client["id"],
            "name": client["name"],
            "email": client["email"],
            "phone": client["phone"],
            "orders_count": 0,
            "account_manager_id": client["account_manager_id"],
            "partner_agency_id": client["partner_agency_id"],
            "has_accepted_offer": client["has_accepted_offer"],
        }
    ]


async def test_returns_orders_count_for_multiple_clients(factory, agency, clients_dm):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], agency["id"])
    await factory.create_order(agency_id=agency["id"], client_id=client1["id"])

    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], agency["id"])
    for _ in range(5):
        await factory.create_order(agency_id=agency["id"], client_id=client2["id"])

    await factory.create_order()

    result = await clients_dm.list_agency_clients(agency["id"])

    key_func = itemgetter("id")
    assert sorted(result, key=key_func) == sorted(
        [
            {
                "id": client1["id"],
                "name": client1["name"],
                "email": client1["email"],
                "phone": client1["phone"],
                "orders_count": 1,
                "account_manager_id": client1["account_manager_id"],
                "partner_agency_id": client1["partner_agency_id"],
                "has_accepted_offer": client1["has_accepted_offer"],
            },
            {
                "id": client2["id"],
                "name": client2["name"],
                "email": client2["email"],
                "phone": client2["phone"],
                "orders_count": 5,
                "account_manager_id": client2["account_manager_id"],
                "partner_agency_id": client2["partner_agency_id"],
                "has_accepted_offer": client2["has_accepted_offer"],
            },
        ],
        key=key_func,
    )
