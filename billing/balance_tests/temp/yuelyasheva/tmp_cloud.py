# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import datetime
from decimal import Decimal as D

import pytest

from balance import balance_steps as steps
from balance import balance_db
from btestlib import utils
from btestlib.constants import NdsNew
from btestlib.data.partner_contexts import CLOUD_MARKETPLACE_CONTEXT
from dateutil.relativedelta import relativedelta
from btestlib.matchers import contains_dicts_with_entries

_, _, month_before_prev_start_dt, month_before_prev_end_dt, prev_month_start_dt, prev_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()
current_month_start_dt, current_month_end_dt = utils.Date.current_month_first_and_last_days()
prev_quarter_start_dt, prev_quarter_end_dt = utils.Date.get_previous_quarter(datetime.datetime.today())
COMPLETION_SUM = D('5004.1')
COEF_1 = D('0.5')
COEF_2 = D('1.3')
context = CLOUD_MARKETPLACE_CONTEXT

def create_entity_completions(client_id, completion_sum, dt):
    entity_id = check_client_tarification(client_id)
    if not entity_id:
        entity_id = create_tarification_entity_row(client_id)
    else:
        entity_id = entity_id[0]['id']

    create_entity_completions_row(entity_id, completion_sum, dt)

def check_client_tarification(client_id):
    entity_id = balance_db.balance().execute('select id from T_TARIFICATION_ENTITY where key_num_1 = :client_id',
                                             {'client_id': client_id})
    return entity_id


def create_tarification_entity_row(client_id):
    entity_id = balance_db.balance().execute('SELECT S_TARIFICATION_ENTITY.nextval id FROM dual')[0]['id']
    sql_insert_completion = "INSERT INTO T_TARIFICATION_ENTITY (ID, PRODUCT_ID, KEY_NUM_1, KEY_NUM_2, KEY_NUM_3, " \
                            "KEY_NUM_4, KEY_NUM_5, KEY_NUM_6) " \
                            "VALUES (:entity_id, 11101, :client_id, -1, -1, -1, -1, -1)"
    params = {'entity_id': entity_id,
              'client_id': client_id}
    balance_db.balance().execute(sql_insert_completion, params)
    return entity_id


def create_entity_completions_row(entity_id, completion_sum, dt):
    #TODO добавить проверку, что в таблице нет записи для клиента на выбранную дату, если есть - менять значение
    # (чтоб было по бизнес-логике)
    query = "INSERT INTO T_ENTITY_COMPLETION (DT, PRODUCT_ID, ENTITY_ID, SRC_ID, VAL_NUM_1) " \
                            "VALUES (:dt, 11101, :entity_id, 30, :completion_sum)"
    params = {'dt': dt,
              'entity_id': entity_id,
              'completion_sum': completion_sum}
    balance_db.balance().execute(query, params)

def update_collateral_dt(contract_id, dt):
    query = "UPDATE t_contract_collateral SET dt = :dt WHERE contract2_id = :contract_id AND collateral_type_id = 7010"
    params = {'contract_id': contract_id, 'dt': dt}
    balance_db.balance().execute(query, params)



def test_cloud_marketplace_no_data():
    # создаем данные
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': prev_month_start_dt})

    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_month_end_dt)

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_acts = []
    utils.check_that(acts, contains_dicts_with_entries(expected_acts),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('nds',
                         [
                             (context.nds),
                             (NdsNew.ZERO)
                         ],
                         ids=['with nds Russia'
                             , 'w/o nds Russia'
                              ]
                         )
def test_cloud_marketplace_two_months(nds):
    # создаем данные
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'nds': nds.nds_id,
                           'start_dt': month_before_prev_start_dt})

    create_entity_completions(client_id, COMPLETION_SUM, month_before_prev_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, month_before_prev_end_dt)
    amount_prev_month = (COMPLETION_SUM / NdsNew.DEFAULT.koef_on_dt(month_before_prev_end_dt)).quantize(D('1.00000'))

    act_data_prev_month = steps.CommonData.create_expected_pad(context, client_id, spendable_contract_id,
                                                               month_before_prev_end_dt, partner_reward=amount_prev_month,
                                                               nds=nds)

    create_entity_completions(client_id, COMPLETION_SUM * COEF_1, month_before_prev_start_dt + relativedelta(days=1))
    create_entity_completions(client_id, COMPLETION_SUM * COEF_2, prev_month_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_month_end_dt)

    amount_cur_month = (COMPLETION_SUM * COEF_1 / NdsNew.DEFAULT.koef_on_dt(month_before_prev_end_dt) + \
                        COMPLETION_SUM * COEF_2 / NdsNew.DEFAULT.koef_on_dt(prev_month_end_dt)).quantize(D('1.00000'))

    act_data_cur_month = steps.CommonData.create_expected_pad(context, client_id, spendable_contract_id,
                                                              prev_month_end_dt, partner_reward=amount_cur_month,
                                                              nds=nds)

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_acts = [act_data_prev_month, act_data_cur_month]
    utils.check_that(acts, contains_dicts_with_entries(expected_acts),
                     'Сравниваем данные из акта с шаблоном')


def test_cloud_marketplace_change_nds():
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month_before_prev_start_dt})

    create_entity_completions(client_id, COMPLETION_SUM, month_before_prev_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, month_before_prev_end_dt)
    amount_prev_month = (COMPLETION_SUM / NdsNew.DEFAULT.koef_on_dt(month_before_prev_start_dt)).quantize(D('1.00000'))

    act_data_prev_month = steps.CommonData.create_expected_pad(context, client_id, spendable_contract_id,
                                                               month_before_prev_end_dt, partner_reward=amount_prev_month,
                                                               nds=context.nds)

    steps.ContractSteps.create_collateral(7010,
                                          {'CONTRACT2_ID': spendable_contract_id,
                                           'DT': utils.Date.nullify_time_of_date(current_month_start_dt).isoformat(),
                                           'IS_SIGNED': utils.Date.nullify_time_of_date(current_month_start_dt).isoformat(),
                                           'NDS': 0})

    update_collateral_dt(spendable_contract_id, prev_month_start_dt)

    create_entity_completions(client_id, COMPLETION_SUM * COEF_1, month_before_prev_start_dt)
    create_entity_completions(client_id, COMPLETION_SUM * COEF_2, prev_month_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_month_end_dt)

    amount_cur_month = (COMPLETION_SUM * COEF_1 / NdsNew.DEFAULT.koef_on_dt(month_before_prev_end_dt) +
                        COMPLETION_SUM * COEF_2 / NdsNew.DEFAULT.koef_on_dt(prev_month_end_dt)).quantize(D('1.00000'))

    act_data_cur_month = steps.CommonData.create_expected_pad(context, client_id, spendable_contract_id,
                                                              prev_month_end_dt, partner_reward=amount_cur_month,
                                                              nds=NdsNew.ZERO)

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_acts = [act_data_prev_month, act_data_cur_month]
    utils.check_that(acts, contains_dicts_with_entries(expected_acts),
                     'Сравниваем данные из акта с шаблоном')

    # PAYMENT_TYPE
@pytest.mark.parametrize('nds',
                         [
                             (CLOUD_MARKETPLACE_CONTEXT.nds),
                             (NdsNew.ZERO)
                         ],
                         ids=['with nds Russia',
                              'w/o nds Russia'
                              ]
                         )
def test_cloud_marketplace_payment_type_2(nds):
    # создаем данные
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'nds': nds.nds_id,
                           'start_dt': prev_quarter_start_dt,
                           'payment_type': 2})
    create_entity_completions(client_id, COMPLETION_SUM, prev_quarter_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, utils.Date.last_day_of_month(prev_quarter_start_dt))
    create_entity_completions(client_id, COMPLETION_SUM / D('2'), utils.Date.first_day_of_month(prev_quarter_end_dt))
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, prev_quarter_end_dt)
    amount = (COMPLETION_SUM * D('1.5') / NdsNew.DEFAULT.koef_on_dt(prev_quarter_end_dt)).quantize(D('1.00000'))

    acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_acts = [
        steps.CommonData.create_expected_pad(context, client_id, spendable_contract_id,
                                             prev_quarter_start_dt, partner_reward=amount,
                                             nds=nds)
    ]
    expected_acts[0]['end_dt'] = prev_quarter_end_dt
    utils.check_that(acts, contains_dicts_with_entries(expected_acts),
                     'Сравниваем данные из акта с шаблоном')