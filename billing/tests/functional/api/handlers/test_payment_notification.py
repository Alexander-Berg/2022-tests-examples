import json
import uuid

import jwt.api_jws
import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.enums import CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.psp_key import PSPKey
from billing.yandex_pay.yandex_pay.interactions import UnifiedAgentMetricPushClient


@pytest.fixture
def psp_external_id(rands):
    return rands()


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


@pytest.fixture(autouse=True)
def mock_ua_push_client(mocker):
    mock = mocker.AsyncMock(return_value=None)
    return mocker.patch.object(UnifiedAgentMetricPushClient, 'send', mock)


@pytest.fixture
def get_detached_token(private_key, psp_external_id):
    def detached_token(body):
        params = {
            'payload': f'POST&/api/psp/v1/payment_notification&&{body}'.encode('utf-8'),
            'headers': {'iat': int(utcnow().timestamp()), 'kid': f'1-{psp_external_id}'},
            'key': private_key,
            'algorithm': 'ES256',
        }
        token = jwt.api_jws.encode(**params)
        token = token.split('.')
        return f'{token[0]}..{token[2]}'
    return detached_token


@pytest.mark.asyncio
@pytest.mark.parametrize('card_network', [CardNetwork.MASTERCARD.value, None])
async def test_payment_notification_call(
    storage, app, public_key, get_detached_token, psp_external_id, card_network
):
    psp = await create_psp_entity(
        storage,
        PSP(psp_id=uuid.uuid4(), psp_external_id=psp_external_id, public_key='', public_key_signature='')
    )
    await storage.psp_key.create(PSPKey(psp_id=psp.psp_id, psp_key_id=1, key=public_key, alg='ES256'))
    body = json.dumps({
        'messageId': 'themsgid',
        'status': 'SUCCESS',
        'eventTime': '2020-01-01T00:00:00+00:00',
        'rrn': 'rrn',
        'approvalCode': 'approvalCode',
        'eci': 'eci',
        'cardNetwork': card_network,
        'amount': 100,
        'currency': 'RUB',
    })
    token = get_detached_token(body=body)

    r = await app.post(
        'api/psp/v1/payment_notification',
        data=body,
        headers={'Authorization': f'beaRER {token}', 'Content-Type': 'application/json'}
    )
    json_body = await r.json()

    assert_that(
        json_body,
        equal_to({
            'code': 200,
            'status': 'success',
            'data': {}
        })
    )
    assert_that(r.status, equal_to(200))
