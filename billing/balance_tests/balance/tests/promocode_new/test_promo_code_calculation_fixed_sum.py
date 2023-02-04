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
from btestlib.constants import PromocodeClass, ProductTypes, Currencies
from promocode_commons import (DIRECT_YANDEX_FIRM_RUB,
                               DIRECT_KZ_FIRM_KZU,
                               DIRECT_BEL_FIRM_BYN_QUASI,
                               DIRECT_KZ_FIRM_KZU_QUASI, DT_1_DAY_AFTER, DT_1_DAY_BEFORE, create_and_reserve_promocode,
                               fill_calc_params_fixed_sum, create_request, is_request_with_promocode,
                               is_invoice_with_promocode,
                               check_invoice_consumes_discount, DIRECT_YANDEX_FIRM_FISH,
                               DIRECT_YANDEX_FIRM_FISH_NON_RES,
                               DIRECT_KZ_FIRM_FISH, create_invoice, check_invoice_rows_discount, NOW, create_order,
                               DIRECT_YANDEX_FIRM_USD,
                               DIRECT_YANDEX_FIRM_USD_FISH, VZGLYAD_YANDEX_FIRM_RUB, DIRECT_TR_FIRM_TRY,
                               DIRECT_SW_FIRM_CHF)

SUM_BONUS = 5.4
QTY = Decimal('10')
QTY_FIRST_ORDER = Decimal('6.5')
QTY_SECOND_ORDER = Decimal('3.5')

pytestmark = [reporter.feature(Features.PROMOCODE)]


@pytest.mark.parametrize('context', [
    # DIRECT_YANDEX_FIRM_USD_FISH,
    DIRECT_YANDEX_FIRM_USD,
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
@pytest.mark.parametrize('adjust_quantity', [True])
def test_fixed_sum_apply_on_pay(context, adjust_quantity):
    client_id = steps.ClientSteps.create()
    another_client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                              currency=context.currency.iso_code, region_id=context.region.id
                                              )
    calc_params = fill_calc_params_fixed_sum(currency_bonuses={context.currency.iso_code: SUM_BONUS},
                                             reference_currency=None, adjust_quantity=adjust_quantity,
                                             apply_on_create=False)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_SUM,
                                                                client_id=client_id,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                service_ids=[context.service.id],
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                binded_client=another_client_id)

    service_order_id, _ = create_order(context=context, agency_id=None, client_id=client_id)
    service_order_id_2, _ = create_order(context=context, agency_id=None, client_id=client_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY_FIRST_ORDER, 'BeginDT': NOW},
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': QTY_SECOND_ORDER,
         'BeginDT': NOW}]
    request_id, _ = create_request(context, client_id, qty=QTY, orders_list=orders_list)
    utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(False))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_invoice(request_id, person_id, paysys_id=context.paysys.id)
    check_invoice_rows_discount(fixed_sum=0, invoice_id=invoice_id, classname=PromocodeClass.FIXED_SUM,
                                precision=context.precision,
                                qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                apply_on_create=False, nds=context.nds, nds_included=context.nds_included)
    steps.InvoiceSteps.pay(invoice_id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    check_invoice_consumes_discount(fixed_sum=SUM_BONUS, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], nds=context.nds,
                                    classname=PromocodeClass.FIXED_SUM,
                                    adjust_quantity=True)


@pytest.mark.parametrize('context', [
    DIRECT_YANDEX_FIRM_USD,
    DIRECT_YANDEX_FIRM_RUB,
    DIRECT_KZ_FIRM_KZU,
    DIRECT_BEL_FIRM_BYN_QUASI,
    DIRECT_KZ_FIRM_KZU_QUASI,
    DIRECT_YANDEX_FIRM_FISH,
    DIRECT_YANDEX_FIRM_FISH_NON_RES,
    DIRECT_KZ_FIRM_FISH,
    # DIRECT_TR_FIRM_TRY,
    DIRECT_SW_FIRM_CHF])
def test_fixed_sum_apply_on_create_adjust_quantity(context):
    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                              currency=context.currency.iso_code, region_id=context.region.id
                                              )
    calc_params = fill_calc_params_fixed_sum(currency_bonuses={context.currency.iso_code: SUM_BONUS},
                                             reference_currency=None, adjust_quantity=True,
                                             apply_on_create=True)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_SUM,
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
    request_id, _ = create_request(context, client_id, qty=QTY, orders_list=orders_list)
    utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(False))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_invoice(request_id, person_id, paysys_id=context.paysys.id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    check_invoice_rows_discount(fixed_sum=SUM_BONUS, invoice_id=invoice_id, precision=context.precision,
                                qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                apply_on_create=True, nds=context.nds,
                                nds_included=context.nds_included, classname=PromocodeClass.FIXED_SUM)
    steps.InvoiceSteps.pay(invoice_id)
    check_invoice_consumes_discount(fixed_sum=SUM_BONUS, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                    apply_on_create=True, nds=context.nds,
                                    nds_included=context.nds_included, classname=PromocodeClass.FIXED_SUM)


@pytest.mark.parametrize('context', [
    VZGLYAD_YANDEX_FIRM_RUB,
    DIRECT_YANDEX_FIRM_USD,
    DIRECT_YANDEX_FIRM_RUB,
    DIRECT_KZ_FIRM_KZU,
    DIRECT_BEL_FIRM_BYN_QUASI,
    DIRECT_KZ_FIRM_KZU_QUASI,
    DIRECT_YANDEX_FIRM_FISH,
    # DIRECT_KZ_FIRM_FISH
    # DIRECT_TR_FIRM_TRY,
    # DIRECT_SW_FIRM_CHF
])
def test_fixed_sum_apply_on_create_adjust_sum(context):
    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                              currency=context.currency.iso_code, region_id=context.region.id
                                              )
    calc_params = fill_calc_params_fixed_sum(currency_bonuses={context.currency.iso_code: SUM_BONUS},
                                             reference_currency=None, adjust_quantity=False,
                                             apply_on_create=True)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_SUM,
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
    request_id, _ = create_request(context, client_id, promocode_code=promocode_code, qty=QTY, orders_list=orders_list)
    utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(True))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_invoice(request_id, person_id, paysys_id=context.paysys.id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    check_invoice_rows_discount(fixed_sum=SUM_BONUS, invoice_id=invoice_id, precision=context.precision,
                                qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=False,
                                apply_on_create=True, nds=context.nds, sum_before=getattr(context, 'sum_before', None),
                                nds_included=context.nds_included, classname=PromocodeClass.FIXED_SUM)
    steps.InvoiceSteps.pay(invoice_id)
    check_invoice_consumes_discount(fixed_sum=SUM_BONUS, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=False,
                                    apply_on_create=True, nds=context.nds,
                                    sum_before=getattr(context, 'sum_before', None),
                                    nds_included=context.nds_included, classname=PromocodeClass.FIXED_SUM)


@pytest.mark.parametrize('context', [
    DIRECT_YANDEX_FIRM_RUB])
@pytest.mark.parametrize('adjust_quantity', [True])
def fixed_sum_apply_on_pay_reference_currency_usage(context, adjust_quantity):
    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                              currency=context.currency.iso_code, region_id=context.region.id
                                              )
    calc_params = fill_calc_params_fixed_sum(currency_bonuses={'USD': SUM_BONUS},
                                             reference_currency='USD', adjust_quantity=adjust_quantity,
                                             apply_on_create=False)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_SUM,
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
    request_id, _ = create_request(context, client_id, promocode_code=promocode_code, qty=QTY, orders_list=orders_list)
    utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(True))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_invoice(request_id, person_id, paysys_id=context.paysys.id)
    check_invoice_rows_discount(fixed_sum=0, invoice_id=invoice_id, precision=context.precision,
                                qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                apply_on_create=False, nds=context.nds, nds_included=context.nds_included)
    steps.InvoiceSteps.pay(invoice_id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    rate = steps.CurrencySteps.get_currency_rate(dt=NOW, currency='USD', base_cc='RUB', rate_src_id=None)
    sum_bonus_rub = Decimal(SUM_BONUS) * Decimal(rate)
    check_invoice_consumes_discount(fixed_sum=sum_bonus_rub, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], nds=context.nds,
                                    adjust_quantity=True)
