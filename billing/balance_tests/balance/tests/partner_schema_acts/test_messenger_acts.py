# coding: utf-8

from decimal import Decimal as D

import pytest
from hamcrest import empty

from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import reporter
from btestlib import utils, matchers
from btestlib.constants import TransactionType, PaymentType, PaysysType, Products
from btestlib.data.partner_contexts import MESSENGER_CONTEXT
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries

generate_acts = steps.CommonPartnerSteps.generate_partner_acts_fair_and_export

pytestmark = [
    reporter.feature(Features.ACT),
    pytest.mark.tickets('BALANCE-28987'),
]

# компенсаций нет https://st.yandex-team.ru/OEBS-21488#1536759931000


payment_sum = D('3000.2')
refund_sum = D('34.7')

_, _, month1_start_dt, month1_end_dt, month2_start_dt, month2_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


# закрытие с платежами за два месяца (нарастающий итог)
@pytest.mark.smoke
def test_close_two_months():
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        MESSENGER_CONTEXT,
        additional_params={'start_dt': month1_start_dt}
    )

    first_month_sum = create_completions(client_id, contract_id, person_id, month1_start_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month1_end_dt)

    second_month_sum_1 = create_completions(client_id, contract_id, person_id, month1_start_dt, coef=D('0.3'))
    second_month_sum_2 = create_completions(client_id, contract_id, person_id, month2_start_dt, coef=D('0.4'))

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_end_dt)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    total_sum = first_month_sum + second_month_sum_1 + second_month_sum_2

    # создаем шаблон для сравнения
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        MESSENGER_CONTEXT, contract_id,
        person_id,
        total_sum,
        dt=month1_start_dt)

    expected_act_data = [
        steps.CommonData.create_expected_act_data(first_month_sum, month1_end_dt),
        steps.CommonData.create_expected_act_data(second_month_sum_1 + second_month_sum_2, month2_end_dt)
    ]

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')

    # проверяем, что в счетах консьюмы выставлены по правильным сервисам
    check_consumes(contract_id, total_sum)


# закрытие без платежей
def test_close_month_wo_transactions():
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        MESSENGER_CONTEXT,
        additional_params={'start_dt': month2_start_dt})

    # вызываем генерацию актов
    generate_acts(client_id, contract_id, month2_end_dt, manual_export=False)

    # проверяем данные в счете
    invoice_data_first_month = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        MESSENGER_CONTEXT, contract_id,
        person_id,
        D('0'),
        dt=month2_start_dt)

    utils.check_that(invoice_data_first_month, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')

    # проверяем, что консьюмов нет
    check_consumes(contract_id, 0)


# ----- utils

def create_completions(client_id, contract_id, person_id, dt, coef=D('1')):
    sum = steps.SimpleApi.create_fake_tpt_data(MESSENGER_CONTEXT, client_id, person_id, contract_id, dt, [
        {'transaction_type': TransactionType.PAYMENT, 'yandex_reward': payment_sum * coef},
        {'transaction_type': TransactionType.REFUND, 'yandex_reward': refund_sum * coef},

        {'transaction_type': TransactionType.PAYMENT, 'payment_type': PaymentType.COMPENSATION,
         'paysys_type_cc': PaysysType.YANDEX},
    ], sum_key='yandex_reward')
    return sum


# проверяем, что в счетах консьюмы выставлены по правильным сервисам
def check_consumes(contract_id, reward):
    expected_consume_lines = []

    if reward > 0:
        expected_consume_lines.append({
            'completion_sum': reward,
            'act_sum': reward,
            'service_id': MESSENGER_CONTEXT.service.id,
            'service_code': Products.MESSENGER.id,
        })

    query_consumes = '''
            SELECT c.completion_sum, c.ACT_SUM, o.SERVICE_ID, o.SERVICE_CODE
            FROM T_CONSUME c,
              t_order o,
              t_invoice i
            WHERE i.CONTRACT_ID = :contract_id
            AND c.PARENT_ORDER_ID = o.id
            AND i.id = c.INVOICE_ID'''

    actual_consume_lines = db.balance().execute(query_consumes, {'contract_id': contract_id})

    utils.check_that(actual_consume_lines,
                     matchers.contains_dicts_with_entries(expected_consume_lines, same_length=True),
                     'Проверяем данные консьюмов')
