import re

import pytest
from aiohttp.web import Response, json_response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.clients.cdp import ApiAccessDenied, BadFilteringParams

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def api_response():
    return {
        "segment": {
            "counter_id": 60315934,
            "segment_id": 3542,
            "version": 1,
            "name": "TEST",
            "filter": "cdp:cn:multiAttrStr_segments=='two'",
            "segment_type": "REALTIME",
        }
    }


@pytest.fixture(autouse=True)
def mock_list_attributes(aresponses):
    aresponses.add(
        "cdp.test",
        re.compile(r"/cdp/internal/v1/counter/(\d+)/schema/attributes"),
        "GET",
        json_response(status=200, data=_attributes_data),
    )


@pytest.fixture(autouse=True)
def mock_segment_size(aresponses):
    aresponses.add(
        "cdp.test",
        re.compile(r"/cdp/internal/v1/counter/(\d+)/segment/(\d+)/size"),
        "GET",
        json_response(status=200, data={"segment_size": 15}),
    )


@pytest.fixture
def mock_create_segment(aresponses):
    def _mocker(resp):
        aresponses.add(
            "cdp.test",
            re.compile(r"/cdp/internal/v1/counter/(\d+)/segment"),
            "POST",
            resp,
        )

    return _mocker


@pytest.mark.parametrize(
    ("filtering_params", "expected_filter"),
    [
        ({"int_param": 11}, "cdp:cn:attrNum_int_param==11"),
        ({"str_param": "value"}, "cdp:cn:attrStr_str_param=='value'"),
        ({"multiint_param": 11}, "cdp:cn:multiAttrNum_multiint_param==11"),
        ({"multistr_param": "value"}, "cdp:cn:multiAttrStr_multistr_param=='value'"),
        (
            {"int_param": 11, "str_param": "value"},
            "cdp:cn:attrNum_int_param==11 AND cdp:cn:attrStr_str_param=='value'",
        ),
    ],
)
async def test_sends_correct_request(
    cdp_client, mock_create_segment, api_response, filtering_params, expected_filter
):
    sent_request = None

    async def _handler(request):
        nonlocal sent_request
        await request.read()
        sent_request = request
        return json_response(status=200, data=api_response)

    mock_create_segment(_handler)

    await cdp_client.create_segment(
        counter_id=60315934, segment_name="some_name", filtering_params=filtering_params
    )

    assert sent_request.scheme == "http"
    assert sent_request.host == "cdp.test"
    assert sent_request.path == "/cdp/internal/v1/counter/60315934/segment"

    assert await sent_request.json() == {
        "segment": {
            "filter": expected_filter,
            "name": "some_name",
        }
    }
    assert sent_request.content_type == "application/x-yametrika+json"


@pytest.mark.parametrize(
    "filtering_params",
    [
        {},
        {"int_param": object()},
        {"dontknow": "11"},
        {"int_param": 11, "dontknow": 12},
        {"int_param": 11, "str_param": object()},
    ],
)
async def test_raises_for_bad_filtering_params(
    cdp_client, mock_create_segment, api_response, filtering_params
):
    mock_create_segment(json_response(status=200, data=api_response))

    with pytest.raises(BadFilteringParams):
        await cdp_client.create_segment(
            counter_id=60315934,
            segment_name="some_name",
            filtering_params=filtering_params,
        )


async def test_returns_segment_data(cdp_client, mock_create_segment, api_response):
    mock_create_segment(json_response(status=200, data=api_response))

    result = await cdp_client.create_segment(
        counter_id=60315934,
        segment_name="some_name",
        filtering_params={"int_param": 11},
    )

    expected_result = api_response["segment"].copy()
    expected_result["size"] = 15
    assert result == expected_result


async def test_raises_for_403(cdp_client, mock_create_segment):
    mock_create_segment(Response(status=403))

    with pytest.raises(ApiAccessDenied):
        await cdp_client.create_segment(
            counter_id=60315934,
            segment_name="some_name",
            filtering_params={"int_param": 11},
        )


async def test_raises_for_unknown_response(cdp_client, mock_create_segment):
    mock_create_segment(Response(status=450))

    with pytest.raises(UnknownResponse):
        await cdp_client.create_segment(
            counter_id=60315934,
            segment_name="some_name",
            filtering_params={"int_param": 11},
        )


_attributes_data = {
    "system_attributes": [
        {
            "name": "uniq_id",
            "type_name": "TEXT",
            "type_group": "PREDEFINED",
            "type_humanized": "Текстовый",
            "type_group_name": "PREDEFINED",
            "multivalued": False,
        }
    ],
    "custom_attributes": [
        {
            "name": "int_param",
            "type_name": "NUMERIC",
            "type_group": "PREDEFINED",
            "type_humanized": "Числовой",
            "type_group_name": "PREDEFINED",
            "multivalued": False,
            "humanized": "",
        },
        {
            "name": "str_param",
            "type_name": "TEXT",
            "type_group": "PREDEFINED",
            "type_humanized": "Текстовый",
            "type_group_name": "PREDEFINED",
            "multivalued": False,
            "humanized": "",
        },
        {
            "name": "multiint_param",
            "type_name": "NUMERIC",
            "type_group": "PREDEFINED",
            "type_humanized": "Числовой",
            "type_group_name": "PREDEFINED",
            "multivalued": True,
            "humanized": "",
        },
        {
            "name": "multistr_param",
            "type_name": "TEXT",
            "type_group": "PREDEFINED",
            "type_humanized": "Текстовый",
            "type_group_name": "PREDEFINED",
            "multivalued": True,
            "humanized": "",
        },
    ],
}
