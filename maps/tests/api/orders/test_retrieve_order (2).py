import pytest

from maps_adv.manul.proto import errors_pb2, orders_pb2
from maps_adv.manul.tests import dt_to_proto

pytestmark = [pytest.mark.asyncio]


def url(order_id: int) -> str:
    return f"/orders/{order_id}/"


async def test_returns_order_details(api, factory):
    client_id = (
        await factory.create_client(name="client0", account_manager_id=100500)
    )["id"]
    order = await factory.create_order("order0", client_id, comment="comment0")

    got = await api.get(
        url(order["id"]), decode_as=orders_pb2.OrderOutput, expected_status=200
    )

    assert got == orders_pb2.OrderOutput(
        id=order["id"],
        title="order0",
        client_id=client_id,
        product_id=1,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment0",
        created_at=dt_to_proto(order["created_at"]),
        rate=orders_pb2.RateType.Value("PAID"),
    )


async def test_returns_error_for_order_does_not_exist(api):
    got = await api.get(url(111), decode_as=errors_pb2.Error, expected_status=404)

    assert got == errors_pb2.Error(code=errors_pb2.Error.ORDER_NOT_FOUND)
