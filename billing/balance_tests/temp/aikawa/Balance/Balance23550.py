import datetime

from balance import balance_steps as steps

dt = datetime.datetime.now()


def test1():
    PERSON_TYPE = 'ua'
    PAYSYS_ID = 1017
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    MSR = 'Bucks'
    contract_type = 'ukr_agent'

    agency_id = 1013041
    # agency_id = steps.ClientSteps.create({'IS_AGENCY':1})
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt,}
    ]
    request_id = steps.RequestSteps.create(client_id=agency_id, orders_list=orders_list)

    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                                         'DT': '2015-04-30T00:00:00',
                                                                         'FINISH_DT': '2016-10-30T00:00:00',
                                                                         'IS_SIGNED': '2015-01-01T00:00:00',
                                                                         'SERVICES': [7],
                                                                         'BANK_DETAILS_ID': 610})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)

    print steps.ActsSteps.generate(client_id, force=1, date=dt)

    # steps.CommonSteps.export('OEBS', 'Act', act_id)
