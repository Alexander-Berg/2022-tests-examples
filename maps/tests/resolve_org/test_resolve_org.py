import pytest
from yandex.maps.proto.common2 import geo_object_pb2, response_pb2

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(
    client, aresponses, mock_resolve_org, make_response
):
    query = None
    resp = make_response()

    async def geo_handler(request):
        nonlocal query
        query = dict(request.query)
        return aresponses.Response(status=200, body=resp.SerializeToString())

    aresponses.add("geosearch.test", "/", "GET", geo_handler)

    await client.resolve_org(12345)

    assert query == {
        "ms": "pb",
        "lang": "ru",
        "type": "biz",
        "origin": "maps-adv-bookings-yang",
        "business_oid": "12345",
        "show_online_orgs": "both",
        "snippets": ",".join(
            [
                "businessimages/1.x",
                "businessrating/1.x",
                "photos/2.x",
                "metrika_snippets/1.x",
                "online_snippets/1.x",
                "sprav_proto_photos",
                "bookings/1.x"
            ]
        ),
    }


async def test_returns_none_on_empty_reply(client, mock_resolve_org):
    mock_resolve_org(response_pb2.Response())

    result = await client.resolve_org(12345)

    assert result is None


async def test_returns_none_on_empty_geo_object(client, mock_resolve_org):
    mock_resolve_org(response_pb2.Response(reply=geo_object_pb2.GeoObject()))

    result = await client.resolve_org(12345)

    assert result is None


@pytest.mark.parametrize(
    ("field", "expected_value"),
    [
        ("permalink", "54321"),
        ("name", "Кафе"),
        ("categories_names", ["Общепит", "Ресторан"]),
        ("formatted_address", "Улица, 1"),
        ("formatted_phones", ["+7 (495) 739-70-00", "+7 (495) 739-70-11"]),
        (
            "links",
            ["http://cafe.ru", "http://haircut.com", "http://cafe.livejournal.com"],
        ),
    ],
)
async def test_simple_fields(
    client, mock_resolve_org, make_response, field, expected_value
):
    mock_resolve_org(make_response())

    result = await client.resolve_org(12345)

    assert getattr(result, field) == expected_value
