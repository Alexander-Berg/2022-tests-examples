# -*- coding: utf-8 -*-
from decimal import Decimal as D

import datetime
import pytest

from balance import constants as const
from balance import mapper
from balance import muzzle_util as ut
from balance import reverse_partners as rp
from tests.balance_tests.rev_partners.common import dsp_terms, gen_acts, gen_contract


def set_mock_func4actual_completions(func):
    rp.get_dsp_actotron_completions = func


@pytest.fixture(autouse=True)
def apply_mock_nds():
    rp.get_dsp_nds_pct = (
        lambda c, dt: D(18)
        if mapper.ActMonth(dt).document_dt < datetime.datetime(2019, 1, 1)
        else D(20)
    )


def gen_dsp_contract(session, service_min_cost=0, test_period_duration=0):
    return gen_contract(
        session,
        postpay=True,
        personal_account=False,
        con_func=lambda c: dsp_terms(c, service_min_cost, test_period_duration),
    )


def _get_act_row4product(act, product):
    res = [r for r in act.rows if r.order.product == product]
    assert len(res) == 1
    return res[0]


def test_no_completions(session):
    """
    Выставление счёта и акта при отсутствии услуг за месяц
    Повторное невыставление счёта при отсутствии услуг за месяц
    """
    service_min_cost_wo_nds = D("100")
    service_id = const.ServiceId.DSP
    test_period_duration = 2
    nds_pct = D(18)
    contract = gen_dsp_contract(session, service_min_cost_wo_nds, test_period_duration)

    set_mock_func4actual_completions(lambda _, on_dt: [(None, D(0))])

    a_m = mapper.ActMonth(
        for_month=ut.add_months_to_date(contract.col0.dt, test_period_duration + 1)
    )
    rpc = rp.ReversePartnerCalc(contract, [service_id], a_m)
    acts = gen_acts(rpc)
    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert acts[0].rows[0].act_qty == service_min_cost_wo_nds * (D(1) + nds_pct/D(100))

    # Retry CalcDsp
    rpc = rp.ReversePartnerCalc(contract, [service_id], a_m)
    res = rpc.process_and_enqueue_act()
    assert res is None


def test_completions_with_apx(session):
    """
    Выставление счёта на услуги и добивку
    """
    service_min_cost_wo_nds = D("100")
    service_id = const.ServiceId.DSP
    test_period_duration = 2
    main_service_qty = service_min_cost_wo_nds - D("50.05")
    nds_pct = D(18)
    contract = gen_dsp_contract(session, service_min_cost_wo_nds, test_period_duration)

    main_product = rp.get_product(
        service_id, contract, order_type=const.MAIN_ORDER_TYPE
    )
    apx_product = rp.get_product(service_id, contract, order_type=const.APX_ORDER_TYPE)
    set_mock_func4actual_completions(lambda _, on_dt: [(main_product.id, main_service_qty)])

    a_m = mapper.ActMonth(
        for_month=ut.add_months_to_date(contract.col0.dt, test_period_duration + 1)
    )
    rpc = rp.ReversePartnerCalc(contract, [service_id], a_m)
    acts = gen_acts(rpc)

    assert len(acts) == 1
    assert len(acts[0].rows) == 2
    assert _get_act_row4product(acts[0], main_product).act_qty == main_service_qty
    assert (
        _get_act_row4product(acts[0], apx_product).act_qty
        == service_min_cost_wo_nds * (D(1) + nds_pct/D(100)) - main_service_qty
    )


def test_no_apx_in_test_period(session):
    """
    Отсутствие добивки в тестовый период
    """
    service_min_cost_wo_nds = D("100")
    service_id = const.ServiceId.DSP
    test_period_duration = 2
    main_service_qty = service_min_cost_wo_nds - D("50.05")
    # nds_pct = D(18)
    contract = gen_dsp_contract(session, service_min_cost_wo_nds, test_period_duration)

    main_product = rp.get_product(
        service_id, contract, order_type=const.MAIN_ORDER_TYPE
    )
    set_mock_func4actual_completions(lambda _, on_dt: [(main_product.id, main_service_qty)])

    a_m = mapper.ActMonth(
        for_month=ut.add_months_to_date(contract.col0.dt, test_period_duration - 1)
    )
    rpc = rp.ReversePartnerCalc(contract, [service_id], a_m)
    acts = gen_acts(rpc)

    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert acts[0].rows[0].act_qty == main_service_qty
