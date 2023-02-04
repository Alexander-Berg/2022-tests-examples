# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

DT = datetime.datetime.now()
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
# SERVICE_ID = 37
# PRODUCT_ID = 502917

SERVICE_ID = 7
PRODUCT_ID = 1475
QTY = 300
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'


def sprav():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    # agency_id  = 13266895
    # person_id = 4237921
    # contract_id = 374757

    # client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)

    # contract_id = steps.ContractSteps.create_contract('opt_agency_prem_post', {'CLIENT_ID': agency_id,
    #                                                                              'PERSON_ID': person_id,
    #                                                                             'IS_FIXED': START_DT, 'DT': START_DT,
    #                                                                               'FIRM': 1, 'SERVICES':[37]})[0]


    invoice_owner = agency_id
    order_owner = agency_id

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # service_order_id1 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': invoice_owner, 'ManagerUID': None})
    # order_id1 = steps.OrderSteps.create(order_owner, service_order_id1, PRODUCT_ID, SERVICE_ID ,
    #                                    {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
        # ,{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': DT}
    ]
    request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 30}, 0, campaigns_dt=DT)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1, {'Bucks': 60}, 0, campaigns_dt=DT)


if __name__ == "__main__":
    sprav()
