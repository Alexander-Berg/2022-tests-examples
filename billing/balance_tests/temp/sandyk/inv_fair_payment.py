# __author__ = 'sandyk'
#
# import datetime
# import pytest
# from decimal import *
#
# from balance import balance_steps as steps
#
# steps.InvoiceSteps.pay_fair(50707491)



__author__ = 'sandyk'

import datetime
import pytest
from btestlib import balance_steps as steps
import time


SERVICE_ID = 11
LOGIN = 'clientuid34'
PRODUCT_ID= 2136
PAYSYS_ID = 1001
PERSON_TYPE ='sw_ytph'
    # 'pu'
QUANT = 10
MAIN_DT = datetime.datetime.now()


def privat():
    # client_id =  steps.ClientSteps.create({'IS_AGENCY': 0})
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # # client_id = 11438876
    client_id = 12739282
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
    #
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    # steps.InvoiceSteps.pay(invoice_id, None, None)
if __name__ == "__main__":
    privat()
__author__ = 'sandyk'
