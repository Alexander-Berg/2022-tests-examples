package ru.yandex.supercheck

val SHOP_LIST_RESPONSE = """
        {
            "result": [
                "1",
                "2",
                "3"
            ],
            "collections": {
                "promo": {
                    "aeadc06d-5cea-4855-a118-ed9225988422": {
                        "entity": "promo",
                        "id": "aeadc06d-5cea-4855-a118-ed9225988422",
                        "name": "promo_1",
                        "description": "not very long promo description, written by Ivan Anisimov, published by fmcg main backend"
                    },
                    "48738d4c-07c0-4286-96bb-85ad35c9f735": {
                        "entity": "promo",
                        "id": "48738d4c-07c0-4286-96bb-85ad35c9f735",
                        "name": "promo_2",
                        "description": "oh my"
                    }
                },
                "product": {
                    "1": {
                        "entity": "product",
                        "id": "1",
                        "dbId": "1",
                        "name": "Product_name_1",
                        "image": {
                            "group": "1393174",
                            "image": "product_25218_917a45f43e7a6ba0b64eb47c0399633d"
                        },
                        "images": [
                            {
                                "group": "1393174",
                                "image": "product_25218_917a45f43e7a6ba0b64eb47c0399633d"
                            }
                        ],
                        "priceFrom": 100,
                        "isPromo": true
                    }
                },
                "promoSet": {
                    "e95dcc81-1a64-4fc1-a9c1-f0b2ce474f40": {
                        "entity": "promoSet",
                        "id": "e95dcc81-1a64-4fc1-a9c1-f0b2ce474f40",
                        "retailChain": "1",
                        "outlet": "1",
                        "items": [
                            "9550889f-0dd2-4940-89d5-8c1de55365ef",
                            "ca01c8b5-e1e8-4c10-8ec3-3862b35f2ac0",
                            "6e2f98ed-9c33-4ec6-a553-a3836e2bbae7"
                        ]
                    },
                    "9f184301-8cf1-4066-83b3-b801dcf5dfd3": {
                        "entity": "promoSet",
                        "id": "9f184301-8cf1-4066-83b3-b801dcf5dfd3",
                        "retailChain": "2",
                        "outlet": "2",
                        "items": [
                            "9550889f-0dd2-4940-89d5-8c1de55365ef",
                            "ca01c8b5-e1e8-4c10-8ec3-3862b35f2ac0",
                            "6e2f98ed-9c33-4ec6-a553-a3836e2bbae7"
                        ]
                    },
                    "55fc8685-b567-4056-8d4a-6c6d3579eb15": {
                        "entity": "promoSet",
                        "id": "55fc8685-b567-4056-8d4a-6c6d3579eb15",
                        "retailChain": "3",
                        "items": [
                            "9550889f-0dd2-4940-89d5-8c1de55365ef",
                            "ca01c8b5-e1e8-4c10-8ec3-3862b35f2ac0",
                            "6e2f98ed-9c33-4ec6-a553-a3836e2bbae7"
                        ]
                    }
                },
                "outletOptions": {
                    "9380dbab-429c-442a-919a-d7ecd8a1f88b": {
                        "entity": "outletOptions",
                        "id": "9380dbab-429c-442a-919a-d7ecd8a1f88b",
                        "outlet": "1",
                        "distance": 1000
                    },
                    "3bb7264a-2c90-49fe-aad9-398f5dc4af48": {
                        "entity": "outletOptions",
                        "id": "3bb7264a-2c90-49fe-aad9-398f5dc4af48",
                        "outlet": "2",
                        "distance": 1500
                    }
                },
                "promoSetItem": {
                    "9550889f-0dd2-4940-89d5-8c1de55365ef": {
                        "entity": "promoSetItem",
                        "id": "9550889f-0dd2-4940-89d5-8c1de55365ef",
                        "type": "PROMO",
                        "promo": "aeadc06d-5cea-4855-a118-ed9225988422"
                    },
                    "ca01c8b5-e1e8-4c10-8ec3-3862b35f2ac0": {
                        "entity": "promoSetItem",
                        "id": "ca01c8b5-e1e8-4c10-8ec3-3862b35f2ac0",
                        "type": "PROMO",
                        "promo": "48738d4c-07c0-4286-96bb-85ad35c9f735"
                    },
                    "6e2f98ed-9c33-4ec6-a553-a3836e2bbae7": {
                        "entity": "promoSetItem",
                        "id": "6e2f98ed-9c33-4ec6-a553-a3836e2bbae7",
                        "type": "PRODUCT",
                        "product": "1"
                    }
                },
                "shoppingListInfo": {
                    "2524b72d-ffba-4ab0-9085-f9c0365fa3a3": {
                        "entity": "shoppingListInfo",
                        "id": "2524b72d-ffba-4ab0-9085-f9c0365fa3a3",
                        "retailChain": "2",
                        "outlet": "2",
                        "count": 3
                    }
                },
                "outlet": {
                    "1": {
                        "entity": "outlet",
                        "id": "1",
                        "retailChain": "1",
                        "address": "улица Шарикоподшипниковская, дом 7, строение 1, корпус 2",
                        "lat": "25.1",
                        "lon": "24.1"
                    },
                    "2": {
                        "entity": "outlet",
                        "id": "2",
                        "retailChain": "1",
                        "address": "улица Скобелевская, 5",
                        "lat": "26.1",
                        "lon": "27.1"
                    }
                },
                "retailChain": {
                    "1": {
                        "entity": "retailChain",
                        "id": "1",
                        "name": "brand_1",
                        "color": "#BF1B1B",
                        "type": "OFFLINE",
                        "systemName": "rc_name_1"
                    },
                    "2": {
                        "entity": "retailChain",
                        "id": "2",
                        "name": "brand_2",
                        "color": "#BF1B1B",
                        "type": "OFFLINE",
                        "systemName": "rc_name_2"
                    },
                    "3": {
                        "entity": "retailChain",
                        "id": "3",
                        "name": "brand_3",
                        "color": "#BF1B1B",
                        "type": "ONLINE",
                        "systemName": "rc_name_3"
                    }
                },
                "deliveryOptions": {
                    "1affd92c-0db7-4e0c-a8cc-49cf2a57b057": {
                        "entity": "deliveryOptions",
                        "id": "1affd92c-0db7-4e0c-a8cc-49cf2a57b057",
                        "retailChain": "3",
                        "deliveryTermsShort": "доставим примерно на следующий день, наверное, ну или послезавтра"
                    }
                }
            },
            "sorting": {
                "promo": [
                    "aeadc06d-5cea-4855-a118-ed9225988422",
                    "48738d4c-07c0-4286-96bb-85ad35c9f735"
                ],
                "product": [
                    "1"
                ],
                "promoSet": [
                    "e95dcc81-1a64-4fc1-a9c1-f0b2ce474f40",
                    "9f184301-8cf1-4066-83b3-b801dcf5dfd3",
                    "55fc8685-b567-4056-8d4a-6c6d3579eb15"
                ],
                "outletOptions": [
                    "9380dbab-429c-442a-919a-d7ecd8a1f88b",
                    "3bb7264a-2c90-49fe-aad9-398f5dc4af48"
                ],
                "promoSetItem": [
                    "9550889f-0dd2-4940-89d5-8c1de55365ef",
                    "ca01c8b5-e1e8-4c10-8ec3-3862b35f2ac0",
                    "6e2f98ed-9c33-4ec6-a553-a3836e2bbae7"
                ],
                "shoppingListInfo": [
                    "2524b72d-ffba-4ab0-9085-f9c0365fa3a3"
                ],
                "outlet": [
                    "1",
                    "2"
                ],
                "deliveryOptions": [
                    "1affd92c-0db7-4e0c-a8cc-49cf2a57b057"
                ]
            }
        }
    """