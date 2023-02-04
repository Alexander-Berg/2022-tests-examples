# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features

SERVICE_ID = 35
PRODUCT_ID = 506540
PAYSYS_ID = 1001
# PERSON_TYPE = 'ph_autoru'
QUANT = 10
MAIN_DT = datetime.datetime.now()
START_DT = str(datetime.datetime.today().strftime("%Y-%m-%d")) + 'T00:00:00'


@reporter.feature(Features.INVOICE)
@pytest.mark.tickets('BALANCE-22576', 'TESTBALANCE-1316')
def test_SERVICE_82_smoke():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, 'ph')
    contract_id = steps.ContractSteps.create_contract('no_agency_test', {'CLIENT_ID': client_id,
                                                                         'PERSON_ID': person_id,
                                                                         'IS_FAXED': START_DT, 'DT': START_DT,
                                                                         'FIRM': 12, 'SERVICES': [35]})

    person_id = steps.PersonSteps.create(client_id, 'ph')
    contract_id = steps.ContractSteps.create_contract('no_agency_test', {'CLIENT_ID': client_id,
                                                                         'PERSON_ID': person_id,
                                                                         'IS_FAXED': START_DT, 'DT': START_DT,
                                                                         'FIRM': 12, 'SERVICES': [35]})

    # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
    #                                    {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    #
    # orders_list = [
    #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': MAIN_DT}
    # ]
    # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': MAIN_DT})
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
    #                                              credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    # client_id = 13874635
    # service_order_id = 22434102
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id,
    #                                           {'Bucks': 0, 'Days': 10, 'Money': 0}, 0, MAIN_DT)
    # steps.ActsSteps.generate(client_id, 0, MAIN_DT)

if __name__ == "__main__":
    test_SERVICE_82_smoke()