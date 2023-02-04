from uuid import uuid4

import pytest

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.delete_key import DeleteMerchantKeyAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import CoreKeyNotFoundError, CoreMerchantIdMismatchError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey


@pytest.fixture
async def merchant(storage):
    return await storage.merchant.create(Merchant(name='1'))


@pytest.fixture
async def merchant_keys(storage, merchant):
    merchant2 = await storage.merchant.create(Merchant(name='2'))
    keys = [
        MerchantKey(merchant_id=merchant.merchant_id, key_hash='1', deleted=True),
        MerchantKey(merchant_id=merchant.merchant_id, key_hash='2'),
        MerchantKey(merchant_id=merchant2.merchant_id, key_hash='4'),
    ]
    return [await storage.merchant_key.create(k) for k in keys]


@pytest.mark.asyncio
async def test_delete_key(merchant, storage, merchant_keys):
    assert merchant_keys[1].deleted is False
    key_id = merchant_keys[1].key_id
    await DeleteMerchantKeyAction(merchant_id=merchant.merchant_id, key_id=key_id).run()

    key = await storage.merchant_key.get(key_id)
    assert key.deleted is True


@pytest.mark.asyncio
async def test_delete_key_idempotency(merchant, storage, merchant_keys):
    assert merchant_keys[0].deleted is True
    key_id = merchant_keys[0].key_id
    await DeleteMerchantKeyAction(merchant_id=merchant.merchant_id, key_id=key_id).run()

    key = await storage.merchant_key.get(key_id)
    assert key.deleted is True


@pytest.mark.asyncio
async def test_delete_non_existing_key(merchant, storage):
    with pytest.raises(CoreKeyNotFoundError):
        await DeleteMerchantKeyAction(merchant_id=merchant.merchant_id, key_id=uuid4()).run()


@pytest.mark.asyncio
async def test_merchant_id_mismatch(merchant, storage, merchant_keys):
    key_id = merchant_keys[-1].key_id

    with pytest.raises(CoreMerchantIdMismatchError):
        await DeleteMerchantKeyAction(merchant_id=merchant.merchant_id, key_id=key_id).run()
