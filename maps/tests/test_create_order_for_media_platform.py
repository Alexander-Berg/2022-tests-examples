import json

import pytest
from aiohttp.web import Response, json_response
from smb.common.http_client import UnknownResponse

pytestmark = [pytest.mark.asyncio]

ORDER_DATA = {"operator_id": 0, "client_id": 1, "product_id": 2, "act_text": "text"}


@pytest.fixture
def mock_response(aresponses):
    return lambda *args: aresponses.add(
        "geoproduct.api", "/v4/balance-client/geoadv-agency-order", "POST", *args
    )


async def test_sends_correct_request(client, mock_response):
    request_url = None
    request_headers = None
    request_body = None

    async def handler(request):
        nonlocal request_url, request_headers, request_body
        request_url = str(request.url)
        request_body = await request.read()
        request_headers = dict(request.headers)
        return json_response({"order_id": 123})

    mock_response(handler)

    await client.create_order_for_media_platform(**ORDER_DATA)

    assert request_url == "http://geoproduct.api/v4/balance-client/geoadv-agency-order"
    assert (
        request_body
        == json.dumps(
            {"operator_id": 0, "client_id": 1, "product_id": 2, "act_text": "text"}
        ).encode()
    )
    assert request_headers["Content-Type"] == "application/json"
    assert request_headers["X-Ya-Service-Ticket"] == "KEK_FROM_AIOTVM_PYTEST_PLUGIN"
    assert request_headers["X-Ya-Default-Uid"] == "1010"


async def test_returns_order_id_on_success(client, mock_response):
    mock_response(json_response({"order_id": 123}))

    order_id = await client.create_order_for_media_platform(**ORDER_DATA)

    assert order_id == 123


async def test_raises_on_error(client, mock_response):
    mock_response(Response(status=403, body=b"ERROR"))

    with pytest.raises(UnknownResponse):
        await client.create_order_for_media_platform(**ORDER_DATA)
