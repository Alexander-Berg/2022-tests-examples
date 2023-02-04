# coding: utf-8

import decimal
import json
import os

import pytest
from hamcrest import contains_string, is_in, empty, has_length
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance import balance_db
from balance.balance_db import balance
from btestlib import utils as butils
from check import db
from check import defaults
from check import steps as check_steps
from check import utils
from check.defaults import DATA_DIR
from check.steps import END_OF_MONTH
from check.shared import CheckSharedBefore
from check import shared_steps


"""
Выполняет сверку абсолютных значений заказов. Период сравнения до текущего момента. (a)
"""

DIFFS_COUNT = 4

def prepare_order(orders_map, client_id, person_id):
    invoice_map = check_steps.create_invoice_map(orders_map, client_id, person_id)
    steps.InvoiceSteps.pay(invoice_map['id'], payment_dt=END_OF_MONTH)
    db.update_date(invoice_map['orders'][1]['id'], END_OF_MONTH)

    #Обновляем дату конзьюма, чтобы была не раньше даты счёта
    query = 'update bo.t_consume set dt = :date where invoice_id = :invoice_id'
    query_params = {'date': END_OF_MONTH, 'invoice_id': invoice_map['id']}
    balance_db.balance().execute(query, query_params)

    return invoice_map


@pytest.fixture(scope="function")
def fixtures_create_invoice_():
    check_defaults = {'type': 'zbg',
                      'service_id': defaults.Services.geo,
                      'product_id': defaults.Products.geo,
                      'paysys_id': 1003,
                      'person_category': 'ur',
                      'person_additional_params': None}
    client_id = check_steps.create_client()
    person_id = check_steps.create_person(
        client_id, person_category=check_defaults['person_category'],
        additional_params=check_defaults['person_additional_params']
    )

    create_invoice = prepare_order({
        1: {
        'paysys_id':   check_defaults.get('paysys_id'),
        'service_id':  check_defaults.get('service_id'),
        'product_id':  check_defaults.get('product_id'),
        'consume_qty': decimal.Decimal('55')
        }
    },
        client_id, person_id)

    return create_invoice['orders'][1], check_defaults


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBG)
def test_no_diffs(shared_data, fixtures_create_invoice_):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        -количество открученного сходится
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['order_info', 'geo_data']
    ) as before:
        before.validate()

        create_invoice, _ = fixtures_create_invoice_

        geo_data = {
            'service_order_id': create_invoice['service_order_id'],
            'order_id':         create_invoice['id'],
            'service_id':       create_invoice['service_id'],
            'amount_a':         create_invoice['consume_qty'],
        }

        order_info = {'service_order_id': geo_data['service_order_id']}

    cmp_data = shared_steps.SharedBlocks.run_zbg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == order_info['service_order_id']]

    reporter.log(result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBG)
def test_not_found_in_billing(shared_data, fixtures_create_invoice_):
    """
    Начальные условия:
        -заказ присутствует в Справочнике
        -заказ отсутствует в Биллинге
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в биллинге"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['order_info', 'geo_data']
                           ) as before:
        before.validate()

        create_invoice, check_defaults = fixtures_create_invoice_
        service_order_id = steps.OrderSteps.next_id(check_defaults['service_id'])

        geo_data = {
            'service_order_id': service_order_id,
            'order_id':         service_order_id,
            'service_id':       create_invoice['service_id'],
            'amount_a':         create_invoice['consume_qty'],
        }

        order_info = {'service_order_id': geo_data['service_order_id']}

    cmp_data = shared_steps.SharedBlocks.run_zbg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == order_info['service_order_id']]

    reporter.log(result)

    butils.check_that((order_info['service_order_id'], 2), is_in(result))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBG)
def test_not_found_in_geo(shared_data, fixtures_create_invoice_):
    """
    Начальные условия:
        -заказ присутствует в Биллинге
        -заказ отсутствует в Справочнике
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в Справочнике"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['order_info', 'geo_data']
                           ) as before:
        before.validate()

        create_invoice, _ = fixtures_create_invoice_

        geo_data = None

        order_info = {'service_order_id': create_invoice['service_order_id']}

    cmp_data = shared_steps.SharedBlocks.run_zbg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == order_info['service_order_id']]

    reporter.log(result)

    butils.check_that((order_info['service_order_id'], 1), is_in(result))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBG)
def test_amount_not_converge(shared_data, fixtures_create_invoice_):
    """
    Начальные условия:
        -заказ присутствует в Справочнике, сумма зачисленного на дату сверки = 100 ед.
        -заказ присутствует в Биллинге, сумма зачисленного на дату сверки = 150 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходятся суммы зачисленного"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['order_info', 'geo_data']
                           ) as before:
        before.validate()

        create_invoice, _ = fixtures_create_invoice_

        geo_data = {
            'service_order_id': create_invoice['service_order_id'],
            'order_id':         create_invoice['id'],
            'service_id':       create_invoice['service_id'],
            'amount_a':         create_invoice['consume_qty'] + decimal.Decimal('9'),
        }

        order_info = {'service_order_id': geo_data['service_order_id']}

    cmp_data = shared_steps.SharedBlocks.run_zbg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == order_info['service_order_id']]

    reporter.log(result)

    butils.check_that((order_info['service_order_id'], 3), is_in(result))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBG)
def test_not_integer(shared_data):
    """
    Начальные условия(Сравниваем на конкретном заказе. Данные уже есть в Биллинге.
    В тесте задаём значения только для Справочника):
        -заказ присутствует в Справочнике, сумма зачисленного на дату сверки = 163.58973 ед.
        -заказ присутствует в Биллинге, сумма зачисленного на дату сверки = 163.58973 ед.
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['order_info', 'geo_data']
                           ) as before:
        before.validate()

        geo_data = {
            'service_order_id': 123902,
            'order_id':         18923249,
            'service_id':       37,
            'amount_a':         '163.58973',
        }

        order_info = {'service_order_id': geo_data['service_order_id']}

    cmp_data = shared_steps.SharedBlocks.run_zbg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == order_info['service_order_id']]

    reporter.log(result)

    butils.check_that(result, empty())

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBG)
def test_not_integer_with_diff(shared_data):
    """
    Начальные условия((Сравниваем на конкретном заказе. Данные уже есть в Биллинге.
    В тесте задаём значения только для Справочника):
        -заказ присутствует в Справочнике, сумма зачисленного на дату сверки = 87 ед.
        -заказ присутствует в Биллинге, сумма зачисленного на дату сверки = 87,138914 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходятся суммы зачисленного"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['order_info', 'geo_data']
                           ) as before:
        before.validate()

        geo_data = {
            'service_order_id': 115987,
            'order_id':         7042029,
            'service_id':       37,
            'amount_a':         '87',
        }

        order_info = {'service_order_id': geo_data['service_order_id']}

    cmp_data = shared_steps.SharedBlocks.run_zbg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == order_info['service_order_id']]

    reporter.log(result)

    butils.check_that((order_info['service_order_id'], 3), is_in(result))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBG)
def test_check_diffs_count(shared_data):
    """
        В тесте проверяем, что общее количество росхождений в тестах совпадает с ожидаемым
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_zbg(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    butils.check_that(cmp_data, has_length(DIFFS_COUNT))
