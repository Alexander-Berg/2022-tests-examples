import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName, SubscriptionStatus
from maps_adv.geosmb.scenarist.server.lib.exceptions import UnknownSubscription

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time(dt("2020-01-01 00:00:00"))]


async def test_returns_subscription_data(dm, factory):
    sub_id = await factory.create_subscription()

    got = await dm.retrieve_subscription(subscription_id=sub_id, biz_id=123)

    assert got == dict(
        subscription=dict(
            subscription_id=sub_id,
            biz_id=123,
            scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
            status=SubscriptionStatus.ACTIVE,
            coupon_id=456,
        ),
        coupons_history=[
            dict(
                coupon_id=456,
                created_at=dt("2020-01-01 00:00:00"),
                statistics={"sent": 0, "clicked": 0, "opened": 0},
            )
        ],
    )


async def test_returns_zeros_stat_if_coupon_has_no_stat(dm, factory):
    sub_id = await factory.create_subscription(coupon_id=456)

    got = await dm.retrieve_subscription(subscription_id=sub_id, biz_id=123)

    assert got["coupons_history"] == [
        dict(
            coupon_id=456,
            created_at=dt("2020-01-01 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        )
    ]


async def test_returns_total_coupon_stats(dm, factory):
    sub_id = await factory.create_subscription()
    await factory.create_certificate_mailing_stats(sent=5, clicked=4, opened=3)
    await factory.create_certificate_mailing_stats(sent=10, clicked=5, opened=1)
    await factory.create_certificate_mailing_stats(sent=20, clicked=8, opened=0)

    got = await dm.retrieve_subscription(subscription_id=sub_id, biz_id=123)

    assert got["coupons_history"] == [
        dict(
            coupon_id=456,
            created_at=dt("2020-01-01 00:00:00"),
            statistics={"sent": 35, "clicked": 17, "opened": 4},
        )
    ]


async def test_does_not_return_another_coupon_stat(dm, factory):
    sub_id = await factory.create_subscription(coupon_id=456)
    await factory.create_certificate_mailing_stats(coupon_id=999)

    got = await dm.retrieve_subscription(subscription_id=sub_id, biz_id=123)

    assert got["coupons_history"] == [
        dict(
            coupon_id=456,
            created_at=dt("2020-01-01 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        )
    ]


async def test_returns_stat_for_each_coupon(factory, dm):
    sub_id = await factory.create_subscription(
        coupon_id=111, created_at=dt("2020-01-01 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=222, created_at=dt("2020-02-02 00:00:00")
    )
    await factory.create_certificate_mailing_stats(
        coupon_id=111, sent=1, clicked=2, opened=3
    )
    await factory.create_certificate_mailing_stats(
        coupon_id=222, sent=10, clicked=20, opened=30
    )

    got = await dm.retrieve_subscription(subscription_id=sub_id, biz_id=123)

    assert got["coupons_history"] == [
        dict(
            coupon_id=222,
            created_at=dt("2020-02-02 00:00:00"),
            statistics={"sent": 10, "clicked": 20, "opened": 30},
        ),
        dict(
            coupon_id=111,
            created_at=dt("2020-01-01 00:00:00"),
            statistics={"sent": 1, "clicked": 2, "opened": 3},
        ),
    ]


async def test_returns_coupons_ordered_by_created_at_desc(dm, factory):
    sub_id = await factory.create_subscription(
        coupon_id=111, created_at=dt("2018-01-01 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=222, created_at=dt("2020-01-01 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=333, created_at=dt("2019-01-01 00:00:00")
    )

    got = await dm.retrieve_subscription(subscription_id=sub_id, biz_id=123)

    assert got["coupons_history"] == [
        dict(
            coupon_id=222,
            created_at=dt("2020-01-01 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        ),
        dict(
            coupon_id=333,
            created_at=dt("2019-01-01 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        ),
        dict(
            coupon_id=111,
            created_at=dt("2018-01-01 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        ),
    ]


async def test_returns_history_with_no_coupon_items(dm, factory):
    sub_id = await factory.create_subscription(
        coupon_id=None, created_at=dt("2020-01-01 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=111, created_at=dt("2020-02-02 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=None, created_at=dt("2020-03-03 00:00:00")
    )

    got = await dm.retrieve_subscription(subscription_id=sub_id, biz_id=123)

    assert got["coupons_history"] == [
        dict(
            coupon_id=None,
            created_at=dt("2020-03-03 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        ),
        dict(
            coupon_id=111,
            created_at=dt("2020-02-02 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        ),
        dict(
            coupon_id=None,
            created_at=dt("2020-01-01 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        ),
    ]


async def test_returns_history_only_with_coupon_changes(dm, factory):
    sub_id = await factory.create_subscription(
        coupon_id=111, created_at=dt("2020-01-01 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=111, created_at=dt("2020-02-02 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=222, created_at=dt("2020-03-03 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=222, created_at=dt("2020-04-04 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=222, created_at=dt("2020-05-05 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=None, created_at=dt("2020-06-06 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=None, created_at=dt("2020-07-07 00:00:00")
    )

    got = await dm.retrieve_subscription(subscription_id=sub_id, biz_id=123)

    assert got["coupons_history"] == [
        dict(
            coupon_id=None,
            created_at=dt("2020-06-06 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        ),
        dict(
            coupon_id=222,
            created_at=dt("2020-03-03 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        ),
        dict(
            coupon_id=111,
            created_at=dt("2020-01-01 00:00:00"),
            statistics={"sent": 0, "clicked": 0, "opened": 0},
        ),
    ]


async def test_raises_if_subscription_not_found(dm):
    with pytest.raises(UnknownSubscription):
        await dm.retrieve_subscription(subscription_id=999, biz_id=123)


async def test_raises_if_subscription_belongs_to_another_biz_id(dm, factory):
    sub_id = await factory.create_subscription(biz_id=564)

    with pytest.raises(UnknownSubscription):
        await dm.retrieve_subscription(subscription_id=sub_id, biz_id=123)
