from datetime import datetime

import pytest

from maps_adv.common.helpers import Any
from maps_adv.geosmb.scenarist.proto.errors_pb2 import Error
from maps_adv.geosmb.scenarist.proto.scenarios_pb2 import (
    CreateSubscriptionInput,
    ScenarioName,
    Subscription,
    SubscriptionStatus,
)
from maps_adv.geosmb.scenarist.server.lib.enums import (
    ScenarioName as ScenarioNameEnum,
    SubscriptionStatus as SubscriptionStatusEnum,
)
from maps_adv.geosmb.scenarist.server.tests.utils import ENUM_MAPS_TO_PB

pytestmark = [pytest.mark.asyncio]


url = "/v1/create_subscription/"


@pytest.mark.parametrize(
    "optional_params", (dict(coupon_id=564000), dict(coupon_id=None))
)
async def test_creates_subscription(api, factory, optional_params):
    await api.post(
        url,
        proto=CreateSubscriptionInput(
            biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST, **optional_params
        ),
        expected_status=201,
    )

    subscriptions = await factory.retrieve_business_subscriptions(biz_id=123)
    assert subscriptions == [
        dict(
            id=Any(int),
            biz_id=123,
            scenario_name=ScenarioNameEnum.DISCOUNT_FOR_LOST,
            status=SubscriptionStatusEnum.ACTIVE,
            **optional_params,
        )
    ]


@pytest.mark.parametrize(
    "optional_params", (dict(coupon_id=564000), dict(coupon_id=None))
)
async def test_creates_subscription_version(api, factory, optional_params):
    got = await api.post(
        url,
        proto=CreateSubscriptionInput(
            biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST, **optional_params
        ),
        decode_as=Subscription,
        expected_status=201,
    )

    sub_versions = await factory.retrieve_subscription_versions(
        subscription_id=got.subscription_id
    )
    assert sub_versions == [
        dict(
            id=Any(int),
            subscription_id=got.subscription_id,
            status=SubscriptionStatusEnum.ACTIVE,
            created_at=Any(datetime),
            **optional_params,
        )
    ]


@pytest.mark.parametrize(
    "optional_params", (dict(coupon_id=564000), dict(coupon_id=None))
)
async def test_returns_subscription_data(api, optional_params):
    got = await api.post(
        url,
        proto=CreateSubscriptionInput(
            biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST, **optional_params
        ),
        decode_as=Subscription,
        expected_status=201,
    )

    assert got == Subscription(
        subscription_id=got.subscription_id,
        biz_id=123,
        scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
        status=SubscriptionStatus.ACTIVE,
        **optional_params,
    )


async def test_can_create_multiple_subscriptions_for_one_business(api, factory):
    for scenario_name in (
        ScenarioName.DISCOUNT_FOR_LOST,
        ScenarioName.ENGAGE_PROSPECTIVE,
        ScenarioName.THANK_THE_LOYAL,
        ScenarioName.DISCOUNT_FOR_DISLOYAL,
    ):
        await api.post(
            url,
            proto=CreateSubscriptionInput(
                biz_id=123, scenario_name=scenario_name, coupon_id=564000
            ),
            expected_status=201,
        )

    got = await factory.retrieve_business_subscriptions(biz_id=123)

    assert [s["scenario_name"] for s in got] == [
        ScenarioNameEnum.DISCOUNT_FOR_LOST,
        ScenarioNameEnum.ENGAGE_PROSPECTIVE,
        ScenarioNameEnum.THANK_THE_LOYAL,
        ScenarioNameEnum.DISCOUNT_FOR_DISLOYAL,
    ]


@pytest.mark.parametrize("scenario_name", [n for n in ScenarioNameEnum])
async def test_returns_error_if_subscription_already_exists(
    scenario_name, api, factory
):
    await factory.create_subscription(scenario_name=scenario_name)

    got = await api.post(
        url,
        proto=CreateSubscriptionInput(
            biz_id=123,
            scenario_name=ENUM_MAPS_TO_PB["scenario_name"][scenario_name],
            coupon_id=564000,
        ),
        decode_as=Error,
        expected_status=409,
    )

    assert got == Error(code=Error.SUBSCRIPTION_ALREADY_EXISTS)
