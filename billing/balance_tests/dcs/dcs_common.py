# coding: utf-8
import contextlib
import datetime as dt

import mock

from balance import mapper
from balance import constants
from balance import muzzle_util as ut
from balance.actions.process_completions import ProcessCompletions
from balance.processors import client_migrate_to_currency

from tests import object_builder as ob


def create_contract(session, **params):
    return ob.ContractBuilder.construct(session, **params)


def create_order(session, client, service_id, product=None, product_id=None, **kwargs):
    assert product or product_id
    if product_id:
        product = ob.Getter(mapper.Product, product_id)
    return ob.OrderBuilder(
        client=client,
        product=product,
        service_id=service_id,
        **kwargs
    ).build(session).obj


def create_product_unit(session, **kwargs):
    return ob.ProductUnitBuilder(**kwargs).build(session).obj


def create_product(session, unit_id, **kwargs):
    return ob.ProductBuilder(unit=ob.Getter(mapper.ProductUnit, unit_id), **kwargs).build(session).obj


def create_client(session, **kwargs):
    return ob.ClientBuilder(**kwargs).build(session).obj


def create_person(session, client=None):
    if client is None:
        client = create_client(session)
    return ob.PersonBuilder(client=client).build(session).obj


def create_overdraft_params(session, service, client, person, client_limit):
    return ob.OverdraftParamsBuilder(
        client=client, service=service, person=person,
        client_limit=client_limit
    ).build(session)


def create_invoice(session, client, person, rows, **kwargs):
    return ob.InvoiceBuilder(
        person=person,
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(
                        order=row[0],
                        quantity=row[1],
                    ) for row in rows
                ]
            )
        ),
        **kwargs
    ).build(session).obj


def process_completions(order, shipment_info=None, stop=0, on_dt=None, force=False):
    if shipment_info:
        order.shipment.update(
            on_dt or dt.datetime.now(),
            shipment_info,
            stop=stop
        )
    ProcessCompletions(order, force_log_tariff_processing=force).process_completions()
    order.session.flush()


def migrate_client(client,
                   convert_type=constants.CONVERT_TYPE_MODIFY,
                   service_id=constants.ServiceId.DIRECT,
                   currency='RUB',
                   on_dt=None):
    migrate_to_currency = on_dt or ut.trunc_date(dt.datetime.now())
    client.set_currency(service_id, currency, migrate_to_currency, convert_type, force=True)
    client_migrate_to_currency.process_client(client, client.exports['MIGRATE_TO_CURRENCY'].input)
    client.session.flush()


@contextlib.contextmanager
def yt_client_mock():
    with mock.patch('balance.utils.yt_helpers.get_token'):
        with mock.patch('yt.wrapper.YtClient') as m:
            yield m
