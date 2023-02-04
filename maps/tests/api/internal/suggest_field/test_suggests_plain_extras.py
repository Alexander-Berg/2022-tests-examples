import pytest

from maps_adv.geosmb.landlord.proto import common_pb2
from maps_adv.geosmb.landlord.proto.internal import suggests_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]

URL = "/v1/suggests/plain_extras/"


@pytest.fixture
async def biz_state_with_permalink(factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=15, slug="cafe", permalink="2233", stable_version=data_id
    )


@pytest.mark.parametrize(
    ("features", "expected_extras"),
    [
        ([{"id": "wifi", "name": "Wi-fi", "value": True}], ["Wi-fi"]),
        (
            # False value ignored
            [{"id": "wifi", "name": "Wi-fi", "value": False}],
            [],
        ),
        (
            # Text value ignored
            [{"id": "wifi", "name": "Wi-fi", "value": ["2.4G"]}],
            [],
        ),
        (
            # Enum value ignored
            [
                {
                    "id": "wifi",
                    "name": "Wi-fi",
                    "value": {"id": "enum_value1", "name": "Значение 1"},
                }
            ],
            [],
        ),
        (
            # Nameless feature ignored
            [{"id": "wifi", "value": True}],
            [],
        ),
        (
            # Combined case
            [
                {"id": "wifi", "name": "Wi-fi", "value": True},
                {"id": "wifi", "name": "Wi-fi", "value": ["2.4G"]},
                {"id": "card", "name": "Оплата картой", "value": True},
            ],
            ["Wi-fi", "Оплата картой"],
        ),
    ],
)
@pytest.mark.usefixtures("biz_state_with_permalink")
async def test_suggests_options_from_geosearch(
    api, geosearch, features, expected_extras
):
    geosearch.resolve_org.coro.return_value.features = features

    got = await api.post(
        URL,
        proto=suggests_pb2.SuggestInput(biz_id=15),
        decode_as=suggests_pb2.PlainSuggest,
        expected_status=200,
    )

    assert got == suggests_pb2.PlainSuggest(options=expected_extras)


@pytest.mark.usefixtures("biz_state_with_permalink")
async def test_uses_geosearch_client_if_needed(api, geosearch):
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
