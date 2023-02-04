# -*- coding: utf-8 -*-
import datetime
import hamcrest
import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils, reporter
from dateutil.relativedelta import relativedelta
from btestlib.constants import Products, Paysyses, Services, PersonTypes
from balance.features import Features
from collections import namedtuple
from btestlib.matchers import has_entries_casted, matches_in_time

pytestmark = [reporter.feature(Features.NOTIFICATION, Features.UNIFIED_ACCOUNT),
              pytest.mark.tickets('BALANCE-30388')]

client = namedtuple('client', 'client_id, person_id, service_id, product_id, now, paysys_id, service_order_id, '
                              'service_order_id_2, order_id, order_id_2, invoice_id')

ORDER_OPCODE = 1


@pytest.mark.parametrize('scenario', [
    'from child to child',
    'from child to parent',
    'from parent to child',
    'from parent to parent'
])
def test_transfer_between_ua(scenario):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    ua_1_params = prepare_client_with_group_order(client_id, person_id)
    ua_2_params = prepare_client_with_group_order(client_id, person_id)
    ua_3_params = prepare_client_with_group_order(client_id, person_id)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': datetime.datetime.now()})

    with reporter.step(u'Уменьшаем открутку на дочернем заказе'):
        steps.CampaignsSteps.do_campaigns(ua_1_params.service_id, ua_1_params.service_order_id_2, {'Bucks': 5}, 0,
                                          ua_1_params.now)

    with reporter.step(u'Проверяем последнюю нотификацию по ОС до переноса'):
        check_notification(ua_1_params.order_id, 200)
        check_notification(ua_2_params.order_id, 200)
        tid_1 = check_notification(ua_3_params.order_id, 200)

    with reporter.step(u'Переносим средства {}'.format(scenario)):
        if scenario == 'from child to child':
            steps.OrderSteps.transfer([{'order_id': ua_1_params.order_id_2, 'qty_old': 10,
                                        'qty_new': 5, 'all_qty': 0}],
                                      [{'order_id': ua_2_params.order_id_2, 'qty_delta': 1}])
        elif scenario == 'from child to parent':
            steps.OrderSteps.transfer([{'order_id': ua_1_params.order_id_2, 'qty_old': 10,
                                        'qty_new': 5, 'all_qty': 0}],
                                      [{'order_id': ua_2_params.order_id, 'qty_delta': 1}])
        elif scenario == 'from parent to child':
            steps.OrderSteps.transfer([{'order_id': ua_1_params.order_id, 'qty_old': 190,
                                        'qty_new': 185, 'all_qty': 0}],
                                      [{'order_id': ua_2_params.order_id_2, 'qty_delta': 1}])
        elif scenario == 'from parent to parent':
            steps.OrderSteps.transfer([{'order_id': ua_1_params.order_id, 'qty_old': 190,
                                        'qty_new': 185, 'all_qty': 0}],
                                      [{'order_id': ua_2_params.order_id, 'qty_delta': 1}])

    with reporter.step(u'Проверяем последнюю нотификацию по ОС после переноса'):
        check_notification(ua_1_params.order_id, 195)
        check_notification(ua_2_params.order_id, 205)
        tid_2 = check_notification(ua_3_params.order_id, 200)
        # utils.check_that(tid_1, hamcrest.equal_to(tid_2),
        #                  step=u'Проверяем, что по не участвующему в переносе ОС не было нотификации')
        # Проверка делает тест мигающим, так как в tid_1 не всегда попадает последняя нотификация


def test_transfer_between_child_orders():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    ua_1_params = prepare_client_with_group_order(client_id, person_id)

    with reporter.step(u'Создаем 2-й дочерний заказ'):
        service_order_id_3 = steps.OrderSteps.next_id(service_id=ua_1_params.service_id)
        order_id_3 = steps.OrderSteps.create(client_id, service_order_id_3, service_id=ua_1_params.service_id,
                                             product_id=ua_1_params.product_id,
                                             params={'AgencyID': None,
                                                     'GroupServiceOrderID': ua_1_params.service_order_id})
        orders_list = [{'ServiceID': ua_1_params.service_id, 'ServiceOrderID': service_order_id_3, 'Qty': 100,
                        'BeginDT': ua_1_params.now}]
        request_id = steps.RequestSteps.create(client_id, orders_list,
                                               additional_params=dict(InvoiceDesireDT=ua_1_params.now))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, ua_1_params.paysys_id, credit=0,
                                                     contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)
        steps.CampaignsSteps.do_campaigns(ua_1_params.service_id, service_order_id_3, {'Bucks': 10}, 0,
                                          ua_1_params.now - relativedelta(days=1))

    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': datetime.datetime.now()})

    with reporter.step(u'Уменьшаем открутку на дочернем заказе'):
        steps.CampaignsSteps.do_campaigns(ua_1_params.service_id, ua_1_params.service_order_id_2, {'Bucks': 5}, 0,
                                          ua_1_params.now)

    with reporter.step(u'Проверяем последнюю нотификацию по ОС до переноса'):
        tid_1 = check_notification(ua_1_params.order_id, 300)

    with reporter.step(u'Переносим средства между дочерними заказами'):
        steps.OrderSteps.transfer([{'order_id': ua_1_params.order_id_2, 'qty_old': 10, 'qty_new': 5, 'all_qty': 0}],
                                  [{'order_id': order_id_3, 'qty_delta': 1}])

    with reporter.step(u'Проверяем последнюю нотификацию по ОС после переноса'):
        tid_2 = check_notification(ua_1_params.order_id, 300)
        # utils.check_that(tid_1, hamcrest.equal_to(tid_2), step=u'Проверяем, что по ОС не было нотификации')
        # Проверка делает тест мигающим, так как в tid_1 не всегда попадает последняя нотификация


def test_reshipment_after_nds_change():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    ua_1_params = prepare_client_with_group_order(client_id, person_id)

    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': datetime.datetime.now()})

    with reporter.step(u'Уменьшаем открутку на дочернем заказе'):
        steps.CampaignsSteps.do_campaigns(ua_1_params.service_id, ua_1_params.service_order_id_2, {'Bucks': 5}, 0,
                                          ua_1_params.now - relativedelta(days=1))

    with reporter.step(u'Проверяем последнюю нотификацию по ОС до переноса'):
        tid_1 = check_notification(ua_1_params.order_id, 200)

    with reporter.step(u'Меняем НДС перед перезачислением откруток'):
        query = 'update T_CONSUME set tax_policy_pct_id = 1 where parent_order_id =:item'
        db.balance().execute(query, {'item': ua_1_params.order_id_2})

    with reporter.step(u'Перезачисляем открутки'):
        steps.CampaignsSteps.do_campaigns(ua_1_params.service_id, ua_1_params.service_order_id_2, {'Bucks': 7}, 0,
                                          ua_1_params.now - relativedelta(days=1))

    with reporter.step(u'Проверяем последнюю нотификацию по ОС после переноса'):
        tid_2 = check_notification(ua_1_params.order_id, 200)
        # utils.check_that(tid_1, hamcrest.equal_to(tid_2), step=u'Проверяем, что по ОС не было нотификации')
        # Проверка делает тест мигающим, так как в tid_1 не всегда попадает последняя нотификация


def prepare_client_with_group_order(client_id=None, person_id=None):
    client_id = client_id or steps.ClientSteps.create()
    person_id = person_id or steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id
    now = datetime.datetime.now()

    with reporter.step(u'Создаем заказы с единым счетом'):
        service_order_id = steps.OrderSteps.next_id(service_id=service_id)
        service_order_id_2 = steps.OrderSteps.next_id(service_id=service_id)
        order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                           product_id=product_id, params={'AgencyID': None})
        order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, service_id=service_id,
                                             product_id=product_id,
                                             params={'AgencyID': None, 'GroupServiceOrderID': service_order_id})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': now},
                   {'ServiceID': service_id, 'ServiceOrderID': service_order_id_2, 'Qty': 100, 'BeginDT': now}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 10}, 0, now - relativedelta(days=1))
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': 10}, 0, now - relativedelta(days=1))

    return client(client_id=client_id, person_id=person_id, service_id=service_id, product_id=product_id, now=now,
                  paysys_id=paysys_id, service_order_id=service_order_id, service_order_id_2=service_order_id_2,
                  order_id=order_id, order_id_2=order_id_2, invoice_id=invoice_id)


def check_notification(order_id, expected_value):
    utils.check_that(
        steps.CommonSteps.build_notification(1, object_id=order_id)['args'][0].get('TotalConsumeQty'),
        hamcrest.equal_to(str(expected_value)))

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(ORDER_OPCODE, order_id),
                     matches_in_time(has_entries_casted({'TotalConsumeQty': str(expected_value)}), timeout=300))

    return steps.CommonSteps.get_last_notification(ORDER_OPCODE, order_id)['Tid']
