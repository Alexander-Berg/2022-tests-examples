from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.core.actions.transaction.get import GetTransactionAction
from pay.bill_payments.bill_payments.storage.entities.enums import OrderStatus, TransactionStatus
from pay.bill_payments.bill_payments.storage.entities.order import Order
from pay.bill_payments.bill_payments.storage.entities.transaction import PayerData, Transaction


@pytest.mark.usefixtures('mock_sessionid_auth')
@pytest.mark.asyncio
async def test_get_transaction_success(app, storage, mocker, auth_user, authenticate_client):
    authenticate_client(app)
    get_transaction_spy = mocker.spy(GetTransactionAction, '__init__')
    order = await storage.order.create(
        Order(
            uid=auth_user.uid,
            order_id=uuid4(),
            status=OrderStatus.NEW,
        )
    )
    transaction = await storage.transaction.create(
        Transaction(
            order_id=order.order_id,
            status=TransactionStatus.PAID,
            amount=100,
            external_payment_id='123',
            payer_data=PayerData(payer_full_name='test'),
        )
    )

    resp = await app.get(f'/api/v1/transactions/{transaction.transaction_id}')

    assert_that(resp.status, equal_to(200))
    assert_that(
        await resp.json(),
        equal_to(
            {
                'data': {
                    'transaction': {
                        'status': transaction.status.name,
                        'transaction_id': str(transaction.transaction_id),
                    }
                },
                'code': 200,
                'status': 'success',
            }
        ),
    )
    assert get_transaction_spy.call_args.kwargs['transaction_id'] == transaction.transaction_id


@pytest.mark.usefixtures('mock_sessionid_auth')
@pytest.mark.asyncio
async def test_transaction_should_be_found(app, authenticate_client):
    authenticate_client(app)
    resp = await app.get(f'/api/v1/transactions/{uuid4()}')

    assert_that(resp.status, equal_to(404))
    assert_that(
        await resp.json(),
        equal_to({'code': 404, 'status': 'fail', 'data': {'message': 'TRANSACTION_NOT_FOUND'}}),
    )
