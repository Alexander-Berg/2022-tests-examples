import uuid

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant


@pytest.mark.asyncio
async def test_create(storage, merchant_entity):
    merchant_entity.is_blocked = True

    created = await storage.merchant.create(merchant_entity)

    merchant_entity.created = created.created
    merchant_entity.updated = created.updated
    merchant_entity.merchant_id = created.merchant_id
    assert_that(
        created,
        equal_to(merchant_entity),
    )


@pytest.mark.asyncio
async def test_get(storage, merchant_entity):
    created = await storage.merchant.create(merchant_entity)
    assert_that(
        await storage.merchant.get(merchant_entity.merchant_id),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Merchant.DoesNotExist):
        await storage.merchant.get(uuid.uuid4())


@pytest.mark.asyncio
async def test_save(storage, merchant_entity):
    created = await storage.merchant.create(merchant_entity)
    created.name = 'other-merchant-name'
    created.callback_url = 'new-callback-url'

    saved = await storage.merchant.save(created)
    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )
