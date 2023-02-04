__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 42
LOGIN = 'clientuid34'
PRODUCT_ID = 507130
PAYSYS_ID = 1601076
PERSON_TYPE = 'sw_ytph'
QUANT = 100
# MAIN_DT = datetime.datetime.now()
DT = datetime.datetime.now()


# START_DT = str((datetime.datetime.now() - datetime.timedelta(days=31)).strftime("%Y-%m-%d")) + 'T00:00:00'


def test_overdraft_notification():
    # inv=[]
    # for i in range(5):
    #     print '----------------------'+str(i) +'----------------------'
    # agency_id = None
    # agency_id= 1693001
    # person_id = 663328
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    invoice_owner = client_id
    order_owner = client_id
    agency_id = None

    # contract_id = steps.ContractSteps.create('comm_pre', {'client_id': agency_id,
    #                                                                              'person_id': person_id,
    #                                                                             'is_faxed': START_DT, 'dt': START_DT,
    #                                                                               'FIRM': 1, 'SERVICES':[7]})[0]
    orders_list = []
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})

    orders_list.append(
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': DT}
    )

    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': DT})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    #
    #
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,
    #                                       {'Bucks': 50,  'Money': 0}, 0, datetime.datetime.now())
    # steps.ActsSteps.generate(agency_id, 1, datetime.datetime.now())


if __name__ == "__main__":
    test_overdraft_notification()
