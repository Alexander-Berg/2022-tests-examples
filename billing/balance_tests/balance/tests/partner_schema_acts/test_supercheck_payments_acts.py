# -*- coding: utf-8 -*-

__author__ = 'mindlin'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.data.partner_contexts import SUPERCHECK_CONTEXT
from btestlib.matchers import contains_dicts_with_entries, contains_dicts_equal_to

contract_start_dt, _, month2_start_dt, month2_end_dt, month3_start_dt, month3_end_dt = \
    utils.Date.previous_three_months_start_end_dates(dt=datetime.datetime.today())

parametrize_integration = pytest.mark.parametrize(
    'partner_integration_params',
    [
        pytest.param(steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT,
                     id='PARTNER_INTEGRATION'),
        pytest.param(None,
                     id='WO_PARTNER_INTEGRATION'),
    ]
)


# тест на генерацию актов для договора с сервисом Суперчек без данных
@reporter.feature(Features.SUPERCHECK, Features.ACT)
@pytest.mark.tickets('BALANCE-32085')
@parametrize_integration
def test_supercheck_act_wo_data(partner_integration_params):
    context = SUPERCHECK_CONTEXT

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params={'start_dt': contract_start_dt},
        partner_integration_params=partner_integration_params)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_start_dt,
                                                                   manual_export=False)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = [steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id,
                                                                                      D('0'), dt=contract_start_dt)]
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')

    # проверяем данные в акте
    act_data_first_month = steps.ActsSteps.get_act_data_by_client(client_id)

    utils.check_that(act_data_first_month, empty(),
                     'Сравниваем данные из акта с шаблоном')


# тест на генерацию актов для договора с сервисом Суперчек с данными второй месяц (нарастающий итог)
@reporter.feature(Features.SUPERCHECK, Features.ACT)
@pytest.mark.tickets('BALANCE-32085')
@parametrize_integration
def test_supercheck_payments_acts(partner_integration_params):
    context = SUPERCHECK_CONTEXT

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params={'start_dt': contract_start_dt},
        partner_integration_params=partner_integration_params)

    # добавляем открутки и генерим акты
    first_month_sum, second_month_sum = steps.SimpleApi.create_tipical_tpt_data_for_act(
        context,
        client_id,
        person_id, contract_id,
        month2_end_dt,
        month3_end_dt,
        sum_key='yandex_reward')

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения
    expected_invoice_data = [steps.CommonData.create_expected_invoice_data_by_context(
        context,
        contract_id,
        person_id,
        first_month_sum + second_month_sum,
        dt=month2_end_dt)]

    expected_act_data = [
        steps.CommonData.create_expected_act_data(first_month_sum, month2_end_dt),
        steps.CommonData.create_expected_act_data(second_month_sum, month3_end_dt),
    ]

    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')
