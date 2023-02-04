from dataclasses import replace

import pytest
from pay.lib.entities.operation import OperationStatus, OperationType
from pay.lib.entities.order import PaymentStatus

from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action, spy_action  # noqa

from hamcrest import assert_that, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.ops.cancel import CancelAction, CancelAsyncableAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.status import UpdateTransactionStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.processing import ResultCancelled, ResultFailed
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CoreInvalidPaymentStatusError,
    UnknownProcessingResultError,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.processings.mock import MockProcessing
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration, IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import TransactionStatus


class TestCancelAsyncableAction:
    @pytest.mark.asyncio
    async def test_runs_cancel_action(self, mock_action, transaction, operation):  # noqa
        mock = mock_action(CancelAction)

        await CancelAsyncableAction(
            transaction_id=transaction.transaction_id, operation_id=operation.operation_id
        ).run()

        mock.assert_run_once_with(transaction=transaction, operation=operation)


class TestCancelAction:
    @pytest.mark.asyncio
    async def test_calls_processing(self, mock_processing_method, params):
        mock = mock_processing_method()

        await CancelAction(**params).run()

        mock.assert_awaited_once_with()

    @pytest.mark.asyncio
    async def test_bad_transaction_status(self, mock_processing_method, params, storage):
        mock_processing_method()
        params['transaction'].status = TransactionStatus.CHARGED
        params['transaction'] = await storage.transaction.save(params['transaction'])

        with pytest.raises(CoreInvalidPaymentStatusError):
            await CancelAction(**params).run()

    @pytest.mark.asyncio
    async def test_success(self, mock_processing_method, params, transaction, storage):
        mock_processing_method()

        await CancelAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'status': TransactionStatus.VOIDED,
                    'reason': CancelAction.DEFAULT_REASON,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_success_with_reason(self, mock_processing_method, params, transaction, storage):
        mock_processing_method()
        params['operation'].params['cancel_reason'] = 'the-reason'

        await CancelAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'status': TransactionStatus.VOIDED,
                    'reason': 'the-reason',
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_failure(self, mock_processing_method, params, transaction, storage):
        mock_processing_method(ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR'))

        await CancelAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'reason': None,
                    'status': TransactionStatus.AUTHORIZED,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_unknown_result(self, mock_processing_method, params, transaction, storage):
        mock_processing_method(object())

        with pytest.raises(UnknownProcessingResultError):
            await CancelAction(**params).run()

    @pytest.mark.parametrize(
        'processing_cancel_result, expected_status',
        (pytest.param(ResultCancelled(), TransactionStatus.VOIDED, id='cancelled'),),
    )
    @pytest.mark.asyncio
    async def test_calls_update_status(
        self,
        spy_action,  # noqa
        mock_processing_method,
        params,
        transaction,
        storage,
        processing_cancel_result,
        expected_status,
    ):
        mock_processing_method(processing_cancel_result)
        spy = spy_action(UpdateTransactionStatusAction)

        await CancelAction(**params).run()

        spy.assert_run_once_with(
            transaction,
            status=expected_status,
            save=False,
        )

    @pytest.mark.parametrize(
        'processing_cancel_result, expected_operation_update, expected_order_status',
        (
            pytest.param(ResultCancelled(), {'status': OperationStatus.SUCCESS}, PaymentStatus.VOIDED, id='canceled'),
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
        processing_cancel_result,
        expected_operation_update,
        expected_order_status,
    ):
        mock_processing_method(processing_cancel_result)

        result = await CancelAction(**params, save=False).run()

        assert_that(result.operation, equal_to(replace(operation, **expected_operation_update)))
        assert_that(result.transaction.order.payment_status, equal_to(expected_order_status))

    @pytest.fixture
    def params(self, integration, transaction, operation):
        return {
            'integration': integration,
            'transaction': transaction,
            'operation': operation,
        }

    @pytest.fixture
    def mock_processing_method(self, mocker):
        def _mock_processing_method(result=ResultCancelled()):
            return mocker.patch.object(
                MockProcessing,
                'cancel',
                mocker.AsyncMock(return_value=result),
            )

        return _mock_processing_method


@pytest.fixture
async def transaction(storage, stored_checkout_order, integration, entity_transaction):
    transaction = await storage.transaction.create(
        replace(
            entity_transaction,
            checkout_order_id=stored_checkout_order.checkout_order_id,
            status=TransactionStatus.AUTHORIZED,
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
            operation_type=OperationType.VOID,
            reason=None,
            params={'cancel_reason': None},
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
