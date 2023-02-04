from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.api.proto.billing_pb2 import Money
from maps_adv.adv_store.api.proto.campaign_list_pb2 import (
    CampaignBudgetAnalysisData,
    CampaignBudgetAnalysisList,
)
from maps_adv.stat_tasks_starter.lib.not_spending_budget import NotSpendingBudgetTask
from maps_adv.stat_tasks_starter.tests.tools import setup_charged_db

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def task(loop, config):
    return NotSpendingBudgetTask(config)


async def test_returns_no_stat_if_no_campaigns(
    adv_store_list_campaigns_for_budget_analysis_rmock, ch_client, task
):

    adv_store_message = CampaignBudgetAnalysisList(campaigns=[]).SerializeToString()

    adv_store_list_campaigns_for_budget_analysis_rmock(
        Response(body=adv_store_message, status=200)
    )
    setup_charged_db(
        ch_client,
        (
            (4242, 100, Decimal(100)),
            (4356, 150, Decimal(50)),
            (1242, 200, Decimal(100)),
        ),
    )

    got = await task.collect_campaign_data()

    assert got == []


async def test_returns_campaign_data(
    adv_store_list_campaigns_for_budget_analysis_rmock, ch_client, task
):
    adv_store_message = CampaignBudgetAnalysisList(
        campaigns=[
            CampaignBudgetAnalysisData(
                id=4242,
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                days_left=5,
            ),
            CampaignBudgetAnalysisData(
                id=4356,
                budget=Money(value=3000000),
                daily_budget=Money(value=300000),
                days_left=10,
            ),
            CampaignBudgetAnalysisData(
                id=1242,
                budget=Money(value=2000000),
                daily_budget=Money(value=300000),
                days_left=15,
            ),
        ]
    ).SerializeToString()
    adv_store_list_campaigns_for_budget_analysis_rmock(
        Response(body=adv_store_message, status=200)
    )
    setup_charged_db(
        ch_client,
        (
            (4242, 100, Decimal(100)),
            (4356, 150, Decimal(50)),
            (1242, 200, Decimal(100)),
        ),
    )

    got = await task.collect_campaign_data()

    assert got == [
        {
            "campaign_id": 4242,
            "budget": Decimal("200"),
            "charged": Decimal("100"),
            "daily_budget": Decimal("20"),
            "days_left": 5,
        },
        {
            "campaign_id": 4356,
            "budget": Decimal("300"),
            "charged": Decimal("50"),
            "daily_budget": Decimal("30"),
            "days_left": 10,
        },
        {
            "campaign_id": 1242,
            "budget": Decimal("200"),
            "charged": Decimal("100"),
            "daily_budget": Decimal("30"),
            "days_left": 15,
        },
    ]


async def test_returns_campaign_data_if_not_charged_yet(
    adv_store_list_campaigns_for_budget_analysis_rmock, ch_client, task
):
    adv_store_message = CampaignBudgetAnalysisList(
        campaigns=[
            CampaignBudgetAnalysisData(
                id=4242,
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                days_left=10,
            )
        ]
    ).SerializeToString()
    adv_store_list_campaigns_for_budget_analysis_rmock(
        Response(body=adv_store_message, status=200)
    )

    got = await task.collect_campaign_data()

    assert got == [
        {
            "campaign_id": 4242,
            "budget": Decimal("200"),
            "charged": Decimal("0"),
            "daily_budget": Decimal("20"),
            "days_left": 10,
        }
    ]
