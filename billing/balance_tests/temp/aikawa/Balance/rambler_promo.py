# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D
import hamcrest
import pytest
from hamcrest import equal_to

from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib import utils

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
DT_1_DAY_AGO = NOW - datetime.timedelta(days=1)
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
BASE_DT = datetime.datetime.now()


def generate_data_test_PROMOCODE_MINIMAL_QTY_FISH():
    return [
        {'minimal_qty': 34, 'qty': 34, 'expected': True},
    ]


@pytest.mark.parametrize('data',
                         generate_data_test_PROMOCODE_MINIMAL_QTY_FISH()
                         )
def test_rambler_Promo(data):
    PERSON_TYPE = 'ur'
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    PROMOCODE_START_DT = NOW
    promocode_id = steps.PromocodeSteps.create(start_dt=PROMOCODE_START_DT, end_dt=None, bonus1=66667, bonus2=66667,
                                               minimal_qty=data['minimal_qty'], reservation_days=None)
    promocode_code = db.get_promocode_by_id(promocode_id)[0]['code']
    steps.PromocodeSteps.reserve(client_id, promocode_id)

    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': HALF_YEAR_BEFORE_NOW_ISO,
                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                       'SERVICES': [SERVICE_ID],
                       'PAYMENT_TYPE': 2,
                       'FIRM': 1
                       }

    CONTRACT_TYPE = 'OPT_AGENCY'
    contract_id, _ = steps.ContractSteps.create_contract_new(CONTRACT_TYPE, contract_params)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)

    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID, service_order_id=service_order_id)

    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': data['qty'], 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(PromoCode=promocode_code))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    if data['expected']:
        utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id), hamcrest.equal_to(True))
    else:
        utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id), hamcrest.equal_to(False))
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 66670}, 0, NOW)
    #
    # steps.ActsSteps.enqueue([client_id], force=1, date=NOW)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=NOW)[0]
    steps.CommonSteps.export('OEBS', 'Invoice', invoice_id)
    steps.CommonSteps.export('OEBS', 'Act', act_id)
