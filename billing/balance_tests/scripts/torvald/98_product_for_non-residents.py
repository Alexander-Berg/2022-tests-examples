# -*- coding: utf-8 -*-

import datetime

from balance import balance_steps as steps

SERVICE_ID = 98
PRODUCT_ID = 507188
PAYSYS_ID = 1046
# PAYSYS_ID = 1043
QTY = 1000
BASE_DT = datetime.datetime.now()

client_id = None or steps.ClientSteps.create()
# agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
agency_id = None
order_owner = client_id
invoice_owner = agency_id or client_id
steps.ClientSteps.link(invoice_owner, 'clientuid32')

person_id = None or steps.PersonSteps.create(invoice_owner, 'sw_yt', params={'verified-docs': '1'})
# person_id = None or steps.PersonSteps.create(invoice_owner, 'sw_ur')

contract_id, _ = steps.ContractSteps.create_contract('shv_client', {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                                    'DT': '2015-04-30T00:00:00',
                                                                    'FINISH_DT': '2016-06-30T00:00:00',
                                                                    'IS_SIGNED': '2015-01-01T00:00:00',
                                                                    'SERVICES': [98],
                                                                    'CURRENCY': 978,
                                                                    'FIRM': 16
                                                                    # 'COMMISSION_TYPE': 48,
                                                                    # 'NON_RESIDENT_CLIENTS': 0,
                                                                    # 'DEAL_PASSPORT': '2015-12-01T00:00:00',
                                                                    # 'REPAYMENT_ON_CONSUME': 0,
                                                                    # 'PERSONAL_ACCOUNT': 1,
                                                                    # 'LIFT_CREDIT_ON_PAYMENT': 1,
                                                                    # 'PERSONAL_ACCOUNT_FICTIVE': 1
                                                                    })

# steps.ContractSteps.create_collateral(1033,{'contract2_id': contract_id, 'dt' : '2015-04-30T00:00:00', 'is_signed': '2015-01-01T00:00:00'})
# contract_id = None

orders_list = []
service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
# service_order_id =16704213
for _ in xrange(1):
    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

steps.InvoiceSteps.pay(invoice_id)
steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100000000, 'Money': 0}, 0, BASE_DT)

steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)
#--------------------------------------------------------------------------------------------------------------------
