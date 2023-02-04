import pytest

from maps_adv.billing_proxy.lib.data_manager.exceptions import ClientsDoNotExist

pytestmark = [pytest.mark.asyncio]


async def test_updates_set_account_manager(factory, clients_dm):
    client = await factory.create_client(
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        account_manager_id=None,
        partner_agency_id=123,
    )

    await clients_dm.set_account_manager_for_client(client["id"], 100500)

    result = await factory.get_client(client["id"])

    assert result == {
        "id": client["id"],
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "account_manager_id": 100500,
        "is_agency": False,
        "domain": "someTestDomain",
        "partner_agency_id": 123,
    }


async def test_error_if_client_not_exists(clients_dm, factory, con):
    inexistent_id = await factory.get_inexistent_client_id()

    with pytest.raises(ClientsDoNotExist) as exc:
        await clients_dm.set_account_manager_for_client(
            client_id=inexistent_id, account_manager_id=200600
        )

    assert exc.value.client_ids == [inexistent_id]
