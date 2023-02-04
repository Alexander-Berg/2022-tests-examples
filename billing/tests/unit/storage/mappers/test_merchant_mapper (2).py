import uuid
from dataclasses import replace

import pytest
from pay.lib.entities.cart import Measurements

from sendr_pytest.helpers import ensure_all_fields

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import (
    DeliveryIntegrationParams,
    Merchant,
    YandexDeliveryParams,
)


@pytest.fixture
def merchant_entity(rands, entity_warehouse):
    return ensure_all_fields(Merchant)(
        name=rands(),
        callback_url='http://127.0.0.1/',
        delivery_integration_params=ensure_all_fields(DeliveryIntegrationParams)(
            yandex_delivery=ensure_all_fields(YandexDeliveryParams)(
                oauth_token=YandexDeliveryParams.encrypt_oauth_token('OaUtHToKeH'),
                autoaccept=True,
                warehouses=[entity_warehouse],
            ),
            measurements=Measurements(length=1, height=2, width=3, weight=4),
        ),
        merchant_id=None,
        partner_id=uuid.uuid4(),
        split_merchant_id=None,
        is_blocked=False,
        created=None,
        updated=None,
        first_transaction=uuid.uuid4(),
    )


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


@pytest.mark.asyncio
async def test_get_merchant_by_split_merchant_id(storage, merchant_entity):
    created = await storage.merchant.create(replace(merchant_entity, split_merchant_id='split'))
    assert_that(
        await storage.merchant.get_by_split_merchant_id('split'),
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_by_split_merchant_id_not_found(storage):
    with pytest.raises(Merchant.DoesNotExist):
        await storage.merchant.get_by_split_merchant_id('split'),
