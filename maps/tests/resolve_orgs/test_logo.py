import pytest
from yandex.maps.proto.search import business_images_pb2

pytestmark = [pytest.mark.asyncio]


async def test_logo(client, mock_resolve_orgs, make_multi_response):
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [r.logo for r in result] == [
        "https://images.ru/logo/%s",
        "https://images.ru/my_logo/%s",
    ]


async def test_logo_optional(
    client, mock_resolve_orgs, make_multi_response, business_images_go_meta_multi
):
    business_images_go_meta_multi[0].ClearField("logo")
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].logo is None


async def test_logo_extension_optional(client, mock_resolve_orgs, make_multi_response):
    response = make_multi_response()
    for metadata in response.reply.geo_object[0].metadata:
        metadata.ClearExtension(business_images_pb2.GEO_OBJECT_METADATA)
    mock_resolve_orgs(response)

    result = await client.resolve_orgs([12345, 23456])

    assert result[0].logo is None
