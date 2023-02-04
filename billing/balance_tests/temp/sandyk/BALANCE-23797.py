# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

DT = datetime.datetime.now() - datetime.timedelta(days=1)
PERSON_TYPE = 'ur'
PAYSYS_ID = 1003

SERVICE_ID = 67
PRODUCT_ID = 2584
OVERDRAFT_LIMIT = 120
MAIN_DT = datetime.datetime.now()
QTY = 20
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'


# @pytest.mark.slow
# @reporter.feature(Features.OVERDRAFT)
# @pytest.mark.tickets('BALANCE-23797')
# def test_fair_overdraft_mv_client():
#     client_id = steps.ClientSteps.create()
#     person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
#
#     service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
#     order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
#     orders_list = [
#         {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
#     ]
#     ##выставляем счет
#     request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=DT))
#     invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                  credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
#     steps.InvoiceSteps.pay(invoice_id)
#     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Shows': 10}, 0, campaigns_dt=MAIN_DT)
#     # steps.ActsSteps.generate(client_id, 1, MAIN_DT, True)



def test_fair_overdraft_mv_client():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    contract_id = steps.ContractSteps.create_contract_new('opt_client', {'CLIENT_ID': client_id,
                                                                         'PERSON_ID': person_id,
                                                                         'IS_FAXED': START_DT, 'DT': START_DT,
                                                                         'FIRM': 1, 'SERVICES': [SERVICE_ID],
                                                                         'PAYMENT_TYPE': 3})[0]

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': MAIN_DT}
    ]
    ##выставляем счет
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=MAIN_DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Shows': 10}, 0, campaigns_dt=MAIN_DT)
    steps.ActsSteps.generate(client_id, 1, MAIN_DT, True)


if __name__ == "__main__":
    test_fair_overdraft_mv_client()
