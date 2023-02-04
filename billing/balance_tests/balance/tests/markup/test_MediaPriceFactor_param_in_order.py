# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_db as db
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import NdsNew as nds, Products, Services
from temp.igogor.balance_objects import Contexts
import btestlib.config as balance_config

pytestmark = [pytest.mark.tickets('BALANCE-21607'),
              reporter.feature(Features.XMLRPC, Features.CLIENT, Features.INVOICE)
              ]

context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MEDIA_70,
                                               product=Products.MEDIA_3)

QTY = D('30')
MEDIASELLING_PRODUCT_PRICE = D('29000')
BASE_DT = datetime.datetime.now()

PRICE_FACTOR = D('1.25')
UPDATED_PRICE_FACTOR = PRICE_FACTOR * D('2')

NDS = nds.YANDEX_RESIDENT

# TODO: add tests with transfers

@pytest.mark.smoke
def test_simple_price_factor():
    client_id, person_id, service_order_id, invoice_id = create_invoice_with_price_factor()
    steps.InvoiceSteps.pay(invoice_id)

    create_act_and_check(context.service.id, service_order_id, client_id, invoice_id, price_factor=PRICE_FACTOR)


def test_update_price_factor_before_invoice():
    client_id = None or steps.ClientSteps.create()
    person_id = None or steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id, product_id=context.product.id,
                            params={'PriceFactor': PRICE_FACTOR})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}
    ]

    # Update PriceFactor
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id, product_id=context.product.id,
                            params={'PriceFactor': UPDATED_PRICE_FACTOR})

    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    create_act_and_check(context.service.id, service_order_id, client_id, invoice_id, price_factor=UPDATED_PRICE_FACTOR)


def test_update_price_factor_before_payment():
    client_id, person_id, service_order_id, invoice_id = create_invoice_with_price_factor()

    # Update PriceFactor
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id, product_id=context.product.id,
                            params={'PriceFactor': UPDATED_PRICE_FACTOR})

    steps.InvoiceSteps.pay(invoice_id)
    create_act_and_check(context.service.id, service_order_id, client_id, invoice_id, price_factor=PRICE_FACTOR)


def test_exc_while_update_price_factor_after_payment():
    client_id, person_id, service_order_id, invoice_id = create_invoice_with_price_factor()
    steps.InvoiceSteps.pay(invoice_id)

    # Update PriceFactor
    with pytest.raises(Exception) as exc:
        steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id, product_id=context.product.id,
                                params={'PriceFactor': UPDATED_PRICE_FACTOR})
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value), equal_to('ORDER_MARKUPS_IMMUTABLE'))

    create_act_and_check(context.service.id, service_order_id, client_id, invoice_id, price_factor=PRICE_FACTOR)


# --------------------------------------------------------------------

def create_act_and_check(service_id, service_order_id, invoice_owner, invoice_id, price_factor):
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 0, 'Days': QTY, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(invoice_owner, force=1, date=BASE_DT)

    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']

    act = db.get_acts_by_invoice(invoice_id)[0]
    expected_act_amount = MEDIASELLING_PRODUCT_PRICE * price_factor * NDS.koef_on_dt(BASE_DT)
    utils.check_that(act['amount'], equal_to(expected_act_amount))


def create_invoice_with_price_factor():
    client_id = None or steps.ClientSteps.create()
    person_id = None or steps.PersonSteps.create(client_id, 'ur')

    campaigns_list = [
        {'service_id': context.service.id,
         'product_id': context.product.id,
         'qty': QTY,
         'begin_dt': BASE_DT,
         'additional_params': {'PriceFactor': PRICE_FACTOR}}
    ]

    invoice_id, _, _, orders_list = \
        steps.InvoiceSteps.create_force_invoice(client_id, person_id,
                                                campaigns_list,
                                                context.paysys.id,
                                                BASE_DT
                                                )
    service_order_id = orders_list[0]['ServiceOrderID']

    return client_id, person_id, service_order_id, invoice_id
