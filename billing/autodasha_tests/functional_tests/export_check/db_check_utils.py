# -*- coding: utf-8 -*-

import datetime as dt

from tests.autodasha_tests.common import db_utils

__all__ = ['get_act', 'get_invoice', 'get_person', 'export_object', 'get_client', 'get_contract', 'get_collateral']


def get_invoice(session):
    client, person = db_utils.create_client_person(session)
    order = db_utils.create_order(session, client)

    invoice = db_utils.create_invoice_simple(session, order, [10], 1000, person, dt=dt.datetime.now().replace(day=1))

    return invoice


def get_act(session, invoice=None):
    if not invoice:
        invoice = get_invoice(session)

    invoice.close_invoice(dt.datetime.now())
    act, = invoice.acts
    return act


def get_client(session):
    return db_utils.create_client(session)


def get_person(session):
    return db_utils.create_person(session)


def get_contract(session):
    client, person = db_utils.create_client_person(session)
    return db_utils.create_general_contract(session, client, person, dt.datetime(2016, 1, 1))


def get_collateral(session):
    contract = get_contract(session)
    return db_utils.add_collateral(contract)


def export_object(queue_obj, obj, state=0, rate=0, type_='OEBS'):
    if state == 0 and type_ not in obj.exports:
        # выставление state == 0
        # то же самое, что и enqueue
        obj.enqueue(type_)

    export = obj.exports[type_]
    export.state = state
    export.rate = rate

    proxy = queue_obj.add_object(obj, type_)

    queue_obj.session.flush()
    return export, proxy
