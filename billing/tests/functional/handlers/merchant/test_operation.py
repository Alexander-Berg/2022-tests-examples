from decimal import Decimal

import pytest

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that, has_entries

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant_key import MerchantKey
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import TransactionStatus


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
    operation_id,
):
    authenticate_client(public_app)
    r = await public_app.post(
        f'/api/merchant/v1/orders/{order_id}/cancel',
        json=params,
        raise_for_status=True,
    )

    r = await public_app.get(
        f'/api/merchant/v1/operations/{operation_id}',
        json=params,
    )

    assert_that(r.status, equal_to(200))
    data = await r.json()
    expected_operation = {
        'status': 'PENDING',
        'externalOperationId': operation_id,
        'operationType': 'VOID',
    }
    assert_that(
        data,
        has_entries({
            'data': has_entries({
                'operation': has_entries(expected_operation)
            }),
        })
    )


@pytest.fixture
def order_id(rands):
    return rands()


@pytest.fixture
def operation_id(rands):
    return rands()


@pytest.fixture
def params(rands, operation_id):
    return {
        'externalOperationId': operation_id,
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
