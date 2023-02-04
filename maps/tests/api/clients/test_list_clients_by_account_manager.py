import pytest

from maps_adv.billing_proxy.proto import clients_pb2, orders_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/clients/by-account-manager/"


async def test_returns_account_clients(api, factory):
    client1 = await factory.create_client(account_manager_id=1)
    client2 = await factory.create_client(account_manager_id=1)

    input_pb = orders_pb2.AccountManagerId(account_manager_id=1)

    result = await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(
        clients=[
            clients_pb2.Client(
                id=client1["id"],
                name=client1["name"],
                email=client1["email"],
                phone=client1["phone"],
                account_manager_id=1,
                has_accepted_offer=client1["has_accepted_offer"],
            ),
            clients_pb2.Client(
                id=client2["id"],
                name=client2["name"],
                email=client2["email"],
                phone=client2["phone"],
                account_manager_id=1,
                has_accepted_offer=client2["has_accepted_offer"],
            ),
        ]
    )


async def test_not_returns_not_account_clients(api, factory):
    await factory.create_client(account_manager_id=None)

    input_pb = orders_pb2.AccountManagerId(account_manager_id=1)

    result = await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(clients=[])


async def test_not_returns_another_account_clients(api, factory, agency, client):
    await factory.create_client(account_manager_id=2)

    input_pb = orders_pb2.AccountManagerId(account_manager_id=1)

    result = await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(clients=[])


async def test_returns_only_account_clients(api, factory):
    client1 = await factory.create_client(account_manager_id=1)
    client2 = await factory.create_client(account_manager_id=1)
    await factory.create_client(account_manager_id=2)

    input_pb = orders_pb2.AccountManagerId(account_manager_id=1)

    result = await api.post(
        API_URL, input_pb, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(
        clients=[
            clients_pb2.Client(
                id=client1["id"],
                name=client1["name"],
                email=client1["email"],
                phone=client1["phone"],
                account_manager_id=1,
                has_accepted_offer=client1["has_accepted_offer"],
            ),
            clients_pb2.Client(
                id=client2["id"],
                name=client2["name"],
                email=client2["email"],
                phone=client2["phone"],
                account_manager_id=1,
                has_accepted_offer=client2["has_accepted_offer"],
            ),
        ]
    )
