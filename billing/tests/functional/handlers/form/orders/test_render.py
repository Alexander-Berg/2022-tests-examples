import json
import re

import pytest
from aioresponses import CallbackResult
from jose import jws

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that, has_entries, match_equality, not_none

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.merchant import Merchant

MERCHANT_BASE_URL = 'https://merchant.com/yandexpay'


@pytest.fixture
def authentication(public_app, authenticate_client):
    authenticate_client(public_app)


@pytest.fixture
async def merchant(storage, rands):
    return await storage.merchant.create(Merchant(name=rands(), callback_url=MERCHANT_BASE_URL))


@pytest.fixture
def order(merchant):
    return {
        'currencyCode': 'RUB',
        'orderAmount': '99.00',
        'enableCoupons': True,
        'enableCommentField': True,
        'merchantId': str(merchant.merchant_id),
        'orderId': 'test-order',
        'cart': {
            'items': [
                {
                    'unitPrice': '10.00',
                    'quantity': {'available': '20', 'label': None, 'count': '2'},
                    'title': 'Socks',
                    'productId': 'product-1',
                    'subtotal': '20.00',
                    'total': '20.00',
                    'receipt': {
                        'tax': 1,
                    },
                    'discountedUnitPrice': '10',
                },
            ],
            'externalId': 'external-cart-id',
            'cartId': '1',
            'coupons': [{'value': 'PROMO', 'status': 'VALID', 'description': '10% discount'}],
            'discounts': [{'discount_id': 'PROMO', 'amount': '2.00', 'description': '10% discount by promo code'}],
            'measurements': {
                'width': 10,
                'height': 1,
                'length': 20,
                'weight': 0.3,
            },
            'total': {'label': None, 'amount': '18.00'},
        },
        'shipping': {
            'availableMethods': ['PICKUP', 'COURIER'],
            'availableCourierOptions': [
                {
                    'amount': '39.00',
                    'category': 'STANDARD',
                    'toTime': None,
                    'fromTime': None,
                    'toDate': None,
                    'title': 'Доставка курьером',
                    'courierOptionId': 'courier-1',
                    'fromDate': '2022-03-01',
                    'provider': 'CDEK',
                    'allowedPaymentMethods': ['CARD'],
                }
            ],
        },
        'availablePaymentMethods': ['CARD', 'SPLIT'],
    }


@pytest.fixture
def params(merchant):
    return {
        'merchant_id': str(merchant.merchant_id),
        'currency_code': 'RUB',
        'cart': {
            'items': [
                {
                    'quantity': {'count': '2'},
                    'product_id': 'product-1',
                    'title': 'Product',
                    'discounted_unit_price': '10',
                    'receipt': {'tax': 1},
                }
            ],
            'coupons': [
                {'value': 'PROMO'},
            ],
            'total': {'amount': '18.00'},
        },
        'order_id': 'order-id',
        'payment_method': {
            'method_type': 'CARD',
            'card_last4': '1234',
            'card_network': 'VISA',
        },
    }


class TestRender:
    @pytest.mark.asyncio
    async def test_success(
        self,
        public_app,
        params,
        authentication,
        aioresponses_mocker,
        yandex_pay_plus_settings,
        order,
    ):
        def callback(url, **kwargs):
            assert_that(kwargs['headers']['X-Ya-Dest-Url'], equal_to(f'{MERCHANT_BASE_URL}/v1/order/render'))
            assert_that(
                json.loads(jws.get_unverified_claims(kwargs['data'])),
                equal_to(
                    {
                        'currencyCode': 'RUB',
                        'merchantId': params['merchant_id'],
                        'orderId': 'order-id',
                        'cart': {
                            'cartId': '106766fb1df7bee930e43509a90a5999c4175055ac3415efed8b79b840a32c94',
                            'items': [
                                {
                                    'type': 'UNSPECIFIED',
                                    'productId': 'product-1',
                                    'receipt': {'tax': 1},
                                    'quantity': {'count': '2'},
                                    'title': 'Product',
                                    'discountedUnitPrice': '10',
                                }
                            ],
                            'total': {'amount': '18.00'},
                            'coupons': [
                                {'value': 'PROMO'},
                            ],
                        },
                    }
                ),
            )
            return CallbackResult(
                payload={
                    'status': 'success',
                    'data': order,
                }
            )

        aioresponses_mocker.post(yandex_pay_plus_settings.ZORA_URL, callback=callback)

        r = await public_app.post(
            '/api/public/v1/orders/render', json=params, headers={'x-pay-session-id': 'paysessid'}
        )
        data = await r.json()

        expected_response = {
            'cart': {
                'items': [
                    {
                        'type': 'UNSPECIFIED',
                        'total': '20.00',
                        'title': 'Socks',
                        'product_id': 'product-1',
                        'discounted_unit_price': '10',
                        'receipt': {'tax': 1},
                        'subtotal': '20.00',
                        'unit_price': '10.00',
                        'quantity': {'available': '20', 'count': '2'},
                    }
                ],
                'discounts': [{'description': '10% discount by promo code', 'discount_id': 'PROMO', 'amount': '2.00'}],
                'total': {'amount': '18.00'},
                'external_id': 'external-cart-id',
                'measurements': {'weight': 0.3, 'length': 20.0, 'width': 10.0, 'height': 1.0},
                'cart_id': '106766fb1df7bee930e43509a90a5999c4175055ac3415efed8b79b840a32c94',  # sha256('paysessid')
                'coupons': [{'description': '10% discount', 'value': 'PROMO', 'status': 'VALID'}],
            },
            'order_amount': '99.00',
            'enable_coupons': True,
            'enable_comment_field': True,
            'order_id': 'test-order',
            'available_payment_methods': ['CARD', 'SPLIT'],
            'merchant_id': params['merchant_id'],
            'currency_code': 'RUB',
            'shipping': {
                'available_courier_options': [
                    {
                        'from_date': '2022-03-01',
                        'provider': 'CDEK',
                        'category': 'STANDARD',
                        'amount': '39.00',
                        'title': 'Доставка курьером',
                        'courier_option_id': 'courier-1',
                        'allowed_payment_methods': ['CARD'],
                    }
                ],
                'available_methods': ['PICKUP', 'COURIER'],
            },
        }
        assert_that(r.status, equal_to(200))
        assert_that(data['data']['order'], equal_to(expected_response))

    @pytest.mark.asyncio
    async def test_fail(self, public_app, authentication):
        r = await public_app.post(
            '/api/public/v1/orders/render',
            json={},
            headers={'x-pay-session-id': 'sessid'},
        )
        data = await r.json()

        assert_that(r.status, equal_to(400))
        assert_that(
            data,
            equal_to(
                {
                    'code': 400,
                    'status': 'fail',
                    'data': {
                        'message': 'BAD_FORMAT',
                        'params': {
                            'cart': {
                                'items': ['Missing data for required field.'],
                            },
                            'currency_code': ['Missing data for required field.'],
                            'merchant_id': ['Missing data for required field.'],
                        },
                    },
                }
            ),
        )


class TestYandexDelivery:
    @pytest.mark.asyncio
    async def test_yandex_delivery(
        self,
        public_app,
        internal_app,
        params,
        merchant,
        authentication,
        order,
        aioresponses_mocker,
        yandex_pay_plus_settings,
    ):
        params = self.make_eligible_api_params(params)
        self.setup_merchant_api_mock(
            typical_order=order,
            aioresponses_mocker=aioresponses_mocker,
            yandex_pay_plus_settings=yandex_pay_plus_settings,
        )
        self.setup_address_api_mock(
            aioresponses_mocker=aioresponses_mocker,
            yandex_pay_plus_settings=yandex_pay_plus_settings,
        )
        self.setup_ydelivery_mock(
            aioresponses_mocker=aioresponses_mocker,
            yandex_pay_plus_settings=yandex_pay_plus_settings,
        )
        await self.configure_merchant(merchant=merchant, internal_app=internal_app)

        r = await public_app.post(
            '/api/public/v1/orders/render', json=params, headers={'x-pay-session-id': 'paysessid'}
        )
        data = await r.json()
        assert_that(r.status, equal_to(200))
        assert_that(
            data['data']['order'],
            has_entries({
                't': not_none(),
                'shipping': has_entries({
                    'available_methods': ['YANDEX_DELIVERY'],
                    'yandex_delivery': equal_to({
                        'options': [
                            match_equality(
                                has_entries({
                                    'amount': '20.00',
                                    'allowed_payment_methods': ['CARD'],
                                    'category': 'EXPRESS',
                                }),
                            ),
                            match_equality(
                                has_entries({
                                    'from_datetime': '2022-01-01T00:00:00+00:00',
                                    'to_datetime': '2022-01-01T06:00:00+00:00',
                                    'amount': '10.00',
                                    'allowed_payment_methods': ['CARD'],
                                    'category': 'TODAY',
                                }),
                            ),
                        ]
                    })
                })
            }),
        )

    def make_eligible_api_params(self, typical_params):
        return typical_params | {'shipping_address_id': 'ship-a-id'}

    def setup_merchant_api_mock(self, typical_order, aioresponses_mocker, yandex_pay_plus_settings):
        typical_order['cart']['items'][0] |= {
            'type': 'PHYSICAL',
            'measurements': {
                'width': 1.0,
                'height': 1.0,
                'length': 1.0,
                'weight': 1.0,
            }
        }
        typical_order['shipping'] = {
            'availableMethods': ['YANDEX_DELIVERY'],
            'yandexDelivery': {
                'warehouse': {
                    'address': {
                        'locality': 'Moscow',
                        'street': 'Kremlin',
                        'building': '10 A',
                        'country': 'Russian Federation',
                        'location': {
                            'latitude': 0.0,
                            'longitude': 0.0,
                        }
                    },
                    'contact': {},
                    'emergencyContact': {},
                }
            },
        }
        aioresponses_mocker.post(
            yandex_pay_plus_settings.ZORA_URL, payload={'status': 'success', 'data': typical_order},
        )

    def setup_address_api_mock(self, aioresponses_mocker, yandex_pay_plus_settings):
        aioresponses_mocker.get(
            re.compile(rf'{yandex_pay_plus_settings.PASSPORT_ADDRESSES_URL}.*'),
            payload={
                'id': 'ship-a-id',
                'country': 'The Moon',
                'locality': 'Mooncity',
                'street': 'Luna avenue',
                'building': '1/81',
                'location': {
                    'latitude': 0.0,
                    'longitude': 0.0,
                },
            },
        )

    def setup_ydelivery_mock(self, aioresponses_mocker, yandex_pay_plus_settings):
        aioresponses_mocker.post(
            re.compile(rf'{yandex_pay_plus_settings.YANDEX_DELIVERY_API_URL}.*/v1/delivery-methods(\?.*)?'),
            payload={
                'express_delivery': {'allowed': True},
                'same_day_delivery': {
                    'allowed': True,
                    'available_intervals': [{'from': '2022-01-01T00:00:00+00:00', 'to': '2022-01-01T06:00:00+00:00'}],
                },
            },
        )
        aioresponses_mocker.post(
            re.compile(rf'{yandex_pay_plus_settings.YANDEX_DELIVERY_API_URL}.*/v1/check-price(\?.*)?'),
            payload={
                'price': '20.00',
            },
        )
        aioresponses_mocker.post(
            re.compile(rf'{yandex_pay_plus_settings.YANDEX_DELIVERY_API_URL}.*/v1/check-price(\?.*)?'),
            payload={
                'price': '10.00',
            },
        )

    async def configure_merchant(self, internal_app, merchant):
        await internal_app.put(
            f'/api/internal/v1/merchants/{merchant.merchant_id}',
            json={
                'name': 'merchant',
                'origins': [],
                'delivery_integration_params': {
                    'yandex_delivery': {'oauth_token': 'OaUtHtOkEn'},
                },
                'callback_url': MERCHANT_BASE_URL,
            },
            raise_for_status=True,
        )
