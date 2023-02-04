# coding: utf-8
__author__ = 'chihiro'

from datetime import datetime, timedelta
from decimal import Decimal
import json

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string
from tenacity import retry, stop_after_attempt, wait_random

from balance import balance_steps as steps
from btestlib.constants import Products, PersonTypes, Paysyses, Firms
from btestlib import utils
from check import steps as check_steps
import balance.balance_db as db
from check import shared_steps
from check.shared import CheckSharedBefore
from check.defaults import LAST_DAY_OF_PREVIOUS_MONTH
from check.db import get_completion_fixed_qty_by_order_id
from check import utils as check_utils

NEW_OPERATION_TEXT = u"""Расхождение вызвано недавними операциями зачисления/снятия средств в Биллинге. Оповещения еще не успели дойти до БК. Расхождений нет"""
DIFFS_COUNT = 6


def prepare_bk_data(order_data, money_qty=0, consume_type='pcs'):
    default_date = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    return [
        str(order_data['service_order_id']),
        str(order_data['service_id']),
        str(order_data['total_sum']),
        str(money_qty), str(consume_type), default_date, default_date
    ]


# Обернул в retry, чтобы уйти от ошибки типа "Response: Object <Order '7-45921704'> is locked"
@retry(stop=stop_after_attempt(2), wait=wait_random(min=0, max=1), reraise=True)
def export_with_retry(func):
    return func()


def do_modify(client_id, service_order_id, service_id):
    export_with_retry(lambda : steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY'))

    db.balance().execute(
        """
        update t_client_service_data
        set migrate_to_currency = to_date( :date), update_dt = to_date( :date)
        where class_id = :client_id
        """,
        {'client_id': client_id, 'date': LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=10)})
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id,
                                      {'Bucks': 30, 'Money': 800}, 0,
                                      LAST_DAY_OF_PREVIOUS_MONTH + timedelta(minutes=15))
    return service_order_id


def create_order(client_id=None, person_id=None, on_dt=None):
    if client_id is None:
        client_id = check_steps.create_client()
    if person_id is None:
        person_id = check_steps.create_person(
            client_id, person_category=PersonTypes.UR.code
        )

    additional_params = {}
    if on_dt is not None:
        additional_params['on_dt'] = on_dt
    order_map = check_steps.create_act_map({1: {'paysys_id': Paysyses.BANK_UR_RUB.id,
                                                'service_id': Products.DIRECT_FISH.service.id,
                                                'product_id': Products.DIRECT_FISH.id,
                                                'shipment_info': {'Bucks': 30}}
                                            }, client_id, person_id, act_needed=True,
                                           **additional_params)
    return {
        'service_order_id': order_map['invoice']['orders'][1]['service_order_id'],
        'service_id': order_map['invoice']['orders'][1]['service_id'],
        'total_sum': order_map['invoice']['total_sum'],
        'client_id': client_id,
        'person_id': person_id,
        'id': order_map['invoice']['orders'][1]['id']
    }


def create_avtooverdraft(client_id=None, person_id=None):
    if client_id is None:
        client_id = check_steps.create_client()
    if person_id is None:
        person_id = check_steps.create_person(
            client_id, person_category=PersonTypes.UR.code)

    # Переходим на мультивалютность миграцией
    export_with_retry(lambda: steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                           dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2)))

    def _create_order(client_id, service_id, product_id):
        service_order_id = steps.OrderSteps.next_id(service_id)
        order_id = steps.OrderSteps.create(
            client_id=client_id, service_order_id=service_order_id,
            product_id=product_id,
            service_id=service_id
        )
        return service_order_id, order_id

    order_data = {}
    order_data['service_order_id'], order_data['id'] = _create_order(
        client_id, Products.DIRECT_FISH.service.id, Products.DIRECT_FISH.id
    )
    order_data_child = {}
    order_data_child['service_order_id'], order_data_child['id'] = _create_order(
        client_id, Products.DIRECT_FISH.service.id, Products.DIRECT_FISH.id
    )
    orders_list = [
        {'ServiceID': Products.DIRECT_FISH.service.id, 'ServiceOrderID': order_data_child['service_order_id'],
         'Qty': Decimal('10'), 'BeginDT': LAST_DAY_OF_PREVIOUS_MONTH}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=LAST_DAY_OF_PREVIOUS_MONTH))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=LAST_DAY_OF_PREVIOUS_MONTH)
    steps.CampaignsSteps.do_campaigns(
        Products.DIRECT_FISH.service.id, order_data_child['service_order_id'],
        {'Bucks': Decimal('50.123')}, 0, LAST_DAY_OF_PREVIOUS_MONTH
    )
    steps.OrderSteps.make_optimized_force(order_data['id'])
    steps.OrderSteps.merge(
        order_data['id'], sub_orders_ids=[order_data_child['id']], group_without_transfer=1)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

    # Подключаем овердрафт
    limit = 90
    steps.OverdraftSteps.set_force_overdraft(
        client_id, Products.DIRECT_FISH.service.id, limit, Firms.YANDEX_1.id, currency='RUB')
    # Подключаем автоовердрафт
    steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=limit)

    autooverdraft_id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                                            {'item': client_id})[0]['id']
    steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', autooverdraft_id, with_enqueue=True)

    steps.ActsSteps.generate(client_id, force=1, date=LAST_DAY_OF_PREVIOUS_MONTH)

    order_data['total_sum'] = 0
    order_data['service_id'] = Products.DIRECT_FISH.service.id
    return order_data


def create_order_with_boid():
    client_id = check_steps.create_client()
    person_id = check_steps.create_person(client_id, person_category=PersonTypes.UR.code)

    service_id = Products.DIRECT_FISH.service.id
    direct_product_id = Products.DIRECT_FISH.id
    direct_media_product_id = Products.MEDIA_DIRECT_RUB.id

    payment_date = LAST_DAY_OF_PREVIOUS_MONTH
    consume_qty = completion_qty = Decimal('10')

    def _create_order(product_id):
        service_order_id = steps.OrderSteps.next_id(service_id)
        order_id = steps.OrderSteps.create(
            client_id=client_id, service_order_id=service_order_id,
            product_id=product_id,
            service_id=service_id
        )
        return service_order_id, order_id

    def _create_paid_order(product_id, bucks=None, money=None):
        service_order_id, order_id = _create_order(product_id)

        orders_list = [{
            'ServiceID': service_id,
            'ServiceOrderID': service_order_id,
            'Qty': consume_qty,
            'BeginDT': payment_date,
        }]

        request_id = steps.RequestSteps.create(
            client_id=client_id, orders_list=orders_list,
            additional_params=dict(InvoiceDesireDT=payment_date)
        )
        invoice_id, _, total_sum = steps.InvoiceSteps.create(
            request_id=request_id, person_id=person_id,
            paysys_id=Paysyses.BANK_UR_RUB.id, credit=0,
            contract_id=None, overdraft=0, endbuyer_id=None
        )
        steps.InvoiceSteps.pay(invoice_id, payment_dt=payment_date)

        campaigns_params = dict()
        if bucks:
            campaigns_params['Bucks'] = bucks
        if money:
            campaigns_params['Money'] = money

        steps.CampaignsSteps.do_campaigns(
            service_id, service_order_id,
            campaigns_params, 0, payment_date
        )

        return service_order_id, order_id, consume_qty

    parent_service_order_id, parent_order_id = _create_order(direct_product_id)
    child_order = _create_paid_order(direct_product_id, bucks=completion_qty)
    boid_order = _create_paid_order(direct_media_product_id, money=completion_qty)

    steps.OrderSteps.make_optimized_force(parent_order_id)
    steps.OrderSteps.merge(
        parent_order_id,
        sub_orders_ids=[child_order[1], boid_order[1]],
        group_without_transfer=1
    )
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

    # Переходим на мультивалютность миграцией
    def _export_func():
        steps.ClientSteps.migrate_to_currency(
            client_id, currency_convert_type='MODIFY',
            dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2)
        )
    export_with_retry(_export_func)

    def _format_order(service_order_id, order_id, total_sum=0):
        order = {
            'id': order_id,
            'service_id': service_id,
            'service_order_id': service_order_id,
            'total_sum': total_sum,
        }
        return order

    orders = {
        'parent': _format_order(parent_service_order_id, parent_order_id),
        'child': _format_order(*child_order),
        'boid': _format_order(*boid_order),
    }
    # Заказы-боиды учитываются в зачислениях на общем счете
    orders['parent']['money_qty'] = orders['boid']['total_sum']
    return orders


def format_boid_data(orders):
    boid_data = {}
    for key, order in orders.iteritems():
        boid_data[key] = order.copy()
        if key != 'boid':
            money_qty = order.get('money_qty', 0)
            boid_data[key]['bk_data'] = prepare_bk_data(order, money_qty)
    return boid_data


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()

        order_data = create_order()
        bk_data = prepare_bk_data(order_data)

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert order_data['service_order_id'] not in [row['service_order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_not_found_in_billing(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()
        order_data = {
            'service_order_id': steps.OrderSteps.next_id(Products.DIRECT_FISH.service.id),
            'service_id': Products.DIRECT_FISH.service.id,
            'total_sum': 100
        }
        bk_data = prepare_bk_data(order_data)

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (order_data['service_order_id'], 2) in [(row['service_order_id'], row['state']) for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_not_found_in_bk(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()

        order_data = create_order()
        bk_data = None

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (order_data['service_order_id'], 1) in [(row['service_order_id'], row['state']) for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_changed_sum(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()

        order_data = create_order()
        bk_data = prepare_bk_data(order_data, money_qty=Decimal('1.5'))

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (order_data['service_order_id'], 3) in [(row['service_order_id'], row['state']) for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_modify_order_with_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()

        order_data = create_order()
        do_modify(
            order_data['client_id'],
            order_data['service_order_id'],
            order_data['service_id']
        )

        completion_fixed_qty = get_completion_fixed_qty_by_order_id(order_data['id'])
        money = (Decimal('55.7') - Decimal(completion_fixed_qty)) * Decimal("45")
        bk_data = prepare_bk_data(order_data, money_qty=money)

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (order_data['service_order_id'], 3) in [(row['service_order_id'], row['state']) for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_modify_order_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()

        order_data = create_order()
        do_modify(
            order_data['client_id'],
            order_data['service_order_id'],
            order_data['service_id']
        )

        order_data['total_sum'] = 30
        bk_data = prepare_bk_data(order_data, money_qty=771, consume_type='RUB')

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert order_data['service_order_id'] not in [row['service_order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_avtooverdraft_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()
        order_data = create_avtooverdraft()
        bk_data = prepare_bk_data(order_data, consume_type='RUB', money_qty=90)

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert order_data['service_order_id'] not in [row['service_order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_avtooverdraft_with_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()
        order_data = create_avtooverdraft()
        bk_data = prepare_bk_data(order_data, consume_type='RUB', money_qty=10)

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert (order_data['service_order_id'], 3) in [(row['service_order_id'], row['state']) for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_new_operations(shared_data):
    # подробнее в CHECK-2341
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()
        order_data = create_order()

        invoice_id, _ = check_steps.create_invoice(
            order_data['client_id'],
            order_data['person_id'],
            orders_list=[{'ServiceID': Products.DIRECT_FISH.service.id,
                          'ServiceOrderID': order_data['service_order_id'],
                          'Qty': 30,
                          'BeginDT': LAST_DAY_OF_PREVIOUS_MONTH}],
            paysys_id=Paysyses.BANK_UR_RUB.id,
            endbuyer_id=None,
            contract_id=None
        )
        steps.InvoiceSteps.pay(invoice_id, payment_dt=datetime.now())

        bk_data = prepare_bk_data(order_data)

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    assert (order_data['service_order_id'], 3) in [(row['service_order_id'], row['state']) for row in cmp_data]


    ticket = check_utils.get_check_ticket('zbb', cmp_id)

    comments = list(ticket.comments.get_all())
    for comment in comments:
        if NEW_OPERATION_TEXT in comment.text:
            utils.check_that(comment.text,
                             contains_string('{}-{}'.format(order_data['service_id'], order_data['service_order_id']))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_direct_paid_orders_not_in_bk(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()

        order_data = create_order()
        bk_data = None

        check_utils.zbb_create_data_file_for_direct(
            content=json.dumps({
                "id": None,
                "result": {"result": [str(order_data['service_order_id'])]},
                "jsonrpc": "2.0"}),
        )

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert order_data['service_order_id'] not in [row['service_order_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_check_diffs_count(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert len(cmp_data) == DIFFS_COUNT


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_order_boid_is_not_missing(shared_data):
    """
    Проверяем случай, когда в Биллинге присутствует дочерний заказ-boid,
      который отсутствует в БК. В данном случае расхождений нет.
    """
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['boid_data']) as before:
        before.validate()

        orders = create_order_with_boid()
        boid_data = format_boid_data(orders)

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

    rows = [row['service_order_id'] for row in cmp_data]
    for order in boid_data.itervalues():
        service_order_id = order['service_order_id']
        assert service_order_id not in rows


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_ZBB)
def test_zbb_auto_overdraft_without_diff_on_side_orders(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['order_data', 'bk_data']) as before:
        before.validate()

        client_id = check_steps.create_client()
        person_id = check_steps.create_person(
            client_id, person_category=PersonTypes.UR.code)

        create_avtooverdraft(client_id, person_id)

        some_old_date = LAST_DAY_OF_PREVIOUS_MONTH + relativedelta(months=-1, days=32)
        order_data = create_order(client_id, person_id, on_dt=some_old_date)
        order_data['total_sum'] = Decimal(order_data['total_sum'] / 30).quantize(Decimal('.000001'))

        bk_data = prepare_bk_data(order_data)

    cmp_data = shared_steps.SharedBlocks.run_zbb(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache and shared_data.cache.get('cmp_data') or []

    rows = set(row['service_order_id'] for row in cmp_data)
    assert order_data['service_order_id'] not in rows
