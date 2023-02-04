# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import equal_to

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms

NOW = datetime.datetime.now()

# PERSON_TYPE = 'ur'
# PAYSYS_ID = 1003
# SERVICE_ID = 7
# CURRENCY_PRODUCT_ID = 503162
# NON_CURRENCY_PRODUCT_ID = 1475
# NON_CURRENCY_MSR = 'Bucks'
# CURRENCY_MSR = 'Money'

QTY = 100
DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)

pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
def test_last_day_of_the_motnh_shipmetn(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)

    parent_service_order_id = steps.OrderSteps.next_id(context.service.id)
    parent_order_id = steps.OrderSteps.create(client_id, parent_service_order_id, context.product.id,
                                              context.service.id)

    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': parent_service_order_id,
                    'Qty': QTY, 'BeginDT': NOW}]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order=parent_order_id, sub_orders_ids=[order_id], group_without_transfer=1)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 50},
                                      campaigns_dt=NOW - datetime.timedelta(days=1))
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 100},
                                      campaigns_dt=utils.Date.nullify_time_of_date(NOW))
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id, input_={'for_dt': NOW})
    act_id = steps.ActsSteps.generate(client_id=client_id, force=1, date=NOW)[0]
    act = db.get_act_by_id(act_id)[0]
    assert act['act_sum'] == 3000
