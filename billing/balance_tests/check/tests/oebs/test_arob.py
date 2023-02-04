# coding: utf-8
__author__ = 'chihiro'

from collections import namedtuple
from decimal import Decimal

import datetime

import pytest
from hamcrest import equal_to, contains_string, is_in, empty, has_length

from balance import balance_api as api
from balance import balance_db
from balance import balance_steps as steps
from btestlib import utils as butils
import btestlib.reporter as reporter
from check import db, shared_steps
from check import steps as check_steps
from check.shared import CheckSharedBefore

"""
Контролировать совпадение данных по комиссиям партнеров в Биллинге и OEBS.

ЗАБОР ДАННЫХ
Забираем данные из метабазы
Список вьюшек, попадающих в сверку (all_agency_rewards):
        v_agency_rewards
        v_commission_auto_reward_2013
        v_comm_market_ru_2013
        v_commission_base_reward_2013
        v_commission_prof_reward_2013
        v_commission_kok_reward_2013
        v_commission_sprav_reward_2013
        v_opt_spec_reward
        v_opt_autoru_reward

        select
              from_dt as reward_period_begin_dt,
              trunc(till_dt, 'dd') as reward_period_end_dt,
              contract_eid,
              contract_id,
              reward_type,
              decode(currency, 'RUR', 'RUB', currency) as currency,
              round(nvl(reward_to_charge, 0), 2) as reward_to_charge,
              round(nvl(reward_to_pay, 0), 2) as reward_to_pay,
              round(nvl(turnover_to_charge, 0), 2) as turnover_to_charge,
              round(nvl(turnover_to_pay, 0), 2) as turnover_to_pay,
              round(nvl(turnover_to_pay_w_nds, 0), 2) as turnover_to_pay_w_nds,
              round(nvl(delkredere_to_charge, 0), 2) as delkredere_to_charge,
              round(nvl(dkv_to_charge, 0), 2) as dkv_to_charge,
              round(nvl(delkredere_to_pay, 0), 2) as delkredere_to_pay,
              round(nvl(dkv_to_pay, 0), 2) as dkv_to_pay,
              nvl(nds, -1) as nds,
              nvl(discount_type, 0) as discount_type,
              nvl(insert_dt, sysdate) as update_dt
        from all_agency_rewards
        where from_dt <= :end_dt
              and trunc(till_dt, 'dd') >= :begin_dt
              -- OEBS не забирает записи с till_dt в будущем
              and trunc(till_dt, 'dd') < trunc(sysdate, 'mm')
              -- CHECK-2134 OEBS исключает такие строки при заборе данных
              and (
                abs(nvl(reward_to_charge, 0)) +
                abs(nvl(reward_to_pay, 0)) +
                abs(nvl(delkredere_to_charge, 0)) +
                abs(nvl(delkredere_to_pay, 0)) +
                abs(nvl(dkv_to_charge, 0)) +
                abs(nvl(dkv_to_pay, 0)) +
                abs(nvl(turnover_to_charge, 0)) +
                abs(nvl(turnover_to_pay, 0))
              ) != 0

Забираем данные из OEBS
     select
          rcl.period_start_date as reward_period_begin_dt,
          rcl.period_end_date as reward_period_end_dt,
          rcl.k_alias as contract_eid,
          rcl.k_number as contract_id,
          rcl.reward_type as reward_type,
          rcl.currency_code as currency,
          nvl(rcl.keep_reward_amt, 0) as reward_to_charge,
          nvl(rcl.keep_pay_amt, 0) as reward_to_pay,
          nvl(rcl.turnover_reward_amt, 0) as turnover_to_charge,
          nvl(rcl.turnover_pay_100_amt, 0) as turnover_to_pay,
          nvl(rcl.turnover_pay_118_amt, 0) as turnover_to_pay_w_nds,
          nvl(rcl.dc_reward_amt, 0) as delkredere_to_charge,
          nvl(rcl.dkv_reward_amt, 0) as dkv_to_charge,
          nvl(rcl.dc_pay_amt, 0) as delkredere_to_pay,
          nvl(rcl.dkv_pay_amt, 0) as dkv_to_pay,
          nvl(decode(rcl.vat_type, 10, 1, 20, 0), -1) as nds,
          nvl(decode(rcl.reklam_type,
            70, 7,
            rcl.reklam_type
          ), 0) as discount_type,
          rcl.last_update_date as update_dt
    from apps.xxar_rep_comiss_lines rcl
    where rcl.period_start_date <= :end_dt
          and rcl.period_end_date >= :begin_dt
          and rcl.deleted_flag = 'N'

Типы расхождений:
    * 1 - Отсутствует в OEBS
    * 2 - Отсутствует в Метабазе
    * 3 - Расходится reward_to_charge
    * 4 - Расходится reward_to_pay
    * 5 - Расходится turnover_to_charge
    * 6 - Расходится turnover_to_pay
    * 7 - Расходится turnover_to_pay_w_nds
    * 8 - Расходится delkredere_to_charge
    * 9 - Расходится dkv_to_charge
    * 10 - Расходится delkredere_to_pay
    * 11 - Расходится dkv_to_pay
"""


def insert_row_into_meta(contract_id, contract_eid, begin, end, reward_to_charge='1', reward_to_pay=12711,
                         turnover_to_charge=111, turnover_to_pay=187, turnover_to_pay_w_nds=2216,
                         delkredere_to_charge=0, dkv_to_charge=0, delkredere_to_pay=0, dkv_to_pay=0):
    query = """
            insert into T_COMM_ESTATE_SRC
            (contract_id, contract_eid, from_dt, till_dt, nds, currency, discount_type, reward_type, reward_to_charge,
            reward_to_pay, delkredere_to_charge, delkredere_to_pay, dkv_to_charge, dkv_to_pay, turnover_to_charge,
            turnover_to_pay_w_nds, turnover_to_pay, insert_dt)
            values
            ({contract_id}, '{contract_eid}', to_date('{begin}','DD.MM.YY HH24:MI:SS'),
            to_date('{reward_period_end_dt}','DD.MM.YY HH24:MI:SS'), {nds}, '{currency}', {discount_type},
            {reward_type}, '{reward_to_charge}', '{reward_to_pay}', {delkredere_to_charge}, {delkredere_to_pay},
            {dkv_to_charge}, {dkv_to_pay}, '{turnover_to_charge}', {turnover_to_pay_w_nds}, '{turnover_to_pay}',
            (select max(insert_dt) from t_ar_run where type = 'calc' and finish_dt is not null))
            """.format(contract_id=contract_id, contract_eid=contract_eid, begin=begin, reward_period_end_dt=end,
                       currency='RUR', reward_type=301, reward_to_charge=reward_to_charge, reward_to_pay=reward_to_pay,
                       turnover_to_charge=turnover_to_charge, turnover_to_pay=turnover_to_pay,
                       turnover_to_pay_w_nds=turnover_to_pay_w_nds, delkredere_to_charge=delkredere_to_charge,
                       dkv_to_charge=dkv_to_charge, delkredere_to_pay=delkredere_to_pay, dkv_to_pay=dkv_to_pay, nds=1,
                       discount_type=0, update_dt=begin)
    api.test_balance().ExecuteSQL('meta', query)


def insert_into_oebs(data):
    params = {
        'reward_to_charge': '1',
        'reward_to_pay': 12711,
        'turnover_to_charge': 111,
        'turnover_to_pay': 187,
        'turnover_to_pay_w_nds': 2216,
        'delkredere_to_charge': 0,
        'dkv_to_charge': 0,
        'delkredere_to_pay': 0,
        'dkv_to_pay': 0
    }
    params.update(data)

    query = """
            insert into apps.xxar_rep_comiss_lines
            (rep_line_id, deleted_flag, period_start_date, period_end_date, k_header_id, k_alias, k_number,
            reklam_type, reward_type, vat_type, currency_code, keep_reward_amt, keep_pay_amt, dc_reward_amt,
            dc_pay_amt, dkv_reward_amt, dkv_pay_amt, turnover_reward_amt, turnover_pay_100_amt, turnover_pay_118_amt,
            line_source, creation_date, last_update_date, last_updated_by, org_id, report_type)
            values
            ((select max(rep_line_id) + 1 as val from apps.xxar_rep_comiss_lines), 'N', to_date('{reward_period_begin_dt}','DD.MM.YY HH24:MI:SS'),
            to_date('{reward_period_end_dt}','DD.MM.YY HH24:MI:SS'), 2874666, '{contract_eid}', {contract_id},
            {discount_type}, {reward_type}, {nds}, '{currency}', {reward_to_charge}, {reward_to_pay},
            {delkredere_to_charge}, {delkredere_to_pay}, {dkv_to_charge}, {dkv_to_pay}, {turnover_to_charge},
            {turnover_to_pay}, {turnover_to_pay_w_nds}, 'BASE', to_date('{begin}','DD.MM.YY HH24:MI:SS'),
            to_date('{update_dt}','DD.MM.YY HH24:MI:SS'), 2470, 121, 'KOMISS')
            """.format(contract_id=params['contract_id'],
                       contract_eid=params['contract_eid'], reward_period_begin_dt=params['begin_dt'],
                       reward_period_end_dt=params['end_dt'], currency='RUB', reward_type=301,
                       reward_to_charge=params['reward_to_charge'], reward_to_pay=params['reward_to_pay'],
                       turnover_to_charge=params['turnover_to_charge'], turnover_to_pay=params['turnover_to_pay'],
                       turnover_to_pay_w_nds=params['turnover_to_pay_w_nds'],
                       delkredere_to_charge=params['delkredere_to_charge'],
                       dkv_to_charge=params['dkv_to_charge'], delkredere_to_pay=params['delkredere_to_pay'],
                       dkv_to_pay=params['dkv_to_pay'], nds=10, discount_type=0, update_dt=params['begin_dt'],
                       begin=params['begin_dt'])
    balance_db.oebs().execute(query)

    new_id = api.test_balance().ExecuteSQL('oebs_qa',
                                           'select max(rep_line_id) as val from apps.xxar_rep_comiss_lines')[0]['val']

    return new_id


def create_contract(client_id, person_id, contract_type, services=[37],
                    start_dt=datetime.datetime.now().replace(day=1),
                    end_dt=datetime.datetime.now().replace(day=1) + datetime.timedelta(weeks=5)):
    start_dt = start_dt.strftime('%Y-%m-%dT00:00:00')
    end_dt = end_dt.strftime('%Y-%m-%dT00:00:00')
    if contract_type in ['universal_distr']:
        _, _, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
    else:
        tag_id = None
    contract_id, contract_eid = steps.ContractSteps.create_contract(contract_type,
                                                                    {'CLIENT_ID': client_id,
                                                                     'PERSON_ID': person_id,
                                                                     'DT': '{0}'.format(start_dt),
                                                                     'FINISH_DT': '{0}'.format(end_dt),
                                                                     'IS_SIGNED': '{0}'.format(start_dt),
                                                                     'SERVICES': services,

                                                                     'DISCOUNT_POLICY_TYPE': 3,
                                                                     'DISTRIBUTION_TAG': tag_id
                                                                     })
    collateral_0_id = db.get_collateral_id_by_contract_id(contract_id)

    steps.ExportSteps.export_oebs(person_id=person_id, contract_id=contract_id, collateral_id=collateral_0_id)

    return contract_id, contract_eid


def new_date():
    begin_dt = (datetime.datetime.now().replace(day=1) - datetime.timedelta(weeks=5)).strftime('%d.%m.%y %H:%M:%S')
    end_dt = (datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)).strftime('%d.%m.%y 00:00:00')
    return begin_dt, end_dt


def delete_from_rep_comiss_lines(line_id):
    balance_db.oebs().execute('delete from apps.xxar_rep_comiss_lines where rep_line_id={id}'.format(id=line_id))


@pytest.fixture(scope="module")
def fixtures():
    person_category = 'ur'
    contract_type = 'no_agency_test'
    client_id = check_steps.create_client()
    steps.ExportSteps.export_oebs(client_id=client_id)
    person_id = check_steps.create_person(client_id, person_category=person_category)
    begin_dt, end_dt = new_date()

    data = namedtuple('data', 'client_id person_id contract_type begin_dt end_dt')
    data_list = data(client_id=client_id,
                     person_id=person_id,
                     contract_type=contract_type,
                     begin_dt=begin_dt,
                     end_dt=end_dt)

    return data_list


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_no_diffs(shared_data):
    """
    Начальные условия:
        -Договор присутствует и в мете, и в оебс
        -Все сверяемые поля сходятся
    Ожидаемый результат:
        договор отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()

        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)

        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    # Проверяем, что нет расхождений
    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_not_found_in_meta(shared_data):
    """
    Начальные условия:
        -договор присутствует в оебс
        -договор отсутствует в метабазе
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Отсутствует в Метабазе"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)


        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
        }

        lines_id = insert_into_oebs(data_for_insert)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 2), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_not_found_in_oebs(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе
        -договор отсутствует в оебс
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Отсутствует в OEBS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures()

        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 1), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_reward_to_charge_not_converge(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, reward_to_charge = 5 ед.
        -договор присутствует в оебс, reward_to_charge = 1 ед.
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится reward_to_charge"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)


        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
            'reward_to_charge': '5',
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 3), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_reward_to_pay_not_converge(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, reward_to_pay = 12711 ед.
        -договор присутствует в оебс, reward_to_pay = 18 ед.
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится reward_to_pay"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)


        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
            'reward_to_pay': 18,
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 4), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_turnover_to_charge_not_converge(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, turnover_to_charge = 111 ед.
        -договор присутствует в оебс, turnover_to_charge = 1 ед.
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится turnover_to_charge"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)


        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
            'turnover_to_charge': 1,
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 5), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_turnover_to_pay_not_converge(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, turnover_to_pay = 187 ед.
        -договор присутствует в оебс, turnover_to_pay = 15 ед.
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится turnover_to_pay"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)

        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
            'turnover_to_pay': 15,
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 6), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_turnover_to_pay_w_nds_not_converge(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, turnover_to_pay_w_nds = 2216 ед.
        -договор присутствует в оебс, turnover_to_pay_w_nds = 113 ед.
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится turnover_to_pay_w_nds"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)

        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
            'turnover_to_pay_w_nds': 113,
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 7), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_delkredere_to_charge_not_converge(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, delkredere_to_charge = 0 ед.
        -договор присутствует в оебс, delkredere_to_charge = 1 ед.
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится delkredere_to_charge"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)

        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
            'delkredere_to_charge': 1,
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 8), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_dkv_to_charge_not_converge(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, dkv_to_charge = 0 ед.
        -договор присутствует в оебс, dkv_to_charge = 13 ед.
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится dkv_to_charge"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)

        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
            'dkv_to_charge': 13,
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 9), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_dlkredere_to_pay_not_converge(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, delkredere_to_pay = 0 ед.
        -договор присутствует в оебс, delkredere_to_pay = 8 ед.
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится delkredere_to_pay"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)

        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
            'delkredere_to_pay': 8,
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 10), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_dkv_to_pay_not_converge(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, dkv_to_pay = 0 ед.
        -договор присутствует в оебс, dkv_to_pay = 8 ед.
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится dkv_to_pay"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)

        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
            'dkv_to_pay': 8
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt)

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 11), is_in(result))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_check_2183(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, dkv_to_pay = 8.12 ед.
        -договор присутствует в оебс, dkv_to_pay = 8.123 ед.
    Ожидаемый результат:
        договор отсутствует в списке расхождений
    """

    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'lines_id']
    ) as before:
        before.validate()

        f = fixtures()
        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)

        data_for_insert = {
            'contract_id': contract_id,
            'contract_eid': contract_eid,
            'begin_dt': f.begin_dt,
            'end_dt': f.end_dt,
            'dkv_to_pay': Decimal('8.12')
        }

        lines_id = insert_into_oebs(data_for_insert)
        insert_row_into_meta(contract_id, contract_eid, f.begin_dt, f.end_dt, dkv_to_pay=Decimal('8.123'))

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    delete_from_rep_comiss_lines(lines_id)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_check_2183_date(shared_data):
    """
    Начальные условия:
        -договор присутствует в метабазе, till_dt > sysdate
        -договор отсутствует в оебс
    Ожидаемый результат:
        договор отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures()
        begin_dt = (datetime.datetime.now().replace(day=1) - datetime.timedelta(weeks=5)).strftime('%d.%m.%y %H:%M:%S')
        end_dt = (datetime.datetime.now() - datetime.timedelta(days=1)).strftime('%d.%m.%y 00:00:00')

        contract_id, contract_eid = create_contract(f.client_id, f.person_id, f.contract_type)
        insert_row_into_meta(contract_id, contract_eid, begin_dt, end_dt, dkv_to_pay=Decimal('8.123'))

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_AROB)
def test_check_diffs_count(shared_data):
    diffs_count = 11
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_arob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    butils.check_that(cmp_data, has_length(diffs_count))