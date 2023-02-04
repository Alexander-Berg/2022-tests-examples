from uuid import uuid4

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.merchant_order.get import GetMerchantOrderAction

MERCHANT_ID = uuid4()


class TestGet:
    @pytest.mark.asyncio
    async def test_success(self, user, app, disable_tvm_checking, order_entity, mock_action):
        mock = mock_action(GetMerchantOrderAction, order_entity)
        r = await app.get(f'/api/web/v1/merchants/{MERCHANT_ID}/orders/{order_entity.order_id}', raise_for_status=True)
        data = await r.json()

        mock.assert_called_once_with(
            user=user,
            merchant_id=MERCHANT_ID,
            order_id=order_entity.order_id,
        )
        expected_order = {
            'currency_code': 'RUB',
            'order_id': 'merchant-order-id',
            'checkout_order_id': str(order_entity.checkout_order_id),
            'metadata': 'metadata',
            'order_amount': '420.00',
            'cart': {
                'total': {'label': None, 'amount': '420.00'},
                'external_id': None,
                'coupons': None,
                'discounts': None,
                'measurements': None,
                'items': [
                    {
                        'subtotal': None,
                        'total': '420.00',
                        'discounted_unit_price': '42.00',
                        'quantity': {'count': '10', 'available': None, 'label': None},
                        'receipt': None,
                        'measurements': None,
                        'title': 'Awesome Product',
                        'unit_price': None,
                        'product_id': 'product-1',
                        'type': 'UNSPECIFIED',
                    }
                ],
                'cart_id': None,
            },
            'available_payment_methods': None,
            'billing_contact': None,
            'enable_comment_field': None,
            'enable_coupons': None,
            'merchant_id': None,
            'payment_method': None,
            'payment_status': 'AUTHORIZED',
            'reason': None,
            'required_fields': None,
            'shipping': None,
            'shipping_address': None,
            'shipping_contact': None,
            'shipping_method': None,
            'created': '2022-02-02T00:00:00+00:00',
            'updated': '2022-02-03T00:00:00+00:00',
            't': None,
        }
        assert_that(
            data['data']['order'],
            equal_to(expected_order),
        )
