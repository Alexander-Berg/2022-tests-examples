import pytest

from yandex.maps.proto.search import experimental_pb2

pytestmark = [pytest.mark.asyncio]


async def test_bookings(client, mock_resolve_org, make_response):
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert result.bookings[0]["standaloneWidgetPath"] == \
           "/web-maps/webview?mode=booking&booking[permalink]=1085365923&booking[standalone]=true&source=partner-cta"


async def test_bookings_extension_is_optional(client, mock_resolve_org, make_response):
    response = make_response()
    for metadata in response.reply.geo_object[0].metadata:
        metadata.ClearExtension(experimental_pb2.GEO_OBJECT_METADATA)
    mock_resolve_org(response)

    result = await client.resolve_org(12345)

    assert result.bookings == []
