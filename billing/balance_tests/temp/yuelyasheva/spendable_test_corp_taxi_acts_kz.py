# -*- coding: utf-8 -*-

__author__ = 'yuelyasheva'

import datetime
from decimal import Decimal as D
from dateutil.relativedelta import relativedelta

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data import defaults
from btestlib.data.simpleapi_defaults import ThirdPartyData
from btestlib.matchers import contains_dicts_with_entries
from btestlib.data.partner_contexts import CORP_TAXI_KZ_CONTEXT_SPENDABLE

common_act_data = {'bucks': None,
                   'clicks': None,
                   'shows': None,
                   'product_price': None,
                   'place_id': None,
                   'partner_reward': None,
                   'page_id': defaults.taxi_corp()['PAGE_ID'],
                   'description': u'Яндекс Корпоративное Такси',
                   'currency': 'KZT',
                   'act_reward': None,
                   'act_reward_wo_nds': None,
                   'product_id': None}

previous_month_start_dt, previous_month_end_dt = utils.Date.previous_month_first_and_last_days()
prev_quarter_start_dt, prev_quarter_end_dt = utils.Date.get_previous_quarter(datetime.datetime.today())
prev_quarter_last_month_start_dt = prev_quarter_end_dt.replace(day=1)

payment_sum = 6000.9
refund_sum = 2000.1

SERVICE_ID = defaults.taxi_corp()['SERVICE_ID']
FIRM_ID = defaults.taxi_corp()['FIRM_ID']
CLIENT_AMOUNT = D('1000')


def create_client_persons_contracts(context, additional_params):
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
            context,
            additional_params=additional_params)
    return client_id, spendable_contract_id, person_id


@reporter.feature(Features.TAXI, Features.SPENDABLE, Features.CORPORATE,
                  Features.PARTNER, Features.ACT)
@pytest.mark.tickets('BALANCE-22114')
@pytest.mark.parametrize('contract_params, act_month, expected_data, contract_end_dt',
                         [
                             ({'PAYMENT_TYPE': 1, 'NDS': '12', 'DT': previous_month_start_dt,
                               'START_DT': previous_month_start_dt, 'END_DT': previous_month_end_dt,
                               'DO_COMPLETIONS': 1},
                              previous_month_end_dt,
                              common_act_data,
                              None),
                             ({'PAYMENT_TYPE': 1, 'NDS': '0', 'DT': previous_month_start_dt + relativedelta(days=5),
                               'START_DT': previous_month_start_dt, 'END_DT': previous_month_end_dt,
                               'DO_COMPLETIONS': 1},
                              previous_month_end_dt,
                              common_act_data,
                              previous_month_end_dt - relativedelta(days=5)),
                             ({'PAYMENT_TYPE': 2, 'NDS': '12', 'DT': prev_quarter_start_dt,
                               'START_DT': prev_quarter_start_dt, 'END_DT': prev_quarter_end_dt,
                               'DO_COMPLETIONS': 1},
                              prev_quarter_start_dt,
                              [],
                              None),
                             ({'PAYMENT_TYPE': 2, 'NDS': '12', 'DT': prev_quarter_start_dt,
                               'START_DT': prev_quarter_start_dt, 'END_DT': prev_quarter_end_dt,
                               'DO_COMPLETIONS': 1},
                              prev_quarter_end_dt,
                              common_act_data,
                              None),
                             ({'PAYMENT_TYPE': 2, 'NDS': '12', 'DT': prev_quarter_last_month_start_dt,
                               'START_DT': prev_quarter_start_dt, 'END_DT': prev_quarter_end_dt,
                               'DO_COMPLETIONS': 1},
                              prev_quarter_end_dt,
                              common_act_data,
                              None),
                             ({'PAYMENT_TYPE': 1, 'NDS': '12', 'DT': previous_month_start_dt,
                               'START_DT': previous_month_start_dt, 'END_DT': previous_month_end_dt,
                               'DO_COMPLETIONS': 0},
                              previous_month_end_dt,
                              [],
                              None),
                         ],
                         ids=[
                             'Acts for month with nds 12'
                             , 'Acts for month with nds 0'
                             , 'Acts for the first month of period'
                             , 'Acts for the last month of period'
                             , 'Acts for the last month of period, contract start date = last month of period'
                             , 'Acts without completions'
                         ]
                         )
def test_corp_taxi_spendable_act(contract_params, act_month, expected_data, contract_end_dt):
    nds = contract_params['NDS']
    payment_type = contract_params['PAYMENT_TYPE']
    contract_start_dt = contract_params['DT']
    start_dt = contract_params['START_DT']
    end_dt = contract_params['END_DT']
    do_completions = contract_params['DO_COMPLETIONS']

    context = CORP_TAXI_KZ_CONTEXT_SPENDABLE
    params = {
             'start_dt': contract_start_dt,
             'nds': nds,
             'payment_type': payment_type
    }

    if contract_end_dt:
        params.update({'END_DT': contract_end_dt})
        completion_dt = contract_end_dt - relativedelta(days=5)
    else: completion_dt = act_month

    taxi_client_id, taxi_contract_spendable_id, taxi_person_partner_id = create_client_persons_contracts(context, params)

    # добавляем открутки
    if do_completions:
        steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_CORP, taxi_contract_spendable_id,
                                                       taxi_person_partner_id,
                                                       taxi_client_id, payment_sum, None, dt=completion_dt,
                                                       client_id=777, client_amount=CLIENT_AMOUNT, internal=1)
        steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_CORP, taxi_contract_spendable_id,
                                                       taxi_person_partner_id,
                                                       taxi_client_id, payment_sum, None, dt=completion_dt,
                                                       client_id=777, client_amount=CLIENT_AMOUNT)
        steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_CORP, taxi_contract_spendable_id,
                                                       taxi_person_partner_id,
                                                       taxi_client_id, refund_sum, None,
                                                       transaction_type=TransactionType.REFUND,
                                                       dt=completion_dt, client_id=777,
                                                       client_amount=CLIENT_AMOUNT, internal=1)
        steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_CORP, taxi_contract_spendable_id,
                                                       taxi_person_partner_id,
                                                       taxi_client_id, refund_sum, None,
                                                       transaction_type=TransactionType.REFUND,
                                                       dt=completion_dt, client_id=777,
                                                       client_amount=CLIENT_AMOUNT)

    # запускаем генерацию актов для расходного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(taxi_contract_spendable_id, act_month)

    # проверяем данные в t_partner_act_data
    data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(taxi_contract_spendable_id)

    if not expected_data:
        expected_act_data = []
    else:
        expected_act_data = expected_data.copy()
        expected_act_data.update({'owner_id': taxi_client_id,
                                  'partner_reward_wo_nds': round(D(payment_sum - refund_sum) / (
                                      1 + D(nds) / 100), 5),
                                  'nds': nds, 'dt': start_dt, 'end_dt': end_dt})
        expected_act_data = [expected_act_data]

    utils.check_that(data, contains_dicts_with_entries(expected_act_data, in_order=True),
                     'Сравниваем данные из акта с шаблоном')


if __name__ == "__main__":
    # test_corp_taxi_spendable_act()
    pytest.main('-v -k "test_corp_taxi_spendable_act_0"')
