import pytest

from maps_adv.adv_store.api.proto.campaign_list_pb2 import (
    CampaignDataForMonitorings,
    CampaignDataForMonitoringsList,
    CampaignIdList,
)
from maps_adv.common.proto.campaign_pb2 import CampaignType

pytestmark = [pytest.mark.asyncio]

API_URL = "/campaigns/monitoring-info/"


async def test_campaigns_returned(api, factory):
    campaign_id_1 = (await factory.create_campaign())["id"]
    campaign_id_2 = (await factory.create_campaign())["id"]
    await factory.create_campaign()
    input_pb = CampaignIdList(ids=[campaign_id_1, campaign_id_2])

    got = await api.post(
        API_URL,
        proto=input_pb,
        decode_as=CampaignDataForMonitoringsList,
        expected_status=200,
    )

    assert got == CampaignDataForMonitoringsList(
        campaigns=[
            CampaignDataForMonitorings(
                id=campaign_id_1, campaign_type=CampaignType.ZERO_SPEED_BANNER
            ),
            CampaignDataForMonitorings(
                id=campaign_id_2, campaign_type=CampaignType.ZERO_SPEED_BANNER
            ),
        ]
    )


async def test_returns_nothing_if_no_ids_passed(api, factory):
    input_pb = CampaignIdList(ids=[])

    got = await api.post(
        API_URL,
        proto=input_pb,
        decode_as=CampaignDataForMonitoringsList,
        expected_status=200,
    )

    assert got == CampaignDataForMonitoringsList(campaigns=[])


async def test_returns_nothing_if_ids_for_unknown_campaigns_passed(api, factory):
    input_pb = CampaignIdList(ids=[111, 222])

    got = await api.post(
        API_URL,
        proto=input_pb,
        decode_as=CampaignDataForMonitoringsList,
        expected_status=200,
    )

    assert got == CampaignDataForMonitoringsList(campaigns=[])
