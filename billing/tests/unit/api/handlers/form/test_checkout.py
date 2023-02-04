import json
import uuid
from base64 import b64encode
from decimal import Decimal

import pytest
from pay.lib.entities.enums import PaymentItemType
from pay.lib.entities.payment_sheet import (
    MITOptionsType, PaymentItemQuantity, PaymentOrder, PaymentOrderItem, PaymentOrderTotal
)
from pay.lib.entities.payment_token import MITInfo
from pay.lib.interactions.split.entities import YandexSplitOrderCheckoutInfo

from hamcrest import assert_that, equal_to, has_entries, has_item, matches_regexp

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.checkout import CheckoutAction
from billing.yandex_pay.yandex_pay.core.entities.checkout import CheckoutContext, MITCustomerChoices, PaymentMethodInfo
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import (
    DirectShippingMethod, DirectShippingMethodAddress, MITOptions, PaymentMerchant, PaymentMethod, PaymentSheet,
    PickupShippingMethod, PickupShippingMethodAddress, PickupShippingMethodAddressLocation, ShippingContact,
    ShippingMethod
)
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.tests.entities import APIKind

ACTUAL_UID = 222


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/checkout',
        APIKind.MOBILE: '/api/mobile/v1/checkout',
    }[api_kind]


@pytest.fixture(autouse=True)
def mock_authentication(mocker):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=User(ACTUAL_UID)))


@pytest.fixture
def checkout_result() -> CheckoutContext:
    return CheckoutContext(
        payment_method_info=PaymentMethodInfo(
            card_last4='1234',
            card_network=CardNetwork.MASTERCARD,
            split_meta=YandexSplitOrderCheckoutInfo(order_id='the-order-id', checkout_url='https://split.test'),
            method_type=PaymentMethodType.CARD,
            payment_token='the-token',
            mit_info=MITInfo(recurring=True),
        )
    )


@pytest.fixture
def expected_json_body(checkout_result):
    return {
        'code': 200,
        'status': 'success',
        'data': {
            'payment_token': checkout_result.payment_token,
            'payment_method_info': {
                'card_last4': '1234',
                'card_network': 'MASTERCARD',
                'split_meta': {'order_id': 'the-order-id', 'checkout_url': 'https://split.test'},
                'type': checkout_result.payment_method_type.value,
                'mit_info': {'recurring': True, 'deferred': False}
            },
        },
    }


@pytest.fixture
def params():
    return {
        'card_id': '50fd0b78-0630-4f24-a532-9e1aac5ea858',
        'merchant_origin': 'https://best-shop.ru',
        'sheet': {
            'version': 2,
            'currency_code': 'USD',
            'country_code': 'ru',
            'merchant': {
                'id': '50fd0b78-0630-4f24-a532-9e1aac5ea859',
                'name': 'merchant-name',
            },
            'payment_methods': [{
                'type': 'CARD',
                'gateway': 'yandex-trust',
                'gateway_merchant_id': 'hmnid',
                'allowed_auth_methods': ['CLOUD_TOKEN'],
                'allowed_card_networks': ['MASTERCARD'],
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
                        'amount': '1.00',
                        'label': 'item_label_2',
                        'quantity': {
                            'count': '10',
                            'label': 'шт'
                        }
                    }
                ],
            },
        },
    }


@pytest.fixture
def uaas_headers(yandex_pay_settings):
    pay_testitem = [
        {
            'HANDLER': yandex_pay_settings.API_UAAS_HANDLER,
            'CONTEXT': {
                'MAIN': {
                    'YANDEX_PAY_BACKEND': {'yandex_pay_plus.cashback_category': '0.15'}
                }
            }
        }
    ]
    other_testitem = [
        {
            'HANDLER': 'OTHER',
            'CONTEXT': {
                'MAIN': {
                    'OTHER': {'setting': 'fake'}
                }
            }
        }
    ]
    flags = ','.join(
        b64encode(json.dumps(each).encode()).decode()
        for each in (other_testitem, pay_testitem)
    )
    return {
        'X-Yandex-ExpFlags': flags,
        'X-Yandex-ExpBoxes': '398290,0,-1;398773,0,-1',
    }


@pytest.mark.asyncio
async def test_returned(app, api_url, mock_action, params, checkout_result, expected_json_body):
    mock_action(CheckoutAction, checkout_result)

    r = await app.post(api_url, json=params)
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_json_body))


@pytest.mark.asyncio
async def test_returned_without_optionals(app, api_url, mock_action, params, checkout_result, expected_json_body):
    checkout_result.payment_method_info.card_last4 = None
    checkout_result.payment_method_info.card_network = None
    checkout_result.payment_method_info.split_meta = None
    checkout_result.payment_method_info.mit_info = None
    mock_action(CheckoutAction, checkout_result)

    r = await app.post(api_url, json=params)
    json_body = await r.json()

    del expected_json_body['data']['payment_method_info']['card_last4']
    del expected_json_body['data']['payment_method_info']['card_network']
    del expected_json_body['data']['payment_method_info']['split_meta']
    del expected_json_body['data']['payment_method_info']['mit_info']
    assert_that(r.status, equal_to(200))
    assert_that(json_body, equal_to(expected_json_body))


@pytest.mark.parametrize('valid_params, expected_call', (
    pytest.param(
        {
            'card_id': '50fd0b78-0630-4f24-a532-9e1aac5ea858',
            'challenge_return_path': 'return_path',
            'mit_customer_choices': {
                'allowed': True,
            },
            'sheet': {
                'version': 2,
                'currency_code': 'USD',
                'country_code': 'ru',
                'merchant': {
                    'id': '50fd0b78-0630-4f24-a532-9e1aac5ea859',
                    'name': 'merchant-name',
                    'url': 'http://site.test',
                },
                'recurring_options': {
                    'type': 'RECURRING',
                    'optional': 'true',
                },
                'payment_methods': [{
                    'type': 'CARD',
                    'gateway': 'yandex-trust',
                    'gateway_merchant_id': 'hmnid',
                    'verification_details': True,
                    'allowed_auth_methods': ['CLOUD_TOKEN'],
                    'allowed_card_networks': ['MASTERCARD'],
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
            },
            'shipping_method': {
                'pickup': {
                    'extra_field': True,
                    'amount': 'not_a_number',
                    'address': {
                        'formatted': 'fake_formatted',
                        'location': {
                            'extra_field': True,
                            'latitude': 55.725296,
                            'longitude': None,
                        }
                    },
                    'id': 'pickup_id',
                },
                'direct': {
                    'provider': 'any',
                    'category': 'any',
                    'amount': 'not_a_number',
                    'address': {
                        'id': 123.456,
                        'country': None,
                        'region': None,
                        'locality': None,
                        'extra_field': True,
                    },
                    'id': 'direct_id',
                    'extra_field': True,
                }
            },
            'shipping_contact': {'id': -1},
        },
        {
            'user': User(ACTUAL_UID),
            'card_id': '50fd0b78-0630-4f24-a532-9e1aac5ea858',
            'challenge_return_path': 'return_path',
            'forbidden_card_networks': None,
            'sheet': PaymentSheet(
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
                ],
                mit_options=MITOptions(
                    type=MITOptionsType.RECURRING,
                    optional=True,
                ),
            ),
            'shipping_method': ShippingMethod(
                direct=DirectShippingMethod(
                    provider='any',
                    category='any',
                    amount='not_a_number',
                    address=DirectShippingMethodAddress(
                        id=123.456,
                        country=None,
                        region=None,
                        locality=None,
                    ),
                    id='direct_id',
                ),
                pickup=PickupShippingMethod(
                    amount='not_a_number',
                    address=PickupShippingMethodAddress(
                        formatted='fake_formatted',
                        location=PickupShippingMethodAddressLocation(
                            latitude=55.725296,
                            longitude=None,
                        ),
                    ),
                    id='pickup_id',
                ),
            ),
            'shipping_contact': ShippingContact(id=-1),
            'mit_customer_choices': MITCustomerChoices(allowed=True),
        },
        id='regular call'
    ),
    pytest.param(
        {
            'card_id': '50fd0b78-0630-4f24-a532-9e1aac5ea858',
            'sheet': {
                'version': 2,
                'currency_code': 'USD',
                'country_code': 'ru',
                'merchant': {
                    'id': '50fd0b78-0630-4f24-a532-9e1aac5ea859',
                    'name': 'merchant-name',
                },
                'payment_methods': [{
                    'type': 'CARD',
                    'gateway': 'yandex-trust',
                    'gateway_merchant_id': 'hmnid',
                    'allowed_auth_methods': ['CLOUD_TOKEN'],
                    'allowed_card_networks': ['MASTERCARD'],
                }],
                'order': {
                    'id': 'order-id',
                    'total': {
                        'amount': '1.00',
                        'label': "l'abel",
                    },
                },
            },
        },
        {
            'user': User(ACTUAL_UID),
            'card_id': '50fd0b78-0630-4f24-a532-9e1aac5ea858',
            'forbidden_card_networks': None,
            'sheet': PaymentSheet(
                version=2,
                order=PaymentOrder(
                    id='order-id',
                    total=PaymentOrderTotal(
                        amount=Decimal('1.00'),
                        label="l'abel",
                    ),
                ),
                merchant=PaymentMerchant(
                    id=uuid.UUID('50fd0b78-0630-4f24-a532-9e1aac5ea859'),
                    name='merchant-name',
                    url=None,
                ),
                currency_code='USD',
                country_code='ru',
                payment_methods=[
                    PaymentMethod(
                        method_type=PaymentMethodType.CARD,
                        gateway='yandex-trust',
                        gateway_merchant_id='hmnid',
                        verification_details=False,
                        allowed_auth_methods=[AuthMethod.CLOUD_TOKEN],
                        allowed_card_networks=[CardNetwork.MASTERCARD],
                    ),
                ],
                mit_options=None,
            ),
            'shipping_method': None,
            'shipping_contact': None,
            'mit_customer_choices': None,
        },
        id='no optional parameters'
    ),
))
@pytest.mark.parametrize('use_uaas,cashback_category_id', [(True, '0.15'), (False, None)])
@pytest.mark.asyncio
async def test_passed_params(
    app,
    api_url,
    api_kind,
    valid_params,
    use_uaas,
    cashback_category_id,
    uaas_headers,
    mock_action,
    expected_call,
):
    method_mock = mock_action(CheckoutAction)
    user_ip = 'fake_ip'
    user_agent = 'fake_agent'

    uaas_headers = uaas_headers if use_uaas else {}
    headers = {'x-real-ip': user_ip, 'user-agent': user_agent, **uaas_headers}
    await app.post(api_url, json=valid_params, headers=headers)

    method_mock.assert_called_once_with(
        **expected_call,
        validate_origin=api_kind != APIKind.MOBILE,
        user_ip=user_ip,
        user_agent=user_agent,
        cashback_category_id=cashback_category_id,
    )


class TestValidatesJSON:
    @pytest.fixture(autouse=True)
    def action_mocked(self, mock_action, checkout_result):
        return mock_action(CheckoutAction, checkout_result)

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
    async def test_requires_fields(self, make_request):
        response, json = await make_request(json={})

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params'],
            has_entries({
                'sheet': has_entries({
                    'payment_methods': has_entries({
                        '0': has_entries({
                            'type': has_item(equal_to('Missing data for required field.')),
                        })
                    }),
                    'country_code': has_item(equal_to('Missing data for required field.')),
                    'merchant': has_entries({
                        'id': has_item(equal_to('Missing data for required field.')),
                        'name': has_item(equal_to('Missing data for required field.')),
                    }),
                    'order': has_entries({
                        'id': has_item(equal_to('Missing data for required field.')),
                        'total': has_entries({
                            'amount': has_item(equal_to('Missing data for required field.')),
                        }),
                    }),
                    'version': has_item(equal_to('Missing data for required field.')),
                    'currency_code': has_item(equal_to('Missing data for required field.')),
                }),
            })
        )

    @pytest.mark.parametrize('field', ['gateway_merchant_id', 'gateway'])
    @pytest.mark.asyncio
    async def test_payment_method_fields_not_empty(self, make_request, params, field):
        params['sheet']['payment_methods'][0][field] = ''
        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params']['sheet']['payment_methods']['0'],
            has_entries({
                '_schema': has_item(has_entries({
                    field: has_item(equal_to('String should not be empty.')),
                }))
            })
        )

    @pytest.mark.asyncio
    async def test_validates_sheet_type(self, make_request, params):
        params['sheet'] = 'what happened???'

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params'],
            has_entries({'sheet': has_entries({'_schema': has_item(matches_regexp(r'Invalid (input )?type.'))})})
        )

    @pytest.mark.asyncio
    async def test_merchant_name_can_not_be_empty(self, make_request, params):
        params['sheet']['merchant']['name'] = ''

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params'],
            has_entries({
                'sheet': has_entries({
                    'merchant': has_entries({
                        'name': has_item(equal_to('String should not be empty.')),
                    }),
                })
            })
        )

    @pytest.mark.parametrize('field', ['currency_code', 'country_code'])
    @pytest.mark.asyncio
    async def test_sheet_field_can_not_be_empty(self, make_request, params, field):
        params['sheet'][field] = ''

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params']['sheet'],
            has_entries({
                field: has_item(equal_to('String should not be empty.')),
            })
        )

    @pytest.mark.asyncio
    async def test_item_label_is_required(self, make_request, params):
        params['sheet']['order']['total']['amount'] = '2.00'
        params['sheet']['order']['items'] = [
            {
                'amount': '1.00',
            },
            {
                'amount': '1.00',
                'label': None,
            }
        ]

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params'],
            has_entries({
                'sheet': has_entries({
                    'order': has_entries({
                        'items': has_entries(
                            {
                                '0': has_entries({'label': has_item(equal_to('Missing data for required field.'))}),
                                '1': has_entries({'label': has_item(equal_to('Field may not be null.'))}),
                            }),
                    }),
                }),
            }))

    @pytest.mark.asyncio
    async def test_card_id_is_required(self, make_request, params):
        params['card_id'] = ''
        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params'],
            has_entries({
                '_schema': ['`card_id` is required for "CARD" payment_method_type']
            })
        )

    @pytest.mark.asyncio
    async def test_card_id_is_not_required_for_cash(self, make_request, params):
        params['card_id'] = ''
        params['payment_method_type'] = 'CASH'
        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(200))

    @pytest.mark.asyncio
    async def test_validates_order_type(self, make_request, params):
        params['sheet']['order'] = 'what happened???'

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params'],
            has_entries({
                'sheet': has_entries({
                    'order': has_entries({
                        '_schema': has_item(matches_regexp(r'Invalid (input )?type.'))
                    })
                })
            })
        )

    @pytest.mark.asyncio
    async def test_validates_payment_methods_type(self, make_request, params):
        params['sheet']['payment_methods'] = 'what happened???'

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params'],
            has_entries({
                'sheet': has_entries({
                    'payment_methods': has_item(matches_regexp(r'Invalid (input )?type.'))
                }),
            })
        )

    @pytest.mark.asyncio
    async def test_validates_sheet(self, make_request, params):
        params['sheet'] = {
            'order': {
                'id': 1,
                'total': {
                    'amount': 'str',
                    'label': 1,
                },
                'items': [{
                    'amount': 'str',
                    'label': 1,
                }],
            },
            'version': 'string',
            'currency_code': 5,
            'country_code': 4,
            'merchant': {
                'id': 'NOT-AN-UUID',
                'name': 22,
            },
            'payment_methods': [{
                'type': '???',
                'gateway': 555,
                'gateway_merchant_id': 666,
                'allowed_auth_methods': {},
                'allowed_card_networks': {},
            }],
        }

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params'],
            has_entries({
                'sheet': has_entries({
                    'order': {
                        'id': ['Not a valid string.'],
                        'total': {
                            'amount': ['Not a valid number.'],
                            'label': ['Not a valid string.'],
                        },
                        'items': {
                            '0': {
                                'amount': ['Not a valid number.'],
                                'label': ['Not a valid string.'],
                            },
                        },
                    },
                    'version': ['Not a valid integer.'],
                    'currency_code': ['Not a valid string.'],
                    'country_code': ['Not a valid string.'],
                    'merchant': {
                        'id': ['Not a valid UUID.'],
                        'name': ['Not a valid string.'],
                    },
                    'payment_methods': {
                        '0': {
                            'type': ['Invalid enum value ???'],
                            'gateway': ['Not a valid string.'],
                            'gateway_merchant_id': ['Not a valid string.'],
                            'allowed_auth_methods': ['Not a valid list.'],
                            'allowed_card_networks': ['Not a valid list.'],
                        }
                    },
                })
            })
        )

    @pytest.mark.asyncio
    async def test_validates_merchant_id(self, make_request, params):
        params['sheet']['merchant']['id'] = 'tratata'
        params['sheet']['merchant']['name'] = 123

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params'],
            has_entries({
                'sheet': has_entries({
                    'merchant': {'id': ['Not a valid UUID.'],
                                 'name': ['Not a valid string.']
                                 }
                })
            })
        )

    @pytest.mark.asyncio
    async def test_validates_order_id_length(self, make_request, params):
        params['sheet']['order']['id'] = ''

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params'],
            has_entries({
                'sheet': has_entries({
                    'order': has_entries({
                        'id': has_item(equal_to('String should not be empty.')),
                    })
                }),
            })
        )

    @pytest.mark.parametrize('payment_methods', ([], [{'type': 'CASH'}] * 4))
    @pytest.mark.asyncio
    async def test_validates_payment_methods_length(self, make_request, params, payment_methods):
        params['sheet']['payment_methods'] = payment_methods

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params']['sheet'],
            has_entries({
                'payment_methods': has_item(equal_to('Length must be between 1 and 3.'))
            })
        )

    @pytest.mark.asyncio
    async def test_duplicate_payment_methods(self, make_request, params):
        params['sheet']['payment_methods'] = [{'type': 'CASH'}] * 2

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params']['sheet'],
            has_entries({
                'payment_methods': ['Duplicate method_type with value ClassicPaymentMethodType.CASH']
            })
        )

    @pytest.mark.asyncio
    async def test_validates_allowed_auth_methods_length(self, make_request, params):
        params['sheet']['payment_methods'] = [{'type': 'CARD', 'allowed_auth_methods': []}]

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params']['sheet']['payment_methods']['0'],
            has_entries({
                '_schema': has_item(equal_to({
                    'allowed_auth_methods': ['Shorter than minimum length 1.'],
                })),
            })
        )

    @pytest.mark.asyncio
    async def test_validates_card_networks_list_length(self, make_request, params):
        params['sheet']['payment_methods'] = [{'type': 'CARD', 'allowed_card_networks': []}]

        response, json = await make_request(json=params)

        assert_that(response.status, equal_to(400))
        assert_that(
            json['data']['params']['sheet']['payment_methods']['0'],
            has_entries({
                '_schema': has_item(equal_to({
                    'allowed_card_networks': ['Shorter than minimum length 1.'],
                }))
            })
        )
