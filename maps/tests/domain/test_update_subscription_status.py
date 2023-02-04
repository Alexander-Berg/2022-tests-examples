import pytest

from maps_adv.geosmb.scenarist.server.lib.enums import SubscriptionStatus
from maps_adv.geosmb.scenarist.server.lib.exceptions import (
    CurrentStatusDuplicate,
    UnknownSubscription,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "current_status, status_for_update",
    [
        (SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED),
        (SubscriptionStatus.ACTIVE, SubscriptionStatus.COMPLETED),
        (SubscriptionStatus.PAUSED, SubscriptionStatus.ACTIVE),
        (SubscriptionStatus.PAUSED, SubscriptionStatus.COMPLETED),
        (SubscriptionStatus.COMPLETED, SubscriptionStatus.ACTIVE),
        (SubscriptionStatus.COMPLETED, SubscriptionStatus.PAUSED),
    ],
)
async def test_calls_update_status_dm_for_expected_status(
    dm, domain, current_status, status_for_update
):
    dm.retrieve_subscription_current_state.coro.return_value = dict(
        coupon_id=456, status=current_status
    )

    await domain.update_subscription_status(
        subscription_id=111, biz_id=123, status=status_for_update
    )

    dm.update_subscription_status.assert_called_with(
        subscription_id=111, biz_id=123, status=status_for_update
    )


async def test_returns_nothing(domain, dm, factory):
    dm.retrieve_subscription_current_state.coro.return_value = dict(
        coupon_id=456, status=SubscriptionStatus.ACTIVE
    )

    got = await domain.update_subscription_status(
        subscription_id=111, biz_id=123, status=SubscriptionStatus.PAUSED
    )

    assert got is None


@pytest.mark.parametrize("status", SubscriptionStatus)
async def test_raises_if_duplicates_current_status(domain, dm, factory, status):
    dm.retrieve_subscription_current_state.coro.return_value = dict(
        coupon_id=456, status=status
    )

    with pytest.raises(CurrentStatusDuplicate):
        await domain.update_subscription_status(
            subscription_id=111, biz_id=123, status=status
        )


@pytest.mark.parametrize("exception_cls", (Exception, UnknownSubscription))
async def test_propagates_dm_exception(domain, dm, exception_cls):
    dm.retrieve_subscription_current_state.coro.side_effect = exception_cls()

    with pytest.raises(exception_cls):
        await domain.update_subscription_status(
            subscription_id=111, biz_id=123, status=SubscriptionStatus.PAUSED
        )
