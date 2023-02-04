import uuid
from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.interactions.psp.uniteller.client import AbstractUnitellerAPIClient
from pay.lib.interactions.psp.uniteller.entities import (
    ChargeResult,
    Cheque,
    ChequeLine,
    ChequeLineAgent,
    ChequeLineProduct,
    ChequePayment,
    Customer,
    PaymentSubjectType,
    PaymentType,
    RefundResult,
    TaxMode,
    ThreeDSV1Params,
    ThreeDSV1Result,
    ThreeDSV2Params,
    ThreeDSV2Result,
    UnitellerCredentials,
    UnitellerPaymentKind,
    UnitellerPaymentType,
    UnitellerTaxType,
)
from pay.lib.interactions.psp.uniteller.exceptions import UnitellerDataError

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.entities.processing import (
    HoldResult3DSV1ChallengeRequired,
    HoldResult3DSV2ChallengeRequired,
    HoldResultAuthorized,
    ResultAuthorized,
    ResultCancelled,
    ResultCleared,
    ResultFailed,
    ResultRefunded,
)
from billing.yandex_pay_plus.yandex_pay_plus.core.processings.uniteller import UnitellerProcessing
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import StorageContact
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration, IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    ThreeDSV1ChallengeRequest,
    ThreeDSV1ChallengeResponse,
    ThreeDSV2ChallengeRequest,
    ThreeDSV2ChallengeResponse,
    Transaction,
    TransactionData,
    TransactionStatus,
    TransactionThreeDSData,
)


class TestHold:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, checkout_order):
        mock = mocker.patch.object(
            AbstractUnitellerAPIClient,
            'charge',
            mocker.AsyncMock(return_value=ChargeResult(result=0, auth_code='A1111', rrn='B2222')),
        )

        await processing.hold(payment_token='yp-token', amount=checkout_order.order_amount)

        mock.assert_awaited_once_with(
            credentials=UnitellerCredentials(
                login='the-login', password='the-password', gateway_merchant_id='the-gwid'
            ),
            order_id='5c31932e-9954-437b-9202-d6572aa76192',
            payment_token='yp-token',
            amount=checkout_order.order_amount,
            user_ip='192.0.2.1',
            threeds_authentication_request=transaction.data.threeds.authentication_request,
            cheque=None,
            hold=True,
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'charge',
            mocker.AsyncMock(return_value=ChargeResult(result=0, auth_code='A1111', rrn='B2222')),
        )

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(result, equal_to(HoldResultAuthorized(rrn='B2222')))

    @pytest.mark.asyncio
    async def test_threeds_v1(self, processing, mocker):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'charge',
            mocker.AsyncMock(
                return_value=ThreeDSV1Result(
                    result=0,
                    redirect_form='<thanks-for-nothing/>',
                    redirect_params=ThreeDSV1Params(
                        acs_url='https://acs.test', pa_req='pareq', md='md', term_url='https://thanks-for-nothing.test'
                    ),
                )
            ),
        )

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(
            result, equal_to(HoldResult3DSV1ChallengeRequired(acs_url='https://acs.test', pareq='pareq', md='md'))
        )

    @pytest.mark.asyncio
    async def test_threeds_v2(self, processing, mocker):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'charge',
            mocker.AsyncMock(
                return_value=ThreeDSV2Result(
                    result=0,
                    redirect_params=ThreeDSV2Params(
                        acs_url='https://acs.test', creq='creq', threeds_session_data='sessdata'
                    ),
                )
            ),
        )

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(
            result,
            equal_to(
                HoldResult3DSV2ChallengeRequired(acs_url='https://acs.test', creq='creq', session_data='sessdata')
            ),
        )

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'charge',
            mocker.AsyncMock(
                side_effect=UnitellerDataError(
                    status_code=200, method='post', service='uniteller', response_status='LOVELY_UNITELLER_ERROR'
                ),
            ),
        )

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_UNITELLER_ERROR')),
        )


class TestSubmit3DSChallenge:
    @pytest.mark.parametrize(
        'challenge_response, challenge_request',
        (
            pytest.param(
                ThreeDSV1ChallengeResponse(pares='the-res'),
                ThreeDSV1ChallengeRequest(acs_url='https://test.', pareq='pa-req', md='the-md'),
                id='3dsv1',
            ),
            pytest.param(
                ThreeDSV2ChallengeResponse(cres='the-res'),
                ThreeDSV2ChallengeRequest(acs_url='https://test.', creq='creq', session_data='the-md'),
                id='3dsv2',
            ),
        ),
    )
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, challenge_response, challenge_request):
        mock = mocker.patch.object(
            AbstractUnitellerAPIClient,
            'finish_3ds',
            mocker.AsyncMock(return_value=ChargeResult(result=0, auth_code='A1111', rrn='B2222')),
        )
        processing.transaction.data.threeds.challenge_response = challenge_response
        processing.transaction.data.threeds.challenge_request = challenge_request

        await processing.submit_3ds()

        mock.assert_awaited_once_with(
            credentials=UnitellerCredentials(
                login='the-login', password='the-password', gateway_merchant_id='the-gwid'
            ),
            pa_res='the-res',
            payment_attempt_id='the-md',
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'finish_3ds',
            mocker.AsyncMock(return_value=ChargeResult(result=0, auth_code='A1111', rrn='B2222')),
        )

        result = await processing.submit_3ds()

        assert_that(result, equal_to(ResultAuthorized(rrn='B2222')))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'finish_3ds',
            mocker.AsyncMock(
                side_effect=UnitellerDataError(
                    status_code=200, method='post', service='uniteller', response_status='LOVELY_UNITELLER_ERROR'
                ),
            ),
        )

        result = await processing.submit_3ds()

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_UNITELLER_ERROR')),
        )


class TestCancel:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, checkout_order, refund_result):
        mock = mocker.patch.object(
            AbstractUnitellerAPIClient,
            'refund',
            mocker.AsyncMock(return_value=refund_result),
        )

        await processing.cancel()

        mock.assert_awaited_once_with(
            credentials=UnitellerCredentials(
                login='the-login', password='the-password', gateway_merchant_id='the-gwid'
            ),
            order_id='5c31932e-9954-437b-9202-d6572aa76192',
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker, refund_result):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'refund',
            mocker.AsyncMock(return_value=refund_result),
        )

        result = await processing.cancel()

        assert_that(result, equal_to(ResultCancelled()))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'refund',
            mocker.AsyncMock(
                side_effect=UnitellerDataError(
                    status_code=200, method='post', service='uniteller', response_status='LOVELY_UNITELLER_ERROR'
                ),
            ),
        )

        result = await processing.cancel()

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_UNITELLER_ERROR')),
        )


class TestClear:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, checkout_order):
        mock = mocker.patch.object(
            AbstractUnitellerAPIClient,
            'confirm',
            mocker.AsyncMock(return_value=None),
        )

        await processing.clear(amount=checkout_order.capture_amount)

        mock.assert_awaited_once_with(
            credentials=UnitellerCredentials(
                login='the-login', password='the-password', gateway_merchant_id='the-gwid'
            ),
            order_id='5c31932e-9954-437b-9202-d6572aa76192',
            amount=checkout_order.capture_amount,
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'confirm',
            mocker.AsyncMock(return_value=None),
        )

        result = await processing.clear(amount=Decimal('12.34'))

        assert_that(result, equal_to(ResultCleared()))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'confirm',
            mocker.AsyncMock(
                side_effect=UnitellerDataError(
                    status_code=200, method='post', service='uniteller', response_status='LOVELY_UNITELLER_ERROR'
                ),
            ),
        )

        result = await processing.clear(amount=Decimal('12.34'))

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_UNITELLER_ERROR')),
        )


class TestRefund:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, refund_result):
        mock = mocker.patch.object(
            AbstractUnitellerAPIClient,
            'refund',
            mocker.AsyncMock(return_value=refund_result),
        )

        await processing.refund(amount=Decimal('123.45'))

        mock.assert_awaited_once_with(
            credentials=UnitellerCredentials(
                login='the-login', password='the-password', gateway_merchant_id='the-gwid'
            ),
            order_id='5c31932e-9954-437b-9202-d6572aa76192',
            amount=Decimal('123.45'),
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker, refund_result):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'refund',
            mocker.AsyncMock(return_value=refund_result),
        )

        result = await processing.refund(amount=Decimal('123.45'))

        assert_that(result, equal_to(ResultRefunded()))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractUnitellerAPIClient,
            'refund',
            mocker.AsyncMock(
                side_effect=UnitellerDataError(
                    status_code=200, method='post', service='uniteller', response_status='LOVELY_UNITELLER_ERROR'
                ),
            ),
        )

        result = await processing.refund(amount=Decimal('123.45'))

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_UNITELLER_ERROR')),
        )


class TestCheque:
    @pytest.fixture
    def mock_client(self, mocker):
        return mocker.patch.object(
            AbstractUnitellerAPIClient,
            'charge',
            mocker.AsyncMock(return_value=ChargeResult(result=0, auth_code='A1111', rrn='B2222')),
        )

    @pytest.mark.asyncio
    async def test_cheque_passed_to_client(self, processing, mock_client, transaction, entity_receipt):
        amount = Decimal('10.00')
        await processing.hold(
            payment_token='yp-token',
            amount=amount,
            receipt=entity_receipt,
        )

        _, call_kwargs = mock_client.call_args_list[0]
        assert_that(
            call_kwargs['cheque'],
            equal_to(
                Cheque(
                    total=float(amount),
                    taxmode=TaxMode.COMMON,
                    lines=[
                        ChequeLine(
                            name='Product',
                            qty=1.0,
                            price=float(amount),
                            vat=UnitellerTaxType.VAT_20,
                            sum=float(amount),
                            product=ChequeLineProduct(kt='', coc='', ncd='', exc=None),
                        )
                    ],
                    payments=[
                        ChequePayment(
                            kind=UnitellerPaymentKind.CARD,
                            type=UnitellerPaymentType.BANK_CARD_OR_DIGITAL,
                            amount=float(amount),
                        )
                    ],
                    customer=Customer(email='email', phone='phone'),
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_full_receipt(self, processing, mock_client, transaction, checkout_order, entity_full_receipt):
        entity_full_receipt.billing_contact = StorageContact(first_name='name', phone='phone')
        amount = amount = Decimal('150.0')
        await processing.hold(
            amount=amount,
            payment_token='yp-token',
            receipt=entity_full_receipt,
        )

        _, call_kwargs = mock_client.call_args_list[0]
        assert_that(
            call_kwargs['cheque'],
            equal_to(
                Cheque(
                    total=float(amount),
                    taxmode=TaxMode.COMMON,
                    lines=[
                        ChequeLine(
                            name='Full Product',
                            qty=1.5,
                            price=100.0,
                            vat=UnitellerTaxType.VAT_10,
                            sum=150.0,
                            payattr=PaymentType.FULL_PAYMENT,
                            lineattr=PaymentSubjectType.PAYMENT,
                            product=ChequeLineProduct(
                                kt='aefc6cdc2ce3223315f4f46585de5217c7c8799529285b9b', coc='', ncd='', exc=9.99
                            ),
                            agent=ChequeLineAgent(
                                agentattr='2',
                                agentphone='+798700000222',
                                accopphone='+79876543210 +798700012345',
                                opphone='+798700000333',
                                opname='Transfer Operator Name',
                                opinn='123456789',
                                opaddress='Transfer Operator Address',
                                operation='operation',
                                suppliername='Supplier Name',
                                supplierinn='123456789',
                                supplierphone='+798700000111',
                            ),
                        )
                    ],
                    payments=[
                        ChequePayment(
                            kind=UnitellerPaymentKind.CARD,
                            type=UnitellerPaymentType.BANK_CARD_OR_DIGITAL,
                            amount=float(amount),
                        )
                    ],
                    customer=Customer(email='email', phone='phone'),
                )
            ),
        )


@pytest.fixture
def processing(core_context, integration, checkout_order, transaction, psp):
    return UnitellerProcessing(
        context=core_context, integration=integration, order=checkout_order, transaction=transaction, psp=psp
    )


@pytest.fixture
async def transaction(storage, psp, merchant, checkout_order, entity_threeds_authentication_request, integration):
    return await storage.transaction.create(
        Transaction(
            transaction_id=uuid.UUID('5c31932e-9954-437b-9202-d6572aa76192'),
            checkout_order_id=checkout_order.checkout_order_id,
            integration_id=integration.integration_id,
            status=TransactionStatus.NEW,
            card_id='card-x1234',
            data=TransactionData(
                user_ip='192.0.2.1',
                threeds=TransactionThreeDSData(
                    authentication_request=entity_threeds_authentication_request,
                    challenge_request=ThreeDSV1ChallengeRequest(acs_url='https://acs.test', pareq='pareq', md='md'),
                    challenge_response=ThreeDSV1ChallengeResponse(pares='PaReS'),
                ),
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
                {'login': 'the-login', 'password': 'the-password', 'gateway_merchant_id': 'the-gwid'}
            ),
        )
    )


@pytest.fixture
async def merchant(stored_merchant):
    return stored_merchant


@pytest.fixture
async def psp(storage):
    return await storage.psp.create(
        PSP(psp_external_id='uniteller', psp_id=uuid.UUID('41e5206f-3587-47e2-bca5-e07305ae911c'))
    )


@pytest.fixture
def refund_result():
    return RefundResult(status='Canceled', error_code='', order_number='12', total='10.00', response_code='AS000')
