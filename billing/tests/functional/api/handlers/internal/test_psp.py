import uuid
from datetime import datetime, timedelta, timezone

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, contains, equal_to, has_entries

from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.psp_key import PSPKey
from billing.yandex_pay.yandex_pay.tests.matchers import close_to_datetime, convert_then_match


@pytest.mark.asyncio
async def test_psp_get(
    internal_app,
    mock_internal_tvm,
    psp,
    psp_key,
):
    r = await internal_app.get(
        f'/api/internal/v1/psp/{str(psp.psp_id)}',
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        json_body,
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'psp': has_entries({
                    'psp_id': str(psp.psp_id),
                    'psp_external_id': psp.psp_external_id,
                    'public_key': psp.public_key,
                    'public_key_signature': psp.public_key_signature,
                    'public_key_updated': convert_then_match(
                        datetime.fromisoformat,
                        equal_to(psp.public_key_updated),
                    ),
                    'created': psp.created.astimezone(tz=timezone.utc).isoformat(),
                    'psp_auth_keys': contains(
                        has_entries({
                            'jws_kid': f'1-{psp.psp_external_id}',
                            'key': psp_key.key,
                            'alg': psp_key.alg,
                            'created': convert_then_match(
                                datetime.fromisoformat,
                                close_to_datetime(utcnow(), timedelta(seconds=10)),
                            ),
                        }),
                    )
                })
            })
        })
    )


@pytest.mark.asyncio
async def test_psp_put(
    aioresponses_mocker,
    yandex_pay_settings,
    internal_app,
    mock_internal_tvm,
    rands,
    public_key,
):
    psp_id = uuid.uuid4()
    psp_external_id = rands()

    aioresponses_mocker.post(
        f'{yandex_pay_settings.DUCKGO_API_URL}/v1/payment_token/verify_recipient_key',
        payload={'code': 200, 'status': 'success', 'data': {}},
    )

    r = await internal_app.put(
        f'/api/internal/v1/psp/{str(psp_id)}',
        headers={'x-ya-service-ticket': 'dummy-service-ticket'},
        json={
            'psp_external_id': psp_external_id,
            'public_key': public_key,
            'public_key_signature': 'xxxyyy',
            'psp_auth_keys': [
                {
                    'key': public_key,
                    'alg': 'ES256',
                }
            ],
        }
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        json_body,
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'psp': has_entries({
                    'psp_id': str(psp_id),
                    'psp_external_id': psp_external_id,
                    'public_key': public_key,
                    'public_key_signature': 'xxxyyy',
                    'created': convert_then_match(
                        lambda dt: datetime.fromisoformat(dt),
                        close_to_datetime(utcnow(), timedelta(seconds=10)),
                    ),
                    'public_key_updated': convert_then_match(
                        lambda dt: datetime.fromisoformat(dt),
                        close_to_datetime(utcnow(), timedelta(seconds=10)),
                    ),
                    'psp_auth_keys': contains(
                        has_entries({
                            'key': public_key,
                            'alg': 'ES256',
                            'created': convert_then_match(
                                lambda dt: datetime.fromisoformat(dt),
                                close_to_datetime(utcnow(), timedelta(seconds=10)),
                            ),
                            'jws_kid': f'1-{psp_external_id}',
                        }),
                    ),
                })
            })
        })
    )


@pytest.fixture
async def psp(internal_app, storage, rands):
    return await create_psp_entity(
        storage,
        PSP(psp_id=uuid.uuid4(), psp_external_id=rands(), public_key='pubkey', public_key_signature='xxxyyy')
    )


@pytest.fixture
async def psp_key(storage, psp):
    return await storage.psp_key.create(
        PSPKey(psp_id=psp.psp_id, psp_key_id=1, key='pubkey', alg='ES256')
    )


@pytest.fixture
def public_key():
    return (
        'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwmTG26LY5/0bc7VwMfJ9kfqfLTlYr/Ue9c0G7jdFp08dWX2osZ22JR/S9PUWKblmyl1CSGiHV0'
        'bAtlbeC3QgjQ=='
    )
