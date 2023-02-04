import pytest
import datetime

from balance import balance_steps as steps
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions

NOW = datetime.datetime.now()
DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH])
def test_migrate(context):
    client_id = steps.ClientSteps.create()
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, 'ur')
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    parent_order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                              product_id=context.product.id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 3333, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    child_service_order_id = steps.OrderSteps.next_id(context.service.id)
    child_order_id = steps.OrderSteps.create(client_id, child_service_order_id, service_id=context.service.id,
                                             product_id=context.product.id, params={'AgencyID': agency_id})
    steps.OrderSteps.merge(parent_order_id, [child_order_id], group_without_transfer=0)
    steps.CampaignsSteps.do_campaigns(context.service.id, child_service_order_id, {'Bucks': 50}, 0,
                                      NOW - datetime.timedelta(days=1))
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY',
                                          service_id=context.service.id,
                                          region_id=Regions.RU.id, currency=context.currency.iso_code,
                                          dt=NOW + datetime.timedelta(hours=1))
    # steps.CommonSteps.export('UA_TRANSFER', 'Client',  agency_id)
