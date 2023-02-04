from datetime import datetime, timezone
from decimal import Decimal
from uuid import UUID

import pytest
from pay.lib.entities.operation import OperationStatus, OperationType

from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action  # noqa

from hamcrest import all_of, assert_that, greater_than_or_equal_to, less_than

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant.authenticate import AuthenticateMerchantAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.operation import (
    GetOrderOperationByExternalIdAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.order_operation import Operation


@pytest.mark.asyncio
async def test_calls_action(public_app, mock_get_operation):
    await public_app.get(
        '/api/merchant/v1/operations/ext-op-id',
    )

    mock_get_operation.assert_run_once_with(
        merchant_id=UUID('1a1d848a-aa27-4241-ab02-ad1f5306c79d'),
        external_operation_id='ext-op-id',
    )


@pytest.mark.asyncio
async def test_returned(
    public_app,
    expected_response,
):
    r = await public_app.get(
        '/api/merchant/v1/operations/ext-op-id',
    )

    assert_that(r.status, all_of(greater_than_or_equal_to(200), less_than(300)))
    response_data = await r.json()
    assert_that(response_data['data'], equal_to(expected_response))


@pytest.fixture(autouse=True)
def mock_get_operation(mock_action, operation):  # noqa
    return mock_action(GetOrderOperationByExternalIdAction, return_value=operation)


@pytest.fixture(autouse=True)
def mock_merchant_authorization(mock_action):  # noqa
    return mock_action(AuthenticateMerchantAction, UUID('1a1d848a-aa27-4241-ab02-ad1f5306c79d'))


@pytest.fixture
def operation(entity_cart):
    return ensure_all_fields(
        Operation,
        operation_id=UUID('3ead36ed-5999-4e7b-810e-590ca13bea22'),
        checkout_order_id=UUID('2f045cf2-f87a-4dfe-a14b-538f6f061254'),
        merchant_id=UUID('a99f9c5e-85e1-4f9f-b724-e3bdcc874524'),
        order_id='order-id',
        amount=Decimal('123.45'),
        operation_type=OperationType.CAPTURE,
        status=OperationStatus.PENDING,
        external_operation_id='external_operation_id',
        reason='reason',
        params={'pa': 'rams', 'sub': {'pa': 1}},
        cart=None,
        shipping_method=None,
        created=datetime(2020, 12, 1, 23, 59, 59, tzinfo=timezone.utc),
        updated=datetime(2020, 12, 1, 23, 59, 59, tzinfo=timezone.utc),
    )


@pytest.fixture
def expected_response():
    return {
        'operation': {
            'operationId': '3ead36ed-5999-4e7b-810e-590ca13bea22',
            'amount': '123.45',
            'orderId': 'order-id',
            'operationType': 'CAPTURE',
            'status': 'PENDING',
            'externalOperationId': 'external_operation_id',
            'reason': 'reason',
            'params': {'pa': 'rams', 'sub': {'pa': 1}},
            'created': '2020-12-01T23:59:59+00:00',
            'updated': '2020-12-01T23:59:59+00:00',
        }
    }
