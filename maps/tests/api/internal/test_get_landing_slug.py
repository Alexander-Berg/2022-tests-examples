import pytest

from maps_adv.geosmb.landlord.proto import common_pb2
from maps_adv.geosmb.landlord.proto.generate_pb2 import GenerateDataOutput
from maps_adv.geosmb.landlord.proto.internal.landing_details_pb2 import PermalinkInput

pytestmark = [pytest.mark.asyncio]

URL = "/v1/get_landing_slug/"


async def test_returns_data(api, factory):
    await factory.insert_biz_state(biz_id=15, permalink="123456", published=True)

    got = await api.post(
        URL,
        proto=PermalinkInput(permalink=123456),
        decode_as=GenerateDataOutput,
        expected_status=200,
    )

    assert got == GenerateDataOutput(slug="cafe")


async def test_raises_if_no_biz_id_for_permalink(api, bvm):
    bvm.fetch_biz_id_no_create_by_permalink.coro.return_value = None

    got = await api.post(
        URL,
        proto=PermalinkInput(permalink=123456),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.NO_BIZ_ID_FOR_ORG)


async def test_raises_if_no_biz_state(api):

    got = await api.post(
        URL,
        proto=PermalinkInput(permalink=123456),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.BIZ_ID_UNKNOWN)


async def test_raises_if_not_published(api, factory):
    await factory.insert_biz_state(biz_id=15, permalink="123456", published=False)

    got = await api.post(
        URL,
        proto=PermalinkInput(permalink=123456),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.BIZ_ID_UNKNOWN)
