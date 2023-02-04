# coding=utf-8

import datetime
import decimal
import json
import os

import pytest
from hamcrest import contains_string, is_in, empty, has_length

import check.db
from balance import balance_api as api
from balance import balance_steps as steps
from balance.balance_db import balance
from btestlib import utils as butils
from btestlib import shared, constants, utils, reporter
from check import steps as check_steps
from check import utils, shared_steps
from check.db import get_consume_qty, get_date_by_order
from check.defaults import Services, data as defaults_data, DATA_DIR
from check.shared import CheckSharedBefore
from check.steps import create_data_in_market

"""
Осуществляет сравнение зачисленных средств на заказ в Биллинге и Маркете
Типы расхождений:
    1 - Отсутствует в Маркете
    2 - Отсутствует в Биллинге
    3 - Расходятся суммы
"""

check_code = 'zmb'
QTY_BUCKS = decimal.Decimal("22")


def create_client_and_contract():
    check_defaults = defaults_data[check_code]
    client_id = check_steps.create_client()
    person_id = check_steps.create_person(client_id,
                                          person_category=check_defaults['person_category'],
                                          additional_params=check_defaults['person_additional_params'])

    return client_id, person_id, check_defaults


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZMB)
def test_without_diff(shared_data):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        -количество зачисленного сходится
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['market_data']) as before:
        before.validate()

        client_id, person_id, check_defaults = create_client_and_contract()

        act_id = check_steps.create_act(client_id, person_id, check_defaults['paysys_id'],
                                        check_defaults['service_id'], check_defaults['product_id'], QTY_BUCKS=QTY_BUCKS)
        service_order_id = check.db.get_service_order_id_by_act_id(act_id)
        order_id = check.db.get_order_id(service_order_id, Services.market)
        new_date = datetime.datetime.now() - datetime.timedelta(days=2)
        check.db.update_date(order_id, new_date)
        market_data = {
            "service_type":     Services.market,
            "service_order_id": service_order_id,
            "consume_qty":      decimal.Decimal(get_consume_qty(order_id)) * 100,
            "date_by_order":    get_date_by_order(order_id)
        }

    cmp_data = shared_steps.SharedBlocks.run_zmb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log(cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == market_data['service_order_id']]

    butils.check_that(result, empty())

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZMB)
def test_order_with_changed_sum(shared_data):
    """
    Начальные условия:
        -заказ присутствует в маркете, сумма зачисленного на дату сверки = 100 ед.
        -заказ присутствует в биллинге, сумма зачисленного на дату сверки = 150 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество зачисленного"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['market_data']) as before:
        before.validate()

        client_id, person_id, check_defaults = create_client_and_contract()

        act_id = check_steps.create_act(client_id, person_id, check_defaults['paysys_id'],
                                        check_defaults['service_id'], check_defaults['product_id'], QTY_BUCKS=QTY_BUCKS)
        service_order_id = check.db.get_service_order_id_by_act_id(act_id)
        order_id = check.db.get_order_id(service_order_id, Services.market)
        new_date = datetime.datetime.now() - datetime.timedelta(days=2)
        check.db.update_date(order_id, new_date)
        market_data = {
            "service_type":     Services.market,
            "service_order_id": service_order_id,
            "consume_qty":      decimal.Decimal(get_consume_qty(order_id)) + 1,
            "date_by_order":    get_date_by_order(order_id)
        }

    cmp_data = shared_steps.SharedBlocks.run_zmb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("Result = %s" % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == market_data['service_order_id']]
    reporter.log("Result = %s" % result)

    butils.check_that((market_data['service_order_id'], 3), is_in(result))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZMB)
def test_order_not_found_in_billing(shared_data):
    """
    Начальные условия:
        -заказ присутствует в Маркете
        -заказ отсутствует в Биллинге
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в биллинге"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['market_data']) as before:
        before.validate()

        service_order_id=steps.OrderSteps.next_id(Services.market)
        dt = datetime.datetime.now().replace(microsecond=0) - datetime.timedelta(days=2)

        market_data = {
            "service_type":     Services.market,
            "service_order_id": service_order_id,
            "consume_qty":      decimal.Decimal(55) * 100,
            "date_by_order":    dt
        }

    cmp_data = shared_steps.SharedBlocks.run_zmb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("Result = %s" % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == market_data['service_order_id']]
    reporter.log("Result = %s" % result)

    butils.check_that((market_data['service_order_id'], 2), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZMB)
def test_order_not_found_in_market(shared_data):
    """
    Начальные условия:
        -заказ присутствует в Биллинге
        -заказ отсутствует в Маркете
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в маркете"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['service_order_id']) as before:
        before.validate()

        client_id, person_id, check_defaults = create_client_and_contract()

        act_id = check_steps.create_act(client_id, person_id, check_defaults['paysys_id'],
                                        check_defaults['service_id'], check_defaults['product_id'], QTY_BUCKS=QTY_BUCKS)

        service_order_id = check.db.get_service_order_id_by_act_id(act_id)
        order_id = check.db.get_order_id(service_order_id, Services.market)
        new_date = datetime.datetime.now() - datetime.timedelta(days=2)
        check.db.update_date(order_id, new_date)

        market_data = None

    cmp_data = shared_steps.SharedBlocks.run_zmb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("Result = %s" % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("Result = %s" % result)

    butils.check_that((service_order_id, 1), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZMB)
def test_check_diffs_count(shared_data):
    diffs_count = 3
    """
        В тесте проверяем, что общее количество росхождений в тестах совпадает с ожидаемым
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_zmb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    butils.check_that(cmp_data, has_length(diffs_count))
