# -*- coding: utf-8 -*-
__author__ = 'atkaya'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import AuditFeatures
from btestlib import utils
from btestlib.constants import Firms, Products, Services
from btestlib.matchers import contains_dicts_with_entries
from temp.igogor.balance_objects import Contexts

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)
MARKET = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                              firm=Firms.MARKET_111)

dt = datetime.datetime.now() - datetime.timedelta(days=1)

@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_4))
@pytest.mark.parametrize('context', [
    pytest.param(DIRECT, id='Direct'),
    pytest.param(MARKET, id='Market'),
])
def test_transfer_from_parent_order(context):
    service_id = context.service.id
    qty_parent = 100
    qty_child = 40

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    with reporter.step(u'Создаем заказ (будущий родительский), выставляем с ним счет и оплачиваем его.'):
        parent_service_order_id = steps.OrderSteps.next_id(service_id)
        parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                                  product_id=context.product.id, service_id=service_id)
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': parent_service_order_id, 'Qty': qty_parent, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(InvoiceDesireDT=dt))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=context.paysys.id)
        steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)

    with reporter.step(u'Создаем заказ (будущий дочерний).'):
        child_service_order_id = steps.OrderSteps.next_id(service_id)
        child_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=child_service_order_id,
                                                 product_id=context.product.id, service_id=service_id)
        steps.CampaignsSteps.do_campaigns(service_id, child_service_order_id, {context.product.type.code: qty_child}, 0,
                                          dt)

    steps.OrderSteps.merge(parent_order_id, sub_orders_ids=[child_order_id], group_without_transfer=1)
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

    with reporter.step(u'Получаем информацию по заявкам дочернего заказа.'):
        consumes = db.get_consumes_by_order(child_order_id)
    with reporter.step(u'Проверяем информацию на заявке дочернего заказа.'):
        utils.check_that(consumes, contains_dicts_with_entries([{'consume_sum': qty_child * context.price,
                                                                 'consume_qty': qty_child}]))
