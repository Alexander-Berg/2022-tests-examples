import json
from datetime import datetime

import pytest

from maps_adv.manul.lib.db.enums import CurrencyType, RateType
from maps_adv.manul.proto import errors_pb2, orders_pb2
from maps_adv.manul.tests import Any

pytestmark = [pytest.mark.asyncio]

url = "/orders/"


@pytest.mark.parametrize(
    "rate, ex_rate",
    [
        (
            dict(rate=orders_pb2.RateType.Value("PAID")),
            orders_pb2.RateType.Value("PAID"),
        ),
        (
            dict(rate=orders_pb2.RateType.Value("FREE")),
            orders_pb2.RateType.Value("FREE"),
        ),
        (dict(), orders_pb2.RateType.Value("PAID")),
    ],
)
async def test_returns_order_details(rate, ex_rate, api, factory):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]
    input_pb = orders_pb2.OrderInput(
        title="title1",
        client_id=client_id,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment1",
        **rate,
    )

    got = await api.post(
        url, proto=input_pb, decode_as=orders_pb2.OrderOutput, expected_status=201
    )

    assert got == orders_pb2.OrderOutput(
        id=got.id,
        title="title1",
        client_id=client_id,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment1",
        created_at=got.created_at,
        rate=ex_rate,
    )


async def test_order_created(api, factory):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]
    input_pb = orders_pb2.OrderInput(
        title="title1",
        client_id=client_id,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment1",
        rate=orders_pb2.RateType.Value("FREE"),
    )

    got = await api.post(
        url, proto=input_pb, decode_as=orders_pb2.OrderOutput, expected_status=201
    )

    order_details = await factory.retrieve_order(got.id)
    assert order_details == dict(
        id=got.id,
        title="title1",
        client_id=client_id,
        product_id=2,
        currency=CurrencyType.RUB,
        comment="comment1",
        created_at=Any(datetime),
        rate=RateType.FREE,
    )


@pytest.mark.parametrize(
    "field_name, field_value, error_message",
    (
        ["title", "", "Length must be between 1 and 256."],
        ["title", "N" * 257, "Length must be between 1 and 256."],
        ["comment", "N" * 1025, "Longer than maximum length 1024."],
    ),
)
async def test_returns_error_for_wrong_length_field(
    field_name, field_value, error_message, api
):
    input_pb = orders_pb2.OrderInput(
        title="title_1",
        client_id=1,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment_1",
        rate=orders_pb2.RateType.Value("PAID"),
    )
    setattr(input_pb, field_name, field_value)

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=400
    )

    assert got == errors_pb2.Error(
        code=errors_pb2.Error.DATA_VALIDATION_ERROR,
        description=json.dumps({field_name: [error_message]}),
    )


async def test_returns_error_for_unknown_client(api):
    input_pb = orders_pb2.OrderInput(
        title="title_1",
        client_id=1,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment_1",
        rate=orders_pb2.RateType.Value("PAID"),
    )

    got = await api.post(
        url, proto=input_pb, decode_as=errors_pb2.Error, expected_status=404
    )

    assert got == errors_pb2.Error(code=errors_pb2.Error.CLIENT_NOT_FOUND)


async def test_can_create_two_identical_orders(api, factory):
    client_id = (
        await factory.create_client(name="client_name", account_manager_id=100500)
    )["id"]
    input_pb = orders_pb2.OrderInput(
        title="title_1",
        client_id=client_id,
        product_id=2,
        currency=orders_pb2.CurrencyType.Value("RUB"),
        comment="comment_1",
        rate=orders_pb2.RateType.Value("PAID"),
    )

    got = [
        await api.post(
            url, proto=input_pb, decode_as=orders_pb2.OrderOutput, expected_status=201
        )
        for _ in range(2)
    ]

    orders = await factory.list_orders()
    assert [el.id for el in got] == [el["id"] for el in orders]
