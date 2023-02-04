# coding: utf-8
__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
LOGIN = 'clientuid45'
PRODUCT_ID= 1475
PAYSYS_ID = 1001
PERSON_TYPE = 'ph'
QUANT = 100
MAIN_DT = datetime.datetime.now()
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'


def test_overdraft_notification():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE, {'email': 'test-balance-notify@yandex-team.ru'})
    contract_id = steps.ContractSteps.create_contract('opt_agency_prem_post',
                                                      {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                       'DT': START_DT, 'IS_FAXED': START_DT,
                                                       'SERVICES': [7], 'FIRM': 1})[0]

    data = []
    for x in range(0, 2):
        # client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        order_id = steps.OrderSteps.create(agency_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                           {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
        orders_list = {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
        data.append(
            {'client_id': agency_id, 'service_order_id': service_order_id, 'order_id': order_id,
             'orders_list': orders_list})
    orders_list = []
    for s in data:
        orders_list.append(s['orders_list'])

    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)

    for s in data:
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, s['service_order_id'],
                                          {'Bucks': 30, 'Days': 0, 'Money': 0}, 0, campaigns_dt=MAIN_DT)

        # request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
        # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
        #                                              credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
        # request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
        # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
        #                                              credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)


if __name__ == "__main__":
    test_overdraft_notification()
