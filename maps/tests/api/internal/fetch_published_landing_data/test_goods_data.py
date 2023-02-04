import pytest

from maps_adv.geosmb.landlord.proto import goods_pb2, organization_details_pb2
from maps_adv.geosmb.landlord.server.lib.enums import Feature

pytestmark = [pytest.mark.asyncio]

URL = "/v1/fetch_landing_data/"


async def test_returns_goods_data_as_expected(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(biz_id=22, slug="cafe", published=True, stable_version=data_id, permalink="54321")
    await factory.create_goods_data(permalink=54321)
    await factory.set_cached_landing_config({"features": {Feature.USE_GOODS.value: "enabled"}})

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.goods == goods_pb2.GoodsData(
        goods_available=True, categories=[goods_pb2.Category(name="Категория 1")], source_name="Источник"
    )


async def test_returns_goods_no_data_as_expected(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(biz_id=22, slug="cafe", published=True, stable_version=data_id, permalink="54321")
    await factory.set_cached_landing_config({"features": {Feature.USE_GOODS.value: "enabled"}})

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.goods == goods_pb2.GoodsData(goods_available=False)


async def test_returns_nothing_if_no_feature(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(biz_id=22, slug="cafe", published=True, stable_version=data_id, permalink="54321")
    await factory.create_goods_data(permalink=54321)

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.goods == goods_pb2.GoodsData()
