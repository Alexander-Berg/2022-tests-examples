from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.api.proto.billing_pb2 import Money
from maps_adv.adv_store.api.proto.campaign_list_pb2 import (
    CampaignBudgetAnalysisData,
    CampaignBudgetAnalysisList,
)
from maps_adv.adv_store.api.proto.event_list_pb2 import (
    CampaignEventDataList,
    CampaignEventType,
)
from maps_adv.stat_tasks_starter.lib.not_spending_budget.task import (
    NotSpendingBudgetTask,
)
from maps_adv.stat_tasks_starter.tests.tools import setup_charged_db

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def pipeline(config, loop):
    return NotSpendingBudgetTask(config)


async def test_no_events_if_no_campaigns(
    pipeline,
    adv_store_list_campaigns_for_budget_analysis_rmock,
    adv_store_create_campaign_events_rmock,
    ch_client,
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
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path)
        req_details.update(body=await request.read())
        return Response(status=200)

    adv_store_create_campaign_events_rmock(_handler)

    await pipeline()

    assert req_details == {}  # was not called


async def test_creates_events_for_campaigns_not_spending_budget(
    pipeline,
    adv_store_list_campaigns_for_budget_analysis_rmock,
    adv_store_create_campaign_events_rmock,
    ch_client,
):
    adv_store_message = CampaignBudgetAnalysisList(
        campaigns=[
            CampaignBudgetAnalysisData(
                id=4242,
                budget=Money(value=2000000),
                daily_budget=Money(value=200000),
                days_left=4,
            ),
            CampaignBudgetAnalysisData(
                id=4356,
                budget=Money(value=3000000),
                daily_budget=Money(value=300000),
                days_left=5,
            ),
            CampaignBudgetAnalysisData(
                id=1242,
                budget=Money(value=2000000),
                daily_budget=Money(value=300000),
                days_left=1,
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
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path)
        req_details.update(body=await request.read())
        return Response(status=200)

    adv_store_create_campaign_events_rmock(_handler)

    await pipeline()

    proto_body = CampaignEventDataList.FromString(req_details["body"])

    campaign_ids = [event.campaign_id for event in proto_body.events]
    assert campaign_ids == [4242, 4356, 1242]
    for event in proto_body.events:
        assert event.event_type == CampaignEventType.NOT_SPENDING_BUDGET


async def test_does_not_creates_events_for_campaigns_spending_budget(
    pipeline,
    adv_store_list_campaigns_for_budget_analysis_rmock,
    adv_store_create_campaign_events_rmock,
    ch_client,
):
    adv_store_message = CampaignBudgetAnalysisList(
        campaigns=[
            CampaignBudgetAnalysisData(
                id=1,
                budget=Money(value=1000000),
                daily_budget=Money(value=10000),
                days_left=3,
            ),
            CampaignBudgetAnalysisData(
                id=2,
                budget=Money(value=10000000),
                daily_budget=Money(value=1000000),
                days_left=5,
            ),
        ]
    ).SerializeToString()

    adv_store_list_campaigns_for_budget_analysis_rmock(
        Response(body=adv_store_message, status=200)
    )
    setup_charged_db(ch_client, ((1, 100, Decimal(50)), (2, 150, Decimal(600))))
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path)
        req_details.update(body=await request.read())
        return Response(status=200)

    adv_store_create_campaign_events_rmock(_handler)

    await pipeline()

    proto_body = CampaignEventDataList.FromString(req_details["body"])

    campaign_ids = [event.campaign_id for event in proto_body.events]
    assert campaign_ids == [1]
    for event in proto_body.events:
        assert event.event_type == CampaignEventType.NOT_SPENDING_BUDGET
