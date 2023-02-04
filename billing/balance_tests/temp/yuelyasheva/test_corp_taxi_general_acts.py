# -*- coding: utf-8 -*-

__author__ = 'atkaya'

from decimal import Decimal as D

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType, InvoiceType
from btestlib.data import defaults
from btestlib.data.defaults import GeneralPartnerContractDefaults as GenParams
from btestlib.data.simpleapi_defaults import ThirdPartyData
from btestlib.matchers import equal_to_casted_dict, contains_dicts_equal_to, contains_dicts_with_entries
from temp.igogor.balance_objects import Contexts
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL, CORP_TAXI_KZ_CONTEXT_GENERAL

AMOUNT = D('5000')

FAKE_TAXI_CLIENT_ID = 1111
FAKE_TAXI_PERSON_ID = 2222
FAKE_TAXI_CONTRACT_ID = 3333

payment_sum1 = D('6000.7')
refund_sum1 = D('2000.1')

payment_sum2 = D('2500.4')
refund_sum2 = D('1533.5')

payment_sum_internal = D('100.4')
refund_sum_internal = D('80.4')

payment_sum2_prev_month = D('399.2')
refund_sum2_prev_month = D('12.4')

final_sum_first_month = payment_sum1 - refund_sum1
final_sum_second_month = payment_sum2 - refund_sum2 + payment_sum2_prev_month - refund_sum2_prev_month

start_dt_1, end_dt_1, start_dt_2, end_dt_2, _, _ = utils.Date.previous_three_months_start_end_dates()

common_act_data = {'type': defaults.taxi_corp()['ACT_TYPE']}


def create_client_persons_contracts_prepay(context, additional_params, client_id=None, person_id=None, is_postpay=1, is_offer=False):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        client_id=client_id,
        person_id=person_id,
        is_postpay=is_postpay,
        is_offer=is_offer,
        additional_params=additional_params)
    return client_id, contract_id, person_id


def corporate_acts_data_creation(context, finish_dt=None, no_acts=None):
    # создаем клиента и плательщика для корпоративного клиента
    params = {'start_dt': start_dt_1, 'personal_account': 1, 'no_acts': no_acts, 'finish_dt': finish_dt,
              'end_dt': finish_dt}
    corp_client_id, corp_contract_id, corp_person_id = create_client_persons_contracts_prepay(context, params)

    return corp_client_id, corp_person_id, corp_contract_id


def get_common_invoice_data(context):
    common_invoice_data = {'currency': context.currency.char_code,
                           'firm_id': context.firm.id,
                           'paysys_id': context.paysys.id,
                           'type': InvoiceType.PERSONAL_ACCOUNT,
                           'nds_pct': context.nds,
                           'nds': 1}
    return common_invoice_data


def create_completions(context, corp_client_id, dt, payment_sum, refund_sum):
    # добавляем открутки
    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                             FAKE_TAXI_CLIENT_ID, dt,
                                             [{'client_amount': payment_sum_internal,
                                               'client_id': corp_client_id,
                                               'amount': AMOUNT,
                                               'transaction_type': TransactionType.PAYMENT,
                                               'internal': 1},
                                              {'client_amount': 0,
                                               'client_id': corp_client_id,
                                               'amount': AMOUNT,
                                               'transaction_type': TransactionType.PAYMENT,
                                               'internal': 1},
                                              {'client_amount': refund_sum_internal,
                                               'client_id': corp_client_id,
                                               'amount': AMOUNT,
                                               'transaction_type': TransactionType.REFUND,
                                               'internal': 1}
                                              ], sum_key='client_amount')
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, dt,
                                         [{'client_amount': payment_sum,
                                           'client_id': corp_client_id,
                                           'amount': AMOUNT,
                                           'transaction_type': TransactionType.PAYMENT},
                                          {'client_amount': 0,
                                           'client_id': corp_client_id,
                                           'amount': AMOUNT,
                                           'transaction_type': TransactionType.PAYMENT},
                                          {'client_amount': refund_sum,
                                           'client_id': corp_client_id,
                                           'amount': AMOUNT,
                                           'transaction_type': TransactionType.REFUND}
                                          ], sum_key='client_amount')


def create_act_first_month(corp_client_id, corp_contract_id, context):
    create_completions(context, corp_client_id, end_dt_1, payment_sum1, refund_sum1)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, end_dt_1)


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-22114', 'BALANCE-27811')
@pytest.mark.parametrize('context', [
    (CORP_TAXI_RU_CONTEXT_GENERAL),
    (CORP_TAXI_KZ_CONTEXT_GENERAL),
], ids=[
    'Corp client acts Yandex.Taxi',
    'Corp client acts Taxi Uber KZT'
])
def test_act_corp_taxi_first_month_wo_data(context):
    corp_client_id, corp_person_id, corp_contract_id = corporate_acts_data_creation(context)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, end_dt_1)

    # ждем конца генерации акта
    steps.CommonSteps.wait_for_export('MONTH_PROC', corp_client_id)

    # проверяем данные в счете
    invoice_data_third_month = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_third_month = steps.ActsSteps.get_act_data_by_client(corp_client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data = get_common_invoice_data(context)
    expected_invoice_data.update({'contract_id': corp_contract_id,
                                  'total_act_sum': 0,
                                  'consume_sum': 0,
                                  'person_id': corp_person_id,
                                  })

    utils.check_that(invoice_data_third_month, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_third_month, empty(),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.smoke
@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-22114', 'BALANCE-27811')
@pytest.mark.parametrize('context, general_params, thirdparty_data', [
    (Contexts.TAXI_RU_CONTEXT, GenParams.YANDEX_TAXI_CORP, ThirdPartyData.TAXI_CORP),
    (Contexts.TAXI_UBER_KZT_CONTEXT, GenParams.TAXI_UBER_KZT_CORP, ThirdPartyData.TAXI_CORP_KZT),
], ids=[
    'Corp client acts Yandex.Taxi',
    'Corp client acts Taxi Uber KZT'
])
def act_corp_taxi_first_month(context, general_params, thirdparty_data):
    corp_client_id, corp_person_id, corp_contract_id = corporate_acts_data_creation(general_params)

    create_act_first_month(corp_client_id, corp_contract_id, thirdparty_data)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    # проверяем данные в счете
    invoice_data_first_month = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_fisrt_month = steps.ActsSteps.get_act_data_by_client(corp_client_id)[0]

    if context == Contexts.TAXI_RU_CONTEXT:
        final_sum = final_sum_first_month
    else:
        final_sum = final_sum_first_month + payment_sum_internal - refund_sum_internal

    # создаем шаблон для сравнения
    expected_invoice_data_fisrt_month = get_common_invoice_data(context)
    expected_invoice_data_fisrt_month.update({'consume_sum': final_sum,
                                              'contract_id': corp_contract_id,
                                              'person_id': corp_person_id,
                                              'total_act_sum': final_sum
                                              })

    expected_act_data_first_month = common_act_data.copy()
    expected_act_data_first_month.update({'act_sum': final_sum,
                                          'amount': final_sum,
                                          'dt': end_dt_1})

    utils.check_that(invoice_data_first_month, equal_to_casted_dict(expected_invoice_data_fisrt_month),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_fisrt_month, equal_to_casted_dict(expected_act_data_first_month),
                     'Сравниваем данные из акта с шаблоном')


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-22114', 'BALANCE-27811')
@pytest.mark.parametrize('context', [
    (CORP_TAXI_RU_CONTEXT_GENERAL),
    (CORP_TAXI_KZ_CONTEXT_GENERAL),
], ids=[
    'Corp client acts Yandex.Taxi',
    'Corp client acts Taxi Uber KZT'
])
def test_act_corp_taxi_second_month(context):
    corp_client_id, corp_person_id, corp_contract_id = corporate_acts_data_creation(context)

    create_act_first_month(corp_client_id, corp_contract_id, context)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    # добавляем открутки
    create_completions(context, corp_client_id, end_dt_2, payment_sum2, refund_sum2)
    create_completions(context, corp_client_id, end_dt_1, payment_sum2_prev_month, refund_sum2_prev_month)

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id, end_dt_2)

    # ждем конца генерации акта
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    # проверяем данные в счете
    invoice_data_second_month = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_second_month = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    if context == CORP_TAXI_RU_CONTEXT_GENERAL:
        final_sum_invoice = final_sum_second_month + final_sum_first_month
        final_sum_act = final_sum_second_month
        first_sum_act = final_sum_first_month
    else:
        final_sum_invoice = final_sum_second_month + final_sum_first_month + 3 * payment_sum_internal \
                            - 3 * refund_sum_internal
        final_sum_act = final_sum_second_month + 2 * payment_sum_internal - 2 * refund_sum_internal
        first_sum_act = final_sum_first_month + payment_sum_internal - refund_sum_internal


    # создаем шаблон для сравнения
    expected_invoice_data_second_month = get_common_invoice_data(context)
    expected_invoice_data_second_month.update({'consume_sum': final_sum_invoice,
                                               'contract_id': corp_contract_id,
                                               'person_id': corp_person_id,
                                               'total_act_sum': final_sum_invoice})

    expected_act_data_first_month = common_act_data.copy()
    expected_act_data_first_month.update({'act_sum': first_sum_act,
                                           'amount': first_sum_act,
                                           'dt': end_dt_1})

    expected_act_data_second_month = common_act_data.copy()
    expected_act_data_second_month.update({'act_sum': final_sum_act,
                                           'amount': final_sum_act,
                                           'dt': end_dt_2})

    utils.check_that(invoice_data_second_month, equal_to_casted_dict(expected_invoice_data_second_month),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_second_month, contains_dicts_with_entries([expected_act_data_first_month,
                                                                         expected_act_data_second_month]),
                     'Сравниваем данные из акта с шаблоном')


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-26237')
def test_act_corp_taxi_two_contracts():
    context = CORP_TAXI_RU_CONTEXT_GENERAL
    contract_1_finish_dt = start_dt_1.replace(day=8)
    contract_1_end_dt = start_dt_1.replace(day=7)

    amount_contract1_1 = D('400')
    amount_contract1_2 = D('23')
    amount_contract2_1 = D('1000')
    amount_contract2_2 = D('154')

    corp_client_id, corp_person_id, corp_contract_id1 = corporate_acts_data_creation(context,
                                                                                     finish_dt=contract_1_finish_dt)

    # def create_client_persons_contracts_prepay(context, additional_params, client_id=None, person_id=None, is_postpay=1,
    #                                            is_offer=False):
    # создаем второй договор с корп клиентом с датой начала = finish_dt первого
    _, corp_contract_id2, _, = create_client_persons_contracts_prepay(context, person_id=corp_person_id,
                                                                     client_id=corp_client_id,
                                                                     additional_params={'start_dt': contract_1_finish_dt,
                                                                                        'personal_account': 1,
                                                                                        'no_acts': None})

    # добавляем открутки
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, start_dt_1,
                                         [{'client_amount': amount_contract1_1,
                                           'client_id': corp_client_id,
                                           'amount': '1000',
                                           'transaction_type': TransactionType.PAYMENT}], sum_key='client_amount')
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, contract_1_end_dt,
                                         [{'client_amount': amount_contract1_2,
                                           'client_id': corp_client_id,
                                           'amount': '1000',
                                           'transaction_type': TransactionType.PAYMENT}], sum_key='client_amount')
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, contract_1_finish_dt,
                                         [{'client_amount': amount_contract2_1,
                                           'client_id': corp_client_id,
                                           'amount': '1000',
                                           'transaction_type': TransactionType.PAYMENT}], sum_key='client_amount')
    steps.SimpleApi.create_fake_tpt_data(context, FAKE_TAXI_CONTRACT_ID, FAKE_TAXI_PERSON_ID,
                                         FAKE_TAXI_CLIENT_ID, end_dt_1,
                                         [{'client_amount': amount_contract2_2,
                                           'client_id': corp_client_id,
                                           'amount': '1000',
                                           'transaction_type': TransactionType.PAYMENT}], sum_key='client_amount')

    # запускаем конец месяца для корпоративного договора
    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id1, end_dt_1)
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    steps.CommonPartnerSteps.generate_partner_acts_fair(corp_contract_id2, end_dt_1)

    # ждем конца генерации акта
    steps.CommonSteps.export('MONTH_PROC', 'Client', corp_client_id)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(corp_client_id)

    # # создаем шаблон для сравнения
    expected_invoice_data = [get_common_invoice_data(context),
                             get_common_invoice_data(context)]
    expected_invoice_data[0].update({'consume_sum': amount_contract1_1 + amount_contract1_2,
                                     'contract_id': corp_contract_id1,
                                     'person_id': corp_person_id,
                                     'total_act_sum': amount_contract1_1 + amount_contract1_2})
    expected_invoice_data[1].update({'consume_sum': amount_contract2_1 + amount_contract2_2,
                                     'contract_id': corp_contract_id2,
                                     'person_id': corp_person_id,
                                     'total_act_sum': amount_contract2_1 + amount_contract2_2})

    expected_act_data = [common_act_data.copy(), common_act_data.copy()]
    expected_act_data[0].update({'act_sum': amount_contract1_1 + amount_contract1_2,
                                 'amount': amount_contract1_1 + amount_contract1_2,
                                 'dt': end_dt_1})
    expected_act_data[1].update({'act_sum': amount_contract2_1 + amount_contract2_2,
                                 'amount': amount_contract2_1 + amount_contract2_2,
                                 'dt': end_dt_1})

    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_equal_to(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-26541')
def test_act_corp_taxi_no_acts_checkbox():
    context = CORP_TAXI_RU_CONTEXT_GENERAL
    corp_client_id, corp_person_id, corp_contract_id = corporate_acts_data_creation(context, no_acts=1)

    create_act_first_month(corp_client_id, corp_contract_id, context)

    # ждем конца генерации акта
    steps.CommonSteps.wait_for_export('MONTH_PROC', corp_client_id)

    # проверяем данные в счете
    invoice_data_third_month = steps.InvoiceSteps.get_invoice_data_by_client(corp_client_id)[0]

    # проверяем данные в акте
    act_data_third_month = steps.ActsSteps.get_act_data_by_client(corp_client_id)
    # готовим ожидаемые данные для счёта
    expected_invoice_data = get_common_invoice_data(context)
    expected_invoice_data.update({'contract_id': corp_contract_id,
                                  'total_act_sum': 0,
                                  'consume_sum': 0,
                                  'person_id': corp_person_id,
                                  })

    utils.check_that(invoice_data_third_month, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data_third_month, empty(),
                     'Сравниваем данные из акта с шаблоном')


if __name__ == "__main__":
    test_act_corp_taxi_second_month()
