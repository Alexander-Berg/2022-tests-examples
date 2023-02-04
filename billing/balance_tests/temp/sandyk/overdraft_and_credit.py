# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
NON_CURRENCY_PRODUCT_ID = 1475
OVERDRAFT_LIMIT = 120
MAIN_DT = datetime.datetime.now()
QTY = 10
dt = datetime.datetime.now()
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'


@pytest.mark.slow
@reporter.feature(Features.OVERDRAFT)
@pytest.mark.tickets('BALANCE-22004')
def test_fair_overdraft_mv_client():
    # agency_id = 862606
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    agency_id = None
    # steps.ClientSteps.set_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, firm_id=1, start_dt=MAIN_DT,
    #                                             currency=None, invoice_currency=None)
    steps.OverdraftSteps.set_force_overdraft(client_id, service_id=SERVICE_ID, limit=OVERDRAFT_LIMIT, firm_id=1,
                                             start_dt=MAIN_DT)
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    # contract_id = steps.ContractSteps.create('no_agency_test', {'client_id': client_id,
    #                                                                              'person_id': person_id,
    #                                                                             'is_faxed': START_DT, 'dt': START_DT,
    #                                                                               'FIRM': 1, 'SERVICES':[7]})[0]
    # client_id  = 14028847
    #
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=MAIN_DT))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)
    #
    #
    #     # api.test_balance().Enqueue('Client', client_id, 'OVERDRAFT')
    #     # steps.CommonSteps.export('OVERDRAFT', 'Client', client_id)
    #
    #     # service_order_id = 80067124\
    #     # steps.InvoiceSteps.pay(in)
    # #     client_id = 14145563
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': QTY / 2}, 0, dt)
    steps.ActsSteps.generate(client_id, 1, dt)


# #

###########################################
# client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
# agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
# person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)
# contract_id = steps.ContractSteps.create('opt_agency_prem_post', {'client_id': agency_id,
#                                                                              'person_id': person_id,
#                                                                             'is_faxed': START_DT, 'dt': START_DT,
#                                                                               'FIRM': 12, 'SERVICES':[SERVICE_ID]})[0]
# # client_id =14028913
# service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
# order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID,
#                                      {'TEXT':'Py_Test order','AgencyID' : agency_id, 'ManagerUID': None})
# orders_list = [
#     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': MAIN_DT}
# ]
# request_id = steps.RequestSteps.create(client_id=agency_id, orders_list=orders_list,
#                                        additional_params=dict(InvoiceDesireDT=MAIN_DT))
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
#                                                  credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
#
# # client_id = 14150515
# # service_order_id = 80082882
# #
# steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': QTY/2}, 0, dt)
# steps.ActsSteps.generate(agency_id, 1, dt)


if __name__ == "__main__":
    test_fair_overdraft_mv_client()
