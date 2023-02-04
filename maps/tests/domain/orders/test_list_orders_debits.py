from decimal import Decimal

import pytest
from maps_adv.common.helpers import dt

from maps_adv.billing_proxy.lib.domain.exceptions import OrdersDoNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(orders_domain, orders_dm):
    orders_dm.list_inexistent_order_ids.coro.return_value = []
    orders_dm.list_orders_debits.coro.return_value = {
        101: [
            {
                "billed_at": dt("2022-01-10 00:00:00"),
                "amount": Decimal("10"),
            },
            {
                "billed_at": dt("2022-01-20 00:00:00"),
                "amount": Decimal("100"),
            },
            {
                "billed_at": dt("2022-01-20 00:00:01"),
                "amount": Decimal("10"),
            },
        ],
        102: [
            {
                "billed_at": dt("2022-01-22 00:00:00"),
                "amount": Decimal("200"),
            },
        ],
    }

    result = await orders_domain.list_orders_debits(
        [101, 102, 103], dt("2022-01-01 00:00:00")
    )
    assert result == {
        "orders_debits": [
            {
                "order_id": 101,
                "debits": [
                    {
                        "billed_at": dt("2022-01-10 00:00:00"),
                        "amount": Decimal("10"),
                    },
                    {
                        "billed_at": dt("2022-01-20 00:00:00"),
                        "amount": Decimal("100"),
                    },
                    {
                        "billed_at": dt("2022-01-20 00:00:01"),
                        "amount": Decimal("10"),
                    },
                ],
            },
            {
                "order_id": 102,
                "debits": [
                    {
                        "billed_at": dt("2022-01-22 00:00:00"),
                        "amount": Decimal("200"),
                    },
                ],
            },
        ]
    }


async def test_raises_on_nonexisting_orders(orders_domain, orders_dm):
    orders_dm.list_inexistent_order_ids.coro.return_value = [103]
    orders_dm.list_orders_debits.coro.return_value = {}

    with pytest.raises(OrdersDoNotExist) as exc:
        await orders_domain.list_orders_debits(
            [101, 102, 103], dt("2022-01-01 00:00:00")
        )

    assert exc.value.order_ids == [103]
