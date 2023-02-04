from decimal import Decimal

import pytest

from maps_adv.stat_tasks_starter.lib.not_spending_budget.campaign_charged_stat import (
    CampaignChargedStat,
)
from maps_adv.stat_tasks_starter.tests.tools import setup_charged_db

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def collector(loop):
    return CampaignChargedStat(
        database="stat", charged_table="accepted_sample", host="localhost", port=9001
    )


async def test_returns_no_stat_if_no_events(collector):
    got = await collector(campaigns_ids=[1, 2])
    assert got == []


async def test_returns_charged_for_campaigns(collector, ch_client):
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
    got = await collector(campaigns_ids=[1, 2, 3])

    assert set(got) == {
        # campaign id, charged
        (1, Decimal(3)),
        (2, Decimal(20)),
        (3, Decimal(100)),
    }


async def test_returns_stat_only_for_requested_campaigns(ch_client, collector):
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
    got = await collector(campaigns_ids=[1])

    assert set(got) == {(1, Decimal(3))}
