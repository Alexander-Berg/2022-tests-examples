# -*- coding: utf-8 -*-

import datetime
import time
from decimal import Decimal as D

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from balance.features import Features
from btestlib import matchers as mtch
from btestlib import utils as utils

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.MULTICURRENCY, Features.CONVERSION)
              ]


SERVICE_ID = 7
# PRODUCT_ID = 502953 ##502918
PRODUCT_DIRECT = Product(7, 1475, 'Bucks')
PRODUCT_DIRECT_RUB = Product(7, 503162, 'Money')
PAYSYS_ID = 1003
QTY = 100
BASE_DT = datetime.datetime.now()

manager_uid = '244916211'

def test_product_currency_attribute_for_currency_products():
    client_id = steps.ClientSteps.create({'REGION_ID': '225', 'CURRENCY': 'RUB', 'SERVICE_ID': SERVICE_ID,
                                          'MIGRATE_TO_CURRENCY': datetime.datetime(2000, 1, 1), 'IS_AGENCY': 0})
    # steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)
    agency_id = None
    order_owner = client_id
    invoice_owner = agency_id or client_id
    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID,
                                       product_id=PRODUCT_DIRECT_RUB.id,
                                       params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
    order = db.get_order_by_id(order_id)
    utils.check_that(order[0]['product_currency'], equal_to('RUB'))


def test_product_currency_attribute_for_fish_products():
    client_id = steps.ClientSteps.create()
    agency_id = None
    order_owner = client_id
    invoice_owner = agency_id or client_id
    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')
    contract_id = None

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID,
                                       product_id=PRODUCT_DIRECT.id,
                                       params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {PRODUCT_DIRECT.shipment_type: 28.99}, 0, BASE_DT)

    steps.ClientSteps.create({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'SERVICE_ID': SERVICE_ID,
                              'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
                              'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    time.sleep(5)
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)

    order = db.get_order_by_id(order_id)
    utils.check_that(order[0]['product_currency'], equal_to('RUB'))


def test_double_convertation_case():
    client_id = steps.ClientSteps.create()
    agency_id = None
    order_owner = client_id
    invoice_owner = agency_id or client_id
    person_id = None or steps.PersonSteps.create(invoice_owner, 'ur')
    contract_id = None

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=SERVICE_ID,
                                       product_id=PRODUCT_DIRECT.id,
                                       params={'AgencyID': agency_id, 'ManagerUID': manager_uid})
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {PRODUCT_DIRECT.shipment_type: 28.99}, 0, BASE_DT)
    # steps.CampaignsSteps.update_campaigns(SERVICE_ID, service_order_id, {'Bucks': 28.99, 'Money': 0}, 0, BASE_DT)
    # steps.CommonSteps.wait_for_export('PROCESS_COMPLETION', order_id)

    steps.ClientSteps.create({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'SERVICE_ID': SERVICE_ID,
                              'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
                              'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    time.sleep(5)
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)

    query = 'update t_client_service_data set migrate_to_currency = null, iso_currency = null where class_id = :client_id'
    db.balance().execute(query, {'client_id': client_id})

    # query = 'update t_order set completion_fixed_qty = null where id = :ordert_id'
    # db.balance().execute(query, {'order_id': order_id})

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {PRODUCT_DIRECT.shipment_type: 32.99}, 0, BASE_DT)

    steps.ClientSteps.create({'CLIENT_ID': client_id, 'REGION_ID': '225', 'CURRENCY': 'RUB', 'SERVICE_ID': SERVICE_ID,
                              'MIGRATE_TO_CURRENCY': datetime.datetime.now() + datetime.timedelta(seconds=5),
                              'CURRENCY_CONVERT_TYPE': 'MODIFY'})
    time.sleep(5)
    steps.CommonSteps.export('MIGRATE_TO_CURRENCY', 'Client', client_id)

    order = db.get_order_by_id(order_id)[0]
    expected = {'product_currency': 'RUB', 'completion_fixed_qty': D('32.99')}
    # utils.check_that(act['amount'], equal_to(200))
    utils.check_that(order, mtch.has_entries_casted(expected))


if __name__ == "__main__":
    pass