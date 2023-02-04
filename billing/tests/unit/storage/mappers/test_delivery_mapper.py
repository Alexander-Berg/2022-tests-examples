from decimal import Decimal
from uuid import uuid4

import pytest
from pay.lib.entities.shipping import DeliveryStatus

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import Delivery, StorageWarehouse


@pytest.fixture
def make_delivery(stored_checkout_order, entity_warehouse):
    def _inner(**kwargs):
        kwargs = {
            'delivery_id': uuid4(),
            'external_id': 'claim-id',
            'checkout_order_id': stored_checkout_order.checkout_order_id,
            'merchant_id': stored_checkout_order.merchant_id,
            'price': Decimal('11.00'),
            'actual_price': Decimal('12.00'),
            'warehouse': StorageWarehouse.from_warehouse(entity_warehouse),
            'status': DeliveryStatus.READY_FOR_APPROVAL,
            'raw_status': 'ready_for_approval',
            'version': 2,
        } | kwargs
        return Delivery(**kwargs)
    return _inner


@pytest.mark.asyncio
async def test_create(storage, make_delivery):
    delivery = make_delivery()

    created = await storage.delivery.create(delivery)

    delivery.created = created.created
    delivery.updated = created.updated
    assert_that(
        created,
        equal_to(delivery),
    )


@pytest.mark.asyncio
async def test_get(storage, make_delivery):
    delivery = make_delivery()

    created = await storage.delivery.create(delivery)

    got = await storage.delivery.get(delivery.delivery_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_by_checkout_order_id(storage, make_delivery):
    delivery = make_delivery()

    created = await storage.delivery.create(delivery)

    got = await storage.delivery.get_by_checkout_order_id(delivery.checkout_order_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Delivery.DoesNotExist):
        await storage.delivery.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage, make_delivery):
    created = await storage.delivery.create(make_delivery())
    created.status = DeliveryStatus.EXPIRED
    created.version += 1

    saved = await storage.delivery.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )
