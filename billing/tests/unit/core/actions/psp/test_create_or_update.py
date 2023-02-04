from datetime import datetime, timezone
from uuid import uuid4

import pytest
from freezegun import freeze_time

from hamcrest import assert_that, contains, contains_inanyorder, equal_to, has_properties

from billing.yandex_pay.yandex_pay.core.actions.psp.create_or_update import CreateOrUpdatePSPAction
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.exceptions import (
    CoreInvalidAuthKeyError, CoreInvalidPublicKeyError, CorePSPExternalIDChangedError
)
from billing.yandex_pay.yandex_pay.interactions import DuckGoClient
from billing.yandex_pay.yandex_pay.interactions.duckgo import InvalidPublicKeyError

PSP_EXTERNAL_ID = 'gwid'
PUBLIC_KEY_SIGNATURE = 'xxxyyy'
OTHER_PUBLIC_KEY_SIGNATURE = 'aaabbb'


@pytest.fixture(autouse=True)
def mock_duckgo_verify_recipient_key(mocker):
    response = {
        'code': 200,
        'status': 'success',
        'data': {},
    }
    return mocker.patch.object(
        DuckGoClient, 'verify_recipient_key', return_value=response
    )


@pytest.mark.asyncio
async def test_create_psp_result(storage, public_key, call_action):
    psp_id = uuid4()
    psp = await call_action(psp_id=psp_id)

    assert_that(
        psp,
        has_properties({
            'psp_id': psp_id,
            'public_key': public_key,
            'public_key_signature': PUBLIC_KEY_SIGNATURE,
            'psp_external_id': 'gwid',
            'psp_auth_keys': contains_inanyorder(
                has_properties({
                    'key': public_key,
                    'alg': 'ES256',
                    'jws_kid': '1-gwid'
                })
            )
        })
    )


@pytest.mark.asyncio
async def test_create_psp__entities_are_created(storage, public_key, call_action):
    psp_id = uuid4()
    await call_action(psp_id=psp_id)

    assert_that(
        await storage.psp.get(psp_id),
        has_properties({
            'psp_id': psp_id,
            'public_key': public_key,
            'public_key_signature': PUBLIC_KEY_SIGNATURE,
            'psp_external_id': PSP_EXTERNAL_ID,
        })
    )
    assert_that(
        await storage.psp_serial.get(psp_id),
        has_properties(psp_id=psp_id)
    )
    assert_that(
        await storage.psp_key.find_by_psp_id(psp_id),
        contains(
            has_properties({
                'key': public_key,
                'alg': 'ES256',
                'deleted': False,
            })
        )
    )


@pytest.mark.asyncio
async def test_duckgo_called_once(
    call_action, public_key, mock_duckgo_verify_recipient_key
):
    await call_action()

    mock_duckgo_verify_recipient_key.assert_called_once_with(
        public_key, PUBLIC_KEY_SIGNATURE
    )


@freeze_time('2022-05-04 03:00:00+00:00')
@pytest.mark.asyncio
async def test_update_psp__entities_are_updated(
    storage,
    public_key,
    other_public_key,
    call_action,
    mock_duckgo_verify_recipient_key,
    mocker,
):
    psp_id = uuid4()
    await call_action(psp_id=psp_id)

    await call_action(
        psp_id=psp_id,
        public_key=other_public_key,
        public_key_signature=OTHER_PUBLIC_KEY_SIGNATURE,
        psp_auth_keys=[{'key': other_public_key, 'alg': 'ES256'}]
    )

    assert_that(
        await storage.psp.get(psp_id),
        has_properties({
            'psp_id': psp_id,
            'public_key': other_public_key,
            'public_key_signature': OTHER_PUBLIC_KEY_SIGNATURE,
            'psp_external_id': PSP_EXTERNAL_ID,
            'public_key_updated': datetime(2022, 5, 4, 3, tzinfo=timezone.utc),
        })
    )
    assert_that(
        await storage.psp_key.find_by_psp_id(psp_id),
        contains(
            has_properties({
                'key': public_key,
                'alg': 'ES256',
                'deleted': True,
            }),
            has_properties({
                'key': other_public_key,
                'alg': 'ES256',
                'deleted': False,
            })
        )
    )
    assert_that(
        mock_duckgo_verify_recipient_key.mock_calls,
        equal_to(
            [
                mocker.call(public_key, PUBLIC_KEY_SIGNATURE),
                mocker.call(other_public_key, OTHER_PUBLIC_KEY_SIGNATURE),
            ]
        )
    )


@pytest.mark.asyncio
async def test_auth_key_delete(storage, public_key, call_action):
    psp_id = uuid4()
    # initial state
    await call_action(psp_id=psp_id, psp_auth_keys=[{'key': public_key, 'alg': 'ES256'}])
    # delete key
    await call_action(psp_id=psp_id, psp_auth_keys=[])

    assert_that(
        await storage.psp_key.find_by_psp_id(psp_id),
        contains(
            has_properties({
                'key': public_key,
                'alg': 'ES256',
                'deleted': True,
            }),
        )
    )


@pytest.mark.asyncio
async def test_auth_key_recreate(storage, public_key, call_action):
    psp_id = uuid4()
    # initial state
    await call_action(psp_id=psp_id, psp_auth_keys=[{'key': public_key, 'alg': 'ES256'}])
    # delete key
    await call_action(psp_id=psp_id, psp_auth_keys=[])
    # create same key again
    await call_action(psp_id=psp_id, psp_auth_keys=[{'key': public_key, 'alg': 'ES256'}])

    assert_that(
        await storage.psp_key.find_by_psp_id(psp_id),
        contains(
            has_properties({
                'key': public_key,
                'alg': 'ES256',
                'deleted': False,
            }),
        )
    )


@pytest.mark.asyncio
async def test_invalid_public_key(storage, call_action, mock_duckgo_verify_recipient_key):
    psp_id = uuid4()
    with pytest.raises(CoreInvalidPublicKeyError):
        await call_action(psp_id=psp_id, public_key='invalid')

    with pytest.raises(PSP.DoesNotExist):
        await storage.psp.get(psp_id)

    mock_duckgo_verify_recipient_key.assert_not_called()


@pytest.mark.asyncio
async def test_unverified_public_key(
    storage, call_action, public_key, mock_duckgo_verify_recipient_key
):
    psp_id = uuid4()
    mock_duckgo_verify_recipient_key.side_effect = InvalidPublicKeyError(
        status_code=400,
        method='POST',
        service=DuckGoClient.SERVICE,
    )
    with pytest.raises(CoreInvalidPublicKeyError):
        await call_action(psp_id=psp_id)

    with pytest.raises(PSP.DoesNotExist):
        await storage.psp.get(psp_id)


@pytest.mark.asyncio
async def test_can_not_change_external_id(
    storage, public_key, other_public_key, call_action
):
    psp_id = uuid4()
    await call_action(psp_id=psp_id, psp_external_id='gwid')

    with pytest.raises(CorePSPExternalIDChangedError):
        await call_action(psp_id=psp_id, psp_external_id='gwid2')


@pytest.mark.asyncio
async def test_invalid_auth_key(storage, call_action, other_public_key):
    with pytest.raises(CoreInvalidAuthKeyError):
        await call_action(
            psp_auth_keys=[{'key': f'bad{other_public_key}', 'alg': 'ES256'}]
        )


@pytest.fixture
def call_action(public_key):
    async def _call_action(**kwargs):
        kwargs.setdefault('psp_id', uuid4())
        kwargs.setdefault('public_key', public_key)
        kwargs.setdefault('psp_external_id', PSP_EXTERNAL_ID)
        kwargs.setdefault('public_key_signature', PUBLIC_KEY_SIGNATURE)
        kwargs.setdefault('psp_auth_keys', [{'key': public_key, 'alg': 'ES256'}])
        return await CreateOrUpdatePSPAction(**kwargs).run()
    return _call_action


@pytest.fixture
def public_key():
    return (
        'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwmTG26LY5/0bc7VwMfJ9kfqfLTlYr/Ue9c0G7jdFp0'
        '8dWX2osZ22JR/S9PUWKblmyl1CSGiHV0bAtlbeC3QgjQ=='
    )


@pytest.fixture
def other_public_key():
    return (
        'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEqjLjAR3fAHsGqL9KITfc3Y1HLob6ZbUXEDq5skFDby'
        '+6aoSSTxipEFyysnOdWuW29d9jbQPpJcZSbvpkLw2PZg=='
    )
