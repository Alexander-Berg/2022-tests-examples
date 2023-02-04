import uuid

import psycopg2.errors
import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.psp_serial import PSPSerial


@pytest.fixture
async def psp(storage):
    return await storage.psp.create(
        PSP(psp_id=uuid.uuid4(), psp_external_id='1', public_key='', public_key_signature='')
    )


@pytest.mark.asyncio
async def test_create(storage, psp):
    psp_serial = PSPSerial(psp_id=psp.psp_id)

    created = await storage.psp_serial.create(psp_serial)

    psp_serial.created = created.created
    psp_serial.updated = created.updated
    assert_that(
        created,
        equal_to(psp_serial),
    )


@pytest.mark.asyncio
async def test_create_requires_psp_to_exist(storage):
    non_existent_psp_id = uuid.uuid4()
    psp_serial = PSPSerial(psp_id=non_existent_psp_id)

    with pytest.raises(psycopg2.errors.ForeignKeyViolation):
        await storage.psp_serial.create(psp_serial)


@pytest.mark.asyncio
async def test_get(storage, card_entity, psp):
    psp_serial = PSPSerial(psp_id=psp.psp_id)
    created = await storage.psp_serial.create(psp_serial)

    got = await storage.psp_serial.get(created.psp_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage, psp):
    with pytest.raises(PSPSerial.DoesNotExist):
        await storage.psp_serial.get(psp.psp_id)


@pytest.mark.parametrize('column', ('next_auth_key_id',))
class TestSerialLogic:
    @pytest.mark.asyncio
    async def test_acquire(self, storage, psp, column):
        psp_serial = PSPSerial(psp_id=psp.psp_id)
        psp_serial = await storage.psp_serial.create(psp_serial)
        acquired = await storage.psp_serial.acquire_serial(psp.psp_id, column)

        assert acquired == 1

    @pytest.mark.asyncio
    async def test_is_indeed_serial(self, storage, psp, column):
        psp_serial = PSPSerial(psp_id=psp.psp_id)
        psp_serial = await storage.psp_serial.create(psp_serial)
        acquired = await storage.psp_serial.acquire_serial(psp.psp_id, column)
        acquired_2 = await storage.psp_serial.acquire_serial(psp.psp_id, column)

        assert acquired == 1 and acquired_2 == 2
