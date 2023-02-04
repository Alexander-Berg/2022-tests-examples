# coding: utf-8

__author__ = 'a-vasin'

from dateutil.relativedelta import relativedelta
from decimal import Decimal

from hamcrest import empty

from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as tsteps
from btestlib import utils
from btestlib.constants import Services, InvoiceType, CorpEdaOrderType, Products
from btestlib.data.partner_contexts import FOOD_CORP_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

AMOUNT = Decimal('100.11')
_, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, \
  PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT = utils.Date.previous_three_months_start_end_dates()


def test_corp_food_wo_data():
    context = FOOD_CORP_CONTEXT
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(
            context, additional_params={'start_dt': PREVIOUS_MONTH_START_DT}
        )

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREVIOUS_MONTH_END_DT,
                                                                   manual_export=False)

    invoice_data_third_month = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = \
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, amount=Decimal('0'))
    utils.check_that(invoice_data_third_month, contains_dicts_with_entries([expected_invoice_data]),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(act_data, empty(), u'Проверяем, что актов нет')

    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    utils.check_that(consume_data, empty(), u'Проверяем, что косьюмов нет')


def test_corp_food_two_months():
    context = FOOD_CORP_CONTEXT
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

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(
            context, additional_params={'start_dt': month_migration_minus2_start_dt}
        )

    create_completions(context, client_id, month_migration_minus2_start_dt, AMOUNT, coef=1)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt)

    create_completions(context, client_id, month_migration_minus1_start_dt, AMOUNT, coef=2)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus1_end_dt)

    invoice_data_third_month = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = \
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, amount=3 * AMOUNT)
    utils.check_that(invoice_data_third_month, contains_dicts_with_entries([expected_invoice_data]),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [
        steps.CommonData.create_expected_act_data(AMOUNT, month_migration_minus2_end_dt),
        steps.CommonData.create_expected_act_data(2 * AMOUNT, month_migration_minus1_end_dt)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')

    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    expected_consume_data = steps.CommonData.create_expected_consume_data(
        Products.FOOD_CORP_RUB.id,
        3 * AMOUNT,
        InvoiceType.PERSONAL_ACCOUNT
    )
    utils.check_that(consume_data, contains_dicts_with_entries([expected_consume_data]),
                     u'Сравниваем данные из консьюма с шаблоном')

    # ЛОГИКА ПОСЛЕ МИГРАЦИИ НА ОЕБС

    create_oebs_completions(context, contract_id, client_id, month_minus2_start_dt, AMOUNT, coef=1)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_minus2_end_dt)

    create_oebs_completions(context, contract_id, client_id, month_minus1_start_dt, AMOUNT, coef=2)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_minus1_end_dt)

    invoice_data_fourth_month = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    expected_invoice_data = \
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, amount=6 * AMOUNT)
    utils.check_that(invoice_data_fourth_month, contains_dicts_with_entries([expected_invoice_data]),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data.extend([
        steps.CommonData.create_expected_act_data(AMOUNT, month_minus2_end_dt),
        steps.CommonData.create_expected_act_data(2 * AMOUNT, month_minus1_end_dt)
    ])
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     u'Сравниваем данные из акта с шаблоном')

    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    expected_consume_data = steps.CommonData.create_expected_consume_data(
        Products.FOOD_CORP_RUB.id,
        6 * AMOUNT,
        InvoiceType.PERSONAL_ACCOUNT
    )
    utils.check_that(consume_data, contains_dicts_with_entries([expected_consume_data]),
                     u'Сравниваем данные из консьюма с шаблоном')


# ---------------------------------------------------------------------------------------------------------------
# Utils


def create_completions(context, client_id, dt, amount, coef):
    orders = [
        {
            'service_id': Services.FOOD_CORP.id,
            'amount': 2 * coef * amount / context.nds.koef_on_dt(dt),
            'type': CorpEdaOrderType.MAIN,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'last_transaction_id': 1
        },
        {
            'service_id': Services.FOOD_CORP.id,
            'amount': -coef * amount / context.nds.koef_on_dt(dt),
            'type': CorpEdaOrderType.MAIN,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'last_transaction_id': 1
        },
    ]
    tsteps.TaxiSteps.create_orders_tlog(client_id, orders)


def create_oebs_completions(context, contract_id, client_id, dt, amount, coef):
    compls_dicts = [
        {
            'service_id': context.service.id,
            'last_transaction_id': 99,
            'amount': 2 * coef * amount,
            'product_id': Products.FOOD_CORP_RUB.id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        },
        {
            'service_id': context.service.id,
            'last_transaction_id': 99,
            'amount': -coef * amount,
            'product_id': Products.FOOD_CORP_RUB.id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        }
    ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_dicts)
