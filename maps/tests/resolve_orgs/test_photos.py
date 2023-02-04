import pytest
from yandex.maps.proto.search import experimental_pb2

pytestmark = [pytest.mark.asyncio]


async def test_photos(client, mock_resolve_orgs, make_multi_response):
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.photos for r in result] == [
        [
            {"id": "id1", "url": "https://images.ru/tpl1/%s"},
            {"id": "id4", "url": "https://images.ru/tpl4/%s"},
        ],
        [
            {"id": "id1", "url": "https://images.ru/tpl1/%s"},
            {"id": "id4", "url": "https://images.ru/tpl4/%s"},
        ],
    ]


async def test_photos_extension_is_optional(client, mock_resolve_orgs, make_multi_response):
    response = make_multi_response()
    for metadata in response.reply.geo_object[0].metadata:
        metadata.ClearExtension(experimental_pb2.GEO_OBJECT_METADATA)
    mock_resolve_orgs(response)

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].photos == []


async def test_cover(client, mock_resolve_orgs, make_multi_response):
    mock_resolve_orgs(make_multi_response())
    result = await client.resolve_orgs([12345, 23456])
    assert [r.cover for r in result] == [
        "https://images.ru/tpl1/%s",
        "https://images.ru/abc6/%s",
    ]
