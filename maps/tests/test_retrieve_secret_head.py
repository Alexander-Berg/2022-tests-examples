from datetime import datetime, timezone

import pytest
from aiohttp.web import json_response

from maps_adv.common.aioyav import AccessError, InvalidOAuthToken, InvalidUUID

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def mock_versions(rmock):
    return lambda secret_uuid, *a: rmock(f"/1/versions/{secret_uuid}/", "GET", *a)


async def test_sends_correct_request(mock_versions, yav):
    request_path = None
    request_headers = None

    async def _handler(request):
        nonlocal request_path, request_headers
        request_path = request.path
        request_headers = request.headers
        return json_response(data=success_response, status=200)

    mock_versions("sec-01e6zwtfg5g4b727yq4w3qvnz3", _handler)

    await yav.retrieve_secret_head("sec-01e6zwtfg5g4b727yq4w3qvnz3")

    assert request_path == "/1/versions/sec-01e6zwtfg5g4b727yq4w3qvnz3/"
    assert request_headers.get("Authorization") == "OAuth oauth_token"


async def test_returns_secret_details(mock_versions, yav):
    mock_versions(
        "sec-01e6zwtfg5g4b727yq4w3qvnz3",
        json_response(data=success_response, status=200),
    )

    got = await yav.retrieve_secret_head("sec-01e6zwtfg5g4b727yq4w3qvnz3")

    assert got == {
        "comment": "",
        "created_at": datetime(2020, 4, 28, 8, 0, 31, 246000, tzinfo=timezone.utc),
        "created_by": 1120000000098712,
        "creator_login": "sivakov512",
        "secret_name": "aioyav-example",
        "secret_uuid": "sec-01e6zwtfg5g4b727yq4w3qvnz3",
        "value": {"key_one": "some value", "100": "500", "key-two": "another\nvalue"},
        "version": "ver-01e6zwtfgen5w925z9qztr9hcv",
    }


async def test_raises_for_access_error(mock_versions, yav):
    mock_versions(
        "sec-01e6zwtfg5g4b727yq4w3qvnz4",
        json_response(
            data={
                "api_request_id": "1f5f212bd9603059226f0dfeb72407e9",
                "code": "access_error",
                "environment": "production",
                "hostname": "vault-v4.passport.yandex.net",
                "message": "Access denied",
                "status": "error",
            },
            status=401,
        ),
    )

    with pytest.raises(AccessError) as exc_info:
        await yav.retrieve_secret_head("sec-01e6zwtfg5g4b727yq4w3qvnz4")

    assert exc_info.value.args[0] == {
        "api_request_id": "1f5f212bd9603059226f0dfeb72407e9",
        "code": "access_error",
        "environment": "production",
        "hostname": "vault-v4.passport.yandex.net",
        "message": "Access denied",
        "status": "error",
    }


async def test_raises_for_invalid_uuid(mock_versions, yav):
    mock_versions(
        "sec-01e6zwtfg5g4b727yq4w3qvnz",
        json_response(
            data={
                "api_request_id": "f77daa307409ea959de04a9b3ef806e3",
                "code": "invalid_uuid_value",
                "environment": "production",
                "hostname": "vault-v2.passport.yandex.net",
                "message": "u'01e6zwtfg5g4b727yq4w3qvnz' is an invalid UUID value",
                "status": "error",
            },
            status=400,
        ),
    )

    with pytest.raises(InvalidUUID) as exc_info:
        await yav.retrieve_secret_head("sec-01e6zwtfg5g4b727yq4w3qvnz")

    assert exc_info.value.args[0] == {
        "api_request_id": "f77daa307409ea959de04a9b3ef806e3",
        "code": "invalid_uuid_value",
        "environment": "production",
        "hostname": "vault-v2.passport.yandex.net",
        "message": "u'01e6zwtfg5g4b727yq4w3qvnz' is an invalid UUID value",
        "status": "error",
    }


async def test_raises_for_invalid_oauth_token(mock_versions, make_yav):
    yav = await make_yav("wrong_token")
    mock_versions(
        "sec-01e6zwtfg5g4b727yq4w3qvnz3",
        json_response(
            data={
                "api_request_id": "5207955f647d8beaf63ee3180b9fdee8",
                "blackbox_error": "expired_token",
                "code": "invalid_oauth_token_error",
                "environment": "production",
                "hostname": "vault-s5.passport.yandex.net",
                "message": "Invalid oauth token",
                "status": "error",
            },
            status=401,
        ),
    )

    with pytest.raises(InvalidOAuthToken) as exc_info:
        await yav.retrieve_secret_head("sec-01e6zwtfg5g4b727yq4w3qvnz3")

    assert exc_info.value.args[0] == {
        "api_request_id": "5207955f647d8beaf63ee3180b9fdee8",
        "blackbox_error": "expired_token",
        "code": "invalid_oauth_token_error",
        "environment": "production",
        "hostname": "vault-s5.passport.yandex.net",
        "message": "Invalid oauth token",
        "status": "error",
    }


success_response = {
    "status": "ok",
    "version": {
        "comment": "",
        "created_at": 1588060831.246,
        "created_by": 1120000000098712,
        "creator_login": "sivakov512",
        "secret_name": "aioyav-example",
        "secret_uuid": "sec-01e6zwtfg5g4b727yq4w3qvnz3",
        "value": [
            {"key": "key_one", "value": "some value"},
            {"key": "100", "value": "500"},
            {"key": "key-two", "value": "another\nvalue"},
        ],
        "version": "ver-01e6zwtfgen5w925z9qztr9hcv",
    },
}
