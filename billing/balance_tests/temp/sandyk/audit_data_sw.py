# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

from balance import balance_steps as steps
from balance.features import AuditFeatures
from btestlib import reporter

pytestmark = [pytest.mark.audit, reporter.feature(AuditFeatures.FR_C22)]

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1047
PERSON_TYPE = 'sw_yt'
QUANT = 980
MAIN_DT = datetime.datetime(2017, 12, 10)
DT = datetime.datetime(2017, 11, 15)
START_DT = str((datetime.datetime(2017, 11, 10)).strftime("%Y-%m-%d")) + 'T00:00:00'


def test_nonrez():
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    invoice_owner = client_id
    order_owner = client_id

    contract_id = steps.ContractSteps.create_contract_new('sw_opt_client',
                                                          {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                           'IS_FAXED': START_DT, 'DT': START_DT, 'FIRM': 7,
                                                           'SERVICES': [
                                                               7], 'CURRENCY': 840, 'PAYMENT_TYPE': 2})[0]
    orders_list = []
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})

    orders_list.append({'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': DT})

    request_id = steps.RequestSteps.create(invoice_owner, orders_list, additional_params={'InvoiceDesireDT': DT})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, None, DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 370, 'Money': 0}, 0, MAIN_DT)
    act_id = steps.ActsSteps.generate(client_id, 1, MAIN_DT)[0]
    steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, contract_id=contract_id,
                                  invoice_id=invoice_id, act_id=act_id)
