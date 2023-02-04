# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data.partner_contexts import REALTY_CONTEXT
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries

pytestmark = [
    reporter.feature(Features.REALTY, Features.ACT),
    pytest.mark.tickets('BALANCE-22481'),
    # Больше не модифицируем технического партнёра в БД.
    # Потому можем теперь просто разбирать платежи на настоящий технический договор
    # pytest.mark.no_parallel('realty')
]

payment_sum = D('1000.4')
refund_sum = D('12.3')

_, _, month1_start_dt, month1_end_dt, month2_start_dt, month2_end_dt = \
    utils.Date.previous_three_months_start_end_dates(dt=datetime.datetime.today())


def create_client_and_contract_for_acts():
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        REALTY_CONTEXT,
        additional_params={'start_dt': month1_start_dt}
    )
    # обвноляем тех клинта в t_config
    # steps.CommonPartnerSteps.update_t_config_ya_partner(REALTY_CONTEXT.service, client_id)

    return client_id, person_id, contract_id


def create_completions(client_id, person_id, contract_id, dt, coef=D('1')):
    sum = steps.SimpleApi.create_fake_tpt_data(REALTY_CONTEXT, client_id, person_id, contract_id, dt, [
        {'transaction_type': TransactionType.PAYMENT, 'amount': payment_sum * coef,
         'yandex_reward': payment_sum * coef, 'internal': 1},
        {'transaction_type': TransactionType.REFUND, 'amount': refund_sum * coef,
         'yandex_reward': refund_sum * coef, 'internal': 1}
    ])
    return sum


def test_realty_act_wo_data():
    client_id, person_id, contract_id = create_client_and_contract_for_acts()

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_end_dt,
                                                                   manual_export=False)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        REALTY_CONTEXT,
        contract_id,
        person_id,
        D('0'),
        dt=month1_start_dt)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.smoke
def test_realty_act_second_month():
    client_id, person_id, contract_id = create_client_and_contract_for_acts()

    first_month_sum = create_completions(client_id, person_id, contract_id, month1_start_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month1_end_dt)

    second_month_sum_1 = create_completions(client_id, person_id, contract_id, month1_start_dt, coef=D('0.3'))
    second_month_sum_2 = create_completions(client_id, person_id, contract_id, month2_start_dt, coef=D('0.4'))

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_end_dt)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(REALTY_CONTEXT, contract_id,
                                                                                     person_id,
                                                                                     first_month_sum + second_month_sum_1 + second_month_sum_2,
                                                                                     dt=month1_start_dt)

    expected_act_data = [
        steps.CommonData.create_expected_act_data(first_month_sum, month1_end_dt),
        steps.CommonData.create_expected_act_data(second_month_sum_1 + second_month_sum_2, month2_end_dt)
    ]

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')
