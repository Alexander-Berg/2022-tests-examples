import uuid
from dataclasses import replace

import pytest
from pay.lib.entities.operation import OperationStatus, OperationType
from pay.lib.entities.order import PaymentStatus

from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, spy_action

from hamcrest import assert_that, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.ops.fingerprint import SubmitFingerprintingAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.status import UpdateTransactionStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.processing import (
    Result3DSV2ChallengeRequired,
    ResultAuthorized,
    ResultFailed,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CoreInvalidPaymentStatusError,
    UnknownProcessingResultError,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.processings.mock import MockProcessing
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration, IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    Transaction,
    TransactionData,
    TransactionStatus,
    TransactionThreeDSData,
)

_dummy_fixture_use = [explain_call_asserts, spy_action]


class TestSubmitFingerprintingAction:
    @pytest.mark.asyncio
    async def test_calls_processing(self, make_processing_method_mock, params):
        mock = make_processing_method_mock()

        await SubmitFingerprintingAction(**params).run()

        mock.assert_awaited_once_with()

    @pytest.mark.asyncio
    async def test_bad_transaction_status(self, params, storage):
        params['transaction'].status = TransactionStatus.AUTHORIZED
        params['transaction'] = await storage.transaction.save(params['transaction'])

        with pytest.raises(CoreInvalidPaymentStatusError):
            await SubmitFingerprintingAction(**params).run()

    @pytest.mark.asyncio
    async def test_success(self, make_processing_method_mock, params, transaction, storage):
        make_processing_method_mock()

        await SubmitFingerprintingAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'status': TransactionStatus.AUTHORIZED,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_failure(self, make_processing_method_mock, params, transaction, storage):
        make_processing_method_mock(ResultFailed(reason='raw:LOVELY_UNITTEST_ERROR'))

        await SubmitFingerprintingAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'status': TransactionStatus.FAILED,
                    'reason': 'raw:LOVELY_UNITTEST_ERROR',
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_unknown_result(self, make_processing_method_mock, params, transaction, storage):
        make_processing_method_mock(object())

        with pytest.raises(UnknownProcessingResultError):
            await SubmitFingerprintingAction(**params).run()

    @pytest.mark.parametrize(
        'processing_submit_fingerprint_result, expected_status',
        (
            pytest.param(ResultAuthorized(), TransactionStatus.AUTHORIZED, id='authorized'),
            pytest.param(
                Result3DSV2ChallengeRequired(acs_url='', creq='', session_data=''),
                TransactionStatus.THREEDS_CHALLENGE,
                id='challenge-required',
            ),
            pytest.param(ResultFailed(reason='raw:LOVELY_UNITTEST_ERROR'), TransactionStatus.FAILED, id='failed'),
        ),
    )
    @pytest.mark.asyncio
    async def test_calls_update_status(
        self,
        spy_action,
        make_processing_method_mock,
        params,
        transaction,
        storage,
        processing_submit_fingerprint_result,
        expected_status,
    ):
        make_processing_method_mock(processing_submit_fingerprint_result)
        spy = spy_action(UpdateTransactionStatusAction)

        await SubmitFingerprintingAction(**params).run()

        spy.assert_run_once_with(
            transaction,
            status=expected_status,
            save=False,
        )

    @pytest.mark.parametrize(
        'processing_submit_fingerprint_result, expected_operation_update, expected_order_status',
        (
            pytest.param(
                ResultAuthorized(),
                {'status': OperationStatus.SUCCESS},
                PaymentStatus.AUTHORIZED,
                id='authorized',
            ),
            pytest.param(
                ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR'),
                {'status': OperationStatus.FAIL, 'reason': 'raw:LOVELY_PAYTURE_ERROR'},
                PaymentStatus.FAILED,
                id='failed',
            ),
        ),
    )
    @pytest.mark.asyncio
    async def test_updates_operation_and_order(
        self,
        make_processing_method_mock,
        params,
        operation,
        processing_submit_fingerprint_result,
        expected_operation_update,
        expected_order_status,
    ):
        make_processing_method_mock(processing_submit_fingerprint_result)

        result = await SubmitFingerprintingAction(**params, save=False).run()

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
    def make_processing_method_mock(self, mocker):
        def _make_processing_method_mock(result=ResultAuthorized()):
            return mocker.patch.object(
                MockProcessing,
                'submit_fingerprinting',
                mocker.AsyncMock(return_value=result),
            )

        return _make_processing_method_mock


@pytest.fixture
async def transaction(storage, stored_checkout_order, integration, entity_threeds_authentication_request):
    transaction = await storage.transaction.create(
        Transaction(
            transaction_id=uuid.UUID('ac3a67e1-9df1-4612-bfdc-c82a3c549c14'),
            checkout_order_id=stored_checkout_order.checkout_order_id,
            integration_id=integration.integration_id,
            status=TransactionStatus.FINGERPRINTING,
            card_id='card-x1234',
            data=TransactionData(
                user_ip='192.0.2.1',
                threeds=TransactionThreeDSData(authentication_request=entity_threeds_authentication_request),
            ),
            version=1,
        )
    )
    transaction.order = stored_checkout_order
    return transaction


@pytest.fixture
async def operation(storage, stored_operation):
    return await storage.order_operation.save(
        replace(
            stored_operation,
            operation_type=OperationType.AUTHORIZE,
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
