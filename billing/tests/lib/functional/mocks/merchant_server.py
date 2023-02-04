import re

from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.mocks.base import BaseMocker


class MerchantServerMocker(BaseMocker):
    def mock_order_render(self, rendered_order):
        return self.mocker.post(
            re.compile(self.endpoint_url('/v1/order/render')),
            payload={
                'status': 'success',
                'data': rendered_order,
            }
        )

    def mock_order_create(self, order_id: str = 'order-id'):
        response = {
            'status': 'success',
            'data': {
                'orderId': order_id,
            }
        }
        return self.mocker.post(
            re.compile(self.endpoint_url('/v1/order/create')),
            payload=response,
        )
