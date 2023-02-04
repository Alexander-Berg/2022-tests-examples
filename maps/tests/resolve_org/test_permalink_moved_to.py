import pytest
from yandex.maps.proto.search import business_pb2

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    ("props", "expected_moved_to"),
    [
        (
            [
                {"key": "moved_to", "value": "12345"},
            ],
            "12345",
        ),
        (
            [
                {"key": "is_something_more", "value": "1"},
                {"key": "moved_to", "value": "12345"},
                {"key": "is_something_else", "value": "1"},
            ],
            "12345",
        ),
        (
            [
                {"key": "is_something_more", "value": "1"},
            ],
            None,
        ),
    ],
)
async def test_permalink_moved_to(
    client, mock_resolve_org, make_response, business_go_meta, props, expected_moved_to
):
    business_go_meta.properties.MergeFrom(
        business_pb2.Properties(
            item=list(business_pb2.Properties.Item(**prop) for prop in props)
        )
    )
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.permalink_moved_to == expected_moved_to
