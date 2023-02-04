# -*- coding: utf-8 -*-

import datetime

import balance.balance_db as db
from balance import balance_steps as steps

DT = datetime.datetime.now()
PAYMENT_TERM_DT = DT - datetime.timedelta(days=20)
# PERSON_TYPE = 'ph'
# PAYSYS_ID = 1001
PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
OVERDRAFT_LIMIT = 1000
FIRM_ID = 1


def test_overdraft():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, 'aikawa-test-0')
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.OverdraftSteps.set_force_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, FIRM_ID)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, PAYMENT_TERM_DT)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=DT)[0]
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0}, 0, PAYMENT_TERM_DT)

    service_order_id2 = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                              service_order_id=service_order_id2)

    steps.OrderSteps.merge(parent_order_id, [order_id])

    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    db.BalanceBO().execute('update t_invoice set payment_term_dt = :PAYMENT_TERM_DT where id=:invoice_id',
                           {'invoice_id': invoice_id, 'PAYMENT_TERM_DT': PAYMENT_TERM_DT})
    db.BalanceBO().execute('update t_invoice set dt = :PAYMENT_TERM_DT where id=:invoice_id',
                           {'invoice_id': invoice_id, 'PAYMENT_TERM_DT': PAYMENT_TERM_DT})
    steps.OverdraftSteps.overdraft_job(client_id)


# steps.CommonSteps.export('OVERDRAFT', 'Client', 17539170)

def test_from_ticket():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, 'aikawa-test-0')
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.ClientSteps.set_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, firm_id=1, start_dt=DT,
                                    currency=None, invoice_currency=None)
    db.balance().execute(
        'update (select * from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\') set priority=-1',
        {'client_id': client_id})
    steps.CommonSteps.wait_for(
        'select state as val from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\' and classname = \'Client\'',
        {'client_id': client_id}, 1, interval=2)

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)

    service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id2 = steps.OrderSteps.create(client_id, service_order_id2, PRODUCT_ID, SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT},
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 10, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 7}, 0, PAYMENT_TERM_DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Bucks': 5}, 0, PAYMENT_TERM_DT)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=DT)[0]
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 6}, 0, PAYMENT_TERM_DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Bucks': 7}, 0, PAYMENT_TERM_DT)

    db.BalanceBO().execute('update t_invoice set payment_term_dt = :PAYMENT_TERM_DT where id=:invoice_id',
                           {'invoice_id': invoice_id, 'PAYMENT_TERM_DT': PAYMENT_TERM_DT})
    db.BalanceBO().execute('update t_invoice set dt = :PAYMENT_TERM_DT where id=:invoice_id',
                           {'invoice_id': invoice_id, 'PAYMENT_TERM_DT': PAYMENT_TERM_DT})
    steps.OverdraftSteps.overdraft_job(client_id)


def test_from_ticket2():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, 'aikawa-test-0')
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.ClientSteps.set_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, firm_id=1, start_dt=DT,
                                    currency=None, invoice_currency=None)
    db.balance().execute(
        'update (select * from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\') set priority=-1',
        {'client_id': client_id})
    steps.CommonSteps.wait_for(
        'select state as val from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\' and classname = \'Client\'',
        {'client_id': client_id}, 1, interval=2)

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)

    service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id2 = steps.OrderSteps.create(client_id, service_order_id2, PRODUCT_ID, SERVICE_ID)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 10, 'BeginDT': DT},
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': 10, 'BeginDT': DT}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 5}, 0, PAYMENT_TERM_DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Bucks': 7}, 0, PAYMENT_TERM_DT)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=DT)[0]
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 0}, 0, PAYMENT_TERM_DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Bucks': 0}, 0, PAYMENT_TERM_DT)

    db.BalanceBO().execute('update t_invoice set payment_term_dt = :PAYMENT_TERM_DT where id=:invoice_id',
                           {'invoice_id': invoice_id, 'PAYMENT_TERM_DT': PAYMENT_TERM_DT})
    db.BalanceBO().execute('update t_invoice set dt = :PAYMENT_TERM_DT where id=:invoice_id',
                           {'invoice_id': invoice_id, 'PAYMENT_TERM_DT': PAYMENT_TERM_DT})
    steps.OverdraftSteps.overdraft_job(client_id)
