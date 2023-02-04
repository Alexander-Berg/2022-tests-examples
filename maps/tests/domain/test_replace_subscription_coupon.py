import pytest

from maps_adv.geosmb.scenarist.server.lib.enums import SubscriptionStatus
from maps_adv.geosmb.scenarist.server.lib.exceptions import (
    CurrentCouponDuplicate,
    UnknownSubscription,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize("current_status", [s for s in SubscriptionStatus])
async def test_updates_subscription_status_to_active_if_coupon_is_set(
    dm, domain, current_status
):
    dm.retrieve_subscription_current_state.coro.return_value = dict(
        coupon_id=456, status=current_status
    )

    await domain.replace_subscription_coupon(
        subscription_id=111, biz_id=123, coupon_id=888
    )

    dm.replace_subscription_coupon.assert_called_with(
        subscription_id=111, biz_id=123, coupon_id=888, status=SubscriptionStatus.ACTIVE
    )


@pytest.mark.parametrize("current_status", [s for s in SubscriptionStatus])
async def test_updates_subscription_status_to_complete_if_no_coupon(
    dm, domain, current_status
):
    dm.retrieve_subscription_current_state.coro.return_value = dict(
        coupon_id=456, status=current_status
    )

    await domain.replace_subscription_coupon(
        subscription_id=111, biz_id=123, coupon_id=None
    )

    dm.replace_subscription_coupon.assert_called_with(
        subscription_id=111,
        biz_id=123,
        coupon_id=None,
        status=SubscriptionStatus.COMPLETED,
    )


@pytest.mark.parametrize("current_status", [s for s in SubscriptionStatus])
async def test_returns_active_status_if_coupon_is_set(dm, domain, current_status):
    dm.retrieve_subscription_current_state.coro.return_value = dict(
        coupon_id=456, status=current_status
    )

    got = await domain.replace_subscription_coupon(
        subscription_id=111, biz_id=123, coupon_id=888
    )

    assert got == SubscriptionStatus.ACTIVE


@pytest.mark.parametrize("current_status", [s for s in SubscriptionStatus])
async def test_returns_completed_status_if_no_coupon(dm, domain, current_status):
    dm.retrieve_subscription_current_state.coro.return_value = dict(
        coupon_id=456, status=current_status
    )

    got = await domain.replace_subscription_coupon(
        subscription_id=111, biz_id=123, coupon_id=None
    )

    assert got == SubscriptionStatus.COMPLETED


@pytest.mark.parametrize("exception_cls", (Exception, UnknownSubscription))
async def test_propagates_dm_exception(domain, dm, exception_cls):
    dm.retrieve_subscription_current_state.coro.side_effect = exception_cls()

    with pytest.raises(exception_cls):
        await domain.replace_subscription_coupon(
            subscription_id=111, biz_id=123, coupon_id=888
        )


async def test_raises_if_duplicates_current_coupon(dm, domain, factory):
    dm.retrieve_subscription_current_state.coro.return_value = dict(
        coupon_id=456, status=SubscriptionStatus.ACTIVE
    )

    with pytest.raises(CurrentCouponDuplicate):
        await domain.replace_subscription_coupon(
            subscription_id=111, biz_id=123, coupon_id=456
        )
