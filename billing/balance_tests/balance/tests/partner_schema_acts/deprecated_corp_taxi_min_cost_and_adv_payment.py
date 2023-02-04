# -*- coding: utf-8 -*-

# ЛОГИКА ДОБИВОК ОТМЕНЕНА С ПЕРЕХОДОМ НА ЕДИНЫЙ ДОГОВОР
from datetime import datetime
from decimal import Decimal as D, ROUND_HALF_UP

import pytest
from dateutil.relativedelta import relativedelta

import balance.tests.promocode_new.promocode_commons as promo_steps
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as tsteps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import Export
from btestlib.constants import PromocodeClass
from btestlib.constants import TransactionType, Collateral, Products, Services, CorpTaxiOrderType, NdsNew, \
    CorpEdaOrderType, Currencies
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, \
    FOOD_CORP_CONTEXT, CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP
from btestlib.matchers import contains_dicts_with_entries

_, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, \
  PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT = utils.Date.previous_three_months_start_end_dates()

CURRENT_MONTH_START_DT, CURRENT_MONTH_END_DT = utils.Date.current_month_first_and_last_days()

FAKE_TPT_CONTRACT_ID = 123
FAKE_TPT_CLIENT_ID = 123
FAKE_TPT_PERSON_ID = 123

ADVANCE_SUM = D('10000')
SERVICE_MIN_COST = D('10000')
PERSONAL_ACC_SUM_1 = D('17300')
PERSONAL_ACC_SUM_2 = D('13700')
PAYMENT_SUM_TLOG = D('8700.2')
REFUND_SUM_TLOG = D('1400.3')
PAYMENT_SUM_TPT = D('8700.12')
REFUND_SUM_TPT = D('1400.8')

# yuelyasheva: чтобы добавить другой сервис, добавь коэффициенты по количеству продуктов и сами продукты,
# а также вынеси продукты в параметризацию
COEF_TLOG_1 = [D('1'), D('2'), D('3')]
COEF_TLOG_1_2 = [D('4'), D('2'), D('1')]
COEF_TLOG_2 = [D('4'), D('2'), D('5')]
ZERO_COEF_TLOG = [D('0'), D('0'), D('0')]
COEF_TPT = D('1.5')
ZERO_COEF_TPT = D('0')

SERVICE_CURRENCY_TO_MAIN_PRODUCT_MAP = {
    (Services.TAXI_CORP_CLIENTS, Currencies.RUB): Products.CORP_TAXI_CLIENTS_RUB,
    (Services.TAXI_CORP, Currencies.RUB): Products.CORP_TAXI_RUB,
    (Services.FOOD_CORP, Currencies.RUB): Products.FOOD_CORP_RUB,
    (Services.TAXI_CORP_CLIENTS, Currencies.BYN): Products.CORP_TAXI_CLIENTS_BYN,
}

SERVICE_CURRENCY_TO_MIN_COST_PRODUCT_MAP = {
    (Services.TAXI_CORP_CLIENTS, Currencies.RUB): Products.CORP_TAXI_CLIENTS_MIN_COST_RUB,
    (Services.TAXI_CORP, Currencies.RUB): Products.CORP_TAXI_MIN_COST,
    (Services.FOOD_CORP, Currencies.RUB): Products.FOOD_CORP_MIN_COST,
    (Services.TAXI_CORP_CLIENTS, Currencies.BYN): Products.CORP_TAXI_CLIENTS_MIN_COST_BYN,
}

SERVICE_CURRENCY_TO_PRODUCTS_MAP = {
    (Services.TAXI_CORP_CLIENTS, Currencies.RUB): [
        Products.CORP_TAXI_CLIENTS_RUB,
        Products.CORP_TAXI_CARGO_RUB,
        Products.CORP_TAXI_DELIVERY_RUB,
    ],
    (Services.TAXI_CORP_CLIENTS, Currencies.BYN): [
            Products.CORP_TAXI_CLIENTS_BYN,
            Products.CORP_TAXI_CARGO_BYN,
            Products.CORP_TAXI_DELIVERY_BYN,
        ],
    (Services.FOOD_CORP, Currencies.RUB): [
        Products.FOOD_CORP_RUB,
    ],
}

PRODUCT_TO_ORDER_TYPE_MAP = {
    Products.CORP_TAXI_CLIENTS_RUB: CorpTaxiOrderType.commission,
    Products.CORP_TAXI_CARGO_RUB: CorpTaxiOrderType.cargo_commission,
    Products.CORP_TAXI_DELIVERY_RUB: CorpTaxiOrderType.delivery_commission,
    Products.FOOD_CORP_RUB: CorpEdaOrderType.MAIN,
    Products.CORP_TAXI_CLIENTS_BYN: CorpTaxiOrderType.commission,
    Products.CORP_TAXI_CARGO_BYN: CorpTaxiOrderType.cargo_commission,
    Products.CORP_TAXI_DELIVERY_BYN: CorpTaxiOrderType.delivery_commission,
}


# no_min_cost_wo_service - галка, должны ли быть добивки, если по договору не было операций.
# выставлена галка - нет добивок, не выставлена - есть
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.parametrize('no_min_cost_wo_service', [
    True, False,
], ids=['WITH_MIN_COST', 'WO_MIN_COST'])
@pytest.mark.parametrize('is_postpay', [
    True, False,
], ids=['POSTPAY', 'PREPAY'])
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP,
    FOOD_CORP_CONTEXT,
], ids=lambda c: c.name)
def test_no_completions(no_min_cost_wo_service, is_postpay, context):
    params = {'start_dt': MONTH_BEFORE_PREV_START_DT, 'no_min_cost_wo_service': no_min_cost_wo_service}
    if is_postpay:
        params.update({'service_min_cost': SERVICE_MIN_COST})
    else:
        params.update({'advance_payment_sum': ADVANCE_SUM})

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                                                       additional_params=params)

    # yuelyasheva: от этого костыля не избавиться. Есть различия в генерации актов для 650 сервиса и 135+650,
    # в коде тоже костыль. Подробности знает @sfreest
    is_act_expected = (Services.TAXI_CORP.id in context.contract_services) or not no_min_cost_wo_service

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREVIOUS_MONTH_END_DT,
                                                                   manual_export=is_act_expected)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    total_sum = D('0') if no_min_cost_wo_service else max(SERVICE_MIN_COST, ADVANCE_SUM)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     total_sum,
                                                                                     total_act_sum=total_sum,
                                                                                     dt=PREVIOUS_MONTH_END_DT)
    expected_act_data = [] if no_min_cost_wo_service \
        else [steps.CommonData.create_expected_act_data(amount=total_sum, act_date=PREVIOUS_MONTH_END_DT)]

    # передаю все коэффициенты пустыми, чтобы ничего не считалось
    expected_order_data = create_expected_order_data(context, contract_id, act_sum_1=total_sum, act_sum_2=D('0'),
                                                     coef_tlog_1=ZERO_COEF_TLOG, coef_tlog_1_2=ZERO_COEF_TLOG,
                                                     coef_tlog_2=ZERO_COEF_TLOG, coef_tpt_1=ZERO_COEF_TPT,
                                                     coef_tpt_2=ZERO_COEF_TPT)

    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из заказа с шаблоном')


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('personal_acc_payment, is_postpay', [
    (False, False),
    (True, False),
    (False, True),
], ids=[
    'No payments prepay',
    'Payments prepay',
    'No payments postpay'
])
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP,
    FOOD_CORP_CONTEXT,
], ids=lambda c: c.name)
def test_completions_less_than_min_payment(personal_acc_payment, is_postpay, context):
    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')
    if migration_dt:
        # 2 месяца до даты миграции
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            utils.Date.previous_two_months_dates(migration_dt)
        # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
        posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
        oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
            utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    else:
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = [None] * 4

    params = {'start_dt': month_migration_minus2_start_dt}


    ### СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты

    completions_1, completions_2, min_advance =  \
        calculate_min_cost_or_advance_sum(context, is_add_sum_expected=True)

    if is_postpay:
        params.update({'service_min_cost': min_advance})
    else:
        params.update({'advance_payment_sum': min_advance})
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                                                       additional_params=params)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)

    # вносим деньги на ЛС, если нужно, создаем открутки и закрываем первый месяц
    if personal_acc_payment:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM_1, payment_dt=month_migration_minus2_start_dt)
    create_completions(context, month_migration_minus2_start_dt, client_id, COEF_TLOG_1, coef_tpt=COEF_TPT)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus2_start_dt + relativedelta(days=2, minutes=5))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt)

    # добавляем откруток в первом месяце во имя нарастающего итога
    create_completions(context, month_migration_minus2_start_dt, client_id, COEF_TLOG_1_2)

    if personal_acc_payment:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM_2, payment_dt=month_migration_minus1_start_dt)
    create_completions(context, month_migration_minus1_start_dt, client_id, COEF_TLOG_2, coef_tpt=COEF_TPT)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus1_start_dt + relativedelta(days=2, minutes=5))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus1_start_dt)

    expected_act_data = create_expected_act_data(min_advance, min_advance,
                                                 month_migration_minus2_end_dt, month_migration_minus1_end_dt)
    consume_sum = utils.dround(max(PERSONAL_ACC_SUM_1 + PERSONAL_ACC_SUM_2, 2 * min_advance), 2)
    act_sum = utils.dround(2 * min_advance, 2)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     consume_sum,
                                                                                     total_act_sum=act_sum,
                                                                                     dt=month_migration_minus2_end_dt)
    expected_order_data = create_expected_order_data(context, contract_id, act_sum_1=min_advance, act_sum_2=min_advance,
                                                     completions_1=completions_1, completions_2=completions_2)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акт с шаблоном')
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из заказов с шаблоном')


    ### НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты
    # суммы откруток по продуктам сделаны такими же, как в старых открутках (только в тлоге суммы без НДС, а у ОЕБС с НДС)
    # поэтому многие расчеты взяты из старых тестов, суммы домножены на 2.
    if migration_dt:
        # вносим деньги на ЛС, если нужно, создаем открутки и закрываем первый месяц
        if personal_acc_payment:
            steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM_1, payment_dt=month_minus2_start_dt)
        create_oebs_completions(context, month_minus2_start_dt, contract_id, client_id, COEF_TLOG_1, coef_tpt=COEF_TPT)
        steps.TaxiSteps.process_taxi(contract_id, month_minus2_start_dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                       month_minus2_end_dt)

        # добавляем откруток в первом месяце во имя нарастающего итога
        # но ОЕБС так делать не должен
        create_oebs_completions(context, month_minus2_start_dt, contract_id, client_id, COEF_TLOG_1_2)

        if personal_acc_payment:
            steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM_2, payment_dt=month_minus1_start_dt)
        create_oebs_completions(context, month_minus1_start_dt, contract_id, client_id, COEF_TLOG_2, coef_tpt=COEF_TPT)
        steps.TaxiSteps.process_taxi(contract_id, month_minus1_start_dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus1_end_dt)

        expected_act_data_2 = create_expected_act_data(min_advance, min_advance, month_minus2_end_dt, month_minus1_end_dt)
        expected_act_data.extend(expected_act_data_2)
        consume_sum = utils.dround(max(PERSONAL_ACC_SUM_1 + PERSONAL_ACC_SUM_2, 2 * min_advance) * 2, 2)
        act_sum = utils.dround(4 * min_advance, 2)
        expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                         contract_id, person_id,
                                                                                         consume_sum,
                                                                                         total_act_sum=act_sum,
                                                                                         dt=month_migration_minus2_end_dt)
        expected_order_data = create_expected_order_data(context, contract_id, act_sum_1=min_advance, act_sum_2=min_advance,
                                                         completions_1=completions_1, completions_2=completions_2,
                                                         repetition_factor=2)

        act_data = steps.ActsSteps.get_act_data_by_client(client_id)
        invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
        order_data = steps.OrderSteps.get_order_data_by_client(client_id)

        utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                         'Сравниваем данные из акт с шаблоном')
        utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                         'Сравниваем данные из счета с шаблоном')
        utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                         'Сравниваем данные из заказов с шаблоном')


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize('personal_acc_payment, is_postpay', [
    (False, False),
    (True, False),
    (False, True),
], ids=[
    'No payments prepay',
    'Payments prepay',
    'No payments postpay'
])
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP,
    FOOD_CORP_CONTEXT
], ids=lambda c: c.name)
def test_completions_greater_than_min_payment(personal_acc_payment, is_postpay, context):
    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')
    if migration_dt:
        # 2 месяца до даты миграции
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            utils.Date.previous_two_months_dates(migration_dt)
        # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
        posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
        oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
            utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    else:
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = [None] * 4

    params = {'start_dt': month_migration_minus2_start_dt}

    ### СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты

    completions_1, completions_2, min_advance =  \
        calculate_min_cost_or_advance_sum(context, is_add_sum_expected=False)

    if is_postpay:
        params.update({'service_min_cost': min_advance})
    else:
        params.update({'advance_payment_sum': min_advance})
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                                                       additional_params=params)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)

    # вносим деньги на ЛС, если нужно, создаем открутки и закрываем первый месяц
    if personal_acc_payment:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM_1, payment_dt=month_migration_minus2_start_dt)
    create_completions(context, month_migration_minus2_start_dt, client_id, COEF_TLOG_1)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus2_start_dt + relativedelta(days=2, minutes=5))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt)

    # добавляем откруток в первом месяце во имя нарастающего итога
    create_completions(context, month_migration_minus2_start_dt, client_id, COEF_TLOG_1_2, coef_tpt=COEF_TPT)

    if personal_acc_payment:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM_2, payment_dt=month_migration_minus1_start_dt)
    create_completions(context, month_migration_minus1_start_dt, client_id, COEF_TLOG_2, coef_tpt=COEF_TPT)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus1_start_dt + relativedelta(days=2, minutes=5))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus1_end_dt)

    expected_act_data = create_expected_act_data(completions_1, completions_2,
                                                 month_migration_minus2_end_dt, month_migration_minus1_end_dt)
    consume_sum = max(PERSONAL_ACC_SUM_1 + PERSONAL_ACC_SUM_2, completions_1 + completions_2)
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     consume_sum,
                                                                                     total_act_sum=completions_1 +
                                                                                                   completions_2,
                                                                                     dt=month_migration_minus2_end_dt)
    expected_order_data = create_expected_order_data(context, contract_id, act_sum_1=min_advance, act_sum_2=min_advance,
                                                     completions_1=completions_1, completions_2=completions_2)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акт с шаблоном')
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из заказов с шаблоном')

    ### НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты
    # суммы откруток по продуктам сделаны такими же, как в старых открутках (только в тлоге суммы без НДС, а у ОЕБС с НДС)
    # поэтому многие расчеты взяты из старых тестов, суммы домножены на 2.
    if migration_dt:
        # вносим деньги на ЛС, если нужно, создаем открутки и закрываем первый месяц
        if personal_acc_payment:
            steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM_1, payment_dt=month_minus2_start_dt)
        create_oebs_completions(context, month_minus2_start_dt, contract_id, client_id, COEF_TLOG_1)
        steps.TaxiSteps.process_taxi(contract_id, month_minus2_start_dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                       month_minus2_end_dt)

        # добавляем откруток в первом месяце во имя нарастающего итога
        # но ОЕБС так делать не должен
        create_oebs_completions(context, month_minus2_start_dt, contract_id, client_id, COEF_TLOG_1_2, coef_tpt=COEF_TPT)


        if personal_acc_payment:
            steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM_2, payment_dt=month_minus1_start_dt)
        create_oebs_completions(context, month_minus1_start_dt, contract_id, client_id, COEF_TLOG_2, coef_tpt=COEF_TPT)
        steps.TaxiSteps.process_taxi(contract_id, month_minus1_start_dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus1_end_dt)

        expected_act_data_2 = create_expected_act_data(completions_1, completions_2, month_minus2_end_dt, month_minus1_end_dt)
        expected_act_data.extend(expected_act_data_2)

        consume_sum = utils.dround(max(PERSONAL_ACC_SUM_1 + PERSONAL_ACC_SUM_2, completions_1 + completions_2) * 2, 2)
        act_sum = utils.dround((completions_1 + completions_2) * 2, 2)
        expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                         contract_id, person_id,
                                                                                         consume_sum,
                                                                                         total_act_sum=act_sum,
                                                                                         dt=month_migration_minus2_end_dt)
        expected_order_data = create_expected_order_data(context, contract_id, act_sum_1=min_advance,
                                                         act_sum_2=min_advance,
                                                         completions_1=completions_1, completions_2=completions_2,
                                                         repetition_factor=2)

        act_data = steps.ActsSteps.get_act_data_by_client(client_id)
        invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
        order_data = steps.OrderSteps.get_order_data_by_client(client_id)

        utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                         'Сравниваем данные из акт с шаблоном')
        utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                         'Сравниваем данные из счета с шаблоном')
        utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                         'Сравниваем данные из заказов с шаблоном')


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.parametrize('coef_tlog_1, coef_tlog_2, coef_tpt_1, coef_tpt_2', [
    (ZERO_COEF_TLOG, COEF_TLOG_2, ZERO_COEF_TPT, COEF_TPT),
    (COEF_TLOG_1, ZERO_COEF_TLOG, COEF_TPT, ZERO_COEF_TPT),
], ids=[
    'No payments in first month',
    'No payment in second month',
])
@pytest.mark.parametrize('is_postpay', [
    True, False,
], ids=['POSTPAY', 'PREPAY'])
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP,
    FOOD_CORP_CONTEXT
], ids=lambda c: c.name)
def test_no_completions_in_one_month(coef_tlog_1, coef_tlog_2, coef_tpt_1, coef_tpt_2, is_postpay, context):
    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')
    if migration_dt:
        # 2 месяца до даты миграции
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            utils.Date.previous_two_months_dates(migration_dt)
        # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
        posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
        oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
            utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    else:
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = [None] * 4

    params = {'start_dt': month_migration_minus2_start_dt}

    ### СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты

    completions_1, completions_2, min_advance =  \
        calculate_min_cost_or_advance_sum(context, is_add_sum_expected=True, coef_tlog_1=coef_tlog_1, coef_tpt_1=coef_tpt_1,
                                          coef_tlog_1_2=coef_tlog_2, coef_tpt_2=coef_tpt_2, coef_tlog_2=coef_tlog_2)

    if is_postpay:
        params.update({'service_min_cost': min_advance})
    else:
        params.update({'advance_payment_sum': min_advance})
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                                                       additional_params=params)

    # вносим деньги на ЛС, если нужно, создаем открутки и закрываем первый месяц
    create_completions(context, month_migration_minus2_start_dt, client_id, coef_tlog=coef_tlog_1, coef_tpt=coef_tpt_1)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus2_start_dt + relativedelta(days=2, minutes=5))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt)

    # добавляем откруток в первом месяце во имя нарастающего итога
    create_completions(context, month_migration_minus2_start_dt, client_id, coef_tlog=coef_tlog_2, coef_tpt=coef_tpt_2)

    create_completions(context, month_migration_minus1_start_dt, client_id, coef_tlog=coef_tlog_2, coef_tpt=coef_tpt_2)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus1_start_dt + relativedelta(days=2, minutes=5))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus1_end_dt)

    expected_act_data = create_expected_act_data(min_advance, min_advance,
                                                 month_migration_minus2_end_dt, month_migration_minus1_end_dt)
    consume_sum = D('2') * min_advance
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     consume_sum,
                                                                                     total_act_sum=D('2') * min_advance,
                                                                                     dt=month_migration_minus2_end_dt)

    expected_order_data = create_expected_order_data(context, contract_id, act_sum_1=min_advance, act_sum_2=min_advance,
                                                     completions_1=completions_1, completions_2=completions_2,
                                                     coef_tlog_1=coef_tlog_1, coef_tlog_1_2=coef_tlog_2,
                                                     coef_tlog_2=coef_tlog_2, coef_tpt_1=coef_tpt_1,
                                                     coef_tpt_2=coef_tpt_2)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акт с шаблоном')
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из заказов с шаблоном')

    ### НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты
    # суммы откруток по продуктам сделаны такими же, как в старых открутках (только в тлоге суммы без НДС, а у ОЕБС с НДС)
    # поэтому многие расчеты взяты из старых тестов, суммы домножены на 2.
    if migration_dt:
        # вносим деньги на ЛС, если нужно, создаем открутки и закрываем первый месяц
        create_oebs_completions(context, month_minus2_start_dt, contract_id, client_id, coef_tlog=coef_tlog_1,
                           coef_tpt=coef_tpt_1)
        steps.TaxiSteps.process_taxi(contract_id, month_minus2_start_dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_end_dt)

        # добавляем откруток в первом месяце во имя нарастающего итога
        create_oebs_completions(context, month_minus2_start_dt, contract_id, client_id, coef_tlog=coef_tlog_2,
                                coef_tpt=coef_tpt_2)

        create_oebs_completions(context, month_minus1_start_dt, contract_id, client_id, coef_tlog=coef_tlog_2,
                                coef_tpt=coef_tpt_2)
        steps.TaxiSteps.process_taxi(contract_id, month_minus1_start_dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                       month_minus1_end_dt)

        expected_act_data_2 = create_expected_act_data(min_advance, min_advance,
                                                       month_minus2_end_dt, month_minus1_end_dt)
        expected_act_data.extend(expected_act_data_2)
        consume_sum = D('4') * min_advance
        expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                         contract_id, person_id,
                                                                                         consume_sum,
                                                                                         total_act_sum=D('4') * min_advance,
                                                                                         dt=month_minus2_end_dt)

        expected_order_data = create_expected_order_data(context, contract_id, act_sum_1=min_advance,
                                                         act_sum_2=min_advance,
                                                         completions_1=completions_1, completions_2=completions_2,
                                                         coef_tlog_1=coef_tlog_1, coef_tlog_1_2=coef_tlog_2,
                                                         coef_tlog_2=coef_tlog_2, coef_tpt_1=coef_tpt_1,
                                                         coef_tpt_2=coef_tpt_2, repetition_factor=2)

        act_data = steps.ActsSteps.get_act_data_by_client(client_id)
        invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
        order_data = steps.OrderSteps.get_order_data_by_client(client_id)

        utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                         'Сравниваем данные из акт с шаблоном')
        utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                         'Сравниваем данные из счета с шаблоном')
        utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                         'Сравниваем данные из заказов с шаблоном')


### ЛОГИКА ДОБИВОК ОТМЕНЯЕТСЯ С ПЕРЕХОДОМ НА ОТКРУТКИ ОЕБС
# @reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
# @pytest.mark.parametrize('no_min_cost_wo_service', [
#     True,
#     pytest.mark.skip(reason='https://st.yandex-team.ru/BALANCE-30766')(False),
# ], ids=['WITH_MIN_COST', 'WO_MIN_COST'])
# @pytest.mark.parametrize('is_postpay', [
#     True, False,
# ], ids=['POSTPAY', 'PREPAY'])
# @pytest.mark.parametrize('context', [
#     CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
#     CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
#     CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP,
#     FOOD_CORP_CONTEXT,
# ], ids=lambda c: c.name)
# def test_refunds_greater_than_payments(no_min_cost_wo_service, is_postpay, context):
#     params = {'start_dt': MONTH_BEFORE_PREV_START_DT, 'no_min_cost_wo_service': no_min_cost_wo_service}
#     if is_postpay:
#         params.update({'service_min_cost': SERVICE_MIN_COST})
#     else:
#         params.update({'advance_payment_sum': ADVANCE_SUM})
#
#     client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
#                                                                                        additional_params=params)
#
#     # делаем рефанды больше платежей
#     transaction_count = D(get_transaction_count(context))
#     dt = PREVIOUS_MONTH_START_DT + relativedelta(days=5)
#
#     if context.service == Services.TAXI_CORP_CLIENTS:
#         steps.SimpleApi.create_fake_tpt_data(context, FAKE_TPT_CLIENT_ID, FAKE_TPT_PERSON_ID,
#                                              FAKE_TPT_CONTRACT_ID, dt,
#                                              [{
#                                                   'client_amount': PAYMENT_SUM_TPT,
#                                                   'client_id': client_id,
#                                                   'transaction_type': TransactionType.PAYMENT
#                                               },
#                                               {
#                                                   'client_amount': 3 * PAYMENT_SUM_TPT,
#                                                   'client_id': client_id,
#                                                   'transaction_type': TransactionType.REFUND
#                                               }
#                                               ])
#
#     order_dicts_tlog = []
#     for idx, product in enumerate(SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)]):
#         order_dicts_tlog += [
#             {
#                 'service_id': context.service.id,
#                 'amount': -PAYMENT_SUM_TLOG / transaction_count / context.nds.koef_on_dt(dt),
#                 'type': PRODUCT_TO_ORDER_TYPE_MAP[product],
#                 'dt': dt,
#                 'transaction_dt': dt,
#                 'currency': context.currency.iso_code,
#             },
#             {
#                 'service_id': context.service.id,
#                 'amount': REFUND_SUM_TLOG / transaction_count / context.nds.koef_on_dt(dt),
#                 'type': PRODUCT_TO_ORDER_TYPE_MAP[product],
#                 'dt': dt,
#                 'transaction_dt': dt,
#                 'currency': context.currency.iso_code,
#             },
#         ]
#     tsteps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)
#
#     steps.TaxiSteps.process_taxi(contract_id, dt + relativedelta(days=2, minutes=5))
#     steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREVIOUS_MONTH_END_DT)
#
#     act_data = steps.ActsSteps.get_act_data_by_client(client_id)
#     invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
#     order_data = steps.OrderSteps.get_order_data_by_client(client_id)
#
#     total_sum = D('0') if no_min_cost_wo_service else max(SERVICE_MIN_COST, ADVANCE_SUM)
#     expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
#                                                                                      contract_id, person_id,
#                                                                                      D('0'),
#                                                                                      total_act_sum=total_sum,
#                                                                                      dt=PREVIOUS_MONTH_END_DT)
#     expected_act_data = [] if no_min_cost_wo_service \
#         else [steps.CommonData.create_expected_act_data(amount=total_sum, act_date=PREVIOUS_MONTH_END_DT)]
#
#     # это костыль из-за бага - в заказах completion_qty становится отрицательным,
#     # а остальные параметры остаются нулевыми
#     expected_order_data = [
#         steps.CommonData.create_expected_order_data(context.service.id,
#                                                     SERVICE_CURRENCY_TO_MIN_COST_PRODUCT_MAP[(context.service, context.currency)].id,
#                                                     contract_id,
#                                                     consume_sum=D('0')),
#     ]
#     for product in SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)]:
#         cq = REFUND_SUM_TLOG / transaction_count - PAYMENT_SUM_TLOG / transaction_count
#         expected_order_data.append(
#             steps.CommonData.create_expected_order_data(context.service.id, product.id,
#                                                         contract_id,
#                                                         consume_sum=D('0'),
#                                                         completion_qty=cq,
#                                                         consume_qty=D('0')),
#         )
#
#     if Services.TAXI_CORP.id in context.contract_services:
#         expected_order_data += [
#             steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_RUB.id, contract_id,
#                                                         consume_sum=D('0'),
#                                                         completion_qty=-2*PAYMENT_SUM_TPT,
#                                                         consume_qty=D('0')),
#             steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_MIN_COST.id,
#                                                         contract_id, D('0'))
#         ]
#
#     utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
#                      'Сравниваем данные из счета с шаблоном')
#     utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
#                      'Сравниваем данные из акта с шаблоном')
#     utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
#                      'Сравниваем данные из заказа с шаблоном')


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.parametrize('advance_sum_coef_1, advance_sum_coef_2',
                         [
                             (D('0'), D('1')),
                             (D('1'), D('0')),
                             (D('2'), D('1')),

                         ],
                         ids=[
                             'No advance payment in first month',
                             'No advance payment in second month',
                             'Different advance payments != 0',
                         ]
                         )
@pytest.mark.parametrize('is_postpay', [
    True, False,
], ids=['POSTPAY', 'PREPAY'])
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    FOOD_CORP_CONTEXT,
], ids=lambda c: c.name)
def test_act_generation_diff_min_sum(advance_sum_coef_1, advance_sum_coef_2, is_postpay, context):
    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')
    if migration_dt:
        # 2 месяца до даты миграции
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            utils.Date.previous_two_months_dates(migration_dt)
        # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
        posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
        oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
            utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    else:
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = [None] * 4

    params = {'start_dt': month_migration_minus2_start_dt}

    ### СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты

    completions_1, completions_2, min_advance = \
        calculate_min_cost_or_advance_sum(context, is_add_sum_expected=True)
    if is_postpay:
        params.update({'service_min_cost': min_advance * advance_sum_coef_1})
    else:
        params.update({'advance_payment_sum': min_advance * advance_sum_coef_1})
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                                                       additional_params=params)

    # вносим деньги на ЛС, если нужно, создаем открутки и закрываем первый месяц
    create_completions(context, month_migration_minus2_start_dt, client_id, COEF_TLOG_1)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus2_start_dt + relativedelta(days=2, minutes=5))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt)

    # создаем допник на изменение минималки
    collateral_params = {'CONTRACT2_ID': contract_id, 'DT': month_migration_minus1_start_dt.isoformat(),
                         'IS_SIGNED': month_migration_minus1_start_dt.isoformat()}

    if is_postpay:
        collateral_type = Collateral.CHANGE_MIN_COST
        collateral_params.update({'SERVICE_MIN_COST': min_advance * advance_sum_coef_2})
    else:
        collateral_type = Collateral.CHANGE_ADV_PAYMENT
        collateral_params.update({'ADVANCE_PAYMENT_SUM': min_advance * advance_sum_coef_2})

    steps.ContractSteps.create_collateral(collateral_type, collateral_params)

    # добавляем откруток в первом месяце во имя нарастающего итога
    create_completions(context, month_migration_minus1_start_dt, client_id, COEF_TLOG_1_2, coef_tpt=COEF_TPT)
    create_completions(context, month_migration_minus1_start_dt, client_id, COEF_TLOG_2, coef_tpt=COEF_TPT)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus1_start_dt + relativedelta(days=2, minutes=5))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus1_end_dt)

    act_sum_1 = max(completions_1, min_advance * advance_sum_coef_1)
    act_sum_2 = max(completions_2, min_advance * advance_sum_coef_2)

    expected_act_data = create_expected_act_data(act_sum_1, act_sum_2,
                                                 month_migration_minus2_end_dt, month_migration_minus1_end_dt)
    consume_sum = act_sum_1 + act_sum_2
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     consume_sum,
                                                                                     total_act_sum=consume_sum,
                                                                                     dt=month_migration_minus2_start_dt)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акт с шаблоном')
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     'Сравниваем данные из счета с шаблоном')

    ### НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты
    # суммы откруток по продуктам сделаны такими же, как в старых открутках (только в тлоге суммы без НДС, а у ОЕБС с НДС)
    # поэтому многие расчеты взяты из старых тестов, суммы домножены на 2.
    if migration_dt:
        # создаем допник на изменение минималки
        collateral_params = {'CONTRACT2_ID': contract_id, 'DT': month_minus2_start_dt.isoformat(),
                             'IS_SIGNED': month_minus2_start_dt.isoformat()}

        if is_postpay:
            collateral_type = Collateral.CHANGE_MIN_COST
            collateral_params.update({'SERVICE_MIN_COST': min_advance * advance_sum_coef_1})
        else:
            collateral_type = Collateral.CHANGE_ADV_PAYMENT
            collateral_params.update({'ADVANCE_PAYMENT_SUM': min_advance * advance_sum_coef_1})

        steps.ContractSteps.create_collateral(collateral_type, collateral_params)

        # вносим деньги на ЛС, если нужно, создаем открутки и закрываем первый месяц
        create_oebs_completions(context, month_minus2_start_dt, contract_id, client_id, COEF_TLOG_1)
        steps.TaxiSteps.process_taxi(contract_id, month_minus2_start_dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                       month_minus2_end_dt)

        # создаем допник на изменение минималки
        collateral_params = {'CONTRACT2_ID': contract_id, 'DT': month_minus1_start_dt.isoformat(),
                             'IS_SIGNED': month_minus1_start_dt.isoformat()}

        if is_postpay:
            collateral_type = Collateral.CHANGE_MIN_COST
            collateral_params.update({'SERVICE_MIN_COST': min_advance * advance_sum_coef_2})
        else:
            collateral_type = Collateral.CHANGE_ADV_PAYMENT
            collateral_params.update({'ADVANCE_PAYMENT_SUM': min_advance * advance_sum_coef_2})

        steps.ContractSteps.create_collateral(collateral_type, collateral_params)

        # добавляем откруток в первом месяце во имя нарастающего итога
        create_oebs_completions(context, month_minus1_start_dt, contract_id, client_id, COEF_TLOG_1_2, coef_tpt=COEF_TPT)
        create_oebs_completions(context, month_minus1_start_dt, contract_id, client_id, COEF_TLOG_2, coef_tpt=COEF_TPT)
        steps.TaxiSteps.process_taxi(contract_id, month_minus1_start_dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                       month_minus1_end_dt)

        act_sum_1 = max(completions_1, min_advance * advance_sum_coef_1)
        act_sum_2 = max(completions_2, min_advance * advance_sum_coef_2)

        expected_act_data_2 = create_expected_act_data(act_sum_1, act_sum_2,
                                                       month_minus2_end_dt, month_minus1_end_dt)
        expected_act_data.extend(expected_act_data_2)

        consume_sum = (act_sum_1 + act_sum_2) * 2
        expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                         contract_id, person_id,
                                                                                         consume_sum,
                                                                                         total_act_sum=consume_sum,
                                                                                         dt=month_minus2_start_dt)

        act_data = steps.ActsSteps.get_act_data_by_client(client_id)
        invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

        utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                         'Сравниваем данные из акт с шаблоном')
        utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                         'Сравниваем данные из счета с шаблоном')


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.parametrize('no_min_cost_wo_service_1, no_min_cost_wo_service_2', [
    (True, False),
    (False, True)
], ids=['SECOND_WO_MIN_COST', 'FIRST_WO_MIN_COST'])
@pytest.mark.parametrize('is_postpay', [
    True, False,
], ids=['POSTPAY', 'PREPAY'])
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    FOOD_CORP_CONTEXT,
], ids=lambda c: c.name)
def test_diff_no_min_cost_wo_service(no_min_cost_wo_service_1, no_min_cost_wo_service_2, is_postpay, context):
    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')
    if migration_dt:
        # 2 месяца до даты миграции
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            utils.Date.previous_two_months_dates(migration_dt)
        # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
        posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
        oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
            utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    else:
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = [None] * 4

    ### СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты
    params = {'start_dt': month_migration_minus2_start_dt, 'no_min_cost_wo_service': no_min_cost_wo_service_1}

    completions_1, completions_2, min_advance = \
        calculate_min_cost_or_advance_sum(context, is_add_sum_expected=True)
    if is_postpay:
        params.update({'service_min_cost': min_advance})
    else:
        params.update({'advance_payment_sum': min_advance})
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                                                       additional_params=params)

    # закрываем первый месяц без откруток
    is_act_expected = (context == CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED) or not no_min_cost_wo_service_1
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt,
                                                                   manual_export=is_act_expected)

    # создаем допник на изменение минималки
    collateral_params = {'CONTRACT2_ID': contract_id, 'DT': month_migration_minus1_start_dt.isoformat(),
                         'IS_SIGNED': month_migration_minus1_start_dt.isoformat(),
                         'NO_MIN_COST_WO_SERVICE': int(no_min_cost_wo_service_2)}
    if is_postpay:
        collateral_type = Collateral.CHANGE_MIN_COST
        collateral_params.update({'SERVICE_MIN_COST': min_advance})
    else:
        collateral_type = Collateral.CHANGE_ADV_PAYMENT
        collateral_params.update({'ADVANCE_PAYMENT_SUM': min_advance})

    steps.ContractSteps.create_collateral(collateral_type, collateral_params)

    # закрываем второй месяц
    is_act_expected = (context == CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED) or not no_min_cost_wo_service_2
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus1_end_dt,
                                                                   manual_export=is_act_expected)

    act_sum_1 = D('0') if no_min_cost_wo_service_1 else min_advance
    act_sum_2 = D('0') if no_min_cost_wo_service_2 else min_advance

    expected_act_data = create_expected_act_data(act_sum_1, act_sum_2,
                                                 month_migration_minus2_start_dt, month_migration_minus1_start_dt)
    consume_sum = act_sum_1 + act_sum_2
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     consume_sum,
                                                                                     total_act_sum=consume_sum,
                                                                                     dt=month_migration_minus2_start_dt)

    expected_order_data = create_expected_order_data(context, contract_id, act_sum_1=act_sum_1, act_sum_2=act_sum_2,
                                                     coef_tlog_1=ZERO_COEF_TLOG, coef_tlog_1_2=ZERO_COEF_TLOG,
                                                     coef_tlog_2=ZERO_COEF_TLOG, coef_tpt_1=ZERO_COEF_TPT,
                                                     coef_tpt_2=ZERO_COEF_TPT)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акт с шаблоном')
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из заказов с шаблоном')

    ### НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты
    # суммы откруток по продуктам сделаны такими же, как в старых открутках (только в тлоге суммы без НДС, а у ОЕБС с НДС)
    # поэтому многие расчеты взяты из старых тестов, суммы домножены на 2.
    if migration_dt:
        # создаем допник на изменение минималки
        collateral_params = {'CONTRACT2_ID': contract_id, 'DT': month_minus2_start_dt.isoformat(),
                             'IS_SIGNED': month_minus2_start_dt.isoformat(),
                             'NO_MIN_COST_WO_SERVICE': int(no_min_cost_wo_service_1)}
        if is_postpay:
            collateral_type = Collateral.CHANGE_MIN_COST
            collateral_params.update({'SERVICE_MIN_COST': min_advance})
        else:
            collateral_type = Collateral.CHANGE_ADV_PAYMENT
            collateral_params.update({'ADVANCE_PAYMENT_SUM': min_advance})

        steps.ContractSteps.create_collateral(collateral_type, collateral_params)

        # закрываем первый месяц без откруток
        is_act_expected = (context == CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED) or not no_min_cost_wo_service_1
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                       month_minus2_end_dt,
                                                                       manual_export=is_act_expected)

        # создаем допник на изменение минималки
        collateral_params = {'CONTRACT2_ID': contract_id, 'DT': month_minus1_start_dt.isoformat(),
                             'IS_SIGNED': month_minus1_start_dt.isoformat(),
                             'NO_MIN_COST_WO_SERVICE': int(no_min_cost_wo_service_2)}
        if is_postpay:
            collateral_type = Collateral.CHANGE_MIN_COST
            collateral_params.update({'SERVICE_MIN_COST': min_advance})
        else:
            collateral_type = Collateral.CHANGE_ADV_PAYMENT
            collateral_params.update({'ADVANCE_PAYMENT_SUM': min_advance})

        steps.ContractSteps.create_collateral(collateral_type, collateral_params)

        # закрываем второй месяц
        is_act_expected = (context == CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED) or not no_min_cost_wo_service_2
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                       month_minus1_end_dt,
                                                                       manual_export=is_act_expected)

        act_sum_1 = D('0') if no_min_cost_wo_service_1 else min_advance
        act_sum_2 = D('0') if no_min_cost_wo_service_2 else min_advance

        expected_act_data_2 = create_expected_act_data(act_sum_1, act_sum_2,
                                                       month_minus2_start_dt, month_minus1_start_dt)
        expected_act_data.extend(expected_act_data_2)
        consume_sum = (act_sum_1 + act_sum_2) * 2
        expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                         contract_id, person_id,
                                                                                         consume_sum,
                                                                                         total_act_sum=consume_sum,
                                                                                         dt=month_migration_minus2_start_dt)

        expected_order_data = create_expected_order_data(context, contract_id, act_sum_1=act_sum_1, act_sum_2=act_sum_2,
                                                         coef_tlog_1=ZERO_COEF_TLOG, coef_tlog_1_2=ZERO_COEF_TLOG,
                                                         coef_tlog_2=ZERO_COEF_TLOG, coef_tpt_1=ZERO_COEF_TPT,
                                                         coef_tpt_2=ZERO_COEF_TPT, repetition_factor=2)

        act_data = steps.ActsSteps.get_act_data_by_client(client_id)
        invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
        order_data = steps.OrderSteps.get_order_data_by_client(client_id)

        utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                         'Сравниваем данные из акт с шаблоном')
        utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data]),
                         'Сравниваем данные из счета с шаблоном')
        utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                         'Сравниваем данные из заказов с шаблоном')


ADVANCE_SUM_PROMO = D('10000')
PERSONAL_ACC_SUM_PROMO = D('10000')
PAYMENT_SUM_PROMO = D('1200')
PROMO_SUM = D('2000')


# yuelyasheva: тут слишком много черной магии. оставляю тест, как был в предыдущей версии,
# сделав создание откруток без метода (теперь он кишками наружу)
# проверка генерации актов с добивками и промокодами у предоплатных счетов
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT, Features.PROMOCODE)
@pytest.mark.parametrize('adjust_quantity, apply_on_create',
                         [
                             (True, True),
                             (False, True),
                         ],
                         ids=[
                             'Quantity promocode',
                             'Sum promocode'
                         ]
                         )
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    FOOD_CORP_CONTEXT,
], ids=lambda c: c.name)
def test_promocode(adjust_quantity, apply_on_create, context):
    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')
    if migration_dt:
        # 2 месяца до даты миграции
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            utils.Date.previous_two_months_dates(migration_dt)
        # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
        posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
        oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
            utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    else:
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = [None] * 4

    ### СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты
    transaction_count = D(get_transaction_count(context))
    params = {'start_dt': month_migration_minus1_start_dt, 'advance_payment_sum': ADVANCE_SUM_PROMO}
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_postpay=False,
        additional_params=params)

    # обрабатываю договоро и вытаскиваю service_order_id по номеру договора (потребуется при создании реквеста)
    steps.TaxiSteps.process_taxi(contract_id)
    service_id = context.service.id
    service_order_id = steps.OrderSteps.get_order_id_by_contract(contract_id, service_id)

    # создаю промокод, привязанный к клиенту
    create_new_promo(client_id, context, adjust_quantity=adjust_quantity, apply_on_create=apply_on_create)

    # создаю реквест и счет-квитанцию
    request_id = create_request_taxi(client_id, service_order_id, PERSONAL_ACC_SUM_PROMO,
                                     month_migration_minus1_start_dt, service_id)
    invoice_id_charge_note, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=False,
                                                             contract_id=contract_id)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    payment_id = steps.InvoiceSteps.get_payment_id_for_charge_note(invoice_id_charge_note)

    # рассчитываю скидку и сумму к оплате
    discount = calculate_discount_pct_from_fixed_sum(PROMO_SUM, context.nds.pct_on_dt(month_migration_minus1_start_dt),
                                                     ADVANCE_SUM_PROMO, adjust_quantity=adjust_quantity)

    sum_to_pay = ADVANCE_SUM_PROMO * (D('1') - discount * D('0.01')) if not adjust_quantity else PERSONAL_ACC_SUM_PROMO

    # оплачиваю счет на сумму с учетом скидки
    steps.TaxiSteps.create_cash_payment_fact(external_invoice_id, sum_to_pay, month_migration_minus1_start_dt, 'INSERT',
                                             payment_id)
    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

    # делаю открутки
    dt = month_migration_minus1_start_dt + relativedelta(days=5)

    if context.service == Services.TAXI_CORP_CLIENTS:
        steps.SimpleApi.create_fake_tpt_data(context, FAKE_TPT_CLIENT_ID, FAKE_TPT_PERSON_ID,
                                             FAKE_TPT_CONTRACT_ID, dt,
                                             [{
                                                  'client_amount': PAYMENT_SUM_PROMO / transaction_count,
                                                  'client_id': client_id,
                                                  'transaction_type': TransactionType.PAYMENT
                                              }])

    order_dicts_tlog = []
    for idx, product in enumerate(SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)]):
        order_dicts_tlog += [
            {
                'service_id': context.service.id,
                'amount': PAYMENT_SUM_PROMO / transaction_count / context.nds.koef_on_dt(dt),
                'type': PRODUCT_TO_ORDER_TYPE_MAP[product],
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
            },
        ]
    tsteps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)

    steps.TaxiSteps.process_taxi(contract_id, dt + relativedelta(days=2, minutes=5))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus1_end_dt)

    # отдельно считаем сумму откруток и добивки со скидкой для корректного округления
    completions_sum = (PAYMENT_SUM_PROMO * (D('1') - discount * D('0.01'))).quantize(D("1.00"), ROUND_HALF_UP)
    added_sum = ((ADVANCE_SUM_PROMO - PAYMENT_SUM_PROMO) * (D('1') - discount * D('0.01'))).quantize(D("1.00"),
                                                                                               ROUND_HALF_UP)

    consume_sum = sum_to_pay if not adjust_quantity else PERSONAL_ACC_SUM_PROMO
    act_sum = completions_sum + added_sum

    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     consume_sum,
                                                                                     total_act_sum=act_sum,
                                                                                     dt=month_migration_minus1_start_dt)
    expected_act_data = steps.CommonData.create_expected_act_data(amount=act_sum,
                                                                  act_date=month_migration_minus1_end_dt)

    transaction_count = get_transaction_count(context)
    single_consume_qty = PAYMENT_SUM_PROMO / transaction_count
    single_consume_sum = single_consume_qty * (1 - discount / D('100'))

    consume_sum_total = ADVANCE_SUM_PROMO - added_sum if adjust_quantity else completions_sum
    consume_sum_main = consume_sum_total - (transaction_count - 1) * single_consume_sum
    consume_qty_main = utils.dround(consume_sum_main / (1 - discount / D('100')), 6)

    consume_qty_add = ADVANCE_SUM_PROMO - PAYMENT_SUM_PROMO
    consume_sum_add = consume_qty_add * (1 - discount / D('100'))

    expected_order_data = [
        steps.CommonData.create_expected_order_data(context.service.id,
                                                    SERVICE_CURRENCY_TO_MIN_COST_PRODUCT_MAP[(context.service, context.currency)].id,
                                                    contract_id,
                                                    consume_sum=consume_sum_add,
                                                    completion_qty=consume_qty_add,
                                                    consume_qty=consume_qty_add),
    ]

    for product in SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)]:
        cs = consume_sum_main if product == SERVICE_CURRENCY_TO_MAIN_PRODUCT_MAP[(context.service, context.currency)] else single_consume_sum
        cq = consume_qty_main if product == SERVICE_CURRENCY_TO_MAIN_PRODUCT_MAP[(context.service, context.currency)] else single_consume_qty
        expected_order_data.append(
            steps.CommonData.create_expected_order_data(context.service.id, product.id, contract_id,
                                                        consume_sum=cs,
                                                        completion_qty=single_consume_qty,
                                                        consume_qty=cq),
        )

    if Services.TAXI_CORP.id in context.contract_services:
        expected_order_data += [
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_RUB.id, contract_id,
                                                        consume_sum=single_consume_sum,
                                                        completion_qty=single_consume_qty,
                                                        consume_qty=single_consume_qty),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_MIN_COST.id,
                                                        contract_id, D('0'))
        ]

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)

    # тут есть ещё charge_note
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data], same_length=False),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data]),
                     'Сравниваем данные из акта с шаблоном')
    utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                     'Сравниваем данные из заказа с шаблоном')

    ### НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты
    # суммы откруток по продуктам сделаны такими же, как в старых открутках (только в тлоге суммы без НДС, а у ОЕБС с НДС)
    # поэтому многие расчеты взяты из старых тестов, суммы домножены на 2.
    if migration_dt:

        # создаю промокод, привязанный к клиенту
        create_new_promo(client_id, context, adjust_quantity=adjust_quantity, apply_on_create=apply_on_create)

        # создаю реквест и счет-квитанцию
        request_id = create_request_taxi(client_id, service_order_id, PERSONAL_ACC_SUM_PROMO,
                                         month_minus1_start_dt, service_id)
        invoice_id_charge_note, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=False,
                                                                 contract_id=contract_id)
        invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
        payment_id = steps.InvoiceSteps.get_payment_id_for_charge_note(invoice_id_charge_note)

        # рассчитываю скидку и сумму к оплате
        discount = calculate_discount_pct_from_fixed_sum(PROMO_SUM,
                                                         context.nds.pct_on_dt(month_minus1_start_dt),
                                                         ADVANCE_SUM_PROMO, adjust_quantity=adjust_quantity)

        sum_to_pay = ADVANCE_SUM_PROMO * (
                    D('1') - discount * D('0.01')) if not adjust_quantity else PERSONAL_ACC_SUM_PROMO

        # оплачиваю счет на сумму с учетом скидки
        steps.TaxiSteps.create_cash_payment_fact(external_invoice_id, sum_to_pay, month_minus1_start_dt,
                                                 'INSERT',
                                                 payment_id)
        steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

        # делаю открутки
        dt = month_minus1_start_dt + relativedelta(days=5)

        if context.service == Services.TAXI_CORP_CLIENTS:
            steps.SimpleApi.create_fake_tpt_data(context, FAKE_TPT_CLIENT_ID, FAKE_TPT_PERSON_ID,
                                                 FAKE_TPT_CONTRACT_ID, dt,
                                                 [{
                                                     'client_amount': PAYMENT_SUM_PROMO / transaction_count,
                                                     'client_id': client_id,
                                                     'transaction_type': TransactionType.PAYMENT
                                                 }])

        compls_dicts = []
        for idx, product in enumerate(SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)]):
            compls_dicts += [
                {
                    'service_id': context.service.id,
                    'last_transaction_id': 99,
                    'amount': PAYMENT_SUM_PROMO / transaction_count,
                    'product_id': product.id,
                    'dt': dt,
                    'transaction_dt': dt,
                    'currency': context.currency.iso_code,
                    'accounting_period': dt
                }
            ]
        steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_dicts)

        steps.TaxiSteps.process_taxi(contract_id, dt + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                       month_minus1_end_dt)

        # отдельно считаем сумму откруток и добивки со скидкой для корректного округления
        completions_sum = (PAYMENT_SUM_PROMO * (D('1') - discount * D('0.01'))).quantize(D("1.00"), ROUND_HALF_UP)
        added_sum = ((ADVANCE_SUM_PROMO - PAYMENT_SUM_PROMO) * (D('1') - discount * D('0.01'))).quantize(D("1.00"),
                                                                                                         ROUND_HALF_UP)

        consume_sum = sum_to_pay if not adjust_quantity else PERSONAL_ACC_SUM_PROMO
        act_sum = completions_sum + added_sum

        expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                         contract_id, person_id,
                                                                                         consume_sum * 2,
                                                                                         total_act_sum=act_sum * 2,
                                                                                         dt=month_minus1_start_dt)
        expected_act_data_2 = steps.CommonData.create_expected_act_data(amount=act_sum,
                                                                        act_date=month_minus1_end_dt)
        expected_act_data = [expected_act_data, expected_act_data_2]

        transaction_count = get_transaction_count(context)
        single_consume_qty = PAYMENT_SUM_PROMO / transaction_count
        single_consume_sum = single_consume_qty * (1 - discount / D('100'))

        consume_sum_total = ADVANCE_SUM_PROMO - added_sum if adjust_quantity else completions_sum
        consume_sum_main = consume_sum_total - (transaction_count - 1) * single_consume_sum
        consume_qty_main = utils.dround(consume_sum_main / (1 - discount / D('100')), 6)

        consume_qty_add = ADVANCE_SUM_PROMO - PAYMENT_SUM_PROMO
        consume_sum_add = consume_qty_add * (1 - discount / D('100'))

        expected_order_data = [
            steps.CommonData.create_expected_order_data(context.service.id,
                                                        SERVICE_CURRENCY_TO_MIN_COST_PRODUCT_MAP[
                                                            (context.service, context.currency)].id,
                                                        contract_id,
                                                        consume_sum=consume_sum_add * 2,
                                                        completion_qty=consume_qty_add * 2,
                                                        consume_qty=consume_qty_add * 2),
        ]

        for product in SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)]:
            cs = consume_sum_main if product == SERVICE_CURRENCY_TO_MAIN_PRODUCT_MAP[
                (context.service, context.currency)] else single_consume_sum
            cq = consume_qty_main if product == SERVICE_CURRENCY_TO_MAIN_PRODUCT_MAP[
                (context.service, context.currency)] else single_consume_qty
            expected_order_data.append(
                steps.CommonData.create_expected_order_data(context.service.id, product.id, contract_id,
                                                            consume_sum=cs * 2,
                                                            completion_qty=single_consume_qty * 2,
                                                            consume_qty=cq * 2),
            )

        if Services.TAXI_CORP.id in context.contract_services:
            expected_order_data += [
                steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_RUB.id,
                                                            contract_id,
                                                            consume_sum=single_consume_sum * 2,
                                                            completion_qty=single_consume_qty * 2,
                                                            consume_qty=single_consume_qty * 2),
                steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_MIN_COST.id,
                                                            contract_id, D('0'))
            ]

        invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
        act_data = steps.ActsSteps.get_act_data_by_client(client_id)
        order_data = steps.OrderSteps.get_order_data_by_client(client_id)

        # тут есть ещё charge_note
        utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data], same_length=False),
                         'Сравниваем данные из счета с шаблоном')
        utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                         'Сравниваем данные из акта с шаблоном')
        utils.check_that(order_data, contains_dicts_with_entries(expected_order_data),
                         'Сравниваем данные из заказа с шаблоном')


# ЛОГИКА ДОБИВОК ОТМЕНЕНА С ПЕРЕХОДОМ НА ОТКРУТКИ ИЗ ОЕБС
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11_Taxi))
@pytest.mark.parametrize(
    'personal_acc_sum_1, personal_acc_sum_2,',
    [
        (D('150'), D('120')),
        (D('150'), D('16')),
        pytest.mark.smoke((D('25'), D('120'))),
        (D('25'), D('16')),
    ],
    ids=[
        'Remains > 0, current month balance > 0',
        'Remains > 0, current month balance < 0',
        'Remains < 0, current month balance > 0',
        'Remains < 0, current month balance < 0'
    ]
)
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    FOOD_CORP_CONTEXT,
], ids=lambda c: c.name)
def test_corp_balances_with_remains(personal_acc_sum_1, personal_acc_sum_2, context):
    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')
    if migration_dt:
        # 2 месяца до даты миграции
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            utils.Date.previous_two_months_dates(migration_dt)
        # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
        posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
        oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
            utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    else:
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = [None] * 4

    ### СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты
    completions_1, completions_2, min_advance = \
        calculate_min_cost_or_advance_sum(context, is_add_sum_expected=True)

    params = {
        'start_dt': month_migration_minus2_start_dt,
        'advance_payment_sum': min_advance
    }
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_postpay=False,
        additional_params=params)

    # положим денег на лицевой счёт
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_sum_1, payment_dt=month_migration_minus2_start_dt)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus2_start_dt + relativedelta(days=1))

    create_completions(context, month_migration_minus2_start_dt + relativedelta(days=2), client_id, COEF_TLOG_1)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus2_end_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt)

    remains = personal_acc_sum_1 - min_advance

    balance_before_pers_acc_2 = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    expected_balance_before_pers_acc_2 = calc_balance(context, min_advance, D('0'), D('0'),
                                                      external_invoice_id, remains)
    utils.check_that(balance_before_pers_acc_2, contains_dicts_with_entries([expected_balance_before_pers_acc_2]),
                     u'Проверяем, что баланс в начале второго месяца совпадает')

    # все операции в текущем месяце выполняются первым числом, чтобы тесты не ломались в начале месяца
    # положим денег на лицевой счёт
    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_sum_2, payment_dt=month_migration_minus2_end_dt)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus2_end_dt)
    balance_pers_acc_2 = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    expected_balance_pers_acc_2 = calc_balance(context, min_advance, personal_acc_sum_2, D('0'),
                                               external_invoice_id, remains)
    utils.check_that(balance_pers_acc_2, contains_dicts_with_entries([expected_balance_pers_acc_2]),
                     u'Проверяем, что баланс после платежа на ЛС во втором месяце совпадает')

    create_completions(context, month_migration_minus1_start_dt, client_id, COEF_TLOG_1_2, coef_tpt=COEF_TPT)
    create_completions(context, month_migration_minus1_start_dt, client_id, COEF_TLOG_2, coef_tpt=COEF_TPT)
    steps.TaxiSteps.process_taxi(contract_id, CURRENT_MONTH_START_DT)
    balance_after_completions_2 = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    expected_balance_completions_2 = calc_balance(context, min_advance, personal_acc_sum_2, completions_2,
                                                  external_invoice_id, remains)
    utils.check_that(balance_after_completions_2, contains_dicts_with_entries([expected_balance_completions_2]),
                     u'Проверяем, что баланс после отвкруток во втором месяце совпадает')


# проверка баланса у предоплатных счетов с добивками, нет остатка с предыдущего месяца
# Сервис отказывается от логики добивок на стороне баланса с переходом на ОЕБСные данные
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.parametrize(
    'personal_acc_sum, coef_tlog, coef_tpt, is_added_sum_expected',
    [
        (D('0'), ZERO_COEF_TLOG, ZERO_COEF_TPT, 1),
        (PERSONAL_ACC_SUM_1, ZERO_COEF_TLOG, ZERO_COEF_TPT, 1),
        (D('0'), COEF_TLOG_1, D('1'), 1),
        (D('0'), COEF_TLOG_1, D('1'), 0),
    ],
    ids=[
        'No operations',
        'Personal account payment only',
        'Completions only, completions < advance payment sum',
        'Completions only, completions > advance payment sum'
    ]
)
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    FOOD_CORP_CONTEXT,
], ids=lambda c: c.name)
def test_corp_balance(personal_acc_sum, coef_tlog, coef_tpt, is_added_sum_expected, context):
    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')
    if migration_dt:
        # 2 месяца до даты миграции
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            utils.Date.previous_two_months_dates(migration_dt)
        # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
        posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
        oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
            utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    else:
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = [None] * 4

    completions_1, completions_2, min_advance = \
        calculate_min_cost_or_advance_sum(context, is_add_sum_expected=True, coef_tlog_1=coef_tlog, coef_tpt_1=coef_tpt)

    params = {
        'start_dt': month_migration_minus1_start_dt,
        'advance_payment_sum': min_advance
    }
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_postpay=0,
        additional_params=params)

    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    if personal_acc_sum:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_sum, payment_dt=month_migration_minus1_start_dt)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus1_start_dt)

    # проверяю только по coef_tpt, т.к. открутки либо есть все, либо нет вообще
    if coef_tpt:
        create_completions(context, month_migration_minus1_start_dt, client_id, coef_tlog, coef_tpt)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus1_start_dt)

    expected_balance = calc_balance(context, min_advance, personal_acc_sum, completions_1,
                                    external_invoice_id)
    balance = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    utils.check_that(balance, contains_dicts_with_entries([expected_balance]),
                     u'Проверяем, что баланс после отвкруток во втором месяце совпадает')


def test_corp_balances_with_noadvance_and_refunds_in_second_month():
    context = CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP

    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')
    if migration_dt:
        # 2 месяца до даты миграции
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            utils.Date.previous_two_months_dates(migration_dt)
        # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
        posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()
        oebs_compls_start_dt = max(posible_oebs_compls_start_dt, migration_dt)
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = \
            utils.Date.previous_two_months_dates(oebs_compls_start_dt + relativedelta(months=2))
    else:
        month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
            MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT
        month_minus2_start_dt, month_minus2_end_dt, month_minus1_start_dt, month_minus1_end_dt = [None] * 4

    personal_acc_sum_1 = D('100')
    compl_sum_wo_nds = D('100')
    reverse_sum_wo_nds = D('50')
    params = {
        'start_dt': month_migration_minus2_start_dt,
    }
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_postpay=False,
        additional_params=params)

    # положим денег на лицевой счёт
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_sum_1, payment_dt=month_migration_minus2_start_dt)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus2_start_dt + relativedelta(days=1))
    order_dicts_tlog = [
        {
            'service_id': context.service.id,
            'amount': compl_sum_wo_nds,
            'type': CorpTaxiOrderType.commission,
            'dt': month_migration_minus2_start_dt + relativedelta(days=2),
            'transaction_dt': month_migration_minus2_start_dt + relativedelta(days=2),
            'currency': context.currency.iso_code
        },
    ]
    tsteps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)
    steps.TaxiSteps.process_taxi(contract_id, month_migration_minus2_end_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_migration_minus2_end_dt)
    balance_before_reverse = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    expected_balance = {'Balance': personal_acc_sum_1 - compl_sum_wo_nds * context.nds.koef_on_dt(month_migration_minus2_end_dt),
                        'CurrMonthCharge': 0,
                        'SubscriptionBalance': 0,
                        'SubscriptionRate': 0,
                        'PersonalAccountExternalID': external_invoice_id,
                        'ReceiptSum': personal_acc_sum_1,
                        'Currency': context.currency.iso_code,
                        'CurrMonthBonus': D('0'),
                        'BonusLeft': D('0')}
    utils.check_that(balance_before_reverse, contains_dicts_with_entries([expected_balance]),
                     u'Проверяем баланс до реверса')
    order_dicts_tlog = [
        {
            'service_id': context.service.id,
            'amount': -reverse_sum_wo_nds,
            'type': CorpTaxiOrderType.commission,
            'dt': month_migration_minus1_start_dt,
            'transaction_dt': month_migration_minus1_start_dt,
            'currency': context.currency.iso_code
        },
    ]
    tsteps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)
    balance_after_reverse = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    expected_balance = {
        'Balance': personal_acc_sum_1 - (compl_sum_wo_nds * context.nds.koef_on_dt(month_migration_minus2_end_dt)
                                         - reverse_sum_wo_nds * context.nds.koef_on_dt(month_migration_minus1_end_dt)),
        'CurrMonthCharge': 0,
        'SubscriptionBalance': 0,
        'SubscriptionRate': 0,
        'PersonalAccountExternalID': external_invoice_id,
        'ReceiptSum': personal_acc_sum_1,
        'Currency': context.currency.iso_code,
        'CurrMonthBonus': D('0'),
        'BonusLeft': D('0')}
    utils.check_that(balance_after_reverse, contains_dicts_with_entries([expected_balance]),
                     u'Проверяем баланс после реверса')

# --------------------------------------------------------------------------------------------------------------
# Utils


def calc_balance(context, advance_sum, personal_acc, completions, invoice_eid, remains=D('0')):
    curr_month_charge = max(completions, D('0'))
    actual_balance = remains + personal_acc
    balance = actual_balance if actual_balance > advance_sum else actual_balance - advance_sum
    if actual_balance > advance_sum or curr_month_charge > advance_sum:
        if personal_acc:
            balance -= curr_month_charge
        else:
            balance = -1 * curr_month_charge

    subscription_balance = max(advance_sum - curr_month_charge, D('0')) if actual_balance > advance_sum else D('0')

    subscription_rate = advance_sum
    expected_balance = {'Balance': balance,
                        'CurrMonthCharge': curr_month_charge,
                        'SubscriptionBalance': subscription_balance,
                        'SubscriptionRate': subscription_rate,
                        'PersonalAccountExternalID': invoice_eid,
                        'Currency': context.currency.iso_code,
                        'CurrMonthBonus': D('0'),
                        'BonusLeft': D('0')}
    return expected_balance


def create_completions(context, dt, client_id, coef_tlog, coef_tpt=D('1')):
    # создаем в любом случае, учитывается в акте, только если узакан сервис TAXI_CORP(135) в договоре
    if context.service == Services.TAXI_CORP_CLIENTS:
        steps.SimpleApi.create_fake_tpt_data(context, FAKE_TPT_CLIENT_ID, FAKE_TPT_PERSON_ID,
                                             FAKE_TPT_CONTRACT_ID, dt,
                                             [{
                                                  'client_amount': PAYMENT_SUM_TPT * coef_tpt,
                                                  'client_id': client_id,
                                                  'transaction_type': TransactionType.PAYMENT
                                              },
                                              {
                                                  'client_amount': REFUND_SUM_TPT * coef_tpt,
                                                  'client_id': client_id,
                                                  'transaction_type': TransactionType.REFUND
                                              }],
                                             sum_key='client_amount')

    order_dicts_tlog = []
    for idx, product in enumerate(SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)]):
        order_dicts_tlog += [
            {
                'service_id': context.service.id,
                'amount': PAYMENT_SUM_TLOG * coef_tlog[idx],
                'type': PRODUCT_TO_ORDER_TYPE_MAP[product],
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
            },
            {
                'service_id': context.service.id,
                'amount': -REFUND_SUM_TLOG * coef_tlog[idx],
                'type': PRODUCT_TO_ORDER_TYPE_MAP[product],
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
            },
        ]
    tsteps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)


def create_oebs_completions(context, dt, contract_id, client_id, coef_tlog, coef_tpt=D('1')):
    # создаем в любом случае, учитывается в акте, только если узакан сервис TAXI_CORP(135) в договоре
    if context.service == Services.TAXI_CORP_CLIENTS:
        steps.SimpleApi.create_fake_tpt_data(context, FAKE_TPT_CLIENT_ID, FAKE_TPT_PERSON_ID,
                                             FAKE_TPT_CONTRACT_ID, dt,
                                             [{
                                                  'client_amount': PAYMENT_SUM_TPT * coef_tpt,
                                                  'client_id': client_id,
                                                  'transaction_type': TransactionType.PAYMENT
                                              },
                                              {
                                                  'client_amount': REFUND_SUM_TPT * coef_tpt,
                                                  'client_id': client_id,
                                                  'transaction_type': TransactionType.REFUND
                                              }],
                                             sum_key='client_amount')

    compls_dicts = []
    for idx, product in enumerate(SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)]):
        # добавляем НДС, т.к. открутки из ОЕБС берем с НДС, а в тлоге были без НДС -
        # чтобы использовать одинаковые цифры в расчетах до и после миграции
        nds_pct = context.nds.pct_on_dt(dt)
        amount = (PAYMENT_SUM_TLOG * coef_tlog[idx] - REFUND_SUM_TLOG * coef_tlog[idx]) * ((100 + nds_pct) / 100)
        compls_dicts += [
            {
                'service_id': context.service.id,
                'last_transaction_id': 99,
                'amount': amount,
                'product_id': product.id,
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
                'accounting_period': dt
            }
        ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_dicts)


# yuelyasheva: метод для вычисления суммы откруток и минималки. сделан для того, чтобы можно было легко менять
# количество продуктов/открутки, не подкручивая каждый раз минималку
# добавляю костыль с игнорированием месяца в расчетах (чтобы легко моделировать ситуацию,
# когда в одном месяце есть добивка, а в другом нет
# выставляю ндс на предыдущий месяц в надежде, что он больше не будет меняться
def calculate_min_cost_or_advance_sum(context, is_add_sum_expected, one_month_added_sum=0,
                                      coef_tlog_1=COEF_TLOG_1, coef_tlog_1_2=COEF_TLOG_1_2, coef_tlog_2=COEF_TLOG_2,
                                      coef_tpt_1=D('1'), coef_tpt_2=COEF_TPT,
                                      nds_pct=NdsNew.DEFAULT.koef_on_dt(PREVIOUS_MONTH_END_DT)):
    completions_1 = 0
    completions_2 = 0

    # открутки за первый месяц
    for coef in coef_tlog_1[:len(SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)])]:
        completions_1 += (PAYMENT_SUM_TLOG - REFUND_SUM_TLOG) * coef * nds_pct
    if Services.TAXI_CORP.id in context.contract_services:
        completions_1 += PAYMENT_SUM_TPT * coef_tpt_1 - REFUND_SUM_TPT * coef_tpt_1

    # открутки за первый месяц, учитываемые во втором
    for coef in coef_tlog_1_2[:len(SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)])]:
        completions_2 += (PAYMENT_SUM_TLOG - REFUND_SUM_TLOG) * coef * nds_pct
    if Services.TAXI_CORP.id in context.contract_services:
        completions_2 += PAYMENT_SUM_TPT * coef_tpt_2 - REFUND_SUM_TPT * coef_tpt_2

    # открутки за второй месяц
    for coef in coef_tlog_2[:len(SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)])]:
        completions_2 += (PAYMENT_SUM_TLOG - REFUND_SUM_TLOG) * coef * nds_pct
    if Services.TAXI_CORP.id in context.contract_services:
        completions_2 += PAYMENT_SUM_TPT * coef_tpt_2 - REFUND_SUM_TPT * coef_tpt_2

    # вычисляем минималку. три варианта - добивки нужны в обоих месяцах, добивки нужны в одном месяце
    # (в каком конкретно, будем регулировать коэффициентами) и добивки не нужны вообще
    if is_add_sum_expected:
        min_cost_or_adv_sum = max(completions_1, completions_2) + 1
    elif not one_month_added_sum:
        min_cost_or_adv_sum = min(completions_1, completions_2) - 1
    else:
        min_cost_or_adv_sum = (completions_1 + completions_2) / D('2')

    return completions_1, completions_2, min_cost_or_adv_sum


def create_expected_order_data(context, contract_id, act_sum_1=0, act_sum_2=0, completions_1=0, completions_2=0,
                               coef_tlog_1=COEF_TLOG_1, coef_tlog_1_2=COEF_TLOG_1_2, coef_tlog_2=COEF_TLOG_2,
                               coef_tpt_1=D('1'), coef_tpt_2=COEF_TPT,
                               nds_pct=NdsNew.DEFAULT.koef_on_dt(PREVIOUS_MONTH_END_DT), repetition_factor=1):
    # repetition_factor: c миграцией на открутки из ОЕБС тесты считают добивку дважды:
    # 2 месяца старой логики, за ними 2 месяца новой логики. Суммы в старых и новых открутках сделаны одинаковыми.
    # чтобы не переписывать страшную логику этих тестов, мы просто на втором запуске все домножим все суммы на 2
    completion_qty_main = completions_1 + completions_2
    consume_sum_add = max(act_sum_1 + act_sum_2 - completion_qty_main, D('0')) if act_sum_1 + act_sum_2 > 0 else D('0')
    consume_sum_add *= repetition_factor

    expected_order_data = [
        steps.CommonData.create_expected_order_data(
            context.service.id,
            SERVICE_CURRENCY_TO_MIN_COST_PRODUCT_MAP[(context.service, context.currency)].id,
            contract_id,
            consume_sum_add
        )
    ]

    for idx, product in enumerate(SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)]):
        cs = (coef_tlog_1[idx] + coef_tlog_2[idx] + coef_tlog_1_2[idx]) * (PAYMENT_SUM_TLOG - REFUND_SUM_TLOG) * nds_pct
        cs *= repetition_factor
        expected_order_data.append(
            steps.CommonData.create_expected_order_data(context.service.id, product.id, contract_id,
                                                        consume_sum=max(cs, D('0')),
                                                        completion_qty=cs),
        )
    single_amount_tpt = (coef_tpt_1 + 2 * coef_tpt_2) * (PAYMENT_SUM_TPT - REFUND_SUM_TPT)
    single_amount_tpt *= repetition_factor
    if Services.TAXI_CORP.id in context.contract_services:
        expected_order_data += [
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_RUB.id, contract_id,
                                                        consume_sum=max(single_amount_tpt, D('0')),
                                                        completion_qty=single_amount_tpt),
            steps.CommonData.create_expected_order_data(Services.TAXI_CORP.id, Products.CORP_TAXI_MIN_COST.id,
                                                        contract_id, D('0'))
        ]

    return expected_order_data


def create_expected_act_data(act_sum_1, act_sum_2, dt_1=MONTH_BEFORE_PREV_END_DT, dt_2=PREVIOUS_MONTH_END_DT):
    expected_act_data = []
    if act_sum_1:
        expected_act_data_first_month = steps.CommonData.create_expected_act_data(amount=utils.dround(act_sum_1, 2),
                                                                                  act_date=utils.Date.last_day_of_month(
                                                                                      dt_1))
        expected_act_data.append(expected_act_data_first_month)

    if act_sum_2:
        expected_act_data_second_month = steps.CommonData.create_expected_act_data(amount=utils.dround(act_sum_2, 2),
                                                                                   act_date=utils.Date.last_day_of_month(
                                                                                       dt_2))
        expected_act_data.append(expected_act_data_second_month)
    return expected_act_data


def get_transaction_count(context):
    return len(SERVICE_CURRENCY_TO_PRODUCTS_MAP[(context.service, context.currency)]) \
                        + (1 if Services.TAXI_CORP.id in context.contract_services else 0)


def calculate_discount_pct_from_fixed_sum(fixed_sum, nds, qty, sum_before=None, adjust_quantity=None):
    internal_price = 1
    if sum_before:
        total_sum = sum(sum_before)
    else:
        total_sum = qty * D(internal_price).quantize(D('0.001'))
    bonus_with_nds = promo_steps.add_nds_to_amount(fixed_sum, nds)
    return promo_steps.calculate_static_discount_sum(total_sum=total_sum, bonus_with_nds=bonus_with_nds,
                                                     adjust_quantity=adjust_quantity)


def create_request_taxi(client_id, service_order_id, qty, dt, service_id, promo=None):
    begin_dt = dt

    additional_params = {}
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': begin_dt}]
    additional_params.update({'TurnOnRows': 1, 'InvoiceDesireType': 'charge_note'})
    if promo:
        additional_params.update({'PromoCode': promo})

    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=additional_params)  # 'InvoiceDesireType': 'charge_note'})
    return request_id


def create_new_promo(client_id, context, adjust_quantity=True, apply_on_create=True):
    start_dt = datetime.now() - relativedelta(years=1)
    end_dt = datetime.now() + relativedelta(years=1)
    minimal_amounts = {context.currency.iso_code: 1}

    calc_params = promo_steps.fill_calc_params_fixed_sum(currency_bonuses={context.currency.iso_code: PROMO_SUM},
                                                         reference_currency=context.currency.iso_code,
                                                         adjust_quantity=adjust_quantity,
                                                         apply_on_create=apply_on_create)

    code = steps.PromocodeSteps.generate_code()
    promo_code_id, promo_code_code = promo_steps.import_promocode(calc_class_name=PromocodeClass.FIXED_SUM,
                                                                  start_dt=start_dt, end_dt=end_dt,
                                                                  calc_params=calc_params,
                                                                  firm_id=context.firm.id,
                                                                  promocodes=[code],
                                                                  service_ids=context.contract_services,
                                                                  minimal_amounts=minimal_amounts)[0]
    if client_id:
        promo_steps.reserve(client_id, promo_code_id)

    return promo_code_id, promo_code_code
