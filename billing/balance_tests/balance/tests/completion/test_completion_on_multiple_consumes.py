# -*- coding: utf-8 -*-

__author__ = 'torvald'

import datetime
import time
from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features
from btestlib import matchers as mtch
from btestlib import utils as utils

pytestmark = [pytest.mark.priority('mid')
    , reporter.feature(Features.COMPLETION, Features.MULTICURRENCY)
              ]

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

PRODUCT = Product(7, 1475, 'Bucks', 'Money')
PAYSYS_ID = 1003
QTY = D('118')
BASE_DT = datetime.datetime.now()

manager_uid = '244916211'


@pytest.mark.smoke
def test_completion_on_multiple_consumes_fish():
    QTY_LIST = [D('8.233247'), D('100'), D('0.171717')]
    COMPLETION = D('107.917')
    client_id = None or steps.ClientSteps.create()
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})

    order_owner = client_id
    invoice_owner = agency_id or client_id

    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('opt_agency_prem_post', {'CLIENT_ID': invoice_owner,
                                                                                  'PERSON_ID': person_id,
                                                                                  'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                  'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                                  'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                  'SERVICES': [7],
                                                                                  'FIRM': 1,
                                                                                  # 'COMMISSION_TYPE': 48,
                                                                                  'NON_RESIDENT_CLIENTS': 0,
                                                                                  # 'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                  'REPAYMENT_ON_CONSUME': 0,
                                                                                  'PERSONAL_ACCOUNT': 1,
                                                                                  'LIFT_CREDIT_ON_PAYMENT': 1,
                                                                                  'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                                                  'CREDIT_LIMIT_SINGLE': 500000
                                                                                  })

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(PRODUCT.service_id)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=PRODUCT.service_id,
                                       product_id=PRODUCT.id,
                                       params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
    for qty in QTY_LIST:
        orders_list = [
            {'ServiceID': PRODUCT.service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': BASE_DT}]
        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1,
                                                     contract_id=contract_id,
                                                     overdraft=0, endbuyer_id=None)

    # steps.CampaignsSteps.update_campaigns(PRODUCT.service_id, service_order_id, {PRODUCT.shipment_type: COMPLETION}, 0, BASE_DT)
    # steps.CommonSteps.wait_for_export('PROCESS_COMPLETIONS', order_id)
    steps.CampaignsSteps.do_campaigns(PRODUCT.service_id, service_order_id, {PRODUCT.shipment_type: COMPLETION}, 0,
                                      BASE_DT)

    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)

    expected = [{'completion_qty': QTY_LIST[0]},
                {'completion_qty': COMPLETION - QTY_LIST[0]}]
    utils.check_that(db.get_consumes_by_invoice(invoice_id), mtch.contains_dicts_with_entries(expected))


@pytest.mark.smoke
def test_completion_on_multiple_consumes_multicurrency():
    QTY_LIST = [D('8.233247'), D('100'), D('0.171717')]
    BUCKS_COMPLETION = D('20.917')
    MONEY_COMPLETION = D('2553.15')
    client_id = None or steps.ClientSteps.create()
    agency_id = None or steps.ClientSteps.create({'IS_AGENCY': 1})

    order_owner = client_id
    invoice_owner = agency_id or client_id

    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    contract_id, _ = steps.ContractSteps.create_contract('opt_agency_prem_post', {'CLIENT_ID': invoice_owner,
                                                                                  'PERSON_ID': person_id,
                                                                                  'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                  'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                                  'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                  'SERVICES': [7],
                                                                                  'FIRM': 1,
                                                                                  # 'COMMISSION_TYPE': 48,
                                                                                  'NON_RESIDENT_CLIENTS': 0,
                                                                                  # 'DEAL_PASSPORT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                  'REPAYMENT_ON_CONSUME': 0,
                                                                                  'PERSONAL_ACCOUNT': 1,
                                                                                  'LIFT_CREDIT_ON_PAYMENT': 1,
                                                                                  'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                                                  'CREDIT_LIMIT_SINGLE': 500000
                                                                                  })

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(PRODUCT.service_id)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=PRODUCT.service_id,
                                       product_id=PRODUCT.id,
                                       params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
    for qty in QTY_LIST:
        orders_list = [
            {'ServiceID': PRODUCT.service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': BASE_DT}]
        request_id = steps.RequestSteps.create(invoice_owner, orders_list)
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=1,
                                                     contract_id=contract_id,
                                                     overdraft=0, endbuyer_id=None)

    # steps.CampaignsSteps.update_campaigns(PRODUCT.service_id, service_order_id, {PRODUCT.shipment_type: COMPLETION}, 0, BASE_DT)
    # steps.CommonSteps.wait_for_export('PROCESS_COMPLETIONS', order_id)
    completions = {PRODUCT.shipment_type: BUCKS_COMPLETION}
    steps.CampaignsSteps.do_campaigns(PRODUCT.service_id, service_order_id, completions, 0)

    steps.ClientSteps.create(
        {'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'SERVICE_ID': PRODUCT.service_id,
         'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
         'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    time.sleep(5)
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)

    completions = {PRODUCT.shipment_type: BUCKS_COMPLETION, PRODUCT.second_shipment_type: MONEY_COMPLETION}
    steps.CampaignsSteps.do_campaigns(PRODUCT.service_id, service_order_id, completions, 0)

    expected = [{'completion_qty': QTY_LIST[0]},
                {'completion_qty': (MONEY_COMPLETION / 30 + BUCKS_COMPLETION) - QTY_LIST[0]}]
    utils.check_that(db.get_consumes_by_invoice(invoice_id), mtch.contains_dicts_with_entries(expected))
