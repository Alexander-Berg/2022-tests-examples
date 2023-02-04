__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 7
LOGIN = 'clientuid34'
PRODUCT_ID = 1475
PAYSYS_ID = 1066
PERSON_TYPE = 'sw_yt'
QUANT = 500
MAIN_DT = datetime.datetime.now()
DT = datetime.datetime.now() - datetime.timedelta(days=25)
START_DT = str((datetime.datetime.now() - datetime.timedelta(days=31)).strftime("%Y-%m-%d")) + 'T00:00:00'


##usd 8538 (yt)  7905(rez)
##eur 6415 (yt)  5940 (rez)
##chf 8538 (yt)   7905(rez)

def test_overdraft_notification():
    #### agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    # steps.ClientSteps.link(client_id,'clientuid45')
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    invoice_owner = client_id
    order_owner = client_id

    # contract_id = steps.ContractSteps.create('shv_client', {'client_id': client_id,
    #                                                                              'person_id': person_id,
    #                                                                             'is_faxed': START_DT, 'dt': START_DT,
    #                                                                               'FIRM': 7, 'SERVICES':[7],
    #                                                                              'CURRENCY':756})[0]
    orders_list = []
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})

    orders_list.append(
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': DT}
    )

    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': DT})

    # steps.CLoseMonth.UpdateLimits(datetime.datetime(2016,7,31),1,[14300826])

    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)

    #
    #
    # service_order_id = 85135811
    # client_id = 14303520
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,
    #                                       {'Bucks': 5,  'Money': 0}, 0, datetime.datetime.now())
    # steps.ActsSteps.generate(client_id, 1, datetime.datetime.now())

    # bucks = decimal.Decimal(0.2).quantize(decimal.Decimal('.01'))
    #
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
