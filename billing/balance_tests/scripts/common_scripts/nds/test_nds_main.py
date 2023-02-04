# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
from decimal import *

import pytest
from hamcrest import equal_to

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils

QTY = 100
BASE_DT = datetime.datetime.now()
BASE_DT_MV = utils.Date.shift_date(BASE_DT, days=-1)
INVOICES = []

region_mapping = {11101001: 225,
                  11101014: 149,
                  1066: 126,
                  1017: 187,
                  11101001: 225}

person_mapper = {'yt': {'ccy': 'USD', 'nds': 1}
    , 'sw_ph': {'ccy': 'USD', 'nds': 1.077}
    , 'ua': {'ccy': 'uah', 'nds': 1.20}}


def get_rate_on_date(ccy, date=None):
    dt = (date or datetime.datetime.today()).strftime("%Y-%m-%d")
    select = "select max(rate) keep(dense_rank first order by rate_dt desc) as rate from T_CURRENCY_RATE_V2 where BASE_CC = 'RUR' and CC='{1}' and rate_dt <= date'{0}'".format(
        dt,
        ccy)

    result = db.balance().execute(select)
    if len(result) == 0:
        return 1
    else:
        return result[0]['rate']


@pytest.mark.priority('mid')
@reporter.feature(Features.CONSUME, Features.NDS)
@pytest.mark.tasks('BALANCE-21231, BALANCE-21037')
@pytest.mark.parametrize('params',
                         [
                             [11, 10000000, 'ph', 11101001, None, {'amt': 3000},  ##1
                              {'price': 30, 'nds': None, 'tax_policy_pct': 325}]
                             , [11, 10000000, 'yt', 11101014, None, {'amt': 2542.37},  ##2
                                {'price': 25, 'nds': None, 'tax_policy_pct': 3}]
                             , [11, 10000000, 'yt', 11101013, None, {'amt': 1},  ##3
                                {'price': 1.20, 'nds': None, 'tax_policy_pct': 3}]
                             , [11, 10000000, 'sw_ph', 1067, None, {'amt': 1},  ##4
                                {'price': 1.20, 'nds': None, 'tax_policy_pct': 221}]
                             # , [11, 10000002, 'ph', 1001, None, {'amt': 3000},  ##7 неактуален (BALANCE-21037)
                             #    {'price': 30, 'nds': None, 'tax_policy_pct': 1}]
                             # , [11, 10000002, 'yt', 11101014, None, {'amt': 2542.37},  ##8  неактуален (BALANCE-21037)
                             #    {'price': 25, 'nds': None, 'tax_policy_pct': 3}]
                             # , [11, 10000002, 'yt', 11101013, None, {'amt': 41},  ##9 неактуален (BALANCE-21037)
                             #    {'price': 0.41, 'nds': None, 'tax_policy_pct': 3}]
                             , [11, 10000004, 'ph', 11101001, None, {'amt': 3540},  ##10
                                {'price': 35.4 / 1.18 * 1.20, 'nds': None, 'tax_policy_pct': 325}]
                             , [11, 10000004, 'yt', 11101014, None, {'amt': 3000},  ##11
                                {'price': 30, 'nds': None, 'tax_policy_pct': 3}]
                             , [11, 10000004, 'yt', 11101013, None, {'amt': 41},  ##12
                                {'price': 0.41, 'nds': None, 'tax_policy_pct': 3}]
                             , [11, 10000005, 'ph', 11101001, None, {'amt': 3540},  ##13
                                {'price': 35.4 / 1.18 * 1.20, 'nds': None, 'tax_policy_pct': 325}]
                             , [11, 10000005, 'yt', 11101014, None, {'amt': 3000},  ##14
                                {'price': 30, 'nds': None, 'tax_policy_pct': 3}]
                             , [11, 10000005, 'yt', 11101013, None, {'amt': 1},  ##15
                                {'price': 1, 'nds': None, 'tax_policy_pct': 3}]
                             #  , [11, 10000006, 'ph', 11101001, None, {'amt': 3000},  ##16 неактуален (BALANCE-21037)
                             #  {'price': 30, 'nds': None, 'tax_policy_pct': 1}]
                             # , [11, 10000006, 'yt', 11101014, None, {'amt': 2542.37},  ##17 неактуален (BALANCE-21037)
                             #    {'price': 25, 'nds': None, 'tax_policy_pct': 3}]
                             # , [11, 10000006, 'yt', 11101013, None, {'amt': 1},  ##18 неактуален (BALANCE-21037)
                             #    {'price': 1.20, 'nds': None, 'tax_policy_pct': 3}]
                             # , [11, 10000007, 'ph', 11101001, None, {'amt': 3540},  ##19 неактуален (BALANCE-21037)
                             #    {'price': 35.4, 'nds': None, 'tax_policy_pct': 1}]
                             # , [11, 10000007, 'yt', 11101014, None, {'amt': 3000},
                             #    {'price': 30, 'nds': None, 'tax_policy_pct': 3}]  ##20  неактуален (BALANCE-21037)
                             # , [11, 10000007, 'yt', 11101013, None, {'amt': 41},
                             #    {'price': 0.41, 'nds': None, 'tax_policy_pct': 3}]  ##21 неактуален (BALANCE-21037)
                             , [11, 10000008, 'ph', 11101001, 'RUB', {'amt': 100},
                                {'price': 1, 'nds': None, 'tax_policy_pct': 325}]  ##22
                             , [11, 10000008, 'yt', 11101014, 'RUB', {'amt': 100},
                                {'price': '1.00000000', 'nds': None, 'tax_policy_pct': 3}]  ##23
                             # ,[11, 10000012, 'ua', 1017, None, {'amt': 1},                     ##25
                             # {'price': 1.18, 'nds': None, 'tax_policy_pct': 5}]
                             # @pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
                             #      , [11, 10000000, 'ua', 1017, None, {'amt': 1200},  ##26
                             #         {'price': '12.00000000', 'nds': None, 'tax_policy_pct': 5}]
                             # ,[11, 10000013, 'ua', 1017, None, {'amt': 1},                     ##27
                             # {'price': 1.18, 'nds': None, 'tax_policy_pct': 5}]
                             # @pytest.mark.skip(reason='UKRAINE WAS TURNED OFF')
                             #      , [11, 10000014, 'ua', 1017, 'UAH', {'amt': 100},  ##28
                             #         {'price': '1.00000000', 'nds': None, 'tax_policy_pct': None}]

                         ])
def test_nds_main(params):
    if params[5]['amt'] == 1:  ##тогда вычисляем прайс
        price = 30 / params[6]['price'] * float(person_mapper[params[2]]['nds']) / float(
            get_rate_on_date(person_mapper[params[2]]['ccy']))
        params[6]['price'] = Decimal(price).quantize(Decimal('.00000001'))
        params[5]['amt'] = Decimal(price * 100).quantize(Decimal('.01'))
        reporter.log(params[6]['price'])
        reporter.log(params[5]['amt'])
    else:
        params[6]['price'] = Decimal(params[6]['price']).quantize(Decimal('0.00000001'))
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
    order_owner = client_id
    invoice_owner = client_id

    person_id = None or steps.PersonSteps.create(invoice_owner, params[2])

    if params[4] is not None:
        steps.ClientSteps.migrate_to_currency(client_id=invoice_owner, currency=params[4], service_id=params[0],
                                              currency_convert_type='MODIFY', dt=BASE_DT_MV,
                                              region_id=region_mapping[params[3]])

    contract_id = None
    service_order_id = steps.OrderSteps.next_id(params[0])
    steps.OrderSteps.create(order_owner, service_order_id, service_id=params[0], product_id=params[1],
                            params={'AgencyID': None})
    orders_list = [
        {'ServiceID': params[0], 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    request_id = steps.RequestSteps.create(invoice_owner, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, params[3], credit=0, contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)
    reporter.log(invoice_id)
    steps.InvoiceSteps.pay(invoice_id, None, None)

    reporter.log((invoice_id, request_id))

    ##проверяем данные
    inv_amt = db.balance().execute('select amount as amt from T_INVOICE where id =:invoice_id',
                                   {'invoice_id': invoice_id})
    reporter.log(float(params[5]['amt']))
    reporter.log(float(inv_amt[0]['amt']))

    utils.check_that(float(params[5]['amt']), (float(inv_amt[0]['amt'])), 'Проверяем данные T_INVOICE')
    inv_order_data = db.balance().execute(
        'select internal_price as price, nds, tax_policy_pct_id as tax_policy_pct from  T_INVOICE_ORDER where INVOICE_ID =:invoice_id',
        {'invoice_id': invoice_id})[0]
    consume_data = db.balance().execute(
        'select price, nds, tax_policy_pct_id as tax_policy_pct from  T_CONSUME where INVOICE_ID =:invoice_id',
        {'invoice_id': invoice_id})[0]

    inv_order_data['price'] = Decimal(inv_order_data['price']).quantize(Decimal('.00000001'))
    consume_data['price'] = Decimal(consume_data['price']).quantize(Decimal('.00000001'))

    utils.check_that(inv_order_data, equal_to(params[6]), 'Проверяем данные T_INVOICE_ORDER')
    utils.check_that(consume_data, equal_to(params[6]), 'Проверяем данные T_CONSUME')
