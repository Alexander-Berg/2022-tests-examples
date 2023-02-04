from uuid import uuid4

import pytest

from hamcrest import assert_that, contains, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_origin import MerchantOrigin
from billing.yandex_pay_plus.yandex_pay_plus.utils.domain import get_canonical_origin


@pytest.fixture
def merchant_entity(rands):
    return Merchant(
        name=rands(),
    )


@pytest.fixture
async def merchant_origin_entity(merchant_entity, storage):
    merchant = await storage.merchant.create(merchant_entity)
    merchant_origin = MerchantOrigin(
        merchant_id=merchant.merchant_id,
        origin=get_canonical_origin('https://origin.ru'),
        is_blocked=True,
    )
    return merchant_origin


@pytest.mark.asyncio
async def test_create(merchant_origin_entity: MerchantOrigin, storage):
    merchant_origin: MerchantOrigin = await storage.merchant_origin.create(merchant_origin_entity)

    merchant_origin_entity.created = merchant_origin.created
    merchant_origin_entity.updated = merchant_origin.updated
    assert_that(merchant_origin, equal_to(merchant_origin_entity))


@pytest.mark.asyncio
async def test_get(merchant_origin_entity, storage):
    merchant_origin_created = await storage.merchant_origin.create(merchant_origin_entity)
    merchant_origin_fetched = await storage.merchant_origin.get(
        merchant_origin_created.merchant_id,
        merchant_origin_created.origin,
    )
    assert_that(merchant_origin_created, equal_to(merchant_origin_fetched))


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(MerchantOrigin.DoesNotExist):
        await storage.merchant_origin.get(uuid4(), get_canonical_origin('https://some-origin.ru'))


@pytest.mark.asyncio
async def test_find_by_merchant_id(merchant_entity, storage):
    merchant = await storage.merchant.create(Merchant(name='merchant'))
    other_merchant = await storage.merchant.create(Merchant(name='other-merchant'))
    for origin, merchant_id in (
        ('https://1.test:443', merchant.merchant_id),
        ('https://2.test:443', merchant.merchant_id),
        ('https://3.test:443', other_merchant.merchant_id),
    ):
        await storage.merchant_origin.create(
            MerchantOrigin(
                merchant_id=merchant_id,
                origin=origin,
            )
        )

    result = await storage.merchant_origin.find_by_merchant_id(
        merchant.merchant_id,
    )

    assert_that(
        [o.origin for o in result],
        contains('https://1.test:443', 'https://2.test:443'),
    )
