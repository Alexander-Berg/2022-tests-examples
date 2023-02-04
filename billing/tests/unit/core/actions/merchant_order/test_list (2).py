from datetime import datetime
from uuid import uuid4

import pytest
from pay.lib.entities.order import Order, PaymentStatus

from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import mock_action  # noqa

from hamcrest import assert_that

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.list import ListMerchantOrdersAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.mappers.checkout_order import CheckoutOrderMapper


@pytest.fixture
async def checkout_order(storage, entity_checkout_order, entity_courier_option):
    return await storage.checkout_order.create(entity_checkout_order)


@pytest.mark.asyncio
async def test_result(checkout_order):
    response = await ListMerchantOrdersAction(merchant_id=checkout_order.merchant_id).run()

    assert_that(
        response,
        equal_to(
            [
                Order(
                    currency_code=checkout_order.currency_code,
                    cart=checkout_order.cart,
                    checkout_order_id=None,
                    merchant_id=checkout_order.merchant_id,
                    order_amount=checkout_order.order_amount,
                    order_id=checkout_order.order_id,
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
                    payment_status=PaymentStatus.PENDING,
                ),
            ]
        ),
    )


@pytest.mark.asyncio
async def test_storage_call_params(mocker):
    merchant_id = uuid4()
    storage_mock = mocker.patch.object(CheckoutOrderMapper, 'find_by_merchant_id', mocker.AsyncMock(return_value=[]))

    await ListMerchantOrdersAction(
        merchant_id=merchant_id,
        limit=30,
        created_gte=datetime(2022, 2, 1),
        created_lt=datetime(2022, 2, 2),
    ).run()

    storage_mock.assert_awaited_once_with(
        merchant_id,
        limit=30,
        created_gte=datetime(2022, 2, 1),
        created_lt=datetime(2022, 2, 2),
    )
