import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.get_keys import GetMerchantKeysAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey


@pytest.fixture
async def merchant(storage):
    return await storage.merchant.create(Merchant(name='1'))


@pytest.mark.asyncio
async def test_get_merchant_keys(merchant, storage):
    merchant2 = await storage.merchant.create(Merchant(name='2'))
    keys = [
        MerchantKey(merchant_id=merchant.merchant_id, key_hash='1', deleted=True),
        MerchantKey(merchant_id=merchant.merchant_id, key_hash='2'),
        MerchantKey(merchant_id=merchant2.merchant_id, key_hash='4'),
    ]
    keys = [await storage.merchant_key.create(k) for k in keys]

    assert_that(
        await GetMerchantKeysAction(merchant.merchant_id).run(),
        equal_to(keys[1:-1]),
    )
