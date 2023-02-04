from datetime import datetime

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.api.schemas.enums import ReasonCampaignStoppedEnum
from maps_adv.adv_store.client import (
    CampaignsNotFound,
    Client,
    UnexpectedNaiveDatetime,
    UnknownResponse,
)
from maps_adv.adv_store.api.proto import charger_api_pb2
from maps_adv.adv_store.api.proto.error_pb2 import Error
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "campaigns, campaigns_proto",
    [
        ({}, []),
        (
            {
                1111: ReasonCampaignStoppedEnum.BUDGET_REACHED,
            },
            [
                charger_api_pb2.CampaignToStop(
                    campaign_id=1111,
                    reason_stopped=charger_api_pb2.ReasonStopped.BUDGET_REACHED,
                ),
            ],
        ),
    ],
)
async def test_requests_data_correctly(
    campaigns, campaigns_proto, mock_stop_campaigns_api
):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path)
        req_details.update(body=await request.read())
        return Response(status=200)

    mock_stop_campaigns_api(_handler)

    async with Client("http://adv_store.server") as client:
        await client.stop_campaigns(
            processed_at=dt("2020-02-20 11:00:00"), campaigns_to_stop=campaigns
        )

    assert req_details["path"] == "/v2/campaigns/stop/"
    proto_body = charger_api_pb2.CampaignsToStopList.FromString(req_details["body"])
    assert proto_body == charger_api_pb2.CampaignsToStopList(
        processed_at=dt("2020-02-20 11:00:00", as_proto=True), campaigns=campaigns_proto
    )


async def test_returns_nothing(mock_stop_campaigns_api):
    mock_stop_campaigns_api(Response(status=200))

    async with Client("http://adv_store.server") as client:
        got = await client.stop_campaigns(
            processed_at=dt("2020-02-20 11:00:00"),
            campaigns_to_stop={
                1111: ReasonCampaignStoppedEnum.BUDGET_REACHED,
            },
        )

    assert got is None


async def test_raises_for_unexpected_status(mock_stop_campaigns_api):
    mock_stop_campaigns_api(Response(status=409))

    async with Client("http://adv_store.server") as client:
        with pytest.raises(UnknownResponse):
            await client.stop_campaigns(
                processed_at=dt("2020-02-20 11:00:00"),
                campaigns_to_stop={
                    1111: ReasonCampaignStoppedEnum.BUDGET_REACHED,
                },
            )


async def test_raises_if_some_of_campaigns_not_found(mock_stop_campaigns_api):
    mock_stop_campaigns_api(
        Response(
            status=404,
            body=Error(
                code=Error.CAMPAIGN_NOT_FOUND, description="1111, 2222"
            ).SerializeToString(),
        )
    )

    async with Client("http://adv_store.server") as client:
        with pytest.raises(CampaignsNotFound) as exc_info:
            await client.stop_campaigns(
                processed_at=dt("2020-02-20 11:00:00"),
                campaigns_to_stop={
                    1111: ReasonCampaignStoppedEnum.BUDGET_REACHED,
                },
            )

    assert exc_info.value.args == ("1111, 2222",)


async def test_raises_for_naive_datetime():
    async with Client("http://adv_store.server") as client:
        with pytest.raises(UnexpectedNaiveDatetime) as exc_info:
            await client.stop_campaigns(
                processed_at=datetime(2020, 2, 2, 12, 00),
                campaigns_to_stop={
                    1111: ReasonCampaignStoppedEnum.BUDGET_REACHED,
                },
            )

    assert exc_info.value.args == (datetime(2020, 2, 2, 12, 0),)


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_stop_campaigns_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_stop_campaigns_api(Response(status=status))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(expected_exc):
            await client.stop_campaigns(
                processed_at=dt("2020-02-20 11:00:00"),
                campaigns_to_stop={
                    1111: ReasonCampaignStoppedEnum.BUDGET_REACHED,
                },
            )


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_200_if_retries_successfully(status, mock_stop_campaigns_api):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_stop_campaigns_api(Response(status=status))
    mock_stop_campaigns_api(Response(status=200))

    async with Client("http://adv_store.server") as client:
        await client.stop_campaigns(
            processed_at=dt("2020-02-20 11:00:00"),
            campaigns_to_stop={
                1111: ReasonCampaignStoppedEnum.BUDGET_REACHED,
            },
        )
