import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.mock_dm
@pytest.mark.mock_domain
@pytest.mark.parametrize(
    ["time_as_string", "time_in_seconds"], [("30s", 30), ("15m", 900)]
)
async def test_returns_expected(domain, api_provider, time_as_string, time_in_seconds):

    domain.calculate_monitoring_data.coro.return_value = [
        {"labels": {"name": "total_users"}, "type": "COUNTER", "value": 1000},
        {"labels": {"name": "total_shows"}, "type": "COUNTER", "value": 10000},
        {"labels": {"name": "total_clicks"}, "type": "COUNTER", "value": 100},
        {"labels": {"name": "zsb_shows"}, "type": "COUNTER", "value": 10000},
        {"labels": {"name": "zsb_clicks"}, "type": "COUNTER", "value": 100},
    ]

    got = await api_provider.calculate_monitoring_data(
        "2020-07-27T01:01:00Z", time_as_string
    )
    domain.calculate_monitoring_data.assert_called_with(
        dt("2020-07-27 01:01:00"), time_in_seconds
    )

    assert got == {
        "metrics": [
            {"labels": {"name": "total_users"}, "type": "COUNTER", "value": 1000},
            {"labels": {"name": "total_shows"}, "type": "COUNTER", "value": 10000},
            {"labels": {"name": "total_clicks"}, "type": "COUNTER", "value": 100},
            {"labels": {"name": "zsb_shows"}, "type": "COUNTER", "value": 10000},
            {"labels": {"name": "zsb_clicks"}, "type": "COUNTER", "value": 100},
        ]
    }
