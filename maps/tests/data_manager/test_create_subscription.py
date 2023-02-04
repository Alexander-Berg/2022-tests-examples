from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName, SubscriptionStatus
from maps_adv.geosmb.scenarist.server.lib.exceptions import SubscriptionAlreadyExists

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "optional_params", (dict(coupon_id=564000), dict(coupon_id=None))
)
async def test_creates_subscription(dm, factory, optional_params):
    await dm.create_subscription(
        biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST, **optional_params
    )

    subscriptions = await factory.retrieve_business_subscriptions(biz_id=123)
    assert subscriptions == [
        dict(
            id=Any(int),
            biz_id=123,
            scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
            status=SubscriptionStatus.ACTIVE,
            **optional_params,
        )
    ]


@pytest.mark.parametrize(
    "optional_params", (dict(coupon_id=564000), dict(coupon_id=None))
)
async def test_creates_subscription_version(dm, factory, optional_params, con):
    got = await dm.create_subscription(
        biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST, **optional_params
    )

    sub_versions = await factory.retrieve_subscription_versions(
        subscription_id=got["subscription_id"]
    )
    assert sub_versions == [
        dict(
            id=Any(int),
            subscription_id=got["subscription_id"],
            status=SubscriptionStatus.ACTIVE,
            created_at=Any(datetime),
            **optional_params,
        )
    ]


@pytest.mark.parametrize(
    "optional_params", (dict(coupon_id=564000), dict(coupon_id=None))
)
async def test_returns_subscription_data(dm, optional_params):
    got = await dm.create_subscription(
        biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST, **optional_params
    )

    assert got == dict(
        subscription_id=Any(int),
        biz_id=123,
        scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
        status=SubscriptionStatus.ACTIVE,
        **optional_params,
    )


async def test_can_create_multiple_subscriptions_for_one_business(dm, factory):
    for scenario_name in ScenarioName:
        await dm.create_subscription(
            biz_id=123, scenario_name=scenario_name, coupon_id=564000
        )

    got = await factory.retrieve_business_subscriptions(biz_id=123)

    assert [s["scenario_name"] for s in got] == [
        ScenarioName.DISCOUNT_FOR_LOST,
        ScenarioName.ENGAGE_PROSPECTIVE,
        ScenarioName.THANK_THE_LOYAL,
        ScenarioName.DISCOUNT_FOR_DISLOYAL,
    ]


@pytest.mark.parametrize("scenario_name", [n for n in ScenarioName])
async def test_raises_if_subscription_already_exists(scenario_name, dm, factory):
    await factory.create_subscription(scenario_name=scenario_name)

    with pytest.raises(SubscriptionAlreadyExists):
        await dm.create_subscription(
            biz_id=123, scenario_name=scenario_name, coupon_id=564000
        )
