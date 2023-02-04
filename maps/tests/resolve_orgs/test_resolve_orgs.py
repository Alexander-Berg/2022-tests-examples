import pytest
from multidict import MultiDict
from yandex.maps.proto.common2 import geo_object_pb2, geometry_pb2, response_pb2

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(
    client, aresponses, mock_resolve_orgs, make_multi_response
):
    query = None
    resp = make_multi_response()

    async def geo_handler(request):
        nonlocal query
        query = MultiDict(request.query)
        return aresponses.Response(status=200, body=resp.SerializeToString())

    aresponses.add("geosearch.test", "/", "GET", geo_handler)

    await client.resolve_orgs([12345, 23456])

    expected = MultiDict(
        {
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
    )
    expected.add("business_oid", "23456")
    assert query.items() == expected.items()


async def test_returns_empty_on_empty_reply(client, mock_resolve_orgs):
    mock_resolve_orgs(response_pb2.Response())

    result = await client.resolve_orgs([12345, 23456])

    assert result == []


async def test_returns_not_return_empty_geo_object(client, mock_resolve_orgs):
    mock_resolve_orgs(
        response_pb2.Response(
            reply=geo_object_pb2.GeoObject(
                geo_object=[
                    geo_object_pb2.GeoObject(
                        geometry=[
                            geometry_pb2.Geometry(
                                point=geometry_pb2.Point(lat=11.22, lon=22.33)
                            )
                        ]
                    )
                ]
            )
        )
    )

    result = await client.resolve_orgs([12345, 23456])

    assert len(result) == 1


@pytest.mark.parametrize(
    ("field", "expected_value"),
    [
        ("permalink", ["54321", "65432"]),
        ("name", ["Кафе", "Парикмахерская"]),
        ("categories_names", [["Общепит", "Ресторан"], ["Парикмахерская", "Маникюр"]]),
        ("formatted_address", ["Улица, 1", "Проспект, 2"]),
        (
            "formatted_phones",
            [
                ["+7 (495) 739-70-00", "+7 (495) 739-70-11"],
                ["+7 (833) 111-22-33", "+7 (833) 111-22-33"],
            ],
        ),
        (
            "links",
            [
                ["http://cafe.ru", "http://haircut.com", "http://cafe.livejournal.com"],
                [
                    "http://haircut.ru",
                    "http://haircut.com",
                    "http://haircut.livejournal.com",
                ],
            ],
        ),
    ],
)
async def test_simple_fields(
    client, mock_resolve_orgs, make_multi_response, field, expected_value
):
    mock_resolve_orgs(make_multi_response())

    result = await client.resolve_orgs([12345, 23456])

    assert [getattr(r, field) for r in result] == expected_value
