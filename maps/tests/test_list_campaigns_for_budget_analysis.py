from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.client import (
    Client,
    UnknownResponse,
)
from maps_adv.adv_store.api.proto.billing_pb2 import Money
from maps_adv.adv_store.api.proto.campaign_list_pb2 import (
    CampaignBudgetAnalysisData,
    CampaignBudgetAnalysisList,
)
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)

pytestmark = [pytest.mark.asyncio]

example_proto = CampaignBudgetAnalysisList(
    campaigns=[
        CampaignBudgetAnalysisData(
            id=4242,
            budget=Money(value=2345600),
            daily_budget=Money(value=200000),
            days_left=10,
        ),
        CampaignBudgetAnalysisData(
            id=4356,
            budget=Money(value=3000000),
            daily_budget=Money(value=345600),
            days_left=20,
        ),
        CampaignBudgetAnalysisData(
            id=1242,
            budget=Money(value=2345600),
            daily_budget=Money(value=345600),
            days_left=30,
        ),
    ]
)

example_result = [
    {
        "campaign_id": 4242,
        "budget": Decimal("234.56"),
        "daily_budget": Decimal("20"),
        "days_left": 10,
    },
    {
        "campaign_id": 4356,
        "budget": Decimal("300"),
        "daily_budget": Decimal("34.56"),
        "days_left": 20,
    },
    {
        "campaign_id": 1242,
        "budget": Decimal("234.56"),
        "daily_budget": Decimal("34.56"),
        "days_left": 30,
    },
]


async def test_requests_data_correctly(mock_list_campaigns_for_budget_analysis):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path)
        return Response(status=200, body=example_proto.SerializeToString())

    mock_list_campaigns_for_budget_analysis(_handler)

    async with Client("http://adv_store.server") as client:
        await client.list_campaigns_for_budget_analysis()

    assert req_details["path"] == "/v2/campaigns/budget-analysis/"


async def test_returns_empty_list_if_server_returns_nothing(
    mock_list_campaigns_for_budget_analysis,
):
    proto = CampaignBudgetAnalysisList(campaigns=[])
    mock_list_campaigns_for_budget_analysis(
        Response(status=200, body=proto.SerializeToString())
    )

    async with Client("http://adv_store.server") as client:
        got = await client.list_campaigns_for_budget_analysis()

    assert got == []


async def test_parse_response_data_correctly(mock_list_campaigns_for_budget_analysis):
    mock_list_campaigns_for_budget_analysis(
        Response(status=200, body=example_proto.SerializeToString())
    )

    async with Client("http://adv_store.server") as client:
        got = await client.list_campaigns_for_budget_analysis()

    assert got == example_result


async def test_raises_for_unexpected_status(mock_list_campaigns_for_budget_analysis):
    mock_list_campaigns_for_budget_analysis(Response(status=409))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(UnknownResponse):
            await client.list_campaigns_for_budget_analysis()


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_list_campaigns_for_budget_analysis
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_list_campaigns_for_budget_analysis(Response(status=status))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(expected_exc):
            await client.list_campaigns_for_budget_analysis()


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(
    status, mock_list_campaigns_for_budget_analysis
):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_list_campaigns_for_budget_analysis(Response(status=status))
    mock_list_campaigns_for_budget_analysis(
        Response(status=200, body=example_proto.SerializeToString())
    )

    async with Client("http://adv_store.server") as client:
        got = await client.list_campaigns_for_budget_analysis()

    assert got == example_result
