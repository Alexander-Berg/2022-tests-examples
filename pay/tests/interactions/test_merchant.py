import decimal
import json
import re
from datetime import date, datetime, time
from uuid import UUID, uuid4

import pytest
from aiohttp import TCPConnector
from jose import jws

from sendr_interactions.exceptions import InteractionResponseError
from sendr_pytest.helpers import ensure_all_fields
from sendr_pytest.matchers import convert_then_match, equal_to
from sendr_utils import utcnow
from sendr_utils.jwt import get_public_key_copy

from hamcrest import assert_that, has_entries, has_item, has_properties, match_equality

from pay.lib.entities.cart import (
    Cart,
    CartItem,
    CartItemType,
    CartTotal,
    Coupon,
    Discount,
    ItemQuantity,
    ItemReceipt,
    Measurements,
)
from pay.lib.entities.enums import CardNetwork, CouponStatus, DeliveryCategory, PaymentMethodType
from pay.lib.entities.enums import ShippingMethodType as OldShippingMethodType
from pay.lib.entities.order import Contact, ContactFields, Order, PaymentMethod, PaymentStatus, RequiredFields
from pay.lib.entities.payment_sheet import PaymentOrder, PaymentOrderTotal
from pay.lib.entities.receipt import (
    Agent,
    AgentType,
    MarkQuantity,
    MeasureType,
    PaymentsOperator,
    PaymentSubjectType,
    PaymentType,
    Supplier,
    TaxType,
    TransferOperator,
)
from pay.lib.entities.shipping import Address as PayAddress
from pay.lib.entities.shipping import (
    BoundingBox,
    CourierOption,
    Location,
    PickupOption,
    PickupSchedule,
    ShippingMethod,
    ShippingMethodType,
    ShippingOptions,
    ShippingWarehouse,
    YandexDeliveryShippingParams,
)
from pay.lib.interactions.merchant.client import AbstractMerchantClient, AbstractZoraMerchantClient
from pay.lib.interactions.merchant.entities import (
    EventType,
    MerchantCreateOrderRequest,
    MerchantResponse,
    MerchantWebhookV1Request,
    OrderWebhookData,
    ShippingMethodInfo,
    ShippingOption,
    TransactionStatus,
    UpdateTransactionStatusRequest,
)
from pay.lib.interactions.merchant.exceptions import MerchantAPIMalformedResponseError, MerchantAPIResponseError
from pay.lib.interactions.passport_addresses.entities import Address

ZORA_URL = 'https://zora.yandex.test/'
MERCHANT_URL = 'https://merchant.test'
JWK = {
    'alg': 'ES256',
    'kty': 'EC',
    'crv': 'P-256',
    'x': 'LEBfQpwTDXJtLFiPcnYvGv-WaFXZGBnFP_yGhLL9MGc',
    'y': 'a1Or3ovkpH12b0o3ruZUtm_z8bg3xQtHXi-uPC7UJT0',
    'd': 'AEjwp7szhRxINz5SF_OKTMmefRbbteONK94nR9CeBEY',
    'kid': 'test-key',
}


class TvmTicketGetterMock:
    async def get_service_ticket_headers(self, *args):
        return {'X-Ya-Service-Ticket': 'ticket'}


class MerchantTestClient(AbstractMerchantClient):
    JWK = JWK
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


class MerchantZoraTestClient(AbstractZoraMerchantClient):
    JWK = JWK
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()
    ZORA_TVM_ID = 1
    ZORA_CLIENT_NAME = 'test-client'
    ZORA_URL = ZORA_URL
    tvm_ticket_getter = TvmTicketGetterMock()


@pytest.fixture
def merchant_mock(aioresponses_mocker):
    def _mock(payload=None, body='', status=200, path=''):
        base = MERCHANT_URL.rstrip('/')
        path = '/' + path.lstrip('/')
        return aioresponses_mocker.post(
            re.compile(f'{base}{path}.*'),
            status=status,
            payload=payload,
            body=body,
        )

    return _mock


@pytest.fixture
def zora_mock(aioresponses_mocker):
    def _mock(payload=None, body='', status=200):
        return aioresponses_mocker.post(
            ZORA_URL,
            status=status,
            payload=payload,
            body=body,
        )

    return _mock


@pytest.fixture
async def merchant_client(create_interaction_client):
    client = create_interaction_client(MerchantTestClient)
    yield client
    await client.close()


@pytest.fixture
async def merchant_zora_client(create_interaction_client):
    client = create_interaction_client(MerchantZoraTestClient)
    yield client
    await client.close()


@pytest.fixture
async def merchant_id():
    return uuid4()


@pytest.fixture
def merchant_order_request(merchant_id: UUID):
    return MerchantCreateOrderRequest(
        merchant_id=merchant_id,
        currency_code='RUB',
        order=PaymentOrder(
            id='456',
            total=PaymentOrderTotal(
                amount=decimal.Decimal(100),
            ),
        ),
        shipping_method_info=ShippingMethodInfo(
            type=OldShippingMethodType.DIRECT,
            shipping_address=Address(country='Russia', locality='Russia', street='Lva Tolstogo', building='16'),
            shipping_option=ShippingOption(id='789'),
        ),
    )


@pytest.fixture
def merchant_order(merchant_id: UUID):
    return Order(
        merchant_id=merchant_id,
        currency_code='ABC',
        cart=Cart(
            cart_id='123',
            external_id='456',
            total=CartTotal(amount=decimal.Decimal('123.45')),
            items=[
                CartItem(
                    type=CartItemType.PHYSICAL,
                    product_id='789',
                    quantity=ItemQuantity(count=decimal.Decimal(10)),
                    title='Product Title',
                    discounted_unit_price=decimal.Decimal(10),
                    receipt=ItemReceipt(
                        tax=TaxType.VAT_20,
                    ),
                )
            ],
            coupons=[Coupon(value='hello')],
        ),
        order_id='order-id',
        shipping_address=Address(country='Russia', locality='Russia', street='Lva Tolstogo', building='16'),
    )


@pytest.mark.asyncio
async def test_zora_merchant_client(
    zora_mock,
    merchant_zora_client: AbstractMerchantClient,
    merchant_id: UUID,
):
    """
    Клиент представлен в двух вариантах - с зорой и без зоры.
    Для зоры проверим один кейс.
    """
    mock = zora_mock()
    event_time = utcnow()
    request = UpdateTransactionStatusRequest(order_id='456', status=TransactionStatus.SUCCESS, event_time=event_time)

    response = await merchant_zora_client.update_transaction_status(
        base_url=MERCHANT_URL, merchant_id=merchant_id, data=request
    )

    assert_that(response, equal_to(MerchantResponse(code=200)))
    mock.assert_called_once()

    kwargs = mock.call_args.kwargs
    token = kwargs['data'].decode('utf-8')
    key = get_public_key_copy(JWK)

    mock.assert_called_once()
    body = json.loads(jws.verify(token, key, algorithms=['ES256']))
    assert_that(
        body,
        equal_to(
            {
                'merchantId': str(merchant_id),
                'event': 'TRANSACTION_STATUS_UPDATE',
                'data': {
                    'orderId': '456',
                    'status': 'SUCCESS',
                    'eventTime': match_equality(convert_then_match(datetime.fromisoformat, event_time)),
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_update_transaction_success(
    merchant_mock,
    merchant_client: AbstractMerchantClient,
    merchant_id: UUID,
):
    mock = merchant_mock(path='/webhook')
    event_time = utcnow()
    request = UpdateTransactionStatusRequest(order_id='456', status=TransactionStatus.SUCCESS, event_time=event_time)

    response = await merchant_client.update_transaction_status(
        base_url=MERCHANT_URL, merchant_id=merchant_id, data=request
    )

    assert_that(response, equal_to(MerchantResponse(code=200)))
    mock.assert_called_once()

    kwargs = mock.call_args.kwargs
    token = kwargs['data'].decode('utf-8')
    key = get_public_key_copy(JWK)

    mock.assert_called_once()
    body = json.loads(jws.verify(token, key, algorithms=['ES256']))
    assert_that(
        body,
        equal_to(
            {
                'merchantId': str(merchant_id),
                'event': 'TRANSACTION_STATUS_UPDATE',
                'data': {
                    'orderId': '456',
                    'status': 'SUCCESS',
                    'eventTime': match_equality(convert_then_match(datetime.fromisoformat, event_time)),
                },
            }
        ),
    )


@pytest.mark.asyncio
async def test_update_transaction_success__not_json_body(
    merchant_mock,
    merchant_client: AbstractMerchantClient,
    merchant_id: UUID,
):
    mock = merchant_mock(body='not a json', path='/webhook')
    request = UpdateTransactionStatusRequest(order_id='456', status=TransactionStatus.SUCCESS, event_time=utcnow())

    response = await merchant_client.update_transaction_status(
        base_url=MERCHANT_URL, merchant_id=merchant_id, data=request
    )

    assert_that(response, equal_to(MerchantResponse(code=200)))
    mock.assert_called_once()


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'error',
    [
        {
            'code': 400,
            'reasonCode': 'ORDER_NOT_FOUND',
            'reason': 'some reason here',
        },
        {
            'code': 403,
            'reasonCode': 'FORBIDDEN',
        },
    ],
)
async def test_update_transaction_failed(
    error, merchant_mock, merchant_client: AbstractMerchantClient, merchant_id: UUID
):
    mock = merchant_mock(error, status=error['code'], path='/webhook')
    request = UpdateTransactionStatusRequest(order_id='456', status=TransactionStatus.SUCCESS, event_time=utcnow())

    with pytest.raises(MerchantAPIResponseError) as e:
        await merchant_client.update_transaction_status(base_url=MERCHANT_URL, merchant_id=merchant_id, data=request)

    mock.assert_called_once()
    assert_that(
        e.value,
        has_properties(
            status_code=error['code'],
            reason=error.get('reason', 'Backend unexpected response'),
            reason_code=error['reasonCode'],
        ),
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'payload',
    [
        None,
        {},
        {'code': 500, 'reason_code': 'OTHER', 'reason': 'test'},
    ],
)
async def test_update_transaction_bad_response(
    merchant_mock, payload, merchant_client: AbstractMerchantClient, merchant_id: UUID
):
    mock = merchant_mock(payload, status=500, path='/webhook')
    request = UpdateTransactionStatusRequest(order_id='456', status=TransactionStatus.SUCCESS, event_time=utcnow())
    with pytest.raises(InteractionResponseError):
        await merchant_client.update_transaction_status(base_url=MERCHANT_URL, merchant_id=merchant_id, data=request)

    mock.assert_called_once()


class TestWebhook:
    @pytest.fixture
    def params(self, merchant_id: UUID):
        return MerchantWebhookV1Request(
            merchant_id=merchant_id,
            event=EventType.ORDER_STATUS_UPDATED,
            event_time=utcnow(),
            order=OrderWebhookData(order_id='456', payment_status=PaymentStatus.CAPTURED),
        )

    @pytest.mark.asyncio
    async def test_success(
        self,
        merchant_mock,
        merchant_client: AbstractMerchantClient,
        merchant_id: UUID,
        params: MerchantWebhookV1Request,
    ):
        mock = merchant_mock(path='/v1/webhook')

        response = await merchant_client.notify(base_url=MERCHANT_URL, request=params)

        assert_that(response, equal_to(MerchantResponse(code=200)))
        mock.assert_called_once()

        kwargs = mock.call_args.kwargs
        token = kwargs['data'].decode('utf-8')
        key = get_public_key_copy(JWK)

        mock.assert_called_once()
        body = json.loads(jws.verify(token, key, algorithms=['ES256']))
        assert_that(
            body,
            equal_to(
                {
                    'merchantId': str(merchant_id),
                    'event': 'ORDER_STATUS_UPDATED',
                    'eventTime': match_equality(convert_then_match(datetime.fromisoformat, params.event_time)),
                    'order': {
                        'orderId': '456',
                        'paymentStatus': 'CAPTURED',
                    },
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_not_json_body(
        self,
        merchant_mock,
        merchant_client: AbstractMerchantClient,
        params: MerchantWebhookV1Request,
    ):
        mock = merchant_mock(body='not a json', path='/v1/webhook')

        response = await merchant_client.notify(base_url=MERCHANT_URL, request=params)

        assert_that(response, equal_to(MerchantResponse(code=200)))
        mock.assert_called_once()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'error',
        [
            {
                'code': 400,
                'reasonCode': 'ORDER_NOT_FOUND',
                'reason': 'some reason here',
            },
            {
                'code': 403,
                'reasonCode': 'FORBIDDEN',
            },
        ],
    )
    async def test_known_error_response(
        self,
        error,
        merchant_mock,
        merchant_client: AbstractMerchantClient,
        params: MerchantWebhookV1Request,
    ):
        mock = merchant_mock(error, status=error['code'], path='/v1/webhook')

        with pytest.raises(MerchantAPIResponseError) as e:
            await merchant_client.notify(base_url=MERCHANT_URL, request=params)

        mock.assert_called_once()
        assert_that(
            e.value,
            has_properties(
                status_code=error['code'],
                reason=error.get('reason', 'Backend unexpected response'),
                reason_code=error['reasonCode'],
            ),
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'payload',
        [
            None,
            {},
            {'code': 500, 'reason_code': 'OTHER', 'reason': 'test'},
        ],
    )
    async def test_bad_response(
        self,
        merchant_mock,
        payload,
        merchant_client: AbstractMerchantClient,
        params: MerchantWebhookV1Request,
    ):
        mock = merchant_mock(payload, status=500, path='/v1/webhook')
        with pytest.raises(InteractionResponseError):
            await merchant_client.notify(base_url=MERCHANT_URL, request=params)

        mock.assert_called_once()


@pytest.mark.asyncio
async def test_created_order_success(
    merchant_mock,
    merchant_order_request,
    merchant_client: AbstractMerchantClient,
    merchant_id: UUID,
):
    mock = merchant_mock(path='/order/create')
    request = merchant_order_request
    key = get_public_key_copy(JWK)

    response = await merchant_client.create_order(
        base_url=MERCHANT_URL,
        merchant_id=request.merchant_id,
        currency_code=request.currency_code,
        order=request.order,
        shipping_method_info=request.shipping_method_info,
    )

    assert_that(response, equal_to(MerchantResponse(code=200)))
    mock.assert_called_once()

    kwargs = mock.call_args.kwargs
    body = json.loads(jws.verify(kwargs['data'], key, algorithms=['ES256']))
    assert_that(
        body,
        equal_to(
            {
                'merchantId': str(merchant_id),
                'currencyCode': 'RUB',
                'order': {
                    'id': '456',
                    'total': {
                        'amount': match_equality(convert_then_match(decimal.Decimal, decimal.Decimal(100))),
                        'label': None,
                    },
                },
                'shippingMethodInfo': {
                    'type': 'DIRECT',
                    'shippingAddress': {
                        'country': 'Russia',
                        'region': None,
                        'locality': 'Russia',
                        'street': 'Lva Tolstogo',
                        'building': '16',
                        'room': None,
                        'entrance': None,
                        'floor': None,
                        'intercom': None,
                        'zip': None,
                        'location': None,
                        'locale': None,
                        'comment': None,
                        'address_line': None,
                        'district': None,
                    },
                    'shippingOption': {'id': '789'},
                },
            }
        ),
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'error',
    [
        {
            'code': 400,
            'reasonCode': 'ORDER_AMOUNT_MISMATCH',
            'reason': 'some reason here',
        },
        {
            'code': 403,
            'reasonCode': 'FORBIDDEN',
        },
    ],
)
async def test_create_order_failed(
    error, merchant_mock, merchant_order_request, merchant_client: AbstractMerchantClient
):
    mock = merchant_mock(error, status=error['code'], path='/order/create')
    request = merchant_order_request

    with pytest.raises(MerchantAPIResponseError) as e:
        await merchant_client.create_order(
            base_url=MERCHANT_URL,
            merchant_id=request.merchant_id,
            currency_code=request.currency_code,
            order=request.order,
            shipping_method_info=request.shipping_method_info,
        )

    mock.assert_called_once()
    assert_that(
        e.value,
        has_properties(
            status_code=error['code'],
            reason=error.get('reason', 'Backend unexpected response'),
            reason_code=error['reasonCode'],
        ),
    )


@pytest.mark.asyncio
async def test_create_order_failed_bad_response(
    dummy_logs, merchant_mock, merchant_order_request, merchant_client: AbstractMerchantClient
):
    mock = merchant_mock(
        {
            'code': 400,
            'reason_code': 'ABC',
            'msg': 'some reason here',
        },
        status=400,
        path='/order/create',
    )
    request = merchant_order_request

    with pytest.raises(InteractionResponseError) as e:
        await merchant_client.create_order(
            base_url=MERCHANT_URL,
            merchant_id=request.merchant_id,
            currency_code=request.currency_code,
            order=request.order,
            shipping_method_info=request.shipping_method_info,
        )

    mock.assert_called_once()

    assert_that(
        e.value,
        has_properties(
            status_code=400,
            message='Backend unexpected response',
        ),
    )

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                _context=has_entries(response={'code': 400, 'reason_code': 'ABC', 'msg': 'some reason here'})
            ),
        ),
    )


class TestRenderOrder:
    @pytest.mark.asyncio
    async def test_response(
        self,
        mock_response,
        merchant_order,
        merchant_client: AbstractMerchantClient,
        merchant_id: UUID,
    ):
        mock_response()

        response = await merchant_client.render_order(base_url=MERCHANT_URL, order=merchant_order)

        assert_that(
            response,
            equal_to(
                Order(
                    currency_code='RUB',
                    available_payment_methods=[PaymentMethodType.CARD, PaymentMethodType.SPLIT],
                    enable_coupons=False,
                    enable_comment_field=False,
                    required_fields=RequiredFields(
                        billing_contact=ContactFields(name=False, email=True, phone=False),
                        shipping_contact=ContactFields(name=False, email=True, phone=True),
                    ),
                    cart=Cart(
                        items=[
                            CartItem(
                                product_id='123',
                                quantity=ItemQuantity(count=decimal.Decimal('10'), available=None),
                                type=CartItemType.PHYSICAL,
                                total=decimal.Decimal('8'),
                                title='title',
                                subtotal=None,
                                unit_price=decimal.Decimal('10'),
                                discounted_unit_price=decimal.Decimal('2'),
                                measurements=None,
                                receipt=ensure_all_fields(ItemReceipt)(
                                    tax=TaxType.VAT_20,
                                    title='Receipt product 1',
                                    payment_method_type=PaymentType.FULL_PAYMENT,
                                    payment_subject_type=PaymentSubjectType.COMMODITY,
                                    measure=MeasureType.METER,
                                    excise=decimal.Decimal('2.00'),
                                    product_code=bytes.fromhex('84706b039ab156d36da9730377d2fdc2abb5'),
                                    mark_quantity=ensure_all_fields(MarkQuantity)(
                                        numerator=2,
                                        denominator=5,
                                    ),
                                    supplier=ensure_all_fields(Supplier)(
                                        name='Supplier Name',
                                        inn='7700123456',
                                        phones=['+79870005500'],
                                    ),
                                    agent=ensure_all_fields(Agent)(
                                        agent_type=AgentType.PAYMENT_AGENT,
                                        operation='operation',
                                        phones=['+79870005501'],
                                        transfer_operator=ensure_all_fields(TransferOperator)(
                                            name='Operator Name',
                                            address='Operator Address',
                                            inn='7700123000',
                                            phones=['+79870005507'],
                                        ),
                                        payments_operator=ensure_all_fields(PaymentsOperator)(
                                            phones=['+79870005511', '+79870005512'],
                                        ),
                                    ),
                                ),
                            )
                        ],
                        coupons=[],
                        discounts=[],
                        total=CartTotal(amount=decimal.Decimal('100'), label=None),
                        cart_id=None,
                        external_id=None,
                        measurements=None,
                    ),
                    shipping=ShippingOptions(
                        available_methods=[
                            ShippingMethodType.COURIER,
                            ShippingMethodType.PICKUP,
                            ShippingMethodType.YANDEX_DELIVERY,
                        ],
                        available_courier_options=[],
                        yandex_delivery=YandexDeliveryShippingParams(
                            warehouse=ShippingWarehouse(
                                address=ensure_all_fields(PayAddress)(
                                    id=None,
                                    country='country',
                                    region='region',
                                    locality='Russia',
                                    street='Lva Tolstogo',
                                    building='16',
                                    room='room',
                                    entrance='entrance',
                                    floor='floor',
                                    intercom='intercom',
                                    comment='comment',
                                    zip='zip',
                                    location=Location(
                                        latitude=55.5,
                                        longitude=33.3,
                                    ),
                                    locale='loc',
                                    district='district',
                                    address_line='address_line',
                                ),
                                contact=ensure_all_fields(Contact)(
                                    id=None,
                                    first_name='John',
                                    second_name='Jane',
                                    last_name='Doe',
                                    email='email@pay.test',
                                    phone='+70001112233',
                                ),
                                emergency_contact=ensure_all_fields(Contact)(
                                    id=None,
                                    first_name='Emergency',
                                    second_name='Alert',
                                    last_name='Doe',
                                    email='alert@pay.test',
                                    phone='+70001112233',
                                ),
                            ),
                        ),
                    ),
                    order_amount=decimal.Decimal('100'),
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_request(
        self,
        mock_response,
        merchant_order,
        merchant_client: AbstractMerchantClient,
        merchant_id: UUID,
    ):
        mock = mock_response()

        await merchant_client.render_order(base_url=MERCHANT_URL, order=merchant_order)

        mock.assert_called_once()
        kwargs = mock.call_args.kwargs
        key = get_public_key_copy(JWK)
        body = json.loads(jws.verify(kwargs['data'], key, algorithms=['ES256']))
        assert_that(
            body,
            equal_to(
                {
                    'currencyCode': 'ABC',
                    'merchantId': str(merchant_id),
                    'shippingAddress': {
                        'street': 'Lva Tolstogo',
                        'country': 'Russia',
                        'building': '16',
                        'locality': 'Russia',
                    },
                    'cart': {
                        'coupons': [{'value': 'hello'}],
                        'total': {'amount': '123.45'},
                        'cartId': '123',
                        'items': [
                            {
                                'type': 'PHYSICAL',
                                'title': 'Product Title',
                                'quantity': {'count': '10'},
                                'productId': '789',
                                'discountedUnitPrice': '10',
                                'receipt': {'tax': 1},
                            }
                        ],
                        'externalId': '456',
                    },
                    'orderId': 'order-id',
                }
            ),
        )

    @pytest.fixture
    def mock_response(self, merchant_mock):
        def _mock_response():
            return merchant_mock(
                {
                    'data': {
                        'orderAmount': '100',
                        'availablePaymentMethods': ['CARD', 'SPLIT'],
                        'currencyCode': 'RUB',
                        'enableCoupons': False,
                        'shipping': {
                            'availableCourierOptions': [],
                            'availableMethods': ['COURIER', 'PICKUP', 'YANDEX_DELIVERY'],
                            'yandexDelivery': {
                                'warehouse': {
                                    'emergencyContact': {
                                        'id': 'id',
                                        'first_name': 'Emergency',
                                        'second_name': 'Alert',
                                        'last_name': 'Doe',
                                        'email': 'alert@pay.test',
                                        'phone': '+70001112233',
                                    },
                                    'contact': {
                                        'id': 'id',
                                        'first_name': 'John',
                                        'second_name': 'Jane',
                                        'last_name': 'Doe',
                                        'email': 'email@pay.test',
                                        'phone': '+70001112233',
                                    },
                                    'address': {
                                        'id': 'id',
                                        'street': 'Lva Tolstogo',
                                        'country': 'country',
                                        'building': '16',
                                        'locality': 'Russia',
                                        'room': 'room',
                                        'region': 'region',
                                        'entrance': 'entrance',
                                        'floor': 'floor',
                                        'intercom': 'intercom',
                                        'comment': 'comment',
                                        'zip': 'zip',
                                        'location': {
                                            'latitude': 55.5,
                                            'longitude': 33.3,
                                        },
                                        'locale': 'loc',
                                        'district': 'district',
                                        'address_line': 'address_line',
                                    },
                                },
                            },
                        },
                        'cart': {
                            'cartId': 'overridencartid',
                            'items': [
                                {
                                    'type': 'PHYSICAL',
                                    'productId': '123',
                                    'title': 'title',
                                    'unitPrice': '10',
                                    'discountedUnitPrice': '2',
                                    'total': '8',
                                    'receipt': {
                                        'title': 'Receipt product 1',
                                        'tax': 1,
                                        'paymentMethodType': 4,
                                        'paymentSubjectType': 1,
                                        'excise': '2.00',
                                        'measure': 22,
                                        'productCode': 'hHBrA5qxVtNtqXMDd9L9wqu1',
                                        'markQuantity': {
                                            'numerator': 2,
                                            'denominator': 5,
                                        },
                                        'supplier': {
                                            'name': 'Supplier Name',
                                            'inn': '7700123456',
                                            'phones': ['+79870005500'],
                                        },
                                        'agent': {
                                            'agentType': 3,
                                            'operation': 'operation',
                                            'phones': ['+79870005501'],
                                            'transferOperator': {
                                                'name': 'Operator Name',
                                                'address': 'Operator Address',
                                                'inn': '7700123000',
                                                'phones': ['+79870005507'],
                                            },
                                            'paymentsOperator': {
                                                'phones': ['+79870005511', '+79870005512'],
                                            },
                                        },
                                    },
                                    'quantity': {'count': '10'},
                                }
                            ],
                            'discounts': [],
                            'coupons': [],
                            'total': {'amount': '100'},
                        },
                        'requiredFields': {
                            'billingContact': {'name': False, 'email': True, 'phone': False},
                            'shippingContact': {'name': False, 'email': True, 'phone': True},
                        },
                        'enableCommentField': False,
                    },
                    'status': 'success',
                },
                path='/v1/order/render',
            )

        return _mock_response


class TestCreateOrderV1:
    @pytest.mark.asyncio
    async def test_result(self, order, merchant_client, mock_response):
        result = await merchant_client.create_order_v1(base_url=MERCHANT_URL, order=order)

        assert_that(result.order_id, equal_to('m-oid'))

    @pytest.mark.asyncio
    async def test_call(self, order, merchant_client, mock_response):
        key = get_public_key_copy(JWK)

        await merchant_client.create_order_v1(base_url=MERCHANT_URL, order=order)

        kwargs = mock_response.call_args.kwargs
        body = json.loads(jws.verify(kwargs['data'], key, algorithms=['ES256']))
        assert_that(
            body,
            equal_to(
                {
                    'currencyCode': 'XTS',
                    'merchantId': str(order.merchant_id),
                    'cart': {
                        'cartId': 'cartid',
                        'externalId': 'cart-externalid',
                        'coupons': [{'value': 'hello', 'description': 'coup-desc', 'status': 'VALID'}],
                        'items': [
                            {
                                'type': 'PHYSICAL',
                                'receipt': {'tax': 1},
                                'title': 'product title',
                                'quantity': {'count': '10'},
                                'discountedUnitPrice': '10',
                                'productId': 'pid-1',
                            }
                        ],
                        'discounts': [{'amount': '5', 'discountId': 'did', 'description': 'desc-disc'}],
                        'measurements': {'weight': 1.0, 'height': 2.0, 'width': 3.0, 'length': 4.0},
                        'total': {
                            'amount': '15.00',
                            'label': 'cart-total',
                        },
                    },
                    'orderId': 'test-order',
                    'shippingAddress': {
                        'street': 'Lva Tolstogo',
                        'country': 'country',
                        'building': '16',
                        'locality': 'Russia',
                        'room': 'room',
                        'region': 'region',
                        'entrance': 'entrance',
                        'floor': 'floor',
                        'intercom': 'intercom',
                        'comment': 'comment',
                        'zip': 'zip',
                        'location': {
                            'latitude': 55.5,
                            'longitude': 33.3,
                        },
                        'locale': 'loc',
                    },
                    'orderAmount': '10.00',
                    'paymentMethod': {
                        'methodType': 'CARD',
                        'cardLast4': '0000',
                        'cardNetwork': 'MASTERCARD',
                    },
                    'shippingContact': {
                        'firstName': 'first-name',
                        'secondName': 'second-name',
                        'lastName': 'last-name',
                        'email': 'e-mail',
                        'phone': 'p-hone',
                    },
                    'billingContact': {
                        'firstName': 'first-name',
                        'secondName': 'second-name',
                        'lastName': 'last-name',
                        'email': 'e-mail',
                        'phone': 'p-hone',
                    },
                    'metadata': 'metadata',
                    'shippingMethod': {
                        'methodType': 'COURIER',
                        'courierOption': {
                            'courierOptionId': 'c-order-id',
                            'provider': 'the-provider',
                            'category': 'STANDARD',
                            'title': 'the-label',
                            'amount': '0',
                            'fromDate': '2000-12-30',
                            'toDate': '2001-12-30',
                            'fromTime': '22:59:59',
                            'toTime': '23:59:59',
                        },
                    },
                }
            ),
        )

    @pytest.fixture
    def order(self):
        return Order(
            merchant_id=uuid4(),
            currency_code='XTS',
            cart=Cart(
                cart_id='cartid',
                external_id='cart-externalid',
                items=[
                    CartItem(
                        product_id='pid-1',
                        type=CartItemType.PHYSICAL,
                        title='product title',
                        discounted_unit_price=decimal.Decimal(10),
                        receipt=ItemReceipt(
                            tax=TaxType.VAT_20,
                        ),
                        quantity=ItemQuantity(count=decimal.Decimal(10)),
                    )
                ],
                coupons=[Coupon(description='coup-desc', status=CouponStatus.VALID, value='hello')],
                total=CartTotal(amount=decimal.Decimal('15.00'), label='cart-total'),
                discounts=[Discount(discount_id='did', amount=decimal.Decimal('5'), description='desc-disc')],
                measurements=Measurements(weight=1, height=2, width=3, length=4),
            ),
            order_amount=decimal.Decimal('10.00'),
            order_id='test-order',
            payment_method=PaymentMethod(
                method_type=PaymentMethodType.CARD,
                card_last4='0000',
                card_network=CardNetwork.MASTERCARD,
            ),
            shipping_method=ShippingMethod(
                method_type=ShippingMethodType.COURIER,
                courier_option=CourierOption(
                    courier_option_id='c-order-id',
                    provider='the-provider',
                    category=DeliveryCategory.STANDARD,
                    title='the-label',
                    amount=decimal.Decimal('0'),
                    from_date=date(2000, 12, 30),
                    to_date=date(2001, 12, 30),
                    from_time=time(22, 59, 59),
                    to_time=time(23, 59, 59),
                ),
            ),
            shipping_address=PayAddress(
                country='country',
                region='region',
                locality='Russia',
                street='Lva Tolstogo',
                building='16',
                room='room',
                entrance='entrance',
                floor='floor',
                intercom='intercom',
                comment='comment',
                zip='zip',
                location=Location(
                    latitude=55.5,
                    longitude=33.3,
                ),
                locale='loc',
            ),
            shipping_contact=Contact(
                first_name='first-name',
                second_name='second-name',
                last_name='last-name',
                email='e-mail',
                phone='p-hone',
            ),
            billing_contact=Contact(
                first_name='first-name',
                second_name='second-name',
                last_name='last-name',
                email='e-mail',
                phone='p-hone',
            ),
            metadata='metadata',
        )

    @pytest.fixture(autouse=True)
    def mock_response(self, merchant_mock):
        return merchant_mock(
            {
                'data': {'orderId': 'm-oid', 'metadata': 'metadata'},
                'status': 'success',
            },
            path='/v1/order/create',
        )


@pytest.mark.asyncio
async def test_malformed_response_error(
    merchant_mock,
    merchant_client: AbstractMerchantClient,
    merchant_id: UUID,
    merchant_order: Order,
):
    merchant_mock(
        {
            'data': {},
            'status': 'success',
        },
        path='/v1/order/render',
    )

    with pytest.raises(MerchantAPIMalformedResponseError) as exc_info:
        await merchant_client.render_order(
            base_url=MERCHANT_URL,
            order=merchant_order,
        )

    assert_that(
        exc_info.value.params,
        has_entries(
            {
                'validation_errors': has_entries(
                    {
                        'data': has_entries(
                            {
                                'currencyCode': ['Missing data for required field.'],
                            }
                        )
                    }
                )
            }
        ),
    )


@pytest.mark.asyncio
async def test_bom_present_in_utf8_response(
    merchant_mock,
    merchant_client: AbstractMerchantClient,
    merchant_id: UUID,
    merchant_order: Order,
):
    merchant_mock(
        body=b'\xef\xbb\xbf{"code": 201}',
        path='/v1/webhook',
    )

    result = await merchant_client.notify(
        base_url=MERCHANT_URL,
        request=MerchantWebhookV1Request(
            merchant_id=merchant_id,
            event=EventType.ORDER_STATUS_UPDATED,
            event_time=utcnow(),
            order=OrderWebhookData(order_id='456', payment_status=PaymentStatus.CAPTURED),
        ),
    )

    assert_that(result.code, equal_to(201))


@pytest.mark.asyncio
async def test_get_pickup_options(
    merchant_mock,
    merchant_order,
    merchant_client: AbstractMerchantClient,
    merchant_id: UUID,
):
    mock = merchant_mock(
        {
            'data': {
                'pickupOptions': [
                    {
                        'pickupPointId': 'option-1',
                        'provider': 'IN_STORE',
                        'address': 'address',
                        'location': {'latitude': 12.34, 'longitude': 12.34},
                        'title': 'title',
                        'fromDate': '2022-03-11',
                        'toDate': '2022-04-11',
                        'amount': '42.00',
                        'description': 'description',
                        'phones': ['+79876543210'],
                        'schedule': [{'label': 'weekdays', 'fromTime': '08:00', 'toTime': '20:00'}],
                        'storagePeriod': 7,
                        'allowedPaymentMethods': ['CARD'],
                    },
                    {
                        'pickupPointId': 'option-2',
                        'provider': 'PICKPOINT',
                        'address': 'address',
                        'location': {'latitude': 34.56, 'longitude': 12.34},
                        'title': 'title',
                    },
                ]
            },
            'status': 'success',
        },
        path='/v1/pickup-options',
    )
    order: Order = merchant_order
    key = get_public_key_copy(JWK)

    response = await merchant_client.get_pickup_options(
        base_url=MERCHANT_URL,
        merchant_id=order.merchant_id,
        currency_code='USD',
        cart=order.cart,
        bounding_box=BoundingBox(
            sw=Location(latitude=12.34, longitude=43.21),
            ne=Location(latitude=23.45, longitude=32.10),
        ),
        metadata='metadata',
    )

    assert_that(
        response,
        equal_to(
            [
                PickupOption(
                    pickup_point_id='option-1',
                    provider='IN_STORE',
                    address='address',
                    location=Location(latitude=12.34, longitude=12.34),
                    title='title',
                    from_date=date(2022, 3, 11),
                    to_date=date(2022, 4, 11),
                    amount=decimal.Decimal('42.00'),
                    description='description',
                    phones=['+79876543210'],
                    schedule=[PickupSchedule(label='weekdays', from_time=time(8, 0), to_time=time(20, 0))],
                    storage_period=7,
                    allowed_payment_methods=[PaymentMethodType.CARD],
                ),
                PickupOption(
                    pickup_point_id='option-2',
                    provider='PICKPOINT',
                    address='address',
                    location=Location(latitude=34.56, longitude=12.34),
                    title='title',
                ),
            ]
        ),
    )
    mock.assert_called_once()

    kwargs = mock.call_args.kwargs
    body = json.loads(jws.verify(kwargs['data'], key, algorithms=['ES256']))
    assert_that(
        body,
        equal_to(
            {
                'merchantId': str(merchant_id),
                'currencyCode': 'USD',
                'boundingBox': {
                    'sw': {'latitude': 12.34, 'longitude': 43.21},
                    'ne': {'latitude': 23.45, 'longitude': 32.10},
                },
                'cart': {
                    'coupons': [{'value': 'hello'}],
                    'cartId': '123',
                    'total': {'amount': '123.45'},
                    'items': [
                        {
                            'type': 'PHYSICAL',
                            'discountedUnitPrice': '10',
                            'quantity': {'count': '10'},
                            'productId': '789',
                            'receipt': {'tax': 1},
                            'title': 'Product Title',
                        }
                    ],
                    'externalId': '456',
                },
                'metadata': 'metadata',
            }
        ),
    )


@pytest.mark.asyncio
async def test_get_pickup_option_details(
    merchant_mock,
    merchant_order,
    merchant_client: AbstractMerchantClient,
    merchant_id: UUID,
):
    mock = merchant_mock(
        {
            'data': {
                'pickupOption': {
                    'pickupPointId': 'option-1',
                    'provider': 'IN_STORE',
                    'address': 'address',
                    'location': {'latitude': 12.34, 'longitude': 12.34},
                    'title': 'title',
                    'fromDate': '2022-03-11',
                    'toDate': '2022-04-11',
                    'amount': '42.00',
                    'description': 'description',
                    'phones': ['+79876543210'],
                    'schedule': [{'label': 'weekdays', 'fromTime': '08:00', 'toTime': '20:00'}],
                    'storagePeriod': 7,
                    'allowedPaymentMethods': ['CARD'],
                }
            },
            'status': 'success',
        },
        path='/v1/pickup-option-details',
    )
    order: Order = merchant_order
    key = get_public_key_copy(JWK)

    response = await merchant_client.get_pickup_option_details(
        base_url=MERCHANT_URL,
        merchant_id=order.merchant_id,
        currency_code='EUR',
        cart=order.cart,
        pickup_point_id='option-1',
        metadata='metadata',
    )

    assert_that(
        response,
        equal_to(
            PickupOption(
                pickup_point_id='option-1',
                provider='IN_STORE',
                address='address',
                location=Location(latitude=12.34, longitude=12.34),
                title='title',
                from_date=date(2022, 3, 11),
                to_date=date(2022, 4, 11),
                amount=decimal.Decimal('42.00'),
                description='description',
                phones=['+79876543210'],
                schedule=[PickupSchedule(label='weekdays', from_time=time(8, 0), to_time=time(20, 0))],
                storage_period=7,
                allowed_payment_methods=[PaymentMethodType.CARD],
            )
        ),
    )
    mock.assert_called_once()

    kwargs = mock.call_args.kwargs
    body = json.loads(jws.verify(kwargs['data'], key, algorithms=['ES256']))
    assert body == {
        'merchantId': str(merchant_id),
        'currencyCode': 'EUR',
        'pickupPointId': 'option-1',
        'cart': {
            'coupons': [{'value': 'hello'}],
            'cartId': '123',
            'total': {'amount': '123.45'},
            'items': [
                {
                    'type': 'PHYSICAL',
                    'discountedUnitPrice': '10',
                    'quantity': {'count': '10'},
                    'productId': '789',
                    'receipt': {'tax': 1},
                    'title': 'Product Title',
                }
            ],
            'externalId': '456',
        },
        'metadata': 'metadata',
    }
