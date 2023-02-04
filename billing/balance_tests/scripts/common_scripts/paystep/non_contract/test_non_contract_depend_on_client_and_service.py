# -*- coding: utf-8 -*-

'''Единственный случай, когда без договора выставиться нельзя никак вообще.
Сервис из заказа реквеста Маркет и регион клиента - США.'''

import datetime

import hamcrest
import pytest

import balance.tests.paystep.paystep_common_steps as PaystepSteps
from balance import balance_steps as steps
from btestlib import utils as utils
from temp.igogor.balance_objects import Contexts

NOW = datetime.datetime.now()
PREVIOUS_MONTH_LAST_DAY = NOW.replace(day=1) - datetime.timedelta(days=1)

MARKET = Contexts.MARKET_RUB_CONTEXT.new()

QTY = 2


@pytest.mark.parametrize('context', [MARKET])
def test_without_contract_is_non_available_market_usa(context):
    client_id = steps.ClientSteps.create({'REGION_ID': 84})
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': PREVIOUS_MONTH_LAST_DAY}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': PREVIOUS_MONTH_LAST_DAY})
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    formatted_request_choices = PaystepSteps.format_request_choices(request_choices)
    utils.check_that(formatted_request_choices, hamcrest.empty())
