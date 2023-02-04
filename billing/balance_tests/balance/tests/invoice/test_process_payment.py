# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime

import balance.balance_steps as steps
from btestlib.constants import PersonTypes, Paysyses, Services

PRODUCT_ID = 506994
ORDER_DT = datetime.now()


# a-vasin: это здесь существует только ради того, чтобы увидеть, что PROCESS_PAYMENT успешно отработал
# взято из temp/aikawa/Balance/balance_24545.py

def test_process_payment():
    client_id = steps.ClientSteps.create()

    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    service_order_id = steps.OrderSteps.next_id(service_id=Services.AUTORU.id)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=Services.AUTORU.id,
                                       service_order_id=service_order_id,
                                       params={'AgencyID': None, 'ActText': 'blabla'})

    service_order_id2 = steps.OrderSteps.next_id(service_id=Services.AUTORU.id)
    order_id2 = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=Services.AUTORU.id,
                                        service_order_id=service_order_id2, params={'AgencyID': None, 'ActText': 'bla'})

    orders_list = [
        {'ServiceID': Services.AUTORU.id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': ORDER_DT},
        {'ServiceID': Services.AUTORU.id, 'ServiceOrderID': service_order_id2, 'Qty': 100, 'BeginDT': ORDER_DT}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay_fair(invoice_id)
