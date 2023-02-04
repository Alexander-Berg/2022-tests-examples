from datetime import datetime

import pytest
from aiohttp.web import Response

from maps_adv.adv_store.client import (
    BadGateway,
    CampaignsNotFound,
    Client,
    ServiceUnavailable,
    UnexpectedNaiveDatetime,
)

from maps_adv.adv_store.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.adv_store.api.proto import campaign_pb2

from maps_adv.adv_store.api.proto.error_pb2 import Error
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]

CAMPAIGN_ID = 100


async def test_updates_campaign(mock_set_paid_till_api):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path)
        req_details.update(body=await request.read())
        return Response(status=200)

    mock_set_paid_till_api(CAMPAIGN_ID, _handler)

    async with Client("http://adv_store.server") as client:
        await client.set_paid_till(CAMPAIGN_ID, dt("2020-02-20 11:00:00"))

    assert req_details["path"] == f"/v2/campaigns/{CAMPAIGN_ID}/paid-till/"

    proto_body = campaign_pb2.CampaignPaidTillChangeInput().FromString(
        req_details["body"]
    )
    assert proto_body == campaign_pb2.CampaignPaidTillChangeInput(
        paid_till=dt("2020-02-20 11:00:00", as_proto=True)
    )


async def test_raises_campaign_not_fount(mock_set_paid_till_api):
    mock_set_paid_till_api(
        CAMPAIGN_ID,
        Response(
            status=404,
            body=Error(
                code=Error.CAMPAIGN_NOT_FOUND, description=f"{CAMPAIGN_ID}"
            ).SerializeToString(),
        ),
    )

    async with Client("http://adv_store.server") as client:
        with pytest.raises(CampaignsNotFound) as exc_info:
            await client.set_paid_till(CAMPAIGN_ID, dt("2020-02-20 11:00:00"))

    assert exc_info.value.args == (f"{CAMPAIGN_ID}",)


async def test_raises_for_naive_datetime():
    async with Client("http://adv_store.server") as client:
        with pytest.raises(UnexpectedNaiveDatetime) as exc_info:
            await client.set_paid_till(
                CAMPAIGN_ID,
                datetime(2020, 2, 2, 12, 00),
            )

    assert exc_info.value.args == (datetime(2020, 2, 2, 12, 0),)


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_set_paid_till_api
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_set_paid_till_api(CAMPAIGN_ID, Response(status=status))

    async with Client("http://adv_store.server") as client:
        with pytest.raises(expected_exc):
            await client.set_paid_till(
                CAMPAIGN_ID,
                dt("2020-02-20 11:00:00"),
            )


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_200_if_retries_successfully(status, mock_set_paid_till_api):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_set_paid_till_api(CAMPAIGN_ID, Response(status=status))
    mock_set_paid_till_api(CAMPAIGN_ID, Response(status=200))

    async with Client("http://adv_store.server") as client:
        await client.set_paid_till(
            CAMPAIGN_ID,
            dt("2020-02-20 11:00:00"),
        )
