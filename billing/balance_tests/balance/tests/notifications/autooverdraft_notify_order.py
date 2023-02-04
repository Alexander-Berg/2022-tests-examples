# -*- coding: utf-8 -*-
import datetime
import hamcrest
import pytest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils, reporter
from dateutil.relativedelta import relativedelta
from btestlib.constants import Products, Paysyses, Services, Firms, PersonTypes
from balance.features import Features

pytestmark = [reporter.feature(Features.OVERDRAFT, Features.NOTIFICATION),
              pytest.mark.tickets('BALANCE-29471')]

ORDER_OPCODE = 1
CLIENT_OPCODE = 10

DT = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
LIMIT = 90
QTY = 600
COMPLETION_QTYS = [10.5, 21.75]

SERVICE_ID = Services.DIRECT.id
PRODUCT_ID = Products.DIRECT_FISH.id
PAYSYS_ID = Paysyses.BANK_UR_RUB.id


@pytest.mark.parametrize('is_auto_ov, is_main_order',
                         [
                             (True, False),
                             (False, True),
                         ],
                         ids=[
                             'order not main',
                             'not auto_ov client',
                         ]
                         )
def ex_test_notification_without_overdraft_info(is_auto_ov, is_main_order):
    client_id, person_id = get_client_person()

    if is_main_order:
        with reporter.step(u'Создаем ОС и несколько заказов'):
            main_order_id = get_main_order(client_id)
            ov_orders = get_ov_orders(client_id, person_id, main_order_id)
    else:
        with reporter.step(u'Создаем несколько заказов'):
            main_order_id = None
            ov_orders = get_ov_orders(client_id, person_id)

    if is_auto_ov:
        with reporter.step(u'Подключаем автоовердрафт, перекручиваем заказы и создаем счет'):
            set_auto_overdraft(client_id, person_id)
            do_campaigns(ov_orders)
            make_auto_overdraft_invoice(client_id)
    else:
        with reporter.step(u'Подключаем овердрафт'):
            steps.OverdraftSteps.set_force_overdraft(client_id, SERVICE_ID, LIMIT, Firms.YANDEX_1.id, currency='RUB')

    if is_main_order:
        ov_limit, ov_spent_qty = get_notification_params(main_order_id)
        utils.check_that(ov_limit, hamcrest.is_(None),
                         step='Проверяем поле OverdraftLimit в нотификации по основному заказу')
        utils.check_that(ov_spent_qty, hamcrest.is_(None),
                         step='Проверяем поле OverdraftSpentQty в нотификации по основному заказу')

    for _i, order_id in ov_orders:
        ov_limit, ov_spent_qty = get_notification_params(order_id)
        utils.check_that(ov_limit, hamcrest.is_(None),
                         step='Проверяем поле OverdraftLimit в нотификации по заказу')
        utils.check_that(ov_spent_qty, hamcrest.is_(None),
                         step='Проверяем поле OverdraftSpentQty в нотификации по заказу')


@pytest.mark.parametrize('completion_qtys, match_ov_spent_qty',
                         [
                             ([0, 0], '0.00'),
                             ([11.66, 34.78], '46.44'),
                             ([45, 45], '90.00'),
                             ([34.55, 78.98], '90.00'),
                         ],
                         ids=[
                             'order have not been shipped',
                             'order have been partially shipped',
                             'order have been completely shipped',
                             'shipment exceeded the limit',
                         ],
                         )
def ex_test_order_have_been_shipped(completion_qtys, match_ov_spent_qty):
    client_id, person_id = get_client_person()
    set_auto_overdraft(client_id, person_id)

    with reporter.step(u'Создаем ОС и несколько связанных овердрафтных заказов'):
        main_order_id = get_main_order(client_id)
        ov_orders = get_ov_orders(client_id, person_id, main_order_id)

    if any(filter(bool, completion_qtys)):
        with reporter.step(u'Перекручиваем заказы и создаем автоовердрафтный счет'):
            do_campaigns(ov_orders, completion_qtys)
            make_auto_overdraft_invoice(client_id)

    with reporter.step(u'Проверяем нотификации по основному заказу'):
        ov_limit, ov_spent_qty = get_notification_params(main_order_id)

        utils.check_that(ov_limit, hamcrest.equal_to('%.2f' % LIMIT),
                         step='Проверяем поле OverdraftLimit в нотификации по основному заказу')
        utils.check_that(ov_spent_qty, hamcrest.equal_to(match_ov_spent_qty),
                         step='Проверяем поле OverdraftSpentQty в нотификации по основному заказу')

        client_notification = steps.CommonSteps.build_notification(CLIENT_OPCODE, client_id)['args'][0]

        utils.check_that(float(ov_limit), hamcrest.equal_to(float(client_notification['OverdraftLimit'])),
                         step='Проверяем совпадение значения поля OverdraftLimit в нотификации по основному заказу и'
                              ' OverdraftLimit в нотификации по клиенту')
        utils.check_that(float(ov_spent_qty), hamcrest.equal_to(float(client_notification['OverdraftSpent'])),
                         step='Проверяем совпадение значения поля OverdraftSpentQty в нотификации по основному заказу и'
                              ' OverdraftSpent в нотификации по клиенту')

    with reporter.step(u'Проверяем нотификации по связанным заказам'):
        for _i, order_id in ov_orders:
            ov_limit, ov_spent_qty = get_notification_params(order_id)
            utils.check_that(ov_limit, hamcrest.is_(None),
                             step='Проверяем поле OverdraftLimit в нотификации по связанному заказу')
            utils.check_that(ov_spent_qty, hamcrest.is_(None),
                             step='Проверяем поле OverdraftSpentQty в нотификации по связанному заказу')


def test_few_overdraft_limit():
    client_id, person_id = get_client_person()

    with reporter.step(u'Подключаем овердрафты для разных фирм'):
        for firm_id, ov_limit in [
            (Firms.YANDEX_1.id, 90),
            (Firms.REKLAMA_BEL_27.id, 70),
            (Firms.KINOPOISK_9.id, 60),
            # не входит в bo.t_config where item='SERVICE_TO_FIRM_TO_OVERDRAFT_PARAMS' для service_id=7
        ]:
            steps.OverdraftSteps.set_force_overdraft(client_id, SERVICE_ID, ov_limit, firm_id, currency='RUB')
    with reporter.step(u'Подключаем автоовердрафт'):
        steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=LIMIT)

    with reporter.step(u'Создаем ОС, автоовердрафтные заказы, перекручиваем и создаем счет'):
        main_order_id = get_main_order(client_id)
        ov_orders = get_ov_orders(client_id, person_id, main_order_id)
        do_campaigns(ov_orders)
        make_auto_overdraft_invoice(client_id)

    with reporter.step(u'Проверяем нотификации по основному заказу'):
        ov_limit, ov_spent_qty = get_notification_params(main_order_id)

        utils.check_that(ov_limit, hamcrest.equal_to('70.00'),
                         step='Проверяем поле OverdraftLimit в нотификации по основному заказу')
        utils.check_that(ov_spent_qty, hamcrest.equal_to(str(sum(COMPLETION_QTYS))),
                         step='Проверяем поле OverdraftSpentQty в нотификации по основному заказу')

        client_notification = steps.CommonSteps.build_notification(CLIENT_OPCODE, client_id)['args'][0]

        utils.check_that(float(ov_limit), hamcrest.equal_to(float(client_notification['OverdraftLimit'])),
                         step='Проверяем совпадение значения поля OverdraftLimit в нотификации по основному заказу и'
                              ' OverdraftLimit в нотификации по клиенту')
        utils.check_that(float(ov_spent_qty), hamcrest.equal_to(float(client_notification['OverdraftSpent'])),
                         step='Проверяем совпадение значения поля OverdraftSpentQty в нотификации по основному заказу и'
                              ' OverdraftSpent в нотификации по клиенту')

    with reporter.step(u'Проверяем нотификации по связанным заказам'):
        for _i, order_id in ov_orders:
            ov_limit, ov_spent_qty = get_notification_params(order_id)
            utils.check_that(ov_limit, hamcrest.is_(None),
                             step='Проверяем поле OverdraftLimit в нотификации по связанному заказу')
            utils.check_that(ov_spent_qty, hamcrest.is_(None),
                             step='Проверяем поле OverdraftSpentQty в нотификации по связанному заказу')


# ----------------------------------------------------------------------------------------------------------------------
def get_client_person():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    with reporter.step(u'Переходим на мультивалютность миграцией'):
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY', dt=DT - relativedelta(days=2))
    return client_id, person_id


def get_main_order(client_id):
    with reporter.step(u'Создаем онсновной заказ'):
        main_service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        main_order_id = steps.OrderSteps.create(client_id, main_service_order_id, service_id=SERVICE_ID,
                                                product_id=PRODUCT_ID, params={'AgencyID': None})
        return main_order_id


def set_auto_overdraft(client_id, person_id):
    with reporter.step(u'Подключаем овердрафт'):
        steps.OverdraftSteps.set_force_overdraft(client_id, SERVICE_ID, LIMIT, Firms.YANDEX_1.id, currency='RUB')
    with reporter.step(u'Подключаем автоовердрафт'):
        steps.OverdraftSteps.set_overdraft_params(person_id=person_id, client_limit=LIMIT)


def get_ov_orders(client_id, person_id, main_order_id=None):
    orders = []
    for ind in xrange(len(COMPLETION_QTYS)):
        with reporter.step(u'Создаем заказ #%d' % (ind + 1)):
            service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
            order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID,
                                               product_id=PRODUCT_ID, params={'AgencyID': None})

            if main_order_id:
                steps.OrderSteps.merge(main_order_id, [order_id])

            orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY}]
            request_id = steps.RequestSteps.create(client_id, orders_list,
                                                   additional_params=dict(InvoiceDesireDT=DT))
            invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID,
                                                         credit=0,
                                                         contract_id=None, overdraft=0,
                                                         endbuyer_id=None)
            steps.InvoiceSteps.pay(invoice_id)
            orders.append((service_order_id, order_id))
    return orders


def do_campaigns(orders, completion_qtys=None):
    completion_qtys = completion_qtys or COMPLETION_QTYS
    for (service_order_id, _order_id), completion_qty in zip(orders, completion_qtys):
        with reporter.step(u'Откручиваем заказ %s' % service_order_id):
            steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Money': completion_qty}, 0, DT)


def make_auto_overdraft_invoice(client_id):
    with reporter.step(u'Выставляем автоовердрафтный счет'):
        autoverdraft_id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                                               {'item': client_id})[0]['id']
        steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', autoverdraft_id, with_enqueue=True)


def get_notification_params(main_order_id):
    notification = steps.CommonSteps.build_notification(ORDER_OPCODE, main_order_id)
    notification_args = notification['args'][0]
    return notification_args.get('OverdraftLimit', None), notification_args.get('OverdraftSpentQty', None)
