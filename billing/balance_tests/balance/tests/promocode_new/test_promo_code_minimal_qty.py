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
    create_order, fill_calc_params

QTY = Decimal('10')
QTY_FIRST_ORDER = Decimal('6')
QTY_SECOND_ORDER = Decimal('4')
FIXED_DISCOUNT = 5
FIXED_BONUS_SUM = 5
FIXED_BONUS_QTY = 5

pytestmark = [reporter.feature(Features.PROMOCODE)]


@pytest.mark.parametrize('context, minimal_amounts, is_promo_applied', [
    (DIRECT_YANDEX_FIRM_RUB, 8.3333, True),
    (DIRECT_YANDEX_FIRM_RUB, 8.34, False),
    (DIRECT_KZ_FIRM_KZU, 8.933, True),
    (DIRECT_KZ_FIRM_KZU, 8.9331, False),
    (DIRECT_BEL_FIRM_BYN_QUASI, 10.004, True),
    (DIRECT_BEL_FIRM_BYN_QUASI, 10.005, False),
    (DIRECT_KZ_FIRM_KZU_QUASI, 10.004, True),
    (DIRECT_KZ_FIRM_KZU_QUASI, 10.005, False),
    (DIRECT_YANDEX_FIRM_FISH, 250, True),
    (DIRECT_YANDEX_FIRM_FISH, 250.01, False),
    (DIRECT_YANDEX_FIRM_FISH_NON_RES, 250, True),
    (DIRECT_YANDEX_FIRM_FISH_NON_RES, 250.01, False),
    (DIRECT_KZ_FIRM_FISH, 1050, True),
    (DIRECT_KZ_FIRM_FISH, 1051, False),
    # (DIRECT_TR_FIRM_TRY, 8.47, True),
    # (DIRECT_TR_FIRM_TRY, 8.48, False),
    (DIRECT_SW_FIRM_CHF, 9.28, True),
    (DIRECT_SW_FIRM_CHF, 9.29, False)
    # )
])
@pytest.mark.parametrize('adjust_quantity', [True])
@pytest.mark.parametrize('promocode_type', [PromocodeClass.FIXED_DISCOUNT])
def test_check_minimal_qty(context, minimal_amounts, is_promo_applied, promocode_type, adjust_quantity):
    client_id = steps.ClientSteps.create()
    if context.product.type == ProductTypes.MONEY:
        steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='COPY', dt=DT_1_DAY_BEFORE,
                                              currency=context.currency.iso_code, region_id=context.region.id)
    calc_params = fill_calc_params(calc_class_name=promocode_type, discount_pct=FIXED_DISCOUNT, apply_on_create=False,
                                   adjust_quantity=adjust_quantity)
    promocode_id, promocode_code = create_and_reserve_promocode(calc_class_name=PromocodeClass.FIXED_DISCOUNT,
                                                                client_id=client_id,
                                                                firm_id=context.firm.id,
                                                                calc_params=calc_params,
                                                                end_dt=DT_1_DAY_AFTER,
                                                                service_ids=[context.service.id],
                                                                start_dt=DT_1_DAY_BEFORE,
                                                                minimal_amounts={
                                                                    context.currency.iso_code: minimal_amounts})
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
    steps.InvoiceSteps.pay(invoice_id)

    if is_promo_applied:
        utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(True))
        check_invoice_consumes_discount(discount_pct=FIXED_DISCOUNT, invoice_id=invoice_id, precision=context.precision,
                                        qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                        apply_on_create=False, classname=PromocodeClass.FIXED_DISCOUNT)
    else:
        utils.check_that(is_invoice_with_promocode(promocode_id, invoice_id), hamcrest.equal_to(False))
        check_invoice_consumes_discount(discount_pct=0, invoice_id=invoice_id, precision=context.precision,
                                        qty_before=[QTY_FIRST_ORDER, QTY_SECOND_ORDER], adjust_quantity=True,
                                        apply_on_create=False, classname=PromocodeClass.FIXED_DISCOUNT)
