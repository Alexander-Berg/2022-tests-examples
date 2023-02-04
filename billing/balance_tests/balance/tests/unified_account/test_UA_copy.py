# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import equal_to

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils

dt = datetime.datetime.now() - datetime.timedelta(days=1)

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
CURRENCY_PRODUCT_ID = 503162
NON_CURRENCY_PRODUCT_ID = 1475
NON_CURRENCY_MSR = 'Bucks'
CURRENCY_MSR = 'Money'


def making_branches(cons_ships_on_fish_order):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    fish_orders_list = []
    currency_orders_list = []
    fish_order_id_list = []
    for order in cons_ships_on_fish_order:
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        order.update({'service_order_id': service_order_id})
        order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
        fish_orders_list.append(
            {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': order['cons_qty'], 'BeginDT': dt}
        )
        fish_order_id_list.append(order_id)

    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=fish_orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    for order in cons_ships_on_fish_order:
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, order['service_order_id'], {NON_CURRENCY_MSR: order['ships_qty']},
                                          0, dt)
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=dt)

    for order in cons_ships_on_fish_order:
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        order_id = steps.OrderSteps.create(client_id, service_order_id, CURRENCY_PRODUCT_ID, SERVICE_ID)
        steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {CURRENCY_MSR: order['ships_on_cur_copy']}, 0,
                                          dt)
        currency_orders_list.append(order_id)

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    main_order = steps.OrderSteps.create(client_id, service_order_id, CURRENCY_PRODUCT_ID, SERVICE_ID)

    for fish_order, currency_order in zip(fish_order_id_list, currency_orders_list):
        steps.OrderSteps.merge(currency_order, sub_orders_ids=[fish_order], group_without_transfer=1)
        steps.OrderSteps.merge(main_order, sub_orders_ids=[currency_order], group_without_transfer=1)

    steps.CommonSteps.export('UA_TRANSFER', 'Client', client_id)

    return currency_orders_list, fish_orders_list, main_order


@pytest.mark.parametrize('cons_ships_on_fish_order', [(
        {'cons_qty': D('100'), 'ships_qty': D('20.666666'), 'ships_on_cur_copy': D('1220.2'),
         'expected_cons_qty': D('1220.2')}
        , {'cons_qty': D('100'), 'ships_qty': D('31.555555'), 'ships_on_cur_copy': D('3213.13'),
           'expected_cons_qty': D('3213.13')}
)])
def test_making_branches(cons_ships_on_fish_order):
    currency_orders_list, fish_orders_list, _ = making_branches(cons_ships_on_fish_order)
    print fish_orders_list
    for cur_order, fish_order in zip(currency_orders_list, cons_ships_on_fish_order):
        consume = db.get_consumes_by_order(cur_order)[-1]
        utils.check_that(D(consume['consume_qty']), equal_to(fish_order['expected_cons_qty']))
    fish_service_order_id = fish_orders_list[0]['ServiceOrderID']
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, fish_service_order_id, {'Bucks': 30}, 0,
                                      dt)


if __name__ == "__main__":
    pytest.main("test_UA_copy.py -v")
