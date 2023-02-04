from operator import attrgetter

import pytest

from maps_adv.billing_proxy.proto import clients_pb2, common_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/agencies/{}/clients/"


async def test_returns_agency_clients(api, factory, agency, client):
    await factory.add_client_to_agency(client["id"], agency["id"])

    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Clients,
        allowed_status_codes=[200],
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


async def test_not_returns_not_agency_clients(api, factory, agency, client):
    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Clients,
        allowed_status_codes=[200],
    )

    assert result == clients_pb2.Clients(clients=[])


async def test_not_returns_another_agency_clients(api, factory, agency, client):
    another_agency = await factory.create_agency()
    await factory.add_client_to_agency(client["id"], another_agency["id"])

    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Clients,
        allowed_status_codes=[200],
    )

    assert result == clients_pb2.Clients(clients=[])


async def test_returns_only_agency_clients(api, factory):
    agency = await factory.create_agency()
    client_of_this_agency1 = await factory.create_client()
    client_of_this_agency2 = await factory.create_client()
    await factory.add_client_to_agency(client_of_this_agency1["id"], agency["id"])
    await factory.add_client_to_agency(client_of_this_agency2["id"], agency["id"])

    another_agency = await factory.create_agency()
    client_of_another_agency = await factory.create_client()
    await factory.add_client_to_agency(
        client_of_another_agency["id"], another_agency["id"]
    )

    await factory.create_client()  # Client of no agency

    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Clients,
        allowed_status_codes=[200],
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


async def test_raises_for_inexistent_agency(api, factory):
    inexistent_id = await factory.get_inexistent_client_id()

    await api.get(
        API_URL.format(inexistent_id),
        expected_error=(
            common_pb2.Error.AGENCY_DOES_NOT_EXIST,
            f"agency_id={inexistent_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_returns_orders_count(api, factory, agency, client):
    await factory.add_client_to_agency(client["id"], agency["id"])
    await factory.create_order(agency_id=agency["id"], client_id=client["id"])
    await factory.create_order(agency_id=agency["id"], client_id=client["id"])

    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Clients,
        allowed_status_codes=[200],
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
    await factory.add_client_to_agency(client["id"], agency["id"])
    await factory.create_order()

    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Clients,
        allowed_status_codes=[200],
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


async def test_not_returns_client_orders_count_in_another_agency(
    api, factory, agency, client
):
    await factory.add_client_to_agency(client["id"], agency["id"])
    another_agency = await factory.create_agency()
    await factory.add_client_to_agency(client["id"], another_agency["id"])
    await factory.create_order(agency_id=another_agency["id"], client_id=client["id"])

    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Clients,
        allowed_status_codes=[200],
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


async def test_returns_orders_count_for_multiple_clients(api, factory, agency):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], agency["id"])
    await factory.create_order(agency_id=agency["id"], client_id=client1["id"])

    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], agency["id"])
    for _ in range(5):
        await factory.create_order(agency_id=agency["id"], client_id=client2["id"])

    await factory.create_order()

    result = await api.get(
        API_URL.format(agency["id"]),
        decode_as=clients_pb2.Clients,
        allowed_status_codes=[200],
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
