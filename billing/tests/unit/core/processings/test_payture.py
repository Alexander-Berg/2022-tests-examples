import uuid
from dataclasses import replace
from decimal import Decimal

import pytest
from pay.lib.interactions.psp.payture.client import AbstractPaytureAPIClient
from pay.lib.interactions.psp.payture.entities import (
    AddInfo,
    AgentInfo,
    Block3DSV1Required,
    Block3DSV2Required,
    BlockSuccess,
    ChargeResult,
    Cheque,
    ChequePosition,
    PaytureAPICredentials,
    RefundResult,
    SupplierInfo,
    UnblockResult,
)
from pay.lib.interactions.psp.payture.exceptions import PaytureDataError

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
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import BillingContactRequiredError
from billing.yandex_pay_plus.yandex_pay_plus.core.processings.payture import PaytureProcessing
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.checkout_order import StorageContact
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration, IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    ThreeDSV1ChallengeResponse,
    ThreeDSV2ChallengeResponse,
    Transaction,
    TransactionData,
    TransactionStatus,
    TransactionThreeDSData,
)

MINIMAL_CHEQUE = Cheque(
    customer_contact='email',
    positions=[ChequePosition(quantity=1.0, price=10.0, tax=1, text='Product')],
)


class TestHold:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, checkout_order, entity_receipt):
        mock = mocker.patch.object(
            AbstractPaytureAPIClient,
            'mobile_block',
            mocker.AsyncMock(return_value=BlockSuccess(AddInfo(auth_code='A1111', ref_number='B2222'))),
        )

        await processing.hold(
            payment_token='yp-token',
            amount=checkout_order.authorize_amount,
            receipt=entity_receipt,
        )

        mock.assert_awaited_once_with(
            credentials=PaytureAPICredentials(key='the-key', password='the-password', gateway_merchant_id='the-gwid'),
            order_id='ac3a67e1-9df1-4612-bfdc-c82a3c549c14',
            payment_token='yp-token',
            amount=checkout_order.authorize_amount,
            user_ip='192.0.2.1',
            threeds_authentication_request=transaction.data.threeds.authentication_request,
            is_checkout=checkout_order.chargeable,
            cheque=MINIMAL_CHEQUE,
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'mobile_block',
            mocker.AsyncMock(return_value=BlockSuccess(AddInfo(auth_code='A1111', ref_number='B2222'))),
        )

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(result, equal_to(HoldResultAuthorized(rrn='B2222')))

    @pytest.mark.asyncio
    async def test_threeds_v1(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'mobile_block',
            mocker.AsyncMock(
                return_value=Block3DSV1Required(acs_url='https://acs.test', pa_req='pareq', three_ds_key='md')
            ),
        )

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(
            result, equal_to(HoldResult3DSV1ChallengeRequired(acs_url='https://acs.test', pareq='pareq', md='md'))
        )

    @pytest.mark.asyncio
    async def test_threeds_v2(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'mobile_block',
            mocker.AsyncMock(
                return_value=Block3DSV2Required(
                    acs_url='https://acs.test', creq='creq', threeds_session_data='sessdata'
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
            AbstractPaytureAPIClient,
            'mobile_block',
            mocker.AsyncMock(
                side_effect=PaytureDataError(
                    status_code=200, method='post', service='payture', response_status='LOVELY_PAYTURE_ERROR'
                ),
            ),
        )

        result = await processing.hold(payment_token='yp-token', amount=Decimal('12.34'))

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR')),
        )


class TestSubmit3DSChallenge:
    @pytest.mark.parametrize(
        'challenge_response, pa_res, cres',
        (
            pytest.param(ThreeDSV1ChallengeResponse(pares='PaReS'), 'PaReS', None, id='3dsv1'),
            pytest.param(ThreeDSV2ChallengeResponse(cres='CrEs'), None, 'CrEs', id='3dsv2'),
        ),
    )
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, challenge_response, pa_res, cres):
        mock = mocker.patch.object(
            AbstractPaytureAPIClient,
            'block_3ds',
            mocker.AsyncMock(return_value=BlockSuccess(AddInfo(auth_code='A1111', ref_number='B2222'))),
        )
        processing.transaction.data.threeds.challenge_response = challenge_response

        await processing.submit_3ds()

        mock.assert_awaited_once_with(
            credentials=PaytureAPICredentials(key='the-key', password='the-password', gateway_merchant_id='the-gwid'),
            order_id='ac3a67e1-9df1-4612-bfdc-c82a3c549c14',
            pa_res=pa_res,
            cres=cres,
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'block_3ds',
            mocker.AsyncMock(return_value=BlockSuccess(AddInfo(auth_code='A1111', ref_number='B2222'))),
        )

        result = await processing.submit_3ds()

        assert_that(result, equal_to(ResultAuthorized(rrn='B2222')))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'block_3ds',
            mocker.AsyncMock(
                side_effect=PaytureDataError(
                    status_code=200, method='post', service='payture', response_status='LOVELY_PAYTURE_ERROR'
                ),
            ),
        )

        result = await processing.submit_3ds()

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR')),
        )


class TestCancel:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, checkout_order):
        mock = mocker.patch.object(
            AbstractPaytureAPIClient,
            'unblock',
            mocker.AsyncMock(return_value=UnblockResult(raw_new_amount=0)),
        )

        await processing.cancel()

        mock.assert_awaited_once_with(
            credentials=PaytureAPICredentials(key='the-key', password='the-password', gateway_merchant_id='the-gwid'),
            order_id='ac3a67e1-9df1-4612-bfdc-c82a3c549c14',
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'unblock',
            mocker.AsyncMock(return_value=UnblockResult(raw_new_amount=0)),
        )

        result = await processing.cancel()

        assert_that(result, equal_to(ResultCancelled()))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'unblock',
            mocker.AsyncMock(
                side_effect=PaytureDataError(
                    status_code=200, method='post', service='payture', response_status='LOVELY_PAYTURE_ERROR'
                ),
            ),
        )

        result = await processing.cancel()

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR')),
        )


class TestClear:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, checkout_order, entity_receipt):
        mock = mocker.patch.object(
            AbstractPaytureAPIClient,
            'charge',
            mocker.AsyncMock(return_value=ChargeResult(raw_new_amount=100)),
        )

        await processing.clear(amount=checkout_order.capture_amount, receipt=entity_receipt)

        mock.assert_awaited_once_with(
            credentials=PaytureAPICredentials(key='the-key', password='the-password', gateway_merchant_id='the-gwid'),
            order_id='ac3a67e1-9df1-4612-bfdc-c82a3c549c14',
            amount=checkout_order.capture_amount,
            cheque=MINIMAL_CHEQUE,
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'charge',
            mocker.AsyncMock(return_value=ChargeResult(raw_new_amount=100)),
        )

        result = await processing.clear(amount=Decimal('12.34'))

        assert_that(result, equal_to(ResultCleared()))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'charge',
            mocker.AsyncMock(
                side_effect=PaytureDataError(
                    status_code=200, method='post', service='payture', response_status='LOVELY_PAYTURE_ERROR'
                ),
            ),
        )

        result = await processing.clear(amount=Decimal('12.34'))

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR')),
        )


class TestRefund:
    @pytest.mark.asyncio
    async def test_calls_client(self, processing, mocker, transaction, entity_receipt):
        mock = mocker.patch.object(
            AbstractPaytureAPIClient,
            'refund',
            mocker.AsyncMock(return_value=RefundResult(raw_new_amount=100)),
        )

        await processing.refund(amount=Decimal('123.45'), receipt=entity_receipt)

        mock.assert_awaited_once_with(
            credentials=PaytureAPICredentials(key='the-key', password='the-password', gateway_merchant_id='the-gwid'),
            order_id='ac3a67e1-9df1-4612-bfdc-c82a3c549c14',
            amount=Decimal('123.45'),
            cheque=MINIMAL_CHEQUE,
        )

    @pytest.mark.asyncio
    async def test_success(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'refund',
            mocker.AsyncMock(return_value=RefundResult(raw_new_amount=100)),
        )

        result = await processing.refund(amount=Decimal('123.45'))

        assert_that(result, equal_to(ResultRefunded()))

    @pytest.mark.asyncio
    async def test_failure(self, processing, mocker):
        mocker.patch.object(
            AbstractPaytureAPIClient,
            'refund',
            mocker.AsyncMock(
                side_effect=PaytureDataError(
                    status_code=200, method='post', service='payture', response_status='LOVELY_PAYTURE_ERROR'
                ),
            ),
        )

        result = await processing.refund(amount=Decimal('123.45'))

        assert_that(
            result,
            equal_to(ResultFailed(reason='raw:LOVELY_PAYTURE_ERROR')),
        )


class TestCheque:
    @pytest.fixture
    def mock_client(self, mocker):
        return mocker.patch.object(
            AbstractPaytureAPIClient,
            'mobile_block',
            mocker.AsyncMock(return_value=BlockSuccess(AddInfo(auth_code='A1111', ref_number='B2222'))),
        )

    @pytest.mark.asyncio
    async def test_full_receipt(self, processing, mock_client, transaction, checkout_order, entity_full_receipt):
        entity_full_receipt.billing_contact = StorageContact(first_name='name', phone='phone')
        await processing.hold(
            payment_token='yp-token',
            amount=Decimal('12.34'),
            receipt=entity_full_receipt,
        )

        _, call_kwargs = mock_client.call_args_list[0]
        assert_that(
            call_kwargs['cheque'],
            equal_to(
                Cheque(
                    customer_contact='email',
                    positions=[
                        ChequePosition(
                            quantity=1.5,
                            price=100.0,
                            tax=2,
                            text='Full Product',
                            agent_type=6,
                            agent_info=AgentInfo(
                                payment_agent_operation='operation',
                                payment_agent_phone_numbers=['+798700000222'],
                                payment_operator_name='Transfer Operator Name',
                                payment_operator_address='Transfer Operator Address',
                                payment_operator_inn='123456789',
                                payment_operator_phone_number=['+798700000333'],
                                payment_transfer_operator_phone_numbers=['+79876543210', '+798700012345'],
                            ),
                            payment_method_type=4,
                            customs_declaration_number=None,
                            excise=9.99,
                            manufacturer_country_code=None,
                            payment_subject_type=10,
                            nomenclature_code='rvxs3CzjIjMV9PRlhd5SF8fIeZUpKFub',
                            supplier_inn='123456789',
                            supplier_info=SupplierInfo(name='Supplier Name', phone_numbers=['+798700000111']),
                            unit_of_measurement=None,
                        )
                    ],
                )
            ),
        )

    @pytest.mark.parametrize('contact', (None, StorageContact(first_name='name')))
    @pytest.mark.asyncio
    async def test_invalid_billing_contact(self, processing, entity_receipt, checkout_order, contact):
        checkout_order.billing_contact = contact
        with pytest.raises(BillingContactRequiredError):
            await processing.hold(
                payment_token='yp-token',
                amount=entity_receipt.total,
                receipt=entity_receipt,
            )


@pytest.fixture
def processing(core_context, integration, checkout_order, transaction, psp):
    return PaytureProcessing(
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
                {'key': 'the-key', 'password': 'the-password', 'gateway_merchant_id': 'the-gwid'}
            ),
        )
    )


@pytest.fixture
async def merchant(storage, psp):
    return await storage.merchant.create(Merchant(name='merchant-name'))


@pytest.fixture
async def psp(storage):
    return await storage.psp.create(
        PSP(psp_external_id='payture', psp_id=uuid.UUID('e51fb2a2-12fc-4f88-9c59-81cb8a395880'))
    )
