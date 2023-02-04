from operator import itemgetter

import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_account_manager_clients(factory, clients_dm):
    client = await factory.create_client(account_manager_id=1)
    result = await clients_dm.list_account_manager_clients(1)

    assert result == [
        {
            "id": client["id"],
            "name": client["name"],
            "email": client["email"],
            "phone": client["phone"],
            "account_manager_id": 1,
            "partner_agency_id": client["partner_agency_id"],
        }
    ]


async def test_not_returns_not_account_manager_clients(
    factory, agency, client, clients_dm
):
    result = await clients_dm.list_account_manager_clients(1)

    assert result == []


async def test_not_returns_another_account_manager_clients(
    factory, agency, client, clients_dm
):
    await factory.create_client(account_manager_id=2)

    result = await clients_dm.list_account_manager_clients(1)

    assert result == []


async def test_returns_only_account_manager_clients(factory, clients_dm):
    client1 = await factory.create_client(account_manager_id=1)
    client2 = await factory.create_client(account_manager_id=1)
    await factory.create_client(account_manager_id=2)
    await factory.create_client(account_manager_id=None)  # Client of no account

    result = await clients_dm.list_account_manager_clients(1)

    expected_result = [
        {
            "id": client1["id"],
            "name": client1["name"],
            "email": client1["email"],
            "phone": client1["phone"],
            "account_manager_id": 1,
            "partner_agency_id": None,
        },
        {
            "id": client2["id"],
            "name": client2["name"],
            "email": client2["email"],
            "phone": client2["phone"],
            "account_manager_id": 1,
            "partner_agency_id": None,
        },
    ]
    key_func = itemgetter("id")
    assert sorted(result, key=key_func) == sorted(expected_result, key=key_func)
