from datetime import datetime

import pytest

from maps_adv.common.helpers import Any
from maps_adv.geosmb.clients.facade import CouponStatus
from maps_adv.geosmb.scenarist.server.lib.enums import SubscriptionStatus

pytestmark = [pytest.mark.asyncio]


async def test_iterator_is_empty_if_there_are_no_data(domain):
    records = []
    async for chunk in domain.iter_subscriptions_for_export(2):
        records.extend(chunk)

    assert len(records) == 0


async def test_returns_all_records(factory, domain, facade):
    for i in range(5):
        await factory.create_subscription(biz_id=i)
    facade.list_coupons_statuses.coro.return_value = [
        dict(biz_id=biz_id, coupon_id=456, status=CouponStatus.RUNNING)
        for biz_id in range(5)
    ]

    records = []
    async for chunk in domain.iter_subscriptions_for_export(2):
        records.extend(chunk)

    assert len(records) == 5


@pytest.mark.parametrize("size", range(1, 2))
async def test_returns_records_by_passed_chunks(factory, domain, facade, size):
    for i in range(10):
        await factory.create_subscription(biz_id=i)
    facade.list_coupons_statuses.coro.return_value = [
        dict(biz_id=biz_id, coupon_id=456, status=CouponStatus.RUNNING)
        for biz_id in range(10)
    ]

    records = []
    async for chunk in domain.iter_subscriptions_for_export(size):
        records.append(chunk)

    assert len(records[0]) == size
    assert len(records[1]) == size


async def test_returns_all_expected_columns(factory, domain, facade):
    subscription_id = await factory.create_subscription()
    facade.list_coupons_statuses.coro.return_value = [
        dict(biz_id=123, coupon_id=456, status=CouponStatus.RUNNING)
    ]

    records = []
    async for _recs in domain.iter_subscriptions_for_export(2):
        records.extend(_recs)

    assert records[0] == dict(
        subscription_id=subscription_id,
        scenario_code="DISCOUNT_FOR_LOST",
        segments=["LOST"],
        biz_id=123,
        coupon_id=456,
    )


@pytest.mark.parametrize(
    "status", (SubscriptionStatus.COMPLETED, SubscriptionStatus.PAUSED)
)
async def test_skips_inactive_subscriptions(factory, domain, facade, status):
    await factory.create_subscription(status=status)
    facade.list_coupons_statuses.coro.return_value = [
        dict(biz_id=123, coupon_id=456, status=CouponStatus.RUNNING)
    ]

    records = []
    async for _recs in domain.iter_subscriptions_for_export(2):
        records.extend(_recs)

    assert len(records) == 0


async def test_skips_subscription_with_unknown_coupon(factory, domain, facade):
    await factory.create_subscription()
    facade.list_coupons_statuses.coro.return_value = []

    records = []
    async for _recs in domain.iter_subscriptions_for_export(2):
        records.extend(_recs)

    assert records == []


async def test_logs_subscription_with_unknown_coupon(
    factory, domain, facade, caplog
):
    await factory.create_subscription(subscription_id=1, biz_id=111, coupon_id=888)
    await factory.create_subscription(subscription_id=2, biz_id=222, coupon_id=999)
    facade.list_coupons_statuses.coro.return_value = []

    records = []
    async for _recs in domain.iter_subscriptions_for_export(2):
        records.extend(_recs)

    assert records == []

    warning_messages = [r for r in caplog.records if r.levelname == "WARNING"]

    assert len(warning_messages) == 1
    assert (
        warning_messages[0].message == "Unknown coupons for facade service: "
        "[{'subscription_id': 1, 'coupon_id': 888}, "
        "{'subscription_id': 2, 'coupon_id': 999}]"
    )


@pytest.mark.parametrize(
    "coupon_status",
    [status for status in CouponStatus if status != CouponStatus.RUNNING],
)
async def test_skips_subscription_with_not_active_coupon(
    factory, domain, facade, coupon_status
):
    await factory.create_subscription()
    facade.list_coupons_statuses.coro.return_value = [
        dict(biz_id=123, coupon_id=456, status=coupon_status)
    ]

    records = []
    async for _recs in domain.iter_subscriptions_for_export(2):
        records.extend(_recs)

    assert records == []


@pytest.mark.parametrize(
    "coupon_status",
    [status for status in CouponStatus if status != CouponStatus.FINISHED],
)
async def test_does_not_change_status_of_subscription_with_not_completed_coupon(
    factory, con, domain, facade, coupon_status
):
    subscription_id = await factory.create_subscription()
    facade.list_coupons_statuses.coro.return_value = [
        dict(biz_id=123, coupon_id=456, status=coupon_status)
    ]

    records = []
    async for _recs in domain.iter_subscriptions_for_export(2):
        records.extend(_recs)

    subscription_status = await con.fetchval(
        "SELECT status FROM subscriptions WHERE id=$1", subscription_id
    )
    assert subscription_status == SubscriptionStatus.ACTIVE


async def test_completes_subscription_with_completed_coupon(
    factory, con, domain, facade
):
    subscription_id = await factory.create_subscription(
        status=SubscriptionStatus.ACTIVE
    )
    facade.list_coupons_statuses.coro.return_value = [
        dict(biz_id=123, coupon_id=456, status=CouponStatus.FINISHED)
    ]

    records = []
    async for _recs in domain.iter_subscriptions_for_export(2):
        records.extend(_recs)

    subscription_status = await con.fetchval(
        "SELECT status FROM subscriptions WHERE id=$1", subscription_id
    )
    assert subscription_status == SubscriptionStatus.COMPLETED


async def test_create_version_for_subscription_with_completed_coupon(
    factory, con, domain, facade
):
    subscription_id = await factory.create_subscription(
        status=SubscriptionStatus.ACTIVE
    )
    facade.list_coupons_statuses.coro.return_value = [
        dict(biz_id=123, coupon_id=456, status=CouponStatus.FINISHED)
    ]

    records = []
    async for _recs in domain.iter_subscriptions_for_export(2):
        records.extend(_recs)

    versions = await factory.retrieve_subscription_versions(subscription_id)
    assert versions == [
        dict(
            id=Any(int),
            subscription_id=subscription_id,
            status=SubscriptionStatus.COMPLETED,
            coupon_id=456,
            created_at=Any(datetime),
        ),
        dict(
            id=Any(int),
            subscription_id=subscription_id,
            status=SubscriptionStatus.ACTIVE,
            coupon_id=456,
            created_at=Any(datetime),
        ),
    ]
