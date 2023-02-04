# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

DT = datetime.datetime.now()
PERSON_TYPE = 'usp'
PAYSYS_ID = 1029
# SERVICE_ID = 37
# PRODUCT_ID = 502917
# Days

# SERVICE_ID = 48
# PRODUCT_ID = 503363

# SERVICE_ID = 11
# PRODUCT_ID = 2136

# SERVICE_ID = 7
# PRODUCT_ID = 1475

# SERVICE_ID = 70
# PRODUCT_ID= 506387


# SERVICE_ID = 67
# PRODUCT_ID = 502660

# SERVICE_ID = 77
# PRODUCT_ID =503024


SERVICE_ID = 7
PRODUCT_ID = 1475

QTY = 300
OLD_DT = datetime.datetime(2016, 9, 25)
START_DT = str(DT.strftime("%Y-%m-%d")) + 'T00:00:00'


def sprav():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})

    invoice_owner = agency_id or None
    order_owner = client_id
    person_id = steps.PersonSteps.create(invoice_owner, PERSON_TYPE)

    contract_id = steps.ContractSteps.create_contract_new('usa_opt_agency', {'CLIENT_ID': invoice_owner,
                                                                             'PERSON_ID': person_id,
                                                                             'IS_FAXED': START_DT, 'DT': START_DT,
                                                                             'FIRM': 4, 'SERVICES': [SERVICE_ID],
                                                                             'PAYMENT_TYPE': 3})[0]

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    service_order_id1 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': invoice_owner, 'ManagerUID': None})
    order_id1 = steps.OrderSteps.create(order_owner, service_order_id1, PRODUCT_ID, SERVICE_ID,
                                        {'TEXT': 'Py_Test order', 'AgencyID': invoice_owner, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
        , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id1, 'Qty': QTY, 'BeginDT': DT}
    ]
    request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 40}, 0, campaigns_dt=DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id1, {'Bucks': 60}, 0, campaigns_dt=DT)

    # request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=DT))
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    # service_order_id  =22613092
    # steps.ActsSteps.generate(agency_id)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Units': 20}, 0, campaigns_dt=DT)


if __name__ == "__main__":
    sprav()
