import re

import pytest
from aiohttp.web import Response, json_response
from smb.common.http_client import UnknownResponse

from maps_adv.geosmb.clients.cdp import (
    ApiAccessDenied,
    BadContacts,
    ContactsValidationFailed,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def api_response():
    return {
        "uploading": {
            "uploading_id": "9556c5f6-187f-424f-9958-74b6ab7531ff",
            "datetime": "2021-03-11 16:56:02",
            "api_validation_status": "PASSED",
            "elements_count": 2,
            "entity_type": "SYSTEM",
            "entity_subtype": "contact",
            "uploading_format": "JSON",
            "uploading_source": "API",
            "uploading_title": "загрузка контактов",
        }
    }


@pytest.fixture
def mock_upload_contacts(aresponses):
    def _mocker(resp):
        aresponses.add(
            "cdp.test",
            re.compile(r"/cdp/internal/v1/counter/(\d+)/data/contacts"),
            "POST",
            resp,
        )

    return _mocker


async def test_sends_correct_request(cdp_client, mock_upload_contacts, api_response):
    sent_request = None

    async def _handler(request):
        nonlocal sent_request
        await request.read()
        sent_request = request
        return json_response(status=200, data=api_response)

    mock_upload_contacts(_handler)

    await cdp_client.upload_contacts(
        counter_id=60315934,
        biz_id=123,
        contacts=[
            {
                "id": "smth1",
                "phone": "322223",
                "email": "email1@ya.ru",
                "segments": ["one", "two"],
                "labels": ["11", "12"],
                "client_ids": [1, 2, 3],
            },
            {
                "id": "smth2",
                "phone": "444555",
                "email": "email2@ya.ru",
                "segments": ["two", "three"],
                "labels": ["12", "13"],
                "client_ids": [4, 5, 6],
            },
        ],
    )

    assert sent_request.scheme == "http"
    assert sent_request.host == "cdp.test"
    assert sent_request.path == "/cdp/internal/v1/counter/60315934/data/contacts"
    assert sent_request.query == {"merge_mode": "SAVE"}
    assert await sent_request.json() == {
        "contacts": [
            {
                "uniq_id": "smth1",
                "phones": ["322223"],
                "emails": ["email1@ya.ru"],
                "client_ids": [1, 2, 3],
                "attribute_values": {
                    "biz_id": 123,
                    "segments": ["one", "two"],
                    "labels": ["11", "12"],
                },
            },
            {
                "uniq_id": "smth2",
                "phones": ["444555"],
                "emails": ["email2@ya.ru"],
                "client_ids": [4, 5, 6],
                "attribute_values": {
                    "biz_id": 123,
                    "segments": ["two", "three"],
                    "labels": ["12", "13"],
                },
            },
        ]
    }
    assert sent_request.content_type == "application/json"


async def test_optional_fields(cdp_client, mock_upload_contacts, api_response):
    sent_request = None

    async def _handler(request):
        nonlocal sent_request
        await request.read()
        sent_request = request
        return json_response(status=200, data=api_response)

    mock_upload_contacts(_handler)

    await cdp_client.upload_contacts(
        counter_id=60315934,
        biz_id=123,
        contacts=[
            {
                "id": "smth1",
                "phone": "322223",
                "segments": ["one", "two"],
                "labels": ["11", "12"],
            },
            {
                "id": "smth2",
                "email": "email1@ya.ru",
                "segments": ["one", "two"],
                "labels": ["11", "12"],
            },
            {
                "id": "smth3",
                "phone": "222333",
                "email": None,
                "segments": ["one", "two"],
                "labels": ["11", "12"],
            },
            {
                "id": "smth4",
                "email": "email2@ya.ru",
                "phone": None,
                "segments": ["one", "two"],
                "labels": ["11", "12"],
            },
            {
                "id": "smth5",
                "segments": ["one", "two"],
                "client_ids": [4, 5, 6],
                "labels": ["11", "12"],
            },
            {"id": "smth6", "client_ids": [4, 5, 6]},
        ],
    )

    assert await sent_request.json() == {
        "contacts": [
            {
                "uniq_id": "smth1",
                "phones": ["322223"],
                "emails": [],
                "client_ids": [],
                "attribute_values": {
                    "biz_id": 123,
                    "segments": ["one", "two"],
                    "labels": ["11", "12"],
                },
            },
            {
                "uniq_id": "smth2",
                "phones": [],
                "emails": ["email1@ya.ru"],
                "client_ids": [],
                "attribute_values": {
                    "biz_id": 123,
                    "segments": ["one", "two"],
                    "labels": ["11", "12"],
                },
            },
            {
                "uniq_id": "smth3",
                "phones": ["222333"],
                "emails": [],
                "client_ids": [],
                "attribute_values": {
                    "biz_id": 123,
                    "segments": ["one", "two"],
                    "labels": ["11", "12"],
                },
            },
            {
                "uniq_id": "smth4",
                "phones": [],
                "emails": ["email2@ya.ru"],
                "client_ids": [],
                "attribute_values": {
                    "biz_id": 123,
                    "segments": ["one", "two"],
                    "labels": ["11", "12"],
                },
            },
            {
                "uniq_id": "smth5",
                "phones": [],
                "emails": [],
                "client_ids": [4, 5, 6],
                "attribute_values": {
                    "biz_id": 123,
                    "segments": ["one", "two"],
                    "labels": ["11", "12"],
                },
            },
            {
                "uniq_id": "smth6",
                "phones": [],
                "emails": [],
                "client_ids": [4, 5, 6],
                "attribute_values": {
                    "biz_id": 123,
                    "segments": [],
                    "labels": [],
                },
            },
        ]
    }


async def test_uploads_data_by_chunks(cdp_client, mock_upload_contacts, api_response):
    sent_requests = []

    async def _handler(request):
        nonlocal sent_requests
        await request.read()
        sent_requests.append(request)
        return json_response(status=200, data=api_response)

    for _ in range(2):
        mock_upload_contacts(_handler)

    contacts = [
        {"id": f"smth{i}", "phone": f"322223-{i}", "segments": ["one", "two"]}
        for i in range(1500)
    ]

    await cdp_client.upload_contacts(counter_id=60315934, biz_id=123, contacts=contacts)

    assert len(sent_requests) == 2
    request1_payload = await sent_requests[0].json()
    assert len(request1_payload["contacts"]) == 1000
    assert request1_payload["contacts"][0]["uniq_id"] == "smth0"
    assert request1_payload["contacts"][-1]["uniq_id"] == "smth999"
    request2_payload = await sent_requests[1].json()
    assert len(request2_payload["contacts"]) == 500
    assert request2_payload["contacts"][0]["uniq_id"] == "smth1000"
    assert request2_payload["contacts"][-1]["uniq_id"] == "smth1499"


async def test_raises_for_clients_with_no_id_field(
    cdp_client, mock_upload_contacts, api_response
):
    mock_upload_contacts(json_response(status=200, data=api_response))

    with pytest.raises(BadContacts):
        await cdp_client.upload_contacts(
            counter_id=60315934,
            biz_id=123,
            contacts=[
                {"id": "smth1", "phone": "322223", "segments": ["one", "two"]},
                {"phone": "444555", "segments": ["one", "two"]},
                {"id": "smth3", "email": "emial1@ya.ru", "segments": ["one", "two"]},
            ],
        )


async def test_raises_for_clients_with_no_required_fields(
    cdp_client, mock_upload_contacts, api_response
):
    mock_upload_contacts(json_response(status=200, data=api_response))

    with pytest.raises(BadContacts):
        await cdp_client.upload_contacts(
            counter_id=60315934,
            biz_id=123,
            contacts=[
                {"id": "smth1", "phone": "322223", "segments": ["one", "two"]},
                {"id": "smth2", "segments": ["one", "two"]},
                {"id": "smth3", "email": "emial1@ya.ru", "segments": ["one", "two"]},
            ],
        )


async def test_raises_if_validation_failed(
    cdp_client, mock_upload_contacts, api_response
):
    api_response["uploading"]["api_validation_status"] = "FAILED"
    mock_upload_contacts(json_response(status=200, data=api_response))

    with pytest.raises(ContactsValidationFailed):
        await cdp_client.upload_contacts(
            counter_id=60315934,
            biz_id=123,
            contacts=[
                {"id": "smth1", "phone": "322223", "segments": ["one", "two"]},
                {"id": "smth3", "email": "emial1@ya.ru", "segments": ["one", "two"]},
            ],
        )


async def test_returns_none(cdp_client, mock_upload_contacts, api_response):
    mock_upload_contacts(json_response(status=200, data=api_response))

    result = await cdp_client.upload_contacts(
        counter_id=60315934,
        biz_id=123,
        contacts=[
            {"id": "smth1", "phone": "322223", "segments": ["one", "two"]},
            {"id": "smth3", "email": "emial1@ya.ru", "segments": ["one", "two"]},
        ],
    )

    assert result is None


async def test_raises_for_403(cdp_client, mock_upload_contacts):
    mock_upload_contacts(Response(status=403))

    with pytest.raises(ApiAccessDenied):
        await cdp_client.upload_contacts(
            counter_id=60315934,
            biz_id=123,
            contacts=[
                {"id": "smth1", "phone": "322223", "segments": ["one", "two"]},
                {"id": "smth3", "email": "emial1@ya.ru", "segments": ["one", "two"]},
            ],
        )


async def test_raises_for_unknown_response(cdp_client, mock_upload_contacts):
    mock_upload_contacts(Response(status=450))

    with pytest.raises(UnknownResponse):
        await cdp_client.upload_contacts(
            counter_id=60315934,
            biz_id=123,
            contacts=[
                {
                    "id": "smth1",
                    "phone": "322223",
                    "email": "email1@ya.ru",
                    "segments": ["one", "two"],
                    "client_ids": [1, 2, 3],
                },
                {
                    "id": "smth2",
                    "phone": "444555",
                    "email": "email2@ya.ru",
                    "segments": ["two", "three"],
                    "client_ids": [4, 5, 6],
                },
            ],
        )
