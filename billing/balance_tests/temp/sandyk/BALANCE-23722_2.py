__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

##недвижимость
SERVICE_ID = 81
LOGIN = 'clientuid45'

PRODUCT_ID = 507123
PRODUCT_ID1 = 503937

PAYSYS_ID = 1001
PERSON_TYPE = 'ur'
# 'pu'
QUANT = 10
MAIN_DT = datetime.datetime.now()


def privat():
    # client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    # steps.ClientSteps.link(client_id, LOGIN)
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    #
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # service_order_id1 = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
    #                                    {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    # order_id1 = steps.OrderSteps.create(client_id, service_order_id1, PRODUCT_ID1, SERVICE_ID,
    #                                     {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    #     , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id1, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    # ]
    # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})


    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(agency_id, LOGIN)
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)

    contract_id, _ = steps.ContractSteps.create_contract('opt_agency', {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                                        # 'dt'       : '2015-04-30T00:00:00',
                                                                        'IS_SIGNED': '2015-01-01T00:00:00'
        , 'SERVICES': [81]
        , 'PAYMENT_TYPE': 2
        , 'FIRM': 111
                                                                        })
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


if __name__ == "__main__":
    privat()
