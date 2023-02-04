from typing import List

import pytest

from maps_adv.export.lib.core.enum import CampaignType, CreativeType
from maps_adv.export.lib.pipeline.transform import split_category_search_campaigns


@pytest.mark.parametrize(
    ["campaigns", "expected_campaigns"],
    [
        ([], []),
        (  # just split by campaign type
            [
                dict(
                    id=1,
                    campaign_type=CampaignType.CATEGORY_SEARCH,
                    placing={},
                    creatives={
                        CreativeType.ICON: {},
                        CreativeType.TEXT: {},
                        CreativeType.PIN_SEARCH: [],
                        CreativeType.PIN: {},
                    },
                )
            ],
            [
                dict(
                    id=1,
                    campaign_type=CampaignType.CATEGORY,
                    placing=dict(organizations=dict(permalinks=[])),
                    creatives={
                        CreativeType.ICON: dict(organizations=[]),
                        CreativeType.PIN: {},
                    },
                ),
                dict(
                    id=1,
                    campaign_type=CampaignType.PIN_SEARCH,
                    placing=dict(organizations=dict(permalinks=[])),
                    creatives={
                        CreativeType.TEXT: {},
                        CreativeType.PIN_SEARCH: [],
                        CreativeType.PIN: {},
                    },
                ),
            ],
        ),
        (  # validate add organizations from pin search creatives to category
            [
                dict(
                    id=1,
                    campaign_type=CampaignType.CATEGORY_SEARCH,
                    placing=dict(organizations=dict(permalinks=[5])),
                    creatives={
                        CreativeType.ICON: dict(organizations=[7]),
                        CreativeType.TEXT: {},
                        CreativeType.PIN_SEARCH: [
                            dict(
                                title="search pin title",
                                organizations=[1, 2],
                                images=[],
                            ),
                            dict(
                                title="search pin title",
                                organizations=[4, 3],
                                images=[],
                            ),
                        ],
                    },
                )
            ],
            [
                dict(
                    id=1,
                    campaign_type=CampaignType.CATEGORY,
                    placing=dict(organizations=dict(permalinks=[5])),
                    creatives={CreativeType.ICON: dict(organizations=[7, 1, 2, 4, 3])},
                ),
                dict(
                    id=1,
                    campaign_type=CampaignType.PIN_SEARCH,
                    placing=dict(organizations=dict(permalinks=[])),
                    creatives={
                        CreativeType.TEXT: {},
                        CreativeType.PIN_SEARCH: [
                            dict(
                                title="search pin title",
                                organizations=[1, 2],
                                images=[],
                            ),
                            dict(
                                title="search pin title",
                                organizations=[4, 3],
                                images=[],
                            ),
                        ],
                    },
                ),
            ],
        ),
    ],
)
def test_returns_campaign_category_and_campaign_pin_search(
    campaigns: List[dict], expected_campaigns: List[dict]
):
    campaigns = split_category_search_campaigns(campaigns)

    assert campaigns == expected_campaigns
