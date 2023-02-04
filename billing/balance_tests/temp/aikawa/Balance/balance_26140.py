import pytest
import datetime
import time
from balance import balance_steps as steps
from balance import balance_db as db
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions

NOW = datetime.datetime.now()
DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
def test_migrate_optimized_ua(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    parent_order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                              product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    child_service_order_id = steps.OrderSteps.next_id(context.service.id)
    child_order_id = steps.OrderSteps.create(client_id, child_service_order_id, service_id=context.service.id,
                                             product_id=context.product.id, params={'AgencyID': None})
    steps.OrderSteps.merge(parent_order_id, [child_order_id], group_without_transfer=1)
    steps.OrderSteps.make_optimized_force(parent_order_id)

    steps.CampaignsSteps.do_campaigns(context.service.id, child_service_order_id, {'Bucks': 50}, 0,
                                      NOW - datetime.timedelta(days=1))

    # steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    print '({0},{1})'.format(child_service_order_id, service_order_id)
    steps.ClientSteps.create(
        {'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'SERVICE_ID': context.service.id,
         'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
         'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    time.sleep(10)
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)
    print db.get_order_by_id(parent_order_id)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
def test_migrate_order_wo_consume(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': None})

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 50}, 0,
                                      NOW - datetime.timedelta(days=1))

    steps.ClientSteps.create(
        {'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'SERVICE_ID': context.service.id,
         'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
         'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    time.sleep(10)
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)
    print db.get_order_by_id(order_id)



    # steps.CommonSteps.build_notification(1, object_id=parent_order_id)
    # steps.CampaignsSteps.do_campaigns(context.service.id, child_service_order_id, {'Money': 10}, 0,
    #                                   NOW+ datetime.timedelta(hours=1))

    # steps.CommonSteps.export('UA_TRANSFER', 'Client',  client_id)

    # = > {'args': [{'CompletionFixedMoneyQty': '0',
    #                'CompletionFixedQty': '0',
    #                'CompletionQty': '0',
    #                'ConsumeMoneyQty': '0',
    #                'ConsumeQty': '0',
    #                'ConsumeSum': '0',
    #                'ProductCurrency': '',
    #                'ServiceID': 7,
    #                'ServiceOrderID': 135599031,
    #                'Signal': 1,
    #                'SignalDescription': 'Order balance have been changed',
    #                'Tid': '9049715214027',
    #                'TotalConsumeQty': '0'}],
    #      'kwargs': {},
    #      'path': ['BalanceClient', 'NotifyOrder2'],
    #      'protocol': 'json-rest',
    # 'url': 'https://balance-proxy.qart.yandex-team.ru/proxy/json/'}
