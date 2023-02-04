from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.data_manager import exceptions as dm_exceptions
from maps_adv.billing_proxy.lib.domain.exceptions import (
    OrdersDoNotExist,
    WrongBalanceServiceID,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(orders_domain, orders_dm):
    await orders_domain.update_orders_limits(
        {
            11: {"new_limit": Decimal("200.1234"), "tid": 123},
            12: {"new_limit": Decimal("100.00"), "tid": 234},
            13: {"new_limit": Decimal("400.00"), "tid": 345},
        },
        service_ids={100},
    )

    orders_dm.update_orders_limits.assert_called_with(
        {
            11: {"new_limit": Decimal("200.1234"), "tid": 123},
            12: {"new_limit": Decimal("100.00"), "tid": 234},
            13: {"new_limit": Decimal("400.00"), "tid": 345},
        }
    )


async def test_raises_if_dm_raises(orders_domain, orders_dm):
    orders_dm.update_orders_limits.coro.side_effect = dm_exceptions.OrdersDoNotExist(
        order_ids=[11, 12]
    )

    with pytest.raises(OrdersDoNotExist) as exc:
        await orders_domain.update_orders_limits(
            {
                11: {"new_limit": Decimal("200.1234"), "tid": 123},
                12: {"new_limit": Decimal("100.00"), "tid": 234},
                13: {"new_limit": Decimal("400.00"), "tid": 345},
            },
            service_ids={100},
        )

    assert exc.value.order_ids == [11, 12]


@pytest.mark.parametrize("service_ids", [{222}, {100, 222}])
async def test_raises_if_service_id_is_wrong(orders_domain, service_ids):
    with pytest.raises(WrongBalanceServiceID):
        await orders_domain.update_orders_limits(
            {
                11: {"new_limit": Decimal("200.1234"), "tid": 123},
                12: {"new_limit": Decimal("100.00"), "tid": 234},
                13: {"new_limit": Decimal("400.00"), "tid": 345},
            },
            service_ids=service_ids,
        )
