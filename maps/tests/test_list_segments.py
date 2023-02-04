import functools
import re

import pytest
from aiohttp.web import Response, json_response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.clients.cdp import ApiAccessDenied

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_segments_list(aresponses):
    def _mocker(resps):
        if not isinstance(resps, (list, tuple)):
            resps = [resps]

        for resp in resps:
            aresponses.add(
                "cdp.test",
                re.compile(r"/cdp/internal/v1/counter/(\d+)/segments"),
                "GET",
                resp,
            )

    return _mocker


async def test_sends_correct_request(cdp_client, mock_segments_list):
    sent_request = None

    async def _handler(request):
        nonlocal sent_request
        sent_request = request
        return json_response(data={"segments": []})

    mock_segments_list(_handler)

    await cdp_client.list_segments(counter_id=60315934)

    assert sent_request.scheme == "http"
    assert sent_request.host == "cdp.test"
    assert sent_request.path == "/cdp/internal/v1/counter/60315934/segments"
    assert not sent_request.has_body


async def test_returns_segments_list(cdp_client, mock_segments_list):
    mock_segments_list(
        [
            json_response(
                data={
                    "segments": [
                        {
                            "counter_id": 60315934,
                            "segment_id": 75,
                            "version": 1,
                            "name": "GEOSMB_ACTIVE",
                            "filter": "cdp:cn:multiAttrStr_segments=='ACTIVE'",
                            "segment_type": "REALTIME",
                        },
                        {
                            "counter_id": 60315934,
                            "segment_id": 77,
                            "version": 1,
                            "name": "GEOSMB_DISLOYAL",
                            "filter": "cdp:cn:multiAttrStr_segments=='DISLOYAL'",
                            "segment_type": "REALTIME",
                        },
                    ]
                }
            ),
            json_response(data={"segments": []}),
        ]
    )

    result = await cdp_client.list_segments(counter_id=60315934)

    assert result == [
        {
            "counter_id": 60315934,
            "segment_id": 75,
            "version": 1,
            "name": "GEOSMB_ACTIVE",
            "filter": "cdp:cn:multiAttrStr_segments=='ACTIVE'",
            "segment_type": "REALTIME",
        },
        {
            "counter_id": 60315934,
            "segment_id": 77,
            "version": 1,
            "name": "GEOSMB_DISLOYAL",
            "filter": "cdp:cn:multiAttrStr_segments=='DISLOYAL'",
            "segment_type": "REALTIME",
        },
    ]


async def test_consumes_all_segments_pages(cdp_client, mock_segments_list):
    request_queries = []
    response_bodies = [
        {
            "segments": [
                {
                    "counter_id": 60315934,
                    "segment_id": 75,
                    "version": 1,
                    "name": "GEOSMB_ACTIVE",
                    "filter": "cdp:cn:multiAttrStr_segments=='ACTIVE'",
                    "segment_type": "REALTIME",
                },
                {
                    "counter_id": 60315934,
                    "segment_id": 77,
                    "version": 1,
                    "name": "GEOSMB_DISLOYAL",
                    "filter": "cdp:cn:multiAttrStr_segments=='DISLOYAL'",
                    "segment_type": "REALTIME",
                },
            ]
        },
        {
            "segments": [
                {
                    "counter_id": 60315934,
                    "segment_id": 85,
                    "version": 1,
                    "name": "GEOSMB_LOYAL",
                    "filter": "cdp:cn:multiAttrStr_segments=='LOYAL'",
                    "segment_type": "REALTIME",
                },
            ]
        },
        {"segments": []},
    ]

    async def _handler(request, response_body):
        nonlocal request_queries
        request_queries.append(request.query)
        return json_response(data=response_body)

    for body in response_bodies:
        mock_segments_list(functools.partial(_handler, response_body=body))

    result = await cdp_client.list_segments(counter_id=60315934)

    assert request_queries == [
        {"limit": "100", "from_segment_id": "0"},
        {"limit": "100", "from_segment_id": "77"},
        {"limit": "100", "from_segment_id": "85"},
    ]
    assert result == [
        {
            "counter_id": 60315934,
            "segment_id": 75,
            "version": 1,
            "name": "GEOSMB_ACTIVE",
            "filter": "cdp:cn:multiAttrStr_segments=='ACTIVE'",
            "segment_type": "REALTIME",
        },
        {
            "counter_id": 60315934,
            "segment_id": 77,
            "version": 1,
            "name": "GEOSMB_DISLOYAL",
            "filter": "cdp:cn:multiAttrStr_segments=='DISLOYAL'",
            "segment_type": "REALTIME",
        },
        {
            "counter_id": 60315934,
            "segment_id": 85,
            "version": 1,
            "name": "GEOSMB_LOYAL",
            "filter": "cdp:cn:multiAttrStr_segments=='LOYAL'",
            "segment_type": "REALTIME",
        },
    ]


async def test_raises_for_403(cdp_client, mock_segments_list):
    mock_segments_list(Response(status=403))

    with pytest.raises(ApiAccessDenied):
        await cdp_client.list_segments(counter_id=60315934)


async def test_raises_for_unknown_response(cdp_client, mock_segments_list):
    mock_segments_list(Response(status=450))

    with pytest.raises(UnknownResponse):
        await cdp_client.list_segments(counter_id=60315934)
