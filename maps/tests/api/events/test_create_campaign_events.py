from maps_adv.adv_store.api.proto.error_pb2 import Error
from maps_adv.adv_store.api.proto.event_list_pb2 import (
    CampaignEventData,
    CampaignEventDataList,
    CampaignEventType,
)
from maps_adv.adv_store.v2.tests import Any, dt

API_URL = "/create-campaign-events/"


async def test_creates_campaign_events(api, factory):
    campaign_1_id = (await factory.create_campaign(order_id=1, name="Campaign 1"))["id"]
    campaign_2_id = (await factory.create_campaign(order_id=1, name="Campaign 2"))["id"]
    pb_input = CampaignEventDataList(
        events=[
            CampaignEventData(
                timestamp=dt("2020-06-01 00:00:00", as_proto=True),
                campaign_id=campaign_1_id,
                event_type=CampaignEventType.NOT_SPENDING_BUDGET,
            ),
            CampaignEventData(
                timestamp=dt("2020-06-01 00:00:00", as_proto=True),
                campaign_id=campaign_1_id,
                event_type=CampaignEventType.STOPPED_MANUALLY,
            ),
            CampaignEventData(
                timestamp=dt("2020-06-01 00:00:00", as_proto=True),
                campaign_id=campaign_2_id,
                event_type=CampaignEventType.NOT_SPENDING_BUDGET,
            ),
        ]
    )

    await api.post(API_URL, proto=pb_input, expected_status=200)

    got_1 = await factory.retrieve_campaign_events(campaign_id=campaign_1_id)
    assert got_1 == [
        {
            "id": Any(int),
            "timestamp": dt("2020-06-01 00:00:00"),
            "campaign_id": campaign_1_id,
            "event_type": "NOT_SPENDING_BUDGET",
            "event_data": {},
        },
        {
            "id": Any(int),
            "timestamp": dt("2020-06-01 00:00:00"),
            "campaign_id": campaign_1_id,
            "event_type": "STOPPED_MANUALLY",
            "event_data": {},
        },
    ]

    got_2 = await factory.retrieve_campaign_events(campaign_id=campaign_2_id)
    assert got_2 == [
        {
            "id": Any(int),
            "timestamp": dt("2020-06-01 00:00:00"),
            "campaign_id": campaign_2_id,
            "event_type": "NOT_SPENDING_BUDGET",
            "event_data": {},
        }
    ]


async def test_returns_error_if_no_campaign(api, factory):
    pb_input = CampaignEventDataList(
        events=[
            CampaignEventData(
                timestamp=dt("2020-06-01 00:00:00", as_proto=True),
                campaign_id=100500,
                event_type=CampaignEventType.NOT_SPENDING_BUDGET,
            )
        ]
    )

    got = await api.post(API_URL, proto=pb_input, decode_as=Error, expected_status=404)

    assert got == Error(code=Error.CAMPAIGN_NOT_FOUND, description="100500")
