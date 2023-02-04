import uuid
from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.entities.enums import AuthMethod, CardNetwork
from pay.lib.entities.operation import OperationStatus, OperationType
from pay.lib.entities.order import PaymentMethodType, PaymentStatus
from pay.lib.interactions.yandex_pay.entities import CreatePaymentTokenResponse, PaymentMethodInfo

from sendr_pytest.matchers import equal_to
from sendr_pytest.mocks import explain_call_asserts, mock_action, spy_action

from hamcrest import assert_that, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.classic import (
    CreateCorrespondingClassicOrderAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.generate_receipt import GenerateReceiptAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.psp.ops.hold import HoldAction, HoldAsyncableAction
from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.status import UpdateTransactionStatusAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.processing import (
    HoldResult3DSV1ChallengeRequired,
    HoldResult3DSV2ChallengeRequired,
    HoldResult3DSV2FingerprintRequired,
    HoldResultAuthorized,
    ResultFailed,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    CoreInvalidPaymentStatusError,
    UnknownProcessingResultError,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.processings.mock import MockProcessing
from billing.yandex_pay_plus.yandex_pay_plus.interactions.yandex_pay import YandexPayClient
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration, IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    ThreeDS2MethodData,
    ThreeDSV1ChallengeRequest,
    ThreeDSV2ChallengeRequest,
    Transaction,
    TransactionData,
    TransactionStatus,
    TransactionThreeDSData,
)

fixtures_dummy_use_for_linters = [explain_call_asserts, mock_action, spy_action]


class TestHoldAsyncableAction:
    @pytest.mark.asyncio
    async def test_runs_hold_action(self, mock_action, transaction, operation):
        mock = mock_action(HoldAction)

        await HoldAsyncableAction(transaction_id=transaction.transaction_id, operation_id=operation.operation_id).run()

        mock.assert_run_once_with(transaction=transaction, operation=operation)


class TestHoldAction:
    @pytest.mark.asyncio
    async def test_calls_create_token(
        self,
        mock_processing_method,
        mock_payment_token,
        params,
        checkout_order,
        stored_unittest_psp,
        transaction,
    ):
        mock_processing_method()

        await HoldAction(**params).run()

        mock_payment_token.assert_awaited_once_with(
            uid=checkout_order.uid,
            gateway_merchant_id='gw-mid',
            psp_external_id=stored_unittest_psp.psp_external_id,
            auth_methods=[AuthMethod.PAN_ONLY],
            amount=checkout_order.authorize_amount,
            currency=checkout_order.currency_code,
            card_id=transaction.card_id,
        )

    @pytest.mark.asyncio
    async def test_calls_processing(self, mock_processing_method, params, entity_receipt, mock_action):
        mock = mock_processing_method()
        mock_action(GenerateReceiptAction, entity_receipt)

        await HoldAction(**params).run()

        order = params['transaction'].order
        mock.assert_awaited_once_with(
            payment_token='yp-token',
            is_checkout=order.chargeable,
            amount=params['operation'].amount,
            receipt=entity_receipt,
        )

    @pytest.mark.asyncio
    async def test_bad_transaction_status(self, params, storage):
        params['transaction'].status = TransactionStatus.THREEDS_CHALLENGE
        params['transaction'] = await storage.transaction.save(params['transaction'])

        with pytest.raises(CoreInvalidPaymentStatusError):
            await HoldAction(**params).run()

    @pytest.mark.asyncio
    async def test_creates_corresponding_classic_order(
        self,
        checkout_order,
        integration,
        mock_processing_method,
        mock_create_corresponding_order_action,
        params,
    ):
        mock_processing_method()

        await HoldAction(**params).run()

        mock_create_corresponding_order_action.assert_run_once_with(
            checkout_order=checkout_order,
            card_id='card-x1234',
            message_id='yp-msgid',
            psp_id=integration.psp_id,
        )

    @pytest.mark.asyncio
    async def test_success(self, mock_processing_method, params, transaction, storage):
        mock_processing_method()

        await HoldAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                status=TransactionStatus.AUTHORIZED,
                message_id='yp-msgid',
                card_last4='1234',
                card_network=CardNetwork.MIR,
                data=has_properties(
                    psp_transaction_id='psp-transaction-id',
                ),
            ),
        )

    @pytest.mark.asyncio
    async def test_threeds_v1(self, mock_processing_method, params, transaction, storage):
        mock_processing_method(
            HoldResult3DSV1ChallengeRequired(
                acs_url='https://acs.test', pareq='pareq', md='md', psp_transaction_id='psp-transaction-id'
            )
        )

        await HoldAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'status': TransactionStatus.THREEDS_CHALLENGE,
                    'data': has_properties(
                        {
                            'threeds': has_properties(
                                {
                                    'challenge_request': ThreeDSV1ChallengeRequest(
                                        acs_url='https://acs.test',
                                        pareq='pareq',
                                        md='md',
                                    )
                                }
                            ),
                            'psp_transaction_id': 'psp-transaction-id',
                        }
                    ),
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_threeds_v2_fingerprinting(self, mock_processing_method, params, transaction, storage):
        mock_processing_method(
            HoldResult3DSV2FingerprintRequired(
                threeds_server_transaction_id='trans-id',
                threeds_server_fingerprint_url='https://3dsserver.test',
                threeds_acs_fingerprint_url='https://acsserver.test',
                threeds_acs_fingerprint_url_params={'pa': 'ram'},
                psp_transaction_id='psp-transaction-id',
            )
        )

        await HoldAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'status': TransactionStatus.FINGERPRINTING,
                    'data': has_properties(
                        {
                            'threeds': has_properties(
                                {
                                    'threeds2_method_data': ThreeDS2MethodData(
                                        threeds_method_notification_url='https://method-result.test',
                                        threeds_server_transaction_id='trans-id',
                                        threeds_server_method_url='https://3dsserver.test',
                                        threeds_acs_method_url='https://acsserver.test',
                                        threeds_acs_method_params={'pa': 'ram'},
                                    )
                                }
                            ),
                            'psp_transaction_id': 'psp-transaction-id',
                        }
                    ),
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_threeds_v2_challenge(self, mock_processing_method, params, transaction, storage):
        mock_processing_method(
            HoldResult3DSV2ChallengeRequired(
                acs_url='https://acs.test',
                creq='creq',
                session_data='sessdata',
                psp_transaction_id='psp-transaction-id',
            )
        )

        await HoldAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'status': TransactionStatus.THREEDS_CHALLENGE,
                    'data': has_properties(
                        {
                            'threeds': has_properties(
                                {
                                    'challenge_request': ThreeDSV2ChallengeRequest(
                                        acs_url='https://acs.test',
                                        creq='creq',
                                        session_data='sessdata',
                                    )
                                }
                            ),
                            'psp_transaction_id': 'psp-transaction-id',
                        }
                    ),
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_failure(self, mock_processing_method, params, transaction, storage):
        mock_processing_method(ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR'))

        await HoldAction(**params).run()

        assert_that(
            await storage.transaction.get(transaction.transaction_id),
            has_properties(
                {
                    'status': TransactionStatus.FAILED,
                    'reason': 'raw:LOVELY_PAYTURE_ERROR',
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_unknown_result(self, mock_processing_method, params, transaction, storage):
        mock_processing_method(object())

        with pytest.raises(UnknownProcessingResultError):
            await HoldAction(**params).run()

    @pytest.mark.parametrize(
        'processing_hold_result, expected_status',
        (
            pytest.param(HoldResultAuthorized(), TransactionStatus.AUTHORIZED, id='authorized'),
            pytest.param(
                HoldResult3DSV1ChallengeRequired(acs_url='https://acs.test', pareq='pareq', md='md'),
                TransactionStatus.THREEDS_CHALLENGE,
                id='3dsv1',
            ),
            pytest.param(
                HoldResult3DSV2ChallengeRequired(acs_url='https://acs.test', creq='creq', session_data='sessdata'),
                TransactionStatus.THREEDS_CHALLENGE,
                id='3dsv1',
            ),
            pytest.param(ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR'), TransactionStatus.FAILED, id='failed'),
        ),
    )
    @pytest.mark.asyncio
    async def test_calls_update_status(
        self,
        spy_action,
        mock_processing_method,
        params,
        transaction,
        processing_hold_result,
        expected_status,
    ):
        mock_processing_method(processing_hold_result)
        spy = spy_action(UpdateTransactionStatusAction)

        await HoldAction(**params).run()

        spy.assert_run_once_with(
            transaction,
            status=expected_status,
            save=False,
        )

    @pytest.mark.parametrize(
        'processing_hold_result, expected_operation_update, expected_order_status',
        (
            pytest.param(
                HoldResultAuthorized(),
                {'status': OperationStatus.SUCCESS},
                PaymentStatus.AUTHORIZED,
                id='authorized',
            ),
            pytest.param(
                HoldResult3DSV1ChallengeRequired(acs_url='https://acs.test', pareq='pareq', md='md'),
                {},
                PaymentStatus.PENDING,
                id='3dsv1',
            ),
            pytest.param(
                HoldResult3DSV2ChallengeRequired(acs_url='https://acs.test', creq='creq', session_data='sessdata'),
                {},
                PaymentStatus.PENDING,
                id='3dsv2',
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
        mock_processing_method,
        params,
        operation,
        processing_hold_result,
        expected_operation_update,
        expected_order_status,
    ):
        mock_processing_method(processing_hold_result)

        result = await HoldAction(**params, save=False).run()

        assert_that(result.operation, equal_to(replace(operation, **expected_operation_update)))
        assert_that(result.transaction.order.payment_status, equal_to(expected_order_status))

    @pytest.fixture
    def params(self, integration, transaction, operation):
        return {
            'integration': integration,
            'transaction': transaction,
            'operation': operation,
        }

    @pytest.fixture(autouse=True)
    def mock_create_corresponding_order_action(self, mock_action):
        return mock_action(CreateCorrespondingClassicOrderAction)

    @pytest.fixture(autouse=True)
    def mock_payment_token(self, mocker):
        return mocker.patch.object(
            YandexPayClient,
            'create_payment_token',
            mocker.AsyncMock(
                return_value=CreatePaymentTokenResponse(
                    payment_token='yp-token',
                    message_id='yp-msgid',
                    payment_method_info=PaymentMethodInfo(
                        card_last4='1234',
                        card_network=CardNetwork.MIR,
                    ),
                )
            ),
        )

    @pytest.fixture
    def mock_processing_method(self, mocker):
        def _mock_processing_method(result=HoldResultAuthorized(psp_transaction_id='psp-transaction-id')):
            return mocker.patch.object(
                MockProcessing,
                'hold',
                mocker.AsyncMock(return_value=result),
            )

        return _mock_processing_method


@pytest.fixture
async def checkout_order(storage, stored_checkout_order):
    return await storage.checkout_order.save(
        replace(
            stored_checkout_order,
            payment_method_type=PaymentMethodType.CARD,
            authorize_amount=Decimal('123.45'),
            order_amount=Decimal('123.46'),
        )
    )


@pytest.fixture
async def transaction(storage, checkout_order, integration, entity_threeds_authentication_request):
    transaction = await storage.transaction.create(
        Transaction(
            transaction_id=uuid.UUID('ac3a67e1-9df1-4612-bfdc-c82a3c549c14'),
            checkout_order_id=checkout_order.checkout_order_id,
            integration_id=integration.integration_id,
            status=TransactionStatus.NEW,
            card_id='card-x1234',
            data=TransactionData(
                user_ip='192.0.2.1',
                threeds=TransactionThreeDSData(
                    authentication_request=entity_threeds_authentication_request,
                    threeds2_method_data=ThreeDS2MethodData(
                        threeds_method_notification_url='https://method-result.test',
                    ),
                ),
            ),
            version=1,
        )
    )
    transaction.order = checkout_order
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
