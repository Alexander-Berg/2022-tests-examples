# -*- coding: utf-8 -*-

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import btestlib.reporter as reporter
from balance.balance_steps import new_taxi_steps as steps
from balance.balance_steps.acts_steps import ActsSteps
from balance.balance_steps.consume_steps import ConsumeSteps
from balance.balance_steps.contract_steps import ContractSteps
from balance.balance_steps.common_data_steps import CommonData
from balance.balance_steps.partner_steps import CommonPartnerSteps
from balance.balance_steps.order_steps import OrderSteps
from balance.balance_steps.other_steps import SharedBlocks
from balance.features import Features, AuditFeatures
from balance.utils import get_config_item
from btestlib import shared
from btestlib import utils
from btestlib.constants import Currencies
from btestlib.constants import InvoiceType
from btestlib.constants import PaymentType
from btestlib.constants import Services
from btestlib.constants import TaxiOrderType
from btestlib.data.defaults import TaxiNewPromo as Taxi
from btestlib.matchers import close_to
from btestlib.matchers import contains_dicts_equal_to
from btestlib.matchers import contains_dicts_with_entries
from btestlib.matchers import equal_to
from balance.balance_steps.new_taxi_steps import DEFAULT_TAXI_CONTEXTS, DEFAULT_PARAMETRIZATION, \
    DEFAULT_TAXI_CONTEXTS_WITH_MARKS
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT, TAXI_YANDEX_GO_SRL_CONTEXT

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

expected_conusme_amount_fields = ['act_qty', 'act_sum', 'completion_qty', 'completion_sum', 'current_qty']

# Общий кейс:
# Оплата на ЛС
# Открутки в первом месяце и втором месяце
# Генерация актов за первый месяц
# Открутки во втором месяце, дополнительные в первом месяце
# Генерация актов за второй месяц
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION + ', payment_sum, parametrization_name',
                        utils.flatten_parametrization(
                            DEFAULT_TAXI_CONTEXTS_WITH_MARKS,
                            [
                                [Decimal('1'), 'PAYMENT_SUM_1'],
                                [Decimal('100000'), 'PAYMENT_SUM_100000'],
                            ]
                        ),
                        ids=lambda c, _1, _2, parametrization_name: c.name + '_' + parametrization_name
)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_2_months_common_case(context, is_offer, payment_sum, parametrization_name, shared_data):
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


    ### СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты

    max_last_transactions_ids = []

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus2_start_dt}
        )

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.pay_to_personal_account(payment_sum, contract_id)

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
        'dt': month_migration_minus1_start_dt,
        'transaction_dt': month_migration_minus1_start_dt,
        'currency': context.currency.iso_code,
        'last_transaction_id': 1000,
    }
    steps.TaxiSteps.create_order_tlog(client_id, **order_in_2nd_month_before_acts_tlog)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_migration_minus2_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, payment_amount=payment_sum,
                                            dt=month_migration_minus1_start_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_migration_minus2_end_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          payment_amount=payment_sum, end_dt=month_migration_minus2_end_dt)

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')

    # подготавливаем ожидаемые данные для заявок
    main_product_id = steps.TaxiSteps.get_main_taxi_product(context.currency)
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(contract_id,
                                                                                end_dt=month_migration_minus1_start_dt)
    for amount_data in amount_list:
        amount = amount_data['amount']
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    # дополнительный заказ не должен попасть в открутки
    amount_list, total_amount = \
        steps.TaxiSteps.get_completions_from_both_views(contract_id, end_dt=month_migration_minus1_start_dt)
    main_product_completions, = filter(lambda amount_data: amount_data['product_id'] == main_product_id, amount_list)
    main_product_completions_in_expected_data, = filter(lambda amount_data: amount_data['service_code'] == main_product_id,
                                                        expected_consume_data)
    main_product_completions_in_expected_data['completion_qty'] = main_product_completions['amount']
    main_product_completions_in_expected_data['completion_sum'] = main_product_completions['amount']
    main_product_completions_in_expected_data['current_qty'] = main_product_completions['amount']

    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    for consume_dict in expected_consume_data:
        for amount_field in expected_conusme_amount_fields:
            consume_dict[amount_field] = close_to(consume_dict[amount_field], Decimal('0.02'))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')

    # второй месяц
    orders_2nd_month_dt = month_migration_minus1_end_dt
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

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, payment_amount=payment_sum,
                                            dt=month_migration_minus1_end_dt + relativedelta(days=1))
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_migration_minus1_end_dt, subt_previous_preiods_sums=True)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency, payment_amount=payment_sum)

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
        amount = amount_data['amount']
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
    for consume_dict in expected_consume_data:
        for amount_field in expected_conusme_amount_fields:
            consume_dict[amount_field] = close_to(consume_dict[amount_field], Decimal('0.02'))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')


    # новая логика, переход на открутки из ОЕБС

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
        'currency': context.currency.iso_code,
        'accounting_period': month_minus2_end_dt + relativedelta(days=1)
    }
    CommonPartnerSteps.create_partner_oebs_compl(contract_id, client_id, **compl_in_4th_month_before_acts)


    steps.TaxiSteps.generate_acts(client_id, contract_id, month_minus2_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, payment_amount=payment_sum,
                                            dt=month_minus1_start_dt, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_minus2_end_dt, subt_previous_preiods_sums=True,
                                        migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          payment_amount=payment_sum, end_dt=month_minus2_end_dt,
                                          migration_dt=migration_dt)

    # подготавливаем ожидаемые данные для заявок
    main_product_id = steps.TaxiSteps.get_main_taxi_product(context.currency)
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(contract_id,
                                                                                end_dt=month_minus1_start_dt,
                                                                                migration_dt=migration_dt)
    for amount_data in amount_list:
        amount = amount_data['amount']
        expected_consume_data.append(CommonData.create_expected_consume_data(amount_data['product_id'],
                                                                             amount,
                                                                             InvoiceType.PERSONAL_ACCOUNT,
                                                                             current_qty=amount,
                                                                             act_qty=amount,
                                                                             act_sum=amount,
                                                                             completion_qty=amount,
                                                                             completion_sum=amount))

    # дополнительный заказ не должен попасть в открутки
    amount_list, total_amount = \
        steps.TaxiSteps.get_completions_from_both_views(contract_id, end_dt=month_minus1_start_dt,
                                                        migration_dt=migration_dt)
    main_product_completions, = filter(lambda amount_data: amount_data['product_id'] == main_product_id,
                                       amount_list)
    main_product_completions_in_expected_data, = filter(
        lambda amount_data: amount_data['service_code'] == main_product_id,
        expected_consume_data)
    main_product_completions_in_expected_data['completion_qty'] = main_product_completions['amount']
    main_product_completions_in_expected_data['completion_sum'] = main_product_completions['amount']
    main_product_completions_in_expected_data['current_qty'] = main_product_completions['amount']

    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    expected_consume_data.sort(key=lambda k: (k['completion_qty'], k['service_code']))
    for consume_dict in expected_consume_data:
        for amount_field in expected_conusme_amount_fields:
            consume_dict[amount_field] = close_to(consume_dict[amount_field], Decimal('0.02'))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')

    # второй месяц
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

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, payment_amount=payment_sum,
                                            dt=month_minus1_end_dt + relativedelta(days=1), migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_minus1_end_dt,
                                        subt_previous_preiods_sums=True, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          payment_amount=payment_sum, migration_dt=migration_dt)


    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = \
        steps.TaxiSteps.get_completions_from_both_views(contract_id,
                                                        end_dt=month_minus1_end_dt + relativedelta(days=1),
                                                        migration_dt=migration_dt)
    for amount_data in amount_list:
        amount = amount_data['amount']
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
    for consume_dict in expected_consume_data:
        for amount_field in expected_conusme_amount_fields:
            consume_dict[amount_field] = close_to(consume_dict[amount_field], Decimal('0.02'))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')


# КЕЙС ДО МИГРАЦИИ В ОЕБС, в будущем удалить
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_wo_payments(context, is_offer, shared_data):
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


    max_last_transactions_ids = []

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)
    orders_data_1st_month = steps.TaxiData.generate_default_orders_data(month_migration_minus1_start_dt,
                                                                        context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data_1st_month)

    orders_data_1st_month_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_start_dt,
                                                                                  context.currency.iso_code,
                                                                                  transaction_ids_range=[1, 999])
    max_last_transactions_ids.insert(0, orders_data_1st_month_tlog.max_last_transaction_ids['all_services'])
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_1st_month_tlog)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_migration_minus1_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_migration_minus1_end_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency)

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки - столько должно открутиться и заактиться:
    amount_list, total_amount = \
        steps.TaxiSteps.get_completions_from_both_views(contract_id)
    for amount_data in amount_list:
        amount = amount_data['amount']
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
    for consume_dict in expected_consume_data:
        for amount_field in expected_conusme_amount_fields:
            consume_dict[amount_field] = close_to(consume_dict[amount_field], Decimal('0.02'))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')


# КЕЙС ПОСЛЕ МИГРАЦИИ
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_wo_payments_oebs_compls(context, is_offer, shared_data):
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

    client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
        context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_minus1_start_dt})

    compls_data_1st_month = steps.TaxiData.generate_default_oebs_compls_data(month_minus1_start_dt,
                                                                             context.currency.iso_code,
                                                                             month_minus1_start_dt)
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_1st_month)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_minus1_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_minus1_end_dt, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency, migration_dt=migration_dt)

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки - столько должно открутиться и заактиться:
    amount_list, total_amount = \
        steps.TaxiSteps.get_completions_from_both_views(contract_id, migration_dt=migration_dt)
    for amount_data in amount_list:
        amount = amount_data['amount']
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
    for consume_dict in expected_consume_data:
        for amount_field in expected_conusme_amount_fields:
            consume_dict[amount_field] = close_to(consume_dict[amount_field], Decimal('0.02'))
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
            context, is_postpay=0, additional_params={'start_dt': month_migration_minus1_start_dt, 'services': services})

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
        amount = amount_data['amount']
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
    for consume_dict in expected_consume_data:
        for amount_field in expected_conusme_amount_fields:
            consume_dict[amount_field] = close_to(consume_dict[amount_field], Decimal('0.02'))
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
        context, is_postpay=0, additional_params={'start_dt': month_minus1_start_dt, 'services': services}
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
def test_personal_account_creation_in_future(context, is_offer):
    start_dt = utils.Date.nullify_time_of_date(datetime.now()) + relativedelta(days=1)
    client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context, is_postpay=0,
                                                                                          is_offer=is_offer,
                                                                                          additional_params=
                                                                                            {'start_dt': start_dt})

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context)


# Атрибут "не генерить акты" в договоре
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_no_acts(context, is_offer, shared_data):
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
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt})
        steps.TaxiSteps.set_no_acts_attribute(contract_id)
    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.pay_to_personal_account(Decimal('100000'), contract_id)

    orders_data_1st_month = steps.TaxiData.generate_default_orders_data(month_migration_minus1_start_dt,
                                                                        context.currency.iso_code)
    steps.TaxiSteps.create_orders(client_id, orders_data_1st_month)
    orders_data_1st_month_tlog = steps.TaxiData.generate_default_orders_data_tlog(month_migration_minus1_start_dt,
                                                                                  context.currency.iso_code)
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_1st_month_tlog)

    compls_data_3rd_month = steps.TaxiData.generate_default_oebs_compls_data(month_minus1_start_dt,
                                                                             context.currency.iso_code,
                                                                             month_minus1_start_dt)
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_3rd_month)

    act_dt = month_minus1_end_dt or month_migration_minus1_end_dt
    steps.TaxiSteps.generate_acts(client_id, contract_id, act_dt, force_month_proc=False)

    with reporter.step(u'Проверяем, что акт не создан для клиента: {}'.format(client_id)):
        act_data = ActsSteps.get_act_data_by_client(client_id)
        utils.check_that(act_data, empty(), u'Проверяем, что акт не создан')



# По договору есть платеж, но нет комиссии
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_with_payments_wo_commission(context, is_offer, shared_data):
    payment_sum = Decimal('1000')
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(context, is_postpay=0,
                                                                                              is_offer=is_offer,
                                                                                              additional_params=
                                                                                                {'start_dt': CONTRACT_START_DT})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.pay_to_personal_account(payment_sum, contract_id)

    steps.TaxiSteps.generate_acts(client_id, contract_id, ACT_DT, force_month_proc=False)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, payment_amount=payment_sum)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          payment_amount=payment_sum,
                                          completion_absence_check=False)

    # Конзюмы не должны формироваться при обработке предоплаты
    consume_data = ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    assert len(consume_data) == 0

    with reporter.step(u'Проверяем, что акт не создан для клиента: {}'.format(client_id)):
        act_data = ActsSteps.get_act_data_by_client(client_id)
        utils.check_that(act_data, empty(), u'Проверяем, что акт не создан')


@pytest.mark.parametrize(DEFAULT_PARAMETRIZATION, DEFAULT_TAXI_CONTEXTS, ids=lambda c, o: c.name)
@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_split_subsidy_promo_greater_then_commission(context, is_offer,  shared_data):
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

    payment_sum = Decimal('1000')
    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=is_offer, additional_params={'start_dt': month_migration_minus1_start_dt})

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    steps.TaxiSteps.pay_to_personal_account(payment_sum, contract_id)

    cash_commission_sum = Decimal('500')
    cash_promocode_sum = Decimal('100')
    subsidy_sum = Decimal('100')
    card_commission_sum = Decimal('100')
    card_promocode_sum = Decimal('200')

    cash_dict = {
         'payment_type': PaymentType.CASH,
         'order_type': TaxiOrderType.commission,
         'commission_sum': cash_commission_sum,
         'promocode_sum': cash_promocode_sum,
         'subsidy_sum': 0,
         'currency': context.currency.iso_code,
    }
    cash_product_id = steps.TaxiData.map_order_dict_to_product(cash_dict)

    subsidy_dict = {
         'payment_type': PaymentType.CASH,
         'commission_sum': 0,
         'order_type': TaxiOrderType.subsidy,
         'promocode_sum': 0,
         'subsidy_sum': subsidy_sum,
         'currency': context.currency.iso_code
    }
    # продукт субсидий тот же, что и для кеша

    cash_dict_tlog = {
        'service_id': Services.TAXI_111.id,
        'amount': cash_commission_sum,
        'type': TaxiOrderType.commission,
        'currency': context.currency.iso_code,
        'last_transaction_id': 1,
    }
    promo_dict_tlog = {
        'service_id': Services.TAXI_111.id,
        'amount': cash_promocode_sum,
        'type': TaxiOrderType.promocode_tlog,
        'currency': context.currency.iso_code,
        'last_transaction_id': 2,
    }
    subsidy_dict_tlog = {
        'service_id': Services.TAXI_111.id,
        'amount': subsidy_sum,
        'type': TaxiOrderType.subsidy_tlog,
        'currency': context.currency.iso_code,
        'last_transaction_id': 3,
    }

    card_dict = {
         'payment_type': PaymentType.CARD,
         'order_type': TaxiOrderType.commission,
         'commission_sum': card_commission_sum,
         'promocode_sum': card_promocode_sum,
         'subsidy_sum': 0,
         'currency': context.currency.iso_code
    }
    card_product_id = steps.TaxiData.map_order_dict_to_product(card_dict)

    card_dict_tlog = {
        'service_id': Services.TAXI_128.id,
        'amount': card_commission_sum,
        'type': TaxiOrderType.commission,
        'currency': context.currency.iso_code,
        'last_transaction_id': 4,
    }
    promo_card_dict_tlog = {
        'service_id': Services.TAXI_128.id,
        'amount': card_promocode_sum,
        'type': TaxiOrderType.promocode_tlog,
        'currency': context.currency.iso_code,
        'last_transaction_id': 5,
    }

    orders_data = [cash_dict, subsidy_dict, card_dict]

    [order_dict.update({'dt': month_migration_minus1_start_dt}) for order_dict in orders_data]
    steps.TaxiSteps.create_orders(client_id, orders_data)

    orders_data_tlog = [cash_dict_tlog, promo_dict_tlog, subsidy_dict_tlog, card_dict_tlog, promo_card_dict_tlog]
    [order_dict_tlog.update({'dt': month_migration_minus1_start_dt, 'transaction_dt': month_migration_minus1_start_dt})
     for order_dict_tlog in orders_data_tlog]
    steps.TaxiSteps.create_orders_tlog(client_id, orders_data_tlog)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month_migration_minus1_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, payment_amount=payment_sum)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month_migration_minus1_end_dt)

    expected_completion_qty = \
        (cash_commission_sum + card_commission_sum) * context.nds.koef_on_dt(month_migration_minus1_start_dt) \
        - cash_promocode_sum - card_promocode_sum - subsidy_sum
    expected_completion_qty_tlog = \
        (cash_commission_sum + card_commission_sum) * context.nds.koef_on_dt(month_migration_minus1_start_dt) \
        - cash_promocode_sum - card_promocode_sum - subsidy_sum
    expected_completion_qty += expected_completion_qty_tlog
    expected_completion_qty = close_to(expected_completion_qty, Decimal('0.02'))

    expected_orders = []
    expected_orders.append(CommonData.create_expected_order_data(Services.TAXI_111.id, cash_product_id, contract_id,
                                                                 consume_sum=expected_completion_qty,
                                                                 consume_qty=expected_completion_qty,
                                                                 completion_qty=expected_completion_qty))
    order_data = OrderSteps.get_order_data_by_client(client_id)
    utils.check_that(order_data, contains_dicts_with_entries(expected_orders, same_length=False),
                     u'Сравниваем данные из заказов с шаблоном')

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []
    expected_consume_data.append(CommonData.create_expected_consume_data(cash_product_id,
                                                                         payment_sum,
                                                                         InvoiceType.PERSONAL_ACCOUNT,
                                                                         current_qty=payment_sum,
                                                                         act_qty=expected_completion_qty,
                                                                         act_sum=expected_completion_qty,
                                                                         completion_qty=expected_completion_qty,
                                                                         completion_sum=expected_completion_qty))

    tlog_notches = steps.TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    utils.check_that(last_transaction_ids, equal_to([5]),
                     'Сравниваем last_transaction_id с ожидаемым')


@pytest.mark.shared(block=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS)
def test_act_on_finished_contract(shared_data):
    context = TAXI_RU_CONTEXT
    payment_sum = Decimal('0')
    migration_dt = datetime(2020, 11, 1)

    month1_start_dt, month1_end_dt, \
    month2_start_dt, month2_end_dt,\
    month3_start_dt, month3_end_dt = \
        utils.Date.previous_three_months_start_end_dates()

    # Подготовка данных ДО общего блока (ОБ)
    cache_vars = ['client_id', 'person_id', 'contract_id']
    with shared.SharedBefore(shared_data=shared_data, cache_vars=cache_vars) as before:
        before.validate()
        client_id, person_id, contract_id, _ = ContractSteps.create_partner_contract(
            context, is_postpay=0, is_offer=1, additional_params={'start_dt': month1_start_dt,
                                                                  'finish_dt': month2_start_dt}
        )

    # Общий блок - длительные операции
    SharedBlocks.refresh_taxi_contract_mviews(shared_data=shared_data, before=before)

    compls_1st_month_dt = month1_end_dt - relativedelta(days=1)
    compls_data_1st_month = steps.TaxiData.generate_default_oebs_compls_data(compls_1st_month_dt,
                                                                             context.currency.iso_code,
                                                                             compls_1st_month_dt)
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_1st_month)

    compls_2nd_month_dt = month1_end_dt + relativedelta(days=1)
    compls_data_for_2nd_month_in_1st_month = steps.TaxiData.generate_default_oebs_compls_data(
        compls_2nd_month_dt, context.currency.iso_code, compls_2nd_month_dt
    )
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_for_2nd_month_in_1st_month)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month1_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, payment_amount=payment_sum,
                                            dt=month2_start_dt, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month1_end_dt, subt_previous_preiods_sums=True,
                                        migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          payment_amount=payment_sum, end_dt=month1_end_dt,
                                          migration_dt=migration_dt)

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = steps.TaxiSteps.get_completions_from_both_views(contract_id,
                                                                                end_dt=month2_start_dt,
                                                                                migration_dt=migration_dt)
    for amount_data in amount_list:
        amount = amount_data['amount']
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
    for consume_dict in expected_consume_data:
        for amount_field in expected_conusme_amount_fields:
            consume_dict[amount_field] = close_to(consume_dict[amount_field], Decimal('0.02'))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')

    # второй месяц
    compls_2nd_month_dt = month2_end_dt - relativedelta(days=1)
    compls_data_2nd_month = steps.TaxiData.generate_default_oebs_compls_data(compls_2nd_month_dt,
                                                                             context.currency.iso_code,
                                                                             compls_2nd_month_dt)

    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_2nd_month)

    compls_data_for_1st_month_in_2nd_month = steps.TaxiData.generate_default_oebs_compls_data(
        compls_1st_month_dt, context.currency.iso_code, compls_1st_month_dt
    )
    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_for_1st_month_in_2nd_month)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month2_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, payment_amount=payment_sum,
                                            dt=month3_start_dt, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month2_end_dt,
                                        subt_previous_preiods_sums=True, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          payment_amount=payment_sum, migration_dt=migration_dt)


    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = \
        steps.TaxiSteps.get_completions_from_both_views(contract_id,
                                                        end_dt=month3_start_dt,
                                                        migration_dt=migration_dt)
    for amount_data in amount_list:
        amount = amount_data['amount']
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
    for consume_dict in expected_consume_data:
        for amount_field in expected_conusme_amount_fields:
            consume_dict[amount_field] = close_to(consume_dict[amount_field], Decimal('0.02'))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')

    # третий месяц
    compls_3rd_month_dt = month3_end_dt - relativedelta(days=1)
    compls_data_3rd_month = steps.TaxiData.generate_default_oebs_compls_data(compls_3rd_month_dt,
                                                                             context.currency.iso_code,
                                                                             compls_3rd_month_dt)

    CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_data_3rd_month)

    steps.TaxiSteps.generate_acts(client_id, contract_id, month3_end_dt)

    steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id, context, payment_amount=payment_sum,
                                            dt=month3_end_dt + relativedelta(days=1), migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, month3_end_dt,
                                        subt_previous_preiods_sums=True, migration_dt=migration_dt)
    steps.TaxiSteps.check_taxi_order_data(context, client_id, contract_id, currency=context.currency,
                                          payment_amount=payment_sum, migration_dt=migration_dt)

    # подготавливаем ожидаемые данные для заявок
    expected_consume_data = []

    # Получаем открутки на конец месяца - столько должно открутиться и заактиться:
    amount_list, total_amount = \
        steps.TaxiSteps.get_completions_from_both_views(contract_id,
                                                        end_dt=month3_end_dt + relativedelta(days=1),
                                                        migration_dt=migration_dt)
    for amount_data in amount_list:
        amount = amount_data['amount']
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
    for consume_dict in expected_consume_data:
        for amount_field in expected_conusme_amount_fields:
            consume_dict[amount_field] = close_to(consume_dict[amount_field], Decimal('0.02'))
    utils.check_that(consume_data, contains_dicts_equal_to(expected_consume_data),
                     u'Сравниваем данные из конзюмов с шаблоном')
