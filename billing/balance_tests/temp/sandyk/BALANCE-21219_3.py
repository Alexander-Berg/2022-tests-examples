# coding: utf-8
__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 70
LOGIN = 'clientuid34'
PRODUCT_ID = 506387
# PAYSYS_ID = 1066
# PERSON_TYPE = 'sw_ph'
PAYSYS_ID = 1001
PERSON_TYPE = 'ph'
QUANT = 1000
MAIN_DT = datetime.datetime.now()


def test_overdraft_notification():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})

    # agency_id  = 13612024
    # person_id = 4282665
    # contract_id = 389457

    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE, {'email': 'test-balance-notify@yandex-team.ru'})
    # contract_id,_=steps.ContractSteps.create('7_opt_agency_post',{'client_id': agency_id, 'person_id': person_id, 'FINISH_DT':'2019-09-22T00:00:00', 'dt': '2015-09-22T00:00:00','is_faxed': '2015-09-22T00:00:00'})
    contract_id, _ = steps.ContractSteps.create_contract('opt_agency_prem_post',
                                                         {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                          'FINISH_DT': '2019-09-22T00:00:00',
                                                          'DT': '2015-09-22T00:00:00',
                                                          'IS_FAXED': '2015-09-22T00:00:00', 'SERVICES': [7, 70]})

    # client_id = 9406582
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : agency_id, 'ManagerUID': None})
    order_id2 = steps.OrderSteps.create (client_id, service_order_id2, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : agency_id, 'ManagerUID': None})

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT},
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': QUANT+10, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Shows': 30}, 0, campaigns_dt=MAIN_DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id2, {'Shows': 60}, 0, campaigns_dt=MAIN_DT)

if __name__ == "__main__":
    test_overdraft_notification()
