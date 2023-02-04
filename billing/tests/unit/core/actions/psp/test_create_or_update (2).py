from uuid import uuid4

import pytest

from hamcrest import assert_that, contains, contains_inanyorder, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.create_or_update import CreateOrUpdatePSPAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CoreInvalidAuthKeyError,
    CorePSPExternalIDChangedError,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP

PSP_EXTERNAL_ID = 'gwid'


@pytest.mark.asyncio
async def test_create_psp_result(public_key, call_action):
    psp_id = uuid4()
    psp = await call_action(psp_id=psp_id)

    assert_that(
        psp,
        has_properties({
            'psp_id': psp_id,
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
async def test_update_psp__entities_are_updated(
    storage,
    public_key,
    other_public_key,
    call_action,
):
    psp_id = uuid4()
    await call_action(psp_id=psp_id)

    await call_action(
        psp_id=psp_id,
        psp_auth_keys=[{'key': other_public_key, 'alg': 'ES256'}]
    )

    assert_that(
        await storage.psp.get(psp_id),
        has_properties({
            'psp_id': psp_id,
            'psp_external_id': PSP_EXTERNAL_ID,
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
async def test_psp_does_not_exists(storage):
    psp_id = uuid4()

    with pytest.raises(PSP.DoesNotExist):
        await storage.psp.get(psp_id)


@pytest.mark.asyncio
async def test_can_not_change_external_id(
    call_action
):
    psp_id = uuid4()
    await call_action(psp_id=psp_id, psp_external_id='gwid')

    with pytest.raises(CorePSPExternalIDChangedError):
        await call_action(psp_id=psp_id, psp_external_id='gwid2')


@pytest.mark.asyncio
async def test_invalid_auth_key(call_action, other_public_key):
    with pytest.raises(CoreInvalidAuthKeyError):
        await call_action(
            psp_auth_keys=[{'key': f'bad{other_public_key}', 'alg': 'ES256'}]
        )


@pytest.fixture
def call_action(public_key):
    async def _call_action(**kwargs):
        kwargs.setdefault('psp_id', uuid4())
        kwargs.setdefault('psp_external_id', PSP_EXTERNAL_ID)
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
