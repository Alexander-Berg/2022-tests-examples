from dataclasses import replace
from uuid import uuid4

import pytest
from pay.lib.entities.operation import OperationType

from sendr_pytest.mocks import mock_action

from hamcrest import assert_that, equal_to, has_properties, match_equality

import billing.yandex_pay_plus.yandex_pay_plus.core.actions.threeds.method_result
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.ops.fingerprint import SubmitFingerprintingAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.threeds.method_result import ThreeDSMethodResultAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionStatus

_dummy_use_fixtures = [mock_action]


@pytest.mark.asyncio
async def test_returns_html(transaction):
    result = await ThreeDSMethodResultAction(transaction_id=transaction.transaction_id).run()

    assert_that(result, equal_to('the-html'))


@pytest.mark.asyncio
async def test_calls_submit_fingerprinting_action(transaction, mock_submit_fingerprint, operation):
    await ThreeDSMethodResultAction(transaction_id=transaction.transaction_id).run()

    mock_submit_fingerprint.assert_run_once_with(
        transaction=match_equality(
            has_properties(
                transaction_id=transaction.transaction_id,
            )
        ),
        operation=operation,
    )


@pytest.mark.asyncio
async def test_calls_render_postmessage_document(transaction, mock_render_html):
    await ThreeDSMethodResultAction(transaction_id=transaction.transaction_id).run()

    mock_render_html.assert_called_once_with(
        target_origin='https://test.pay.yandex.ru',
        post_message_data={'type': 'fingerprinting_complete', 'status': TransactionStatus.FAILED.name},
    )


@pytest.fixture
async def transaction(storage, stored_transaction):
    return await storage.transaction.save(
        replace(
            stored_transaction,
            status=TransactionStatus.AUTHORIZED,
        )
    )


@pytest.fixture(autouse=True)
async def operation(storage, transaction, entity_operation, stored_checkout_order):
    return await storage.order_operation.create(
        replace(
            entity_operation,
            operation_type=OperationType.AUTHORIZE,
            checkout_order_id=transaction.checkout_order_id,
            operation_id=uuid4(),
            merchant_id=stored_checkout_order.merchant_id,
        )
    )


@pytest.fixture(autouse=True)
def mock_submit_fingerprint(mock_action, mocker, transaction):
    return mock_action(
        SubmitFingerprintingAction,
        return_value=mocker.Mock(
            transaction=replace(
                transaction,
                status=TransactionStatus.FAILED,
            )
        )
    )


@pytest.fixture(autouse=True)
def mock_render_html(mocker):
    return mocker.patch.object(
        billing.yandex_pay_plus.yandex_pay_plus.core.actions.threeds.method_result,
        'render_postmessage_document',
        mocker.Mock(return_value='the-html'),
    )
