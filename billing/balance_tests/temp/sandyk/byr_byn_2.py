__author__ = 'sandyk'

import datetime

import balance.balance_db as db
from balance import balance_steps as steps

SERVICE_ID = 37
LOGIN = 'clientuid34'
PRODUCT_ID = 502953
# PRODUCT_ID = 502997
PAYSYS_ID = 1036
PERSON_TYPE = 'yt'
QUANT = 100
MAIN_DT = datetime.datetime.now() - datetime.timedelta(days=40)
DT = str((datetime.datetime.now() - datetime.timedelta(days=40)).strftime("%Y-%m-%d")) + 'T00:00:00'
START_DT = str((datetime.datetime.now() - datetime.timedelta(days=45)).strftime("%Y-%m-%d")) + 'T00:00:00'


def test_overdraft_notification():
    # agency_id= 1693001
    # person_id = 663328
    # agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})

    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    agency_id = 1693001
    person_id = 663328
    contract_id = 61815
    # person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)
    # contract_id = steps.ContractSteps.create_new('pr_agency', {'client_id': agency_id,
    #                                                                              'person_id': person_id,
    #                                                                             'is_faxed': START_DT, 'dt': START_DT,
    #                                                                               'FIRM': 1, 'SERVICES':[37], 'PAYMENT_TYPE':3,
    #                                                                              'CURRENCY':974,
    #                                                                              'DEAL_PASSPORT': '2016-06-01T00:00:00'})
    invoice_owner = agency_id
    order_owner = client_id
    db.balance().execute(
        'update (select * from t_client where id  =:agency_id) set is_docs_separated = 1,IS_DOCS_DETAILED= 1',
        {'agency_id': client_id})

    soi = []
    orders_list = []
    inv = []
    for r in range(2):
        for i in range(3):
            service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
            order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                               {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})

            orders_list.append(
                {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
            )
            # soi.append(service_order_id)
        request_id = steps.RequestSteps.create(invoice_owner, orders_list,
                                               additional_params={'InvoiceDesireDT': MAIN_DT})

        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                     credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
        inv.append(invoice_id)
        db.balance().execute(
            'update (select * from T_INVOICE where id  = :invoice_id) set IS_DOCS_DETAILED= 1,IS_DOCS_SEPARATED = 1',
            {'invoice_id': invoice_id})

        days = 10
        for i in soi:
            steps.CampaignsSteps.do_campaigns(SERVICE_ID, i,
                                              {'Bucks': 0, 'Days': days, 'Money': 0}, 0, MAIN_DT)
            days += 5
    print inv
    # inv = [54024038]
    # for invoice in inv:
    #     db.balance().execute("update (select * from T_DEFERPAY where invoice_id =:invoice_id) set issue_dt ='20.07.2016 18:24:16'", {'invoice_id':invoice})
    # agency_id = 1693001
    # steps.ActsSteps.generate(agency_id, 1, MAIN_DT)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, 22351873,
    #                                               {'Bucks': 0, 'Days': 50, 'Money': 0}, 0, datetime.datetime.now())

    # steps.InvoiceSteps.pay(54026137)


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
