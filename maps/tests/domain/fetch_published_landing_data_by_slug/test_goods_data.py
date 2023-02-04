import pytest

from maps_adv.geosmb.landlord.server.lib.enums import Feature, LandingVersion

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "goods_data",
    [
        {"goods_available": False},
        {
            "goods_available": True,
            "categories": [
                {"name": "Category 1"},
                {"name": "Category 2"},
            ],
            "source_name": "source",
        },
    ],
)
async def test_returns_goods_data_as_expected(goods_data, domain, dm):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled" if feature == Feature.USE_GOODS else None
    )
    dm.fetch_goods_data_for_permalink.coro.return_value = goods_data

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result["goods"] == goods_data


async def test_returns_nothing_if_feature_disabled(domain, dm):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "disabled" if feature == Feature.USE_GOODS else None
    )

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=LandingVersion.STABLE
    )

    assert result.get("goods") is None
