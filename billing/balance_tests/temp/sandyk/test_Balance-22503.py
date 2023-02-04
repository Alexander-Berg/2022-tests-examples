__author__ = 'sandyk'

import datetime

import pytest

from balance import balance_steps as steps

SERVICE_ID = 7
LOGIN = 'clientuid34'
PRODUCT_ID= 1475
PAYSYS_ID = 1001
PERSON_TYPE ='ph'
QUANT = 100
MAIN_DT = datetime.datetime.now()



def test_history():
    client_id = steps.ClientSteps.create({'IS_AGENCY':0})
    agency_id = steps.ClientSteps.create({'IS_AGENCY':1})
    order_owner = client_id
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, PERSON_TYPE)
    contract_id, _ = steps.ContractSteps.create_contract('opt_agency_prem_post',
                                                         {'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                          'DT': '2015-04-30T00:00:00',
                                                          'FINISH_DT': '2016-06-30T00:00:00',
                                                          'IS_SIGNED': '2015-01-01T00:00:00',
                                                          'SERVICES': [7],
                                                          'FIRM': 1,
                                                          'NON_RESIDENT_CLIENTS': 1,
                                                          'PERSONAL_ACCOUNT': 1,
                                                          'PERSONAL_ACCOUNT_FICTIVE': 1
                                                          })



    ### credit
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create (order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : agency_id, 'ManagerUID': None})

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, None, None)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,{'Bucks': 10, 'Days': 0, 'Money': 0}, 0, MAIN_DT)
    steps.ActsSteps.create(invoice_id, MAIN_DT)
    ### prepayment
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create (order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
    #     {'TEXT':'Py_Test order','AgencyID' : agency_id, 'ManagerUID': None})
    #
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    # ]
    # request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    #
    steps.InvoiceSteps.pay(invoice_id, None, None)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,{'Bucks': 10, 'Days': 0, 'Money': 0}, 0, MAIN_DT)
    steps.ActsSteps.create(invoice_id, MAIN_DT)
if __name__ == "__main__":
    pytest.main("-v -s test_Balance-22503.py")




