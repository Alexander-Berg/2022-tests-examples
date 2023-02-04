import pytest

from maps_adv.geosmb.scenarist.server.lib.enums import (
    ScenarioName,
    SegmentType,
    SubscriptionStatus,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm_with_expected_params(domain, dm):
    await domain.list_scenarios(biz_id=123)

    dm.list_scenarios.assert_called_with(biz_id=123)


async def test_returns_scenario_details(domain, dm):
    dm_result = [
        dict(
            name=ScenarioName.DISCOUNT_FOR_LOST,
            segments=[SegmentType.LOST],
            scenario=dict(
                subscription_id=111,
                biz_id=123,
                scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
                status=SubscriptionStatus.ACTIVE,
                coupon_id=456,
            ),
        )
    ]
    dm.list_scenarios.coro.return_value = dm_result

    got = await domain.list_scenarios(biz_id=123)

    assert got == dm_result
