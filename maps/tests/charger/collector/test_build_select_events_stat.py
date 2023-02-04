from datetime import datetime

import pytest
from pytz import UnknownTimeZoneError

from maps_adv.stat_tasks_starter.lib.charger.collector.query_builders import (
    UnexpectedEmptyCampaignsIds,
    UnexpectedNaiveDateTime,
    build_select_events_stat,
)
from maps_adv.stat_tasks_starter.tests.tools import dt, dt_timestamp, squash_whitespaces


def prepare_range_part(from_ts, to_ts):
    return squash_whitespaces(f"ReceiveTimestamp BETWEEN {from_ts} AND {to_ts}")


select_template_for_testing = """
    SELECT CampaignID, charged_daily, charged_total, events_count
    FROM
    (
        SELECT CampaignID, events_count
        FROM (
            SELECT DISTINCT CampaignID
            FROM stat.normalized_sample
            WHERE
                CampaignID IN ({campaign_ids})
                AND EventName = 'geoadv.bb.pin.show'
        ) LEFT JOIN (
            SELECT CampaignID, Count(*) AS events_count
            FROM stat.normalized_sample
            WHERE
                CampaignID IN ({campaign_ids})
                AND EventName = 'geoadv.bb.pin.show'
                AND ReceiveTimestamp BETWEEN 90000 AND 90300
            GROUP BY CampaignID
        ) USING (CampaignID)
    ) LEFT JOIN (
        SELECT CampaignID, charged_total, charged_daily
        FROM (
            SELECT CampaignID, SUM(Cost) AS charged_total
            FROM stat.accepted_sample
            WHERE
                CampaignID IN ({campaign_ids})
                AND EventName = 'geoadv.bb.pin.show'
            GROUP BY CampaignID
        ) LEFT JOIN (
            SELECT CampaignID, Sum(Cost) AS charged_daily
            FROM stat.accepted_sample
            WHERE
                CampaignID IN ({campaign_ids})
                AND EventName = 'geoadv.bb.pin.show'
                AND ReceiveTimestamp BETWEEN 86400 AND 172799
            GROUP BY CampaignID
        ) USING (CampaignID)
    ) USING (CampaignID)"""


def format_test_select(campaigns_ids):
    return squash_whitespaces(
        select_template_for_testing.format(
            campaign_ids=", ".join(map(str, campaigns_ids))
        )
    )


def test_returns_expected_query():
    campaigns_ids = [10, 100, 100500]

    got = build_select_events_stat(
        database="stat",
        normalized_table="normalized_sample",
        charged_table="accepted_sample",
        timing_from=dt(90000),
        timing_to=dt(90300),
        campaigns_ids=campaigns_ids,
        tz_name="utc",
    )

    assert squash_whitespaces(got) == format_test_select(campaigns_ids)


@pytest.mark.parametrize(
    "times, tz_name, expected_day_range, expected_time_range",
    (
        [(dt(0), dt(300)), "utc", (0, 86399), (0, 300)],
        [(dt(100500), dt(101000)), "utc", (86400, 172799), (100500, 101000)],
        # impossible case: interval > 1 day => day interval is strictly one day
        [
            (dt("2019-06-20 02:30:30"), dt("2019-06-30 02:30:30")),
            "utc",
            (dt_timestamp("2019-06-20 00:00:00"), dt_timestamp("2019-06-20 23:59:59")),
            (dt_timestamp("2019-06-20 02:30:30"), dt_timestamp("2019-06-30 02:30:30")),
        ],
        [
            (dt("2019-06-20 02:30:30"), dt("2019-06-30 02:30:30")),
            "Europe/Moscow",
            (dt_timestamp("2019-06-19 21:00:00"), dt_timestamp("2019-06-20 20:59:59")),
            (dt_timestamp("2019-06-20 02:30:30"), dt_timestamp("2019-06-30 02:30:30")),
        ],
        # impossible case: interval overlap 2 days
        # => day interval is strictly one day
        [
            (dt("2019-06-20 23:55:00"), dt("2019-06-21 00:05:00")),
            "utc",
            (dt_timestamp("2019-06-20 00:00:00"), dt_timestamp("2019-06-20 23:59:59")),
            (dt_timestamp("2019-06-20 23:55:00"), dt_timestamp("2019-06-21 00:05:00")),
        ],
        # Timezones
        [
            (dt("2019-06-20 05:30:30"), dt("2019-06-20 05:40:30")),
            "Europe/Moscow",
            # UTC +3
            (dt_timestamp("2019-06-19 21:00:00"), dt_timestamp("2019-06-20 20:59:59")),
            (dt_timestamp("2019-06-20 05:30:30"), dt_timestamp("2019-06-20 05:40:30")),
        ],
        [
            (dt("2019-06-20 05:30:30"), dt("2019-06-20 05:40:30")),
            "America/Montevideo",
            # UTC -3
            (dt_timestamp("2019-06-20 03:00:00"), dt_timestamp("2019-06-21 02:59:59")),
            (dt_timestamp("2019-06-20 05:30:30"), dt_timestamp("2019-06-20 05:40:30")),
        ],
        [
            (dt("2019-06-20 05:30:30"), dt("2019-06-20 05:40:30")),
            "Europe/Bratislava",
            # UTC +1, with DST (summer time) = +2 offset
            (dt_timestamp("2019-06-19 22:00:00"), dt_timestamp("2019-06-20 21:59:59")),
            (dt_timestamp("2019-06-20 05:30:30"), dt_timestamp("2019-06-20 05:40:30")),
        ],
        [
            (dt("2019-01-20 05:30:30"), dt("2019-01-20 05:40:30")),
            "Europe/Bratislava",
            # UTC +1, no DST (winter time) = +1 offset
            (dt_timestamp("2019-01-19 23:00:00"), dt_timestamp("2019-01-20 22:59:59")),
            (dt_timestamp("2019-01-20 05:30:30"), dt_timestamp("2019-01-20 05:40:30")),
        ],
    ),
)
def test_includes_expected_timestamps(
    times, tz_name, expected_day_range, expected_time_range
):
    expected_day_part = prepare_range_part(*expected_day_range)
    expected_time_part = prepare_range_part(*expected_time_range)

    got = build_select_events_stat(
        "stat", "normalized_sample", "accepted_sample", times[0], times[1], [1], tz_name
    )
    got = squash_whitespaces(got)

    assert expected_day_part in got
    assert expected_time_part in got


@pytest.mark.parametrize(
    "campaigns_ids, expected_part",
    (
        ([10, 100, 1], "CampaignID IN (10, 100, 1)"),
        ([10, 100500], "CampaignID IN (10, 100500)"),
        ([10], "CampaignID IN (10)"),
    ),
)
def test_includes_expected_campaigns_ids(campaigns_ids, expected_part):
    got = build_select_events_stat(
        "stat",
        "normalized_sample",
        "accepted_sample",
        dt(0),
        dt(300),
        campaigns_ids,
        "utc",
    )

    assert expected_part in got


@pytest.mark.parametrize(
    "times",
    (
        (dt(0), datetime(2019, 3, 27, 20, 4, 55)),
        (datetime(2019, 3, 27, 19, 57, 59), dt(300)),
        (datetime(2019, 3, 27, 19, 57, 59), datetime(2019, 3, 27, 20, 4, 55)),
    ),
)
def test_raises_for_naive_datetime(times):
    with pytest.raises(UnexpectedNaiveDateTime):
        build_select_events_stat(
            "stat",
            "normalized_sample",
            "accepted_sample",
            times[0],
            times[1],
            [10],
            "utc",
        )


def test_raises_for_empty_campaigns_ids():
    with pytest.raises(UnexpectedEmptyCampaignsIds):
        build_select_events_stat(
            "stat", "normalized_sample", "accepted_sample", dt(0), dt(300), [], "utc"
        )


def test_raises_for_unknown_timezone():
    with pytest.raises(UnknownTimeZoneError):
        build_select_events_stat(
            "stat",
            "normalized_sample",
            "accepted_sample",
            dt(0),
            dt(300),
            [1],
            "unknown_tz",
        )
