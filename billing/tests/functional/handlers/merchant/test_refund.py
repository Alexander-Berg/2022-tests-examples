from decimal import Decimal

import pytest

from sendr_pytest.matchers import equal_to

from hamcrest import anything, assert_that, has_entries, match_equality, not_none

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import TransactionStatus
from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import is_datetime_with_tz, is_uuid4


@pytest.fixture
async def authenticate_client(stored_merchant, storage):
    await storage.merchant_key.create(MerchantKey.create(merchant_id=stored_merchant.merchant_id, key='UNITTEST_KEY'))

    def _authenticate_client(test_client):
        test_client.session.headers['Authorization'] = f'Api-Key {stored_merchant.merchant_id.hex}.UNITTEST_KEY'

    return _authenticate_client


@pytest.mark.asyncio
async def test_unauthorized(public_app, yandex_pay_plus_settings):
    r = await public_app.get(
        '/api/merchant/v1/orders/test-order/refund',
    )
    data = await r.json()

    assert_that(r.status, equal_to(401))
    assert_that(data, equal_to({
        'reasonCode': 'AUTHENTICATION_ERROR',
        'reason': 'Authorization header is missing',
        'status': 'fail',
    }))


@pytest.mark.asyncio
async def test_full_refund(
    public_app,
    params,
    order_id,
    stored_merchant,
    checkout_order,
    authenticate_client,
):
    authenticate_client(public_app)
    r = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/refund',
        json=params,
    )
    data = await r.json()

    assert_that(r.status, equal_to(200))
    expected_response = {
        'operationId': is_uuid4,
        'reason': None,
        'status': 'PENDING',
        'orderId': checkout_order.order_id,
        'externalOperationId': 'merchant-capture-id',
        'operationType': 'REFUND',
        'params': {},
        'created': is_datetime_with_tz,
        'updated': is_datetime_with_tz,
        'amount': '50.00',
    }
    assert_that(data, equal_to({'data': {'operation': expected_response}, 'status': 'success', 'code': 200}))


@pytest.mark.asyncio
async def test_overlimit(public_app, params, order_id, authenticate_client):
    params['orderAmount'] = '-10.00'
    params['refundAmount'] = '60.00'
    params['cart']['total']['amount'] = '0.00'

    authenticate_client(public_app)
    r = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/refund',
        json=params,
    )
    data = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        data,
        equal_to({
            'reasonCode': 'REFUND_AMOUNT_TOO_LARGE',
            'details': {
                'max_amount': '50.00',
                'requested_amount': '60.00'
            },
            'status': 'fail',
        })
    )


@pytest.mark.asyncio
async def test_expectation_failed(public_app, params, order_id, authenticate_client):
    params['orderAmount'] = '10.00'
    params['refundAmount'] = '50.00'
    params['cart']['total']['amount'] = '0.00'

    authenticate_client(public_app)
    r = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/refund',
        json=params,
    )
    data = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        data,
        equal_to({
            'reasonCode': 'ORDER_AMOUNT_EXPECTATION_FAILED',
            'details': {
                'current_order_amount': '50.00',
                'expected_order_amount': '10.00',
                'refund_amount': '50.00',
                'description': match_equality(anything()),
            },
            'reason': match_equality(not_none()),
            'status': 'fail',
        })
    )


@pytest.mark.asyncio
async def test_refund_race(public_app, params, order_id, authenticate_client):
    authenticate_client(public_app)

    r1 = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/refund',
        json=params,
    )

    params['externalOperationId'] = 'another-merchant-capture-id'
    r2 = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/refund',
        json=params,
    )

    data1 = await r1.json()
    data2 = await r2.json()

    assert_that(r1.status, equal_to(200))
    assert_that(r2.status, equal_to(409))

    assert_that(
        data1['data']['operation'],
        has_entries({
            'operationType': 'REFUND',
            'status': 'PENDING'
        })
    )

    assert_that(
        data2,
        has_entries({
            'status': 'fail',
            'reasonCode': 'ANOTHER_OPERATION_IN_PROGRESS'
        })
    )


@pytest.fixture
def params():
    return {
        'externalOperationId': 'merchant-capture-id',
        'orderAmount': '0.00',
        'refundAmount': '50.00',
        'cart': {'total': {'amount': '0.00'}, 'items': []},
    }


@pytest.fixture
def order_id(rands):
    return rands()


@pytest.fixture(autouse=True)
async def checkout_order(storage, stored_checkout_order, order_id):
    stored_checkout_order.order_id = order_id
    stored_checkout_order.order_amount = stored_checkout_order.authorize_amount = Decimal('50.00')
    return await storage.checkout_order.save(stored_checkout_order)


@pytest.fixture(autouse=True)
async def transaction(storage, stored_transaction):
    stored_transaction.status = TransactionStatus.CHARGED
    return await storage.transaction.save(stored_transaction)
