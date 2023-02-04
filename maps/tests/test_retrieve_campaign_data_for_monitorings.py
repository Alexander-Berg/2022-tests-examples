import pytest
from aiohttp.web import Response

from maps_adv.adv_store.client import (
    Client,
    UnknownResponse,
)
from maps_adv.adv_store.api.proto.campaign_list_pb2 import (
    CampaignDataForMonitorings,
    CampaignDataForMonitoringsList,
    CampaignIdList,
)
from maps_adv.common.client.lib.client import REQUEST_MAX_ATTEMPTS
from maps_adv.common.client.lib.exceptions import (
    BadGateway,
    ServiceUnavailable,
)
from maps_adv.common.helpers.enums import CampaignTypeEnum
from maps_adv.common.proto import campaign_pb2

pytestmark = [pytest.mark.asyncio]

example_proto = CampaignDataForMonitoringsList(
    campaigns=[
        CampaignDataForMonitorings(
            id=111, campaign_type=campaign_pb2.CampaignType.ZERO_SPEED_BANNER
        ),
        CampaignDataForMonitorings(
            id=222, campaign_type=campaign_pb2.CampaignType.OVERVIEW_BANNER
        ),
    ]
)

example_result = [
    {"id": 111, "campaign_type": CampaignTypeEnum.ZERO_SPEED_BANNER},
    {"id": 222, "campaign_type": CampaignTypeEnum.OVERVIEW_BANNER},
]


async def test_requests_data_correctly(mock_retrieve_campaign_data_for_monitorings):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path)
        return Response(status=200, body=example_proto.SerializeToString())

    mock_retrieve_campaign_data_for_monitorings(_handler)

    async with Client("http://adv_store.server") as client:
        await client.retrieve_campaign_data_for_monitorings([111, 222])

    assert req_details["path"] == "/v2/campaigns/monitoring-info/"


async def test_returns_empty_list_if_server_returns_nothing(
    mock_retrieve_campaign_data_for_monitorings,
):
    proto = CampaignIdList(ids=[])
    mock_retrieve_campaign_data_for_monitorings(
        Response(status=200, body=proto.SerializeToString())
    )

    async with Client("http://adv_store.server") as client:
        got = await client.retrieve_campaign_data_for_monitorings([111, 222])

    assert got == []


async def test_parse_response_data_correctly(
    mock_retrieve_campaign_data_for_monitorings,
):
    mock_retrieve_campaign_data_for_monitorings(
        Response(status=200, body=example_proto.SerializeToString())
    )

    async with Client("http://adv_store.server") as client:
        got = await client.retrieve_campaign_data_for_monitorings([111, 222])

    assert got == example_result


async def test_raises_for_unexpected_status(
    mock_retrieve_campaign_data_for_monitorings,
):
    mock_retrieve_campaign_data_for_monitorings(Response(status=409))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(UnknownResponse):
            await client.retrieve_campaign_data_for_monitorings([111, 222])


@pytest.mark.parametrize(
    "status, expected_exc", ([502, BadGateway], [503, ServiceUnavailable])
)
async def test_raises_for_expected_statuses_if_retrying_fails(
    status, expected_exc, mock_retrieve_campaign_data_for_monitorings
):
    for _ in range(REQUEST_MAX_ATTEMPTS):
        mock_retrieve_campaign_data_for_monitorings(Response(status=status))

    async with Client("http://adv_store.server") as client:

        with pytest.raises(expected_exc):
            await client.retrieve_campaign_data_for_monitorings([111, 222])


@pytest.mark.parametrize("status", (502, 503))
async def test_returns_result_if_retries_successfully(
    status, mock_retrieve_campaign_data_for_monitorings
):
    for _ in range(REQUEST_MAX_ATTEMPTS - 1):
        mock_retrieve_campaign_data_for_monitorings(Response(status=status))
    mock_retrieve_campaign_data_for_monitorings(
        Response(status=200, body=example_proto.SerializeToString())
    )

    async with Client("http://adv_store.server") as client:
        got = await client.retrieve_campaign_data_for_monitorings([111, 222])

    assert got == example_result
