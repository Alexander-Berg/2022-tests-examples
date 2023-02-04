__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
LOGIN = 'clientuid34'
PRODUCT_ID = 1475
PAYSYS_ID = 1003
PERSON_TYPE = 'ur'
QUANT = 134
MAIN_DT = datetime.datetime.now()
DT = datetime.datetime.now() - datetime.timedelta(days=20)
START_DT = str((datetime.datetime.now() - datetime.timedelta(days=31)).strftime("%Y-%m-%d")) + 'T00:00:00'


def test_overdraft_notification():
    # inv=[]
    # for i in range(5):
    #     print '----------------------'+str(i) +'----------------------'
    # agency_id = None
    # agency_id= 1693001
    # person_id = 663328
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)

    invoice_owner = agency_id
    order_owner = client_id

    contract_id = steps.ContractSteps.create_contract('comm_pre', {'CLIENT_ID': agency_id,
                                                                   'PERSON_ID': person_id,
                                                                   'IS_FAXED': START_DT, 'DT': START_DT,
                                                                   'FIRM': 1, 'SERVICES': [7]})[0]
    orders_list = []
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})

    orders_list.append(
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': DT}
    )

    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': DT})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,
                                      {'Bucks': 50, 'Money': 0}, 0, datetime.datetime.now())
    steps.ActsSteps.generate(agency_id, 1, datetime.datetime.now())

    # bucks = decimal.Decimal(0.2).quantize(decimal.Decimal('.01'))

    # for i in range(5):
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,
    #                                           {'Bucks': 0, 'Days': bucks, 'Money': 0}, 0, datetime.datetime.now())
    #     steps.ActsSteps.generate(client_id, 1, datetime.datetime.now())
    #     bucks += decimal.Decimal(0.2).quantize(decimal.Decimal('.01'))

    # inv.append(invoice_id)


    #
    # steps.InvoiceSteps.pay(invoice_id, None, None)


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
