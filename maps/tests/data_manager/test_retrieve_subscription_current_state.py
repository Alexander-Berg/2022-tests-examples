import pytest

from maps_adv.geosmb.scenarist.server.lib.enums import SubscriptionStatus
from maps_adv.geosmb.scenarist.server.lib.exceptions import UnknownSubscription

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("status", [s for s in SubscriptionStatus])
async def test_returns_subscription_state_data(dm, factory, status):
    sub_id = await factory.create_subscription(status=status, coupon_id=456)

    got = await dm.retrieve_subscription_current_state(
        subscription_id=sub_id, biz_id=123
    )

    assert got == dict(status=status, coupon_id=456)


async def test_raises_if_subscription_not_found(dm):
    with pytest.raises(UnknownSubscription):
        await dm.retrieve_subscription_current_state(subscription_id=999, biz_id=123)


async def test_raises_if_subscription_belongs_to_another_biz_id(dm, factory):
    sub_id = await factory.create_subscription(biz_id=564)

    with pytest.raises(UnknownSubscription):
        await dm.retrieve_subscription_current_state(subscription_id=sub_id, biz_id=123)
