__author__ = 'aikawa'

import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'

dt = datetime.datetime.now() - datetime.timedelta(days=1)

def test1():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    order_ids_list = []

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {MSR: 150}, 0, dt)
    order_ids_list.append(order_id)
    #
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {MSR: 150}, 0, dt)
    order_ids_list.append(order_id)



    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)

    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)

    steps.OrderSteps.make_optimized(parent_order_id)

    steps.OrderSteps.merge(parent_order_id, order_ids_list)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

def test2():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    order_ids_list = []
    invoice_ids_list = []

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {MSR: 150}, 0, dt)
    order_ids_list.append(order_id)
    invoice_ids_list.append(invoice_id)
    #
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    invoice_ids_list.append(invoice_id)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {MSR: 150}, 0, dt)
    order_ids_list.append(order_id)



    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)

    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    invoice_ids_list.append(invoice_id)
    # steps.OrderSteps.make_optimized(parent_order_id)

    steps.OrderSteps.merge(parent_order_id, order_ids_list)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    print steps.ActsSteps.generate(client_id, force=1, date=dt)

def test3():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    order_ids_list = []

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {MSR: 150}, 0, dt)
    order_ids_list.append(order_id)
    #
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {MSR: 150}, 0, dt)
    order_ids_list.append(order_id)



    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)

    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 70, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)

    # steps.OrderSteps.make_optimized(parent_order_id)

    steps.OrderSteps.merge(parent_order_id, order_ids_list)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    print steps.ActsSteps.generate(client_id, force=1, date=dt)

def test4():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    order_ids_list = []

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {MSR: 150}, 0, dt)
    order_ids_list.append(order_id)
    #
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    orders_list = [
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
            ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                                  credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {MSR: 150}, 0, dt)
    order_ids_list.append(order_id)



    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)

    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID, service_id=SERVICE_ID)
    # orders_list = [
    #             {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 70, 'BeginDT': dt}
    #         ]
    # request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
    # invoice_id,_,_= steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                                               credit=0, contract_id=None, overdraft=0,endbuyer_id=None)
    #
    # steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)

    steps.OrderSteps.make_optimized(parent_order_id)

    steps.OrderSteps.merge(parent_order_id, order_ids_list)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    print steps.ActsSteps.generate(client_id, force=1, date=dt)
# test1()
# test2()
# test3()
test4()