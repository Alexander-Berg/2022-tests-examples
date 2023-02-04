import uuid

import pytest

from hamcrest import assert_that, has_properties

from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.actions.psp.key import CreatePSPAuthKeyAction
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.exceptions import CoreAuthKeyAlgNotSupportedError, CorePSPNotFoundError


@pytest.fixture
def key():
    return (
        'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwmTG26LY5/0bc7VwMfJ9kfqfLTlYr/Ue9c0G7jdFp08dWX2osZ22JR/S9PUWKblmyl1CSGiHV0'
        'bAtlbeC3QgjQ=='
    )


@pytest.mark.asyncio
async def test_create_key_returns_key(storage, key):
    psp = await create_psp_entity(
        storage,
        PSP(psp_id=uuid.uuid4(), psp_external_id='ext-id', public_key='1', public_key_signature='2'),
    )

    psp_key = await CreatePSPAuthKeyAction(psp_id=psp.psp_id, key=key).run()

    assert_that(
        psp_key,
        has_properties({
            'psp_id': psp.psp_id,
            'psp_key_id': 1,
            'key': key,
            'alg': 'ES256',
        })
    )


@pytest.mark.asyncio
async def test_create_key_when_psp_not_exists__raises_error(storage, key):
    non_existent_psp_id = uuid.uuid4()
    with pytest.raises(CorePSPNotFoundError):
        await CreatePSPAuthKeyAction(psp_id=non_existent_psp_id, key=key).run()


@pytest.mark.asyncio
async def test_create_key_when_unknown_alg__raises_error(storage, key):
    non_existent_psp_id = uuid.uuid4()
    with pytest.raises(CoreAuthKeyAlgNotSupportedError):
        await CreatePSPAuthKeyAction(psp_id=non_existent_psp_id, key=key, alg='alg').run()
