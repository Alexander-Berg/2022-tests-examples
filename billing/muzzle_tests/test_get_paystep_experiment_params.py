import json

from tests.muzzle_tests.paystep.paystep_common import (
    create_client,
)
import muzzle.api.invoice as invoice_api
from balance.constants import DIRECT_PRODUCT_RUB_ID, ServiceId
from balance import mapper
from tests import object_builder as ob

import hamcrest as hm


def create_request(session, client, orders, qtys):
    basket = ob.BasketBuilder(
        client=client,
        rows=[
            ob.BasketItemBuilder(order=orders[i], quantity=qtys[i])
            for i in xrange(len(orders))
        ]
    )

    return ob.RequestBuilder(basket=basket).build(session).obj


def create_order(session, client, service_id=ServiceId.DIRECT):
    return ob.OrderBuilder(service_id=service_id, client=client,
                           product=ob.Getter(mapper.Product, DIRECT_PRODUCT_RUB_ID))


def create_client(session, with_single_account=False):
    return ob.ClientBuilder(with_single_account=with_single_account).build(session).obj


def create_person(session, client, type='ph'):
    return ob.PersonBuilder(
        type=type, client=client
    ).build(session).obj


def create_contract(session, client, person):
    return ob.ContractBuilder(
        client=client,
        person=person,
    ).build(session).obj


def test_get_paystep_experiment_params(session):
    client = create_client(session, with_single_account=True)
    person = create_person(session, client)
    orders = [create_order(session, client, service_id=3-i) for i in range(3)]
    request = create_request(session, client=client, orders=orders, qtys=[4, 6, 5])
    contract = create_contract(session, client, person)

    res = invoice_api.get_paystep_experiment_params(session, request.id)
    hm.assert_that(
        res,
        hm.has_entries({
            'client_id': client.id,
            'client_region_id': client.region_id or 0,
            'number_of_orders': len(orders),
            'services_in_orders': '1;2;3',
            'orders_qtys': '5;6;4',
            'orders_qtys_sum': '15',
            'is_agency': 0,
            'can_create_endbuyers': 1 if contract.can_have_endbuyer else 0
        }),
    )
