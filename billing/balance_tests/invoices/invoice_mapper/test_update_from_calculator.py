# -*- coding: utf-8 -*-

import datetime
import pytest
from decimal import Decimal as D
from butils.decimal_unit import DecimalUnit as DU
from balance import muzzle_util as ut
from balance.mapper import Invoice, PersonalAccount, TaxPolicyPct
from balance.constants import PaymentMethodIDs
from tests.balance_tests.invoices.invoice_factory.invoice_factory_common import (create_paysys, create_client,
                                                                                 create_person, create_agency,
                                                                                 create_person_category, create_firm,
                                                                                 create_currency,
                                                                                 create_service, create_request,
                                                                                 create_currency_rate, create_manager,
                                                                                 create_order, create_product,
                                                                                 create_contract, create_price_tax_rate,
                                                                                 create_tax_policy_pct,
                                                                                 create_tax_policy)

BANK = PaymentMethodIDs.bank
NOW = datetime.datetime.now()
DIRECT_DISCOUNT_TYPE = 7
DIRECT_COMMISSION_TYPE = 7


def check_invoice_order(invoice_order, request_order):
    assert invoice_order.order == request_order.order


def test_update_from_calculator(session, client, firm, currency):
    request = create_request(session, client, dt=NOW)
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=1)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
    invoice = Invoice(request=request, paysys=paysys, client=client, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])
    invoice.update_head_attributes()
    calc = invoice.get_invoice_calculator(qty_is_amount=False)
    invoice.update_from_calculator(calc.calc())
    assert invoice.invoice_orders[0].quantity == DU('1.000000', 'QTY')
    assert invoice.invoice_orders[0].initial_quantity == DU('1.000000', 'QTY')
    assert invoice.invoice_orders[0].price == DU('1.00', currency.char_code, [D('1'), 'QTY'])
    assert invoice.invoice_orders[0].price_wo_nds == DU('0.81', currency.char_code, [D('1'), 'QTY'])
    assert invoice.invoice_orders[0].amount == DU('1.00', currency.char_code)
    assert invoice.invoice_orders[0].amount_no_discount == DU('1.00', currency.char_code)
    assert invoice.invoice_orders[0].amount_nds == DU('0.18', currency.char_code)
    assert invoice.invoice_orders[0].amount_nsp == DU('0.02', currency.char_code)
    assert invoice.invoice_orders[0].effective_sum == DU('1.00', 'FISH')


def test_agency(session, client, agency, firm, currency):
    request = create_request(session, agency, dt=NOW, orders=[create_order(session, client=client, agency=agency)])
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=1)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    create_price_tax_rate(session, request.request_orders[0].order.product, firm.country, currency)
    invoice = Invoice(request=request, paysys=paysys, client=agency, firm=firm, temporary=False,
                      basket_rows=[ro.basket_item() for ro in request.request_orders])
    invoice.update_head_attributes()
    calc = invoice.get_invoice_calculator(qty_is_amount=False)
    invoice.update_from_calculator(calc.calc())
    assert invoice.invoice_orders[0].client == client
    assert invoice.client == agency


def test_forced_tax_policy_pct(session, person, firm, currency):
    tax_policy = create_tax_policy(session, resident=1, country=firm.country)
    tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=None, nds_pct=D('16661.33'),
                                           nsp_pct=None)
    paysys = create_paysys(session, iso_currency=currency.iso_code,
                           currency=currency.char_code, nds=1)

    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    invoice = PersonalAccount(paysys=paysys, client=person.client, person=person, firm=firm, temporary=False)
    invoice.update_head_attributes()
    calc = invoice.get_invoice_calculator(qty_is_amount=False, params={'tax_policy_pct': tax_policy_pct})
    invoice.update_from_calculator(calc.calc())
    assert invoice.nds == 1
    assert invoice.nds_pct == D('16661.33')
