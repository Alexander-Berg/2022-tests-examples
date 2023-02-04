import pytest
import datetime
from xmlrpclib import Fault

NOW = datetime.datetime.now()
DT_18_NDS = datetime.datetime(2018, 11, 1)
DT_20_NDS = datetime.datetime(2019, 1, 1)

from balance import balance_steps as steps
from balance import balance_db as db
from temp.igogor.balance_objects import Contexts, Firms, Products, Regions, Currencies
import btestlib.reporter as reporter
from balance.features import Features


DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)
MEDIA_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.MEDIA_509291)
DIRECT_YANDEX_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_RUB,
                                                              region=Regions.RU, currency=Currencies.RUB)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [
    DIRECT_YANDEX_FIRM_RUB])
def test_begin_dt_in_future_currency(context):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY',
                                          currency=context.currency.iso_code,
                                          region_id=context.region.id)

    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id,
                                       service_id=context.service.id,
                                       product_id=context.product.id, params={'begin_dt': DT_20_NDS})

    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
         },

    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                 credit=0, overdraft=0, contract_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    assert db.get_invoice_by_id(invoice_id)[0]['nds_pct'] == 20
