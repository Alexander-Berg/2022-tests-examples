# -*- coding: utf-8 -*-

import datetime

import pytest

from balance import balance_steps as steps

SERVICE_ID = 35
PRODUCT_ID = 506525 #505123
PAYSYS_ID = 1014
QTY = 1000
BASE_DT = datetime.datetime.now()

def test_method():
    # client_params = {'REGION_ID': '225', 'CURRENCY': 'RUB', 'MIGRATE_TO_CURRENCY': datetime.datetime(2000, 1, 1),
    #                  'SERVICE_ID': SERVICE_ID}
    client_params = {}
    client_id = None or steps.ClientSteps.create(client_params)
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
    order_owner = client_id
    invoice_owner = agency_id if agency_id is not None else client_id

    person_id = None or steps.PersonSteps.create(invoice_owner, 'yt')
    contract_id = None

    # steps.ClientSteps.set_force_overdraft(client_id, SERVICE_ID, 10000, 10, BASE_DT, 'RUB')


    # contract_id, _ = steps.ContractSteps.create('auto_ru_non_agency_post', {'client_id': invoice_owner, 'person_id': person_id,
    #                                                                         'dt': '2015-04-30T00:00:00',
    #                                                                         'FINISH_DT': '2016-06-30T00:00:00',
    #                                                                         'is_signed': '2015-01-01T00:00:00',
    #                                                                         # 'SERVICES': [7,11],
    #                                                                         # 'NON_RESIDENT_CLIENTS': 0,
    #                                                                         # 'REPAYMENT_ON_CONSUME': 0,
    #                                                                         # 'PERSONAL_ACCOUNT': 1,
    #                                                                         # 'LIFT_CREDIT_ON_PAYMENT': 0,
    #                                                                         # 'PERSONAL_ACCOUNT_FICTIVE': 0
    #                                                                         })

    # service_order_id = 21288706
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                            params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 20, 'Money': 0}, 0, BASE_DT)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID ,service_order_id2, {'Bucks': 0, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)

if __name__ == "__main__":
    pytest.main()
    # test_simple_client()
