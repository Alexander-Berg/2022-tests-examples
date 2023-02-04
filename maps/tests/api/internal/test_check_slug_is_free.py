import pytest

from maps_adv.geosmb.landlord.proto.internal import landing_details_pb2

pytestmark = [pytest.mark.asyncio]

URL = "/v1/check_slug_is_free/"


@pytest.mark.parametrize("published", [False, True])
async def test_returns_true_if_slug_is_not_used(factory, api, published):
    await factory.insert_biz_state(biz_id=22, slug="rest", published=published)

    result = await api.post(
        URL,
        proto=landing_details_pb2.CheckSlugAvailableInput(slug="cafe"),
        decode_as=landing_details_pb2.CheckSlugAvailableOutput,
        expected_status=200,
    )

    assert result == landing_details_pb2.CheckSlugAvailableOutput(available=True)


@pytest.mark.parametrize("published", [False, True])
async def test_returns_false_if_slug_is_in_use(factory, api, published):
    await factory.insert_biz_state(biz_id=22, slug="cafe", published=published)

    result = await api.post(
        URL,
        proto=landing_details_pb2.CheckSlugAvailableInput(slug="cafe"),
        decode_as=landing_details_pb2.CheckSlugAvailableOutput,
        expected_status=200,
    )

    assert result == landing_details_pb2.CheckSlugAvailableOutput(available=False)


@pytest.mark.parametrize("published", [False, True])
async def test_returns_false_if_slug_is_used_by_another_biz(factory, api, published):
    await factory.insert_biz_state(biz_id=22, slug="cafe", published=published)

    result = await api.post(
        URL,
        proto=landing_details_pb2.CheckSlugAvailableInput(
            slug="cafe",
            biz_id=21,
        ),
        decode_as=landing_details_pb2.CheckSlugAvailableOutput,
        expected_status=200,
    )

    assert result == landing_details_pb2.CheckSlugAvailableOutput(available=False)


@pytest.mark.parametrize("published", [False, True])
async def test_returns_true_if_slug_is_used_by_me(factory, api, published):
    await factory.insert_biz_state(biz_id=22, slug="cafe", published=published)

    result = await api.post(
        URL,
        proto=landing_details_pb2.CheckSlugAvailableInput(slug="cafe", biz_id=22),
        decode_as=landing_details_pb2.CheckSlugAvailableOutput,
        expected_status=200,
    )

    assert result == landing_details_pb2.CheckSlugAvailableOutput(available=True)
