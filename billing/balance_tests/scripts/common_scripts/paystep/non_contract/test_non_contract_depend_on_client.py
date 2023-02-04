# -*- coding: utf-8 -*-
import datetime
import hamcrest
import pytest
import collections

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Services, Products
from temp.igogor.balance_objects import Contexts
from btestlib import utils as utils
import balance.tests.paystep.paystep_common_steps as PaystepSteps
from simpleapi.matchers import deep_equals as de

'''
1) test_sub_client_non_resident:
- нельзя выставиться без договора с субклиентом-нерезидентом ни клиенту, ни агенству

2) test_client_force_contractless_invoice
Если у сервиса нет признака allowed_agency_without_contract и есть признак contract_needed,
агенствам можно выставляться без договора, если у клиента в заказах реквеста есть признак force_contractless_invoice
 ПРОВЕРКИ:
 - Агенства имеют доступные способы оплаты
 - Клиенты имеют доступные способы оплаты
'''

QTY = 100

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()
OFD = DIRECT.new(service=Services.OFD, product=Products.OFD_BUCKS)

NOW = datetime.datetime.now()
PREVIOUS_MONTH_LAST_DAY = NOW.replace(day=1) - datetime.timedelta(days=1)


def get_request_choices(request_id):
    request_choices = steps.RequestSteps.get_request_choices(request_id)
    return PaystepSteps.format_request_choices(request_choices)


def rec_dd():
    return collections.defaultdict(rec_dd)


def create_order_and_request(context, with_agency):
    client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1}) if with_agency else None
    order_owner = client_id
    invoice_owner = agency_id or client_id
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=order_owner, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 2,
         'BeginDT': PREVIOUS_MONTH_LAST_DAY}]
    request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': PREVIOUS_MONTH_LAST_DAY})
    return request_id


@pytest.mark.parametrize('context', [DIRECT])
@pytest.mark.parametrize('with_agency', [True, False])
def test_sub_client_non_resident(with_agency, context):
    result = rec_dd()
    client_id = steps.ClientSteps.create_sub_client_non_resident('USD')
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1}) if with_agency else None
    order_owner = client_id
    invoice_owner = agency_id or client_id
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=order_owner, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})
    result['with_subclient_non_resident'] = get_request_choices(request_id)
    db.balance().execute('''UPDATE (SELECT * FROM t_client WHERE id = :client_id) SET IS_NON_RESIDENT = 0''',
                         {'client_id': client_id})
    result['without_subclient_non_resident'] = get_request_choices(request_id)
    utils.check_that(result['with_subclient_non_resident'], hamcrest.empty())
    utils.check_that(result['without_subclient_non_resident'], hamcrest.is_not(hamcrest.empty()))


@pytest.mark.parametrize('context', [OFD])
@pytest.mark.parametrize('with_agency', [True,
                                         False])
def test_client_force_contractless_invoice(with_agency, context):
    client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1}) if with_agency else None
    order_owner = client_id
    invoice_owner = agency_id or client_id
    steps.CommonSteps.set_extprops('Client', invoice_owner, 'force_contractless_invoice', {'value_num': 1})
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=order_owner, product_id=context.product.id, service_id=context.service.id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id=invoice_owner, orders_list=orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    utils.check_that(get_request_choices(request_id), hamcrest.is_not(hamcrest.empty()))
