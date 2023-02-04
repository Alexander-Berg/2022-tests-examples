from decimal import Decimal

import pytest
from aiohttp.web import Response

from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.common.helpers import dt
from maps_adv.statistics.dashboard.client import (
    Client,
    NoCampaignsPassed,
    NoStatistics,
    UnknownResponse,
)
from maps_adv.statistics.dashboard.proto.campaign_stat_pb2 import (
    CampaignChargedSum,
    CampaignChargedSumInput,
    CampaignChargedSumOutput,
)

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "now, on_datetime, campaign_ids, expected_request_body",
    [
        (
            dt("2020-02-20 20:20:20"),
            None,
            [1, 2, 3],
            CampaignChargedSumInput(campaign_ids=[1, 2, 3], on_timestamp=1582230020),
        ),
        (
            dt("2020-02-20 20:20:20"),
            dt("2020-02-20 20:20:00"),
            [1],
            CampaignChargedSumInput(campaign_ids=[1], on_timestamp=1582230000),
        ),
    ],
)
async def test_requests_data_correctly(
    now, on_datetime, campaign_ids, expected_request_body, mock_charged_sum_api, freezer
):
    freezer.move_to(now)

    async def _handler(request):
        assert (
            CampaignChargedSumInput.FromString(await request.read())
            == expected_request_body
        )
        return Response(body=CampaignChargedSumOutput().SerializeToString(), status=200)

    mock_charged_sum_api(_handler)

    async with Client("http://dashboard.server") as client:
        await client.campaigns_charged_sum(*campaign_ids, on_datetime=on_datetime)


@pytest.mark.parametrize(
    "campaigns_pbs, expected",
    (
        [[CampaignChargedSum(campaign_id=10, charged_sum="1.1")], {10: Decimal("1.1")}],
        [[CampaignChargedSum(campaign_id=20, charged_sum="5")], {20: Decimal("5")}],
        [
            [
                CampaignChargedSum(campaign_id=10, charged_sum="1.1"),
                CampaignChargedSum(campaign_id=20, charged_sum="5"),
            ],
            {10: Decimal("1.1"), 20: Decimal("5")},
        ],
    ),
)
async def test_parses_response_correctly(campaigns_pbs, expected, mock_charged_sum_api):
    mock_charged_sum_api(
        Response(
            body=CampaignChargedSumOutput(
                campaigns_charged_sums=campaigns_pbs
            ).SerializeToString(),
            status=200,
        )
    )

    async with Client("http://dashboard.server") as client:
        got = await client.campaigns_charged_sum(1, 2, 3)

    assert got == expected


async def test_raises_if_server_returns_nothing(mock_charged_sum_api):
    mock_charged_sum_api(Response(status=204))

    async with Client("http://dashboard.server") as client:

        with pytest.raises(NoStatistics):
            await client.campaigns_charged_sum(1, 2, 3)


async def test_raises_if_no_campaigns_passed():
    async with Client("http://dashboard.server") as client:

        with pytest.raises(NoCampaignsPassed):
            await client.campaigns_charged_sum()


async def test_raises_for_unexpected_status(mock_charged_sum_api):
    mock_charged_sum_api(Response(status=409))

    async with Client("http://dashboard.server") as client:

        with pytest.raises(UnknownResponse):
            await client.campaigns_charged_sum(1, 2, 3)


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_charged_sum_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_charged_sum_api(Response(status=status))

    async with Client("http://dashboard.server") as client:

        with pytest.raises(expected_exc):
            await client.campaigns_charged_sum(1, 2, 3)


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(status, mock_charged_sum_api):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_charged_sum_api(Response(status=status))
    campaign_pb = CampaignChargedSum(campaign_id=10, charged_sum="1.1")
    mock_charged_sum_api(
        Response(
            body=CampaignChargedSumOutput(
                campaigns_charged_sums=[campaign_pb]
            ).SerializeToString(),
            status=200,
        )
    )

    async with Client("http://dashboard.server") as client:
        got = await client.campaigns_charged_sum(10)

    assert got == {10: Decimal("1.1")}
