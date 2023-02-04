import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.scenarist.proto.errors_pb2 import Error
from maps_adv.geosmb.scenarist.proto.scenarios_pb2 import (
    Coupon,
    CouponStatistics,
    CouponStatisticsLine,
    RetrieveSubscriptionInput,
    RetrieveSubscriptionOutput,
    ScenarioName,
    Subscription,
    SubscriptionStatus,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time(dt("2020-01-01 00:00:00"))]


url = "/v1/retrieve_subscription/"


async def test_returns_subscription_data(api, factory):
    sub_id = await factory.create_subscription()

    got = await api.post(
        url,
        proto=RetrieveSubscriptionInput(subscription_id=sub_id, biz_id=123),
        decode_as=RetrieveSubscriptionOutput,
        expected_status=200,
    )

    assert got == RetrieveSubscriptionOutput(
        subscription=Subscription(
            subscription_id=sub_id,
            biz_id=123,
            scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
            status=SubscriptionStatus.ACTIVE,
            coupon_id=456,
        ),
        coupons_history=[
            Coupon(
                created_at=dt("2020-01-01 00:00:00", as_proto=True),
                coupon_id=456,
                statistics=CouponStatistics(
                    total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
                ),
            )
        ],
    )


async def test_returns_zeros_stat_if_coupon_has_no_stat(api, factory):
    sub_id = await factory.create_subscription(coupon_id=456)

    got = await api.post(
        url,
        proto=RetrieveSubscriptionInput(subscription_id=sub_id, biz_id=123),
        decode_as=RetrieveSubscriptionOutput,
        expected_status=200,
    )

    assert list(got.coupons_history) == [
        Coupon(
            created_at=dt("2020-01-01 00:00:00", as_proto=True),
            coupon_id=456,
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        )
    ]


async def test_returns_total_coupon_stats(api, factory):
    sub_id = await factory.create_subscription()
    await factory.create_certificate_mailing_stats(sent=5, clicked=4, opened=3)
    await factory.create_certificate_mailing_stats(sent=10, clicked=5, opened=1)
    await factory.create_certificate_mailing_stats(sent=20, clicked=8, opened=0)

    got = await api.post(
        url,
        proto=RetrieveSubscriptionInput(subscription_id=sub_id, biz_id=123),
        decode_as=RetrieveSubscriptionOutput,
        expected_status=200,
    )

    assert list(got.coupons_history) == [
        Coupon(
            created_at=dt("2020-01-01 00:00:00", as_proto=True),
            coupon_id=456,
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=35, clicked=17, opened=4)
            ),
        )
    ]


async def test_does_not_return_another_coupon_stat(api, factory):
    sub_id = await factory.create_subscription(coupon_id=456)
    await factory.create_certificate_mailing_stats(coupon_id=999)

    got = await api.post(
        url,
        proto=RetrieveSubscriptionInput(subscription_id=sub_id, biz_id=123),
        decode_as=RetrieveSubscriptionOutput,
        expected_status=200,
    )

    assert list(got.coupons_history) == [
        Coupon(
            created_at=dt("2020-01-01 00:00:00", as_proto=True),
            coupon_id=456,
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        )
    ]


async def test_returns_stat_for_each_coupon(factory, api):
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

    got = await api.post(
        url,
        proto=RetrieveSubscriptionInput(subscription_id=sub_id, biz_id=123),
        decode_as=RetrieveSubscriptionOutput,
        expected_status=200,
    )

    assert list(got.coupons_history) == [
        Coupon(
            coupon_id=222,
            created_at=dt("2020-02-02 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=10, clicked=20, opened=30)
            ),
        ),
        Coupon(
            coupon_id=111,
            created_at=dt("2020-01-01 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=1, clicked=2, opened=3)
            ),
        ),
    ]


async def test_returns_coupons_ordered_by_created_at_desc(api, factory):
    sub_id = await factory.create_subscription(
        coupon_id=111, created_at=dt("2018-01-01 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=222, created_at=dt("2020-01-01 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=333, created_at=dt("2019-01-01 00:00:00")
    )

    got = await api.post(
        url,
        proto=RetrieveSubscriptionInput(subscription_id=sub_id, biz_id=123),
        decode_as=RetrieveSubscriptionOutput,
        expected_status=200,
    )

    assert list(got.coupons_history) == [
        Coupon(
            coupon_id=222,
            created_at=dt("2020-01-01 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        ),
        Coupon(
            coupon_id=333,
            created_at=dt("2019-01-01 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        ),
        Coupon(
            coupon_id=111,
            created_at=dt("2018-01-01 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        ),
    ]


async def test_returns_history_with_no_coupon_items(api, factory):
    sub_id = await factory.create_subscription(
        coupon_id=None, created_at=dt("2020-01-01 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=111, created_at=dt("2020-02-02 00:00:00")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, coupon_id=None, created_at=dt("2020-03-03 00:00:00")
    )

    got = await api.post(
        url,
        proto=RetrieveSubscriptionInput(subscription_id=sub_id, biz_id=123),
        decode_as=RetrieveSubscriptionOutput,
        expected_status=200,
    )

    assert list(got.coupons_history) == [
        Coupon(
            coupon_id=None,
            created_at=dt("2020-03-03 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        ),
        Coupon(
            coupon_id=111,
            created_at=dt("2020-02-02 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        ),
        Coupon(
            coupon_id=None,
            created_at=dt("2020-01-01 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        ),
    ]


async def test_returns_history_only_with_coupon_changes(api, factory):
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

    got = await api.post(
        url,
        proto=RetrieveSubscriptionInput(subscription_id=sub_id, biz_id=123),
        decode_as=RetrieveSubscriptionOutput,
        expected_status=200,
    )

    assert list(got.coupons_history) == [
        Coupon(
            coupon_id=None,
            created_at=dt("2020-06-06 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        ),
        Coupon(
            coupon_id=222,
            created_at=dt("2020-03-03 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        ),
        Coupon(
            coupon_id=111,
            created_at=dt("2020-01-01 00:00:00", as_proto=True),
            statistics=CouponStatistics(
                total=CouponStatisticsLine(sent=0, clicked=0, opened=0)
            ),
        ),
    ]


async def test_returns_error_if_subscription_not_found(api):
    got = await api.post(
        url,
        proto=RetrieveSubscriptionInput(subscription_id=999, biz_id=123),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(
        code=Error.UNKNOWN_SUBSCRIPTION, description="subscription_id=999, biz_id=123"
    )


async def test_returns_error_if_subscription_belongs_to_another_biz_id(api, factory):
    sub_id = await factory.create_subscription(biz_id=564)

    got = await api.post(
        url,
        proto=RetrieveSubscriptionInput(subscription_id=sub_id, biz_id=123),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(
        code=Error.UNKNOWN_SUBSCRIPTION,
        description=f"subscription_id={sub_id}, biz_id=123",
    )
