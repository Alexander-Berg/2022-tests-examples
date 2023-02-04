# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import balance.balance_api as api
import balance.balance_db as db
from balance import balance_steps as steps

SERVICE_ID = 7
PRODUCT_ID_BEFORE_MULTICURRENCY = 1475
PRODUCT_ID_AFTER_MULTICURRENCY = 503162
PAYSYS_ID = 1001
PERSON_TYPE = 'ph'
OVERDRAFT_LIMIT = 120
QUANT = 10
MAIN_DT = datetime.datetime.now()


def test_overdraft_notification():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID_BEFORE_MULTICURRENCY, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty':3600.034, 'BeginDT': MAIN_DT}
    ]
    print client_id, orders_list,  MAIN_DT

    steps.ClientSteps.set_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, firm_id=1, start_dt=MAIN_DT,
                                                currency=None, invoice_currency=None)
    db.balance().execute(
        'update (select * from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\') set priority=-1',
        {'client_id': client_id})
    steps.CommonSteps.wait_for(
                'select state as val from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\' and classname = \'Client\'',
                {'client_id': client_id}, 1, interval=2)


    steps.ClientSteps.migrate_to_currency(client_id,'MODIFY')

    api.test_balance().CalculateOverdraft([client_id])
    db.balance().execute(
        'update (select * from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\') set priority=-1',
        {'client_id': client_id})
    steps.CommonSteps.wait_for(
                'select state as val from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\' and classname = \'Client\'',
                {'client_id': client_id}, 1, interval=2)

    # client_id = 9308646

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT} )

    # steps.InvoiceSteps.pay(46822093, 20, None)


if __name__ == "__main__":
    test_overdraft_notification()
    # print overdraft_limit
    pytest.main("test_overdraft_notification.py")
    # assert overdraft_limit == '3000'

    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID_AFTER_MULTICURRENCY, SERVICE_ID,
    #     {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
    #
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 2000, 'BeginDT': MAIN_DT}
    # ]
    # request_id = steps.RequestSteps.create(client_id, orders_list, None, MAIN_DT)
    # invoice_id = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID,overdraft=1)[0]
    # steps.InvoiceSteps.pay(invoice_id, None, None)


# data_generator()