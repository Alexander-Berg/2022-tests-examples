import uuid
from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.entities.shipping import DeliveryStatus
from pay.lib.interactions.yandex_delivery.entities import CancelClaimResponse, CancelState, ClaimStatus
from pay.lib.interactions.yandex_delivery.exceptions import CancelNotAvailableInteractionError

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.cancel_claim import (
    CancelDeliveryClaimAction,
    CancelDeliveryClaimByOrderIdAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.update import UpdateDeliveryAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.enums import SatisfactoryDeliveryCancelState
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import DeliveryCancelNotAvailableError, OrderNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.interactions.yandex_delivery import YandexDeliveryClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import Delivery, StorageWarehouse
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)


@pytest.mark.asyncio
async def test_cancel_claim_by_order_id(mock_action, delivery, stored_checkout_order):
    mock = mock_action(CancelDeliveryClaimAction, delivery)
    returned = await CancelDeliveryClaimByOrderIdAction(
        merchant_id=stored_checkout_order.merchant_id,
        order_id=stored_checkout_order.order_id,
        cancel_state=SatisfactoryDeliveryCancelState.FREE,
    ).run()

    mock.assert_called_once_with(
        merchant_id=stored_checkout_order.merchant_id,
        checkout_order_id=stored_checkout_order.checkout_order_id,
        cancel_state=SatisfactoryDeliveryCancelState.FREE,
    )

    assert returned == delivery


@pytest.mark.asyncio
async def test_cancel_claim_by_order_id__order_not_found(mock_action, stored_checkout_order):
    with pytest.raises(OrderNotFoundError):
        await CancelDeliveryClaimByOrderIdAction(
            merchant_id=uuid.uuid4(),
            order_id=stored_checkout_order.order_id,
            cancel_state=SatisfactoryDeliveryCancelState.FREE,
        ).run()


@pytest.mark.asyncio
async def test_calls_delivery_update(delivery, mock_action):
    updated_delivery = replace(delivery, status=DeliveryStatus.CANCELLED)
    update_mock = mock_action(UpdateDeliveryAction, updated_delivery)

    await CancelDeliveryClaimAction(
        merchant_id=delivery.merchant_id,
        checkout_order_id=delivery.checkout_order_id,
        cancel_state=SatisfactoryDeliveryCancelState.FREE,
    ).run()

    update_mock.assert_called_once_with(
        delivery=delivery,
        claim_id='claim-id',
        status=ClaimStatus.CANCELLED,
        version=4,
    )


@pytest.mark.asyncio
async def test_calls_ydelivery(delivery, mock_ydelivery):
    await CancelDeliveryClaimAction(
        merchant_id=delivery.merchant_id,
        checkout_order_id=delivery.checkout_order_id,
        cancel_state=SatisfactoryDeliveryCancelState.FREE,
    ).run()

    mock_ydelivery.assert_awaited_once_with(
        auth_token='OaUtHtOkEn',
        claim_id='claim-id',
        version=4,
        cancel_state=CancelState.FREE,
    )


@pytest.mark.asyncio
async def test_cancel_not_available(storage, delivery, mocker):
    mocker.patch.object(
        YandexDeliveryClient,
        'cancel_claim',
        mocker.AsyncMock(side_effect=CancelNotAvailableInteractionError(status_code=0, method='', service='')),
    )

    with pytest.raises(DeliveryCancelNotAvailableError):
        await CancelDeliveryClaimAction(
            merchant_id=delivery.merchant_id,
            checkout_order_id=delivery.checkout_order_id,
            cancel_state=SatisfactoryDeliveryCancelState.FREE,
        ).run()


@pytest.fixture(autouse=True)
def mock_ydelivery(mocker):
    return mocker.patch.object(
        YandexDeliveryClient,
        'cancel_claim',
        mocker.AsyncMock(
            return_value=CancelClaimResponse(id='claim-id', status=ClaimStatus.CANCELLED),
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
            version=4,
        )
    )
