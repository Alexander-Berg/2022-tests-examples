# -*- coding: utf-8 -*-

from copy import deepcopy
from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import btestlib.reporter as reporter
from balance.balance_steps import new_taxi_steps as steps
from balance.balance_steps.acts_steps import ActsSteps
from balance.balance_steps.common_data_steps import CommonData
from balance.balance_steps.consume_steps import ConsumeSteps
from balance.balance_steps.contract_steps import ContractSteps
from balance.balance_steps.new_taxi_steps import DEFAULT_TAXI_CONTEXTS, DEFAULT_PARAMETRIZATION, \
    DEFAULT_TAXI_CONTEXTS_WITH_MARKS
from balance.balance_steps.other_steps import SharedBlocks
from balance.balance_steps.partner_steps import CommonPartnerSteps
from balance.features import Features, AuditFeatures
from btestlib import shared
from btestlib import utils
from btestlib.constants import Collateral
from btestlib.constants import Currencies
from btestlib.constants import InvoiceType
from btestlib.constants import PaymentType
from btestlib.constants import Services
from btestlib.constants import TaxiOrderType
from btestlib.data.defaults import TaxiNewPromo as Taxi
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT
from btestlib.matchers import contains_dicts_equal_to, equal_to, close_to

pytestmark = [
    reporter.feature(Features.TAXI)
]

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))
ACT_DT = utils.Date.get_last_day_of_previous_month()
COMPLETION_DT = utils.Date.first_day_of_month(ACT_DT)

DEFAULT_SERVICES = [Services.TAXI_111.id,
                    Services.TAXI_128.id,
                    Services.TAXI.id,
                    Services.UBER.id,
                    Services.UBER_ROAMING.id,
                    Services.TAXI_SVO.id]

DEFAULT_SERVICES_WO_CASH = [
    Services.TAXI_128.id,
    Services.TAXI.id,
    Services.UBER.id,
    Services.UBER_ROAMING.id
]


# Общий кейс:
# Открутки в первом месяце
# Открутки во втором месяце
# Генерация актов за первый месяц
# Открутки во втором месяце
# Генерация актов за второй месяц
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS_WITH_MARKS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_2_months_common_case(context, is_offer, shared_data):
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

    params = {'start_dt': month_migration_minus2_start_dt}

    ### СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты

    # _, _, first_day_of_2month_ago, last_day_of_2month_ago, \
    #     first_day_of_1month_ago, last_day_of_1month_ago = utils.Date.previous_three_months_start_end_dates()
    max_last_transactions_ids = []

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context,
            is_offer=is_offer,
            additional_params={'start_dt': month_migration_minus2_start_dt}
        )

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_1st_month_dt = month_migration_minus2_end_dt - relativedelta(days=1)
    orders_data_1st_month = steps.TaxiData.generate_default_orders_data(orders_1st_month_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data_1st_month)
    orders_data_1st_month_tlog = steps.TaxiData.generate_default_orders_data_tlog(orders_1st_month_dt, context.currency.iso_code,
                                                                                  transaction_ids_range=[1, 999])
    max_last_transactions_ids.insert(0, orders_data_1st_month_tlog.max_last_transaction_ids['all_services'])
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_1st_month_tlog)

    order_in_2nd_month_before_acts = {
        'dt': month_migration_minus2_end_dt + relativedelta(days=1),
        'payment_type': PaymentType.CASH,
        'order_type': TaxiOrderType.commission,
        'commission_sum': Decimal('125'),
        'currency': context.currency.iso_code,
    }
    steps.TaxiSteps.create_order(client_id, **order_in_2nd_month_before_acts)

    order_in_2nd_month_before_acts_tlog = {
        'service_id': Services.TAXI_111.id,
        'amount': Decimal('125'),
        'type': TaxiOrderType.commission,
        'dt': month_migration_minus2_end_dt + relativedelta(days=1),
        'transaction_dt': month_migration_minus2_end_dt + relativedelta(days=1),
        'currency': context.currency.iso_code,
        'last_transaction_id': 1000,
    }
    steps.TaxiSteps.create_order_tlog(client_id, **order_in_2nd_month_before_acts_tlog)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_migration_minus2_end_dt)
    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context,
                                            dt=month_migration_minus1_start_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_migration_minus2_end_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          end_dt=month_migration_minus1_start_dt)

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # TODO: подумать над способом проверки консьюмов без похода во вьюху с открутками
    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(
        contract_id, end_dt=month_migration_minus1_start_dt
    )

    for amount_data in amount_list:
        amount = close_to(amount_data['amount'], Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')

    # второй месяц
    orders_2nd_month_dt = month_migration_minus1_end_dt - relativedelta(days=1)
    orders_data_2nd_month = steps.TaxiData.generate_default_orders_data(orders_2nd_month_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data_2nd_month)
    orders_data_2nd_month_tlog = steps.TaxiData.generate_default_orders_data_tlog(orders_2nd_month_dt, context.currency.iso_code,
                                                                                  transaction_ids_range=[3001, 3999])
    max_last_transactions_ids.insert(0, orders_data_2nd_month_tlog.max_last_transaction_ids['all_services'])
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_2nd_month_tlog)

    orders_data_for_1st_month_in_2nd_month = steps.TaxiData.generate_default_orders_data(orders_1st_month_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data_for_1st_month_in_2nd_month)
    orders_data_for_1st_month_in_2nd_month_tlog = steps.TaxiData.generate_default_orders_data_tlog(orders_1st_month_dt, context.currency.iso_code,
                                                                                                   transaction_ids_range=[2001, 2999])
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_for_1st_month_in_2nd_month_tlog)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id,
                                            context, dt=month_migration_minus1_end_dt + relativedelta(days=1))
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_migration_minus1_end_dt, subt_previous_preiods_sums=True)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency)

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = \
        steps.TaxiSteps.get_completions_from_both_views(contract_id, end_dt=month_migration_minus1_end_dt + relativedelta(days=1))
    for amount_data in amount_list:
        amount = close_to(amount_data['amount'], Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')


    ### НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты


    compls_3rd_month_dt = month_minus2_end_dt - relativedelta(days=1)
    compls_data_3rd_month = steps.TaxiData.generate_default_oebs_compls_data(compls_3rd_month_dt,
                                                                             context.currency.iso_code,
                                                                             compls_3rd_month_dt)
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_3rd_month)

    product_id = steps.TaxiData.map_order_dict_to_product_tlog({
        'currency': context.currency.iso_code,
        'type': TaxiOrderType.commission,
        'service_id': Services.TAXI_111.id,
    })

    compl_in_4th_month_before_acts = {
        'service_id': Services.TAXI_111.id,
        'last_transaction_id': 99,
        'amount': Decimal('125'),
        'product_id': product_id,
        'dt': month_minus2_end_dt + relativedelta(days=1),
        'transaction_dt': month_minus2_end_dt + relativedelta(days=1),
        'currency':  context.currency.iso_code,
        'accounting_period': month_minus2_end_dt + relativedelta(days=1)
    }
    CommonPartnerSteps.create_partner_oebs_compl(contract_id, client_id, **compl_in_4th_month_before_acts)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_minus2_end_dt)
    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context,
                                            dt=month_minus1_start_dt, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_minus2_end_dt, subt_previous_preiods_sums=True,
                                        migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          end_dt=month_minus1_start_dt, migration_dt=migration_dt)

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # TODO: подумать над способом проверки консьюмов без похода во вьюху с открутками
    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(
        contract_id, end_dt=month_minus1_start_dt, migration_dt=migration_dt
    )

    for amount_data in amount_list:
        amount = close_to(amount_data['amount'], Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')

    # 4ый месяц
    compls_4th_month_dt = month_minus1_end_dt - relativedelta(days=1)
    compls_data_4th_month = steps.TaxiData.generate_default_oebs_compls_data(compls_4th_month_dt,
                                                                             context.currency.iso_code,
                                                                             compls_4th_month_dt)
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_4th_month)

    compls_data_for_3rd_month_in_4th_month = steps.TaxiData.generate_default_oebs_compls_data(
        compls_3rd_month_dt, context.currency.iso_code, compls_3rd_month_dt
    )
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_for_3rd_month_in_4th_month)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_minus1_end_dt)
    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id,
                                            context, dt=month_minus1_end_dt + relativedelta(days=1),
                                            migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_migration_minus1_end_dt,
                                        subt_previous_preiods_sums=True, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          migration_dt=migration_dt)


    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = \
        steps.TaxiSteps.get_completions_from_both_views(contract_id,
                                                        end_dt=month_minus1_end_dt + relativedelta(days=1),
                                                        migration_dt=migration_dt)
    for amount_data in amount_list:
        amount = close_to(amount_data['amount'], Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')


# проверяем сценарий, когда дата начала и окончания договора внутри одного месяца
# и договор не действует ни на первое, ни на последнее число месяца
# КЕЙС ДО МИГРАЦИИ, ПОСЛЕ МИГРАЦИИ МОЖНО ВЫПИЛИТЬ
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_contract_1_month_start_end_in_middle_of_month(context, is_offer, shared_data):
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

    start_dt = month_migration_minus1_start_dt + relativedelta(days=5)
    end_dt = start_dt + relativedelta(days=10)
    completion_dt = end_dt - relativedelta(days=1)

    max_last_transactions_ids = []

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context, is_offer=is_offer,
                                                                                              additional_params=
                                                                                                {'start_dt': start_dt})
        # создаем доп на расторжение
        params = {'CONTRACT2_ID': contract_id, 'FINISH_DT': end_dt,
                  'DT': end_dt.isoformat(), 'IS_FAXED': end_dt.isoformat(), 'IS_BOOKED': end_dt.isoformat()}
        ContractSteps.create_collateral(Collateral.TERMINATE, params)

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    orders_data_1st_month = steps.TaxiData.generate_default_orders_data(completion_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data_1st_month)
    orders_data_1st_month_tlog = steps.TaxiData.generate_default_orders_data_tlog(completion_dt, context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_1st_month_tlog)
    max_last_transactions_ids.insert(0, orders_data_1st_month_tlog.max_last_transaction_ids['all_services'])

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_migration_minus1_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, dt=month_migration_minus1_end_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency)

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(contract_id)
    for amount_data in amount_list:
        amount = close_to(amount_data['amount'], Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')


# проверяем сценарий, когда дата начала и окончания договора внутри одного месяца
# и договор не действует ни на первое, ни на последнее число месяца
# КЕЙС ПОСЛЕ МИГРАЦИИ
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
def test_contract_1_month_start_end_in_middle_of_month_oebs_compls(context, is_offer):
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

    start_dt = month_minus1_start_dt + relativedelta(days=5)
    end_dt = start_dt + relativedelta(days=10)
    completion_dt = month_minus1_start_dt

    client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
        context, is_offer=is_offer, additional_params={'start_dt': start_dt}
    )
    # создаем доп на расторжение
    params = {'CONTRACT2_ID': contract_id, 'FINISH_DT': end_dt,
              'DT': end_dt.isoformat(), 'IS_FAXED': end_dt.isoformat(), 'IS_BOOKED': end_dt.isoformat()}
    ContractSteps.create_collateral(Collateral.TERMINATE, params)

    compls_data_1st_month = steps.TaxiData.generate_default_oebs_compls_data(completion_dt,
                                                                             context.currency.iso_code,
                                                                             completion_dt)
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_1st_month)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_minus1_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, dt=month_minus1_end_dt,
                                            migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_minus1_end_dt, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          migration_dt=migration_dt)

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(contract_id,
                                                                                migration_dt=migration_dt)
    for amount_data in amount_list:
        amount = close_to(amount_data['amount'], Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')


# без откруток
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_wo_commission(context, is_offer, shared_data):
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context, is_offer=is_offer,
                                                                                              additional_params=
                                                                                                {'start_dt': CONTRACT_START_DT})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    CommonPartnerSteps.generate_partner_acts_fair(contract_id, ACT_DT)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, dt=ACT_DT)

    with reporter.step(u'Проверяем, что акт не создан для клиента: {}'.format(client_id)):
        act_data = ActsSteps.get_act_data_by_client(client_id)
        utils.check_that(act_data, empty(), u'Проверяем, что акт не создан')

    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          completion_absence_check=False)

    with reporter.step(u'Проверяем, что консьюмы не созданы для клиента: {}'.format(client_id)):
        consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
        utils.check_that(consume_data, empty(), u'Сравниваем данные из косьюмов с шаблоном')


# проверяем, что акт генерится, когда нет откруток по главному заказу для разных типов откруток
# КЕЙС ДО МИГРАЦИИ, ПОСЛЕ МИГРАЦИИ МОЖНо ВЫПИЛИТЬ
@pytest.mark.parametrize('payment_type, order_type, services, main_product_id',
                         [
                             (PaymentType.CASH, TaxiOrderType.childchair, DEFAULT_SERVICES,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['cash']),
                             (PaymentType.CASH, TaxiOrderType.hiring_with_car, DEFAULT_SERVICES,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['cash']),
                             (PaymentType.CARD, TaxiOrderType.commission, DEFAULT_SERVICES,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['cash']),
                             (PaymentType.CARD, TaxiOrderType.hiring_with_car, DEFAULT_SERVICES,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['cash']),
                             (PaymentType.CARD, TaxiOrderType.childchair, DEFAULT_SERVICES,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['cash']),
                             (PaymentType.CARD, TaxiOrderType.childchair, DEFAULT_SERVICES_WO_CASH,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['card']),
                             (PaymentType.CARD, TaxiOrderType.hiring_with_car, DEFAULT_SERVICES_WO_CASH,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['card'])],
                         ids=[
                             'Completion only of type childchair cash with 111,128 in contract',
                             'Completion only of type hiring_with_car cash with 111,128 in contract',
                             'Completion only of type order card with 111,128 in contract',
                             'Completion only of type hiring_with_car card with 111,128 in contract',
                             'Completion only of type childchair card with 111,128 in contract',
                             'Completion only of type childchair card with 128 in contract',
                             'Completion only of type hiring_with_car card with 128 in contract'])
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_no_commissions_in_main_order(payment_type, order_type, services, main_product_id, shared_data):
    context = TAXI_RU_CONTEXT

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

    commission_sum = Decimal('100.22')

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, additional_params={'start_dt': month_migration_minus1_start_dt, 'services': services}
        )

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    order_dict = {
        'dt': month_migration_minus1_start_dt,
        'payment_type': payment_type,
        'order_type': order_type,
        'commission_sum': commission_sum,
        'currency': context.currency.iso_code,
    }
    steps.TaxiSteps.create_order(client_id, **order_dict)

    order_dict_tlog = {
        'service_id': {PaymentType.CASH: Services.TAXI_111.id,
                       PaymentType.CARD: Services.TAXI_128.id}[payment_type],
        'amount': commission_sum,
        'type': order_type,
        'dt': month_migration_minus1_start_dt,
        'transaction_dt': month_migration_minus1_start_dt,
        'currency': context.currency.iso_code,
        'last_transaction_id': 100,
    }
    steps.TaxiSteps.create_order_tlog(client_id, **order_dict_tlog)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_migration_minus1_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, dt=month_migration_minus1_end_dt)

    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_migration_minus1_end_dt)
    _services = [Services.TAXI_128.id, ] if services == DEFAULT_SERVICES_WO_CASH else [Services.TAXI_111.id, Services.TAXI_128.id, ]
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency, contract_monetization_services=_services)

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(contract_id)
    for amount_data in amount_list:
        amount = close_to(amount_data['amount'], Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))
    # По главному заказу на конзюмах должно быть все в нулях
    # (изначально деньги зачислятся на него и польностью разнесутся на дочерние)
    expected_consume_data.append(CommonData.create_expected_consume_data(main_product_id,
                                                                         Decimal('0'),
                                                                         InvoiceType.PERSONAL_ACCOUNT,
                                                                         current_qty=Decimal('0'),
                                                                         act_qty=Decimal('0'),
                                                                         act_sum=Decimal('0'),
                                                                         completion_qty=Decimal('0'),
                                                                         completion_sum=Decimal('0')))

    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')


# проверяем, что акт генерится, когда нет откруток по главному заказу для разных типов откруток
# КЕЙС ДО МИГРАЦИИ, ПОСЛЕ МИГРАЦИИ МОЖНо ВЫПИЛИТЬ
@pytest.mark.parametrize('payment_type, order_type, services, main_product_id',
                         [
                             (PaymentType.CASH, TaxiOrderType.childchair, DEFAULT_SERVICES,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['cash']),
                             (PaymentType.CASH, TaxiOrderType.hiring_with_car, DEFAULT_SERVICES,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['cash']),
                             (PaymentType.CARD, TaxiOrderType.commission, DEFAULT_SERVICES,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['cash']),
                             (PaymentType.CARD, TaxiOrderType.hiring_with_car, DEFAULT_SERVICES,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['cash']),
                             (PaymentType.CARD, TaxiOrderType.childchair, DEFAULT_SERVICES,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['cash']),
                             (PaymentType.CARD, TaxiOrderType.childchair, DEFAULT_SERVICES_WO_CASH,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['card']),
                             (PaymentType.CARD, TaxiOrderType.hiring_with_car, DEFAULT_SERVICES_WO_CASH,
                              Taxi.CURRENCY_TO_PRODUCT[Currencies.RUB]['card'])],
                         ids=[
                             'Completion only of type childchair cash with 111,128 in contract',
                             'Completion only of type hiring_with_car cash with 111,128 in contract',
                             'Completion only of type order card with 111,128 in contract',
                             'Completion only of type hiring_with_car card with 111,128 in contract',
                             'Completion only of type childchair card with 111,128 in contract',
                             'Completion only of type childchair card with 128 in contract',
                             'Completion only of type hiring_with_car card with 128 in contract'])
def test_no_commissions_in_main_order_oebs_compls(payment_type, order_type, services, main_product_id):
    context = TAXI_RU_CONTEXT

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


    commission_sum = Decimal('100.22')
    client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
        context, additional_params={'start_dt': month_minus1_start_dt, 'services': services}
    )

    product_id = steps.TaxiData.map_order_dict_to_product_tlog({
        'currency': context.currency.iso_code,
        'type': order_type,
        'service_id': {PaymentType.CASH: Services.TAXI_111.id,
                       PaymentType.CARD: Services.TAXI_128.id}[payment_type],
    })

    compl_dict = {
        'service_id': {PaymentType.CASH: Services.TAXI_111.id,
                       PaymentType.CARD: Services.TAXI_128.id}[payment_type],
        'last_transaction_id': 99,
        'amount': commission_sum,
        'product_id': product_id,
        'dt': month_minus1_start_dt,
        'transaction_dt': month_minus1_start_dt,
        'currency': context.currency.iso_code,
        'accounting_period': month_minus1_start_dt
    }
    CommonPartnerSteps.create_partner_oebs_compl(contract_id, client_id, **compl_dict)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_minus1_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, dt=month_minus1_end_dt,
                                            migration_dt=migration_dt)

    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_minus1_end_dt, migration_dt=migration_dt)
    _services = [Services.TAXI_128.id, ] if services == DEFAULT_SERVICES_WO_CASH else [Services.TAXI_111.id, Services.TAXI_128.id, ]
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          contract_monetization_services=_services, migration_dt=migration_dt)

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(contract_id,
                                                                                migration_dt=migration_dt)
    for amount_data in amount_list:
        amount = close_to(amount_data['amount'], Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))
    # По главному заказу на конзюмах должно быть все в нулях
    # (изначально деньги зачислятся на него и польностью разнесутся на дочерние)
    expected_consume_data.append(CommonData.create_expected_consume_data(main_product_id,
                                                                         Decimal('0'),
                                                                         InvoiceType.PERSONAL_ACCOUNT,
                                                                         current_qty=Decimal('0'),
                                                                         act_qty=Decimal('0'),
                                                                         act_sum=Decimal('0'),
                                                                         completion_qty=Decimal('0'),
                                                                         completion_sum=Decimal('0')))

    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')


# Проверяем, что создается ЛС, если дата начала договора в будущем
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
def test_personal_account_creation(context, is_offer):
    start_dt = utils.Date.first_day_of_month() + relativedelta(months=1)

    client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context, is_offer=is_offer,
                                                                                          additional_params=
                                                                                            {'start_dt': start_dt})

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, dt=start_dt)


# Два договора на одного клиента, один на кэш (111), второй карту (128)
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, [DEFAULT_TAXI_CONTEXTS[0]], ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_separate_contracts_cash_and_card(context, is_offer, shared_data):
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

    orders_data_1st_month = steps.TaxiData.generate_default_orders_data(month_migration_minus1_start_dt,
                                                                        context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data_1st_month)
    orders_data_1st_month_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_start_dt,
                                                                                  context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_1st_month_tlog)

    # only cash (111) contract
    steps.TaxiSteps.generate_acts(client_id, cash_contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_taxi_invoice_data(client_id, cash_contract_id, person_id, context)
    steps.TaxiSteps.check_taxi_act_data(client_id, cash_contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, cash_contract_id, currency=context.currency,
                                          contract_monetization_services=[Services.TAXI_111.id, ])

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(cash_contract_id)
    for amount_data in amount_list:
        amount = amount_data['amount']
        # Для сервиса, которого нет в договоре, во вьюхе лежат нулевые суммы.
        if not amount:
            continue
        amount = close_to(amount, Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    consume_data = ConsumeSteps.get_consumes_sum_by_invoice_contract_id(cash_contract_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=cash_contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids,
                     equal_to([orders_data_1st_month_tlog.max_last_transaction_ids[Services.TAXI_111.id]]),
                     'Сравниваем last_transaction_id с ожидаемым')

    # only card (128) contract
    steps.TaxiSteps.generate_acts(client_id, card_contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_taxi_invoice_data(client_id, card_contract_id, person_id, context)
    steps.TaxiSteps.check_taxi_act_data(client_id, card_contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, card_contract_id, currency=context.currency,
                                          contract_monetization_services=[Services.TAXI_128.id,])

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(card_contract_id)
    for amount_data in amount_list:
        amount = amount_data['amount']
        # Для сервиса, которого нет в договоре, во вьюхе лежат нулевые суммы.
        if not amount:
            continue
        amount = close_to(amount, Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    consume_data = ConsumeSteps.get_consumes_sum_by_invoice_contract_id(card_contract_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=card_contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids,
                     equal_to([orders_data_1st_month_tlog.max_last_transaction_ids[Services.TAXI_128.id]]),
                     'Сравниваем last_transaction_id с ожидаемым')


    # Второй месяц - кейс после миграции на открутки из ОЕБС

    compls_data_3rd_month = steps.TaxiData.generate_default_oebs_compls_data(month_minus1_start_dt,
                                                                             context.currency.iso_code,
                                                                             month_minus1_start_dt)
    CommonPartnerSteps.create_partner_oebs_completions(cash_contract_id, client_id, compls_data_3rd_month)
    CommonPartnerSteps.create_partner_oebs_completions(card_contract_id, client_id, compls_data_3rd_month)

    # only cash (111) contract
    steps.TaxiSteps.generate_acts(client_id, cash_contract_id, month_minus1_end_dt)
    steps.TaxiSteps.check_taxi_invoice_data(client_id, cash_contract_id, person_id, context,
                                            migration_dt=migration_dt, oebs_compls_service_id=Services.TAXI_111.id)
    steps.TaxiSteps.check_taxi_act_data(client_id, cash_contract_id, month_minus1_end_dt,
                                        subt_previous_preiods_sums=True,
                                        migration_dt=migration_dt,
                                        oebs_compls_service_id=Services.TAXI_111.id)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, cash_contract_id, currency=context.currency,
                                          contract_monetization_services=[Services.TAXI_111.id, ],
                                          migration_dt=migration_dt,
                                          oebs_compls_service_id=Services.TAXI_111.id)

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(
        cash_contract_id, migration_dt=migration_dt, oebs_compls_service_id=Services.TAXI_111.id)
    for amount_data in amount_list:
        amount = amount_data['amount']
        # Для сервиса, которого нет в договоре, во вьюхе лежат нулевые суммы.
        if not amount:
            continue
        amount = close_to(amount, Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    consume_data = ConsumeSteps.get_consumes_sum_by_invoice_contract_id(cash_contract_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')


    # only card (128) contract
    steps.TaxiSteps.generate_acts(client_id, card_contract_id, month_minus1_end_dt)
    steps.TaxiSteps.check_taxi_invoice_data(client_id, card_contract_id, person_id, context,
                                            migration_dt=migration_dt, oebs_compls_service_id=Services.TAXI_128.id)
    steps.TaxiSteps.check_taxi_act_data(client_id, card_contract_id, month_minus1_end_dt,
                                        subt_previous_preiods_sums=True,
                                        migration_dt=migration_dt, oebs_compls_service_id=Services.TAXI_128.id)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, card_contract_id, currency=context.currency,
                                          contract_monetization_services=[Services.TAXI_128.id, ],
                                          migration_dt=migration_dt, oebs_compls_service_id=Services.TAXI_128.id)

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(card_contract_id,
                                                                                migration_dt=migration_dt,
                                                                                oebs_compls_service_id=Services.TAXI_128.id)
    for amount_data in amount_list:
        amount = amount_data['amount']
        # Для сервиса, которого нет в договоре, во вьюхе лежат нулевые суммы.
        if not amount:
            continue
        amount = close_to(amount, Decimal('0.02'))
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    consume_data = ConsumeSteps.get_consumes_sum_by_invoice_contract_id(card_contract_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')
