# coding: utf-8

from datetime import datetime, timedelta
from decimal import Decimal

import pytest
from hamcrest import equal_to, contains_string


from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as b_utils
import btestlib.reporter as reporter
import check.db
from check import steps as check_steps
from check import shared_steps
from check import utils as ch_utils
from check.defaults import Products
from check.utils import relative_date, LAST_DAY_OF_MONTH
from check.shared import CheckSharedBefore


END_OF_MONTH = relative_date(months=-1, day=LAST_DAY_OF_MONTH)
DIFFS_COUNT = 4
LATE_SHIPMENT_TEXT = u"""Расхождение вызвано поздними открутками, успешно обработанными Биллингом позднее сверяемого периода. Расхождений нет."""


def _create_acted_orders(orders_map):
    for keys in orders_map:
        orders_map[keys].update({'paysys_id': 1003})
    client_id = steps.ClientSteps.create()
    return check_steps.create_acted_orders(orders_map, client_id, steps.PersonSteps.create(client_id, 'ur'))



@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_UAO)
def test_new_ua_without_diff(shared_data):
    """
    Начальные условия:
        -количество открученного на родительском заказе равно сумме открученного на дочерних заказах
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['orders_map']) as before:
        before.validate()

        # новая схема общего счета;
        # перекрутка на дочернем - сверка должна его игнорировать.

        orders_map = _create_acted_orders(orders_map=
                                          {'child': {'shipment_info': {'Bucks': 30},
                                                     'consume_qty': 50},
                                           'parent': {'consume_qty': 50}},
                                          )

        # переходим к общему счету
        steps.OrderSteps.ua_enqueue([orders_map['parent']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['parent']['client_id'])
        steps.ClientSteps.migrate_to_currency(orders_map['parent']['client_id'], currency_convert_type='MODIFY',
                                              dt=datetime.now() - timedelta(days=1))

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(orders_map['parent']['id'])

        # создаем перекрутку на дочернем
        orders_map['child']['shipment_info']['Bucks'] += 50
        check_steps.do_campaign(orders_map['child'])

        # Отражаем открутку на родительском заказе
        steps.OrderSteps.ua_enqueue([orders_map['child']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child']['client_id'])

    cmp_data = shared_steps.SharedBlocks.run_uao(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    ids = []
    for key in orders_map:
        id = orders_map[key]['id']
        ids.append(id)

    expected_result = []

    result = [(row['root_order_id'], row['state']) for row in cmp_data if row['root_order_id'] in ids]
    reporter.log("RESULT = %s" % result)

    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_UAO)
def test_new_ua_with_diff(shared_data):
    """
    Начальные условия:
        -количество открученного на родительском заказе не равно сумме открученного на дочерних заказах
    Ожидаемый результат:
        заказ попадает в список расхождений,
        состояние = "Расходится количество открученного"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['orders_map']) as before:
        before.validate()

        # новая схема общего счета;
        # перекрутка на дочернем - сверка должна его игнорировать.

        orders_map = _create_acted_orders(orders_map=
                                          {'child': {'shipment_info': {'Bucks': 30},
                                                     'consume_qty': 50, 'paysys_id': 1003,
                                                     'service_id': 7, 'product_id': 1475},
                                           'parent': {'consume_qty': 50, 'paysys_id': 1003,
                                                      'service_id': 7, 'product_id': 1475}}
                                          )

        # переходим к общему счету
        steps.OrderSteps.merge(orders_map['parent']['id'],
                               [orders_map['child']['id']])
        steps.OrderSteps.ua_enqueue([orders_map['parent']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['parent']['client_id'])
        steps.ClientSteps.migrate_to_currency(orders_map['parent']['client_id'], currency_convert_type='MODIFY',
                                              dt=datetime.now() - timedelta(days=1))

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(orders_map['parent']['id'])

        # Отражаем открутку на родительском заказе
        steps.OrderSteps.ua_enqueue([orders_map['child']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child']['client_id'])

        # создаем перекрутку на дочернем
        orders_map['child']['shipment_info']['Bucks'] += 50
        check_steps.do_campaign(orders_map['child'])

    cmp_data = shared_steps.SharedBlocks.run_uao(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    ids = []
    for key in orders_map:
        id = orders_map[key]['id']
        ids.append(id)

    expected_result = [(orders_map['parent']['id'], 1)]

    result = [(row['root_order_id'], row['state']) for row in cmp_data if row['root_order_id'] in ids]
    reporter.log("RESULT = %s" % result)

    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_UAO)
def test_new_modify_ua_without_diff(shared_data):
    """
    Начальные условия:
        -мультивалютный клиент, один из заказов - фишечный
        -количество открученного на родительском заказе равно сумме открученного на дочерних заказах
        (перекрутка на одном дочернем равна откату на другом дочернем)
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['orders_map']) as before:
        before.validate()

        # новая схема общего счета;
        # клиент - мультивалютный;
        # в рамках отдельно взятого дочернего заказа имеем расхождение,
        # но в рамках общего счета расхождение отсутствует

        # создаем два дочерних заказа: монетный и фишечный
        orders_map = _create_acted_orders(orders_map=
                                          {'child_money': {'shipment_info': {'Money': 30},
                                                           'consume_qty': 500,
                                                           'product_id': Products.direct_money},
                                           'child_pcs': {'shipment_info': {'Bucks': 30},
                                                         'consume_qty': 50},
                                           'parent': {'consume_qty': 50}}
                                          )

        # делаем клиента мультивалютным на последний день предыдущего месяца
        steps.ClientSteps.migrate_to_currency(orders_map['child_money']['client_id'], 'MODIFY', dt=END_OF_MONTH)
        steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', orders_map['child_money']['client_id'])
        # db.balance().execute(
        #     """
        #     update t_client_service_data
        #     set migrate_to_currency = to_date( :date), update_dt = to_date( :date)
        #     where class_id = :client_id
        #     """,
        #     {'client_id': orders_map['child_money']['client_id'], 'date': END_OF_MONTH})

        # переходим к общему счету
        steps.OrderSteps.ua_enqueue([orders_map['parent']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['parent']['client_id'])

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(orders_map['parent']['id'])

        # включаем разбор общего счета
        steps.OrderSteps.ua_enqueue([orders_map['parent']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child_money']['client_id'])

        # делаем откат по второму дочернему заказу и перекрутку по второму дочернему заказу
        delta = 20
        orders_map['child_money']['shipment_info']['Money'] -= delta
        check_steps.do_campaign(orders_map['child_money'])
        orders_map['child_pcs']['shipment_info']['Money'] += delta
        check_steps.do_campaign(orders_map['child_pcs'])

        # включаем разбор общего счета
        steps.OrderSteps.ua_enqueue([orders_map['child_money']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child_money']['client_id'])


    cmp_data = shared_steps.SharedBlocks.run_uao(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    ids = []
    for key in orders_map:
        id = orders_map[key]['id']
        ids.append(id)

    # в рамках общего счета сумма откруток и актов сходится => Расхождений нет
    expected_result = []

    result = [(row['root_order_id'], row['state']) for row in cmp_data if row['root_order_id'] in ids]
    reporter.log("RESULT = %s" % result)

    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_UAO)
def test_new_modify_ua_with_diff(shared_data):
    """
    Начальные условия:
        -мультивалютный клиент, один из заказов - фишечный
        -количество открученного на родительском заказе не равно сумме открученного на дочерних заказах
    Ожидаемый результат:
        заказ попадает в список расхождений,
        состояние = "Расходится количество открученного"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['orders_map']) as before:
        before.validate()

        # новая схема общего счета;
        # клиент - мультивалютный;
        # имеем расхождение по сумме откруток между родительским заказом и дочерним

        # создаем два дочерних заказа: монетный и фишечный
        orders_map = _create_acted_orders(orders_map=
                                          {'child_money': {'shipment_info': {'Money': 30},
                                                           'consume_qty': 500,
                                                           'product_id': Products.direct_money},
                                           'child_pcs': {'shipment_info': {'Bucks': 30},
                                                         'consume_qty': 50},
                                           'parent': {'consume_qty': 50}}
                                          )

        # делаем клиента мультивалютным на последний день предыдущего месяца
        steps.ClientSteps.migrate_to_currency(orders_map['child_money']['client_id'], 'MODIFY', dt=END_OF_MONTH)
        steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', orders_map['child_money']['client_id'])
        # db.balance().execute(
        #     """
        #     update t_client_service_data
        #     set migrate_to_currency = to_date( :date), update_dt = to_date( :date)
        #     where class_id = :client_id
        #     """,
        #     {'client_id': orders_map['child_money']['client_id'], 'date': END_OF_MONTH})

        child_order_ids = [orders_map['child_money']['id'], orders_map['child_pcs']['id']]

        # переходим к общему счету
        steps.OrderSteps.merge(orders_map['parent']['id'], child_order_ids)
        steps.OrderSteps.ua_enqueue([orders_map['parent']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['parent']['client_id'])

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(orders_map['parent']['id'])

        # включаем разбор общего счета
        steps.OrderSteps.ua_enqueue([orders_map['child_money']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child_money']['client_id'])

        # делаем откат по второму дочернему заказу и перекрутку по второму дочернему заказу
        orders_map['child_pcs']['shipment_info']['Money'] += 20
        check_steps.do_campaign(orders_map['child_pcs'])


    cmp_data = shared_steps.SharedBlocks.run_uao(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    ids = []
    for key in orders_map:
        id = orders_map[key]['id']
        ids.append(id)

    # так как после прихода последней открутки мы не включали разбор общего счета,
    # то будем иметь расхождение в рамках общего счета
    expected_result = [(orders_map['parent']['id'], 1)]

    result = [(row['root_order_id'], row['state']) for row in cmp_data if row['root_order_id'] in ids]
    reporter.log("RESULT = %s" % result)

    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_UAO)
def test_new_ua_ignored_diff(shared_data):
    """
    Начальные условия:
        - количество открученного на дочернем заказе не равно сумме открученного на родительском заказах
        - перекрут в фишках
    Ожидаемый результат:
        заказ не должен попадать в список расхождений => заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['orders_map']) as before:
        before.validate()

        # новая схема общего счета;
        # перекрутка на дочернем - сверка должна его игнорировать.

        orders_map = _create_acted_orders(orders_map=
                                          {'child': {'shipment_info': {'Bucks': 30},
                                                     'consume_qty': 50},
                                           'parent_money': {'consume_qty': 50,
                                                            'product_id': Products.direct_money}}
                                          )

        # переходим к общему счету
        steps.OrderSteps.merge(orders_map['parent_money']['id'],
                               [orders_map['child']['id']])
        steps.OrderSteps.ua_enqueue([orders_map['parent_money']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['parent_money']['client_id'])
        steps.ClientSteps.migrate_to_currency(orders_map['parent_money']['client_id'], currency_convert_type='MODIFY',
                                              dt=datetime.now() - timedelta(days=1))

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(orders_map['parent_money']['id'])

        # включаем разбор общего счета
        steps.OrderSteps.ua_enqueue([orders_map['child']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child']['client_id'])

        # создаем перекрутку на дочернем
        orders_map['child']['shipment_info']['Bucks'] = round((Decimal(orders_map['child']['shipment_info']['Bucks']) + \
                                                               Decimal(0.0001)), 6)
        check_steps.do_campaign(orders_map['child'], datetime.now() - timedelta(days=1))


    cmp_data = shared_steps.SharedBlocks.run_uao(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    ids = []
    for key in orders_map:
        id = orders_map[key]['id']
        ids.append(id)

    expected_result = []

    result = [(row['root_order_id'], row['state']) for row in cmp_data if row['root_order_id'] in ids]
    reporter.log("RESULT = %s" % result)

    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_UAO)
def test_new_ua_different_product(shared_data):
    """
    Начальные условия:
        - количество открученного на дочернем заказе не равно сумме открученного на родительском заказах
        - перекрут в деньгах
    Ожидаемый результат:
        заказ не должен попадать в список расхождений => заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['orders_map']) as before:
        before.validate()

        # новая схема общего счета;
        # перекрутка на дочернем - сверка должна его игнорировать.

        orders_map = _create_acted_orders(orders_map=
                                          {'child_money': {'shipment_info': {'Bucks': 30},
                                                           'consume_qty': 50,
                                                           'product_id': Products.direct_money},
                                           'parent': {'consume_qty': 50}}
                                          )

        # переходим к общему счету
        steps.OrderSteps.merge(orders_map['parent']['id'],
                               [orders_map['child_money']['id']])
        steps.OrderSteps.ua_enqueue([orders_map['parent']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['parent']['client_id'])
        steps.ClientSteps.migrate_to_currency(orders_map['parent']['client_id'], currency_convert_type='MODIFY',
                                              dt=datetime.now() - timedelta(days=1))

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(orders_map['parent']['id'])

        # включаем разбор общего счета
        steps.OrderSteps.ua_enqueue([orders_map['child_money']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child_money']['client_id'])

        # создаем перекрутку на дочернем
        orders_map['child_money']['shipment_info']['Money'] += 5
        check_steps.do_campaign(orders_map['child_money'], datetime.now() - timedelta(days=1))


    cmp_data = shared_steps.SharedBlocks.run_uao(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    ids = []
    for key in orders_map:
        id = orders_map[key]['id']
        ids.append(id)

    # Есть расхождения в рамках общего счёта
    expected_result = [(orders_map['parent']['id'], 1)]

    result = [(row['root_order_id'], row['state']) for row in cmp_data if row['root_order_id'] in ids]
    reporter.log("RESULT = %s" % result)

    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_UAO)
def test_check_2464_new_ua_use_reverse_completions(shared_data):
    """
    Начальные условия:
        - количество открученного на родительском заказе не равно сумме перекруток на дочерних заказах
        - по дочернему заказу был откат
        - Дочерний заказ с переакченным - Такой случай не является расхождением
        - подробнее в задаче CHECK-2464
    Ожидаемый результат:
        заказ отсутствует в списке расхождений

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['orders_map']) as before:
        before.validate()

        # новая схема общего счета;
        # Дочерний заказ с переакченным - Такой случай не является расхождением
        # подробнее в задаче CHECK-2464

        orders_map = _create_acted_orders(orders_map=
                                          {'child': {'shipment_info': {'Bucks': 30},
                                                     'consume_qty': 50},
                                           'parent': {'consume_qty': 50}}
                                          )

        # переходим к общему счету
        steps.OrderSteps.merge(orders_map['parent']['id'],
                               [orders_map['child']['id']])
        steps.OrderSteps.ua_enqueue([orders_map['parent']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['parent']['client_id'])
        steps.ClientSteps.migrate_to_currency(orders_map['parent']['client_id'], currency_convert_type='MODIFY',
                                              dt=datetime.now() - timedelta(days=1))

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(orders_map['parent']['id'])

        # Отражаем открутку на родительском заказе
        steps.OrderSteps.ua_enqueue([orders_map['child']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child']['client_id'])

        # делаем откат по дочернему заказу
        orders_map['child']['shipment_info']['Bucks'] -= 10
        check_steps.do_campaign(orders_map['child'],
                                on_dt=(END_OF_MONTH.replace(hour=(END_OF_MONTH.hour + 1)) + timedelta(days=1)))
        query = """
                    delete from t_reverse_completion
                    where order_id = :child_order_id
                    """
        query_params = {'child_order_id': orders_map['child']['id']}
        db.balance().execute(query, query_params)

        # запись отсутствует в t_revers_completions, следовательно заказ попадает в список расхождений

    cmp_data = shared_steps.SharedBlocks.run_uao(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    ids = []
    for key in orders_map:
        id = orders_map[key]['id']
        ids.append(id)

    expected_result = []

    result = [(row['root_order_id'], row['state']) for row in cmp_data if row['root_order_id'] in ids]
    reporter.log("RESULT = %s" % result)

    b_utils.check_that(set(result), equal_to(set(expected_result)))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_UAO)
def test_check_2328_new_campaign(shared_data):
    """
    Начальные условия:
        -количество открученного на родительском заказе не равно сумме открученного на дочерних заказах(по дочернему заказу - перекрутка)
        -дата обновления открутки - будущее
    Ожидаемый результат:
        заказ попадает в список расхождений,
        но происходит авторазбор такого расхождения. В связанном с заказом тикете имеем комментарий
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['orders_map']) as before:
        before.validate()

        # новая схема общего счета;
        # на дочернем заказе - запоздавшая открутка

        orders_map = _create_acted_orders(orders_map=
                                          {'child': {'shipment_info': {'Bucks': 30},
                                                     'consume_qty': 50},
                                           'parent': {'consume_qty': 50}}
                                          )

        # переходим к общему счету
        steps.OrderSteps.merge(orders_map['parent']['id'],
                               [orders_map['child']['id']])
        steps.OrderSteps.ua_enqueue([orders_map['parent']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['parent']['client_id'])
        steps.ClientSteps.migrate_to_currency(orders_map['parent']['client_id'], currency_convert_type='MODIFY',
                                              dt=datetime.now() - timedelta(days=1))

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(orders_map['parent']['id'])

        # Отражаем открутку на родительском заказе
        steps.OrderSteps.ua_enqueue([orders_map['child']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child']['client_id'])

        # присылаем открутку на дочерний заказ c датой обновления открутки - будущее
        orders_map['child']['shipment_info']['Bucks'] += 10
        check_steps.do_campaign(orders_map['child'],
                                on_dt=(END_OF_MONTH.replace(hour=(END_OF_MONTH.hour + 1)) + timedelta(days=1)))

        query = """
                update bo.t_shipment
                set update_dt = :update_dt
                where service_order_id = :service_order_id
                        """
        query_params = {'update_dt': datetime.now() + timedelta(weeks=5),
                        'service_order_id': orders_map['child']['service_order_id']}
        db.balance().execute(query, query_params)


    cmp_data = shared_steps.SharedBlocks.run_uao(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']
    reporter.log("CMP_DATA = %s" % cmp_data)

    ids = []
    for key in orders_map:
        id = orders_map[key]['id']
        ids.append(id)

    expected_result = [(orders_map['parent']['id'], 1)]

    result = [(row['root_order_id'], row['state']) for row in cmp_data if row['root_order_id'] in ids]
    reporter.log("RESULT = %s" % result)

    b_utils.check_that(set(result), equal_to(set(expected_result)))
    
    
    # Проверяем правильность комментария в тикете
    check_code_name = 'uao'
    ticket = ch_utils.get_check_ticket(check_code_name, cmp_id)
    comments = list(ticket.comments.get_all())

    order_id = str(orders_map['parent']['id'])

    for comment in comments:
        if order_id in comment.text:
            b_utils.check_that(comments[0].text, contains_string(LATE_SHIPMENT_TEXT),
                              u'Проверяем, что в комментарии содержится требуемый текст, '
                              u'В комментарии указан ID заказа')
            break
    else:
        assert False, u'Комментарий авторазбора не найден'

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_UAO)
def test_aob_check_diffs_count(shared_data):
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_uao(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    b_utils.check_that(len(cmp_data), equal_to(DIFFS_COUNT),
                       u'Проверяем, что количество расхождений, выявленное сверкой, '
                       u'равно ожидаемому количеству расхождений')







