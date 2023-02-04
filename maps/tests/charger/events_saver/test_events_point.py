from decimal import Decimal

import pytest

from maps_adv.stat_tasks_starter.lib.charger.events_saver import EventsPoint
from maps_adv.stat_tasks_starter.tests.tools import dt, make_event, squash_whitespaces


@pytest.fixture(autouse=True)
def setup_normalize_table(ch_client):
    norm_events = [
        make_event(*args)
        for args in (
            (1, 1553716282),
            (1, 1553716283),
            (1, 1553716284),
            (2, 1553716285),
            (2, 1553716286),
            (3, 1553716287),
        )
    ]
    ch_client.execute("INSERT INTO stat.normalized_sample VALUES", norm_events)


def make_events_point():
    return EventsPoint(
        normalized_table="normalized_sample",
        charged_table="accepted_sample",
        database="stat",
        host="localhost",
        port=9001,
    )


select_query_template = squash_whitespaces(
    """SELECT *, 2
        FROM stat.normalized_sample
        WHERE CampaignID={campaign_id}
            AND ReceiveTimestamp BETWEEN 1553716281 AND 1553716581
            AND EventName = 'geoadv.bb.pin.show'
        ORDER BY ReceiveTimestamp DESC
        LIMIT 1 OFFSET 0"""
)


def test_select_events_sql_looks_as_expected():
    got = make_events_point().prepare_select_events_sql(
        campaign_id=10,
        event_cost=Decimal(2.0),
        timing_from=dt("2019-03-27 19:51:21"),
        timing_to=dt("2019-03-27 19:56:21"),
        limit=1,
        limit_offset=0,
    )

    assert squash_whitespaces(got) == select_query_template.format(campaign_id=10)


@pytest.mark.asyncio
async def test_insert_charged_events_as_expected(ch_client):
    select_sqls = [
        select_query_template.format(campaign_id=campaign_id) for campaign_id in (1, 2)
    ]

    await make_events_point()(select_sqls)

    got = ch_client.execute("SELECT * FROM stat.accepted_sample")

    assert set(got) == {
        make_event(*args, cost=Decimal(2.0))
        for args in ((1, 1553716284), (2, 1553716286))
    }
