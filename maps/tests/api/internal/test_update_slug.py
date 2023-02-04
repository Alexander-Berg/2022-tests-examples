import pytest

from maps_adv.geosmb.landlord.proto import common_pb2
from maps_adv.geosmb.landlord.proto.internal import landing_details_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/update_slug/"


async def test_updates_slug(api, factory):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran"),
        expected_status=204,
    )

    assert (await factory.fetch_biz_state(biz_id=15))["slug"] == "restoran"


async def test_does_not_update_other_biz_id_slug(api, factory):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    await factory.insert_biz_state(biz_id=25, slug="sushi")

    await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran"),
        expected_status=204,
    )

    assert (await factory.fetch_biz_state(biz_id=25))["slug"] == "sushi"


async def test_returns_nothing(api, factory):
    await factory.insert_biz_state(biz_id=15, slug="cafe")

    got = await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran"),
        expected_status=204,
    )

    assert got == b""


async def test_returns_error_if_slug_in_use(api, factory):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    await factory.insert_biz_state(biz_id=25, slug="restoran")

    got = await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran"),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.SLUG_IN_USE)


async def test_returns_error_if_maked_more_than_five_attempts(api, factory):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran1"),
        expected_status=204,
    )
    await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran2"),
        expected_status=204,
    )
    await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran3"),
        expected_status=204,
    )
    await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran4"),
        expected_status=204,
    )
    await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran5"),
        expected_status=204,
    )
    got = await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran6"),
        decode_as=common_pb2.Error,
        expected_status=400,
    )
    assert got == common_pb2.Error(code=common_pb2.Error.TOO_MUCH_SLUGS)


async def test_returns_error_if_no_such_biz_id(api, factory):
    await factory.insert_biz_state(biz_id=25, slug="restoran")

    got = await api.post(
        URL,
        proto=landing_details_pb2.UpdateSlugInput(biz_id=15, slug="restoran"),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.BIZ_ID_UNKNOWN)
