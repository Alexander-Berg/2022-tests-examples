import pytest
from yandex.maps.proto.search import business_images_pb2

pytestmark = [pytest.mark.asyncio]


async def test_logo(client, mock_resolve_org, make_response):
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.logo == "https://images.ru/logo/%s"


async def test_logo_optional(
    client, mock_resolve_org, make_response, business_images_go_meta
):
    business_images_go_meta.ClearField("logo")
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.logo is None


async def test_logo_extension_optional(client, mock_resolve_org, make_response):
    response = make_response()
    for metadata in response.reply.geo_object[0].metadata:
        metadata.ClearExtension(business_images_pb2.GEO_OBJECT_METADATA)
    mock_resolve_org(response)

    result = await client.resolve_org(12345)

    assert result.logo is None
