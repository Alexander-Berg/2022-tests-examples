# -*- coding: utf-8 -*-
import datetime

import hamcrest
import pytest
from decimal import Decimal
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_db as db
from btestlib.constants import PromocodeClass, ProductTypes
from promocode_commons import (DIRECT_YANDEX_FIRM_RUB, DIRECT_KZ_FIRM_KZU, DIRECT_BEL_FIRM_BYN_QUASI,
                               fill_calc_params_act_bonus, DT_1_DAY_AFTER, DT_1_DAY_BEFORE,
                               create_and_reserve_promocode, create_act, DIRECT_UZB_FIRM_AG,
                               fill_calc_params_fixed_discount, create_request, is_request_with_promocode,
                               is_invoice_with_promocode, get_bonus_from_act_amount, DIRECT_UZB_FIRM_AG44,
                               check_invoice_consumes_discount, DIRECT_YANDEX_FIRM_FISH,
                               DIRECT_YANDEX_FIRM_FISH_NON_RES, DIRECT_YANDEX_FIRM_USD,
                               DIRECT_KZ_FIRM_FISH, create_invoice, check_invoice_rows_discount, NOW, create_order,
                               DIRECT_KZ_FIRM_KZU_QUASI)

QTY = Decimal('10')
QTY_FIRST_ORDER = Decimal('6')
QTY_SECOND_ORDER = Decimal('4')
ACT_BONUS_PCT = 10


@pytest.mark.parametrize('context', [
    DIRECT_YANDEX_FIRM_RUB,
    DIRECT_KZ_FIRM_KZU,
    DIRECT_BEL_FIRM_BYN_QUASI,
    DIRECT_KZ_FIRM_KZU_QUASI,
    DIRECT_YANDEX_FIRM_FISH,
    DIRECT_YANDEX_FIRM_FISH_NON_RES,
    DIRECT_KZ_FIRM_FISH,
    DIRECT_UZB_FIRM_AG,
    DIRECT_UZB_FIRM_AG44
])
def test_act_bonus_check_apply_on_pay(context):
    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
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

    act_id, _ = create_act(client_id, context=context, dt=NOW.replace(month=NOW.month - 2), person_id=person_id,
                           qty=100)
    act_amount = db.get_act_by_id(act_id)[0]['amount']
    sum_bonus = get_bonus_from_act_amount(act_amount, ACT_BONUS_PCT)
    start_dt = NOW.replace(month=NOW.month - 2, day=1)
    calc_params = fill_calc_params_act_bonus(apply_on_create=False,
                                             adjust_quantity=True,
                                             act_bonus_pct=ACT_BONUS_PCT,
                                             max_discount_pct=10,
                                             max_bonus_amount=10000,
                                             currency='USD',
                                             min_act_amount=1,
                                             act_period_days=1,
                                             )
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.ACT_BONUS,
                                                                client_id=client_id,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                service_ids=[context.service.id],
                                                                start_dt=start_dt)

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
    check_invoice_rows_discount(fixed_sum=0, invoice_id=invoice_id, precision=context.precision,
                                qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                apply_on_create=False, nds=context.nds, nds_included=context.nds_included)
    steps.InvoiceSteps.pay(invoice_id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    check_invoice_consumes_discount(fixed_sum=sum_bonus, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], nds=0,
                                    adjust_quantity=True)


@pytest.mark.parametrize('context', [
    DIRECT_YANDEX_FIRM_USD,
    DIRECT_YANDEX_FIRM_RUB,
    DIRECT_KZ_FIRM_KZU,
    DIRECT_BEL_FIRM_BYN_QUASI,
    DIRECT_KZ_FIRM_KZU_QUASI,
    DIRECT_YANDEX_FIRM_FISH,
    DIRECT_YANDEX_FIRM_FISH_NON_RES,
    DIRECT_UZB_FIRM_AG,
    DIRECT_KZ_FIRM_FISH
])
def test_fixed_sum_apply_on_create_adjust_quantity(context):
    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                              currency=context.currency.iso_code, region_id=context.region.id
                                              )

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                    'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    act_id, _ = create_act(client_id, context=context, dt=NOW.replace(month=NOW.month - 2), person_id=person_id,
                           qty=100)
    act_amount = db.get_act_by_id(act_id)[0]['amount']
    sum_bonus = get_bonus_from_act_amount(act_amount, ACT_BONUS_PCT)
    calc_params = fill_calc_params_act_bonus(apply_on_create=True,
                                             adjust_quantity=True,
                                             act_bonus_pct=ACT_BONUS_PCT,
                                             max_discount_pct=10,
                                             max_bonus_amount=10000,
                                             currency='RUR',
                                             min_act_amount=1,
                                             act_period_days=1,
                                             )
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.ACT_BONUS,
                                                                client_id=client_id,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                service_ids=[context.service.id],
                                                                start_dt=NOW.replace(month=NOW.month - 2, day=1))

    service_order_id, _ = create_order(context=context, agency_id=None, client_id=client_id)
    service_order_id_2, _ = create_order(context=context, agency_id=None, client_id=client_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY_FIRST_ORDER, 'BeginDT': NOW},
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id_2, 'Qty': QTY_SECOND_ORDER,
         'BeginDT': NOW}]
    request_id, _ = create_request(context, client_id, promocode_code=promocode_code, qty=QTY, orders_list=orders_list)
    utils.check_that(is_request_with_promocode(promocode_id, request_id), hamcrest.equal_to(True))

    invoice_id = create_invoice(request_id, person_id, paysys_id=context.paysys.id)
    utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
    check_invoice_rows_discount(fixed_sum=sum_bonus, invoice_id=invoice_id, precision=context.precision,
                                qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                apply_on_create=True, nds=0,
                                nds_included=context.nds_included)
    steps.InvoiceSteps.pay(invoice_id)
    check_invoice_consumes_discount(fixed_sum=sum_bonus, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                    apply_on_create=True, nds=0,
                                    nds_included=context.nds_included)


@pytest.mark.parametrize('context', [
    DIRECT_YANDEX_FIRM_USD,
    DIRECT_YANDEX_FIRM_RUB,
    DIRECT_KZ_FIRM_KZU,
    DIRECT_BEL_FIRM_BYN_QUASI,
    DIRECT_KZ_FIRM_KZU_QUASI,
    DIRECT_YANDEX_FIRM_FISH,
    DIRECT_KZ_FIRM_FISH])
def test_fixed_sum_apply_on_create_adjust_sum(context):
    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                              currency=context.currency.iso_code, region_id=context.region.id
                                              )
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                    'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    act_id, _ = create_act(client_id, context=context, dt=NOW.replace(month=NOW.month - 2), person_id=person_id,
                           qty=100)
    act_amount = db.get_act_by_id(act_id)[0]['amount']
    sum_bonus = get_bonus_from_act_amount(act_amount, ACT_BONUS_PCT)
    calc_params = fill_calc_params_act_bonus(apply_on_create=True,
                                             adjust_quantity=False,
                                             act_bonus_pct=ACT_BONUS_PCT,
                                             max_discount_pct=10,
                                             max_bonus_amount=10000,
                                             currency='RUR',
                                             min_act_amount=1,
                                             act_period_days=1,
                                             )
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.ACT_BONUS,
                                                                client_id=client_id,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                service_ids=[context.service.id],
                                                                start_dt=NOW.replace(month=NOW.month - 2, day=1))

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
    check_invoice_rows_discount(discount_pct=10, invoice_id=invoice_id, precision=context.precision,
                                qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=False,
                                apply_on_create=True, nds=0, sum_before=getattr(context, 'sum_before', None),
                                nds_included=context.nds_included)
    steps.InvoiceSteps.pay(invoice_id)
    check_invoice_consumes_discount(discount_pct=10, invoice_id=invoice_id, precision=context.precision,
                                    qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=False,
                                    apply_on_create=True, nds=0,
                                    sum_before=getattr(context, 'sum_before', None),
                                    nds_included=context.nds_included)
