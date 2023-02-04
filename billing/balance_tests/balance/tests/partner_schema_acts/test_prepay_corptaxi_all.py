# -*- coding: utf-8 -*-

__author__ = 'alshkit'

from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as tsteps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Services, TransactionType, CorpTaxiOrderType, CorpEdaOrderType, DriveB2BOrderType, \
    Products, Currencies
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, \
    CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, FOOD_CORP_CONTEXT, CORP_DISPATCHING_BY_CONTEXT, \
    CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP, DRIVE_B2B_CONTEXT
from btestlib.matchers import has_entries_casted, equal_to_casted_dict, contains_dicts_with_entries

# эту сумму зачисляем на счёт
PERSONAL_ACC_SUM = Decimal('4305')
PERSONAL_ACC_SUM_SMALL = Decimal('1')
# суммы для откруток
PAYMENT_SUM = Decimal('2143')
REFUND_SUM = Decimal('89')
PAYMENT_SUM_INT = Decimal('57')
REFUND_SUM_INT = Decimal('23')
PAYMENT_SUM_DECOUP = Decimal('2252')
REFUND_SUM_DECOUP = Decimal('41')
INT_COEF_1 = Decimal('0.4')
INT_COEF_2 = Decimal('0.9')
DISCOUNT_BONUS_SUM = Decimal('0')
APX_SUM = Decimal('0')

MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT, _, _ = \
    utils.Date.previous_three_months_start_end_dates()
CURRENT_MONTH_START_DT, _ = utils.Date.current_month_first_and_last_days()
params = {
    'start_dt': MONTH_BEFORE_PREV_START_DT
}

FAKE_TPT_CONTRACT_ID = 123
FAKE_TPT_CLIENT_ID = 123
FAKE_TPT_PERSON_ID = 123

EXPECTED_BALANCE_DATA = {
    'Balance': None,
    'BonusLeft': '0',
    'ClientID': None,
    'ContractID': None,
    'CurrMonthBonus': '0',
    'CurrMonthCharge': '0',
}

SERVICE_TO_ORDER_TYPE = {
    Services.TAXI_CORP_CLIENTS: CorpTaxiOrderType.commission,
    Services.FOOD_CORP: CorpEdaOrderType.MAIN,
    Services.DRIVE_B2B: DriveB2BOrderType.MAIN,
    Services.CORP_DISPATCHING: CorpTaxiOrderType.dispatching_commission,
}

SERVICE_CURRENCY_TO_PRODUCT_ID = {
    (Services.CORP_DISPATCHING, Currencies.BYN): Products.CORP_DISPATCHING_B2B_TRIPS_ACCESS_PAYMENT.id,
    (Services.TAXI_CORP_CLIENTS, Currencies.RUB): Products.CORP_TAXI_CLIENTS_RUB.id,
    (Services.TAXI_CORP_CLIENTS, Currencies.KZT): Products.CORP_TAXI_CLIENTS_KZT.id,
    (Services.TAXI_CORP_CLIENTS, Currencies.BYN): Products.CORP_TAXI_CLIENTS_BYN.id,
    (Services.FOOD_CORP, Currencies.RUB): Products.FOOD_CORP_RUB.id,
    (Services.DRIVE_B2B, Currencies.RUB): Products.DRIVE_B2B_RUB.id,
}


# проверим, что баланс после откруток и возвратов считается верно
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.parametrize(
    'personal_acc_payment, is_tpt_needed, context',
    [
        pytest.param(0, 0, CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
                     id="No payments RU 650"),
        pytest.param(PERSONAL_ACC_SUM, 0, CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
                     id="Personal_acc payment, no thirdparty payments and refunds RU 650"),
        pytest.param(PERSONAL_ACC_SUM, 1, CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
                     id="Personal_acc payments, thirdparty payments and refunds RU 650"),
        pytest.param(PERSONAL_ACC_SUM_SMALL, 1, CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
                     id="Thirdparty payments and refunds personal_acc RU 650"),

        pytest.param(0, 0, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
                     id="No payments RU 135 and 650"),
        pytest.param(PERSONAL_ACC_SUM, 0, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
                     id="Personal_acc payment, no thirdparty payments and refunds RU 135 and 650"),
        pytest.param(PERSONAL_ACC_SUM, 1, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
                     id="Personal_acc payments, thirdparty payments and refunds RU 135 and 650"),
        pytest.param(PERSONAL_ACC_SUM_SMALL, 1, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
                     id="Thirdparty payments and refunds personal_acc RU 135 and 650"),

        pytest.param(0, 0, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED,
                     id="No payments KZ"),
        pytest.param(PERSONAL_ACC_SUM, 0, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED,
                     id="Personal_acc payment, no thirdparty payments and refunds KZ"),
        pytest.param(PERSONAL_ACC_SUM, 1, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED,
                     id="Personal_acc payments, thirdparty payments and refunds KZ"),
        pytest.param(PERSONAL_ACC_SUM_SMALL, 1, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED,
                     id="Thirdparty payments and refunds personal_acc KZ"),

        pytest.param(0, 0, CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP, id="No payments BY 650"),
        pytest.param(PERSONAL_ACC_SUM, 0, CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP,
                     id="Personal_acc payment, no thirdparty payments and refunds BY 650"),
        pytest.param(PERSONAL_ACC_SUM, 1, CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP,
                     id="Personal_acc payments, thirdparty payments and refunds BY 650"),
        pytest.param(PERSONAL_ACC_SUM_SMALL, 1, CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP,
                     id="Thirdparty payments and refunds personal_acc BY 650"),

        pytest.param(0, 0, FOOD_CORP_CONTEXT,
                     id="No payments Food RU"),
        pytest.param(PERSONAL_ACC_SUM, 0, FOOD_CORP_CONTEXT,
                     id="Personal_acc payment Food RU"),

        pytest.param(0, 0, DRIVE_B2B_CONTEXT,
                     id="No payments Drive b2b"),
        pytest.param(PERSONAL_ACC_SUM, 0, DRIVE_B2B_CONTEXT,
                     id="Personal_acc payment Drive b2b"),
    ])
def test_taxi_balances(personal_acc_payment, is_tpt_needed, context):
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
        context,
        additional_params={'start_dt': month_migration_minus1_start_dt},
        is_offer=True,
        is_postpay=0
    )

    # положим денег на лицевой счёт
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_payment,
                           payment_dt=month_minus1_start_dt or month_migration_minus1_start_dt)

    sum_first_month_650, sum_first_month_int_650, sum_first_month_135, sum_first_month_int_135 = \
        create_completions(context, month_migration_minus1_start_dt, client_id)

    # старые открутки 650 (тлог) не учитываются по transaction_dt, 135 учитываются
    _, _, sum_first_month_135_1, sum_first_month_int_135_1 = \
        create_completions(context, month_minus1_start_dt, client_id)
    sum_first_month_oebs = create_partner_oebs_completions(context, contract_id, client_id, month_minus1_start_dt)

    taxi_balance_data = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])[0]

    first_month_total_charge = sum_first_month_650 + sum_first_month_int_650 + sum_first_month_135 + \
        sum_first_month_int_135 + sum_first_month_135_1 + \
        sum_first_month_int_135_1 + sum_first_month_oebs

    expected_data = utils.copy_and_update_dict(EXPECTED_BALANCE_DATA,
                                               {
                                                   'Balance': personal_acc_payment - first_month_total_charge,
                                                   'ClientID': client_id,
                                                   'ContractID': contract_id,
                                                   'CurrMonthCharge': first_month_total_charge,
                                                   'PersonalAccountExternalID': external_invoice_id,
                                                   'ReceiptSum': personal_acc_payment,
                                                   'DiscountBonusSum': DISCOUNT_BONUS_SUM,
                                                   'TotalCharge': first_month_total_charge,
                                                   'ApxSum': APX_SUM,
                                                   'Currency': context.currency.iso_code
                                               })

    utils.check_that(taxi_balance_data, has_entries_casted(expected_data),
                     'Проверим, что после оплаты и возврата баланс верный')


parametrize_get_taxi_balance_context = pytest.mark.parametrize(
    'context',
    [
        pytest.param(DRIVE_B2B_CONTEXT, id="Drive b2b"),
        pytest.param(CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, id="RU 650"),
        pytest.param(CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, id="RU 135 and 650"),
        pytest.param(CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, id="KZ"),
        pytest.param(CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP, id="BY 650"),
        pytest.param(FOOD_CORP_CONTEXT, id="Food RU"),
    ]
)


# тест для GetTaxiBalance после закрытия первого месяца
@reporter.feature(Features.TRUST, Features.PAYMENT, Features.TAXI, Features.COMPENSATION)
@parametrize_get_taxi_balance_context
def test_get_balance_after_act(context):
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
        context,
        additional_params={'start_dt': month_migration_minus1_start_dt},
        is_offer=True,
        is_postpay=0
    )

    # создаем акт. Создается за тот месяц, день из которого передаем в качестве даты.

    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM, payment_dt=month_migration_minus1_start_dt)

    # создаем открутки до миграции, все учтутся
    old_sum_650, old_sum_internal_650, old_sum_135, old_sum_internal_135 = \
        create_completions(context, month_migration_minus1_start_dt, client_id)

    # старые открутки 650 (тлог) не учитываются по transaction_dt, 135 учитываются
    _, _, sum_135_1, sum_int_135_1 = \
        create_completions(context, month_minus2_start_dt, client_id)
    sum_oebs = create_partner_oebs_completions(context, contract_id, client_id, month_minus2_start_dt,
                                               accounting_period=month_minus2_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_minus2_end_dt)

    taxi_balance_data = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])[0]

    total_charge = old_sum_650 + old_sum_internal_650 + old_sum_135 + old_sum_internal_135 + \
        sum_135_1 + sum_int_135_1 + sum_oebs

    balance = PERSONAL_ACC_SUM - total_charge

    expected_balance_data = utils.copy_and_update_dict(EXPECTED_BALANCE_DATA,
                                                       {
                                                           'Balance': balance,
                                                           'ClientID': client_id,
                                                           'ContractID': contract_id,
                                                           'PersonalAccountExternalID': external_invoice_id,
                                                           'ReceiptSum': PERSONAL_ACC_SUM,
                                                           'DiscountBonusSum': DISCOUNT_BONUS_SUM,
                                                           'TotalCharge': total_charge,
                                                           'ApxSum': APX_SUM,
                                                           'Currency': context.currency.iso_code
                                                       })

    utils.check_that(taxi_balance_data, has_entries_casted(expected_balance_data),
                     'Проверим, что после оплаты и возврата баланс верный')


# тест на кэш баланса
@reporter.feature(Features.PAYMENT, Features.TAXI)
@parametrize_get_taxi_balance_context
@pytest.mark.parametrize('is_postpay', [0, 1], ids=['prepay', 'postpay'])
def test_cache_balance(context, is_postpay):
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
        context,
        additional_params={'start_dt': month_migration_minus1_start_dt},
        is_offer=True,
        is_postpay=is_postpay
    )

    # положим денег на лицевой счёт
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM,
                           payment_dt=month_minus2_start_dt or month_migration_minus2_start_dt)

    # создадим кэш баланса
    api.test_balance().CacheBalance(contract_id)

    # ожидаем, что получим данные отсюда
    expected_data = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])[0]

    # создадим открутки
    create_completions(context, month_migration_minus1_start_dt, client_id)

    create_partner_oebs_completions(context, contract_id, client_id, month_minus1_start_dt)

    # узнаем баланс. Проверим, что он совпадает с ожидаемым.
    taxi_balance_data = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])[0]
    utils.check_that(taxi_balance_data, has_entries_casted(expected_data),
                     'Проверим, что после оплаты и возврата баланс верный')


parametrize_act_context = pytest.mark.parametrize(
    'context',
    [
        pytest.param(CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, id="RU 650"),
        pytest.param(CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, id="RU 135 and 650"),
        pytest.param(CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, id="KZ"),
        pytest.param(CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP, id="BY 650"),
        pytest.param(FOOD_CORP_CONTEXT, id="Food RU"),
    ]
)


# проверим генерацию актов без данных
@reporter.feature(Features.PAYMENT, Features.TAXI)
@parametrize_act_context
def test_act_generation_wo_data(context):
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
@reporter.feature(Features.PAYMENT, Features.TAXI)
@parametrize_act_context
def test_act_generation_two_month(context):
    migration_params = steps.CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
    migration_dt = migration_params and migration_params.get('migration_date')
    # 2 месяца до даты миграции
    month_migration_minus2_start_dt, month_migration_minus2_end_dt, \
        month_migration_minus1_start_dt, month_migration_minus1_end_dt = \
        utils.Date.previous_two_months_dates(migration_dt)
    # 2 предыдуших месяца от текущего, если они больше даты миграции, либо 2 месяца вперед от даты миграции
    posible_oebs_compls_start_dt, _, _, _ = utils.Date.previous_two_months_dates()

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month_migration_minus2_start_dt},
        is_offer=True,
        is_postpay=0
    )

    # СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты

    # сгенерируем акт за 2 месяца до миграции
    sum_migration_minus2_month_650_1, sum_migration_minus2_month_650_int_1, \
        sum_migration_minus2_month_135_1, sum_migration_minus2_month_135_int_1 = \
        create_act(month_migration_minus2_start_dt, client_id, contract_id, context=context)
    # создадим фэйковый платеж в предпредыдущем закрытом периоде во имя накопительного итога
    sum_migration_minus2_month_650_2, sum_migration_minus2_month_650_int_2, \
        sum_migration_minus2_month_135_2, sum_migration_minus2_month_135_int_2 = \
        create_completions(context, month_migration_minus2_start_dt, client_id, coef=INT_COEF_1)

    # платеж за 1 месяц до миграции
    sum_migration_minus1_month_650, sum_migration_minus1_month_650_int, \
        sum_migration_minus1_month_135, sum_migration_minus1_month_135_int = \
        create_completions(context, month_migration_minus1_end_dt, client_id, coef=INT_COEF_2)

    # закроем месяц за 1 месяц до миграции
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_migration_minus1_end_dt)

    # закроем предыдущий месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_migration_minus1_end_dt)

    # получим данные для проверки. Проверяем только второй месяц
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    invoice_amount = (sum_migration_minus2_month_650_1 + sum_migration_minus2_month_650_int_1
                      + sum_migration_minus2_month_135_1 + sum_migration_minus2_month_135_int_1
                      + sum_migration_minus2_month_650_2 + sum_migration_minus2_month_650_int_2
                      + sum_migration_minus2_month_135_2 + sum_migration_minus2_month_135_int_2
                      + sum_migration_minus1_month_650 + sum_migration_minus1_month_650_int
                      + sum_migration_minus1_month_135 + sum_migration_minus1_month_135_int)

    # Сформируем ожидаемые данные для счета.
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     invoice_amount,
                                                                                     dt=month_migration_minus2_start_dt)

    act_amount_migration_minus2_month = (sum_migration_minus2_month_650_1 + sum_migration_minus2_month_650_int_1
                                         + sum_migration_minus2_month_135_1 + sum_migration_minus2_month_135_int_1)

    act_amount_migration_minus1_month = (sum_migration_minus2_month_650_2 + sum_migration_minus2_month_650_int_2
                                         + sum_migration_minus2_month_135_2 + sum_migration_minus2_month_135_int_2
                                         + sum_migration_minus1_month_650 + sum_migration_minus1_month_650_int
                                         + sum_migration_minus1_month_135 + sum_migration_minus1_month_135_int)

    # сформируем ожидаемые данные для акта.
    expected_act_data_migration_minus2_month = steps.CommonData.create_expected_act_data(
        amount=act_amount_migration_minus2_month,
        act_date=month_migration_minus2_end_dt
    )
    expected_act_data_migration_minus1_month = steps.CommonData.create_expected_act_data(
        amount=act_amount_migration_minus1_month,
        act_date=month_migration_minus1_end_dt
    )

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_migration_minus2_month,
                                                            expected_act_data_migration_minus1_month]),
                     'Сравниваем данные из акт с шаблоном')


# акты за два месяца и накопительный итог
@reporter.feature(Features.TAXI)
@pytest.mark.parametrize(
    'context, is_postpay',
    [
        pytest.param(CORP_DISPATCHING_BY_CONTEXT, 0, id="CORP_DISPATCHING_BY prepay"),
        pytest.param(CORP_DISPATCHING_BY_CONTEXT, 1, id="CORP_DISPATCHING_BY postpay"),
    ]
)
def test_act_generation_dispatching(context, is_postpay):
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
        context,
        additional_params={'start_dt': month_migration_minus2_start_dt},
        is_postpay=is_postpay
    )

    # СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты

    # сгенерируем акт за 2 месяца до миграции
    sum_migration_minus2_month_1, _, _, _ = create_act(month_migration_minus2_start_dt, client_id, contract_id,
                                                       context=context)
    # создадим фэйковый платеж в предпредыдущем закрытом периоде во имя накопительного итога
    sum_migration_minus2_month_2, _, _, _ = create_completions(context, month_migration_minus2_start_dt, client_id,
                                                               coef=INT_COEF_1)
    # платеж за 1 месяц до миграции
    sum_migration_minus1_month, _, _, _ = create_completions(context, month_migration_minus1_end_dt, client_id,
                                                             coef=INT_COEF_2)

    # закроем месяц за 1 месяц до миграции
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_migration_minus1_end_dt)

    # получим данные для проверки. Проверяем только второй месяц
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    invoice_amount_before_migration = sum_migration_minus2_month_1 + sum_migration_minus2_month_2 + sum_migration_minus1_month

    # Сформируем ожидаемые данные для счета.
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     invoice_amount_before_migration,
                                                                                     dt=month_migration_minus2_start_dt)

    act_amount_before_migration_minus2 = sum_migration_minus2_month_1
    act_amount_before_migration_minus1 = sum_migration_minus2_month_2 + sum_migration_minus1_month

    # сформируем ожидаемые данные для акта.
    expected_act_data_before_migration_minus2 = steps.CommonData.create_expected_act_data(
        amount=act_amount_before_migration_minus2,
        act_date=month_migration_minus2_end_dt
    )
    expected_act_data_before_migration_minus1 = steps.CommonData.create_expected_act_data(
        amount=act_amount_before_migration_minus1,
        act_date=month_migration_minus1_end_dt
    )

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_before_migration_minus2,
                                                            expected_act_data_before_migration_minus1]),
                     'Сравниваем данные из акт с шаблоном')

    # НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты

    # 1. первый месяц:
    # создаем старые открутки с датой transaction_dt, большей даты миграции, не должны учитываться
    create_completions(context, month_minus2_start_dt, client_id, coef=INT_COEF_1)
    # создаем окрутки без accounting_period, не должны учитываться
    create_partner_oebs_completions(context, contract_id, client_id, month_minus2_start_dt)

    # создаем окрутки для учета c нормальными датами
    sum_month_minus2_1 = create_partner_oebs_completions(context, contract_id, client_id, month_minus2_start_dt,
                                                         accounting_period=month_minus2_start_dt, coef=INT_COEF_1)

    # с dt в будущем, т.к. акте идет учет по accounting_date
    sum_month_minus2_2 = create_partner_oebs_completions(context, contract_id, client_id, month_minus1_end_dt,
                                                         accounting_period=month_minus2_start_dt, coef=INT_COEF_1)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_minus2_end_dt)

    # создадим открутки в закрытом периоде во имя накопительного итога,
    # но ОЕБС так не должен делать!
    sum_month_minus2_3 = create_partner_oebs_completions(context, contract_id, client_id, month_minus2_start_dt,
                                                         accounting_period=month_minus2_start_dt, coef=INT_COEF_1)
    # актуальные открутки второго месяца
    sum_month_minus1_1 = create_partner_oebs_completions(context, contract_id, client_id, month_minus1_start_dt,
                                                         accounting_period=month_minus1_start_dt, coef=INT_COEF_1)

    # закроем второй месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_minus1_end_dt)

    # получим данные для проверки. Проверяем только второй месяц
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    invoice_amount = invoice_amount_before_migration + sum_month_minus2_1 + sum_month_minus2_2 + \
        sum_month_minus2_3 + sum_month_minus1_1

    # Сформируем ожидаемые данные для счета.
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     contract_id, person_id,
                                                                                     invoice_amount,
                                                                                     dt=month_migration_minus2_start_dt)

    act_amount_month_minus2 = sum_month_minus2_1 + sum_month_minus2_2
    act_amount_month_minus1 = sum_month_minus2_3 + sum_month_minus1_1

    # сформируем ожидаемые данные для акта.
    expected_act_data_month_minus2 = steps.CommonData.create_expected_act_data(amount=act_amount_month_minus2,
                                                                               act_date=month_minus2_end_dt)
    expected_act_data_month_minus1 = steps.CommonData.create_expected_act_data(amount=act_amount_month_minus1,
                                                                               act_date=month_minus1_end_dt)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(
        act_data, contains_dicts_with_entries([
            expected_act_data_before_migration_minus2,
            expected_act_data_before_migration_minus1,
            expected_act_data_month_minus2,
            expected_act_data_month_minus1]),
        'Сравниваем данные из акт с шаблоном'
    )


# --------------------------------------------------------------------------------------------------------------------
# Utils

def create_act(dt, client_id, contract_id, context):
    first_month_day, last_month_day = utils.Date.current_month_first_and_last_days(dt)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=PERSONAL_ACC_SUM, payment_dt=first_month_day)

    act_sum, act_sum_internal, act_sum_135, act_sum_internal_135 = create_completions(context, first_month_day,
                                                                                      client_id)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, last_month_day)
    return act_sum, act_sum_internal, act_sum_135, act_sum_internal_135


def create_completions(context, dt, client_id, coef=Decimal('1')):
    act_sum_internal = Decimal('0')
    act_sum_internal_135 = Decimal('0')
    act_sum = Decimal('0')
    act_sum_135 = Decimal('0')
    if context == CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED:
        act_sum_internal_135 += steps.SimpleApi.create_fake_tpt_data(
            context, FAKE_TPT_CLIENT_ID, FAKE_TPT_PERSON_ID,
            FAKE_TPT_CONTRACT_ID, dt,
            [
                {
                    'client_amount': PAYMENT_SUM_INT * coef,
                    'client_id': client_id,
                    'transaction_type': TransactionType.PAYMENT,
                    'internal': 1
                },
                {
                    'client_amount': REFUND_SUM_INT * coef,
                    'client_id': client_id,
                    'transaction_type': TransactionType.REFUND,
                    'internal': 1
                }
            ],
            sum_key='client_amount')
    else:
        order_dicts_tlog = [
            {
                'service_id': context.service.id,
                'amount': PAYMENT_SUM_DECOUP * coef / context.nds.koef_on_dt(dt),
                'type': SERVICE_TO_ORDER_TYPE[context.service],
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
                'last_transaction_id': 99
            },
            {
                'service_id': context.service.id,
                'amount': -REFUND_SUM_DECOUP * coef / context.nds.koef_on_dt(dt),
                'type': SERVICE_TO_ORDER_TYPE[context.service],
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
                'last_transaction_id': 100
            },
        ]
        tsteps.TaxiSteps.create_orders_tlog(client_id, order_dicts_tlog)
        act_sum += (PAYMENT_SUM_DECOUP - REFUND_SUM_DECOUP) * coef

    if Services.TAXI_CORP.id in context.contract_services:
        act_sum_135 += steps.SimpleApi.create_fake_tpt_data(
            context, FAKE_TPT_CLIENT_ID, FAKE_TPT_PERSON_ID,
            FAKE_TPT_CONTRACT_ID, dt,
            [
                {
                    'client_amount': PAYMENT_SUM * coef,
                    'client_id': client_id,
                    'transaction_type': TransactionType.PAYMENT
                },
                {
                    'client_amount': REFUND_SUM * coef,
                    'client_id': client_id,
                    'transaction_type': TransactionType.REFUND
                }
            ],
            sum_key='client_amount')

    return act_sum, act_sum_internal, act_sum_135, act_sum_internal_135


def create_partner_oebs_completions(context, contract_id, client_id, dt,
                                    transaction_dt=None, accounting_period=None, coef=Decimal('1')):
    compls_dicts = [
        {
            'service_id': context.service.id,
            'last_transaction_id': 99,
            'amount': PAYMENT_SUM_DECOUP * coef,
            'product_id': SERVICE_CURRENCY_TO_PRODUCT_ID[(context.service, context.currency)],
            'dt': dt,
            'currency': context.currency.iso_code,
            'transaction_dt': transaction_dt or dt,
            'accounting_period': accounting_period,
        },
        {
            'service_id': context.service.id,
            'last_transaction_id': 100,
            'amount': PAYMENT_SUM_DECOUP * coef,
            'product_id': SERVICE_CURRENCY_TO_PRODUCT_ID[(context.service, context.currency)],
            'dt': dt,
            'currency': context.currency.iso_code,
            'transaction_dt': transaction_dt or dt,
            'accounting_period': accounting_period,
        },
    ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_dicts)
    act_sum = PAYMENT_SUM_DECOUP * 2 * coef
    return act_sum
