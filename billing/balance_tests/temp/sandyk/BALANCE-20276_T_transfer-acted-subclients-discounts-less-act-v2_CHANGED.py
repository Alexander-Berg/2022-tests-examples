#-*- coding: utf-8 -*-

##from MTest import mtl
import igogor.data
import igogor.balance_api as api
from igogor.balance_api import TransferInfo as transfer
import igogor.balance_steps as steps
from igogor.data import defaults as defaults
import igogor.logger
import igogor.utils as utils

import xmlrpclib
import pprint
import datetime
import urlparse
import time

##def Print(obj):
##    print pprint.pformat(obj).decode('unicode_escape')
##
##rpc = mtl.rpc
##test_rpc = mtl.test_rpc

auto_prefix = '[MT]: '

uid = 'clientuid33'
##Заказ  / Реквест
SERVICE_ID = 7;
PRODUCT_ID = 1475 ##503162
##service_id = 11; PRODUCT_ID = 2136
##service_id = 70; PRODUCT_ID = 502761
##service_id = 77; PRODUCT_ID = 504083
##service_id = 99; PRODUCT_ID = 504850

QTY  = 100
QTY2 = 200
PAYSYS_ID = 1003

##after = datetime.datetime.now()
AFTER = datetime.datetime(2015,8,26,11,0,0)

begin_dt     = AFTER
request_dt   = AFTER ##не меняется
invoice_dt   = AFTER
payment_dt   = AFTER
campaigns_dt = AFTER
act_dt       = AFTER
migrate_dt   = AFTER
##manager_uid = '241593318'
manager_uid = None
##------------------------------------------------------------------------------
def data_generator():
    client_id  = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Petrov3'})['client_id']
    client_id2 = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Petrov3'})['client_id']
    client_id3 = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Petrov3'})['client_id']
    client_id4 = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Petrov3'})['client_id']
    client_id5 = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Petrov3'})['client_id']
    agency_id  = steps.ClientSteps.create({'IS_AGENCY': 1, 'NAME': u'Иванов Иван Иванович'})['client_id'] ##mass

    order_owner   = client_id
    order_owner2   = client_id2
    order_owner3   = client_id3
    order_owner4   = client_id4
    order_owner5   = client_id5
    invoice_owner = agency_id
    if order_owner == invoice_owner: agency_id = None

    person_id = steps.PersonSteps.create(invoice_owner, 'ur', {'phone':'234'})

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create (order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})
##
    service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id2 = steps.OrderSteps.create (order_owner2, service_order_id2, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})

    service_order_id3 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id3 = steps.OrderSteps.create  (order_owner3, service_order_id3, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None}
##        , AgencyID = agency_id, manager_uid = manager_uid
        )

    service_order_id4 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id4 = steps.OrderSteps.create (order_owner4, service_order_id4, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})

    service_order_id5 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id5 = steps.OrderSteps.create  (order_owner, service_order_id5, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})

    service_order_id6 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id5 = steps.OrderSteps.create  (order_owner5, service_order_id6, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': begin_dt}
        , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': begin_dt}
        , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id5, 'Qty': QTY, 'BeginDT': begin_dt}
        , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id6, 'Qty': QTY, 'BeginDT': begin_dt}
    ]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list, None, request_dt)
    invoice_id = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, overdraft = 0)['invoice_id']
    steps.ClientSteps.get_direct_discount(client_id2, datetime.datetime(2015,8,1), pct=3)
    steps.InvoiceSteps.pay(invoice_id, None, None)
    steps.CampaignsSteps.do_campaigns(7, service_order_id, {'Bucks': 30, 'Money': 0}, 0, campaigns_dt)
    steps.ActsSteps.generate(invoice_owner, 1, campaigns_dt)

    steps.CampaignsSteps.do_campaigns(7, service_order_id2, {'Bucks': 30, 'Money': 0}, 0, campaigns_dt)
    steps.ActsSteps.generate(invoice_owner, 1, campaigns_dt)
    steps.CampaignsSteps.do_campaigns(7, service_order_id, {'Bucks': 20, 'Money': 0}, 0, campaigns_dt)
    steps.CampaignsSteps.do_campaigns(7, service_order_id2, {'Bucks': 20, 'Money': 0}, 0, campaigns_dt)
    print '... (3)'; time.sleep(3)
    response = api.Medium(raise_error=False).create_transfer_multiple(16571028,[{"QtyOld":"100.000000","ServiceOrderID": service_order_id2,"ServiceID":7,"QtyNew":"20.000000", 'AllQty': 1}],[{"QtyDelta":"92.158000","ServiceOrderID":service_order_id4,"ServiceID":7}], 1,steps.TransferSteps.create_operation())
    response = api.Medium(raise_error=False).create_transfer_multiple(16571028,[{"QtyOld":"100.000000","ServiceOrderID": service_order_id,"ServiceID":7,"QtyNew":"20.000000", 'AllQty': 1}],[{"QtyDelta":"92.158000","ServiceOrderID":service_order_id3,"ServiceID":7}],  1,steps.TransferSteps.create_operation())
    print '... (3)'; time.sleep(3)
    steps.CampaignsSteps.do_campaigns(7, service_order_id5, {'Bucks': 10, 'Money': 0}, 0, campaigns_dt)
    steps.CampaignsSteps.do_campaigns(7, service_order_id6, {'Bucks': 10, 'Money': 0}, 0, campaigns_dt)
    steps.ClientSteps.get_direct_discount(client_id3, datetime.datetime(2015,8,1), pct=3)
    steps.CampaignsSteps.do_campaigns(7, service_order_id3, {'Bucks': 5.8, 'Money': 0}, 0, campaigns_dt)
    steps.ClientSteps.get_direct_discount(client_id4, datetime.datetime(2015,8,1), pct=5)
    steps.CampaignsSteps.do_campaigns(7, service_order_id4, {'Bucks': 4.4, 'Money': 0}, 0, campaigns_dt)
    steps.ActsSteps.generate(invoice_owner, 1, campaigns_dt)
    steps.ActsSteps.generate(invoice_owner, 1, campaigns_dt)



##    mtl.do_campaigns(7, service_order_id3, {'Bucks': 20, 'Money': 0}, 0, campaigns_dt)
##    mtl.act_accounter(invoice_owner, 1, campaigns_dt)

##    mtl.Print(dict(mtl.objects))

# Test_1 [Transfer_acted]_transfer_acted_with_groupping_by_discounts
def init_transfer_acted_with_groupping_by_discounts ():
    data_generator()

if __name__ == '__main__':
    init_transfer_acted_with_groupping_by_discounts()
    pass

