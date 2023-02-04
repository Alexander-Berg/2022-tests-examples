from datetime import datetime, timezone
from uuid import UUID, uuid4

import pytest

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.core.actions.order.create import CreateOrderAction
from pay.bill_payments.bill_payments.storage.entities.enums import OrderStatus
from pay.bill_payments.bill_payments.storage.entities.order import Order

URI = '/api/v1/orders'


@pytest.fixture
def example_post_data():
    return dict(
        bill_ids=['870d746c-1215-4679-8abe-5be3708a037a', '92c1d690-6aa1-4ccf-94c0-22e8d43170a0'],
    )


@pytest.fixture
def example_order():
    return Order(
        uid=777000,
        status=OrderStatus.NEW,
        order_id=UUID('ed8716c9-7f68-4f4d-83a2-7c8882002ce3'),
        created=datetime(2020, 12, 30, 14, 31, 59, tzinfo=timezone.utc),
        updated=datetime(2020, 12, 30, 14, 31, 59, tzinfo=timezone.utc),
    )


@pytest.fixture(autouse=True)
def mock_authentication(mocker, auth_user):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=auth_user))


async def check_schema_error(response, expected_error):
    assert_that(response.status, equal_to(400))
    assert_that(
        await response.json(),
        equal_to(
            {
                'status': 'fail',
                'code': 400,
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': expected_error,
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_post_orders_response(app, mock_action, example_order, example_post_data):
    mock_action(CreateOrderAction, example_order)

    resp = await app.post(URI, json=example_post_data)

    assert_that(resp.status, equal_to(201))
    assert_that(
        await resp.json(),
        equal_to(
            {
                'status': 'success',
                'code': 201,
                'data': {
                    'order': {
                        'status': 'NEW',
                        'order_id': 'ed8716c9-7f68-4f4d-83a2-7c8882002ce3',
                        'created': '2020-12-30T14:31:59+00:00',
                        'updated': '2020-12-30T14:31:59+00:00',
                    }
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_post_orders_validates_uuids(app, example_post_data):
    example_post_data['bill_ids'] = ['not-uuid']
    resp = await app.post(URI, json=example_post_data)

    await check_schema_error(resp, {'bill_ids': {'0': ['Not a valid UUID.']}})


@pytest.mark.asyncio
async def test_post_orders_validates_required_fields(app, example_post_data):
    del example_post_data['bill_ids']
    resp = await app.post(URI, json=example_post_data)

    await check_schema_error(resp, {'bill_ids': ['Missing data for required field.']})


@pytest.mark.parametrize('length', (0, 101))
@pytest.mark.asyncio
async def test_post_orders_validates_bill_ids_length(app, length, example_post_data):
    example_post_data['bill_ids'] = [str(uuid4())] * length

    resp = await app.post(URI, json=example_post_data)

    await check_schema_error(resp, {'bill_ids': ['Length must be between 1 and 100.']})


@pytest.mark.asyncio
async def test_post_orders_calls_action(app, mock_action, auth_user, example_post_data, example_order):
    mock = mock_action(CreateOrderAction, example_order)

    await app.post(URI, json=example_post_data)

    expected_call_params = {
        'uid': auth_user.uid,
        'bill_ids': [UUID('870d746c-1215-4679-8abe-5be3708a037a'), UUID('92c1d690-6aa1-4ccf-94c0-22e8d43170a0')],
    }
    mock.assert_called_once_with(**expected_call_params)
