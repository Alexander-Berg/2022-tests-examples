import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7


def UA_FISH_1():

    client_id = 11460404
    person_id = 4143127

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': 46834231, 'Qty': 100, 'BeginDT': dt}
        # ,{'ServiceID': SERVICE_ID, 'ServiceOrderID': 46834241, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(31666427, [31666426])

# UA_FISH_1()

def UA_FISH_2():


    client_id = 1353760
    person_id = 4143496

    orders_list = [
        # {'ServiceID': SERVICE_ID, 'ServiceOrderID': 46834196, 'Qty': 100, 'BeginDT': dt}
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': 46834201, 'Qty': 100, 'BeginDT': dt}
        ,{'ServiceID': SERVICE_ID, 'ServiceOrderID': 46834206, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(31666406, [31666407, 31666408])

UA_FISH_2()