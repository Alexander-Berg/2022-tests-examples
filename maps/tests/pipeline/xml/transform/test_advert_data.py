from operator import attrgetter

import pytest

from maps_adv.adv_store.api.schemas.enums import PlatformEnum, PublicationEnvEnum
from maps_adv.export.lib.core.enum import (
    ActionType,
    CampaignType,
    CreativeType,
    ImageType,
)
from maps_adv.export.lib.pipeline.xml.transform.advert_data import advert_data_transform

pytestmark = [pytest.mark.asyncio]

AVATARS_NAMESPACE = "test-avatars-namespace"
DEFAULT_PIN_SEARCH_IMAGES = [
    dict(
        type=ImageType.LOGO, image_name="123", group_id=11, alias_template="logo_{zoom}"
    ),
    dict(
        type=ImageType.BANNER,
        image_name="124",
        group_id=12,
        alias_template="banner_{zoom}",
    ),
    dict(
        type=ImageType.DUST,
        image_name="124",
        group_id=13,
        alias_template="geo_adv_dust_{zoom}",
    ),
    dict(
        type=ImageType.DUST_HOVER,
        image_name="124",
        group_id=14,
        alias_template="geo_adv_dust_{zoom}",
    ),
    dict(
        type=ImageType.DUST_VISITED,
        image_name="124",
        group_id=15,
        alias_template="geo_adv_dust_{zoom}",
    ),
    dict(
        type=ImageType.PIN,
        image_name="124",
        group_id=16,
        alias_template="geo_adv_drop_{zoom}",
    ),
    dict(
        type=ImageType.PIN_HOVER,
        image_name="124",
        group_id=17,
        alias_template="geo_adv_drop_hover_{zoom}",
    ),
    dict(
        type=ImageType.PIN_VISITED,
        image_name="124",
        group_id=18,
        alias_template="geo_adv_drop_visited_{zoom}",
    ),
    dict(
        type=ImageType.PIN_SELECTED,
        image_name="124",
        group_id=19,
        alias_template="geo_adv_pin_{zoom}",
    ),
    dict(
        type=ImageType.PIN_ROUND,
        image_name="124",
        group_id=20,
        alias_template="pin_search_round_{zoom}",
    ),
]
DEFAULT_MAPS_EXPECTED_FIELDS_IMAGES = dict(
    anchorDust="0.5 0.5",
    anchorDustHover="0.5 0.5",
    anchorDustVisited="0.5 0.5",
    anchorIcon="0.5 0.89",
    anchorIconHover="0.5 0.89",
    anchorIconVisited="0.5 0.89",
    anchorSelected="0.5 0.94",
    sizeDust="18 18",
    sizeDustHover="18 18",
    sizeDustVisited="18 18",
    sizeIcon="32 38",
    sizeIconHover="32 38",
    sizeIconVisited="32 38",
    sizeSelected="60 68",
    styleBalloonBanner=f"{AVATARS_NAMESPACE}--12--124--banner",
    styleDust=f"{AVATARS_NAMESPACE}--13--124--geo_adv_dust",
    styleDustHover=f"{AVATARS_NAMESPACE}--14--124--geo_adv_dust",
    styleDustVisited=f"{AVATARS_NAMESPACE}--15--124--geo_adv_dust",
    styleIcon=f"{AVATARS_NAMESPACE}--16--124--geo_adv_drop",
    styleIconHover=f"{AVATARS_NAMESPACE}--17--124--geo_adv_drop_hover",
    styleIconVisited=f"{AVATARS_NAMESPACE}--18--124--geo_adv_drop_visited",
    styleLogo=f"{AVATARS_NAMESPACE}--11--123--logo",
    styleSelected=f"{AVATARS_NAMESPACE}--19--124--geo_adv_pin",
)
DEFAULT_NAVI_EXPECTED_FIELDS_IMAGES = dict(
    anchorDust="0.5 0.5",
    anchorDustHover="0.5 0.5",
    anchorDustVisited="0.5 0.5",
    anchorIcon="0.5 0.5",
    anchorIconHover="0.5 0.5",
    anchorIconVisited="0.5 0.5",
    anchorSelected="0.5 0.94",
    sizeDust="18 18",
    sizeDustHover="18 18",
    sizeDustVisited="18 18",
    sizeIcon="32 32",
    sizeIconHover="32 32",
    sizeIconVisited="32 32",
    sizeSelected="60 68",
    styleDust=f"{AVATARS_NAMESPACE}--13--124--geo_adv_dust",
    styleDustHover=f"{AVATARS_NAMESPACE}--14--124--geo_adv_dust",
    styleDustVisited=f"{AVATARS_NAMESPACE}--15--124--geo_adv_dust",
    styleIcon=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
    styleIconHover=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
    styleIconVisited=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
    styleSelected=f"{AVATARS_NAMESPACE}--19--124--geo_adv_pin",
)
DEFAULT_EXPECTED_PHONE = dict(
    telephone="+7(111)2223344",
    formatted="+7 (111) 222-33-44",
    country="7",
    prefix="111",
    number="2223344",
)


@pytest.mark.parametrize(
    ["platform", "pin_search_images", "expected_fields"],
    [
        (
            PlatformEnum.NAVI,
            DEFAULT_PIN_SEARCH_IMAGES,
            dict(advert_type="menu_icon", **DEFAULT_NAVI_EXPECTED_FIELDS_IMAGES),
        ),
        (
            PlatformEnum.NAVI,
            [
                dict(
                    type=ImageType.PIN_SELECTED,
                    image_name="124",
                    group_id=19,
                    alias_template="geo_adv_pin_{zoom}",
                ),
                dict(
                    type=ImageType.PIN_ROUND,
                    image_name="124",
                    group_id=20,
                    alias_template="pin_search_round_{zoom}",
                ),
            ],
            dict(
                advert_type="menu_icon",
                anchorIcon="0.5 0.5",
                anchorIconHover="0.5 0.5",
                anchorIconVisited="0.5 0.5",
                anchorSelected="0.5 0.94",
                sizeIcon="32 32",
                sizeIconHover="32 32",
                sizeIconVisited="32 32",
                sizeSelected="60 68",
                styleIcon=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                styleIconHover=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                styleIconVisited=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                styleSelected=f"{AVATARS_NAMESPACE}--19--124--geo_adv_pin",
            ),
        ),
        (
            PlatformEnum.MAPS,
            DEFAULT_PIN_SEARCH_IMAGES,
            dict(advert_type="menu_icon", **DEFAULT_MAPS_EXPECTED_FIELDS_IMAGES),
        ),
    ],
)
async def test_will_transform_campaign_type_of_pin_search_as_expected(
    platform, pin_search_images, expected_fields, companies_factory, config
):
    companies = companies_factory(count=5)
    companies_ids = list(map(attrgetter("permalink"), companies))

    campaign = dict(
        id=1,
        campaign_type=CampaignType.PIN_SEARCH,
        platforms=[platform],
        publication_envs=[
            PublicationEnvEnum.PRODUCTION,
            PublicationEnvEnum.DATA_TESTING,
        ],
        pages=["p1", "p2", "p1/datatesting"],
        placing=dict(organizations={value.permalink: value for value in companies[3:]}),
        creatives={
            CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
            CreativeType.PIN_SEARCH: [
                dict(
                    title="search pin title",
                    organizations={value.permalink: value for value in companies[:3]},
                    images=pin_search_images,
                )
            ],
        },
        actions=[
            dict(type=ActionType.PHONE_CALL, phone=DEFAULT_EXPECTED_PHONE["telephone"])
        ],
    )

    result = await advert_data_transform(campaign)

    assert len(result) == 1
    assert result == [
        dict(
            pages=["p1", "p2", "p1/datatesting"],
            log_id="ac_auto_log_id_campaign_1",
            title="search pin title",
            text="text",
            disclaimer="disclaimer",
            phone=DEFAULT_EXPECTED_PHONE,
            companies=companies_ids[:3],
            fields=expected_fields,
        )
    ]


@pytest.mark.parametrize(
    ["pin_search_images", "expected_fields"],
    [
        [
            [
                dict(
                    type=ImageType.LOGO,
                    image_name="123",
                    group_id=11,
                    alias_template="logo_{zoom}",
                ),
                dict(
                    type=ImageType.DUST,
                    image_name="124",
                    group_id=13,
                    alias_template="geo_adv_dust_{zoom}",
                ),
                dict(
                    type=ImageType.DUST_HOVER,
                    image_name="124",
                    group_id=14,
                    alias_template="geo_adv_dust_{zoom}",
                ),
                dict(
                    type=ImageType.DUST_VISITED,
                    image_name="124",
                    group_id=15,
                    alias_template="geo_adv_dust_{zoom}",
                ),
                dict(
                    type=ImageType.PIN,
                    image_name="124",
                    group_id=16,
                    alias_template="geo_adv_drop_{zoom}",
                ),
                dict(
                    type=ImageType.PIN_HOVER,
                    image_name="124",
                    group_id=17,
                    alias_template="geo_adv_drop_hover_{zoom}",
                ),
                dict(
                    type=ImageType.PIN_VISITED,
                    image_name="124",
                    group_id=18,
                    alias_template="geo_adv_drop_visited_{zoom}",
                ),
                dict(
                    type=ImageType.PIN_SELECTED,
                    image_name="124",
                    group_id=19,
                    alias_template="geo_adv_pin_{zoom}",
                ),
            ],
            dict(
                advert_type="menu_icon",
                anchorDust="0.5 0.5",
                anchorDustHover="0.5 0.5",
                anchorDustVisited="0.5 0.5",
                anchorIcon="0.5 0.89",
                anchorIconHover="0.5 0.89",
                anchorIconVisited="0.5 0.89",
                anchorSelected="0.5 0.94",
                sizeDust="18 18",
                sizeDustHover="18 18",
                sizeDustVisited="18 18",
                sizeIcon="32 38",
                sizeIconHover="32 38",
                sizeIconVisited="32 38",
                sizeSelected="60 68",
                styleDust=f"{AVATARS_NAMESPACE}--13--124--geo_adv_dust",
                styleDustHover=f"{AVATARS_NAMESPACE}--14--124--geo_adv_dust",
                styleDustVisited=f"{AVATARS_NAMESPACE}--15--124--geo_adv_dust",
                styleIcon=f"{AVATARS_NAMESPACE}--16--124--geo_adv_drop",
                styleIconHover=f"{AVATARS_NAMESPACE}--17--124--geo_adv_drop_hover",
                styleIconVisited=f"{AVATARS_NAMESPACE}--18--124--geo_adv_drop_visited",
                styleLogo=f"{AVATARS_NAMESPACE}--11--123--logo",
                styleSelected=f"{AVATARS_NAMESPACE}--19--124--geo_adv_pin",
            ),
        ]
    ],
)
async def test_expected_transform_without_image_banner_for_campaign_type_pin_search_in_maps(  # noqa: E501
    pin_search_images, expected_fields, companies_factory, config
):
    companies = companies_factory(count=5)
    companies_ids = list(map(attrgetter("permalink"), companies))

    campaign = dict(
        id=1,
        campaign_type=CampaignType.PIN_SEARCH,
        platforms=[PlatformEnum.MAPS],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        placing=dict(organizations={value.permalink: value for value in companies[3:]}),
        creatives={
            CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
            CreativeType.PIN_SEARCH: [
                dict(
                    title="search pin title",
                    organizations={value.permalink: value for value in companies[:3]},
                    images=pin_search_images,
                )
            ],
        },
        actions=[
            dict(type=ActionType.PHONE_CALL, phone=DEFAULT_EXPECTED_PHONE["telephone"])
        ],
    )

    result = await advert_data_transform(campaign)

    assert len(result) == 1
    assert result == [
        dict(
            pages=["p1", "p2"],
            log_id="ac_auto_log_id_campaign_1",
            title="search pin title",
            text="text",
            disclaimer="disclaimer",
            phone=DEFAULT_EXPECTED_PHONE,
            companies=companies_ids[:3],
            fields=expected_fields,
        )
    ]


@pytest.mark.parametrize(
    ["platform", "expected_fields"],
    [
        [
            PlatformEnum.NAVI,
            dict(advert_type="menu_icon", **DEFAULT_NAVI_EXPECTED_FIELDS_IMAGES),
        ],
        [
            PlatformEnum.MAPS,
            dict(advert_type="menu_icon", **DEFAULT_MAPS_EXPECTED_FIELDS_IMAGES),
        ],
    ],
)
async def test_returns_expected_campaign_without_phone(
    platform, expected_fields, companies_factory, config
):
    companies = companies_factory(count=3)
    companies_ids = list(map(attrgetter("permalink"), companies))
    campaign = dict(
        id=1,
        campaign_type=CampaignType.PIN_SEARCH,
        platforms=[platform],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        creatives={
            CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
            CreativeType.PIN_SEARCH: [
                dict(
                    title="search pin title",
                    organizations={value.permalink: value for value in companies},
                    images=DEFAULT_PIN_SEARCH_IMAGES,
                )
            ],
        },
        actions=[],
    )

    result = await advert_data_transform(campaign)

    assert len(result) == 1
    assert result == [
        dict(
            pages=["p1", "p2"],
            log_id="ac_auto_log_id_campaign_1",
            title="search pin title",
            text="text",
            disclaimer="disclaimer",
            companies=companies_ids,
            fields=expected_fields,
        )
    ]


@pytest.mark.parametrize(
    ["platform", "expected_fields"],
    [
        [
            PlatformEnum.NAVI,
            dict(advert_type="menu_icon", **DEFAULT_NAVI_EXPECTED_FIELDS_IMAGES),
        ],
        [
            PlatformEnum.MAPS,
            dict(advert_type="menu_icon", **DEFAULT_MAPS_EXPECTED_FIELDS_IMAGES),
        ],
    ],
)
async def test_multi_campaigns_with_company_titles_creative_icon_has_no_title(
    platform, expected_fields, companies_factory, config
):
    companies = companies_factory(count=3)
    companies_ids = list(map(attrgetter("permalink"), companies))
    campaign = dict(
        id=1,
        campaign_type=CampaignType.PIN_SEARCH,
        platforms=[platform],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        creatives={
            CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
            CreativeType.PIN_SEARCH: [
                dict(
                    organizations={value.permalink: value for value in companies},
                    images=DEFAULT_PIN_SEARCH_IMAGES,
                )
            ],
        },
        actions=[],
    )

    result = await advert_data_transform(campaign)

    assert result == [
        dict(
            pages=["p1", "p2"],
            log_id="ac_auto_log_id_campaign_1",
            title=companies[0].title,
            text="text",
            disclaimer="disclaimer",
            companies=[companies_ids[0]],
            fields=expected_fields,
        ),
        dict(
            pages=["p1", "p2"],
            log_id="ac_auto_log_id_campaign_1",
            title=companies[1].title,
            text="text",
            disclaimer="disclaimer",
            companies=[companies_ids[1]],
            fields=expected_fields,
        ),
        dict(
            pages=["p1", "p2"],
            log_id="ac_auto_log_id_campaign_1",
            title=companies[2].title,
            text="text",
            disclaimer="disclaimer",
            companies=[companies_ids[2]],
            fields=expected_fields,
        ),
    ]


@pytest.mark.parametrize(
    ["platform", "expected_fields"],
    [
        [
            PlatformEnum.NAVI,
            dict(advert_type="menu_icon", **DEFAULT_NAVI_EXPECTED_FIELDS_IMAGES),
        ],
        [
            PlatformEnum.MAPS,
            dict(advert_type="menu_icon", **DEFAULT_MAPS_EXPECTED_FIELDS_IMAGES),
        ],
    ],
)
async def test_multi_campaigns_with_two_pin_search_creative(
    platform, expected_fields, companies_factory
):
    companies = companies_factory(count=2)
    companies_ids = list(map(attrgetter("permalink"), companies))

    campaign = dict(
        id=1,
        campaign_type=CampaignType.PIN_SEARCH,
        platforms=[platform],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        creatives={
            CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
            CreativeType.PIN_SEARCH: [
                dict(
                    organizations={value.permalink: value for value in companies[:1]},
                    images=DEFAULT_PIN_SEARCH_IMAGES,
                ),
                dict(
                    organizations={value.permalink: value for value in companies[1:]},
                    images=DEFAULT_PIN_SEARCH_IMAGES,
                ),
            ],
        },
        actions=[],
    )

    result = await advert_data_transform(campaign)

    assert result == [
        dict(
            pages=["p1", "p2"],
            log_id="ac_auto_log_id_campaign_1",
            title=companies[0].title,
            text="text",
            disclaimer="disclaimer",
            companies=companies_ids[:1],
            fields=expected_fields,
        ),
        dict(
            pages=["p1", "p2"],
            log_id="ac_auto_log_id_campaign_1",
            title=companies[1].title,
            text="text",
            disclaimer="disclaimer",
            companies=companies_ids[1:],
            fields=expected_fields,
        ),
    ]


@pytest.mark.parametrize("campaign_id", [1, 2])
async def test_generates_expected_log_id(campaign_id, companies_factory):
    campaign = dict(
        id=campaign_id,
        campaign_type=CampaignType.PIN_SEARCH,
        platforms=[PlatformEnum.NAVI],
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        pages=["p1", "p2"],
        creatives={
            CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
            CreativeType.PIN_SEARCH: [
                dict(
                    title="search pin title",
                    organizations={
                        value.permalink: value for value in companies_factory(count=3)
                    },
                    images=DEFAULT_PIN_SEARCH_IMAGES,
                )
            ],
        },
        actions=[],
    )

    result = await advert_data_transform(campaign)

    assert len(result) == 1
    assert result[0]["log_id"] == "ac_auto_log_id_campaign_{}".format(campaign_id)


@pytest.mark.parametrize(
    "campaign_type", list(set(CampaignType) - {CampaignType.PIN_SEARCH})
)
async def test_returns_empty_result_if_campaign_type_is_not_supported(campaign_type):
    campaign = dict(campaign_type=campaign_type)
    result = await advert_data_transform(campaign)

    assert result == []


@pytest.mark.parametrize(
    ["pin_search_images", "expected_fields"],
    [
        (
            DEFAULT_PIN_SEARCH_IMAGES,
            dict(
                advert_type="menu_icon",
                anchorDust="0.5 0.5",
                anchorDustHover="0.5 0.5",
                anchorDustVisited="0.5 0.5",
                anchorIcon="0.5 0.5",
                anchorIconHover="0.5 0.5",
                anchorIconVisited="0.5 0.5",
                anchorSelected="0.5 0.94",
                sizeDust="18 18",
                sizeDustHover="18 18",
                sizeDustVisited="18 18",
                sizeIcon="32 32",
                sizeIconHover="32 32",
                sizeIconVisited="32 32",
                sizeSelected="60 68",
                styleDust=f"{AVATARS_NAMESPACE}--13--124--geo_adv_dust",
                styleDustHover=f"{AVATARS_NAMESPACE}--14--124--geo_adv_dust",
                styleDustVisited=f"{AVATARS_NAMESPACE}--15--124--geo_adv_dust",
                styleIcon=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                styleIconHover=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                styleIconVisited=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                styleSelected=f"{AVATARS_NAMESPACE}--19--124--geo_adv_pin",
            ),
        ),
        (
            [
                dict(
                    type=ImageType.PIN_SELECTED,
                    image_name="124",
                    group_id=19,
                    alias_template="geo_adv_pin_{zoom}",
                ),
                dict(
                    type=ImageType.PIN_ROUND,
                    image_name="124",
                    group_id=20,
                    alias_template="pin_search_round_{zoom}",
                ),
            ],
            dict(
                advert_type="menu_icon",
                anchorIcon="0.5 0.5",
                anchorIconHover="0.5 0.5",
                anchorIconVisited="0.5 0.5",
                anchorSelected="0.5 0.94",
                sizeIcon="32 32",
                sizeIconHover="32 32",
                sizeIconVisited="32 32",
                sizeSelected="60 68",
                styleIcon=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                styleIconHover=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                styleIconVisited=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                styleSelected=f"{AVATARS_NAMESPACE}--19--124--geo_adv_pin",
            ),
        ),
    ],
)
async def test_navi_campaign_pin_search_when_datatesing_have_new_format_and_no_production_env(  # noqa
    pin_search_images, expected_fields, companies_factory
):
    companies = companies_factory(count=2)
    companies_ids = list(map(attrgetter("permalink"), companies))

    campaign = dict(
        id=1,
        campaign_type=CampaignType.PIN_SEARCH,
        publication_envs=[PublicationEnvEnum.DATA_TESTING],
        platforms=[PlatformEnum.NAVI],
        pages=["p2/datatesting"],
        creatives={
            CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
            CreativeType.PIN_SEARCH: [
                dict(
                    title="search pin title 1",
                    organizations={value.permalink: value for value in companies},
                    images=pin_search_images,
                )
            ],
        },
        actions=[],
    )

    result = await advert_data_transform(campaign)

    assert result == [
        dict(
            pages=["p2/datatesting"],
            log_id="ac_auto_log_id_campaign_1",
            title="search pin title 1",
            text="text",
            disclaimer="disclaimer",
            companies=companies_ids,
            fields=expected_fields,
        )
    ]


@pytest.mark.parametrize(
    ["experiment", "publication_env", "pages", "expected_attrs"],
    [
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
            {"highlighted": "false"},
        ),
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.PRODUCTION,
            ["p1"],
            {"highlighted": "false"},
        ),
        (
            [PublicationEnvEnum.DATA_TESTING],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
            {"highlighted": "false"},
        ),
        (
            [PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.PRODUCTION,
            ["p1"],
            {"highlighted": "false"},
        ),
        ([PublicationEnvEnum.DATA_TESTING], PublicationEnvEnum.PRODUCTION, ["p1"], {}),
        (
            [PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
            {},
        ),
    ],
)
async def test_returns_highlighted_attr_for_maps_campaign_pin_search(
    experiment,
    publication_env,
    pages,
    expected_attrs,
    companies_factory,
    experimental_options,
):
    with experimental_options(
        {"EXPERIMENT_USE_HIGHLIGHTED_ATTR_FOR_PIN_SEARCH": experiment}
    ):
        companies = companies_factory(count=2)
        companies_ids = list(map(attrgetter("permalink"), companies))

        campaign = dict(
            id=1,
            campaign_type=CampaignType.PIN_SEARCH,
            publication_envs=[publication_env],
            platforms=[PlatformEnum.MAPS],
            pages=pages,
            creatives={
                CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
                CreativeType.PIN_SEARCH: [
                    dict(
                        title="search pin title 1",
                        organizations={value.permalink: value for value in companies},
                        images=DEFAULT_PIN_SEARCH_IMAGES,
                    )
                ],
            },
            actions=[],
        )

        result = await advert_data_transform(campaign)

        assert result == [
            dict(
                pages=pages,
                log_id="ac_auto_log_id_campaign_1",
                title="search pin title 1",
                text="text",
                disclaimer="disclaimer",
                companies=companies_ids,
                fields=dict(
                    advert_type="menu_icon",
                    anchorDust="0.5 0.5",
                    anchorDustHover="0.5 0.5",
                    anchorDustVisited="0.5 0.5",
                    anchorIcon="0.5 0.89",
                    anchorIconHover="0.5 0.89",
                    anchorIconVisited="0.5 0.89",
                    anchorSelected="0.5 0.94",
                    sizeDust="18 18",
                    sizeDustHover="18 18",
                    sizeDustVisited="18 18",
                    sizeIcon="32 38",
                    sizeIconHover="32 38",
                    sizeIconVisited="32 38",
                    sizeSelected="60 68",
                    styleLogo=f"{AVATARS_NAMESPACE}--11--123--logo",
                    styleBalloonBanner=f"{AVATARS_NAMESPACE}--12--124--banner",
                    styleDust=f"{AVATARS_NAMESPACE}--13--124--geo_adv_dust",
                    styleDustHover=f"{AVATARS_NAMESPACE}--14--124--geo_adv_dust",
                    styleDustVisited=f"{AVATARS_NAMESPACE}--15--124--geo_adv_dust",
                    styleIcon=f"{AVATARS_NAMESPACE}--16--124--geo_adv_drop",
                    styleIconHover=f"{AVATARS_NAMESPACE}--17--124--geo_adv_drop_hover",
                    styleIconVisited=f"{AVATARS_NAMESPACE}--18--124--geo_adv_drop_visited",  # noqa: E501
                    styleSelected=f"{AVATARS_NAMESPACE}--19--124--geo_adv_pin",
                ),
                **expected_attrs,
            )
        ]


@pytest.mark.parametrize(
    ["experiment", "publication_env", "pages"],
    [
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
        ),
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.PRODUCTION,
            ["p1"],
        ),
    ],
)
async def test_no_returns_highlighted_attr_for_navi_campaign_pin_search(
    experiment, publication_env, pages, companies_factory, experimental_options
):
    with experimental_options(
        {"EXPERIMENT_USE_HIGHLIGHTED_ATTR_FOR_PIN_SEARCH": experiment}
    ):
        companies = companies_factory(count=2)
        companies_ids = list(map(attrgetter("permalink"), companies))

        campaign = dict(
            id=1,
            campaign_type=CampaignType.PIN_SEARCH,
            publication_envs=[publication_env],
            platforms=[PlatformEnum.NAVI],
            pages=pages,
            creatives={
                CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
                CreativeType.PIN_SEARCH: [
                    dict(
                        title="search pin title 1",
                        organizations={value.permalink: value for value in companies},
                        images=DEFAULT_PIN_SEARCH_IMAGES,
                    )
                ],
            },
            actions=[],
        )

        result = await advert_data_transform(campaign)

        assert result == [
            dict(
                pages=pages,
                log_id="ac_auto_log_id_campaign_1",
                title="search pin title 1",
                text="text",
                disclaimer="disclaimer",
                companies=companies_ids,
                fields=dict(
                    advert_type="menu_icon",
                    anchorDust="0.5 0.5",
                    anchorDustHover="0.5 0.5",
                    anchorDustVisited="0.5 0.5",
                    anchorIcon="0.5 0.5",
                    anchorIconHover="0.5 0.5",
                    anchorIconVisited="0.5 0.5",
                    anchorSelected="0.5 0.94",
                    sizeDust="18 18",
                    sizeDustHover="18 18",
                    sizeDustVisited="18 18",
                    sizeIcon="32 32",
                    sizeIconHover="32 32",
                    sizeIconVisited="32 32",
                    sizeSelected="60 68",
                    styleDust=f"{AVATARS_NAMESPACE}--13--124--geo_adv_dust",
                    styleDustHover=f"{AVATARS_NAMESPACE}--14--124--geo_adv_dust",
                    styleDustVisited=f"{AVATARS_NAMESPACE}--15--124--geo_adv_dust",
                    styleIcon=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                    styleIconHover=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                    styleIconVisited=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                    styleSelected=f"{AVATARS_NAMESPACE}--19--124--geo_adv_pin",
                ),
            )
        ]


@pytest.mark.parametrize(
    ["experiment", "publication_env", "pages", "expected_attrs"],
    [
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
            {"highlighted": "false"},
        ),
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.PRODUCTION,
            ["p1"],
            {"highlighted": "false"},
        ),
        (
            [PublicationEnvEnum.DATA_TESTING],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
            {"highlighted": "false"},
        ),
        (
            [PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.PRODUCTION,
            ["p1"],
            {"highlighted": "false"},
        ),
        ([PublicationEnvEnum.DATA_TESTING], PublicationEnvEnum.PRODUCTION, ["p1"], {}),
        (
            [PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
            {},
        ),
    ],
)
async def test_returns_highlighted_attr_for_navi_campaign_pin_search(
    experiment,
    publication_env,
    pages,
    expected_attrs,
    companies_factory,
    experimental_options,
):
    with experimental_options(
        {"EXPERIMENT_USE_HIGHLIGHTED_ATTR_FOR_PIN_SEARCH_NAVI": experiment}
    ):
        companies = companies_factory(count=2)
        companies_ids = list(map(attrgetter("permalink"), companies))

        campaign = dict(
            id=1,
            campaign_type=CampaignType.PIN_SEARCH,
            publication_envs=[publication_env],
            platforms=[PlatformEnum.NAVI],
            pages=pages,
            creatives={
                CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
                CreativeType.PIN_SEARCH: [
                    dict(
                        title="search pin title 1",
                        organizations={value.permalink: value for value in companies},
                        images=DEFAULT_PIN_SEARCH_IMAGES,
                    )
                ],
            },
            actions=[],
        )

        result = await advert_data_transform(campaign)

        assert result == [
            dict(
                pages=pages,
                log_id="ac_auto_log_id_campaign_1",
                title="search pin title 1",
                text="text",
                disclaimer="disclaimer",
                companies=companies_ids,
                fields=dict(
                    advert_type="menu_icon",
                    anchorDust="0.5 0.5",
                    anchorDustHover="0.5 0.5",
                    anchorDustVisited="0.5 0.5",
                    anchorIcon="0.5 0.5",
                    anchorIconHover="0.5 0.5",
                    anchorIconVisited="0.5 0.5",
                    anchorSelected="0.5 0.94",
                    sizeDust="18 18",
                    sizeDustHover="18 18",
                    sizeDustVisited="18 18",
                    sizeIcon="32 32",
                    sizeIconHover="32 32",
                    sizeIconVisited="32 32",
                    sizeSelected="60 68",
                    styleDust=f"{AVATARS_NAMESPACE}--13--124--geo_adv_dust",
                    styleDustHover=f"{AVATARS_NAMESPACE}--14--124--geo_adv_dust",
                    styleDustVisited=f"{AVATARS_NAMESPACE}--15--124--geo_adv_dust",
                    styleIcon=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                    styleIconHover=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                    styleIconVisited=f"{AVATARS_NAMESPACE}--20--124--pin_search_round",
                    styleSelected=f"{AVATARS_NAMESPACE}--19--124--geo_adv_pin",
                ),
                **expected_attrs,
            )
        ]


@pytest.mark.parametrize(
    ["experiment", "publication_env", "pages"],
    [
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
        ),
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.PRODUCTION,
            ["p1"],
        ),
    ],
)
async def test_no_returns_highlighted_attr_for_maps_campaign_pin_search(
    experiment, publication_env, pages, companies_factory, experimental_options
):
    with experimental_options(
        {"EXPERIMENT_USE_HIGHLIGHTED_ATTR_FOR_PIN_SEARCH_NAVI": experiment}
    ):
        companies = companies_factory(count=2)
        companies_ids = list(map(attrgetter("permalink"), companies))

        campaign = dict(
            id=1,
            campaign_type=CampaignType.PIN_SEARCH,
            publication_envs=[publication_env],
            platforms=[PlatformEnum.MAPS],
            pages=pages,
            creatives={
                CreativeType.TEXT: dict(text="text", disclaimer="disclaimer"),
                CreativeType.PIN_SEARCH: [
                    dict(
                        title="search pin title 1",
                        organizations={value.permalink: value for value in companies},
                        images=DEFAULT_PIN_SEARCH_IMAGES,
                    )
                ],
            },
            actions=[],
        )

        result = await advert_data_transform(campaign)

        assert result == [
            dict(
                pages=pages,
                log_id="ac_auto_log_id_campaign_1",
                title="search pin title 1",
                text="text",
                disclaimer="disclaimer",
                companies=companies_ids,
                fields=dict(
                    advert_type="menu_icon",
                    anchorDust="0.5 0.5",
                    anchorDustHover="0.5 0.5",
                    anchorDustVisited="0.5 0.5",
                    anchorIcon="0.5 0.89",
                    anchorIconHover="0.5 0.89",
                    anchorIconVisited="0.5 0.89",
                    anchorSelected="0.5 0.94",
                    sizeDust="18 18",
                    sizeDustHover="18 18",
                    sizeDustVisited="18 18",
                    sizeIcon="32 38",
                    sizeIconHover="32 38",
                    sizeIconVisited="32 38",
                    sizeSelected="60 68",
                    styleLogo=f"{AVATARS_NAMESPACE}--11--123--logo",
                    styleBalloonBanner=f"{AVATARS_NAMESPACE}--12--124--banner",
                    styleDust=f"{AVATARS_NAMESPACE}--13--124--geo_adv_dust",
                    styleDustHover=f"{AVATARS_NAMESPACE}--14--124--geo_adv_dust",
                    styleDustVisited=f"{AVATARS_NAMESPACE}--15--124--geo_adv_dust",
                    styleIcon=f"{AVATARS_NAMESPACE}--16--124--geo_adv_drop",
                    styleIconHover=f"{AVATARS_NAMESPACE}--17--124--geo_adv_drop_hover",
                    styleIconVisited=f"{AVATARS_NAMESPACE}--18--124--geo_adv_drop_visited",  # noqa: E501
                    styleSelected=f"{AVATARS_NAMESPACE}--19--124--geo_adv_pin",
                ),
            )
        ]
