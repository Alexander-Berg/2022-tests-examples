import pytest

from maps_adv.export.lib.core.enum import CampaignType
from maps_adv.export.lib.pipeline.resolver.polygons import polygons_resolver

pytestmark = [pytest.mark.asyncio]


SUPPORTED_CAMPAIGN_TYPES = [
    CampaignType.ZERO_SPEED_BANNER,
    CampaignType.OVERVIEW_BANNER,
    CampaignType.PROMOCODE,
    CampaignType.ROUTE_BANNER,
]
UNSUPPORTED_CAMPAIGN_TYPES = list(set(CampaignType) - set(SUPPORTED_CAMPAIGN_TYPES))


@pytest.mark.parametrize("campaign_type", SUPPORTED_CAMPAIGN_TYPES)
async def test_expected_resolve_polygons_for_supported_campaigns(
    campaign_type: CampaignType,
):
    campaigns = [
        dict(
            id=1,
            campaign_type=campaign_type,
            placing=dict(
                area={
                    "version": 1,
                    "areas": [
                        {
                            "points": [
                                {"longitude": 25, "latitude": 25},
                                {"longitude": 55, "latitude": 20},
                                {"longitude": 30, "latitude": -15},
                            ]
                        },
                        {
                            "points": [
                                {"longitude": 35, "latitude": 35},
                                {"longitude": 55.1, "latitude": 20.1},
                                {"longitude": 53, "latitude": 53},
                            ]
                        },
                    ],
                }
            ),
        )
    ]
    polygons = await polygons_resolver(campaigns)

    assert campaigns[0]["polygons"] == ["campaign:1.1", "campaign:1.2"]
    assert polygons == {
        "campaign:1.1": dict(
            id="campaign:1.1",
            points=[
                {"longitude": 25, "latitude": 25},
                {"longitude": 55, "latitude": 20},
                {"longitude": 30, "latitude": -15},
            ],
        ),
        "campaign:1.2": dict(
            id="campaign:1.2",
            points=[
                {"longitude": 35, "latitude": 35},
                {"longitude": 55.1, "latitude": 20.1},
                {"longitude": 53, "latitude": 53},
            ],
        ),
    }


@pytest.mark.parametrize("campaign_type", UNSUPPORTED_CAMPAIGN_TYPES)
async def test_no_change_unsupported_campaign(campaign_type: CampaignType):
    campaigns = [
        dict(
            id=1,
            campaign_type=campaign_type,
            placing=dict(
                area={
                    "version": 1,
                    "areas": [
                        {
                            "points": [
                                {"longitude": 25, "latitude": 25},
                                {"longitude": 55, "latitude": 20},
                                {"longitude": 30, "latitude": -15},
                            ]
                        },
                        {
                            "points": [
                                {"longitude": 35, "latitude": 35},
                                {"longitude": 55.1, "latitude": 20.1},
                                {"longitude": 53, "latitude": 53},
                            ]
                        },
                    ],
                }
            ),
        )
    ]

    polygons = await polygons_resolver(campaigns)

    assert campaigns[0]["polygons"] == []
    assert polygons == {}


@pytest.mark.parametrize("campaign_type", SUPPORTED_CAMPAIGN_TYPES)
async def test_raises_for_campaign_without_areas_in_placing(
    campaign_type: CampaignType,
):
    campaigns = [dict(id=1, campaign_type=campaign_type, placing=dict())]

    with pytest.raises(KeyError):
        await polygons_resolver(campaigns)
