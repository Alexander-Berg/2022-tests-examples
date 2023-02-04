import functools

import pytest

from maps_adv.geosmb.booking_yang.proto.orders_pb2 import (
    ClearOrdersForGdprInput,
    ClearOrdersForGdprOutput,
)

pytestmark = [pytest.mark.asyncio]

url = "/internal/v1/clear_orders_for_gdpr/"


@pytest.fixture
def api(api):
    api.post = functools.partial(
        api.post, headers={"X-Ya-Service-Ticket": "abc-ticket"}
    )

    return api


async def test_clears_orders_table_for_matched_orders(factory, api, con):
    await factory.create_order(
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        customer_passport_uid=12345,
    )

    await api.post(url, proto=ClearOrdersForGdprInput(passport_uid=12345))

    order = await con.fetchrow(
        "SELECT customer_name, customer_phone, customer_passport_uid FROM orders"
    )
    assert dict(order) == dict(
        customer_name=None, customer_phone=None, customer_passport_uid=None
    )


async def test_does_not_clear_not_matched_orders(factory, api, con):
    await factory.create_order(
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        customer_passport_uid=999,
    )

    await api.post(url, proto=ClearOrdersForGdprInput(passport_uid=12345))

    order = await con.fetchrow(
        "SELECT customer_name, customer_phone, customer_passport_uid FROM orders"
    )
    assert dict(order) == dict(
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        customer_passport_uid=999,
    )


async def test_returns_cleared_order_ids(factory, api, con):
    order_id = await factory.create_order(
        customer_passport_uid=12345, yang_suite_id="111"
    )

    got = await api.post(
        url,
        proto=ClearOrdersForGdprInput(passport_uid=12345),
        decode_as=ClearOrdersForGdprOutput,
        expected_status=200,
    )

    assert got == ClearOrdersForGdprOutput(cleared_order_ids=[order_id])


async def test_returns_nothing_if_no_matches(factory, api, con):
    await factory.create_order(customer_passport_uid=12345)

    got = await api.post(
        url,
        proto=ClearOrdersForGdprInput(passport_uid=999),
        decode_as=ClearOrdersForGdprOutput,
        expected_status=200,
    )

    assert got == ClearOrdersForGdprOutput(cleared_order_ids=[])
