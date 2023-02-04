import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_default_options(domain, dm):
    await domain.generate_data_for_biz_id(biz_id=15)

    landing_data = dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    assert landing_data["blocks_options"] == {
        "show_cover": True,
        "show_logo": True,
        "show_schedule": True,
        "show_photos": True,
        "show_map_and_address": True,
        "show_services": True,
        "show_reviews": True,
        "show_extras": True,
    }


async def test_generates_show_cover_false_if_no_cover(domain, dm, geosearch):
    geosearch.resolve_org.coro.return_value.cover = None

    await domain.generate_data_for_biz_id(biz_id=15)

    landing_data = dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    assert landing_data["blocks_options"]["show_cover"] is False


async def test_generates_show_logo_false_if_no_logo(domain, dm, geosearch):
    geosearch.resolve_org.coro.return_value.logo = None

    await domain.generate_data_for_biz_id(biz_id=15)

    landing_data = dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    assert landing_data["blocks_options"]["show_logo"] is False


async def test_generates_show_extras_false_if_no_extras(domain, dm, geosearch):
    geosearch.resolve_org.coro.return_value.features = []

    await domain.generate_data_for_biz_id(biz_id=15)

    landing_data = dm.save_landing_data_for_biz_id.call_args.kwargs["landing_data"]
    assert landing_data["blocks_options"]["show_extras"] is False
