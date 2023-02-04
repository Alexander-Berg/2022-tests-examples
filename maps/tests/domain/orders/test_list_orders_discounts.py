from datetime import datetime, timezone
import pytz


from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import OrdersDoNotExist
from maps_adv.billing_proxy.lib.db.enums import CampaignType, CurrencyType

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]
MOSCOW_TZ = pytz.timezone("Europe/Moscow")


async def test_uses_dm(orders_domain, orders_dm):
    orders_dm.list_inexistent_order_ids.coro.return_value = []
    orders_dm.find_orders.coro.return_value = [
        {
            "id": 1,
            "type": "REGULAR",
            "campaign_type": CampaignType.BILLBOARD,
            "currency": CurrencyType.RUB,
        },
        {
            "id": 2,
            "type": "REGULAR",
            "campaign_type": CampaignType.CATEGORY_SEARCH_PIN,
            "currency": CurrencyType.RUB,
        },
        {
            "id": 3,
            "type": "YEARLONG",
            "campaign_type": CampaignType.BILLBOARD,
            "currency": CurrencyType.RUB,
        },
        {
            "id": 4,
            "type": "REGULAR",
            "campaign_type": CampaignType.BILLBOARD,
            "currency": CurrencyType.BYN,
        },
        {
            "id": 5,
            "type": "REGULAR",
            "campaign_type": CampaignType.BILLBOARD,
            "currency": CurrencyType.KZT,
        },
        {
            "id": 6,
            "type": "REGULAR",
            "campaign_type": CampaignType.OVERVIEW_BANNER,
            "currency": CurrencyType.USD,
        },
        {
            "id": 7,
            "type": "REGULAR",
            "campaign_type": CampaignType.ZERO_SPEED_BANNER,
            "currency": CurrencyType.EUR,
        },
    ]
    result = await orders_domain.list_orders_discounts(
        order_ids=[1, 2, 3, 4, 5, 6, 7],
        billed_at=datetime(2000, 1, 1, tzinfo=timezone.utc),
    )

    orders_dm.find_orders.assert_called_with([1, 2, 3, 4, 5, 6, 7])
    assert result == {
        1: {"discount": Decimal("0.7")},
        2: {"discount": Decimal("1.0")},
        3: {"discount": Decimal("1.0")},
        4: {"discount": Decimal("1.0")},
        5: {"discount": Decimal("1.0")},
        6: {"discount": Decimal("0.7")},
        7: {"discount": Decimal("0.7")},
    }


async def test_raises_for_inexistent_orders(orders_domain, orders_dm):
    orders_dm.list_inexistent_order_ids.coro.return_value = [1, 2]
    orders_dm.find_orders.coro.return_value = [
        {
            "id": 1,
            "type": "REGULAR",
            "campaign_type": CampaignType.BILLBOARD,
            "currency": CurrencyType.RUB,
        },
        {
            "id": 2,
            "type": "REGULAR",
            "campaign_type": CampaignType.CATEGORY_SEARCH_PIN,
            "currency": CurrencyType.RUB,
        },
        {
            "id": 3,
            "type": "YEARLONG",
            "campaign_type": CampaignType.BILLBOARD,
            "currency": CurrencyType.RUB,
        },
    ]

    with pytest.raises(OrdersDoNotExist) as exc:
        await orders_domain.list_orders_discounts(
            order_ids=[1, 2, 3], billed_at=datetime(2000, 2, 2, tzinfo=timezone.utc)
        )

    assert exc.value.order_ids == [1, 2]


@pytest.mark.parametrize(
    ("month", "discount"),
    (
        (1, Decimal("0.7")),
        (2, Decimal("0.8")),
        (3, Decimal("1.0")),
        (4, Decimal("1.0")),
        (5, Decimal("1.0")),
        (6, Decimal("1.0")),
        (7, Decimal("1.0")),
        (8, Decimal("1.0")),
        (9, Decimal("1.3")),
        (10, Decimal("1.3")),
        (11, Decimal("1.3")),
        (12, Decimal("1.3")),
    ),
)
async def test_uses_monthly_discounts(orders_domain, orders_dm, month, discount):
    orders_dm.list_inexistent_order_ids.coro.return_value = []
    orders_dm.find_orders.coro.return_value = [
        {
            "id": 1,
            "type": "REGULAR",
            "campaign_type": CampaignType.BILLBOARD,
            "currency": CurrencyType.RUB,
        },
        {
            "id": 2,
            "type": "REGULAR",
            "campaign_type": CampaignType.CATEGORY_SEARCH_PIN,
            "currency": CurrencyType.RUB,
        },
        {
            "id": 3,
            "type": "YEARLONG",
            "campaign_type": CampaignType.BILLBOARD,
            "currency": CurrencyType.RUB,
        },
    ]

    result = await orders_domain.list_orders_discounts(
        order_ids=[1, 2, 3], billed_at=datetime(2000, month, 1, tzinfo=timezone.utc)
    )

    orders_dm.find_orders.assert_called_with([1, 2, 3])
    assert result == {
        1: {"discount": discount},
        2: {"discount": Decimal("1.0")},
        3: {"discount": Decimal("1.0")},
    }


@pytest.mark.parametrize(
    ("billed_at", "discount"),
    (
        (datetime(2000, 1, 31, 12, 0, 0, tzinfo=MOSCOW_TZ), Decimal("0.7")),
        (datetime(2000, 1, 31, 21, 0, 0, tzinfo=MOSCOW_TZ), Decimal("0.7")),
        (datetime(2000, 1, 31, 23, 0, 0, tzinfo=MOSCOW_TZ), Decimal("0.7")),
        (datetime(2000, 1, 31, 23, 0, 0, tzinfo=MOSCOW_TZ), Decimal("0.7")),
        (datetime(2000, 2, 1, 0, 0, 0, tzinfo=MOSCOW_TZ), Decimal("0.8")),
        (datetime(2000, 2, 1, 12, 0, 0, tzinfo=MOSCOW_TZ), Decimal("0.8")),
        (datetime(2000, 2, 28, 23, 59, 59, tzinfo=MOSCOW_TZ), Decimal("0.8")),
        (datetime(2000, 3, 1, 0, 0, 0, tzinfo=MOSCOW_TZ), Decimal("1.0")),
        (datetime(2000, 12, 31, 23, 0, 0, tzinfo=MOSCOW_TZ), Decimal("1.3")),
        (datetime(2001, 1, 1, 0, 0, 0, tzinfo=MOSCOW_TZ), Decimal("0.7")),
    ),
)
async def test_uses_moscow_time_zone(orders_domain, orders_dm, billed_at, discount):
    orders_dm.list_inexistent_order_ids.coro.return_value = []
    orders_dm.find_orders.coro.return_value = [
        {
            "id": 1,
            "type": "REGULAR",
            "campaign_type": CampaignType.BILLBOARD,
            "currency": CurrencyType.RUB,
        },
        {
            "id": 2,
            "type": "REGULAR",
            "campaign_type": CampaignType.CATEGORY_SEARCH_PIN,
            "currency": CurrencyType.RUB,
        },
        {
            "id": 3,
            "type": "YEARLONG",
            "campaign_type": CampaignType.BILLBOARD,
            "currency": CurrencyType.RUB,
        },
    ]

    result = await orders_domain.list_orders_discounts(
        order_ids=[1, 2, 3], billed_at=billed_at.astimezone(timezone.utc)
    )

    orders_dm.find_orders.assert_called_with([1, 2, 3])
    assert result == {
        1: {"discount": discount},
        2: {"discount": Decimal("1.0")},
        3: {"discount": Decimal("1.0")},
    }
