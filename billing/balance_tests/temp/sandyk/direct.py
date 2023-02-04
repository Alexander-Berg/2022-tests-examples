__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID =7
LOGIN = 'clientuid45'
PRODUCT_ID= 1475
PAYSYS_ID = 1001
PERSON_TYPE = 'ph'
QUANT = 10
MAIN_DT = datetime.datetime.now()
DT = datetime.datetime.now() - datetime.timedelta(days=1)
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'


def test_overdraft_notification():
    # inv=[]
    # for i in range(5):
    #     print '----------------------'+str(i) +'----------------------'
    # client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    # # steps.ClientSteps.link(client_id,LOGIN)
    # # client_id = 1693001
    # # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # # contract_id = steps.ContractSteps.create('no_agency_test', {'client_id': client_id,
    # #                                                                              'person_id': person_id,
    # #                                                                             'is_faxed': START_DT, 'dt': START_DT,
    # #                                                                               'FIRM': 1, 'SERVICES':[7]})
    # # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # # contract_id = steps.ContractSteps.create('no_agency_test', {'client_id': client_id,
    # #                                                                              'person_id': person_id,
    # #                                                                             'is_faxed': START_DT, 'dt': START_DT,
    # #                                                                               'FIRM': 12, 'SERVICES':[7]})
    # # steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY', dt=DT)
    # # steps.ClientSteps.link(client_id,'clientuid40')
    # # client_id =853319
    # # person_id = steps.PersonSteps.create(client_id, 'sw_ph', {'email': 'test-balance-notify@yandex-team.ru'})
    # # client_id = 14016492
    #
    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
    #     {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
    #
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    # ]
    # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})

    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

        # inv.append(invoice_id)
    # print inv
    client_id = 14149485
    invoice_id = 52548586
    service_order_id = 80082803
    steps.InvoiceSteps.pay(invoice_id)  # service_order_id = 80052261
    # client_id = 13939330
    # bucks = 1000
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 5, 'Money': 0}, 0,
                                      datetime.datetime.now())
    steps.ActsSteps.generate(client_id, 1, datetime.datetime.now())


    # invoice_id = 52545707
    # service_order_id  = 80082737
    # client_id = 14146841
    # # steps.InvoiceSteps.pay(invoice_id, None, None)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 5, 'Money': 0}, 0, datetime.datetime.now())
    # steps.ActsSteps.generate(client_id, 1, datetime.datetime.now())

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