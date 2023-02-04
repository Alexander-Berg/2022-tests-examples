# -*- coding: utf-8 -*-

import decimal

import pytest
import hamcrest as hm

from balance import mapper
from balance.actions.freefunds.overact import OveractedFreeFundsQuery
from balance.actions.consumption import reverse_consume
from balance.constants import (
    TransferMode,
)

from .common import (
    extract_qtys,
    create_invoice,
    do_consume_state,
    do_complete,
    do_consume,
    do_act,
)

D = decimal.Decimal


def test_uncompleted(session, invoice, order_qty, order_rur):
    """invoice1
    - order1
      2/60 0/0 0/0
    - order2
      100/100 0/0 0/0
    Overact: 0
    Free funds: 102
    """
    co1 = do_consume(invoice, order_qty, 2)
    co2 = do_consume(invoice, order_rur, 100)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [
        (co1.id, 2),
        (co2.id, 100),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_part_completed(session, invoice, order_rur):
    """invoice1
    order1
    - 1000/1000 666/666 0/0
    Overact: 0
    Completed: 666
    Free funds: 334
    """
    co = do_consume(invoice, order_rur, 1000)
    do_complete(order_rur, 666)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [(co.id, 334)]
    assert extract_qtys(consumes) == req_consumes


def test_all_completed(session, invoice, order_rur):
    """invoice1
    order1
    - 1000/1000 1000/1000 0/0
    Overacted: 0
    Completed: 1000
    Free funds: 0
    """
    invoice.transfer(order_rur)
    do_complete(order_rur, order_rur.consume_qty)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()
    assert consumes == []


def test_all_overacted_single_consume(session, invoice, order_rur):
    """invoice1
    order1
    - 1000/0 0/0 1000/1000
    Overacted: 1000
    Completed: 0
    Free funds:0
    """
    do_consume(invoice, order_rur, 1000)
    do_complete(order_rur, 1000)
    do_act(invoice)
    do_complete(order_rur, 0)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()
    assert consumes == []


def test_all_overacted_other_consume(session, invoice, order_rur, order_qty):
    """invoice1
    order1
    - 0/0 0/0 1000/1000
    order2
    - 33.33/1000 0/0 0/0
    Overacted: 1000 всё перекрученное компенсируется начисленным на второй конзюм
    Completed: 0
    Free funds: 0
    """
    do_consume_state(invoice, order_rur, 0, 0, 300)
    do_consume(invoice, order_qty, 10)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()
    assert consumes == []


def test_underacted_single_consume(session, invoice, order_rur):
    """invoice1
    order1
    - 1000/1000 800/800 500/500
    Overacted: 0
    Completed: 800
    Free funds: 200
    """
    co = do_consume(invoice, order_rur, 1000)
    do_complete(order_rur, 500)
    do_act(invoice)
    do_complete(order_rur, 800)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()
    req_consumes = [(co.id, 200)]
    assert extract_qtys(consumes) == req_consumes


def test_overacted_single_consume(session, invoice, order_rur):
    """invoice1
    order1
    - 1000/1000 500/500 750/750
    Overacted: 0
    Completed: 500
    Free funds: 250
    """
    co = do_consume(invoice, order_rur, 1000)
    do_complete(order_rur, 750)
    do_act(invoice)
    do_complete(order_rur, 500)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [(co.id, 250)]
    assert extract_qtys(consumes) == req_consumes


def test_overacted_other_consume(session, invoice, order_rur, order_qty):
    """invoice1
    order1
    - 0/0 0/0 300/300
    order2
    - 20/600 0/0 0/0
    Overacted: 300
    Free funds: (600 - 300) * 20/600 = 10
    """
    do_consume(invoice, order_rur, 600)
    do_complete(order_rur, 300)
    do_act(invoice)
    do_complete(order_rur, 0)
    order_rur.transfer(order_qty)
    co2, = order_qty.consumes
    invoice.session.flush()

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [(co2.id, 10)]
    assert extract_qtys(consumes) == req_consumes


def test_overacted_other_consume_part_completed(session, invoice, order_rur, order_qty):
    """invoice1
    order1
    - 0/0 0/0 300/300
    order2
    - 3/90 3/90 0/0
    order1
    - 510/510 0/0 0/0
    - 66/66 0/0 0/0
    Overacted: 300
    Free funds: сортировка по количеству свободных средств:
    0 (тут переакт был), 0 (90 на компенсацию), 300 (210 на компенсацию), 66 (здесь уже не трогаем)
    """
    do_consume(invoice, order_rur, 600)
    do_complete(order_rur, 300)
    do_act(invoice)
    do_complete(order_rur, 0)
    order_rur.transfer(order_qty)
    do_complete(order_qty, 3)
    order_qty.transfer(order_rur)
    do_consume(invoice, order_rur, 66)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [
        (order_rur.consumes[-2].id, 300),
        (order_rur.consumes[-1].id, 66),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_other_consume_overacted(session, invoice, order_rur, order_qty):
    """invoice1
    order1
    - 0/0 0/0 400/400
    - 210/210 100/100 0/0  с неё спишутся впервую очередь, тк есть открутки
    - 1/1 0/0 0/0
    - 200/200 0/0 0/0
    - 66/66 0/0 0/0
    Overacted: 400 - 100 = 300
    Free funds: сортировка по модулю переакта, а потом по свободному количеству:
    0, 0 (все 210 зачислены на переакт), 1, 10 (190 переакта оставалось), 66
    """
    co0 = do_consume(invoice, order_rur, 400)
    do_complete(order_rur, 400)
    do_act(invoice)
    do_complete(order_rur, 0)
    reverse_consume(co0, None, 400)

    co1 = do_consume(invoice, order_rur, 210)
    do_complete(order_rur, 100)
    co2 = do_consume(invoice, order_rur, 1)
    co3 = do_consume(invoice, order_rur, 200)
    co4 = do_consume(invoice, order_rur, 66)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [
        (co2.id, 1),
        (co3.id, 10),
        (co4.id, 66),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_qty2_sum(session, invoice, order_rur):
    """Расчет свободных средств производим в сумме,
    а возвращаем количество
    invoice1
    order1
    - 0/0 0/0 8/8
    - 20/10 0/0 0/0
    Overact: 8
    Free funds: (10 - 8) * 20/10 = 4
    """
    co1 = do_consume(invoice, order_rur, 8)
    do_complete(order_rur, 8)
    do_act(invoice)
    do_complete(order_rur, 0)
    reverse_consume(co1, None, 8)

    co2 = invoice.transfer(order_rur, TransferMode.src, 10, discount_obj=mapper.DiscountObj(0, 50)).consume

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [(co2.id, 4)]
    assert extract_qtys(consumes) == req_consumes


def test_qty2_sum_2(session, invoice, order_rur, order_qty):
    """Проверяем, что количество свободных средств посчитано правильно, несмотря на коэф.
    invoice1
    order1
    - 0/0 0/0 30/30
    order2
    - 2/60 0/0 0/0
    Overacted: 30
    Free funds: 1
    """
    co1 = do_consume(invoice, order_rur, 60)
    do_complete(order_rur, 30)
    do_act(invoice)
    do_complete(order_rur, 0)

    order_rur.transfer(order_qty)
    co2 = invoice.consumes[-1]

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [(co2.id, 1)]
    assert extract_qtys(consumes) == req_consumes


def test_overacted_other_consume_part_completed_free(session, invoice, order_rur, order_qty):
    """invoice1
    order1
    - 0/0 0/0 300/300
    order2
    - 5/150 3/90 0/0
    order1
    - 450/450 0/0 0/0
    - 66/0 0/0 0/0
    Overacted: 300
    Free funds: 0 (150 на компенсацию), 300 (150 на компенсацию), 66
    """
    do_consume(invoice, order_rur, 600)
    do_complete(order_rur, 300)
    do_act(invoice)
    do_complete(order_rur, 0)
    order_rur.transfer(order_qty)
    do_complete(order_qty, 3)
    order_qty.transfer(order_rur, TransferMode.src, 15)
    do_consume(invoice, order_rur, 66)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [
        (order_rur.consumes[-2].id, 300),
        (order_rur.consumes[-1].id, 66),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_zero_sum_after_overact(session, invoice, order_rur, order_qty):
    """invoice1
    order1
    - 0/0 0/0 1/30
    order2
    - 30/30 0/0 0/0
    - 0.001/0 0/0 0/0
    - 1/1 0/0 0/0
    - 0.002/0 0/0 0/0
    Overacted: 30
    Free funds: потому что по порядку:
    0 (здесь переакт), 0 (компенсация), 0.001, 1, 0.002
    """
    do_consume(invoice, order_qty, 1)
    do_complete(order_qty, 1)
    do_act(invoice)
    do_complete(order_qty, 0)
    order_qty.transfer(order_rur)

    co1 = do_consume(invoice, order_rur, D('0.001'))
    co2 = do_consume(invoice, order_rur, 1)
    co3 = do_consume(invoice, order_rur, D('0.002'))

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [
        (co1.id, D('0.001')),
        (co2.id, 1),
        (co3.id, D('0.002')),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_zero_sum_inside_overact(session, invoice, order_rur, order_qty):
    """invoice1
    order1
    - 0/0 0/0 1/30
    order2
    - 29/29 0/0 0/0
    - 0.001/0 0/0 0/0
    - 1/1 0/0 0/0
    - 0.002/0 0/0 0/0
    - 3/3 0/0 0/0
    - 0.003/0 0/0 0/0
    Overacted: 30
    Free funds: благодаря сортировке по свободным средствам получим:
    0, 0 (29 на компенсацию), 0.001, 1, 0.002, 2 (1 уходит на компенсацию переакта, тк это второй по величине свободного остатка), 0.003
    """
    do_consume(invoice, order_qty, 1)
    do_complete(order_qty, 1)
    do_act(invoice)
    do_complete(order_qty, 0)
    co_overact, = order_qty.consumes
    reverse_consume(co_overact, None, 1)

    do_consume(invoice, order_rur, 29)
    co1 = do_consume(invoice, order_rur, D('0.001'))
    co2 = do_consume(invoice, order_rur, 1)
    co3 = do_consume(invoice, order_rur, D('0.002'))
    co4 = do_consume(invoice, order_rur, 3)
    co5 = do_consume(invoice, order_rur, D('0.003'))

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get().all()

    req_consumes = [
        (co1.id, D('0.001')),
        (co2.id, 1),
        (co3.id, D('0.002')),
        (co4.id, 2),
        (co5.id, D('0.003')),
    ]
    assert extract_qtys(consumes) == req_consumes


def test_w_filter(session, invoice, order_rur, order_qty):
    """invoice1
    order1
    - 0/0 0/0 300/300
    order2
    - 20/600 0/0 0/0
    Overacted: 300
    Free funds: 20 (т.к. мы фильтруем только по order1, поэтому не видим переакта на другом заказе)
    """
    do_consume(invoice, order_rur, 600)
    do_complete(order_rur, 300)
    do_act(invoice)
    do_complete(order_rur, 0)
    order_rur.transfer(order_qty)
    co2, = order_qty.consumes
    invoice.session.flush()

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).base_filter(mapper.Consume.order == order_qty).get().all()

    req_consumes = [(co2.id, 20)]
    assert extract_qtys(consumes) == req_consumes


@pytest.mark.parametrize(
    'use_base_filter',
    [True, False],
)
def test_w_filters(session, invoice, order_rur, order_qty, use_base_filter):
    """invoice1
    order1
    - 0/0 0/0 300/300
    order2
    - 5/150 0/0 0/0
    order1
    - 310/310 0/0 0/0
    Overacted: 300
    Результат зависит от того фильтруем ли мы всю выборку или только итоговый результат.
    Если мы используем base_filter: 0, 0 (вообще не учавствует в расчетах), 10
    Используем condition_filter: 0, 0 (компенсирует нам 150, но в любом случае не попадет в итог), 160
    """
    do_consume(invoice, order_rur, 300)
    do_complete(order_rur, 300)
    do_act(invoice)
    do_complete(order_rur, 0)

    order_rur.transfer(order_qty)
    co2, = order_qty.consumes
    reverse_consume(co2, None, 5)

    co3 = do_consume(invoice, order_rur, 310)

    if use_base_filter:
        consumes = OveractedFreeFundsQuery(session, [invoice.id]).base_filter(mapper.Consume.order == order_rur).get().all()
    else:
        consumes = OveractedFreeFundsQuery(session, [invoice.id]).get(mapper.Consume.order == order_rur).all()

    if use_base_filter:
        req_consumes = [(co3.id, 10)]
    else:
        req_consumes = [(co3.id, 160)]
    assert extract_qtys(consumes) == req_consumes


def test_condition_filter(session, invoice, order_rur, order_qty):
    """Прореряем, что:
    - переакт будет посчитан по всем конзюмам
    - сначала средства снимутся не с целевых актов
    - а потом будут сниматься в порядке убывания свободных средств
    invoice1
    order1
    - 0/0 0/0 60/60
    - 20/20 0/0 0/0 - целевой 3. спишем с него 15
    - 15/15 0/0 0/0 - 2. спишем с него 15, несмотря на то, что он меньше целевого
    - 15/15 0/0 0/0 - целевой 4. 15 свободно
    - 30/30 0/0 0/0 - 1. спишем с него 30
    Overact: 60
    """
    co0 = do_consume(invoice, order_rur, 60)
    do_complete(order_rur, 60)
    do_act(invoice)
    do_complete(order_rur, 0)
    reverse_consume(co0, None, 60)

    co1 = do_consume(invoice, order_rur, 20)
    do_consume(invoice, order_rur, 15)
    co3 = do_consume(invoice, order_rur, 15)
    do_consume(invoice, order_rur, 30)

    consumes = OveractedFreeFundsQuery(session, [invoice.id]).get(mapper.Consume.id.in_([co1.id, co3.id])).all()

    req_consumes = [(co1.id, 5), (co3.id, 15)]
    assert extract_qtys(consumes) == req_consumes


def test_several_invoices(session, client, order_rur):
    """
    invoice1
    order1
    - 0/0 0/0 7/7
    - 10/10 0/0 0/0
    invoice2
    - 0/0 0/0 10/10
    - 50/50 0/0 0/0
    на каждом заказе переакт и свободные считаются отдельно
    """
    invoice1 = create_invoice(session, client, order_rur)
    invoice2 = create_invoice(session, client, order_rur)

    consumes = []
    for invoice, qtys in [(invoice1, (7, 10)), (invoice2, (20, 50))]:
        overact_qty, free_current_qty = qtys

        co = do_consume(invoice, order_rur, overact_qty)
        do_complete(order_rur, overact_qty)
        do_act(invoice)
        do_complete(order_rur, 0)
        reverse_consume(co, None, overact_qty)

        co = do_consume(invoice, order_rur, free_current_qty)
        consumes.append(co)

    res_consumes = OveractedFreeFundsQuery(session, [invoice1.id, invoice2.id]).get().all()

    co1, co2 = consumes
    hm.assert_that(
        res_consumes,
        hm.contains(
            hm.contains(
                hm.has_properties({'id': co1.id, 'invoice_id': invoice1.id}),
                3,
            ),
            hm.contains(
                hm.has_properties({'id': co2.id, 'invoice_id': invoice2.id}),
                40,
            ),
        ),
    )


@pytest.mark.parametrize('lock', [False, True])
def test_lock(session, invoice, order_rur, lock):
    co = do_consume(invoice, order_rur, 666)
    consumes = OveractedFreeFundsQuery(session, [invoice.id], lock=lock).get().all()

    assert extract_qtys(consumes) == [(co.id, 666)]
