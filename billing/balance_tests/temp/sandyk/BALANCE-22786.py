__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 70
PRODUCT_ID = 505039
PRODUCT_ID1 = 503257

# 505039  ##COMMISSION_TYPE = 17
# 503928  ##ENGINE_ID  =70 and COMMISSION_TYPE != 17

PAYSYS_ID = 1001
PERSON_TYPE = 'ur'
# 'pu'
QUANT = 10
MAIN_DT = datetime.datetime.now()


def privat():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)
    contract_id, _ = steps.ContractSteps.create_contract_new('opt_agency_prem',
                                                             {'client_id': agency_id, 'person_id': person_id,
                                                                  'is_signed': '2015-01-01T00:00:00',
                                                              'SERVICES': [70, 67, 77, 7], 'SCALE': 3
                                                                 , 'PAYMENT_TYPE': 2})
    contract_id, _ = steps.ContractSteps.create_contract_new('opt_agency_prem',
                                                             {'client_id': agency_id, 'person_id': person_id,
                                                                  'is_signed': '2015-01-01T00:00:00',
                                                              'SERVICES': [70, 67, 77, 7], 'SCALE': 1
                                                                 , 'PAYMENT_TYPE': 2})
    contract_id, _ = steps.ContractSteps.create_contract_new('commiss', {'client_id': agency_id, 'person_id': person_id,
                                                             'is_signed': '2015-01-01T00:00:00',
                                                                         'SERVICES': [70, 67, 77, 7],
                                                                         'PAYMENT_TYPE': 2})

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    service_order_id1 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})
    order_id1 = steps.OrderSteps.create(client_id, service_order_id1, PRODUCT_ID1, SERVICE_ID,
                                        {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
        , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id1, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]

    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    #
    # steps.InvoiceSteps.pay(invoice_id, None, None)


if __name__ == "__main__":
    privat()
