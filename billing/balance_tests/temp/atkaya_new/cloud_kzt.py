# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from balance import balance_steps as steps
from btestlib.constants import Currencies, \
    Firms, PersonTypes, NdsNew
from btestlib.data.partner_contexts import CLOUD_RU_CONTEXT


CLOUD_KZT_CONTEXT = CLOUD_RU_CONTEXT.new(person_type=PersonTypes.KZU,
    firm=Firms.CLOUD_KZT,
    currency=Currencies.KZT,
    paysys=102001020,
    nds=NdsNew.KAZAKHSTAN,)

def test_create_invoice_cloud_kzt():
    context = CLOUD_KZT_CONTEXT
    is_offer = 1
    qty = 100
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                                       is_offer=is_offer)
    # service_order_id = api.medium().GetOrdersInfo({'ContractID': contract_id})[0]['ServiceOrderID']
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=512220,
                            service_id=context.service.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': datetime.datetime.now()}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': datetime.datetime.now(),
                                                              'InvoiceDesireType': 'charge_note'})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys,
                                                 credit=0, contract_id=contract_id)