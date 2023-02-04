import pytest

from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName, SubscriptionStatus
from maps_adv.geosmb.scenarist.server.lib.exceptions import SubscriptionAlreadyExists

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


subscription_data = dict(
    subscription_id=111,
    biz_id=123,
    scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
    status=SubscriptionStatus.ACTIVE,
    coupon_id=564000,
)


async def test_returns_subscription_data(domain, dm):
    dm.create_subscription.coro.return_value = subscription_data

    got = await domain.create_subscription(
        biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST, coupon_id=564000
    )

    assert got == subscription_data


async def test_calls_dm_correctly(domain, dm):
    await domain.create_subscription(
        biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST, coupon_id=564000
    )

    dm.create_subscription.assert_called_with(
        biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST, coupon_id=564000
    )


@pytest.mark.parametrize("exception_cls", (Exception, SubscriptionAlreadyExists))
async def test_propagates_dm_exception(domain, dm, exception_cls):
    dm.create_subscription.coro.side_effect = exception_cls()

    with pytest.raises(exception_cls):
        await domain.create_subscription(
            biz_id=123, scenario_name=ScenarioName.DISCOUNT_FOR_LOST, coupon_id=564000
        )
