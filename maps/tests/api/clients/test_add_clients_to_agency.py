import pytest

from maps_adv.billing_proxy.proto import clients_pb2, common_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]


API_URL = "/agencies/{}/clients/"


async def test_adds_client_to_agency(api, factory, agency, client):
    input_pb = clients_pb2.ClientIds(client_ids=[client["id"]])

    await api.patch(API_URL.format(agency["id"]), input_pb, allowed_status_codes=[201])

    assert await factory.get_agency_clients_ids(agency["id"]) == [client["id"]]


async def test_adds_many_clients_to_agency(api, factory, agency):
    client1 = await factory.create_client()
    client2 = await factory.create_client()
    client3 = await factory.create_client()

    input_pb = clients_pb2.ClientIds(
        client_ids=[client1["id"], client2["id"], client3["id"]]
    )
    await api.patch(API_URL.format(agency["id"]), input_pb, allowed_status_codes=[201])

    result = await factory.get_agency_clients_ids(agency["id"])
    expected_result = [client1["id"], client2["id"], client3["id"]]
    assert sorted(result) == sorted(expected_result)


async def test_add_another_client_to_agency(api, factory, agency, client):
    await factory.add_client_to_agency(client["id"], agency["id"])
    another_client = await factory.create_client()

    input_pb = clients_pb2.ClientIds(client_ids=[another_client["id"]])
    await api.patch(API_URL.format(agency["id"]), input_pb, allowed_status_codes=[201])

    assert sorted(await factory.get_agency_clients_ids(agency["id"])) == sorted(
        [client["id"], another_client["id"]]
    )


async def test_returns_error_if_client_already_in_agency(api, factory, agency, client):
    await factory.add_client_to_agency(client["id"], agency["id"])

    input_pb = clients_pb2.ClientIds(client_ids=[client["id"]])
    await api.patch(
        API_URL.format(agency["id"]),
        input_pb,
        expected_error=(
            common_pb2.Error.CLIENTS_ARE_ALREADY_IN_AGENCY,
            f"client_ids=[{client['id']}]",
        ),
        allowed_status_codes=[422],
    )

    assert await factory.get_agency_clients_ids(agency["id"]) == [client["id"]]


async def test_raises_for_nonexistent_agency(api, factory, client):
    inexistent_id = await factory.get_inexistent_client_id()

    input_pb = clients_pb2.ClientIds(client_ids=[client["id"]])
    await api.patch(
        API_URL.format(inexistent_id),
        input_pb,
        expected_error=(
            common_pb2.Error.AGENCY_DOES_NOT_EXIST,
            f"agency_id={inexistent_id}",
        ),
        allowed_status_codes=[422],
    )


async def test_raises_for_nonexistent_clients(api, factory, agency, client):
    inexistent_id1 = await factory.get_inexistent_client_id()
    inexistent_id2 = inexistent_id1 + 1

    input_pb = clients_pb2.ClientIds(
        client_ids=[client["id"], inexistent_id1, inexistent_id2]
    )
    await api.patch(
        API_URL.format(agency["id"]),
        input_pb,
        expected_error=(
            common_pb2.Error.CLIENTS_DO_NOT_EXIST,
            f"client_ids={sorted([inexistent_id1, inexistent_id2])}",
        ),
        allowed_status_codes=[422],
    )
