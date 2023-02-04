# coding=utf-8

import random
import decimal
from datetime import datetime, timedelta

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import equal_to, contains_string

from balance import balance_api as api
from balance import balance_steps as steps
from balance.balance_db import balance
from btestlib import utils as b_utils
from check import db
from check import steps as check_steps
from check import shared_steps
from check.shared import CheckSharedBefore
from check import utils
from check.db import update_date
from check.defaults import Services, Products, LAST_DAY_OF_PREVIOUS_MONTH


# FAKE_SHIPMENT_TEXT = u"""По заказам в сверяемом периоде приходили только нулевые открутки. Расхождений нет"""
FAKE_SHIPMENT_TEXT = u"""Заказы сходятся на текущий момент"""
QTY_CAMPAIGN = decimal.Decimal("23")
QTY_BUCKS = decimal.Decimal("22")
QTY = decimal.Decimal("55.7")

DIFFS_COUNT = 13  # Попадает расхождение из теста 'test_add_dt_to_cmp_table', которое отдельно не рассматриваем, т.к. просто тестовые данные для другой проверки генерируем

check_defaults = {'service_id': Services.direct, 'product_id': Products.direct_pcs, 'paysys_id': 1003,
                  'person_category': 'ur', 'person_additional_params': None}


def _create_acted_orders(product_id=check_defaults.get('product_id'), service_id=check_defaults.get('service_id'),
                         shipment_info={'Bucks': 30}, client_id=None, person_id=None):
    if client_id is None or person_id is None:
        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category=check_defaults['person_category'],
                                              additional_params=check_defaults['person_additional_params']
                                              )
    return check_steps.create_act_map(
        orders_map={1: {'paysys_id': check_defaults.get('paysys_id'),
                        'service_id': service_id,
                        'product_id': product_id,
                        'shipment_info': shipment_info}},
        client_id=client_id, person_id=person_id
    )[0]


def do_modify(client_id, service_order_id, service_id):
    order_id = db.get_order_id(service_order_id, Services.direct)
    update_date(order_id, LAST_DAY_OF_PREVIOUS_MONTH)

    steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
    balance().execute(
        """
        update t_client_service_data
        set migrate_to_currency = to_date( :date), update_dt = to_date( :date)
        where class_id = :client_id
        """,
        {'client_id': client_id, 'date': LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=10)})
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id,
                                      {'Bucks': db.get_completion_qty(order_id), 'Money': 800}, 0,
                                      LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=15))
    return service_order_id


def format_bk_data(service_id, service_order_id, bucks=None, money=None, shows=None, clicks=None, currency=None):
    if not currency:
        if money:
            currency = 'RUB'
        else:
            currency = 'pcs'

    data = []

    def append(value):
        if value is None:
            value = '0'
        data.append(str(value))

    append(service_order_id)
    append(service_id)
    append(shows)
    append(clicks)
    append(0)
    append(money)
    append(currency)
    append(bucks and bucks * 10 ** 6)
    append(0)
    append(0)
    append(0)

    return data


def format_yt_data(service_id, service_order_id,
                   bucks=None, money=None, shows=None, clicks=None, days=None, currency=None):
    return [{
        'BillingExportID': int(service_order_id),
        'OrderID': int(random.randint(1, 10 ** 6)),
        'EngineID': int(service_id),
        'Days': days or 0,
        'Shows': shows or 0,
        'Clicks': clicks or 0,
        'Cost': 0,
        'CostCur': money or 0,
        'CostFinal': int(bucks or 0) * 10 ** 6,
        'CurrencyRatio': currency or 1,
    }]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_no_diff(shared_data):
    """
        Начальные условия:
            -заказ присутствует в обеих системах
            -количество открученного сходится
        Ожидаемый результат:
            заказ отсутствует в списке расхождений
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()

        order_info = _create_acted_orders()

        """ 
        Order of 'bk_data'= 'ExportID', 'EngineID', 'Shows', 'Clicks', 'Cost', 'CostCur', 'CurrencyCode',
                            'CostFinal', 'DShows', 'DCost', 'DCostCur'
        """

        bk_data = [
            str(order_info['service_order_id']),
            str(order_info['service_id']),
            str(0),
            str(order_info['shipment_info']['Shows']),
            str(0),
            str(order_info['shipment_info']['Money']),
            str('pcs'),
            str(int(order_info['shipment_info']['Bucks']) * 1000000),
            str(0),
            str(0),
            str(0),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что нет расхождений
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_not_diffs_but_bucks_not_converge(shared_data):
    """
        Начальные условия:
            -заказ присутствует в БК, сумма открученного в фишках на дату сверки = 100 ед.
            -заказ присутствует в биллинге, сумма открученного в фишках на дату сверки = 101 ед.
        Ожидаемый результат:
            заказ отсутствует в списке расхождений
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()

        order_info = _create_acted_orders()

        bk_data = [
            (str(order_info['service_order_id'])),
            (str(order_info['service_id'])),
            (str(0)),
            (str(order_info['shipment_info']['Shows'])),
            (str(0)),
            (str(order_info['shipment_info']['Money'])),
            (str('pcs')),
            (str((int(order_info['shipment_info']['Bucks']) + 1) * 1000000)),
            (str(0)),
            (str(0)),
            (str(0)),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что нет расхождений
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_diffs_bucks_not_converge(shared_data):
    """
            Начальные условия:
                -заказ присутствует в БК, сумма открученного в фишках на дату сверки = 100 ед.
                -заказ присутствует в биллинге, сумма открученного в фишках на дату сверки = 121 ед.
            Ожидаемый результат:
                -заказ попадает в список с расхождений,
                -состояние = "Расходится количество открученного"
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()

        order_info = _create_acted_orders()

        bk_data = [
            (str(order_info['service_order_id'])),
            (str(order_info['service_id'])),
            (str(0)),
            (str(order_info['shipment_info']['Shows'])),
            (str(0)),
            (str(order_info['shipment_info']['Money'])),
            (str('pcs')),
            (str((int(order_info['shipment_info']['Bucks']) + 21) * 1000000)),
            (str(0)),
            (str(0)),
            (str(0)),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что есть расхождения (3 - Расходится к-во открученного - баксы)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = [(order_info['service_order_id'], 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_shows_not_converge(shared_data):
    """
    Начальные условия:
        -заказ присутствует в БК, сумма открученного в показах на дату сверки = 100 ед.
        -заказ присутствует в биллинге, сумма открученного в показах на дату сверки = 150 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество открученного"
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()

        order_info = _create_acted_orders(product_id=Products.bk, service_id=Services.bk, shipment_info={'Shows': 30})

        query = """
                    select dt, update_dt
                    from bo.t_shipment
                    where service_id = :service_id
                    and service_order_id = :service_order_id
                """
        result = balance().execute(query, {'service_id': order_info['service_id'],
                                           'service_order_id': order_info['service_order_id']})[0]

        order_info.update({
            'shipment_dt': result['dt'].strftime('%d.%m.%y %H:%M:%S'),
            'shipment_update_dt': result['update_dt'].strftime('%d.%m.%y %H:%M:%S')
        })

        bk_data = [
            str(order_info['service_order_id']),
            str(order_info['service_id']),
            str(0),
            str(int(order_info['shipment_info']['Shows']) + 1),
            str(0),
            str(order_info['shipment_info']['Money']),
            str('pcs'),
            str(int(order_info['shipment_info']['Bucks']) * 1000000),
            str(0),
            str(0),
            str(0),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что есть расхождения (3 - Расходится к-во открученного - показы)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = [(order_info['service_order_id'], 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_diffs_money_not_converge(shared_data):
    """
    Начальные условия:
        -заказ присутствует в БК, сумма открученного в деньгах на дату сверки = 100 ед.
        -заказ присутствует в биллинге, сумма открученного в деньгах на дату сверки = 701 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество открученного"

    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()
        order_info = _create_acted_orders()

        bk_data = [
            str(order_info['service_order_id']),
            str(order_info['service_id']),
            str(0),
            str(order_info['shipment_info']['Shows']),
            str(0),
            str(int(order_info['shipment_info']['Money']) + 601),
            str('pcs'),
            str(int(order_info['shipment_info']['Bucks']) * 1000000),
            str(0),
            str(0),
            str(0),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что есть расхождения (3 - Расходится к-во открученного - money)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = [(order_info['service_order_id'], 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_not_found_in_bk(shared_data):
    """
    Начальные условия:
        -заказ присутствует в Биллинге
        -заказ отсутствует в БК
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в БК"
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info']) as before:
        before.validate()
        order_info = _create_acted_orders()
        bk_data = None

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что есть расхождения (1 - Отсутствует в БК)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = [(order_info['service_order_id'], 1)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_no_diffs_modify_order_but_money_not_converge(shared_data):
    """
    Начальные условия:
        -мультивалютный заказ присутствует в БК, сумма открученного в деньгах на дату сверки = 100 ед.
        -мультивалютный заказ присутствует в биллинге, сумма открученного в деньгах на дату сверки = 701 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество открученного"

    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()
        order_info = _create_acted_orders()

        do_modify(order_info['client_id'], order_info['service_order_id'], order_info['service_id'])

        bk_data = [
            str(order_info['service_order_id']),
            str(order_info['service_id']),
            str(0),
            str(order_info['shipment_info']['Shows']),
            str(0),
            str(int(order_info['shipment_info']['Money']) + 801),
            str('RUB'),
            str(int(order_info['shipment_info']['Bucks']) * 1000000),
            str(0),
            str(0),
            str(0),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что нет расхождений
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_money_not_converge_in_modify_order(shared_data):
    """
    Начальные условия:
        -мультивалютный заказ присутствует в БК, сумма открученного в деньгах на дату сверки = 100 ед.
        -мультивалютный заказ присутствует в биллинге, сумма открученного в деньгах на дату сверки = 101 ед.
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()
        order_info = _create_acted_orders()

        do_modify(order_info['client_id'], order_info['service_order_id'], order_info['service_id'])

        bk_data = [
            str(order_info['service_order_id']),
            str(order_info['service_id']),
            str(0),
            str(order_info['shipment_info']['Shows']),
            str(0),
            str(int(order_info['shipment_info']['Money']) + 601 + 800),
            str('RUB'),
            str(int(order_info['shipment_info']['Bucks']) * 1000000),
            str(0),
            str(0),
            str(0),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что есть расхождения (3 - Расходится к-во открученного - money)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = [(order_info['service_order_id'], 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_no_diffs_modify_order(shared_data):
    """
    Начальные условия:
        -мультивалютный заказ присутствует в обеих системах
        -количество открученного сходится
    Ожидаемый результат:
        заказ отсутствует в списке расхождений

    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()
        order_info = _create_acted_orders()

        do_modify(order_info['client_id'], order_info['service_order_id'], order_info['service_id'])

        bk_data = [
            str(order_info['service_order_id']),
            str(order_info['service_id']),
            str(0),
            str(order_info['shipment_info']['Shows']),
            str(0),
            str(int(order_info['shipment_info']['Money']) + 800),
            str('RUB'),
            str(int(order_info['shipment_info']['Bucks']) * 1000000),
            str(0),
            str(0),
            str(0),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что нет расхождений
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_not_found_in_billing(shared_data):
    """
    Начальные условия:
        -заказ присутствует в БК
        -заказ отсутствует в Биллинге
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в Биллинге"
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()
        order_info = _create_acted_orders()

        # Получаем только service_order_id(следующий по номеру) и больше ничего не будет
        service_order_id = steps.OrderSteps.next_id(Services.direct)
        order_info['service_order_id'] = service_order_id
        bk_data = [
            str(order_info['service_order_id']),
            str(Services.direct),
            str(0),
            str(order_info['shipment_info']['Shows']),
            str(0),
            str(int(order_info['shipment_info']['Money'])),
            str('RUB'),
            str(int(order_info['shipment_info']['Bucks']) * 1000000),
            str(0),
            str(0),
            str(0),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что есть расхождения (2 - Отсутствует в Биллинге)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = [(order_info['service_order_id'], 2)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_check_2209_child_parent(shared_data):
    """
    в новой схеме общего счета родительские заказы исключаются из сверки
    Начальные условия:
        -два заказа, включенные в общий счет. родительский отсутствует в БК, на дочернем расходятся открутки в Bucks
    Ожидаемый результат:
        родительский заказ отсутствует в списке расхождений,
        дочерний заказ попадает в список расхождений с состоянием "Расходится количество открученного"

    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()
        client_id = steps.ClientSteps.create()
        order_info = check_steps.create_acted_orders(orders_map={'child': {'shipment_info': {'Bucks': 30},
                                                                           'consume_qty': 50, 'paysys_id': 1003,
                                                                           'service_id': 7, 'product_id': 1475},
                                                                 'parent': {'consume_qty': 50, 'paysys_id': 1003,
                                                                            'service_id': 7, 'product_id': 1475}},
                                                     client_id=client_id,
                                                     person_id=steps.PersonSteps.create(client_id, 'ur'))

        # переходим к общему счету
        steps.OrderSteps.merge(order_info['parent']['id'],
                               [order_info['child']['id']])
        steps.OrderSteps.ua_enqueue([client_id])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                              dt=datetime.now() - timedelta(days=1))

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(order_info['parent']['id'])

        # создаем перекрутку на дочернем
        order_info['child']['shipment_info']['Bucks'] += 50
        check_steps.do_campaign(order_info['child'])
        bk_data = [
            str(order_info['child']['service_order_id']),
            str(order_info['child']['service_id']),
            str(0),
            str(order_info['child']['shipment_info']['Shows']),
            str(0),
            str(order_info['child']['shipment_info']['Money']),
            str('pcs'),
            str((int(order_info['child']['shipment_info']['Bucks']) + 21) * 1000000),
            str(0),
            str(0),
            str(0),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что есть расхождения для child (3 - Расходится к-во открученного - Bucks)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['child']['service_order_id']]

    expected_result = [(order_info['child']['service_order_id'], 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))

    # Проверяем, что нет расхождений для parent
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['parent']['service_order_id']]

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_check_2203_late_shipment(shared_data):
    """
    Начальные условия:
        -открутка присутствует в Биллинге, сумма открученного в показах на дату сверки = 100 ед,
        дата открутки - sysdate - 2, но открутка пришла 1го числа предыдущего месяца до момента
        планового запуска сверки за предыдущий месяц (до 9:00)
        -открутка отсутствует в БК.
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(
            client_id, person_category=check_defaults['person_category'],
            additional_params=check_defaults['person_additional_params']
        )
        order_info = check_steps.create_invoice_map({1: {'paysys_id': check_defaults.get('paysys_id'),
                                                         'service_id': check_defaults.get('service_id'),
                                                         'product_id': check_defaults.get('product_id')}
                                                     }, client_id, person_id)
        steps.InvoiceSteps.pay(order_info['id'], payment_dt=LAST_DAY_OF_PREVIOUS_MONTH)
        steps.CampaignsSteps.do_campaigns(
            check_defaults['service_id'], order_info['orders'][1]['service_order_id'],
            campaigns_params={'Bucks': QTY_BUCKS, 'Money': 0, 'Days': 0, 'Shows': 0},
            do_stop=0,
            campaigns_dt=datetime.now().replace(hour=12) - relativedelta(months=2)
        )
        query = """
                    update bo.t_shipment
                    set update_dt = :upd_dt
                    where service_id = :service_id
                    and service_order_id = :service_order_id
                """
        query_params = {'upd_dt': datetime.now().replace(hour=3, day=1) - relativedelta(months=1),
                        'service_id': check_defaults['service_id'],
                        'service_order_id': order_info['orders'][1]['service_order_id']}
        balance().execute(query, query_params)

        bk_data = None

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что нет расхождений
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['orders'][1]['service_order_id']]

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_check_2203_fake_shipment(shared_data):
    """
    Начальные условия:
        -открутка присутствует в Биллинге, сумма открученного в показах на дату сверки = 100 ед.
        -за сверяемый период не менялись деньги (cost или costcur) у заказа (в t_shipment_history имеется открытка по данному заказу с нулевыми cost или costcur)
        -открутка отсутствует в БК
    Ожидаемый результат:
        заказ присутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()

        order_info = _create_acted_orders()
        steps.CampaignsSteps.do_campaigns(
            order_info['service_id'], order_info['service_order_id'],
            campaigns_params={'Bucks': order_info['shipment_info']['Bucks'], 'Money': 0, 'Days': 0, 'Shows': 0},
            do_stop=0,
            campaigns_dt=LAST_DAY_OF_PREVIOUS_MONTH
        )

        query = """
                    insert into t_shipment_history
                    (order_id, service_id, service_order_id, dt, target_type, shows, clicks, cost, costcur, update_dt, bucks,
                    consumed_sum, consumption, days, money, need_python_processing, shipment_type, stop, units)
                    VALUES
                    (:order_id, :service_id, :service_order_id, :dt, 0, 0, 0, 0, 0, :update_dt, null, null, null, null, null,
                    null, null, null, null )
                """
        query_params = {'order_id': order_info['id'],
                        'service_id': order_info['service_id'],
                        'service_order_id': order_info['service_order_id'],
                        'dt': LAST_DAY_OF_PREVIOUS_MONTH.replace(hour=0, minute=0, second=0),
                        'update_dt': LAST_DAY_OF_PREVIOUS_MONTH.replace(hour=0, minute=0, second=0)
                        }
        balance().execute(query, query_params)

        bk_data = None

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # Проверяем, что есть расхождения (1 - Отсутствует в БК)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = [(order_info['service_order_id'], 1)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_add_dt_to_cmp_table(shared_data):
    """
        В тесте используются данные полностью совпадающие с данными в тесте 'test_shows_not_converge'
        Проверка, что в obb2_cmp_data правильно записывается shipment_dt и shipment_update_dt
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()

        order_info = _create_acted_orders(product_id=Products.bk, service_id=Services.bk,
                                          shipment_info={'Shows': 30})

        query = """
                    select dt, update_dt
                    from bo.t_shipment
                    where service_id = :service_id
                    and service_order_id = :service_order_id
                """
        result = balance().execute(query, {'service_id': order_info['service_id'],
                                           'service_order_id': order_info['service_order_id']})[0]

        order_info.update({
            'shipment_dt': result['dt'].strftime('%d.%m.%y %H:%M:%S'),
            'shipment_update_dt': result['update_dt'].strftime('%d.%m.%y %H:%M:%S')
        })

        bk_data = [
            str(order_info['service_order_id']),
            str(order_info['service_id']),
            str(0),
            str(int(order_info['shipment_info']['Shows']) + 1),
            str(0),
            str(order_info['shipment_info']['Money']),
            str('pcs'),
            str(int(order_info['shipment_info']['Bucks']) * 1000000),
            str(0),
            str(0),
            str(0),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # В тесте есть расхождения, но их проверка происходит в тесте 'test_shows_not_converge'
    # Проверяем, что есть расхождения (3 - Расходится к-во открученного - показы)

    cmp_id = cmp_data[0]['cmp_id']

    query = """
                        select balance_shipment_dt, balance_shipment_update_dt
                        from obb2_cmp_data
                        where cmp_id = :cmp_id
                        and service_id = :service_id
                        and service_order_id = :service_order_id
                        """
    query_params = {'cmp_id': cmp_id, 'service_id': Services.bk,
                    'service_order_id': order_info['service_order_id']}
    shipment_info = api.test_balance().ExecuteSQL('cmp', query, query_params)[0]

    b_utils.check_that(shipment_info['balance_shipment_dt'].strftime('%d.%m.%y %H:%M:%S'),
                      equal_to(order_info['shipment_dt']))
    b_utils.check_that(shipment_info['balance_shipment_update_dt'].strftime('%d.%m.%y %H:%M:%S'),
                      equal_to(order_info['shipment_update_dt']))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_check_2340_fake_shipment(shared_data):
    """
        Начальные условия:
            -открутка присутствует в Биллинге, сумма открученного в показах на дату сверки = 100 ед.
            -за сверяемый период не менялись деньги (cost или costcur) у заказа (в YT имеется открытка
            по данному заказу с нулевыми cost или costcur)
            -открутка отсутствует в БК
        Ожидаемый результат:
            происходит авторазбор расхождения. В связанном с заказом тикете имеем комментарий
       |-------------------------------------------------------------------------------------|
       | По заказам в сверяемом периоде приходили только нулевые открутки. Расхождений нет.  |
       |     {service_id}-{service_order_id}                                                 |
       |-------------------------------------------------------------------------------------|
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data', 'yt_data']) as before:
        before.validate()

        order_info = _create_acted_orders()
        steps.CampaignsSteps.do_campaigns(order_info['service_id'], order_info['service_order_id'],
                                          campaigns_params={
                                              'Bucks': order_info['shipment_info']['Bucks'],
                                              'Money': 0, 'Days': 0, 'Shows': 0},
                                          do_stop=0, campaigns_dt=LAST_DAY_OF_PREVIOUS_MONTH
                                          )

        yt_data = [{
            'BillingExportID': int(order_info['service_order_id']),
            'OrderID': int(order_info['id']),
            'EngineID': int(order_info['service_id']),
            'Days': 0,
            'Shows': 0,
            'Clicks': 0,
            'Cost': 0,
            'CostCur': 0,
            'CostFinal': int(order_info['shipment_info']['Bucks'] * 10 ** 6),
            'CurrencyRatio': 1,
        }]

        bk_data = None

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    # Проверяем, что есть расхождения (1 - Отсутствует в БК)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = [(order_info['service_order_id'], 1)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))

    ticket = utils.get_check_ticket('obb2', cmp_id)

    for comment in ticket.comments.get_all():
        if str(order_info['service_id']) in comment.text:
            b_utils.check_that(comment.text, contains_string(FAKE_SHIPMENT_TEXT))
            b_utils.check_that(comment.text, contains_string(
                '{}-{}'.format(order_info['service_id'], order_info['service_order_id'])),
                               u'Проверяем, что в комментарии содержится требуемый текст, '
                               u'В комментарии указан ID заказа'
                               )
            break
    else:
        assert False, u'Требуемый комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_check_2388_auto_analysis(shared_data):
    """
        Начальные условия:
            -открутка присутствует в Биллинге, сумма открученного в фишках на дату сверки = 100 ед.
            -открутка присутствует в БК, сумма открученного в фишках на дату сверки = 150 ед.
            -в БК в таблице OrderStat, сумма открученного в фишках на дату сверки(два разных заказа) = 100 ед.
        Ожидаемый результат:
            происходит авторазбор расхождения. В связанном с заказом тикете имеем комментарий:
            |--------------------------------------|
            |  Заказы сходятся на текущий момент.  |
            |   {service_id}-{service_order_id}'   |
            |--------------------------------------|
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data', 'yt_data']) as before:
        before.validate()

        order_info = _create_acted_orders()

        bk_data = [
            (str(order_info['service_order_id'])),
            (str(order_info['service_id'])),
            (str(0)),
            (str(order_info['shipment_info']['Shows'])),
            (str(0)),
            (str(order_info['shipment_info']['Money'])),
            (str('pcs')),
            (str((int(order_info['shipment_info']['Bucks']) + 601) * 1000000)),
            (str(0)),
            (str(0)),
            (str(0)),
        ]

        x = 10  # Дполнительная переменная, чтобы из двух заказов получить полную сумму (order_info['shipment_info']['Bucks'])
        base = {
            'BillingExportID': int(order_info['service_order_id']),
            'EngineID': int(order_info['service_id']),
            'Days': 0,
            'Shows': int(order_info['shipment_info']['Shows']),
            'Clicks': int(order_info['shipment_info']['Shows']),
            'Cost': 0,
            'CostCur': int(order_info['shipment_info']['Money'] * 10 ** 6),
            'CurrencyRatio': 1,
        }

        first_order = dict(base)  # Создаём копию словаря 'base'
        first_order.update({
            'OrderID': 666,
            'CostFinal': int((order_info['shipment_info']['Bucks'] - x) * 10 ** 6),
        })

        second_order = dict(base)
        second_order.update({
            'OrderID': 999,
            'CostFinal': int(x * 10 ** 6),
        })

        yt_data = [first_order, second_order]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    # Проверяем, что есть расхождения (3 - Расходится кол-во открученного)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = [(order_info['service_order_id'], 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))

    ticket = utils.get_check_ticket('obb2', cmp_id)

    for comment in ticket.comments.get_all():
        if str(order_info['service_order_id']) in comment.text:
            b_utils.check_that(comment.text, contains_string(u'Заказы сходятся на текущий момент'),
                               u'Проверяем, что в комментарии содержится требуемый текст, '
                               u'В комментарии указан ID заказа'
                               )
            break
    else:
        assert False, u'Требуемый комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_check_2388_auto_analysis_money(shared_data):
    """
        Начальные условия:
            -открутка присутствует в Биллинге, сумма открученного в деньгах на дату сверки = 100 ед.
            -открутка присутствует в БК, сумма открученного в деньгах на дату сверки = 150 ед.
            -в БК в таблице OrderStat, сумма открученного в деньгах на дату сверки = 100.009 ед.
        Ожидаемый результат:
            происходит авторазбор расхождения. В связанном с заказом тикете имеем комментарий:
            |--------------------------------------|
            |  Заказы сходятся на текущий момент.  |
            |   {service_id}-{service_order_id}'   |
            |--------------------------------------|
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data', 'yt_data']) as before:
        before.validate()

        order_info = _create_acted_orders()

        bk_data = [
            str(order_info['service_order_id']),
            str(order_info['service_id']),
            str(0),
            str(order_info['shipment_info']['Shows']),
            str(0),
            str(order_info['shipment_info']['Money'] + 80),
            str('pcs'),
            str(int(order_info['shipment_info']['Bucks']) * 10 ** 6),
            str(0),
            str(0),
            str(0),
        ]

        x = 10
        base = {
            'BillingExportID': int(order_info['service_order_id']),
            'EngineID': int(order_info['service_id']),
            'Days': 0,
            'Shows': int(order_info['shipment_info']['Shows']),
            'Clicks': int(order_info['shipment_info']['Shows']),
            'Cost': 0,
            'CostCur': int(order_info['shipment_info']['Money'] * 10 ** 6),
            'CurrencyRatio': 1,
        }

        first_order = dict(base)  # Создаём копию словаря 'base'
        first_order.update({
            'OrderID': 666,
            'CostFinal': int((order_info['shipment_info']['Bucks'] - x) * 10 ** 6),
            'CostCur': first_order['CostCur'] + 1,
        })

        second_order = dict(base)
        second_order.update({
            'OrderID': 999,
            'CostFinal': int(x * 10 ** 6),
        })

        yt_data = [first_order, second_order]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    # Проверяем, что есть расхождения (3 - Расходится кол-во открученного)
    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    expected_result = [(order_info['service_order_id'], 3)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))

    ticket = utils.get_check_ticket('obb2', cmp_id)

    for comment in ticket.comments.get_all():
        if str(order_info['service_order_id']) in comment.text:
            b_utils.check_that(comment.text, contains_string(u'Заказы сходятся на текущий момент'),
                               u'Проверяем, что в комментарии содержится требуемый текст, '
                               u'В комментарии указан ID заказа'
                               )
            break
    else:
        assert False, u'Требуемый комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_check_3276_auto_analysis(shared_data):
    """
    Начальные условия
      - Дочерний заказ в биллинге без откруток (dt = null) и он не попадает в сверку
      - В БК этот заказ имеет микро-показы, но отсутствуют фишки или деньги и он попадает в сверку
      - Получаем ситуацию - заказ есть в БК, отсуствует в Биллинге
      - После этого сломался авторазбор, т.к. в Биллинге мы получали None, а в БК - 0 и не могли посчитать разницу

    P.S. Для уменьшения количества используемых данных создаем только один, дочерний заказ без откруток
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data', 'yt_data']) as before:
        before.validate()

        order_map = check_steps.create_order_map({'order': {
            'consume_qty': 0,
            'shipment_info': {
                'Bucks': None,
                'Money': None,
                'Shows': None,
                'Days': None,
            }
        }})
        order_info = order_map['order']

        bk_data = format_bk_data(
            order_info['service_id'],
            order_info['service_order_id'],
            shows=42,
        )

        yt_data = format_yt_data(
            order_info['service_id'],
            order_info['service_order_id'],
            shows=42
        )

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    # Проверяем, что заказ не найден в Биллинге
    result = [
        (row['service_order_id'], row['state']) for row in cmp_data
        if row['service_order_id'] == order_info['service_order_id']
    ]
    expected_result = [(order_info['service_order_id'], 2)]
    b_utils.check_that(set(result), equal_to(set(expected_result)))

    # Проверяем, что заказ сходится на текущий момент
    ticket = utils.get_check_ticket('obb2', cmp_id)
    for comment in ticket.comments.get_all():
        if str(order_info['service_order_id']) in comment.text:
            b_utils.check_that(comment.text, contains_string(u'Заказы сходятся на текущий момент'),
                               u'Проверяем, что в комментарии содержится требуемый текст, '
                               u'В комментарии указан ID заказа'
                               )
            break
    else:
        assert False, u'Требуемый комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_obb2_check_diffs_count(shared_data):
    """
        В тесте проверяем, что общее количество росхождений в тестах совпадает с ожидаемым
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    b_utils.check_that(len(cmp_data), equal_to(DIFFS_COUNT))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBB2)
def test_no_diff_with_unexpected_service(shared_data):
    """
        Начальные условия:
            - приходит заказ на сервис, который не сверяется сверкой (наприимер 8)
        Ожидаемый результат:
            - заказ отсутствует в списке расхождений
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_info', 'bk_data']) as before:
        before.validate()
        order_info = _create_acted_orders()

        # Получаем только service_order_id(следующий по номеру) и больше ничего не будет
        service_order_id = steps.OrderSteps.next_id(Services.client)
        order_info['service_id'] = Services.client
        order_info['service_order_id'] = service_order_id

        bk_data = [
            str(order_info['service_order_id']),
            str(Services.client),
            str(0),
            str(order_info['shipment_info']['Shows']),
            str(0),
            str(int(order_info['shipment_info']['Money'])),
            str('RUB'),
            str(int(order_info['shipment_info']['Bucks']) * 1000000),
            str(0),
            str(0),
            str(0),
        ]

    cmp_data = shared_steps.SharedBlocks.run_obb2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == order_info['service_order_id']]

    # Проверяем, что заказ не найден в расхождениях
    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)))


"""
    Тесты, которые решено не переносить на SHARED:
    test_CHECK_2267:
        Начальные условия:
            -порядок колонок в файле изменен
            -заказы используются из предыдущего запуска
        Ожидаемый результат:
            проверяем, что результат запуска полностью совпадает с предыдущим результатам запуска
    
    test_CHECK_2542_without_end:
        Начальные условия:
            -в ручке от БК не приходит в конце #End
        Ожидаемый результат:
            запуск сверк падает с ошибкой  IncompleteBKFile
"""
