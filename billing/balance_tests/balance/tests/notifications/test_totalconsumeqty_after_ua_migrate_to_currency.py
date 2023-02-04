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

pytestmark = [reporter.feature(Features.NOTIFICATION, Features.UNIFIED_ACCOUNT, Features.MULTICURRENCY),
              pytest.mark.tickets('BALANCE-26652')]

client = namedtuple('client', 'client_id, person_id, service_id, product_id, now, paysys_id, service_order_id, '
                              'service_order_id_2, order_id, order_id_2, invoice_id')


def test_notification_after_migrate_by_copy():
    client_params = prepare_client_with_group_order()

    migrated_service_order_id, migrated_service_order_id_2, migrated_order_id, migrated_order_id_2 = \
        prepare_order_copies(client_params.client_id, client_params.service_id)

    migrate_to_currency_by_copy(client_params.client_id, client_params.service_order_id, migrated_service_order_id)

    after_migrate = steps.CommonSteps.build_notification(1, object_id=client_params.order_id)['args'][0].get(
        'TotalConsumeQty')
    after_migrate_new_order = steps.CommonSteps.build_notification(1, object_id=migrated_order_id)['args'][0].get(
        'TotalConsumeQty')

    utils.check_that(after_migrate, hamcrest.equal_to(None),
                     step=u'Проверяем отсутствие TotalConsumeQty на фишечном заказе после перехода на '
                          u'мультивалютность копированием')

    utils.check_that(after_migrate_new_order, hamcrest.equal_to('5400'),
                     step=u'Проверяем TotalConsumeQty на валютном заказе после перехода на '
                          u'мультивалютность копированием')


def test_notification_after_migrate_by_copy_after_new_campaigns():
    client_params = prepare_client_with_group_order()

    migrated_service_order_id, migrated_service_order_id_2, migrated_order_id, migrated_order_id_2 = \
        prepare_order_copies(client_params.client_id, client_params.service_id)

    migrate_to_currency_by_copy(client_params.client_id, client_params.service_order_id, migrated_service_order_id)

    campaign_money_orders(client_params.client_id, client_params.person_id, client_params.service_id,
                          client_params.paysys_id, migrated_service_order_id,
                          migrated_service_order_id_2, client_params.now)

    after_migrate = \
        steps.CommonSteps.build_notification(1, object_id=migrated_order_id)['args'][0].get('TotalConsumeQty')

    utils.check_that(after_migrate, hamcrest.equal_to('25400'),
                     step=u'Проверяем TotalConsumeQty на валютном заказе после перехода на '
                          u'мультивалютность копированием и зачислений на валютный заказ')


def test_notification_after_migrate_by_copy_after_new_campaigns_and_rollback():
    client_params = prepare_client_with_group_order()

    migrated_service_order_id, migrated_service_order_id_2, migrated_order_id, migrated_order_id_2 \
        = prepare_order_copies(client_params.client_id, client_params.service_id)

    migrate_to_currency_by_copy(client_params.client_id, client_params.service_order_id, migrated_service_order_id)

    campaign_money_orders(client_params.client_id, client_params.person_id, client_params.service_id,
                          client_params.paysys_id, migrated_service_order_id, migrated_service_order_id_2,
                          client_params.now)

    with reporter.step(u'Откатываем открутки по фишечным заказам'):
        steps.CampaignsSteps.do_campaigns(client_params.service_id, client_params.service_order_id, {'Bucks': 9.5}, 0,
                                          client_params.now)
        steps.CampaignsSteps.do_campaigns(client_params.service_id, client_params.service_order_id_2, {'Bucks': 7}, 0,
                                          client_params.now)

    ua_transfer_today(client_params.client_id)

    after_migrate = \
        steps.CommonSteps.build_notification(1, object_id=migrated_order_id)['args'][0].get('TotalConsumeQty')
    utils.check_that(after_migrate, hamcrest.equal_to('25505'),
                     step=u'Проверяем TotalConsumeQty на валютном заказе после перехода на '
                          u'мультивалютность копированием и откатов по фишечным заказам')


def test_notification_after_migrate_by_copy_after_new_campaigns_and_rollback_after_act():
    client_params = prepare_client_with_group_order()

    migrated_service_order_id, migrated_service_order_id_2, migrated_order_id, migrated_order_id_2 = \
        prepare_order_copies(client_params.client_id, client_params.service_id)

    migrate_to_currency_by_copy(client_params.client_id, client_params.service_order_id, migrated_service_order_id)

    campaign_money_orders(client_params.client_id, client_params.person_id, client_params.service_id,
                          client_params.paysys_id, migrated_service_order_id, migrated_service_order_id_2,
                          client_params.now)

    ua_transfer_today(client_params.client_id)

    with reporter.step(u'Выставляем акт'):
        steps.ActsSteps.generate(client_params.client_id, force=1, date=client_params.now)
    # Откатываем открутки по фишечному заказу
    steps.CampaignsSteps.do_campaigns(client_params.service_id, client_params.service_order_id_2, {'Bucks': 5}, 0,
                                      client_params.now)

    with reporter.step(u'Проверяем, что заакченное не переносится - BALANCE-23013'):
        before_transfer = \
            steps.CommonSteps.build_notification(1, object_id=migrated_order_id)['args'][0].get('TotalConsumeQty')
        utils.check_that(before_transfer, hamcrest.equal_to('25400'),
                         step=u'Проверяем TotalConsumeQty на валютном заказе после перехода на '
                              u'мультивалютность копированием и откатов по заакченным фишечным заказам')

    # Чтобы перенести откаченные деньги с заакченного, нужно счету постаить transfer_acted 1
    db.balance().execute("UPDATE T_INVOICE SET T_INVOICE.TRANSFER_ACTED = 1 WHERE ID = :invoice_id",
                         {'invoice_id': client_params.invoice_id})

    with reporter.step(u'Переносим средства на кошелек'):
        steps.OrderSteps.transfer([{'order_id': client_params.order_id_2, 'qty_old': 10, 'qty_new': 5, 'all_qty': 0}],
                                  [{'order_id': migrated_order_id, 'qty_delta': 1}])

    after_transfer = \
        steps.CommonSteps.build_notification(1, object_id=migrated_order_id)['args'][0].get('TotalConsumeQty')
    utils.check_that(after_transfer, hamcrest.equal_to('25550'),
                     step=u'Проверяем TotalConsumeQty на валютном заказе после перехода на мультивалютность '
                          u'копированием и откатов по заакченным фишечным заказам после переноса средств')


def test_notification_after_migrate_by_modify_and_new_campaigns():
    client_params = prepare_client_with_group_order()

    with reporter.step(u'Переходим на мультивалютность миграцией'):
        steps.ClientSteps.migrate_to_currency(client_params.client_id, currency_convert_type='MODIFY')

    after_migrate = steps.CommonSteps.build_notification(1, object_id=client_params.order_id)['args'][0].get(
        'TotalConsumeQty')
    utils.check_that(after_migrate, hamcrest.equal_to('6000'),
                     step=u'Проверяем TotalConsumeQty после перехода на мультивалютность миграцией')

    migrated_orders_list = [
        {'ServiceID': client_params.service_id, 'ServiceOrderID': client_params.service_order_id, 'Qty': 1000,
         'BeginDT': client_params.now},
        {'ServiceID': client_params.service_id, 'ServiceOrderID': client_params.service_order_id_2, 'Qty': 1000,
         'BeginDT': client_params.now}]

    migrated_request_id = steps.RequestSteps.create(client_params.client_id, migrated_orders_list,
                                                    additional_params=dict(InvoiceDesireDT=client_params.now))
    migrated_invoice_id, _, _ = steps.InvoiceSteps.create(migrated_request_id, client_params.person_id,
                                                          client_params.paysys_id, credit=0,
                                                          contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(migrated_invoice_id)

    steps.CampaignsSteps.do_campaigns(client_params.service_id, client_params.service_order_id, {'Money': 900}, 0,
                                      client_params.now)
    steps.CampaignsSteps.do_campaigns(client_params.service_id, client_params.service_order_id_2, {'Money': 900}, 0,
                                      client_params.now)

    ua_transfer_today(client_params.client_id)

    after_migrate_2 = steps.CommonSteps.build_notification(1, object_id=client_params.order_id)['args'][0].get(
        'TotalConsumeQty')

    utils.check_that(after_migrate_2, hamcrest.equal_to('66000'),
                     step=u'Проверяем TotalConsumeQty после перехода на мультивалютность миграцией и новых зачислений')


def prepare_client_with_group_order():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_PH_RUB.id
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

    before_migrate = steps.CommonSteps.build_notification(1, object_id=order_id)['args'][0].get(
        'TotalConsumeQty')
    utils.check_that(before_migrate, hamcrest.equal_to('200'), step=u'Проверяем TotalConsumeQty до миграции')

    return client(client_id=client_id, person_id=person_id, service_id=service_id, product_id=product_id, now=now,
                  paysys_id=paysys_id, service_order_id=service_order_id, service_order_id_2=service_order_id_2,
                  order_id=order_id, order_id_2=order_id_2, invoice_id=invoice_id)


def prepare_order_copies(client_id, service_id):
    with reporter.step(u'Создаем валютные копии фишечных заказов'):
        migrated_product_id = Products.DIRECT_RUB.id
        migrated_service_order_id = steps.OrderSteps.next_id(service_id=service_id)
        migrated_service_order_id_2 = steps.OrderSteps.next_id(service_id=service_id)
        migrated_order_id = steps.OrderSteps.create(client_id, migrated_service_order_id, service_id=service_id,
                                                    product_id=migrated_product_id, params={'AgencyID': None})
        migrated_order_id_2 = steps.OrderSteps.create(client_id, migrated_service_order_id_2, service_id=service_id,
                                                      product_id=migrated_product_id,
                                                      params={'AgencyID': None,
                                                              'GroupServiceOrderID': migrated_service_order_id})
    return migrated_service_order_id, migrated_service_order_id_2, migrated_order_id, migrated_order_id_2


def campaign_money_orders(client_id, person_id, service_id, paysys_id, migrated_service_order_id,
                          migrated_service_order_id_2, now):
    migrated_orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': migrated_service_order_id, 'Qty': 10000, 'BeginDT': now},
        {'ServiceID': service_id, 'ServiceOrderID': migrated_service_order_id_2, 'Qty': 10000, 'BeginDT': now}]
    migrated_request_id = steps.RequestSteps.create(client_id, migrated_orders_list,
                                                    additional_params=dict(InvoiceDesireDT=now))
    migrated_invoice_id, _, _ = steps.InvoiceSteps.create(migrated_request_id, person_id, paysys_id, credit=0,
                                                          contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(migrated_invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, migrated_service_order_id, {'Money': 1000}, 0,
                                      now - relativedelta(days=1))
    steps.CampaignsSteps.do_campaigns(service_id, migrated_service_order_id_2, {'Money': 1000}, 0,
                                      now - relativedelta(days=1))

    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)


def migrate_to_currency_by_copy(client_id, service_order_id, migrated_service_order_id):
    with reporter.step(u'Переходим на мультивалютность копированием'):
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='COPY')
    with reporter.step(u'Делаем фишечный заказ дочерним для валютного кошелька'):
        steps.OrderSteps.create(client_id, service_order_id, params={'GroupServiceOrderID': migrated_service_order_id})
    with reporter.step(u'Разбираем общий счет'):
        steps.OrderSteps.ua_enqueue([client_id])
        steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)


def ua_transfer_today(client_id):
    steps.OrderSteps.ua_enqueue([client_id])
    with reporter.step(u'Разбираем общий счет за сегодня'):
        query = "SELECT input FROM T_EXPORT " \
                "WHERE CLASSNAME = 'Client' AND OBJECT_ID = {} AND TYPE = 'UA_TRANSFER'".format(client_id)
        export_input = steps.CommonSteps.get_pickled_value(query, 'input')
        export_input['for_dt'] += relativedelta(days=1)
        export_input = steps.CommonSteps.make_pickled_value(export_input)
        db.balance().execute("UPDATE T_EXPORT "
                             "SET INPUT = {input} "
                             "WHERE CLASSNAME = 'Client' AND OBJECT_ID = {client} "
                             "AND TYPE = 'UA_TRANSFER'".format(input=export_input, client=client_id))
        steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
