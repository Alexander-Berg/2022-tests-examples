__author__ = 'sandyk'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features

SERVICE_ID = 7
PRODUCT_ID_AFTER_MULTICURRENCY = 503162
PRODUCT_ID_BEFORE_MULTICURRENCY = 1475
PAYSYS_ID = 1001
PERSON_TYPE = 'ph'
OVERDRAFT_LIMIT = 120
QUANT = 10
MAIN_DT = datetime.datetime.now()


@pytest.mark.slow
@pytest.mark.priority('low')
@reporter.feature(Features.OVERDRAFT, Features.NOTIFICATION)
def overdraft_notification():

    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    steps.ClientSteps.set_overdraft(client_id, 7, OVERDRAFT_LIMIT, firm_id=1, start_dt=MAIN_DT,
                                    currency=None, invoice_currency=None)
    # db.balance().execute('update (select * from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\') set priority=-1',
    #           {'client_id': client_id})

    # overdraft_limit = []
    # while len(overdraft_limit) == 0:
    # overdraft_limit = db.balance().execute('select overdraft_limit from T_CLIENT_OVERDRAFT where client_id = :client_id',
        #                           {'client_id': client_id})
        # time.sleep(3)
    # else:
    #     print overdraft_limit[0]['overdraft_limit']
    # client_params = {'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB',
    #                  'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
    #                  'SERVICE_ID': SERVICE_ID, 'CURRENCY_CONVERT_TYPE': 'MODIFY'}
    # client_id = steps.ClientSteps.create(client_params)
    # api.TestBalance().calculate_overdraft([client_id])
    # steps.CommonSteps.wait_for('select state as val from t_export where OBJECT_ID = :client_id and type  = \'OVERDRAFT\'',{'client_id': client_id},1)
    # steps.CommonSteps.wait_for_notification (10, client_id, 4 , timeout=420)
    # overdraft_limit = steps.CommonSteps.parse_notification (10, client_id, 3, 'info', 'overdraft_limit')

    # return overdraft_limit


if __name__ == "__main__":
    # pytest.main("test_overdraft_notification.py")
    overdraft_notification()
    # assert overdraft_limit == '3000'
    # overdraft_notification()

    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID_AFTER_MULTICURRENCY, SERVICE_ID,
    # {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
    #
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 2000, 'BeginDT': MAIN_DT}
    # ]
    # request_id = steps.RequestSteps.create(client_id, orders_list, None, MAIN_DT)
    # invoice_id = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID,overdraft=1)[0]
    # steps.InvoiceSteps.pay(invoice_id, None, None)




 # client_id = 9022456
    #
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
    # {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
    #
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': MAIN_DT}
    # ]
    # request_id = steps.RequestSteps.create(client_id, orders_list, None, MAIN_DT)