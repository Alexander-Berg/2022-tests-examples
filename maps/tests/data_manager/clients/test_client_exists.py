import pytest

pytestmark = [pytest.mark.asyncio]


async def test_returns_true_for_existing_client(client, clients_dm):
    assert await clients_dm.client_exists(client_id=client["id"]) is True


async def test_returns_false_for_inexistent_client(factory, clients_dm):
    inexistent_id = await factory.get_inexistent_client_id()
    assert await clients_dm.client_exists(client_id=inexistent_id) is False


async def test_returns_false_for_existing_agency(agency, clients_dm):
    assert await clients_dm.client_exists(client_id=agency["id"]) is False
