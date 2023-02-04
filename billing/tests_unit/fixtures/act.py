# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import datetime
from decimal import Decimal as D
import pytest
import allure

from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import utils as test_utils


@pytest.fixture(name='act')
@allure.step('create act')
def create_act(qty=D('50'), client=None, firm_id=cst.FirmId.YANDEX_OOO, person=None, extra_params={}):
    session = test_utils.get_test_session()

    if person:
        assert not(client and person.client != client)
        client = person.client
    else:
        client = client or ob.ClientBuilder(*extra_params.get('client', {}))
        person_params = extra_params.get('person', {'person_type': 'ur'})
        person_params["client"] = client
        person = ob.PersonBuilder(**person_params).build(session)
    order_params = extra_params.get('order', {})
    order_params["client"] = client
    order = ob.OrderBuilder(**order_params).build(session).obj
    invoice = ob.InvoiceBuilder(
        person=person,
        request=ob.RequestBuilder(
            firm_id=firm_id,
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=order, quantity=qty)],
            ),
        ),
    ).build(session).obj
    invoice.turn_on_rows()
    order.calculate_consumption(
        dt=datetime.datetime.today() - datetime.timedelta(days=1),
        stop=0,
        shipment_info={'Bucks': qty},
    )

    acts = invoice.generate_act(force=True)
    return acts[0]


@pytest.fixture(name='client_acts')
@allure.step('create client with acts')
def create_client_acts():
    session = test_utils.get_test_session()

    acts = []
    qty = 50
    client = ob.ClientBuilder().build(session).obj

    for _i in range(3):
        order = ob.OrderBuilder(client=client).build(session).obj
        basket_b = ob.BasketBuilder(rows=[ob.BasketItemBuilder(order=order, quantity=qty)])
        request_b = ob.RequestBuilder(basket=basket_b)
        invoice = ob.InvoiceBuilder(request=request_b).build(session).obj
        invoice.turn_on_rows()
        order.calculate_consumption(
            dt=datetime.datetime.today() - datetime.timedelta(days=1),
            stop=0,
            shipment_info={'Bucks': qty},
        )
        acts.extend(invoice.generate_act(force=True))

    return client.id, acts
