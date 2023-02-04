# coding: utf-8
__author__ = 'chihiro'

import datetime
from decimal import Decimal

import pytest
from hamcrest import contains_string, not_
from dateutil.relativedelta import relativedelta

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as b_utils
from btestlib.data import defaults
from check import steps as check_steps
from check import shared_steps
from check import utils
from btestlib import constants
from btestlib.constants import Firms
from check.defaults import MEANINGLESS_DEFAULTS, Services, Products
from check.utils import relative_date, LAST_DAY_OF_MONTH
from check.shared import CheckSharedBefore
from check.db import update_adfox_completions

CHECK_CODE_NAME = 'bua'
END_OF_MONTH = relative_date(months=-1, day=LAST_DAY_OF_MONTH)
# Нужна любая дата до 2008 года
OLD_DATE = datetime.datetime.now().replace(day=17, year=2007)
_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    b_utils.Date.previous_three_months_start_end_dates()
DIFFS_COUNT = 18


def setup_module(module):
    # Исключаем 99 сервис для test_bua_exclude_service_from_config
    query = 'select exclude from t_bua_service where id = {}'. \
        format(Services.autoru)
    result = api.test_balance().ExecuteSQL('cmp', query)
    excluded = result and result[0] or None

    if not excluded:
        if excluded is None:
            query = """
              insert into t_bua_service (id, is_partner, exclude, update_dt) 
              values ({}, 0, 1, sysdate)
            """.format(Services.autoru)
        else:
            query = 'update t_bua_service set exclude = 1 where id = {}'. \
                format(Services.autoru)
        api.test_balance().ExecuteSQL('cmp', query)

    # Сейчас BUA запускается с отслеживанием, начал ли процесс работу или
    #   висит на параллелях. Легально это не протестировать, так как это
    #   требует некоторого вмешательства в код или нагружение тестовой базы
    #   неким тяжёлым запросом.
    # Но для проверки того, что данный механизм не влияет на работу сверки
    #   мы не отключаем его, а запускаем сверку вместе с ним.
    # Единственное, в этот механизм входит проверка на то, что BUA
    #   не подключилась к RW инстансу БД Баланса. Тестовая среда является RW
    #   средой, поэтому требуется отключать данную проверку.
    query = """
        update t_config 
        set value_num = 0 
        where item = 'watchdog_should_assert_rw'
    """.strip()
    api.test_balance().ExecuteSQL('cmp', query)


create_acted_orders_ = lambda orders_map_, client_id, person_id, on_dt=END_OF_MONTH, contract_id=None: check_steps.create_acted_orders(
    orders_map_,
    client_id,
    person_id,
    on_dt,
    contract_id
)


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_old_invoices_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # Создаем заказ с заявкой по старому счету
        orders_map = create_acted_orders_({1: {}}, client_id, person_id, on_dt=OLD_DATE)
        # Полностью его откручиваем, но не актим
        orders_map[1]['shipment_info']['Bucks'] = orders_map[1]['consume_qty']
        check_steps.do_campaign(orders_map[1])
        # Так как все заявки на заказе по старым счетам, заказ не должен попасть
        # в список расхождений

        consume_id = db.balance().execute(
            'select id from t_consume where PARENT_ORDER_ID = :order_id',
            {'order_id': orders_map[1]['id']}, fail_empty=False
        )[0]['id']
        api.test_balance().ExecuteSQL(
            'cmp', 'insert into T_BUA_OLD_CONSUMES_CORRECTIONS values (:order_id, :consume_id, :act_qty)',
            {'order_id': orders_map[1]['id'], 'consume_id': consume_id, 'act_qty': Decimal('55.7')})

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert orders_map[1]['id'] not in [row['order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_old_invoices_with_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # Создаем заказ с заявкой по старому счету
        orders_map = create_acted_orders_({1: {}}, client_id, person_id, on_dt=OLD_DATE)
        # Создаем заявку по обычному счету
        invoice_id, _ = check_steps.create_invoice(
            orders_map[1]['client_id'], orders_map[1]['person_id'],
            orders_list=[{'ServiceID': orders_map[1]['service_id'],
                          'ServiceOrderID': orders_map[1]['service_order_id'],
                          'Qty': MEANINGLESS_DEFAULTS['qty']}],
            paysys_id=1001
        )
        steps.InvoiceSteps.pay(invoice_id)
        orders_map[1]['consume_qty'] += MEANINGLESS_DEFAULTS['qty']

        # Полностью его откручиваем
        orders_map[1]['shipment_info']['Bucks'] = orders_map[1]['consume_qty']
        check_steps.do_campaign(orders_map[1])

        # Актим только новый счет
        api.test_balance().OldAct(invoice_id, END_OF_MONTH)

        # Должны найти расхождение, потому что недоакченное по старому счету
        # должно присутствовать в табоице T_BUA_OLD_CONSUMES_CORRECTIONS

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (orders_map[1]['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_more_compare_order(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # Заказ, у которого на последний день предыдущего месяца сумма
        # откруток больше, чем сумма актов.

        # Создаем заказ с заакченной откруткой на последний день предыдущего месяца
        # сумма актов = сумме откруток
        orders_map = create_acted_orders_({1: {'shipment_info': {'Bucks': 30},
                                               'consume_qty': 50}}, client_id, person_id)
        # создаем перекрутку на заказе и после этого не актим
        # сумма актов < сумма откруток
        orders_map[1]['shipment_info']['Bucks'] += 15
        check_steps.do_campaign(orders_map[1])

        # заказ должен попасть в список расхождений

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (orders_map[1]['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_more_acted_order(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # Заказ, у которого на последний день предыдущего
        # месяца сумма актов больше сумма откруток;
        # имеется запись в t_acted_completion

        # Создаем заказ с заакченной откруткой на последний день предыдущего месяца
        # сумма актов = сумме откруток
        orders_map = create_acted_orders_(
            {1: {'shipment_info': {'Bucks': 30},
                 'consume_qty': 50}},
            client_id, person_id
        )

        # делаем откат по заказу
        # сумма актов > суммы откруток
        orders_map[1]['shipment_info']['Bucks'] -= 15
        check_steps.do_campaign(orders_map[1])

        # заказ не попадает в список расхождений,
        # так как имеется запись в t_acted_completion

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert orders_map[1]['id'] not in [row['order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_more_acted_order_with_shipment(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # Заказ, у которого на последний день предыдущего
        # месяца сумма актов больше сумма откруток;
        # имеется запись в t_acted_completion

        # Создаем заказ с заакченной откруткой на последний день предыдущего месяца
        # сумма актов = сумме откруток
        orders_map = create_acted_orders_(
            {1: {'shipment_info': {'Bucks': 30},
                 'consume_qty': 50}},
            client_id, person_id
        )

        # делаем откат по заказу
        # сумма актов > суммы откруток
        orders_map[1]['shipment_info']['Bucks'] -= 15
        check_steps.do_campaign(orders_map[1])

        # присылаем открутку по заказу
        # сумма откруток < суммы актов
        # дата открутки > даты сверки
        orders_map[1]['shipment_info']['Bucks'] += 10

        check_steps.do_campaign(orders_map[1], on_dt=END_OF_MONTH + datetime.timedelta(days=1))

        # заказ не должен попадть в список расхождений,
        # так как имеется запись в t_acted_completion

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert orders_map[1]['id'] not in [row['order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_more_acted_order_with_equal_shipment(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # Заказ, у которого на последний день предыдущего
        # месяца сумма актов больше сумма откруток;
        # имеется запись в t_acted_completion

        # Создаем заказ с заакченной откруткой на последний день предыдущего месяца
        # сумма актов = сумме откруток
        orders_map = create_acted_orders_(
            {1: {'shipment_info': {'Bucks': 30},
                 'consume_qty': 50}},
            client_id, person_id
        )

        # делаем откат по заказу
        # сумма актов > суммы откруток
        orders_map[1]['shipment_info']['Bucks'] -= 15
        check_steps.do_campaign(orders_map[1])

        # присылаем открутку по заказу
        # сумма откруток = сумме актов
        # дата открутки > даты сверки
        orders_map[1]['shipment_info']['Bucks'] += 15

        check_steps.do_campaign(orders_map[1], on_dt=END_OF_MONTH + datetime.timedelta(days=1))

        # заказ не должен попадть в список расхождений,
        # так как имеется запись в t_acted_completion

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert orders_map[1]['id'] not in [row['order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_more_acted_order_with_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # Заказ, у которого сумма актов больше суммы откруток;
        # запись в t_acted_completion отсутствует

        # Создаем заказ с заакченной откруткой на последний день предыдущего месяца
        # сумма актов = сумме откруток
        orders_map = create_acted_orders_(
            {1: {'shipment_info': {'Bucks': 30},
                 'consume_qty': 50}},
            client_id, person_id
        )

        # делаем откат по заказу
        # сумма актов > суммы откруток
        orders_map[1]['shipment_info']['Bucks'] -= 15
        check_steps.do_campaign(orders_map[1])

        # Удаляем запись из t_acted_completion
        query = """
                delete from bo.t_acted_completion
                where consume_id in (
                    select id
                    from bo.t_consume
                    where parent_order_id = :order_id
                  )
                  and update_dt > date '2014-01-01'
            """

        db.balance().execute(
            query, {'order_id': orders_map[1]['id']}, fail_empty=False
        )
        # Так как запись в t_acted_completion отсутствует,
        # то заказ попадает в список расхождений

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (orders_map[1]['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_old_ua_with_reverse_without_diff(shared_data):
    """
    Случай, когда по дочерним счетам есть расхождения, но в сумме по общему счету расхождений нет.
    Пример:
      - Два дочерних заказа имеют полные открутки и акты за 2000-01-31, дополнительных средств на общем счету нет
      - 2000-02-01 сервис переносит часть открутки с одного дочернего заказа на другой
      - 2000-02-01 акты генерируются на то количество откруток, что были до переноса
      - На 2000-02-29 получаем расхождение по обоим заказам, но по общему счету в сумме расхождения нет
        - child1:
          - consume_qty:    ##########
          - completion_qty: ########
          - act_qty:        ##########
        - child2:
          - consume_qty:    ##########
          - completion_qty: ############
          - act_qty:        ##########

    BUA должна автоматически пропускать такие случаи даже без авторазборов (функция DiffsFetcher._filter_result_rows).
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        # За два месяца до сверки, изначально состояние заказа
        the_initial_month = relative_date(months=-3, day=LAST_DAY_OF_MONTH)
        # За месяц до сверки, для создания актов после выполнения актов на
        the_month_before_previous = relative_date(months=-2, day=LAST_DAY_OF_MONTH)
        # Первые числа месяца сверки, для переноса откруток
        the_first_day_of_previous_month = relative_date(months=-1, day=1)

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')

        # Создаем будущие дочерние с каким-то исходным состоянием
        orders_map = create_acted_orders_({
            'child_1': {
                'consume_qty': 10,
                'shipment_info': {'Bucks': 10},
            },
            'child_2': {
                'consume_qty': 10,
                'shipment_info': {'Bucks': 10},
            },
        }, client_id, person_id, on_dt=the_initial_month)

        child_1 = orders_map['child_1']
        child_2 = orders_map['child_2']

        # создаем родительский заказ
        parent_order_id, service_order_id_parent = check_steps.create_order(
            client_id, Services.direct, Products.direct_pcs
        )
        orders_map['parent'] = {'id': parent_order_id}

        # переходим к общему счету
        steps.OrderSteps.merge(parent_order_id, [child_1['id'], child_2['id']])

        # Зачисляем дополнительные средства на общий счет
        parent_invoice_id, _ = check_steps.create_invoice(client_id, person_id, [
            {
                'ServiceID': Services.direct,
                'ServiceOrderID': service_order_id_parent,
                'Qty': 30, 'BeginDT': the_month_before_previous,
            }
        ], paysys_id=1001, invoice_dt=the_month_before_previous)
        steps.InvoiceSteps.pay(parent_invoice_id, payment_dt=the_month_before_previous)

        # Откручиваем зачисленные средства
        child_1['shipment_info']['Bucks'] += 30
        check_steps.do_campaign(child_1, on_dt=the_month_before_previous)

        # Запускаем разбор общего счета для переноса средств на открученный дочерний заказ
        api.test_balance().UATransfer(client_id, {'for_dt': the_month_before_previous})
        query = 'update t_consume set dt = :dt where invoice_id = :invoice_id'
        db.balance().execute(query, {'dt': the_month_before_previous, 'invoice_id': parent_invoice_id})

        # Выполняем перенос откруток с одного дочернего заказа на другой
        # Внимательно: выполняем перенос уже после даты, на которую будут генерироваться акты
        child_1['shipment_info']['Bucks'] -= 10
        child_2['shipment_info']['Bucks'] += 10
        check_steps.do_campaign(child_1, on_dt=the_first_day_of_previous_month)
        check_steps.do_campaign(child_2, on_dt=the_first_day_of_previous_month)

        # Создаем акты на дату перед откатом, получаем расхождение (лучше всего видно в интерфейсе)
        steps.ActsSteps.create(parent_invoice_id, act_dt=the_month_before_previous)

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data') or []

    result = [row['order_id'] for row in cmp_data]

    assert orders_map['child_1']['id'] not in result
    assert orders_map['child_2']['id'] not in result
    assert orders_map['parent']['id'] not in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_old_ua_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # Старая схема общего счета;
        # в рамках отдельно взятого дочернего заказа имеем расхождение,
        # но в рамках общего счета расхождение отсутствует

        # создаем два дочерних заказа
        # Один полностью открученный и заакченный
        # На втором пока только перекрутка
        orders_map = create_acted_orders_(
            {'child_1': {'shipment_info': {'Bucks': 50},
                         'consume_qty': 50},
             'child_2': {'shipment_info': {'Bucks': 30},
                         'consume_qty': 0}},
            client_id, person_id
        )

        # создаем родительский заказ
        parent_order_id, service_order_id_parent = check_steps.create_order(
            orders_map['child_1']['client_id'], Services.direct,
            Products.direct_pcs
        )

        # переходим к общему счету
        steps.OrderSteps.merge(
            parent_order_id,
            [orders_map['child_1']['id'], orders_map['child_2']['id']]
        )

        orders_map['child_1']['shipment_info']['Bucks'] -= 30
        check_steps.do_campaign(orders_map['child_1'])

        # Запускаем разбор общего счета, который должен перенести лишние средства
        # (образовавшиеся после отката) с первого дочернего заказа на второй
        # После этой операции общая сумма откруток по обоим заказам равна
        # общей сумме актов.
        steps.OrderSteps.ua_enqueue([orders_map['child_1']['client_id']])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child_1']['client_id'])

        # Если биллинг включил автоматическую генерацию нулевых актов, то
        # здесь сформируется такой акт и исправит act_qty на обоих заказах.
        # Если его не включили, то акт не сформируется.
        # В обоих случаях сверка не должна найти расхождений.
        steps.ActsSteps.create(orders_map['child_1']['invoice_ids'][0], act_dt=END_OF_MONTH)

        # Обновляем дату заявки, чтобы она была тогда же когда и акт
        query = 'update bo.t_consume set dt = :date where invoice_id = :invoice_id'
        query_params = {'date': END_OF_MONTH, 'invoice_id': orders_map['child_2']['invoice_ids'][0]}
        db.balance().execute(query, query_params)

        orders_map['parent'] = {'id': parent_order_id}

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [row['order_id'] for row in cmp_data]

    assert orders_map['child_1']['id'] not in result
    assert orders_map['child_2']['id'] not in result
    assert orders_map['parent']['id'] not in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_old_ua_with_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # Старая схема общего счета;
        # в рамках общего счета имеем расхождение

        # Создаем дочерние заказы
        orders_map = create_acted_orders_(
            {'child_1': {'shipment_info': {'Bucks': 30},
                         'consume_qty': 50},
             'child_2': {'shipment_info': {'Bucks': 30},
                         'consume_qty': 50}},
            client_id, person_id
        )

        # создаем родительский заказ
        parent_order_id, parent_service_order_id = check_steps.create_order(
            orders_map['child_1']['client_id'], Services.direct,
            Products.direct_pcs
        )

        # переходим к общему счету
        steps.OrderSteps.merge(
            parent_order_id,
            [orders_map['child_1']['id'], orders_map['child_2']['id']]
        )

        # Создаем несовпадающие расхождения на дочерних заказах
        # в рамках общего счета сумма откруток и актов не сходится
        orders_map['child_1']['shipment_info']['Bucks'] -= 5
        check_steps.do_campaign(orders_map['child_1'])
        orders_map['child_2']['shipment_info']['Bucks'] += 10
        check_steps.do_campaign(orders_map['child_2'])

        orders_map['parent'] = {'id': parent_order_id}

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [row['order_id'] for row in cmp_data]

    assert orders_map['child_1']['id'] not in result
    assert (orders_map['child_2']['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]
    assert orders_map['parent']['id'] not in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_new_ua_ignore_child_overshipment(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # новая схема общего счета;
        # перекрутка на дочернем - сверка должна его игнорировать.

        orders_map = create_acted_orders_(
            {'child': {'shipment_info': {'Bucks': 30},
                       'consume_qty': 50},
             'parent': {'consume_qty': 50}},
            client_id, person_id
        )

        # переходим к общему счету
        steps.OrderSteps.merge(orders_map['parent']['id'],
                               [orders_map['child']['id']])

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(orders_map['parent']['id'])

        # создаем перекрутку на дочернем
        orders_map['child']['shipment_info']['Bucks'] += 50
        check_steps.do_campaign(orders_map['child'])

        # Отражаем открутку на родительском заказе
        api.test_balance().UATransfer(orders_map['child']['client_id'],
                                      {'for_dt': END_OF_MONTH})

        steps.ActsSteps.create(orders_map['child']['invoice_ids'][0],
                               act_dt=END_OF_MONTH + datetime.timedelta(seconds=1))

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    result = [row['order_id'] for row in cmp_data]

    assert orders_map['child']['id'] not in result
    assert orders_map['parent']['id'] not in result


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_new_ua_parent_with_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # новая схема общего счета;
        # Расхождение на родительском заказе. Сверка должна его учитывать.

        orders_map = create_acted_orders_(
            {'child': {'shipment_info': {'Bucks': 50}, 'consume_qty': 50},
             'parent': {'consume_qty': 50}},
            client_id, person_id
        )

        # переходим к общему счету
        steps.OrderSteps.merge(orders_map['parent']['id'],
                               [orders_map['child']['id']])

        # делаем общий счет невыключаемым
        steps.OrderSteps.make_optimized(orders_map['parent']['id'])

        # Разбираем общий счет
        api.test_balance().UATransfer(orders_map['child']['client_id'], {'for_dt': END_OF_MONTH})
        # steps.OrderSteps.ua_enqueue([orders_map['child']['client_id']])
        # steps.CommonSteps.export('UA_TRANSFER', 'Client', orders_map['child']['client_id'])

        # создаем перекрутку на родительском
        orders_map['parent']['shipment_info']['Bucks'] += 70
        check_steps.do_campaign(orders_map['parent'])

        # Генерируем акты (чтобы все было по честному :) )
        steps.ActsSteps.create(orders_map['child']['invoice_ids'][0], act_dt=END_OF_MONTH)

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert orders_map['child']['id'] not in [row['order_id'] for row in cmp_data]
    assert (orders_map['parent']['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_reverse_auto_analyzer(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # у сконвертированных заказов после конвертации изменяется money
        # раньше учитывали только completion_qty
        # подробнее CHECK-2735

        orders_map = check_steps.create_order_map(
            {1: {'shipment_info': {'Bucks': 200},
                 'consume_qty': 50}},
            client_id)

        orders_map_2 = check_steps.create_order_map(
            {1: {'consume_qty': Decimal("300")}},
            client_id)

        # создаем счет
        orders_list = [
                {'ServiceID': orders_map[1]['service_id'], 'ServiceOrderID': orders_map[1]['service_order_id'],
                 'Qty': Decimal("300"), 'BeginDT': END_OF_MONTH}
            ]
        invoice_id, _ = check_steps.create_invoice(
            client_id, person_id, orders_list, paysys_id=1001, invoice_dt=END_OF_MONTH - datetime.timedelta(days=10))
        steps.InvoiceSteps.pay(invoice_id, payment_dt=END_OF_MONTH - datetime.timedelta(days=10))

        check_steps.do_campaign(orders_map[1], on_dt=END_OF_MONTH - datetime.timedelta(days=4))

        # переводим клиента на мультивалютность
        steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
        db.balance().execute(
            """		
            update t_client_service_data		
            set migrate_to_currency = to_date( :date), update_dt = to_date( :date)		
            where class_id = :client_id		
            """,
            {'client_id': client_id, 'date': END_OF_MONTH - datetime.timedelta(days=3)})

        # крутим в деньгах
        orders_map[1]['shipment_info']['Money'] = 3000
        check_steps.do_campaign(orders_map[1], on_dt=END_OF_MONTH - datetime.timedelta(days=2))

        # генерируем акт
        steps.ActsSteps.generate(client_id, force=1, date=END_OF_MONTH)

        # присылаем откат
        orders_map[1]['shipment_info']['Money'] -= 660
        check_steps.do_campaign(orders_map[1], on_dt=END_OF_MONTH)

        # делаем перенос отрученного
        steps.OrderSteps.transfer([{'order_id': orders_map[1]['id'], 'qty_old': 9000, 'qty_new': 8640, 'all_qty': 0}],
                                  [{'order_id': orders_map_2[1]['id'], 'qty_delta': 1}])
        db.balance().execute(
            '''update t_shipment set update_dt = to_date('{dt}','DD.MM.YY HH24:MI:SS') where service_id = :service_id and service_order_id = :service_order_id'''.format(
                dt=END_OF_MONTH.strftime('%d.%m.%y 00:00:00')),
            {'service_id': orders_map[1]['service_id'],
             'service_order_id': orders_map[1]['service_order_id']},
            descr='Изменяем дату открутки в t_shipment')

        # проставляем данное значение для попадания в расхождения
        db.balance().execute(
            '''update T_CONSUME set act_qty = 1 where PARENT_ORDER_ID = {}'''.format(orders_map[1]['id']))

    # Запускаем разбор сверки + авторазбор
    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (orders_map[1]['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]

    cmp_id = cmp_data[0]['cmp_id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
    rationale = utils.get_db_config_value('bua_reverse_rationale')

    comments = list(ticket.comments.get_all())
    for comment in comments:
        if rationale in comment.text:
            b_utils.check_that(
                comment.text,
                contains_string(str(orders_map[1]['id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_CHECK_2153(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')
        # фишечный заказ, у которого сумма окруток больше суммы актов на 0.019 копеек,
        # что не является расхождением, потому что
        # * либо акты на 0 копеек не формируются,
        # * либо формируются, и тогда это точно не расхождение

        # создаем заказ
        # сумма актов = сумме откруток
        orders_map = create_acted_orders_(
            {1: {'shipment_info': {'Money': 30},
                 'consume_qty': 50,
                 'product_id': Products.direct_money}},
            client_id, person_id
        )

        # увеличиваем количество открученного по заказу меньше, чем на 1 копейку
        orders_map[1]['shipment_info']['Money'] += Decimal('0.019')
        check_steps.do_campaign(orders_map[1])

        # по задаче CHECK-2153 - (bua) Исключить из расхождений в сверке незаакченное менее, чем на 1 коп
        # заказ не попадает в список расхождений

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert orders_map[1]['id'] not in [row['order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_air_ticket_CHECK_2221(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ur')
        finish_dt = datetime.datetime.now() + datetime.timedelta(weeks=5)
        contract_id, _ = steps.ContractSteps.create_contract('no_agency_post', {'CLIENT_ID': client_id,
                                                                                'PERSON_ID': person_id,
                                                                                'DT': END_OF_MONTH,
                                                                                'SERVICES': [114],
                                                                                'FINISH_DT': finish_dt})

        # Заказ, у которого на последний день предыдущего месяца сумма
        # откруток больше, чем сумма актов по сервису Авиабилеты.

        # Создаем заказ с заакченной откруткой на последний день предыдущего месяца
        # сумма актов = сумме откруток
        orders_map = create_acted_orders_(
            {1: {'shipment_info': {'Bucks': 30},
                 'consume_qty': 50,
                 'product_id': Products.ticket,
                 'service_id': Services.ticket,
                 'paysys_id': 1003}},
            client_id, person_id,
            contract_id=contract_id
        )

        # создаем перекрутку на заказе и после этого не актим
        # сумма актов < сумма откруток
        orders_map[1]['shipment_info']['Bucks'] += Decimal('100.1')
        check_steps.do_campaign(orders_map[1])

        # заказ попадет в список расхождений, если сервис авиабилеты будет включен в запуск

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert orders_map[1]['id'] not in [row['order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_exclude_service_from_config(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ur')

        orders_map = create_acted_orders_({1: {'paysys_id': 1201003,
                                               'service_id': Services.autoru,
                                               'product_id': Products.autoru,
                                               'shipment_info': {'Days': 11},
                                               'consume_qty': 54}}, client_id, person_id)
        # создаем перекрутку на заказе и после этого не актим
        # сумма актов < сумма откруток
        orders_map[1]['shipment_info']['Days'] += Decimal('4')
        check_steps.do_campaign(orders_map[1])

        # заказ не попадет в список расхождений, так как сервис исключили в конфиге

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert orders_map[1]['id'] not in [row['order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA_ADFOX)
def test_bua_CHECK_2534_AdFox_completions_with_diff(shared_data):
    import btestlib.reporter as reporter

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()
        # на заказах сервиса AdFox если выполняется условие - на заказе есть перекрутка менее 1000,
        # считать перекрут техническим(несущественным), закрывать данное расхождение автоматически

        client_id, person_id, contract_id = check_steps.create_contract('ur')

        # добавляем открутки
        steps.PartnerSteps.create_adfox_completion(contract_id, first_month_start_dt,
                                                   product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                                   requests=defaults.ADFox.DEFAULT_REQUESTS,
                                                   shows=defaults.ADFox.DEFAULT_SHOWS,
                                                   units=0)

        _, _, _ = check_steps.generate_act_and_get_data(contract_id, client_id, first_month_end_dt)
        orders = db.get_orders('Client', client_id)
        update_adfox_completions(orders[0]['service_order_id'], orders[0]['service_id'], order_id=orders[0]['id'],
                                 completion_qty=orders[0]['completion_qty'] + Decimal(1001))
        orders_map = {1: orders[0]}

    cmp_data = shared_steps.SharedBlocks.run_bua_adfox(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    reporter.log("CMP DATA: {}".format(cmp_data))
    reporter.log("ORDERS_MAP: {}".format(orders_map))

    assert (orders_map[1]['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_data[0]['cmp_id'])
    text_for_comment = utils.get_db_config_value('bua_adfox_significant_overshipment_rationale')
    threshold = utils.get_db_config_value('bua_adfox_overshipment_threshold')

    rationale = text_for_comment.format(threshold=threshold)

    comments = list(ticket.comments.get_all())
    for comment in comments:
        if rationale in comment.text:
            b_utils.check_that(
                comment.text, contains_string(str(orders_map[1]['id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA_ADFOX)
def test_bua_CHECK_2534_AdFox_completions_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()
        # на заказах сервиса AdFox если выполняется условие - на заказе есть перекрутка менее 1000,
        # считать перекрут техническим(несущественным), закрывать данное расхождение автоматически

        client_id, person_id, contract_id = check_steps.create_contract('ur')

        # добавляем открутки
        steps.PartnerSteps.create_adfox_completion(contract_id, first_month_start_dt,
                                                   product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                                   requests=defaults.ADFox.DEFAULT_REQUESTS,
                                                   shows=defaults.ADFox.DEFAULT_SHOWS,
                                                   units=0)

        _, _, _ = check_steps.generate_act_and_get_data(contract_id, client_id, first_month_end_dt)
        orders = db.get_orders('Client', client_id)
        update_adfox_completions(orders[1]['service_order_id'], orders[1]['service_id'], order_id=orders[1]['id'],
                                 completion_qty=orders[1]['completion_qty'] + Decimal(999))
        orders_map = {1: orders[1]}

    cmp_data = shared_steps.SharedBlocks.run_bua_adfox(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (orders_map[1]['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]
    assert len(cmp_data) == 2

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_data[0]['cmp_id'])
    text_for_comment = utils.get_db_config_value('bua_adfox_overshipment_rationale')
    threshold = utils.get_db_config_value('bua_adfox_overshipment_threshold')

    rationale = text_for_comment.format(threshold=threshold)

    comments = list(ticket.comments.get_all())
    for comment in comments:
        if rationale in comment.text:
            b_utils.check_that(
                comment.text,
                contains_string(str(orders_map[1]['id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_group_order_overshipment_auto_analyzer(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map', 'parent_order_id']) as before:
        before.validate()

        client_id = check_steps.create_client()

        # переводим клиента на мультивалютность
        steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
        db.balance().execute(
            """
            update t_client_service_data
            set migrate_to_currency = to_date( :date), update_dt = to_date( :date)		
            where class_id = :client_id
            """,
            {'client_id': client_id, 'date': END_OF_MONTH - datetime.timedelta(days=3)}
        )

        person_id = check_steps.create_person(client_id, person_category='ph')

        # Создаем два дочерних заказа и один будущий родительский
        # Один полностью открученный и заакченный
        # На втором пока только перекрутка
        # Родительский по нулям, чтобы включить неотключаемость
        orders_map = create_acted_orders_(
            {'child_1': {'shipment_info': {'Bucks': 50},
                         'consume_qty': 50},
             'child_2': {'shipment_info': {'Bucks': 30},
                         'consume_qty': 0},
             'parent': {'shipment_info': {'Bucks': 0},
                        'consume_qty': 0}},
            client_id, person_id
        )

        parent_order_id = orders_map['parent']['id']

        # переходим к общему счету
        steps.OrderSteps.merge(
            parent_order_id,
            [orders_map['child_1']['id'], orders_map['child_2']['id']]
        )

        # Помечаем общий счёт неотключаемым
        steps.OrderSteps.make_optimized(parent_order_id)

        # Создаём перекрут на общем счету
        orders_map['parent']['shipment_info']['Bucks'] = \
            orders_map['child_2']['shipment_info']['Bucks']
        check_steps.do_campaign(orders_map['parent'])

        # Обновляем дату заявки, чтобы она была тогда же когда и акт
        query = 'update bo.t_consume set dt = :date where invoice_id = :invoice_id'
        query_params = {'date': END_OF_MONTH, 'invoice_id': orders_map['child_2']['invoice_ids'][0]}
        db.balance().execute(query, query_params)

    # Запускаем разбор сверки + авторазбор
    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
    rationale = utils.get_db_config_value('bua_group_order_overshipment_rationale')

    for comment in ticket.comments.get_all():
        if rationale in comment.text:
            b_utils.check_that(comment.text, contains_string(str(parent_order_id)))
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_auto_overdraft_auto_analyzer(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        product_id = Products.direct_pcs

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, 'ur')

        service_id = Services.direct
        service_order_id = steps.OrderSteps.next_id(service_id)

        order_id = steps.OrderSteps.create(
            client_id=client_id,
            service_id=service_id,
            service_order_id=service_order_id,
            product_id=product_id,
        )
        steps.ClientSteps.migrate_to_currency(
            client_id, currency_convert_type='MODIFY',
            # TODO: move to arguments?
            dt=END_OF_MONTH - relativedelta(days=2)
        )
        steps.OrderSteps.make_optimized(order_id)
        steps.OrderSteps.ua_enqueue([client_id], for_dt=END_OF_MONTH)
        steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

        child_service_order_id = steps.OrderSteps.next_id(service_id)
        child_order_id = steps.OrderSteps.create(
            client_id=client_id,
            service_id=service_id,
            service_order_id=child_service_order_id,
            product_id=product_id,
        )

        steps.OrderSteps.merge(order_id, sub_orders_ids=[child_order_id],
                               group_without_transfer=1)

        check_steps.do_campaign({'service_id': service_id,
                                 'service_order_id': child_service_order_id,
                                 'shipment_info': {'Bucks': 2}})
        steps.OrderSteps.ua_enqueue([client_id], for_dt=END_OF_MONTH)
        steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

        overdraft_limit = 100

        # Подключаем овердрафт
        steps.OverdraftSteps.set_force_overdraft(
            client_id, service_id, overdraft_limit,
            Firms.YANDEX_1.id, currency='RUB'
        )

        invoice_order_list = [{
            'ServiceID': service_id,
            'ServiceOrderID': service_order_id,
            'Qty': 30,
            'BeginDB': END_OF_MONTH,
        }]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=invoice_order_list, additional_params=dict(InvoiceDesireDT=END_OF_MONTH))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1003,
                                                     credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

        # Подключаем автоовердрафт
        steps.OverdraftSteps.set_overdraft_params(
            person_id=person_id,
            client_limit=overdraft_limit
        )

        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=invoice_order_list, additional_params=dict(InvoiceDesireDT=END_OF_MONTH))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1003,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

        steps.InvoiceSteps.pay(invoice_id)
        query = 'update t_consume set dt = :dt where invoice_id = :invoice_id'
        api.test_balance().ExecuteSQL('balance', query, {'dt': END_OF_MONTH, 'invoice_id': invoice_id})

        orders_map = {
            'parent': {'id': order_id},
            'child': {'id': child_order_id}
        }

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
    rationale = utils.get_db_config_value('bua_auto_overdraft_rationale')

    for comment in ticket.comments.get_all():
        if rationale in comment.text:
            b_utils.check_that(
                comment.text,
                contains_string(str(orders_map['parent']['id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.parametrize('check', ['check-3146', 'old_main_order'])
@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_overshipment_without_money_old_order_auto_analyzer(shared_data, check):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')

        orders_map = create_acted_orders_(
            {'child_1': {'shipment_info': {'Bucks': 50},
                         'consume_qty': 50},
             'child_2': {'shipment_info': {'Bucks': 30},
                         'consume_qty': 0},
             'parent': {'shipment_info': {'Bucks': 0},
                        'consume_qty': 30}},
            client_id, person_id
        )

        parent_order_id = orders_map['parent']['id']

        # переходим к общему счету
        steps.OrderSteps.merge(
            parent_order_id,
            [orders_map['child_1']['id'], orders_map['child_2']['id']]
        )

        if check == 'check-3146':
            # Проставляем родительскому счёту is_ua_optimize=1 - родительский счёт по Новой схеме
            # дочерние остаются на Старой схеме child_ua_type=0
            steps.OrderSteps.make_optimized_force(orders_map['parent']['id'])

        query = 'update t_consume set current_qty = 0, current_sum = 0 where parent_order_id = :order_id'
        api.test_balance().ExecuteSQL('balance', query, {'order_id': parent_order_id})

        query = 'update t_shipment set update_dt = :day_before_act where service_id = :service_id and service_order_id = :service_order_id'
        api.test_balance().ExecuteSQL('balance', query, {'service_id': orders_map['child_2']['service_id'],
                                                         'service_order_id': orders_map['child_2']['service_order_id'],
                                                         'day_before_act': END_OF_MONTH - datetime.timedelta(days=1)})

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
    rationale = utils.get_db_config_value('bua_overshipment_without_money_on_old_main_order_rationale')

    for comment in ticket.comments.get_all():
        if rationale in comment.text:
            b_utils.check_that(
                comment.text,
                contains_string(str(orders_map['child_2']['id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_overshipment_without_money_new_order_auto_analyzer(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map', 'parent_order_id']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')

        # переводим клиента на мультивалютность
        steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
        db.balance().execute(
            """
            update t_client_service_data
            set migrate_to_currency = to_date( :date), update_dt = to_date( :date)		
            where class_id = :client_id
            """,
            {'client_id': client_id, 'date': END_OF_MONTH - datetime.timedelta(days=3)}
        )

        # person_id = check_steps.create_person(client_id, person_category='ph')

        # Создаем два дочерних заказа и один будущий родительский
        # Один полностью открученный и заакченный
        # На втором пока только перекрутка
        # Родительский не имеет откруток, чтобы включить неотключаемость
        orders_map = create_acted_orders_(
            {'child_1': {'shipment_info': {'Bucks': 50},
                         'consume_qty': 50},
             'child_2': {'shipment_info': {'Bucks': 30},
                         'consume_qty': 0},
             'parent': {'shipment_info': {'Bucks': 0},
                        'consume_qty': 0}},
            client_id, person_id
        )

        parent_order_id = orders_map['parent']['id']

        # переходим к общему счету
        steps.OrderSteps.merge(
            parent_order_id,
            [orders_map['child_1']['id'], orders_map['child_2']['id']]
        )

        # Помечаем общий счёт неотключаемым
        steps.OrderSteps.make_optimized(parent_order_id)

        # Создаём перекрут на общем счету
        orders_map['parent']['shipment_info']['Bucks'] = \
            orders_map['child_2']['shipment_info']['Bucks']
        check_steps.do_campaign(orders_map['parent'])

        # Можно и без изменения даты открутки(тогда и открутка и акт будут одним числом)
        # Но так - наверняка и более правильно - открутки до акта
        query = 'update t_shipment set update_dt = :day_before_act where service_id = :service_id and service_order_id = :service_order_id'
        api.test_balance().ExecuteSQL('balance', query, {'service_id': orders_map['child_2']['service_id'],
                                                         'service_order_id': orders_map['child_2']['service_order_id'],
                                                         'day_before_act': END_OF_MONTH - datetime.timedelta(days=1)})

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
    rationale = utils.get_db_config_value('bua_overshipment_without_money_on_new_main_order_rationale')

    for comment in ticket.comments.get_all():
        if rationale in comment.text:
            b_utils.check_that(
                comment.text,
                contains_string(str(orders_map['parent']['id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_overshipment_not_enough_money_auto_analyzer(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ph')

        orders_map = create_acted_orders_(
            {1: {'shipment_info': {'Bucks': 30},
                 'consume_qty': 30}},
            client_id, person_id
        )
        # создаем перекрутку на заказе
        orders_map[1]['shipment_info']['Bucks'] += 10
        check_steps.do_campaign(orders_map[1])

        # Оставлю здесь этот комментарий:
        # код ниже нужен для того, чтобы сделать t_consume.current_qty < cmp_data.act_qty
        # Иначе они получаются равнми, что тоже подходит под условие нужного авторазбора
        # query = 'update t_consume set current_qty = 25' \
        #         'where parent_order_id = :order_id'
        # api.test_balance().ExecuteSQL('balance', query, {'order_id': orders_map[1]['id']})

        query = 'update t_shipment set update_dt = :day_before_act ' \
                'where service_id = :service_id and service_order_id = :service_order_id'
        api.test_balance().ExecuteSQL('balance', query, {'service_id': orders_map[1]['service_id'],
                                                         'service_order_id': orders_map[1]['service_order_id'],
                                                         'day_before_act': END_OF_MONTH - datetime.timedelta(days=1)})


    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
    rationale = utils.get_db_config_value('bua_forced_overshipment_by_service')

    for comment in ticket.comments.get_all():
        if rationale in comment.text:
            b_utils.check_that(
                comment.text,
                contains_string(str(orders_map[1]['id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_check_diffs_list_and_attach(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    cmp_id = cmp_data[0]['cmp_id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
    rationale = utils.get_db_config_value('bua_diffs_report_rationale')

    comments = list(ticket.comments.get_all(expand='attachments'))

    for comment in comments:
        if rationale in comment.text:
            attachment_name = comment.attachments[0].name
            b_utils.check_that(
                attachment_name, contains_string(str(u'.xlsx'))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.parametrize('p', [
    {'qty': 10, 'rationale_key': 'bua_ofd_overshipment_rationale',
     'format_key': 'threshold', 'format_value_key': 'bua_ofd_overshipment_threshold'},
    {'qty': 100, 'rationale_key': 'bua_ofd_overshipment_approved_threshold_rationale',
     'format_func': lambda rationale_template, p: rationale_template.format(p['qty'])},
], ids=lambda p: 'overshipment-by-' + str(p['qty']))
@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_ofd_overshipment_auto_analyzers(shared_data, p):
    """
    В данном тесте проверяется два OFD авторазбора:
      - OFDOvershipmentAutoAnalyzer (overshipment-by-10)
      - OFDSignificantOvershipmentAutoAnalyzer (overshipment-by-100)

    В случае OFDSignificantOvershipmentAutoAnalyzer проверяется только один случай -
      когда сумма всех откруток по сервису OFD не превышает <bua_ofd_overshipment_approved_threshold> ед.
    Из-за того, что невозможно проверить обратный случай без ещё одного запуска сверки -
      случай с превышением вынесен в следующий тест:
      ```
      check.tests.no_shared.test_no_shared_bua.test_bua_ofd_significant_overshipment_auto_analyzers
      ```
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ur')

        product = constants.Products.OFD_YEAR

        orders_map = create_acted_orders_(
            {'order': {
                'service_id': product.service.id,
                'product_id': product.id,
                'paysys_id': 1003,
                'shipment_info': {product.type.code: 100},
                'consume_qty': 100,
            }}, client_id, person_id
        )

        orders_map['order']['shipment_info'][product.type.code] += p['qty']
        check_steps.do_campaign(orders_map['order'])
        check_steps.update_shipment_date(client_id, END_OF_MONTH)

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)

    rationale_key = p.get('rationale_key')
    if rationale_key:
        rationale = utils.get_db_config_value(rationale_key)

        format_key = p.get('format_key')
        format_value_key = p.get('format_value_key')
        if format_key and format_value_key:
            format_value = utils.get_db_config_value(format_value_key)
            rationale = rationale.format(**{format_key: format_value})

        format_func = p.get('format_func')
        if format_func and callable(format_func):
            rationale = format_func(rationale, p)
    else:
        rationale = p['rationale_text']

    for comment in ticket.comments.get_all():
        if rationale in comment.text:
            b_utils.check_that(
                comment.text,
                contains_string(str(orders_map['order']['id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_CHECK_3164_ofd_overshipment_auto_analyzers(shared_data):
    """
    В данном тесте проверяется OFD авторазбор для продукта 508474:
    Созадётся акт на сумму 100 ед
    После этого к заказу добавляем ещё 100 откруток

    В итоге получаем расхождение, котрые покрыто авторазбором
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ur')

        product = constants.Products.OFD_YEAR

        orders_map = create_acted_orders_(
            {'order': {
                'service_id': product.service.id,
                'product_id': 508474,
                'paysys_id': 1003,
                'shipment_info': {product.type.code: 100},
                'consume_qty': 100,
            }}, client_id, person_id
        )

        orders_map['order']['shipment_info'][product.type.code] += 100
        check_steps.do_campaign(orders_map['order'])
        check_steps.update_shipment_date(client_id, END_OF_MONTH)

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    assert (orders_map['order']['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
    text_for_comment = utils.get_db_config_value('bua_ofd_orders_overshipment_rationale')
    products = utils.get_db_config_value('bua_ofd_products_overshipment')

    rationale = text_for_comment.format(products)

    comments = list(ticket.comments.get_all())
    for comment in comments:
        if rationale in comment.text:
            b_utils.check_that(
                comment.text, contains_string(str(orders_map['order']['id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.parametrize('p', [
    {'qty': 100, 'product': constants.Products.VENDOR,
     'rationale_key': 'bua_market_overshipment_approved_threshold_rationale',
     'format_func': lambda rationale_template, p: rationale_template.format(p['qty'])},
    {'qty': 200, 'product': constants.Products.MARKET,
     'rationale_key': 'bua_market_overshipment_approved_threshold_rationale',
     'format_func': lambda rationale_template, p: rationale_template.format(p['qty'])},
], ids=lambda p: 'overshipment-by-' + str(p['qty']))
@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_market_vendor_overshipment_auto_analyzers(shared_data, p):
    """
    В данном тесте проверяется два Маркетных авторазбора:
      - MarketVendorsOvershipmentAutoAnalyzer (overshipment-by-100)
      - MarketOvershipmentAutoAnalyzer (overshipment-by-200)

    В этих кейсах проверяется только один случай -
      когда сумма всех откруток по сервису Market и MarketVendor не превышает <bua_..._overshipment_approved_threshold> ед.
    Из-за того, что невозможно проверить обратный случай без ещё одного запуска сверки -
      случаи с превышением вынесен в следующий тест:
      ```
      check.tests.no_shared.test_no_shared_bua.test_no_shared_bua_market_vendor_significant_overshipment_auto_analyzers
      check.tests.no_shared.test_no_shared_bua.test_no_shared_bua_market_significant_overshipment_auto_analyzers
      ```
    """

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ur')

        product = p['product']

        orders_map = create_acted_orders_(
            {'order': {
                'service_id': product.service.id,
                'product_id': product.id,
                'paysys_id': 1003,
                'shipment_info': {product.type.code: 100},
                'consume_qty': 100,
            }}, client_id, person_id
        )

        orders_map['order']['shipment_info'][product.type.code] += p['qty']
        check_steps.do_campaign(orders_map['order'])
        check_steps.update_shipment_date(client_id, END_OF_MONTH)

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)

    rationale_key = p.get('rationale_key')
    if rationale_key:
        rationale = utils.get_db_config_value(rationale_key)

        format_func = p.get('format_func')
        if format_func and callable(format_func):
            rationale = format_func(rationale, p)
    else:
        rationale = p['rationale_text']

    for comment in ticket.comments.get_all():
        if rationale in comment.text:
            b_utils.check_that(
                comment.text,
                contains_string(str(orders_map['order']['id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_geo_ticket_CHECK_3066(shared_data):
    """
            Начальные условия:
                -создаём заказ на 100 услуг
                -откруток деалем на 200,1 и актим
            Ожидаемый результат:
                заказ попадает в список с расхождений,
                Происходит авторазбор и добавляется комментарий:
                "Перекрутки на заказах Справочника. Будем разбирать в задаче   SUPGEOCONTEXT-..."
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['orders_map']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(client_id, person_category='ur')

        product = constants.Products.GEO_4

        orders_map = create_acted_orders_(
            {'order': {
                'service_id': product.service.id,
                'product_id': product.id,
                'paysys_id': 1003,
                'shipment_info': {product.type.code: 100},
                'consume_qty': 100,
            }}, client_id, person_id
        )

        # откручиваем больше, чем указано в заказе и актим
        orders_map['order']['shipment_info'][product.type.code] += Decimal('100.1')
        check_steps.do_campaign(orders_map['order'])
        check_steps.update_shipment_date(client_id, END_OF_MONTH)

        # заказ попадет в список расхождений, если сервис гео будет включен в запуск

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (orders_map['order']['id'], 1) in [(row['order_id'], row['state']) for row in cmp_data]

    cmp_id = cmp_data[0]['cmp_id']
    order_id = orders_map['order']['id']

    ticket = utils.get_check_ticket(CHECK_CODE_NAME, cmp_id)
    rationale = u'Перекрутки на заказах Справочника. Будем разбирать в задаче SUPGEOCONTEXT'

    for comment in ticket.comments.get_all():
        if str(order_id) in comment.text:
            b_utils.check_that(comment.text, contains_string(rationale))
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_BUA)
def test_bua_check_diffs_count(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_bua(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    # из всех тестов вычитаем тест на количество, тесты на другие запуски и тесты без расхождения
    assert len(cmp_data) == DIFFS_COUNT
