# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

from cluster_tools import generate_partner_acts as gpa
from balance.mapper import ActMonth

from tests.base import BalanceTest
from tests.balance_tests.entity_reward.distribution_utils import (
    add_place, create_contract as create_distr_contract, create_primitive_completion as create_distr_compl
)
from tests.balance_tests.entity_reward.spendable_utils import (
    create_contract as create_spendable_contract, create_primitive_completion as create_spendable_compl
)
from tests.balance_tests.entity_reward.entity_utils import run_calculator


CUR_MONTH_DT = datetime.datetime.now().replace(day=1, hour=0, minute=0, second=0, microsecond=0)


def generate_act(contract, month_dt):

    am = ActMonth(for_month=month_dt)
    generator = gpa.get_generator(contract, act_month=am)
    generator.generate(am)
    contract.session.flush()


def _get_contract_money(session, contract_id,
                        dt_start=datetime.datetime(1900, 1, 1),
                        dt_end=datetime.datetime(2999, 1, 1)):

    res = session.execute(
        '''
        select dt from bo.t_entity_money
        where contract_id = :cid and dt between :dt1 and :dt2
        order by dt
        ''', dict(cid=contract_id, dt1=dt_start, dt2=dt_end)
    ).fetchall()

    return res


def _get_contract_acts(session, contract_id, month_dt=None):

    dt_filter = 'and dt = :dt' if month_dt else ''

    res = session.execute(
        '''
        select partner_reward_wo_nds from bo.t_partner_act_data
        where partner_contract_id = :cid
        {dt_filter}
        '''.format(dt_filter=dt_filter),
        dict(cid=contract_id, dt=month_dt)
    ).fetchall()

    return res


# ======= TestRewardCalculation ==========

def test_contract_end_dt(session):

    start_dt = CUR_MONTH_DT - relativedelta(months=2)
    act_dt = CUR_MONTH_DT - relativedelta(months=1)
    end_dt = act_dt.replace(day=22)
    after_end_dt = act_dt.replace(day=23)

    contract_params = dict(start_dt=start_dt, end_dt=end_dt, products_revshare={10000: 40}, tail_time=0)
    contract = create_distr_contract(session, contract_params)
    place_id = add_place(contract, [10000])

    create_distr_compl(session, end_dt, 10000, place_id)
    create_distr_compl(session, after_end_dt, 10000, place_id)
    run_calculator(contract, act_dt)

    has_reward_end_dt = len(_get_contract_money(session, contract.id, dt_start=end_dt, dt_end=end_dt))

    has_reward_after_end_dt = len(_get_contract_money(session, contract.id, dt_start=after_end_dt, dt_end=after_end_dt))

    assert has_reward_end_dt == 1
    assert has_reward_after_end_dt == 0


def test_tail_end_dt(session):

    start_dt = CUR_MONTH_DT - relativedelta(months=6)
    act_dt = CUR_MONTH_DT - relativedelta(months=1)
    end_dt = start_dt.replace(day=22)
    tail_months = 5
    tail_end_dt = end_dt + relativedelta(months=tail_months)
    after_end_dt = tail_end_dt + relativedelta(days=1)

    contract_params = dict(start_dt=start_dt, end_dt=end_dt, products_revshare={10000: 40}, tail_time=tail_months)
    contract = create_distr_contract(session, contract_params)
    place_id = add_place(contract, [10000])

    create_distr_compl(session, tail_end_dt, 10000, place_id)
    create_distr_compl(session, after_end_dt, 10000, place_id)
    run_calculator(contract, act_dt)

    has_reward_end_dt = len(_get_contract_money(session, contract.id, dt_start=tail_end_dt, dt_end=tail_end_dt))

    has_reward_after_end_dt = len(_get_contract_money(session, contract.id, dt_start=after_end_dt, dt_end=after_end_dt))

    assert has_reward_end_dt == 1
    assert has_reward_after_end_dt == 0


def _check_contract_start_and_end_in_same_month(session, contract, create_compl_func, act_dt, expected_sum):
    u""" Создаём 5 откруток, но в акт должны попасть только три, которые между start_dt и end_dt"""
    create_compl_func(act_dt.replace(day=4))
    create_compl_func(act_dt.replace(day=5))
    create_compl_func(act_dt.replace(day=7))
    create_compl_func(act_dt.replace(day=10))
    create_compl_func(act_dt.replace(day=11))
    run_calculator(contract, act_dt)
    month_begin_dt = act_dt
    month_end_dt = (act_dt + relativedelta(months=1)) - relativedelta(days=1)

    money_days = [item['dt'] for item in
                  _get_contract_money(session, contract.id, month_begin_dt, month_end_dt)]

    assert money_days == [act_dt.replace(day=d) for d in (5, 7, 10)]

    generate_act(contract, act_dt)

    act_sum = sum(item['partner_reward_wo_nds'] for item in
                  _get_contract_acts(session, contract.id, act_dt))

    assert expected_sum == act_sum


def test_contract_start_and_end_in_same_month__distribution(session):
    last_month_dt = CUR_MONTH_DT - relativedelta(months=1)
    start_dt = last_month_dt.replace(day=5)
    end_dt = last_month_dt.replace(day=10)
    contract_params = dict(start_dt=start_dt, end_dt=end_dt, products_revshare={10000: 40}, tail_time=0)
    contract = create_distr_contract(session, contract_params)
    product_id = 10000
    place_id = add_place(contract, [product_id])
    create_compl_func = lambda dt: create_distr_compl(session, dt, product_id, place_id)

    # num_completions * bucks / 1000000 * 30 * partner_pct / rus_nds_koef
    expected_sum = 3 * Decimal(5000) / 1000000 * 30 * Decimal('0.4') / Decimal('1.2')

    _check_contract_start_and_end_in_same_month(
        session, contract, create_compl_func, act_dt=last_month_dt, expected_sum=expected_sum,
    )


def test_contract_start_and_end_in_same_month__spendable(session):
    last_month_dt = CUR_MONTH_DT - relativedelta(months=1)
    start_dt = last_month_dt.replace(day=5)
    end_dt = last_month_dt.replace(day=10)
    contract_params = dict(start_dt=start_dt, end_dt=end_dt)
    contract = create_spendable_contract(session, contract_params)
    product_id = 20701
    create_compl_func = lambda dt: create_spendable_compl(session, dt, product_id, contract.client.id)

    expected_sum = 3 * 5000  # num_completions * money

    _check_contract_start_and_end_in_same_month(
        session, contract, create_compl_func, act_dt=last_month_dt, expected_sum=expected_sum,
    )


def test_bound_dates__spendable(session):
    u""" Проверка на то, что первый и последний день месяца нормально актятся"""
    last_month_dt = CUR_MONTH_DT - relativedelta(months=1)
    contract_start_dt = last_month_dt - relativedelta(months=1)
    act_dt = last_month_dt
    last_month_end_dt = CUR_MONTH_DT - relativedelta(days=1)

    contract_params = dict(start_dt=contract_start_dt)
    contract = create_spendable_contract(session, contract_params)

    product_id = 20701

    create_spendable_compl(session, last_month_dt, product_id, contract.client.id)
    create_spendable_compl(session, last_month_end_dt, product_id, contract.client.id)

    run_calculator(contract, act_dt)

    money_days = [item['dt'] for item in
                  _get_contract_money(session, contract.id, dt_start=last_month_dt, dt_end=last_month_end_dt)
                  ]

    assert [last_month_dt, last_month_end_dt] == money_days

    run_calculator(contract, act_dt)

    generate_act(contract, act_dt)

    act_sum = sum(item['partner_reward_wo_nds'] for item in
                  _get_contract_acts(session, contract.id, act_dt))

    assert 2*5000 == act_sum


# ======= TestSpecialCases ==========

def _check_empty_case(session, contract, case_descr):
    u""" Проверка случаев, когда никаких данных не должно сгенериться, просто всё должно отработать вхолостую """
    money = _get_contract_money(session, contract)
    assert money == [], case_descr + ' must have not money records'
    acts = _get_contract_acts(session, contract)
    assert acts == [], case_descr + ' must have not act records'

def test_empty_cases(session):

    last_month_dt = CUR_MONTH_DT - relativedelta(months=1)

    def add_distr_data(contract):
        product_id = 10000
        place_id = add_place(contract, [product_id])
        create_distr_compl(session, last_month_dt, product_id, place_id)

    def add_spendable_data(contract):
        product_id = 20701
        create_spendable_compl(session, last_month_dt, product_id, contract.client.id)

    ctype_cases = (
        dict(creating_contract_func=create_distr_contract,
             add_stat_func=add_distr_data,
             params={'products_revshare': {10000: 40}}),
        dict(creating_contract_func=create_spendable_contract,
             add_stat_func=add_spendable_data,
             params={}),
    )

    for case in ctype_cases:

        create_contract = case['creating_contract_func']
        add_stat = case['add_stat_func']
        params = case['params']
        params.update(start_dt=last_month_dt)

        # Valid contract, but none completions

        c1 = create_contract(session, params)
        run_calculator(c1, last_month_dt)
        generate_act(c1, last_month_dt)

        _check_empty_case(session, c1.id, '{} contract without completion'.format(c1.ctype.type))

        # Cancelled contract

        c2 = create_contract(session, params)
        add_stat(c2)
        c2.col0.is_cancelled = last_month_dt
        session.add(c2)
        session.flush()
        run_calculator(c2, last_month_dt)
        generate_act(c2, last_month_dt)

        _check_empty_case(session, c2.id, 'Cancelled {} contract'.format(c2.ctype.type))

        # Not signed contract

        not_signed_params = params.copy()
        not_signed_params['signed'] = 0
        c3 = create_contract(session, not_signed_params)
        add_stat(c3)
        run_calculator(c3, last_month_dt)
        generate_act(c3, last_month_dt)

        _check_empty_case(session, c3.id, 'Not signed {} contract'.format(c3.ctype.type))
