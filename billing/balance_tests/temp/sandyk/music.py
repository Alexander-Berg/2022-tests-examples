__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 23
LOGIN = 'clientuid34'
PRODUCT_ID = 503355
PAYSYS_ID = 1057
PERSON_TYPE = 'ph'
QUANT = 10
MAIN_DT = datetime.datetime.now()


def test_overdraft_notification():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    steps.ClientSteps.link(client_id, LOGIN)
    person_id = steps.PersonSteps.create(client_id, 'ph', {'email': 'test-balance-notify@yandex-team.ru'})
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    invoice_id = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID,overdraft=0)[0]

if __name__ == "__main__":
    test_overdraft_notification()
    # pytest.main("test_overdraft_notification.py")
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