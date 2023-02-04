import uuid
from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.entities.receipt import (
    AgentType,
    MarkQuantity,
    MeasureType,
    PaymentsOperator,
    PaymentSubjectType,
    PaymentType,
    Receipt,
    ReceiptItem,
    ReceiptItemQuantity,
    Supplier,
    TransferOperator,
)
from pay.lib.interactions.psp.rbs.client import AbstractRBSRestClient
from pay.lib.interactions.psp.rbs.entities import (
    AgentInfo,
    Cart,
    CustomerDetails,
    ItemAttributes,
    Order,
    OrderBundle,
    PaymentResult3DSV1,
    PaymentResult3DSV2Challenge,
    PaymentResult3DSV2Fingerprinting,
    PaymentResultSuccess,
    Position,
    Quantity,
    RBSAcquirer,
    RBSCredentials,
    RBSOrderStatus,
    RBSTaxType,
    RegisterResult,
    Tax,
)
from pay.lib.interactions.psp.rbs.exceptions import RBSDataError

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.entities.processing import (
    HoldResult3DSV1ChallengeRequired,
    HoldResult3DSV2ChallengeRequired,
    HoldResult3DSV2FingerprintRequired,
    HoldResultAuthorized,
    Result3DSV2ChallengeRequired,
    ResultAuthorized,
    ResultCancelled,
    ResultCleared,
    ResultFailed,
    ResultRefunded,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    AcquirerNotSupportedError,
    UnexpectedProcessingResponseError,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.processings.rbs import RBSProcessing
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration, IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    ThreeDS2MethodData,
    ThreeDSV1ChallengeResponse,
    ThreeDSV2ChallengeResponse,
    Transaction,
    TransactionData,
    TransactionStatus,
    TransactionThreeDSData,
)


@pytest.mark.asyncio
async def test_acquirer_not_supported(processing, yandex_pay_plus_settings):
    yandex_pay_plus_settings.RBS_MTS_BASE_URL = None
    with pytest.raises(AcquirerNotSupportedError):
        await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))


@pytest.mark.parametrize(
    'psp_external_id, acquirer, expected_url',
    (
        ('alfabank', None, 'https://alfa.test'),
        ('rbs', RBSAcquirer.MTS, 'https://mts.test'),
    ),
)
@pytest.mark.asyncio
async def test_url_for_each_acquirer(
    processing, yandex_pay_plus_settings, mocker, psp_external_id, acquirer, expected_url, psp, integration
):
    yandex_pay_plus_settings.RBS_ALFA_BANK_BASE_URL = 'https://alfa.test'
    yandex_pay_plus_settings.RBS_MTS_BASE_URL = 'https://mts.test'
    mock = mocker.patch.object(
        AbstractRBSRestClient,
        'deposit',
        mocker.AsyncMock(return_value=None),
    )
    psp.psp_external_id = psp_external_id
    integration.set_creds(integration.get_creds() | {'acquirer': acquirer.value if acquirer else None})

    await processing.clear(amount=Decimal('12.34'))

    assert_that(mock.call_args.kwargs['base_url'], equal_to(expected_url))


class TestHold:
    @pytest.mark.asyncio
    async def test_calls_register_pre_auth(
        self, processing, transaction, checkout_order, mock_register_pre_auth, make_mock_yandex_payment, expected_creds
    ):
        make_mock_yandex_payment()

        await processing.hold(payment_token='yp-token', amount=checkout_order.authorize_amount)

        mock_register_pre_auth.assert_awaited_once_with(
            base_url='https://web.rbsuat.com/mtsbank/',
            credentials=expected_creds,
            order_number=str(transaction.transaction_id),
            amount=checkout_order.authorize_amount,
            currency='XTS',
            user_ip='192.0.2.1',
            order_bundle=None,
        )

    @pytest.mark.asyncio
    async def test_receipt(self, processing, mock_register_pre_auth, entity_full_receipt, make_mock_yandex_payment):
        make_mock_yandex_payment()

        await processing.hold(
            payment_token='yp-token',
            amount=Decimal('12.34'),
            receipt=entity_full_receipt,
        )

        expected = OrderBundle(
            customer_details=CustomerDetails(
                full_name='fname',
                phone='phone',
                email='email',
            ),
            cart_items=Cart(
                items=[
                    Position(
                        position_id='p-1-10000',
                        item_code='p-1',
                        name='Full Product',
                        item_price=10000,
                        quantity=Quantity(value=1.5, measure=MeasureType.PIECE),
                        tax=Tax(tax_type=RBSTaxType.VAT_10, tax_sum=None),
                        item_attributes=ItemAttributes(
                            payment_method=PaymentType.FULL_PAYMENT,
                            payment_object=PaymentSubjectType.PAYMENT,
                            mark_quantity=MarkQuantity(numerator=1, denominator=3),
                            nomenclature='aefc6cdc2ce3223315f4f46585de5217c7c8799529285b9b',
                            agent_info=AgentInfo(
                                agent_type=AgentType.OTHER,
                                operation='operation',
                                phones=['+798700000222'],
                                transfer_operator=TransferOperator(
                                    inn='123456789',
                                    name='Transfer Operator Name',
                                    address='Transfer Operator Address',
                                    phones=['+798700000333'],
                                ),
                                payments_operator=PaymentsOperator(phones=['+79876543210', '+798700012345']),
                            ),
                            supplier_info=Supplier(
                                inn='123456789',
                                name='Supplier Name',
                                phones=['+798700000111'],
                            ),
                        ),
                    )
                ]
            ),
        )
        _, call_kwargs = mock_register_pre_auth.call_args_list[0]
        assert_that(call_kwargs['order_bundle'], equal_to(expected))

    @pytest.mark.asyncio
    async def test_calls_yandex_payment(
        self, processing, transaction, checkout_order, mock_register_pre_auth, make_mock_yandex_payment, expected_creds
    ):
        mock = make_mock_yandex_payment()

        await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        mock.assert_awaited_once_with(
            base_url='https://web.rbsuat.com/mtsbank/',
            credentials=expected_creds,
            payment_token='yp-token',
            order_id='new-rbs-order-id',
            threeds2_challenge_notification_url='https://challenge_notification_url.test',
            threeds2_method_notification_url='https://method-term-url.test',
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker, make_mock_yandex_payment):
        make_mock_yandex_payment()

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(result, equal_to(HoldResultAuthorized(psp_transaction_id='new-rbs-order-id')))

    @pytest.mark.asyncio
    async def test_threeds_v1(self, processing, mocker, make_mock_yandex_payment):
        make_mock_yandex_payment(PaymentResult3DSV1(acs_url='https://acs.test', pa_req='pareq'))

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(
            result,
            equal_to(
                HoldResult3DSV1ChallengeRequired(
                    acs_url='https://acs.test',
                    pareq='pareq',
                    md='new-rbs-order-id',
                    psp_transaction_id='new-rbs-order-id',
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_threeds_v2_fingerprinting_required(self, processing, mocker, make_mock_yandex_payment):
        make_mock_yandex_payment(
            PaymentResult3DSV2Fingerprinting(
                threeds_server_transaction_id='trans-id',
                three_ds_server_fingerprint_url='https://3dsfp.test',
                three_ds_acs_fingerprint_url='https://3ds-acs-fp.test',
                three_ds_acs_fingerprint_url_param_value='fp-url-param-value',
            )
        )

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(
            result,
            equal_to(
                HoldResult3DSV2FingerprintRequired(
                    threeds_server_transaction_id='trans-id',
                    threeds_server_fingerprint_url='https://3dsfp.test',
                    threeds_acs_fingerprint_url='https://3ds-acs-fp.test',
                    threeds_acs_fingerprint_url_params={
                        'threeDSMethodData': 'fp-url-param-value',
                    },
                    psp_transaction_id='new-rbs-order-id',
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_threeds_v2_challenge_required(self, processing, mocker, make_mock_yandex_payment):
        make_mock_yandex_payment(
            PaymentResult3DSV2Challenge(
                acs_url='https://acs.test',
                creq='packedCReq',
            )
        )

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(
            result,
            equal_to(
                HoldResult3DSV2ChallengeRequired(
                    acs_url='https://acs.test',
                    creq='packedCReq',
                    session_data='',
                    psp_transaction_id='new-rbs-order-id',
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractRBSRestClient,
            'yandex_payment',
            mocker.AsyncMock(
                side_effect=RBSDataError(
                    status_code=200, method='post', service='rbs', response_status='LOVELY_RBS_ERROR'
                ),
            ),
        )

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_RBS_ERROR')),
        )

    @pytest.fixture(autouse=True)
    def mock_register_pre_auth(self, mocker):
        return mocker.patch.object(
            AbstractRBSRestClient,
            'register_pre_auth',
            mocker.AsyncMock(
                return_value=RegisterResult(
                    order_id='new-rbs-order-id',
                )
            ),
        )

    @pytest.fixture
    def make_mock_yandex_payment(self, mocker):
        def _make_mock_yandex_payment(return_value=PaymentResultSuccess()):
            return mocker.patch.object(
                AbstractRBSRestClient,
                'yandex_payment',
                mocker.AsyncMock(return_value=return_value),
            )

        return _make_mock_yandex_payment


class TestSubmitFingerprinting:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, expected_creds):
        mock = mocker.patch.object(
            AbstractRBSRestClient,
            'yandex_payment',
            mocker.AsyncMock(return_value=PaymentResultSuccess()),
        )
        transaction.data.set_payment_token('ptoken')

        await processing.submit_fingerprinting()

        mock.assert_awaited_once_with(
            base_url='https://web.rbsuat.com/mtsbank/',
            credentials=expected_creds,
            payment_token='ptoken',
            order_id='psp-order-id',
            threeds2_challenge_notification_url='https://challenge_notification_url.test',
            threeds2_method_notification_url='https://method-term-url.test',
            threeds2_server_transaction_id='trans-id',
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker, transaction):
        mocker.patch.object(
            AbstractRBSRestClient,
            'yandex_payment',
            mocker.AsyncMock(return_value=PaymentResultSuccess()),
        )
        transaction.data.set_payment_token('ptoken')

        result = await processing.submit_fingerprinting()

        assert_that(result, equal_to(ResultAuthorized()))

    @pytest.mark.asyncio
    async def test_challenge_required(self, processing, mocker, transaction):
        mocker.patch.object(
            AbstractRBSRestClient,
            'yandex_payment',
            mocker.AsyncMock(
                return_value=PaymentResult3DSV2Challenge(
                    acs_url='https://acs.test',
                    creq='packedCReq',
                )
            ),
        )
        transaction.data.set_payment_token('ptoken')

        result = await processing.submit_fingerprinting()

        assert_that(
            result,
            equal_to(Result3DSV2ChallengeRequired(acs_url='https://acs.test', creq='packedCReq', session_data='')),
        )

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker, transaction):
        mocker.patch.object(
            AbstractRBSRestClient,
            'yandex_payment',
            mocker.AsyncMock(
                side_effect=RBSDataError(
                    status_code=200, method='post', service='rbs', response_status='LOVELY_RBS_ERROR'
                ),
            ),
        )
        transaction.data.set_payment_token('ptoken')

        result = await processing.submit_fingerprinting()

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_RBS_ERROR')),
        )


class TestSubmit3DSChallenge:
    @pytest.mark.asyncio
    async def test_calls_client_3dsv1(
        self, processing, make_mock_finish_3ds, make_mock_get_order, transaction, expected_creds
    ):
        mock = make_mock_finish_3ds()
        make_mock_get_order()
        processing.transaction.data.threeds.challenge_response = ThreeDSV1ChallengeResponse(pares='PaReS')

        await processing.submit_3ds()

        mock.assert_awaited_once_with(
            base_url='https://web.rbsuat.com/mtsbank/',
            credentials=expected_creds,
            md='psp-order-id',
            pa_res='PaReS',
        )

    @pytest.mark.asyncio
    async def test_calls_client_3dsv2(self, processing, mocker, make_mock_get_order, transaction, expected_creds):
        mock = mocker.patch.object(
            AbstractRBSRestClient,
            'finish_3ds_ver2',
            mocker.AsyncMock(return_value=None),
        )
        make_mock_get_order()
        processing.transaction.data.threeds.challenge_response = ThreeDSV2ChallengeResponse(cres=None)

        await processing.submit_3ds()

        mock.assert_awaited_once_with(
            base_url='https://web.rbsuat.com/mtsbank/',
            credentials=expected_creds,
            threeds_server_transaction_id='trans-id',
            order_id='psp-order-id',
        )

    @pytest.mark.parametrize('status', [RBSOrderStatus.AUTHORIZED, RBSOrderStatus.CLEARED])
    @pytest.mark.asyncio
    async def test_order_state_is_successful(
        self, processing, make_mock_finish_3ds, make_mock_get_order, status, rbs_order
    ):
        rbs_order.order_status = status
        make_mock_get_order(return_value=rbs_order)
        make_mock_finish_3ds()
        processing.transaction.data.threeds.challenge_response = ThreeDSV1ChallengeResponse(pares='PaReS')

        result = await processing.submit_3ds()

        assert_that(result, equal_to(ResultAuthorized()))

    @pytest.mark.asyncio
    async def test_order_state_is_bad(self, processing, make_mock_finish_3ds, make_mock_get_order, rbs_order):
        rbs_order.order_status = RBSOrderStatus.FAILED
        make_mock_get_order(return_value=rbs_order)
        make_mock_finish_3ds()
        processing.transaction.data.threeds.challenge_response = ThreeDSV1ChallengeResponse(pares='PaReS')

        result = await processing.submit_3ds()

        assert_that(result, equal_to(ResultFailed(reason='raw:action-code:1337')))

    @pytest.mark.parametrize(
        'status', set(RBSOrderStatus) - {RBSOrderStatus.AUTHORIZED, RBSOrderStatus.CLEARED, RBSOrderStatus.FAILED}
    )
    @pytest.mark.asyncio
    async def test_order_state_is_unexpected(
        self, processing, make_mock_finish_3ds, make_mock_get_order, status, rbs_order
    ):
        rbs_order.order_status = status
        make_mock_get_order(return_value=rbs_order)
        make_mock_finish_3ds()
        processing.transaction.data.threeds.challenge_response = ThreeDSV1ChallengeResponse(pares='PaReS')

        with pytest.raises(UnexpectedProcessingResponseError):
            await processing.submit_3ds()

    @pytest.mark.asyncio
    async def test_submit_3ds_failure(self, processing, make_mock_finish_3ds, make_mock_get_order):
        make_mock_finish_3ds(
            side_effect=RBSDataError(status_code=200, method='post', service='rbs', response_status='LOVELY_RBS_ERROR'),
        )
        make_mock_get_order()
        processing.transaction.data.threeds.challenge_response = ThreeDSV1ChallengeResponse(pares='PaReS')

        result = await processing.submit_3ds()

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_RBS_ERROR')),
        )

    @pytest.mark.asyncio
    async def test_get_order_failure(self, processing, make_mock_finish_3ds, make_mock_get_order):
        make_mock_finish_3ds()
        make_mock_get_order(
            side_effect=RBSDataError(status_code=200, method='post', service='rbs', response_status='LOVELY_RBS_ERROR'),
        )
        processing.transaction.data.threeds.challenge_response = ThreeDSV1ChallengeResponse(pares='PaReS')

        result = await processing.submit_3ds()

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_RBS_ERROR')),
        )

    @pytest.fixture
    def rbs_order(self):
        return Order(order_status=RBSOrderStatus.AUTHORIZED, action_code=1337)

    @pytest.fixture
    def make_mock_finish_3ds(self, mocker):
        def _make_mock_finish_3ds(return_value=None, side_effect=None):
            return mocker.patch.object(
                AbstractRBSRestClient,
                'finish_3ds',
                mocker.AsyncMock(return_value=return_value, side_effect=side_effect),
            )

        return _make_mock_finish_3ds

    @pytest.fixture
    def make_mock_get_order(self, mocker, rbs_order):
        def _make_mock_get_order(return_value=rbs_order, side_effect=None):
            return mocker.patch.object(
                AbstractRBSRestClient,
                'get_order',
                mocker.AsyncMock(return_value=return_value, side_effect=side_effect),
            )

        return _make_mock_get_order


class TestCancel:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, checkout_order, expected_creds):
        mock = mocker.patch.object(
            AbstractRBSRestClient,
            'reverse',
            mocker.AsyncMock(return_value=None),
        )

        await processing.cancel()

        mock.assert_awaited_once_with(
            base_url='https://web.rbsuat.com/mtsbank/',
            credentials=expected_creds,
            order_id='psp-order-id',
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractRBSRestClient,
            'reverse',
            mocker.AsyncMock(return_value=None),
        )

        result = await processing.cancel()

        assert_that(result, equal_to(ResultCancelled()))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractRBSRestClient,
            'reverse',
            mocker.AsyncMock(
                side_effect=RBSDataError(
                    status_code=200, method='post', service='rbs', response_status='LOVELY_RBS_ERROR'
                ),
            ),
        )

        result = await processing.cancel()

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_RBS_ERROR')),
        )


class TestClear:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, checkout_order, expected_creds, entity_receipt):
        mock = mocker.patch.object(
            AbstractRBSRestClient,
            'deposit',
            mocker.AsyncMock(return_value=None),
        )

        await processing.clear(amount=checkout_order.capture_amount, receipt=entity_receipt)

        mock.assert_awaited_once_with(
            base_url='https://web.rbsuat.com/mtsbank/',
            credentials=expected_creds,
            order_id='psp-order-id',
            amount=checkout_order.capture_amount,
            currency='XTS',
            deposit_items=Cart(
                items=[
                    Position(
                        position_id='p-1-1000',
                        name='Product',
                        quantity=Quantity(value=1.0, measure=MeasureType.PIECE),
                        item_code='p-1',
                        item_price=1000,
                        tax=Tax(tax_type=RBSTaxType.VAT_20),
                    ),
                ],
            ),
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractRBSRestClient,
            'deposit',
            mocker.AsyncMock(return_value=None),
        )

        result = await processing.clear(amount=Decimal('12.34'))

        assert_that(result, equal_to(ResultCleared()))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractRBSRestClient,
            'deposit',
            mocker.AsyncMock(
                side_effect=RBSDataError(
                    status_code=200, method='post', service='rbs', response_status='LOVELY_RBS_ERROR'
                ),
            ),
        )

        result = await processing.clear(amount=Decimal('12.34'))

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_RBS_ERROR')),
        )


class TestRefund:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, expected_creds, checkout_order):
        mock = mocker.patch.object(
            AbstractRBSRestClient,
            'refund',
            mocker.AsyncMock(return_value=None),
        )
        item = checkout_order.cart.items[0]
        receipt = Receipt(
            items=[
                ReceiptItem(
                    title=item.title,
                    discounted_unit_price=item.discounted_unit_price,
                    quantity=ReceiptItemQuantity(count=Decimal('1')),
                    tax=item.receipt.tax,
                    product_id=item.product_id,
                )
            ]
        )

        await processing.refund(amount=Decimal('111.11'), receipt=receipt)

        mock.assert_awaited_once_with(
            base_url='https://web.rbsuat.com/mtsbank/',
            credentials=expected_creds,
            order_id='psp-order-id',
            amount=Decimal('111.11'),
            currency='XTS',
            refund_items=Cart(
                items=[
                    Position(
                        position_id='oduct-1-4200',
                        name='Awesome Product',
                        quantity=Quantity(value=9.0, measure=MeasureType.PIECE),
                        item_code='product-1',
                        item_price=4200,
                        tax=Tax(tax_type=RBSTaxType.VAT_20, tax_sum=None),
                    ),
                    Position(
                        position_id='oduct-2-2100',
                        name='Awesome Product 2',
                        quantity=Quantity(value=1.0, measure=MeasureType.PIECE),
                        item_code='product-2',
                        item_price=2100,
                        tax=Tax(tax_type=RBSTaxType.VAT_20, tax_sum=None),
                    ),
                ]
            ),
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractRBSRestClient,
            'refund',
            mocker.AsyncMock(return_value=None),
        )

        result = await processing.refund(amount=Decimal('123.45'))

        assert_that(result, equal_to(ResultRefunded()))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractRBSRestClient,
            'refund',
            mocker.AsyncMock(
                side_effect=RBSDataError(
                    status_code=200, method='post', service='rbs', response_status='LOVELY_RBS_ERROR'
                ),
            ),
        )

        result = await processing.refund(amount=Decimal('123.45'))

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_RBS_ERROR')),
        )


@pytest.fixture
def processing(core_context, integration, checkout_order, transaction, psp):
    return RBSProcessing(
        context=core_context, integration=integration, order=checkout_order, transaction=transaction, psp=psp
    )


@pytest.fixture
async def transaction(storage, psp, merchant, checkout_order, entity_threeds_authentication_request, integration):
    return await storage.transaction.create(
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
                    challenge_response=ThreeDSV1ChallengeResponse(pares='PaReS'),
                    threeds2_method_data=ThreeDS2MethodData(
                        threeds_method_notification_url='https://method-term-url.test',
                        threeds_server_transaction_id='trans-id',
                        threeds_server_method_url='method-url',
                    ),
                ),
                psp_transaction_id='psp-order-id',
            ),
            version=1,
        )
    )


@pytest.fixture
async def checkout_order(storage, entity_checkout_order):
    return await storage.checkout_order.create(
        replace(
            entity_checkout_order,
            capture_amount=Decimal('123.45'),
            authorize_amount=Decimal('543.21'),
            billing_contact=entity_checkout_order.shipping_contact,
        )
    )


@pytest.fixture
async def integration(storage, psp, merchant):
    return await storage.integration.create(
        Integration(
            merchant_id=merchant.merchant_id,
            psp_id=psp.psp_id,
            status=IntegrationStatus.DEPLOYED,
            creds=Integration.encrypt_creds(
                {
                    'username': 'the-username',
                    'password': 'the-password',
                    'gateway_merchant_id': 'the-gwid',
                    'acquirer': 'MTS',
                }
            ),
        )
    )


@pytest.fixture
def expected_creds():
    return RBSCredentials(
        username='the-username',
        password='the-password',
        gateway_merchant_id='the-gwid',
        acquirer=RBSAcquirer.MTS,
    )


@pytest.fixture
async def merchant(storage, psp):
    return await storage.merchant.create(Merchant(name='merchant-name'))


@pytest.fixture
async def psp(storage):
    return await storage.psp.create(
        PSP(psp_external_id='rbs', psp_id=uuid.UUID('e51fb2a2-12fc-4f88-9c59-81cb8a395880'))
    )
