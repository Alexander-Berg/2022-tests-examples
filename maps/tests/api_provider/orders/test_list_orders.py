import pytest

from maps_adv.manul.lib.api_providers import OrdersApiProvider
from maps_adv.manul.lib.db.enums import CurrencyType, RateType
from maps_adv.manul.proto import orders_pb2
from maps_adv.manul.tests import dt, dt_to_proto

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def provider(orders_dm):
    return OrdersApiProvider(orders_dm)


async def test_returns_orders_data(orders_dm, provider):
    orders_dm.list_orders.coro.return_value = [
        dict(
            id=1,
            title="title_1",
            client_id=1,
            product_id=2,
            currency=CurrencyType.RUB,
            comment="comment_1",
            created_at=dt("2019-01-01 00:00:00"),
            rate=RateType.FREE,
        ),
        dict(
            id=2,
            title="title_2",
            client_id=2,
            product_id=2,
            currency=CurrencyType.EUR,
            comment="comment_2",
            created_at=dt("2019-02-01 00:00:00"),
            rate=RateType.PAID,
        ),
    ]

    raw_got = await provider.list_orders(b"")
    got_pb = orders_pb2.OrdersList.FromString(raw_got)

    assert got_pb == orders_pb2.OrdersList(
        orders=[
            orders_pb2.OrderOutput(
                id=1,
                title="title_1",
                client_id=1,
                product_id=2,
                currency=orders_pb2.CurrencyType.Value("RUB"),
                comment="comment_1",
                created_at=dt_to_proto(dt("2019-01-01 00:00:00")),
                rate=orders_pb2.RateType.Value("FREE"),
            ),
            orders_pb2.OrderOutput(
                id=2,
                title="title_2",
                client_id=2,
                product_id=2,
                currency=orders_pb2.CurrencyType.Value("EUR"),
                comment="comment_2",
                created_at=dt_to_proto(dt("2019-02-01 00:00:00")),
                rate=orders_pb2.RateType.Value("PAID"),
            ),
        ]
    )


async def test_data_manager_called_ok_for_all_orders(orders_dm, provider):
    orders_dm.list_orders.coro.return_value = []

    await provider.list_orders(b"")

    orders_dm.list_orders.assert_called_with(ids=[])


async def test_data_manager_called_ok_for_list_of_order_ids(orders_dm, provider):
    orders_dm.list_orders.coro.return_value = []

    input_data = orders_pb2.OrdersFilter(ids=[1, 3, 5]).SerializeToString()
    await provider.list_orders(input_data)

    orders_dm.list_orders.assert_called_with(ids=[1, 3, 5])
