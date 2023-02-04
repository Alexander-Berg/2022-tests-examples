import uuid
from decimal import Decimal

import pytest
from pay.lib.entities.shipping import DeliveryStatus
from pay.lib.interactions.yandex_delivery.entities import CancelState

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.cancel_info import GetDeliveryCancelInfoAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.enums import DeliveryCancelState
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import DeliveryNotFoundError, OrderNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.interactions.yandex_delivery import YandexDeliveryClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import Delivery, StorageWarehouse
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)


@pytest.mark.asyncio
async def test_get_cancel_info(delivery, stored_checkout_order):
    returned = await GetDeliveryCancelInfoAction(
        merchant_id=stored_checkout_order.merchant_id, order_id=stored_checkout_order.order_id
    ).run()

    assert_that(returned, equal_to(DeliveryCancelState.PAID))


@pytest.mark.asyncio
async def test_calls_ydelivery(delivery, stored_checkout_order, mock_ydelivery):
    await GetDeliveryCancelInfoAction(merchant_id=delivery.merchant_id, order_id=stored_checkout_order.order_id).run()

    mock_ydelivery.assert_awaited_once_with(
        auth_token='OaUtHtOkEn',
        claim_id='yd-claim-id',
    )


@pytest.mark.asyncio
async def test_order_not_found(stored_checkout_order):
    with pytest.raises(OrderNotFoundError):
        await GetDeliveryCancelInfoAction(merchant_id=uuid.uuid4(), order_id=stored_checkout_order.order_id).run()


@pytest.mark.asyncio
async def test_delivery_not_found(stored_merchant, stored_checkout_order):
    with pytest.raises(DeliveryNotFoundError):
        await GetDeliveryCancelInfoAction(
            merchant_id=stored_merchant.merchant_id, order_id=stored_checkout_order.order_id
        ).run()


@pytest.fixture(autouse=True)
def mock_ydelivery(mocker):
    return mocker.patch.object(
        YandexDeliveryClient,
        'get_cancel_info',
        mocker.AsyncMock(
            return_value=CancelState.PAID,
        ),
    )


@pytest.fixture(autouse=True)
async def merchant(storage, stored_merchant):
    stored_merchant.delivery_integration_params = DeliveryIntegrationParams(
        yandex_delivery=YandexDeliveryParams(oauth_token=YandexDeliveryParams.encrypt_oauth_token('OaUtHtOkEn')),
    )
    return await storage.merchant.save(stored_merchant)


@pytest.fixture
async def delivery(storage, stored_checkout_order, entity_warehouse):
    return await storage.delivery.create(
        Delivery(
            checkout_order_id=stored_checkout_order.checkout_order_id,
            merchant_id=stored_checkout_order.merchant_id,
            price=Decimal('12'),
            warehouse=StorageWarehouse.from_warehouse(entity_warehouse),
            external_id='yd-claim-id',
            status=DeliveryStatus.READY_FOR_APPROVAL,
            raw_status='yd-status',
            version=1,
        )
    )
