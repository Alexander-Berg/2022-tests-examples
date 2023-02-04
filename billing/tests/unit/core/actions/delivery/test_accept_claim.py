import uuid
from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.entities.shipping import DeliveryStatus
from pay.lib.interactions.yandex_delivery.entities import AcceptClaimResponse, ClaimStatus

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.accept_claim import (
    AcceptDeliveryClaimAction,
    AcceptDeliveryClaimByOrderIdAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.update import UpdateDeliveryAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import InvalidDeliveryStatusError, OrderNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.interactions.yandex_delivery import YandexDeliveryClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import Delivery, StorageWarehouse
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)


@pytest.mark.asyncio
async def test_accept_claim_by_order_id(mock_action, delivery, stored_checkout_order):
    mock = mock_action(AcceptDeliveryClaimAction, delivery)

    returned = await AcceptDeliveryClaimByOrderIdAction(
        merchant_id=stored_checkout_order.merchant_id, order_id=stored_checkout_order.order_id
    ).run()

    mock.assert_called_once_with(
        merchant_id=stored_checkout_order.merchant_id,
        checkout_order_id=stored_checkout_order.checkout_order_id,
    )

    assert returned == delivery


@pytest.mark.asyncio
async def test_accept_claim_by_order_id__order_not_found(mock_action, stored_checkout_order):
    mock = mock_action(AcceptDeliveryClaimAction)
    with pytest.raises(OrderNotFoundError):
        await AcceptDeliveryClaimByOrderIdAction(
            merchant_id=uuid.uuid4(), order_id=stored_checkout_order.order_id
        ).run()

    mock.assert_not_called()


@pytest.mark.asyncio
async def test_calls_delivery_update(delivery, mock_action):
    updated_delivery = replace(delivery, status=DeliveryStatus.DELIVERING)
    update_mock = mock_action(UpdateDeliveryAction, updated_delivery)

    await AcceptDeliveryClaimAction(
        merchant_id=delivery.merchant_id, checkout_order_id=delivery.checkout_order_id
    ).run()

    update_mock.assert_called_once_with(
        delivery=delivery,
        claim_id='claim-id',
        status=ClaimStatus.ACCEPTED,
        version=5,
    )


@pytest.mark.asyncio
async def test_calls_ydelivery(delivery, mock_ydelivery):
    await AcceptDeliveryClaimAction(
        merchant_id=delivery.merchant_id, checkout_order_id=delivery.checkout_order_id
    ).run()

    mock_ydelivery.assert_awaited_once_with(
        auth_token='OaUtHtOkEn',
        claim_id='claim-id',
        version=5,
    )


@pytest.mark.asyncio
async def test_invalid_delivery_status(storage, delivery):
    delivery.status = DeliveryStatus.ESTIMATING
    await storage.delivery.save(delivery)

    with pytest.raises(InvalidDeliveryStatusError):
        await AcceptDeliveryClaimAction(
            merchant_id=delivery.merchant_id, checkout_order_id=delivery.checkout_order_id
        ).run()


@pytest.fixture(autouse=True)
def mock_ydelivery(mocker):
    return mocker.patch.object(
        YandexDeliveryClient,
        'accept_claim',
        mocker.AsyncMock(
            return_value=AcceptClaimResponse(id='claim-id', status=ClaimStatus.ACCEPTED, version=5),
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
            price=Decimal('11'),
            warehouse=StorageWarehouse.from_warehouse(entity_warehouse),
            external_id='claim-id',
            status=DeliveryStatus.READY_FOR_APPROVAL,
            raw_status='estimating',
            version=5,
        )
    )
