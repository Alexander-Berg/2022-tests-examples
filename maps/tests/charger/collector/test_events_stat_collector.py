from decimal import Decimal

import pytest

from maps_adv.stat_tasks_starter.lib.charger.collector.events_stat import Collector
from maps_adv.stat_tasks_starter.tests.tools import (
    dt,
    dt_timestamp,
    setup_charged_db,
    setup_normalized_db,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def collector(loop):
    return Collector(
        database="stat",
        normalized_table="normalized_sample",
        charged_table="accepted_sample",
        host="localhost",
        port=9001,
    )


async def test_returns_no_stat_if_no_events(collector):
    got = await collector(dt(0), dt(50), timezone_campaigns={"utc": [1, 2]})
    assert got == []


async def test_returns_zero_count_if_all_events_already_charged(ch_client, collector):
    setup_normalized_db(
        ch_client, ((1, 100), (1, 150), (1, 200), (2, 100), (2, 150), (3, 300))
    )

    setup_charged_db(
        ch_client,
        (
            (1, 100, Decimal(1)),
            (1, 150, Decimal(1)),
            (1, 200, Decimal(1)),
            (2, 100, Decimal(10)),
            (2, 150, Decimal(10)),
            (3, 300, Decimal(100)),
        ),
    )

    got = await collector(dt(1000), dt(2000), timezone_campaigns={"utc": [1, 2, 3]})
    assert set(got) == {
        # campaign id, charged per day, charged all, events in time range
        (1, Decimal(3), Decimal(3), 0),
        (2, Decimal(20), Decimal(20), 0),
        (3, Decimal(100), Decimal(100), 0),
    }


async def test_returns_counts_of_not_charged_events(ch_client, collector):
    setup_normalized_db(
        ch_client, ((1, 100), (1, 150), (1, 200), (2, 100), (2, 150), (3, 300))
    )

    charged_events_args = ((1, 100, Decimal(1)),)
    setup_charged_db(ch_client, charged_events_args)

    got = await collector(dt(101), dt(1000), timezone_campaigns={"utc": [1, 2, 3]})
    assert set(got) == {
        # campaign id, charged per day, charged all, events in time range
        (1, Decimal(1), Decimal(1), 2),
        (2, Decimal(0), Decimal(0), 1),
        (3, Decimal(0), Decimal(0), 1),
    }


async def test_skips_not_pin_show_events(ch_client, collector):
    setup_normalized_db(
        ch_client,
        (
            (1, 100),
            (1, 100, "lol"),
            (1, 150),
            (1, 200),
            (2, 100),
            (2, 100, "kek"),
            (2, 150),
            (3, 300),
            (3, 300, "cheburek"),
        ),
    )

    charged_events_args = ((1, 100, Decimal(1)),)
    setup_charged_db(ch_client, charged_events_args)

    got = await collector(dt(101), dt(1000), timezone_campaigns={"utc": [1, 2, 3]})
    assert set(got) == {
        # campaign id, charged per day, charged all, events in time range
        (1, Decimal(1), Decimal(1), 2),
        (2, Decimal(0), Decimal(0), 1),
        (3, Decimal(0), Decimal(0), 1),
    }


async def test_skips_not_charged_events_outside_time_interval(ch_client, collector):
    setup_normalized_db(
        ch_client, ((1, 100), (1, 150), (1, 200), (1, 250), (1, 300), (1, 350))
    )

    got = await collector(dt(101), dt(200), timezone_campaigns={"utc": [1]})
    assert set(got) == {
        # campaign id, charged per day, charged all, events in time range
        (1, Decimal(0), Decimal(0), 2)
    }


@pytest.mark.parametrize(
    # winter/summer year_month for checking DST
    "tz_name, year_month, expected_daily_charged",
    (
        ["utc", "2019-01", Decimal(300)],
        ["utc", "2019-06", Decimal(300)],
        # UTC+3, DST agnostic
        ["Europe/Moscow", "2019-01", Decimal(320)],
        ["Europe/Moscow", "2019-06", Decimal(320)],
        # UTC+1, with DST
        ["Europe/Bratislava", "2019-01", Decimal(310)],
        ["Europe/Bratislava", "2019-06", Decimal(320)],
        # UTC-2, DST agnostic
        ["America/Noronha", "2019-01", Decimal(100)],
        ["America/Noronha", "2019-06", Decimal(100)],
        # UTC-1, with DST
        ["America/Scoresbysund", "2019-01", Decimal(200)],
        ["America/Scoresbysund", "2019-06", Decimal(300)],
    ),
)
async def test_daily_charged_calculation_according_to_campaign_tz(
    ch_client, collector, tz_name, year_month, expected_daily_charged
):
    setup_normalized_db(
        ch_client,
        (
            (1, dt_timestamp(f"{year_month}-18 22:30:00")),
            (1, dt_timestamp(f"{year_month}-19 22:30:00")),
            (1, dt_timestamp(f"{year_month}-19 23:30:00")),
            (1, dt_timestamp(f"{year_month}-20 00:30:00")),
            (1, dt_timestamp(f"{year_month}-20 01:30:00")),
            (1, dt_timestamp(f"{year_month}-20 02:30:00")),
            (1, dt_timestamp(f"{year_month}-20 02:33:00")),
            (1, dt_timestamp(f"{year_month}-21 02:33:00")),
        ),
    )

    setup_charged_db(
        ch_client,
        (
            (1, dt_timestamp(f"{year_month}-18 22:30:00"), Decimal(1)),
            (1, dt_timestamp(f"{year_month}-19 22:30:00"), Decimal(10)),
            (1, dt_timestamp(f"{year_month}-19 23:30:00"), Decimal(10)),
            (1, dt_timestamp(f"{year_month}-20 00:30:00"), Decimal(100)),
            (1, dt_timestamp(f"{year_month}-20 01:30:00"), Decimal(100)),
            (1, dt_timestamp(f"{year_month}-20 02:30:00"), Decimal(100)),
            (1, dt_timestamp(f"{year_month}-21 02:30:00"), Decimal(1000)),
        ),
    )

    got = await collector(
        dt(f"{year_month}-20 02:31:00"),
        dt(f"{year_month}-20 02:41:00"),
        timezone_campaigns={tz_name: [1]},
    )

    assert set(got) == {
        # campaign id, charged per day, charged all, events in time range
        (1, expected_daily_charged, Decimal(1321), 1)
    }


async def test_daily_charged_calculation_for_multi_tz_campaigns(ch_client, collector):
    setup_normalized_db(
        ch_client,
        (
            # utc
            (1, dt_timestamp("2019-06-19 23:59:59")),  # yesterday
            (1, dt_timestamp("2019-06-20 00:30:00")),  # today
            (1, dt_timestamp("2019-06-21 00:00:00")),  # tomorrow
            # Hawaii, -10:00
            (2, dt_timestamp("2019-06-20 09:59:59")),  # yesterday
            (2, dt_timestamp("2019-06-20 10:30:00")),  # today
            (2, dt_timestamp("2019-06-21 10:00:00")),  # tomorrow
            # Moscow, +03:00
            (3, dt_timestamp("2019-06-19 20:59:59")),  # yesterday
            (3, dt_timestamp("2019-06-19 21:30:00")),  # today
            (3, dt_timestamp("2019-06-19 21:00:00")),  # tomorrow
            # Kamchatka, +12:00
            (4, dt_timestamp("2019-06-19 11:59:59")),  # yesterday
            # today (inside interval)
            (4, dt_timestamp("2019-06-19 12:30:00")),
            # today (inside interval)
            (4, dt_timestamp("2019-06-19 12:31:00")),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 13:00:00")),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 14:00:00")),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 15:00:00")),
            (4, dt_timestamp("2019-06-20 12:00:00")),  # tomorrow
        ),
    )

    setup_charged_db(
        ch_client,
        (
            # utc
            (1, dt_timestamp("2019-06-19 23:59:59"), Decimal(1)),  # yesterday
            (1, dt_timestamp("2019-06-20 00:30:00"), Decimal(1)),  # today
            (1, dt_timestamp("2019-06-21 00:00:00"), Decimal(1)),  # tomorrow
            # Hawaii, -10:00
            (2, dt_timestamp("2019-06-20 09:59:59"), Decimal(1)),  # yesterday
            (2, dt_timestamp("2019-06-20 10:30:00"), Decimal(1)),  # today
            (2, dt_timestamp("2019-06-21 10:00:00"), Decimal(1)),  # tomorrow
            # Moscow, +03:00
            (3, dt_timestamp("2019-06-19 20:59:59"), Decimal(1)),  # yesterday
            (3, dt_timestamp("2019-06-19 21:30:00"), Decimal(1)),  # today
            (3, dt_timestamp("2019-06-20 21:00:00"), Decimal(1)),  # tomorrow
            # Kamchatka, +12:00
            (4, dt_timestamp("2019-06-19 11:59:59"), Decimal(1)),  # yesterday
            # today (inside interval)
            (4, dt_timestamp("2019-06-19 12:30:00"), Decimal(1)),
            # today (inside interval)
            (4, dt_timestamp("2019-06-19 12:31:00"), Decimal(1)),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 13:00:00"), Decimal(1)),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 14:00:00"), Decimal(1)),
            # today (outside interval)
            (4, dt_timestamp("2019-06-19 15:00:00"), Decimal(1)),
            (4, dt_timestamp("2019-06-20 12:00:00"), Decimal(1)),  # tomorrow
        ),
    )

    got = await collector(
        dt("2019-06-19 12:29:00"),
        dt("2019-06-19 12:41:00"),
        timezone_campaigns={
            "utc": [1],  # +00:00
            "Pacific/Honolulu": [2],  # -10:00
            "Europe/Moscow": [3],  # +03:00
            "Asia/Kamchatka": [4],  # +12:00
        },
    )

    assert set(got) == {
        # campaign id, charged per day, charged all, events in time range
        (1, Decimal(1), Decimal(3), 0),
        (2, Decimal(1), Decimal(3), 0),
        (3, Decimal(1), Decimal(3), 0),
        (4, Decimal(5), Decimal(7), 2),
    }


async def test_returns_stat_only_for_requested_campaigns(ch_client, collector):
    setup_normalized_db(
        ch_client, ((1, 100), (1, 150), (1, 200), (2, 100), (2, 150), (3, 300))
    )

    charged_events_args = ((1, 100, Decimal(1)),)
    setup_charged_db(ch_client, charged_events_args)

    got = await collector(dt(101), dt(1000), timezone_campaigns={"utc": [1, 99]})
    assert set(got) == {
        # campaign id, charged per day, charged all, events in time range
        (1, Decimal(1), Decimal(1), 2)
    }
