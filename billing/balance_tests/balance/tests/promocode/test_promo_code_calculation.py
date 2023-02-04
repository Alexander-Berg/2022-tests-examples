# -*- coding: utf-8 -*-
import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Services, PromocodeClass
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.PROMOCODE, Features.REQUEST, Features.INVOICE)]

dt = datetime.datetime.now()
dt_1_day_before = dt - datetime.timedelta(days=1)
dt_1_day_after = dt + datetime.timedelta(days=1)

PROMOCODE_BONUS = 20
PROMOCODE_CURRENCY_BONUS = 2400
QTY = 10

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)

DIRECT_YANDEX_FIRM_FISH_NON_RES = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, person_type=PersonTypes.YT,
                                                                       paysys=Paysyses.BANK_YT_RUB)

DIRECT_KZ_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
                                                           firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                           paysys=Paysyses.BANK_KZ_UR_TG)

DIRECT_YANDEX_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_RUB,
                                                              region=Regions.RU, currency=Currencies.RUB)

DIRECT_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT,
                                                          firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                          paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ)

DIRECT_UZB_FIRM_AG = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_USD,
                                                          firm=Firms.YANDEX_1, person_type=PersonTypes.YT,
                                                          paysys=Paysyses.BANK_YT_RUB, region=Regions.UZB,
                                                          currency=Currencies.USD)

DIRECT_KZ_FIRM_KZU_QUASI = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT,
                                                                product=Products.DIRECT_KZT_QUASI,
                                                                firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                                paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ)

DIRECT_BEL_FIRM_BYN_QUASI = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN,
                                                                 firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                                 paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                                 currency=Currencies.BYN)

PRODUCT_REGION_CURRENCY_MAP = {Products.DIRECT_UAH: {'region_id': 187, 'currency': 'UAH', 'precision': '0.00001'},
                               Products.DIRECT_RUB: {'region_id': 225, 'currency': 'RUB', 'precision': '0.0001'},
                               Products.DIRECT_KZT: {'precision': '0.0001'},
                               Products.DIRECT_KZT_QUASI: {'precision': '0.000001'},
                               Products.DIRECT_BYN: {'precision': '0.000001'},
                               Products.DIRECT_USD: {'precision': '0.00001'}}

FIRM_NDS_MAP = {Firms.YANDEX_UA_2.id: 1.20,
                Firms.YANDEX_1.id: 1.2,
                Firms.KZ_25.id: 1.12,
                Firms.REKLAMA_BEL_27.id: 1.20
                }


def create_and_reserve_promocode(client_id=None, firm_id=1, is_global_unique=0, start_dt=None, end_dt=None,
                                 new_clients_only=0,
                                 valid_until_paid=1, currency=None, minimal_qty_currency=None, minimal_qty=0,
                                 services=None,
                                 code=None, middle_dt=None, bonus1=None, bonus2=None):
    if not start_dt:
        start_dt = dt_1_day_before
    if not end_dt:
        end_dt = dt_1_day_after
    if middle_dt is None:
        middle_dt = start_dt + datetime.timedelta(seconds=1)
    if not bonus1:
        bonus1 = PROMOCODE_BONUS
    if not bonus2:
        bonus2 = PROMOCODE_BONUS
    if code:
        exist_promo = db.get_promocode_by_code(code)
        if exist_promo:
            id = exist_promo[0]['id']
            steps.PromocodeSteps.clean_up(id)
            steps.PromocodeSteps.delete_promocode(id)
    else:
        code = steps.PromocodeSteps.generate_code()

    calc_params = steps.PromocodeSteps.fill_calc_params(
        promocode_type=PromocodeClass.LEGACY_PROMO,
        middle_dt=middle_dt,
        bonus1=bonus1,
        bonus2=bonus2,
        minimal_qty=minimal_qty,
        currency=currency,
        multicurrency_bonus1=PROMOCODE_CURRENCY_BONUS,
        multicurrency_bonus2=PROMOCODE_CURRENCY_BONUS,
        multicurrency_minimal_qty=minimal_qty_currency,
        discount_pct=0
    )
    promo_resp, = steps.PromocodeSteps.create_new(
        calc_class_name=PromocodeClass.LEGACY_PROMO,
        calc_params=calc_params,
        promocodes=[code],
        start_dt=start_dt,
        end_dt=end_dt,
        reservation_days=None,
        firm_id=firm_id,
        is_global_unique=is_global_unique,
        new_clients_only=new_clients_only,
        valid_until_paid=valid_until_paid,
        service_ids=services
    )
    promo_code_id = promo_resp['id']
    promo_code_code = promo_resp['code']

    if client_id:
        steps.PromocodeSteps.make_reservation(client_id, promo_code_id, start_dt, end_dt)
    return promo_code_id, promo_code_code


def create_request(service_id, client_id, product_id, promocode_code=None, invoice_dt=None, agency_id=None, qty=None):
    if qty is None:
        qty = QTY
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    steps.OrderSteps.create(client_id=client_id, product_id=product_id, service_id=service_id,
                            service_order_id=service_order_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(client_id=agency_id if agency_id else client_id, orders_list=orders_list,
                                           additional_params={'PromoCode': promocode_code,
                                                              'InvoiceDesireDT': invoice_dt})
    return request_id, orders_list


def create_payed_invoice(request_id, person_id, paysys_id):
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=paysys_id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)
    return invoice_id


@reporter.feature(Features.TO_UNIT)
@pytest.mark.smoke
@pytest.mark.parametrize('context, params', [
    (DIRECT_YANDEX_FIRM_RUB, {'minimal_qty_currency': None, 'is_with_discount': True}),
    (DIRECT_YANDEX_FIRM_RUB, {'minimal_qty_currency': 8.33, 'is_with_discount': True}),
    (DIRECT_YANDEX_FIRM_RUB, {'minimal_qty_currency': 8.34, 'is_with_discount': False}),

    (DIRECT_KZ_FIRM_KZU, {'minimal_qty_currency': None, 'is_with_discount': True}),
    (DIRECT_KZ_FIRM_KZU, {'minimal_qty_currency': 8.933, 'is_with_discount': True}),
    (DIRECT_KZ_FIRM_KZU, {'minimal_qty_currency': 8.9331, 'is_with_discount': False}),

    (DIRECT_UZB_FIRM_AG, {'minimal_qty_currency': None, 'is_with_discount': True}),
    (DIRECT_UZB_FIRM_AG, {'minimal_qty_currency': 8.9285, 'is_with_discount': True})

])
def test_promo_code_minimal_qty_currency_check(context, params):
    client_id = steps.ClientSteps.create_multicurrency(currency_convert_type='COPY', dt=None,
                                                       service_id=context.service.id,
                                                       region_id=context.region.id,
                                                       currency=context.currency.iso_code)

    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id,
                                                                firm_id=context.firm.id,
                                                                currency=context.currency.iso_code,
                                                                minimal_qty=0,
                                                                minimal_qty_currency=params['minimal_qty_currency'],
                                                                bonus1=0, bonus2=0)

    request_id, _ = create_request(context.service.id, client_id, context.product.id)
    utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id), hamcrest.equal_to(False))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_payed_invoice(request_id, person_id, paysys_id=context.paysys.id)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_with_discount']))



@reporter.feature(Features.TO_UNIT)
@pytest.mark.smoke
@pytest.mark.parametrize('context, params', [
    (DIRECT_KZ_FIRM_KZU_QUASI, {'minimal_qty_currency': None, 'is_with_discount': True}),
    (DIRECT_KZ_FIRM_KZU_QUASI, {'minimal_qty_currency': 10, 'is_with_discount': True}),
    (DIRECT_KZ_FIRM_KZU_QUASI, {'minimal_qty_currency': 10.01, 'is_with_discount': False}),

    (DIRECT_BEL_FIRM_BYN_QUASI, {'minimal_qty_currency': None, 'is_with_discount': True}),
    (DIRECT_BEL_FIRM_BYN_QUASI, {'minimal_qty_currency': 10, 'is_with_discount': True}),
    (DIRECT_BEL_FIRM_BYN_QUASI, {'minimal_qty_currency': 10.01, 'is_with_discount': False}),

])
def test_promo_code_minimal_qty_quazi_currency_check(context, params):
    client_id = steps.ClientSteps.create_multicurrency(currency_convert_type='COPY', dt=None,
                                                       service_id=context.service.id,
                                                       region_id=context.region.id,
                                                       currency=context.currency.iso_code)

    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id, firm_id=context.firm.id,
                                                                currency=context.currency.iso_code,
                                                                minimal_qty=0,
                                                                minimal_qty_currency=params['minimal_qty_currency'],
                                                                bonus1=None, bonus2=None)

    request_id, _ = create_request(context.service.id, client_id, context.product.id)
    utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id), hamcrest.equal_to(False))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_payed_invoice(request_id, person_id, paysys_id=context.paysys.id)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_with_discount']))
    steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_CURRENCY_BONUS,
                                                        is_with_discount=params['is_with_discount'],
                                                        qty=QTY, nds=None,
                                                        precision=PRODUCT_REGION_CURRENCY_MAP[context.product][
                                                            'precision'])


@reporter.feature(Features.TO_UNIT)
@pytest.mark.smoke
@pytest.mark.parametrize('context, params', [

    (DIRECT_YANDEX_FIRM_FISH, {'minimal_qty': 0, 'is_with_discount': True}),
    (DIRECT_YANDEX_FIRM_FISH, {'minimal_qty': 10.000166, 'is_with_discount': True}),
    (DIRECT_YANDEX_FIRM_FISH, {'minimal_qty': 10.000167, 'is_with_discount': False}),

    (DIRECT_YANDEX_FIRM_FISH_NON_RES, {'minimal_qty': 0, 'is_with_discount': True}),
    (DIRECT_YANDEX_FIRM_FISH_NON_RES, {'minimal_qty': 10.00, 'is_with_discount': True}),
    (DIRECT_YANDEX_FIRM_FISH_NON_RES, {'minimal_qty': 10.001, 'is_with_discount': False}),

    (DIRECT_KZ_FIRM_FISH, {'minimal_qty': 0, 'is_with_discount': True}),
    (DIRECT_KZ_FIRM_FISH, {'minimal_qty': 10, 'is_with_discount': True}),
    (DIRECT_KZ_FIRM_FISH, {'minimal_qty': 10.0001, 'is_with_discount': False}),

])
def test_promo_code_minimal_qty_fish_check(context, params):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=client_id, firm_id=context.firm.id,
                                                                minimal_qty=params['minimal_qty'],
                                                                minimal_qty_currency=0,
                                                                services=[context.service.id])
    request_id, _ = create_request(context.service.id, client_id, context.product.id)
    utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id), hamcrest.equal_to(False))
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_payed_invoice(request_id, person_id, paysys_id=context.paysys.id)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_with_discount']))
    steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_BONUS,
                                                        is_with_discount=params['is_with_discount'], qty=QTY,
                                                        qty_before=QTY)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.now
@pytest.mark.smoke
@pytest.mark.parametrize('context, params', [
    (DIRECT_YANDEX_FIRM_FISH, {'minimal_qty': 0, 'is_with_discount': True}),
])
def test_request_promo_code(context, params):
    client_id = steps.ClientSteps.create()
    promocode_id, promocode_code = create_and_reserve_promocode(client_id=None, firm_id=context.firm.id,
                                                                minimal_qty=params['minimal_qty'],
                                                                minimal_qty_currency=0,
                                                                services=[context.service.id])

    # промокод не должен мешать создавать реквесты
    request_id_1, _ = create_request(context.service.id, client_id, context.product.id, promocode_code)
    request_id_2, _ = create_request(context.service.id, client_id, context.product.id, promocode_code)

    utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id_1), hamcrest.equal_to(True))
    utils.check_that(steps.PromocodeSteps.is_request_with_promo(promocode_id, request_id_2), hamcrest.equal_to(True))

    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    invoice_id = create_payed_invoice(request_id_2, person_id, paysys_id=context.paysys.id)
    utils.check_that(steps.PromocodeSteps.is_invoice_with_promo(promocode_id, invoice_id),
                     hamcrest.equal_to(params['is_with_discount']))
    steps.PromocodeSteps.check_invoice_is_with_discount(invoice_id, bonus=PROMOCODE_BONUS,
                                                        is_with_discount=params['is_with_discount'], qty=QTY,
                                                        qty_before=QTY)


# import decimal
#
#
# def round00(value, round=decimal.ROUND_HALF_UP):
#     if not isinstance(value, decimal.Decimal):
#         value = decimal.Decimal(value)
#     return value.quantize(decimal.Decimal('0.01'), round)
#
#
# def test_count_minimal_qty():
#     qty = D('9.9')
#     qty_expected = D('11.2')
#     nds = D('12')
#     result = qty
#     while result < qty_expected:
#         qty += D('0.0001')
#         qty_before = qty
#         result = qty * round00(1 + (nds / decimal.Decimal(100)))
#     print qty_before
#     print (qty_before - D('0.0001')) * round00(1 + (nds / decimal.Decimal(100)))
#     print (qty_before) * round00(1 + (nds / decimal.Decimal(100)))
#
#
# def test_check():
#     qty = D('10.0001')
#     qty *= round00(1 + (D('12') / decimal.Decimal(100)))
#     print qty
#
#
# def test_count_qty():
#     total_sum = D('254.24')
#     qty = D('10')
#     fish_price_wo_nds = D('25.42')
#
#     money = qty * fish_price_wo_nds
#     while money < total_sum:
#         qty += D('0.00001')
#         money_before = qty
#         money = qty * fish_price_wo_nds
#     print qty


# if __name__ == "__main__":
#     pytest.main("test_ua_promo.py -v")
