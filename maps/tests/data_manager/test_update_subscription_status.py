from datetime import datetime

import pytest

from maps_adv.common.helpers import Any
from maps_adv.geosmb.scenarist.server.lib.enums import SubscriptionStatus

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("current_status", SubscriptionStatus)
@pytest.mark.parametrize("status_for_update", SubscriptionStatus)
async def test_updates_subscription_status(
    dm, factory, current_status, status_for_update
):
    sub_id = await factory.create_subscription(status=current_status)

    await dm.update_subscription_status(
        subscription_id=sub_id, biz_id=123, status=status_for_update
    )

    subscription = await factory.retrieve_subscription(subscription_id=sub_id)
    assert subscription["status"] == status_for_update


async def test_returns_nothing(dm, factory):
    sub_id = await factory.create_subscription()

    got = await dm.update_subscription_status(
        subscription_id=sub_id, biz_id=123, status=SubscriptionStatus.PAUSED
    )

    assert got is None


@pytest.mark.real_db
@pytest.mark.parametrize("current_status", SubscriptionStatus)
@pytest.mark.parametrize("status_for_update", SubscriptionStatus)
async def test_creates_subscription_version(
    dm, factory, current_status, status_for_update
):
    sub_id = await factory.create_subscription(status=current_status)

    await dm.update_subscription_status(
        subscription_id=sub_id, biz_id=123, status=status_for_update
    )

    sub_versions = await factory.retrieve_subscription_versions(subscription_id=sub_id)
    assert sub_versions == [
        dict(
            id=Any(int),
            subscription_id=sub_id,
            status=status_for_update,
            coupon_id=456,
            created_at=Any(datetime),
        ),
        dict(
            id=Any(int),
            subscription_id=sub_id,
            status=current_status,
            coupon_id=456,
            created_at=Any(datetime),
        ),
    ]
