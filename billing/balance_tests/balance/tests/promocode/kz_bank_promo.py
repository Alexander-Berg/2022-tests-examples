# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import equal_to

from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_objects import Product
from btestlib import utils
import btestlib.reporter as reporter
from balance.features import Features

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
DT_1_DAY_AGO = NOW - datetime.timedelta(days=1)
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
BASE_DT = datetime.datetime.now()

QTY = 250

PRODUCT = Product(7, 1475, 'Bucks', 'Money')

KZ_CC_UR = 1120
KZ_BANK_UR = 2501020
KZ_BANK_PH = 2501021

PERSON_TYPE_KZ_UR = 'kzu'
PERSON_TYPE_KZ_PH = 'kzp'
PERSON_PAYSYS_MAP = {PERSON_TYPE_KZ_PH: KZ_BANK_PH,
                     PERSON_TYPE_KZ_UR: KZ_BANK_UR}

ALFA_KZ_PROMOCODES = [
    {'code': 'ALKZTF47MR5XKYN3', 'bin': '120640012118', 'valid_bin': True},
    {'code': 'ALKZZEZMYNKAX92Y', 'bin': '111111111', 'valid_bin': False},
    {'code': 'ALKZ7TZ5W5TS86TD', 'bin': None, 'valid_bin': False}
]

ALFA_KZ_NON_ISSUED_PROMOS = [{'code': 'ALKZCUVDQHKDENDH'}]

pytestmark = [
    pytest.mark.tickets('BALANCE-26323'),
    reporter.feature(Features.PROMOCODE, Features.INVOICE)
]



def create_invoice_with_promo(code, bin=None, params=None):
    promocode = db.get_promocode_by_code(code)

    if promocode:
        promocode_id = db.get_promocode_by_code(code)[0]['id']
    else:
        promocode_id = steps.PromocodeSteps.create(start_dt=DT_1_DAY_AGO, code=code, end_dt=None, bonus1=20,
                                                   bonus2=20, minimal_qty=None, reservation_days=None)

    steps.PromocodeSteps.clean_up(promocode_id)
    steps.PromocodeSteps.set_dates(promocode_id, DT_1_DAY_AGO)

    client_id = steps.ClientSteps.create()

    steps.PromocodeSteps.reserve(client_id, promocode_id)

    # определяем тип плательщика из параметров или юрик по умолчанию
    if params:
        person_type = PERSON_TYPE_KZ_UR if not params.get('person_type', False) else params['person_type']
    else:
        person_type = PERSON_TYPE_KZ_UR

    person_params = None if bin is None else {'kz-in': bin}
    person_id = steps.PersonSteps.create(client_id, person_type, params=person_params)

    product = PRODUCT

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(product.service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=product.service_id, product_id=product.id)
    orders_list.append(
        {'ServiceID': product.service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT})

    # определяем paysys из маппера, если он не передан в параметризации
    if params:
        paysys_id = PERSON_PAYSYS_MAP[person_type] if not params.get('paysys', False) else params['paysys']
    else:
        paysys_id = PERSON_PAYSYS_MAP[person_type]

    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    return invoice_id, promocode_id


def checker(invoice_id, bonus, is_with_discount):
    consumes = db.get_consumes_by_invoice(invoice_id)
    discount_list = [consume['discount_pct'] for consume in consumes]
    expected_discount = steps.PromocodeSteps.calculate_static_discount(250, bonus) if is_with_discount else D('0')
    for discount in discount_list:
        utils.check_that(D(discount), equal_to(expected_discount))
    for consume in consumes:
        current_sum = D(consume['current_sum'])
        price = D(consume['price'])
        qty_before = current_sum / price
        expected_qty = D(
            steps.PromocodeSteps.calculate_qty_with_static_discount(qty_before, expected_discount, '0.000001'))
        utils.check_that(D(consume['current_qty']), equal_to(expected_qty))


@pytest.mark.no_parallel
@pytest.mark.parametrize('promo_params', ALFA_KZ_PROMOCODES)
@pytest.mark.parametrize('other_params', [
    {'paysys': KZ_CC_UR, 'expected_discount': False},
    {'paysys': KZ_BANK_UR, 'expected_discount': True}
                                         ])
@pytest.mark.parametrize('payment_params', [
    {'payment_type': 'ai'},
    {'payment_type': 'xmlprc'}
])
@pytest.mark.skip(reason='Test KZT Alfa promocodes not longer supported on their side')
def test_discount_depend_on_paysys(promo_params, other_params, payment_params):
    if not promo_params['valid_bin'] and not other_params['expected_discount']:
        reporter.log(u'если оба условия не выполняются, потом не поймешь, почему промокод не применился')
        return
    code = promo_params['code']
    bin = promo_params['bin']
    invoice_id, promocode_id = create_invoice_with_promo(code, bin, other_params)

    if other_params['paysys'] == KZ_CC_UR:
        steps.InvoiceSteps.turn_on(invoice_id)
    else:
        if payment_params['payment_type'] == 'ai':
            reporter.log('muzzle_config check')
            steps.InvoiceSteps.turn_on_ai(invoice_id)
        else:
            reporter.log('medium_config check')
            steps.InvoiceSteps.pay(invoice_id)

    is_with_discount = promo_params['valid_bin'] and other_params['expected_discount']
    bonus = steps.PromocodeSteps.define_bonus_on_dt(promocode_id, NOW)
    checker(invoice_id, bonus, is_with_discount)


@pytest.mark.no_parallel
@pytest.mark.parametrize('promo_params', ALFA_KZ_PROMOCODES)
@pytest.mark.parametrize('other_params', [
                                          {'person_type': PERSON_TYPE_KZ_UR, 'expected_discount': True},
                                          {'person_type': PERSON_TYPE_KZ_PH, 'expected_discount': False}
])
@pytest.mark.parametrize('payment_params', [
    {'payment_type': 'ai'},
    {'payment_type': 'xmlprc'}
])
@pytest.mark.skip(reason='Test KZT Alfa promocodes not longer supported on their side')
def test_discount_depend_on_person_type(promo_params, other_params, payment_params):
    if not promo_params['valid_bin'] and not other_params['expected_discount']:
        reporter.log(u'если оба условия не выполняются, потом не поймешь, почему промокод не применился')
        return
    code = promo_params['code']
    bin = promo_params['bin']
    invoice_id, promocode_id = create_invoice_with_promo(code, bin, other_params)
    bonus = steps.PromocodeSteps.define_bonus_on_dt(promocode_id, NOW)
    if payment_params['payment_type'] == 'ai':
        reporter.log('muzzle_config check')
        steps.InvoiceSteps.turn_on_ai(invoice_id)
    else:
        reporter.log('medium_config check')
        steps.InvoiceSteps.pay(invoice_id)
    is_with_discount = promo_params['valid_bin'] and other_params['expected_discount']
    checker(invoice_id, bonus, is_with_discount)


@pytest.mark.no_parallel
@pytest.mark.parametrize('promo_params',ALFA_KZ_NON_ISSUED_PROMOS)
@pytest.mark.skip(reason='Test KZT Alfa promocodes not longer supported on their side')
def test_discount_depend_on_promo_status(promo_params):
    code = promo_params['code']
    invoice_id, promocode_id = create_invoice_with_promo(code)
    bonus = steps.PromocodeSteps.define_bonus_on_dt(promocode_id, NOW)
    steps.InvoiceSteps.pay(invoice_id)
    checker(invoice_id, bonus, is_with_discount=False)
