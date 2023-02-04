# -*- coding: utf-8 -*-
from datetime import date

__author__ = 'sandyk'

import datetime

import allure
import pytest

import balance.balance_db as db
from balance import balance_steps as steps
from balance.features import Features

DT = datetime.datetime.now()
PERSON_TYPE = 'ur'
PAYSYS_ID =1003
SERVICE_ID =7
NON_CURRENCY_PRODUCT_ID =  1475
# OVERDRAFT_LIMIT = 1000
MAIN_DT = datetime.datetime.now()

QTY = 100
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'

# 502962
@pytest.mark.slow
@allure.feature(Features.OVERDRAFT)
@pytest.mark.tickets('BALANCE-22004')
def test_fair_overdraft_mv_client():
    client_id = steps.ClientSteps.create(params={'IS_AGENCY':1})
    #
    # # steps.ClientSteps.link(client_id,'clientuid45')
    person_id = steps.PersonSteps.create(client_id, 'ur')
    # # contract_id = None
    contract_id = steps.ContractSteps.create_contract_new('pr_agency', {'CLIENT_ID': client_id,
                                                                                 'PERSON_ID': person_id,
                                                                                'IS_FIXED': START_DT, 'DT': START_DT, 'IS_SIGNED': START_DT,
                                                                                  'FIRM': 1, 'SERVICES':[SERVICE_ID],
                                                                            'PAYMENT_TYPE':2,
        'UNILATERAL':1,
     'CURRENCY':810})[0]

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID ,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
    ]

    ##выставляем счет
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=DT))


    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
###############################
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}]

    ##выставляем счет
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)



if __name__ == "__main__":
    test_fair_overdraft_mv_client()