# -*- coding: utf-8 -*-

__author__ = 'mindlin'

from dateutil.relativedelta import relativedelta
from decimal import Decimal

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as tsteps
from balance.features import Features
from btestlib import utils
from btestlib.constants import DriveB2BOrderType, Products
from btestlib.data.partner_contexts import DRIVE_B2B_CONTEXT
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries

# эту сумму зачисляем на счёт
PERSONAL_ACC_SUM = Decimal('4305')
# суммы для откруток
PAYMENT_SUM_DECOUP = Decimal('2252')
REFUND_SUM_DECOUP = Decimal('41')
INT_COEF_1 = Decimal('0.4')
INT_COEF_2 = Decimal('0.9')

_, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, \
    PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT = utils.Date.previous_three_months_start_end_dates()


# проверим генерацию актов без данных
@reporter.feature(Features.DRIVE_B2B)
def test_act_generation_wo_data():
    context = DRIVE_B2B_CONTEXT
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params={'start_dt': PREVIOUS_MONTH_START_DT}, is_offer=True, is_postpay=0
    )
    # Сгенерируем пустой акт без платежей и откруток
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, PREVIOUS_MONTH_START_DT)

    # Возьмём данные по счету
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # Возьмём данные по акту
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     Decimal('0'),
                                                                                     dt=PREVIOUS_MONTH_START_DT)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')


# акты за два месяца и накопительный итог
@reporter.feature(Features.DRIVE_B2B)
@pytest.mark.smoke
def test_act_generation_two_month():
    context = DRIVE_B2B_CONTEXT
    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')

    # 2 месяца до даты миграции
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
    month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
        utils.Date.previous_two_months_dates(migration_dt)
    # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
    posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
    oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
    month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
        utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, additional_params={'start_dt': month_migration_minus2_start_dt}, is_offer=True, is_postpay=0
    )

    # сгенерируем акт за предпредыдущий месяц
    sum_first_month_1 = create_act(month_migration_minus2_start_dt, client_id, contract_id, context=context)
    # создадим фэйковый платеж в предпредыдущем во имя накопительного итога
    sum_first_month_2 = create_completions(context, month_migration_minus2_end_dt, client_id, coef=INT_COEF_1)

    sum_second_month = create_completions(context, month_migration_minus1_end_dt, client_id, coef=INT_COEF_2)

    # закроем предыдущий месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_migration_minus1_end_dt)

    # получим данные для проверки. Проверяем только второй месяц
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    invoice_amount = sum_first_month_1 + sum_first_month_2 + sum_second_month

    # Сформируем ожидаемые данные для счета.
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     invoice_amount,
                                                                                     dt=month_migration_minus2_start_dt)

    act_amount_first_month = sum_first_month_1
    act_amount_second_month = sum_first_month_2 + sum_second_month

    # сформируем ожидаемые данные для акта.
    expected_act_data_first_month = steps.CommonData.create_expected_act_data(amount=act_amount_first_month,
                                                                              act_date=month_migration_minus2_end_dt)
    expected_act_data_second_month = steps.CommonData.create_expected_act_data(amount=act_amount_second_month,
                                                                               act_date=month_migration_minus1_end_dt)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month,
                                                            expected_act_data_second_month]),
                     'Сравниваем данные из акт с шаблоном')

    # ЛОГИКА ПОСЛЕ МИГРАЦИИ НА ОЕБС

    # сгенерируем акт за предпредыдущий месяц
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM, payment_dt=month_minus2_start_dt)

    sum_third_month_1 = create_oebs_completions(context, month_minus2_start_dt, contract_id, client_id)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_end_dt)

    # создадим фэйковый платеж в предпредыдущем во имя накопительного итога
    sum_third_month_2 = create_oebs_completions(context, month_minus2_end_dt, contract_id, client_id, coef=INT_COEF_1)

    sum_fourth_month = create_oebs_completions(context, month_minus1_end_dt, contract_id, client_id, coef=INT_COEF_2)

    # закроем предыдущий месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus1_end_dt)

    # получим данные для проверки. Проверяем только второй месяц
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    invoice_amount = sum_first_month_1 + sum_first_month_2 + sum_second_month \
                     + sum_third_month_1 + sum_third_month_2 + sum_fourth_month

    # Сформируем ожидаемые данные для счета.
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     invoice_amount,
                                                                                     dt=month_migration_minus2_start_dt)

    # У ОЕБС не должно быть нарастающего итога, но на всякий случай
    act_amount_third_month = sum_third_month_1
    act_amount_fourth_month = sum_third_month_2 + sum_fourth_month

    # сформируем ожидаемые данные для акта.
    expected_act_data_third_month = steps.CommonData.create_expected_act_data(amount=act_amount_third_month,
                                                                              act_date=month_minus2_end_dt)
    expected_act_data_fourth_month = steps.CommonData.create_expected_act_data(amount=act_amount_fourth_month,
                                                                               act_date=month_minus1_end_dt)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month,
                                                            expected_act_data_second_month,
                                                            expected_act_data_third_month,
                                                            expected_act_data_fourth_month]),
                     'Сравниваем данные из акт с шаблоном')


# --------------------------------------------------------------------------------------------------------------------
# Utils

def create_act(dt, client_id, contract_id, context):
    first_month_day, last_month_day = utils.Date.current_month_first_and_last_days(dt)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM, payment_dt=first_month_day)

    act_sum = create_completions(context, first_month_day, client_id)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, last_month_day)
    return act_sum


def create_completions(context, dt, client_id, coef=Decimal('1')):
    act_sum = Decimal('0')

    order_dicts_tlog = [
        {
            'service_id': context.service.id,
            'amount': PAYMENT_SUM_DECOUP * coef / context.nds.koef_on_dt(dt),
            'type': DriveB2BOrderType.MAIN,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'last_transaction_id': 99
        },
        {
            'service_id': context.service.id,
            'amount': -REFUND_SUM_DECOUP * coef / context.nds.koef_on_dt(dt),
            'type': DriveB2BOrderType.MAIN,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'last_transaction_id': 100
        },
    ]
    tsteps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)
    act_sum += (PAYMENT_SUM_DECOUP - REFUND_SUM_DECOUP) * coef

    return act_sum


def create_oebs_completions(context, dt, contract_id, client_id, coef=Decimal('1')):
    act_sum = Decimal('0')
    compls_dicts = [
        {
            'service_id': context.service.id,
            'last_transaction_id': 99,
            'amount': PAYMENT_SUM_DECOUP * coef,
            'product_id': Products.DRIVE_B2B_RUB.id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        },
        {
            'service_id': context.service.id,
            'last_transaction_id': 99,
            'amount': -REFUND_SUM_DECOUP * coef,
            'product_id': Products.DRIVE_B2B_RUB.id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        }
    ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_dicts)
    act_sum += (PAYMENT_SUM_DECOUP - REFUND_SUM_DECOUP) * coef

    return act_sum
