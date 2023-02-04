import pytest

from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "now, expected",
    [
        (dt("2020-01-01 15:42:00"), dt("2020-01-01 15:00:00")),
        (dt("2019-10-23 05:49:13"), dt("2019-10-23 05:00:00")),
    ],
)
async def test_will_call_data_manager_for_current_day(
    now, expected, campaigns_domain, campaigns_dm, freezer
):
    freezer.move_to(now)

    await campaigns_domain.refresh_auto_daily_budgets()

    campaigns_dm.refresh_auto_daily_budgets.assert_called_with(expected)
