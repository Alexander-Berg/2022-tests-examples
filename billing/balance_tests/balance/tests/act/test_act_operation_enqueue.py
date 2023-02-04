__author__ = 'aikawa'
import datetime

import pytest
from hamcrest import not_none, equal_to, none

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib import reporter
from balance.features import Features

dt = datetime.datetime.now()

PERSON_TYPE = 'ur'
PAYSYS_ID = 1003
SERVICE_ID = 7
PRODUCT_ID = 1475


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('force_value, operation_type',
                         [(1, 8),
                          (0, 9)]
                         )
def test_act_enqueuer(force_value, operation_type):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                            service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 200}, 0, dt)
    steps.ActsSteps.enqueue([client_id], force=force_value, date=dt)
    input_value = steps.CommonSteps.get_pickled_value('''select state, export_dt, input from t_export where type = 'MONTH_PROC' and object_id = {0} and classname = 'Client'
    '''.format(client_id))
    operation_id = input_value['enq_operation_id']
    operation = db.get_operation_by_id(operation_id)[0]
    utils.check_that(operation['insert_traceback_id'], none())
    utils.check_that(operation['create_traceback_id'], not_none())
    utils.check_that(operation['passport_id'], none())
    utils.check_that(operation['type_id'], equal_to(operation_type))
    utils.check_that(operation['parent_operation_id'], none())
    utils.check_that(operation['invoice_id'], none())
