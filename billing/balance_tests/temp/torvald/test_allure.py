# -*- coding: utf-8 -*-

import datetime

import btestlib.reporter as reporter
from balance import balance_steps as steps

pytest_plugins = "allure.pytest_plugin"

SERVICE_ID = 7
# PRODUCT_ID = 502953 ##502918
PRODUCT_ID = 1475
PAYSYS_ID = 1003
QTY = 10
BASE_DT = datetime.datetime.now()

# def test_1 ():
#     client_id = None or steps.ClientSteps.create({})
#     with pytest.reporter.step('Create client with params:'):
#         client_id = None or steps.ClientSteps.create({'IS_AGENCY': 0})

def test_simple_client ():
    with reporter.step('1 step'):
        client_id = None or steps.ClientSteps.create()
        # db.balance().execute('''Update t_client set FULLNAME = u'UL Nonres', CURRENCY_PAYMENT = 'USD', ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1
        #                     where ID = :client_id ''',
        #         {'client_id':client_id})
        agency_id = None
        # agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})
        person_id = None or steps.PersonSteps.create(client_id, 'ur')

        # contract_id, _ = steps.ContractSteps.create('comm_post',{'client_id': agency_id, 'person_id': person_id,
        #                                                    'dt'       : '2015-04-30T00:00:00',
        #                                                    'FINISH_DT': '2016-06-30T00:00:00',
        #                                                    'is_signed': '2015-01-01T00:00:00',
        #                                                    'SERVICES': [7,11],
        #                                                    # 'COMMISSION_TYPE': 57,
        #                                                    'NON_RESIDENT_CLIENTS': 0,
        #                                                    'REPAYMENT_ON_CONSUME': 0,
        #                                                    'PERSONAL_ACCOUNT': 1,
        #                                                    'LIFT_CREDIT_ON_PAYMENT': 1,
        #                                                    'PERSONAL_ACCOUNT_FICTIVE': 1
        #                                                    })

        contract_id = None

    with reporter.step('2 step'):
        # service_order_id = 21288706
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                params={'AgencyID': agency_id})
        service_order_id2 = steps.OrderSteps.next_id(SERVICE_ID)
        steps.OrderSteps.create(client_id, service_order_id2, service_id=SERVICE_ID, product_id=PRODUCT_ID)
        # service_order_id3 = steps.OrderSteps.next_id(SERVICE_ID)
        # steps.OrderSteps.create(client_id, service_order_id3, service_id=SERVICE_ID, product_id=PRODUCT_ID)
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
            , {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id2, 'Qty': QTY, 'BeginDT': BASE_DT}
        ]
        request_id = steps.RequestSteps.create(client_id, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                     overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)

        steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 20, 'Money': 0}, 0, BASE_DT)
        # steps.CampaignsSteps.do_campaigns(SERVICE_ID ,service_order_id2, {'Bucks': 0, 'Money': 0}, 0, BASE_DT)
        steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)

if __name__ == "__main__":
    # pytest.main()
    test_simple_client()