import pytest

from operator import attrgetter
from maps_adv.billing_proxy.proto import orders_pb2, common_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/clients/{}/orders/"


def _sorted_order_ids(orders):
    return sorted(map(attrgetter("id"), orders.orders))


async def test_returns_offer_orders(api, factory, client):
    order1 = await factory.create_order(
        agency_id=None, contract_id=None, client_id=client["id"]
    )
    order2 = await factory.create_order(agency_id=None, client_id=client["id"])
    order3 = await factory.create_order(contract_id=None, client_id=client["id"])
    order4 = await factory.create_order(client_id=client["id"])

    result = await api.get(
        API_URL.format(client["id"]),
        decode_as=orders_pb2.Orders,
        allowed_status_codes=[200],
    )

    assert _sorted_order_ids(result) == sorted(
        [order1["id"], order2["id"], order3["id"], order4["id"]]
    )


async def test_fails_on_non_existing_client(api, factory):
    inexistent_id = await factory.get_inexistent_client_id()

    await api.get(
        API_URL.format(inexistent_id),
        expected_error=(
            common_pb2.Error.CLIENT_DOES_NOT_EXIST,
            f"client_id={inexistent_id}",
        ),
        allowed_status_codes=[422],
    )
