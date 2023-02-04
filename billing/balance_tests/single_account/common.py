# -*- coding: utf-8 -*-

from balance.constants import ServiceId

from tests.object_builder import ProductBuilder, RequestBuilder, BasketBuilder, OrderBuilder, BasketItemBuilder


QUANTITY = 10
PRICE = 10

SERVICE_PRODUCT_DATA = {ServiceId.DIRECT: {'commission_type': 7, 'discount_type': 7},
                        ServiceId.GEOCON: {'commission_type': 13, 'discount_type': 1}}


def get_request_builder(client, service_ids):
    return RequestBuilder(
        basket=BasketBuilder(
            rows=[BasketItemBuilder(
                quantity=QUANTITY,
                order=OrderBuilder(
                    client=client,
                    product=ProductBuilder(
                        price=PRICE,
                        engine_id=service_id,
                        commission_type=SERVICE_PRODUCT_DATA.get(service_id, {}).get('commission_type', 0),
                        media_discount=SERVICE_PRODUCT_DATA.get(service_id, {}).get('discount_type', 0),
                    ),
                    service_id=service_id)
            ) for service_id in service_ids]
        )
    )
