# -*- coding: utf-8 -*-

from decimal import Decimal as D

import datetime

from balance import mapper
from balance import reverse_partners as rp
from balance.constants import *
from butils import logger
from tests.balance_tests.rev_partners.common import (
    _create_scale,
    check_qty,
    check_sum,
    gen_contract,
    generate_acts,
    multiship_terms,
)

log = logger.get_logger("test_revpartners.test_multiship")


def test_multiship(session):
    def multiship_comp(contract, on_dt):
        return [(504969, D(100)), (504970, D(0)), (504972, D(300)), (504971, D(400))]

    rp.compl_map[ServiceId.MULTISHIP_DELIVERY] = multiship_comp

    a_m = mapper.ActMonth(for_month=datetime.datetime(2015, 5, 1))

    contract = gen_contract(session, postpay=True, con_func=multiship_terms)
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.MULTISHIP_DELIVERY], a_m)
    res = rpc.process_and_enqueue_act()

    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 1
    assert len(acts[0].rows) == 3
    check_qty(acts[0], multiship_comp(None, None))


def test_multiship_2months(session):
    def multiship_comp(contract, on_dt):
        log.debug("on_dt = %s" % on_dt)
        if on_dt < datetime.datetime(2015, 7, 1):
            return [(504969, D(100)), (504970, D(200)), (504972, D(300))]
        if on_dt < datetime.datetime(2015, 8, 1):
            return [(504969, D(100)), (504970, D(200)), (504971, D(400))]

    rp.compl_map[ServiceId.MULTISHIP_DELIVERY] = multiship_comp

    a_m = mapper.ActMonth(for_month=datetime.datetime(2015, 5, 1))

    contract = gen_contract(session, postpay=True, con_func=multiship_terms)
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.MULTISHIP_DELIVERY], a_m)
    res = rpc.process_and_enqueue_act()

    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 1
    assert len(acts[0].rows) == 3
    check_qty(acts[0], multiship_comp(None, a_m.end_dt))

    rp.compl_map[ServiceId.MULTISHIP_DELIVERY] = multiship_comp

    a_m = mapper.ActMonth(for_month=datetime.datetime(2015, 6, 1))

    contract = gen_contract(session, postpay=True, con_func=multiship_terms)
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.MULTISHIP_DELIVERY], a_m)
    res = rpc.process_and_enqueue_act()

    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 1
    assert len(acts[0].rows) == 3
    check_qty(acts[0], multiship_comp(None, a_m.end_dt))


def test_multiship_pay(session):
    def transactions(contract, on_dt):
        log.debug("running mock multiship_completions")
        return [(None, D(600))]

    rp.compl_map[ServiceId.MULTISHIP_PAYMENT] = transactions
    a_m = mapper.ActMonth(for_month=datetime.datetime(2015, 5, 1))

    contract = gen_contract(session, postpay=True, con_func=multiship_terms)
    rpc = rp.ReversePartnerCalc(contract, [ServiceId.MULTISHIP_PAYMENT], a_m)
    res = rpc.process_and_enqueue_act()

    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert acts[0].rows[0].act_qty == D("600")


def test_multiship_discount(session):
    service_id = ServiceId.MULTISHIP_DELIVERY
    act_dt = datetime.datetime(2015, 5, 1)

    def multiship_comp(contract, on_dt):
        return [(504970, D(2000)), (504971, D(1000))]

    rp.compl_map[service_id] = multiship_comp
    a_m = mapper.ActMonth(for_month=act_dt)
    contract = gen_contract(session, postpay=True, con_func=multiship_terms)

    DISCOUNT = D("40")
    points = [(100, DISCOUNT)]
    scale = _create_scale(session, "multiship_shipment", points, namespace="discount")

    for n in range(499):
        add_multiship_transaction(n, contract, 504970, act_dt)

    rpc = rp.ReversePartnerCalc(contract, [service_id], a_m)
    res = rpc.process_and_enqueue_act()

    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 1
    assert len(acts[0].rows) == 2
    check_qty(acts[0], multiship_comp(None, None))
    check_sum(acts[0], multiship_comp(None, None), DISCOUNT)


def add_multiship_transaction(id, contract, product_id, dt):
    session = contract.session
    trans = mapper.PartnerMultishipStat(
        contract_id=contract.id,
        product_id=product_id,
        id=id,
        entity_id=1,
        dt=dt,
        amount=D(1),
        is_correction=0,
    )
    session.add(trans)
