import pytest
import datetime
import pprint

from balance import balance_steps as steps
from btestlib.constants import Services, Products, PersonTypes, Paysyses, Firms
from temp.igogor.balance_objects import Contexts, Regions
from btestlib import utils
from simpleapi.matchers import deep_equals as de

DIRECT_CLIENT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(with_agency=0)
MARKET_CLIENT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(with_agency=0, service=Services.MARKET)
SHOP = Contexts.DIRECT_FISH_RUB_CONTEXT.new(with_agency=0, service=Services.SHOP, product=Products.DIRECT_FISH)
NOW = datetime.datetime.now()


@pytest.mark.parametrize('context', [DIRECT_CLIENT])
def test_client_with_region(context):
    client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, 'yt')
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=7,
                                       product_id=1475, params={'AgencyID': None})
    orders_list = [{'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))


@pytest.mark.parametrize('context',
                         [
                             DIRECT_CLIENT
                         ])
def test_client_with_non_resident(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'yt')
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))


@pytest.mark.parametrize('context',
                         [
                             SHOP
                         ])
def test_client_with_non_resident_firm_assigned(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'yt_kzu')
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW, FirmID=Firms.YANDEX_1.id))
    request_choices = steps.RequestSteps.get_request_choices(request_id=request_id)
    formatted_request_choices = steps.RequestSteps.format_request_choices(request_choices)
    pprint.pprint(formatted_request_choices)
    utils.check_that(formatted_request_choices,
                     de.deep_equals_to({'without_contract': {'1': {'EUR': {'yt': [1023]},
                                                                   'RUR': {'ph': [1000, 1001],
                                                                           'ur': [1003],
                                                                           'yt': [1014, 11069]},
                                                                   'USD': {'yt': [1013]
                                                                           }}}}))


@pytest.mark.parametrize('context', [DIRECT_CLIENT.new(service=Services.CLOUD_143)])
def test_client_with_non_resident_ql(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'yt')
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=1475, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))


@pytest.mark.parametrize('context', [DIRECT_CLIENT])
def test_client_with_non_default_person(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'yt_kzu')
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=7,
                                       product_id=1475, params={'AgencyID': None})
    orders_list = [{'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))


@pytest.mark.parametrize('context', [DIRECT_CLIENT])
def test_two_services(context):
    client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, 'ur')
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=1475, params={'AgencyID': None})

    service_order_id1 = steps.OrderSteps.next_id(service_id=11)
    order_id = steps.OrderSteps.create(client_id, service_order_id1, service_id=11,
                                       product_id=1475, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW},
                   {'ServiceID': 11, 'ServiceOrderID': service_order_id1, 'Qty': 3333, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))


@pytest.mark.parametrize('context, context2', [(DIRECT_CLIENT, MARKET_CLIENT)])
def test_client_with_order_agency_invoice(context, context2):
    client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create_agency()
    person_id = steps.PersonSteps.create(agency_id, 'ur')
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': NOW})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0)
    steps.InvoiceSteps.pay(invoice_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context2.service.id,
                                       product_id=context2.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context2.service.id, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': NOW})

