import re
from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.interactions import TrustPaymentsClient
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.entities import (
    CardTrustPaymentMethod,
    TrustPayment,
    TrustPaymentsProduct,
    UnknownTrustPaymentMethod,
    YandexAccountTrustPaymentMethod,
    YandexPlusAccount,
)
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.enums import (
    TrustPaymentStatus,
    TrustRefundStatus,
)
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.exceptions import (
    TrustPaymentsResponseError,
    YandexPlusAccountNotFoundError,
)

CURRENCY = 'RUB'
_trust_request_errors = [
    (400, {'status': 'error', 'status_code': 'invalid_request'}),
    (401, {'status': 'no_auth', 'status_code': 'invalid_request'}),
    (500, {'status': 'error', 'status_code': 'internal_server_error'}),
    (200, {'status': 'error', 'status_code': 'some_error'}),
]


@pytest.fixture
def fake_account_id():
    return str(uuid4())


@pytest.fixture
def uid():
    return 123456789


@pytest.fixture
def purchase_token():
    return 'fake_purchase_token'


@pytest.fixture
async def trust_payments_client(create_client):
    client = create_client(TrustPaymentsClient)
    client.REQUEST_RETRY_TIMEOUTS = ()
    yield client
    await client.close()


class TestCreateProduct:
    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings):
        base_url = yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL
        return f'{base_url}/trust-payments/v2/products'

    @pytest.mark.asyncio
    async def test_product_creation_succeeded(
        self,
        trust_payments_client: TrustPaymentsClient,
        fake_account_id,
        aioresponses_mocker,
        endpoint_url,
    ):
        mock_ = aioresponses_mocker.post(
            re.compile(f'{endpoint_url}.*'),
            payload={'status': 'success'},
        )

        response = await trust_payments_client.create_product('fake_id', 'fake_name')
        expected = TrustPaymentsProduct('fake_id', 'fake_name')

        assert_that(response, equal_to(expected))

        mock_.assert_called_once()
        _, call_kwargs = mock_.call_args_list[0]
        assert_that(
            call_kwargs,
            has_entries(
                json={'product_id': 'fake_id', 'name': 'fake_name'},
                params={'skip_mandatory_partner_check': 'true'},
            ),
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('status_code,payload', _trust_request_errors)
    async def test_product_creation_failed(
        self,
        trust_payments_client: TrustPaymentsClient,
        aioresponses_mocker,
        endpoint_url,
        status_code,
        payload,
    ):
        aioresponses_mocker.post(
            re.compile(f'{endpoint_url}.*'),
            status=status_code,
            payload=payload,
        )

        with pytest.raises(TrustPaymentsResponseError) as exc_info:
            await trust_payments_client.create_product('fake_id', 'fake_name')

        assert_that(
            exc_info.value,
            has_properties(
                status_code=status_code,
                method='post',
                service=trust_payments_client.SERVICE,
                params=payload,
            )
        )


class TestCreatePlusAccount:
    @pytest.fixture
    def trust_response_data_success(self, fake_account_id):
        response_data = {
            'status': 'success',
            'currency': CURRENCY,
            'id': f'w/{fake_account_id}',
            'payment_method_id': f'yandex_account-w/{fake_account_id}',
        }
        return response_data

    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings):
        base_url = yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL
        return f'{base_url}/trust-payments/v2/account'

    @pytest.mark.asyncio
    async def test_account_creation_succeeded(
        self,
        trust_payments_client: TrustPaymentsClient,
        uid,
        fake_account_id,
        trust_response_data_success,
        aioresponses_mocker,
        endpoint_url,
    ):
        mock_ = aioresponses_mocker.post(endpoint_url, payload=trust_response_data_success)

        response = await trust_payments_client.create_plus_account(uid, CURRENCY)
        expected = YandexPlusAccount(
            account_id=f'w/{fake_account_id}', uid=uid, currency=CURRENCY
        )

        assert_that(response, equal_to(expected))

        mock_.assert_called_once()
        _, call_kwargs = mock_.call_args_list[0]
        assert_that(
            call_kwargs,
            has_entries(
                json={'currency': CURRENCY},
                headers=has_entries({'X-Uid': str(uid)}),
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('status_code,payload', _trust_request_errors)
    async def test_account_creation_failed(
        self,
        trust_payments_client: TrustPaymentsClient,
        uid,
        aioresponses_mocker,
        endpoint_url,
        status_code,
        payload,
    ):
        aioresponses_mocker.post(endpoint_url, status=status_code, payload=payload)

        with pytest.raises(TrustPaymentsResponseError) as exc_info:
            await trust_payments_client.create_plus_account(uid, CURRENCY)

        assert_that(
            exc_info.value,
            has_properties(
                status_code=status_code,
                method='post',
                service=trust_payments_client.SERVICE,
                params=payload,
            )
        )


class TestGetPlusAccount:
    @pytest.fixture
    def trust_response_data_success(self, fake_account_id, uid):
        response_data = {
            'status': 'success',
            'accounts': [
                {
                    'currency': 'other_currency',
                    'id': 'w/other_account_id',
                    'passport_id': uid,
                },
                {
                    'currency': CURRENCY,
                    'id': f'w/{fake_account_id}',
                    'passport_id': uid,
                },
            ]
        }
        return response_data

    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings):
        base_url = yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL
        return f'{base_url}/trust-payments/v2/account'

    @pytest.mark.asyncio
    async def test_account_exists(
        self,
        trust_payments_client: TrustPaymentsClient,
        uid,
        fake_account_id,
        trust_response_data_success,
        aioresponses_mocker,
        endpoint_url,
    ):
        mock_ = aioresponses_mocker.get(endpoint_url, payload=trust_response_data_success)

        response = await trust_payments_client.get_plus_account(uid, CURRENCY)
        expected = YandexPlusAccount(
            account_id=f'w/{fake_account_id}', uid=uid, currency=CURRENCY
        )

        assert_that(response, equal_to(expected))

        mock_.assert_called_once()
        _, call_kwargs = mock_.call_args_list[0]
        assert_that(
            call_kwargs,
            has_entries(
                headers=has_entries({'X-Uid': str(uid)}),
            ),
        )

    @pytest.mark.asyncio
    async def test_account_missing(
        self,
        trust_payments_client: TrustPaymentsClient,
        uid,
        trust_response_data_success,
        aioresponses_mocker,
        endpoint_url,
    ):
        trust_response_data_success['accounts'].pop()
        aioresponses_mocker.get(endpoint_url, payload=trust_response_data_success)

        with pytest.raises(YandexPlusAccountNotFoundError) as exc_info:
            await trust_payments_client.get_plus_account(uid, CURRENCY)

        assert_that(
            exc_info.value,
            has_properties(
                status_code=404,
                message='Yandex Plus account not found',
                method='get',
                service=trust_payments_client.SERVICE,
                params={'uid': uid, 'currency': CURRENCY},
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('status_code,payload', _trust_request_errors)
    async def test_trust_request_failed(
        self,
        trust_payments_client: TrustPaymentsClient,
        uid,
        aioresponses_mocker,
        endpoint_url,
        status_code,
        payload,
    ):
        aioresponses_mocker.get(endpoint_url, status=status_code, payload=payload)

        with pytest.raises(TrustPaymentsResponseError) as exc_info:
            await trust_payments_client.get_plus_account(uid, CURRENCY)

        assert_that(
            exc_info.value,
            has_properties(
                status_code=status_code,
                method='get',
                service=trust_payments_client.SERVICE,
                params=payload,
            )
        )


class TestCreatePlusTransaction:
    @pytest.fixture
    def trust_response_data_success(self, purchase_token):
        response_data = {
            'status': 'success',
            'purchase_token': purchase_token,
        }
        return response_data

    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings):
        base_url = yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL
        return f'{base_url}/trust-payments/v2/topup'

    @pytest.mark.asyncio
    async def test_transaction_creation_succeeded(
        self,
        trust_payments_client: TrustPaymentsClient,
        uid,
        trust_response_data_success,
        aioresponses_mocker,
        endpoint_url,
        purchase_token,
        fake_account_id,
    ):
        aioresponses_mocker.post(endpoint_url, payload=trust_response_data_success)

        account = YandexPlusAccount(
            account_id=f'w/{fake_account_id}', uid=uid, currency=CURRENCY
        )
        response = await trust_payments_client.create_plus_transaction(
            account, 'fake_product_id', 100
        )

        assert_that(response, equal_to(purchase_token))

    @pytest.mark.asyncio
    async def test_transaction_creation_payload_passed(
        self,
        trust_payments_client: TrustPaymentsClient,
        uid,
        trust_response_data_success,
        aioresponses_mocker,
        endpoint_url,
        purchase_token,
        fake_account_id,
    ):
        mock_ = aioresponses_mocker.post(endpoint_url, payload=trust_response_data_success)

        account = YandexPlusAccount(
            account_id=f'w/{fake_account_id}', uid=uid, currency=CURRENCY
        )
        billing_payload = {
            'cashback_service': 'yapay',
            'cashback_type': 'nontransaction',
            'has_plus': 'false',
            'service_id': '1024',
            'issuer': 'marketing',
            'campaign_name': 'campaign_name',
            'ticket': 'FAKE-000',
            'product_id': 'product_id',
        }
        await trust_payments_client.create_plus_transaction(
            account, 'fake_product_id', 100, billing_payload=billing_payload
        )

        mock_.assert_called_once()
        _, call_kwargs = mock_.call_args_list[0]
        assert_that(
            call_kwargs,
            has_entries(
                json={
                    'currency': CURRENCY,
                    'amount': 100,
                    'product_id': 'fake_product_id',
                    'paymethod_id': account.payment_method_id,
                    'pass_params': {'payload': billing_payload},
                },
                headers=has_entries({'X-Uid': str(uid)}),
            )
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('amount', [0, -1])
    async def test_non_positive_amount_declined(
        self,
        trust_payments_client: TrustPaymentsClient,
        uid,
        fake_account_id,
        amount,
    ):
        account = YandexPlusAccount(
            account_id=f'w/{fake_account_id}', uid=uid, currency=CURRENCY
        )

        pattern = 'Top-up amount must be a positive integer'
        with pytest.raises(AssertionError, match=pattern):
            await trust_payments_client.create_plus_transaction(
                account, 'fake_product_id', amount
            )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('status_code,payload', _trust_request_errors)
    async def test_transaction_creation_failed(
        self,
        trust_payments_client: TrustPaymentsClient,
        aioresponses_mocker,
        uid,
        fake_account_id,
        endpoint_url,
        status_code,
        payload,
    ):
        aioresponses_mocker.post(endpoint_url, status=status_code, payload=payload)

        account = YandexPlusAccount(
            account_id=f'w/{fake_account_id}', uid=uid, currency=CURRENCY
        )
        with pytest.raises(TrustPaymentsResponseError) as exc_info:
            await trust_payments_client.create_plus_transaction(
                account, 'fake_product_id', 1
            )

        assert_that(
            exc_info.value,
            has_properties(
                status_code=status_code,
                method='post',
                service=trust_payments_client.SERVICE,
                params=payload,
            )
        )


class TestStartPlusTransaction:
    @pytest.fixture
    def trust_response_data_success(self):
        response_data = {
            'status': 'success',
            'payment_status': 'started',
        }
        return response_data

    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings, purchase_token):
        base_url = yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL
        return f'{base_url}/trust-payments/v2/topup/{purchase_token}/start'

    @pytest.mark.asyncio
    async def test_transaction_start_succeeded(
        self,
        trust_payments_client: TrustPaymentsClient,
        trust_response_data_success,
        aioresponses_mocker,
        endpoint_url,
        purchase_token,
    ):
        mock_ = aioresponses_mocker.post(endpoint_url, payload=trust_response_data_success)

        payment_status = await trust_payments_client.start_plus_transaction(
            purchase_token
        )

        assert_that(payment_status, equal_to(TrustPayment(TrustPaymentStatus.STARTED)))

        mock_.assert_called_once()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('status_code,payload', _trust_request_errors)
    async def test_transaction_start_failed(
        self,
        trust_payments_client: TrustPaymentsClient,
        aioresponses_mocker,
        purchase_token,
        endpoint_url,
        status_code,
        payload,
    ):
        aioresponses_mocker.post(endpoint_url, status=status_code, payload=payload)

        with pytest.raises(TrustPaymentsResponseError) as exc_info:
            await trust_payments_client.start_plus_transaction(purchase_token)

        assert_that(
            exc_info.value,
            has_properties(
                status_code=status_code,
                method='post',
                service=trust_payments_client.SERVICE,
                params=payload,
            )
        )


class TestGetPlusTransactionStatus:
    @pytest.fixture(params=list(TrustPaymentStatus))
    def payment_status(self, request):
        return request.param

    @pytest.fixture
    def trust_response_data_success(self, payment_status):
        response_data = {
            'status': 'success',
            'payment_status': payment_status.value,
        }
        return response_data

    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings, purchase_token):
        base_url = yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL
        return f'{base_url}/trust-payments/v2/payments/{purchase_token}'

    @pytest.mark.asyncio
    async def test_get_transaction_status_succeeded(
        self,
        trust_payments_client: TrustPaymentsClient,
        trust_response_data_success,
        aioresponses_mocker,
        endpoint_url,
        purchase_token,
        payment_status,
    ):
        mock_ = aioresponses_mocker.get(endpoint_url, payload=trust_response_data_success)

        returned_payment_status = await trust_payments_client.get_plus_transaction_status(
            purchase_token
        )

        assert_that(returned_payment_status, equal_to(TrustPayment(payment_status)))

        mock_.assert_called_once()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('status_code,payload', _trust_request_errors)
    async def test_get_transaction_status_failed(
        self,
        trust_payments_client: TrustPaymentsClient,
        aioresponses_mocker,
        purchase_token,
        endpoint_url,
        status_code,
        payload,
    ):
        aioresponses_mocker.get(endpoint_url, status=status_code, payload=payload)

        with pytest.raises(TrustPaymentsResponseError) as exc_info:
            await trust_payments_client.get_plus_transaction_status(purchase_token)

        assert_that(
            exc_info.value,
            has_properties(
                status_code=status_code,
                method='get',
                service=trust_payments_client.SERVICE,
                params=payload,
            )
        )


class TestCreatePlusRefundTransaction:
    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings):
        return f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}/trust-payments/v2/refunds'

    @pytest.mark.asyncio
    async def test_create_refund_call(
        self,
        trust_payments_client: TrustPaymentsClient,
        aioresponses_mocker,
        endpoint_url,
    ):
        mock = aioresponses_mocker.post(endpoint_url, payload={'status': 'success', 'trust_refund_id': 'refundid'})

        await trust_payments_client.create_plus_refund_transaction(
            purchase_token='p-token',
            amount=5,
            uid=500,
            reason_desc='reason-dddd',
        )

        mock.assert_called_once()
        assert_that(
            mock.call_args.kwargs,
            has_entries(
                headers=has_entries({
                    'x-uid': '500',
                    'x-user-ip': trust_payments_client.SERVERSIDE_USER_IP,
                }),
                json={
                    'purchase_token': 'p-token',
                    'reason_desc': 'reason-dddd',
                    'orders': [{'delta_amount': '5'}],
                }
            )
        )

    @pytest.mark.asyncio
    async def test_create_refund_call_with_optional_params(
        self,
        trust_payments_client: TrustPaymentsClient,
        aioresponses_mocker,
        endpoint_url,
    ):
        mock = aioresponses_mocker.post(endpoint_url, payload={'status': 'success', 'trust_refund_id': 'refundid'})

        await trust_payments_client.create_plus_refund_transaction(
            purchase_token='p-token',
            amount=5,
            uid=500,
            reason_desc='reason-dddd',
            user_ip='192.0.2.1',
        )

        mock.assert_called_once()
        assert_that(
            mock.call_args.kwargs,
            has_entries(
                headers=has_entries({
                    'x-uid': '500',
                    'x-user-ip': '192.0.2.1',
                }),
                json={
                    'purchase_token': 'p-token',
                    'reason_desc': 'reason-dddd',
                    'orders': [{'delta_amount': '5'}],
                }
            )
        )

    @pytest.mark.asyncio
    async def test_returned(
        self,
        trust_payments_client: TrustPaymentsClient,
        aioresponses_mocker,
        endpoint_url,
    ):
        aioresponses_mocker.post(endpoint_url, payload={'status': 'success', 'trust_refund_id': 'refundid'})

        returned_id = await trust_payments_client.create_plus_refund_transaction(
            purchase_token='p-token',
            amount=5,
            uid=500,
            reason_desc='reason-dddd',
        )

        assert_that(returned_id, equal_to('refundid'))


class TestStartPlusRefundTransaction:
    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings):
        return (
            f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}'
            '/trust-payments/v2/refunds/the-real-trust-refund-id/start'
        )

    @pytest.mark.asyncio
    async def test_returned(
        self,
        trust_payments_client: TrustPaymentsClient,
        aioresponses_mocker,
        endpoint_url,
    ):
        aioresponses_mocker.post(endpoint_url, payload={'status_desc': 'the-status', 'status': 'wait_for_notification'})

        refund = await trust_payments_client.start_plus_refund_transaction(
            trust_refund_id='the-real-trust-refund-id',
        )

        assert_that(
            refund,
            has_properties({
                'status_desc': 'the-status',
                'status': TrustRefundStatus.WAIT_FOR_NOTIFICATION,
            })
        )


class TestGetPlusRefundTransaction:
    @pytest.fixture
    def endpoint_url(self, yandex_pay_plus_settings):
        return f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}/trust-payments/v2/refunds/the-real-trust-refund-id'

    @pytest.mark.asyncio
    async def test_returned(
        self,
        trust_payments_client: TrustPaymentsClient,
        aioresponses_mocker,
        endpoint_url,
    ):
        aioresponses_mocker.get(endpoint_url, payload={'status_desc': 'the-status', 'status': 'wait_for_notification'})

        refund = await trust_payments_client.get_plus_refund_transaction(
            trust_refund_id='the-real-trust-refund-id',
        )

        assert_that(
            refund,
            has_properties({
                'status_desc': 'the-status',
                'status': TrustRefundStatus.WAIT_FOR_NOTIFICATION,
            })
        )


class TestPaymentMethodsNormalResponse:
    @pytest.fixture
    def card_method_data(self, uid):
        return {
            'region_id': 225,
            'payment_method': 'card',
            'system': 'MasterCard',
            'expiration_month': '01',
            'card_country': 'RUS',
            'binding_ts': '1586458392.247',
            'ebin_tags_version': 0,
            'card_level': 'STANDARD',
            'holder': 'Card Holder',
            'id': 'card-a1a1234567a12abcd12345a1a',
            'payment_system': 'MasterCard',
            'last_paid_ts': '1586458392.247',
            'account': '123456****7890',
            'ebin_tags': [],
            'expiration_year': '2030',
            'aliases': [
                'card-a1a1234567a12abcd12345a1a'
            ],
            'expired': False,
            'card_bank': 'SBERBANK OF RUSSIA',
            'card_id': 'card-a1a1234567a12abcd12345a1a',
            'recommended_verification_type': 'standard2',
            'orig_uid': str(uid),
            'binding_systems': [
                'trust'
            ]
        }

    @pytest.fixture
    def unknown_method_data(self, uid):
        return {
            'payment_method': 'sbp',
            'currency': 'XTS',
            'id': 'sbp',
        }

    @pytest.fixture
    def yandex_account_method_data(self, uid):
        return {
            'payment_system': None,
            'account': 'w/12e67d92-a5e3-575e-b7a7-000000000000',
            'payment_method': 'yandex_account',
            'update_ts': 1644228829.371562,
            'currency': 'XTS',
            'balance': '4695.00',
            'id': 'yandex_account-w/12e67d92-a5e3-575e-b7a7-000000000000',
            'cached_until_ts': 1644315229.371562
        }

    @pytest.fixture
    def payment_methods_response_data(self, yandex_account_method_data, unknown_method_data, card_method_data):
        response_data = {
            'status': 'success',
            'bound_payment_methods': [
                card_method_data,
                yandex_account_method_data,
                unknown_method_data,
            ],
        }
        return response_data

    @pytest.fixture(autouse=True)
    async def mock_response(
        self,
        aioresponses_mocker,
        yandex_pay_plus_settings,
        uid,
        payment_methods_response_data,
    ):
        return aioresponses_mocker.get(
            f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}/trust-payments/v2/payment-methods',
            payload=payment_methods_response_data
        )

    @pytest.mark.asyncio
    async def test_ok_response(
        self,
        uid,
        trust_payments_client: TrustPaymentsClient,
        card_method_data,
        yandex_account_method_data,
        unknown_method_data,
    ):
        payment_methods = await trust_payments_client.get_payment_methods(uid)
        assert_that(
            payment_methods,
            equal_to([
                CardTrustPaymentMethod(
                    id=card_method_data['id'],
                    card_id=card_method_data['card_id'],
                    binding_systems=card_method_data['binding_systems'],
                    orig_uid=card_method_data['orig_uid'],
                    system=card_method_data['system'],
                    payment_system=card_method_data['payment_system'],
                    expiration_month=card_method_data['expiration_month'],
                    expiration_year=card_method_data['expiration_year'],
                    card_bank=card_method_data['card_bank'],
                    expired=card_method_data['expired'],
                    account=card_method_data['account'],
                    last_paid_ts=datetime.fromtimestamp(float(card_method_data['last_paid_ts']), tz=timezone.utc),
                    binding_ts=datetime.fromtimestamp(float(card_method_data['binding_ts']), tz=timezone.utc),
                ),
                YandexAccountTrustPaymentMethod(
                    id=yandex_account_method_data['id'],
                    balance=Decimal(yandex_account_method_data['balance']),
                    currency=yandex_account_method_data['currency'],
                    account=yandex_account_method_data['account'],
                ),
                UnknownTrustPaymentMethod(id='sbp'),
            ])
        )


class TestPaymentMethodsErrorResponse:
    @dataclass
    class ExpectedResponse:
        payload: dict
        http_status: int

    # для "документирования" что приходит
    RESPONSES = [
        # non existing UID
        ExpectedResponse(
            http_status=401,
            payload={
                'status': 'no_auth',
                'status_desc': 'passport not found',
                'method': 'trust_payments.get_payment_methods',
            },
        ),
        # bad tvm service ticket
        ExpectedResponse(
            http_status=400,
            payload={
                'status': 'error',
                'status_code': 'invalid_service',
                'status_desc': 'invalid_service',
            },
        ),
        # bad service token
        ExpectedResponse(
            http_status=500,
            payload={
                'status': 'error',
                'status_code': 'technical_error',
                'status_desc': 'Server internal error',
                'method': 'trust_payments.get_payment_methods',
            }
        )
    ]

    @pytest.fixture(params=RESPONSES)
    def expected_response(self, request) -> ExpectedResponse:
        return request.param

    @pytest.fixture(autouse=True)
    def mock_response(self, yandex_pay_plus_settings, aioresponses_mocker, expected_response: ExpectedResponse):
        return aioresponses_mocker.get(
            f'{yandex_pay_plus_settings.TRUST_PAYMENTS_API_URL}/trust-payments/v2/payment-methods',
            payload=expected_response.payload,
            status=expected_response.http_status,
        )

    @pytest.mark.asyncio
    async def test_error_response(
        self,
        trust_payments_client: TrustPaymentsClient,
        uid,
    ):
        with pytest.raises(TrustPaymentsResponseError):
            await trust_payments_client.get_payment_methods(uid)
