import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName, SubscriptionStatus
from maps_adv.geosmb.scenarist.server.lib.exceptions import UnknownSubscription

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


scenario_data = dict(
    subscription_id=111,
    biz_id=123,
    scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
    status=SubscriptionStatus.ACTIVE,
    coupon_id=564000,
    coupons_history=[
        dict(
            coupon_id=456,
            created_at=dt("2020-01-01 00:00:00"),
            statistics={"sent": 1, "clicked": 2, "opened": 3},
        )
    ],
)


async def test_returns_subscription_data(domain, dm):
    dm.retrieve_subscription.coro.return_value = scenario_data

    got = await domain.retrieve_subscription(subscription_id=111, biz_id=123)

    assert got == scenario_data


async def test_calls_dm_correctly(domain, dm):
    await domain.retrieve_subscription(biz_id=123, subscription_id=111)

    dm.retrieve_subscription.assert_called_with(biz_id=123, subscription_id=111)


@pytest.mark.parametrize("exception_cls", (Exception, UnknownSubscription))
async def test_propagates_dm_exception(domain, dm, exception_cls):
    dm.retrieve_subscription.coro.side_effect = exception_cls()

    with pytest.raises(exception_cls):
        await domain.retrieve_subscription(subscription_id=111, biz_id=123)
