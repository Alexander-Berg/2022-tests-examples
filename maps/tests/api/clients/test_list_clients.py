from operator import attrgetter

import pytest

from maps_adv.billing_proxy.proto import clients_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/clients/"


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


async def test_returns_clients(api, client):
    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )
    assert result == clients_pb2.Clients(
        clients=[clients_pb2.Client(**canonize_client(client))]
    )


async def test_not_returns_agency(api, agency):
    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(clients=[])


async def test_return_all_clients(api, factory):
    client1 = await factory.create_client()
    client2 = await factory.create_client()
    client3 = await factory.create_client()

    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(
        clients=sorted(
            [
                clients_pb2.Client(**canonize_client(client1)),
                clients_pb2.Client(**canonize_client(client2)),
                clients_pb2.Client(**canonize_client(client3)),
            ],
            key=attrgetter("id"),
        )
    )


async def test_returns_only_clients(api, client, agency):
    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(
        clients=[clients_pb2.Client(**canonize_client(client))]
    )
