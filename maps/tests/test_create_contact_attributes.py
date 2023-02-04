import re

import pytest
from aiohttp.web import Response, json_response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.clients.cdp import ApiAccessDenied, SchemaValidationFailed

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(cdp_client, aresponses):
    sent_request = None

    async def _handler(request):
        nonlocal sent_request
        await request.read()
        sent_request = request
        return json_response(status=200, data={"success": True})

    aresponses.add(
        "cdp.test",
        re.compile(r"/cdp/internal/v1/counter/(\d+)/schema/attributes"),
        "POST",
        _handler,
    )

    await cdp_client.create_contacts_schema(
        counter_id=60315934,
        attributes=[
            {
                "type_name": "numeric",
                "name": "biz_id",
                "multivalued": False,
                "humanized": "Бузайди",
            },
            {
                "type_name": "text",
                "name": "segments",
                "multivalued": True,
                "humanized": "Сегменты",
            },
        ],
    )

    assert sent_request.scheme == "http"
    assert sent_request.host == "cdp.test"
    assert sent_request.path == "/cdp/internal/v1/counter/60315934/schema/attributes"
    assert sent_request.query == {"entity_type": "contact"}
    assert await sent_request.json() == {
        "attributes": [
            {
                "type_name": "numeric",
                "name": "biz_id",
                "multivalued": False,
                "humanized": "Бузайди",
            },
            {
                "type_name": "text",
                "name": "segments",
                "multivalued": True,
                "humanized": "Сегменты",
            },
        ]
    }
    assert sent_request.content_type == "application/x-yametrika+json"


async def test_appends_optional_keys(cdp_client, aresponses):
    sent_request_json = None

    async def _handler(request):
        nonlocal sent_request_json
        sent_request_json = await request.json()
        return json_response(status=200, data={"success": True})

    aresponses.add(
        "cdp.test",
        re.compile(r"/cdp/internal/v1/counter/(\d+)/schema/attributes"),
        "POST",
        _handler,
    )

    await cdp_client.create_contacts_schema(
        counter_id=60315934,
        attributes=[
            {"type_name": "numeric", "name": "biz_id", "multivalued": True},
            {"type_name": "text", "name": "segments", "humanized": "Сегменты"},
            {"type_name": "text", "name": "labels"},
        ],
    )

    assert sent_request_json == {
        "attributes": [
            {
                "type_name": "numeric",
                "name": "biz_id",
                "multivalued": True,
                "humanized": "",
            },
            {
                "type_name": "text",
                "name": "segments",
                "multivalued": False,
                "humanized": "Сегменты",
            },
            {
                "type_name": "text",
                "name": "labels",
                "multivalued": False,
                "humanized": "",
            },
        ]
    }


async def test_return_none(cdp_client, aresponses):
    aresponses.add(
        "cdp.test",
        re.compile(r"/cdp/internal/v1/counter/(\d+)/schema/attributes"),
        "POST",
        json_response(status=200, data={"success": True}),
    )

    result = await cdp_client.create_contacts_schema(
        counter_id=60315934,
        attributes=[
            {"type_name": "numeric", "name": "biz_id", "multivalued": True},
        ],
    )

    assert result is None


async def test_raises_if_validation_fails(cdp_client, aresponses):
    aresponses.add(
        "cdp.test",
        re.compile(r"/cdp/internal/v1/counter/(\d+)/schema/attributes"),
        "POST",
        json_response(status=200, data={"success": False}),
    )

    with pytest.raises(SchemaValidationFailed):
        await cdp_client.create_contacts_schema(
            counter_id=60315934,
            attributes=[
                {"type_name": "numeric", "name": "biz_id", "multivalued": True},
            ],
        )


async def test_raises_for_403(cdp_client, aresponses):
    aresponses.add(
        "cdp.test",
        re.compile(r"/cdp/internal/v1/counter/(\d+)/schema/attributes"),
        "POST",
        Response(status=403),
    )

    with pytest.raises(ApiAccessDenied):
        await cdp_client.create_contacts_schema(
            counter_id=60315934,
            attributes=[
                {"type_name": "numeric", "name": "biz_id", "multivalued": True},
            ],
        )


async def test_raises_for_unknown_response(cdp_client, aresponses):
    aresponses.add(
        "cdp.test",
        re.compile(r"/cdp/internal/v1/counter/(\d+)/schema/attributes"),
        "POST",
        Response(status=450),
    )

    with pytest.raises(UnknownResponse):
        await cdp_client.create_contacts_schema(
            counter_id=60315934,
            attributes=[
                {"type_name": "numeric", "name": "biz_id", "multivalued": True},
            ],
        )
