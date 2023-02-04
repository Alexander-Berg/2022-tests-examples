import pytest

from maps_adv.manul.lib.data_managers.exceptions import ClientExists
from maps_adv.manul.tests import Any

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("manager_id", [None, 100500])
async def test_creates_client(clients_dm, con, manager_id):
    args = dict(name="client_name")
    if manager_id is not None:
        args["account_manager_id"] = manager_id
    got = await clients_dm.create_client(**args)

    sql = """
        SELECT EXISTS(
            SELECT id
            FROM clients
            WHERE id = $1 AND name = $2
        )
    """
    assert await con.fetchval(sql, got["id"], "client_name") is True


@pytest.mark.parametrize("manager_id", [None, 100500])
async def test_returns_client_details(clients_dm, manager_id):
    args = dict(name="client_name")
    if manager_id is not None:
        args["account_manager_id"] = manager_id
    got = await clients_dm.create_client(**args)

    assert got == dict(id=Any(int), name="client_name", account_manager_id=manager_id)


@pytest.mark.parametrize("manager_id", [None, 100500])
async def test_raises_if_client_already_exists(clients_dm, manager_id):
    args = dict(name="client_name")
    if manager_id is not None:
        args["account_manager_id"] = manager_id
    await clients_dm.create_client(**args)

    with pytest.raises(ClientExists):
        await clients_dm.create_client(name="client_name", account_manager_id=manager_id)
