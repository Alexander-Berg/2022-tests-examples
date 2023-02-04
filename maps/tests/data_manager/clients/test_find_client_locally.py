import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_client_if_found_locally(client, clients_dm):
    result = await clients_dm.find_client_locally(client["id"])

    assert result == {
        "id": client["id"],
        "name": client["name"],
        "email": client["email"],
        "phone": client["phone"],
        "is_agency": client["is_agency"],
        "account_manager_id": client["account_manager_id"],
        "domain": client["domain"],
        "partner_agency_id": client["partner_agency_id"],
        "has_accepted_offer": client["has_accepted_offer"],
        "representatives": [],
    }


async def test_returns_none_for_inexistent_client(factory, clients_dm):
    inexistent_id = await factory.get_inexistent_client_id()

    assert await clients_dm.find_client_locally(inexistent_id) is None
