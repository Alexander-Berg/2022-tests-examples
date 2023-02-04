import uuid
from datetime import datetime, timezone

import jwt.api_jws
import pytest
import yarl

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.psp.auth import AuthPSPRequestAction
from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.psp_key import PSPKey
from billing.yandex_pay.yandex_pay.core.exceptions import CoreAccessDenyError

BASE_STRING = 'POST&/pa/th&pa=rams&qu=ery&{}'


@pytest.fixture
def public_key():
    return (
        'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwmTG26LY5/0bc7VwMfJ9kfqfLTlYr/Ue9c0G7jdFp08dWX2osZ22JR/S9PUWKblmyl1CSGiHV0'
        'bAtlbeC3QgjQ=='
    )


@pytest.fixture
def private_key():
    return """
-----BEGIN EC PRIVATE KEY-----
MHcCAQEEIHL7AznIhb0OArkGsK7qbLsg8SMIGzsQWDz3SFXCk7WeoAoGCCqGSM49
AwEHoUQDQgAEwmTG26LY5/0bc7VwMfJ9kfqfLTlYr/Ue9c0G7jdFp08dWX2osZ22
JR/S9PUWKblmyl1CSGiHV0bAtlbeC3QgjQ==
-----END EC PRIVATE KEY-----
    """


@pytest.fixture
def get_detached_token(private_key):
    def detached_token(**kwargs):
        params = {
            'payload': bytes(BASE_STRING, 'utf-8'),
            'headers': {'iat': int(utcnow().timestamp()), 'kid': '1-gw'},
            'key': private_key,
            'algorithm': 'ES256',
        }
        params.update(kwargs)
        token = jwt.api_jws.encode(**params)
        token = token.split('.')
        return f'{token[0]}..{token[2]}'
    return detached_token


@pytest.fixture
async def psp(storage):
    return await create_psp_entity(
        storage,
        PSP(psp_id=uuid.uuid4(), psp_external_id='gw', public_key='', public_key_signature='')
    )


@pytest.fixture
async def psp_key(storage, psp, public_key):
    return await storage.psp_key.create(PSPKey(psp_id=psp.psp_id, psp_key_id=1, key=public_key, alg='ES256'))


@pytest.fixture
def params(private_key, get_detached_token):
    token = get_detached_token()
    return {
        'method': 'POST',
        'url': yarl.URL('http://whatever.test/pa/th?qu=ery&pa=rams'),
        'body': b'{}',
        'authorization_header': (
            f'BeArEr {token}'
        )
    }


@pytest.mark.asyncio
async def test_success(storage, params, psp, psp_key):
    returned_psp, returned_psp_key = await AuthPSPRequestAction(**params).run()

    assert_that(returned_psp, equal_to(psp))
    assert_that(returned_psp_key, equal_to(psp_key))


@pytest.mark.asyncio
@pytest.mark.parametrize('auth_header', (None, '', 'SomethingSomehing'))
async def test_auth_header_is_empty(params, auth_header):
    params['authorization_header'] = auth_header

    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(exc_info.value.params['description'], equal_to('Authorization header is malformed'))


@pytest.mark.asyncio
async def test_auth_header_unknown_kind(params):
    params['authorization_header'] = (
        'Something eyJhbGciOiJFUzI1NiIsImlhdCI6MTYxMzU2Mjc5NSwia2lkIjoiMS1ndyJ9..fwuDnUbmwNHyd4wK-YS4pE6kiYtKVYo7ENPUJ'
    )

    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(exc_info.value.params['description'], equal_to('Auth kind "Something" is not supported'))


@pytest.mark.asyncio
async def test_jws_rfc7515_malformed(params):
    params['authorization_header'] = (
        'Bearer eyJhbGciOiJFUzI1NiIsImlhdCI6MTYxMzU2Mjc5NSwia2lkIjoiMS1ndyJ9.fwuDnUbmwNHyd4wK-YS4pE6kiYtKVYo7ENPUJ'
    )

    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(exc_info.value.params['description'], equal_to('JWS is malformed: expected 3 parts, found 2'))


@pytest.mark.asyncio
async def test_kid_malformed(params, private_key, get_detached_token):
    token = get_detached_token(
        headers={'iat': int(utcnow().timestamp()), 'kid': '1gw'},
    )
    params['authorization_header'] = (
        f'Bearer {token}'
    )

    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(exc_info.value.params['description'], equal_to('JWS kid is malformed: "1gw"'))


@pytest.mark.asyncio
async def test_iat_malformed(params, private_key, get_detached_token):
    token = get_detached_token(
        headers={'iat': 'cool-dude', 'kid': '1-gw'},
    )
    params['authorization_header'] = (
        f'Bearer {token}'
    )

    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(exc_info.value.params['description'], equal_to('JWS iat is malformed: "cool-dude"'))


@pytest.mark.asyncio
async def test_token_expired(params, private_key, mocker, yandex_pay_settings, get_detached_token):
    yandex_pay_settings.API_PSP_JWS_LIFETIME_SECONDS = 3600
    mocker.patch(
        'billing.yandex_pay.yandex_pay.core.actions.psp.auth.utcnow',
        mocker.Mock(return_value=datetime.fromtimestamp(1500086400, tz=timezone.utc)),
    )
    iat = 1500000000
    token = get_detached_token(
        headers={'iat': iat, 'kid': '1-gw'},
    )
    params['authorization_header'] = (
        f'Bearer {token}'
    )

    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(
        exc_info.value.params['description'],
        equal_to(
            'JWS is expired assuming lifetime is 3600: iat is "1500000000" '
            'but now is "1500086400" (time difference is 86400)'
        )
    )


@pytest.mark.asyncio
async def test_psp_not_found(params, private_key, get_detached_token):
    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(exc_info.value.params['description'], equal_to('PSP "gw" not found'))


@pytest.mark.asyncio
async def test_key_not_found(storage, params, private_key, get_detached_token, psp):
    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(exc_info.value.params['description'], equal_to('Key "1-gw" not found'))


@pytest.mark.asyncio
async def test_invalid_signature(storage, params, private_key, get_detached_token, psp_key):
    token = get_detached_token()
    token_with_invalid_signature = '.'.join(token.split('.')[:-1]) + '.'
    params['authorization_header'] = f'Bearer {token_with_invalid_signature}'

    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(
        exc_info.value.params['description'],
        equal_to(
            f'Invalid signature, assuming base string is "{BASE_STRING}"',
        )
    )


@pytest.mark.asyncio
async def test_jwt_is_invalid_utf8(storage, params, private_key, get_detached_token, psp_key):
    params['authorization_header'] = 'Bearer 123..123'

    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(
        exc_info.value.params['description'],
        equal_to(
            'JWS decode error',
        )
    )


@pytest.mark.asyncio
async def test_jwt_is_invalid_base64(storage, params, private_key, get_detached_token, psp_key):
    """a=== - невозможный base64, потому что для кодирования Одного октета одного символа недостаточно"""
    params['authorization_header'] = 'Bearer a..a'

    with pytest.raises(CoreAccessDenyError) as exc_info:
        await AuthPSPRequestAction(**params).run()

    assert_that(
        exc_info.value.params['description'],
        equal_to(
            'JWS decode error',
        )
    )
