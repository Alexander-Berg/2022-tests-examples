import uuid

import psycopg2.errors
import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp_key import PSPKey


@pytest.fixture
async def psp(storage):
    return await storage.psp.create(
        PSP(psp_id=uuid.uuid4(), psp_external_id='1')
    )


@pytest.mark.asyncio
async def test_create(storage, psp):
    psp_key = PSPKey(
        psp_id=psp.psp_id,
        psp_key_id=2,
        key='foobar',
        alg='ES256',
    )

    created = await storage.psp_key.create(psp_key)

    psp_key.created = created.created
    psp_key.updated = created.updated
    assert_that(
        created,
        equal_to(psp_key),
    )


@pytest.mark.asyncio
async def test_create_requires_psp_to_exist(storage):
    non_existent_psp_id = uuid.uuid4()
    psp_key = PSPKey(
        psp_id=non_existent_psp_id,
        psp_key_id=2,
        key='foobar',
        alg='ES256',
    )

    with pytest.raises(psycopg2.errors.ForeignKeyViolation):
        await storage.psp_key.create(psp_key)


@pytest.mark.asyncio
async def test_get(storage, psp):
    psp_key = PSPKey(
        psp_id=psp.psp_id,
        psp_key_id=2,
        key='foobar',
        alg='ES256',
    )

    created = await storage.psp_key.create(psp_key)

    got = await storage.psp_key.get(created.psp_id, created.psp_key_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage, psp):
    with pytest.raises(PSPKey.DoesNotExist):
        await storage.psp_key.get(psp.psp_id, 67890)


@pytest.mark.asyncio
async def test_save(storage, psp):
    psp_key = PSPKey(
        psp_id=psp.psp_id,
        psp_key_id=1,
        key='key1',
        alg='ES256',
    )
    created = await storage.psp_key.create(psp_key)
    created.key = 'key2'

    saved = await storage.psp_key.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )
