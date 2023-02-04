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

dt = datetime.datetime.now()
DT_1_DAY_BEFORE = dt - datetime.timedelta(days=1)
DIRECT_MONEY_RUB_CONTEXT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(person=PersonTypes.UR, region=Regions.RU)

pytestmark = [reporter.feature(Features.UNIFIED_ACCOUNT)]


@pytest.mark.parametrize('context', [DIRECT_MONEY_RUB_CONTEXT])
def test_shipment_after_overshipment_on_parent(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person.code)
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                          currency=context.currency.iso_code, region_id=context.region.id
                                          )
    # Заказ 1, главный
    service_order_id_1 = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id_1 = steps.OrderSteps.create(client_id, service_order_id_1, service_id=context.service.id,
                                         product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_1, 'Qty': 20, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=None, overdraft=0,
                                                 endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Заказ 2, дочерний
    service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, service_id=context.service.id,
                                         product_id=context.product.id,
                                         params={'AgencyID': None, 'GroupServiceOrderID': service_order_id_1})
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2, {'Money': 30}, 0, dt)
    steps.OrderSteps.make_optimized(order_id_1)

    steps.OrderSteps.ua_enqueue(client_ids=[client_id], for_dt=dt)
    steps.CommonSteps.export(queue_='UA_TRANSFER', object_id=client_id, classname='Client')
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=None, overdraft=0,
                                                 endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.OrderSteps.ua_enqueue(client_ids=[client_id], for_dt=dt+datetime.timedelta(days=1))
    steps.CommonSteps.export(queue_='UA_TRANSFER', object_id=client_id, classname='Client')