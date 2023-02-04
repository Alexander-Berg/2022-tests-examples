# -*- coding: utf-8 -*-

__author__ = 'atkaya'

from copy import deepcopy
from decimal import Decimal as D


import pytest
from hamcrest import empty

import balance.balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as tsteps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType, Services, CorpTaxiOrderType
from btestlib.matchers import equal_to_casted_dict, contains_dicts_equal_to, contains_dicts_with_entries, equal_to
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL, CORP_TAXI_KZ_CONTEXT_GENERAL

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


def create_completions(context, corp_client_id, dt, coef=D('1')):
    # добавляем открутки
    if context.name == CORP_TAXI_KZ_CONTEXT_GENERAL.name:
        steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
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
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, dt,
                                         [{'client_amount': payment_sum * coef,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.PAYMENT},
                                          {'client_amount': refund_sum * coef,
                                           'client_id': corp_client_id,
                                           'transaction_type': TransactionType.REFUND}
                                          ])


def create_completions_tlog(context, corp_client_id, dt, coef=D('1'), last_transaction_id=100):
    order_dicts_tlog = [
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': payment_sum_tlog / context.nds.koef_on_dt(dt) * coef,
         'type': CorpTaxiOrderType.commission,
         'dt': dt,
         'transaction_dt': dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': last_transaction_id-1},
        {'service_id': Services.TAXI_CORP_CLIENTS.id,
         'amount': -refund_sum_tlog / context.nds.koef_on_dt(dt) * coef,
         'type': CorpTaxiOrderType.commission,
         'dt': dt,
         'transaction_dt': dt,
         'currency': context.currency.iso_code,
         'last_transaction_id': last_transaction_id},
    ]
    tsteps.TaxiSteps.create_orders_tlog(corp_client_id, order_dicts_tlog)


def create_act_first_month(corp_client_id, corp_contract_id, context):
    create_completions(context, corp_client_id, end_dt_1)
    # create_completions_tlog(context, corp_client_id, end_dt_1, last_transaction_id=100)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, end_dt_1)


def calc_final_sums(context):
    act_first_month = D('0')
    act_second_month = D('0')
    act_first_month += payment_sum - refund_sum
    act_second_month += (payment_sum - refund_sum + payment_sum_tlog - refund_sum_tlog) * (coef_1 + coef_2)
    # act_second_month += (payment_sum_tlog - refund_sum_tlog) * (coef_2)

    invoice_sum = act_first_month + act_second_month
    return invoice_sum, act_second_month, act_first_month


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-22114', 'BALANCE-27811')
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL,
    CORP_TAXI_KZ_CONTEXT_GENERAL,
], ids=[
    'Corp client acts Yandex.Taxi',
    'Corp client acts Taxi Uber KZT'
])
@pytest.mark.parametrize('contract_services', [
    [Services.TAXI_CORP.id],
], ids=[
    'OLD_CORP',
])
def test_act_corp_taxi_first_month_wo_data(context, contract_services):
    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={'start_dt': start_dt_1})

    api.test_balance().MigrateContractToDecoupling(corp_contract_id)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id, end_dt_1, manual_export=False)

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
    utils.check_that(act_data_third_month, empty(),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.smoke
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-22114', 'BALANCE-27811')
@pytest.mark.parametrize('context', [
    CORP_TAXI_RU_CONTEXT_GENERAL,
], ids=[
    'Corp client acts Yandex.Taxi',
])
@pytest.mark.parametrize('contract_services', [
    [Services.TAXI_CORP.id],
], ids=[
    'OLD_CORP',
])
def test_act_corp_taxi_second_month(context, contract_services):
    context = context.new(special_contract_params=deepcopy(context.special_contract_params),
                          contract_services=contract_services)
    if Services.TAXI_CORP_CLIENTS.id in contract_services:
        context.special_contract_params.pop('ctype', None)
    corp_client_id, corp_person_id, corp_contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={'start_dt': start_dt_1})

    create_act_first_month(corp_client_id, corp_contract_id, context)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    api.test_balance().MigrateContractToDecoupling(corp_contract_id)

    # добавляем открутки
    create_completions(context, corp_client_id, end_dt_2, coef_2)
    create_completions(context, corp_client_id, end_dt_1, coef_1)
    create_completions_tlog(context, corp_client_id, end_dt_2, coef_2, last_transaction_id=200)
    create_completions_tlog(context, corp_client_id, end_dt_1, coef_1, last_transaction_id=300)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(corp_client_id, corp_contract_id, end_dt_2)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    final_sum_invoice, second_sum_act, first_sum_act = calc_final_sums(context)

    # создаем шаблон для сравнения
    expected_invoice_data_second_month = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                                                  corp_contract_id,
                                                                                                  corp_person_id,
                                                                                                  final_sum_invoice,
                                                                                                  dt=start_dt_1)

    expected_act_data_first_month = steps.CommonData.create_expected_act_data(first_sum_act,
                                                                              end_dt_1)

    expected_act_data_second_month = steps.CommonData.create_expected_act_data(second_sum_act,
                                                                               end_dt_2)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data_second_month),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries([expected_act_data_first_month,
                                                            expected_act_data_second_month]),
                     'Сравниваем данные из акта с шаблоном')

    tlog_notches = tsteps.TaxiSteps.get_tlog_timeline_notch(contract_id=corp_contract_id)
    last_transaction_ids = [n['last_transaction_id'] for n in tlog_notches]
    max_last_transactions_ids = [300, 0]
    utils.check_that(last_transaction_ids, equal_to(max_last_transactions_ids),
                     'Сравниваем last_transaction_id с ожидаемым')


if __name__ == "__main__":
    test_act_corp_taxi_second_month()
