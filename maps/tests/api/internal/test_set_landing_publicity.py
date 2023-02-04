import pytest

from maps_adv.geosmb.landlord.proto import common_pb2
from maps_adv.geosmb.landlord.proto.internal import landing_details_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/set_landing_publicity/"


@pytest.mark.parametrize("old_published", [True, False])
@pytest.mark.parametrize("new_published", [True, False])
async def test_sets_publicity_data(factory, api, old_published, new_published):
    landing_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", published=old_published, stable_version=landing_id
    )

    await api.post(
        URL,
        proto=landing_details_pb2.SetLandingPublicityInput(
            biz_id=15, is_published=new_published
        ),
        expected_status=204,
    )

    state = await factory.fetch_biz_state(biz_id=15)
    assert state["published"] == new_published


async def test_returns_error_if_no_data_exists_for_biz_id(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="restoran", stable_version=data_id, published=True
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.SetLandingPublicityInput(
            biz_id=999, is_published=True
        ),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.BIZ_ID_UNKNOWN)


async def test_returns_error_if_publish_landing_without_stable_version(api, factory):
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", published=False, stable_version=None
    )

    got = await api.post(
        URL,
        proto=landing_details_pb2.SetLandingPublicityInput(
            biz_id=15, is_published=True
        ),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(
        code=common_pb2.Error.NO_STABLE_VERSION_FOR_PUBLISHING
    )
