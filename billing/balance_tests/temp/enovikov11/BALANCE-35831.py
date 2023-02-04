# -*- coding: utf-8 -*-

import pytest
from datetime import datetime

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib.data.partner_contexts import LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL


def test_create_request_charge_note(is_postpay, is_offer):
    context = LOGISTICS_CLIENTS_RU_CONTEXT_GENERAL.new(is_offer=is_offer)
    qty = 50
    is_promo = 1

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, is_postpay=is_postpay, is_offer=is_offer)
    service_order_id = api.medium().GetOrdersInfo({'ContractID': contract_id})[0]['ServiceOrderID']
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': datetime.now()}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireType': 'charge_note',
                                                              'TurnOnRows': is_promo})
    paysys_id = 13001003
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                 credit=0, contract_id=contract_id)
    return invoice_id

a = test_create_request_charge_note(False, False)
b = test_create_request_charge_note(False, True)
c = test_create_request_charge_note(True, False)
d = test_create_request_charge_note(True, True)

print(a)
print(b)
print(c)
print(d)