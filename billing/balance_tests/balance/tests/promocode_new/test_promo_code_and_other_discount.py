# -*- coding: utf-8 -*-
import datetime

import hamcrest

import pytest
from decimal import Decimal
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_db as db
import btestlib.reporter as reporter
from balance.features import Features
from btestlib.constants import PromocodeClass
from promocode_commons import DIRECT_YANDEX_FIRM_RUB, DT_1_DAY_AFTER, DT_1_DAY_BEFORE, create_and_reserve_promocode, \
    fill_calc_params_fixed_discount, NOW, calculate_qty_with_static_discount, multiply_discount, \
    calculate_sum_with_static_discount, get_amount
from btestlib.matchers import contains_dicts_with_entries
import btestlib.config as balance_config

AGENCY_DISCOUNT_PCT = 15
PROMOCODE_DISCOUNT_PCT = 10
QTY = 20

pytestmark = [reporter.feature(Features.PROMOCODE)]


@pytest.mark.parametrize('agency_discount_adjust_qty, promocode_adjust_qty', [
    (True, True),
    (False, True),
    (True, False),
    (False, False)
])
@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_RUB])
def test_multiple_discount_promo_apply_on_create(context, agency_discount_adjust_qty, promocode_adjust_qty):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                          currency=context.currency.iso_code, region_id=context.region.id
                                          )
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                    'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})
    calc_params = fill_calc_params_fixed_discount(discount_pct=PROMOCODE_DISCOUNT_PCT, apply_on_create=True,
                                                  adjust_quantity=promocode_adjust_qty)
    create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_DISCOUNT,
                                 client_id=client_id,
                                 firm_id=context.firm.id,
                                 calc_params=calc_params,
                                 end_dt=DT_1_DAY_AFTER,
                                 service_ids=[context.service.id],
                                 start_dt=DT_1_DAY_BEFORE)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0,
                                                 agency_discount=AGENCY_DISCOUNT_PCT,
                                                 adjust_qty=agency_discount_adjust_qty)
    if agency_discount_adjust_qty and promocode_adjust_qty:
        expected_qty_promo = calculate_qty_with_static_discount(qty=QTY, discount=PROMOCODE_DISCOUNT_PCT,
                                                                precision=Decimal(context.precision))
        expected_qty = calculate_qty_with_static_discount(qty=expected_qty_promo,
                                                          discount=AGENCY_DISCOUNT_PCT,
                                                          precision=Decimal(context.precision))
        result_discount = multiply_discount(PROMOCODE_DISCOUNT_PCT, AGENCY_DISCOUNT_PCT)
        invoice_row = db.get_invoice_orders_by_invoice_id(invoice_id)[0]
        expected_sum = utils.dround2(expected_qty * invoice_row['internal_price'])
        sum_before = QTY * invoice_row['internal_price']
        utils.check_that([invoice_row], contains_dicts_with_entries([{'quantity': expected_qty,
                                                                      'discount_pct': result_discount,
                                                                      'amount_no_discount': expected_sum,
                                                                      'amount': sum_before}]))
    elif agency_discount_adjust_qty or promocode_adjust_qty:
        discount_to_qty = PROMOCODE_DISCOUNT_PCT if promocode_adjust_qty else AGENCY_DISCOUNT_PCT
        discount_to_sum = AGENCY_DISCOUNT_PCT if promocode_adjust_qty else PROMOCODE_DISCOUNT_PCT
        expected_qty_promo = calculate_qty_with_static_discount(qty=QTY, discount=discount_to_qty,
                                                                precision=Decimal(context.precision))
        result_discount = multiply_discount(discount_to_sum, discount_to_qty)
        invoice_row = db.get_invoice_orders_by_invoice_id(invoice_id)[0]
        expected_sum = utils.dround2(expected_qty_promo * invoice_row['internal_price'])
        sum_no_discount = utils.dround2(expected_sum * (Decimal('1') - result_discount / Decimal('100')))
        utils.check_that([invoice_row], contains_dicts_with_entries([{'quantity': expected_qty_promo,
                                                                      'discount_pct': result_discount,
                                                                      'amount_no_discount': expected_sum,
                                                                      'amount': sum_no_discount}]))
    elif not (agency_discount_adjust_qty and promocode_adjust_qty):
        result_discount = multiply_discount(PROMOCODE_DISCOUNT_PCT, AGENCY_DISCOUNT_PCT)

        invoice_row = db.get_invoice_orders_by_invoice_id(invoice_id)[0]
        sum_ = utils.dround2(QTY * invoice_row['internal_price'])
        expected_sum = utils.dround2(calculate_sum_with_static_discount(sum_, result_discount))
        utils.check_that([invoice_row], contains_dicts_with_entries([{'quantity': QTY,
                                                                      'discount_pct': result_discount,
                                                                      'amount_no_discount': sum_,
                                                                      'amount': expected_sum}]))


@pytest.mark.parametrize('agency_discount_adjust_qty, promocode_adjust_qty', [
    (True, True),
    (False, True)
])
@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_RUB])
def test_multiple_discount_promo_apply_on_pay(context, agency_discount_adjust_qty, promocode_adjust_qty):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                          currency=context.currency.iso_code, region_id=context.region.id
                                          )
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                    'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})
    calc_params = fill_calc_params_fixed_discount(discount_pct=PROMOCODE_DISCOUNT_PCT, apply_on_create=False,
                                                  adjust_quantity=promocode_adjust_qty)
    create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_DISCOUNT,
                                 client_id=client_id,
                                 firm_id=context.firm.id,
                                 calc_params=calc_params,
                                 end_dt=DT_1_DAY_AFTER,
                                 service_ids=[context.service.id],
                                 start_dt=DT_1_DAY_BEFORE)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0,
                                                 agency_discount=AGENCY_DISCOUNT_PCT,
                                                 adjust_qty=agency_discount_adjust_qty)
    if (agency_discount_adjust_qty, promocode_adjust_qty) in [(True, True)]:
        expected_qty_agency_discount = calculate_qty_with_static_discount(qty=QTY, discount=AGENCY_DISCOUNT_PCT,
                                                                          precision=Decimal(context.precision))
        invoice_row = db.get_invoice_orders_by_invoice_id(invoice_id)[0]
        expected_sum = utils.dround2(expected_qty_agency_discount * invoice_row['internal_price'])
        sum_before = QTY * invoice_row['internal_price']
        utils.check_that([invoice_row], contains_dicts_with_entries([{'quantity': expected_qty_agency_discount,
                                                                      'discount_pct': AGENCY_DISCOUNT_PCT,
                                                                      'amount_no_discount': expected_sum,
                                                                      'amount': sum_before}]))
    else:
        invoice_row = db.get_invoice_orders_by_invoice_id(invoice_id)[0]
        sum_ = get_amount(QTY, nds=context.nds, internal_price=invoice_row['internal_price'], nds_included=context.nds_included)
        expected_sum = utils.dround2(calculate_sum_with_static_discount(sum_, AGENCY_DISCOUNT_PCT))
        utils.check_that([invoice_row], contains_dicts_with_entries([{'quantity': QTY,
                                                                      'discount_pct': AGENCY_DISCOUNT_PCT,
                                                                      'amount_no_discount': sum_,
                                                                      'amount': expected_sum,
                                                                      'effective_sum': expected_sum}]))
    steps.InvoiceSteps.pay(invoice_id=invoice_id)
    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    if (agency_discount_adjust_qty, promocode_adjust_qty) in [(True, True)]:
        consume = db.get_consumes_by_invoice(invoice_id)[0]
        expected_qty_promo = calculate_qty_with_static_discount(qty=QTY, discount=PROMOCODE_DISCOUNT_PCT,
                                                                precision=Decimal(context.precision))
        expected_qty = calculate_qty_with_static_discount(qty=expected_qty_promo,
                                                          discount=AGENCY_DISCOUNT_PCT,
                                                          precision=Decimal(context.precision))
        result_discount = multiply_discount(PROMOCODE_DISCOUNT_PCT, AGENCY_DISCOUNT_PCT)
        utils.check_that([consume], contains_dicts_with_entries([{'current_qty': expected_qty,
                                                                  'discount_pct': result_discount,
                                                                  'static_discount_pct': result_discount,
                                                                  'consume_qty': expected_qty}]))
    else:
        discount_to_qty = PROMOCODE_DISCOUNT_PCT if promocode_adjust_qty else AGENCY_DISCOUNT_PCT
        discount_to_sum = AGENCY_DISCOUNT_PCT if promocode_adjust_qty else PROMOCODE_DISCOUNT_PCT
        expected_qty_promo = calculate_qty_with_static_discount(qty=QTY, discount=discount_to_qty,
                                                                precision=Decimal(context.precision))
        result_discount = multiply_discount(discount_to_sum, discount_to_qty)
        consume = db.get_consumes_by_invoice(invoice_id)[0]
        expected_sum = utils.dround2(expected_qty_promo * invoice_row['internal_price'])
        sum_no_discount = utils.dround2(expected_sum * (Decimal('1') - result_discount / Decimal('100')))
        utils.check_that([consume], contains_dicts_with_entries([{'current_qty': expected_qty_promo,
                                                                  'discount_pct': result_discount,
                                                                  'static_discount_pct': result_discount,
                                                                  'consume_qty': expected_qty_promo}]))
