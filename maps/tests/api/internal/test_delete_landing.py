import functools

import pytest

from maps_adv.geosmb.landlord.proto import common_pb2
from maps_adv.geosmb.landlord.proto.internal import landing_details_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/delete_landing/"


@pytest.fixture
def api(api):
    api.post = functools.partial(api.post, headers={"X-Ya-Service-Ticket": "ticket"})

    return api


async def test_deletes_landing(api, factory):
    await factory.insert_biz_state(biz_id=15)

    await api.post(
        URL,
        proto=landing_details_pb2.DeleteLandingInput(biz_id=15),
        expected_status=204,
    )

    assert await factory.fetch_biz_state(biz_id=15) is None


async def test_does_not_affect_another_biz_state(api, factory):
    await factory.insert_biz_state(biz_id=15, slug="cafe")
    await factory.insert_biz_state(biz_id=25, slug="sushi")

    await api.post(
        URL,
        proto=landing_details_pb2.DeleteLandingInput(biz_id=15),
        expected_status=204,
    )

    assert await factory.fetch_biz_state(biz_id=25) is not None


async def test_returns_nothing(api, factory):
    await factory.insert_biz_state(biz_id=15)

    got = await api.post(
        URL,
        proto=landing_details_pb2.DeleteLandingInput(biz_id=15),
        expected_status=204,
    )

    assert got == b""


async def test_returns_error_if_no_such_biz_id(api, factory):
    await factory.insert_biz_state(biz_id=25, slug="restoran")

    got = await api.post(
        URL,
        proto=landing_details_pb2.DeleteLandingInput(biz_id=15),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.BIZ_ID_UNKNOWN)
