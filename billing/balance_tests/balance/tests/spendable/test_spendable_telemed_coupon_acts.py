# -*- coding: utf-8 -*-

__author__ = 'atkaya'

from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import NdsNew
from btestlib.constants import TransactionType
from btestlib.data.partner_contexts import TELEMEDICINE_CONTEXT, TELEMEDICINE_SPENDABLE_CONTEXT
from btestlib.matchers import contains_dicts_equal_to

_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


@reporter.feature(Features.TELEMEDICINE, Features.PARTNER, Features.ACT)
@pytest.mark.tickets('BALANCE-26070')
# при нулевых открутках акта нет
def test_telemedicine_spendable_wo_act():
    # создаем данные
    client_id, spendable_contract_id, partner_person_id = create_client_persons_contracts()

    # добавляем открутки
    steps.SimpleApi.create_fake_tpt_data(TELEMEDICINE_SPENDABLE_CONTEXT, client_id, partner_person_id,
                                         spendable_contract_id,
                                         first_month_start_dt,
                                         [{'transaction_type': TransactionType.PAYMENT,
                                           'amount': D('0')},
                                          {'transaction_type': TransactionType.REFUND,
                                           'amount': D('0')}])

    # запускаем генерацию актов для расходного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, first_month_end_dt)

    # проверяем данные в t_partner_act_data
    data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)

    utils.check_that(data, contains_dicts_equal_to([]),
                     'Сравниваем данные из акта с шаблоном')


# проверяем нарастающий итог
@reporter.feature(Features.TELEMEDICINE, Features.PARTNER, Features.ACT)
@pytest.mark.tickets('BALANCE-26070')
@pytest.mark.parametrize(
        'nds',
        [
            (NdsNew.DEFAULT),
            (NdsNew.NOT_RESIDENT),
        ],
        ids=['Act for second month'
            , 'Act for second month wo nds']
)
def test_telemedicine_coupon_act_second_period(nds):
    # создаем данные
    client_id, contract_id, person_id = create_client_persons_contracts(nds)

    # добавляем открутки
    first_month_sum = create_data(client_id, person_id, contract_id, first_month_start_dt)

    # запускаем генерацию актов для расходного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_end_dt)

    # добавляем новые открутки
    second_month_sum_1 = create_data(client_id, person_id, contract_id, first_month_start_dt, coef=D('0.3'))
    second_month_sum_2 = create_data(client_id, person_id, contract_id, second_month_start_dt, coef=D('0.4'))

    # запускаем генерацию актов для расходного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, second_month_end_dt)

    # проверяем данные в t_partner_act_data
    data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_act_data = [create_expected_act_data(TELEMEDICINE_SPENDABLE_CONTEXT, client_id, contract_id, nds,
                                                      first_month_sum,
                                                      first_month_start_dt),
                             create_expected_act_data(TELEMEDICINE_SPENDABLE_CONTEXT, client_id, contract_id, nds,
                                                      second_month_sum_1+second_month_sum_2,
                                                      second_month_start_dt)]

    utils.check_that(data, contains_dicts_equal_to(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# --------------Utils--------------

def create_client_persons_contracts(nds=NdsNew.DEFAULT):
    client_id, person_id, general_contract_id, _ = steps.ContractSteps.create_partner_contract(TELEMEDICINE_CONTEXT,
                                                                                               additional_params={
                                                                                                   'start_dt': first_month_start_dt})

    _, partner_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
            TELEMEDICINE_SPENDABLE_CONTEXT,
            client_id=client_id,
            additional_params={'start_dt': first_month_start_dt, 'link_contract_id': general_contract_id,
                               'nds': nds.nds_id})
    return client_id, spendable_contract_id, partner_person_id


def create_data(client_id, person_id, contract_id, dt, coef=D('1')):
    amount = steps.SimpleApi.create_fake_tpt_data(TELEMEDICINE_SPENDABLE_CONTEXT, client_id, person_id,
                                                  contract_id,
                                                  dt,
                                                  [{'transaction_type': TransactionType.PAYMENT,
                                                    'amount': D('34.2') * coef},
                                                   {'transaction_type': TransactionType.REFUND,
                                                    'amount': D('2.7') * coef}],
                                                  sum_key='amount')
    return amount

def create_expected_act_data(context, client_id, contract_id, nds, sum, dt):
    amount = round(D(sum) / nds.koef_on_dt(dt), 5)
    expected_act_data = steps.CommonData.create_expected_pad(context, client_id, contract_id, dt,
                                                             partner_reward=amount, nds=nds)
    return expected_act_data