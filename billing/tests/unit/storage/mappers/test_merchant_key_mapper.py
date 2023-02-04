import uuid

import psycopg2.errors
import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey
from billing.yandex_pay_plus.yandex_pay_plus.storage.exceptions import DuplicateMerchantKeyStorageError


@pytest.fixture
async def merchant(storage):
    return await storage.merchant.create(Merchant(name='1'))


@pytest.fixture
async def merchant_keys(merchant, storage):
    merchant2 = await storage.merchant.create(Merchant(name='2'))
    keys = [
        MerchantKey(merchant_id=merchant.merchant_id, key_hash='1', deleted=True),
        MerchantKey(merchant_id=merchant.merchant_id, key_hash='2'),
        MerchantKey(merchant_id=merchant.merchant_id, key_hash='3'),
        MerchantKey(merchant_id=merchant2.merchant_id, key_hash='4'),
        MerchantKey(merchant_id=merchant2.merchant_id, key_hash='5'),
    ]
    return [await storage.merchant_key.create(k) for k in keys]


@pytest.mark.asyncio
async def test_create(storage, merchant):
    merchant_key = MerchantKey.create(
        merchant_id=merchant.merchant_id,
        key='foobar',
    )

    created = await storage.merchant_key.create(merchant_key)

    merchant_key.created = created.created
    merchant_key.updated = created.updated
    assert_that(
        created,
        equal_to(merchant_key),
    )


@pytest.mark.asyncio
async def test_create_requires_merchant_to_exist(storage):
    merchant_key = MerchantKey.create(
        merchant_id=uuid.uuid4(),
        key='foobar',
    )

    with pytest.raises(psycopg2.errors.ForeignKeyViolation):
        await storage.merchant_key.create(merchant_key)


@pytest.mark.asyncio
async def test_get(storage, merchant):
    merchant_key = MerchantKey.create(
        merchant_id=merchant.merchant_id,
        key='foobar',
    )

    created = await storage.merchant_key.create(merchant_key)

    assert_that(
        await storage.merchant_key.get(created.key_id),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_by_key_hash(storage, merchant):
    merchant_key = MerchantKey.create(
        merchant_id=merchant.merchant_id,
        key='foobar',
    )

    created = await storage.merchant_key.create(merchant_key)

    assert_that(
        await storage.merchant_key.get_by_key_hash(merchant_key.key_hash),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_find_by_merchant_id(storage, merchant, merchant_keys):
    assert_that(
        await storage.merchant_key.find_by_merchant_id(merchant.merchant_id),
        equal_to(merchant_keys[1:3]),
    )


@pytest.mark.asyncio
async def get_keys_count(storage, merchant, merchant_keys):
    assert_that(
        await storage.merchant_key.find_by_merchant_id(merchant.merchant_id),
        equal_to(2),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage, merchant):
    with pytest.raises(MerchantKey.DoesNotExist):
        await storage.merchant_key.get(uuid.uuid4())


@pytest.mark.asyncio
async def test_save(storage, merchant):
    merchant_key = MerchantKey.create(
        merchant_id=merchant.merchant_id,
        key='foobar',
    )
    created = await storage.merchant_key.create(merchant_key)
    created.deleted = not created.deleted

    saved = await storage.merchant_key.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_duplicate(storage, merchant):
    await storage.merchant_key.create(
        MerchantKey.create(merchant_id=merchant.merchant_id, key='foobar')
    )

    with pytest.raises(DuplicateMerchantKeyStorageError):
        await storage.merchant_key.create(
            MerchantKey.create(merchant_id=merchant.merchant_id, key='foobar')
        )
