from dataclasses import replace
from uuid import uuid4

import pytest
from pay.lib.entities.enums import CardNetwork, PaymentMethodType, ShippingMethodType
from pay.lib.entities.order import ContactFields, Order, PaymentMethod, PaymentStatus

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import mock_action  # noqa

from hamcrest import assert_that, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.get import GetMerchantOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.merchant_order import GetMerchantOrderResponse
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import OrderNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import (
    StorageContact,
    StorageRequiredFields,
    StorageShippingMethod,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import TransactionStatus


@pytest.fixture
async def checkout_order(storage, entity_checkout_order, entity_courier_option):
    entity_checkout_order.payment_method_type = PaymentMethodType.CARD
    entity_checkout_order.billing_contact = StorageContact(id='bcid', email='email')
    entity_checkout_order.available_payment_methods = [PaymentMethodType.CARD]
    entity_checkout_order.shipping_method = StorageShippingMethod(
        method_type=ShippingMethodType.COURIER,
        courier_option=entity_courier_option,
    )
    entity_checkout_order.required_fields = StorageRequiredFields(
        billing_contact=ContactFields(name=True, email=True, phone=True),
        shipping_contact=ContactFields(name=False, email=False, phone=False),
    )
    entity_checkout_order.payment_status = PaymentStatus.AUTHORIZED
    return await storage.checkout_order.create(entity_checkout_order)


@pytest.fixture
async def operation(storage, entity_operation, checkout_order):
    return await storage.order_operation.create(
        replace(
            entity_operation,
            merchant_id=checkout_order.merchant_id,
            checkout_order_id=checkout_order.checkout_order_id,
        )
    )


@pytest.fixture
async def transaction(storage, entity_transaction, checkout_order, stored_integration):
    return await storage.transaction.create(
        replace(
            entity_transaction,
            checkout_order_id=checkout_order.checkout_order_id,
            integration_id=stored_integration.integration_id,
            status=TransactionStatus.AUTHORIZED,
            reason='the-reason',
            card_last4='1234',
            card_network=CardNetwork.MIR,
        )
    )


@pytest.mark.asyncio
async def test_success(checkout_order, operation, transaction):
    response = await GetMerchantOrderAction(
        merchant_id=checkout_order.merchant_id,
        order_id=checkout_order.order_id,
    ).run()

    assert_that(
        response,
        equal_to(
            GetMerchantOrderResponse(
                order=ensure_all_fields(Order)(
                    currency_code=checkout_order.currency_code,
                    cart=checkout_order.cart,
                    checkout_order_id=None,
                    merchant_id=checkout_order.merchant_id,
                    order_amount=checkout_order.order_amount,
                    order_id=checkout_order.order_id,
                    t=None,
                    payment_method=PaymentMethod(
                        method_type=checkout_order.payment_method_type,
                        card_last4='1234',
                        card_network=CardNetwork.MIR,
                    ),
                    shipping=None,
                    shipping_method=checkout_order.shipping_method,
                    shipping_address=checkout_order.shipping_address,
                    shipping_contact=checkout_order.shipping_contact,
                    billing_contact=checkout_order.billing_contact,
                    available_payment_methods=checkout_order.available_payment_methods,
                    enable_coupons=checkout_order.enable_coupons,
                    enable_comment_field=checkout_order.enable_comment_field,
                    required_fields=checkout_order.required_fields,
                    metadata=checkout_order.metadata,
                    created=checkout_order.created,
                    updated=checkout_order.updated,
                    payment_status=PaymentStatus.AUTHORIZED,
                    reason=transaction.reason,
                ),
                operations=[operation],
            )
        ),
    )


@pytest.mark.asyncio
async def test_no_transaction(checkout_order, operation):
    response = await GetMerchantOrderAction(
        merchant_id=checkout_order.merchant_id,
        order_id=checkout_order.order_id,
    ).run()

    assert_that(
        response.order,
        has_properties(
            payment_status=PaymentStatus.AUTHORIZED,
            reason=None,
        ),
    )


@pytest.mark.asyncio
async def test_no_payment_method_type(storage, checkout_order, operation, transaction):
    checkout_order = await storage.checkout_order.save(
        replace(
            checkout_order,
            payment_method_type=None,
        )
    )
    response = await GetMerchantOrderAction(
        merchant_id=checkout_order.merchant_id,
        order_id=checkout_order.order_id,
    ).run()

    assert_that(
        response.order,
        has_properties(
            payment_method=None,
        ),
    )


@pytest.mark.asyncio
async def test_not_found(checkout_order):
    with pytest.raises(OrderNotFoundError):
        await GetMerchantOrderAction(
            merchant_id=uuid4(),
            order_id=checkout_order.order_id,
        ).run()
