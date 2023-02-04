from decimal import Decimal

import pytest
from yandex.maps.proto.search import business_rating_pb2

pytestmark = [pytest.mark.asyncio]


async def test_rating(client, mock_resolve_org, make_response):
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.rating == {"ratings": 10, "reviews": 5, "score": Decimal("3.5")}


async def test_rating_extension_optional(client, mock_resolve_org, make_response):
    response = make_response()
    for metadata in response.reply.geo_object[0].metadata:
        metadata.ClearExtension(business_rating_pb2.GEO_OBJECT_METADATA)
    mock_resolve_org(response)

    result = await client.resolve_org(12345)

    assert result.rating is None


async def test_rating_score_is_none_if_no_data(
    client, mock_resolve_org, make_response, business_rating_go_meta
):
    business_rating_go_meta.ratings = 0
    business_rating_go_meta.reviews = 0
    business_rating_go_meta.ClearField("score")
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.rating == {"ratings": 0, "reviews": 0, "score": None}
