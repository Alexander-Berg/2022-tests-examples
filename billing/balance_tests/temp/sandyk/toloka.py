__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 42
LOGIN = 'clientuid34'
PRODUCT_ID= 507130
PAYSYS_ID = 1601070

PERSON_TYPE = 'sw_ytph'
QUANT = 10
MAIN_DT = datetime.datetime.now()


def test_overdraft_notification():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # person_id = steps.PersonSteps.create(client_id, 'sw_ph', {'email': 'test-balance-notify@yandex-team.ru'})
    # client_id = 9406582
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
        {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)


    # client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # contract_id,_=steps.ContractSteps.create('shv_client',{'client_id': client_id, 'person_id': person_id, 'FINISH_DT':'2019-09-22T00:00:00', 'dt': '2015-09-22T00:00:00','is_faxed': '2015-09-22T00:00:00', 'SERVICES':[42], 'FIRM_ID':16})
    #
    #
    # # person_id = steps.PersonSteps.create(client_id, 'sw_ph', {'email': 'test-balance-notify@yandex-team.ru'})
    # # client_id = 9406582
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
    #     {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
    #
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    # ]
    # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)

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
# steps.InvoiceSteps.pay(48290351, None, None)


# data_generator()