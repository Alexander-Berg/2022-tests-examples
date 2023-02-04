# -*- coding: utf-8 -*-

import datetime
import time

from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib import utils
from btestlib.constants import Services, PersonTypes

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
BASE_DT = datetime.datetime.now()
# BASE_DT = datetime.datetime(2017, 7, 1)

QTY = 250

AUTO_RU_AG = 14

# PRODUCT = Product(98, 506587, 'Bucks', 'Money')
PRODUCT = Product(7, 1475, 'Bucks', 'Money')

PAYSYS_ID = 1003

USD = 840
EUR = 978
CHF = 756

manager_uid = '244916211'



client_params = None
client_id = None or steps.ClientSteps.create(params=client_params)

# query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :client_id"
# query_params = {'client_id': client_id}
# steps.sql(query, query_params)

# agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
agency_id = None

# query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :agency_id"
# query_params = {'agency_id': agency_id}
# steps.sql(query, query_params)

order_owner = client_id
invoice_owner = agency_id or client_id
# steps.ClientSteps.link(invoice_owner, 'clientuid34')

person_params = {}
# person_params = {'fias_guid': '807648a6-7adb-4d82-ac78-251dcce950f4', 'legal_fias_guid': '8e05359f-282e-45a0-8e04-645b1573a06f',
#                  'is-partner': '1'}
# person_params = {'inn': '590579876860'}
person_id = None or steps.PersonSteps.create(invoice_owner, 'ph', person_params)
# person_id = 6389107

# contract_id, _ = steps.ContractSteps.create_contract('no_agency_post',
#                                                      {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
#                                                       'DT': HALF_YEAR_BEFORE_NOW_ISO,
#                                                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
#                                                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
#                                                       # 'is_signed': None,
#                                                       'SERVICES': [Services.DIRECT.id, Services.GEO.id,
#                                                                    Services.MEDIA.id],
#                                                       # 'COMMISSION_TYPE': 48,
#                                                       # 'NON_RESIDENT_CLIENTS': 0,
#                                                       # # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
#                                                       'REPAYMENT_ON_CONSUME': 0,
#                                                       'PERSONAL_ACCOUNT': 1,
#                                                       'LIFT_CREDIT_ON_PAYMENT': 0,
#                                                       'PERSONAL_ACCOUNT_FICTIVE': 1,
#                                                       # 'DISCOUNT_POLICY_TYPE': 0,
#                                                       'CREDIT_LIMIT_SINGLE': 6000
#                                                       })

# contract_id = None

product = PRODUCT

orders_list = []
service_order_id = steps.OrderSteps.next_id(product.service_id)
# service_order_id =11443682
order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=product.service_id, product_id=product.id,
                        params={'AgencyID': agency_id, 'ManagerUID': manager_uid})

orders_list.append(
    {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': 80, 'BeginDT': BASE_DT})

request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=BASE_DT))

invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                             overdraft=0, endbuyer_id=None)

steps.InvoiceSteps.pay(invoice_id)

steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0, BASE_DT)

steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)

