# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from collections import defaultdict
from decimal import Decimal as D
from dateutil.relativedelta import relativedelta

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as tsteps
from balance.features import Features, AuditFeatures
from balance.tests.xmlrpc.test_GetPartnerBalance_cloud import calc_expired_dt
from btestlib import utils
from btestlib.constants import TransactionType, Services, CorpTaxiOrderType, InvoiceType, Products, Regions
from btestlib.matchers import equal_to_casted_dict, contains_dicts_equal_to, contains_dicts_with_entries, equal_to, \
    has_entries_casted
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, \
    CORP_TAXI_KZ_CONTEXT_GENERAL_DECOUP, CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED, \
    CORP_TAXI_ISRAEL_CONTEXT_GENERAL_DECOUP, CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED, \
    CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP, CORP_TAXI_YANGO_ISRAEL_CONTEXT_GENERAL_DECOUP, CORP_TAXI_USN_RU_CONTEXT, \
    CORP_TAXI_USN_RU_SPENDABLE_CONTEXT, CORP_TAXI_KGZ_CONTEXT_GENERAL

AMOUNT = D('5000')

FAKE_TAXI_CLIENT_ID = 1111
FAKE_TAXI_PERSON_ID = 2222
FAKE_TAXI_CONTRACT_ID = 3333

payment_sum = D('5999.7')
payment_sum_tlog = D('601.8')
refund_sum = D('2000.2')
refund_sum_tlog = D('300.9')
payment_sum_internal = D('90.4')
refund_sum_internal = D('78.4')
coef_1 = D('0.5')
coef_2 = D('0.8')

_, _, start_dt_1, end_dt_1, start_dt_2, end_dt_2 = utils.Date.previous_three_months_start_end_dates()
_, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, \
    PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT = utils.Date.previous_three_months_start_end_dates()

CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP = tsteps.CORP_TAXI_CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP


def create_completions(context, corp_client_id, dt, coef=D('1')):
    # добавляем открутки
    if 'CORP_TAXI_KZ_CONTEXT_GENERAL' in context.name:
        steps.SimpleApi.create_fake_tpt_data(
            context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
            FAKE_TAXI_CLIENT_ID, dt,
            [{'client_amount': payment_sum_internal * coef,
              'client_id': corp_client_id,
              'transaction_type': TransactionType.PAYMENT,
              'internal': 1},
             {'client_amount': refund_sum_internal * coef,
              'client_id': corp_client_id,
              'transaction_type': TransactionType.REFUND,
              'internal': 1}
             ])
    steps.SimpleApi.create_fake_tpt_data(
        context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
        FAKE_TAXI_CLIENT_ID, dt,
        [{'client_amount': payment_sum * coef,
          'client_id': corp_client_id,
          'transaction_type': TransactionType.PAYMENT},
         {'client_amount': refund_sum * coef,
          'client_id': corp_client_id,
          'transaction_type': TransactionType.REFUND}
         ])


def create_completions_tlog(context, corp_client_id, dt, coef=D('1'), last_transaction_id=100):
    order_dicts_tlog = []
    for idx, (order_type, product_id) in enumerate(CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP[context.currency]):
        order_dicts_tlog += [
            {'service_id': Services.TAXI_CORP_CLIENTS.id,
             'amount': (idx + 1) * payment_sum_tlog / context.nds.koef_on_dt(dt) * coef,
             'type': order_type,
             'dt': dt,
             'transaction_dt': dt,
             'currency': context.currency.iso_code,
             'last_transaction_id': last_transaction_id - 2 * idx - 1},
            {'service_id': Services.TAXI_CORP_CLIENTS.id,
             'amount': (idx + 1) * -refund_sum_tlog / context.nds.koef_on_dt(dt) * coef,
             'type': order_type,
             'dt': dt,
             'transaction_dt': dt,
             'currency': context.currency.iso_code,
             'last_transaction_id': last_transaction_id - 2 * idx},
        ]
    tsteps.TaxiSteps.create_orders_tlog(corp_client_id, order_dicts_tlog)


def create_partner_oebs_completions(context, corp_contract_id, corp_client_id, dt, coef=D('1')):
    compls_dicts = []
    for idx, (order_type, product_id) in enumerate(CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP[context.currency]):
        compls_dicts += [
            {
                'service_id': Services.TAXI_CORP_CLIENTS.id,
                'last_transaction_id': 99,
                'amount': (idx + 1) * (payment_sum_tlog - refund_sum_tlog) * coef,
                'product_id': product_id,
                'dt': dt,
                'transaction_dt': dt,
                'currency': context.currency.iso_code,
                'accounting_period': dt
            },
        ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(corp_contract_id, corp_client_id, compls_dicts)


def create_act_first_month(corp_client_id, corp_contract_id, context, dt):
    create_completions(context, corp_client_id, dt)
    create_completions_tlog(context, corp_client_id, dt, last_transaction_id=100)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, dt)


def prepare_expected_balance_data(context, contract_id, client_id, invoice_eid, is_postpay, personal_account_pay_sum,
                                  total_compls_sum, cur_month_charge, act_info):
    if is_postpay:
        expected_balance = {
            'ClientID': client_id,
            'ContractID': contract_id,
            'Currency': context.currency.iso_code,
            'PersonalAccountExternalID': {
                '1181': invoice_eid[0],
                '1183': invoice_eid[1]
            },
            'ReceiptSum': personal_account_pay_sum,
            'CommissionToPay': cur_month_charge,
            # 'ActAndDebtInfo': act_info
        }
    else:
        expected_balance = {
            'ClientID': client_id,
            'ContractID': contract_id,
            'Currency': context.currency.iso_code,
            'PersonalAccountExternalID': {
                '1181': invoice_eid[0],
                '1183': invoice_eid[1]
            },
            'ReceiptSum': personal_account_pay_sum,
            'TotalCharge': total_compls_sum,
            'Balance': personal_account_pay_sum - total_compls_sum,
            'CurrMonthCharge': cur_month_charge,
            # 'ActAndDebtInfo': act_info
        }
    return expected_balance


def check_balance(descr, context, contract_id, client_id, invoice_eid, is_postpay, personal_account_pay_sum,
                  total_compls_sum, cur_month_charge, act_info):
    partner_balance = steps.PartnerSteps.get_partner_balance(context.balance_service, [contract_id])
    expected_balance = prepare_expected_balance_data(context, contract_id, client_id, invoice_eid,
                                                     is_postpay, personal_account_pay_sum,
                                                     total_compls_sum, cur_month_charge, act_info)
    utils.check_that(partner_balance[0]['ActAndDebtInfo']['1181'], has_entries_casted(act_info['1181']), descr)
    utils.check_that(partner_balance[0]['ActAndDebtInfo']['1183'], has_entries_casted(act_info['1183']), descr)
    del partner_balance[0]['ActAndDebtInfo']
    utils.check_that(partner_balance, contains_dicts_with_entries([expected_balance]), descr)


def calc_final_sums(context):
    act_first_month = D('0')
    act_second_month = D('0')
    consume_sums = defaultdict(int)

    if Services.TAXI_CORP.id in context.contract_services:
        sum_first = payment_sum - refund_sum
        sum_second = (payment_sum - refund_sum) * (coef_1 + coef_2)

        if 'CORP_TAXI_KZ_CONTEXT_GENERAL' in context.name:
            sum_first += payment_sum_internal - refund_sum_internal
            sum_second += (payment_sum_internal - refund_sum_internal) * (coef_1 + coef_2)

        act_first_month += sum_first
        act_second_month += sum_second
        consume_sums['main'] += sum_first + sum_second

    if Services.TAXI_CORP_CLIENTS.id in context.contract_services:
        for idx, (order_type, product_id) in enumerate(CURRENCY_TO_ORDER_TYPES_PRODUCTS_MAP[context.currency]):
            sum_first = (idx + 1) * (payment_sum_tlog - refund_sum_tlog)
            sum_second = (idx + 1) * (payment_sum_tlog - refund_sum_tlog) * (coef_1 + coef_2)

            act_first_month += sum_first
            act_second_month += sum_second
            consume_sums[order_type] += sum_first + sum_second

    invoice_sum = act_first_month + act_second_month
    return consume_sums, invoice_sum, act_second_month, act_first_month


def calc_final_sums_usn(context, decoef=1):
    act_first_month = D('0')
    act_second_month = D('0')

    sum_first = (payment_sum_tlog - refund_sum_tlog) * 2 / decoef
    sum_second = (payment_sum_tlog - refund_sum_tlog) * (coef_1 + coef_2) * 2 / decoef

    act_first_month += sum_first
    act_second_month += sum_second

    consume_sums = defaultdict(int)
    consume_sums['main'] += sum_first + sum_second

    invoice_sum = act_first_month + act_second_month

    return consume_sums, invoice_sum, act_second_month, act_first_month


def test_act_corp_taxi_usn_first_month_wo_data():
    context = CORP_TAXI_USN_RU_CONTEXT
    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={'start_dt': start_dt_1})

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        corp_client_id,
        corp_contract_id,
        end_dt_1,
        manual_export=False,
    )

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)

    # готовим ожидаемые данные для счёта
    expected_invoice_data_general = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                             corp_contract_id,
                                                                                             corp_person_id,
                                                                                             D('0'), dt=start_dt_1)
    expected_invoice_data_agent = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                           corp_contract_id,
                                                                                           corp_person_id,
                                                                                           D('0'), dt=start_dt_1)
    expected_invoice_data_agent['nds_pct'] = 0

    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data_general,
                                                                expected_invoice_data_agent]),
                     'Сравниваем данные из счета с шаблоном')

    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    utils.check_that(act_data, empty(), 'Сравниваем данные из акта с шаблоном')
    utils.check_that(consume_data, empty(), 'Проверяем, что косьюмов нет')


@pytest.mark.smoke
@pytest.mark.parametrize(
    'personal_account_payment_sum',
    [D('10000'), D('100'), D('0')]
)
@pytest.mark.parametrize(
    'is_postpay',
    [0, 1]
)
def test_act_corp_taxi_usn_second_month(personal_account_payment_sum, is_postpay):
    context = CORP_TAXI_USN_RU_CONTEXT
    month_minus3_start_dt, month_minus3_end_dt, month_minus2_start_dt, month_minus2_end_dt, _, _ = \
        utils.Date.previous_three_months_start_end_dates()

    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month_minus3_start_dt})

    # steps.ExportSteps.export_oebs(client_id=corp_client_id, person_id=corp_person_id, contract_id=corp_contract_id)

    # check contract creation for spendable service
    spendable_client_id, spendable_person_id, spendable_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(CORP_TAXI_USN_RU_SPENDABLE_CONTEXT, is_postpay=is_postpay,
                                                    additional_params={'start_dt': month_minus3_start_dt})

    # steps.ExportSteps.export_oebs(client_id=spendable_client_id, person_id=spendable_person_id, contract_id=spendable_contract_id)

    compls_data = [
        {
            'service_id': context.service.id,
            'last_transaction_id': 99,
            'amount': (payment_sum_tlog - refund_sum_tlog) * 2,
            'product_id': Products.CORP_TAXI_USN_AGENT_RUB.id,
            'dt': month_minus3_end_dt,
            'transaction_dt': month_minus3_end_dt,
            'currency': context.currency.iso_code,
            'accounting_period': month_minus3_end_dt
        }
    ]

    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(corp_client_id, expected_inv_count=2)

    # steps.ExportSteps.export_oebs(invoice_id=invoice_id[0])
    # steps.ExportSteps.export_oebs(invoice_id=invoice_id[1])

    act_info = {
        '1181': {
            'ActSum': D('0'),
        },
        '1183': {
            'ActSum': D('0'),
        }
    }

    check_balance(u'Проверяем баланс до платежа', context, corp_contract_id, corp_client_id, external_invoice_id,
                  is_postpay, personal_account_pay_sum=D('0'), total_compls_sum=D('0'), cur_month_charge=D('0'),
                  act_info=act_info)

    if personal_account_payment_sum:
        steps.InvoiceSteps.pay(invoice_id[1], payment_sum=personal_account_payment_sum,
                               payment_dt=month_minus3_start_dt)

    check_balance(u'Проверяем баланс после платежа', context, corp_contract_id, corp_client_id, external_invoice_id,
                  is_postpay, personal_account_pay_sum=personal_account_payment_sum, total_compls_sum=D('0'),
                  cur_month_charge=D('0'), act_info=act_info)

    # добавляем открутки по 1183
    create_completions(context, corp_client_id, month_minus3_end_dt)
    steps.CommonPartnerSteps.create_partner_oebs_completions(corp_contract_id, corp_client_id, compls_data)

    check_balance(u'Проверяем баланс после откруток по 1183', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=(payment_sum_tlog - refund_sum_tlog) * 2,
                  cur_month_charge=(payment_sum_tlog - refund_sum_tlog) * 2,
                  act_info=act_info)

    for i in compls_data:
        i['service_id'] = Services.TAXI_CORP_CLIENTS_USN_GENERAL.id
        i['product_id'] = Products.CORP_TAXI_USN_GENERAL_RUB.id
        i['amount'] /= 2

    # добавляем открутки по 1181
    create_completions(context, corp_client_id, month_minus3_end_dt)
    steps.CommonPartnerSteps.create_partner_oebs_completions(corp_contract_id, corp_client_id, compls_data)

    check_balance(u'Проверяем баланс после откруток по 1181', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=(payment_sum_tlog - refund_sum_tlog) * 3,
                  cur_month_charge=(payment_sum_tlog - refund_sum_tlog) * 3,
                  act_info=act_info)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, month_minus3_end_dt)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    payment_term = 10 if is_postpay else 0
    expired_dt = calc_expired_dt(region=Regions.RU.id, debt_dt=month_minus3_end_dt, payment_term=payment_term)

    act_info = {
        '1181': {
            'ActSum': (payment_sum_tlog - refund_sum_tlog),
            'ExpiredDT': expired_dt.isoformat(),
            'ExpiredDebtAmount': (payment_sum_tlog - refund_sum_tlog),
            'FirstDebtAmount': (payment_sum_tlog - refund_sum_tlog),
            'FirstDebtFromDT': month_minus3_end_dt.isoformat(),
            'FirstDebtPaymentTermDT': expired_dt.isoformat(),
            'LastActDT': month_minus3_end_dt.isoformat(),
        },
        '1183': {
            'ActSum': (payment_sum_tlog - refund_sum_tlog) * 2,
            'LastActDT': month_minus3_end_dt.isoformat(),
        }
    }

    if personal_account_payment_sum != 10000:
        act_info['1183'].update(
            {
                'ExpiredDT': expired_dt.isoformat(),
                'ExpiredDebtAmount': (payment_sum_tlog - refund_sum_tlog) * 2 - personal_account_payment_sum,
                'FirstDebtAmount': (payment_sum_tlog - refund_sum_tlog) * 2 - personal_account_payment_sum,
                'FirstDebtFromDT': month_minus3_end_dt.isoformat(),
                'FirstDebtPaymentTermDT': expired_dt.isoformat(),
            }
        )

    check_balance(u'Проверяем баланс после генерации актов', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=(payment_sum_tlog - refund_sum_tlog) * 3, cur_month_charge=0,
                  act_info=act_info)

    compls_data = [
        {
            'service_id': context.service.id,
            'last_transaction_id': 99,
            'amount': (payment_sum_tlog - refund_sum_tlog) * coef_2 * 2,
            'product_id': Products.CORP_TAXI_USN_AGENT_RUB.id,
            'dt': month_minus2_end_dt,
            'transaction_dt': month_minus2_end_dt,
            'currency': context.currency.iso_code,
            'accounting_period': month_minus2_end_dt
        },
        {
            'service_id': context.service.id,
            'last_transaction_id': 99,
            'amount': (payment_sum_tlog - refund_sum_tlog) * coef_1 * 2,
            'product_id': Products.CORP_TAXI_USN_AGENT_RUB.id,
            'dt': month_minus2_end_dt,
            'transaction_dt': month_minus2_end_dt,
            'currency': context.currency.iso_code,
            'accounting_period': month_minus2_end_dt
        }
    ]

    # добавляем открутки по 1183
    create_completions(context, corp_client_id, month_minus2_end_dt, coef_2 * 2)
    create_completions(context, corp_client_id, month_minus2_end_dt, coef_1 * 2)
    steps.CommonPartnerSteps.create_partner_oebs_completions(corp_contract_id, corp_client_id, compls_data)

    check_balance(u'Проверяем баланс после откруток по 1183', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=(payment_sum_tlog - refund_sum_tlog) * (3 + (coef_1 + coef_2) * 2),
                  cur_month_charge=(payment_sum_tlog - refund_sum_tlog) * (coef_1 + coef_2) * 2,
                  act_info=act_info)

    for i in compls_data:
        i['service_id'] = Services.TAXI_CORP_CLIENTS_USN_GENERAL.id
        i['product_id'] = Products.CORP_TAXI_USN_GENERAL_RUB.id
        i['amount'] /= 2

    # добавляем открутки по 1181
    create_completions(context, corp_client_id, month_minus2_end_dt, coef_2)
    create_completions(context, corp_client_id, month_minus2_end_dt, coef_1)
    steps.CommonPartnerSteps.create_partner_oebs_completions(corp_contract_id, corp_client_id, compls_data)

    check_balance(u'Проверяем баланс после откруток по 1181', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=(payment_sum_tlog - refund_sum_tlog) * (coef_1 + coef_2 + 1) * 3,
                  cur_month_charge=(payment_sum_tlog - refund_sum_tlog) * (coef_1 + coef_2) * 3,
                  act_info=act_info)

    # запускаем конец месяца для корпоративного договора
    # steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id,
    #                                                                month_minus2_end_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, month_minus2_end_dt)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    act_info = {
        '1181': {
            'ActSum': (payment_sum_tlog - refund_sum_tlog) * (1 + coef_1 + coef_2),
            'ExpiredDT': expired_dt.isoformat(),
            'ExpiredDebtAmount': (payment_sum_tlog - refund_sum_tlog) * (1 + coef_1 + coef_2),
            'FirstDebtAmount': (payment_sum_tlog - refund_sum_tlog),
            'FirstDebtFromDT': month_minus3_end_dt.isoformat(),
            'FirstDebtPaymentTermDT': expired_dt.isoformat(),
            'LastActDT': month_minus2_end_dt.isoformat(),
        },
        '1183': {
            'ActSum': (payment_sum_tlog - refund_sum_tlog) * (2 + (coef_1 + coef_2) * 2),
            'LastActDT': month_minus2_end_dt.isoformat(),
        }
    }

    if personal_account_payment_sum != 10000:
        act_info['1183'].update(
            {
                'ExpiredDT': expired_dt.isoformat(),
                'ExpiredDebtAmount': (payment_sum_tlog - refund_sum_tlog) * (2 + (coef_1 + coef_2) * 2)
                                     - personal_account_payment_sum,
                'FirstDebtAmount': (payment_sum_tlog - refund_sum_tlog) * 2 - personal_account_payment_sum,
                'FirstDebtFromDT': month_minus3_end_dt.isoformat(),
                'FirstDebtPaymentTermDT': expired_dt.isoformat(),
            }
        )

    check_balance(u'Проверяем баланс после генерации актов', context, corp_contract_id, corp_client_id,
                  external_invoice_id, is_postpay, personal_account_pay_sum=personal_account_payment_sum,
                  total_compls_sum=(payment_sum_tlog - refund_sum_tlog) * (coef_1 + coef_2 + 1) * 3,
                  cur_month_charge=0,
                  act_info=act_info)

    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    product_mapping = tsteps.TaxiSteps.get_product_mapping()

    consume_sums, final_sum_invoice, second_sum_act, first_sum_act = calc_final_sums_usn(context)
    expected_consumes = []

    for order_type, amount in consume_sums.iteritems():
        service = Services.TAXI_CORP_CLIENTS_USN_AGENT
        expected_consumes.append(
            steps.CommonData.create_expected_consume_data(
                product_mapping[(service.id, context.currency.iso_code, context.agent_order_type)],
                amount,
                InvoiceType.PERSONAL_ACCOUNT
            )
        )

    # создаем шаблон для сравнения
    expected_invoice_data_second_month_agent = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, final_sum_invoice, dt=month_minus3_end_dt
    )
    expected_invoice_data_second_month_agent['nds_pct'] = 0

    expected_act_data_first_month_agent = steps.CommonData.create_expected_act_data(first_sum_act,
                                                                              month_minus3_end_dt)

    expected_act_data_second_month_agent = steps.CommonData.create_expected_act_data(second_sum_act,
                                                                               month_minus2_end_dt)

    consume_sums, final_sum_invoice, second_sum_act, first_sum_act = calc_final_sums_usn(context, decoef=2)

    for order_type, amount in consume_sums.iteritems():
        service = Services.TAXI_CORP_CLIENTS_USN_GENERAL
        expected_consumes.append(
            steps.CommonData.create_expected_consume_data(
                product_mapping[(service.id, context.currency.iso_code, context.general_order_type)],
                amount,
                InvoiceType.PERSONAL_ACCOUNT
            )
        )

    # создаем шаблон для сравнения
    expected_invoice_data_second_month_general = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, final_sum_invoice, dt=month_minus3_end_dt
    )

    expected_act_data_first_month_general = steps.CommonData.create_expected_act_data(first_sum_act,
                                                                              month_minus3_end_dt)

    expected_act_data_second_month_general = steps.CommonData.create_expected_act_data(second_sum_act,
                                                                               month_minus2_end_dt)

    utils.check_that(consume_data, contains_dicts_with_entries(expected_consumes),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data, contains_dicts_with_entries([expected_invoice_data_second_month_agent,
                                                               expected_invoice_data_second_month_general]),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month_agent,
                                                            expected_act_data_second_month_agent,
                                                            expected_act_data_first_month_general,
                                                            expected_act_data_second_month_general]),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-22114', 'BALANCE-27811', 'BALANCE-31222')
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    CORP_TAXI_KZ_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED,
    CORP_TAXI_ISRAEL_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_YANGO_ISRAEL_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED,
    CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_KGZ_CONTEXT_GENERAL
], ids=lambda context: context.name)
def test_act_corp_taxi_first_month_wo_data(context):
    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context,
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
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        corp_client_id,
        corp_contract_id,
        end_dt_1,
        manual_export=manual_export,
    )

    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data_third_month = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_third_month = steps.ActsSteps.get_act_data_by_client(corp_client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     corp_contract_id, corp_person_id,
                                                                                     D('0'), dt=start_dt_1)

    utils.check_that(invoice_data_third_month, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_third_month, empty(), 'Сравниваем данные из акта с шаблоном')
    utils.check_that(consume_data, empty(), 'Проверяем, что косьюмов нет')


@pytest.mark.smoke
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-22114', 'BALANCE-27811', 'BALANCE-31222')
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
    CORP_TAXI_KZ_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED,
    CORP_TAXI_ISRAEL_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_ISRAEL_CONTEXT_GENERAL_MIGRATED,
    CORP_TAXI_YANGO_ISRAEL_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_BY_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_KGZ_CONTEXT_GENERAL,
], ids=lambda context: context.name)
def test_act_corp_taxi_second_month(context):
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
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={'start_dt': month_migration_minus2_start_dt})

    create_act_first_month(corp_client_id, corp_contract_id, context, month_migration_minus2_end_dt)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    # добавляем открутки
    create_completions(context, corp_client_id, month_migration_minus1_end_dt, coef_2)
    create_completions(context, corp_client_id, month_migration_minus1_end_dt, coef_1)
    create_completions_tlog(context, corp_client_id, month_migration_minus1_end_dt, coef_2, last_transaction_id=200)
    create_completions_tlog(context, corp_client_id, month_migration_minus1_end_dt, coef_1, last_transaction_id=300)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        corp_client_id,
        corp_contract_id,
        month_migration_minus1_end_dt,
    )

    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    consume_sums, final_sum_invoice, second_sum_act, first_sum_act = calc_final_sums(context)
    product_mapping = tsteps.TaxiSteps.get_product_mapping()

    expected_consumes = []
    for order_type, amount in consume_sums.iteritems():
        service = Services.TAXI_CORP if order_type == 'main' else Services.TAXI_CORP_CLIENTS
        expected_consumes.append(
            steps.CommonData.create_expected_consume_data(
                product_mapping[(service.id, context.currency.iso_code, order_type)],
                amount,
                InvoiceType.PERSONAL_ACCOUNT
            )
        )

    # создаем шаблон для сравнения
    expected_invoice_data_second_month = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, final_sum_invoice, dt=month_migration_minus2_start_dt
    )

    expected_act_data_first_month = steps.CommonData.create_expected_act_data(first_sum_act,
                                                                              month_migration_minus2_end_dt)

    expected_act_data_second_month = steps.CommonData.create_expected_act_data(second_sum_act,
                                                                               month_migration_minus1_end_dt)

    utils.check_that(consume_data, contains_dicts_with_entries(expected_consumes),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data_second_month),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month,
                                                            expected_act_data_second_month]),
                     'Сравниваем данные из акта с шаблоном')

    tlog_notches = tsteps.TaxiSteps.get_tlog_timeline_notch(contract_id=corp_contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    if Services.TAXI_CORP_CLIENTS.id in context.contract_services:
        max_last_transactions_ids = [300, 100]
    else:
        max_last_transactions_ids = [0, 0]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')

    # НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты
    # суммы откруток по продуктам сделаны такими же, как в старых открутках (только в тлоге суммы без НДС, а у ОЕБС с НДС)
    # поэтому многие расчеты взяты из старых тестов, суммы домножены на 2.

    create_completions(context, corp_client_id, month_minus2_end_dt)
    create_partner_oebs_completions(context, corp_contract_id, corp_client_id, month_minus2_end_dt)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, month_minus2_end_dt)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    # добавляем открутки
    create_completions(context, corp_client_id, month_minus1_end_dt, coef_2)
    create_completions(context, corp_client_id, month_minus1_end_dt, coef_1)
    create_partner_oebs_completions(context, corp_contract_id, corp_client_id, month_minus1_end_dt, coef_2)
    create_partner_oebs_completions(context, corp_contract_id, corp_client_id, month_minus1_end_dt, coef_1)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id,
                                                                   month_minus1_end_dt)

    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    consume_sums, final_sum_invoice, second_sum_act, first_sum_act = calc_final_sums(context)
    product_mapping = tsteps.TaxiSteps.get_product_mapping()

    expected_consumes = []
    for order_type, amount in consume_sums.iteritems():
        service = Services.TAXI_CORP if order_type == 'main' else Services.TAXI_CORP_CLIENTS
        expected_consumes.append(
            steps.CommonData.create_expected_consume_data(
                product_mapping[(service.id, context.currency.iso_code, order_type)],
                amount * 2,
                InvoiceType.PERSONAL_ACCOUNT
            )
        )

    # создаем шаблон для сравнения
    expected_invoice_data_second_month = steps.CommonData.create_expected_invoice_data_by_context(
        context, corp_contract_id, corp_person_id, final_sum_invoice * 2, dt=month_migration_minus2_start_dt
    )

    expected_act_data_third_month = steps.CommonData.create_expected_act_data(first_sum_act,
                                                                              month_minus2_end_dt)

    expected_act_data_fourth_month = steps.CommonData.create_expected_act_data(second_sum_act,
                                                                               month_minus1_end_dt)

    utils.check_that(consume_data, contains_dicts_with_entries(expected_consumes),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data_second_month),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month,
                                                            expected_act_data_second_month,
                                                            expected_act_data_third_month,
                                                            expected_act_data_fourth_month, ]),
                     'Сравниваем данные из акта с шаблоном')


# В открутках ОЕБС не предполагается отрицательных сумм, после миграции тест можно выпилить
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
], ids=lambda context: context.name)
def test_corp_taxi_ua_transfer_overact_to_main(context):
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

    payment_sum_w_nds_1 = D('120')
    payment_sum_w_nds_tlog_1 = D('240')

    payment_sum_w_nds_tlog_2 = D('360')
    refund_sum_w_nds_2 = D('12')
    refund_sum_w_nds_tlog_2 = D('24')

    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={'start_dt': month_migration_minus2_start_dt})

    # создаем платеж в tpt - станет откруткой на 135 сервисе (дочерний зазак)
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, month_migration_minus2_end_dt,
                                         [{'client_amount': payment_sum_w_nds_1,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.PAYMENT},
                                          ])

    # создаем открутки в tlog - станут открутками на 650 сервисе - на заказах с продуктами с соответствующими type.
    # client_b2b_trip_payment - главный заказ
    # cargo_client_b2b_trip_payment - дочерний заказ
    order_dicts_tlog = [
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': payment_sum_w_nds_tlog_1 / context.nds.koef_on_dt(month_migration_minus2_end_dt),
         'type': 'client_b2b_trip_payment',
         'dt': month_migration_minus2_end_dt,
         'transaction_dt': month_migration_minus2_end_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 100},
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': payment_sum_w_nds_tlog_1 / context.nds.koef_on_dt(month_migration_minus2_end_dt),
         'type': 'cargo_client_b2b_trip_payment',
         'dt': month_migration_minus2_end_dt,
         'transaction_dt': month_migration_minus2_end_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 110},
    ]
    tsteps.TaxiSteps.create_orders_tlog(corp_client_id, order_dicts_tlog)

    product_mapping = tsteps.TaxiSteps.get_product_mapping()

    # рассчитываем ожидаемые суммы на конзюмах и общую сумму по счету
    invoice_amount = D('0')
    expected_consumes = []
    if Services.TAXI_CORP.id in context.contract_services:
        expected_consumes.append(
            steps.CommonData.create_expected_consume_data(
                product_mapping[(Services.TAXI_CORP.id, context.currency.iso_code, 'main')],
                payment_sum_w_nds_1,
                InvoiceType.PERSONAL_ACCOUNT
            )
        )
        invoice_amount += payment_sum_w_nds_1

    expected_consumes.append(
        steps.CommonData.create_expected_consume_data(
            product_mapping[
                (Services.TAXI_CORP_CLIENTS.id, context.currency.iso_code, 'cargo_client_b2b_trip_payment')],
            payment_sum_w_nds_tlog_1,
            InvoiceType.PERSONAL_ACCOUNT
        )
    )
    invoice_amount += payment_sum_w_nds_tlog_1

    # главный заказ
    expected_consumes.append(
        steps.CommonData.create_expected_consume_data(
            product_mapping[(Services.TAXI_CORP_CLIENTS.id, context.currency.iso_code, 'client_b2b_trip_payment')],
            payment_sum_w_nds_tlog_1,
            InvoiceType.PERSONAL_ACCOUNT
        )
    )
    invoice_amount += payment_sum_w_nds_tlog_1

    # генерим акты за первый месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, month_migration_minus2_end_dt)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    # забираем полученные данные из базы
    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    # создаем шаблон для сравнения
    expected_invoice_data_first_month = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                                 corp_contract_id,
                                                                                                 corp_person_id,
                                                                                                 invoice_amount,
                                                                                                 dt=month_migration_minus2_start_dt)
    act_sum_first_month = invoice_amount
    expected_act_data_first_month = steps.CommonData.create_expected_act_data(act_sum_first_month,
                                                                              month_migration_minus2_end_dt)

    utils.check_that(consume_data, contains_dicts_with_entries(expected_consumes),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data_first_month),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month]),
                     'Сравниваем данные из акта с шаблоном')

    tlog_notches = tsteps.TaxiSteps.get_tlog_timeline_notch(contract_id=corp_contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    if Services.TAXI_CORP_CLIENTS.id in context.contract_services:
        max_last_transactions_ids = [110]
    else:
        max_last_transactions_ids = [0]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')

    # Второй месяц

    # создаем рефанд в tpt - станет откруткой на 135 сервисе (дочерний заказ), создаст переакт
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, month_migration_minus1_end_dt,
                                         [{'client_amount': refund_sum_w_nds_2,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.REFUND},
                                          ])

    # создаем открутки в tlog - станут открутками на 650 сервисе - на заказах с продуктами с соответствующими type.
    # client_b2b_trip_payment - главный заказ
    # cargo_client_b2b_trip_payment - дочерний заказ
    # на дочеренем создаем переакт, на главном - нормальное зачисление
    order_dicts_tlog = [
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': payment_sum_w_nds_tlog_2 / context.nds.koef_on_dt(month_migration_minus1_end_dt),
         'type': 'client_b2b_trip_payment',
         'dt': month_migration_minus1_end_dt,
         'transaction_dt': month_migration_minus1_end_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 200},
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': -refund_sum_w_nds_tlog_2 / context.nds.koef_on_dt(month_migration_minus1_end_dt),
         'type': 'cargo_client_b2b_trip_payment',
         'dt': month_migration_minus1_end_dt,
         'transaction_dt': month_migration_minus1_end_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 210},
    ]
    tsteps.TaxiSteps.create_orders_tlog(corp_client_id, order_dicts_tlog)

    # рассчитываем ожидаемые суммы на конзюмах и общую сумму по счету
    expected_consumes = []
    if Services.TAXI_CORP.id in context.contract_services:
        expected_consumes.append(
            steps.CommonData.create_expected_consume_data(
                product_mapping[(Services.TAXI_CORP.id, context.currency.iso_code, 'main')],
                payment_sum_w_nds_1 - refund_sum_w_nds_2,
                InvoiceType.PERSONAL_ACCOUNT,
            )
        )
        invoice_amount -= refund_sum_w_nds_2

    expected_consumes.append(
        steps.CommonData.create_expected_consume_data(
            product_mapping[
                (Services.TAXI_CORP_CLIENTS.id, context.currency.iso_code, 'cargo_client_b2b_trip_payment')],
            payment_sum_w_nds_tlog_1 - refund_sum_w_nds_tlog_2,
            InvoiceType.PERSONAL_ACCOUNT,
        )
    )
    invoice_amount -= refund_sum_w_nds_tlog_2

    # главный заказ
    expected_consumes.append(
        steps.CommonData.create_expected_consume_data(
            product_mapping[(Services.TAXI_CORP_CLIENTS.id, context.currency.iso_code, 'client_b2b_trip_payment')],
            payment_sum_w_nds_tlog_1 + payment_sum_w_nds_tlog_2,
            InvoiceType.PERSONAL_ACCOUNT,
        )
    )
    invoice_amount += payment_sum_w_nds_tlog_2

    # генерим акты за второй месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, month_migration_minus1_end_dt)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    # забираем полученные данные из базы
    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    # создаем шаблон для сравнения
    expected_invoice_data_second_month = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                                  corp_contract_id,
                                                                                                  corp_person_id,
                                                                                                  invoice_amount,
                                                                                                  dt=month_migration_minus2_start_dt)
    act_sum_second_month = invoice_amount - act_sum_first_month
    expected_act_data_second_month = steps.CommonData.create_expected_act_data(act_sum_second_month,
                                                                               month_migration_minus1_end_dt)

    utils.check_that(consume_data, contains_dicts_with_entries(expected_consumes),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data_second_month),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month,
                                                            expected_act_data_second_month]),
                     'Сравниваем данные из акта с шаблоном')

    tlog_notches = tsteps.TaxiSteps.get_tlog_timeline_notch(contract_id=corp_contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    if Services.TAXI_CORP_CLIENTS.id in context.contract_services:
        max_last_transactions_ids = [210, 110]
    else:
        max_last_transactions_ids = [0, 0]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')


# В открутках ОЕБС не предполагается отрицательных сумм, после миграции тест можно выпилить
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
], ids=lambda context: context.name)
def test_corp_taxi_ua_transfer_overact_all_orders(context):
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

    payment_sum_w_nds_1 = D('120')
    payment_sum_w_nds_tlog_1 = D('240')

    refund_sum_w_nds_2 = D('12')
    refund_sum_w_nds_tlog_2 = D('24')

    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={'start_dt': month_migration_minus2_start_dt})

    # создаем платеж в tpt - станет откруткой на 135 сервисе (дочерний зазак)
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, month_migration_minus2_end_dt,
                                         [{'client_amount': payment_sum_w_nds_1,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.PAYMENT},
                                          ])

    # создаем открутки в tlog - станут открутками на 650 сервисе - на заказах с продуктами с соответствующими type.
    # client_b2b_trip_payment - главный заказ
    # cargo_client_b2b_trip_payment - дочерний заказ
    order_dicts_tlog = [
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': payment_sum_w_nds_tlog_1 / context.nds.koef_on_dt(month_migration_minus2_end_dt),
         'type': 'client_b2b_trip_payment',
         'dt': month_migration_minus2_end_dt,
         'transaction_dt': month_migration_minus2_end_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 100},
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': payment_sum_w_nds_tlog_1 / context.nds.koef_on_dt(month_migration_minus2_end_dt),
         'type': 'cargo_client_b2b_trip_payment',
         'dt': month_migration_minus2_end_dt,
         'transaction_dt': month_migration_minus2_end_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 110},
    ]
    tsteps.TaxiSteps.create_orders_tlog(corp_client_id, order_dicts_tlog)

    product_mapping = tsteps.TaxiSteps.get_product_mapping()

    # рассчитываем ожидаемые суммы на конзюмах и общую сумму по счету
    invoice_amount = D('0')
    expected_consumes = []
    if Services.TAXI_CORP.id in context.contract_services:
        expected_consumes.append(
            steps.CommonData.create_expected_consume_data(
                product_mapping[(Services.TAXI_CORP.id, context.currency.iso_code, 'main')],
                payment_sum_w_nds_1,
                InvoiceType.PERSONAL_ACCOUNT
            )
        )
        invoice_amount += payment_sum_w_nds_1

    expected_consumes.append(
        steps.CommonData.create_expected_consume_data(
            product_mapping[
                (Services.TAXI_CORP_CLIENTS.id, context.currency.iso_code, 'cargo_client_b2b_trip_payment')],
            payment_sum_w_nds_tlog_1,
            InvoiceType.PERSONAL_ACCOUNT
        )
    )
    invoice_amount += payment_sum_w_nds_tlog_1

    # главный заказ
    expected_consumes.append(
        steps.CommonData.create_expected_consume_data(
            product_mapping[(Services.TAXI_CORP_CLIENTS.id, context.currency.iso_code, 'client_b2b_trip_payment')],
            payment_sum_w_nds_tlog_1,
            InvoiceType.PERSONAL_ACCOUNT
        )
    )
    invoice_amount += payment_sum_w_nds_tlog_1

    # генерим акты за первый месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, month_migration_minus2_end_dt)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    # забираем полученные данные из базы
    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    # создаем шаблон для сравнения
    expected_invoice_data_first_month = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                                 corp_contract_id,
                                                                                                 corp_person_id,
                                                                                                 invoice_amount,
                                                                                                 dt=month_migration_minus2_start_dt)
    act_sum_first_month = invoice_amount
    expected_act_data_first_month = steps.CommonData.create_expected_act_data(act_sum_first_month,
                                                                              month_migration_minus2_end_dt)

    utils.check_that(consume_data, contains_dicts_with_entries(expected_consumes),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data_first_month),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month]),
                     'Сравниваем данные из акта с шаблоном')

    tlog_notches = tsteps.TaxiSteps.get_tlog_timeline_notch(contract_id=corp_contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    if Services.TAXI_CORP_CLIENTS.id in context.contract_services:
        max_last_transactions_ids = [110]
    else:
        max_last_transactions_ids = [0]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')

    # Второй месяц

    # создаем рефанд в tpt - станет откруткой на 135 сервисе (дочерний заказ), создаст переакт
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, month_migration_minus1_end_dt,
                                         [{'client_amount': refund_sum_w_nds_2,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.REFUND},
                                          ])

    # создаем открутки в tlog - станут открутками на 650 сервисе - на заказах с продуктами с соответствующими type.
    # client_b2b_trip_payment - главный заказ
    # cargo_client_b2b_trip_payment - дочерний заказ
    # переакт на всех заказах
    order_dicts_tlog = [
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': -refund_sum_w_nds_tlog_2 / context.nds.koef_on_dt(month_migration_minus1_end_dt),
         'type': 'client_b2b_trip_payment',
         'dt': month_migration_minus1_end_dt,
         'transaction_dt': month_migration_minus1_end_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 200},
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': -refund_sum_w_nds_tlog_2 / context.nds.koef_on_dt(month_migration_minus1_end_dt),
         'type': 'cargo_client_b2b_trip_payment',
         'dt': month_migration_minus1_end_dt,
         'transaction_dt': month_migration_minus1_end_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 210},
    ]
    tsteps.TaxiSteps.create_orders_tlog(corp_client_id, order_dicts_tlog)

    # рассчитываем ожидаемые суммы на конзюмах и общую сумму по счету
    transfer_to_main_sum = D('0')
    expected_consumes = []
    if Services.TAXI_CORP.id in context.contract_services:
        expected_consumes.append(
            steps.CommonData.create_expected_consume_data(
                product_mapping[(Services.TAXI_CORP.id, context.currency.iso_code, 'main')],
                payment_sum_w_nds_1 - refund_sum_w_nds_2,
                InvoiceType.PERSONAL_ACCOUNT,
                act_qty=payment_sum_w_nds_1,
                act_sum=payment_sum_w_nds_1,
            )
        )
        # invoice_amount -= refund_sum_w_nds_2
        transfer_to_main_sum += refund_sum_w_nds_2

    expected_consumes.append(
        steps.CommonData.create_expected_consume_data(
            product_mapping[
                (Services.TAXI_CORP_CLIENTS.id, context.currency.iso_code, 'cargo_client_b2b_trip_payment')],
            payment_sum_w_nds_tlog_1 - refund_sum_w_nds_tlog_2,
            InvoiceType.PERSONAL_ACCOUNT,
            act_qty=payment_sum_w_nds_tlog_1,
            act_sum=payment_sum_w_nds_tlog_1,
        )
    )
    # invoice_amount -= refund_sum_w_nds_tlog_2
    transfer_to_main_sum += refund_sum_w_nds_tlog_2

    # главный заказ
    expected_consumes.append(
        steps.CommonData.create_expected_consume_data(
            product_mapping[(Services.TAXI_CORP_CLIENTS.id, context.currency.iso_code, 'client_b2b_trip_payment')],
            payment_sum_w_nds_tlog_1 + transfer_to_main_sum,
            InvoiceType.PERSONAL_ACCOUNT,
            completion_qty=payment_sum_w_nds_tlog_1 - refund_sum_w_nds_tlog_2,
            completion_sum=payment_sum_w_nds_tlog_1 - refund_sum_w_nds_tlog_2,
            act_qty=payment_sum_w_nds_tlog_1,
            act_sum=payment_sum_w_nds_tlog_1,
        )
    )
    # invoice_amount += payment_sum_w_nds_tlog_2

    # генерим акты за второй месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, month_migration_minus1_end_dt)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    # забираем полученные данные из базы
    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(corp_client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    # создаем шаблон для сравнения
    expected_invoice_data_second_month = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                                  corp_contract_id,
                                                                                                  corp_person_id,
                                                                                                  invoice_amount,
                                                                                                  dt=month_migration_minus2_start_dt)
    # act_sum_second_month = invoice_amount - act_sum_first_month
    # expected_act_data_second_month = steps.CommonData.create_expected_act_data(act_sum_second_month,
    #                                                                            end_dt_2)

    utils.check_that(consume_data, contains_dicts_with_entries(expected_consumes),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data_second_month),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month]),
                     'Сравниваем данные из акта с шаблоном')

    tlog_notches = tsteps.TaxiSteps.get_tlog_timeline_notch(contract_id=corp_contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    if Services.TAXI_CORP_CLIENTS.id in context.contract_services:
        max_last_transactions_ids = [210, 110]
    else:
        max_last_transactions_ids = [0, 0]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')


# После миграции на открутки ОЕБС данные будут отдаваться в разрезе договоров, тест выпилить
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-26237')
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
], ids=lambda context: context.name)
def test_act_corp_taxi_two_contracts(context):
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

    contract_1_finish_dt = month_migration_minus2_start_dt.replace(day=8)
    contract_1_end_dt = month_migration_minus2_start_dt.replace(day=7)

    amount_contract1_1 = D('400')
    amount_contract1_2 = D('23')
    amount_contract2_1 = D('1000')
    amount_contract2_2 = D('154')

    amount_contract1_1_tlog = D('566.4')
    amount_contract1_2_tlog = D('42.48')
    amount_contract2_1_tlog = D('1416')
    amount_contract2_2_tlog = D('219.48')

    corp_client_id, corp_person_id, corp_contract_id1, _ = \
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={'start_dt': month_migration_minus2_start_dt,
                                                                       'finish_dt': contract_1_finish_dt})

    # создаем второй договор с корп клиентом с датой начала = finish_dt первого
    _, _, corp_contract_id2, _, = steps.ContractSteps.create_partner_contract(context, person_id=corp_person_id,
                                                                              client_id=corp_client_id,
                                                                              additional_params={
                                                                                  'start_dt': contract_1_finish_dt})

    expected_amount_1 = 0
    expected_amount_2 = 0
    # добавляем открутки
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, month_migration_minus2_start_dt,
                                         [{'client_amount': amount_contract1_1,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.PAYMENT}])
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, contract_1_end_dt,
                                         [{'client_amount': amount_contract1_2,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.PAYMENT}])
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, contract_1_finish_dt,
                                         [{'client_amount': amount_contract2_1,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.PAYMENT}])
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, month_migration_minus2_end_dt,
                                         [{'client_amount': amount_contract2_2,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.PAYMENT}])

    if Services.TAXI_CORP.id in context.contract_services:
        expected_amount_1 += amount_contract1_1 + amount_contract1_2
        expected_amount_2 += amount_contract2_1 + amount_contract2_2

    order_dicts_tlog = [
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': amount_contract1_1_tlog / context.nds.koef_on_dt(month_migration_minus2_start_dt),
         'type': CorpTaxiOrderType.commission,
         'dt': month_migration_minus2_start_dt,
         'transaction_dt': month_migration_minus2_start_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 50},
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': amount_contract1_2_tlog / context.nds.koef_on_dt(contract_1_end_dt),
         'type': CorpTaxiOrderType.commission,
         'dt': contract_1_end_dt,
         'transaction_dt': contract_1_end_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 60},
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': amount_contract2_1_tlog / context.nds.koef_on_dt(contract_1_finish_dt),
         'type': CorpTaxiOrderType.commission,
         'dt': contract_1_finish_dt,
         'transaction_dt': contract_1_finish_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 70},
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': amount_contract2_2_tlog / context.nds.koef_on_dt(month_migration_minus2_end_dt),
         'type': CorpTaxiOrderType.commission,
         'dt': month_migration_minus2_end_dt,
         'transaction_dt': month_migration_minus2_end_dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': 80},
    ]
    tsteps.TaxiSteps.create_orders_tlog(corp_client_id, order_dicts_tlog)

    if Services.TAXI_CORP_CLIENTS.id in context.contract_services:
        expected_amount_1 += amount_contract1_1_tlog + amount_contract1_2_tlog
        expected_amount_2 += amount_contract2_1_tlog + amount_contract2_2_tlog

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id1,
                                                                   month_migration_minus2_end_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id2,
                                                                   month_migration_minus2_end_dt)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    # # создаем шаблон для сравнения
    expected_invoice_data = [steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                      corp_contract_id1, corp_person_id,
                                                                                      expected_amount_1,
                                                                                      dt=month_migration_minus2_start_dt),
                             steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                      corp_contract_id2, corp_person_id,
                                                                                      expected_amount_2,
                                                                                      dt=contract_1_finish_dt)]

    expected_act_data = [steps.CommonData.create_expected_act_data(expected_amount_1, month_migration_minus2_end_dt),
                         steps.CommonData.create_expected_act_data(expected_amount_2, month_migration_minus2_end_dt)]

    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')

    tlog_notches = tsteps.TaxiSteps.get_tlog_timeline_notch(contract_id=corp_contract_id1)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    if Services.TAXI_CORP_CLIENTS.id in context.contract_services:
        max_last_transactions_ids = [60]
    else:
        max_last_transactions_ids = [0]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')

    tlog_notches = tsteps.TaxiSteps.get_tlog_timeline_notch(contract_id=corp_contract_id2)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    if Services.TAXI_CORP_CLIENTS.id in context.contract_services:
        max_last_transactions_ids = [80]
    else:
        max_last_transactions_ids = [0]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-26541')
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL_DECOUP,
    CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
], ids=lambda context: context.name)
def test_act_corp_taxi_no_acts_checkbox(context):
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
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={'start_dt': month_migration_minus2_start_dt,
                                                                       'no_acts': 1})

    create_act_first_month(corp_client_id, corp_contract_id, context, month_migration_minus2_end_dt)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     corp_contract_id, corp_person_id,
                                                                                     D('0'),
                                                                                     dt=month_migration_minus2_start_dt)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')

    # НОВАЯ ЛОГИКА - переход на ОЕБСовые агрегаты
    # суммы откруток по продуктам сделаны такими же, как в старых открутках (только в тлоге суммы без НДС, а у ОЕБС с НДС)
    # поэтому многие расчеты взяты из старых тестов, суммы домножены на 2.

    create_completions(context, corp_client_id, month_minus2_end_dt)
    create_partner_oebs_completions(context, corp_contract_id, corp_client_id, month_minus2_end_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, month_minus2_end_dt)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                     corp_contract_id,
                                                                                     corp_person_id,
                                                                                     D('0'),
                                                                                     dt=month_migration_minus2_start_dt)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')
