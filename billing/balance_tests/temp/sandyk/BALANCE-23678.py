__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
LOGIN = 'clientuid45'
PRODUCT_ID = 1475
# PERSON_TYPE ='by_ytph'
# 'pu'
QUANT = 3333
MAIN_DT = datetime.datetime.now()
DT = datetime.datetime.now() - datetime.timedelta(days=1)


def privat():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'REGION_ID': 149})

    # steps.ClientSteps.link(client_id,LOGIN)
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    steps.ClientSteps.link(client_id, LOGIN)
    # service_order_id = 95171343
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    # steps.InvoiceSteps.pay(55606465)
    # # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    # #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    #
    # steps.InvoiceSteps.pay(invoice_id, None, None)


if __name__ == "__main__":
    privat()
