import pytest

from maps_adv.adv_store.api.schemas.enums import PublicationEnvEnum
from maps_adv.export.lib.core.enum import (
    CampaignType,
    CreativeType,
    ImageType,
)
from maps_adv.export.lib.pipeline.xml.transform.base import menu_item_transform

pytestmark = [pytest.mark.asyncio]


async def test_will_transform_campaign_category_search_as_expected(
    avatars_factory, faker
):
    category = avatars_factory(ImageType.CATEGORY)
    campaign = dict(
        id=1,
        campaign_type=CampaignType.CATEGORY,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        title="title",
        pages=["p1", "p2"],
        placing=dict(organizations={2: {}, 3: {}, 4: {}}),
        creatives={
            CreativeType.ICON: dict(
                style="icon style",
                position=5,
                title="icon title",
                search_text="icon search text",
                images=[category.avatar],
                organizations={5: {}},
            )
        },
    )

    result = await menu_item_transform(campaign)

    assert result == dict(
        id="ac_auto_log_id_campaign_1",
        pages=["p1", "p2"],
        style="icon style",
        title=[dict(value="icon title")],
        search_text="icon search text",
        position=5,
        companies=[2, 3, 4, 5],
    )


async def test_field_companies_contains_permalinks_without_organizations_in_pin_search_creatives(  # noqa: E501
    avatars_factory, faker
):
    category = avatars_factory(ImageType.CATEGORY)
    campaign = dict(
        id=1,
        campaign_type=CampaignType.CATEGORY,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        title="title",
        pages=["p1", "p2"],
        placing=dict(organizations={1: {}}),
        creatives={
            CreativeType.ICON: dict(
                style="icon style",
                position=5,
                title="icon title",
                search_text="icon search text",
                images=[category.avatar],
                organizations={7: {}},
            ),
            CreativeType.PIN_SEARCH: [
                dict(organizations={2: {}}),
                dict(organizations={3: {}, 4: {}}),
            ],
        },
    )

    result = await menu_item_transform(campaign)

    assert result == dict(
        id="ac_auto_log_id_campaign_1",
        pages=["p1", "p2"],
        style="icon style",
        title=[dict(value="icon title")],
        search_text="icon search text",
        position=5,
        companies=[1, 7],
    )


@pytest.mark.parametrize("campaign_id", [1, 2])
async def test_generates_expected_menu_item_id(campaign_id, avatars_factory):
    category = avatars_factory(ImageType.CATEGORY)
    campaign = dict(
        id=campaign_id,
        campaign_type=CampaignType.CATEGORY,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        title="title",
        pages=["p1", "p2"],
        placing=dict(organizations={3: {}, 4: {}}),
        creatives={
            CreativeType.ICON: dict(
                style="icon style",
                position=5,
                title="icon title",
                search_text="icon search text",
                images=[category.avatar],
                organizations={2: {}},
            )
        },
    )

    result = await menu_item_transform(campaign)

    assert result == dict(
        id=f"ac_auto_log_id_campaign_{campaign_id}",
        pages=["p1", "p2"],
        style="icon style",
        title=[dict(value="icon title")],
        search_text="icon search text",
        position=5,
        companies=[2, 3, 4],
    )


@pytest.mark.parametrize(
    ["icon_search_text", "expected_search_text"],
    [
        [dict(search_text="overload"), "overload"],
        [
            dict(search_tag_id="94629eb2f"),
            '{"text": "", "ad": {"advert_tag_id": "94629eb2f"}}',
        ],
        [dict(search_text="overload", search_tag_id="94629eb2f"), "overload"],
    ],
)
async def test_priority_search_text(
    icon_search_text: dict, expected_search_text: str, avatars_factory, faker
):
    category = avatars_factory(ImageType.CATEGORY)
    campaign = dict(
        id=1,
        campaign_type=CampaignType.CATEGORY,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        title="title",
        pages=["p1", "p2"],
        placing=dict(organizations={1: {}}),
        creatives={
            CreativeType.ICON: dict(
                style="icon style",
                position=5,
                title="icon title",
                images=[category.avatar],
                organizations={},
                **icon_search_text,
            )
        },
    )

    result = await menu_item_transform(campaign)

    assert result == dict(
        id="ac_auto_log_id_campaign_1",
        pages=["p1", "p2"],
        style="icon style",
        title=[dict(value="icon title")],
        search_text=expected_search_text,
        position=5,
        companies=[1],
    )


@pytest.mark.parametrize(
    "campaign_type", list(set(CampaignType) - {CampaignType.CATEGORY})
)
async def test_returns_empty_result_if_campaign_type_is_not_supported(campaign_type):
    campaign = dict(campaign_type=campaign_type)
    result = await menu_item_transform(campaign)

    assert result == {}


async def test_adds_data_testing_to_id(avatars_factory, faker):
    category = avatars_factory(ImageType.CATEGORY)
    campaign = dict(
        id=1,
        campaign_type=CampaignType.CATEGORY,
        publication_envs=[PublicationEnvEnum.DATA_TESTING],
        title="title",
        pages=["p1", "p2"],
        placing=dict(organizations={2: {}, 3: {}, 4: {}}),
        creatives={
            CreativeType.ICON: dict(
                style="icon style",
                position=5,
                title="icon title",
                search_text="icon search text",
                images=[category.avatar],
                organizations={},
            )
        },
    )

    result = await menu_item_transform(campaign)

    assert result == dict(
        id="ac_auto_log_id_campaign_1_data_testing",
        pages=["p1", "p2"],
        style="icon style",
        title=[dict(value="icon title")],
        search_text="icon search text",
        position=5,
        companies=[2, 3, 4],
    )
