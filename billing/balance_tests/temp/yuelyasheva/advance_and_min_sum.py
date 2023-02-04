# -*- coding: utf-8 -*-
from decimal import Decimal as D

import pytest
from decimal import Decimal
from datetime import datetime
from dateutil.relativedelta import relativedelta
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType, InvoiceType, Services
from btestlib.data import defaults
from btestlib.data.defaults import GeneralPartnerContractDefaults as GenParams
from btestlib.data.simpleapi_defaults import ThirdPartyData
from btestlib.matchers import equal_to_casted_dict, contains_dicts_equal_to, contains_dicts_with_entries
from temp.igogor.balance_objects import Contexts
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL, CORP_TAXI_KZ_CONTEXT_GENERAL


# эту сумму зачисляем на счёт
PERSONAL_ACC_SUM = Decimal('41.05')
# суммы для откруток
PAYMENT_SUM = Decimal('21.63')
REFUND_SUM = Decimal('1.89')
PAYMENT_SUM_INT = Decimal('5.3')
REFUND_SUM_INT = Decimal('2.7')

CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
MONTH_BEFORE_PREV_START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=2))
MONTH_BEFORE_PREV_END_DT = utils.Date.last_day_of_month(datetime.now() - relativedelta(months=2))
PREVIUS_MONTH_START_DT, PREVIUS_MONTH_END_DT = utils.Date.previous_month_first_and_last_days(datetime.today())
CURRENT_MONTH_START_DT, CURRENT_MONTH_END_DT = utils.Date.current_month_first_and_last_days()

FAKE_TPT_CONTRACT_ID = 123
FAKE_TPT_CLIENT_ID = 123
FAKE_TPT_PERSON_ID = 123

start_dt_1, end_dt_1, start_dt_2, end_dt_2, _, _ = utils.Date.previous_three_months_start_end_dates()

MIN_COST = D('100')

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

common_act_data = {'type': defaults.taxi_corp()['ACT_TYPE']}

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



def create_client_persons_contracts_prepay(context, additional_params, client_id=None, person_id=None,
                                           is_postpay=1, is_offer=False):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        client_id=client_id,
        person_id=person_id,
        is_postpay=is_postpay,
        is_offer=is_offer,
        additional_params=additional_params)
    return client_id, contract_id, person_id


# переименовать
def process_payment(dt, client_id, contract_id,
                    personal_acc_payment=0.0, payment_sum=0.0,
                    refund_sum=0.0, context=CORP_TAXI_RU_CONTEXT_GENERAL, process=True):
    first_month_day, last_month_day = utils.Date.current_month_first_and_last_days(dt)
    invoice_id, external_invoice_id = steps.InvoiceSteps.get_invoice_ids(client_id)
    steps.InvoiceSteps.pay(invoice_id, payment_sum=personal_acc_payment, payment_dt=first_month_day)

    if context == CORP_TAXI_KZ_CONTEXT_GENERAL:
        if payment_sum:
            steps.SimpleApi.create_fake_tpt_data(context, client_id, 666,
                                                 contract_id, first_month_day + relativedelta(days=1),
                                                 [{'client_amount': PAYMENT_SUM_INT,
                                                   'client_id': client_id,
                                                   'amount': 666,
                                                   'transaction_type': TransactionType.PAYMENT,
                                                   'internal': 1}])
        if refund_sum:
            steps.SimpleApi.create_fake_tpt_data(context, client_id, 666,
                                                 contract_id, first_month_day + relativedelta(days=2),
                                                 [{'client_amount': REFUND_SUM_INT,
                                                   'client_id': client_id,
                                                   'amount': 666,
                                                   'transaction_type': TransactionType.REFUND,
                                                   'internal': 1}])

    steps.SimpleApi.create_fake_tpt_data(context, client_id, 666,
                                         contract_id, first_month_day + relativedelta(days=1),
                                         [{'client_amount': payment_sum,
                                           'client_id': client_id,
                                           'amount': 666,
                                           'transaction_type': TransactionType.PAYMENT},
                                          {'client_amount': refund_sum,
                                           'client_id': client_id,
                                           'amount': 666,
                                           'transaction_type': TransactionType.REFUND}])

    if process:
        steps.TaxiSteps.process_taxi(contract_id, first_month_day + relativedelta(days=2, minutes=5))
        steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, last_month_day)
        steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)



# OFF
# акты за один месяц
@reporter.feature(Features.PAYMENT, Features.TAXI)
@pytest.mark.parametrize('month_payment_sum, refund_sum, context',
                         [
                             (PAYMENT_SUM, REFUND_SUM, CORP_TAXI_RU_CONTEXT_GENERAL),
                         ], ids=['Payments<Invoice_sum RU',
                                 ])
def test_act_generation_first_month_prepay(month_payment_sum, refund_sum, context):
    params = {'start_dt': CONTRACT_START_DT, 'advance_payment_sum': MIN_COST}
    client_id, contract_id, person_id = create_client_persons_contracts_prepay(context, params, is_postpay=0)

    # создадим платежи и сгенерируем акт за 1 месяц
    process_payment(PREVIUS_MONTH_START_DT, client_id, contract_id, 50, 25, 0,
                    context=context)

    taxi_balance_data_1 = steps.PartnerSteps.get_partner_balance(Services.TAXI_CORP, [contract_id])[0]

    # process_payment(PREVIUS_MONTH_START_DT, client_id, contract_id, 50, 50, 0,
    #                 context=context, process=False)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]
    taxi_balance_data_2 = steps.PartnerSteps.get_partner_balance(Services.TAXI_CORP, [contract_id])[0]

    # проверяем данные в акте
    #act_data = steps.ActsSteps.get_act_data_by_client(client_id)[0]
    print 'taxi_balance_data_1: \n', taxi_balance_data_1
    print 'taxi_balance_data_2: \n', taxi_balance_data_2



@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-22114', 'BALANCE-27811')
@pytest.mark.parametrize('payment_sum, refund_sum', [
    (D('0'), D('0')),
    # (D('0'), D('1')),
    # (D('10'), D('3')),
    # (D('10'), D('105')),
    # (D('150'), D('5'))
], ids=[
    'Corp client acts Yandex.Taxi',
])
def test_act_corp_taxi_postpay_one_month(payment_sum, refund_sum):
    context = CORP_TAXI_RU_CONTEXT_GENERAL
    params = {'start_dt': CONTRACT_START_DT, 'personal_account': 1, 'service_min_cost': MIN_COST}
    client_id, contract_id, person_id = create_client_persons_contracts_prepay(context, params, is_postpay=1)

    # создадим платежи и сгенерируем акт за 1 месяц
    # process_payment(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, 0, payment_sum, refund_sum,
    #                context=context)

    process_payment(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, 0, payment_sum, refund_sum,
                    context=context)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)[0]

    if MIN_COST >= payment_sum - refund_sum:
        final_act_sum = MIN_COST
    else:
        final_act_sum = payment_sum - refund_sum


    # # создадим платежи и сгенерируем акт за 1 месяц
    # process_payment(PREVIUS_MONTH_START_DT, client_id, contract_id, 0, 25, 0,
    #                 context=context)
    #
    # invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]
    #
    # # проверяем данные в акте
    # act_data = steps.ActsSteps.get_act_data_by_client(client_id)[0]


@reporter.feature(Features.TAXI, Features.CORP_TAXI, Features.ACT)
@pytest.mark.tickets('BALANCE-22114', 'BALANCE-27811')
@pytest.mark.parametrize('min_cost_1, min_cost_2, payment_sum_1_1, refund_sum_1_1, '
                         'payment_sum_1_2, refund_sum_1_2, payment_sum_2, refund_sum_2', [
    (D('200'), D('100'), D('10'), D('0'), D('10'), D('0'), D('10'), D('0')),
    (D('5'), D('100'), D('10'), D('0'), D('10'), D('0'), D('10'), D('0')),
], ids=[
    'min_cost_1 > min_cost_2',
    'min_cost_1 < min_cost_2',
])
def test_act_corp_taxi_postpay_two_months(min_cost_1, min_cost_2, payment_sum_1_1, refund_sum_1_1,
                                          payment_sum_1_2, refund_sum_1_2, payment_sum_2, refund_sum_2):
    context = CORP_TAXI_RU_CONTEXT_GENERAL
    params = {'start_dt': CONTRACT_START_DT, 'personal_account': 1, 'service_min_cost': min_cost_1}
    client_id, contract_id, person_id = create_client_persons_contracts_prepay(context, params, is_postpay=1)

    process_payment(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, 0, payment_sum_1_1, refund_sum_1_1,
                    context=context)

    steps.ContractSteps.create_collateral(1038, {'CONTRACT2_ID': contract_id,
                                                'DT': utils.Date.to_iso(PREVIUS_MONTH_START_DT),
                                                'IS_SIGNED': utils.Date.to_iso(PREVIUS_MONTH_START_DT),
                                                'service_min_cost': min_cost_2})

    process_payment(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, 0, 10, 0,
                    context=context)
    process_payment(PREVIUS_MONTH_START_DT, client_id, contract_id, 0, 10, 0,
                    context=context)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    #
    # if min_cost_1 >= payment_sum_1 - refund_sum_1:
    #     act_sum_1 = min_cost_1
    # else:
    #     act_sum_1 = payment_sum_1 - refund_sum_1

def test_check_old_contract_acts():
    context = CORP_TAXI_RU_CONTEXT_GENERAL
    params = {'start_dt': CONTRACT_START_DT, 'personal_account': 1}
    client_id, contract_id, person_id = create_client_persons_contracts_prepay(context, params, is_postpay=1)

    process_payment(MONTH_BEFORE_PREV_START_DT, client_id, contract_id, 0, 10, 0,
                    context=context)

    invoice_data_prepay = steps.InvoiceSteps.get_invoice_data_by_client(client_id)
    act_data_prepay = steps.ActsSteps.get_act_data_by_client(client_id)

def test_do_payments_collateral():
    process_payment(MONTH_BEFORE_PREV_START_DT, 105297176, 989420, 0, 10, 0,
                    context=CORP_TAXI_RU_CONTEXT_GENERAL)

    process_payment(PREVIUS_MONTH_START_DT, 105297176, 989420, 0, 10, 0,
                    context=CORP_TAXI_RU_CONTEXT_GENERAL)

    invoice_data_prepay = steps.InvoiceSteps.get_invoice_data_by_client(105297176)
    act_data_prepay = steps.ActsSteps.get_act_data_by_client(105297176)