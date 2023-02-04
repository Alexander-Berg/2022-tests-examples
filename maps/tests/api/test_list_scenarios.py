import pytest

from maps_adv.geosmb.scenarist.proto.scenarios_pb2 import (
    ListScenariosInput,
    ListScenariosOutput,
    Scenario,
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


url = "/v1/list_scenarios/"


async def test_returns_all_scenarios_with_subscription_details(factory, api):
    sub_id = await factory.create_subscription()

    got = await api.post(
        url,
        proto=ListScenariosInput(biz_id=123),
        decode_as=ListScenariosOutput,
        expected_status=200,
    )

    assert got == ListScenariosOutput(
        scenarios=[
            Scenario(
                name=ScenarioName.DISCOUNT_FOR_LOST,
                segments=[Scenario.LOST],
                subscription=Subscription(
                    subscription_id=sub_id,
                    biz_id=123,
                    scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
                    status=SubscriptionStatus.ACTIVE,
                    coupon_id=456,
                ),
            ),
            Scenario(
                name=ScenarioName.ENGAGE_PROSPECTIVE,
                segments=[Scenario.NO_ORDERS, Scenario.PROSPECTIVE],
            ),
            Scenario(name=ScenarioName.THANK_THE_LOYAL, segments=[Scenario.LOYAL]),
            Scenario(
                name=ScenarioName.DISCOUNT_FOR_DISLOYAL, segments=[Scenario.DISLOYAL]
            ),
        ]
    )


@pytest.mark.parametrize(
    "scenario_name, expected_segments",
    [
        (ScenarioNameEnum.DISCOUNT_FOR_LOST, [Scenario.LOST]),
        (
            ScenarioNameEnum.ENGAGE_PROSPECTIVE,
            [Scenario.NO_ORDERS, Scenario.PROSPECTIVE],
        ),
        (ScenarioNameEnum.THANK_THE_LOYAL, [Scenario.LOYAL]),
        (ScenarioNameEnum.DISCOUNT_FOR_DISLOYAL, [Scenario.DISLOYAL]),
    ],
)
@pytest.mark.parametrize("status", [s for s in SubscriptionStatusEnum])
@pytest.mark.parametrize("coupon_id", (456, None))
async def test_returns_subscription_details(
    api, factory, scenario_name, expected_segments, status, coupon_id
):
    sub_id = await factory.create_subscription(
        scenario_name=scenario_name, status=status, coupon_id=coupon_id
    )

    got = await api.post(
        url,
        proto=ListScenariosInput(biz_id=123),
        decode_as=ListScenariosOutput,
        expected_status=200,
    )

    scenario_name_pb = ENUM_MAPS_TO_PB["scenario_name"][scenario_name]
    scenarios = [st for st in got.scenarios if st.name == scenario_name_pb]
    assert scenarios == [
        Scenario(
            name=scenario_name_pb,
            segments=expected_segments,
            subscription=Subscription(
                subscription_id=sub_id,
                biz_id=123,
                scenario_name=scenario_name_pb,
                status=ENUM_MAPS_TO_PB["subscription_status"][status],
                coupon_id=coupon_id,
            ),
        )
    ]


async def test_returns_subscriptions_only_for_subscribed_scenarios(api, factory):
    await factory.create_subscription(scenario_name=ScenarioNameEnum.DISCOUNT_FOR_LOST)
    await factory.create_subscription(scenario_name=ScenarioNameEnum.THANK_THE_LOYAL)

    got = await api.post(
        url,
        proto=ListScenariosInput(biz_id=123),
        decode_as=ListScenariosOutput,
        expected_status=200,
    )

    assert [t.name for t in got.scenarios if t.HasField("subscription")] == [
        ScenarioName.DISCOUNT_FOR_LOST,
        ScenarioName.THANK_THE_LOYAL,
    ]


async def test_does_not_return_subscriptions_for_not_subscribed_scenarios(api, factory):
    await factory.create_subscription(scenario_name=ScenarioNameEnum.DISCOUNT_FOR_LOST)

    got = await api.post(
        url,
        proto=ListScenariosInput(biz_id=123),
        decode_as=ListScenariosOutput,
        expected_status=200,
    )

    assert [t.name for t in got.scenarios if not t.HasField("subscription")] == [
        ScenarioName.ENGAGE_PROSPECTIVE,
        ScenarioName.THANK_THE_LOYAL,
        ScenarioName.DISCOUNT_FOR_DISLOYAL,
    ]


async def test_does_not_return_other_business_subscriptions(api, factory):
    await factory.create_subscription(
        biz_id=123, scenario_name=ScenarioNameEnum.DISCOUNT_FOR_LOST
    )
    await factory.create_subscription(
        biz_id=999, scenario_name=ScenarioNameEnum.THANK_THE_LOYAL
    )

    got = await api.post(
        url,
        proto=ListScenariosInput(biz_id=123),
        decode_as=ListScenariosOutput,
        expected_status=200,
    )

    assert [t.name for t in got.scenarios if t.HasField("subscription")] == [
        ScenarioName.DISCOUNT_FOR_LOST
    ]
