# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps


DT = datetime.datetime.now()
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
SERVICE_ID = 37
PRODUCT_ID = 502917
QTY = 100

def sprav():
    agency_id =  steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id =  steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    order_owner = client_id

    person_id = steps.PersonSteps.create(invoice_owner, PERSON_TYPE)
    contract_id, _ = steps.ContractSteps.create_contract('opt_prem_post',
                                                         {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                          # 'dt'       : '2015-04-30T00:00:00',
                                                          # 'FINISH_DT': None,
                                                          'IS_SIGNED': '2015-01-01T00:00:00'
                                                          # 'is_signed': None,
                                                             , 'SERVICES': [7, 37]
                                                          # 'FIRM': 1,
                                                             , 'SCALE': 1
                                                          })

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    service_order_id1 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID ,
                                       {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})
    order_id1 = steps.OrderSteps.create(order_owner, service_order_id1, PRODUCT_ID, SERVICE_ID ,
                                       {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
        ,{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': DT}
    ]
    request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                     credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Days': 20}, 0, campaigns_dt=DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1, {'Days': 50}, 0, campaigns_dt=DT)

if __name__ == "__main__":
    sprav()