import uuid

import psycopg2.errors
import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP


@pytest.mark.asyncio
async def test_create(storage):
    psp_id = uuid.uuid4()
    psp = PSP(
        psp_id=psp_id,
        psp_external_id='123',
        is_blocked=True,
    )

    created = await storage.psp.create(psp)

    psp.created = created.created
    psp.updated = created.updated

    assert_that(
        created,
        equal_to(psp),
    )


@pytest.mark.asyncio
async def test_get(storage):
    psp = PSP(
        psp_id=uuid.uuid4(),
        psp_external_id='123',
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
    )
    created = await storage.psp.create(psp)
    created.psp_external_id = '456'

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
    )
    await storage.psp.create(psp)

    with pytest.raises(psycopg2.errors.UniqueViolation):
        await storage.psp.create(psp)


@pytest.mark.asyncio
async def test_psp_external_id_is_not_empty(storage):
    psp = PSP(
        psp_id=uuid.uuid4(),
        psp_external_id='',
    )

    with pytest.raises(psycopg2.errors.CheckViolation):
        await storage.psp.create(psp)


@pytest.mark.asyncio
async def test_get_by_external_id(storage):
    psp = PSP(
        psp_id=uuid.uuid4(),
        psp_external_id='ex-id',
    )
    created = await storage.psp.create(psp)

    got = await storage.psp.get_by_external_id('ex-id')

    assert_that(got.psp_id, equal_to(created.psp_id))


@pytest.mark.asyncio
async def test_get_by_external_id_when_not_found(storage):
    with pytest.raises(PSP.DoesNotExist):
        await storage.psp.get_by_external_id('not-existent-ex-id')
