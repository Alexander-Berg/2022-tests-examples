from datetime import timedelta

import pytest

from maps_adv.common.helpers import dt
from maps_adv.statistics.dashboard.server.tests import (
    make_raw_event as make_raw_appmetrica_event,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.no_setup_ch]


@pytest.fixture
def fill_tables(ch, setup_ch_for_monitorings):
    try:
        ch.execute(
            "INSERT INTO stat.maps_adv_statistics_raw_metrika_log_distributed VALUES",
            [
                make_raw_appmetrica_event(*event)
                for event in [
                    (dt("2020-07-27 00:00:01"), 555, "di50", "geoadv.bb.pin.show"),
                    (dt("2020-07-27 00:01:02"), 555, "di50", "geoadv.bb.pin.show"),
                ]
            ],
        )
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


@pytest.mark.parametrize(
    ["end_time", "expected_result"],
    [
        (
            dt("2020-07-27 00:01:00"),
            [
                {"table": "mapkit_events", "max_receive_timestamp": 1595808011},
                {
                    "table": "maps_adv_statistics_raw_metrika_log",
                    "max_receive_timestamp": 1595808001,
                },
                {"table": "normalized_events", "max_receive_timestamp": 1595808021},
                {"table": "processed_events", "max_receive_timestamp": 1595808031},
            ],
        ),
        (
            dt("2020-07-27 00:01:02") + timedelta(days=7),
            [
                {"table": "mapkit_events", "max_receive_timestamp": 1595808072},
                {
                    "table": "maps_adv_statistics_raw_metrika_log",
                    "max_receive_timestamp": 1595808062,
                },
                {"table": "normalized_events", "max_receive_timestamp": 1595808082},
                {"table": "processed_events", "max_receive_timestamp": 1595808092},
            ],
        ),
    ],
)
@pytest.mark.usefixtures("fill_tables")
async def test_returns_metrics(end_time, expected_result, dm):
    got = await dm.retrieve_tables_metrics(end_time=end_time)

    assert got == expected_result


@pytest.mark.usefixtures("fill_tables")
async def test_does_not_events_for_7_days(dm):
    got = await dm.retrieve_tables_metrics(
        end_time=dt("2020-07-27 00:01:33") + timedelta(days=7)
    )

    assert got == [
        {"table": "mapkit_events", "max_receive_timestamp": 0},
        {"table": "maps_adv_statistics_raw_metrika_log", "max_receive_timestamp": 0},
        {"table": "normalized_events", "max_receive_timestamp": 0},
        {"table": "processed_events", "max_receive_timestamp": 0},
    ]
