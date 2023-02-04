# -*- coding: utf-8 -*-

__author__ = 'blubimov'

from decimal import Decimal

import pytest
from hamcrest import empty

from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import constants as const
from btestlib import matchers
from btestlib import reporter
from btestlib import utils
from btestlib.data.partner_contexts import STATION_PAYMENTS_CONTEXT, STATION_SERVICES_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

generate_acts = steps.CommonPartnerSteps.generate_partner_acts_fair_and_export

pytestmark = [
    reporter.feature(Features.STATION, Features.ACT),
    pytest.mark.tickets('BALANCE-27187'),
    pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/partnersimpleacts/yandexstation')
]

AR_SERVICE = STATION_PAYMENTS_CONTEXT.service.id
SERVICES_SERVICE = STATION_SERVICES_CONTEXT.service.id

PRODUCT_IDS = {AR_SERVICE: const.Products.QUASAR.id,
               SERVICES_SERVICE: const.Products.QUASAR_SRV.id}

_, _, month1_start_dt, month1_end_dt, month2_start_dt, month2_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


# закрытие с платежами за два месяца (нарастающий итог)
@pytest.mark.smoke
def test_close_two_months():
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        STATION_PAYMENTS_CONTEXT,
        is_offer=1,
        additional_params={'start_dt': month1_start_dt}
    )

    invoices = get_invoices(contract_id)

    first_sum_ar, first_sum_services = create_completions(client_id, person_id, contract_id, month1_start_dt,
                                                          invoices[SERVICES_SERVICE])
    generate_acts(client_id, contract_id, month1_start_dt)

    second_sum_ar_1, second_sum_services_1 = create_completions(client_id, person_id, contract_id, month1_start_dt,
                                                                invoices[SERVICES_SERVICE], coef=Decimal('0.3'))
    second_sum_ar_2, second_sum_services_2 = create_completions(client_id, person_id, contract_id, month2_start_dt,
                                                                invoices[SERVICES_SERVICE], coef=Decimal('0.4'))
    generate_acts(client_id, contract_id, month2_start_dt)
    rewards = {AR_SERVICE: first_sum_ar + second_sum_ar_1 + second_sum_ar_2,
               SERVICES_SERVICE: first_sum_services + second_sum_services_1 + second_sum_services_2}

    # проверяем данные счетов
    check_invoices(contract_id, person_id, invoices, rewards)

    # проверяем, что в счетах консьюмы выставлены по правильным сервисам
    check_consumes(contract_id, invoices, rewards)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_act_data = [
        steps.CommonData.create_expected_act_data(first_sum_ar, month1_end_dt),
        steps.CommonData.create_expected_act_data(first_sum_services, month1_end_dt),
        steps.CommonData.create_expected_act_data(second_sum_ar_1 + second_sum_ar_2, month2_end_dt),
        steps.CommonData.create_expected_act_data(second_sum_services_1 + second_sum_services_2, month2_end_dt)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из актов с шаблоном')


# закрытие без платежей
def test_close_month_wo_transactions():
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        STATION_PAYMENTS_CONTEXT,
        is_offer=1,
        additional_params={'start_dt': month1_start_dt}
    )

    invoices = get_invoices(contract_id)
    rewards = {AR_SERVICE: 0, SERVICES_SERVICE: 0}

    # вызываем генерацию актов
    generate_acts(client_id, contract_id, month2_end_dt, manual_export=False)

    # проверяем данные счетов
    check_invoices(contract_id, person_id, invoices, rewards)

    # проверяем, что в счетах консьюмы выставлены по правильным сервисам
    check_consumes(contract_id, invoices, rewards)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')


# ---------- utils ----------
# https://st.yandex-team.ru/BALANCE-27187#1524859717000
def create_completions(client_id, person_id, contract_id, dt, eid_services, coef=Decimal('1')):
    amount_ar = steps.SimpleApi.create_fake_tpt_data(STATION_PAYMENTS_CONTEXT, client_id, person_id, contract_id, dt, [
        {'transaction_type': const.TransactionType.PAYMENT, 'yandex_reward': Decimal('30.2') * coef},
        # не вычитает АВ у рефандов, если они расчитываются.
        # Сейчас АВ для рефандов не рассчитывается, поэтому yandex_reward делаем = None
        {'transaction_type': const.TransactionType.REFUND, 'yandex_reward': None},

        {'transaction_type': const.TransactionType.PAYMENT, 'paysys_type_cc': const.PaysysType.YANDEX,
         'payment_type': const.PaymentType.COMPENSATION},
        {'transaction_type': const.TransactionType.REFUND, 'paysys_type_cc': const.PaysysType.YANDEX,
         'payment_type': const.PaymentType.COMPENSATION}
    ], sum_key='yandex_reward')

    amount_services = steps.SimpleApi.create_fake_tpt_data(STATION_PAYMENTS_CONTEXT, client_id, person_id, contract_id,
                                                           dt, [
                                                               {'transaction_type': const.TransactionType.REFUND,
                                                                'amount': Decimal('55.3') * coef,
                                                                'paysys_type_cc': const.PaysysType.YANDEX,
                                                                'payment_type': const.PaymentType.CORRECTION_COMMISSION,
                                                                'invoice_eid': eid_services}], sum_key='amount')

    return amount_ar, amount_services * Decimal('-1')


def get_invoices(contract_id):
    invoice_eid_ar, _ = steps.InvoiceSteps.get_personal_account_external_id_with_service_code(
        contract_id,
        const.ServiceCode.AGENT_REWARD)
    invoice_eid_ser, _ = steps.InvoiceSteps.get_personal_account_external_id_with_service_code(
        contract_id,
        const.ServiceCode.YANDEX_SERVICE)
    return {AR_SERVICE: invoice_eid_ar, SERVICES_SERVICE: invoice_eid_ser}


def check_invoices(contract_id, person_id, invoices, rewards):
    expected_invoice_lines = [
        steps.CommonData.create_expected_invoice_data_by_context(STATION_PAYMENTS_CONTEXT, contract_id, person_id,
                                                                 rewards[AR_SERVICE], dt=month1_start_dt,
                                                                 external_id=invoices[AR_SERVICE]),
        steps.CommonData.create_expected_invoice_data_by_context(STATION_SERVICES_CONTEXT, contract_id, person_id,
                                                                 rewards[SERVICES_SERVICE], dt=month1_start_dt,
                                                                 external_id=invoices[SERVICES_SERVICE]),
    ]

    actual_invoice_lines = db.balance().execute('SELECT * FROM t_invoice WHERE CONTRACT_ID = :contract_id',
                                                {'contract_id': contract_id})

    utils.check_that(actual_invoice_lines,
                     matchers.contains_dicts_with_entries(expected_invoice_lines, same_length=True),
                     'Проверяем данные счетов')


# проверяем, что в счетах консьюмы выставлены по правильным сервисам
def check_consumes(contract_id, invoices, rewards):
    expected_consume_lines = []
    if rewards[AR_SERVICE] > 0:
        expected_consume_lines.append({
            'external_id': invoices[AR_SERVICE],
            'completion_sum': rewards[AR_SERVICE],
            'act_sum': rewards[AR_SERVICE],
            'service_id': STATION_PAYMENTS_CONTEXT.service.id,
            'service_code': PRODUCT_IDS[AR_SERVICE],
        })
    if rewards[SERVICES_SERVICE] > 0:
        expected_consume_lines.append({
            'external_id': invoices[SERVICES_SERVICE],
            'completion_sum': rewards[SERVICES_SERVICE],
            'act_sum': rewards[SERVICES_SERVICE],
            'service_id': STATION_SERVICES_CONTEXT.service.id,
            'service_code': PRODUCT_IDS[SERVICES_SERVICE],
        })

    query_consumes = '''
            SELECT i.external_id, c.completion_sum, c.ACT_SUM, o.SERVICE_ID, o.SERVICE_CODE
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
