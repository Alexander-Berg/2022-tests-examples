import pytest
from yandex.maps.proto.search import business_pb2

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("props", "expected_is_online"),
    [
        (
            [
                {"key": "is_online", "value": "1"},
            ],
            True,
        ),
        (
            [
                {"key": "is_online", "value": "0"},
            ],
            False,
        ),
        (
            [
                {"key": "is_online", "value": "38"},
            ],
            False,
        ),
        (
            [
                {"key": "is_something_else", "value": "1"},
            ],
            False,
        ),
        (
            [
                {"key": "is_something_more", "value": "1"},
                {"key": "is_online", "value": "1"},
                {"key": "is_something_else", "value": "1"},
            ],
            True,
        ),
        (
            [
                {"key": "is_something_more", "value": "1"},
                {"key": "is_online", "value": "0"},
                {"key": "is_something_else", "value": "1"},
            ],
            False,
        ),
    ],
)
async def test_org_is_online(
    client, mock_resolve_org, make_response, business_go_meta, props, expected_is_online
):
    business_go_meta.properties.MergeFrom(
        business_pb2.Properties(
            item=list(business_pb2.Properties.Item(**prop) for prop in props)
        )
    )
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.is_online is expected_is_online
