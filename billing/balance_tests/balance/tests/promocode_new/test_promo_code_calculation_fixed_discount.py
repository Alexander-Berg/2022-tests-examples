# -*- coding: utf-8 -*-
import datetime

import hamcrest
import pytest
from decimal import Decimal
import btestlib.utils as utils
from balance import balance_steps as steps
import btestlib.reporter as reporter
from balance.features import Features
from btestlib.constants import PromocodeClass, ProductTypes
from promocode_commons import DIRECT_YANDEX_FIRM_RUB, DIRECT_KZ_FIRM_KZU, DIRECT_BEL_FIRM_BYN_QUASI, \
    DIRECT_KZ_FIRM_KZU_QUASI, DT_1_DAY_AFTER, DT_1_DAY_BEFORE, create_and_reserve_promocode, \
    fill_calc_params_fixed_discount, create_request, is_request_with_promocode, is_invoice_with_promocode, \
    check_invoice_consumes_discount, DIRECT_YANDEX_FIRM_FISH, DIRECT_YANDEX_FIRM_FISH_NON_RES, \
    DIRECT_KZ_FIRM_FISH, DIRECT_TR_FIRM_TRY, DIRECT_SW_FIRM_CHF, create_invoice, check_invoice_rows_discount, NOW, \
    create_order

QTY = Decimal('10')
QTY_FIRST_ORDER = Decimal('6')
QTY_SECOND_ORDER = Decimal('4')

pytestmark = [reporter.feature(Features.PROMOCODE)]


@pytest.mark.parametrize('context', [
    DIRECT_YANDEX_FIRM_RUB,
    DIRECT_KZ_FIRM_KZU,
    DIRECT_BEL_FIRM_BYN_QUASI,
    DIRECT_KZ_FIRM_KZU_QUASI,
    DIRECT_YANDEX_FIRM_FISH,
    DIRECT_YANDEX_FIRM_FISH_NON_RES,
    DIRECT_KZ_FIRM_FISH,
    # DIRECT_TR_FIRM_TRY,
    DIRECT_SW_FIRM_CHF
])
@pytest.mark.parametrize('discount_pct', [5.5])
@pytest.mark.parametrize('adjust_quantity', [True])
def test_fixed_discount_check_apply_on_pay(context, discount_pct, adjust_quantity):
    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                              currency=context.currency.iso_code, region_id=context.region.id
                                              )
    calc_params = fill_calc_params_fixed_discount(discount_pct=discount_pct, apply_on_create=False,
                                                  adjust_quantity=adjust_quantity)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_DISCOUNT,
                                                                client_id=client_id,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                service_ids=[context.service.id],
                                                                start_dt=DT_1_DAY_BEFORE)
    service_order_id, _ = create_order(context=context, agency_id=None, client_id=client_id)
    service_order_id_2, _ = create_order(context=context, agency_id=None, client_id=client_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY_FIRST_ORDER, 'BeginDT': NOW},
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': QTY_SECOND_ORDER,
         'BeginDT': NOW}]
    request_id, _ = create_request(context, client_id, orders_list=orders_list)
    utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(False))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_invoice(request_id, person_id, paysys_id=context.paysys.id)
    check_invoice_rows_discount(classname=PromocodeClass.FIXED_DISCOUNT, discount_pct=0, invoice_id=invoice_id,
                                precision=context.precision,
                                qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                apply_on_create=False, nds=context.nds, nds_included=context.nds_included)
    steps.InvoiceSteps.pay(invoice_id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    check_invoice_consumes_discount(classname=PromocodeClass.FIXED_DISCOUNT, discount_pct=discount_pct,
                                    invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                    apply_on_create=False)


@pytest.mark.parametrize('context', [
    DIRECT_YANDEX_FIRM_RUB,
    DIRECT_KZ_FIRM_KZU,
    DIRECT_BEL_FIRM_BYN_QUASI,
    DIRECT_KZ_FIRM_KZU_QUASI,
    DIRECT_YANDEX_FIRM_FISH,
    DIRECT_YANDEX_FIRM_FISH_NON_RES,
    DIRECT_KZ_FIRM_FISH,
    # DIRECT_TR_FIRM_TRY,
    DIRECT_SW_FIRM_CHF
])
@pytest.mark.parametrize('discount_pct', [5.5])
def test_fixed_discount_apply_on_create_adjust_quantity(context, discount_pct):
    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                              currency=context.currency.iso_code, region_id=context.region.id
                                              )
    calc_params = fill_calc_params_fixed_discount(discount_pct=discount_pct, apply_on_create=True,
                                                  adjust_quantity=True)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_DISCOUNT,
                                                                client_id=None,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                service_ids=[context.service.id],
                                                                start_dt=DT_1_DAY_BEFORE)

    service_order_id, _ = create_order(context=context, agency_id=None, client_id=client_id)
    service_order_id_2, _ = create_order(context=context, agency_id=None, client_id=client_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY_FIRST_ORDER, 'BeginDT': NOW},
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': QTY_SECOND_ORDER,
         'BeginDT': NOW}]
    request_id, _ = create_request(context, client_id, orders_list=orders_list, promocode_code=promocode_code)
    utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(True))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_invoice(request_id, person_id, paysys_id=context.paysys.id)
    check_invoice_rows_discount(classname=PromocodeClass.FIXED_DISCOUNT,
                                discount_pct=discount_pct, invoice_id=invoice_id, precision=context.precision,
                                qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                apply_on_create=True, nds=context.nds,
                                nds_included=context.nds_included)
    steps.InvoiceSteps.pay(invoice_id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    check_invoice_consumes_discount(classname=PromocodeClass.FIXED_DISCOUNT, discount_pct=discount_pct,
                                    invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                    apply_on_create=True, nds=context.nds,
                                    nds_included=context.nds_included)


@pytest.mark.parametrize('context', [
    DIRECT_YANDEX_FIRM_RUB,
    DIRECT_KZ_FIRM_KZU,
    DIRECT_BEL_FIRM_BYN_QUASI,
    DIRECT_KZ_FIRM_KZU_QUASI,
    DIRECT_YANDEX_FIRM_FISH,
    DIRECT_YANDEX_FIRM_FISH_NON_RES,
    DIRECT_KZ_FIRM_FISH,
    # DIRECT_TR_FIRM_TRY,
    DIRECT_SW_FIRM_CHF
])
@pytest.mark.parametrize('discount_pct', [5.5])
def test_fixed_discount_apply_on_create_adjust_sum(context, discount_pct):
    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                              currency=context.currency.iso_code, region_id=context.region.id
                                              )
    calc_params = fill_calc_params_fixed_discount(discount_pct=discount_pct, apply_on_create=True,
                                                  adjust_quantity=False)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_DISCOUNT,
                                                                client_id=None,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                service_ids=[context.service.id],
                                                                start_dt=DT_1_DAY_BEFORE)

    service_order_id, _ = create_order(context=context, agency_id=None, client_id=client_id)
    service_order_id_2, _ = create_order(context=context, agency_id=None, client_id=client_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY_FIRST_ORDER, 'BeginDT': NOW},
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': QTY_SECOND_ORDER,
         'BeginDT': NOW}]
    request_id, _ = create_request(context, client_id, promocode_code=promocode_code, orders_list=orders_list)
    utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(True))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_invoice(request_id, person_id, paysys_id=context.paysys.id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    check_invoice_rows_discount(discount_pct=discount_pct, invoice_id=invoice_id, precision=context.precision,
                                qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=False,
                                apply_on_create=True, nds=context.nds,classname=PromocodeClass.FIXED_DISCOUNT,
                                nds_included=context.nds_included)
    steps.InvoiceSteps.pay(invoice_id)

    check_invoice_consumes_discount(discount_pct=discount_pct, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=False,
                                    apply_on_create=True, nds=context.nds,classname=PromocodeClass.FIXED_DISCOUNT,
                                    nds_included=context.nds_included)
