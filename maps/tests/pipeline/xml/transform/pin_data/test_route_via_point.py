from typing import Callable

import pytest

from maps_adv.adv_store.api.schemas.enums import PlatformEnum, PublicationEnvEnum
from maps_adv.export.lib.core.enum import (
    CampaignType,
    CreativeType,
    ImageType,
)
from maps_adv.export.lib.pipeline.xml.transform.pin_data import pin_data_transform

pytestmark = [pytest.mark.asyncio]

SUPPORTED_PLATFORMS = [PlatformEnum.NAVI]
UNSUPPORTED_PLATFORMS = list(set(PlatformEnum) - set(SUPPORTED_PLATFORMS))


@pytest.mark.parametrize(
    ["limits", "expected_fields", "expected_limits"],
    [
        (
            dict(
                user_daily_display_limit=3,
                display_probability="0.123",
                display_chance=645,
            ),
            dict(limitImpressionsPerDay=3),
            dict(displayProbability="0.123", impressions=dict(dailyPerUser=3)),
        ),
        (dict(display_probability="0.123"), dict(), dict(displayProbability="0.123")),
        (
            dict(user_daily_display_limit=3),
            dict(limitImpressionsPerDay=3),
            dict(impressions=dict(dailyPerUser=3)),
        ),
        (dict(display_chance=0), dict(), dict()),
        ({}, {}, {}),
        (
            dict(
                user_daily_display_limit=3,
                display_probability="0.123",
            ),
            dict(limitImpressionsPerDay=3),
            dict(
                displayProbability="0.123",
                impressions=dict(dailyPerUser=3),
            ),
        ),
    ],
)
@pytest.mark.parametrize("platform", SUPPORTED_PLATFORMS)
async def test_will_transform_campaign_type_route_via_point_as_expected(
    platform: PlatformEnum,
    limits: dict,
    expected_limits: dict,
    expected_fields: dict,
    avatars_batch: Callable,
    config,
):
    campaign_id = 1
    product = "route_via_point"
    avatars = avatars_batch(dict(pin=ImageType.PIN))
    campaign = dict(
        id=campaign_id,
        pages=["p1", "p2"],
        places=["bb:1", "bb:2"],
        campaign_type=CampaignType.ROUTE_VIA_POINT,
        publication_envs=[PublicationEnvEnum.PRODUCTION],
        platforms=[platform],
        targeting=dict(tag="and"),
        creatives={
            CreativeType.VIA_POINT: dict(
                button_text_active="via active text",
                button_text_inactive="via inactive text",
                description="via description",
                images=[avatars["pin"].avatar],
            )
        },
        cost=0.0,
        **limits,
    )

    result = await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)

    assert result == [
        dict(
            pages=["p1", "p2"],
            places=["bb:1", "bb:2"],
            polygons=[],
            disclaimer="",
            fields=dict(
                campaignId=campaign_id,
                product=product,
                anchorViaPin="0.5 0.5",
                sizeViaPin="32 32",
                styleViaPin=avatars["pin"].filename,
                stylePin=avatars["pin"].filename,
                viaActiveTitle="via active text",
                viaInactiveTitle="via inactive text",
                viaDescription="via description",
                **expected_fields,
            ),
            creatives=[],
            actions=[],
            limits=expected_limits,
            target=dict(tag="and"),
            log_info=dict(
                advertiserId=None, campaignId=str(campaign_id), product=product
            ),
            cost=0.0,
        )
    ]


@pytest.mark.parametrize("platform", UNSUPPORTED_PLATFORMS)
async def test_returns_empty_result_if_route_via_point_is_not_supported_on_platform(
    platform, config
):
    campaign = dict(campaign_type=CampaignType.ROUTE_VIA_POINT, platforms=[platform])
    result = await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)

    assert result == []


@pytest.mark.parametrize(
    ["experiment", "publication_env", "pages", "expected_fields"],
    [
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
            {},
        ),
        (
            [PublicationEnvEnum.DATA_TESTING, PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.PRODUCTION,
            ["p1"],
            {},
        ),
        (
            [PublicationEnvEnum.DATA_TESTING],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
            {},
        ),
        ([PublicationEnvEnum.PRODUCTION], PublicationEnvEnum.PRODUCTION, ["p1"], {}),
        (
            [PublicationEnvEnum.DATA_TESTING],
            PublicationEnvEnum.PRODUCTION,
            ["p1"],
            {"stylePin": "test-avatars-namespace--4968--spxHlSeqo--vvcaTUxml"},
        ),
        (
            [PublicationEnvEnum.PRODUCTION],
            PublicationEnvEnum.DATA_TESTING,
            ["p1/datatesting"],
            {"stylePin": "test-avatars-namespace--4968--spxHlSeqo--vvcaTUxml"},
        ),
    ],
)
@pytest.mark.parametrize("platform", SUPPORTED_PLATFORMS)
async def test_experiment_remove_stylepin_field(
    experiment,
    publication_env,
    pages,
    platform,
    expected_fields,
    experimental_options,
    config,
):
    with experimental_options(
        {"EXPERIMENT_WITHOUT_STYLE_PIN_FOR_VIAPOINT": experiment}
    ):
        campaign_id = 1
        product = "route_via_point"
        campaign = dict(
            id=campaign_id,
            pages=pages,
            places=["bb:1", "bb:2"],
            campaign_type=CampaignType.ROUTE_VIA_POINT,
            publication_envs=[publication_env],
            platforms=[platform],
            targeting=dict(tag="and"),
            creatives={
                CreativeType.VIA_POINT: dict(
                    button_text_active="via active text",
                    button_text_inactive="via inactive text",
                    description="via description",
                    images=[
                        dict(
                            type=ImageType.PIN,
                            image_name="spxHlSeqo",
                            group_id="4968",
                            alias_template="vvcaTUxml",
                        )
                    ],
                )
            },
            cost=0.0,
        )

        result = await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)

        assert result == [
            dict(
                pages=pages,
                places=["bb:1", "bb:2"],
                polygons=[],
                disclaimer="",
                fields=dict(
                    campaignId=campaign_id,
                    product=product,
                    anchorViaPin="0.5 0.5",
                    sizeViaPin="32 32",
                    styleViaPin="test-avatars-namespace--4968--spxHlSeqo--vvcaTUxml",
                    viaActiveTitle="via active text",
                    viaInactiveTitle="via inactive text",
                    viaDescription="via description",
                    **expected_fields,
                ),
                creatives=[],
                actions=[],
                limits={},
                target=dict(tag="and"),
                log_info=dict(
                    advertiserId=None, campaignId=str(campaign_id), product=product
                ),
                cost=0.0,
            )
        ]


@pytest.mark.parametrize("platform", SUPPORTED_PLATFORMS)
async def test_correct_split_campaign_experiment_remove_stylepin_field(
    platform, experimental_options, config
):
    with experimental_options(
        {"EXPERIMENT_WITHOUT_STYLE_PIN_FOR_VIAPOINT": [PublicationEnvEnum.DATA_TESTING]}
    ):
        campaign_id = 1
        product = "route_via_point"
        campaign = dict(
            id=campaign_id,
            pages=["p1", "p1/datatesting", "p1/testing_abc"],
            places=["bb:1", "bb:2"],
            campaign_type=CampaignType.ROUTE_VIA_POINT,
            publication_envs=[
                PublicationEnvEnum.DATA_TESTING,
                PublicationEnvEnum.PRODUCTION,
            ],
            platforms=[platform],
            targeting=dict(tag="and"),
            creatives={
                CreativeType.VIA_POINT: dict(
                    button_text_active="via active text",
                    button_text_inactive="via inactive text",
                    description="via description",
                    images=[
                        dict(
                            type=ImageType.PIN,
                            image_name="spxHlSeqo",
                            group_id="4968",
                            alias_template="vvcaTUxml",
                        )
                    ],
                )
            },
        )

        result = await pin_data_transform(campaign, None, config.INSTANCE_TAG_CTYPE)

        assert result == [
            dict(
                pages=["p1"],
                places=["bb:1", "bb:2"],
                polygons=[],
                disclaimer="",
                fields=dict(
                    campaignId=campaign_id,
                    product=product,
                    anchorViaPin="0.5 0.5",
                    sizeViaPin="32 32",
                    stylePin="test-avatars-namespace--4968--spxHlSeqo--vvcaTUxml",
                    styleViaPin="test-avatars-namespace--4968--spxHlSeqo--vvcaTUxml",
                    viaActiveTitle="via active text",
                    viaInactiveTitle="via inactive text",
                    viaDescription="via description",
                ),
                creatives=[],
                actions=[],
                limits={},
                target=dict(tag="and"),
                log_info=dict(
                    advertiserId=None, campaignId=str(campaign_id), product=product
                ),
                cost=None,
            ),
            dict(
                pages=["p1/datatesting", "p1/testing_abc"],
                places=["bb:1", "bb:2"],
                polygons=[],
                disclaimer="",
                fields=dict(
                    campaignId=campaign_id,
                    product=product,
                    anchorViaPin="0.5 0.5",
                    sizeViaPin="32 32",
                    styleViaPin="test-avatars-namespace--4968--spxHlSeqo--vvcaTUxml",
                    viaActiveTitle="via active text",
                    viaInactiveTitle="via inactive text",
                    viaDescription="via description",
                ),
                creatives=[],
                actions=[],
                limits={},
                target=dict(tag="and"),
                log_info=dict(
                    advertiserId=None, campaignId=str(campaign_id), product=product
                ),
                cost=None,
            ),
        ]
