# -*- coding: utf-8 -*-

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
import btestlib.config as balance_config
from balance.features import Features

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'


#
@reporter.feature(Features.TO_UNIT)
def test_single_archive_consume():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    consume = db.get_consumes_by_invoice(invoice_id)[0]['archive']
    assert consume == 1
    reporter.log(consume)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.smoke
def test_multiple_archive_consume():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    consume = db.get_consumes_by_invoice(invoice_id)
    assert consume[0]['archive'] == 1
    db.balance().execute('''update t_consume set act_qty = 199 where id = :consume_id''',
                         {'consume_id': consume[0]['id']})
    consume = db.get_consumes_by_invoice(invoice_id)
    assert consume[0]['archive'] == 0

    db.balance().execute('''update t_consume set act_qty = 200, act_sum = 5999 where id = :consume_id''',
                         {'consume_id': consume[0]['id']})
    consume = db.get_consumes_by_invoice(invoice_id)
    assert consume[0]['archive'] == 0

    db.balance().execute('''update t_consume set act_qty = 200, act_sum = 6000 where id = :consume_id''',
                         {'consume_id': consume[0]['id']})
    consume = db.get_consumes_by_invoice(invoice_id)
    assert consume[0]['archive'] == 1

