import re

import pytest
from aiohttp.web import Response, json_response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.clients.cdp import ApiAccessDenied

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_segment_size(aresponses):
    def _mocker(resps):
        if not isinstance(resps, (list, tuple)):
            resps = [resps]

        for resp in resps:
            aresponses.add(
                "cdp.test",
                re.compile(r"/cdp/internal/v1/counter/(\d+)/segment/(\d+)/size"),
                "GET",
                resp,
            )

    return _mocker


async def test_sends_correct_request(cdp_client, mock_segment_size):
    sent_request = None

    async def _handler(request):
        nonlocal sent_request
        sent_request = request
        return json_response(data={"segment_size": 15})

    mock_segment_size(_handler)

    await cdp_client.get_segment_size(counter_id=60315934, segment_id=22)

    assert sent_request.scheme == "http"
    assert sent_request.host == "cdp.test"
    assert sent_request.path == "/cdp/internal/v1/counter/60315934/segment/22/size"
    assert not sent_request.has_body


async def test_returns_segment_size(cdp_client, mock_segment_size):
    mock_segment_size(json_response(status=200, data={"segment_size": 15}))

    result = await cdp_client.get_segment_size(counter_id=60315934, segment_id=22)

    assert result == 15


async def test_raises_for_403(cdp_client, mock_segment_size):
    mock_segment_size(Response(status=403))

    with pytest.raises(ApiAccessDenied):
        await cdp_client.get_segment_size(counter_id=60315934, segment_id=22)


async def test_raises_for_unknown_response(cdp_client, mock_segment_size):
    mock_segment_size(Response(status=450))

    with pytest.raises(UnknownResponse):
        await cdp_client.get_segment_size(counter_id=60315934, segment_id=22)
