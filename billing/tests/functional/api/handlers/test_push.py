import base64
import json
import time

import jwt
import pytest
from aiohttp.client_exceptions import ClientConnectionError

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.wallet.decrypt_app_id import DecryptAppIdAction


@pytest.fixture
def uri():
    return '/wallet/thales/wps/1/0/SendPushNotification'


@pytest.fixture
def push_token():
    return 'some_push_token'


@pytest.fixture
def payload():
    return {
        'data': 'some_data'
    }


@pytest.fixture
def base64_payload(payload):
    return base64.b64encode(json.dumps(payload).encode('utf-8')).decode('utf-8')


@pytest.fixture
async def encrypted_push_token(
    app,
    push_token,
):
    r = await app.post(
        '/api/mobile/v1/wallet/app/encrypted_id',
        json={
            'raw_app_id': push_token
        },
    )
    json_body = await r.json()
    return json_body['data']['encrypted_app_id']


@pytest.fixture
def push_notification_body(encrypted_push_token, base64_payload) -> dict:
    return {
        'walletProviderId': 'Yandex',
        'pushToken': encrypted_push_token,
        'payload': base64_payload,
        'deviceInformation': {
            'platform': 'ANDROID'
        },
    }


@pytest.fixture
def rsa_private_key() -> bytes:
    return b'''-----BEGIN RSA PRIVATE KEY-----
MIIBOAIBAAJAaJEH3caaTIJX0HSSUuS2tCI7vhVK3PuzDXQq4OvVo/AIYAGiTzBt
UPSKzv490cJNHvMH5NtMWAgsOS4nEGtp3QIDAQABAkAC2yM53XjNain32Zc9iF2t
Ido74N0AmCdV0LxzFD3rklO4oee8OdIJI2x/fDT1S4HWdHg4eP+4d3PlesLX0rxB
AiEAwSY09uUGaeBlNW2kela9NLpPGYyrQAj/G5S5B3P3/oUCIQCKl6wCWaNBFfbt
/f8HDkK3buTDlzoIGDw2jTHcBg65eQIgHCtMUi24r7xQRmiFMmpwEb6SRrGjUCV/
vzADhDi/lFkCIDArSxN+CCnE8pABKbRQFowetM/uLbNGiRcEuAGa8V8xAiBSu0Mo
Si5v+T7McyM9QENE8FrTor5rEXSF+HcZHlO0AQ==
-----END RSA PRIVATE KEY-----'''


@pytest.fixture
def rsa_public_key() -> bytes:
    return b'''-----BEGIN PUBLIC KEY-----
MFswDQYJKoZIhvcNAQEBBQADSgAwRwJAaJEH3caaTIJX0HSSUuS2tCI7vhVK3Puz
DXQq4OvVo/AIYAGiTzBtUPSKzv490cJNHvMH5NtMWAgsOS4nEGtp3QIDAQAB
-----END PUBLIC KEY-----'''


@pytest.fixture
def another_rsa_public_key() -> bytes:
    return b'''-----BEGIN PUBLIC KEY-----
MFswDQYJKoZIhvcNAQEBBQADSgAwRwJAaQJ/XfGbPi3DbuVTeHyTBtPO99XApakU
fnftMmqZy5r9kkReCR9UT31CY7yOdjrl2O76RgiM/GalaLtiQGTbHQIDAQAB
-----END PUBLIC KEY-----'''


@pytest.fixture
def apply_rsa_public_key_to_settings(yandex_pay_settings, rsa_public_key):
    yandex_pay_settings.API_THALES_RSA_PUBLIC_KEY = rsa_public_key


@pytest.fixture
def valid_jwt_access_token(rsa_private_key) -> str:
    valid_payload = {
        'exp': time.time() + 60
    }
    return jwt.encode(valid_payload, rsa_private_key, algorithm='RS256')


@pytest.fixture
def expired_jwt_access_token(rsa_private_key) -> str:
    expired_payload = {
        'exp': time.time() - 60
    }
    return jwt.encode(expired_payload, rsa_private_key, algorithm='RS256')


@pytest.mark.asyncio
@pytest.mark.usefixtures('apply_rsa_public_key_to_settings')
async def test_should_not_authorize_if_token_expired(
    app,
    uri,
    push_notification_body,
    expired_jwt_access_token,
):
    r = await app.post(
        uri,
        headers={
            'Authorization': f'Bearer {expired_jwt_access_token}'
        },
        json=push_notification_body,
    )

    assert r.status == 401
    assert r.headers['WWW-Authenticate'] == 'Bearer realm="Access to the Wallet Push API", charset="UTF-8"'


@pytest.mark.asyncio
async def test_should_not_authorize_if_no_header_passed(
    app,
    uri,
    push_notification_body,
):
    r = await app.post(
        uri,
        json=push_notification_body,
    )

    assert r.status == 401


@pytest.mark.asyncio
async def test_should_not_authorize_if_signature_not_matched(
    app,
    uri,
    push_notification_body,
    another_rsa_public_key,
    yandex_pay_settings,
    valid_jwt_access_token,
):
    yandex_pay_settings.API_THALES_RSA_PUBLIC_KEY = another_rsa_public_key
    r = await app.post(
        uri,
        headers={
            'Authorization': f'Bearer {valid_jwt_access_token}'
        },
        json=push_notification_body,
    )

    assert r.status == 401


@pytest.mark.asyncio
async def test_should_support_key_rotation(
    app,
    uri,
    push_notification_body,
    rsa_public_key,
    another_rsa_public_key,
    yandex_pay_settings,
    aioresponses_mocker,
    valid_jwt_access_token,
):
    yandex_pay_settings.API_THALES_RSA_PUBLIC_KEY = another_rsa_public_key
    yandex_pay_settings.API_THALES_RSA_PUBLIC_KEY_NEW = rsa_public_key
    aioresponses_mocker.post(
        f'{yandex_pay_settings.SUP_URL}/pushes?dry_run=0',
        status=200,
        payload={
            'id': '123',
            'receiver': [],
            'data': {},
            'request_time': 123,
        },
    )
    r = await app.post(
        uri,
        headers={
            'Authorization': f'Bearer {valid_jwt_access_token}'
        },
        json=push_notification_body,
    )

    assert r.status == 200


@pytest.mark.asyncio
@pytest.mark.usefixtures('apply_rsa_public_key_to_settings')
async def test_successful_push_notification(
    app,
    uri,
    push_notification_body,
    valid_jwt_access_token,
    yandex_pay_settings,
    aioresponses_mocker

):
    aioresponses_mocker.post(
        f'{yandex_pay_settings.SUP_URL}/pushes?dry_run=0',
        status=200,
        payload={
            'id': '123',
            'receiver': [],
            'data': {},
            'request_time': 123,
        },
    )
    r = await app.post(
        uri,
        headers={
            'Authorization': f'Bearer {valid_jwt_access_token}'
        },
        json=push_notification_body,
    )
    json_body = await r.json()

    assert r.status == 200
    assert json_body == {
        'status': 'SUCCESS'
    }


@pytest.mark.asyncio
async def test_send_thales_push_not_authorize(
    app,
    valid_jwt_access_token,
    uri,
):
    r = await app.post(
        uri,
        json={},
        headers={
            'Authorization': f'Bearer {valid_jwt_access_token}'
        },
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(401))
    assert_that(
        json_body,
        equal_to({
            'status': 'FAILED',
            'status_reason': 102,
            'status_message': 'Authorization failed'
        })
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('apply_rsa_public_key_to_settings')
async def test_send_thales_push_bad_request(
    app,
    valid_jwt_access_token,
    uri,
):
    r = await app.post(
        uri,
        json={},
        headers={
            'Authorization': f'Bearer {valid_jwt_access_token}'
        },
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json_body,
        equal_to({
            'status': 'FAILED',
            'status_reason': 102,
            'status_message': 'Can not parse request data'
        })
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('apply_rsa_public_key_to_settings')
async def test_send_thales_push_bad_push_token(
    app,
    valid_jwt_access_token,
    uri,
):
    r = await app.post(
        uri,
        json={
            'payload': 'xxx',
            'deviceInformation': {
                'platform': 'ANDROID'
            },
            'walletProviderId': 'zzz',
            'pushToken': 'encrypted',
        },
        headers={
            'Authorization': f'Bearer {valid_jwt_access_token}'
        },
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json_body,
        equal_to({
            'status': 'FAILED',
            'status_reason': 100,
            'status_message': 'Can not decrypt push token'
        })
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('apply_rsa_public_key_to_settings')
async def test_send_thales_push_sup_connection_error(
    app,
    mock_action,
    aioresponses_mocker,
    yandex_pay_settings,
    valid_jwt_access_token,
    uri,
):
    mock_action(DecryptAppIdAction, 'decrypted')
    aioresponses_mocker.post(
        f'{yandex_pay_settings.SUP_URL}/pushes?dry_run=0',
        exception=ClientConnectionError,
    )
    r = await app.post(
        uri,
        json={
            'payload': 'xxx',
            'deviceInformation': {
                'platform': 'ANDROID'
            },
            'walletProviderId': 'zzz',
            'pushToken': '1:encrypted',
        },
        headers={
            'Authorization': f'Bearer {valid_jwt_access_token}'
        },
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(500))
    assert_that(
        json_body,
        equal_to({
            'status': 'FAILED',
            'status_reason': 102,
            'status_message': 'Can not send payload to device'
        })
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('apply_rsa_public_key_to_settings')
async def test_send_thales_push_sup_interaction_error(
    app,
    mock_action,
    aioresponses_mocker,
    yandex_pay_settings,
    valid_jwt_access_token,
    uri,
):
    mock_action(DecryptAppIdAction, 'decrypted')
    aioresponses_mocker.post(
        f'{yandex_pay_settings.SUP_URL}/pushes?dry_run=0',
        status=500,
    )
    r = await app.post(
        uri,
        json={
            'payload': 'xxx',
            'deviceInformation': {
                'platform': 'ANDROID'
            },
            'walletProviderId': 'zzz',
            'pushToken': '1:encrypted',
        },
        headers={
            'Authorization': f'Bearer {valid_jwt_access_token}'
        },
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(500))
    assert_that(
        json_body,
        equal_to({
            'status': 'FAILED',
            'status_reason': 102,
            'status_message': 'Can not send payload to device'
        })
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('apply_rsa_public_key_to_settings')
async def test_send_thales_push_sup_interaction_error_common(
    app,
    mock_action,
    aioresponses_mocker,
    yandex_pay_settings,
    valid_jwt_access_token,
    uri,
):
    mock_action(DecryptAppIdAction, 'decrypted')
    aioresponses_mocker.post(
        f'{yandex_pay_settings.SUP_URL}/pushes?dry_run=0',
        exception=Exception,
    )
    r = await app.post(
        uri,
        json={
            'payload': 'xxx',
            'deviceInformation': {
                'platform': 'ANDROID'
            },
            'walletProviderId': 'zzz',
            'pushToken': '1:encrypted',
        },
        headers={
            'Authorization': f'Bearer {valid_jwt_access_token}'
        },
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(500))
    assert_that(
        json_body,
        equal_to({
            'status': 'FAILED',
            'status_reason': 102,
            'status_message': 'Can not send payload to device'
        })
    )
