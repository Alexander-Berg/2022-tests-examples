from typing import Any
from uuid import UUID


class OrderRenderRequestBuilder:
    """
    По большому счёту, тебе плевать, какие конкретно аргументы лежат в запросе order/render.
    Важно, что вернул в ответ мерчант.

    НО есть и существенные параметры. Например, без shipping_address_id интеграции с доставками не заведутся.

    Поэтому вот тебе builder
    """

    def __init__(self, merchant_id: UUID):
        self._object = {
            'merchant_id': str(merchant_id),
            'currency_code': 'XTS',
            'cart': {
                'items': [
                    {
                        'title': 'Товар',
                        'quantity': {'count': '1'},
                        'total': '100.0',
                        'product_id': 'pid1',
                        'type': 'PHYSICAL',
                    }
                ],
                'total': {'amount': '100.0'},
            },
            'payment_method': {
                'method_type': 'CARD',
            },
            'order_amount': '100.00',
        }

    def with_delivery(self, shipping_address_id: str = 'ship-a-id'):
        self._object['shipping_address_id'] = shipping_address_id
        return self

    def build(self) -> dict[str, Any]:
        return self._object
