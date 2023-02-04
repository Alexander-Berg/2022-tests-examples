# -*- coding: utf-8 -*-
import datetime

import hamcrest
import pytest

import btestlib.utils as ut
from balance import balance_db as db
from balance import balance_steps as steps

import btestlib.reporter as reporter
from balance.features import Features


def check_paysys_id_in_paysys_list(request_id, show_disabled_paysyses=False):
    check = False
    for i in steps.RequestSteps.get_request_choices(request_id, show_disabled_paysyses=show_disabled_paysyses)[
        'paysys_list']:
        if i['id'] == 1033:
            check = True
    return check


def check_paysys_id_in_pcp_list(request_id, show_disabled_paysyses=False):
    paysyses_id_1033_list = []
    for i in steps.RequestSteps.get_request_choices(request_id, show_disabled_paysyses=show_disabled_paysyses)[
        'pcp_list']:
        for j in i['paysyses']:
            if j['id'] == 1033:
                paysyses_id_1033_list.append(
                    {'disabled_reasons': j['disabled_reasons'], 'payment_limit': j['payment_limit']})
    return paysyses_id_1033_list


@reporter.feature(Features.TO_UNIT)
@pytest.mark.tickets('BALANCE-27962')
def test_unable_to_pay_over_limit():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    service_order_id = steps.OrderSteps.next_id(service_id=7)
    SERVICE_ID = 7
    PRODUCT_ID = 1475
    NOW = datetime.datetime.now()
    PAYSYS_ID = 1033
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=SERVICE_ID,
                                       product_id=PRODUCT_ID, params={'AgencyID': None})
    orders_list = [{'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 10000, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))

    try:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, PAYSYS_ID, credit=0,
                                                     contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.turn_on(invoice_id)
    except Exception as exc:
        print exc
        ut.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('PAYSYS_LIMIT_EXCEEDED'))
    else:
        assert False
