import datetime

import balance.balance_db as db
from balance import balance_steps as steps

SERVICE_ID_DIRECT = 7
PRODUCT_ID_DIRECT = 1475

SERVICE_ID_MARKET = 11
PRODUCT_ID_MARKET = 2136

# usp
# PAYSYS_ID = 1029
# usu
PAYSYS_ID = 1003
QTY = 30
# BASE_DT = datetime.datetime.now()
# NEW_CAMPAIGNS_DT = datetime.datetime.now() + datetime.timedelta(days=1)

def get_order_id(service_order_id, service_id):
    query = 'select id from t_order where SERVICE_ORDER_ID= :service_order_id and SERVICE_ID = :service_id'
    query_params = {'service_order_id': service_order_id, 'service_id': service_id}
    order_id = db.balance().execute(query, query_params)[0]['id']
    return order_id


def test_1():
    BASE_DT = datetime.datetime.now()
    NEW_CAMPAIGNS_DT = datetime.datetime.now() + datetime.timedelta(days=1)
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.link(client_id, 'clientuid32')

    service_id = SERVICE_ID_DIRECT
    product_id = PRODUCT_ID_DIRECT

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)
    steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
    service_order_id_2 = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id_2, service_id=service_id, product_id=503162)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT},
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id_2, 'Qty': 1000, 'BeginDT': BASE_DT}
    ]
    contract_type = 'opt_agency_prem_post'
    request_id = steps.RequestSteps.create(client_id, orders_list)
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'PERSON_ID': person_id, 'CLIENT_ID': client_id,
                                                                         'SERVICES': [service_id],
                                                                         'FINISH_DT': NEW_CAMPAIGNS_DT,
                                                                         'REPAYMENT_ON_CONSUME': 0,
                                                                         'PERSONAL_ACCOUNT': 1})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    # db.balance().execute('update bo.t_invoice set TRANSFER_ACTED = 1 where id = :id', {'id': invoice_id})
    campaigns_qty = 20

    # steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    campaigns_delta = 2
    # campaigns_qty = campaigns_qty+campaigns_delta
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    campaigns_qty = campaigns_qty+campaigns_delta

    # steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    campaigns_qty_2 = 30
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': campaigns_qty_2, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
    # campaigns_delta_2 = 11
    campaigns_qty_2 = campaigns_qty_2 - campaigns_delta
    # campaigns_qty = campaigns_qty + 5
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': campaigns_qty, 'Money': 0}, 0, BASE_DT)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': campaigns_qty_2, 'Money': 0}, 0, BASE_DT)
    # dpt_order_id = get_order_id(service_order_id_2, service_id)

    # child_order_id = get_order_id(service_order_id, service_id)
    # steps.OrderSteps.transfer([{'order_id': dpt_order_id, 'qty_old': QTY, 'qty_new': QTY / 2, 'all_qty': 0}],[{'order_id': child_order_id, 'qty_delta': 1}])


if __name__ == "__main__":
    test_1()
