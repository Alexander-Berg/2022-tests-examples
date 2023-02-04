__author__ = 'aikawa'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils as ut

pytestmark = [pytest.mark.priority('mid')
    , reporter.feature(Features.ACT)
              ]

dt = datetime.datetime.now()
start_dt_current_month, end_dt_current_month = ut.Date.current_month_first_and_last_days(dt)
start_dt_next_month, end_dt_next_month = ut.Date.next_month_first_and_last_days()
contract_finish_dt = (datetime.datetime.now() + datetime.timedelta(days=1))

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'
contract_type = 'no_agency_post'


def test_cut_agava_active_consume_check():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)

    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': start_dt_current_month},
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': start_dt_current_month}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=start_dt_current_month))

    contract_id, _ = steps.ContractSteps.create_contract(
        contract_type,
        {
            'CLIENT_ID': client_id, 'PERSON_ID': person_id,
            'DT': ut.Date.date_to_iso_format(start_dt_current_month),
            'FINISH_DT': ut.Date.date_to_iso_format(contract_finish_dt),
            'IS_SIGNED': ut.Date.date_to_iso_format(start_dt_current_month),
            'SERVICES': [7],
            'REPAYMENT_ON_CONSUME': 1
        }
    )

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID, credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, start_dt_current_month)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 300}, 0, start_dt_next_month)

    steps.ActsSteps.generate(client_id, force=1, date=end_dt_current_month)


