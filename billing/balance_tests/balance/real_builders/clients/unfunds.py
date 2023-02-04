# -*- coding: utf-8 -*-

from decimal import Decimal

from balance import balance_steps
from temp.igogor.balance_objects import Contexts

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()


def test_unfunds():
    service_id = DIRECT.service.id
    client_id = balance_steps.ClientSteps.create()
    person_id = balance_steps.PersonSteps.create(client_id, DIRECT.person_type.code)
    service_order_id = balance_steps.OrderSteps.next_id(service_id)
    balance_steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                    product_id=DIRECT.product.id, service_id=service_id)
    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': Decimal('24.43')}
    ]
    request_id = balance_steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = balance_steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                         paysys_id=DIRECT.paysys.id)
    balance_steps.InvoiceSteps.pay(invoice_id)
    balance_steps.InvoiceSteps.make_rollback_ai(invoice_id)
    return client_id
