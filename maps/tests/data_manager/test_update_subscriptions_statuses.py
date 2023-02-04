from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName, SubscriptionStatus

pytestmark = [pytest.mark.asyncio]


async def test_updates_subscriptions_statuses(con, factory, dm):
    sub_id_1 = await factory.create_subscription(
        scenario_name=ScenarioName.DISCOUNT_FOR_LOST, status=SubscriptionStatus.ACTIVE
    )
    sub_id_2 = await factory.create_subscription(
        scenario_name=ScenarioName.THANK_THE_LOYAL, status=SubscriptionStatus.ACTIVE
    )

    await dm.update_subscriptions_statuses(
        subscription_ids=[sub_id_1, sub_id_2], status=SubscriptionStatus.COMPLETED
    )

    sub_1 = await factory.retrieve_subscription(subscription_id=sub_id_1)
    sub_2 = await factory.retrieve_subscription(subscription_id=sub_id_2)
    assert sub_1["status"] == SubscriptionStatus.COMPLETED
    assert sub_2["status"] == SubscriptionStatus.COMPLETED


@pytest.mark.parametrize("current_status", SubscriptionStatus)
@pytest.mark.parametrize("status_for_update", SubscriptionStatus)
async def test_updates_status_regardless_the_current_one(
    factory, dm, current_status, status_for_update
):
    sub_id = await factory.create_subscription(status=current_status)

    await dm.update_subscriptions_statuses(
        subscription_ids=[sub_id], status=status_for_update
    )

    subscription = await factory.retrieve_subscription(subscription_id=sub_id)
    assert subscription["status"] == status_for_update


async def test_updates_subscriptions_regardless_biz_id(con, factory, dm):
    sub_id_1 = await factory.create_subscription(
        biz_id=123, status=SubscriptionStatus.ACTIVE
    )
    sub_id_2 = await factory.create_subscription(
        biz_id=456, status=SubscriptionStatus.ACTIVE
    )

    await dm.update_subscriptions_statuses(
        subscription_ids=[sub_id_1, sub_id_2], status=SubscriptionStatus.COMPLETED
    )

    sub_1 = await factory.retrieve_subscription(subscription_id=sub_id_1)
    sub_2 = await factory.retrieve_subscription(subscription_id=sub_id_2)
    assert sub_1["status"] == SubscriptionStatus.COMPLETED
    assert sub_2["status"] == SubscriptionStatus.COMPLETED


async def test_does_not_affect_not_matched_subscription(factory, dm):
    await factory.create_subscription(
        subscription_id=111, scenario_name=ScenarioName.DISCOUNT_FOR_LOST
    )
    await factory.create_subscription(
        subscription_id=222,
        scenario_name=ScenarioName.THANK_THE_LOYAL,
        status=SubscriptionStatus.ACTIVE,
    )

    await dm.update_subscriptions_statuses(
        subscription_ids=[111], status=SubscriptionStatus.COMPLETED
    )

    subscription = await factory.retrieve_subscription(subscription_id=222)
    assert subscription["status"] == SubscriptionStatus.ACTIVE


async def test_does_not_raises_for_unknown_subscription(factory, dm):
    try:
        await dm.update_subscriptions_statuses(
            subscription_ids=[111], status=SubscriptionStatus.COMPLETED
        )
    except Exception:
        pytest.fail("Should not raise")


async def test_returns_nothing(dm, factory):
    sub_id = await factory.create_subscription()

    got = await dm.update_subscriptions_statuses(
        subscription_ids=[sub_id], status=SubscriptionStatus.COMPLETED
    )

    assert got is None


async def test_creates_subscription_version(dm, factory):
    sub_id_1 = await factory.create_subscription(
        scenario_name=ScenarioName.DISCOUNT_FOR_LOST, status=SubscriptionStatus.ACTIVE
    )
    sub_id_2 = await factory.create_subscription(
        scenario_name=ScenarioName.THANK_THE_LOYAL, status=SubscriptionStatus.ACTIVE
    )

    await dm.update_subscriptions_statuses(
        subscription_ids=[sub_id_1, sub_id_2], status=SubscriptionStatus.COMPLETED
    )

    versions_1 = await factory.retrieve_subscription_versions(subscription_id=sub_id_1)
    versions_2 = await factory.retrieve_subscription_versions(subscription_id=sub_id_2)
    assert versions_1 == [
        dict(
            id=Any(int),
            subscription_id=sub_id_1,
            status=SubscriptionStatus.COMPLETED,
            coupon_id=456,
            created_at=Any(datetime),
        ),
        dict(
            id=Any(int),
            subscription_id=sub_id_1,
            status=SubscriptionStatus.ACTIVE,
            coupon_id=456,
            created_at=Any(datetime),
        ),
    ]
    assert versions_2 == [
        dict(
            id=Any(int),
            subscription_id=sub_id_2,
            status=SubscriptionStatus.COMPLETED,
            coupon_id=456,
            created_at=Any(datetime),
        ),
        dict(
            id=Any(int),
            subscription_id=sub_id_2,
            status=SubscriptionStatus.ACTIVE,
            coupon_id=456,
            created_at=Any(datetime),
        ),
    ]
