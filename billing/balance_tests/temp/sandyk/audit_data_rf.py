# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import reporter

pytestmark = [pytest.mark.audit, reporter.feature(AuditFeatures.FR_C22)]

PERSON_TYPE = 'yt'
PAYSYS_ID = 1023
SERVICE_ID = 7
PRODUCT_ID = 1475
MAIN_DT = datetime.datetime(2017, 12, 20)
DT = datetime.datetime(2017, 11, 15)
START_DT = str((datetime.datetime.now() - datetime.timedelta(days=120)).strftime("%Y-%m-%d")) + 'T00:00:00'
QTY = 400


def test_resident_RF():
    agency_id = steps.ClientSteps.create(params={'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create(params={'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(agency_id, 'yt')
    contract_id = steps.ContractSteps.create_contract_new('commiss',
                                                          {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                           'IS_FIXED': START_DT, 'DT': START_DT, 'IS_SIGNED': START_DT,
                                                           'FIRM': 1, 'SERVICES': [
                                                              SERVICE_ID], 'PAYMENT_TYPE': 2,
                                                           'UNILATERAL': 1,
                                                           'CURRENCY': 978,
                                                           'DEAL_PASSPORT': START_DT})[
        0]
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}, ]
    request_id = steps.RequestSteps.create(client_id=agency_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, None, DT)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 150}, 0, campaigns_dt=MAIN_DT)
    act_id = steps.ActsSteps.generate(agency_id, 1, MAIN_DT)[0]
    # steps.ExportSteps.export_oebs(act_id=act_id)
    # steps.ExportSteps.export_oebs(client_id=60555880, person_id=6588235, contract_id=970289,
    #                               invoice_id=70190374, act_id=71550277)
