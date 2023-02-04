# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime
from decimal import Decimal as D

from balance import balance_steps as steps

dt = datetime.datetime.now() - datetime.timedelta(days=1)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475
MSR = 'Bucks'


def creating_order(order_params, client_id, person_id, service_id, product_id, msr):
    print '***********************************************'
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=product_id, service_id=service_id)
    if 'with_payment' in order_params:
        orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': order_params['with_payment'], 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=dt))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    if 'with_shipment' in order_params:
        steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {msr: order_params['with_shipment']}, 0, dt)
    return order_id


def creating_tree(order_params):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    for order in order_params:
        order_id = creating_order(order, client_id, person_id, SERVICE_ID, PRODUCT_ID, MSR)
        order['order_id'] = order_id
    for order in order_params:
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID)
        steps.OrderSteps.make_optimized(order_id)
        order['copy_order_id'] = order_id
        print order
    parent_service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    parent_order_id = steps.OrderSteps.create(client_id, parent_service_order_id, PRODUCT_ID, SERVICE_ID)
    steps.OrderSteps.make_optimized(parent_order_id)

    for order in order_params:
        steps.OrderSteps.merge(order['order_id'], sub_orders=[order['order_id']], group_without_transfer=1)
        steps.OrderSteps.merge(parent_order_id, sub_orders=[order['order_id']], group_without_transfer=1)

    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

    return order_params


order_params = [{'with_shipment': D('100.4545'), 'expected_cons_qty': [{'current_qty': D('0')}]}
                # , {'with_shipment': D('50.123'), 'expected_cons_qty': [{'current_qty': D('50.123')}]}
                # , {'with_shipment': D('50.3315'), 'expected_cons_qty': [{'current_qty': D('50.3315')}]}
                ]

creating_tree(order_params=order_params)



# @pytest.mark.parametrize('cons_ships_on_fish_order',  [pytest.mark.xfail(reason = 'BALANCE-21746')(
#                                                     {'cons_qty': D('100'), 'ships_qty': D('20.666666'), 'ships_on_cur_copy': D('0')}
#                                                     ,{'cons_qty': D('100'), 'ships_qty': D('31.555555'), 'ships_on_cur_copy': D('0')}
#                                                     )])
#
# def test_making_branches(cons_ships_on_fish_order):
#     ships_sum = 0
#     currency_orders_list, main_order = making_branches(cons_ships_on_fish_order)
#     for num, item in enumerate(currency_orders_list):
#         ships_sum+=item['ships_qty']
#     utils.check_that([{'consume_qty': ships_sum}],
#                 mtch.EndsMatch(mtch.get_consumes_by_order(item)))


# if __name__ == "__main__":
#     pytest.main("test_UA_copy.py -v")


# cons_ships_on_fish_order=[
#                                                     {'cons_qty': D('1'), 'ships_qty': D('20.666666'), 'ships_on_cur_copy': D('0')}
#                                                     ,{'cons_qty': D('1'), 'ships_qty': D('31.555555'), 'ships_on_cur_copy': D('0')}
#                         ]
#
# making_branches(cons_ships_on_fish_order)
