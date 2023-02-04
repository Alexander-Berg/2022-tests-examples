# -*- coding: utf-8 -*-
import datetime
import pytest
import json

from balance import balance_db as db
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils, reporter
from btestlib.constants import Products, Paysyses, ContractCommissionType, ContractPaymentType, Services, Collateral, \
    ContractCreditType
from balance.features import Features
from btestlib.data.defaults import Date
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions

NOW = datetime.datetime.now()
DT_1_DAY_BEFORE = NOW - datetime.timedelta(days=1)
LAST_YEAR = NOW - datetime.timedelta(days=30)
DIRECT_MONEY_RUB_CONTEXT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(person=PersonTypes.UR, region=Regions.RU)

pytestmark = [reporter.feature(Features.UNIFIED_ACCOUNT)]


@pytest.mark.parametrize('context', [DIRECT_MONEY_RUB_CONTEXT])
def test_main_order_20_pct_child_18(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person.code)
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY',
                                          dt=LAST_YEAR - datetime.timedelta(days=10),
                                          currency=context.currency.iso_code, region_id=context.region.id)
    # Заказ 1, главный
    service_order_id_1 = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id_1 = steps.OrderSteps.create(client_id, service_order_id_1, service_id=context.service.id,
                                         product_id=context.product.id, params={'AgencyID': None})

    # Заказ 2, дочерний
    service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, service_id=context.service.id,
                                         product_id=context.product.id,
                                         params={'AgencyID': None, 'GroupServiceOrderID': service_order_id_1})
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2, {'Money': 30}, 0,
                                      LAST_YEAR - datetime.timedelta(days=1))
    steps.OrderSteps.make_optimized(order_id_1)

    # steps.OrderSteps.ua_enqueue(client_ids=[client_id], for_dt=LAST_YEAR)
    # steps.CommonSteps.export(queue_='UA_TRANSFER', object_id=client_id, classname='Client')

    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_1, 'Qty': 20, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=None, overdraft=0,
                                                 endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.ua_enqueue(client_ids=[client_id], for_dt=LAST_YEAR)
    steps.CommonSteps.export(queue_='UA_TRANSFER', object_id=client_id, classname='Client')

    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_1, 'Qty': 10, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=None, overdraft=0,
                                                 endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.ua_enqueue(client_ids=[client_id])
    steps.CommonSteps.export(queue_='UA_TRANSFER', object_id=client_id, classname='Client')
    steps.CommonSteps.export(queue_='PROCESS_COMPLETION', object_id=order_id_1, classname='Order')
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
    #                                              contract_id=None, overdraft=0,
    #                                              endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    # steps.OrderSteps.ua_enqueue(client_ids=[client_id], for_dt=dt+datetime.timedelta(days=1))
    # steps.CommonSteps.export(queue_='UA_TRANSFER', object_id=client_id, classname='Client')

@pytest.mark.parametrize('context', [DIRECT_MONEY_RUB_CONTEXT])
def test_main_order_2323(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person.code)
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY',
                                          dt=LAST_YEAR - datetime.timedelta(days=10),
                                          currency=context.currency.iso_code, region_id=context.region.id)
    # Заказ 1, главный
    service_order_id_1 = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id_1 = steps.OrderSteps.create(client_id, service_order_id_1, service_id=context.service.id,
                                         product_id=context.product.id, params={'AgencyID': None})

    # Заказ 2, дочерний
    service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, service_id=context.service.id,
                                         product_id=context.product.id,
                                         params={'AgencyID': None, 'GroupServiceOrderID': service_order_id_1})
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2, {'Money': 30}, 0,
                                      LAST_YEAR - datetime.timedelta(days=1))
    steps.OrderSteps.make_optimized(order_id_1)

    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_1, 'Qty': 20, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=None, overdraft=0,
                                                 endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.ua_enqueue(client_ids=[client_id], for_dt=LAST_YEAR)
    steps.CommonSteps.export(queue_='UA_TRANSFER', object_id=client_id, classname='Client')

    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_1, 'Qty': 15, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=None, overdraft=0,
                                                 endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.ua_enqueue(client_ids=[client_id])
    steps.CommonSteps.export(queue_='UA_TRANSFER', object_id=client_id, classname='Client')
    # steps.CommonSteps.export(queue_='PROCESS_COMPLETION', object_id=order_id_1, classname='Order')
    # # invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
    #                                              contract_id=None, overdraft=0,
    #                                              endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    # steps.OrderSteps.ua_enqueue(client_ids=[client_id], for_dt=dt+datetime.timedelta(days=1))
    # steps.CommonSteps.export(queue_='UA_TRANSFER', object_id=client_id, classname='Client')