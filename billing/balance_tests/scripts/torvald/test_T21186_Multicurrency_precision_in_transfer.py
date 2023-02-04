# -*- coding: utf-8 -*-

import datetime

import balance.balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from btestlib.data import defaults

SERVICE_ID = 7
PRODUCT_ID = 503162
PAYSYS_ID = 1003
QTY = 100.1234
BASE_DT = datetime.datetime.now()


def simple_tranfser():
    client_params = {'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime(2000, 1, 1),
                     'SERVICE_ID': SERVICE_ID, 'IS_AGENCY': 0}
    client_id = steps.ClientSteps.create(client_params)
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    service_order_id3 = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id3, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    response = api.medium().create_transfer_multiple(defaults.PASSPORT_UID,
                                                     [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": service_order_id,
                                                                           "QtyOld": 100.1234, "QtyNew": 0}
                                                                      ],
                                                     [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": service_order_id2,
                                                                           "QtyDelta": 1}
                                                                          , {"ServiceID": SERVICE_ID,
                                                                             "ServiceOrderID": service_order_id3,
                                                                             "QtyDelta": 2}
                                                                      ], 1, None)
    reporter.log(response)


def ua_not_enough_money_for_transfer():
    client_params = {'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime(2000, 1, 1),
                     'SERVICE_ID': SERVICE_ID, 'IS_AGENCY': 0}
    client_id = steps.ClientSteps.create(client_params)
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id2 = steps.OrderSteps.create(client_id, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    service_order_id3 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id3 = steps.OrderSteps.create(client_id, service_order_id3, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Bucks': 100, 'Money': 0}, 0, BASE_DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id3, {'Bucks': 200, 'Money': 0}, 0, BASE_DT)

    api.medium().server.Balance2.CreateOrUpdateOrdersBatch(defaults.PASSPORT_UID,
                                                           [
                                                               {'ServiceID': SERVICE_ID,
                                                                'ServiceOrderID': service_order_id2,
                                                                'ProductID': PRODUCT_ID, 'ClientID': client_id,
                                                                'GroupServiceOrderID': service_order_id}
                                                               , {'ServiceID': SERVICE_ID,
                                                                  'ServiceOrderID': service_order_id3,
                                                                  'ProductID': PRODUCT_ID, 'ClientID': client_id,
                                                                  'GroupServiceOrderID': service_order_id}
                                                           ])

    query = ''

    response = api.medium(raise_error=False).create_transfer_multiple(defaults.PASSPORT_UID,
                                                                      [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": service_order_id,
                                                                           "QtyOld": 100.1234, "QtyNew": 0}
                                                                      ],
                                                                      [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": service_order_id2,
                                                                           "QtyDelta": 1}
                                                                          , {"ServiceID": SERVICE_ID,
                                                                             "ServiceOrderID": service_order_id3,
                                                                             "QtyDelta": 2}
                                                                      ], 1, None)
    reporter.log(response)


if __name__ == '__main__':
    ua_not_enough_money_for_transfer()
