import datetime
import re
from decimal import Decimal

import pytest
from pay.lib.entities.cart import Cart, CartItem, CartTotal, ItemQuantity, ItemReceipt
from pay.lib.entities.contact import Contact
from pay.lib.entities.enums import PaymentMethodType
from pay.lib.entities.order import Order
from pay.lib.entities.receipt import TaxType
from pay.lib.entities.shipping import (
    Address,
    DeliveryCategory,
    ShippingMethodType,
    ShippingOptions,
    ShippingWarehouse,
    YandexDeliveryOption,
    YandexDeliveryShippingParams,
)

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.order_token import (
    GetOrderTokenCryptogramAction,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.delivery import StorageWarehouse
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant

MERCHANT_BASE_URL = 'https://merchant.test/yandexpay'


@pytest.mark.asyncio
async def test_unauthorized(public_app, yandex_pay_plus_settings):
    r = await public_app.post(
        '/api/public/v1/orders/create',
    )
    data = await r.json()

    assert_that(r.status, equal_to(401))
    assert_that(data, equal_to({'code': 401, 'status': 'fail', 'data': {'message': 'MISSING_CREDENTIALS'}}))


@pytest.mark.asyncio
async def test_create_yandex_delivery_order(
    public_app,
    entity_auth_user,
    authenticate_client,
    merchant,
    storage
):
    authenticate_client(public_app)
    order = {
        'merchant_id': str(merchant.merchant_id),
        'currency_code': 'XTS',
        'cart': {
            'items': [
                {
                    'product_id': 'pid1',
                    'title': 'product title',
                    'total': '5',
                    'receipt': {
                        'tax': 1
                    },
                    'quantity': {'count': '1'}
                }
            ],
            'total': {'amount': '5'},
        },
        'order_amount': '15.00',
        'payment_method': {
            'method_type': 'CARD',
        },
        'shipping_method': {
            'method_type': ShippingMethodType.YANDEX_DELIVERY.value,
            'yandex_delivery_option': {
                'yandex_delivery_option_id': 'yd-option-id',
                'amount': '10',
                'title': 'Доставка',
                'category': 'TODAY',
                'from_datetime': '2022-01-01T00:00:00+00:00',
                'to_datetime': '2022-01-01T00:00:00+00:00',
                'allowed_payment_methods': ['CARD'],
                'receipt': {'tax': 1},
            },
        },
        'shipping_address_id': 'ship-a-id',
        'shipping_contact_id': 'ship-c-id',
        'billing_contact': {
            'id': 'bill-c-id',
            'phone': '+70001112233',
            'email': 'address@email.test',
            'first_name': 'fname',
            'second_name': 'sname',
            'last_name': 'lname',
        }
    }
    order['t'] = await GetOrderTokenCryptogramAction(
        user=entity_auth_user,
        merchant_id=order['merchant_id'],
        order=Order(
            currency_code=order['currency_code'],
            order_amount=Decimal(order['order_amount']),
            shipping=ShippingOptions(
                available_methods=[ShippingMethodType.YANDEX_DELIVERY],
                yandex_delivery=YandexDeliveryShippingParams(
                    warehouse=ShippingWarehouse(
                        address=Address(
                            id='ship-b-id',
                            country='Russian Federation',
                            locality='Moscow',
                            street='Kremlin',
                            building='10 A',
                        ),
                        contact=Contact(),
                        emergency_contact=Contact()
                    ),
                    options=[
                        YandexDeliveryOption(
                            amount=Decimal('10'),
                            category=DeliveryCategory.TODAY,
                            title='Доставка',
                            receipt=ItemReceipt(tax=TaxType.VAT_20),
                            yandex_delivery_option_id='yd-option-id',
                            allowed_payment_methods=[PaymentMethodType.CARD],
                            from_datetime=datetime.datetime(2022, 1, 1),
                            to_datetime=datetime.datetime(2022, 1, 1)
                        )
                    ]
                ),
            ),
            shipping_address=Address(id='ship-a-id', country='', locality='', building=''),
            cart=Cart(
                items=[CartItem(
                    product_id=i['product_id'],
                    quantity=ItemQuantity(count=Decimal(i['quantity']['count']))
                ) for i in order['cart']['items']],
                total=CartTotal(amount=Decimal(order['cart']['total']['amount']))
            ),
        )
    ).run()
    r = await public_app.post(
        '/api/public/v1/orders/create',
        headers={'x-pay-session-id': 'sessid-123'},
        json=order,
    )
    data = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        data['data']['order'],
        has_entries(
            {
                # подхватили ордер от мерча
                'order_id': 'real-order-id',
                # разрезолвили контакт доставки
                'shipping_contact': has_entries({
                    'id': 'ship-c-id',
                    'first_name': 'fname',
                }),
                # разрезолвили адрес доставки
                'shipping_address': has_entries({
                    'id': 'ship-a-id',
                    'locality': 'passp-locality',
                }),
                # разрезолвили платежный контакт
                'billing_contact': has_entries({
                    'id': 'bill-c-id',
                    'first_name': 'fname',
                }),
                # пробросили метаданные из ответа мерча
                'metadata': 'create-order-metadata',
            }
        ),
    )
    delivery = await storage.delivery.get_by_checkout_order_id(
        checkout_order_id=data['data']['order']['checkout_order_id'],
    )

    assert delivery.price == Decimal('10')
    assert delivery.warehouse == StorageWarehouse.from_warehouse(
        ShippingWarehouse(
            address=Address(
                id='ship-b-id',
                country='Russian Federation',
                locality='Moscow',
                street='Kremlin',
                building='10 A',
            ),
            contact=Contact(),
            emergency_contact=Contact()
        )
    )


@pytest.mark.asyncio
async def test_create_order(
    public_app,
    entity_auth_user,
    authenticate_client,
    merchant,
    storage
):
    authenticate_client(public_app)
    order = {
        'merchant_id': str(merchant.merchant_id),
        'currency_code': 'XTS',
        'cart': {
            'items': [
                {
                    'product_id': 'pid1',
                    'title': 'product title',
                    'total': '5',
                    'receipt': {
                        'tax': 1
                    },
                    'quantity': {'count': '1'}
                }
            ],
            'total': {'amount': '5'},
        },
        'order_amount': '15.00',
        'payment_method': {
            'method_type': 'CARD',
        },
        'shipping_method': {
            'method_type': ShippingMethodType.COURIER.value,
            'courier_option': {
                'courier_option_id': 'cid',
                'provider': 'privoder',
                'category': DeliveryCategory.STANDARD.value,
                'title': 'label',
                'amount': '10.00',
                'from_date': '2000-12-30',
            },
        },
        'shipping_address_id': 'ship-a-id',
        'shipping_contact_id': 'ship-c-id',
        'billing_contact': {
            'id': 'bill-c-id',
            'phone': '+70001112233',
            'email': 'address@email.test',
            'first_name': 'fname',
            'second_name': 'sname',
            'last_name': 'lname',
        }
    }
    r = await public_app.post(
        '/api/public/v1/orders/create',
        headers={'x-pay-session-id': 'sessid-123'},
        json=order,
    )
    data = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        data['data']['order'],
        has_entries(
            {
                # подхватили ордер от мерча
                'order_id': 'real-order-id',
                # разрезолвили контакт доставки
                'shipping_contact': has_entries({
                    'id': 'ship-c-id',
                    'first_name': 'fname',
                }),
                # разрезолвили адрес доставки
                'shipping_address': has_entries({
                    'id': 'ship-a-id',
                    'locality': 'passp-locality',
                }),
                # разрезолвили платежный контакт
                'billing_contact': has_entries({
                    'id': 'bill-c-id',
                    'first_name': 'fname',
                }),
                # пробросили метаданные из ответа мерча
                'metadata': 'create-order-metadata',
            }
        ),
    )
    assert_that(
        await storage.checkout_order.get_by_merchant_id_and_order_id(
            merchant_id=merchant.merchant_id,
            order_id=data['data']['order']['order_id'],
        ),
        has_properties(
            order_amount=Decimal('15.00'),
            shipping_address=has_properties(id='ship-a-id'),
            shipping_contact=has_properties(id='ship-c-id'),
            billing_contact=has_properties(id='bill-c-id'),
        ),
    )


@pytest.fixture(autouse=True)
def mock_shipping_address(aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.get(
        re.compile(rf'{yandex_pay_plus_settings.PASSPORT_ADDRESSES_URL}/address/get\?id=ship-a-id&.*'),
        payload={
            'id': 'ship-a-id',
            'country': 'passp-country',
            'locality': 'passp-locality',
            'building': 'passp-building',
        }
    )


@pytest.fixture(autouse=True)
def mock_shipping_contact(aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.get(
        re.compile(rf'{yandex_pay_plus_settings.PASSPORT_ADDRESSES_URL}/contact/get\?id=ship-c-id&.*'),
        payload={
            'id': 'ship-c-id',
            'phone': '+70001112233',
            'email': 'address@email.test',
            'first_name': 'fname',
            'second_name': 'sname',
            'last_name': 'lname',
        }
    )


@pytest.fixture(autouse=True)
def mock_merchant(aioresponses_mocker, yandex_pay_plus_settings):
    return aioresponses_mocker.post(
        yandex_pay_plus_settings.ZORA_URL,
        payload={
            'status': 'success',
            'data': {
                'orderId': 'real-order-id',
                'metadata': 'create-order-metadata',
                'receipt': {
                    'items': [
                        {
                            'title': 'Product 1',
                            'discountedUnitPrice': '10.00',
                            'quantity': {'count': '1.00'},
                            'tax': 1,
                            'agent': {
                                'agentType': 3,
                                'paymentsOperator': {'phones': ['+79870005511']},
                            },
                        }
                    ],
                },
            },
        },
    )


@pytest.fixture(autouse=True)
async def merchant(storage, rands):
    return await storage.merchant.create(Merchant(name=rands(), callback_url=MERCHANT_BASE_URL))
