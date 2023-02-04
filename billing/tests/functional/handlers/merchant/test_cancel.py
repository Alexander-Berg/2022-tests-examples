from decimal import Decimal

import pytest

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that, match_equality, not_none

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
        '/api/merchant/v1/orders/test-order/cancel',
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
    order_id,
    public_app,
    params,
    storage,
    stored_merchant,
    stored_checkout_order,
    authenticate_client,
):
    authenticate_client(public_app)
    r = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/cancel',
        json=params,
    )
    data = await r.json()

    assert_that(r.status, equal_to(200))
    expected_response = {
        'operationId': is_uuid4,
        'reason': None,
        'status': 'PENDING',
        'orderId': stored_checkout_order.order_id,
        'externalOperationId': 'merchant-cancel-id',
        'operationType': 'VOID',
        'params': {'cancel_reason': 'the-reason'},
        'created': is_datetime_with_tz,
        'updated': is_datetime_with_tz,
        'amount': '50.00',
    }
    assert_that(data, equal_to({'data': {'operation': expected_response}, 'status': 'success', 'code': 200}))


@pytest.mark.asyncio
async def test_wrong_status(public_app, order_id, storage, transaction, params, authenticate_client):
    transaction.status = TransactionStatus.CHARGED
    await storage.transaction.save(transaction)
    authenticate_client(public_app)

    r = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/cancel',
        json=params,
    )
    data = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(data, equal_to({
        'status': 'fail',
        'reasonCode': 'INVALID_PAYMENT_STATUS',
        'reason': match_equality(not_none()),
        'details': {'expected': ['AUTHORIZED'], 'actual': 'CAPTURED'}
    }))


@pytest.mark.asyncio
async def test_no_reason(public_app, order_id, authenticate_client):
    authenticate_client(public_app)

    r = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/cancel',
        json={},
    )
    data = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(data, equal_to({
        'details': {'reason': ['Missing data for required field.']},
        'reasonCode': 'BAD_REQUEST',
        'status': 'fail',
    }))


@pytest.fixture
def order_id(rands):
    return rands()


@pytest.fixture
def params():
    return {
        'externalOperationId': 'merchant-cancel-id',
        'reason': 'the-reason',
    }


@pytest.fixture(autouse=True)
async def checkout_order(storage, stored_checkout_order, order_id):
    stored_checkout_order.order_id = order_id
    stored_checkout_order.order_amount = stored_checkout_order.authorize_amount = Decimal('50.00')
    return await storage.checkout_order.save(stored_checkout_order)


@pytest.fixture(autouse=True)
async def transaction(storage, stored_transaction):
    stored_transaction.status = TransactionStatus.AUTHORIZED
    return await storage.transaction.save(stored_transaction)
