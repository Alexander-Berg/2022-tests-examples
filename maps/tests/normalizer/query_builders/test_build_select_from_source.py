from datetime import datetime

import pytest

from maps_adv.stat_tasks_starter.lib.normalizer.query_builders import (
    UnexpectedNaiveDateTime,
    build_select_from_source,
)
from maps_adv.stat_tasks_starter.tests.tools import dt

expected_query = """INSERT INTO stat.normalized_sample (
        ReceiveTimestamp,
        CampaignID,
        EventGroupId,
        APIKey,
        DeviceID,
        AppPlatform,
        AppVersionName,
        AppBuildNumber,
        Latitude,
        Longitude,
        EventName
    )
    SELECT
        ReceiveTimestamp,
        CampaignID,
        EventGroupId,
        APIKey,
        DeviceID,
        AppPlatform,
        AppVersionName,
        AppBuildNumber,
        Latitude,
        Longitude,
        EventName
    FROM stat.source_sample
    WHERE
        APIKey IN (2, 4, 30488)
        AND ReceiveTimestamp BETWEEN 1553716281 AND 1553716581
        AND CampaignID != 0"""


def test_returns_expected_query():
    got = build_select_from_source(
        database="stat",
        table_from="source_sample",
        table_to="normalized_sample",
        timing_from=dt("2019-03-27 19:51:21"),
        timing_to=dt("2019-03-27 19:56:21"),
    )

    assert got == expected_query


@pytest.mark.parametrize(
    "time_range, timestamp_part",
    (
        [
            (dt("2019-03-27 19:51:21"), dt("2019-03-27 19:56:21")),
            "ReceiveTimestamp BETWEEN 1553716281 AND 1553716581",
        ],
        [
            (dt("2019-03-27 19:56:49"), dt("2019-03-27 20:06:49")),
            "ReceiveTimestamp BETWEEN 1553716609 AND 1553717209",
        ],
        [
            (dt("2019-03-27 19:57:59"), dt("2019-03-27 20:04:55")),
            "ReceiveTimestamp BETWEEN 1553716679 AND 1553717095",
        ],
    ),
)
def test_includes_expected_timestamps(time_range, timestamp_part):
    got = build_select_from_source(
        "stat", "source_sample", "normalized_sample", *time_range
    )

    assert timestamp_part in got


def test_raises_for_non_utc_datetimes():
    with pytest.raises(UnexpectedNaiveDateTime):
        build_select_from_source(
            database="stat",
            table_from="source_sample",
            table_to="normalized_sample",
            timing_from=datetime(2019, 3, 27, 19, 51, 21),
            timing_to=datetime(2019, 3, 27, 19, 56, 21),
        )
