from datetime import datetime

import asyncpg
import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.scenarist.server.lib.enums import SubscriptionStatus

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("coupon_id", (888, None))
async def test_creates_subscription_version(dm, factory, coupon_id):
    sub_id = await factory.create_subscription(coupon_id=456)

    await dm.replace_subscription_coupon(
        subscription_id=sub_id,
        biz_id=123,
        coupon_id=coupon_id,
        status=SubscriptionStatus.PAUSED,
    )

    versions = await factory.retrieve_subscription_versions(subscription_id=sub_id)
    assert versions == [
        dict(
            id=Any(int),
            subscription_id=sub_id,
            status=SubscriptionStatus.PAUSED,
            coupon_id=coupon_id,
            created_at=Any(datetime),
        ),
        dict(
            id=Any(int),
            subscription_id=sub_id,
            status=SubscriptionStatus.ACTIVE,
            coupon_id=456,
            created_at=Any(datetime),
        ),
    ]


@pytest.mark.parametrize("current_status", [s for s in SubscriptionStatus])
@pytest.mark.parametrize("updated_status", [s for s in SubscriptionStatus])
async def test_updates_subscription_status(dm, factory, current_status, updated_status):
    sub_id = await factory.create_subscription(status=current_status)

    await dm.replace_subscription_coupon(
        subscription_id=sub_id, biz_id=123, coupon_id=888, status=updated_status
    )

    subscription = await factory.retrieve_subscription(subscription_id=sub_id)
    assert subscription["status"] == updated_status


@pytest.mark.parametrize("coupon_id", [None, 888])
async def test_updates_subscription_current_coupon_id(dm, factory, coupon_id):
    sub_id = await factory.create_subscription()

    await dm.replace_subscription_coupon(
        subscription_id=sub_id,
        biz_id=123,
        coupon_id=coupon_id,
        status=SubscriptionStatus.COMPLETED,
    )

    subscription = await factory.retrieve_subscription(subscription_id=sub_id)
    assert subscription["coupon_id"] == coupon_id


async def test_returns_nothing(dm, factory):
    sub_id = await factory.create_subscription()

    got = await dm.replace_subscription_coupon(
        subscription_id=sub_id,
        biz_id=123,
        coupon_id=888,
        status=SubscriptionStatus.PAUSED,
    )

    assert got is None


async def test_raises_for_unknown_subscription(con, dm):
    with pytest.raises(asyncpg.ForeignKeyViolationError):
        await dm.replace_subscription_coupon(
            subscription_id=999,
            biz_id=123,
            coupon_id=888,
            status=SubscriptionStatus.PAUSED,
        )
