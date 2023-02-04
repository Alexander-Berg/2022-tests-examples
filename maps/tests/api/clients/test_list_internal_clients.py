from operator import attrgetter

import pytest

from maps_adv.billing_proxy.proto import clients_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/agencies/internal/clients/"


async def test_returns_internal_clients(api, factory, client):
    await factory.add_client_to_agency(client["id"], None)

    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(
        clients=[
            clients_pb2.Client(
                id=client["id"],
                name=client["name"],
                email=client["email"],
                phone=client["phone"],
                orders_count=0,
                account_manager_id=client["account_manager_id"],
                has_accepted_offer=client["has_accepted_offer"],
            )
        ]
    )


async def test_not_returns_not_internal_clients(api, factory, client):
    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(clients=[])


async def test_not_returns_another_agency_clients(api, factory, client):
    another_agency = await factory.create_agency()
    await factory.add_client_to_agency(client["id"], another_agency["id"])

    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(clients=[])


async def test_returns_only_agency_clients(api, factory):
    client_of_this_agency1 = await factory.create_client()
    client_of_this_agency2 = await factory.create_client()
    await factory.add_client_to_agency(client_of_this_agency1["id"], None)
    await factory.add_client_to_agency(client_of_this_agency2["id"], None)

    another_agency = await factory.create_agency()
    client_of_another_agency = await factory.create_client()
    await factory.add_client_to_agency(
        client_of_another_agency["id"], another_agency["id"]
    )

    await factory.create_client()  # Client of no agency

    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(
        clients=sorted(
            [
                clients_pb2.Client(
                    id=client_of_this_agency1["id"],
                    name=client_of_this_agency1["name"],
                    email=client_of_this_agency1["email"],
                    phone=client_of_this_agency1["phone"],
                    orders_count=0,
                    account_manager_id=client_of_this_agency1["account_manager_id"],
                    has_accepted_offer=client_of_this_agency1["has_accepted_offer"],
                ),
                clients_pb2.Client(
                    id=client_of_this_agency2["id"],
                    name=client_of_this_agency2["name"],
                    email=client_of_this_agency2["email"],
                    phone=client_of_this_agency2["phone"],
                    orders_count=0,
                    account_manager_id=client_of_this_agency2["account_manager_id"],
                    has_accepted_offer=client_of_this_agency2["has_accepted_offer"],
                ),
            ],
            key=attrgetter("id"),
        )
    )


async def test_returns_orders_count(api, factory, client):
    await factory.add_client_to_agency(client["id"], None)
    await factory.create_order(agency_id=None, client_id=client["id"])
    await factory.create_order(agency_id=None, client_id=client["id"])

    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(
        clients=[
            clients_pb2.Client(
                id=client["id"],
                name=client["name"],
                email=client["email"],
                phone=client["phone"],
                orders_count=2,
                account_manager_id=client["account_manager_id"],
                has_accepted_offer=client["has_accepted_offer"],
            )
        ]
    )


async def test_not_returns_other_orders_count(api, factory, agency, client):
    await factory.add_client_to_agency(client["id"], None)
    await factory.create_order(agency_id=agency["id"])

    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(
        clients=[
            clients_pb2.Client(
                id=client["id"],
                name=client["name"],
                email=client["email"],
                phone=client["phone"],
                orders_count=0,
                account_manager_id=client["account_manager_id"],
                has_accepted_offer=client["has_accepted_offer"],
            )
        ]
    )


async def test_not_returns_client_orders_count_in_another_agency(api, factory, client):
    await factory.add_client_to_agency(client["id"], None)
    another_agency = await factory.create_agency()
    await factory.add_client_to_agency(client["id"], another_agency["id"])
    await factory.create_order(agency_id=another_agency["id"], client_id=client["id"])

    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(
        clients=[
            clients_pb2.Client(
                id=client["id"],
                name=client["name"],
                email=client["email"],
                phone=client["phone"],
                orders_count=0,
                account_manager_id=client["account_manager_id"],
                has_accepted_offer=client["has_accepted_offer"],
            )
        ]
    )


async def test_returns_orders_count_for_multiple_clients(api, factory):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], None)
    await factory.create_order(agency_id=None, client_id=client1["id"])

    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], None)
    for _ in range(5):
        await factory.create_order(agency_id=None, client_id=client2["id"])

    await factory.create_order()

    result = await api.get(
        API_URL, decode_as=clients_pb2.Clients, allowed_status_codes=[200]
    )

    assert result == clients_pb2.Clients(
        clients=sorted(
            [
                clients_pb2.Client(
                    id=client1["id"],
                    name=client1["name"],
                    email=client1["email"],
                    phone=client1["phone"],
                    orders_count=1,
                    account_manager_id=client1["account_manager_id"],
                    has_accepted_offer=client1["has_accepted_offer"],
                ),
                clients_pb2.Client(
                    id=client2["id"],
                    name=client2["name"],
                    email=client2["email"],
                    phone=client2["phone"],
                    orders_count=5,
                    account_manager_id=client2["account_manager_id"],
                    has_accepted_offer=client2["has_accepted_offer"],
                ),
            ],
            key=attrgetter("id"),
        )
    )
