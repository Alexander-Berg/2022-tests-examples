import pytest

from maps_adv.geosmb.scenarist.server.lib.enums import (
    ScenarioName,
    SegmentType,
    SubscriptionStatus,
)

pytestmark = [pytest.mark.asyncio]


async def test_returns_all_scenarios_with_subscription_details(factory, dm):
    sub_id = await factory.create_subscription()

    got = await dm.list_scenarios(biz_id=123)

    assert got == [
        dict(
            name=ScenarioName.DISCOUNT_FOR_LOST,
            segments=[SegmentType.LOST],
            subscription=dict(
                subscription_id=sub_id,
                biz_id=123,
                scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
                status=SubscriptionStatus.ACTIVE,
                coupon_id=456,
            ),
        ),
        dict(
            name=ScenarioName.ENGAGE_PROSPECTIVE,
            segments=[SegmentType.NO_ORDERS, SegmentType.PROSPECTIVE],
        ),
        dict(name=ScenarioName.THANK_THE_LOYAL, segments=[SegmentType.LOYAL]),
        dict(name=ScenarioName.DISCOUNT_FOR_DISLOYAL, segments=[SegmentType.DISLOYAL]),
    ]


@pytest.mark.parametrize(
    "scenario_name, expected_segments",
    [
        (ScenarioName.DISCOUNT_FOR_LOST, [SegmentType.LOST]),
        (
            ScenarioName.ENGAGE_PROSPECTIVE,
            [SegmentType.NO_ORDERS, SegmentType.PROSPECTIVE],
        ),
        (ScenarioName.THANK_THE_LOYAL, [SegmentType.LOYAL]),
        (ScenarioName.DISCOUNT_FOR_DISLOYAL, [SegmentType.DISLOYAL]),
    ],
)
@pytest.mark.parametrize("status", [s for s in SubscriptionStatus])
@pytest.mark.parametrize("coupon_id", (456, None))
async def test_returns_subscription_details(
    dm, factory, scenario_name, expected_segments, status, coupon_id
):
    sub_id = await factory.create_subscription(
        scenario_name=scenario_name, status=status, coupon_id=coupon_id
    )

    got = await dm.list_scenarios(biz_id=123)

    scenarios = [st for st in got if st["name"] == scenario_name]
    assert scenarios == [
        dict(
            name=scenario_name,
            segments=expected_segments,
            subscription=dict(
                subscription_id=sub_id,
                biz_id=123,
                scenario_name=scenario_name,
                status=status,
                coupon_id=coupon_id,
            ),
        )
    ]


async def test_returns_subscriptions_only_for_subscribed_scenarios(dm, factory):
    await factory.create_subscription(scenario_name=ScenarioName.DISCOUNT_FOR_LOST)
    await factory.create_subscription(scenario_name=ScenarioName.THANK_THE_LOYAL)

    got = await dm.list_scenarios(biz_id=123)

    assert [t["name"] for t in got if "subscription" in t] == [
        ScenarioName.DISCOUNT_FOR_LOST,
        ScenarioName.THANK_THE_LOYAL,
    ]


async def test_does_not_return_subscriptions_for_not_subscribed_scenarios(dm, factory):
    await factory.create_subscription(scenario_name=ScenarioName.DISCOUNT_FOR_LOST)

    got = await dm.list_scenarios(biz_id=123)

    assert [t["name"] for t in got if "subscription" not in t] == [
        ScenarioName.ENGAGE_PROSPECTIVE,
        ScenarioName.THANK_THE_LOYAL,
        ScenarioName.DISCOUNT_FOR_DISLOYAL,
    ]


async def test_does_not_return_other_business_subscriptions(dm, factory):
    await factory.create_subscription(
        biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST
    )
    await factory.create_subscription(
        biz_id=999, scenario_name=ScenarioName.THANK_THE_LOYAL
    )

    got = await dm.list_scenarios(biz_id=123)

    assert [t["name"] for t in got if "subscription" in t] == [
        ScenarioName.DISCOUNT_FOR_LOST
    ]
