from typing import List

import pytest

from maps_adv.export.lib.core.enum import CampaignType, CreativeType
from maps_adv.export.lib.core.exception import CrossCampaignsForMerging
from maps_adv.export.lib.pipeline.transform import merge_category_campaigns


@pytest.mark.parametrize(
    ["campaigns", "expected_campaigns"],
    [
        ([], []),
        (
            [
                dict(
                    id=1,
                    campaign_type=CampaignType.CATEGORY,
                    placing={},
                    creatives={CreativeType.ICON: dict()},
                ),
                dict(
                    id=2,
                    campaign_type=CampaignType.CATEGORY,
                    placing={},
                    creatives={CreativeType.ICON: dict()},
                ),
            ],
            [
                dict(
                    id=1,
                    campaign_type=CampaignType.CATEGORY,
                    placing=dict(organizations=dict(permalinks=[])),
                    creatives={CreativeType.ICON: dict(organizations=[])},
                )
            ],
        ),
        (
            [
                dict(
                    id=1,
                    campaign_type=CampaignType.CATEGORY,
                    placing=dict(organizations=dict(permalinks=[5])),
                    creatives={CreativeType.ICON: dict(organizations=[50])},
                ),
                dict(
                    id=2,
                    campaign_type=CampaignType.CATEGORY,
                    placing=dict(organizations=dict(permalinks=[7])),
                    creatives={CreativeType.ICON: dict(organizations=[70])},
                ),
            ],
            [
                dict(
                    id=1,
                    campaign_type=CampaignType.CATEGORY,
                    placing=dict(organizations=dict(permalinks=[5, 7])),
                    creatives={CreativeType.ICON: dict(organizations=[50, 70])},
                )
            ],
        ),
        (
            [
                dict(
                    id=3,
                    campaign_type=CampaignType.CATEGORY,
                    placing=dict(),
                    creatives={CreativeType.ICON: dict()},
                ),
                dict(
                    id=4,
                    campaign_type=CampaignType.CATEGORY,
                    placing=dict(organizations=dict(permalinks=[7])),
                    creatives={CreativeType.ICON: dict(organizations=[70])},
                ),
                dict(
                    id=5,
                    campaign_type=CampaignType.BILLBOARD,
                    placing=dict(organizations=dict(permalinks=[3])),
                ),
            ],
            [
                dict(
                    id=3,
                    campaign_type=CampaignType.CATEGORY,
                    placing=dict(),
                    creatives={CreativeType.ICON: dict()},
                ),
                dict(
                    id=4,
                    campaign_type=CampaignType.CATEGORY,
                    placing=dict(organizations=dict(permalinks=[7])),
                    creatives={CreativeType.ICON: dict(organizations=[70])},
                ),
                dict(
                    id=5,
                    campaign_type=CampaignType.BILLBOARD,
                    placing=dict(organizations=dict(permalinks=[3])),
                ),
            ],
        ),
    ],
)
def test_returns_campaign_category_and_campaign_pin_search(
    campaigns: List[dict], expected_campaigns: List[dict]
):
    mc_campaigns = {1: [2]}
    campaigns = merge_category_campaigns(campaigns, mc_campaigns)

    assert campaigns == expected_campaigns


def test_raises_cross_campaigns_for_merging():
    campaigns = []
    mc_campaigns = {1: [2], 2: [3]}

    with pytest.raises(CrossCampaignsForMerging):
        campaigns = merge_category_campaigns(campaigns, mc_campaigns)
