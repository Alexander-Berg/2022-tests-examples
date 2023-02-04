# coding: utf-8
__author__ = 'chihiro'

import datetime
import decimal


import pytest
from hamcrest import contains_string, is_in, empty, has_length

import btestlib.reporter as reporter
import check
from balance import balance_steps as steps
from balance import balance_db
from btestlib import utils as butils
from btestlib.constants import Currencies, PersonTypes, Paysyses, Services
from check import db
from check import steps as check_steps
from check import utils, shared_steps
from check.shared import CheckSharedBefore

"""
Сверка Баланс - ADO (oba2)

Выполняет сверку зачисленного и открученного по заказам в разрезе сервиса ADO.
Период сравнения до текущего момента.

Типы расхождений:
    1 - Отсутствует в AdOffice
    2 - Отсутствует в Биллинге
    3 - Расходится кол-во зачисленного
    4 - Расходится кол-во открученного
    
Забор данных
Забираем данные из ADO (в таблицу cmp.oba2_ado)
Забор данных осуществляется по http-ручке, которая возвращает json с заказами из ADO. Данные, полученные из json, систематизируем и добавляем в таблицу
Описание полей:
  _columns = [
        'service_order_id',
        'volume_accepted',
        'volume_realized',
        'volume_plan',
        'billing_realized',
        'date_begin',
        'date_end',
        'product_type_nmb',
        'is_specproject'
    ]

    _value_parsers = {
        'service_order_id': int,
        'volume_accepted': int,
        'volume_realized': int,
        'volume_plan': int,
        'billing_realized': int,
        'date_begin': parse_ad_office_date,
        'date_end': parse_ad_office_date,
        'product_type_nmb': int,
        'is_specproject': parse_ad_office_bool
    }
  

Забираем данные из Биллинга (в таблицу cmp.oba2_billing)
sql-запрос :
  with
    orders as (
      select --+ %(orders_hints)s
        o.id,
        o.service_id,
        o.service_order_id,
        o.dt,
        o.begin_dt
      from bo.t_order o
      where o.service_id = %(ad_office_service_id)d
        and o.dt < %(max_order_creation_dt)s + 1
        and nvl(o.begin_dt, o.dt) < %(max_order_begin_dt)s + 1
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
      where trunc(s.dt) <= %(completions_dt)s
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
      o.service_order_id,
      -- Из AdOffice зачисленное приходит в таком виде
      trunc(nvl(ot.consume_qty, 0)) as consume_qty,
      nvl(c.completion_qty, 0) as completion_qty,
      c.shipment_dt,
      c.shipment_update_dt,
      ot.last_operation_dt
    from orders o
    left join operation_totals ot on ot.order_id = o.id
    left join completions c on c.order_id = o.id
    where coalesce(c.shipment_dt, o.begin_dt, o.dt) >= %(check_period_begin_dt)s
  

"""

CHECK_CODE_NAME = 'oba2'

CURRENCY_TO_PAYSYS_AND_PERSON = {
    Currencies.USD: {'paysys_id': Paysyses.BANK_SW_YT_USD.id, 'person_type': PersonTypes.SW_YT.code,
                     'qty': decimal.Decimal('100')},
    Currencies.EUR: {'paysys_id': Paysyses.BANK_SW_UR_EUR.id, 'person_type': PersonTypes.SW_UR.code,
                     'qty': decimal.Decimal('2000')},
    Currencies.RUB: {'paysys_id': Paysyses.BANK_UR_RUB.id, 'person_type': PersonTypes.UR.code,
                     'qty': decimal.Decimal('5500')},
    Currencies.TRY: {'paysys_id': Paysyses.BANK_TR_UR_TRY.id, 'person_type': PersonTypes.TRU.code,
                     'qty': decimal.Decimal('10000')}
}
DEFAULT_PRODUCT_2 = {'id': 508334, 'name': u"Использование+данных+'Aidata.me'.+Тариф+03"}

contract_start_dt, _, month2_start_dt, month2_end_dt, month3_start_dt, month3_end_dt = \
    butils.Date.previous_three_months_start_end_dates(dt=datetime.datetime.today())


def data_for_test_exclude():
    service_id = Services.MEDIA_70.id
    product_id = DEFAULT_PRODUCT_2['id']
    paysys_id = CURRENCY_TO_PAYSYS_AND_PERSON[Currencies.USD]['paysys_id']
    client_id = steps.ClientSteps.create()
    
    return service_id, product_id, paysys_id, client_id


def create_act_for_dmp(service_id=Services.MEDIA_70.id, product_id=DEFAULT_PRODUCT_2['id'], currency=Currencies.USD,
                       shipment_qty=11):
    paysys = CURRENCY_TO_PAYSYS_AND_PERSON[currency]['paysys_id']
    person_type = CURRENCY_TO_PAYSYS_AND_PERSON[currency]['person_type']
    qty = CURRENCY_TO_PAYSYS_AND_PERSON[currency]['qty']
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_type)

    service_order_id = steps.OrderSteps.next_id(service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                                       service_id=service_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': month3_end_dt}, ]

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': month3_end_dt})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys, credit=0,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Shows': shipment_qty}, 0, month3_end_dt)
    steps.ActsSteps.generate(client_id, force=1, date=month3_end_dt)
    return service_order_id, order_id, qty, shipment_qty, client_id, invoice_id


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBA2)
def test_without_diff(shared_data):
    """
    Начальные условия:
        -заказ присутствует в обеих системах
        -количество открученного и зачисленного сходится
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['ado_data']) as before:
        before.validate()

        service_order_id, order_id, consume_qty, shipment_qty, client_id, invoice_id = create_act_for_dmp()
        new_date = datetime.datetime.now() - datetime.timedelta(days=1)
        check.db.update_date(order_id, new_date)

        # Обновляем дату заявки
        query = 'update bo.t_consume set dt = :date where invoice_id = :invoice_id'
        query_params = {'date': new_date, 'invoice_id': invoice_id}
        balance_db.balance().execute(query, query_params)

        ado_data = {
            'service_order_id': service_order_id,
            'billing_realized': int(decimal.Decimal(shipment_qty)),
            'volume_accepted':  int(decimal.Decimal(consume_qty)),
            'date_begin':       datetime.datetime.now().strftime('%d.%m.%Y'),
            'date_end':         new_date.strftime('%d.%m.%Y')
        }

    cmp_data = shared_steps.SharedBlocks.run_oba2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == ado_data['service_order_id']]
    reporter.log("Result = %s" % cmp_data)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBA2)
def test_not_found_in_ado(shared_data):
    """
    Начальные условия:
        -заказ присутствует в Биллинге
        -заказ отсутствует в ADO
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в ADO"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['service_order_id']) as before:
        before.validate()

        service_order_id, order_id, consume_qty, shipment_qty, client_id, invoice_id = create_act_for_dmp()
        new_date = datetime.datetime.now() - datetime.timedelta(days=1)
        check.db.update_date(order_id, new_date)
        # check.db.update_date_consume_by_invoice(invoice_id, new_date)
        # Обновляем дату заявки
        query = 'update bo.t_consume set dt = :date where invoice_id = :invoice_id'
        query_params = {'date': new_date, 'invoice_id': invoice_id}
        balance_db.balance().execute(query, query_params)

        ado_data = None

    cmp_data = shared_steps.SharedBlocks.run_oba2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == service_order_id]
    reporter.log("Result = %s" % cmp_data)

    butils.check_that((service_order_id, 1), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBA2)
def test_not_found_in_billing(shared_data):
    """
    Начальные условия:
        -заказ присутствует в ADO
        -заказ отсутствует в Биллинге
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в биллинге"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['ado_data']) as before:
        before.validate()
        _, _, consume_qty, shipment_qty, _, _ = create_act_for_dmp()
        new_date = datetime.datetime.now() - datetime.timedelta(days=1)
        ado_data = {
            'service_order_id': 1,
            'billing_realized': int(decimal.Decimal(shipment_qty)),
            'volume_accepted': int(decimal.Decimal(consume_qty)),
            'date_begin': datetime.datetime.now().strftime('%d.%m.%Y'),
            'date_end': new_date.strftime('%d.%m.%Y')
        }

    cmp_data = shared_steps.SharedBlocks.run_oba2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == ado_data['service_order_id']]
    reporter.log("Result = %s" % cmp_data)

    butils.check_that((ado_data['service_order_id'], 2), is_in(result))
    

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBA2)
def test_consume_qty_not_converge(shared_data):
    """
    Начальные условия:
        -заказ присутствует в ADO, сумма зачисленного на дату сверки = 11 ед.
        -заказ присутствует в биллинге, сумма зачисленного на дату сверки = 21 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество открученного"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['ado_data']) as before:
        before.validate()

        service_order_id, order_id, consume_qty, shipment_qty, client_id, invoice_id = create_act_for_dmp()
        new_date = datetime.datetime.now() - datetime.timedelta(days=1)
        check.db.update_date(order_id, new_date)
        # check.db.update_date_consume_by_invoice(invoice_id, new_date)
        # Обновляем дату заявки
        query = 'update bo.t_consume set dt = :date where invoice_id = :invoice_id'
        query_params = {'date': new_date, 'invoice_id': invoice_id}
        balance_db.balance().execute(query, query_params)

        ado_data = {
            'service_order_id': service_order_id,
            'billing_realized': int(decimal.Decimal(shipment_qty)),
            'volume_accepted':  int(decimal.Decimal(consume_qty) + 10),
            'date_begin':       datetime.datetime.now().strftime('%d.%m.%Y'),
            'date_end':         new_date.strftime('%d.%m.%Y')
        }

    cmp_data = shared_steps.SharedBlocks.run_oba2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == ado_data['service_order_id']]
    reporter.log("Result = %s" % cmp_data)

    butils.check_that((ado_data['service_order_id'], 3), is_in(result))
    

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBA2)
def test_completion_qty_not_converge(shared_data):
    """
    Начальные условия:
        -заказ присутствует в ADO, сумма открученного на дату сверки = 55 ед.
        -заказ присутствует в биллинге, сумма открученного на дату сверки = 65 ед.
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Расходится количество зачисленного"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['ado_data']) as before:
        before.validate()

        service_order_id, order_id, consume_qty, shipment_qty, client_id, invoice_id = create_act_for_dmp()
        new_date = datetime.datetime.now() - datetime.timedelta(days=1)
        check.db.update_date(order_id, new_date)
        # check.db.update_date_consume_by_invoice(invoice_id, new_date)
        # Обновляем дату заявки
        query = 'update bo.t_consume set dt = :date where invoice_id = :invoice_id'
        query_params = {'date': new_date, 'invoice_id': invoice_id}
        balance_db.balance().execute(query, query_params)

        ado_data = {
            'service_order_id': service_order_id,
            'billing_realized': int(decimal.Decimal(shipment_qty)) + 10,
            'volume_accepted':  int(decimal.Decimal(consume_qty)),
            'date_begin':       datetime.datetime.now().strftime('%d.%m.%Y'),
            'date_end':         new_date.strftime('%d.%m.%Y')
        }

    cmp_data = shared_steps.SharedBlocks.run_oba2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == ado_data['service_order_id']]
    reporter.log("Result = %s" % cmp_data)

    butils.check_that((ado_data['service_order_id'], 4), is_in(result))
    

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBA2)
def test_exclude_empty_order_from_billing(shared_data):
    """
    Начальные условия:
        -заказ отсутствует в ADO.
        -заказ присутствует в биллинге, сумма зачисленного на дату сверки = 0 ед, сумма открученного на дату сверки = 0 ед.
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['service_order_id']) as before:
        before.validate()

        service_id, product_id, paysys_id, client_id = data_for_test_exclude()
        order_map = check_steps.create_order_map({1: {'paysys_id': paysys_id,
                                                      'service_id': service_id,
                                                      'product_id': product_id}},
                                                 client_id)
        new_date = datetime.datetime.now() - datetime.timedelta(days=1)
        service_order_id = order_map[1]['service_order_id']
        check.db.update_date(order_map[1]['id'], new_date)
        
    cmp_data = shared_steps.SharedBlocks.run_oba2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == service_order_id]
    reporter.log("Result = %s" % cmp_data)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBA2)
def test_exclude_empty_order_from_ado(shared_data):
    """
    Начальные условия:
        -заказ присутствует в ADO, сумма зачисленного на дату сверки = 0 ед, сумма открученного на дату сверки = 0 ед.
        -заказ отсутствует в биллинге.
    Ожидаемый результат:
        заказ отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['ado_data']) as before:
        before.validate()

        new_date = datetime.datetime.now() - datetime.timedelta(days=1)
        ado_data = {
            'service_order_id': 3,
            'billing_realized': 0,
            'volume_accepted': 0,
            'date_begin': datetime.datetime.now().strftime('%d.%m.%Y'),
            'date_end': new_date.strftime('%d.%m.%Y')}

    cmp_data = shared_steps.SharedBlocks.run_oba2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['service_order_id'], row['state'])
              for row in cmp_data if row['service_order_id'] == ado_data['service_order_id']]
    reporter.log("Result = %s" % cmp_data)

    butils.check_that(result, empty())

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBA2)
def test_check_diffs_count(shared_data):
    diffs_count = 4
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_oba2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    butils.check_that(cmp_data, has_length(diffs_count))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_OBA2)
def test_check_autoanalyzer_comments(shared_data):
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_oba2(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    cmp_id = cmp_data[0]['cmp_id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
    rationale = u'Выясняем причину расхождения на стороне сервиса'

    comments = list(ticket.comments.get_all(expand='attachments'))

    for comment in comments:
        if rationale in comment.text:
            attachment_name = comment.attachments[0].name

            butils.check_that(
                attachment_name, contains_string(str(u'.xls'))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


"""
Следующие тесты не будут перенесены в Shared:
test_CHECK_2268_parsing_errorstest_CHECK_2268_bad_file
test_CHECK_2268_bad_file
test_CHECK_2268_file_not_found

"""
