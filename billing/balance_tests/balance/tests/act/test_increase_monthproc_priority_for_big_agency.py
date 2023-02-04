# coding=utf-8
__author__ = 'aikawa'

import datetime

import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features

after = datetime.datetime.now()
dt = after

SERVICE_ID = 7
PRODUCT_ID = 1475
PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
QTY = 100


@pytest.mark.priority('low')
@reporter.feature(Features.ACT, Features.TO_UNIT)
@pytest.mark.tickets('BALANCE-21145')
def test_set_high_priority_to_acts():
    client_id = steps.ClientSteps.create()
    steps.CommonSteps.set_extprops(classname='Client', object_id=client_id, attrname='acts_most_valuable_priority',
                                   params={'VALUE_NUM': 1})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50, 'Days': QTY, 'Money': 0}, 0, dt)

    # We should enqueue object only, not process
    steps.ActsSteps.enqueue([client_id], force=1, date=dt)
    # steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)

    priority_value = db.balance().execute(
        "select priority from t_export where type = 'MONTH_PROC' and classname = 'Client' and object_id = :object_id",
        {'object_id': client_id})[0]['priority']
    assert priority_value == -1


if __name__ == "__main__":
    test_set_high_priority_to_acts()
