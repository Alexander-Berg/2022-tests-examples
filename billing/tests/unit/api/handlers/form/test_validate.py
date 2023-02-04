import uuid
from decimal import Decimal

import pytest
from pay.lib.entities.enums import PaymentItemType
from pay.lib.entities.payment_sheet import PaymentItemQuantity, PaymentOrder, PaymentOrderItem, PaymentOrderTotal

from hamcrest import assert_that, equal_to, has_entries

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.payment_sheet.validate_sheet import (
    ValidatePaymentSheetAction, ValidatePaymentSheetResult
)
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import (
    ContactFields, PaymentMerchant, PaymentMethod, PaymentSheet, RequiredFields, ShippingTypes
)
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.tests.entities import APIKind

ACTUAL_UID = 222
PSP_EXTERNAL_ID = 'yandex-trust'


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/validate',
        APIKind.MOBILE: '/api/mobile/v1/validate',
    }[api_kind]


@pytest.fixture(autouse=True)
def mock_authentication(mocker):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=User(ACTUAL_UID)))


@pytest.fixture
def validation_result() -> ValidatePaymentSheetResult:
    psp = PSP(
        psp_id=uuid.uuid4(),
        psp_external_id=PSP_EXTERNAL_ID,
        public_key='public-key',
        public_key_signature='public-key-signature',
    )
    return {
        'psp': psp,
        'normalized_amount': 100,
    }


@pytest.fixture
def request_json():
    return {
        'merchant_origin': 'https://market.yandex.ru',
        'sheet': {
            'version': 2,
            'currency_code': 'USD',
            'country_code': 'ru',
            'merchant': {
                'id': '50fd0b78-0630-4f24-a532-9e1aac5ea859',
                'name': 'merchant-name',
                'url': 'http://site.test',
            },
            'payment_methods': [{
                'type': 'CARD',
                'gateway': PSP_EXTERNAL_ID,
                'verification_details': True,
                'gateway_merchant_id': 'hmnid',
                'allowed_auth_methods': ['CLOUD_TOKEN'],
                'allowed_card_networks': ['MASTERCARD'],
            }, {
                'type': 'CASH',
            }],
            'order': {
                'id': 'order-id',
                'total': {
                    'amount': '1.00',
                    'label': "l'abel",
                },
                'items': [
                    {
                        'amount': '2.00',
                        'label': 'item_label',
                    },
                    {
                        'amount': '-1.00',
                        'label': 'discount',
                        'type': 'DISCOUNT',
                    },
                    {
                        'amount': '0.00',
                        'label': 'pickup',
                        'type': 'PICKUP',
                    },
                    {
                        'amount': '1.00',
                        'label': 'item_label_2',
                        'quantity': {
                            'count': '10',
                            'label': 'шт'
                        }
                    }
                ],
            },
            'required_fields': {
                'billing_contact': {
                    'email': True,
                },
                'shipping_contact': {
                    'email': False,
                    'name': True,
                    'phone': True,
                },
                'shipping_types': {
                    'direct': True,
                    'pickup': False,
                }
            }
        },
    }


@pytest.fixture
def expected_sheet():
    return PaymentSheet(
        version=2,
        order=PaymentOrder(
            id='order-id',
            total=PaymentOrderTotal(
                amount=Decimal('1.00'),
                label="l'abel",
            ),
            items=[
                PaymentOrderItem(
                    amount=Decimal('2.00'),
                    label='item_label',
                ),
                PaymentOrderItem(
                    amount=Decimal('-1.00'),
                    label='discount',
                    type=PaymentItemType.DISCOUNT,
                ),
                PaymentOrderItem(
                    amount=Decimal('0.00'),
                    label='pickup',
                    type=PaymentItemType.PICKUP,
                ),
                PaymentOrderItem(
                    amount=Decimal('1.00'),
                    label='item_label_2',
                    quantity=PaymentItemQuantity(
                        count=Decimal('10.00'),
                        label='шт',
                    )
                ),
            ],
        ),
        merchant=PaymentMerchant(
            id=uuid.UUID('50fd0b78-0630-4f24-a532-9e1aac5ea859'),
            name='merchant-name',
            url='http://site.test',
        ),
        currency_code='USD',
        country_code='ru',
        payment_methods=[
            PaymentMethod(
                method_type=PaymentMethodType.CARD,
                gateway='yandex-trust',
                gateway_merchant_id='hmnid',
                verification_details=True,
                allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                allowed_card_networks=[CardNetwork.MASTERCARD],
            ),
            PaymentMethod(
                method_type=PaymentMethodType.CASH,
            )
        ],
        required_fields=RequiredFields(
            billing_contact=ContactFields(email=True),
            shipping_contact=ContactFields(email=False, name=True, phone=True),
            shipping_types=ShippingTypes(direct=True, pickup=False),
        )
    )


@pytest.mark.asyncio
async def test_handler_returns_ok(app, api_url, mock_action, request_json, validation_result):
    mock_action(ValidatePaymentSheetAction, validation_result)

    r = await app.post(api_url, json=request_json)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to({'status': 'success', 'data': {}, 'code': 200}))


@pytest.mark.asyncio
async def test_action_params(app, api_url, api_kind, mock_action, request_json, expected_sheet, validation_result):
    mock = mock_action(ValidatePaymentSheetAction, validation_result)

    await app.post(api_url, json=request_json)

    mock.assert_called_once_with(
        merchant_origin='https://market.yandex.ru',
        sheet=expected_sheet,
        validate_origin=api_kind != APIKind.MOBILE,
    )


@pytest.mark.asyncio
async def test_optional_params(app, api_url, api_kind, mock_action, request_json, expected_sheet, validation_result):
    del request_json['sheet']['merchant']['url']
    del request_json['sheet']['order']['items']
    del request_json['sheet']['order']['total']['label']
    del request_json['sheet']['payment_methods'][0]['verification_details']
    expected_sheet.merchant.url = None
    expected_sheet.order.items = None
    expected_sheet.order.total.label = None
    expected_sheet.payment_methods[0].verification_details = False

    mock = mock_action(ValidatePaymentSheetAction, validation_result)

    await app.post(api_url, json=request_json)

    mock.assert_called_once_with(
        merchant_origin='https://market.yandex.ru',
        sheet=expected_sheet,
        validate_origin=api_kind != APIKind.MOBILE,
    )


class TestInputValidation:
    @pytest.fixture(autouse=True)
    def mock_validate_action(self, mock_action, validation_result):
        return mock_action(ValidatePaymentSheetAction, validation_result)

    @pytest.fixture
    def make_request(self, api_url, app):
        async def make(**kwargs):
            response = await app.post(api_url, **kwargs)
            try:
                json = await response.json()
            except Exception:
                json = None

            return response, json

        return make

    @pytest.mark.asyncio
    async def test_validates_merchant_origin(self, make_request, request_json):
        request_json['merchant_origin'] = 'one-two-three'
        response, data = await make_request(json=request_json)

        assert_that(
            response.status,
            equal_to(400),
        )
        assert_that(
            data['data']['params'],
            has_entries({'merchant_origin': ['Not a valid Origin.']}),
        )

    @pytest.mark.parametrize('payment_method_type, expected_response', (
        (None, {
            'type': ['Missing data for required field.'],
        }),
        ('CASH', None),
        ('CARD', {
            '_schema': [
                {'gateway': 'Missing data for required field.'},
                {'gateway_merchant_id': 'Missing data for required field.'},
                {'allowed_auth_methods': 'Missing data for required field.'},
                {'allowed_card_networks': 'Missing data for required field.'},
            ]
        })
    ))
    @pytest.mark.asyncio
    async def test_validates_sheet(self, make_request, request_json, payment_method_type, expected_response):
        request_json['sheet'] = {}
        if payment_method_type is not None:
            request_json['sheet']['payment_methods'] = [{'type': payment_method_type}]

        response, data = await make_request(json=request_json)

        assert_that(
            response.status,
            equal_to(400),
        )
        expected_params = {
            'country_code': ['Missing data for required field.'],
            'merchant': {
                'id': ['Missing data for required field.'],
                'name': ['Missing data for required field.'],
            },
            'order': {
                'id': ['Missing data for required field.'],
                'total': {
                    'amount': ['Missing data for required field.'],
                },
            },
            'version': ['Missing data for required field.'],
            'currency_code': ['Missing data for required field.'],
        }
        if expected_response:
            expected_params['payment_methods'] = {'0': expected_response}
        assert_that(
            data['data']['params'],
            has_entries({
                'sheet': expected_params
            }),
        )
