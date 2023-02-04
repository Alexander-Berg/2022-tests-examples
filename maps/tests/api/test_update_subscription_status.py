from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.scenarist.proto.errors_pb2 import Error
from maps_adv.geosmb.scenarist.proto.scenarios_pb2 import UpdateSubscriptionStatusInput
from maps_adv.geosmb.scenarist.server.lib.enums import (
    SubscriptionStatus as SubscriptionStatusEnum,
)

pytestmark = [pytest.mark.asyncio]


url = "/v1/update_subscription_status/"


@pytest.mark.parametrize(
    "current_status, status_for_update, expected_status",
    [
        (
            SubscriptionStatusEnum.ACTIVE,
            UpdateSubscriptionStatusInput.StatusForUpdate.PAUSED,
            SubscriptionStatusEnum.PAUSED,
        ),
        (
            SubscriptionStatusEnum.PAUSED,
            UpdateSubscriptionStatusInput.StatusForUpdate.ACTIVE,
            SubscriptionStatusEnum.ACTIVE,
        ),
        (
            SubscriptionStatusEnum.COMPLETED,
            UpdateSubscriptionStatusInput.StatusForUpdate.ACTIVE,
            SubscriptionStatusEnum.ACTIVE,
        ),
        (
            SubscriptionStatusEnum.COMPLETED,
            UpdateSubscriptionStatusInput.StatusForUpdate.PAUSED,
            SubscriptionStatusEnum.PAUSED,
        ),
    ],
)
async def test_updates_subscription_status(
    api, factory, current_status, status_for_update, expected_status
):
    sub_id = await factory.create_subscription(status=current_status)

    await api.post(
        url,
        proto=UpdateSubscriptionStatusInput(
            subscription_id=sub_id, biz_id=123, status=status_for_update
        ),
        expected_status=200,
    )

    subscription = await factory.retrieve_subscription(subscription_id=sub_id)
    assert subscription["status"] == expected_status


async def test_returns_nothing(api, factory):
    sub_id = await factory.create_subscription()

    got = await api.post(
        url,
        proto=UpdateSubscriptionStatusInput(
            subscription_id=sub_id,
            biz_id=123,
            status=UpdateSubscriptionStatusInput.StatusForUpdate.PAUSED,
        ),
        expected_status=200,
    )

    assert got == b""


@pytest.mark.parametrize(
    "current_status, status_for_update, expected_status",
    [
        (
            SubscriptionStatusEnum.ACTIVE,
            UpdateSubscriptionStatusInput.StatusForUpdate.PAUSED,
            SubscriptionStatusEnum.PAUSED,
        ),
        (
            SubscriptionStatusEnum.PAUSED,
            UpdateSubscriptionStatusInput.StatusForUpdate.ACTIVE,
            SubscriptionStatusEnum.ACTIVE,
        ),
        (
            SubscriptionStatusEnum.COMPLETED,
            UpdateSubscriptionStatusInput.StatusForUpdate.ACTIVE,
            SubscriptionStatusEnum.ACTIVE,
        ),
        (
            SubscriptionStatusEnum.COMPLETED,
            UpdateSubscriptionStatusInput.StatusForUpdate.PAUSED,
            SubscriptionStatusEnum.PAUSED,
        ),
    ],
)
async def test_creates_subscription_version(
    api, factory, current_status, status_for_update, expected_status
):
    sub_id = await factory.create_subscription(status=current_status)

    await api.post(
        url,
        proto=UpdateSubscriptionStatusInput(
            subscription_id=sub_id,
            biz_id=123,
            status=status_for_update,
        ),
        expected_status=200,
    )

    sub_versions = await factory.retrieve_subscription_versions(subscription_id=sub_id)
    assert sub_versions[0] == dict(
        id=Any(int),
        subscription_id=sub_id,
        status=expected_status,
        coupon_id=456,
        created_at=Any(datetime),
    )


@pytest.mark.parametrize(
    "current_status, updated_status",
    [
        (
            SubscriptionStatusEnum.ACTIVE,
            UpdateSubscriptionStatusInput.StatusForUpdate.ACTIVE,
        ),
        (
            SubscriptionStatusEnum.PAUSED,
            UpdateSubscriptionStatusInput.StatusForUpdate.PAUSED,
        ),
    ],
)
async def test_returns_error_if_duplicates_current_status(
    api, factory, current_status, updated_status
):
    sub_id = await factory.create_subscription(status=current_status)

    got = await api.post(
        url,
        proto=UpdateSubscriptionStatusInput(
            subscription_id=sub_id, biz_id=123, status=updated_status
        ),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.CURRENT_STATUS_DUPLICATE)


async def test_returns_error_if_subscription_not_found(api):
    got = await api.post(
        url,
        proto=UpdateSubscriptionStatusInput(
            subscription_id=999,
            biz_id=123,
            status=UpdateSubscriptionStatusInput.StatusForUpdate.PAUSED,
        ),
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
        proto=UpdateSubscriptionStatusInput(
            subscription_id=sub_id,
            biz_id=123,
            status=UpdateSubscriptionStatusInput.StatusForUpdate.PAUSED,
        ),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(
        code=Error.UNKNOWN_SUBSCRIPTION,
        description=f"subscription_id={sub_id}, biz_id=123",
    )
