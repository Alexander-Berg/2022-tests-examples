# -*- coding: utf-8 -*-

__author__ = 'sandyk'

import datetime

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features
from btestlib import utils as utils
from balance import balance_web as web

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))

SNG_DISCOUNT_POLICY = 22

DIRECT= Product(7, 1475, 'Bucks', 'Money')
SERVICE_ID = DIRECT.service_id
PRODUCT_ID = DIRECT.id
RUB = 810
PAYSYS_ID=  1014
QTY = 100
OLD_DT = datetime.datetime(2016, 10, 10, 0, 0, 0)


def prepare_budget(acts, agency_id, person_id, contract_id, paysys_id):
    for product, completions, dt in acts:
        # Создаём отдельного субклиента для каждого счёта:
        tmp_client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
        qty_for_invoice = completions * 2
        campaigns_list = [
            {'client_id': tmp_client_id, 'service_id': product.service_id, 'product_id': product.id,
             'qty': qty_for_invoice,
             'begin_dt': dt}
        ]
        invoice_id, _, _, orders_list = steps.InvoiceSteps.create_force_invoice(tmp_client_id,
                                                                                person_id,
                                                                                campaigns_list,
                                                                                paysys_id,
                                                                                dt,
                                                                                agency_id=agency_id,
                                                                                credit=0,
                                                                                contract_id=contract_id,
                                                                                overdraft=0,
                                                                                manager_uid=None)
        steps.InvoiceSteps.pay(invoice_id)
        steps.CampaignsSteps.do_campaigns(product.service_id, orders_list[0]['ServiceOrderID'],
                                          {product.shipment_type: completions}, 0, dt)
        steps.ActsSteps.generate(agency_id, 1, dt)


@pytest.mark.priority('mid')
@reporter.feature(Features.DISCOUNT)
@pytest.mark.tickets('BALANCE-23131')
def test_2016_SNG_DIRECT_MEDIA_agency_discount():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = agency_id
    person_id = steps.PersonSteps.create(invoice_owner, 'yt', {})
    contract_id, _ = steps.ContractSteps.create_contract_new('opt_agency', {
                'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id,
                                                     'DT': '2016-08-11T00:00:00',
                                                     'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                     'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                     'DEAL_PASSPORT': '2016-08-11T00:00:00',
                                                     'SERVICES': [SERVICE_ID],
                                                     'CURRENCY': RUB,
                                                     'DISCOUNT_POLICY_TYPE': SNG_DISCOUNT_POLICY}
                                                                 )
    prepare_budget(acts=[(DIRECT, 1400, datetime.datetime(2016, 9, 30, 0, 0, 0))],
                   agency_id=invoice_owner, person_id=person_id,
                contract_id=contract_id, paysys_id=PAYSYS_ID)

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID, product_id=PRODUCT_ID,
                                       params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': OLD_DT}]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list,
                                           additional_params={'InvoiceDesireDT': OLD_DT})
    invoice_id, external_id, total_sum = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID,
                                                                   credit=0,
                                                                   contract_id=contract_id, overdraft=0,
                                                                   endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    # invoice_id = 65063980
    # with web.Driver() as driver:
        # invoice_page = web.AdminInterface.InvoicePage.open(driver, invoice_id)
        # invoice_page.date_change()
