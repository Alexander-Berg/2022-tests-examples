# -*- coding: utf-8 -*-

__author__ = 'mindlin'

from decimal import Decimal
from pprint import pprint

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType, PartnerPaymentType
from btestlib.data.partner_contexts import DRIVE_CORP_CONTEXT
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries

# эту сумму зачисляем на счёт
PERSONAL_ACC_SUM = Decimal('4305')
# суммы для откруток
PAYMENT_SUM = Decimal('2143')
REFUND_SUM = Decimal('89')
INT_COEF_1 = Decimal('0.4')
INT_COEF_2 = Decimal('0.9')

MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT, _, _ = \
    utils.Date.previous_three_months_start_end_dates()
CURRENT_MONTH_START_DT, _ = utils.Date.current_month_first_and_last_days()
params = {
    'start_dt': MONTH_BEFORE_PREV_START_DT
}


# проверим генерацию актов без данных
@reporter.feature(Features.DRIVE_CORP)
def test_act_generation_wo_data():
    context = DRIVE_CORP_CONTEXT
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                                       additional_params=params,
                                                                                       is_offer=True, is_postpay=0)
    # Сгенерируем пустой акт без платежей и откруток
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, MONTH_BEFORE_PREV_START_DT)

    # Возьмём данные по счету
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # Возьмём данные по акту
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     Decimal('0'),
                                                                                     dt=MONTH_BEFORE_PREV_START_DT)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')


# акты за два месяца и накопительный итог
@reporter.feature(Features.DRIVE_CORP)
@pytest.mark.smoke
def test_act_generation_two_month():
    context = DRIVE_CORP_CONTEXT
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                                       additional_params=params,
                                                                                       is_offer=True, is_postpay=0)

    # сгенерируем акт за предпредыдущий месяц
    sum_first_month_1, sum_first_month_int_1 = create_act(MONTH_BEFORE_PREV_START_DT,
                                                          client_id, contract_id, context=context)
    # создадим фэйковый платеж в предпредыдущем во имя накопительного итога
    sum_first_month_2, sum_first_month_int_2 = create_completions(context, MONTH_BEFORE_PREV_END_DT,
                                                                  client_id, coef=INT_COEF_1)

    sum_second_month, sum_second_month_int = create_completions(context, PREVIOUS_MONTH_END_DT, client_id,
                                                                coef=INT_COEF_2)

    # закроем предыдущий месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREVIOUS_MONTH_END_DT)

    # получим данные для проверки. Проверяем только второй месяц
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    invoice_amount = sum_first_month_1 + sum_first_month_2 + sum_second_month \
                     + sum_first_month_int_1 + sum_first_month_int_2 + sum_second_month_int

    # Сформируем ожидаемые данные для счета.
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     invoice_amount,
                                                                                     dt=MONTH_BEFORE_PREV_START_DT)

    act_amount_first_month = sum_first_month_1 + sum_first_month_int_1

    act_amount_second_month = sum_first_month_2 + sum_second_month + sum_first_month_int_2 + sum_second_month_int

    # сформируем ожидаемые данные для акта.
    expected_act_data_first_month = steps.CommonData.create_expected_act_data(amount=act_amount_first_month,
                                                                              act_date=utils.Date.last_day_of_month(
                                                                                  MONTH_BEFORE_PREV_END_DT))
    expected_act_data_second_month = steps.CommonData.create_expected_act_data(amount=act_amount_second_month,
                                                                               act_date=PREVIOUS_MONTH_END_DT)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month,
                                                            expected_act_data_second_month]),
                     'Сравниваем данные из акт с шаблоном')

    balance = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    pprint(balance)

    # BALANCE-38167 - отдаем PersonalAccountExternalID для сервиса 702
    pa_external_id = steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code)
    assert balance[0]['PersonalAccountExternalID'] == pa_external_id


# --------------------------------------------------------------------------------------------------------------------
# Utils

def create_act(dt, client_id, contract_id, context):
    first_month_day, last_month_day = utils.Date.current_month_first_and_last_days(dt)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM, payment_dt=first_month_day)

    act_sum, act_sum_internal = create_completions(context, first_month_day, client_id)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, last_month_day)
    return act_sum, act_sum_internal


def create_completions(context, dt, client_id, coef=Decimal('1')):
    act_sum_internal = Decimal('0')
    act_sum = Decimal('0')

    steps.PartnerSteps.create_fake_product_completion(
        dt,
        transaction_dt=dt,
        client_id=client_id,
        service_id=context.service.id,
        service_order_id=0,
        type='carsharing',
        payment_type=PartnerPaymentType.WALLET,
        transaction_type=TransactionType.PAYMENT.name,
        currency=context.currency.iso_code,
        amount=(PAYMENT_SUM / 2) * coef,
    )

    steps.PartnerSteps.create_fake_product_completion(
        dt,
        transaction_dt=dt,
        client_id=client_id,
        service_id=context.service.id,
        service_order_id=0,
        type='toll_road',
        payment_type=PartnerPaymentType.WALLET,
        transaction_type=TransactionType.PAYMENT.name,
        currency=context.currency.iso_code,
        amount=(PAYMENT_SUM / 2) * coef,
    )

    steps.PartnerSteps.create_fake_product_completion(
        dt,
        transaction_dt=dt,
        client_id=client_id,
        service_id=context.service.id,
        service_order_id=0,
        type='carsharing',
        payment_type=PartnerPaymentType.WALLET,
        transaction_type=TransactionType.REFUND.name,
        currency=context.currency.iso_code,
        amount=REFUND_SUM * coef,
    )

    act_sum += utils.dround((PAYMENT_SUM - REFUND_SUM) * coef * context.nds.koef_on_dt(dt), 2)

    return act_sum, act_sum_internal
