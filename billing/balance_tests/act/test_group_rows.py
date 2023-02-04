# -*- coding: utf-8 -*-

import datetime
from itertools import chain
import functools
import pytest

from balance.actions import acts as a_a
from balance import mapper, constants as cst
from balance.actions.acts.account import (get_act_row,
                                          group_rows,
                                          _gen_acts,
                                          get_invoice_desired_sums,
                                          get_act_prev_sums)

from balance.actions.invoice_turnon import InvoiceTurnOn
from tests import object_builder as ob

from balance.actions.acts.utils import gen_act_label


def create_invoice(session, client=None, agency=None, orders=None):
    if not orders:
        orders = [ob.OrderBuilder(
            client=client,
            service=ob.Getter(mapper.Service, 7),
            product=ob.ProductBuilder(media_discount=7),
            agency=agency
        ).build(session).obj]
    rows = [ob.BasketItemBuilder(order=order, quantity=900) for order in orders]
    request_b = ob.RequestBuilder(basket=ob.BasketBuilder(rows=rows, client=agency or orders[0].client))
    invoice = ob.InvoiceBuilder(request=request_b,
                                internal_rate=15,
                                credit=2
                                ).build(session).obj

    InvoiceTurnOn(invoice, sum=invoice.effective_sum, manual=True).do()

    for order in orders:
        order.calculate_consumption(dt=datetime.datetime.today() - datetime.timedelta(days=2), stop=0,
                                    shipment_info={'Money': 800, 'Shows': 0, 'Clicks': 0, 'Units': 0, 'Bucks': 800})
    return invoice


def get_gen_params(invoice):
    f_is_docs = mapper.get_printable_docs_func(invoice.client)
    pt_groups = invoice.payment_term_groups(on_dt=datetime.datetime.now(),
                                            clients=[c.order.client for c in invoice.consumes])
    pt_client_map = dict(chain.from_iterable(((v, g) for v in vals) for g, vals in pt_groups))
    return f_is_docs, pt_groups, pt_client_map

@pytest.mark.parametrize('agency_is_docs_separated', [True, False])
@pytest.mark.parametrize('agency_is_docs_detailed', [True, False])
@pytest.mark.parametrize('w_agency', [True, False])
@pytest.mark.parametrize('is_docs_separated', [1, 0])
@pytest.mark.parametrize('is_docs_detailed', [1, 0])
@pytest.mark.parametrize('apply_imho_patch', [True, False])
def test_group_rows_docs(session, is_docs_separated, is_docs_detailed, w_agency, agency_is_docs_separated,
                         agency_is_docs_detailed, apply_imho_patch):
    """
    Если стоит признак отдельные документы, в ключе группировки возвращаем клиента,
    """
    client = ob.ClientBuilder.construct(session)
    agency = ob.ClientBuilder.construct(session, is_agency=1) if w_agency else None

    invoice = create_invoice(session, client=client, agency=agency)
    invoice.client.is_docs_separated = is_docs_separated
    invoice.client.is_docs_detailed = is_docs_detailed
    if w_agency:
        client.agencies_printable_doc_types = {str(agency.id): (agency_is_docs_detailed, agency_is_docs_separated)}

    row = get_act_row(invoice.consumes[0], 900, force=1)
    f_is_docs, pt_groups, pt_client_map = get_gen_params(invoice)
    result = group_rows(contract_state=None,
                        f_is_docs=f_is_docs,
                        pt_map=pt_client_map,
                        row=row,
                        on_dt=datetime.datetime.now(),
                        overact_splitted=False,
                        apply_imho_patch=apply_imho_patch
                        )
    if w_agency:
        docs_key = client if agency_is_docs_separated else agency_is_docs_detailed
    else:
        docs_key = client if is_docs_separated else is_docs_detailed

    req_result = [
        docs_key,
        None,
        7,
        0,
        False,
        None,
        invoice.consumes[0].tax_policy_pct.policy
    ]

    if not apply_imho_patch:
        req_result.append(None)

    assert result == tuple(req_result)


@pytest.mark.parametrize('agency_is_docs_separated', [1, 0])
@pytest.mark.parametrize('agency_is_docs_detailed', [1, 0])
@pytest.mark.parametrize('client_is_docs_separated', [1, 0])
@pytest.mark.parametrize('client_is_docs_detailed', [1, 0])
def test_gen_acts(session, agency_is_docs_separated, agency_is_docs_detailed,
                  client_is_docs_separated, client_is_docs_detailed):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    orders = [ob.OrderBuilder(
        client=client,
        service=ob.Getter(mapper.Service, 7),
        product=ob.ProductBuilder(media_discount=7),
        agency=agency
    ).build(session).obj for client in [ob.ClientBuilder.construct(session) for _ in range(2)]]

    orders[0].client.is_docs_separated = client_is_docs_separated
    orders[0].client.is_docs_detailed = client_is_docs_detailed

    invoice = create_invoice(session, orders=orders, agency=agency)
    invoice.client.is_docs_separated = agency_is_docs_separated
    invoice.client.is_docs_detailed = agency_is_docs_detailed

    session.flush()
    f_is_docs, pt_groups, pt_client_map = get_gen_params(invoice)
    f_group_rows = functools.partial(group_rows,
                                     None,
                                     f_is_docs,
                                     pt_client_map,
                                     session.now)
    month = a_a.ActMonth(for_month=invoice.dt)
    operation = mapper.Operation(cst.OperationTypeIDs.generate_acts,
                                 parent_operation_id=None,
                                 client_id=invoice.client.id)
    operation.invoice_desired_sums = get_invoice_desired_sums(session, invoice)
    operation.act_prev_sums = get_act_prev_sums(session, invoice)
    acts = _gen_acts(invoice,
                     consume_iter=invoice.consumes,
                     consume_dt2compl={(consume.id, session.now()): 900 for consume in invoice.consumes},
                     backdate=session.now(),
                     force=1,
                     f_group_rows=f_group_rows,
                     act_month=month,
                     operation=operation,
                     g_label=(gen_act_label(),))

    if agency_is_docs_separated:
        assert len(acts) == 2
    else:
        assert len(acts) == 1
