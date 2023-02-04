# -*- coding: utf-8 -*-

import datetime

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib.data import defaults

SERVICE_ID = 50
# PRODUCT_ID = 502953 ##502918
PRODUCT_ID = 504666
PAYSYS_ID = 1301003
QTY = 118
BASE_DT = datetime.datetime.now()

manager_uid = '244916211'

# def test_1 ():
#     with pytest.reporter.step('Create client with params:'):
#         client_id = None or steps.ClientSteps.create()


# client_params = {'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime(2000, 1, 1),
#                      'SERVICE_ID': SERVICE_ID, 'IS_AGENCY': 0}
client_id = 7001802 or steps.ClientSteps.create()
# steps.ClientSteps.set_force_overdraft(client_id, 7, 1000, firm_id=1)
# db.balance().execute("Update t_client set REGION_ID = 208 where ID = :client_id", {'client_id':client_id})
# query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :client_id"
# query_params = {'client_id': client_id}
# db.balance().execute(query, query_params)
# agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
agency_id = None
# query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :agency_id"
# query_params = {'agency_id': agency_id}
# db.balance().execute(query, query_params)

# agency_id = None
order_owner = client_id
invoice_owner = agency_id or client_id
steps.ClientSteps.link(invoice_owner, 'clientuid32')

# person_id = None or steps.PersonSteps.create(invoice_owner, 'sw_yt', params={'name': 'YANDEX LLC'})
# person_id = None or steps.PersonSteps.create(invoice_owner, 'ph')
person_id = 2386338 or steps.PersonSteps.create(invoice_owner, 'ur')
# person_id = 2857963
# person_id = None or steps.PersonSteps.create(invoice_owner, 'yt_kzu')
# person_id = None or steps.PersonSteps.create(invoice_owner, 'yt')

contract_id, _ = steps.ContractSteps.create_contract('no_agency', {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                                   'DT': '2015-04-30T00:00:00',
                                                                   'FINISH_DT': '2016-06-30T00:00:00',
                                                                   'IS_SIGNED': '2015-01-01T00:00:00',
                                                                   'SERVICES': [50],
                                                                   'FIRM_ID': 13,
                                                                   # 'COMMISSION_TYPE': 48,
                                                                   # 'NON_RESIDENT_CLIENTS': 0,
                                                                   # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                                   # 'REPAYMENT_ON_CONSUME': 0,
                                                                   'PERSONAL_ACCOUNT': 1,
                                                                   # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                                   # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                                   })

# contract_id, _ = steps.ContractSteps.create('comm_post',{'client_id': invoice_owner, 'person_id': person_id,
#                                                    'dt'       : '2015-04-30T00:00:00',
#                                                    'FINISH_DT': '2016-06-30T00:00:00',
#                                                    'is_signed': '2015-01-01T00:00:00',
#                                                    # 'is_signed': None,
#                                                    'SERVICES': [7, 11],
#                                                    # 'COMMISSION_TYPE': 48,
#                                                    'NON_RESIDENT_CLIENTS': 0,
#                                                    'DEAL_PASSPORT': '2015-12-01T00:00:00',
#                                                    # 'REPAYMENT_ON_CONSUME': 0,
#                                                    # 'PERSONAL_ACCOUNT': 1,
#                                                    # 'LIFT_CREDIT_ON_PAYMENT': 1,
#                                                    # 'PERSONAL_ACCOUNT_FICTIVE': 1
#                                                    })
# contract_id, contract_eid = steps.ContractSteps.create('opt_agency_post_kz', {'client_id': invoice_owner, 'person_id': person_id
#                                                          , 'dt': '2015-12-01T00:00:00'
#                                                          , 'FINISH_DT': '2017-12-31T00:00:00'
#                                                          , 'is_signed': '2015-12-01T00:00:00'
#                                                          , 'SERVICES': [7, 11, 37, 67, 70, 77]
#                                                          , 'DISCOUNT_POLICY_TYPE': 18
#                                                          , 'PERSONAL_ACCOUNT': 1
#                                                          , 'LIFT_CREDIT_ON_PAYMENT': 1
#                                                          , 'PERSONAL_ACCOUNT_FICTIVE': 1
#                                                          , 'CURRENCY': 398
#                                                          , 'BANK_DETAILS_ID': 320
#                                                          , 'DEAL_PASSPORT': '2015-12-01T00:00:00'
#                                                             })

#
# steps.ContractSteps.create_collateral(1033,{'contract2_id': contract_id, 'dt' : '2015-04-30T00:00:00', 'is_signed': '2015-01-01T00:00:00'})
# contract_id = None

orders_list = []
# service_order_id =16704213
for _ in xrange(1):
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id, 'ManagerUID': manager_uid, 'ContractID': contract_id})
    orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    api.medium().TurnOnRequest({'ContractID': contract_id, 'RequestID': request_id})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

steps.InvoiceSteps.pay(invoice_id)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
#--------------------------------------------------------------------------------------------------------------------
orders_list = []
for _ in xrange(10):
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

request_id = steps.RequestSteps.create(invoice_owner, orders_list)
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

api.medium().create_transfer_multiple(defaults.PASSPORT_UID,
                                      [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": orders_list[0]['ServiceOrderID'],
                                                                           "QtyOld": 10, "QtyNew": 8}
                                                                      ],
                                      [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": orders_list[1]['ServiceOrderID'],
                                                                           "QtyDelta": 1}
                                                                          , {"ServiceID": SERVICE_ID,
                                                                             "ServiceOrderID": orders_list[2]['ServiceOrderID'],
                                                                             "QtyDelta": 2}
                                                                      ], 1, None)

api.medium().create_transfer_multiple(defaults.PASSPORT_UID,
                                      [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": orders_list[0]['ServiceOrderID'],
                                                                           "QtyOld": 8, "QtyNew": 5}
                                                                      ],
                                      [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": orders_list[1]['ServiceOrderID'],
                                                                           "QtyDelta": 1}
                                                                          , {"ServiceID": SERVICE_ID,
                                                                             "ServiceOrderID": orders_list[2]['ServiceOrderID'],
                                                                             "QtyDelta": 2}
                                                                      ], 1, None)

api.medium().create_transfer_multiple(defaults.PASSPORT_UID,
                                      [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": orders_list[0]['ServiceOrderID'],
                                                                           "QtyOld": 5, "QtyNew": 3}
                                                                      ],
                                      [
                                                                          {"ServiceID": SERVICE_ID,
                                                                           "ServiceOrderID": orders_list[1]['ServiceOrderID'],
                                                                           "QtyDelta": 2}
                                                                          , {"ServiceID": SERVICE_ID,
                                                                             "ServiceOrderID": orders_list[2]['ServiceOrderID'],
                                                                             "QtyDelta": 2}
                                                                      ], 1, None)

for num, order in enumerate(orders_list):
    steps.CampaignsSteps.do_campaigns(order['ServiceID'], order['ServiceOrderID'], {'Bucks': num, 'Money': 0}, 0, BASE_DT)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, orders_list[1]['ServiceOrderID'], {'Bucks': 12.01, 'Money': 0}, 0, BASE_DT)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, orders_list[2]['ServiceOrderID'], {'Bucks': 14.23, 'Money': 0}, 0, BASE_DT)

steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
#-----------------------------------------------------------------------------------------------------

for it in xrange(5):
    client_id2 = None or steps.ClientSteps.create()
    steps.ClientSteps.merge(client_id, client_id2)
    # service_order_id = 21288706
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    # service_order_id3 = steps.OrderSteps.next_id(SERVICE_ID)
    # steps.OrderSteps.create(client_id, service_order_id3, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
        , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)

SERVICE_ID = 11
# PRODUCT_ID = 502953 ##502918
PRODUCT_ID = 2136
PAYSYS_ID = 1033

for it2 in xrange(5):
    client_id2 = None or steps.ClientSteps.create()
    steps.ClientSteps.merge(client_id, client_id2)
    # service_order_id = 21288706
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    # service_order_id3 = steps.OrderSteps.next_id(SERVICE_ID)
    # steps.OrderSteps.create(client_id, service_order_id3, service_id=SERVICE_ID, product_id=PRODUCT_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
        , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)

steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 20, 'Money': 0}, 0, BASE_DT)
# steps.CampaignsSteps.do_campaigns(SERVICE_ID ,service_order_id2, {'Bucks': 0, 'Money': 0}, 0, BASE_DT)
steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
