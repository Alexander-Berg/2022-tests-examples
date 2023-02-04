#-*- coding: utf-8 -*-

##from MTest import mtl
from MTestlib import MTestlib as mtl
import xmlrpclib
import pprint
import datetime
import time
import urlparse
##from balance import mapper

##import session

def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

rpc = mtl.rpc
test_rpc = mtl.test_rpc


uid = 'clientuid34'

service_id = 50;
product_id = 504446
qty= 500

dt = datetime.datetime.now()
campaigns_dt = datetime.datetime.now()
begin_dt = datetime.datetime.now()
paysys_id    = 1003
is_credit=0
overdraft=0

##504445		Пополнение лицевого счета по договору (без НДС)
##504446     	Пополнение лицевого счета по договору (с НДС)

def test_client():

##    client_id = mtl.create_client({'IS_AGENCY': 0})
##    mtl.link_client_uid(client_id, uid)
##    person_id = mtl.create_person(client_id, 'ph',{'email': 'test-balance-notify@yandex-team.ru'})
    client_id = 29230940
    person_id = 4439227
##    service_order_id = 46184310

##    mtl.do_campaigns(service_id, service_order_id, {'Bucks': 5, 'Money': 0}, 0, campaigns_dt)

##    print '3'
##    mtl.create_act(invoice_id, campaigns_dt)

    service_order_id = mtl.get_next_service_order_id(service_id)
    order_id = mtl.create_or_update_order (client_id, product_id, service_id, service_order_id ,  {'TEXT':'Py_Test order'})
    orders_list = []
    orders_list.append({'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': datetime.datetime.now()})

    request_id = mtl.create_request (client_id, orders_list, begin_dt)
##    Service_order_id: 46184310
##    Order: 15305347
##    request_id = mtl.create_request (client_id, orders_list, datetime.datetime(2014,10,15))

##def get_rit_contract_personal_account(contract=None, paysys=None, with_nds=None, auto_create=False):
##
##    nds_mapper = {0: 0, 1: 18}
##
##    assert contract and paysys and with_nds in (0, 1), (contract, paysys, with_nds)
##    invoice = None
##
##    person = 4439227
##    session = (contract or person).session
##    invoices = session.query(mapper.Invoice).filter_by(person=person, contract=contract, postpay=1, hidden=0).all()
##    invoices = [i for i in invoices if i.subclient_id is None]
##    if len(invoices) > 2:
##        raise Exception('There are too many personal accounts: %s' % invoices)
##    elif len(invoices) == 2:
##        # У договора РИТ может быть 2 ЛС, один с НДС 18, другой c НДС 0.
##        check = sum([i.nds_pct for i in invoices])
##        assert check == nds_mapper[1], 'Contract %s has PA of same NDS type' % contract.external_id
##        invoice, = [i for i in invoices if (i.nds_pct == nds_mapper[with_nds] and i.nds == 1)]
##    elif len(invoices) == 1:
##        invoice = invoices[0] if invoices[0].nds_pct == nds_mapper[with_nds] else None
##
##    if not invoice and auto_create:
##        session.refresh(person, lockmode='update')
##
##        from balance.actions.invoice_create import InvoiceFactory
##        invoice = InvoiceFactory.create(paysys=paysys, person=person,
##                                        contract=contract, postpay=1,
##                                        temporary=False, strict_match=True,
##                                        subclient_id=None)
##        session.add(invoice)
##        session.flush()
##        invoice.nds = 1
##        invoice.nds_pct = nds_mapper[with_nds]
##        session.flush()
##
##    if not invoice:
##        raise Exception('%s or %s has no suitable personal account' % (contract, person))
##
##    return invoice
##
##
##get_rit_contract_personal_account(347719,1001,1,True)

test_client()