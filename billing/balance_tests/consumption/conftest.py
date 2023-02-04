# -*- coding: utf-8 -*-

import pytest

from balance.constants import (
    DIRECT_PRODUCT_RUB_ID,
)

from tests import object_builder as ob


@pytest.fixture
def order(session):
    return ob.OrderBuilder.construct(session, product_id=DIRECT_PRODUCT_RUB_ID)


@pytest.fixture
def invoice(session, order):
    return ob.InvoiceBuilder(
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=order.client,
                rows=[ob.BasketItemBuilder(order=order, quantity=666)]
            )
        )
    ).build(session).obj
