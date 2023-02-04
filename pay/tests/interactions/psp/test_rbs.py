import json
from decimal import Decimal

import pytest
from aiohttp import TCPConnector

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that, instance_of

from pay.lib.entities.receipt import (
    AgentType,
    MarkQuantity,
    MeasureType,
    PaymentsOperator,
    PaymentSubjectType,
    PaymentType,
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
from pay.lib.tests.helpers import parse_form_data_naive

BASE_RBS_URL = 'https://rbsuat.test/bank/mountpoint'

ORDER_BUNDLE = OrderBundle(
    customer_details=CustomerDetails(
        email='email',
        phone='phone',
        full_name='full name',
        passport='passport',
        inn='inn',
        contact=None,
    ),
    cart_items=Cart(
        items=[
            Position(
                position_id='1',
                name='Классный товар',
                quantity=Quantity(
                    value=1.234,
                    measure=MeasureType.TON,
                ),
                item_code='item_code',
                item_price=99900,
                tax=Tax(tax_type=RBSTaxType.VAT_20, tax_sum=None),
                item_attributes=ItemAttributes(
                    payment_method=PaymentType.CREDIT_PAYMENT,
                    payment_object=PaymentSubjectType.SERVICE,
                    nomenclature='nomenclature',
                    mark_quantity=MarkQuantity(denominator=2, numerator=1),
                    agent_info=AgentInfo(
                        agent_type=AgentType.ATTORNEY,
                        operation='operation',
                        phones=['+79876543210', '+79990123456'],
                        payments_operator=PaymentsOperator(phones=['+79876543210', '+79990123456']),
                        transfer_operator=TransferOperator(
                            inn='inn',
                            name='name',
                            address='address',
                            phones=[],
                        ),
                    ),
                    supplier_info=Supplier(
                        inn='inn',
                        name='name',
                        phones=['+79990123456'],
                    ),
                ),
            )
        ]
    ),
)
SERIALIZED_ORDER_BUNDLE = {
    'customerDetails': {
        'email': 'email',
        'fullName': 'full name',
        'phone': 'phone',
        'inn': 'inn',
        'passport': 'passport',
    },
    'cartItems': {
        'items': [
            {
                'tax': {'taxType': 6},
                'name': 'Классный товар',
                'positionId': '1',
                'quantity': {'measure': 12, 'value': 1.234},
                'itemPrice': 99900,
                'itemCode': 'item_code',
                'itemAttributes': {
                    'attributes': [
                        {'name': 'agentInfo.MTOperator.address', 'value': 'address'},
                        {'name': 'agentInfo.MTOperator.inn', 'value': 'inn'},
                        {'name': 'agentInfo.MTOperator.name', 'value': 'name'},
                        {'name': 'agentInfo.MTOperator.phones', 'value': ''},
                        {'name': 'agentInfo.agentType', 'value': '5'},
                        {'name': 'agentInfo.paying.operation', 'value': 'operation'},
                        {'name': 'agentInfo.paying.phones', 'value': '+79876543210, +79990123456'},
                        {'name': 'agentInfo.paymentsOperator.phones', 'value': '+79876543210, +79990123456'},
                        {'name': 'markQuantity.denominator', 'value': '2'},
                        {'name': 'markQuantity.numerator', 'value': '1'},
                        {'name': 'nomenclature', 'value': 'nomenclature'},
                        {'name': 'paymentMethod', 'value': '7'},
                        {'name': 'paymentObject', 'value': '4'},
                        {'name': 'supplierInfo.inn', 'value': 'inn'},
                        {'name': 'supplierInfo.name', 'value': 'name'},
                        {'name': 'supplierInfo.phones', 'value': '+79990123456'},
                    ]
                },
            }
        ]
    },
}


class RBSRestTestClient(AbstractRBSRestClient):
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


class TestErrorHandling:
    @pytest.mark.asyncio
    async def test_common_error(self, rbs_api_client, aioresponses_mocker):
        aioresponses_mocker.post(
            f'{BASE_RBS_URL}/rest/Error',
            payload={'errorCode': '42', 'errorMessage': 'feelsbadman'},
        )

        with pytest.raises(RBSDataError) as exc_info:
            await rbs_api_client.post(
                interaction_method='something',
                url=rbs_api_client.endpoint_url('/rest/Error', base_url_override=BASE_RBS_URL),
            )

        assert_that(exc_info.value.response_status, equal_to('42'))
        assert_that(exc_info.value.params['reason'], equal_to('feelsbadman'))

    @pytest.mark.asyncio
    async def test_yandex_payment_error(self, rbs_api_client, aioresponses_mocker):
        aioresponses_mocker.post(
            f'{BASE_RBS_URL}/rest/Error',
            payload={'error': {'code': '42', 'message': 'feelsbadman'}},
        )

        with pytest.raises(RBSDataError) as exc_info:
            await rbs_api_client.post(
                interaction_method=rbs_api_client.INTERACTION_METHOD_YANDEX_PAYMENT,
                url=rbs_api_client.endpoint_url('/rest/Error', base_url_override=BASE_RBS_URL),
            )

        assert_that(exc_info.value.response_status, equal_to('42'))
        assert_that(exc_info.value.params['reason'], equal_to('feelsbadman'))


class TestRegisterPreAuth:
    @pytest.fixture(autouse=True)
    def mock_register_pre_auth(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{BASE_RBS_URL}/rest/registerPreAuth.do',
            payload={'orderId': 'b8de3bcd-8782-4f15-a5aa-28d6cacfbc6f', 'formUrl': 'https://whatever.test'},
        )

    @pytest.mark.asyncio
    async def test_call(self, rbs_api_client, mock_register_pre_auth, params):
        await rbs_api_client.register_pre_auth(**params)

        assert_that(
            await parse_form_data_naive(mock_register_pre_auth.call_args.kwargs['data']),
            equal_to(
                {
                    'userName': 'rbs-username',
                    'password': 'rbs-password',
                    'orderNumber': 'yapay-order-id',
                    'currency': '643',
                    'amount': '1542',
                    'returnUrl': 'https://return.invalid',
                    'failUrl': 'https://fail.invalid',
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_call_with_order_bundle(self, rbs_api_client, mock_register_pre_auth, params):
        params['order_bundle'] = ORDER_BUNDLE

        await rbs_api_client.register_pre_auth(**params)

        response = await parse_form_data_naive(mock_register_pre_auth.call_args.kwargs['data'])
        assert_that(
            json.loads(response['orderBundle']),
            equal_to(SERIALIZED_ORDER_BUNDLE),
        )

    @pytest.mark.asyncio
    async def test_result(self, rbs_api_client, params):
        result = await rbs_api_client.register_pre_auth(**params)

        assert_that(
            result,
            equal_to(RegisterResult(order_id='b8de3bcd-8782-4f15-a5aa-28d6cacfbc6f')),
        )

    @pytest.fixture
    def params(self, credentials):
        return dict(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_number='yapay-order-id',
            amount=Decimal('15.42'),
            currency='RUB',
            user_ip='192.0.2.1',
        )


class TestGetOrder:
    @pytest.fixture(autouse=True)
    def mock_get_order_status_extended(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{BASE_RBS_URL}/rest/getOrderStatusExtended.do',
            payload={
                'actionCode': -100,
                'actionCodeDescription': '',
                'amount': 1000,
                'attributes': [{'name': 'mdOrder', 'value': '09095d73-477c-7bad-809f-17d102031ba9'}],
                'bankInfo': {'bankCountryCode': 'UNKNOWN', 'bankCountryName': '&ltUnknown&gt'},
                'authRefNum': '384858241055',
                'chargeback': False,
                'currency': '810',
                'date': 1644572632468,
                'errorCode': '0',
                'errorMessage': 'Success',
                'merchantOrderParams': [],
                'orderDescription': '',
                'orderNumber': 'ordernum-2022-02-11-002',
                'orderStatus': 0,
                'paymentAmountInfo': {
                    'approvedAmount': 0,
                    'depositedAmount': 0,
                    'paymentState': 'CREATED',
                    'refundedAmount': 0,
                },
                'terminalId': '123456',
                'transactionAttributes': [],
            },
        )

    @pytest.mark.asyncio
    async def test_call(self, rbs_api_client, mock_get_order_status_extended, credentials):
        await rbs_api_client.get_order(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
        )

        assert_that(
            await parse_form_data_naive(mock_get_order_status_extended.call_args.kwargs['data']),
            equal_to(
                {
                    'userName': 'rbs-username',
                    'password': 'rbs-password',
                    'orderId': 'rbs-order-id',
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_returned(self, rbs_api_client, credentials):
        result = await rbs_api_client.get_order(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
        )

        assert_that(
            result,
            equal_to(Order(order_status=RBSOrderStatus.NEW, action_code=-100, auth_ref_num='384858241055')),
        )


class TestYandexPayment:
    @pytest.fixture
    def mock_yandex_payment(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{BASE_RBS_URL}/yandex/payment.do',
            payload={
                'data': {
                    'orderId': 'rbs-order-id',
                    'redirectUrl': 'https://return.invalid?orderId=rbs-order-id&lang=ru',
                },
                'success': True,
            },
        )

    @pytest.mark.asyncio
    async def test_call(self, rbs_api_client, mock_yandex_payment, credentials):
        await rbs_api_client.yandex_payment(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
            payment_token='PaymentTokeh',
            threeds2_challenge_notification_url='https://tds2termurl.test',
            threeds2_method_notification_url='https://tds2methodtermurl.test',
        )

        assert_that(
            mock_yandex_payment.call_args.kwargs['json'],
            equal_to(
                {
                    'username': 'rbs-username',
                    'password': 'rbs-password',
                    'orderId': 'rbs-order-id',
                    'paymentToken': 'PaymentTokeh',
                    'threeDSVer2FinishUrl': 'https://tds2termurl.test',
                    'threeDSMethodNotificationUrl': 'https://tds2methodtermurl.test',
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_call_after_fingerprinting(self, rbs_api_client, mock_yandex_payment, credentials):
        await rbs_api_client.yandex_payment(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
            payment_token='PaymentTokeh',
            threeds2_challenge_notification_url='https://tds2termurl.test',
            threeds2_method_notification_url='https://tds2methodtermurl.test',
            threeds2_server_transaction_id='trans-id',
        )

        assert_that(
            mock_yandex_payment.call_args.kwargs['json'],
            equal_to(
                {
                    'username': 'rbs-username',
                    'password': 'rbs-password',
                    'orderId': 'rbs-order-id',
                    'paymentToken': 'PaymentTokeh',
                    'threeDSVer2FinishUrl': 'https://tds2termurl.test',
                    'threeDSMethodNotificationUrl': 'https://tds2methodtermurl.test',
                    'threeDSServerTransId': 'trans-id',
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_result_success(self, rbs_api_client, mock_yandex_payment, credentials):
        result = await rbs_api_client.yandex_payment(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
            payment_token='PaymentTokeh',
            threeds2_challenge_notification_url='https://tds2termurl.test',
            threeds2_method_notification_url='https://tds2methodtermurl.test',
        )

        assert_that(
            result,
            equal_to(PaymentResultSuccess()),
        )

    @pytest.mark.asyncio
    async def test_result_threedsv1(self, rbs_api_client, aioresponses_mocker, credentials):
        aioresponses_mocker.post(
            f'{BASE_RBS_URL}/yandex/payment.do',
            payload={
                'data': {
                    'acsUrl': 'https://web.rbsuat.com/acs/auth/start.do',
                    'is3DSVer2': False,
                    'orderId': '09098dcd-5103-7d8d-a051-d76002031ba9',
                    'paReq': 'aGV5IHRoZXJlISBob3cgYXJlIHlvdSBkb2luZz8=',
                    'termUrl': 'https://web.rbsuat.com/ab/rest/finish3ds.do?lang=ru',
                },
                'success': True,
            },
        )
        result = await rbs_api_client.yandex_payment(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
            payment_token='PaymentTokeh',
            threeds2_challenge_notification_url='https://tds2termurl.test',
            threeds2_method_notification_url='https://tds2methodtermurl.test',
        )

        assert_that(
            result,
            equal_to(
                PaymentResult3DSV1(
                    acs_url='https://web.rbsuat.com/acs/auth/start.do',
                    pa_req='aGV5IHRoZXJlISBob3cgYXJlIHlvdSBkb2luZz8=',
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_result_threedsv2_fingerprinting(self, rbs_api_client, aioresponses_mocker, credentials):
        aioresponses_mocker.post(
            f'{BASE_RBS_URL}/yandex/payment.do',
            payload={
                'data': {
                    'is3DSVer2': True,
                    'threeDSServerTransId': '09098dcd-5103-7d8d-a051-d76002031ba9',
                    'threeDSMethodURL': 'https://acs-fp.test',
                    'threeDSMethodURLServer': 'https://3ds-server-fp.test',
                    'threeDSMethodDataPacked': 'Z290IGFueSBncmFwZXM/',
                    'threeDSMethodURLServerDirect': 'https://whocares.test',
                },
                'success': True,
            },
        )
        result = await rbs_api_client.yandex_payment(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
            payment_token='PaymentTokeh',
            threeds2_challenge_notification_url='https://tds2termurl.test',
            threeds2_method_notification_url='https://tds2methodtermurl.test',
        )

        assert_that(
            result,
            equal_to(
                PaymentResult3DSV2Fingerprinting(
                    threeds_server_transaction_id='09098dcd-5103-7d8d-a051-d76002031ba9',
                    three_ds_server_fingerprint_url='https://3ds-server-fp.test',
                    three_ds_acs_fingerprint_url='https://acs-fp.test',
                    three_ds_acs_fingerprint_url_param_value='Z290IGFueSBncmFwZXM/',
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_result_threedsv2_challenge(self, rbs_api_client, aioresponses_mocker, credentials):
        aioresponses_mocker.post(
            f'{BASE_RBS_URL}/yandex/payment.do',
            payload={
                'data': {
                    'info': '...',
                    'acsUrl': 'https://acs.test/acs2/acs/creq',
                    'is3DSVer2': True,
                    'packedCReq': 'eyJ0aHJlZURTU2VydmVy',
                },
                'success': True,
            },
        )
        result = await rbs_api_client.yandex_payment(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
            payment_token='PaymentTokeh',
            threeds2_challenge_notification_url='https://tds2termurl.test',
            threeds2_method_notification_url='https://tds2methodtermurl.test',
        )

        assert_that(
            result,
            equal_to(
                PaymentResult3DSV2Challenge(
                    acs_url='https://acs.test/acs2/acs/creq',
                    creq='eyJ0aHJlZURTU2VydmVy',
                )
            ),
        )

    @pytest.mark.parametrize(
        'response, expected_cls',
        [
            pytest.param(
                {
                    'data': {
                        'orderId': 'rbs-order-id',
                        'redirectUrl': 'https://return.invalid?orderId=rbs-order-id&lang=ru',
                    },
                    'success': True,
                },
                PaymentResultSuccess,
                id='alfa-no3ds',
            ),
            pytest.param(
                {
                    'data': {
                        'is3DSVer2': True,
                        'orderId': 'rbs-order-id',
                        'redirectUrl': 'https://return.invalid?orderId=rbs-order-id&lang=ru',
                    },
                    'success': True,
                },
                PaymentResultSuccess,
                id='alfa-frictionless',
            ),
            pytest.param(
                {
                    'data': {
                        'is3DSVer2': False,
                        'orderId': 'rbs-order-id',
                        'redirectUrl': 'https://return.invalid?orderId=rbs-order-id&lang=ru',
                    },
                    'success': True,
                },
                PaymentResultSuccess,
                id='mts-no3ds',
            ),
        ],
    )
    @pytest.mark.asyncio
    async def test_response_classification(
        self, credentials, rbs_api_client, aioresponses_mocker, response, expected_cls
    ):
        aioresponses_mocker.post(
            f'{BASE_RBS_URL}/yandex/payment.do',
            payload=response,
        )

        result = await rbs_api_client.yandex_payment(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
            payment_token='PaymentTokeh',
            threeds2_challenge_notification_url='https://tds2termurl.test',
            threeds2_method_notification_url='https://tds2methodtermurl.test',
        )

        assert_that(
            result,
            instance_of(expected_cls),
        )


class TestFinish3DS:
    @pytest.fixture(autouse=True)
    def mock_finish_3ds(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{BASE_RBS_URL}/rest/finish3dsPayment.do',
            payload={'errorCode': 0, 'redirect': 'https://return.invalid?orderId=rbs-order-id'},
        )

    @pytest.mark.asyncio
    async def test_call(self, rbs_api_client, mock_finish_3ds, credentials):
        await rbs_api_client.finish_3ds(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            md='order-md',
            pa_res='pares',
        )

        assert_that(
            await parse_form_data_naive(mock_finish_3ds.call_args.kwargs['data']),
            equal_to(
                {
                    'userName': 'rbs-username',
                    'password': 'rbs-password',
                    'mdOrder': 'order-md',
                    'paRes': 'pares',
                }
            ),
        )


class TestFinish3DSVer2:
    @pytest.fixture(autouse=True)
    def mock_finish_3ds_ver2(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{BASE_RBS_URL}/rest/finish3dsVer2Payment.do',
            payload={'errorCode': 0, 'redirect': 'https://return.invalid?orderId=rbs-order-id'},
        )

    @pytest.mark.asyncio
    async def test_call(self, rbs_api_client, mock_finish_3ds_ver2, credentials):
        await rbs_api_client.finish_3ds_ver2(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            threeds_server_transaction_id='trans-id',
            order_id='rbs-order-id',
        )

        assert_that(
            await parse_form_data_naive(mock_finish_3ds_ver2.call_args.kwargs['data']),
            equal_to(
                {
                    'userName': 'rbs-username',
                    'password': 'rbs-password',
                    'threeDSServerTransId': 'trans-id',
                    'mdOrder': 'rbs-order-id',
                }
            ),
        )


class TestDeposit:
    @pytest.fixture(autouse=True)
    def mock_deposit(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{BASE_RBS_URL}/rest/deposit.do',
            payload={'errorCode': 0, 'errorMessage': 'Успешно'},
        )

    @pytest.mark.asyncio
    async def test_call(self, rbs_api_client, mock_deposit, credentials):
        await rbs_api_client.deposit(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
            amount=Decimal('10'),
            currency='RUB',
            deposit_items=ORDER_BUNDLE.cart_items,
        )

        call_data = await parse_form_data_naive(mock_deposit.call_args.kwargs['data'])
        call_data['depositItems'] = json.loads(call_data['depositItems'])
        assert_that(
            call_data,
            equal_to(
                {
                    'userName': 'rbs-username',
                    'password': 'rbs-password',
                    'orderId': 'rbs-order-id',
                    'amount': '1000',
                    'depositItems': SERIALIZED_ORDER_BUNDLE['cartItems'],
                }
            ),
        )


class TestReverse:
    @pytest.fixture(autouse=True)
    def mock_reverse(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{BASE_RBS_URL}/rest/reverse.do',
            payload={'errorCode': 0, 'errorMessage': 'Успешно'},
        )

    @pytest.mark.asyncio
    async def test_call(self, rbs_api_client, mock_reverse, credentials):
        await rbs_api_client.reverse(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
        )

        assert_that(
            await parse_form_data_naive(mock_reverse.call_args.kwargs['data']),
            equal_to(
                {
                    'userName': 'rbs-username',
                    'password': 'rbs-password',
                    'orderId': 'rbs-order-id',
                }
            ),
        )


class TestRefund:
    @pytest.fixture(autouse=True)
    def mock_refund(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{BASE_RBS_URL}/rest/refund.do',
            payload={'errorCode': 0, 'errorMessage': 'Успешно'},
        )

    @pytest.mark.asyncio
    async def test_call(self, rbs_api_client, mock_refund, credentials):
        await rbs_api_client.refund(
            base_url=BASE_RBS_URL,
            credentials=credentials,
            order_id='rbs-order-id',
            amount=Decimal('10'),
            currency='RUB',
            refund_items=ORDER_BUNDLE.cart_items,
        )

        call_data = await parse_form_data_naive(mock_refund.call_args.kwargs['data'])
        call_data['refundItems'] = json.loads(call_data['refundItems'])
        assert_that(
            call_data,
            equal_to(
                {
                    'userName': 'rbs-username',
                    'password': 'rbs-password',
                    'orderId': 'rbs-order-id',
                    'amount': '1000',
                    'refundItems': SERIALIZED_ORDER_BUNDLE['cartItems'],
                }
            ),
        )


@pytest.fixture
async def rbs_api_client(create_interaction_client) -> RBSRestTestClient:
    client = create_interaction_client(RBSRestTestClient)
    yield client
    await client.close()


@pytest.fixture
def credentials():
    return RBSCredentials(
        username='rbs-username',
        password='rbs-password',
        gateway_merchant_id='rbs-gw',
        acquirer=RBSAcquirer.MTS,
    )
