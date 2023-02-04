from datetime import datetime
from decimal import Decimal

import pytest

from maps_adv.stat_tasks_starter.lib.base.exceptions import UnexpectedNaiveDateTime
from maps_adv.stat_tasks_starter.lib.charger.events_saver.query_builders import (  # noqa
    build_select_events,
)
from maps_adv.stat_tasks_starter.tests.tools import dt, squash_whitespaces

expected_query = """SELECT *, 2
    FROM stat.normalized_sample
    WHERE CampaignID=10
        AND ReceiveTimestamp BETWEEN 1553716281 AND 1553716581
        AND EventName = 'geoadv.bb.pin.show'
    ORDER BY ReceiveTimestamp DESC
    LIMIT 1 OFFSET 5
"""

expected_query = squash_whitespaces(expected_query)


def test_returns_expected_query():
    got = build_select_events(
        database="stat",
        table="normalized_sample",
        campaign_id=10,
        event_cost=Decimal(2.0),
        timing_from=dt("2019-03-27 19:51:21"),
        timing_to=dt("2019-03-27 19:56:21"),
        limit=1,
        limit_offset=5,
    )

    assert squash_whitespaces(got) == expected_query


@pytest.mark.parametrize(
    "timing_from, timing_to",
    (
        (dt("2019-03-27 19:57:59"), datetime(2019, 3, 27, 20, 4, 55)),
        (datetime(2019, 3, 27, 19, 57, 59), dt("2019-03-27 20:4:55")),
        (datetime(2019, 3, 27, 19, 57, 59), datetime(2019, 3, 27, 20, 4, 55)),
    ),
)
def test_raises_for_naive_datetime(timing_from, timing_to):
    with pytest.raises(UnexpectedNaiveDateTime):
        build_select_events(
            database="stat",
            table="normalized_sample",
            campaign_id=10,
            event_cost=Decimal(2.0),
            timing_from=timing_from,
            timing_to=timing_to,
            limit=1,
            limit_offset=5,
        )
