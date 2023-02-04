import re

import pytest
import yarl

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.merchant import Merchant

pytestmark = pytest.mark.usefixtures('mock_app_authentication', 'setup_interactions_tvm')


@pytest.fixture
async def merchant(storage, partner, role):
    return await storage.merchant.create(
        Merchant(
            name='merchant name',
            partner_id=partner.partner_id,
        )
    )


@pytest.fixture
def plus_url(merchant, yandex_pay_admin_settings):
    url = yarl.URL(yandex_pay_admin_settings.YANDEX_PAY_PLUS_BACKEND_PRODUCTION_URL)
    return url / f'api/internal/v1/merchants/{merchant.merchant_id}/orders'


@pytest.fixture
def order(merchant):
    return {
        'merchant_id': '88072b15-cbd2-4013-8c7d-6c2d1abd24b0',
        'cart': {
            'items': [
                {
                    'discounted_unit_price': '42.00',
                    'type': 'UNSPECIFIED',
                    'subtotal': '42.00',
                    'title': 'Awesome Product',
                    'quantity': {'available': '100', 'count': '10', 'label': None},
                    'measurements': None,
                    'unit_price': '42.00',
                    'receipt': None,
                    'product_id': 'product-1',
                    'total': '42.00',
                }
            ],
            'external_id': None,
            'coupons': [],
            'discounts': [],
            'cart_id': 'cart-id',
            'measurements': None,
            'total': {'label': None, 'amount': '441.00'},
        },
        'available_payment_methods': [],
        'billing_contact': None,
        'checkout_order_id': '88072b15-cbd2-4013-8c7d-6c2d1abd24b0',
        'order_id': 'order-1',
        'order_amount': '441.45',
        'currency_code': 'XTS',
        'enable_comment_field': False,
        'enable_coupons': True,
        'metadata': 'mdata',
        'payment_method': {'method_type': 'CARD', 'card_last4': '0000', 'card_network': 'MASTERCARD'},
        'payment_status': 'PENDING',
        'reason': None,
        'required_fields': None,
        'shipping': None,
        'shipping_address': None,
        'shipping_method': None,
        'shipping_contact': {
            'email': 'email',
            'id': 'cid',
            'phone': 'phone',
            'second_name': 'sname',
            'last_name': 'lname',
            'first_name': 'fname',
        },
        'created': '2022-02-01T00:00:00+00:00',
        'updated': '2022-02-01T00:00:00+00:00',
        't': None,
    }


@pytest.mark.asyncio
async def test_get_orders(aioresponses_mocker, plus_url, app, merchant, order):
    plus_mock = aioresponses_mocker.get(
        re.compile(f'^{plus_url}.*'),
        payload={'data': {'orders': [order]}},
    )

    r = await app.get(
        f'/api/web/v1/merchants/{merchant.merchant_id}/orders',
        params={
            'limit': 3,
            'created_gte': '2022-02-01T00:00:00+00:00',
            'created_lt': '2022-02-01T00:00:00+00:00',
        },
    )
    response = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        response['data']['orders'],
        equal_to([order]),
    )
    assert plus_mock.call_args.kwargs['params'] == {
        'limit': 3,
        'created_gte': '2022-02-01T00:00:00+00:00',
        'created_lt': '2022-02-01T00:00:00+00:00',
    }
