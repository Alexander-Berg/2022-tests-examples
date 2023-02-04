# -*- coding: utf-8 -*-

__author__ = 'sandyk'

import datetime

import allure
import pytest

import balance.balance_api as api
from balance import balance_steps as steps
from balance.features import Features

DT = datetime.datetime.now()
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
SERVICE_ID = 129
NON_CURRENCY_PRODUCT_ID = 508896
FIRM_ID = 1
MAIN_DT = datetime.datetime.now()
SHIPMENT_DT = MAIN_DT.replace(minute=0, hour=0, second=0, microsecond=0)
QTY = 49999.99
START_DT = str(datetime.datetime.now().strftime("%Y-%m-%d")) + 'T00:00:00'


# 502962
@pytest.mark.slow
@allure.feature(Features.OVERDRAFT)
@pytest.mark.tickets('BALANCE-22004 ')
def test_fair_overdraft_mv_client():
    client_id = steps.ClientSteps.create(params={'IS_AGENCY': 0})
    # steps.ClientSteps.link(client_id,'yndx-web-test-uid-1')
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    contract_id = None
    # contract_id = steps.ContractSteps.create_contract_new('no_agency', {'CLIENT_ID': client_id,
    #                                                                              'PERSON_ID': person_id,
    #                                                                             'IS_FIXED': START_DT, 'DT': START_DT, 'IS_SIGNED': START_DT,
    #                                                                               'FIRM': FIRM_ID, 'SERVICES':[SERVICE_ID],
    #                                                                         'PAYMENT_TYPE':2,
    #     'UNILATERAL':1,
    #  'CURRENCY':810})[0]

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': 403026233})

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY
            , 'BeginDT': DT
         }
    ]

    api.medium().GetPersonalAccount(
        {'PersonID': person_id, 'PaysysID': 1128, 'FirmID': FIRM_ID, 'ProductID': NON_CURRENCY_PRODUCT_ID})
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list
                                           , additional_params=dict(InvoiceDesireType='charge_note',
                                                                    FirmID=FIRM_ID))
    ##InvoiceDesireType = 'charge_note',
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Money': 90}, 0, campaigns_dt=MAIN_DT)
    # steps.ActsSteps.generate(client_id, 1, MAIN_DT)
