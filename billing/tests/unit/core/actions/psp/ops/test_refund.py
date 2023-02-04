from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.entities.operation import OperationStatus, OperationType
from pay.lib.entities.order import PaymentStatus

from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action, spy_action  # noqa

from hamcrest import assert_that, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.generate_receipt import GenerateReceiptAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.ops.refund import RefundAction, RefundAsyncableAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.status import UpdateTransactionStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.processing import ResultFailed, ResultRefunded
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CoreInvalidPaymentStatusError,
    UnknownProcessingResultError,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.processings.mock import MockProcessing
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration, IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import TransactionStatus


class TestRefundAsyncableAction:
    @pytest.mark.asyncio
    async def test_runs_refund_action(self, mock_action, transaction, operation):  # noqa
        mock = mock_action(RefundAction)

        await RefundAsyncableAction(
            transaction_id=transaction.transaction_id, operation_id=operation.operation_id
        ).run()

        mock.assert_run_once_with(transaction=transaction, operation=operation)


class TestRefundAction:
    @pytest.mark.parametrize('partial', (True, False))
    @pytest.mark.asyncio
    async def test_calls_processing(self, mock_processing_method, params, partial, mock_action, entity_receipt):  # noqa
        mock = mock_processing_method()
        mock_action(GenerateReceiptAction, entity_receipt)

        order = params['transaction'].order
        order.capture_amount = order.order_amount
        params['operation'].amount = Decimal('1') if partial else order.capture_amount

        await RefundAction(**params).run()

        mock.assert_awaited_once_with(
            amount=params['operation'].amount,
            receipt=entity_receipt if partial else None,
        )

    @pytest.mark.asyncio
    async def test_bad_transaction_status(self, params, storage):
        params['transaction'].status = TransactionStatus.FAILED
        params['transaction'] = await storage.transaction.save(params['transaction'])

        with pytest.raises(CoreInvalidPaymentStatusError):
            await RefundAction(**params).run()

    @pytest.mark.parametrize(
        'partial, actual_status, expected_status',
        [
            (False, TransactionStatus.CHARGED, TransactionStatus.REFUNDED),
            (False, TransactionStatus.PARTIALLY_REFUNDED, TransactionStatus.REFUNDED),
            (True, TransactionStatus.CHARGED, TransactionStatus.PARTIALLY_REFUNDED),
            (True, TransactionStatus.PARTIALLY_REFUNDED, TransactionStatus.PARTIALLY_REFUNDED),
        ],
    )
    @pytest.mark.asyncio
    async def test_success(
        self, mock_processing_method, params, transaction, storage, partial, actual_status, expected_status
    ):
        mock_processing_method()

        params['transaction'].status = actual_status
        params['operation'].amount = Decimal(1) if partial else params['transaction'].order.order_amount

        await RefundAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'status': expected_status,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_failure__transaction_status(self, mock_processing_method, params, transaction, storage):
        mock_processing_method(ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR'))

        await RefundAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'status': TransactionStatus.CHARGED,
                    'reason': None,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_unknown_result(self, mock_processing_method, params, transaction, storage):
        mock_processing_method(object())

        with pytest.raises(UnknownProcessingResultError):
            await RefundAction(**params).run()

    @pytest.mark.parametrize(
        'processing_refund_result, expected_operation_update, expected_order_status',
        (
            pytest.param(
                ResultRefunded(),
                {'status': OperationStatus.SUCCESS},
                PaymentStatus.REFUNDED,
                id='refunded',
            ),
            pytest.param(
                ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR'),
                {'status': OperationStatus.FAIL, 'reason': 'raw:LOVELY_PAYTURE_ERROR'},
                PaymentStatus.PENDING,
                id='failed',
            ),
        ),
    )
    @pytest.mark.asyncio
    async def test_updates_operation_and_order(
        self,
        mock_processing_method,
        params,
        operation,
        processing_refund_result,
        expected_operation_update,
        expected_order_status,
    ):
        mock_processing_method(processing_refund_result)

        result = await RefundAction(**params, save=False).run()

        assert_that(result.operation, equal_to(replace(operation, **expected_operation_update)))
        assert_that(result.transaction.order.payment_status, equal_to(expected_order_status))

    @pytest.mark.parametrize(
        'processing_refund_result, should_call',
        (
            pytest.param(ResultRefunded(), True, id='refunded'),
            pytest.param(ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR'), False, id='failed'),
        ),
    )
    @pytest.mark.asyncio
    async def test_calls_update_transaction_status(
        self,
        spy_action,  # noqa
        mock_processing_method,
        params,
        transaction,
        processing_refund_result,
        should_call,
    ):
        mock_processing_method(processing_refund_result)
        spy = spy_action(UpdateTransactionStatusAction)

        await RefundAction(**params).run()

        if should_call:
            spy.assert_run_once_with(transaction, status=TransactionStatus.REFUNDED, save=False)
        else:
            spy.assert_not_run()

    @pytest.fixture
    def params(self, integration, transaction, operation):
        return {
            'integration': integration,
            'transaction': transaction,
            'operation': operation,
        }

    @pytest.fixture
    def mock_processing_method(self, mocker):
        def _mock_processing_method(result=ResultRefunded()):
            return mocker.patch.object(
                MockProcessing,
                'refund',
                mocker.AsyncMock(return_value=result),
            )

        return _mock_processing_method


@pytest.fixture
async def transaction(storage, stored_checkout_order, integration, entity_transaction):
    transaction = await storage.transaction.create(
        replace(
            entity_transaction,
            checkout_order_id=stored_checkout_order.checkout_order_id,
            status=TransactionStatus.CHARGED,
            integration_id=integration.integration_id,
        )
    )
    transaction.order = stored_checkout_order
    return transaction


@pytest.fixture
async def operation(storage, stored_operation):
    return await storage.order_operation.save(
        replace(
            stored_operation,
            operation_type=OperationType.REFUND,
            amount=Decimal('1212.12'),
            reason=None,
        )
    )


@pytest.fixture
async def integration(storage, stored_unittest_psp, stored_merchant):
    return await storage.integration.create(
        Integration(
            merchant_id=stored_merchant.merchant_id,
            psp_id=stored_unittest_psp.psp_id,
            status=IntegrationStatus.DEPLOYED,
            creds=Integration.encrypt_creds(
                {
                    'key': 'the-key',
                    'password': 'the-password',
                    'gateway_merchant_id': 'gw-mid',
                }
            ),
        )
    )
