# -*- coding: utf-8 -*-
import datetime

from balance import constants as cst
from balance.actions.unified_account import UnifiedAccountRelations
from tests import object_builder as ob


def add_block_input(nirvana_block, **kwargs):
    nirvana_block.download.side_effect = lambda i: kwargs[i]


def create_invoice(session, rows_count=1, exported=True):
    client = ob.ClientBuilder.construct(session)
    invoice = ob.InvoiceBuilder.construct(
        session,
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[
                    ob.BasketItemBuilder(
                        order=ob.OrderBuilder(
                            client=client,
                            product_id=cst.DIRECT_PRODUCT_RUB_ID
                        ),
                        quantity=666
                    )
                    for _ in range(rows_count)
                ]
            )
        )
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows()
    if exported:
        invoice.enqueue('LOG_TARIFF_ACT', force=True)
        invoice.exports['LOG_TARIFF_ACT'].state = cst.ExportState.exported
    session.flush()
    return invoice


def create_act(invoice):
    invoice.close_invoice(datetime.datetime.now())
    act, = invoice.acts
    return act


def gen_act_external_id(session):
    return session.execute('select bo.s_act_external_id.nextval from dual').scalar()


def mk_currency(client):
    client.set_currency(cst.ServiceId.DIRECT, 'RUB', datetime.datetime(2000, 1, 1), None)


def link_orders(main_order, children):
    UnifiedAccountRelations().link(main_order, children)
    main_order.turn_on_optimize()


def create_client(session):
    client = ob.ClientBuilder.construct(session)
    mk_currency(client)
    return client


def create_order(session, client, product_id=cst.DIRECT_PRODUCT_RUB_ID, **kw):
    order = ob.OrderBuilder.construct(
        session,
        client=client,
        product_id=product_id,
        **kw
    )
    return order
