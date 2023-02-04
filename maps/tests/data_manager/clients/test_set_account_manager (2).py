import pytest

from maps_adv.manul.lib.data_managers.exceptions import ClientNotFound

pytestmark = [pytest.mark.asyncio]


async def test_update_client_account_manager(clients_dm, factory, con):
    client_id = (await factory.create_client("client_name", account_manager_id=100500))[
        "id"
    ]

    await clients_dm.set_account_manager_for_client(client_id, 200600)

    result = await factory.retrieve_client(client_id)

    assert result == {
        "id": client_id,
        "name": "client_name",
        "account_manager_id": 200600,
        "orders_count": 0,
    }


async def test_error_if_client_not_exists(clients_dm, factory, con):

    with pytest.raises(ClientNotFound, match="1"):
        await clients_dm.set_account_manager_for_client(100500, 200600)
