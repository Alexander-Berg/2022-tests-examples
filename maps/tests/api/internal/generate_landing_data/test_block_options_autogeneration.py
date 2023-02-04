import pytest

from maps_adv.geosmb.landlord.proto import generate_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/generate_landing_data/"


async def test_default_options(api, factory):
    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(biz_id=15),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert all(
        landing_data["blocks_options"]
        == {
            "show_cover": True,
            "show_logo": True,
            "show_schedule": True,
            "show_photos": True,
            "show_map_and_address": True,
            "show_services": True,
            "show_reviews": True,
            "show_extras": True,
        }
        for landing_data in landing_datas
    )


async def test_generates_show_cover_false_if_no_cover(api, factory, geosearch):
    geosearch.resolve_org.coro.return_value.cover = None

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(biz_id=15),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert all(
        landing_data["blocks_options"]["show_cover"] is False
        for landing_data in landing_datas
    )


async def test_generates_show_logo_false_if_no_logo(api, factory, geosearch):
    geosearch.resolve_org.coro.return_value.logo = None

    await api.post(
        URL,
        proto=generate_pb2.GenerateDataInput(biz_id=15),
        decode_as=generate_pb2.GenerateDataOutput,
        expected_status=201,
    )

    landing_datas = await factory.list_all_landing_data()
    assert all(
        landing_data["blocks_options"]["show_logo"] is False
        for landing_data in landing_datas
    )
