# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime
import pytest
import json

from balance import balance_steps as steps
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Currencies, Services, Regions, ContractCommissionType, Collateral, Products
import btestlib.utils as utils
from balance import balance_db as db

dt = datetime.datetime.now()

# PERSON_TYPE = 'ur'
# SERVICE_ID = 7
# PRODUCT_ID = 1475
# CURRENCY_PRODUCT_ID = 503162
# CURRENCY_PRODUCT_ID_ANOTHER = 503163
# PAYSYS_ID = 1003

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(contract_type=ContractCommissionType.OPT_AGENCY_PREM,
                                              collateral_type=Collateral.SUBCLIENT_CREDIT_LIMIT)

DIRECT_PRODUCT = Products.DIRECT_RUB.id
MEDIA_OH = Products.MEDIA_DIRECT_RUB.id
MEDIA_CH = Products.MEDIA_DIRECT_CHAST.id


@pytest.mark.parametrize('context', [DIRECT])
def test_child_order_type0(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=dt - datetime.timedelta(days=1))
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    child_order_id_DIRECT = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                                    product_id=DIRECT_PRODUCT,
                                                    service_id=context.service.id)

    service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
    child_order_id_MEDIA_OH = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                                      product_id=MEDIA_OH,
                                                      service_id=context.service.id)

    service_order_id_3 = steps.OrderSteps.next_id(service_id=context.service.id)
    child_order_id_MEDIA_CH = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_3,
                                                      product_id=MEDIA_CH,
                                                      service_id=context.service.id)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=DIRECT_PRODUCT, service_id=context.service.id)

    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': parent_service_order_id, 'Qty': 300, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id,
                           sub_orders_ids=[child_order_id_DIRECT, child_order_id_MEDIA_OH, child_order_id_MEDIA_CH])
    print steps.OrderSteps.make_optimized(parent_order_id)
    print db.get_order_by_id(child_order_id_DIRECT)[0]['child_ua_type']
    print db.get_order_by_id(child_order_id_MEDIA_CH)[0]['child_ua_type']
    print db.get_order_by_id(child_order_id_MEDIA_OH)[0]['child_ua_type']
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Money': 100}, 0,
                                      dt - datetime.timedelta(days=1))
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2, {'Money': 50}, 0,
                                      dt - datetime.timedelta(days=1))
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_3, {'Money': 50}, 0,
                                      dt - datetime.timedelta(days=1))
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    steps.ActsSteps.generate(client_id, force=1, date=dt)
    # child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    # assert child_ua_type == 0


@pytest.mark.parametrize('context', [DIRECT])
def test_child_order_type1(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=dt - datetime.timedelta(days=1))
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    child_order_id_DIRECT = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                                    product_id=DIRECT_PRODUCT,
                                                    service_id=context.service.id)

    service_order_id_2 = steps.OrderSteps.next_id(service_id=context.service.id)
    child_order_id_MEDIA_OH = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                                      product_id=MEDIA_OH,
                                                      service_id=context.service.id)

    service_order_id_3 = steps.OrderSteps.next_id(service_id=context.service.id)
    child_order_id_MEDIA_CH = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_3,
                                                      product_id=MEDIA_CH,
                                                      service_id=context.service.id)

    parent_service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    parent_order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=parent_service_order_id,
                                              product_id=DIRECT_PRODUCT, service_id=context.service.id)

    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': parent_service_order_id, 'Qty': 300, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    steps.OrderSteps.merge(parent_order_id,
                           sub_orders_ids=[child_order_id_DIRECT, child_order_id_MEDIA_OH, child_order_id_MEDIA_CH])
    print steps.OrderSteps.make_optimized(parent_order_id)
    print db.get_order_by_id(child_order_id_DIRECT)[0]['child_ua_type']
    print db.get_order_by_id(child_order_id_MEDIA_CH)[0]['child_ua_type']
    print db.get_order_by_id(child_order_id_MEDIA_OH)[0]['child_ua_type']
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Money': 100}, 0,
                                      dt - datetime.timedelta(days=1))
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_2, {'Money': 50}, 0,
                                      dt - datetime.timedelta(days=1))
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id_3, {'Money': 50}, 0,
                                      dt - datetime.timedelta(days=1))
    steps.OrderSteps.ua_enqueue([client_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)
    # child_ua_type = db.get_order_by_id(child_order_id)[0]['child_ua_type']
    # assert child_ua_type == 0
