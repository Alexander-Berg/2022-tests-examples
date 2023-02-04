# -*- coding: utf-8 -*-

import datetime
import json

import balance.balance_db as db
from balance import balance_steps as steps

SERVICE_ID = 77
# PRODUCT_ID = 502953 ##502918
PRODUCT_ID = 2584
PAYSYS_ID = 1026
QTY = 118
BASE_DT = datetime.datetime(2016,4,1)

manager_uid = None

def test_USD_non_resident_with_individual_limit():
    client_id = None or steps.ClientSteps.create()
    db.balance().execute("Update t_client set REGION_ID = 134 where ID = :client_id", {'client_id': client_id})
    query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :client_id"
    query_params = {'client_id': client_id}
    db.balance().execute(query, query_params)

    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(client_id, 'clientuid32')
    # agency_id = None

    order_owner = client_id
    invoice_owner = agency_id or client_id


    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('comm_post',
                                                         {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                          'DT': '2015-04-30T00:00:00',
                                                          'FINISH_DT': '2016-06-30T00:00:00',
                                                          'IS_SIGNED': '2015-01-01T00:00:00',
                                                          'SERVICES': [7, 77],
                                                          'COMMISSION_TYPE': 57,
                                                          'NON_RESIDENT_CLIENTS': 1,
                                                          # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                          # 'REPAYMENT_ON_CONSUME': 0,
                                                          # 'PERSONAL_ACCOUNT': 1,
                                                          # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                          # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                          })

    individual_limits = json.dumps([{u'client_credit_type': u'2', u'id': u'1', u'client_limit_currency': u'', u'num': order_owner, u'client': order_owner, u'client_payment_term': u'45', u'client_limit': u'50000'}])
    steps.ContractSteps.create_collateral(1035, {'CONTRACT2_ID': contract_id,
                                                 'DT': '2015-04-30T00:00:00',
                                                 'IS_SIGNED': '2015-01-01T00:00:00',
                                                 'CLIENT_LIMITS': individual_limits})
    # contract_id = None

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # service_order_id =16704213
    for _ in xrange(1):
        steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
        orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

        # request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': BASE_DT})
        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1, contract_id=contract_id,
                                                     overdraft=0, endbuyer_id=None)

    db.balance().execute('update t_invoice set dt = :dt where id = :invoice_id',
                         {'dt': BASE_DT, 'invoice_id': invoice_id})

    # 12: 67,125
    # 13: 66,3456
    # 14: 65,7662

    # steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Shows': 99.99, 'Money': 0}, 0, BASE_DT)

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
#--------------------------------------------------------------------------------------------------------------------


def test_RUB_non_resident_with_individual_limit():

    SERVICE_ID = 7
    # PRODUCT_ID = 502953 ##502918
    PRODUCT_ID = 1475
    PAYSYS_ID = 1025

    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})

    client_id = None or steps.ClientSteps.create({'AGENCY_ID': agency_id})
    db.balance().execute("Update t_client set REGION_ID = 171 where ID = :client_id", {'client_id': client_id})
    query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'RUB', ISO_CURRENCY_PAYMENT = 'RUB', IS_NON_RESIDENT = 1 where ID = :client_id"
    query_params = {'client_id': client_id}
    db.balance().execute(query, query_params)

    steps.ClientSteps.link(client_id, 'clientuid32')
    # agency_id = None

    order_owner = client_id
    invoice_owner = agency_id or client_id


    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('comm_post',
                                                         {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                          'DT': '2015-04-30T00:00:00',
                                                          'FINISH_DT': '2016-06-30T00:00:00',
                                                          'IS_SIGNED': '2015-01-01T00:00:00',
                                                          'SERVICES': [7, 77],
                                                          'COMMISSION_TYPE': 57,
                                                          'NON_RESIDENT_CLIENTS': 1,
                                                          # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                          # 'REPAYMENT_ON_CONSUME': 0,
                                                          # 'PERSONAL_ACCOUNT': 1,
                                                          # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                          # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                          })

    individual_limits = json.dumps([{u'client_credit_type': u'2', u'id': u'1', u'client_limit_currency': u'', u'num': order_owner, u'client': order_owner, u'client_payment_term': u'45', u'client_limit': u'50000'}])
    steps.ContractSteps.create_collateral(1035, {'CONTRACT2_ID': contract_id,
                                                 'DT': '2015-04-30T00:00:00',
                                                 'IS_SIGNED': '2015-01-01T00:00:00',
                                                 'CLIENT_LIMITS': individual_limits})
    # contract_id = None

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # service_order_id =16704213
    for _ in xrange(1):
        steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
        orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

        # request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': BASE_DT})
        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1, contract_id=contract_id,
                                                     overdraft=0, endbuyer_id=None)

    db.balance().execute('update t_invoice set dt = :dt where id = :invoice_id',
                         {'dt': BASE_DT, 'invoice_id': invoice_id})

    # 12: 67,125
    # 13: 66,3456
    # 14: 65,7662

    # steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Shows': 99.99, 'Money': 0}, 0, BASE_DT)

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
    pass