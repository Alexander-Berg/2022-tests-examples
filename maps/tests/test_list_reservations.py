import pytest
from aiohttp.web import Response, json_response

from smb.common.aiotvm import UnknownResponse

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_list_reservations(aresponses):
    return lambda *a: aresponses.add("geoproduct.api", "/v3/reservations", "GET", *a)


async def test_sends_correct_request(client, mock_list_reservations):
    request_url = None
    request_body = None
    request_headers = None

    async def _handler(request):
        nonlocal request_url, request_body, request_headers
        request_url = str(request.url)
        request_body = await request.read()
        request_headers = request.headers
        return json_response(status=200, data=[])

    mock_list_reservations(_handler)

    await client.list_reservations(permalink=123456)

    assert request_url == "http://geoproduct.api/v3/reservations?permalink=123456"
    assert request_headers["X-Ya-Default-Uid"] == "1010"
    assert request_body == b""


@pytest.mark.parametrize(
    "response_data",
    [
        [],
        [
            {
                "id": 123,
                "active": True,
                "permalinks": [123456],
                "data": {
                    "phone_number": "+7 (000) 000-00-00",
                    "some_field": "some_value",
                },
            },
            {
                "id": 567,
                "active": False,
                "permalinks": [7654432],
                "data": {
                    "phone_number": "+7 (000) 000-00-01",
                    "some_field": "another_value",
                },
            },
        ],
    ],
)
async def test_returns_reservation_data(client, mock_list_reservations, response_data):
    mock_list_reservations(json_response(status=200, data=response_data))

    got = await client.list_reservations(permalink=123456)

    assert got == response_data


async def test_raises_for_unknown_response(client, mock_list_reservations):
    mock_list_reservations(Response(status=499, body=b"any_body"))

    with pytest.raises(UnknownResponse) as exc:
        await client.list_reservations(permalink=123456)

    assert exc.value.request_info.headers["X-Ya-Service-Ticket"] == "..."
