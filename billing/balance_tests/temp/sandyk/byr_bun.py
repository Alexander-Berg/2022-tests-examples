__author__ = 'sandyk'

import datetime

import balance.balance_db as db
from balance import balance_steps as steps

SERVICE_ID = 37
LOGIN = 'clientuid34'
PRODUCT_ID = 502953
# PRODUCT_ID = 502997
PAYSYS_ID = 1100
PERSON_TYPE = 'byp'
QUANT = 100
MAIN_DT = datetime.datetime.now()
DT = datetime.datetime.now() - datetime.timedelta(days=1)
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'


def test_overdraft_notification():
    # inv=[]
    # for i in range(5):
    #     print '----------------------'+str(i) +'----------------------'
    # agency_id = None
    agency_id = 1693001
    person_id = 663328

    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    invoice_owner = agency_id
    order_owner = client_id
    db.balance().execute(
        'update (select * from t_client where id  =:agency_id) set is_docs_separated = 1,IS_DOCS_DETAILED= 1',
        {'agency_id': client_id})

    # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    # contract_id = steps.ContractSteps.create('no_agency_test', {'client_id': client_id,
    #                                                                              'person_id': person_id,
    #                                                                             'is_faxed': START_DT, 'dt': START_DT,
    #                                                                               'FIRM': 12, 'SERVICES':[7]})
    # steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY', dt=DT)
    # steps.ClientSteps.link(client_id,'clientuid40')
    # client_id =853319
    # person_id = steps.PersonSteps.create(client_id, 'sw_ph', {'email': 'test-balance-notify@yandex-team.ru'})
    # client_id = 9406582
    soi = []
    orders_list = []
    for i in range(5):
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                           {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})

        orders_list.append(
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
        )
        soi.append(service_order_id)
    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=61815, overdraft=0, endbuyer_id=None)
    db.balance().execute(
        'update (select * from T_INVOICE where id  = :invoice_id) set IS_DOCS_DETAILED= 1,IS_DOCS_SEPARATED = 1',
        {'invoice_id': invoice_id})
    steps.InvoiceSteps.pay(invoice_id)
    days = 10
    for i in soi:
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, i,
                                          {'Bucks': 0, 'Days': days, 'Money': 0}, 0, datetime.datetime.now())
        days += 5
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
