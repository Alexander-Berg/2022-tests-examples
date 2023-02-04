# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features
from btestlib.constants import Services, Paysyses, PersonTypes, Products

UR = PersonTypes.UR.code
PAYSYS_ID = Paysyses.BANK_UR_RUB.id
SERVICE_ID = Services.DIRECT.id
PRODUCT_ID = Products.DIRECT_FISH.id

QTY = 100
CAMPAIGN_QTY = 65
DT = datetime.datetime.now()

pytestmark = [pytest.mark.tickets('BALANCE-26544'),
              reporter.feature(Features.UI, Features.ACT)]


##позже надо добавить тестов для физика и бух.логина
def data_prep(user):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    person_id = steps.PersonSteps.create(client_id, UR)
    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None})
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': CAMPAIGN_QTY}, 0, campaigns_dt=DT)
    steps.ActsSteps.generate(client_id, 1, DT)


@pytest.mark.parametrize("type",
                         [
                             'all',
                             'selected'
                         ],
                         ids=[
                             'order_all_docs',
                             'order_selected_docs'
                         ]
                         )
def test_docs_for_ur(type, get_free_user):
    user = get_free_user()
    data_prep(user)
    with web.Driver(user=user) as driver:
        act_page = web.ClientInterface.ActsPage.open(driver)
        act_page.is_advanced_form_expanded()
        act_page.order_original_docs(type=type)
