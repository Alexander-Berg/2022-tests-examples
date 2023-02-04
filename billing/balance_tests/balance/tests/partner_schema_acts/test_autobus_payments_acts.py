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
from btestlib.constants import TransactionType, PaysysType, PaymentType
from btestlib.data.partner_contexts import BUSES_RU_CONTEXT
from btestlib.matchers import contains_dicts_with_entries, contains_dicts_equal_to

payment_sum = D('30.2')
refund_sum = D('10.4')
service_sum = D('45.3')
compensation_discount_sum = D('12.2')
compensation_discount_refund_sum = D('3.2')
cash_sum = D('34.2')
cash_refund_sum = D('1.1')

contract_start_dt, _, month2_start_dt, month2_end_dt, month3_start_dt, month3_end_dt = \
    utils.Date.previous_three_months_start_end_dates(dt=datetime.datetime.today())


def create_completions(context, client_id, contract_id, person_id, dt, additional_services=None, coef=D('1')):
    sum_service = D('0')
    if additional_services:
        sum_service = service_sum * coef
        steps.PartnerSteps.create_autobus_completion(client_id, month2_start_dt, sum_service)
    sum_payment = steps.SimpleApi.create_fake_tpt_data(
        context, client_id, person_id, contract_id, dt,
        [{'transaction_type': TransactionType.PAYMENT,
          'yandex_reward': coef * payment_sum},
         {'transaction_type': TransactionType.REFUND,
          'yandex_reward': coef * refund_sum},

         # промо с 0 АВ
         {'transaction_type': TransactionType.PAYMENT,
          'yandex_reward': 0,
          'payment_type': PaymentType.COMPENSATION_DISCOUNT,
          'paysys_type_cc': PaysysType.YANDEX},

         # промо с АВ > 0
         {'transaction_type': TransactionType.PAYMENT,
          'yandex_reward': compensation_discount_sum * coef,
          'payment_type': PaymentType.COMPENSATION_DISCOUNT,
          'paysys_type_cc': PaysysType.YANDEX},
         {'transaction_type': TransactionType.REFUND,
          'yandex_reward': compensation_discount_refund_sum * coef,
          'payment_type': PaymentType.COMPENSATION_DISCOUNT,
          'paysys_type_cc': PaysysType.YANDEX},

         # сash
         {'transaction_type': TransactionType.PAYMENT,
          'yandex_reward': cash_sum * coef,
          'payment_type': PaymentType.CASH,
          'paysys_type_cc': PaysysType.SUBPARTNER,
          'paysys_partner_id': 888888},
         {'transaction_type': TransactionType.REFUND,
          'yandex_reward': cash_refund_sum * coef,
          'payment_type': PaymentType.CASH,
          'paysys_type_cc': PaysysType.SUBPARTNER,
          'paysys_partner_id': 888888}],
        sum_key='yandex_reward')
    return sum_payment, sum_service


# тест на генерацию актов для договора с сервисом автобусы без данных
@reporter.feature(Features.AUTOBUS, Features.ACT)
@pytest.mark.tickets('BALANCE-23988')
@pytest.mark.parametrize(
    'context',
    [
        pytest.param(BUSES_RU_CONTEXT, id='Buses1 Russia'),
    ]
)
def test_autobus_act_wo_data(context):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params={'start_dt': contract_start_dt})

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id, contract_id, month2_start_dt, manual_export=False)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = [steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id, person_id, D('0'), dt=contract_start_dt)]

    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')

    # проверяем данные в акте
    act_data_first_month = steps.ActsSteps.get_act_data_by_client(client_id)

    utils.check_that(act_data_first_month, empty(),
                     'Сравниваем данные из акта с шаблоном')


# тест на генерацию актов для договора с сервисом автобусы с данными второй месяц (нарастающий итог)
@reporter.feature(Features.AUTOBUS, Features.ACT)
@pytest.mark.tickets('BALANCE-23988', 'BALANCE-30205')
@pytest.mark.parametrize(
    'context, additional_services',
    [
        pytest.param(BUSES_RU_CONTEXT, False, id='Buses1 Russia'),
    ]
)
def test_autobus_payments_acts(context, additional_services):
    coef_1 = D('1.3')
    coef_2 = D('2')
    coef_sum_second_month = coef_1 + coef_2
    coef_sum = coef_sum_second_month + D('1')

    services = [context.service.id]
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': contract_start_dt, 'services': services}
    )

    sum_payment, sum_service = create_completions(context, client_id, contract_id, person_id,
                                                  month2_start_dt, additional_services=additional_services)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_start_dt)

    create_completions(context, client_id, contract_id, person_id,
                       month2_start_dt, additional_services=additional_services, coef=D('1.3'))
    create_completions(context, client_id, contract_id, person_id,
                       month3_start_dt, additional_services=additional_services, coef=D('2'))

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month3_start_dt)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    # подготавливаем ожидаемые данные для счета
    invoice_sum = sum_payment * coef_sum
    if additional_services:
        invoice_sum += service_sum * coef_sum
    expected_invoice_data = [steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id, person_id, invoice_sum, dt=contract_start_dt)]

    # подготавливаем ожидаемые данные для акта
    expected_act_data = [steps.CommonData.create_expected_act_data(sum_payment, month2_end_dt)] + \
                        [steps.CommonData.create_expected_act_data(coef_sum_second_month * sum_payment, month3_end_dt)]
    if additional_services:
        expected_act_data.append(steps.CommonData.create_expected_act_data(sum_service, month2_end_dt))
        expected_act_data.append(
            steps.CommonData.create_expected_act_data(coef_sum_second_month * sum_service, month3_end_dt))

    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')
