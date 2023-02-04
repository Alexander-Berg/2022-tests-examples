# -*- coding: utf-8 -*-
from decimal import Decimal as D, ROUND_HALF_UP

import pytest
from datetime import datetime
from dateutil.relativedelta import relativedelta
import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType,  Collateral
from btestlib.constants import Export
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries, has_entries_casted
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL
from btestlib.constants import PromocodeClass
import balance.tests.promocode_new.promocode_commons as promo_steps

_, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, \
    PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT = utils.Date.previous_three_months_start_end_dates()

CURRENT_MONTH_START_DT, CURRENT_MONTH_END_DT = utils.Date.current_month_first_and_last_days()

FAKE_TPT_CONTRACT_ID = 123
FAKE_TPT_CLIENT_ID = 123
FAKE_TPT_PERSON_ID = 123

ADVANCE_SUM = D('100')
SERVICE_MIN_COST = D('100')
PROMO_SUM = D('20')
PERSONAL_ACC_SUM_1 = D('173')
PAYMENT_SUM_1 = D('87.12')
REFUND_SUM_1 = D('14.94')
PERSONAL_ACC_SUM_2 = D('137')
PAYMENT_SUM_2 = D('45.73')
REFUND_SUM_2 = D('12.98')
PERSONAL_ACC_SUM_PROMO = D('100')
PAYMENT_SUM_PROMO = D('10')

context = CORP_TAXI_RU_CONTEXT_GENERAL

@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.parametrize('service_min_cost_1, service_min_cost_2',
                         [
                             (D('0'), SERVICE_MIN_COST)
                         ])
def test_collateral(service_min_cost_1, service_min_cost_2):
    params = {'start_dt': MONTH_BEFORE_PREV_START_DT, 'service_min_cost': service_min_cost_1}
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params=params)

    # !!!!!!! при создании допника здесь не работает
    # создаем допник на изменение минималки
    steps.ContractSteps.create_collateral(Collateral.CHANGE_MIN_COST,
                                          {'CONTRACT2_ID': contract_id, 'DT': PREVIOUS_MONTH_START_DT.isoformat(),
                                           'IS_SIGNED': PREVIOUS_MONTH_START_DT.isoformat(),
                                           'SERVICE_MIN_COST': service_min_cost_2})

    is_act_expected, act_sum_1 = act_expectation(service_min_cost_1, PAYMENT_SUM_1, REFUND_SUM_1)

    # создадим платежи и сгенерируем акт за 1 месяц
    create_payments_and_completions(MONTH_BEFORE_PREV_START_DT, client_id, contract_id,
                    payment_sum=PAYMENT_SUM_1, refund_sum=REFUND_SUM_1,
                    is_act_expected=is_act_expected)

    # !!!!!!! при создании допника здесь работает
    # создаем допник на изменение минималки
    # steps.ContractSteps.create_collateral(Collateral.CHANGE_MIN_COST,
    #                                       {'CONTRACT2_ID': contract_id, 'DT': PREVIOUS_MONTH_START_DT.isoformat(),
    #                                        'IS_SIGNED': PREVIOUS_MONTH_START_DT.isoformat(),
    #                                        'SERVICE_MIN_COST': service_min_cost_2})

    is_act_expected, act_sum_2 = act_expectation(service_min_cost_2, PAYMENT_SUM_2, REFUND_SUM_2)

    # и еще платежи и акт за 1 месяц
    create_payments_and_completions(MONTH_BEFORE_PREV_START_DT, client_id, contract_id,
                                    payment_sum=D('0.3') * PAYMENT_SUM_2, refund_sum=D('0.3') * REFUND_SUM_2,
                                    process_taxi=False)

    # #а теперь за второй
    create_payments_and_completions(PREVIOUS_MONTH_START_DT, client_id, contract_id,
                                    payment_sum=D('0.7') * PAYMENT_SUM_2, refund_sum=D('0.7') * REFUND_SUM_2,
                                    is_act_expected=is_act_expected)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    expected_act_data, expected_invoice_data = prepare_expected_data(contract_id, person_id, act_sum_1, act_sum_2)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акт с шаблоном')


def prepare_expected_data(contract_id, person_id, act_sum_1, act_sum_2, pers_acc_sum_1=D('0'), pers_acc_sum_2=D('0'),
                          dt_1=MONTH_BEFORE_PREV_END_DT, dt_2=PREVIOUS_MONTH_END_DT):
    expected_act_data = []
    if act_sum_1:
        expected_act_data_first_month = steps.CommonData.create_expected_act_data(amount=act_sum_1,
                                                                                  act_date=utils.Date.last_day_of_month(
                                                                                      dt_1))
        expected_act_data.append(expected_act_data_first_month)

    if act_sum_2:
        expected_act_data_second_month = steps.CommonData.create_expected_act_data(amount=act_sum_2,
                                                                                   act_date=utils.Date.last_day_of_month(
                                                                                       dt_2))
        expected_act_data.append(expected_act_data_second_month)

    consume_sum = max(pers_acc_sum_1 + pers_acc_sum_2, act_sum_1 + act_sum_2)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     consume_sum,
                                                                                     total_act_sum=act_sum_1 + act_sum_2,
                                                                                     dt=dt_1)
    return expected_act_data, expected_invoice_data


def act_expectation(min_cost, payment_sum, refund_sum):
    is_act_expected = False if payment_sum - refund_sum <= 0 else True
    act_sum = max(min_cost, payment_sum - refund_sum) if is_act_expected else D('0')
    return is_act_expected, act_sum


def create_payments_and_completions(dt, client_id, contract_id,
                                    personal_acc_payment=0.0, payment_sum=0.0,
                                    refund_sum=0.0, is_act_expected=True, process_taxi=True):
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    if personal_acc_payment:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_payment, payment_dt=dt)
    if payment_sum - refund_sum != 0:
        create_tpt(context, dt, client_id, payment_sum=payment_sum, refund_sum=refund_sum)

    if process_taxi:
        steps.TaxiSteps.process_taxi(contract_id, dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, dt,
                                                                       manual_export=is_act_expected)


def create_tpt(context, dt, client_id, payment_sum=0.0, refund_sum=0.0):
    act_sum = steps.SimpleApi.create_fake_tpt_data(context, FAKE_TPT_CLIENT_ID, FAKE_TPT_PERSON_ID,
                                                   FAKE_TPT_CONTRACT_ID, dt,
                                                   [{'client_amount': payment_sum,
                                                     'client_id': client_id,
                                                     'transaction_type': TransactionType.PAYMENT},
                                                    {'client_amount': refund_sum,
                                                     'client_id': client_id,
                                                     'transaction_type': TransactionType.REFUND}],
                                                   sum_key='client_amount')
    return act_sum