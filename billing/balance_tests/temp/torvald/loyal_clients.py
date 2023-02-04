# -*- coding: utf-8 -*-

import datetime
import json

from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib import utils

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
# BASE_DT = datetime.datetime.now()
BASE_DT = datetime.datetime(2017, 2, 1)

QTY = 601

AUTO_RU_AG = 14

PRODUCT = Product(7, 1475, 'Days', 'Money')
PAYSYS_ID = 1003

USD = 840
EUR = 978
CHF = 756

manager_uid = '244916211'

# client_params = {'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime(2000, 1, 1),
#                      'SERVICE_ID': SERVICE_ID, 'IS_AGENCY': 0}
client_id = None or steps.ClientSteps.create()
# steps.ClientSteps.set_overdraft(client_id, 7, 1000)

# query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :client_id"
# query_params = {'client_id': client_id}
# steps.sql(query, query_params)

agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
# agency_id = None
# agency_id = None

# query = "Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :agency_id"
# query_params = {'agency_id': agency_id}
# steps.sql(query, query_params)

order_owner = client_id
invoice_owner = agency_id or client_id
# steps.ClientSteps.link(invoice_owner, 'clientuid32')

person_id = None or steps.PersonSteps.create(invoice_owner, 'ur', params={'inn': '7814647618'})
# person_id = 2822846

contract_id, _ = steps.ContractSteps.create_contract('opt_agency_prem_post',
                                                     {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                      'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                      'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                      'SERVICES': [7],
                                                      # 'COMMISSION_TYPE': 48,
                                                      # 'NON_RESIDENT_CLIENTS': 0,
                                                      # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                      'REPAYMENT_ON_CONSUME': 0,
                                                      'PERSONAL_ACCOUNT': 1,
                                                      'LIFT_CREDIT_ON_PAYMENT': 1,
                                                      'PERSONAL_ACCOUNT_FICTIVE': 1
                                                      })

#
# contract_id, _ = steps.ContractSteps.create_contract('tr_comm_post',{'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
#                                                    'DT'       : '2015-04-30T00:00:00',
#                                                    'FINISH_DT': '2016-12-30T00:00:00',
#                                                    'IS_SIGNED': '2015-01-01T00:00:00',
#                                                    # 'is_signed': None,
#                                                    'SERVICES': [7],
#                                                    # 'COMMISSION_TYPE': 48,
#                                                    # 'NON_RESIDENT_CLIENTS': 0,
#                                                    # # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
#                                                    'REPAYMENT_ON_CONSUME': 1,
#                                                    'PERSONAL_ACCOUNT': 1,
#                                                    'LIFT_CREDIT_ON_PAYMENT': 1,
#                                                    'PERSONAL_ACCOUNT_FICTIVE': 1
#                                                    })

loyal_clients = json.dumps(
    [{u"id": u"1", u"num": client_id, u"client": client_id, u"turnover": u"10000", u"todate": u"2017-08-15"}])

steps.ContractSteps.create_collateral(1024, {'CONTRACT2_ID': contract_id, 'LOYAL_CLIENTS': loyal_clients,
                                             'DT': '2017-02-15T00:00:00', 'IS_SIGNED': '2017-02-15T00:00:00'})
# contract_id = 237598
# contract_id = 601382

product = PRODUCT

orders_list = []
service_order_id = steps.OrderSteps.next_id(product.service_id)
# service_order_id =11443682
steps.OrderSteps.create(order_owner, service_order_id, service_id=product.service_id, product_id=product.id,
                        params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
orders_list.append(
    {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params=dict(InvoiceDesireDT=BASE_DT))
invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                             overdraft=0, endbuyer_id=None)

steps.InvoiceSteps.pay(invoice_id)
steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id, {'Bucks': 49.99, 'Money': 0}, 0,
                                  datetime.datetime(2017, 2, 2))
steps.CampaignsSteps.do_campaigns(product.service_id, service_order_id, {'Bucks': 99.99, 'Money': 0}, 0,
                                  datetime.datetime(2017, 2, 18))

steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)

if __name__ == '__main__':
    pass
