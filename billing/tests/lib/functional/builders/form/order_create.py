from decimal import Decimal
from typing import Any
from uuid import UUID


class OrderCreateRequestBuilder:
    """
    Параметры запроса в create = параметры ответа из render + пользовательские действия.
    Такая вот формула.
    Поэтому конструктор принимает на вход содержимое ответа из order/render, и ты можешь заслать его обратно прям так,
    либо добавить действий (напр. выбор доставки).
    """

    def __init__(self, merchant_id: UUID, render_response: dict[str, Any]):
        self._object = render_response | {
            'merchant_id': str(merchant_id),
            'payment_method': {'method_type': 'CARD'},
        }

    def build(self) -> dict[str, Any]:
        return self._object

    def with_delivery(self, shipping_address_id: str, shipping_contact_id: str):
        self._object['shipping_address_id'] = shipping_address_id
        # NOTE: заказ получится создать и shipping_contact_id, но доставку зааппрувить не получится
        self._object['shipping_contact_id'] = shipping_contact_id
        return self

    def with_yandex_delivery(
        self,
        chosen_option: dict[str, Any],
        shipping_address_id: str = 'ship-a-id',
        shipping_contact_id: str = 'ship-c-id',
    ):
        self._object['shipping_method'] = {
            'method_type': 'YANDEX_DELIVERY',
            'yandex_delivery_option': chosen_option,
        }
        return (
            self
            .with_delivery(shipping_address_id=shipping_address_id, shipping_contact_id=shipping_contact_id)
            .with_order_amount(self._object['cart']['total']['amount'], chosen_option['amount'])
        )

    def with_order_amount(self, cart_total: str, shipping_price: str):
        self._object['order_amount'] = str(Decimal(cart_total) + Decimal(shipping_price))
        return self
