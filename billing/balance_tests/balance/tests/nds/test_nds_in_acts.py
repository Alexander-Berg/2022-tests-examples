# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
import decimal

import pytest
from hamcrest import equal_to

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Products, PersonTypes, Services, Paysyses

SERVICE_ID = Services.DIRECT.id
PRODUCT_ID = Products.DIRECT_FISH.id
PAYSYS_ID = Paysyses.BANK_PH_RUB.id
PERSON_TYPE = PersonTypes.PH.code
DT = datetime.datetime.now()
QUANT = 0.8


@reporter.feature(Features.NDS)
@pytest.mark.tickets('BALANCE-22545', 'TESTBALANCE-1315')
def test_nds_in_acts():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)
    data = []
    for x in range(0, 15):
        client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                           {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})
        orders_list = {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT,
                       'BeginDT': DT}
        data.append({'client_id': client_id, 'service_order_id': service_order_id, 'order_id': order_id,
                     'orders_list': orders_list})
    orders_list = []
    for s in data:
        orders_list.append(s['orders_list'])

    request_id = steps.RequestSteps.create(agency_id, orders_list,
                                           additional_params={'InvoiceDesireDT': DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    db.balance().execute('update (select * from t_client where id  =:agency_id) set is_docs_separated = 1',
                         {'agency_id': agency_id})

    dates = {}
    dates[1] = utils.Date.shift_date(DT, days=32)
    dates[2] = utils.Date.shift_date(dates[1], days=32)
    dates[3] = utils.Date.shift_date(dates[2], days=32)
    dates[4] = utils.Date.shift_date(dates[3], days=32)

    bucks = decimal.Decimal(0.2).quantize(decimal.Decimal('.01'))
    steps.InvoiceSteps.pay(invoice_id)
    for i in dates:
        for s in data:
            steps.CampaignsSteps.do_campaigns(SERVICE_ID, s['service_order_id'],
                                              {'Bucks': bucks, 'Days': 0, 'Money': 0}, 0, dates[i])
        steps.ActsSteps.generate(agency_id, 1, dates[i])
        bucks += decimal.Decimal(0.2).quantize(decimal.Decimal('.01'))

    ## check
    inv_nds = db.balance().execute('select sum(amount_nds) as nds from T_INVOICE_ORDER where INVOICE_ID  =:invoice_id',
                                   {'invoice_id': invoice_id})[0]['nds']
    act_nds = \
        db.balance().execute('select sum(amount_nds) as nds from t_act where invoice_id =:invoice_id',
                             {'invoice_id': invoice_id})[0]['nds']
    act_tr_nds = db.balance().execute(
        'select sum(amount_nds) as nds from T_ACT_TRANS where ACT_ID in (select  id from t_act where invoice_id =:invoice_id)',
        {'invoice_id': invoice_id})[0]['nds']

    utils.check_that(act_nds, equal_to(inv_nds), 'Проверяем cуммы НДС в T_INVOICE_ORDER и T_ACT')
    utils.check_that(act_tr_nds, equal_to(inv_nds), 'Проверяем cуммы НДС в T_INVOICE_ORDER и T_ACT_TRANS')
