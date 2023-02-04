import pytest

from maps_adv.export.lib.core.enum import ActionType, CampaignType, CreativeType
from maps_adv.export.lib.pipeline.resolver.search_tags import search_tags_resolver

pytestmark = [pytest.mark.asyncio]


async def test_add_expected_search_tags_for_action_search():
    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.PIN_ON_ROUTE,
            creatives={},
            placing={},
            actions=[
                dict(type=ActionType.SEARCH, organizations={1: {}, 2: {}}),
                dict(type=ActionType.SEARCH, organizations={3: {}, 4: {}}),
            ],
        )
    ]
    search_tags = await search_tags_resolver(campaigns)

    assert search_tags == [
        dict(id="search_tag-1-6b51d431df5d7f141cbececcf79edf3d", companies=[1, 2]),
        dict(id="search_tag-1-86e50149658661312a9e0b35558d84f6", companies=[3, 4]),
    ]


@pytest.mark.parametrize(
    "campaign_type", list(set(CampaignType) - {CampaignType.CATEGORY})
)
async def test_will_not_change_search_tags_without_action_search_and_campaign_type_category(  # noqa: E501
    campaign_type,
):
    campaigns = [
        dict(id=1, campaign_type=campaign_type, creatives={}, placing={}, actions=[])
    ]
    search_tags = await search_tags_resolver(campaigns)

    assert search_tags == []


async def test_no_add_search_tags_for_creative_pin_search():
    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.PIN_SEARCH,
            placing={},
            creatives={
                CreativeType.PIN_SEARCH: [
                    dict(organizations={3: {}, 4: {}}),
                    dict(organizations={1: {}, 2: {}}),
                ]
            },
            actions=[],
        )
    ]
    search_tags = await search_tags_resolver(campaigns)

    assert campaigns == [
        dict(
            id=1,
            campaign_type=CampaignType.PIN_SEARCH,
            placing={},
            creatives={
                CreativeType.PIN_SEARCH: [
                    dict(organizations={3: {}, 4: {}}),
                    dict(organizations={1: {}, 2: {}}),
                ]
            },
            actions=[],
        )
    ]
    assert search_tags == []


async def test_add_search_tag_for_campaign_type_of_category():
    campaigns = [
        dict(
            id=1,
            campaign_type=CampaignType.CATEGORY,
            creatives={
                CreativeType.ICON: dict(
                    search_text="icon search text",
                    organizations={2: {}, 3: {}, 4: {}, 1: {}},
                )
            },
            actions=[],
        )
    ]
    search_tags = await search_tags_resolver(campaigns)

    assert campaigns == [
        dict(
            id=1,
            campaign_type=CampaignType.CATEGORY,
            creatives={
                CreativeType.ICON: dict(
                    search_text="icon search text",
                    search_tag_id="search_tag-1-03ac674216f3e15c761ee1a5e255f067",
                    organizations={2: {}, 3: {}, 4: {}, 1: {}},
                )
            },
            actions=[],
        )
    ]
    assert search_tags == [
        dict(id="search_tag-1-03ac674216f3e15c761ee1a5e255f067", companies=[1, 2, 3, 4])
    ]


@pytest.mark.parametrize(
    "campaigns",
    [
        [  # duplicate creative of icon type
            dict(
                id=1,
                campaign_type=CampaignType.CATEGORY,
                creatives={
                    CreativeType.ICON: dict(
                        search_text="icon search text",
                        organizations={2: {}, 3: {}, 4: {}, 1: {}},
                    )
                },
                actions=[],
            ),
            dict(
                id=1,
                campaign_type=CampaignType.CATEGORY,
                creatives={
                    CreativeType.ICON: dict(
                        search_text="icon search text",
                        organizations={2: {}, 3: {}, 4: {}, 1: {}},
                    )
                },
                actions=[],
            ),
        ],
        [  # cross duplicate of icon and actions
            dict(
                id=1,
                campaign_type=CampaignType.CATEGORY,
                creatives={
                    CreativeType.ICON: dict(
                        search_text="icon search text",
                        organizations={2: {}, 3: {}, 4: {}, 1: {}},
                    )
                },
                actions=[],
            ),
            dict(
                id=1,
                campaign_type=CampaignType.CATEGORY,
                creatives={},
                actions=[
                    dict(
                        type=ActionType.SEARCH,
                        organizations={2: {}, 3: {}, 4: {}, 1: {}},
                    )
                ],
            ),
        ],
        [  # duplicate actions for one campaign
            dict(
                id=1,
                campaign_type=CampaignType.PIN_ON_ROUTE,
                creatives={},
                actions=[
                    dict(type=ActionType.SEARCH, organizations={1: {}, 2: {}}),
                    dict(type=ActionType.SEARCH, organizations={2: {}, 1: {}}),
                ],
            )
        ],
        [  # duplicate actios in different campaigns
            dict(
                id=1,
                campaign_type=CampaignType.PIN_ON_ROUTE,
                creatives={},
                actions=[dict(type=ActionType.SEARCH, organizations={1: {}, 2: {}})],
            ),
            dict(
                id=1,
                campaign_type=CampaignType.PIN_ON_ROUTE,
                creatives={},
                actions=[dict(type=ActionType.SEARCH, organizations={2: {}, 1: {}})],
            ),
        ],
    ],
)
async def test_does_not_duplicates_search_tags(campaigns):
    """
    Проверяем что после работы резолвера поисковых тэгов не будет
    дубликатов. Определение дубликатов происходит по ID, так как в нем
    зашит номер кампании и хэш сумма от набора кампаний.
    """
    search_tags = await search_tags_resolver(campaigns)

    assert len(search_tags) == 1
