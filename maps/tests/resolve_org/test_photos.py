import pytest

from yandex.maps.proto.search import experimental_pb2

pytestmark = [pytest.mark.asyncio]


async def test_photos(client, mock_resolve_org, make_response):
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.photos == [
        {
            "id": "id1",
            "url": "https://images.ru/tpl1/%s",
        },
        {
            "id": "id4",
            "url": "https://images.ru/tpl4/%s",
        },
    ]


async def test_photos_extension_is_optional(client, mock_resolve_org, make_response):
    response = make_response()
    for metadata in response.reply.geo_object[0].metadata:
        metadata.ClearExtension(experimental_pb2.GEO_OBJECT_METADATA)
    mock_resolve_org(response)

    result = await client.resolve_org(12345)

    assert result.photos == []


async def test_cover(client, mock_resolve_org, make_response):
    mock_resolve_org(make_response())
    result = await client.resolve_org(12345)
    assert result.cover == "https://images.ru/tpl1/%s"
