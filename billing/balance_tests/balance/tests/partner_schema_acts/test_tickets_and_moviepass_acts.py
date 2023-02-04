# -*- coding: utf-8 -*-

__author__ = 'a-vasin'

from decimal import Decimal

import pytest
from hamcrest import empty

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType, PaymentType, PaysysType
from btestlib.data.partner_contexts import TICKETS_118_CONTEXT, TICKETS_MOVIEPASS_CONTEXT
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries

_, _, month1_start_dt, month1_end_dt, month2_start_dt, month2_end_dt = \
    utils.Date.previous_three_months_start_end_dates()

parametrize_context = pytest.mark.parametrize("context", [
    TICKETS_MOVIEPASS_CONTEXT,
], ids=lambda c: c.name)


@parametrize_context
def test_act_wo_data(context):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month2_start_dt}
    )

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_start_dt,
                                                                   manual_export=False)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context, contract_id,
                                                                                     person_id, Decimal('0'),
                                                                                     dt=month2_start_dt)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.smoke
@parametrize_context
def test_act_second_month(context):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month1_start_dt}
    )

    first_tickets_sum, first_moviepass_sum = create_completions(context, client_id, person_id, contract_id,
                                                                month1_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month1_start_dt)

    second_tickets_sum_1, second_moviepass_sum_1 = create_completions(context, client_id, person_id, contract_id,
                                                                      month1_start_dt, coef=Decimal('0.3'))
    second_tickets_sum_2, second_moviepass_sum_2 = create_completions(context, client_id, person_id, contract_id,
                                                                      month2_start_dt, coef=Decimal('0.4'))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_start_dt)

    final_sum = first_tickets_sum + first_moviepass_sum + second_tickets_sum_1 + second_moviepass_sum_1 + \
                second_tickets_sum_2 + second_moviepass_sum_2

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context, contract_id,
                                                                                     person_id,
                                                                                     final_sum,
                                                                                     dt=month1_start_dt)

    expected_act_data = [
        steps.CommonData.create_expected_act_data(first_tickets_sum + first_moviepass_sum, month1_end_dt),
        steps.CommonData.create_expected_act_data(second_tickets_sum_1 + second_moviepass_sum_1 +
                                                  second_tickets_sum_2 + second_moviepass_sum_2, month2_end_dt)
    ]

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


# ------------------------------------------------------------
# Utils

def create_completions(context, client_id, person_id, contract_id, dt, coef=Decimal('1')):
    sum_tickets = steps.SimpleApi.create_fake_tpt_data(TICKETS_118_CONTEXT, client_id, person_id, contract_id, dt, [
        {'transaction_type': TransactionType.PAYMENT, 'yandex_reward': Decimal('45.2') * coef},
        {'transaction_type': TransactionType.REFUND, 'yandex_reward': Decimal('2.7') * coef},

        {'transaction_type': TransactionType.PAYMENT, 'payment_type': PaymentType.COMPENSATION,
         'paysys_type_cc': PaysysType.YANDEX},
    ], sum_key='yandex_reward')

    sum_moviepass = steps.SimpleApi.create_fake_tpt_data(
        context, client_id, person_id, contract_id, dt,
        [
            {'transaction_type': TransactionType.PAYMENT,
             'yandex_reward': Decimal('77.8') * coef},
            {'transaction_type': TransactionType.REFUND,
             'yandex_reward': Decimal('7.5') * coef},

            {'transaction_type': TransactionType.PAYMENT,
             'payment_type': PaymentType.COMPENSATION,
             'paysys_type_cc': PaysysType.YANDEX},
        ], sum_key='yandex_reward')
    return sum_tickets, sum_moviepass
