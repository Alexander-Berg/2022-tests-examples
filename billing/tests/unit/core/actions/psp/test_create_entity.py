from uuid import uuid4

import pytest

from hamcrest import assert_that, has_properties

from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP

PSP_EXTERNAL_ID = 'gwid'


@pytest.mark.asyncio
async def test_create_psp_entities(storage):
    psp_id = uuid4()

    await create_psp_entity(
        storage,
        PSP(psp_id=psp_id, public_key='public_key', public_key_signature='xxxyyy', psp_external_id=PSP_EXTERNAL_ID)
    )

    assert_that(
        await storage.psp.get(psp_id),
        has_properties({
            'psp_id': psp_id,
            'public_key': 'public_key',
            'public_key_signature': 'xxxyyy',
            'psp_external_id': PSP_EXTERNAL_ID,
        })
    )
    assert_that(
        await storage.psp_serial.get(psp_id),
        has_properties({
            'psp_id': psp_id,
            'next_auth_key_id': 1,
        })
    )
