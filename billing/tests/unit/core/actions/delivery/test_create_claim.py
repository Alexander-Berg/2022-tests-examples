import uuid
from dataclasses import replace
from datetime import datetime

import pytest
from pay.lib.entities.cart import Measurements
from pay.lib.entities.shipping import DeliveryStatus, ShippingMethodType
from pay.lib.interactions.yandex_delivery.entities import Claim, ClaimStatus, Item, ItemSize

from sendr_pytest.matchers import equal_to
from sendr_utils import alist

from hamcrest import assert_that, has_entries

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.check_state import CheckClaimStateAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.create_claim import (
    CreateDeliveryClaimAction,
    CreateDeliveryClaimByOrderIdAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.delivery.update import UpdateDeliveryAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import OrderNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.interactions.yandex_delivery import YandexDeliveryClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import StorageShippingMethod
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import Delivery, StorageWarehouse
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import (
    DeliveryIntegrationParams,
    YandexDeliveryParams,
)


@pytest.mark.asyncio
async def test_create_claim_by_order_id(mock_action, delivery, checkout_order):
    mock = mock_action(CreateDeliveryClaimAction, delivery)

    returned = await CreateDeliveryClaimByOrderIdAction(
        merchant_id=checkout_order.merchant_id, order_id=checkout_order.order_id
    ).run()

    mock.assert_called_once_with(
        merchant_id=checkout_order.merchant_id,
        checkout_order_id=checkout_order.checkout_order_id,
    )

    assert returned == delivery


@pytest.mark.asyncio
async def test_create_claim_by_order_id__order_not_found(mock_action, checkout_order):
    mock = mock_action(CreateDeliveryClaimAction)
    with pytest.raises(OrderNotFoundError):
        await CreateDeliveryClaimByOrderIdAction(merchant_id=uuid.uuid4(), order_id=checkout_order.order_id).run()

    mock.assert_not_called()


@pytest.mark.asyncio
async def test_calls_delivery_update(delivery, mock_action, claim):
    updated_delivery = replace(delivery, status=DeliveryStatus.ESTIMATING)
    update_mock = mock_action(UpdateDeliveryAction, updated_delivery)

    returned = await CreateDeliveryClaimAction(
        merchant_id=delivery.merchant_id, checkout_order_id=delivery.checkout_order_id
    ).run()

    update_mock.assert_called_once_with(
        delivery=delivery,
        claim_id=claim.id,
        status=claim.status,
        version=claim.version,
        pricing=claim.pricing,
    )
    assert_that(returned, equal_to(updated_delivery))


@pytest.mark.asyncio
async def test_calls_ydelivery(checkout_order, delivery, mock_ydelivery):
    await CreateDeliveryClaimAction(
        merchant_id=checkout_order.merchant_id, checkout_order_id=checkout_order.checkout_order_id
    ).run()

    mock_ydelivery.assert_awaited_once_with(
        auth_token='OaUtHtOkEn',
        request_id=str(checkout_order.checkout_order_id),
        items=[
            Item(
                cost_currency='RUB',
                cost_value='420.00',
                droppof_point=1,
                pickup_point=0,
                title='Awesome Product',
                size=ItemSize(length=1, height=2, width=3),
                weight=4,
                quantity=10,
            ),
            Item(
                cost_currency='RUB',
                cost_value='21.00',
                droppof_point=1,
                pickup_point=0,
                title='Awesome Product 2',
                size=ItemSize(length=1, height=2, width=3),
                weight=4,
                quantity=1,
            ),
        ],
        warehouse=delivery.warehouse,
        shipping_address=checkout_order.shipping_address,
        shipping_contact=checkout_order.shipping_contact,
        delivery_option=checkout_order.shipping_method.yandex_delivery_option,
    )


@pytest.mark.asyncio
async def test_schedules_status_check(checkout_order, delivery, storage):
    await CreateDeliveryClaimAction(
        merchant_id=checkout_order.merchant_id, checkout_order_id=checkout_order.checkout_order_id
    ).run()

    [task] = await alist(storage.task.find(filters={'action_name': CheckClaimStateAction.action_name}))
    assert_that(
        task.params,
        has_entries(
            action_kwargs=dict(
                merchant_id=str(checkout_order.merchant_id),
                checkout_order_id=str(checkout_order.checkout_order_id),
                delivery_version=1,
            ),
        ),
    )


@pytest.fixture
def claim():
    return Claim(
        id='claim-id',
        version=1,
        revision=1,
        updated_ts=datetime(2022, 2, 22),
        status=ClaimStatus.ESTIMATING,
    )


@pytest.fixture(autouse=True)
def mock_ydelivery(mocker, claim):
    return mocker.patch.object(YandexDeliveryClient, 'create_claim', mocker.AsyncMock(return_value=claim))


@pytest.fixture(autouse=True)
async def merchant(storage, stored_merchant):
    stored_merchant.delivery_integration_params = DeliveryIntegrationParams(
        yandex_delivery=YandexDeliveryParams(oauth_token=YandexDeliveryParams.encrypt_oauth_token('OaUtHtOkEn')),
        measurements=Measurements(length=1., height=2., width=3., weight=4.),
    )
    return await storage.merchant.save(stored_merchant)


@pytest.fixture
async def checkout_order(storage, stored_checkout_order, entity_yd_option):
    stored_checkout_order.shipping_method = StorageShippingMethod(
        method_type=ShippingMethodType.YANDEX_DELIVERY,
        yandex_delivery_option=entity_yd_option,
    )
    for item in stored_checkout_order.cart.items:
        if item.measurements is None:
            item.measurements = Measurements(length=1, height=2, width=3, weight=4)
    return await storage.checkout_order.save(stored_checkout_order)


@pytest.fixture
async def delivery(storage, checkout_order, entity_warehouse):
    return await storage.delivery.create(
        Delivery(
            checkout_order_id=checkout_order.checkout_order_id,
            merchant_id=checkout_order.merchant_id,
            price=checkout_order.shipping_method.yandex_delivery_option.amount,
            warehouse=StorageWarehouse.from_warehouse(entity_warehouse),
        )
    )
