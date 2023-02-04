# -*- coding: utf-8 -*-

import decimal

import pytest
import sqlalchemy as sa
from sqlalchemy import orm

from balance import mapper
from balance.actions.consumption import reverse_consume
from balance import exc
from balance.actions.freefunds.overact import ClientsOveractedFreeFundsQuery
from balance.constants import (
    TransferMode,
)

from .common import (
    extract_qtys,
    create_client,
    create_order,
    create_invoice,
    do_complete,
    do_consume,
    do_act,
    do_consume_state,
    PAYSYS_ID_BANK_UR,
    PAYSYS_ID_CARD_PH,
)

D = decimal.Decimal


def test_uncompleted(session):
    client1 = create_client(session)
    o1 = create_order(session, client1)
    o2 = create_order(session, client1)

    client2 = create_client(session)
    o3 = create_order(session, client2)

    invoice1 = create_invoice(session, client1, o1)
    invoice2 = create_invoice(session, client1, o1)

    co1_1 = invoice1.transfer(o1, TransferMode.dst, 2).consume
    co1_2 = invoice2.transfer(o1, TransferMode.dst, 3).consume

    co2_2 = invoice2.transfer(o2, TransferMode.dst, 4).consume
    co3_2 = invoice2.transfer(o3, TransferMode.dst, 5).consume

    consumes = ClientsOveractedFreeFundsQuery(session, [o1.id, o2.id, o3.id], old_consumes_first=True).get().all()

    req_consumes = [
        (co1_1.id, 2),
        (co1_2.id, 3),
        (co2_2.id, 4),
        (co3_2.id, 5),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_part_completed(session):
    client = create_client(session)
    o = create_order(session, client)
    invoice = create_invoice(session, client, o)

    do_consume(invoice, o, 10)
    co2 = do_consume(invoice, o, 20)
    co3 = do_consume(invoice, o, 30)
    do_complete(o, D('17.666'))

    consumes = ClientsOveractedFreeFundsQuery(session, [o.id], old_consumes_first=True).get().all()

    req_consumes = [
        (co2.id, D('12.334')),
        (co3.id, 30),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_all_completed(session):
    client = create_client(session)
    o = create_order(session, client)
    invoice = create_invoice(session, client, o)

    do_consume(invoice, o, 10)
    do_complete(o, 10)

    consumes = ClientsOveractedFreeFundsQuery(session, [o.id], old_consumes_first=True).get().all()
    assert consumes == []


@pytest.mark.parametrize(
    'paysys_id',
    [
        pytest.param(PAYSYS_ID_BANK_UR, id='base'),
        pytest.param(PAYSYS_ID_CARD_PH, id='force'),
    ]
)
def test_all_acted_single_consume(session, paysys_id):
    client = create_client(session)
    o = create_order(session, client)
    invoice = create_invoice(session, client, o, paysys_id=paysys_id)

    do_consume_state(invoice, o, 10, 0, 10)

    consumes = ClientsOveractedFreeFundsQuery(session, [o.id], old_consumes_first=True).get().all()
    assert consumes == []


@pytest.mark.parametrize(
    'paysys_id, acted_qty, free_qty',
    [
        pytest.param(PAYSYS_ID_BANK_UR, 10, 0, id='full_base'),
        pytest.param(PAYSYS_ID_CARD_PH, 10, 10, id='full_force'),
        pytest.param(PAYSYS_ID_BANK_UR, 9, 1, id='part_base'),
        pytest.param(PAYSYS_ID_CARD_PH, 9, 10, id='part_force'),
    ]
)
def test_acted_other_consume_free(session, paysys_id, acted_qty, free_qty):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1, paysys_id=paysys_id)

    do_consume(invoice, o1, 10)
    co = do_consume_state(invoice, o2, 10, 0, acted_qty)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()
    if free_qty:
        req_consumes = [
            (co.id, free_qty),
        ]
    else:
        req_consumes = []
    assert extract_qtys(consumes) == req_consumes


@pytest.mark.parametrize(
    'paysys_id',
    [
        pytest.param(PAYSYS_ID_BANK_UR, id='base'),
        pytest.param(PAYSYS_ID_CARD_PH, id='force'),
    ]
)
def test_all_acted_other_consume(session, paysys_id):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1, paysys_id=paysys_id)

    do_consume_state(invoice, o1, 10, 0, 10)
    co2 = do_consume(invoice, o2, 10)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()
    req_consumes = [
        (co2.id, 10),
    ]
    assert extract_qtys(consumes) == req_consumes


@pytest.mark.parametrize(
    'completion_qty',
    [
        pytest.param(0, id='full'),
        pytest.param(1, id='part'),
    ]
)
def test_overacted_other_consume(session, completion_qty):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1)

    do_consume_state(invoice, o1, 3, completion_qty, 10)

    co2 = do_consume(invoice, o2, 4)
    co3 = do_consume(invoice, o2, 4)
    do_consume(invoice, o2, 4)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()
    req_consumes = [
        (co2.id, 4),
        (co3.id, 1),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_overacted_other_consume_border_case(session):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1)

    do_consume_state(invoice, o1, 0, 0, 10)

    co1 = do_consume(invoice, o2, 2)
    co2 = do_consume(invoice, o2, 3)
    do_consume(invoice, o2, 5)
    do_consume(invoice, o2, 5)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()
    req_consumes = [
        (co1.id, 2),
        (co2.id, 3),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_other_client_overacted(session):
    client1 = create_client(session)
    o1 = create_order(session, client1)
    invoice = create_invoice(session, client1, o1)

    client2 = create_client(session)
    o2 = create_order(session, client2)

    do_consume_state(invoice, o1, 0, 0, 30)

    co2 = do_consume(invoice, o2, 10)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()
    req_consumes = [
        (co2.id, 10),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_overacted_other_consume_part_completed_free(session):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1)

    do_consume_state(invoice, o1, 0, 0, 10)
    do_consume(invoice, o1, 6)

    co2 = do_consume(invoice, o2, 10)
    do_complete(o2, 3)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()
    req_consumes = [
        (co2.id, 6),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_overacted_other_consume_part_overacted_free(session):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1)

    do_consume_state(invoice, o1, 0, 0, 10)
    do_consume(invoice, o1, 6)

    co2 = do_consume_state(invoice, o2, 10, 0, 3)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()
    req_consumes = [
        (co2.id, 6),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_zero_sum_after_overact(session):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1)

    do_consume_state(invoice, o1, 0, 0, 30)

    co1 = do_consume(invoice, o2, D('0.001'))
    co2 = do_consume(invoice, o2, 2)
    do_consume(invoice, o2, D('0.002'))
    do_consume(invoice, o2, 30)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()

    req_consumes = [
        (co1.id, D('0.001')),
        (co2.id, 2),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_zero_sum_inside_overact(session):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1)

    do_consume_state(invoice, o1, 0, 0, 30)

    co1 = do_consume(invoice, o2, D('0.001'))
    co2 = do_consume(invoice, o2, 1)
    co3 = do_consume(invoice, o2, D('0.002'))
    co4 = do_consume(invoice, o2, 3)
    do_consume(invoice, o2, D('0.003'))
    do_consume(invoice, o2, 29)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()

    req_consumes = [
        (co1.id, D('0.001')),
        (co2.id, 1),
        (co3.id, D('0.002')),
        (co4.id, 2),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_invoice_only_zero_consumes(session):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1)

    do_consume(invoice, o2, D('0.001'))
    do_consume(invoice, o2, D('0.002'))

    consumes = ClientsOveractedFreeFundsQuery(session, invoice_ids=[invoice.id]).get().all()
    assert not consumes


def test_multiple_invoices_clients(session):
    client1 = create_client(session)
    client2 = create_client(session)
    o1 = create_order(session, client1)
    o2 = create_order(session, client1)
    o3 = create_order(session, client2)

    invoice1 = create_invoice(session, client1, o1)
    invoice2 = create_invoice(session, client1, o1)
    invoice3 = create_invoice(session, client1, o1)

    do_consume_state(invoice1, o3, 0, 0, 10)
    do_consume_state(invoice2, o1, 0, 0, 10)
    do_consume_state(invoice3, o3, 0, 0, 10)

    co1 = do_consume(invoice1, o2, 10)
    co2 = do_consume(invoice2, o2, 20)
    co3 = do_consume(invoice3, o2, 30)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()

    req_consumes = [
        (co1.id, 10),
        (co2.id, 10),
        (co3.id, 30),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_price(session):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1)

    co1 = invoice.transfer(o1, TransferMode.src, 10, discount_obj=mapper.DiscountObj(D('33.33'))).consume
    do_complete(o1, co1.current_qty)
    do_act(invoice)
    do_complete(o1, 0)
    reverse_consume(co1, None, co1.current_qty)

    co2 = invoice.transfer(o2, TransferMode.src, 15, discount_obj=mapper.DiscountObj(D('66.66'))).consume

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=True).get().all()

    req_consumes = [(co2.id, D('14.997000'))]
    assert extract_qtys(consumes) == req_consumes


def test_old_consumes_last(session):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)
    invoice = create_invoice(session, client, o1)

    do_consume_state(invoice, o1, 3, 3, 10)

    do_consume(invoice, o2, 4)
    co2 = do_consume(invoice, o2, 4)
    co3 = do_consume(invoice, o2, 4)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], old_consumes_first=False).get().all()
    req_consumes = [
        (co3.id, 4),
        (co2.id, 1),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_invoices_filter(session):
    client = create_client(session)
    o1 = create_order(session, client)
    o2 = create_order(session, client)

    invoice1 = create_invoice(session, client, o1)
    invoice2 = create_invoice(session, client, o1)
    invoice3 = create_invoice(session, client, o1)

    do_consume_state(invoice2, o1, 0, 0, 2)
    do_consume(invoice1, o2, 10)
    co2_2 = do_consume(invoice2, o2, 20)
    do_consume(invoice3, o2, 30)

    consumes = ClientsOveractedFreeFundsQuery(session, [o2.id], [invoice2.id]).get().all()

    req_consumes = [
        (co2_2.id, 18),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_invalid_base_filter(session):
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        ClientsOveractedFreeFundsQuery(session, [], [])

    assert 'Should specify at least some filtering criteria' in exc_info.value.msg


def test_add_filter(session):
    client = create_client(session)
    o = create_order(session, client)
    invoice = create_invoice(session, client, o)

    co1 = do_consume_state(invoice, o, 0, 0, 5)
    co2 = do_consume(invoice, o, 3)
    co3 = do_consume(invoice, o, 6)

    consumes = (
        ClientsOveractedFreeFundsQuery(session, [o.id])
            .get(sa.not_(mapper.Consume.id.in_([co1.id, co2.id])))
            .all()
    )

    req_consumes = [
        (co3.id, 4),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_query_manip(session):
    client = create_client(session)
    o = create_order(session, client)
    invoice = create_invoice(session, client, o)

    do_consume(invoice, o, 3)
    co2 = do_consume(invoice, o, 6)

    consumes = (
        ClientsOveractedFreeFundsQuery(session, [o.id])
            .get()
            .filter(mapper.Consume.id == co2.id)
            .options(orm.joinedload(mapper.Consume.order))
            .all()
    )

    req_consumes = [
        (co2.id, 6),
    ]
    assert extract_qtys(consumes) == req_consumes
