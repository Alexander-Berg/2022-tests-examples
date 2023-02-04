# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils, constants as cst
from btestlib.data import defaults

QTY = 100
BASE_DT = datetime.datetime.now()
BASE_DT_MV = datetime.datetime(2016, 4, 17)
INVOICES = []

region_mapping = {
    11101014: cst.Regions.BY.id,
    1066: cst.Regions.SW.id,
    1017: cst.Regions.UA.id,
    11101001: cst.Regions.RU.id}

person_mapper = {
    'yt': {'ccy': 'USD', 'nds': 1},
    'sw_ph': {'ccy': 'USD', 'nds': 1.08},
    'ua': {'ccy': 'uah', 'nds': 1.20},
}


def get_rate_on_date(ccy, date=None):
    dt = (date or datetime.datetime.today()).strftime("%Y-%m-%d")
    select = "select rate from T_CURRENCY_RATE_V2  where RATE_DT  = date'{0}' and BASE_CC = 'RUR' and CC='{1}'".format(
        dt,
        ccy)
    result = db.balance().execute(select)
    if len(result) == 0:
        return 1
    else:
        return result[0]['rate']


def create_data(params, client_id, person_id, product_id):
    order_owner = client_id
    invoice_owner = client_id
    contract_id = None
    service_order_id = steps.OrderSteps.next_id(params[0])
    order_id = steps.OrderSteps.create(order_owner, service_order_id, service_id=params[0], product_id=product_id,
                                       params={'AgencyID': None})
    orders_list = [
        {'ServiceID': params[0], 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, params[4], credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, None, None)
    reporter.log((invoice_id, request_id))
    return service_order_id, order_id, invoice_id, request_id


def create_transfer_multiple(service_id, soid1, qty_old, qty_new, soid2, all_qty=0):
    api.medium().CreateTransferMultiple(
        defaults.PASSPORT_UID,
        [
            {"ServiceID": service_id,
             "ServiceOrderID": soid1,
             "QtyOld": qty_old, "QtyNew": qty_new, "AllQty": all_qty}
        ],
        [
            {"ServiceID": service_id,
             "ServiceOrderID": soid2,
             "QtyDelta": 1}
        ],
        1, None)


@pytest.mark.priority('mid')
@reporter.feature(Features.CONSUME, Features.NDS)
@pytest.mark.tasks('BALANCE-21231,https://st.yandex-team.ru/BALANCE-21037')
@pytest.mark.parametrize(
    'params',
    # сервис, продукт1, продукт2,плательщик, пейсис, валюта перевода на MV
    [
        [cst.Services.MARKET.id, cst.Products.MARKET_TEST_10000000.id, cst.Products.MARKET_TEST_10000008.id, cst.PersonTypes.PH.code, 11101001, 'RUB'],
        [cst.Services.MARKET.id, cst.Products.MARKET_TEST_10000000.id, cst.Products.MARKET_TEST_10000004.id, cst.PersonTypes.PH.code, 11101001, None],
        [cst.Services.MARKET.id, cst.Products.MARKET_TEST_10000004.id, cst.Products.MARKET_TEST_10000008.id, cst.PersonTypes.UR.code, 11101003, None],
        [cst.Services.MARKET.id, cst.Products.MARKET_TEST_10000000.id, cst.Products.MARKET_TEST_10000012.id, cst.PersonTypes.SW_PH.code, 1067, None],
    ])
def test_nds_transfer(params):
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    invoice_owner = client_id
    person_id = None or steps.PersonSteps.create(invoice_owner, params[3])

    service_order_id1, order_id1, invoice_id1, request_id1 = create_data(params, client_id, person_id, params[1])
    if params[5] is not None:
        steps.ClientSteps.migrate_to_currency(client_id=invoice_owner, currency=params[5], service_id=params[0],
                                              currency_convert_type='MODIFY', dt=BASE_DT_MV,
                                              region_id=region_mapping[params[4]])
    service_order_id2, order_id2, invoice_id2, request_id2 = create_data(params, client_id, person_id, params[2])
    # [{'order_id', 'qty_old', 'qty_new', 'all_qty'}]/[{'order_id', 'qty_delta'}]

    create_transfer_multiple(params[0], service_order_id1, 100, 80, service_order_id2)
    utils.check_that(1, 1)


if __name__ == "__main__":
    pytest.main("-v -s test_nds_transfer.py")
