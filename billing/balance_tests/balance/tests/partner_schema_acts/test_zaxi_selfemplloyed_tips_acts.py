# -*- coding: utf-8 -*-
import copy

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import empty

from balance import balance_steps as steps
from btestlib import utils
from btestlib.data.partner_contexts import ZAXI_RU_SELFEMPLOYED_TIPS_CONTEXT
from btestlib.matchers import contains_dicts_with_entries, contains_dicts_equal_to

contract_start_dt, _, month2_start_dt, month2_end_dt, month3_start_dt, month3_end_dt = \
    utils.Date.previous_three_months_start_end_dates(dt=datetime.datetime.today())

PARTNER_INTEGRATIONS_PARAMS = {
    'link_integration_to_client': 1,
    'link_integration_to_client_args': {
        'integration_cc': 'zaxi_selfemployed_tips',
        'configuration_cc': 'zaxi_selfemployed_tips_conf',
    },
    'set_integration_to_contract': 1,
    'set_integration_to_contract_params': {
        'integration_cc': 'zaxi_selfemployed_tips',
    },
}


def test_zaxi_selfemployed_tips_act_wo_data():
    context = ZAXI_RU_SELFEMPLOYED_TIPS_CONTEXT

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params={'start_dt': contract_start_dt},
        partner_integration_params=copy.deepcopy(PARTNER_INTEGRATIONS_PARAMS))

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


def test_zaxi_selfemployed_tips_payments_acts():
    context = ZAXI_RU_SELFEMPLOYED_TIPS_CONTEXT

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params={'start_dt': contract_start_dt},
        partner_integration_params=copy.deepcopy(PARTNER_INTEGRATIONS_PARAMS), is_postpay=0)

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
