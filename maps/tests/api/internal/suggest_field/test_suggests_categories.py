import pytest

from maps_adv.geosmb.landlord.proto import common_pb2
from maps_adv.geosmb.landlord.proto.internal import suggests_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]

URL = "/v1/suggests/categories/"


@pytest.fixture
async def biz_state_with_permalink(factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", permalink="2233", stable_version=data_id
    )


@pytest.mark.parametrize(
    "categories_names",
    [
        ["Общепит", "Ресторан"],
        ["Карусель"],
        [],
    ],
)
@pytest.mark.usefixtures("biz_state_with_permalink")
async def test_suggests_options_from_geosearch(
    api, factory, geosearch, categories_names
):
    geosearch.resolve_org.coro.return_value.categories_names = categories_names

    got = await api.post(
        URL,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.PlainSuggest,
        expected_status=200,
    )

    assert got == suggests_pb2.PlainSuggest(options=categories_names)


@pytest.mark.usefixtures("biz_state_with_permalink")
async def test_uses_geosearch_client(api, geosearch):
    await api.post(
        URL,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.PlainSuggest,
        expected_status=200,
    )

    geosearch.resolve_org.assert_called_with(permalink=2233)


async def test_returns_error_for_unknown_biz_id(api):
    got = await api.post(
        URL,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=common_pb2.Error,
        expected_status=400,
    )

    assert got == common_pb2.Error(code=common_pb2.Error.BIZ_ID_UNKNOWN)
