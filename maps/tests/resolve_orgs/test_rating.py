from decimal import Decimal

import pytest
from yandex.maps.proto.search import business_rating_pb2

pytestmark = [pytest.mark.asyncio]


async def test_rating(client, mock_resolve_orgs, make_multi_response):
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.rating for r in result] == [
        {"ratings": 10, "reviews": 5, "score": Decimal("3.5")},
        {"ratings": 100, "reviews": 50, "score": Decimal("1.5")},
    ]


async def test_rating_extension_optional(
    client, mock_resolve_orgs, make_multi_response
):
    response = make_multi_response()
    for metadata in response.reply.geo_object[0].metadata:
        metadata.ClearExtension(business_rating_pb2.GEO_OBJECT_METADATA)
    mock_resolve_orgs(response)

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].rating is None


async def test_rating_score_is_none_if_no_data(
    client, mock_resolve_orgs, make_multi_response, business_rating_go_meta_multi
):
    business_rating_go_meta_multi[0].ratings = 0
    business_rating_go_meta_multi[0].reviews = 0
    business_rating_go_meta_multi[0].ClearField("score")
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].rating == {"ratings": 0, "reviews": 0, "score": None}
