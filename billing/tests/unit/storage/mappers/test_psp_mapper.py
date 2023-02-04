import uuid
from dataclasses import replace
from datetime import timedelta

import psycopg2.errors
import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.psp import PSP


@pytest.mark.asyncio
async def test_create(storage):
    psp_id = uuid.uuid4()
    psp = PSP(
        psp_id=psp_id,
        psp_external_id='123',
        public_key='foobar',
        public_key_signature='signature',
        is_blocked=True,
    )

    created = await storage.psp.create(psp)

    psp.public_key_updated = created.public_key_updated
    psp.created = created.created
    psp.updated = created.updated

    assert_that(
        created,
        equal_to(psp),
    )


@pytest.mark.asyncio
async def test_get(storage, card_entity):
    psp = PSP(
        psp_id=uuid.uuid4(),
        psp_external_id='123',
        public_key='foobar',
        public_key_signature='signature',
    )

    created = await storage.psp.create(psp)

    got = await storage.psp.get(created.psp_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(PSP.DoesNotExist):
        await storage.psp.get(uuid.uuid4())


@pytest.mark.asyncio
async def test_save(storage):
    psp = PSP(
        psp_id=uuid.uuid4(),
        psp_external_id='123',
        public_key='foobar',
        public_key_signature='signature',
    )
    created = await storage.psp.create(psp)
    created.psp_external_id = '456'
    created.public_key_signature = 'changed_signature'

    saved = await storage.psp.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_psp_external_id_is_unique(storage):
    psp = PSP(
        psp_id=uuid.uuid4(),
        psp_external_id='123',
        public_key='foobar',
        public_key_signature='signature',
    )
    await storage.psp.create(psp)

    with pytest.raises(psycopg2.errors.UniqueViolation):
        await storage.psp.create(psp)


@pytest.mark.asyncio
async def test_psp_external_id_is_not_empty(storage):
    psp = PSP(
        psp_id=uuid.uuid4(),
        psp_external_id='',
        public_key='foobar',
        public_key_signature='signature',
    )

    with pytest.raises(psycopg2.errors.CheckViolation):
        await storage.psp.create(psp)


@pytest.mark.asyncio
async def test_get_by_external_id(storage):
    psp = PSP(
        psp_id=uuid.uuid4(),
        psp_external_id='ex-id',
        public_key='foobar',
        public_key_signature='signature',
    )
    created = await storage.psp.create(psp)

    got = await storage.psp.get_by_external_id('ex-id')

    assert_that(got.psp_id, equal_to(created.psp_id))


@pytest.mark.asyncio
async def test_get_by_external_id_when_not_found(storage):
    with pytest.raises(PSP.DoesNotExist):
        await storage.psp.get_by_external_id('not-existent-ex-id')


@pytest.mark.asyncio
async def test_get_oldest_public_key_update_time(storage):
    psp = [await storage.psp.create(
        PSP(
            psp_id=uuid.uuid4(),
            psp_external_id=f'p{i}',
            public_key=f'foobar-{i}',
            public_key_signature='signature'
        )
    ) for i in range(3)]
    old_blocked = await storage.psp.create(
        PSP(
            psp_id=uuid.uuid4(),
            psp_external_id='p-old',
            public_key='publickey-old',
            public_key_signature='signature',
            is_blocked=True,
        )
    )
    await storage.psp.save(replace(old_blocked, public_key_updated=psp[0].public_key_updated - timedelta(days=1)))

    assert_that(
        await storage.psp.get_oldest_public_key_update_time(),
        equal_to(psp[0].public_key_updated)
    )
