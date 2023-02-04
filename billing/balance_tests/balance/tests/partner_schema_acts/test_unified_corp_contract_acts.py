# -*- coding: utf-8 -*-

from collections import defaultdict
from datetime import datetime, timedelta
from decimal import Decimal as D
from dateutil.relativedelta import relativedelta

import pytest
from hamcrest import empty

import balance.balance_db as db

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as tsteps, client_steps as csteps
from balance.balance_steps import contract_steps
import balance.tests.promocode_new.promocode_commons as promo_steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import Collateral, TransactionType, Services, InvoiceType, \
    Products, CorpEdaOrderType, DriveB2BOrderType, PromocodeClass, Export, ContractSubtype
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries, equal_to, \
    has_entries_casted, contains_dicts_equal_to
from btestlib.data import person_defaults
from btestlib.data.partner_contexts import UNIFIED_CORP_CONTRACT_SERVICES, \
    UNIFIED_CORP_CONTRACT_CONTEXT, UNIFIED_CORP_CONTRACT_CONTEXT_WITH_ZAXI, \
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, \
    FOOD_CORP_CONTEXT, DRIVE_B2B_CONTEXT, ZAXI_RU_CONTEXT

FAKE_TAXI_CLIENT_ID = 1111
FAKE_TAXI_PERSON_ID = 2222
FAKE_TAXI_CONTRACT_ID = 3333
APX_SUM = D('0')
DISCOUNT_BONUS_SUM = D('0')

# В тестах сделана костыльная параметризация для того, чтобы сохранять порядок применения параметризации
# (пайтест применяет порядок параметризациии рандомно, поэтому генерятся разные навания тестов, тесты аудита,
# проверяющие названия, падают

postpay_params = [
    [0, 'prepay'],
    [1, 'postpay'],
]

paysum_params = [
    [D('10000'), 'pay-sum-10000'],
    [D('0'), 'no-pay-sum'],
]

contexts = [
    pytest.mark.smoke(CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP),
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    FOOD_CORP_CONTEXT,
    DRIVE_B2B_CONTEXT,
]

contexts_with_unified = contexts+[pytest.mark.smoke(UNIFIED_CORP_CONTRACT_CONTEXT)]


@utils.memoize
def get_product_mapping():
    return tsteps.TaxiSteps.get_product_mapping()


@utils.memoize
def get_ua_root_product_mapping():
    with reporter.step(u"Получаем маппинг из service_id + currency + order_type -> product_id"):
        query = "SELECT * FROM bo.t_partner_product"
        rows = db.balance().execute(query)

        return {(row['service_id'], row['currency_iso_code']): row['product_id']
                for row in rows
                if row['unified_account_root'] == 1}


@utils.memoize
def get_product_id_service_mapping():
    return {product_id: service_id for (service_id, _, _), product_id in get_product_mapping().items()}


def create_corp_taxi_completions_tt(context, corp_contract_id, corp_client_id, dt,
                                    payment_sum=D('0'), refund_sum=D('0')):
    product_mapping = get_product_mapping()
    product_id = product_mapping[Services.TAXI_CORP.id, context.currency.iso_code, 'main']
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, dt,
                                         [{'client_amount': payment_sum,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.PAYMENT},
                                          {'client_amount': refund_sum,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.REFUND}
                                          ])
    amount_w_nds = payment_sum - refund_sum
    return amount_w_nds, {product_id: payment_sum - refund_sum}


def create_corp_taxi_completions_tlog(context, corp_contract_id, corp_client_id, dt,
                                      payment_sum=D('0'), refund_sum=D('0')):
    sum_w_nds = D('0')
    sums_by_products = defaultdict(lambda: D('0'))
    order_dicts_tlog = []
    for idx, (order_type, product_id) in enumerate(
            tsteps.CORP_TAXI_CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP[context.currency]):
        order_dicts_tlog += [
            {'service_id': Services.TAXI_CORP_CLIENTS.id,
             'amount': payment_sum / context.nds.koef_on_dt(dt),
             'type': order_type,
             'dt': dt,
             'transaction_dt': dt,
             'currency': context.currency.iso_code},
            {'service_id': Services.TAXI_CORP_CLIENTS.id,
             'amount': -refund_sum / context.nds.koef_on_dt(dt),
             'type': order_type,
             'dt': dt,
             'transaction_dt': dt,
             'currency': context.currency.iso_code},
        ]
        amount_w_nds = payment_sum - refund_sum
        sum_w_nds += amount_w_nds
        sums_by_products[product_id] += amount_w_nds

    tsteps.TaxiSteps.create_orders_tlog(corp_client_id, order_dicts_tlog)
    return sum_w_nds, sums_by_products


def create_food_corp_completions_tlog(context, corp_contract_id, corp_client_id, dt,
                                      payment_sum=D('0'), refund_sum=D('0')):
    product_mapping = get_product_mapping()
    product_id = product_mapping[Services.FOOD_CORP.id, context.currency.iso_code, CorpEdaOrderType.MAIN]
    orders = [
        {
            'service_id': Services.FOOD_CORP.id,
            'amount': payment_sum / context.nds.koef_on_dt(dt),
            'type': CorpEdaOrderType.MAIN,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
        },
        {
            'service_id': Services.FOOD_CORP.id,
            'amount': -refund_sum / context.nds.koef_on_dt(dt),
            'type': CorpEdaOrderType.MAIN,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
        },
    ]
    tsteps.TaxiSteps.create_orders_tlog(corp_client_id, orders)
    amount_w_nds = payment_sum - refund_sum
    return amount_w_nds, {product_id: payment_sum - refund_sum}


def create_drive_b2b_completions_tlog(context, corp_contract_id, corp_client_id, dt,
                                      payment_sum=D('0'), refund_sum=D('0')):
    product_mapping = get_product_mapping()
    product_id = product_mapping[Services.DRIVE_B2B.id, context.currency.iso_code, DriveB2BOrderType.MAIN]
    order_dicts_tlog = [
        {
            'service_id': Services.DRIVE_B2B.id,
            'amount': payment_sum / context.nds.koef_on_dt(dt),
            'type': DriveB2BOrderType.MAIN,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
        },
        {
            'service_id': Services.DRIVE_B2B.id,
            'amount': -refund_sum / context.nds.koef_on_dt(dt),
            'type': DriveB2BOrderType.MAIN,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
        },
    ]
    tsteps.TaxiSteps.create_orders_tlog(corp_client_id, order_dicts_tlog)
    amount_w_nds = payment_sum - refund_sum
    return amount_w_nds, {product_id: payment_sum - refund_sum}


def create_corp_taxi_partner_oebs_completions(context, corp_contract_id, corp_client_id, dt,
                                              payment_sum=D('0'), refund_sum=D('0')):
    sum_w_nds = D('0')
    sums_by_products = defaultdict(lambda: D('0'))
    compls_dicts = []
    for idx, (order_type, product_id) in enumerate(
            tsteps.CORP_TAXI_CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP[context.currency]):
        compls_dicts += [
            {
                'service_id': Services.TAXI_CORP_CLIENTS.id,
                'amount': (payment_sum - refund_sum),
                'product_id': product_id,
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
                'accounting_period': dt
            },
        ]
        amount_w_nds = payment_sum - refund_sum
        sum_w_nds += amount_w_nds
        sums_by_products[product_id] += amount_w_nds
    steps.CommonPartnerSteps.create_partner_oebs_completions(corp_contract_id, corp_client_id, compls_dicts)
    return sum_w_nds, sums_by_products


def create_food_corp_partner_oebs_completions(context, corp_contract_id, corp_client_id, dt,
                                              payment_sum=D('0'), refund_sum=D('0')):
    compls_dicts = [
        {
            'service_id': Services.FOOD_CORP.id,
            'amount': payment_sum,
            'product_id': Products.FOOD_CORP_RUB.id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        },
        {
            'service_id': Services.FOOD_CORP.id,
            'amount': -refund_sum,
            'product_id': Products.FOOD_CORP_RUB.id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        }
    ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(corp_contract_id, corp_client_id, compls_dicts)
    sum_w_nds = payment_sum - refund_sum
    return sum_w_nds, {Products.FOOD_CORP_RUB.id: sum_w_nds}


def create_drive_b2b_partner_oebs_completions(context, corp_contract_id, corp_client_id, dt,
                                              payment_sum=D('0'), refund_sum=D('0')):
    compls_dicts = [
        {
            'service_id': Services.DRIVE_B2B.id,
            'amount': payment_sum,
            'product_id': Products.DRIVE_B2B_RUB.id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        },
        {
            'service_id': Services.DRIVE_B2B.id,
            'amount': -refund_sum,
            'product_id': Products.DRIVE_B2B_RUB.id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        }
    ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(corp_contract_id, corp_client_id, compls_dicts)
    sum_w_nds = payment_sum - refund_sum
    return sum_w_nds, {Products.DRIVE_B2B_RUB.id: sum_w_nds}


def prepare_expected_balance_data(context, contract_id, client_id, invoice_eid, is_postpay, personal_account_pay_sum,
                                  total_compls_sum, cur_month_charge, act_sum):
    if is_postpay:
        expected_balance = {
            'ClientID': client_id,
            'ContractID': contract_id,
            'Currency': context.currency.iso_code,
            'PersonalAccountExternalID': invoice_eid,
            'ReceiptSum': personal_account_pay_sum,
            'CommissionToPay': cur_month_charge,
            'ActSum': act_sum,
            'BonusLeft': D('0'),
            'CurrMonthBonus': D('0'),
            'TotalCharge': total_compls_sum,
        }
    else:
        expected_balance = {
            'ClientID': client_id,
            'ContractID': contract_id,
            'PersonalAccountExternalID': invoice_eid,
            'ReceiptSum': personal_account_pay_sum,
            'DiscountBonusSum': DISCOUNT_BONUS_SUM,
            'TotalCharge': total_compls_sum,
            'ApxSum': APX_SUM,
            'Currency': context.currency.iso_code,
            'Balance': personal_account_pay_sum - total_compls_sum,
            'CurrMonthCharge': cur_month_charge,
            'ActSum': act_sum,
            'BonusLeft': D('0'),
            'CurrMonthBonus': D('0'),
        }
    return expected_balance


def create_change_services_collateral(contract_id, col_dt, services):
    params = {
        'SERVICES': services,
        'DT': col_dt,
        'SIGN': 1
    }
    return contract_steps.ContractSteps.create_collateral_real(contract_id, Collateral.CHANGE_SERVICES, params)


def create_change_services_zapravki_collateral(contract_id, col_dt, services, link_contract_id):
    params = {
        'DT': col_dt,
        'SIGN': 1,
        'LINK_CONTRACT_ID': link_contract_id,
        'SERVICES': services
    }
    return contract_steps.ContractSteps.create_collateral_real(contract_id, Collateral.CHANGE_SERVICES_ZAPRAVKI, params)


def check_balance(descr, context, contract_id, client_id, invoice_eid, is_postpay, personal_account_pay_sum,
                  total_compls_sum, cur_month_charge, act_sum):
    partner_balance = steps.PartnerSteps.get_partner_balance(context.service, [contract_id])
    expected_balance = prepare_expected_balance_data(context, contract_id, client_id, invoice_eid,
                                                     is_postpay, personal_account_pay_sum,
                                                     total_compls_sum, cur_month_charge, act_sum)
    utils.check_that(partner_balance, contains_dicts_with_entries([expected_balance]), descr)


def filter_taxi_corp_service(context, services):
    if Services.TAXI_CORP.id not in context.contract_services:
        services = filter(lambda sid: sid != Services.TAXI_CORP.id, services)
    return services


def get_appendable_services(context):
    max_index = -1
    for i, sid in enumerate(UNIFIED_CORP_CONTRACT_SERVICES):
        if sid in context.contract_services:
            max_index = max(i, max_index)
    services = context.contract_services + UNIFIED_CORP_CONTRACT_SERVICES[max_index + 1:]
    return filter_taxi_corp_service(context, services)


def merge_sums_dicts(result_defaultdict, dict_for_merge):
    for k, v in dict_for_merge.items():
        result_defaultdict[k] += v
    return result_defaultdict


def get_expected_fast_balance_data(apx_sum, receipt_sum, discount_bonus_sum, contract_id, currency):
    return {"ApxSum": apx_sum,
            "Currency": currency,
            "ReceiptSum": receipt_sum,
            "DiscountBonusSum": discount_bonus_sum,
            "ContractID": contract_id}


service_completions_before_oebs_func_mapping = {
    Services.TAXI_CORP_CLIENTS.id: create_corp_taxi_completions_tlog,
    Services.TAXI_CORP.id: create_corp_taxi_completions_tt,
    Services.FOOD_CORP.id: create_food_corp_completions_tlog,
    Services.DRIVE_B2B.id: create_drive_b2b_completions_tlog,
}

service_completions_after_oebs_func_mapping = {
    Services.TAXI_CORP_CLIENTS.id: create_corp_taxi_partner_oebs_completions,
    Services.TAXI_CORP.id: create_corp_taxi_completions_tt,
    Services.FOOD_CORP.id: create_food_corp_partner_oebs_completions,
    Services.DRIVE_B2B.id: create_drive_b2b_partner_oebs_completions,
}


@reporter.feature(Features.ZAXI, Features.UNIFIED_CORP_CONTRACT_CONTEXT_WITH_ZAXI, Features.UNIFIED_CORP_CONTRACT)
def test_orders_unified_with_zaxi_fuel_stations():
    context_zaxi = ZAXI_RU_CONTEXT
    context_unified_with_zaxi = UNIFIED_CORP_CONTRACT_CONTEXT_WITH_ZAXI

    _, _, start_dt_1, end_dt_1, start_dt_2, end_dt_2 = utils.Date.previous_three_months_start_end_dates()

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context_zaxi.person_type.code, {'is-partner': '0'},
                                         inn_type=person_defaults.InnType.RANDOM)

    contracts = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
    utils.check_that(contracts, empty(),
                     'На клиента {} уже заведены договора!'.format(client_id))

    _, _, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context_zaxi, client_id=client_id, person_id=person_id,
                                                    additional_params={'start_dt': start_dt_1, })

    contracts = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
    utils.check_that(contracts, not empty(),
                     'На клиента {} не завелась заправочная оферта!'.format(client_id))
    assert len(contracts) == 1 and contracts[0]['ID'] == contract_id, 'На клиента {} не завелась заправочная оферта!'

    _, _, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context_unified_with_zaxi, client_id=client_id, person_id=person_id,
                                                    is_postpay=True,
                                                    additional_params={'start_dt': start_dt_1,
                                                                       'link_contract_id': contract_id})

    contracts = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
    utils.check_that(contracts, not empty(),
                     'На клиента {} не завелся единый договор!'.format(client_id))
    assert len(contracts) == 2 and (contracts[0]['ID'] == corp_contract_id or contracts[1]['ID'] == corp_contract_id),\
        'На клиента {} не завелся единый договор!'

    orders_len = steps.CommonPartnerSteps.client_orders(client_id, service_id=1171)

    utils.check_that(orders_len, not empty(),
                     'На клиента {} нет заказов по 1171 сервису'.format(client_id))

    context_unified = UNIFIED_CORP_CONTRACT_CONTEXT

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context_zaxi.person_type.code, {'is-partner': '0'},
                                         inn_type=person_defaults.InnType.RANDOM)

    contracts = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
    utils.check_that(contracts, empty(),
                     'На клиента {} уже заведены договора!'.format(client_id))

    _, _, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context_zaxi, client_id=client_id, person_id=person_id,
                                                    additional_params={'start_dt': start_dt_1, })

    contracts = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
    utils.check_that(contracts, not empty(),
                     'На клиента {} не завелась заправочная оферта!'.format(client_id))
    assert len(contracts) == 1 and contracts[0]['ID'] == contract_id, 'На клиента {} не завелась заправочная оферта!'

    _, _, corp_contract_id_2, _ = \
        steps.ContractSteps.create_partner_contract(context_unified, client_id=client_id, person_id=person_id,
                                                    is_postpay=True, additional_params={'start_dt': start_dt_1})

    contracts = steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
    utils.check_that(contracts, not empty(),
                     'На клиента {} не завелся единый договор!'.format(client_id))
    assert len(contracts) == 2 and (contracts[0]['ID'] == corp_contract_id_2 or contracts[1]['ID'] == corp_contract_id_2), \
        'На клиента {} не завелся единый договор!'

    orders_len = steps.CommonPartnerSteps.client_orders(client_id, service_id=1171)
    utils.check_that(orders_len, empty(),
                     'На клиента {} почему-то уже есть заказ по 1171 сервису'.format(client_id))

    create_change_services_zapravki_collateral(corp_contract_id_2, start_dt_2,
                                               UNIFIED_CORP_CONTRACT_CONTEXT_WITH_ZAXI.contract_services, contract_id)
    orders_len = steps.CommonPartnerSteps.client_orders(client_id, service_id=1171)
    utils.check_that(orders_len, not empty(),
                     'На клиента {} нет заказов по 1171 сервису'.format(client_id))

    context = CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context_zaxi, additional_params={'start_dt': start_dt_1, })
    _, _, corp_contract_id_2, _ = \
        steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                    is_postpay=True, additional_params={'start_dt': start_dt_1})

    create_change_services_collateral(corp_contract_id_2, start_dt_2, get_appendable_services(context))

    cs_info = csteps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL, dt=start_dt_2, signed=1)
    assert set(cs_info[1]['SERVICES']) == set(get_appendable_services(context))

    orders_len = steps.CommonPartnerSteps.client_orders(client_id, service_id=1171)
    utils.check_that(orders_len, empty(),
                     'На клиента {} почему-то уже есть заказ по 1171 сервису'.format(client_id))

    services = get_appendable_services(context) + [Services.ZAXI_UNIFIED_CONTRACT.id]
    create_change_services_zapravki_collateral(corp_contract_id_2, start_dt_2, services, contract_id)

    cs_info = csteps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL, dt=start_dt_2, signed=1)
    assert set(cs_info[1]['SERVICES']) == set(services)

    cs_info = csteps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL,
                                                      dt=start_dt_2 - timedelta(days=1), signed=1)
    assert set(cs_info[1]['SERVICES']) == set(context.contract_services)

    cs_info = csteps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL,
                                                      dt=start_dt_2 + timedelta(days=1), signed=1)
    assert set(cs_info[1]['SERVICES']) == set(services)

    orders_len = steps.CommonPartnerSteps.client_orders(client_id, service_id=1171)
    utils.check_that(orders_len, not empty(),
                     'На клиента {} нет заказов по 1171 сервису'.format(client_id))


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.DRIVE_B2B, Features.FOOD_CORP,
                  Features.UNIFIED_CORP_CONTRACT, Features.ACT)
@pytest.mark.parametrize(
    'context, is_postpay, _postpay_id',
    utils.flatten_parametrization(contexts, postpay_params),
    ids=lambda _context, _is_postpay, _postpay_id: '_'.join([_context.name, _postpay_id])
)
def test_act_unified_with_change_services_wo_data(context, is_postpay, _postpay_id):
    _, _, start_dt_1, end_dt_1, start_dt_2, end_dt_2 = utils.Date.previous_three_months_start_end_dates()
    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': start_dt_1})

    # Откруточная функция для 135 сервиса (TAXI_CORP) всегда возвращает нулевое количество по продукту,
    # даже если откруток нет вообще.
    # Откруточная функция для 650 сервиса (TAXI_CORP_CLIENTS) не возвращает откруток по продуктам,
    # если по ним нет откруток.
    # Логика всегда проставляет ЛС на генерацию акта, если из откруточных функций что-то вернулось.
    # Поэтому:
    #   - Если в договоре есть 135 сервис, ЛС в любом случае проставится на генерацию,
    #     нужно позвать generate_partner_acts_fair_and_export с manual_export=True, чтобы разобрался экспорт MONTH_PROC,
    #     (хотя актов не будет)
    #   - Если в договоре только 650 сервис и нет откруток - ЛС не проставится на генерацию,
    #     нужно позвать generate_partner_acts_fair_and_export с manual_export=False, т.к. экспорт MONTH_PROC создается
    #     всегда при создании клиента в state=1 и пустым input (без счета).
    #     generate_partner_acts_fair_and_export тогда просто проверит, что экспорт сразу в state=1

    manual_export = Services.TAXI_CORP.id in context.contract_services

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id, end_dt_1,
                                                                   manual_export=manual_export)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(corp_client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data_1 = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                       corp_contract_id, corp_person_id,
                                                                                       D('0'), dt=start_dt_1)

    utils.check_that(invoice_data_1, equal_to_casted_dict(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, empty(), 'Сравниваем данные из акта с шаблоном')
    utils.check_that(consume_data_1, empty(), 'Проверяем, что конзюмов нет')

    services_for_append = get_appendable_services(context)

    res = create_change_services_collateral(corp_contract_id, start_dt_2, services_for_append)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id, end_dt_2,
                                                                   manual_export=manual_export)

    consume_data_2 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_2 = steps.ActsSteps.get_act_data_by_client(corp_client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data_2 = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                       corp_contract_id, corp_person_id,
                                                                                       D('0'), dt=start_dt_1)

    utils.check_that(invoice_data_2, equal_to_casted_dict(expected_invoice_data_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, empty(), 'Сравниваем данные из акта с шаблоном')
    utils.check_that(consume_data_2, empty(), 'Проверяем, что косьюмов нет')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11_Taxi))
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.DRIVE_B2B, Features.FOOD_CORP,
                  Features.UNIFIED_CORP_CONTRACT, Features.ACT)
@pytest.mark.parametrize(
    'context, is_postpay, _postpay_id, personal_account_payment_sum, _paysum_id',
    utils.flatten_parametrization(contexts, postpay_params, paysum_params),
    ids=lambda _context, _is_postpay, _postpay_id, _paysum, _paysum_id: '_'.join([_context.name, _postpay_id, _paysum_id])
)
def test_act_unified_with_change_services_before_migration(context, is_postpay, _postpay_id,
                                                           personal_account_payment_sum, _paysum_id):
    # Релиз единого договора после миграции на открутки из ОЕБС,
    # при добавлении новых сервисов в договор по ним невозможны старые открутки
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

    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month_migration_minus2_start_dt})

    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(corp_client_id)

    check_balance(u'Проверяем баланс до платежа', context, corp_contract_id, corp_client_id, external_invoice_id,
                  is_postpay, personal_account_pay_sum=D('0'), total_compls_sum=D('0'), cur_month_charge=D('0'),
                  act_sum=D('0'))

    if personal_account_payment_sum:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_account_payment_sum,
                               payment_dt=month_migration_minus2_start_dt)
    check_balance(u'Проверяем баланс после платежа', context, corp_contract_id, corp_client_id, external_invoice_id,
                  is_postpay, personal_account_pay_sum=personal_account_payment_sum, total_compls_sum=D('0'),
                  cur_month_charge=D('0'), act_sum=D('0'))

    payment_sum_1 = D('420.69')
    refund_sum_1 = D('120.15')

    total_compls_sum = D('0')
    total_compls_sum_1 = D('0')
    total_sums_by_products = defaultdict(lambda: D('0'))
    for service_id in context.contract_services:
        compls_sum, sums_by_products = service_completions_before_oebs_func_mapping[service_id] \
            (context, corp_contract_id, corp_client_id, month_migration_minus2_start_dt, payment_sum_1, refund_sum_1)
        total_compls_sum += compls_sum
        total_compls_sum_1 += compls_sum
        total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    check_balance(u'Проверяем баланс после  создания откруток в 1м месяце', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=total_compls_sum_1, act_sum=D('0'))

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id,
                                                                   month_migration_minus2_end_dt)

    check_balance(u'Проверяем баланс после сощдания актов в 1м месяце', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=D('0'), act_sum=total_compls_sum_1)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    expected_consumes_1 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_1.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_1 = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, total_compls_sum, dt=month_migration_minus2_start_dt
    )

    expected_act_data_1 = steps.CommonData.create_expected_act_data(total_compls_sum_1, month_migration_minus2_end_dt)
    utils.check_that(consume_data_1, contains_dicts_with_entries(expected_consumes_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_1, equal_to_casted_dict(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, contains_dicts_with_entries([expected_act_data_1]),
                     'Сравниваем данные из акта с шаблоном')

    # ### НОВАЯ ЛОГИКА - ОЕБСовые агрегаты

    services_for_append = get_appendable_services(context)

    res = create_change_services_collateral(corp_contract_id, month_minus2_start_dt, services_for_append)

    check_balance(u'Проверяем баланс после создания ДС на новые сервисы', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=D('0'), act_sum=total_compls_sum_1)

    total_compls_sum_2 = D('0')
    for service_id in services_for_append:
        compls_sum, sums_by_products = service_completions_after_oebs_func_mapping[service_id] \
            (context, corp_contract_id, corp_client_id, month_minus2_start_dt, payment_sum_1, refund_sum_1)
        total_compls_sum += compls_sum
        total_compls_sum_2 += compls_sum
        total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    check_balance(u'Проверяем баланс после создания откруток во 2м месяце', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=total_compls_sum_2, act_sum=total_compls_sum_1)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id,
                                                                   month_minus2_end_dt)
    check_balance(u'Проверяем баланс после создания акта во 2м месяце', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=D('0'), act_sum=total_compls_sum)

    consume_data_2 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_2 = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    expected_consumes_2 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_2.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_2 = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, total_compls_sum, dt=month_migration_minus2_start_dt
    )

    expected_act_data_2 = steps.CommonData.create_expected_act_data(total_compls_sum_2, month_minus2_end_dt)
    utils.check_that(consume_data_2, contains_dicts_with_entries(expected_consumes_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_2, equal_to_casted_dict(expected_invoice_data_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, contains_dicts_with_entries([expected_act_data_1,
                                                              expected_act_data_2]),
                     'Сравниваем данные из акта с шаблоном')

    total_compls_sum_3 = D('0')
    for service_id in services_for_append:
        compls_sum, sums_by_products = service_completions_after_oebs_func_mapping[service_id] \
            (context, corp_contract_id, corp_client_id, month_minus1_start_dt, payment_sum_1, refund_sum_1)
        total_compls_sum += compls_sum
        total_compls_sum_3 += compls_sum
        total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    check_balance(u'Проверяем баланс после создания откруток в 3м месяце', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=total_compls_sum_3,
                  act_sum=total_compls_sum_1 + total_compls_sum_2)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id,
                                                                   month_minus1_end_dt)
    check_balance(u'Проверяем баланс после создания акта во 3м месяце', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=D('0'), act_sum=total_compls_sum)

    consume_data_3 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_3 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_3 = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    expected_consumes_3 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_3.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_3 = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, total_compls_sum, dt=month_migration_minus2_start_dt
    )

    expected_act_data_3 = steps.CommonData.create_expected_act_data(total_compls_sum_3, month_minus1_end_dt)
    utils.check_that(consume_data_3, contains_dicts_with_entries(expected_consumes_3),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_3, equal_to_casted_dict(expected_invoice_data_3),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_3, contains_dicts_with_entries([expected_act_data_1,
                                                              expected_act_data_2,
                                                              expected_act_data_3]),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11_Taxi))
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.DRIVE_B2B, Features.FOOD_CORP,
                  Features.UNIFIED_CORP_CONTRACT, Features.ACT)
@pytest.mark.parametrize(
    'context, is_postpay, _postpay_id, personal_account_payment_sum, _paysum_id',
    utils.flatten_parametrization(contexts_with_unified, postpay_params, paysum_params),
    ids=lambda _context, _is_postpay, _postpay_id, _paysum, _paysum_id: '_'.join([_context.name, _postpay_id, _paysum_id])
)
def test_act_unified_after_migration(context, is_postpay, _postpay_id, personal_account_payment_sum, _paysum_id):
    # Релиз единого договора после миграции на открутки из ОЕБС,
    # единый договор со всеми сервисами может заводиться только после миграции
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

    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month_minus2_start_dt})
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(corp_client_id)

    check_balance(u'Проверяем баланс до платежа', context, corp_contract_id, corp_client_id, external_invoice_id,
                  is_postpay, personal_account_pay_sum=D('0'), total_compls_sum=D('0'), cur_month_charge=D('0'),
                  act_sum=D('0'))

    if personal_account_payment_sum:
        steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_account_payment_sum,
                               payment_dt=month_minus2_start_dt)
    check_balance(u'Проверяем баланс после платежа', context, corp_contract_id, corp_client_id, external_invoice_id,
                  is_postpay, personal_account_pay_sum=personal_account_payment_sum, total_compls_sum=D('0'),
                  cur_month_charge=D('0'), act_sum=D('0'))

    payment_sum_1 = D('420.69')
    refund_sum_1 = D('120.15')

    total_compls_sum = D('0')
    total_compls_sum_1 = D('0')
    total_sums_by_products = defaultdict(lambda: D('0'))
    for service_id in context.contract_services:
        compls_sum, sums_by_products = service_completions_after_oebs_func_mapping[service_id] \
            (context, corp_contract_id, corp_client_id, month_minus2_start_dt, payment_sum_1, refund_sum_1)
        total_compls_sum += compls_sum
        total_compls_sum_1 += compls_sum
        total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    check_balance(u'Проверяем баланс после создания откруток в 1м месяце', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=total_compls_sum_1, act_sum=D('0'))

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id,
                                                                   month_minus2_end_dt)

    check_balance(u'Проверяем баланс после создания актов в 1м месяце', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=D('0'), act_sum=total_compls_sum_1)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    expected_consumes_1 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_1.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_1 = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, total_compls_sum, dt=month_minus2_start_dt
    )

    expected_act_data_1 = steps.CommonData.create_expected_act_data(total_compls_sum_1, month_minus2_end_dt)
    utils.check_that(consume_data_1, contains_dicts_with_entries(expected_consumes_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_1, equal_to_casted_dict(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, contains_dicts_with_entries([expected_act_data_1]),
                     'Сравниваем данные из акта с шаблоном')

    services_for_append = get_appendable_services(context)
    res = create_change_services_collateral(corp_contract_id, month_minus2_start_dt, services_for_append)

    total_compls_sum_2 = D('0')
    for service_id in services_for_append:
        compls_sum, sums_by_products = service_completions_after_oebs_func_mapping[service_id] \
            (context, corp_contract_id, corp_client_id, month_minus1_start_dt, payment_sum_1, refund_sum_1)
        total_compls_sum += compls_sum
        total_compls_sum_2 += compls_sum
        total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    check_balance(u'Проверяем баланс после создания откруток во 2м месяце', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=total_compls_sum_2,
                  act_sum=total_compls_sum_1)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id,
                                                                   month_minus1_end_dt)

    check_balance(u'Проверяем баланс после создания акта во 2м месяце', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=total_compls_sum, cur_month_charge=D('0'), act_sum=total_compls_sum)

    consume_data_2 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_2 = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    expected_consumes_2 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_2.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_2 = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, total_compls_sum, dt=month_minus2_start_dt
    )

    expected_act_data_2 = steps.CommonData.create_expected_act_data(total_compls_sum_2, month_minus1_end_dt)
    utils.check_that(consume_data_2, contains_dicts_with_entries(expected_consumes_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_2, equal_to_casted_dict(expected_invoice_data_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, contains_dicts_with_entries([expected_act_data_1, expected_act_data_2]),
                     'Сравниваем данные из акта с шаблоном')

    steps.common_steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

    steps.CommonPartnerSteps.export_partner_fast_balance(corp_contract_id)

    expected_fast_balance = get_expected_fast_balance_data(APX_SUM, personal_account_payment_sum, DISCOUNT_BONUS_SUM,
                                                           corp_contract_id, context.currency.iso_code)
    fast_balance_object = steps.api.test_balance().GetPartnerFastBalance(corp_contract_id, 'Contract',
                                                                         'partner-fast-balance-corp-taxi')
    fast_balance_object['balance_info'].pop('DT')

    utils.check_that(fast_balance_object['balance_info'], has_entries_casted(expected_fast_balance),
                     'Сравниваем балансы c типом Export.Classname.CONTRACT')


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.DRIVE_B2B, Features.FOOD_CORP,
                  Features.UNIFIED_CORP_CONTRACT, Features.ACT)
@pytest.mark.parametrize(
    'context, is_postpay, _postpay_id',
    utils.flatten_parametrization([CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP], postpay_params),
    ids=lambda _context, _is_postpay, _postpay_id: '_'.join([_context.name, _postpay_id])
)
def test_act_unified_change_services_with_skip(context, is_postpay, _postpay_id):
    # Релиз единого договора после миграции на открутки из ОЕБС,
    # единый договор со всеми сервисами может заводиться только после миграции
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

    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month_minus2_start_dt})
    payment_sum_1 = D('420.69')
    refund_sum_1 = D('120.15')

    total_compls_sum = D('0')
    total_compls_sum_1 = D('0')
    total_sums_by_products = defaultdict(lambda: D('0'))
    for service_id in context.contract_services:
        compls_sum, sums_by_products = service_completions_after_oebs_func_mapping[service_id] \
            (context, corp_contract_id, corp_client_id, month_minus2_start_dt, payment_sum_1, refund_sum_1)
        total_compls_sum += compls_sum
        total_compls_sum_1 += compls_sum
        total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id,
                                                                   month_minus2_end_dt)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    expected_consumes_1 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_1.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_1 = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, total_compls_sum, dt=month_minus2_start_dt
    )

    expected_act_data_1 = steps.CommonData.create_expected_act_data(total_compls_sum_1, month_minus2_end_dt)
    utils.check_that(consume_data_1, contains_dicts_with_entries(expected_consumes_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_1, equal_to_casted_dict(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, contains_dicts_with_entries([expected_act_data_1]),
                     'Сравниваем данные из акта с шаблоном')

    services_for_append = context.contract_services + [Services.DRIVE_B2B.id]
    res = create_change_services_collateral(corp_contract_id, month_minus2_start_dt, services_for_append)

    total_compls_sum_2 = D('0')
    for service_id in UNIFIED_CORP_CONTRACT_SERVICES:
        compls_sum, sums_by_products = service_completions_after_oebs_func_mapping[service_id] \
            (context, corp_contract_id, corp_client_id, month_minus1_start_dt, payment_sum_1, refund_sum_1)
        if service_id in services_for_append:
            total_compls_sum += compls_sum
            total_compls_sum_2 += compls_sum
            total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id,
                                                                   month_minus1_end_dt)

    consume_data_2 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_2 = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    expected_consumes_2 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_2.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_2 = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, total_compls_sum, dt=month_minus2_start_dt
    )

    expected_act_data_2 = steps.CommonData.create_expected_act_data(total_compls_sum_2, month_minus1_end_dt)
    utils.check_that(consume_data_2, contains_dicts_with_entries(expected_consumes_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_2, equal_to_casted_dict(expected_invoice_data_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, contains_dicts_with_entries([expected_act_data_1, expected_act_data_2]),
                     'Сравниваем данные из акта с шаблоном')


################ ПРОМОКОДЫ ####################################
def create_new_promo(client_id, context, promo_sum, adjust_quantity=True, apply_on_create=True):
    start_dt = datetime.now() - relativedelta(years=1)
    end_dt = datetime.now() + relativedelta(years=1)
    minimal_amounts = {context.currency.iso_code: 1}

    calc_params = promo_steps.fill_calc_params_fixed_sum(currency_bonuses={context.currency.iso_code: promo_sum},
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


def calculate_discount_pct_from_fixed_sum(fixed_sum, nds, qty, sum_before=None, adjust_quantity=None):
    internal_price = 1
    if sum_before:
        total_sum = sum(sum_before)
    else:
        total_sum = qty * D(internal_price).quantize(D('0.001'))
    bonus_with_nds = promo_steps.add_nds_to_amount(fixed_sum, nds)
    return promo_steps.calculate_static_discount_sum(total_sum=total_sum, bonus_with_nds=bonus_with_nds,
                                                     adjust_quantity=adjust_quantity)


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT, Features.PROMOCODE)
# @parametrize_context
@pytest.mark.parametrize(
    'context', contexts, ids=lambda _context: _context.name
)
def test_promocode(context):
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

    # корп.такси применяет промокоды только FixedSumBonusPromoCodeGroup, т.е. на сумму оплаты докидываются лишние qty
    adjust_quantity = True
    # сумма промо без НДС
    promo_sum = D('50000')
    # сумма платежа с НДС
    pa_payment_sum = D('100000')

    # суммы откруток
    payment_sum_1 = D('420')
    refund_sum_1 = D('160')

    ### СТАРАЯ ЛОГИКА ОТКРУТОК - ДО ПЕРЕХОДА НА ОЕБСовые агрегаты
    params = {'start_dt': month_migration_minus1_start_dt}
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        is_postpay=False,
        additional_params=params)

    service_order_id = steps.OrderSteps.get_order_id_by_contract(contract_id, context.service.id)

    # создаю промокод, привязанный к клиенту
    create_new_promo(client_id, context, promo_sum, adjust_quantity=adjust_quantity)

    # создаю реквест и счет-квитанцию
    request_id = create_request_taxi(client_id, service_order_id, pa_payment_sum,
                                     month_migration_minus1_start_dt, context.service.id)
    invoice_id_charge_note, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=False,
                                                             contract_id=contract_id)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    payment_id = steps.InvoiceSteps.get_payment_id_for_charge_note(invoice_id_charge_note)

    # рассчитываю скидку и сумму к оплате
    discount = calculate_discount_pct_from_fixed_sum(promo_sum, context.nds.pct_on_dt(month_migration_minus1_start_dt),
                                                     pa_payment_sum, adjust_quantity=adjust_quantity)

    # оплачиваю счет на сумму с учетом скидки
    steps.TaxiSteps.create_cash_payment_fact(external_invoice_id, pa_payment_sum, month_migration_minus1_start_dt,
                                             'INSERT', payment_id)
    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

    # делаю открутки
    dt = month_migration_minus1_start_dt + relativedelta(days=5)

    total_compls_qty = D('0')
    cur_compls_qty_1 = D('0')
    total_qty_by_products = defaultdict(lambda: D('0'))
    cur_qty_by_products_1 = defaultdict(lambda: D('0'))
    for service_id in context.contract_services:
        compls_qty_sum, qty_by_products = service_completions_before_oebs_func_mapping[service_id] \
            (context, contract_id, client_id, dt, payment_sum_1, refund_sum_1)
        total_compls_qty += compls_qty_sum
        cur_compls_qty_1 += compls_qty_sum
        total_qty_by_products = merge_sums_dicts(total_qty_by_products, qty_by_products)
        cur_qty_by_products_1 = merge_sums_dicts(cur_qty_by_products_1, qty_by_products)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_migration_minus1_end_dt)

    main_product_id = get_ua_root_product_mapping()[context.service.id, context.currency.iso_code]
    total_sum_by_products = {product_id: (product_sum * (D('1') - discount * D('0.01')))
                             for (product_id, product_sum) in total_qty_by_products.items()}
    cur_sum_by_products_1 = {product_id: (product_sum * (D('1') - discount * D('0.01')))
                             for (product_id, product_sum) in cur_qty_by_products_1.items()}
    total_compls_sum = sum(_sum for (product_id, _sum) in total_sum_by_products.items())
    cur_compls_sum_1 = sum(_sum for (product_id, _sum) in cur_sum_by_products_1.items())

    expected_invoice_data_1 = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                       contract_id, person_id,
                                                                                       pa_payment_sum,
                                                                                       total_act_sum=total_compls_sum,
                                                                                       dt=month_migration_minus1_start_dt)

    expected_act_data_1 = steps.CommonData.create_expected_act_data(amount=cur_compls_sum_1,
                                                                    act_date=month_migration_minus1_end_dt)
    # на главном заказе сумма будет равна: общее зачисления со счета - сумма всех заказов (включая главный)
    # + сумма откруток по самому заказу
    consume_sum_main = pa_payment_sum - total_compls_sum + (payment_sum_1 - refund_sum_1) * (D('100') - discount) * D(
        '0.01')
    consume_qty_main = consume_sum_main / (1 - discount * D('0.01'))

    expected_order_data_1 = []
    for product_id, consume_sum in total_sum_by_products.items():
        cs = consume_sum_main if product_id == main_product_id else consume_sum
        cq = consume_qty_main if product_id == main_product_id else total_qty_by_products[product_id]
        expected_order_data_1.append(
            steps.CommonData.create_expected_order_data(get_product_id_service_mapping()[product_id],
                                                        product_id, contract_id,
                                                        consume_sum=cs,
                                                        completion_qty=total_qty_by_products[product_id],
                                                        consume_qty=cq),
        )

    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    act_data_1 = steps.ActsSteps.get_act_data_by_client(client_id)
    order_data_1 = steps.OrderSteps.get_order_data_by_client(client_id)
    # отфильтруем нулевые заказы, (сюда попадают добивочные заказы, на которые ничего не зачисляется и не откручивается)
    order_data_1 = filter(lambda o: (o['completion_qty'] > 0 or o['consume_sum'] > 0 or o['consume_qty'] > 0),
                          order_data_1)
    # тут есть ещё charge_note
    utils.check_that(invoice_data_1, contains_dicts_with_entries([expected_invoice_data_1], same_length=False),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, contains_dicts_with_entries([expected_act_data_1]),
                     'Сравниваем данные из акта с шаблоном')
    utils.check_that(order_data_1, contains_dicts_with_entries(expected_order_data_1),
                     'Сравниваем данные из заказа с шаблоном')

    # ### НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты
    # Периодически в рассчетах будет умножение на 2 - т.к. общая сумма зачислений и откруток по заказам, которые были
    # в первом месяце - повторятся. Открутки по новым добавленным сервисам будут в одном экземпляре

    services_for_append = get_appendable_services(context)
    res = create_change_services_collateral(contract_id, month_minus2_start_dt, services_for_append)

    # создаю промокод, привязанный к клиенту
    create_new_promo(client_id, context, promo_sum, adjust_quantity=adjust_quantity)

    # создаю реквест и счет-квитанцию
    request_id = create_request_taxi(client_id, service_order_id, pa_payment_sum,
                                     month_minus2_start_dt, context.service.id)
    invoice_id_charge_note, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=False,
                                                             contract_id=contract_id)
    payment_id = steps.InvoiceSteps.get_payment_id_for_charge_note(invoice_id_charge_note)
    # оплачиваю счет на сумму с учетом скидки
    steps.TaxiSteps.create_cash_payment_fact(external_invoice_id, pa_payment_sum, month_minus2_start_dt,
                                             'INSERT', payment_id)
    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

    # делаю открутки
    dt = month_minus2_start_dt + relativedelta(days=5)

    cur_compls_qty_2 = D('0')
    cur_qty_by_products_2 = defaultdict(lambda: D('0'))
    for service_id in context.contract_services:
        compls_qty_sum, qty_by_products = service_completions_after_oebs_func_mapping[service_id] \
            (context, contract_id, client_id, dt, payment_sum_1, refund_sum_1)
        total_compls_qty += compls_qty_sum
        cur_compls_qty_2 += compls_qty_sum
        total_qty_by_products = merge_sums_dicts(total_qty_by_products, qty_by_products)
        cur_qty_by_products_2 = merge_sums_dicts(cur_qty_by_products_2, qty_by_products)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   month_minus2_end_dt)

    total_sum_by_products = {product_id: (product_sum * (D('1') - discount * D('0.01')))
                             for (product_id, product_sum) in total_qty_by_products.items()}
    cur_sum_by_products_2 = {product_id: (product_sum * (D('1') - discount * D('0.01')))
                             for (product_id, product_sum) in cur_qty_by_products_2.items()}
    total_compls_sum = sum(_sum for (product_id, _sum) in total_sum_by_products.items())
    cur_compls_sum_2 = sum(_sum for (product_id, _sum) in cur_sum_by_products_2.items())

    expected_invoice_data_2 = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                       contract_id, person_id,
                                                                                       pa_payment_sum * 2,
                                                                                       total_act_sum=total_compls_sum,
                                                                                       dt=month_migration_minus1_start_dt)

    expected_act_data_2 = steps.CommonData.create_expected_act_data(amount=cur_compls_sum_2,
                                                                    act_date=month_minus2_end_dt)
    # на главном заказе сумма будет равна: общее зачисления со счета - сумма всех заказов (включая главный)
    # + сумма откруток по самому заказу
    consume_sum_main = 2 * pa_payment_sum - total_compls_sum \
                       + 2 * (payment_sum_1 - refund_sum_1) * (D('100') - discount) * D('0.01')
    consume_qty_main = consume_sum_main / (1 - discount * D('0.01'))

    expected_order_data_2 = []
    for product_id, consume_sum in total_sum_by_products.items():
        cs = consume_sum_main if product_id == main_product_id else consume_sum
        cq = consume_qty_main if product_id == main_product_id else total_qty_by_products[product_id]
        expected_order_data_2.append(
            steps.CommonData.create_expected_order_data(get_product_id_service_mapping()[product_id],
                                                        product_id, contract_id,
                                                        consume_sum=cs,
                                                        completion_qty=total_qty_by_products[product_id],
                                                        consume_qty=cq),
        )

    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    act_data_2 = steps.ActsSteps.get_act_data_by_client(client_id)
    order_data_2 = steps.OrderSteps.get_order_data_by_client(client_id)
    # отфильтруем нулевые заказы, (сюда попадают добивочные заказы, на которые ничего не зачисляется и не откручивается)
    order_data_2 = filter(lambda o: (o['completion_qty'] > 0 or o['consume_sum'] > 0 or o['consume_qty'] > 0),
                          order_data_2)
    # тут есть ещё charge_note
    utils.check_that(invoice_data_2, contains_dicts_with_entries([expected_invoice_data_2], same_length=False),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, contains_dicts_with_entries([expected_act_data_1,
                                                              expected_act_data_2]),
                     'Сравниваем данные из акта с шаблоном')
    utils.check_that(order_data_2, contains_dicts_with_entries(expected_order_data_2),
                     'Сравниваем данные из заказа с шаблоном')


@pytest.mark.parametrize(
    'context, is_postpay, _postpay_id',
    utils.flatten_parametrization(contexts, postpay_params),
    ids=lambda _context, _is_postpay, _postpay_id: '_'.join([_context.name, _postpay_id])
)
def test_acts_on_finished_contract(context, is_postpay, _postpay_id):

    month1_start_dt, month1_end_dt, \
    month2_start_dt, month2_end_dt,\
    month3_start_dt, month3_end_dt = \
        utils.Date.previous_three_months_start_end_dates()

    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month1_start_dt,
                                                                       'finish_dt': month2_start_dt})
    payment_sum_1 = D('420.69')
    refund_sum_1 = D('120.15')

    total_compls_sum = D('0')
    total_compls_sum_1 = D('0')
    total_sums_by_products = defaultdict(lambda: D('0'))
    for service_id in context.contract_services:
        compls_sum, sums_by_products = service_completions_after_oebs_func_mapping[service_id] \
            (context, corp_contract_id, corp_client_id, month1_start_dt, payment_sum_1, refund_sum_1)
        total_compls_sum += compls_sum
        total_compls_sum_1 += compls_sum
        total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id,
                                                                   month1_end_dt)

    consume_data_1 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_1 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_1 = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    expected_consumes_1 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_1.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_1 = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, total_compls_sum, dt=month1_start_dt
    )

    expected_act_data_1 = steps.CommonData.create_expected_act_data(total_compls_sum_1, month1_end_dt)
    utils.check_that(consume_data_1, contains_dicts_with_entries(expected_consumes_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_1, equal_to_casted_dict(expected_invoice_data_1),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_1, contains_dicts_with_entries([expected_act_data_1]),
                     'Сравниваем данные из акта с шаблоном')

    services_for_append = get_appendable_services(context)
    res = create_change_services_collateral(corp_contract_id, month1_start_dt, services_for_append)

    total_compls_sum_2 = D('0')
    for service_id in services_for_append:
        compls_sum, sums_by_products = service_completions_after_oebs_func_mapping[service_id] \
            (context, corp_contract_id, corp_client_id, month2_start_dt, payment_sum_1, refund_sum_1)
        # открутки 135 сервис не учитываются в закрытых договорах
        if service_id == Services.TAXI_CORP.id:
            continue
        total_compls_sum += compls_sum
        total_compls_sum_2 += compls_sum
        total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id, month2_end_dt)

    consume_data_2 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_2 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_2 = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    expected_consumes_2 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_2.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_2 = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, total_compls_sum, dt=month1_start_dt
    )

    expected_act_data_2 = steps.CommonData.create_expected_act_data(total_compls_sum_2, month2_end_dt)
    utils.check_that(consume_data_2, contains_dicts_with_entries(expected_consumes_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_2, equal_to_casted_dict(expected_invoice_data_2),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_2, contains_dicts_with_entries([expected_act_data_1, expected_act_data_2]),
                     'Сравниваем данные из акта с шаблоном')

    # 3ий месяц
    total_compls_sum_3 = D('0')
    for service_id in services_for_append:
        compls_sum, sums_by_products = service_completions_after_oebs_func_mapping[service_id] \
            (context, corp_contract_id, corp_client_id, month3_start_dt, payment_sum_1, refund_sum_1)
        # открутки 135 сервис не учитываются в закрытых договорах
        if service_id == Services.TAXI_CORP.id:
            continue
        total_compls_sum += compls_sum
        total_compls_sum_3 += compls_sum
        total_sums_by_products = merge_sums_dicts(total_sums_by_products, sums_by_products)

    # запускаем конец месяца для единого договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id, month3_end_dt)

    consume_data_3 = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_3 = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_3 = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    expected_consumes_3 = []
    for product_id, amount in total_sums_by_products.items():
        expected_consumes_3.append(
            steps.CommonData.create_expected_consume_data(product_id, amount, InvoiceType.PERSONAL_ACCOUNT)
        )

    # создаем шаблон для сравнения
    expected_invoice_data_3 = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, total_compls_sum, dt=month1_start_dt
    )

    expected_act_data_3 = steps.CommonData.create_expected_act_data(total_compls_sum_3, month3_end_dt)
    utils.check_that(consume_data_3, contains_dicts_with_entries(expected_consumes_3),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data_3, equal_to_casted_dict(expected_invoice_data_3),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_3, contains_dicts_with_entries([expected_act_data_1, expected_act_data_2,
                                                              expected_act_data_3]),
                     'Сравниваем данные из акта с шаблоном')
