from uuid import uuid4

import pytest
from pay.lib.entities.order import Order

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.merchant_order.get import GetMerchantOrderAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.merchant_order import GetMerchantOrderResponse


@pytest.fixture
def merchant_id():
    return uuid4()


@pytest.fixture
def order(entity_cart):
    return Order(
        currency_code='RUB',
        cart=entity_cart,
        checkout_order_id=uuid4(),
        order_id='123'
    )


@pytest.fixture
def url(merchant_id, order):
    return f'/api/internal/v1/merchants/{merchant_id}/orders/{order.order_id}'


class TestGet:
    @pytest.mark.asyncio
    async def test_success(self, app, url, merchant_id, order, mock_action):
        mock = mock_action(GetMerchantOrderAction, GetMerchantOrderResponse(order=order, operations=None))

        r = await app.get(url, raise_for_status=True)
        data = await r.json()

        mock.assert_called_once_with(
            merchant_id=merchant_id,
            order_id=order.order_id,
        )
        expected_order = {
            'currency_code': 'RUB',
            'order_id': str(order.order_id),
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
            data['data']['order'],
            equal_to(expected_order),
        )
