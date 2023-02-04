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
    client,
    mock_resolve_orgs,
    make_multi_response,
    business_go_meta_multi,
    props,
    expected_is_online,
):
    business_go_meta_multi[0].properties.MergeFrom(
        business_pb2.Properties(
            item=list(business_pb2.Properties.Item(**prop) for prop in props)
        )
    )
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].is_online is expected_is_online
