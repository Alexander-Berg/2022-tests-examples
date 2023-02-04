import pytest
from aiohttp.web import Response, json_response

from smb.common.aiotvm import UnknownResponse
from maps_adv.common.geoproduct import RequestedObjectNotFound

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_list_reservations(aresponses):
    return lambda *a: aresponses.add("geoproduct.api", "/v3/reservations", "GET", *a)


async def test_sends_correct_request(client, mock_list_reservations, aresponses):
    request_url = []
    request_body = []
    request_headers = []

    async def _handler(request):
        nonlocal request_url, request_body, request_headers
        request_url.append(str(request.url))
        request_body.append(await request.read())
        request_headers.append(request.headers)
        return json_response(status=200)

    mock_list_reservations(
        json_response(status=200, data=[{"id": 678932}, {"id": 345672}])
    )
    aresponses.add("geoproduct.api", "/v3/reservations/678932", "DELETE", _handler)
    aresponses.add("geoproduct.api", "/v3/reservations/345672", "DELETE", _handler)

    await client.delete_organization_reservations(permalink=98765324)

    assert request_url == [
        "http://geoproduct.api/v3/reservations/678932",
        "http://geoproduct.api/v3/reservations/345672",
    ]
    assert request_body == [b"", b""]
    assert [r["X-Ya-Default-Uid"] for r in request_headers] == [
        "1010",
        "1010",
    ]


async def test_returns_nothing(client, mock_list_reservations, aresponses):
    mock_list_reservations(json_response(status=200, data=[{"id": 678932}]))
    aresponses.add(
        "geoproduct.api",
        "/v3/reservations/678932",
        "DELETE",
        json_response(status=200),
    )

    got = await client.delete_organization_reservations(permalink=98765324)

    assert got is None


async def test_raises_for_unknown_reservation_id(
    client, mock_list_reservations, aresponses
):
    mock_list_reservations(json_response(status=200, data=[{"id": 678932}]))
    aresponses.add(
        "geoproduct.api",
        "/v3/reservations/678932",
        "DELETE",
        json_response(status=404),
    )

    with pytest.raises(RequestedObjectNotFound):
        await client.delete_organization_reservations(permalink=98765324)


async def test_raises_for_unknown_response(client, mock_list_reservations, aresponses):
    mock_list_reservations(json_response(status=200, data=[{"id": 678932}]))
    aresponses.add(
        "geoproduct.api",
        "/v3/reservations/678932",
        "DELETE",
        Response(status=499, body=b"any_body"),
    )

    with pytest.raises(UnknownResponse) as exc:
        await client.delete_organization_reservations(permalink=98765324)

    assert exc.value.request_info.headers["X-Ya-Service-Ticket"] == "..."
