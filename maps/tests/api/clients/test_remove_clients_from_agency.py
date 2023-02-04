import pytest

from maps_adv.billing_proxy.proto import clients_pb2, common_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/agencies/{}/clients/"


async def test_removes_client_from_agency(api, factory, agency, client):
    await factory.add_client_to_agency(client["id"], agency["id"])

    input_pb = clients_pb2.ClientIds(client_ids=[client["id"]])
    await api.delete(API_URL.format(agency["id"]), input_pb, allowed_status_codes=[204])

    assert await factory.get_agency_clients_ids(agency["id"]) == []


async def test_does_nothing_if_client_not_in_agency(api, factory, agency, client):
    input_pb = clients_pb2.ClientIds(client_ids=[client["id"]])
    await api.delete(API_URL.format(agency["id"]), input_pb, allowed_status_codes=[204])

    assert await factory.get_agency_clients_ids(agency["id"]) == []


async def test_removes_multiple_clients_from_agency(api, factory, agency):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], agency["id"])
    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], agency["id"])

    input_pb = clients_pb2.ClientIds(client_ids=[client1["id"], client2["id"]])
    await api.delete(API_URL.format(agency["id"]), input_pb, allowed_status_codes=[204])

    assert await factory.get_agency_clients_ids(agency["id"]) == []


async def test_removes_only_provided_clients_from_agency(api, factory, agency):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], agency["id"])
    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], agency["id"])
    client3 = await factory.create_client()
    await factory.add_client_to_agency(client3["id"], agency["id"])

    input_pb = clients_pb2.ClientIds(client_ids=[client1["id"], client2["id"]])
    await api.delete(API_URL.format(agency["id"]), input_pb, allowed_status_codes=[204])

    assert await factory.get_agency_clients_ids(agency["id"]) == [client3["id"]]


async def test_removes_multiple_clients_from_agency_ignoring_those_not_in_agency(
    api, factory, agency
):
    client1 = await factory.create_client()
    await factory.add_client_to_agency(client1["id"], agency["id"])
    client2 = await factory.create_client()
    await factory.add_client_to_agency(client2["id"], agency["id"])
    client3 = await factory.create_client()

    input_pb = clients_pb2.ClientIds(client_ids=[client1["id"], client3["id"]])
    await api.delete(API_URL.format(agency["id"]), input_pb, allowed_status_codes=[204])

    assert await factory.get_agency_clients_ids(agency["id"]) == [client2["id"]]


async def test_raises_for_inexistent_agency(api, factory, client):
    inexistent_id = await factory.get_inexistent_client_id()

    input_pb = clients_pb2.ClientIds(client_ids=[client["id"]])
    await api.delete(
        API_URL.format(inexistent_id),
        input_pb,
        expected_error=(
            common_pb2.Error.AGENCY_DOES_NOT_EXIST,
            f"agency_id={inexistent_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_raises_if_client_has_orders_with_agency(api, factory, agency, client):
    await factory.add_client_to_agency(client["id"], agency["id"])
    await factory.create_order(client_id=client["id"], agency_id=agency["id"])

    input_pb = clients_pb2.ClientIds(client_ids=[client["id"]])
    await api.delete(
        API_URL.format(agency["id"]),
        input_pb,
        expected_error=(
            common_pb2.Error.CLIENTS_HAVE_ORDERS_WITH_AGENCY,
            f"client_ids=[{client['id']}]",
        ),
        allowed_status_codes=[422],
    )

    assert await factory.get_agency_clients_ids(agency["id"]) == [client["id"]]
