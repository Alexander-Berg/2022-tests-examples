import pytest

from maps_adv.export.lib.core.enum import CampaignType, CreativeType
from maps_adv.export.lib.pipeline.filter.campaign import CampaignWithoutPlacesFilter

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def campaign_filter():
    return CampaignWithoutPlacesFilter()


@pytest.mark.parametrize(
    "campaigns, expected_campaigns",
    [
        (
            [
                {  # should be filtered
                    "id": 1242,
                    "campaign_type": CampaignType.PIN_ON_ROUTE,
                    "placing": {"organizations": []},
                    "creatives": {},
                },
                {
                    "id": 7543,
                    "campaign_type": CampaignType.PIN_ON_ROUTE,
                    "placing": {"organizations": [1234, 2135, 345]},
                    "creatives": {},
                },
                {
                    "id": 2453,
                    "campaign_type": CampaignType.PIN_ON_ROUTE,
                    "placing": {"organizations": [1234, 2135, 345]},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title1", "organizations": []}
                        ]
                    },
                },
                {  # should NOT be filtered
                    "id": 8764,
                    "campaign_type": CampaignType.PIN_SEARCH,
                    "placing": {"organizations": []},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title1", "organizations": []},
                            {"title": "title2", "organizations": [324, 345]},
                        ]
                    },
                },
                {  # should NOT be filtered
                    "id": 8764,
                    "campaign_type": CampaignType.CATEGORY,
                    "placing": {},
                    "creatives": {
                        CreativeType.ICON: {"organizations": [2134, 23142, 4256]}
                    },
                },
                {  # should be filtered
                    "id": 9786,
                    "campaign_type": CampaignType.PIN_SEARCH,
                    "placing": {"organizations": [2134, 23142, 4256]},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title1", "organizations": []},
                            {"title": "title2", "organizations": []},
                        ]
                    },
                },
                {  # should be filtered
                    "id": 9787,
                    "campaign_type": CampaignType.CATEGORY,
                    "placing": {"organizations": []},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title1", "organizations": [2134, 23142]}
                        ]
                    },
                },
                {
                    "id": 3257,
                    "campaign_type": CampaignType.PIN_SEARCH,
                    "placing": {"organizations": [2134, 23142, 4256]},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title", "organizations": [234, 345]}
                        ],
                        CreativeType.PIN: [{"title": "title", "subtitle": "subtitle"}],
                    },
                },
                {  # should be filtered
                    "id": 9875,
                    "campaign_type": CampaignType.PIN_SEARCH,
                    "placing": {"organizations": [213, 35345]},
                    "creatives": {
                        CreativeType.PIN: [{"title": "title", "subtitle": "subtitle"}]
                    },
                },
                {
                    "id": 7864,
                    "campaign_type": CampaignType.BILLBOARD,
                    "placing": {"organizations": []},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title", "organizations": [2134, 23142, 4256]}
                        ]
                    },
                    "places": {
                        33: {"longitude": 25.0, "latitude": 25.0, "id": 33},
                        55: {"longitude": -65.0, "latitude": 20.0, "id": 55},
                    },
                },
                {
                    "id": 4578,
                    "campaign_type": CampaignType.ZERO_SPEED_BANNER,
                    "placing": {"organizations": [234, 53425, 3425]},
                    "creatives": {
                        CreativeType.BANNER: [
                            {
                                "images": "title",
                                "title": "banner title",
                                "description": "banner description",
                                "disclaimer": "banner disclaimer",
                                "show_ads_label": True,
                            }
                        ]
                    },
                    "places": {
                        33: {"longitude": 25.0, "latitude": 25.0, "id": 33},
                        55: {"longitude": -65.0, "latitude": 20.0, "id": 55},
                    },
                },
                {
                    "id": 4579,
                    "campaign_type": CampaignType.OVERVIEW_BANNER,
                    "placing": {"organizations": [234, 53425, 3425]},
                    "creatives": {
                        CreativeType.BANNER: [
                            {
                                "images": "title",
                                "title": "banner title",
                                "description": "banner description",
                                "disclaimer": "banner disclaimer",
                                "show_ads_label": True,
                            }
                        ]
                    },
                    "places": {
                        33: {"longitude": 25.0, "latitude": 25.0, "id": 33},
                        55: {"longitude": -65.0, "latitude": 20.0, "id": 55},
                    },
                },
            ],
            [
                {
                    "id": 7543,
                    "campaign_type": CampaignType.PIN_ON_ROUTE,
                    "placing": {"organizations": [1234, 2135, 345]},
                    "creatives": {},
                },
                {
                    "id": 2453,
                    "campaign_type": CampaignType.PIN_ON_ROUTE,
                    "placing": {"organizations": [1234, 2135, 345]},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title1", "organizations": []}
                        ]
                    },
                },
                {
                    "id": 8764,
                    "campaign_type": CampaignType.PIN_SEARCH,
                    "placing": {"organizations": []},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title1", "organizations": []},
                            {"title": "title2", "organizations": [324, 345]},
                        ]
                    },
                },
                {
                    "id": 8764,
                    "campaign_type": CampaignType.CATEGORY,
                    "placing": {},
                    "creatives": {
                        CreativeType.ICON: {"organizations": [2134, 23142, 4256]}
                    },
                },
                {
                    "id": 3257,
                    "campaign_type": CampaignType.PIN_SEARCH,
                    "placing": {"organizations": [2134, 23142, 4256]},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title", "organizations": [234, 345]}
                        ],
                        CreativeType.PIN: [{"title": "title", "subtitle": "subtitle"}],
                    },
                },
                {
                    "id": 7864,
                    "campaign_type": CampaignType.BILLBOARD,
                    "placing": {"organizations": []},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title", "organizations": [2134, 23142, 4256]}
                        ]
                    },
                    "places": {
                        33: {"longitude": 25.0, "latitude": 25.0, "id": 33},
                        55: {"longitude": -65.0, "latitude": 20.0, "id": 55},
                    },
                },
                {
                    "id": 4578,
                    "campaign_type": CampaignType.ZERO_SPEED_BANNER,
                    "placing": {"organizations": [234, 53425, 3425]},
                    "creatives": {
                        CreativeType.BANNER: [
                            {
                                "images": "title",
                                "title": "banner title",
                                "description": "banner description",
                                "disclaimer": "banner disclaimer",
                                "show_ads_label": True,
                            }
                        ]
                    },
                    "places": {
                        33: {"longitude": 25.0, "latitude": 25.0, "id": 33},
                        55: {"longitude": -65.0, "latitude": 20.0, "id": 55},
                    },
                },
                {
                    "id": 4579,
                    "campaign_type": CampaignType.OVERVIEW_BANNER,
                    "placing": {"organizations": [234, 53425, 3425]},
                    "creatives": {
                        CreativeType.BANNER: [
                            {
                                "images": "title",
                                "title": "banner title",
                                "description": "banner description",
                                "disclaimer": "banner disclaimer",
                                "show_ads_label": True,
                            }
                        ]
                    },
                    "places": {
                        33: {"longitude": 25.0, "latitude": 25.0, "id": 33},
                        55: {"longitude": -65.0, "latitude": 20.0, "id": 55},
                    },
                },
            ],
        ),
        (
            [
                {  # should be filtered
                    "id": 1242,
                    "campaign_type": CampaignType.PIN_ON_ROUTE,
                    "placing": {"organizations": []},
                    "creatives": {},
                },
                {  # should be filtered
                    "id": 9786,
                    "campaign_type": CampaignType.PIN_SEARCH,
                    "placing": {"organizations": [2134, 23142, 4256]},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title1", "organizations": []},
                            {"title": "title2", "organizations": []},
                        ]
                    },
                },
                {  # should be filtered
                    "id": 9875,
                    "campaign_type": CampaignType.PIN_SEARCH,
                    "placing": {"organizations": [213, 35345]},
                    "creatives": {
                        CreativeType.PIN: [{"title": "title", "subtitle": "subtitle"}]
                    },
                },
                {  # should be filtered
                    "id": 9876,
                    "campaign_type": CampaignType.CATEGORY,
                    "placing": {"organizations": []},
                    "creatives": {
                        CreativeType.PIN: [{"title": "title", "subtitle": "subtitle"}]
                    },
                },
                {  # should be filtered
                    "id": 9877,
                    "campaign_type": CampaignType.CATEGORY,
                    "placing": {"organizations": []},
                    "creatives": {
                        CreativeType.PIN_SEARCH: [
                            {"title": "title1", "organizations": [2134, 23142]}
                        ]
                    },
                },
                {  # should be filtered
                    "id": 9877,
                    "campaign_type": CampaignType.ROUTE_VIA_POINT,
                    "placing": {"organizations": []},
                    "creatives": {},
                },
            ],
            [],
        ),
    ],
)
async def test_filters_campaigns_without_companies(
    campaign_filter, campaigns, expected_campaigns
):
    campaigns = await campaign_filter(campaigns)

    assert campaigns == expected_campaigns


async def test_does_nothing_if_empty_campaigns(campaign_filter):
    campaigns = await campaign_filter([])

    assert campaigns == []


@pytest.mark.parametrize(
    "campaigns, expected_campaigns",
    [
        (
            [
                {
                    "id": 4580,
                    "campaign_type": CampaignType.BILLBOARD,
                    "places": {
                        33: {"longitude": 25.0, "latitude": 25.0, "id": 33},
                        55: {"longitude": -65.0, "latitude": 20.0, "id": 55},
                    },
                    "creatives": {},
                },
                # should be filtered
                {
                    "id": 7865,
                    "campaign_type": CampaignType.BILLBOARD,
                    "places": {},
                    "creatives": {},
                },
            ],
            [
                {
                    "id": 4580,
                    "campaign_type": CampaignType.BILLBOARD,
                    "places": {
                        33: {"longitude": 25.0, "latitude": 25.0, "id": 33},
                        55: {"longitude": -65.0, "latitude": 20.0, "id": 55},
                    },
                    "creatives": {},
                },
            ],
        )
    ],
)
async def test_filters_campaigns_without_points(
    campaign_filter, campaigns, expected_campaigns
):
    campaigns = await campaign_filter(campaigns)

    assert campaigns == expected_campaigns
