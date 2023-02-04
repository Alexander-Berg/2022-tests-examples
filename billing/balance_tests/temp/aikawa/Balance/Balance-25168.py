import datetime
import pytest
from balance import balance_steps as steps

dt = datetime.datetime.now()

SERVICE_ID = 11
PRODUCT_ID = 2136
MSR = 'Bucks'


@pytest.mark.parametrize('params', [
    # {'PERSON_TYPE': 'yt', 'PAYSYS_ID': 11101013},
    # {'PERSON_TYPE': 'yt', 'PAYSYS_ID': 11101014},
    # {'PERSON_TYPE': 'yt', 'PAYSYS_ID': 11101023},
    # {'PERSON_TYPE': 'yt', 'PAYSYS_ID': 11101100},
    {'PERSON_TYPE': 'by_ytph', 'PAYSYS_ID': 1075}])
def test_byn_market(params):
    client_id = steps.ClientSteps.create({'REGION_ID': 149})
    person_id = steps.PersonSteps.create(client_id, params['PERSON_TYPE'])
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=params['PAYSYS_ID'],
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    #
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
    #
    steps.ActsSteps.enqueue([client_id], force=1, date=dt)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
