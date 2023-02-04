__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps

SERVICE_ID = 11
LOGIN = 'clientuid34'
# PAYSYS_ID = 11101003
# PERSON_TYPE = 'ur'
QUANT = 100
MAIN_DT = datetime.datetime(2016, 8, 21)
# MAIN_DT = datetime.datetime.now()


# PAYSYS_ID = 1201003
# PERSON_TYPE = 'tru'
# PRODUCT_ID= 1475

### 7(40)
PAYSYS_ID = 11101013
PERSON_TYPE = 'yt'
PRODUCT_ID= 2136


# PAYSYS_ID=11101023

###5(39)  rub
# PAYSYS_ID = 1014
# PERSON_TYPE = 'yt'
# PRODUCT_ID= 2136

###13(41)   https://balance.greed-ts1f.yandex.ru/success.xml?invoice_id=52235555
# PAYSYS_ID = 11101003
# PERSON_TYPE = 'ur'
# PRODUCT_ID= 2136


###20(42)
# PAYSYS_ID = 11101060
# PERSON_TYPE = 'yt_kzu'
# PRODUCT_ID= 2136

###29(43)  https://balance.greed-ts1f.yandex.ru/success.xml?invoice_id=52235591
# PAYSYS_ID = 11101003
# PERSON_TYPE = 'ur'
# PRODUCT_ID= 506537
#
###30 (44)
# PAYSYS_ID = 11101014
# PERSON_TYPE = 'yt'
# PRODUCT_ID= 506537

### ph    https://balance.greed-ts1f.yandex.ru/invoice-publish.xml?ft=html&object_id=52235648
# PAYSYS_ID = 11101001
# PERSON_TYPE = 'ph'
# PRODUCT_ID= 2136

def test_overdraft_notification():
    # agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    # # # client_id = 13497857
    # # steps.ClientSteps.link(client_id,'clientuid40')
    # client_id = 14322801
    # agency_id = 14322800
    # person_id = 4426047
    # contract_id=  438854
    invoice_owner = client_id
    order_owner = client_id
    person_id = steps.PersonSteps.create(invoice_owner, PERSON_TYPE)
    #
    contract_id = None
    agency_id = None
    # contract_id,_ =  steps.ContractSteps.create('opt_agency_post',{'client_id': invoice_owner, 'person_id': person_id,
    #                                                'dt'       : '2016-05-01T00:00:00',
    #                                                # 'FINISH_DT': None,
    #                                                'is_signed': '2016-05-01T00:00:00'
    #                                                # 'is_signed': None,
    #                                                ,'SERVICES': [11]
    #                                                ,'FIRM': 111
    #                                                # ,'SCALE':3
    #                                                })
    # # # person_id = steps.PersonSteps.create(client_id, 'sw_ph', {'email': 'test-balance-notify@yandex-team.ru'})
    # #
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                        {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)

    # steps.InvoiceSteps.pay(invoice_id, None, None)
    # service_order_id = 22496040
    # client_id  = 14322800
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 20, 'Money': 0}, 0, datetime.datetime.now())
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