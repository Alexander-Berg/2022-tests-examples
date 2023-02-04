__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
LOGIN = 'clientuid45'

# PRODUCT_ID = 100000001  ##==33
# PRODUCT_ID1 = 100000002 ##==33

# PRODUCT_ID = 503270   ##<>17 <>33
# PRODUCT_ID1 = 503271  ##<>17 <>33

PRODUCT_ID = 1475  ##==17
PRODUCT_ID1 = 1475  ##==17

PAYSYS_ID = 1001
PERSON_TYPE = 'ur'
# 'pu'
QUANT = 10
MAIN_DT = datetime.datetime.now()


def privat():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    steps.ClientSteps.link(client_id, LOGIN)
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    service_order_id1 = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    order_id1 = steps.OrderSteps.create(client_id, service_order_id1, PRODUCT_ID1, SERVICE_ID,
                                        {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
        , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id1, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})


    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    #
    # steps.InvoiceSteps.pay(invoice_id, None, None)


if __name__ == "__main__":
    privat()
