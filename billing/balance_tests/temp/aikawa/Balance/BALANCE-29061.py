import pytest
import datetime
from temp.igogor.balance_objects import Contexts
from balance import balance_steps as steps
from btestlib.constants import Paysyses, PersonTypes, Services, Products, ContractCommissionType, Firms, \
    ContractPaymentType, Regions, Currencies

direct_context = Contexts.DIRECT_BYN_BYU_CONTEXT.new(person_type=PersonTypes.BY_YTPH, region=Regions.UZB,
                                              currency=Currencies.RUB, product=Products.DIRECT_RUB)

NOW = datetime.datetime.now()


@pytest.mark.parametrize('context', [direct_context])
def test_ust_to_byn(context):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency=context.currency.iso_code,
                                          region_id=context.region.id, currency_convert_type='COPY')
    person_id = steps.PersonSteps.create(client_id=client_id, type_=context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 1,
                    'BeginDT': NOW+datetime.timedelta(days=1)}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW+datetime.timedelta(days=1)))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=1133,
                                                 credit=0, contract_id=None, overdraft=0)

    steps.InvoiceSteps.turn_on(invoice_id)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Money': 100}, 0, datetime.datetime.now())
    act_id = steps.ActsSteps.generate(client_id, force=1, date=NOW)[0]
