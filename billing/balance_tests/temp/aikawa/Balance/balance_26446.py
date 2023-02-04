import datetime

from balance import balance_db as db
from balance import balance_steps as steps
from temp.igogor.balance_objects import Contexts

NOW = datetime.datetime.now()

DIRECT = Contexts.DIRECT_MONEY_RUB_CONTEXT.new()


def test0():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          # dt=NOW - datetime.timedelta(days=1)
                                          )
    service_order_id = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    order = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id, service_id=DIRECT.service.id,
                                    service_order_id=service_order_id, params={'AgencyID': None, 'is_ua_optimize': 0})

    print db.get_order_by_id(order)[0]['completion_qty']

    order = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id, service_id=DIRECT.service.id,
                                    service_order_id=service_order_id, params={'AgencyID': None, 'is_ua_optimize': 1})


def test1():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=NOW - datetime.timedelta(days=1))
    person_id = steps.PersonSteps.create(client_id, DIRECT.person_type.code)
    orders_list = []
    service_order_id = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    order = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id, service_id=DIRECT.service.id,
                                    service_order_id=service_order_id, params={'AgencyID': None})

    service_order_id2 = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    order2 = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id, service_id=DIRECT.service.id,
                                     service_order_id=service_order_id2, params={'AgencyID': None})

    steps.OrderSteps.merge(parent_order=order, sub_orders_ids=[order2])
    steps.OrderSteps.make_optimized(order)
    steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id,
                            service_id=DIRECT.service.id,
                            service_order_id=service_order_id,
                            params={'AgencyID': None, 'is_ua_optimize': '0'})


def test2():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=NOW - datetime.timedelta(days=1))
    person_id = steps.PersonSteps.create(client_id, DIRECT.person_type.code)
    service_order_id = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    parent_order = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id,
                                           service_id=DIRECT.service.id,
                                           service_order_id=service_order_id,
                                           params={'AgencyID': None, 'is_ua_optimize': 1})

    child_service_order_id = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    child_order = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id,
                                          service_id=DIRECT.service.id,
                                          service_order_id=child_service_order_id,
                                          params={'AgencyID': None})

    steps.OrderSteps.merge(parent_order=parent_order, sub_orders_ids=[child_order])

    steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id,
                            service_id=DIRECT.service.id,
                            service_order_id=child_service_order_id,
                            params={'AgencyID': None, 'GroupServiceOrderID': None})


def test3():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=NOW - datetime.timedelta(days=1))
    person_id = steps.PersonSteps.create(client_id, DIRECT.person_type.code)
    parent_service_order_id = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    parent_order = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id,
                                           service_id=DIRECT.service.id,
                                           service_order_id=parent_service_order_id,
                                           params={'AgencyID': None, 'is_ua_optimize': 1})

    child_service_order_id = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    child_order = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id,
                                          service_id=DIRECT.service.id,
                                          service_order_id=child_service_order_id,
                                          params={'AgencyID': None})

    steps.OrderSteps.merge(parent_order=parent_order, sub_orders_ids=[child_order])

    parent_service_order_id2 = steps.OrderSteps.next_id(service_id=DIRECT.service.id)
    parent_order = steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id,
                                           service_id=DIRECT.service.id,
                                           service_order_id=parent_service_order_id2,
                                           params={'AgencyID': None, 'is_ua_optimize': 1})

    steps.OrderSteps.create(client_id=client_id, product_id=DIRECT.product.id,
                            service_id=DIRECT.service.id,
                            service_order_id=child_service_order_id,
                            params={'AgencyID': None, 'GroupServiceOrderID': parent_service_order_id2})
