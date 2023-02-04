import pytest
import hamcrest
import datetime
from decimal import Decimal

from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, Nds
from balance import balance_steps as steps
from btestlib.constants import Paysyses, Regions, Products, Currencies
from balance import balance_db as db
import btestlib.reporter as reporter
from balance.features import Features

DIRECT_YANDEX_FIRM_FISH_BANK = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, precision=Decimal('0.000001'),
                                                                    nds=Nds.YANDEX_RESIDENT, nds_included=True,
                                                                    paysys=Paysyses.BANK_UR_RUB)
DIRECT_YANDEX_FIRM_FISH_CC = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, precision=Decimal('0.000001'),
                                                                  nds=Nds.YANDEX_RESIDENT, nds_included=True,
                                                                  paysys=Paysyses.CC_UR_RUB)

DIRECT_YANDEX_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_RUB,
                                                              region=Regions.RU, currency=Currencies.RUB)

DT_18_NDS = datetime.datetime(2018, 11, 1)
DT_20_NDS = datetime.datetime(2019, 1, 1)
NOW = datetime.datetime.now()


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH_BANK, DIRECT_YANDEX_FIRM_FISH_CC])
def test_nds_recalc_after_payment(context):
    client_id = steps.ClientSteps.create()
    steps.OverdraftSteps.set_force_overdraft(client_id=client_id, service_id=7, limit=100)

    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                       product_id=context.product.id,
                                       service_id=context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT_18_NDS}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    assert db.get_invoice_by_id(invoice_id)[0]['nds_pct'] == 20
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 50}, 0, DT_18_NDS)
    db.get_consumes_by_invoice(invoice_id)[0]['tax_policy_pct_id'] = 1
    steps.InvoiceSteps.turn_on(invoice_id)
    db.get_consumes_by_invoice(invoice_id)[0]['tax_policy_pct_id'] = 301

    #
    # assert db.get_invoice_by_id(invoice_id)[0]['nds_pct'] == 18
    # act_id = steps.ActsSteps.generate(client_id, force=1, date=NOW)[0]


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_RUB])
def test_nds_recalc_after_payment_currency(context):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY',
                                          currency=context.currency.iso_code,
                                          region_id=context.region.id)
    steps.OverdraftSteps.set_force_overdraft(client_id=client_id, service_id=7, limit=100, currency='RUB')

    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                       product_id=context.product.id,
                                       service_id=context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': DT_18_NDS}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)

    assert db.get_invoice_by_id(invoice_id)[0]['nds_pct'] == 20
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 50}, 0, DT_18_NDS)
    db.get_consumes_by_invoice(invoice_id)[0]['tax_policy_pct_id'] = 1
    steps.InvoiceSteps.turn_on(invoice_id)
    db.get_consumes_by_invoice(invoice_id)[0]['tax_policy_pct_id'] = 301

    #
    # assert db.get_invoice_by_id(invoice_id)[0]['nds_pct'] == 18
    # act_id = steps.ActsSteps.generate(client_id, force=1, date=NOW)[0]
