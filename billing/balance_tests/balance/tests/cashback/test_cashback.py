# -*- coding: utf-8 -*-

import datetime

import pytest

from balance import balance_steps as steps
from btestlib.constants import Products, PersonTypes
from temp.igogor.balance_objects import Contexts

DIRECT_KZT_QUASI = Contexts.DIRECT_FISH_KZ_CONTEXT.new(product=Products.DIRECT_KZT_QUASI,
                                                       person_type=PersonTypes.KZU,
                                                       currency='KZT')
DIRECT_BYN_QUASI = Contexts.DIRECT_BYN_BYU_CONTEXT.new(product=Products.DIRECT_BYN,
                                                       person_type=PersonTypes.BYU,
                                                       currency='BYN')
NOW = datetime.datetime.now()
PREVIOUS_MONTH_LAST_DAY = NOW.replace(day=1) - datetime.timedelta(days=1)
NOTIFY_CLIENT_CASHBACK_OPCODE = 12


@pytest.mark.parametrize('with_agency', [True, False])
@pytest.mark.parametrize('context', [DIRECT_KZT_QUASI, DIRECT_BYN_QUASI])
def test_cashback_quasi(context, with_agency):
    client_id = steps.ClientSteps.create()
    if with_agency:
        agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    else:
        agency_id = None
    next_sequence = steps.CashbackSteps.create(client_id, context.service.id, context.currency, 100000000000)

    person_id = steps.PersonSteps.create(agency_id or client_id, context.person_type.code)

    steps.ClientSteps.link(agency_id or client_id, 'aikawa-test-10')
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
                    'BeginDT': PREVIOUS_MONTH_LAST_DAY}]
    request_id = steps.RequestSteps.create(client_id=agency_id or client_id, orders_list=orders_list, )

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CommonSteps.build_notification(NOTIFY_CLIENT_CASHBACK_OPCODE, next_sequence)
    steps.CommonSteps.wait_and_get_notification(NOTIFY_CLIENT_CASHBACK_OPCODE, next_sequence, 1, timeout=420)
