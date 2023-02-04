from decimal import Decimal

import pytest
from pay.lib.entities.shipping import DeliveryStatus
from pay.lib.interactions.yandex_delivery.entities import ClaimStatus, Offer, Pricing

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.notify import NotifyMerchantDeliveryAsyncAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.update import UpdateDeliveryAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import Delivery, StorageWarehouse


@pytest.mark.asyncio
async def test_saves_to_db(delivery, params, storage, mock_action):
    notify_mock = mock_action(NotifyMerchantDeliveryAsyncAction)
    returned = await UpdateDeliveryAction(**params).run()

    delivery.status = DeliveryStatus.ESTIMATING
    delivery.raw_status = 'estimating'
    delivery.external_id = 'claim-id'
    delivery.version = 2
    delivery.updated = returned.updated

    assert_that(returned, equal_to(delivery))
    assert_that(
        await storage.delivery.get(delivery.delivery_id),
        equal_to(returned),
    )
    notify_mock.assert_called_once_with(delivery)


@pytest.mark.parametrize(
    'expected, pricing',
    [
        (Decimal('12.12'), Pricing(offer=Offer(offer_id='1', price=Decimal('12.12')))),
        (Decimal('32.10'), Pricing(offer=Offer(offer_id='1', price=Decimal('12.12')), final_price=Decimal('32.10'))),
    ],
)
@pytest.mark.asyncio
async def test_update_actual_price(params, pricing, expected):
    params['pricing'] = pricing

    returned = await UpdateDeliveryAction(**params).run()

    assert_that(returned.actual_price, equal_to(expected))


@pytest.mark.asyncio
async def test_notify_merchant__succes(params, mock_action):
    mock_notify = mock_action(NotifyMerchantDeliveryAsyncAction)

    await UpdateDeliveryAction(**params).run()

    mock_notify.assert_called_once()


@pytest.mark.asyncio
async def test_notify_merchant__same_status(params, mock_action):
    params['status'] = ClaimStatus.NEW
    mock_notify = mock_action(NotifyMerchantDeliveryAsyncAction)

    await UpdateDeliveryAction(**params).run()

    mock_notify.assert_not_called()


@pytest.fixture
def params(delivery):
    return dict(
        delivery=delivery,
        claim_id='claim-id',
        version=2,
        status=ClaimStatus.ESTIMATING,
    )


@pytest.fixture
async def delivery(storage, stored_checkout_order, entity_warehouse):
    return await storage.delivery.create(
        Delivery(
            checkout_order_id=stored_checkout_order.checkout_order_id,
            merchant_id=stored_checkout_order.merchant_id,
            price=Decimal('11'),
            warehouse=StorageWarehouse.from_warehouse(entity_warehouse),
        )
    )
