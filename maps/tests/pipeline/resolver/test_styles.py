import pytest

from maps_adv.export.lib.core.enum import CampaignType, CreativeType, ImageType
from maps_adv.export.lib.pipeline.resolver.styles import styles_resolver

pytestmark = [pytest.mark.asyncio]


async def test_returns_expected_style_for_campaign_type_of_category(config):
    namespace = config.AVATARS_NAMESPACE
    campaign_id = 1
    campaigns = [
        dict(
            id=campaign_id,
            campaign_type=CampaignType.CATEGORY,
            creatives={
                CreativeType.ICON: dict(
                    text="icon text",
                    position=5,
                    title="icon title",
                    search_text="icon search text",
                    images=[
                        dict(
                            type=ImageType.CATEGORY,
                            image_name="image-name",
                            group_id="group-id",
                            alias_template="category_{zoom}",
                        )
                    ],
                )
            },
        )
    ]

    await styles_resolver(campaigns)

    creatives = campaigns[0]["creatives"]
    icon = creatives[CreativeType.ICON]["style"]

    assert icon == f"{namespace}--group-id--image-name"


@pytest.mark.parametrize(
    "campaign_type", list(set(CampaignType) - {CampaignType.CATEGORY})
)
async def test_no_exists_style_field_for_any_campaign_types_with_the_exception_of_category_and_pin_search(  # noqa: E501
    campaign_type,
):
    campaigns = [dict(campaign_type=campaign_type, creatives={})]

    await styles_resolver(campaigns)

    assert "style" not in campaigns[0]
