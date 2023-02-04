from datetime import datetime, timezone
from uuid import uuid4

import pytest
from pay.lib.entities.order import Order

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.list import ListMerchantOrdersAction


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def url(merchant_id):
    return f'/api/internal/v1/merchants/{merchant_id}/orders'


@pytest.fixture
def order(entity_cart):
    return Order(
        currency_code='RUB',
        cart=entity_cart,
        checkout_order_id=uuid4(),
    )


class TestList:
    @pytest.mark.asyncio
    async def test_success(self, app, url, merchant_id, order, mock_action):
        mock = mock_action(ListMerchantOrdersAction, [order])
        params = {
            'limit': 10,
            'created_gte': '2022-02-02T00:00:00+00:00',
            'created_lt': '2022-02-22T00:00:00+00:00',
        }
        r = await app.get(url, params=params, raise_for_status=True)
        data = await r.json()

        mock.assert_called_once_with(
            merchant_id=merchant_id,
            limit=10,
            created_gte=datetime(2022, 2, 2, tzinfo=timezone.utc),
            created_lt=datetime(2022, 2, 22, tzinfo=timezone.utc),
        )
        expected_order = {
            'currency_code': 'RUB',
            'checkout_order_id': str(order.checkout_order_id),
            'cart': {
                'items': [
                    {
                        'type': 'UNSPECIFIED',
                        'product_id': 'product-1',
                        'total': '420.00',
                        'quantity': {'count': '10'},
                        'title': 'Awesome Product',
                        'discounted_unit_price': '42.00',
                        'receipt': {'tax': 1},
                    },
                    {
                        'type': 'UNSPECIFIED',
                        'product_id': 'product-2',
                        'total': '21.00',
                        'quantity': {'count': '1'},
                        'title': 'Awesome Product 2',
                        'discounted_unit_price': '21.00',
                        'receipt': {'tax': 1},
                    },
                ],
                'total': {'amount': '441.00'},
            },
        }
        assert_that(
            data['data']['orders'],
            equal_to([expected_order]),
        )

    @pytest.mark.parametrize('limit', [0, 100500])
    @pytest.mark.asyncio
    async def test_limit(self, app, url, merchant_id, limit):
        r = await app.get(url, params={'limit': limit})
        data = await r.json()

        assert_that(r.status, equal_to(400))
        assert_that(
            data,
            equal_to(
                {
                    'code': 400,
                    'status': 'fail',
                    'data': {'message': 'BAD_FORMAT', 'params': {'limit': ['Must be between 1 and 1000.']}},
                }
            ),
        )
