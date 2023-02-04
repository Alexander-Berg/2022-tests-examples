# -*- coding: utf-8 -*-

from decimal import Decimal as D

import pytest

from balance import constants as const
from balance import mapper
from balance import muzzle_util as ut
from balance import reverse_partners as rp
from tests.balance_tests.rev_partners.common import (
    corp_taxi_terms,
    gen_acts,
    gen_contract,
)


def gen_corp_taxi_contract(
    session, postpay=True, min_cost=0, no_min_cost_wo_service=True
):
    return gen_contract(
        session,
        postpay=postpay,
        personal_account=True,
        con_func=lambda c: corp_taxi_terms(
            c, min_cost, no_min_cost_wo_service=no_min_cost_wo_service
        ),
    )


def _get_act_row4product(act, product):
    res = [r for r in act.rows if r.order.product == product]
    assert len(res) == 1
    return res[0]


def test_get_balance(session):
    from balance.partner_balance import partner_balance_factory_unit_processing

    factory = partner_balance_factory_unit_processing

    service_id = const.ServiceId.TAXI_CORP_CLIENTS
    main_service_qty = D("420.69")
    contract = gen_corp_taxi_contract(session, postpay=False)

    main_product = rp.get_product(service_id, contract, ua_root=True)
    main_order = rp.get_order(service_id, contract, product=main_product)
    rp.get_partner_taxi_stat_aggr_completions = lambda *args, **kwargs: [
        (main_product, main_service_qty, main_order, 0)
    ]

    a_m = mapper.ActMonth(for_month=contract.col0.dt)
    rpc = rp.ReversePartnerCalc(contract, [service_id], a_m)
    gen_acts(rpc)

    partner_balance = factory(session, const.ServiceId.TAXI_CORP_CLIENTS)
    balance_info = partner_balance.get_info(contract.id)
    print(balance_info)


def test_2months(session):
    postpay = False
    nds_pct = D("20")

    service_id = const.ServiceId.TAXI_CORP_CLIENTS
    main_service_qty = D(25)
    contract = gen_corp_taxi_contract(session, postpay)

    main_product = rp.get_product(service_id, contract, ua_root=True)
    main_order = rp.get_order(service_id, contract, product=main_product)
    rp.get_partner_taxi_stat_aggr_completions = lambda *args, **kwargs: [
        (main_product, main_service_qty / (1 + nds_pct / D("100")), main_order, 0)
    ]

    a_m = mapper.ActMonth(for_month=contract.col0.dt)
    rpc = rp.ReversePartnerCalc(contract, [service_id], a_m)
    acts = gen_acts(rpc)
    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert _get_act_row4product(acts[0], main_product).act_qty == main_service_qty
    assert acts[0].invoice.consume_sum == main_service_qty
    assert "MONTH_PROC" not in contract.client.exports

    rp.get_partner_taxi_stat_aggr_completions = lambda *args, **kwargs: [
        (main_product, 2 * main_service_qty / (1 + nds_pct / D("100")), main_order, 0)
    ]

    a_m = mapper.ActMonth(for_month=ut.add_months_to_date(contract.col0.dt, 1))
    rpc = rp.ReversePartnerCalc(contract, [service_id], a_m)
    acts = gen_acts(rpc)

    assert len(acts[0].rows) == 1
    assert _get_act_row4product(acts[0], main_product).act_qty == main_service_qty

    assert acts[0].invoice.consume_sum == 2 * main_service_qty
