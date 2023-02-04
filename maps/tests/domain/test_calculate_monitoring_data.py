from unittest.mock import call

import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.mock_dm
async def test_returns_expected(dm, domain, freezer):
    freezer.move_to(dt("2020-07-28 00:00:00"))
    dm.calculate_metrics.coro.return_value = {
        "users": 1000,
        "shows": 10000,
        "clicks": 100,
    }
    dm.retrieve_tables_metrics.coro.return_value = [
        {"table": "t1", "max_receive_timestamp": 1},
        {"table": "t2", "max_receive_timestamp": 2},
        {"table": "t4", "max_receive_timestamp": 4},
        {"table": "t3", "max_receive_timestamp": 3},
    ]
    dm.get_campaign_ids_for_period.coro.return_value = [111, 222, 333, 444, 555]

    got = await domain.calculate_monitoring_data(dt("2020-07-27 01:01:00"), 30)

    assert got == [
        {"labels": {"name": "total_users"}, "type": "COUNTER", "value": 1000},
        {"labels": {"name": "total_shows"}, "type": "COUNTER", "value": 10000},
        {"labels": {"name": "total_clicks"}, "type": "COUNTER", "value": 100},
        {"labels": {"name": "zsb_shows"}, "type": "COUNTER", "value": 10000},
        {"labels": {"name": "zsb_clicks"}, "type": "COUNTER", "value": 100},
        {"labels": {"name": "route_banner_shows"}, "type": "COUNTER", "value": 10000},
        {"labels": {"name": "route_banner_clicks"}, "type": "COUNTER", "value": 100},
        {
            "labels": {"name": "overview_banner_shows"},
            "type": "COUNTER",
            "value": 10000,
        },
        {"labels": {"name": "overview_banner_clicks"}, "type": "COUNTER", "value": 100},
        {"labels": {"name": "pin_on_route_shows"}, "type": "COUNTER", "value": 10000},
        {"labels": {"name": "pin_on_route_clicks"}, "type": "COUNTER", "value": 100},
        {"labels": {"name": "billboard_shows"}, "type": "COUNTER", "value": 10000},
        {"labels": {"name": "billboard_clicks"}, "type": "COUNTER", "value": 100},
        {
            "labels": {"timestamp": "now"},
            "type": "IGAUGE",
            "value": int(dt("2020-07-28 00:00:00").timestamp()),
        },
        {
            "labels": {"timestamp": "request_from"},
            "type": "IGAUGE",
            "value": int(dt("2020-07-27 01:00:30").timestamp()),
        },
        {
            "labels": {"timestamp": "request_to"},
            "type": "IGAUGE",
            "value": int(dt("2020-07-27 01:01:00").timestamp()),
        },
        {
            "labels": {"table": "t1", "column": "max_receive_timestamp"},
            "type": "IGAUGE",
            "value": 1,
        },
        {
            "labels": {"table": "t2", "column": "max_receive_timestamp"},
            "type": "IGAUGE",
            "value": 2,
        },
        {
            "labels": {"table": "t4", "column": "max_receive_timestamp"},
            "type": "IGAUGE",
            "value": 4,
        },
        {
            "labels": {"table": "t3", "column": "max_receive_timestamp"},
            "type": "IGAUGE",
            "value": 3,
        },
    ]


@pytest.mark.mock_dm
async def test_passes_parameters_to_dm(dm, domain):
    dm.calculate_metrics.coro.return_value = {
        "users": 1000,
        "shows": 10000,
        "clicks": 100,
    }
    dm.retrieve_tables_metrics.coro.return_value = []
    dm.get_campaign_ids_for_period.coro.return_value = [111, 222, 333, 444, 555]

    await domain.calculate_monitoring_data(dt("2020-07-27 01:01:00"), 30)

    dm.get_campaign_ids_for_period.assert_called_with(
        dt("2020-07-27 01:00:30"), dt("2020-07-27 01:01:00")
    )
    dm.calculate_metrics.assert_has_calls(
        [
            call(dt("2020-07-27 01:00:30"), dt("2020-07-27 01:01:00")),
            call(dt("2020-07-27 01:00:30"), dt("2020-07-27 01:01:00"), [111]),
            call(dt("2020-07-27 01:00:30"), dt("2020-07-27 01:01:00"), [222]),
            call(dt("2020-07-27 01:00:30"), dt("2020-07-27 01:01:00"), [333]),
            call(dt("2020-07-27 01:00:30"), dt("2020-07-27 01:01:00"), [444]),
            call(dt("2020-07-27 01:00:30"), dt("2020-07-27 01:01:00"), [555]),
        ],
        any_order=True,
    )
    dm.retrieve_tables_metrics.assert_called_once_with(dt("2020-07-27 01:01:00"))
