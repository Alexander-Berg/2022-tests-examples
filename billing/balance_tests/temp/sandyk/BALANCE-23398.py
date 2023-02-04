# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

DT = datetime.datetime.now()
PERSON_TYPE = 'ph'
PAYSYS_ID = 11101002
SERVICE_ID = 11
PRODUCT_ID = 2136
QTY = 300
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'


def sprav():
    # agency_id =  steps.ClientSteps.create({'IS_AGENCY': 1})
    # agency_id  = 13266895
    # person_id = 4237921
    # contract_id = 374757

    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    # contract_id = steps.ContractSteps.create('opt_agency_prem_post', {'client_id': client_id,
    #                                                                              'person_id': person_id,
    #                                                                             'is_faxed': START_DT, 'dt': START_DT,
    #                                                                               'FIRM': 1, 'SERVICES':[37]})[0]


    invoice_owner = client_id
    order_owner = client_id

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
    ]
    request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Days': 30}, 0, campaigns_dt=DT)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1, {'Days': 60}, 0, campaigns_dt=DT)


if __name__ == "__main__":
    sprav()
