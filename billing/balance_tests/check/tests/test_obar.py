# coding: utf-8

import datetime
import decimal


from collections import namedtuple
import pytest
from hamcrest import contains_string, is_in, empty, has_length, not_
from startrek_client import Startrek

import btestlib.reporter as reporter
import check
from balance import balance_api as api, balance_steps as steps
from btestlib import utils as butils
from check import db
from check import steps as check_steps
from check import shared_steps
from check.shared import CheckSharedBefore
from check.defaults import STARTREK_PARAMS

"""
Выполняет сверку зачисленного и открученного по заказам в Биллинге и Auto.ru.
Период сравнения до текущего момента.

Забор данных
Забираем данные из Auto.ru (в таблицу cmp.obar_auto_ru)
Забор данных осуществляется по http-ручке, которая возвращает json с заказами из Auto.ru. Данные, полученные из json, систематизируем и добавляем в таблицу

Описание полей:
  [
  {
    "order_id": int,
    "consume_qty": float,
    "completion_qty": float,
    "unit": "days/units/money" // Все заказы автору в vsbilling money
    "dt": "2017-10-12 00:05:03" // Время на момент которого был зафиксирован срез
  },
  {
    ...
  }
]
  
Забираем данные из Биллинга (в таблицу cmp.obar_billing)
sql-запрос:
  with
    orders as (
      select --+ %(orders_hints)s
        o.id,
        o.service_id,
        o.service_order_id,
        o.dt,
        o.begin_dt
      from bo.t_order o
      where o.service_id = %(auto_ru_service_id)d
        and o.service_code not in (%(old_product_ids)s)
        and o.dt < %(max_order_creation_dt)s + 1
        %(objects_filter)s
    ),
    completions as (
      select
        ch.order_id,
        ch.completion_qty,
        ch.shipment_dt,
        ch.shipment_update_dt
      from bo.t_completion_history ch
      join orders o on o.id = ch.order_id
      where ch.start_dt <= %(completions_dt)s
        and ch.end_dt > %(completions_dt)s

      union all

      select
        o.id as order_id,
        s.consumption as completion_qty,
        s.dt as shipment_dt,
        s.update_dt as shipment_update_dt
      from bo.t_shipment s
      join orders o on o.service_id = s.service_id
        and o.service_order_id = s.service_order_id
      where s.dt < %(completions_dt)s + 1
    ),
    operations as (
      select o.id order_id, q.consume_qty qty, q.dt
      from bo.t_consume q
      join orders o on o.id = q.parent_order_id
      where q.dt < %(operations_dt)s + 1

      union all

      select o.id order_id, -r.reverse_qty qty, r.dt
      from bo.t_reverse r
      join bo.t_consume q on q.id = r.consume_id
      join orders o on o.id = q.parent_order_id
      where r.dt < %(operations_dt)s + 1
    ),
    operation_totals as (
      select
        op.order_id,
        sum(op.qty) as consume_qty,
        max(op.dt) as last_operation_dt
      from operations op
      group by op.order_id
    )
    select --+ %(result_query_hints)s
      o.service_order_id as order_id,
      nvl(ot.consume_qty, 0) as consume_qty,
      nvl(c.completion_qty, 0) as completion_qty,
      c.shipment_dt,
      c.shipment_update_dt,
      ot.last_operation_dt
    from orders o
    left join operation_totals ot on ot.order_id = o.id
    left join completions c on c.order_id = o.id
  
из сверки исключаются старые продукты (подробнее про старые продукты тут:
https://wiki.yandex-team.ru/testirovanie/functesting/billing/check/check_list/obar/)

Типы расхождений:
    1 - Отсутствует в Auto.ru
    2 - Отсутствует в Биллинге
    3 - Расходится количество зачисленного
    4 - Расходится количество открученного


"""


@pytest.fixture(scope="module")
def fixtures():

    client_id = check_steps.create_client()
    person_id = check_steps.create_person(client_id, person_category='ur')

    data = namedtuple('data', 'client_id person_id service_id product_id paysys_id')
    data_list = data(client_id=client_id,
                     person_id=person_id,
                     service_id=99,
                     product_id=505123,
                     paysys_id=1201003)

    # return client_id, person_id, contract_type, begin_dt, end_dt
    return data_list



@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBAR)
def test_no_diffs(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        -количество открученного и зачисленного сходится
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """

    with CheckSharedBefore(shared_data=shared_data, cache_vars=['auto_data', 'service_order_id']) as before:
        before.validate()

        f = fixtures

        order_map = check_steps.create_act_map({1: {'paysys_id':     f.paysys_id,
                                                  'service_id':    f.service_id,
                                                  'product_id':    f.product_id,
                                                  'shipment_info': {'Money': 11},
                                                  'consume_qty':   54}},
                                             f.client_id, f.person_id)
        new_date = datetime.datetime.now()

        current_order = order_map[0]
        service_order_id = current_order['service_order_id']

        auto_data = {
            'service_order_id': current_order['service_order_id'],
            'order_id':         current_order['id'],
            'consume_qty':      int(decimal.Decimal(current_order['consume_qty'])),
            'completion_qty':   int(decimal.Decimal(current_order['shipment_info']['Money'])),
            'unit':             'days',
            'dt':               new_date.strftime("%Y-%m-%dT%H:%M: %S.625+03: 00")
        }

    cmp_data = shared_steps.SharedBlocks.run_obar(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)


    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBAR)
def test_not_found_in_autoru(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в Биллинге
        -заказ отсутствует в Auto.ru
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в Auto.ru"
    """

    with CheckSharedBefore(shared_data=shared_data, cache_vars=['service_order_id']) as before:
        before.validate()

        f = fixtures

        order_map = check_steps.create_act_map({1: {'paysys_id':     f.paysys_id,
                                                  'service_id':    f.service_id,
                                                  'product_id':    f.product_id,
                                                  'shipment_info': {'Money': 11},
                                                  'consume_qty':   54}},
                                             f.client_id, f.person_id)

        current_order = order_map[0]
        service_order_id = current_order['service_order_id']

        auto_data = None

    cmp_data = shared_steps.SharedBlocks.run_obar(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((service_order_id, 1), is_in(result))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBAR)
def test_not_found_in_billing(shared_data, fixtures):
    """
    Начальные условия:
        -заказ присутствует в Auto.ru
        -заказ отсутствует в Биллинге
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в биллинге"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['auto_data', 'service_order_id']) as before:
        before.validate()

        f = fixtures

        order_map = check_steps.create_act_map({1: {'paysys_id':     f.paysys_id,
                                                  'service_id':    f.service_id,
                                                  'product_id':    f.product_id,
                                                  'shipment_info': {'Money': 11},
                                                  'consume_qty':   54}},
                                             f.client_id, f.person_id)

        new_date = datetime.datetime.now()

        current_order = order_map[0]
        service_order_id = steps.OrderSteps.next_id(f.service_id)

        auto_data = {
            'service_order_id': service_order_id,
            'order_id':         current_order['id'],
            'consume_qty':      int(decimal.Decimal(current_order['consume_qty'])),
            'completion_qty':   int(decimal.Decimal(current_order['shipment_info']['Money'])),
            'unit': 'days',
            'dt': new_date.strftime("%Y-%m-%dT%H:%M: %S.625+03: 00")
        }

    cmp_data = shared_steps.SharedBlocks.run_obar(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((service_order_id, 2), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBAR)
def test_consume_qty_not_converge(shared_data, fixtures):

    """
    Начальные условия:
        -заказ присутствует в Auto.ru, сумма зачисленного на дату сверки = 11 ед.
        -заказ присутствует в биллинге, сумма зачисленного на дату сверки = 21 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество открученного"
    """

    with CheckSharedBefore(shared_data=shared_data, cache_vars=['auto_data', 'service_order_id']) as before:
        before.validate()

        f = fixtures

        order_map = check_steps.create_act_map({1: {'paysys_id': f.paysys_id,
                                                  'service_id': f.service_id,
                                                  'product_id': f.product_id,
                                                  'shipment_info': {'Money': 11},
                                                  'consume_qty': 54}},
                                             f.client_id, f.person_id)

        new_date = datetime.datetime.now()

        current_order = order_map[0]
        service_order_id = current_order['service_order_id']

        auto_data = {
            'service_order_id': current_order['service_order_id'],
            'order_id':         current_order['id'],
            'consume_qty':      int(decimal.Decimal(current_order['consume_qty'])) + 5,
            'completion_qty':   int(decimal.Decimal(current_order['shipment_info']['Money'])),
            'unit':             'days',
            'dt':               new_date.strftime("%Y-%m-%dT%H:%M: %S.625+03: 00")
        }

    cmp_data = shared_steps.SharedBlocks.run_obar(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((service_order_id, 3), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBAR)
def test_completion_qty_not_converge(shared_data, fixtures):

    """
    Начальные условия:
        -заказ присутствует в Auto.ru, сумма открученного на дату сверки = 55 ед.
        -заказ присутствует в биллинге, сумма открученного на дату сверки = 65 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество зачисленного"
    """

    with CheckSharedBefore(shared_data=shared_data, cache_vars=['auto_data', 'service_order_id']) as before:
        before.validate()

        f = fixtures

        act_map = check_steps.create_act_map({1: {'paysys_id': f.paysys_id,
                                                  'service_id': f.service_id,
                                                  'product_id': f.product_id,
                                                  'shipment_info': {'Money': 11},
                                                  'consume_qty': 54}},
                                             f.client_id, f.person_id)

        new_date = datetime.datetime.now()
        new_date_autoru = new_date - datetime.timedelta(days=1, hours=3)

        current_order = act_map[0]
        service_order_id = current_order['service_order_id']


        auto_data = {
            'service_order_id': current_order['service_order_id'],
            'order_id':         current_order['id'],
            'consume_qty':      int(decimal.Decimal(current_order['consume_qty'])) ,
            'completion_qty':   int(decimal.Decimal(current_order['shipment_info']['Money'])) + 8,
            'unit':             'days',
            'dt':               new_date_autoru.strftime("%Y-%m-%dT%H:%M: %S.625+03: 00")
        }

    cmp_data = shared_steps.SharedBlocks.run_obar(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((service_order_id, 4), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBAR)
def test_check_2593_old_product(shared_data, fixtures):

    """
    Начальные условия:
        -product_id = 505206
        -заказ присутствует в Биллинге
        -заказ отсутствует в Auto.ru
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
"""

    with CheckSharedBefore(shared_data=shared_data, cache_vars=['auto_data', 'service_order_id']) as before:
        before.validate()

        f = fixtures

        old_product_id = 505206
        old_paysys_id = 1201003
        order_map = check_steps.create_act_map({1: {'paysys_id': old_paysys_id,
                                                  'service_id': f.service_id,
                                                  'product_id': old_product_id,
                                                  'shipment_info': {'Days': 12},
                                                  'consume_qty': 54}}, f.client_id, f.person_id)

        current_order = order_map[0]
        service_order_id = current_order['service_order_id']

        auto_data = None

    cmp_data = shared_steps.SharedBlocks.run_obar(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBAR)
def test_sent_completions_auto_analysis_qty_diff(shared_data, fixtures):

    """
    Начальные условия:
        -заказ присутствует в Auto.ru, сумма открученного на дату сверки = 65 ед.
        -заказ присутствует в биллинге, сумма открученного на дату сверки = 55 ед.
        -заказ присутствует в ручке http://billing-internal-api.http.yandex.net:34150/api/1.x/service/autoru/orders/sent-spendings?date=%(date)s, сумма открученного на дату сверки = 65 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество зачисленного"
        происходит авторазбор по заказу с комментарием "Открутки по заказу были отправлены в следующий день"
    """

    with CheckSharedBefore(shared_data=shared_data, cache_vars=['auto_data', 'service_order_id',
                                                                'auto_ru_completion_qty', 'auto_analysis']) as before:
        before.validate()

        f = fixtures

        auto_analysis = True

        billing_completion_qty = 11
        auto_ru_completion_qty = billing_completion_qty + 9

        new_date = datetime.datetime.now()

        sent_date_auto_ru = (new_date.replace(hour=0, minute=0, second=0) +
                             datetime.timedelta(days=1, minutes=30))
        auto_ru_dt_format = '%Y-%m-%dT%H:%M:%S.625+03:00'

        order_map = check_steps.create_act_map(
                                        {1: {'paysys_id':  f.paysys_id,
                                             'service_id': f.service_id,
                                             'product_id': f.product_id,
                                             'shipment_info': {'Money': billing_completion_qty},
                                             'consume_qty': 54}},
                                        f.client_id, f.person_id)

        current_order = order_map[0]
        service_order_id = current_order['service_order_id']

        auto_data= {
                    'service_order_id': current_order['service_order_id'],
                    'order_id':         current_order['id'],
                    'consume_qty':      int(decimal.Decimal(current_order['consume_qty'])),
                    'completion_qty':   int(auto_ru_completion_qty),
                    'unit':             'days',
                    'dt':               new_date.strftime(auto_ru_dt_format),
                    'sent_dt':          sent_date_auto_ru.strftime(auto_ru_dt_format),
        }

    cmp_data = shared_steps.SharedBlocks.run_obar(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((service_order_id, 4), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBAR)
def test_sent_completions_auto_analysis_with_diff(shared_data, fixtures):

    """
    Начальные условия:
        -заказ присутствует в Auto.ru, сумма открученного на дату сверки = 65 ед.
        -заказ присутствует в биллинге, сумма открученного на дату сверки = 55 ед.
        -заказ присутствует в ручке http://billing-internal-api.http.yandex.net:34150/api/1.x/service/autoru/orders/sent-spendings?date=%(date)s, сумма открученного на дату сверки = 55 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество зачисленного"
        авторазбор не происходит
    """

    with CheckSharedBefore(shared_data=shared_data, cache_vars=['auto_data', 'service_order_id',
                                                                'auto_ru_completion_qty', 'auto_analysis']) as before:
        before.validate()

        f = fixtures

        auto_analysis = True

        billing_completion_qty = 11
        auto_ru_completion_qty = billing_completion_qty + 9

        new_date = datetime.datetime.now()

        sent_date_auto_ru = (new_date.replace(hour=0, minute=0, second=0) +
                             datetime.timedelta(days=1, minutes=30))
        auto_ru_dt_format = '%Y-%m-%dT%H:%M:%S.625+03:00'

        order_map = check_steps.create_act_map(
                                        {1: {'paysys_id':  f.paysys_id,
                                             'service_id': f.service_id,
                                             'product_id': f.product_id,
                                             'shipment_info': {'Money': billing_completion_qty},
                                             'consume_qty': 54}},
                                        f.client_id, f.person_id)

        current_order = order_map[0]
        service_order_id = current_order['service_order_id']

        auto_data= {
            'service_order_id': current_order['service_order_id'],
            'order_id':         current_order['id'],
            'consume_qty':      int(decimal.Decimal(current_order['consume_qty'])),
            'completion_qty':   int(billing_completion_qty),
            'unit':             'days',
            'dt':               new_date.strftime(auto_ru_dt_format),
            'sent_dt':          sent_date_auto_ru.strftime(auto_ru_dt_format),
        }

    cmp_data = shared_steps.SharedBlocks.run_obar(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((service_order_id, 4), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBAR)
def test_check_diffs_count(shared_data):
    """
        В тесте проверяем, что общее количество росхождений в тестах совпадает с ожидаемым
    """
    additional_data = 2 # дополнительные тестовые данные, которые попадают в cmp_data
    diffs_count = 6 + additional_data

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_obar(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    butils.check_that((cmp_data), has_length(diffs_count))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBAR)
def test_sent_completions_auto_analyzer(shared_data):

    with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_obar(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    order_id_qty_diff = [row['order_id'] for row in cmp_data if row['state'] == 'qty_diff'][0]
    order_id_with_diff =[row['order_id'] for row in cmp_data if row['state'] == 'with_diff'][0]

    cmp_id = cmp_data[0]['cmp_id']
    reporter.log("CMP_ID = %s " % cmp_id)

    query = 'select issue_key from CMP.obar_cmp where id = {cmp_id}'
    query = query.format(cmp_id=cmp_id)
    issue_key = api.test_balance().ExecuteSQL('cmp', query)[0]['issue_key']

    startrek = Startrek(**STARTREK_PARAMS)
    ticket = startrek.issues[issue_key]

    comments = list(ticket.comments.get_all())
    comment = comments and comments[0].text or ''

    expected_comment = u'Открутки по заказу были отправлены в следующий день'
    expected_order = str(order_id_qty_diff)

    butils.check_that(comment, contains_string(expected_comment))
    butils.check_that(comment, contains_string(expected_order))

    butils.check_that(comment, not_(contains_string(str(order_id_with_diff))))
