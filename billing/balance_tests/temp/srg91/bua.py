# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as b_utils
from check import utils
from check.defaults import Services
from check.utils import relative_date, LAST_DAY_OF_MONTH

CHECK_CODE_NAME = 'bua'

END_OF_MONTH = relative_date(months=-1, day=LAST_DAY_OF_MONTH)
NEXT_DAY = datetime.datetime.now() + datetime.timedelta(days=1)
_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    b_utils.Date.previous_three_months_start_end_dates()


def create_bua_order():
    LAST_DAY_OF_PREVIOUS_MONTH = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
    LAST_DAY_OF_PREVIOUS_MONTH_0 = datetime.datetime.now().replace(day=1, minute=0, hour=0, second=0, microsecond=0) - datetime.timedelta(days=1)
    QTY = Decimal("300")
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ph')

    service_id = 7

    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id_1 = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=1475)
    service_order_id2 = steps.OrderSteps.next_id(service_id)
    order_id_2 = steps.OrderSteps.create(client_id, service_order_id2, service_id=service_id, product_id=1475)

    orders_list = [{
        'ServiceID': service_id,
        'ServiceOrderID': service_order_id,
        'Qty': QTY,
        'BeginDT': LAST_DAY_OF_PREVIOUS_MONTH
    }]

    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT= LAST_DAY_OF_PREVIOUS_MONTH - datetime.timedelta(days=10)))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1001, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt= LAST_DAY_OF_PREVIOUS_MONTH - datetime.timedelta(days=10))

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 200}, 0,
                                      LAST_DAY_OF_PREVIOUS_MONTH - datetime.timedelta(days=4))

    steps.ClientSteps.migrate_to_currency(client_id, 'MODIFY')
    db.balance().execute(
        """		
        update t_client_service_data		
        set migrate_to_currency = to_date( :date), update_dt = to_date( :date)		
        where class_id = :client_id		
        """,
        {'client_id': client_id, 'date': LAST_DAY_OF_PREVIOUS_MONTH - datetime.timedelta(days=3)})

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 200, 'Money': 3000}, 0,
                                      LAST_DAY_OF_PREVIOUS_MONTH - datetime.timedelta(days=2))
    steps.ActsSteps.generate(client_id, force=1, date=LAST_DAY_OF_PREVIOUS_MONTH)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 200, 'Money': 2340}, 0,
                                      LAST_DAY_OF_PREVIOUS_MONTH_0)

    steps.OrderSteps.transfer([{'order_id': order_id_1, 'qty_old': 9000, 'qty_new': 8640, 'all_qty': 0}],
                              [{'order_id': order_id_2, 'qty_delta': 1}])
    db.balance().execute(
        """
            update t_shipment 
            set update_dt = to_date('{dt}','DD.MM.YY HH24:MI:SS') 
            where 
                service_id = :service_id 
                and service_order_id = :service_order_id
        """.format(dt=LAST_DAY_OF_PREVIOUS_MONTH_0.strftime('%d.%m.%y %H:%M:%S')),
        {'service_id': service_id, 'service_order_id': service_order_id},
        descr=u'Изменяем дату открутки в t_shipment'
    )

    db.balance().execute(
        """
            update T_CONSUME 
            set act_qty = 1 
            where PARENT_ORDER_ID = :parent_order_id
        """, {'parent_order_id': order_id_1}
    )
    return order_id_1


def run_bua_cmp(order_id):
    date = END_OF_MONTH.strftime('%Y-%m-%d')

    params = {
        'completions-dt': date,
        'acts-dt': date,
        'exclude-service-ids': str(Services.ticket),
    }
    cmp_id = utils.run_check_new(CHECK_CODE_NAME, str(order_id), params)
    utils.run_auto_analyze('bua', cmp_id)


def mian():
    order_id = create_bua_order()
    run_bua_cmp(order_id)


if __name__ == '__main__':
    main()

# vim:ts=4:sts=4:sw=4:tw=79:et:
