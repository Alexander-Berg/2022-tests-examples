import datetime

import balance.balance_db as db
from balance import balance_steps as steps
from check import retrying

SERVICE_ID_DIRECT = 7
PRODUCT_ID_DIRECT = 1475

SERVICE_ID_MARKET = 11
PRODUCT_ID_MARKET = 2136

PAYSYS_ID = 1028
QTY = 55.7
BASE_DT = datetime.datetime.now()

@retrying.retry(stop_max_attempt_number=10, wait_exponential_multiplier=1 * 1000)
def unhide(act_id):
    steps.ActsSteps.unhide(act_id)

@retrying.retry(stop_max_attempt_number=5, wait_exponential_multiplier=1 * 1000)
def export(act_id):
    steps.CommonSteps.export('OEBS', 'Act', act_id)

def test_1 ():
    act_id=46011278
    export(act_id)
    pass
    unhide(act_id)
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'usu')
    steps.ClientSteps.link(client_id, 'clientuid32')

    contract_id = None

    service_id = SERVICE_ID_DIRECT
    product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    service_order_id2 = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id2, service_id=service_id, product_id=product_id)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 10, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)

    query = "select ID from t_act where client_id=:client_id"
    query_params = {'client_id': client_id}
    act_id = db.balance().execute(query, query_params)[0]['id']
    eid = db.balance().execute('select external_id from t_act where id= :id', {'id': act_id})[0]['external_id']
    steps.CommonSteps.export('OEBS', 'Act', eid)

    request_id = steps.RequestSteps.create(client_id, orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 0, 'Money': 23.41}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 0, 'Money': 23.41}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)

if __name__ == "__main__":
    test_1()
    # test_2()