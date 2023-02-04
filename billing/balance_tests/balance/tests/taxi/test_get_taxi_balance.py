# -*- coding: utf-8 -*-

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from balance.balance_steps import ContractSteps, CommonPartnerSteps
from balance.balance_steps import new_taxi_steps as steps
from balance.balance_steps.new_taxi_steps import DEFAULT_TAXI_CONTEXTS, DEFAULT_PARAMETRIZATION, \
    DEFAULT_TAXI_CONTEXTS_WITH_MARKS
from balance.balance_steps.other_steps import SharedBlocks
from balance.features import Features, AuditFeatures
from btestlib import shared
from btestlib import utils
from btestlib.constants import Export, TaxiOrderType, OEBSOperationType
from btestlib.constants import Services
from btestlib.matchers import close_to
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    reporter.feature(Features.TAXI)
]

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))
COMPLETION_DT = utils.Date.first_day_of_month()
PROCESS_DT = COMPLETION_DT + relativedelta(seconds=1)
INSERT_NETTING_DT = COMPLETION_DT + relativedelta(seconds=27)

COMPLETION_DT_LAST_DAY_OF_PREV_MONTH = utils.Date.get_last_day_of_previous_month()
ACT_DT = COMPLETION_DT_LAST_DAY_OF_PREV_MONTH + relativedelta(seconds=1)

INSERT_NETTING_AMOUNT = Decimal('1')

MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT, _, _ = \
    utils.Date.previous_three_months_start_end_dates()

# --------------------------------------------
# Postpay


# Постоплата, все виды откруток (включая корретктировки), комиссии больше чем промокодов и субсидий по каждому продукту.
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS_WITH_MARKS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_postpay_common_case(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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


    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context,
                                                                                     is_offer=is_offer,
                                                                                     additional_params=
                                                                                       {'start_dt': month_migration_minus1_start_dt})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.check_postpay_taxi_balance(client_id, contract_id, context, commission=Decimal('0'))
    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_start_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)
    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_start_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    compls_data_oebs = None

    compls_data_oebs = steps.TaxiData.generate_default_oebs_compls_data(
        month_minus1_start_dt, context.currency.iso_code, month_minus1_start_dt
    )
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_oebs)
    steps.TaxiSteps.check_postpay_taxi_balance(client_id, contract_id, context, orders_data=orders_data,
                                               orders_data_tlog=orders_data_tlog, compls_data_oebs=compls_data_oebs)


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, [DEFAULT_TAXI_CONTEXTS[0]], ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_last_netting_dt_postpay(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    insert_netting_dt = month_migration_minus1_start_dt + relativedelta(days=1)
    netting_pct = Decimal('100.01')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_offer=is_offer,
            additional_params={'start_dt': month_migration_minus1_start_dt, 'netting': 1, 'netting_pct': netting_pct}
        )

    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)
    pa_id, pa_eid = steps.TaxiSteps.get_completions_taxi_invoice(contract_id)

    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_start_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)

    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_start_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data(orders_data, context.nds)
    total_amount = sum([amount for _, amount in amount_by_products.items()])

    amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, context.nds)
    total_amount += sum([amount for _, amount in amount_by_products.items()])


    compls_data_oebs = steps.TaxiData.generate_default_oebs_compls_data(
        month_minus1_start_dt, context.currency.iso_code, month_minus1_start_dt
    )
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_oebs)
    total_amount += sum([compl_dict['amount'] for compl_dict in compls_data_oebs])

    total_amount = close_to(total_amount, Decimal('0.02'))

    taxi_balance = steps.TaxiSteps.get_taxi_balance([contract_id])
    expected_taxi_balance = steps.TaxiData.create_expected_postpay_taxi_balance(client_id, contract_id,
                                                                                total_amount, currency=context.currency)
    expected_taxi_balance[0]['NettingLastDt'] = None
    utils.check_that(taxi_balance, contains_dicts_with_entries(expected_taxi_balance),
                     u"Проверяем, что взаимозачет еще не производился")

    steps.TaxiSteps.create_cash_payment_fact(pa_eid, INSERT_NETTING_AMOUNT,
                                             insert_netting_dt, OEBSOperationType.INSERT_NETTING)

    taxi_balance = steps.TaxiSteps.get_taxi_balance([contract_id])
    expected_taxi_balance[0]['NettingLastDt'] = None
    utils.check_that(taxi_balance, contains_dicts_with_entries(expected_taxi_balance),
                     u"Проверяем дату последнего взаимозачета до PROCESS_PAYMENTS")

    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, pa_id)
    taxi_balance = steps.TaxiSteps.get_taxi_balance([contract_id])
    expected_taxi_balance[0]['NettingLastDt'] = insert_netting_dt.isoformat()
    utils.check_that(taxi_balance, contains_dicts_with_entries(expected_taxi_balance),
                     u"Проверяем дату последнего взаимозачета после PROCESS_PAYMENTS")


# --------------------------------------------
# Prepay

# без оплат, предоплата, все виды откруток (включая корретктировки), комиссии больше чем промокодов и субсидий
# по каждому продукту.
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS_WITH_MARKS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_prepay_wo_payments_common_commission(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt}
        )
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=Decimal('0'), currency=context.currency)

    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_start_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)
    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_start_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data(orders_data, context.nds)
    charge = sum([amount for _, amount in amount_by_products.items()])
    amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, context.nds)
    charge += sum([amount for _, amount in amount_by_products.items()])

    compls_data_oebs = steps.TaxiData.generate_default_oebs_compls_data(
        month_minus1_start_dt, context.currency.iso_code, month_minus1_start_dt
    )
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_oebs)
    charge += sum([compl_dict['amount'] for compl_dict in compls_data_oebs])

    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=-charge, charge=charge, currency=context.currency)


# C оплатами, предоплата, все виды откруток (включая корретктировки), комиссии больше чем промокодов и субсидий
# по каждому продукту.
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_prepay_with_payments_common_commission(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    payment_sum = Decimal('100')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=Decimal('0'), currency=context.currency)

    steps.TaxiSteps.pay_to_personal_account(payment_sum, contract_id)
    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=payment_sum, currency=context.currency)

    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_start_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)
    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_start_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data(orders_data, context.nds)
    charge = sum([amount for _, amount in amount_by_products.items()])
    amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, context.nds)
    charge += sum([amount for _, amount in amount_by_products.items()])

    compls_data_oebs = steps.TaxiData.generate_default_oebs_compls_data(
        month_minus1_start_dt, context.currency.iso_code, month_minus1_start_dt
    )
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_oebs)
    charge += sum([compl_dict['amount'] for compl_dict in compls_data_oebs])

    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=payment_sum - charge, charge=charge,
                                              currency=context.currency)


# C оплатами, предоплата, без откруток
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_prepay_with_payments_wo_commission(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt}
        )
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=Decimal('0'), currency=context.currency)

    payment_sum = Decimal('100')
    steps.TaxiSteps.pay_to_personal_account(payment_sum, contract_id)
    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id,  balance=payment_sum, currency=context.currency)


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C02_1_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, [DEFAULT_TAXI_CONTEXTS[0]], ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_last_netting_dt_prepay(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    insert_netting_dt = month_migration_minus1_start_dt + relativedelta(days=1)
    netting_pct = Decimal('100.01')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'contract_id', 'person_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer,
            additional_params={'start_dt': month_migration_minus1_start_dt, 'netting': 1, 'netting_pct': netting_pct}
        )
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    pa_id, pa_eid = steps.TaxiSteps.get_completions_taxi_invoice(contract_id)

    taxi_balance = steps.TaxiSteps.get_taxi_balance([contract_id])
    expected_taxi_balance = steps.TaxiData.create_expected_prepay_taxi_balance(client_id, contract_id,
                                                                               balance=0,
                                                                               charge=0,
                                                                               balance_close_to=Decimal('0.02'),
                                                                               currency=context.currency)
    expected_taxi_balance[0]['NettingLastDt'] = None
    utils.check_that(taxi_balance, contains_dicts_with_entries(expected_taxi_balance),
                     u"Проверяем, что взаимозачет еще не производился")

    steps.TaxiSteps.create_cash_payment_fact(pa_eid, INSERT_NETTING_AMOUNT,
                                             insert_netting_dt, OEBSOperationType.INSERT_NETTING)

    taxi_balance = steps.TaxiSteps.get_taxi_balance([contract_id])
    expected_taxi_balance[0]['NettingLastDt'] = None
    utils.check_that(taxi_balance, contains_dicts_with_entries(expected_taxi_balance),
                     u"Проверяем дату последнего взаимозачета до PROCESS_PAYMENTS")

    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, pa_id)

    taxi_balance = steps.TaxiSteps.get_taxi_balance([contract_id])
    expected_taxi_balance[0]['NettingLastDt'] = insert_netting_dt.isoformat()
    expected_taxi_balance[0]['Balance'] = str(INSERT_NETTING_AMOUNT)
    utils.check_that(taxi_balance, contains_dicts_with_entries(expected_taxi_balance),
                     u"Проверяем дату последнего взаимозачета после PROCESS_PAYMENTS")


# Учет остатков предыдущего месяца с субсидиями и промокодами
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_promo_and_subsidy_with_prev_month_reminder(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    completion_dt_1 = month_migration_minus2_start_dt
    completion_dt_2 = month_minus1_start_dt or month_migration_minus1_start_dt

    payment_amount_1 = Decimal('70.3')
    payment_amount_2 = Decimal('80.3')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus2_start_dt}
        )

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=Decimal('0'),
                                              currency=context.currency)

    with reporter.step(u'Первый месяц'):
        steps.TaxiSteps.pay_to_personal_account(payment_amount_1, contract_id)
        orders_data = steps.TaxiData.generate_default_orders_data(completion_dt_1, context.currency.iso_code)
        steps.TaxiSteps.create_orders(client_id, orders_data)
        orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(completion_dt_1, context.currency.iso_code)
        steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

        amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data(orders_data, context.nds)
        charge_month_1 = sum([amount for _, amount in amount_by_products.items()])
        amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, context.nds)
        charge_month_1 += sum([amount for _, amount in amount_by_products.items()])

        steps.TaxiSteps.generate_acts(client_id, contract_id, utils.Date.last_day_of_month(completion_dt_1))
        second_month_leftover = payment_amount_1 - charge_month_1

    with reporter.step(u'Второй месяц'):
        steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id,
                                                  balance=second_month_leftover,
                                                  charge=Decimal('0'),
                                                  currency=context.currency)

        steps.TaxiSteps.pay_to_personal_account(payment_amount_2, contract_id)
        steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id,
                                                  balance=payment_amount_2 + second_month_leftover,
                                                  charge=Decimal('0'),
                                                  currency=context.currency)

        orders_data = steps.TaxiData.generate_default_orders_data(completion_dt_1, context.currency.iso_code)
        steps.TaxiSteps.create_orders(client_id, orders_data)
        orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(completion_dt_1, context.currency.iso_code)
        steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

        amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data(orders_data, context.nds)
        charge_month_2 = sum([amount for _, amount in amount_by_products.items()])
        amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, context.nds)
        charge_month_2 += sum([amount for _, amount in amount_by_products.items()])

        compls_data_oebs = steps.TaxiData.generate_default_oebs_compls_data(
            month_minus1_start_dt, context.currency.iso_code, month_minus1_start_dt
        )
        CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_oebs)
        charge_month_2 += sum([compl_dict['amount'] for compl_dict in compls_data_oebs])

        total_leftover = payment_amount_1 + payment_amount_2 - charge_month_1 - charge_month_2
        steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id,
                                                  balance=total_leftover,
                                                  charge=charge_month_2,
                                                  currency=context.currency)


# комиссия должна расчитаться нулевой, т.к. вся вычтена субсидиями и промокодами
# КЕЙС ДО МИГРАЦИИ В ОЕБС
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION + ', subsidy_sum, promocode_sum, parametrization_name',
                         utils.flatten_parametrization(DEFAULT_TAXI_CONTEXTS,
                                       [
                                          [Decimal('10000'), Decimal('0'), 'GREATER_SUBSIDY_NO_PROMO'],
                                          [Decimal('0'), Decimal('10000'), 'NO_SUBSIDY_GREATER_PROMO'],
                                       ]
                         ),
                         ids=lambda c, _1, _2, _3, parametrization_name: c.name + '_' + parametrization_name
)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_subsidy_promo_greater_then_commission(context, is_offer, subsidy_sum, promocode_sum, parametrization_name,
                                               shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt}
        )
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=Decimal('0'), currency=context.currency)

    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_start_dt, context.currency.iso_code)
    # сохраним знак, актуально для субсидий (отрицательные должны обнулиться)
    sign = lambda x: (1, -1)[x < 0]
    for order_dict in orders_data:
        if order_dict['promocode_sum']:
            order_dict['promocode_sum'] = sign(order_dict['promocode_sum']) * promocode_sum
        if order_dict['subsidy_sum']:
            order_dict['subsidy_sum'] = sign(order_dict['subsidy_sum']) * subsidy_sum

    steps.TaxiSteps.create_orders(client_id, orders_data)
    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=Decimal('0'),
                                              charge=Decimal('0'), currency=context.currency)

# КЕЙС ДО МИГРАЦИИ НА ОТКРУТКИ ОЕБС
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_subsidy_promo_greater_then_commission_tlog(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    subvention_amount = Decimal('10000')
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt}
        )
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=Decimal('0'), currency=context.currency)

    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_start_dt, context.currency.iso_code)
    orders_data_tlog_template = [
        {'service_id': Services.TAXI_111.id,
         'amount': subvention_amount,
         'type': TaxiOrderType.subsidy_tlog,
         },
        {'service_id': Services.TAXI_128.id,
         'amount': subvention_amount,
         'type': TaxiOrderType.subsidy_tlog,
         },
        {'service_id': Services.TAXI_111.id,
         'amount': subvention_amount / Decimal('-2'),
         'type': TaxiOrderType.subsidy_tlog,
         },
        {'service_id': Services.TAXI_128.id,
         'amount': subvention_amount / Decimal('-2'),
         'type': TaxiOrderType.subsidy_tlog,
         },
        # открутки с типом promocode
        {'service_id': Services.TAXI_111.id,
         'amount': subvention_amount,
         'type': TaxiOrderType.promocode_tlog,
         },
        {'service_id': Services.TAXI_128.id,
         'amount': subvention_amount,
         'type': TaxiOrderType.promocode_tlog,
         },
        {'service_id': Services.TAXI_111.id,
         'amount': subvention_amount / Decimal('-2'),
         'type': TaxiOrderType.promocode_tlog,
         },
        {'service_id': Services.TAXI_128.id,
         'amount': subvention_amount / Decimal('-2'),
         'type': TaxiOrderType.promocode_tlog,
         },
    ]
    orders_data_tlog = []
    for order_dict in orders_data_tlog_template:
        order_dict = order_dict.copy()
        order_dict.update({'dt': month_migration_minus1_start_dt, 'transaction_dt': month_migration_minus1_start_dt, 'currency': context.currency.iso_code,
                           'last_transaction_id': None})
        orders_data_tlog.append(order_dict)

    steps.TaxiSteps.create_orders(client_id, orders_data)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=Decimal('0'),
                                              charge=Decimal('0'), currency=context.currency)

# Два договора на одного клиента, один на кэш (111), второй карту (128) постоплата
# КЕЙС ДО МИГРАЦИИ НА ОТКРУТКИ ИЗ ОЕБС, ИЗ ОЕБС ПРИХОДЯТ ОТКРУТКИ В РАЗРЕЗЕ ДОГОВОРОВ
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, [DEFAULT_TAXI_CONTEXTS[0]], ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_separate_postpay_contracts_cash_and_card(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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


    services_cash = [Services.TAXI_111.id, ]
    services_card = [sid for sid in context.contract_services if sid not in (Services.TAXI_111.id, Services.TAXI_SVO.id)]

    cache_vars = ['client_id', 'person_id', 'cash_contract_id', 'card_contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, cash_contract_id, _ = ContractSteps.create_partner_contract(
            context, is_offer=is_offer,
            additional_params={'start_dt': month_migration_minus1_start_dt, 'services': services_cash}
        )

        _, _, card_contract_id, _ = ContractSteps.create_partner_contract(
            context, client_id=client_id, person_id=person_id, is_offer=is_offer,
            additional_params={'start_dt': month_migration_minus1_start_dt, 'services': services_card}
        )

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.check_postpay_taxi_balance(client_id, card_contract_id, context, commission=Decimal('0'))
    steps.TaxiSteps.check_postpay_taxi_balance(client_id, cash_contract_id, context, commission=Decimal('0'))

    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_start_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)

    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_start_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    _, cash_total_commission = steps.TaxiSteps.get_completions_from_view(cash_contract_id)
    _, cash_total_commission_tlog = steps.TaxiSteps.get_completions_from_view_tlog(cash_contract_id)

    steps.TaxiSteps.check_postpay_taxi_balance(client_id, cash_contract_id, context,
                                               commission=cash_total_commission + cash_total_commission_tlog)

    _, card_total_commission = steps.TaxiSteps.get_completions_from_view(card_contract_id)
    _, card_total_commission_tlog = steps.TaxiSteps.get_completions_from_view_tlog(card_contract_id)

    steps.TaxiSteps.check_postpay_taxi_balance(client_id, card_contract_id, context,
                                               commission=card_total_commission + card_total_commission_tlog)

# AFTER_ACTS BELOW ==========================================================================================


@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_postpay_commission_before_acts(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt}
        )
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_end_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)
    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_end_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_postpay_taxi_balance(client_id, contract_id, context, commission=Decimal('0'))


@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_prepay_wo_payments_common_commission_before_acts(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt}
        )
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_end_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)
    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_end_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)
    amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data(orders_data, context.nds)
    charge = sum([amount for _, amount in amount_by_products.items()])
    amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, context.nds)
    charge += sum([amount for _, amount in amount_by_products.items()])

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=-charge, currency=context.currency)


@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_prepay_with_payments_common_commission_before_acts(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    payment_sum = Decimal('100')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.pay_to_personal_account(payment_sum, contract_id)

    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_end_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)
    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_end_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)
    amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data(orders_data, context.nds)
    charge = sum([amount for _, amount in amount_by_products.items()])
    amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, context.nds)
    charge += sum([amount for _, amount in amount_by_products.items()])

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=payment_sum - charge, currency=context.currency)


# Учет остатков предыдущего месяца с субсидиями и промокодами
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_promo_and_subsidy_with_prev_month_reminder_commission_before_acts(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    completion_dt_1 = month_migration_minus2_start_dt
    completion_dt_2 = month_minus1_start_dt or month_migration_minus1_start_dt

    payment_amount_1 = Decimal('70.3')
    payment_amount_2 = Decimal('80.3')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus2_start_dt})
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=Decimal('0'),
                                              currency=context.currency)

    with reporter.step(u'Первый месяц'):
        steps.TaxiSteps.pay_to_personal_account(payment_amount_1, contract_id)
        orders_data = steps.TaxiData.generate_default_orders_data(completion_dt_1, context.currency.iso_code)
        steps.TaxiSteps.create_orders(client_id, orders_data)
        orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(completion_dt_1, context.currency.iso_code)
        steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)
        amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data(orders_data, context.nds)
        charge_month_1 = sum([amount for _, amount in amount_by_products.items()])
        amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, context.nds)
        charge_month_1 += sum([amount for _, amount in amount_by_products.items()])
        steps.TaxiSteps.generate_acts(client_id, contract_id, utils.Date.last_day_of_month(completion_dt_1))
        second_month_leftover = payment_amount_1 - charge_month_1

    with reporter.step(u'Второй месяц'):
        steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id,
                                                  balance=second_month_leftover,
                                                  charge=Decimal('0'),
                                                  currency=context.currency)

        steps.TaxiSteps.pay_to_personal_account(payment_amount_2, contract_id)
        steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id,
                                                  balance=payment_amount_2 + second_month_leftover,
                                                  charge=Decimal('0'),
                                                  currency=context.currency)

        orders_data = steps.TaxiData.generate_default_orders_data(completion_dt_1, context.currency.iso_code)
        steps.TaxiSteps.create_orders(client_id, orders_data)
        orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(completion_dt_1, context.currency.iso_code)
        steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

        amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data(orders_data, context.nds)
        charge_month_2 = sum([amount for _, amount in amount_by_products.items()])
        amount_by_products = steps.TaxiData.create_expected_completions_data_by_orders_data_tlog(orders_data_tlog, context.nds)
        charge_month_2 += sum([amount for _, amount in amount_by_products.items()])

        compls_data_oebs = steps.TaxiData.generate_default_oebs_compls_data(
            month_minus1_start_dt, context.currency.iso_code, month_minus1_start_dt
        )
        CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_oebs)
        charge_month_2 += sum([compl_dict['amount'] for compl_dict in compls_data_oebs])

        total_leftover = payment_amount_1 + payment_amount_2 - charge_month_1 - charge_month_2
        steps.TaxiSteps.generate_acts(client_id, contract_id, utils.Date.last_day_of_month(completion_dt_2))

        steps.TaxiSteps.check_prepay_taxi_balance(client_id, contract_id, balance=total_leftover,
                                                  currency=context.currency, close_to_delta=Decimal('0.02'))


# Два договора на одного клиента, один на кэш (111), второй карту (128)
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, [DEFAULT_TAXI_CONTEXTS[0]], ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_separate_contracts_cash_and_card_commission(context, is_offer, shared_data):
    migration_params = CommonPartnerSteps.get_partner_oebs_compls_migration_params(context.migration_alias)
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

    # Подготовка данных ДО общего блока (ОБ)

    services_cash = [Services.TAXI_111.id, ]
    services_card = [sid for sid in context.contract_services if sid not in (Services.TAXI_111.id, Services.TAXI_SVO.id)]

    cache_vars = ['client_id', 'person_id', 'cash_contract_id', 'card_contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()

        client_id, person_id, cash_contract_id, _ = ContractSteps.create_partner_contract(
            context, is_offer=is_offer,
            additional_params={'start_dt': month_migration_minus1_start_dt, 'services': services_cash}
        )

        _, _, card_contract_id, _ = ContractSteps.create_partner_contract(
            context, client_id=client_id, person_id=person_id, is_offer=is_offer,
            additional_params={'start_dt': month_migration_minus1_start_dt, 'services': services_card}
        )

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data = steps.TaxiData.generate_default_orders_data(month_migration_minus1_end_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data)
    orders_data_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_end_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    steps.TaxiSteps.generate_acts(client_id, cash_contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.generate_acts(client_id, card_contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_postpay_taxi_balance(client_id, cash_contract_id, context, commission=Decimal('0'))
    steps.TaxiSteps.check_postpay_taxi_balance(client_id, card_contract_id, context, commission=Decimal('0'))
