import pytest

from hamcrest import assert_that, equal_to


@pytest.mark.asyncio
async def test_unauthorized(public_app, yandex_pay_plus_settings):
    r = await public_app.post(
        '/api/public/v1/transactions/e590845f-8534-4b3b-b83d-2656f16519bc',
    )
    data = await r.json()

    assert_that(r.status, equal_to(401))
    assert_that(data, equal_to({'code': 401, 'status': 'fail', 'data': {'message': 'MISSING_CREDENTIALS'}}))


@pytest.mark.asyncio
async def test_get_transaction_view_for_fresh_card_transaction(
    public_app,
    yandex_pay_plus_settings,
    authenticate_client,
    create_order,
    create_transaction,
    create_integration,
    storage,
):
    authenticate_client(public_app)
    order = await create_order()
    await create_integration()
    checkout_order_id = order['checkout_order_id']
    transaction = await create_transaction(checkout_order_id)
    transaction_id = transaction['transaction_id']

    r = await public_app.get(
        f'/api/public/v1/transactions/{transaction_id}',
        headers={'x-pay-session-id': 'sessid-123'},
    )

    assert_that(r.status, equal_to(200))
    response = await r.json()
    assert_that(
        response['data'],
        equal_to(
            {
                'transaction': {
                    'transaction_id': transaction_id,
                    'status': 'NEW',
                    'version': 1,
                    'action': None,
                    'action_url': None,
                    'reason': None,
                }
            }
        )
    )


@pytest.mark.asyncio
async def test_get_transaction_view_for_fresh_split_transaction(
    public_app,
    yandex_pay_plus_settings,
    authenticate_client,
    create_order,
    create_transaction,
    create_integration,
    storage,
):
    authenticate_client(public_app)
    order = await create_order(payment_method={'method_type': 'SPLIT'})
    await create_integration()
    checkout_order_id = order['checkout_order_id']
    transaction = await create_transaction(checkout_order_id)
    transaction_id = transaction['transaction_id']

    r = await public_app.get(
        f'/api/public/v1/transactions/{transaction_id}',
        headers={'x-pay-session-id': 'sessid-123'},
    )

    assert_that(r.status, equal_to(200))
    response = await r.json()
    assert_that(
        response['data'],
        equal_to(
            {
                'transaction': {
                    'transaction_id': transaction_id,
                    'status': 'NEW',
                    'version': 1,
                    'action': 'SPLIT_IFRAME',
                    'action_url': 'https://split-checkout-url.test',
                    'reason': None,
                }
            }
        )
    )
