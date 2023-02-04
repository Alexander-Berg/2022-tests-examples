from decimal import Decimal

import pytest

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import TransactionStatus
from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import is_datetime_with_tz, is_uuid4


@pytest.fixture
async def authenticate_client(stored_merchant, storage):
    await storage.merchant_key.create(MerchantKey.create(merchant_id=stored_merchant.merchant_id, key='UNITTEST_KEY'))

    def _authenticate_client(test_client):
        test_client.session.headers['Authorization'] = f'Api-Key {stored_merchant.merchant_id.hex}.UNITTEST_KEY'

    return _authenticate_client


@pytest.fixture
def params():
    return {
        'externalOperationId': 'merchant-capture-id',
        'orderAmount': '40.00',
        'cart': {'total': {'amount': '40.00'}, 'items': []},
    }


@pytest.mark.asyncio
async def test_unauthorized(public_app, yandex_pay_plus_settings):
    r = await public_app.get(
        '/api/merchant/v1/orders/test-order/capture',
    )
    data = await r.json()

    assert_that(r.status, equal_to(401))
    assert_that(data, equal_to({
        'reasonCode': 'AUTHENTICATION_ERROR',
        'reason': 'Authorization header is missing',
        'status': 'fail',
    }))


@pytest.mark.asyncio
async def test_success(
    rands,
    public_app,
    params,
    storage,
    stored_merchant,
    stored_checkout_order,
    stored_transaction,
    authenticate_client,
):
    order_id = rands()
    stored_checkout_order.order_id = order_id
    stored_checkout_order.authorize_amount = stored_checkout_order.order_amount = Decimal('50')
    stored_checkout_order = await storage.checkout_order.save(stored_checkout_order)
    stored_transaction.status = TransactionStatus.AUTHORIZED
    stored_transaction = await storage.transaction.save(stored_transaction)

    authenticate_client(public_app)
    r = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/capture',
        json=params,
    )
    data = await r.json()

    assert_that(r.status, equal_to(200))
    expected_response = {
        'operationId': is_uuid4,
        'reason': None,
        'status': 'PENDING',
        'orderId': stored_checkout_order.order_id,
        'externalOperationId': 'merchant-capture-id',
        'operationType': 'CAPTURE',
        'params': {},
        'created': is_datetime_with_tz,
        'updated': is_datetime_with_tz,
        'amount': '40.00',
    }
    assert_that(data, equal_to({'data': {'operation': expected_response}, 'status': 'success', 'code': 200}))


@pytest.mark.asyncio
async def test_overlimit(
    rands,
    public_app,
    params,
    storage,
    stored_merchant,
    stored_checkout_order,
    stored_transaction,
    authenticate_client,
):
    order_id = rands()
    stored_checkout_order.order_id = order_id
    stored_checkout_order.order_amount = stored_checkout_order.authorize_amount = Decimal('100.00')
    stored_checkout_order = await storage.checkout_order.save(stored_checkout_order)
    stored_transaction.status = TransactionStatus.AUTHORIZED
    stored_transaction = await storage.transaction.save(stored_transaction)

    params['orderAmount'] = '200.00'
    params['cart']['total']['amount'] = '200.00'

    authenticate_client(public_app)
    r = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/capture',
        json=params,
    )
    data = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        data,
        equal_to({
            'reasonCode': 'CAPTURE_AMOUNT_TOO_LARGE',
            'details': {
                'max_amount': '100.00',
                'requested_amount': '200.00'
            },
            'status': 'fail',
        })
    )
