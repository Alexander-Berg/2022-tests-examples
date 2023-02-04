# coding: utf-8
__author__ = 'chihiro'

import datetime
from decimal import Decimal

import pytest
from hamcrest import contains_string, is_in, empty, has_length, equal_to
from dateutil.relativedelta import relativedelta

from balance import balance_api as api
from balance import balance_steps as steps
from balance.balance_db import balance
from btestlib import utils as b_utils
import btestlib.reporter as reporter
from btestlib.data.defaults import Taxi
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT, TAXI_BV_LAT_EUR_CONTEXT

from check import steps as check_steps
from check import shared_steps, utils
from check.shared import CheckSharedBefore
from check.steps import TRANSACTION_LOG_DT

context_ru = TAXI_RU_CONTEXT
context_bv_eur = TAXI_BV_LAT_EUR_CONTEXT

CONTRACT_START_DT = b_utils.Date.first_day_of_month(datetime.datetime.now() - relativedelta(months=1))
CONTRACTS_DATA_RU = []
CONTRACTS_DATA_EUR = []


def setup_module():
    # Сдвигаем дату перехода на транзакционный лог
    check_steps.update_partner_transaction_log_dt()

    # Заполняем списки с данными для тестов (client_id, person_id, contract_id)
    for _ in range(5):
        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context_bv_eur,
                                                                                           additional_params = {
                                                                                               'start_dt': CONTRACT_START_DT})
        CONTRACTS_DATA_EUR.append((client_id, person_id, contract_id))

    for _ in range(12):
        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context_ru,
                                                                                           additional_params = {
                                                                                               'start_dt': CONTRACT_START_DT})
        CONTRACTS_DATA_RU.append((client_id, person_id, contract_id))

    balance().execute("begin dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); end;")


@pytest.mark.parametrize('data_generator, completion_dt',
                         [(check_steps.create_partner_completions_postpay, check_steps.COMPLETION_DT),
                          (check_steps.create_partner_completions_postpay_tlog, TRANSACTION_LOG_DT)])
@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_no_diffs_postpay(data_generator, completion_dt, shared_data):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_EUR.pop()
        data_generator(context_bv_eur, client_id, contract_id, completion_dt=completion_dt)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['payment_type'], row['state']) for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))

    b_utils.check_that(result, empty(), "Расхождений нет")


@pytest.mark.parametrize('data_generator, completion_dt',
                         [(check_steps.create_partner_completions_prepay, check_steps.COMPLETION_DT),
                          (check_steps.create_partner_completions_prepay_tlog, TRANSACTION_LOG_DT)])
@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_no_diffs_prepay(data_generator, completion_dt, shared_data):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        data_generator(context_ru, client_id, contract_id, completion_dt=completion_dt)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))


    result = [(row['payment_type'], row['state']) for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))

    b_utils.check_that(result, empty(), "Расхождений нет")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_not_found_in_pct_postpay(shared_data):
    # в тесте проверяем, что в расхождения попал и cash, и card

    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_EUR.pop()
        check_steps.create_partner_completions_postpay(context_bv_eur, client_id, contract_id)

        check_steps.delete_order_from_pct(client_id)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))


    result = [(row['payment_type'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, has_length(2), "Всего две записи в cmp_data('card', 'cash') для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = [('card', 1), ('cash', 1)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "Постоплатный заказ с типом оплаты cash и card присутствует в таблице t_order"
                       " и отсутствует в v_partner_taxi_completion(T_PARTNER_TAXI_STAT_AGGR): {}".format(expected_result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_completion_qty_is_sum_of_cash_and_prepaid(shared_data):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        check_steps.create_partner_completions_prepay(context_ru, client_id, contract_id)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    print('contract_id - ' + str(contract_id))

    result = [(row['payment_type'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, empty(), "Расхождений нет(проверяем только сумму)")


    orders = balance().execute('select * from t_order where client_id = :client_id',
                               {'client_id': client_id})
    reporter.log("RESULT: {}".format(orders))
    total_sum_cash_prepaid = orders[0]['completion_qty']

    procent = Decimal('0.2')
    sum_of_commission = Taxi.order_commission_prepaid + Taxi.order_commission_cash
    expected_count = sum_of_commission + (sum_of_commission) * procent

    b_utils.check_that(total_sum_cash_prepaid, equal_to(str(expected_count)), "Общая сумма комиссии = сумма комиссии cash + сумма комиссии prepaid(т.к. prepaid относится к типу cash)")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_not_found_in_pct_prepay(shared_data):
    # в тесте проверяем, что в расхождения попал и cash, и card

    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        check_steps.create_partner_completions_prepay(context_ru, client_id, contract_id)

        check_steps.delete_order_from_pct(client_id)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    print('contract_id - ' + str(contract_id))

    result = [(row['payment_type'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, has_length(2),
                       "Всего две записи в cmp_data('card' и 'cash') для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = [('card', 1), ('cash', 1)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "предоплатный заказ с типом оплаты cash и card присутствует в таблице t_order "
                       "и отсутствует в v_partner_taxi_completion(T_PARTNER_TAXI_STAT_AGGR): {}".format(expected_result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_not_found_in_pct_prepay_for_prepaid(shared_data):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        check_steps.create_partner_completions_prepay(context_ru, client_id, contract_id, prepaid=True)

        check_steps.delete_order_from_pct(client_id)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    print('contract_id - ' + str(contract_id))


    result = [(row['payment_type'], row['state']) for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))

    b_utils.check_that(result, has_length(1),
                       "Всего одна запись в cmp_data('cash') для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = [('cash', 1)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "Заказ присутствует в таблице t_order и отсутствует в v_partner_taxi_completion(T_PARTNER_TAXI_STAT_AGGR): {}".format(expected_result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_not_found_in_tord_postpay(shared_data):
    # в одном и том же тесте проверяем, что в расхождения попал и cash, и card

    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_EUR.pop()
        check_steps.create_partner_completions_postpay(context_bv_eur, client_id, contract_id, with_act=False)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    print('contract_id - ' + str(contract_id))

    result = [(row['payment_type'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, has_length(2),
                       "Всего две записи в cmp_data('card', 'cash') для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = [('card', 2), ('cash', 2)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "В расхождении два типа платежей('card' и 'cash') в статусе '2' - 'Отсутствует в t_order': {}".format(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_not_found_in_tord_prepay(shared_data):
    # в одном и том же тесте проверяем, что в расхождения попал и cash, и card

    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        check_steps.create_partner_completions_prepay(context_ru, client_id, contract_id, with_act=False)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))


    print('contract_id - ' + str(contract_id))

    result = [(row['payment_type'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, has_length(2),
                       "Всего две записи в cmp_data('card', 'cash') для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = [('card', 2), ('cash', 2)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "В расхождении два типа платежей('card' и 'cash') в статусе '2' - 'Отсутствует в t_order': {}".format(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_completion_qty_not_converge_postpay(shared_data):
    # в одном и том же тесте проверяем, что в расхождения попал и cash, и card

    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_EUR.pop()
        check_steps.create_partner_completions_postpay(context_bv_eur, client_id, contract_id)

        check_steps.update_commission_sum(client_id)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))


    print('contract_id - ' + str(contract_id))

    result = [(row['payment_type'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, has_length(2),
                       "Всего две записи в cmp_data('card', 'cash') для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = [('card', 3), ('cash', 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "В расхождении два типа платежей('card' и 'cash') в статусе '3' - 'Расходится сумма комиссии': {}".format(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_completion_qty_not_converge_prepay(shared_data):
    # в одном и том же тесте проверяем, что в расхождения попал и cash, и card

    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        check_steps.create_partner_completions_prepay(context_ru, client_id, contract_id)

        check_steps.update_commission_sum(client_id)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    print('contract_id - ' + str(contract_id))

    result = [(row['payment_type'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, has_length(2),
                       "Всего две записи в cmp_data('card', 'cash') для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = [('card', 3), ('cash', 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "В расхождении два типа платежей('card' и 'cash') в статусе '3' - 'Расходится сумма комиссии': {}".format(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_completion_qty_not_converge_prepay_for_prepaid(shared_data):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        check_steps.create_partner_completions_prepay(context_ru, client_id, contract_id, prepaid=True)

        check_steps.update_commission_sum(client_id)

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    print('contract_id - ' + str(contract_id))

    result = [(row['payment_type'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, has_length(1),
                       "Всего одна запись в cmp_data('cash') для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = [('cash', 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "В расхождении платежей('cash') в статусе '3' - расхождения по сумме: {}".format(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_completion_qty_not_converge_auto_analysis_postpay(shared_data):
    # в одном и том же тесте проверяем, что в расхождения попал и cash, и card

    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        # - постоплатный заказ с типом оплаты corporate присутствует в обеих таблицах, валюта - EUR
        # - сумма комиссии в v_partner_taxi_completion = 57 на дату - первое число предыдущего месяца
        # - сумма комиссии в t_order = 0 на дату - первое число предыдущего месяца и 57 на дату - первое
        # число текущего месяца

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        check_steps.create_partner_completions_postpay(context_ru, client_id, contract_id)
                                                       # completion_dt=b_utils.Date.get_last_day_of_previous_month())

        check_steps.update_shipment_date(client_id, shipment_dt=b_utils.Date.first_day_of_month())

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))
    cmp_id = cmp_data[0]['cmp_id']

    print('contract_id - ' + str(contract_id))

    result = [(row['payment_type'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, has_length(2),
                       "Всего две записи в cmp_data('card', 'cash') для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = [('card', 2), ('cash', 2)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "В расхождении два типа платежей('card' и 'cash') в статусе '2' - 'Отсутствует в t_order': {}".format(result))

    product_ids = [(row['product_id']) for row in cmp_data
                   if row['contract_id'] == contract_id]

    ticket = utils.get_check_ticket('tctc', cmp_id)
    for comment in ticket.comments.get_all():
        if str(contract_id) in comment.text:
            b_utils.check_that(comment.text, contains_string(
                u'Открутки попали в соседний период, общая сумма комиссии по договору сходится'),
                              u'Проверяем, что в комментарии содержится требуемый текст, '
                              u'и в комментарии указан ID заказа')
            b_utils.check_that(comment.text, contains_string(str(product_ids[0])))
            b_utils.check_that(comment.text, contains_string(str(product_ids[1])))
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_completion_qty_not_converge_auto_analysis_prepay(shared_data):
    # в одном и том же тесте проверяем, что в расхождения попал и cash, и card

    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        # - постоплатный заказ с типом оплаты corporate присутствует в обеих таблицах, валюта - EUR
        # - сумма комиссии в v_partner_taxi_completion = 57 на дату - первое число предыдущего месяца
        # - сумма комиссии в t_order = 0 на дату - первое число предыдущего месяца и 57 на дату - первое
        # число текущего месяца

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        check_steps.create_partner_completions_prepay(context_ru, client_id, contract_id)

        check_steps.update_shipment_date(client_id, shipment_dt=b_utils.Date.first_day_of_month())

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))
    cmp_id = cmp_data[0]['cmp_id']

    print('contract_id - ' + str(contract_id))

    result = [(row['payment_type'], row['state'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, has_length(2),
                       "Всего две записи в cmp_data('card', 'cash') для конкретного 'contract_id' = {} ".format(
                           contract_id))

    expected_result = [('card', 2), ('cash', 2)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       "В расхождении два типа платежей('card' и 'cash') в статусе '2' - 'Отсутствует в t_order': {}".format(result))


    product_ids = [(row['product_id'])
                   for row in cmp_data if row['contract_id'] == contract_id]


    ticket = utils.get_check_ticket('tctc', cmp_id)

    for comment in ticket.comments.get_all():
        if str(contract_id) in comment.text:
            b_utils.check_that(comment.text, contains_string(
                u'Открутки попали в соседний период, общая сумма комиссии по договору сходится'),
                               u'Проверяем, что в комментарии содержится требуемый текст, '
                               u'и в комментарии указан ID заказа')
            b_utils.check_that(comment.text, contains_string(str(product_ids[0])))
            b_utils.check_that(comment.text, contains_string(str(product_ids[1])))

            break
    else:
        assert False, u'Комментарий авторазбора не найден'


def get_cash_ru_id():
    return balance().execute('select PRODUCT_ID from t_partner_product '
                             'where ORDER_TYPE = :text '
                             'and SERVICE_ID = 111 and currency_iso_code = :currency',
                             {'text': 'hiring_with_car', 'currency': 'RUB'})


def get_act_id(client_id):
    return balance().execute('select id from t_act '
                             'where client_id = :client_id ',
                             {'client_id': client_id})


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_allocate_subsidy_to_different_orders(shared_data):

    subsidy_sum = 101.8  # Указываем субсидии и промокды и проверяем, что они учитываются
    promocode_sum = 1

    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        # Создаём три заказа СASH с разным уровнем PROMO_SUBT_ORDER
        # Проверим, что списании субсидии идёт в нужном порядке - от 0 и дальше по порядку
        # Посмотреть порядок можно тут:
        # select * from t_partner_product
        # where  service_id in (111, 128) and currency_iso_code = 'RUB';

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        check_steps.create_partner_completions_prepay(context_ru, client_id, contract_id, subsidy_sum=subsidy_sum,
                                                                                          promocode_sum=promocode_sum)

    cash_ru_id = get_cash_ru_id()

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))
    cmp_id = cmp_data[0]['cmp_id']

    result = [(row['product_id'], row['state']) for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))

    b_utils.check_that(result, has_length(3),
                       "Есть три заказа  для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = (cash_ru_id[0]['product_id'], 3)
    b_utils.check_that(expected_result, is_in(result),
                       "Cоздался заказ с нужным product_id и в статусе '3' - расхождения по сумме': {}".format(expected_result))

    # Проверяем, что создался тикет о расхождении и там учтены субсидии
    product_ids = [(row['product_id'])
                   for row in cmp_data if row['contract_id'] == contract_id]

    ticket = utils.get_check_ticket('tctc', cmp_id)

    for comment in ticket.comments.get_all():
        if str(contract_id) in comment.text:
            b_utils.check_that(comment.text, contains_string(
                u'По данным договорам была применена логика вычета промокодов и субсидий. Суммы откруток сходятся.'),
                               u'Проверяем, что в комментарии содержится требуемый текст, '
                               u'В комментарии указан ID заказа')

            for product_id in product_ids:
                b_utils.check_that(comment.text, contains_string(str(product_id)))
            break
    else:
        assert False, u'Комментарий авторазбора не найден'

    # Проверяем, что при подсчёте учитывается субсидии и промокоды
    total_sum = balance().execute(
        'select completion_qty from t_order where service_code = 509001 and client_id = :client_id',
        {'client_id': client_id})
    completion_qty = total_sum[0]['completion_qty']

    procent = Decimal('0.2')
    sum_of_commission = Taxi.childchair_cash + Taxi.order_commission_cash + Taxi.hiring_with_car_cash
    expected_count = (sum_of_commission + (sum_of_commission) * procent) - (
            Decimal(str(subsidy_sum)) + Decimal(str(promocode_sum)) * 3)
    assert str(expected_count) == completion_qty
    b_utils.check_that(str(expected_count), equal_to(completion_qty), "Проверили, что при подсчёте учитывается субсидии и промокоды: {} = {}".format(str(expected_count), completion_qty))
    print('Sums are equal: ' + str(expected_count) + ' == ' + str(completion_qty))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_subsidy_without_act(shared_data):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'person_id', 'contract_id']
    ) as before:
        before.validate()

        # Создаём три заказа СASH с разным уровнем PROMO_SUBT_ORDER
        # Задаём сумму субсидии больше чем сумма заказлв
        # В таком случае акт не должен сформироваться
        # Специально передаю параметр 'witout_act=True'

        subsidy_sum = 121.8  # Размер субсидии перекрывает сумму заказов

        client_id, person_id, contract_id = CONTRACTS_DATA_RU.pop()
        check_steps.create_partner_completions_prepay(context_ru, client_id, contract_id, with_act=False,
                                                                                          subsidy_sum=subsidy_sum)

    act_id = get_act_id(client_id)

    # Проверяем, что нет акта
    assert len(act_id) == 0
    b_utils.check_that(act_id, empty(), "Проверили, что нет акта")

    cash_ru_id = get_cash_ru_id()

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    result = [(row['product_id'], row['state']) for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT: {}".format(result))


    b_utils.check_that(result, has_length(3),
                       "есть три заказа  для конкретного 'contract_id' = {} ".format(contract_id))

    expected_result = (cash_ru_id[0]['product_id'], 2)
    b_utils.check_that(expected_result, is_in(result),
                       "Cоздался заказ с нужным product_id в статусе 2 - Отсутствует в t_order': {}".format(expected_result))

DIFFS_COUNT = 24

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TCTC)
def test_tctc_check_diffs_count(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_tctc(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA: {}".format(cmp_data))

    b_utils.check_that(cmp_data, has_length(DIFFS_COUNT))
