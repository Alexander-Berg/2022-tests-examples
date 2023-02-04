import datetime

from balance import balance_steps as steps

SERVICE_ID_DIRECT = 7
PRODUCT_ID_DIRECT = 1475

SERVICE_ID_MARKET = 11
PRODUCT_ID_MARKET = 2136

PAYSYS_ID = 1003
QTY = 57.3
BASE_DT = datetime.datetime.now()
NEW_CAMPAIGNS_DT = datetime.datetime.now()+datetime.timedelta(days=1)

def test_1 ():
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'clientuid32')

    service_id = SERVICE_ID_DIRECT
    product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]
    contract_type = 'no_agency_post'
    request_id = steps.RequestSteps.create(client_id, orders_list)
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'PERSON_ID': person_id, 'CLIENT_ID': client_id,
                                                                         'SERVICES': [service_id],
                                                                         'FINISH_DT': NEW_CAMPAIGNS_DT,
                                                                         'REPAYMENT_ON_CONSUME': 0})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 20, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)




if __name__ == "__main__":
    test_1()