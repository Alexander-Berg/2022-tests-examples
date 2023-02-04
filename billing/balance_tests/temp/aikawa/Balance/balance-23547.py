import datetime

from balance import balance_steps as steps
from temp.igogor.balance_objects import Contexts

NOW = datetime.datetime.now()

DIRECT = Contexts.MARKET_RUB_CONTEXT.new()
PERSON_TYPE = 'ur'


def test23547():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    service_order_id = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    order = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id, service_id=DIRECT.service.id,
                                    service_order_id=service_order_id, params={'AgencyID': None})

    service_order_id2 = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    order2 = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id, service_id=DIRECT.service.id,
                                     service_order_id=service_order_id2, params={'AgencyID': None})

    service_order_id3 = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    order3 = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id, service_id=DIRECT.service.id,
                                     service_order_id=service_order_id3, params={'AgencyID': None})

    orders_list = [
        {'ServiceID': DIRECT.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': NOW},
        {'ServiceID': DIRECT.service.id, 'ServiceOrderID': service_order_id2, 'Qty': 50, 'BeginDT': NOW}
    ]

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1003,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.OrderSteps.merge(order, [order2], group_without_transfer=1)

    steps.OrderSteps.transfer([{'order_id': order, 'qty_old': 150, 'qty_new': 140, 'all_qty': 0}],
                              [{'order_id': order3, 'qty_delta': 1}])
