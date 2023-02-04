from dataclasses import replace
from uuid import uuid4

import pytest
from pay.lib.entities.shipping import ShippingMethodType

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that, has_entries, match_equality

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import StorageShippingMethod
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey


@pytest.fixture
async def merchant(storage):
    return await storage.merchant.create(
        Merchant(
            merchant_id=uuid4(),
            name='Merchant',
        )
    )


@pytest.fixture
async def authentication(merchant, storage):
    await storage.merchant_key.create(MerchantKey.create(merchant_id=merchant.merchant_id, key='key'))
    return {'headers': {'Authorization': f'Api-Key {merchant.merchant_id.hex}.key'}}


@pytest.fixture
async def checkout_order(storage, merchant, entity_checkout_order, entity_courier_option):
    entity_checkout_order.merchant_id = merchant.merchant_id
    entity_checkout_order.shipping_method = StorageShippingMethod(
        method_type=ShippingMethodType.COURIER,
        courier_option=entity_courier_option,
    )
    return await storage.checkout_order.create(entity_checkout_order)


@pytest.fixture
async def order_operation(storage, checkout_order, entity_operation):
    return await storage.order_operation.create(
        replace(
            entity_operation,
            checkout_order_id=checkout_order.checkout_order_id,
            merchant_id=checkout_order.merchant_id,
        )
    )


@pytest.mark.asyncio
async def test_unauthorized(public_app, yandex_pay_plus_settings):
    r = await public_app.get(
        '/api/merchant/v1/orders/test-order',
    )
    data = await r.json()

    assert_that(r.status, equal_to(401))
    assert_that(
        data,
        equal_to(
            {
                'reasonCode': 'AUTHENTICATION_ERROR',
                'reason': 'Authorization header is missing',
                'status': 'fail',
            }
        ),
    )


@pytest.mark.asyncio
async def test_get_order(public_app, merchant, checkout_order, order_operation, authentication):
    r = await public_app.get(
        f'/api/merchant/v1/orders/{checkout_order.order_id}',
        **authentication,
    )
    data = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        data['data'],
        equal_to(
            {
                'order': match_equality(
                    has_entries({
                        'paymentStatus': 'PENDING',
                        'currencyCode': 'XTS',
                        'orderAmount': '123.45',
                        'merchantId': str(merchant.merchant_id),
                        'metadata': 'mdata',
                        'orderId': checkout_order.order_id,
                        'cart': has_entries({
                            'total': {'label': None, 'amount': '441.00'},
                        }),
                    })
                ),
                'operations': [
                    match_equality(
                        has_entries({
                            'operationId': str(order_operation.operation_id),
                            'operationType': 'AUTHORIZE',
                        })
                    )
                ],
                'delivery': None,
            },
        ),
    )
