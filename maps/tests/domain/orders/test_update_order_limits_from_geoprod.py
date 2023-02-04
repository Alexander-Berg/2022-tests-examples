from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.data_manager import exceptions as dm_exceptions
from maps_adv.billing_proxy.lib.domain.exceptions import (
    ExternalOrdersDoNotExist,
    OrdersDoNotExist,
    WrongBalanceServiceID,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


def map_external_id_to_order(external_id: int):
    mapping = {11: 111, 12: 222, 13: 333}
    return mapping.get(external_id)


async def test_uses_dm(orders_domain, orders_dm):
    orders_dm.retrieve_order_id_by_external_id.coro.side_effect = (
        map_external_id_to_order
    )

    await orders_domain.update_orders_limits_from_geoprod(
        {
            11: {"new_limit": Decimal("200.1234"), "tid": 123},
            12: {"new_limit": Decimal("100.00"), "tid": 234},
            13: {"new_limit": Decimal("400.00"), "tid": 345},
        },
        service_ids={200},
    )

    orders_dm.update_orders_limits.assert_called_with(
        {
            111: {"new_limit": Decimal("200.1234"), "tid": 123},
            222: {"new_limit": Decimal("100.00"), "tid": 234},
            333: {"new_limit": Decimal("400.00"), "tid": 345},
        }
    )


async def test_raises_if_dm_raises(orders_domain, orders_dm):
    orders_dm.retrieve_order_id_by_external_id.coro.side_effect = (
        map_external_id_to_order
    )
    orders_dm.update_orders_limits.coro.side_effect = dm_exceptions.OrdersDoNotExist(
        order_ids=[111, 222]
    )

    with pytest.raises(OrdersDoNotExist) as exc:
        await orders_domain.update_orders_limits_from_geoprod(
            {
                11: {"new_limit": Decimal("200.1234"), "tid": 123},
                12: {"new_limit": Decimal("100.00"), "tid": 234},
                13: {"new_limit": Decimal("400.00"), "tid": 345},
            },
            service_ids={200},
        )

    assert exc.value.order_ids == [111, 222]


@pytest.mark.parametrize("service_ids", [{111}, {200, 111}])
async def test_raises_if_service_id_is_wrong(orders_domain, service_ids):
    with pytest.raises(WrongBalanceServiceID):
        await orders_domain.update_orders_limits_from_geoprod(
            {
                11: {"new_limit": Decimal("200.1234"), "tid": 123},
                12: {"new_limit": Decimal("100.00"), "tid": 234},
                13: {"new_limit": Decimal("400.00"), "tid": 345},
            },
            service_ids=service_ids,
        )


async def test_raises_if_external_id_is_unknown(orders_domain, orders_dm):
    orders_dm.retrieve_order_id_by_external_id.coro.side_effect = (
        map_external_id_to_order
    )
    with pytest.raises(ExternalOrdersDoNotExist) as exc:
        await orders_domain.update_orders_limits_from_geoprod(
            {
                55: {"new_limit": Decimal("200.1234"), "tid": 123},
                12: {"new_limit": Decimal("100.00"), "tid": 234},
            },
            service_ids={200},
        )
    assert exc.value.external_order_ids == [55]
