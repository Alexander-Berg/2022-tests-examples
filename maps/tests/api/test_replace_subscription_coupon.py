import pytest

from maps_adv.geosmb.scenarist.proto.errors_pb2 import Error
from maps_adv.geosmb.scenarist.proto.scenarios_pb2 import (
    ReplaceCouponInput,
    ReplaceCouponOutput,
    SubscriptionStatus,
)
from maps_adv.geosmb.scenarist.server.lib.enums import (
    SubscriptionStatus as SubscriptionStatusEnum,
)

pytestmark = [pytest.mark.asyncio]


url = "/v1/replace_subscription_coupon/"


@pytest.mark.parametrize("coupon_id", [888, None])
async def test_creates_subscription_version(api, factory, coupon_id):
    sub_id = await factory.create_subscription(coupon_id=456)

    await api.post(
        url,
        proto=ReplaceCouponInput(
            subscription_id=sub_id, biz_id=123, coupon_id=coupon_id
        ),
        expected_status=200,
    )

    versions = await factory.retrieve_subscription_versions(subscription_id=sub_id)
    assert [version["coupon_id"] for version in versions] == [coupon_id, 456]


@pytest.mark.parametrize("current_status", [s for s in SubscriptionStatusEnum])
async def test_updates_subscription_status_to_active_if_coupon_is_set(
    api, factory, current_status
):
    sub_id = await factory.create_subscription(status=current_status)

    await api.post(
        url,
        proto=ReplaceCouponInput(subscription_id=sub_id, biz_id=123, coupon_id=888),
        expected_status=200,
    )

    subscription = await factory.retrieve_subscription(subscription_id=sub_id)
    versions = await factory.retrieve_subscription_versions(subscription_id=sub_id)
    assert subscription["status"] == SubscriptionStatusEnum.ACTIVE
    assert versions[0]["status"] == SubscriptionStatusEnum.ACTIVE


@pytest.mark.parametrize("current_status", [s for s in SubscriptionStatusEnum])
async def test_updates_subscription_status_to_complete_if_no_coupon(
    api, factory, current_status
):
    sub_id = await factory.create_subscription(status=current_status)

    await api.post(
        url,
        proto=ReplaceCouponInput(subscription_id=sub_id, biz_id=123),
        expected_status=200,
    )

    subscription = await factory.retrieve_subscription(subscription_id=sub_id)
    versions = await factory.retrieve_subscription_versions(subscription_id=sub_id)
    assert subscription["status"] == SubscriptionStatusEnum.COMPLETED
    assert versions[0]["status"] == SubscriptionStatusEnum.COMPLETED


@pytest.mark.parametrize("coupon_id", [None, 888])
async def test_updates_subscription_current_coupon_id(api, factory, coupon_id):
    sub_id = await factory.create_subscription()

    await api.post(
        url,
        proto=ReplaceCouponInput(
            subscription_id=sub_id, biz_id=123, coupon_id=coupon_id
        ),
        expected_status=200,
    )

    subscription = await factory.retrieve_subscription(subscription_id=sub_id)
    assert subscription["coupon_id"] == coupon_id


@pytest.mark.parametrize("current_status", [s for s in SubscriptionStatusEnum])
async def test_returns_active_status_if_coupon_is_set(api, factory, current_status):
    sub_id = await factory.create_subscription(status=current_status)

    got = await api.post(
        url,
        proto=ReplaceCouponInput(subscription_id=sub_id, biz_id=123, coupon_id=888),
        decode_as=ReplaceCouponOutput,
        expected_status=200,
    )

    assert got == ReplaceCouponOutput(status=SubscriptionStatus.ACTIVE)


@pytest.mark.parametrize("current_status", [s for s in SubscriptionStatusEnum])
async def test_returns_completed_status_if_no_coupon(api, factory, current_status):
    sub_id = await factory.create_subscription(status=current_status)

    got = await api.post(
        url,
        proto=ReplaceCouponInput(subscription_id=sub_id, biz_id=123),
        decode_as=ReplaceCouponOutput,
        expected_status=200,
    )

    assert got == ReplaceCouponOutput(status=SubscriptionStatus.COMPLETED)


async def test_returns_error_if_subscription_not_found(api):
    got = await api.post(
        url,
        proto=ReplaceCouponInput(subscription_id=999, biz_id=123, coupon_id=888),
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
        proto=ReplaceCouponInput(subscription_id=sub_id, biz_id=123, coupon_id=888),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(
        code=Error.UNKNOWN_SUBSCRIPTION,
        description=f"subscription_id={sub_id}, biz_id=123",
    )


async def test_raises_if_duplicates_current_coupon(api, factory):
    sub_id = await factory.create_subscription(coupon_id=456)

    got = await api.post(
        url,
        proto=ReplaceCouponInput(subscription_id=sub_id, biz_id=123, coupon_id=456),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.CURRENT_COUPON_DUPLICATE)
