import pytest

from maps_adv.geosmb.landlord.server.lib.enums import LandingVersion

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize("version", list(LandingVersion))
@pytest.mark.parametrize(
    ("false_option", "cleared_field"),
    [
        ("show_cover", "cover"),
        ("show_logo", "logo"),
        ("show_schedule", "schedule"),
        ("show_services", "services"),
        ("show_reviews", "rating"),
        ("show_extras", "extras"),
    ],
)
async def test_respects_blocks_options_for_removable_fields(
    domain, dm, version, false_option, cleared_field
):
    dm.fetch_landing_data_by_slug.coro.return_value["blocks_options"][
        false_option
    ] = False

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=version
    )

    assert cleared_field not in result


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_respects_block_options_for_list_fields(domain, dm, version):
    dm.fetch_landing_data_by_slug.coro.return_value["blocks_options"][
        "show_photos"
    ] = False

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=version
    )

    assert result["photos"] == []


@pytest.mark.parametrize("version", list(LandingVersion))
async def test_respects_block_options_for_address_and_map(domain, dm, version):
    dm.fetch_landing_data_by_slug.coro.return_value["blocks_options"][
        "show_map_and_address"
    ] = False

    result = await domain.fetch_published_landing_data_by_slug(
        slug="cafe", token="fetch_data_token", version=version
    )

    assert "geo" not in result["contacts"]
