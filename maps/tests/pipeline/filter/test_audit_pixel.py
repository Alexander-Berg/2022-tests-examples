import pytest

from maps_adv.export.lib.pipeline.filter import AuditPixelCampaignTypeFilter
from maps_adv.export.lib.core.enum import CampaignType

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "campaigns, expected_campaigns",
    [
        (
            [
                {
                    "id": 1,
                    "settings": {"verification_data": []},
                    "campaign_type": CampaignType.ZERO_SPEED_BANNER,
                },
                {  # shouldn't be filtered out
                    "id": 2,
                    "settings": {"verification_data": []},
                    "campaign_type": CampaignType.BILLBOARD,
                },
                {
                    "id": 3,
                    "campaign_type": CampaignType.BILLBOARD,
                },
                {
                    "id": 4,
                    "campaign_type": CampaignType.ZERO_SPEED_BANNER,
                },
                {
                    "id": 5,
                    "settings": {"verification_data": []},
                    "campaign_type": CampaignType.OVERVIEW_BANNER,
                },
                {  # should be filtered out
                    "id": 6,
                    "settings": {"verification_data": [{}]},
                    "campaign_type": CampaignType.BILLBOARD,
                },
            ],
            [
                {
                    "id": 1,
                    "settings": {"verification_data": []},
                    "campaign_type": CampaignType.ZERO_SPEED_BANNER,
                },
                {
                    "id": 2,
                    "settings": {"verification_data": []},
                    "campaign_type": CampaignType.BILLBOARD,
                },
                {
                    "id": 3,
                    "campaign_type": CampaignType.BILLBOARD,
                },
                {
                    "id": 4,
                    "campaign_type": CampaignType.ZERO_SPEED_BANNER,
                },
                {
                    "id": 5,
                    "settings": {"verification_data": []},
                    "campaign_type": CampaignType.OVERVIEW_BANNER,
                },
            ],
        )
    ],
)
async def test_filters_audit_pixel_not_zbf_filter(campaigns, expected_campaigns):
    campaigns = await AuditPixelCampaignTypeFilter()(campaigns)

    assert campaigns == expected_campaigns
