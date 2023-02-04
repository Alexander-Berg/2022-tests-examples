import json

import pytest

from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.no_setup_ch]

url = "/monitorings/?now=2020-07-27T00:15:00Z&period=900s"


@pytest.fixture
def fill_tables(ch, setup_ch_for_monitorings):
    try:
        ch.execute(
            "INSERT INTO stat.mapkit_events_distributed VALUES",
            [
                event
                for event in [[dt("2020-07-27 00:00:11")], [dt("2020-07-27 00:01:12")]]
            ],
        )
        ch.execute(
            "INSERT INTO stat.normalized_events_distributed VALUES",
            [
                event
                for event in [[dt("2020-07-27 00:00:21")], [dt("2020-07-27 00:01:22")]]
            ],
        )
        ch.execute(
            "INSERT INTO stat.processed_events_distributed VALUES",
            [
                event
                for event in [[dt("2020-07-27 00:00:31")], [dt("2020-07-27 00:01:32")]]
            ],
        )

        yield

    finally:
        ch.execute(
            "TRUNCATE TABLE stat.maps_adv_statistics_raw_metrika_log_distributed"
        )
        ch.execute("TRUNCATE TABLE stat.mapkit_events_distributed")
        ch.execute("TRUNCATE TABLE stat.normalized_events_distributed")
        ch.execute("TRUNCATE TABLE stat.processed_events_distributed")


@pytest.mark.usefixtures("fill_raw_table")
@pytest.mark.usefixtures("fill_tables")
async def test_returns_metrics(app, adv_store_client, api, freezer, mocker):
    mocker.patch.object(app.domain, "_adv_store_client", adv_store_client)
    freezer.move_to(dt("2020-07-28 00:00:00"))
    result = json.loads(await api.get(url, expected_status=200))

    assert result == {
        "metrics": [
            {"labels": {"name": "total_users"}, "type": "COUNTER", "value": 9},
            {"labels": {"name": "total_shows"}, "type": "COUNTER", "value": 11},
            {"labels": {"name": "total_clicks"}, "type": "COUNTER", "value": 3},
            {"labels": {"name": "zsb_shows"}, "type": "COUNTER", "value": 2},
            {"labels": {"name": "zsb_clicks"}, "type": "COUNTER", "value": 0},
            {"labels": {"name": "route_banner_shows"}, "type": "COUNTER", "value": 2},
            {"labels": {"name": "route_banner_clicks"}, "type": "COUNTER", "value": 0},
            {
                "labels": {"name": "overview_banner_shows"},
                "type": "COUNTER",
                "value": 3,
            },
            {
                "labels": {"name": "overview_banner_clicks"},
                "type": "COUNTER",
                "value": 1,
            },
            {"labels": {"name": "pin_on_route_shows"}, "type": "COUNTER", "value": 1},
            {"labels": {"name": "pin_on_route_clicks"}, "type": "COUNTER", "value": 1},
            {"labels": {"name": "billboard_shows"}, "type": "COUNTER", "value": 3},
            {"labels": {"name": "billboard_clicks"}, "type": "COUNTER", "value": 1},
            {
                "labels": {"timestamp": "now"},
                "type": "IGAUGE",
                "value": int(dt("2020-07-28 00:00:00").timestamp()),
            },
            {
                "labels": {"timestamp": "request_from"},
                "type": "IGAUGE",
                "value": int(dt("2020-07-27 00:00:00").timestamp()),
            },
            {
                "labels": {"timestamp": "request_to"},
                "type": "IGAUGE",
                "value": int(dt("2020-07-27 00:15:00").timestamp()),
            },
            {
                "labels": {"table": "mapkit_events", "column": "max_receive_timestamp"},
                "type": "IGAUGE",
                "value": int(dt("2020-07-27 00:01:12").timestamp()),
            },
            {
                "labels": {
                    "table": "maps_adv_statistics_raw_metrika_log",
                    "column": "max_receive_timestamp",
                },
                "type": "IGAUGE",
                "value": int(dt("2020-07-27 00:05:01").timestamp()),
            },
            {
                "labels": {
                    "table": "normalized_events",
                    "column": "max_receive_timestamp",
                },
                "type": "IGAUGE",
                "value": int(dt("2020-07-27 00:01:22").timestamp()),
            },
            {
                "labels": {
                    "table": "processed_events",
                    "column": "max_receive_timestamp",
                },
                "type": "IGAUGE",
                "value": int(dt("2020-07-27 00:01:32").timestamp()),
            },
        ]
    }


async def test_returns_error_if_wrong_parameters_passed(api):
    await api.get(
        "/monitorings/?notnow=2020-07-27T03:15:00Z&notperiod=900s", expected_status=400
    )
    await api.get("/monitorings/?now=XXXX&period=900s", expected_status=400)
    await api.get(
        "/monitorings/?now=2020-07-27T03:15:00Z&period=XXX", expected_status=400
    )
