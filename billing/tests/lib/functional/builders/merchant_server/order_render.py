from typing import Any


class OrderRenderResponseBuilder:
    """
    Параметры запроса в create = параметры ответа из render + пользовательские действия.
    Такая вот формула.
    Поэтому конструктор принимает на вход содержимое ответа из order/render, и ты можешь заслать его обратно прям так,
    либо добавить действий (напр. выбор доставки).
    """

    def __init__(self):
        self._object = {
            "currencyCode": 'XTS',
            "order_amount": "350",
            "availablePaymentMethods": ['CARD'],
            "cart": {
                "items": [
                    {
                        "title": "Слон",
                        "quantity": {"count": "1"},
                        "total": "350.0",
                        "product_id": "p2",
                        "type": "PHYSICAL",
                    }
                ],
                "total": {  # Суммарная стоимость корзины (без учёта доставки)
                    "amount": "350.00",
                },
            },
        }

    def build(self) -> dict[str, Any]:
        return self._object

    def with_yandex_delivery(self):
        for item in self._object['cart']['items']:
            item['measurements'] = {"length": 0.2, "weight": 1.5, "width": 0.1, "height": 0.1}
        self._object['shipping'] = {
            "availableMethods": ["YANDEX_DELIVERY"],
            "yandexDelivery": {
                "warehouse": {
                    "contact": {
                        "firstName": "John",
                        "secondName": None,
                        "lastName": "Doe",
                        "email": "john@email.test",
                        "phone": "+70001112233",
                    },
                    "emergencyContact": {
                        "firstName": "Jane",
                        "secondName": None,
                        "lastName": "Doe",
                        "email": "jane@email.test",
                        "phone": "+70004445566",
                    },
                    "address": {
                        "country": "Российская Федерация",
                        "region": "Москва",
                        "locality": "Москва",
                        "district": None,
                        "street": "Льва Толстого",
                        "building": "16",
                        "room": None,
                        "entrance": None,
                        "floor": None,
                        "intercom": None,
                        "zip": None,
                        "locale": None,
                        "comment": None,
                        "addressLine": "Российская Федерация, Москва, Льва Толстого, 16",
                        "location": {
                            "longitude": "37.588592",
                            "latitude": "55.734045",
                        },
                    },
                }
            },
        }
        return self
