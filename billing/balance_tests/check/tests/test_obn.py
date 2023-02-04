# coding: utf-8
__author__ = 'chihiro'

import datetime
import decimal
import json
import os
import random

import pytest
from hamcrest import contains_string, is_in, empty, has_length, equal_to

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from btestlib import utils as butils
from balance.balance_db import balance
from check import db, defaults, utils, shared_steps
from check import steps as check_steps
from check.defaults import Services, DATA_DIR
from check.shared import CheckSharedBefore


'''
Осуществляет сравнение оказанных услуг (откруток) в рамках одного заказа в Биллинге и Навигаторе

ЗАБОР ДАННЫХ

Забираем данные из Справочника
    Забор данных осуществляется по http-ручке, которая возвращает данные по заказам из Навигатора.

      UNIQUE_FIELDS = [
         Field('order_id', sa.Integer, nullable=False)
     ]
     
     COMPARABLE_FIELDS = [
         Field('consume_qty', sa.Numeric, nullable=False),
         Field('completion_qty', sa.Numeric, nullable=False)
     ]
  
Забираем данные из Биллинга

  query_template = u"""
    with
    orders as (
      select %(orders_hints_comment)s
        o.id,
        o.service_id,
        o.service_order_id,
        o.dt,
        o.begin_dt
      from bo.t_order o
      where o.service_id = %(navigator_service_id)d
        and o.dt < %(cut_dt)s + 1
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
      where ch.start_dt <= %(cut_dt)s
        and ch.end_dt > %(cut_dt)s

      union all

      select
        o.id as order_id,
        s.consumption as completion_qty,
        s.dt as shipment_dt,
        s.update_dt as shipment_update_dt
      from bo.t_shipment s
      join orders o on o.service_id = s.service_id
        and o.service_order_id = s.service_order_id
      where s.dt < %(cut_dt)s + 1
    ),
    operations as (
      select o.id order_id, q.consume_qty qty, q.dt
      from bo.t_consume q
      join bo.t_operation op on op.id = q.operation_id
      join orders o on o.id = q.parent_order_id
      where op.dt < %(cut_dt)s + 1

      union all

      select o.id order_id, -r.reverse_qty qty, r.dt
      from bo.t_reverse r
      join bo.t_operation op on op.id = r.operation_id
      join bo.t_consume q on q.id = r.consume_id
      join orders o on o.id = q.parent_order_id
      where op.dt < %(cut_dt)s + 1
    ),
    operation_totals as (
      select
        op.order_id,
        sum(op.qty) as consume_qty,
        max(op.dt) as last_operation_dt
      from operations op
      group by op.order_id
    )
    select %(result_query_hints_comment)s
      o.service_order_id as order_id,
      nvl(ot.consume_qty, 0) as consume_qty,
      nvl(c.completion_qty, 0) as completion_qty,
      c.shipment_dt,
      c.shipment_update_dt,
      ot.last_operation_dt
    from orders o
    left join operation_totals ot on ot.order_id = o.id
    left join completions c on c.order_id = o.id
    """
    
    Типы расхождений:  
        1 - Отсутствует в Навигаторе
        2 - Отсутствует в Биллинге
        3 - Расходится количество зачисленного
        4 - Расходится количество открученного
'''

TIME_NOW = datetime.datetime.now()


def create_order(count=100.0):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    service_order_id = steps.OrderSteps.next_id(service_id=110)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=110, product_id=1475)
    orders_list = [{'ServiceID': 110, 'ServiceOrderID': service_order_id, 'Qty': 1, 'BeginDT': TIME_NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=TIME_NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1003,
                                                 credit=0, contract_id=None, overdraft=0)

    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(110, service_order_id, {'Bucks': count}, 0, datetime.datetime.now())
    act_id = steps.ActsSteps.generate(client_id, force=1, date=TIME_NOW)[0]

    return service_order_id, order_id




@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBN)
def test_without_diff(shared_data):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        -количество открученного и зачисленного сходится
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['navi_data']) as before:
        before.validate()

        # TODO is there need order_id ?
        service_order_id, order_id = create_order()
        navi_data = {
            'completion_qty':  100,
            'consumption_qty': 1,
            'order_id':        service_order_id
        }

    cmp_data = shared_steps.SharedBlocks.run_obn(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == navi_data['order_id']]
    reporter.log("Result = %s" % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBN)
def test_not_found_in_navi(shared_data):
    """
    Начальные условия:
        -заказ присутствует в Биллинге
        -заказ отсутствует в Навигаторе
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в Навигаторе"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['service_order_id']) as before:
        before.validate()

        service_order_id, order_id = create_order()
        navi_data = None

    cmp_data = shared_steps.SharedBlocks.run_obn(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == service_order_id]
    reporter.log("Result = %s" % result)

    butils.check_that((service_order_id, 1), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBN)
def test_not_found_in_billing(shared_data):
    """
    Начальные условия:
        -заказ присутствует в Навигаторе
        -заказ отсутствует в Биллинге
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в биллинге"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['navi_data']) as before:
        before.validate()

        service_order_id = random.randint(100000000, 999999999)
        navi_data = {
            'completion_qty': 100,
            'consumption_qty': 1,
            'order_id': service_order_id
        }

    cmp_data = shared_steps.SharedBlocks.run_obn(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == navi_data['order_id']]
    reporter.log("Result = %s" % result)

    butils.check_that((navi_data['order_id'], 2), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBN)
def test_consume_qty_not_converge(shared_data):
    """
    Начальные условия:
        -заказ присутствует в Навигаторе, сумма зачисленного на дату сверки = 100 ед.
        -заказ присутствует в Биллинге, сумма зачисленного на дату сверки = 150 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество зачисленного"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['navi_data']) as before:
        before.validate()

        service_order_id, order_id = create_order()
        navi_data = {
            'completion_qty': 100,
            'consumption_qty': 30,
            'order_id': service_order_id
        }

    cmp_data = shared_steps.SharedBlocks.run_obn(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == navi_data['order_id']]
    reporter.log("Result = %s" % result)

    butils.check_that((navi_data['order_id'], 3), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBN)
def test_comptetion_qty_not_converge(shared_data):
    """
    Начальные условия:
        -заказ присутствует в Навигаторе, сумма открученного на дату сверки = 10 ед.
        -заказ присутствует в Биллинге, сумма открученного на дату сверки = 30 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество открученного"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['navi_data']) as before:
        before.validate()

        service_order_id, order_id = create_order()
        navi_data = {
            'completion_qty': 150,
            'consumption_qty': 1,
            'order_id': service_order_id
        }

    cmp_data = shared_steps.SharedBlocks.run_obn(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == navi_data['order_id']]
    reporter.log("Result = %s" % result)

    butils.check_that((navi_data['order_id'], 4), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBN)
def test_check_2657_new_operation(shared_data):
    """
    Начальные условия:
        -заказ присутствует в Навигаторе, сумма зачисленного на дату сверки = 10 ед.
        -заказ присутствует в Биллинге, сумма зачисленного на дату сверки = 10 ед.
        -в t_operation у заказа проставлена дата больше даты среза
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество зачисленного"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['navi_data']) as before:
        before.validate()

        service_order_id, order_id = create_order()
        invoice_id = db.invoice_id_by_order(order_id)

        balance().execute(
            'update t_operation set dt = :dt where invoice_id = :invoice_id',
            {'dt': datetime.datetime.now() + datetime.timedelta(days=10), 'invoice_id': invoice_id}
        )

        navi_data = {
            'completion_qty': 100,
            'consumption_qty': 1,
            'order_id': service_order_id
        }

    cmp_data = shared_steps.SharedBlocks.run_obn(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == navi_data['order_id']]
    reporter.log("Result = %s" % result)

    butils.check_that((navi_data['order_id'], 3), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBN)
def test_precision_without_diff(shared_data):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        -количество открученного и зачисленного сходится с точность до 4-х знаков после запятой
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['navi_data']) as before:
        before.validate()

        # TODO is there need order_id ?
        service_order_id, order_id = create_order(count=100.1111)
        navi_data = {
            'completion_qty':  100.1111,
            'consumption_qty': 1,
            'order_id':        service_order_id
        }

    cmp_data = shared_steps.SharedBlocks.run_obn(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == navi_data['order_id']]
    reporter.log("Result = %s" % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBN)
def test_precision_with_diff(shared_data):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        -количество открученного и зачисленного рассходится на 0,0001
    Ожидаемый результат:
        заказ присутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['navi_data']) as before:
        before.validate()

        # TODO is there need order_id ?
        service_order_id, order_id = create_order(count=100.1111)
        navi_data = {
            'completion_qty':  100.1110,
            'consumption_qty': 1,
            'order_id':        service_order_id
        }

    cmp_data = shared_steps.SharedBlocks.run_obn(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['order_id'], row['state'])
              for row in cmp_data if row['order_id'] == navi_data['order_id']]
    reporter.log("Result = %s" % result)

    butils.check_that((navi_data['order_id'], 4), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBN)
def test_diffs_count(shared_data):
    """
        В тесте проверяем, что общее количество росхождений в тестах совпадает с ожидаемым
    """
    diffs_count = 6

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_obn(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    butils.check_that(cmp_data, has_length(diffs_count))

