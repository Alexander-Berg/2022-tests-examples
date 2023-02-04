from dataclasses import replace
from datetime import datetime, timezone
from uuid import uuid4

import pytest

from sendr_utils import without_none

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import is_datetime_with_tz


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def url(merchant_id):
    return f'/api/internal/v1/merchants/{merchant_id}/orders'


@pytest.fixture
async def merchant_orders(storage, merchant_id, entity_checkout_order):
    orders = []
    for i in range(1, 5):
        order = replace(entity_checkout_order, merchant_id=merchant_id, order_id=f'order-{i}')
        order = await storage.checkout_order.create(order)
        created = datetime(2022, 2, i, tzinfo=timezone.utc)
        orders.append(await storage.checkout_order.forge_created_datetime(order.checkout_order_id, created=created))

    return orders


@pytest.mark.asyncio
async def test_list_orders(app, merchant_orders, url, merchant_id):
    limit = 2
    created_lt = None
    result = []

    while True:
        r = await app.get(url, params=without_none({'limit': limit, 'created_lt': created_lt}), raise_for_status=True)
        data = await r.json()
        result += data['data']['orders']
        if len(data['data']['orders']) < limit:
            break
        else:
            created_lt = result[-1]['created']

    assert_that(
        result[::-1],
        equal_to(
            [
                {
                    'shipping_contact': {'email': 'email', 'id': 'cid', 'phone': 'phone', 'first_name': 'fname'},
                    'order_amount': '123.45',
                    'updated': is_datetime_with_tz,
                    'created': f'2022-02-0{i}T00:00:00+00:00',
                    'merchant_id': str(merchant_id),
                    'shipping_address': {
                        'entrance': '4',
                        'comment': 'comment',
                        'street': 'Tolstogo st.',
                        'building': '16',
                        'floor': '3',
                        'intercom': '21',
                        'zip': '400000',
                        'locality': 'Moscow',
                        'id': 'addr-id',
                        'country': 'Russia',
                        'region': 'Moscow',
                        'location': {'longitude': 56.78, 'latitude': 12.34},
                        'room': '3556',
                        'address_line': 'Russia, Moscow, Tolstogo st., 16',
                        'district': '12',
                    },
                    'cart': {
                        'total': {'amount': '441.00'},
                        'items': [
                            {
                                'type': 'UNSPECIFIED',
                                'product_id': 'product-1',
                                'discounted_unit_price': '42.00',
                                'quantity': {'count': '10'},
                                'total': '420.00',
                                'title': 'Awesome Product',
                                'receipt': {'tax': 1},
                            },
                            {
                                'type': 'UNSPECIFIED',
                                'product_id': 'product-2',
                                'discounted_unit_price': '21.00',
                                'quantity': {'count': '1'},
                                'total': '21.00',
                                'title': 'Awesome Product 2',
                                'receipt': {'tax': 1},
                            },
                        ],
                    },
                    'metadata': 'mdata',
                    'currency_code': 'XTS',
                    'enable_coupons': True,
                    'enable_comment_field': False,
                    'order_id': f'order-{i}',
                    'payment_status': 'PENDING',
                } for i in range(1, 5)
            ]
        ),
    )
