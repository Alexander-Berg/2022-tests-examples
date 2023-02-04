# -*- coding: utf-8 -*-

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
# PRODUCT_ID = 502953 ##502918
PRODUCT_ID = 1475
PAYSYS_ID = 1003
QTY = 10
BASE_DT = datetime.datetime.now()

# def test_1 ():
#     with pytest.reporter.step('Create client with params:'):
#         client_id = None or steps.ClientSteps.create()

def test_1():
    client_id = None or steps.ClientSteps.create()

    # db.balance().execute("Update t_client set REGION_ID = 208 where ID = :client_id", {'client_id':client_id})
    # query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :client_id"
    # query_params = {'client_id': client_id}
    # db.balance().execute(query, query_params)
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    # steps.ClientSteps.link(client_id, 'ichalykin')
    # agency_id = None
    # query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :agency_id"
    # query_params = {'agency_id': agency_id}
    # db.balance().execute(query, query_params)

    steps.CommonSteps.set_extprops('Client', agency_id, 'force_contractless_invoice', {'value_num': 1})

    # agency_id = None
    order_owner = client_id
    invoice_owner = agency_id or client_id

    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')
    person_id2 = None or steps.PersonSteps.create(invoice_owner, 'ur', {'postcode': '666666'})
    # person_id = None or steps.PersonSteps.create(client_id, 'sw_ytph')
    # person_id = None or steps.PersonSteps.create(client_id, 'yt')
    # person_id = None or steps.PersonSteps.create(invoice_owner, 'trp')

    # contract_id, _ = steps.ContractSteps.create('opt_prem',{'client_id': invoice_owner, 'person_id': person_id,
    #                                                    'dt'       : '2015-04-30T00:00:00',
    #                                                    'FINISH_DT': '2016-06-30T00:00:00',
    #                                                    'is_signed': '2015-01-01T00:00:00',
    #                                                    'SERVICES': [7]
    #                                                    # 'COMMISSION_TYPE': 48,
    #                                                    # 'NON_RESIDENT_CLIENTS': 0,
    #                                                    # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
    #                                                    # 'REPAYMENT_ON_CONSUME': 0,
    #                                                    # 'PERSONAL_ACCOUNT': 1,
    #                                                    # 'LIFT_CREDIT_ON_PAYMENT': 1,
    #                                                    # 'PERSONAL_ACCOUNT_FICTIVE': 1
    #                                                    })

    contract_id2, _ = steps.ContractSteps.create_contract('comm_post',
                                                          {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                           'DT': '2015-04-30T00:00:00',
                                                           'FINISH_DT': '2016-06-30T00:00:00',
                                                           'IS_SIGNED': '2015-01-01T00:00:00',
                                                           'SERVICES': [7],
                                                           # 'COMMISSION_TYPE': 48,
                                                           'NON_RESIDENT_CLIENTS': 0
                                                           # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                           # 'REPAYMENT_ON_CONSUME': 0,
                                                           # 'PERSONAL_ACCOUNT': 1,
                                                           # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                           # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                           })

    # contract_id3, _ = steps.ContractSteps.create('opt_prem',{'client_id': invoice_owner, 'person_id': person_id,
    #                                                    'dt'       : '2015-04-30T00:00:00',
    #                                                    'FINISH_DT': '2016-06-30T00:00:00',
    #                                                    'is_signed': '2015-01-01T00:00:00',
    #                                                    'SERVICES': [7],
    #                                                    # 'COMMISSION_TYPE': 48,
    #                                                    # 'NON_RESIDENT_CLIENTS': 0
    #                                                    # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
    #                                                    # 'REPAYMENT_ON_CONSUME': 0,
    #                                                    # 'PERSONAL_ACCOUNT': 1,
    #                                                    # 'LIFT_CREDIT_ON_PAYMENT': 1,
    #                                                    # 'PERSONAL_ACCOUNT_FICTIVE': 1
    #                                                    })
    # #
    # contract_id4, _ = steps.ContractSteps.create('opt_prem',{'client_id': invoice_owner, 'person_id': person_id,
    #                                                    'dt'       : '2015-04-30T00:00:00',
    #                                                    'FINISH_DT': '2016-06-30T00:00:00',
    #                                                    'is_signed': '2015-01-01T00:00:00',
    #                                                    'SERVICES': [7],
    #                                                    # 'COMMISSION_TYPE': 48,
    #                                                    # 'NON_RESIDENT_CLIENTS': 0
    #                                                    # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
    #                                                    # 'REPAYMENT_ON_CONSUME': 0,
    #                                                    # 'PERSONAL_ACCOUNT': 1,
    #                                                    # 'LIFT_CREDIT_ON_PAYMENT': 1,
    #                                                    # 'PERSONAL_ACCOUNT_FICTIVE': 1
    #                                                    })
    #
    # contract_id5, _ = steps.ContractSteps.create('comm_post',{'client_id': invoice_owner, 'person_id': person_id,
    #                                                    'dt'       : '2015-04-30T00:00:00',
    #                                                    'FINISH_DT': '2016-06-30T00:00:00',
    #                                                    'is_signed': '2015-01-01T00:00:00',
    #                                                    'SERVICES': [7],
    #                                                    # 'COMMISSION_TYPE': 48,
    #                                                    'NON_RESIDENT_CLIENTS': 0
    #                                                    # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
    #                                                    # 'REPAYMENT_ON_CONSUME': 0,
    #                                                    # 'PERSONAL_ACCOUNT': 1,
    #                                                    # 'LIFT_CREDIT_ON_PAYMENT': 1,
    #                                                    # 'PERSONAL_ACCOUNT_FICTIVE': 1
    #                                                    })

    # steps.ContractSteps.create_collateral(1033,{'contract2_id': contract_id2, 'dt' : '2015-04-30T00:00:00', 'is_signed': '2015-01-01T00:00:00'})
    # contract_id = None
    # contract_id2 = None
    # contract_id3 = None
    # contract_id4 = None

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    for _ in xrange(1):
        steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                params={'AgencyID': agency_id})
        orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id2,
                                                     overdraft=0, endbuyer_id=None)

    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id2, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id3,
                                                     overdraft=0, endbuyer_id=None)

    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)

    steps.InvoiceSteps.pay(invoice_id2)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)

def test_2():
    client_id = None or steps.ClientSteps.create()
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    # steps.ClientSteps.link(client_id, 'ichalykin')
    # agency_id = None

    # steps.CommonSteps.set_extprops('Client', agency_id, 'force_contractless_invoice', {'value_num': 1})

    order_owner = client_id
    invoice_owner = agency_id or client_id

    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')
    person_id2 = None or steps.PersonSteps.create(invoice_owner, 'ur', {'postcode': '666666'})

    # contract_id, _ = steps.ContractSteps.create('comm_post',{'client_id': invoice_owner, 'person_id': person_id,
    #                                                    'dt'       : '2015-04-30T00:00:00',
    #                                                    'FINISH_DT': '2016-06-30T00:00:00',
    #                                                    'is_signed': '2015-01-01T00:00:00',
    #                                                    'SERVICES': [7],
    #                                                    # 'COMMISSION_TYPE': 48,
    #                                                    'NON_RESIDENT_CLIENTS': 0
    #                                                    # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
    #                                                    # 'REPAYMENT_ON_CONSUME': 0,
    #                                                    # 'PERSONAL_ACCOUNT': 1,
    #                                                    # 'LIFT_CREDIT_ON_PAYMENT': 1,
    #                                                    # 'PERSONAL_ACCOUNT_FICTIVE': 1
    #                                                    })
    #
    contract_id2, _ = steps.ContractSteps.create_contract('comm_post',
                                                          {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                           'DT': '2016-03-30T00:00:00',
                                                           'FINISH_DT': '2016-06-30T00:00:00',
                                                           'IS_SIGNED': '2015-01-01T00:00:00',
                                                           'SERVICES': [7],
                                                           # 'COMMISSION_TYPE': 48,
                                                           'NON_RESIDENT_CLIENTS': 0
                                                           # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                           # 'REPAYMENT_ON_CONSUME': 0,
                                                           # 'PERSONAL_ACCOUNT': 1,
                                                           # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                           # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                           })

    # steps.ContractSteps.create_collateral(1033,{'contract2_id': contract_id2, 'dt' : '2015-04-30T00:00:00', 'is_signed': '2015-01-01T00:00:00'})
    contract_id = None
    contract_id2 = None

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    for _ in xrange(1):
        steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                params={'AgencyID': agency_id})
        orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                     overdraft=0, endbuyer_id=None)

    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id2, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                     overdraft=0, endbuyer_id=None)

    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)

    steps.InvoiceSteps.pay(invoice_id2)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)

def test_3():
    client_id = None or steps.ClientSteps.create()
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    # steps.ClientSteps.link(client_id, 'ichalykin')
    # agency_id = None

    steps.CommonSteps.set_extprops('Client', agency_id, 'force_contractless_invoice', {'value_num': 1})

    order_owner = client_id
    invoice_owner = agency_id or client_id

    person_id = None or steps.PersonSteps.create(invoice_owner, 'sw_ur')
    person_id2 = None or steps.PersonSteps.create(invoice_owner, 'sw_ur', {'postcode': '666666'})

    contract_id2, _ = steps.ContractSteps.create_contract('sw_agent_post',
                                                          {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                           'DT': '2016-01-30T00:00:00',
                                                           'FINISH_DT': '2016-06-30T00:00:00',
                                                           'IS_SIGNED': '2015-01-01T00:00:00',
                                                           'SERVICES': [7],
                                                           # 'COMMISSION_TYPE': 48,
                                                           # 'NON_RESIDENT_CLIENTS': 0
                                                           # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                           # 'REPAYMENT_ON_CONSUME': 0,
                                                           # 'PERSONAL_ACCOUNT': 1,
                                                           # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                           # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                           })

    # steps.ContractSteps.create_collateral(1033,{'contract2_id': contract_id2, 'dt' : '2015-04-30T00:00:00', 'is_signed': '2015-01-01T00:00:00'})
    contract_id = None
    contract_id2 = None

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    for _ in xrange(1):
        steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                params={'AgencyID': agency_id})
        orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                     overdraft=0, endbuyer_id=None)

    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id2, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                     overdraft=0, endbuyer_id=None)

    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)

    steps.InvoiceSteps.pay(invoice_id2)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)

if __name__ == "__main__":
    test_3()