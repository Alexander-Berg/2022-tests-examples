import datetime

import balance.balance_api as api
from balance import balance_steps as steps

dt = datetime.datetime.now()

SERVICE_ID = 7

contract_type = 'no_agency_post'


def pay_test_1():
    param_list = [
        {'person_category': 'yt', 'paysys_id': 1036, 'region_id': 149, 'currency': 'BYR', 'product_id': 507255, 'currency_id': 974},
        {'person_category': 'yt', 'paysys_id': 1100, 'region_id': 149, 'currency': 'BYN', 'product_id': 507256, 'currency_id': 933}]
    for param in param_list:
        client_id = steps.ClientSteps.create_multicurrency(currency=param['currency'], region_id=149)
        person_id = steps.PersonSteps.create(client_id, param['person_category'])
        service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        steps.OrderSteps.create(client_id=client_id, product_id=param['product_id'], service_id=SERVICE_ID, service_order_id=service_order_id)
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

        contract_id, _ = steps.ContractSteps.create_contract(contract_type,
                                                             {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                              'DT': '2015-04-30T00:00:00',
                                                              'FINISH_DT': '2016-06-30T00:00:00',
                                                              'IS_SIGNED': '2015-01-01T00:00:00',
                                                              'SERVICES': [7],
                                                              'CURRENCY': param['currency_id'],
                                                              'FIRM': '1',
                                                              'CREDIT_CURRENCY_LIMIT': param['currency_id'],
                                                              'DEAL_PASSPORT': '2015-01-01T00:00:00'})

        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=param['paysys_id'], credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Money': 500}, 0, dt)
        steps.ActsSteps.generate(client_id, force=1, date=dt)


pay_test_1()


def pay_test_2():
    region_id = 149
    param_list = [
        {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 149}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 167}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 168}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 169}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 170}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 171}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 207}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 208}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 209}
        , {'person_category': 'by_ytph', 'paysys_id': 1075, 'region_id': 29386}
    ]
    for param in param_list:
        client_id = steps.ClientSteps.create({'REGION_ID': param['region_id']})
        person_id = steps.PersonSteps.create(client_id, param['person_category'])
        service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
        steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)
        orders_list = [
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 500, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=param['paysys_id'],
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id)


# pay_test_2()


def pay_test_3():
    print api.medium().GetCurrencyProducts(7)
    print api.medium().GetDirectBalance()

# pay_test_3()
