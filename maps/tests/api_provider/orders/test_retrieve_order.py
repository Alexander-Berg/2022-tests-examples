import pytest

from maps_adv.manul.lib.api_providers import OrdersApiProvider
from maps_adv.manul.lib.db.enums import CurrencyType, RateType
from maps_adv.manul.proto import orders_pb2
from maps_adv.manul.tests import dt, dt_to_proto

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def provider(orders_dm):
    return OrdersApiProvider(orders_dm)


async def test_returns_order_data(orders_dm, provider):
    orders_dm.retrieve_order.coro.return_value = dict(
        id=1,
        title="title_1",
        client_id=1,
        product_id=2,
        currency=CurrencyType.RUB,
        comment="comment_1",
        created_at=dt("2019-01-01 00:00:00"),
        rate=RateType.PAID,
    )

    raw_got = await provider.retrieve_order(order_id=1)
    got = orders_pb2.OrderOutput.FromString(raw_got)

    assert got == orders_pb2.OrderOutput(
        id=1,
        title="title_1",
        client_id=1,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment_1",
        created_at=dt_to_proto(dt("2019-01-01 00:00:00")),
        rate=orders_pb2.RateType.Value("PAID"),
    )


async def test_data_manager_called_ok(orders_dm, provider):
    orders_dm.retrieve_order.coro.return_value = dict(
        id=1,
        title="title_1",
        client_id=1,
        product_id=2,
        currency=CurrencyType.RUB,
        comment="comment_1",
        created_at=dt("2019-01-01 00:00:00"),
        rate=RateType.PAID,
    )

    await provider.retrieve_order(order_id=1)

    orders_dm.retrieve_order.assert_called_with(order_id=1)
