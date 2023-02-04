import functools

import pytest

from maps_adv.geosmb.booking_yang.proto.orders_pb2 import (
    SearchOrdersForGdprInput,
    SearchOrdersForGdprOutput,
)

pytestmark = [
    pytest.mark.asyncio,
]

url = "/internal/v1/search_orders_for_gdpr/"


@pytest.fixture
def api(api):
    api.post = functools.partial(
        api.post, headers={"X-Ya-Service-Ticket": "abc-ticket"}
    )

    return api


async def test_returns_true_if_matched_by_passport(factory, api):
    await factory.create_order(customer_passport_uid=12345)

    got = await api.post(
        url,
        proto=SearchOrdersForGdprInput(passport_uid=12345),
        decode_as=SearchOrdersForGdprOutput,
        expected_status=200,
    )

    assert got == SearchOrdersForGdprOutput(orders_exist=True)


async def test_returns_false_if_is_not_matched_by_passport(factory, api):
    await factory.create_order(customer_passport_uid=111)

    got = await api.post(
        url,
        proto=SearchOrdersForGdprInput(passport_uid=222),
        decode_as=SearchOrdersForGdprOutput,
        expected_status=200,
    )

    assert got == SearchOrdersForGdprOutput(orders_exist=False)
