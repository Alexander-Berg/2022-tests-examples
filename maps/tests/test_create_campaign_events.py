import pytest
from aiohttp.web import Response

from maps_adv.adv_store.client import CampaignsNotFound, Client
from maps_adv.adv_store.api.schemas.enums import CampaignEventTypeEnum
from maps_adv.adv_store.api.proto import event_list_pb2
from maps_adv.adv_store.api.proto.error_pb2 import Error
from maps_adv.adv_store.api.proto.event_list_pb2 import (
    CampaignEventData,
    CampaignEventDataList,
)
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


async def test_requests_data_correctly(mock_create_campaign_events_api):
    req_details = {}

    async def _handler(request):
        req_details.update(path=request.path)
        req_details.update(body=await request.read())
        return Response(status=200)

    mock_create_campaign_events_api(_handler)

    events = [
        {
            "timestamp": dt("2020-06-01 00:00:00"),
            "campaign_id": 1,
            "event_type": CampaignEventTypeEnum.NOT_SPENDING_BUDGET,
        }
    ]
    async with Client("http://adv_store.server") as client:
        await client.create_campaign_events(events=events)

    assert req_details["path"] == "/v2/create-campaign-events/"
    proto_body = CampaignEventDataList.FromString(req_details["body"])
    assert proto_body == CampaignEventDataList(
        events=[
            CampaignEventData(
                timestamp=dt("2020-06-01 00:00:00", as_proto=True),
                campaign_id=1,
                event_type=event_list_pb2.CampaignEventType.NOT_SPENDING_BUDGET,
            )
        ]
    )


async def test_raises_if_some_of_campaigns_not_found(mock_create_campaign_events_api):
    mock_create_campaign_events_api(
        Response(
            status=404,
            body=Error(
                code=Error.CAMPAIGN_NOT_FOUND, description="1111, 2222"
            ).SerializeToString(),
        )
    )

    events = [
        {
            "timestamp": dt("2020-06-01 00:00:00"),
            "campaign_id": 1111,
            "event_type": CampaignEventTypeEnum.NOT_SPENDING_BUDGET,
        },
        {
            "timestamp": dt("2020-06-01 00:00:00"),
            "campaign_id": 2222,
            "event_type": CampaignEventTypeEnum.NOT_SPENDING_BUDGET,
        },
    ]

    async with Client("http://adv_store.server") as client:
        with pytest.raises(CampaignsNotFound) as exc_info:
            await client.create_campaign_events(events=events)

    assert exc_info.value.args == ("1111, 2222",)
