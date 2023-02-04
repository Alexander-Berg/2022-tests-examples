import datetime

from balance import balance_steps as steps
from btestlib.constants import Services
from temp.igogor.balance_objects import Contexts

NOW = datetime.datetime.now()

NAVI = Contexts.DIRECT_MONEY_RUB_CONTEXT.new(service=Services.NAVI)


def test0():
    client_id = steps.ClientSteps.create()
    service_order_id = steps.OrderSteps.next_id(service_id=NAVI.service.id)
    order = steps.OrderSteps.create(client_id=client_id, product_id=NAVI.product.id, service_id=NAVI.service.id,
                                    service_order_id=service_order_id, params={'AgencyID': None})
    service_order_id2 = steps.OrderSteps.next_id(service_id=NAVI.service.id)
    order2 = steps.OrderSteps.create(client_id=client_id, product_id=NAVI.product.id, service_id=NAVI.service.id,
                                     service_order_id=service_order_id2, params={'AgencyID': None})
    steps.OrderSteps.merge(order, [order2])


def test_elama():
    print steps.OrderSteps.ua_enqueue([322057])
    # steps.CommonSteps.export('OEBS', 'CurrencyR

# ate', 208867)
#     steps.ClientSteps.link(56349914, 'yndx-tst-role-5')
