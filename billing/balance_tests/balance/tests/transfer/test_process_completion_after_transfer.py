# -*- coding: utf-8 -*-

import datetime

import pytest
from hamcrest import equal_to, greater_than

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as utils
from btestlib.data import defaults

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.TRANSFER, Features.COMPLETION)]

SERVICE_ID = 7
# PRODUCT_ID = 502953 ##502918
PRODUCT_ID = 1475
PAYSYS_ID = 1003
QTY = 100
BASE_DT = datetime.datetime.now()

manager_uid = '244916211'

def data_generator():
    client_id = None or steps.ClientSteps.create()
    agency_id = None

    order_owner = client_id
    invoice_owner = agency_id or client_id
    steps.ClientSteps.link(invoice_owner, 'clientuid32')

    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')
    contract_id = None

    orders_list = []
    for i in xrange(3):
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                    params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
        orders_list.append({'OrderID': order_id, 'ServiceID': SERVICE_ID,
                            'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0,
                                                     contract_id=contract_id,
                                                     overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    return orders_list


@pytest.fixture
def service_order_ids():
    return data_generator()


def test_process_completion_after_sync_transfer(service_order_ids):

    now = datetime.datetime.now()
    api.medium().CreateTransferMultiple(defaults.PASSPORT_UID,
                                          [
                                              {"ServiceID": SERVICE_ID,
                                               "ServiceOrderID": service_order_ids[0]['ServiceOrderID'],
                                               "QtyOld": 100, "QtyNew": 87
                                               }
                                          ],
                                          [
                                              {"ServiceID": SERVICE_ID,
                                               "ServiceOrderID": service_order_ids[1]['ServiceOrderID'],
                                               "QtyDelta": 1
                                               },
                                              {"ServiceID": SERVICE_ID,
                                               "ServiceOrderID": service_order_ids[2]['ServiceOrderID'],
                                               "QtyDelta": 2
                                               }
                                          ], 1, None)

    query = "select state, update_dt from t_export where type = 'PROCESS_COMPLETION' and object_id = :order_id"
    for i in xrange(len(service_order_ids)):
        state = db.balance().execute(query, {'order_id': service_order_ids[i]['OrderID']})[0]
        if state['state']:
            utils.check_that(state['update_dt'], greater_than(now))
        else:
            pass


def test_process_completion_after_async_transfer(service_order_ids):
    operation_id = steps.TransferSteps.create_operation(passport_uid=defaults.PASSPORT_UID)

    now = datetime.datetime.now()
    response = api.medium().CreateTransferMultiple(defaults.PASSPORT_UID,
                                                   [
                                                      {"ServiceID": SERVICE_ID,
                                                       "ServiceOrderID": service_order_ids[0]['ServiceOrderID'],
                                                       "QtyOld": 100, "QtyNew": 87}
                                                   ],
                                                   [
                                                      {"ServiceID": SERVICE_ID,
                                                       "ServiceOrderID": service_order_ids[1]['ServiceOrderID'],
                                                       "QtyDelta": 1},
                                                      {"ServiceID": SERVICE_ID,
                                                       "ServiceOrderID": service_order_ids[2]['ServiceOrderID'],
                                                       "QtyDelta": 2}
                                                  ], 1, operation_id)

    query = "select state, update_dt from t_export where type = 'PROCESS_COMPLETION' and object_id = :order_id"
    for i in xrange(len(service_order_ids)):
        state = db.balance().execute(query, {'order_id': service_order_ids[i]['OrderID']})[0]
        if state['state']:
            utils.check_that(state['update_dt'], greater_than(now))
        else:
            pass


if __name__ == "__main__":
    test_process_completion_after_sync_transfer()