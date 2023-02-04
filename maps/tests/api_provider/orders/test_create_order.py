import pytest
from marshmallow import ValidationError

from maps_adv.manul.lib.api_providers import OrdersApiProvider
from maps_adv.manul.lib.db.enums import CurrencyType, RateType
from maps_adv.manul.proto import orders_pb2
from maps_adv.manul.tests import dt, dt_to_proto

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def provider(orders_dm):
    return OrdersApiProvider(orders_dm)


async def test_returns_order_data(orders_dm, provider):
    orders_dm.create_order.coro.return_value = dict(
        id=1,
        title="title1",
        client_id=1,
        product_id=2,
        currency=CurrencyType.RUB,
        comment="comment1",
        created_at=dt("2019-01-01 00:00:00"),
        rate=RateType.FREE,
    )
    input_pb = orders_pb2.OrderInput(
        title="title1",
        client_id=1,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment1",
        rate=orders_pb2.RateType.Value("FREE"),
    )

    raw_got = await provider.create_order(input_pb.SerializeToString())
    got = orders_pb2.OrderOutput.FromString(raw_got)

    assert got == orders_pb2.OrderOutput(
        id=1,
        title="title1",
        client_id=1,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment1",
        created_at=dt_to_proto(dt("2019-01-01 00:00:00")),
        rate=orders_pb2.RateType.Value("FREE"),
    )


@pytest.mark.parametrize(
    "field_name, field_value, error_message",
    (
        ["title", "", "Length must be between 1 and 256."],
        ["title", "N" * 257, "Length must be between 1 and 256."],
        ["comment", "N" * 1025, "Longer than maximum length 1024."],
    ),
)
async def test_raises_for_wrong_length_field(
    field_name, field_value, error_message, provider
):
    input_pb = orders_pb2.OrderInput(
        title="title_1",
        client_id=1,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment_1",
        rate=orders_pb2.RateType.Value("FREE"),
    )
    setattr(input_pb, field_name, field_value)

    with pytest.raises(ValidationError) as exc:
        await provider.create_order(input_pb.SerializeToString())

    assert exc.value.messages == {field_name: [error_message]}


async def test_data_manager_called_ok(orders_dm, provider):
    orders_dm.create_order.coro.return_value = dict(
        id=1,
        title="title1",
        client_id=1,
        product_id=2,
        currency=CurrencyType.RUB,
        comment="comment1",
        created_at=dt("2019-01-01 00:00:00"),
        rate=RateType.PAID,
    )
    input_pb = orders_pb2.OrderInput(
        title="title1",
        client_id=1,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment1",
        rate=orders_pb2.RateType.Value("PAID"),
    )

    await provider.create_order(input_pb.SerializeToString())

    orders_dm.create_order.assert_called_with(
        title="title1",
        client_id=1,
        product_id=2,
        currency=CurrencyType.RUB,
        comment="comment1",
        rate=RateType.PAID,
    )
