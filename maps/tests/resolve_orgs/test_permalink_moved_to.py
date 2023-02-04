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
    client,
    mock_resolve_orgs,
    make_multi_response,
    business_go_meta_multi,
    props,
    expected_moved_to,
):
    business_go_meta_multi[0].properties.MergeFrom(
        business_pb2.Properties(
            item=list(business_pb2.Properties.Item(**prop) for prop in props)
        )
    )
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].permalink_moved_to == expected_moved_to
