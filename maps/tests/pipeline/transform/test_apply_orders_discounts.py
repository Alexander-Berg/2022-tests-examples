import pytest
from aiohttp.web import Response

from maps_adv.export.lib.pipeline.steps import ApplyOrderDiscountsStep
from maps_adv.billing_proxy.proto.orders_for_stat_pb2 import (
    OrderDiscountInfo,
    OrdersDiscountInfo,
)

pytestmark = [pytest.mark.asyncio]


async def test_changes_total_daily_display_limit(config, mock_billing_orders_discounts):
    message = OrdersDiscountInfo(
        discount_info=[
            OrderDiscountInfo(order_id=1234, discount="0.7"),
            OrderDiscountInfo(order_id=5678, discount="1.3"),
            OrderDiscountInfo(order_id=9012, discount="0"),
        ]
    ).SerializeToString()
    mock_billing_orders_discounts(Response(body=message, status=200))

    campaigns = [
        {"id": 1, "order_id": 1234, "total_daily_display_limit": 100},
        {"id": 2, "order_id": 9012, "total_daily_display_limit": 100},
        {"id": 3, "order_id": 1234},
        {"id": 4, "order_id": 1111, "total_daily_display_limit": 100},
        {"id": 5, "order_id": 5678, "total_daily_display_limit": 100},
        {"id": 6, "order_id": None, "total_daily_display_limit": 100},
        {"id": 7, "total_daily_display_limit": 100},
        {"id": 8, "order_id": 1234, "total_daily_display_limit": None},
    ]

    await ApplyOrderDiscountsStep(config)(campaigns)

    assert campaigns == [
        {"id": 1, "order_id": 1234, "total_daily_display_limit": 142},
        {"id": 2, "order_id": 9012, "total_daily_display_limit": 100},
        {"id": 3, "order_id": 1234},
        {"id": 4, "order_id": 1111, "total_daily_display_limit": 100},
        {"id": 5, "order_id": 5678, "total_daily_display_limit": 76},
        {"id": 6, "order_id": None, "total_daily_display_limit": 100},
        {"id": 7, "total_daily_display_limit": 100},
        {"id": 8, "order_id": 1234, "total_daily_display_limit": None},
    ]
