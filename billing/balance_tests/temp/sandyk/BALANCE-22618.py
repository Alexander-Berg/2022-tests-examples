# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest
from hamcrest import equal_to

import balance.balance_db as db
from balance import balance_steps as steps
from balance.features import Features
import btestlib.reporter as reporter
from btestlib import utils
from btestlib.constants import Products, Regions, Processings, PersonTypes, Services, Currencies, Firms
from simpleapi.common.payment_methods import PaystepCard
from simpleapi.data import uids_pool as uids
from simpleapi.data.cards_pool import ALPHA_PAYSTEP_VISA
from simpleapi.steps import paystep_steps as paystep

from balance import balance_steps as true_balance_steps

PERSON_TYPE = PersonTypes.PH.code
SERVICE_ID = Services.DIRECT.id
PRODUCT_ID = Products.DIRECT_FISH.id
CURRENCY = Currencies.RUB.iso_code
FIRM_ID = Firms.YANDEX_1.id
REGION_ID = Regions.RU.id
MAIN_DT = datetime.datetime.now()
OVERDRAFT_LIMIT = 1000
INSTANT_PAYSYS = 1033
BANK_PAYSYS = 1001
QTY = 10

pytestmark = [pytest.mark.tickets('BALANCE-22618'),
    reporter.feature(Features.OVERDRAFT, Features.ACT, Features.PAYMENT)]


def act_creation(paysys_id):
    client_id = steps.ClientSteps.create(params={'IS_AGENCY': 0})
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.ClientSteps.set_force_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, firm_id=FIRM_ID,
                                          start_dt=datetime.datetime.now(), currency=None)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': MAIN_DT}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=MAIN_DT))

    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                           paysys_id=paysys_id, credit=0, contract_id=None, overdraft=1,
                                                           endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': QTY}, 0, campaigns_dt=MAIN_DT)
    act_id = steps.ActsSteps.generate(client_id, 1, MAIN_DT)[0]
    service = Products.DIRECT_FISH.service
    uid = uids.get_random_of_type(uids.Types.random_from_all)
    true_balance_steps.ClientSteps.link(client_id, uid.login)
    return invoice_id, external_id, act_id, service, uid


def check_paid_amount(act_id):
    res = db.balance().execute('select paid_amount from t_act where id = :act_id', {'act_id': act_id})[0]
    utils.check_that(res['paid_amount'], equal_to(300), u'Проверяем, что акт пометился оплаченным в базе')


def test_act_paid_amount_instant_paysys():
    invoice_id, external_id, act_id, service, uid = act_creation(INSTANT_PAYSYS)
    paystep.pay_by(PaystepCard(Processings.ALPHA), service, user=uid, card=ALPHA_PAYSTEP_VISA, region_id=REGION_ID,
                   invoice_id=invoice_id,
                   data_for_checks={'invoice_id': invoice_id, 'external_id': external_id, 'total_sum': 300, 'currency_iso_code': CURRENCY})
    check_paid_amount(act_id)


def test_act_paid_amount_bank_paysys():
    invoice_id, external_id, act_id, service, uid = act_creation(BANK_PAYSYS)
    steps.InvoiceSteps.pay(invoice_id)
    check_paid_amount(act_id)
