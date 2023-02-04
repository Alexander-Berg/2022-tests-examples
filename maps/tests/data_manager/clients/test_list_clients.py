from operator import itemgetter
import pytest

pytestmark = [pytest.mark.asyncio]


def canonize_client(client):
    return {
        "id": client["id"],
        "name": client["name"],
        "email": client["email"],
        "phone": client["phone"],
        "account_manager_id": client["account_manager_id"],
        "partner_agency_id": client["partner_agency_id"],
        "has_accepted_offer": client["has_accepted_offer"],
        "orders_count": None,
    }


async def test_returns_clients(client, clients_dm):
    result = await clients_dm.list_clients()

    assert result == [canonize_client(client)]


async def test_not_returns_agency(agency, clients_dm):
    result = await clients_dm.list_clients()

    assert result == []


async def test_returns_all_clients(factory, clients_dm):
    client1 = await factory.create_client()
    client2 = await factory.create_client()
    client3 = await factory.create_client()

    result = await clients_dm.list_clients()

    expected_result = [
        canonize_client(client1),
        canonize_client(client2),
        canonize_client(client3),
    ]

    assert sorted(result, key=itemgetter("id")) == sorted(
        expected_result, key=itemgetter("id")
    )
