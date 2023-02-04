# -*- coding: utf-8 -*-

import pytest
import datetime
from decimal import Decimal

from tests import object_builder as ob
from butils.decimal_unit import DecimalUnit as DU
from balance.constants import RegionId, FirmId
from balance.mapper import Country, CurrencyRate, Currency, Firm, Product

from tests.balance_tests.price.price_common import create_currency_product, create_non_currency_product, create_valid_tax, \
    create_country, create_tax_policy, create_tax_policy_pct, create_price

from balance import muzzle_util as ut
from balance import exc

NOW = datetime.datetime.now().replace(microsecond=0)
PAST = NOW - datetime.timedelta(1)
FUTURE = NOW + datetime.timedelta(1)

NDS_PCT = 45
NSP_PCT = 12


def create_currency(session, **kwargs):
    currency = ob.CurrencyBuilder(**kwargs).build(session).obj
    currency.iso_code = currency.char_code
    return currency


def create_currency_rate(session, **kwargs):
    return ob.CurrencyRateBuilder(**kwargs).build(session).obj


def create_cross_rate(session, currency_from, currency_to):
    # создаем кросс курс через рубли
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency_from.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=100)
    create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency_to.char_code,
                         base_cc='RUR', rate_src_id=1000, rate=78)


def create_product_season_coeff(session, **kwargs):
    return ob.ProdSeasonCoeffBuilder(**kwargs).build(session).obj


def check_price_object(price_object, price, type_rate, tax_policy_pct, price_mapper, dt, currency):
    assert price_object.price == price
    assert price_object.type_rate == type_rate
    assert price_object.tax_policy_pct == tax_policy_pct
    assert price_object.price_mapper == price_mapper
    assert price_object.dt == dt
    assert price_object.currency == currency


def test_get_price_currency_product_w_tax(session):
    """базовый случай для валютного продукта, цена не нужна"""
    currency = create_currency(session)
    product = create_currency_product(session, iso_currency=currency.iso_code)
    country = create_country(session)
    _, _, tax_policy_pct = create_valid_tax(session, product=product, currency=currency, country=country, dt=NOW)
    price_object = product.get_price(dt=NOW, currency=currency, country=country, resident=1)
    check_price_object(price_object, price=DU(1, [product.unit.iso_currency], [1, 'QTY']),
                       type_rate=product.unit.type_rate, tax_policy_pct=tax_policy_pct,
                       price_mapper=None, dt=NOW, currency=currency.char_code)


def test_get_price_non_currency_product_w_tax_w_price(session):
    """базовый случай для фишечного"""
    currency = create_currency(session)
    product = create_non_currency_product(session)
    country = create_country(session)
    _, _, tax_policy_pct = create_valid_tax(session, product=product, currency=currency, country=country, dt=NOW)
    price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                         tax_policy_pct=tax_policy_pct)
    price_object = product.get_price(dt=NOW, currency=currency.char_code, country=country, resident=1)
    check_price_object(price_object, price=DU(price.price, [currency.iso_code], [product.unit.type_rate, 'QTY']),
                       type_rate=product.unit.type_rate, tax_policy_pct=tax_policy_pct,
                       price_mapper=price, dt=NOW, currency=currency.char_code)


def test_get_price_non_currency_product_old_style_tax(session):
    """если цена включает налог, а налоговая ставка не указана, выбрасываем исключение"""
    currency = create_currency(session)
    product = create_non_currency_product(session)
    country = create_country(session)
    _, _, tax_policy_pct = create_valid_tax(session, product=product, currency=currency, country=country, dt=NOW)
    create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                 tax_policy_pct=None, tax=1)
    with pytest.raises(exc.OLD_STYLE_TAX) as exc_info:
        product.get_price(dt=NOW, currency=currency.char_code, country=country, resident=1)
    assert exc_info.value.msg == 'Price for product {} on date {} for currency u\'{}\' has OLD-style tax'.format(
        product.id, NOW, currency.char_code)


def test_get_price_non_currency_product_w_tax_w_price_pcts_differs(session):
    """если цена продукта включает налог и налоговая ставка цены не совпадает с ставкой из действующей налоговой
     политикой, от цены отрываем налог и добавляем налоговую ставку из наловогой политики"""
    currency = create_currency(session)
    product = create_non_currency_product(session)
    country = create_country(session)
    _, tax_policy, tax_policy_pct = create_valid_tax(session, product=product, currency=currency, country=country,
                                                     dt=NOW)
    price_tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=13,
                                                 nsp_pct=45)
    price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                         tax_policy_pct=price_tax_policy_pct)
    price_wo_tax = price.price / (DU(1) + (price_tax_policy_pct.nds_pct + price_tax_policy_pct.nsp_pct) / DU(100, '%'))
    price_w_current_tax = price_wo_tax * (DU(1) + (tax_policy_pct.nds_pct + tax_policy_pct.nsp_pct) / DU(100, '%'))
    price_object = product.get_price(dt=NOW, currency=currency.char_code, country=country, resident=1)
    check_price_object(price_object, price=price_w_current_tax, type_rate=product.unit.type_rate,
                       tax_policy_pct=tax_policy_pct, price_mapper=price, dt=NOW, currency=currency.char_code)


def test_get_price_non_currency_product_w_tax_w_price_tax_not_included(session):
    """если цена продукта не включает налог, добавляем налог к цене"""
    currency = create_currency(session)
    product = create_non_currency_product(session)
    country = create_country(session)
    _, _, tax_policy_pct = create_valid_tax(session, product=product, currency=currency, country=country, dt=NOW)
    price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                         tax_policy_pct=None)
    price_w_current_tax = price.price * (DU(1) + (tax_policy_pct.nds_pct + tax_policy_pct.nsp_pct) / DU(100, '%'))
    price_object = product.get_price(dt=NOW, currency=currency.char_code, country=country, resident=1)
    check_price_object(price_object, price=price_w_current_tax,
                       type_rate=product.unit.type_rate, tax_policy_pct=tax_policy_pct,
                       price_mapper=price, dt=NOW, currency=currency.char_code)


def test_get_price_non_currency_product_wo_tax_w_price_tax_not_included(session):
    """если цена продукта не включает налог, и запрашиваем цену без налога просто возвращаем цену"""
    currency = create_currency(session)
    product = create_non_currency_product(session)
    country = create_country(session)
    _, _, tax_policy_pct = create_valid_tax(session, product=product, currency=currency, country=country, dt=NOW)
    price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                         tax_policy_pct=None)
    price_object = product.get_price(dt=NOW, currency=currency.char_code, country=country, resident=1, wo_tax=True)
    check_price_object(price_object, price=price.price,
                       type_rate=product.unit.type_rate, tax_policy_pct=None,
                       price_mapper=price, dt=NOW, currency=currency.char_code)


def test_get_price_non_currency_product_wo_tax_w_price_tax_included(session):
    """если цена продукта включает налог, а запрашиваем цену без налога, отрываем налог от цены"""
    currency = create_currency(session)
    product = create_non_currency_product(session)
    country = create_country(session)
    tax_policy = create_tax_policy(session, resident=1, country=country)
    price_tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=13, nsp_pct=45)
    price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                         tax_policy_pct=price_tax_policy_pct)
    price_wo_tax = price.price / (DU(1) + (price_tax_policy_pct.nds_pct + price_tax_policy_pct.nsp_pct) / DU(100, '%'))
    price_object = product.get_price(dt=NOW, currency=currency.char_code, country=country, resident=1, wo_tax=True)
    check_price_object(price_object, price=price_wo_tax,
                       type_rate=product.unit.type_rate, tax_policy_pct=None,
                       price_mapper=price, dt=NOW, currency=currency.char_code)


def test_get_price_from_cross_rate_currency_product(session):
    """при несовпадении валют в цене продукта и продукте, реальную цену получаем через кросс курс"""
    price_currency = create_currency(session)
    product = create_currency_product(session, iso_currency=price_currency.iso_code)
    country = create_country(session)
    _, _, tax_policy_pct = create_valid_tax(session, product=product, currency=price_currency, country=country, dt=NOW)
    requested_currency = create_currency(session)
    create_cross_rate(session, price_currency, requested_currency)
    price_object = product.get_price(dt=NOW, currency=requested_currency.char_code, country=country, resident=1)
    rate = CurrencyRate.get_cross_rate(NOW, Currency.fix_iso_code(price_currency.char_code),
                                       Currency.fix_iso_code(requested_currency.char_code),
                                       session=session, real_rate=False)
    check_price_object(price_object, price=DU(1 * rate, [requested_currency.char_code], [1, 'QTY']),
                       type_rate=product.unit.type_rate, tax_policy_pct=tax_policy_pct,
                       price_mapper=None, dt=NOW, currency=requested_currency.char_code)


def test_get_price_from_cross_rate_non_currency_product(session):
    """при несовпадении валют в цене продукта и продукте, реальную цену получаем через кросс курс"""
    price_currency = create_currency(session)
    product = create_non_currency_product(session, reference_price_currency=price_currency.char_code)
    country = create_country(session)
    _, _, tax_policy_pct = create_valid_tax(session, product=product, currency=price_currency, country=country, dt=NOW)

    price = create_price(session, currency_code=price_currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                         tax_policy_pct=tax_policy_pct)
    requested_currency = create_currency(session)
    create_cross_rate(session, price_currency, requested_currency)
    price_object = product.get_price(dt=NOW, currency=requested_currency.char_code, country=country, resident=1)
    rate = CurrencyRate.get_cross_rate(NOW, Currency.fix_iso_code(price_currency.char_code),
                                       Currency.fix_iso_code(requested_currency.char_code),
                                       session=session, real_rate=False)
    check_price_object(price_object,
                       price=DU(price.price * rate, [requested_currency.char_code], [product.unit.type_rate, 'QTY']),
                       type_rate=product.unit.type_rate, tax_policy_pct=tax_policy_pct,
                       price_mapper=price, dt=NOW, currency=requested_currency.char_code)


def test_get_price_rate_to_uzs_sw(session):
    """если регион Швейцария и валюта - Узбекские сомы, при несовпадении валют в цене продукта и продукте,
    курс валюта из цены/UZS получаем в узбекском банке"""
    currency = create_currency(session)
    product = create_currency_product(session, iso_currency=currency.iso_code)
    country_sw = ob.Getter(Country, RegionId.SWITZERLAND).build(session).obj
    _, _, tax_policy_pct = create_valid_tax(session, product=product, currency=currency, country=country_sw, dt=NOW)
    create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                 tax_policy_pct=tax_policy_pct)
    currency_rate = create_currency_rate(session, rate_dt=ut.trunc_date(NOW), cc=currency.char_code,
                                         base_cc='UZS', rate_src_id=1011, rate=100)
    price_object = product.get_price(dt=NOW, currency='UZS', country=country_sw, resident=1)
    check_price_object(price_object,
                       price=DU(1 * currency_rate.rate, ['UZS'], [1, 'QTY']),
                       type_rate=product.unit.type_rate,
                       tax_policy_pct=tax_policy_pct,
                       price_mapper=None,
                       dt=NOW,
                       currency='UZS')


def test_get_price_future_price_dt_no_price_on_now_tax_included(session):
    """запрашиваем цену с датой в будущем и налог на дату до начала действия цены, цена с налогом внутри. Возвращаем
    цену с текущим налогом внутри"""
    currency = create_currency(session)
    product = create_non_currency_product(session, iso_currency=currency.iso_code)
    country = create_country(session)
    _, tax_policy, current_tax_policy_pct = create_valid_tax(session, product, currency, country, NOW)

    future_tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0,
                                                  dt=FUTURE, nds_pct=12, nsp_pct=0)
    future_price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=FUTURE,
                                tax_policy_pct=future_tax_policy_pct)

    price_object = product.get_price(NOW, currency.char_code, country, resident=1, price_dt=FUTURE)

    check_price_object(
        price_object,
        price=DU(future_price.price, [currency.char_code], [product.unit.type_rate, 'QTY']),
        type_rate=product.unit.type_rate,
        tax_policy_pct=current_tax_policy_pct,
        price_mapper=future_price,
        dt=NOW,
        currency=currency.char_code
    )


@pytest.mark.parametrize('price_dt', [FUTURE, PAST])
def test_get_price_force_price_dt_same_tax_policy_tax_included(session, price_dt):
    """если после получения цены (налог внутри) и налога в них отличаются только налоговые ставки,
    используем цену на дату налога"""
    currency = create_currency(session)
    product = create_non_currency_product(session, iso_currency=currency.iso_code)
    country = create_country(session)

    _, tax_policy, current_tax_policy_pct = create_valid_tax(session, product, currency, country, NOW)
    current_price = create_price(session, price=666, currency_code=currency.char_code, product_id=product.id, hidden=0,
                                 dt=NOW, tax_policy_pct=current_tax_policy_pct)

    price_tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=price_dt, nds_pct=12,
                                                 nsp_pct=0)
    non_current_price = create_price(session, price=666, currency_code=currency.char_code, product_id=product.id, hidden=0,
                                     dt=price_dt, tax_policy_pct=price_tax_policy_pct)

    price_object = product.get_price(NOW, currency.char_code, country, resident=1, price_dt=price_dt)

    check_price_object(
        price_object,
        price=DU(current_price.price, [currency.char_code], [product.unit.type_rate, 'QTY']),
        type_rate=product.unit.type_rate,
        tax_policy_pct=current_tax_policy_pct,
        price_mapper=non_current_price,
        dt=NOW,
        currency=currency.char_code
    )


def test_get_price_force_price_dt_same_tax_policy_tax_included_season_coeff(session):
    """если после получения цены (с включенным налогом) и налога в них отличаются только налоговые ставки,
     и на дату налога ставки совпадали, используем цену на дату налога, сезонный коэффициент на дату счета"""
    currency = create_currency(session)
    product = create_non_currency_product(session, iso_currency=currency.iso_code)
    create_product_season_coeff(session, target_id=product.id, dt=NOW, finish_dt=NOW + datetime.timedelta(1),
                                coeff=4)

    future_coeff = create_product_season_coeff(session, target_id=product.id, dt=FUTURE,
                                               finish_dt=FUTURE + datetime.timedelta(1), coeff=6)
    country = create_country(session)

    _, tax_policy, current_tax_policy_pct = create_valid_tax(session, product, currency, country, NOW)
    current_price = create_price(session, price=666, currency_code=currency.char_code, product_id=product.id, hidden=0,
                                 dt=NOW, tax_policy_pct=current_tax_policy_pct)

    price_tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=FUTURE, nds_pct=12,
                                                 nsp_pct=0)
    future_price = create_price(session, price=666, currency_code=currency.char_code, product_id=product.id, hidden=0,
                                dt=FUTURE, tax_policy_pct=price_tax_policy_pct)

    price_object = product.get_price(NOW, currency.char_code, country, resident=1, price_dt=FUTURE)

    check_price_object(
        price_object,
        price=DU(current_price.price * future_coeff.coeff, [currency.char_code], [product.unit.type_rate, 'QTY']),
        type_rate=product.unit.type_rate,
        tax_policy_pct=current_tax_policy_pct,
        price_mapper=future_price,
        dt=NOW,
        currency=currency.char_code
    )


def test_get_price_force_price_dt_different_tax_policy_pcts(session):
    """если после получения цены и налога в них отличаются только налоговые ставки, но на дату налога налоговые ставки
    не совпадали, пересчитываем цену с полученным налогом"""
    currency = create_currency(session)
    product = create_non_currency_product(session, iso_currency=currency.iso_code)
    country = create_country(session)

    _, tax_policy, current_tax_policy_pct = create_valid_tax(session, product, currency, country, NOW)
    current_price_tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=FUTURE, nds_pct=17,
                                                         nsp_pct=0)
    create_price(session, price=666, currency_code=currency.char_code, product_id=product.id, hidden=0,
                 dt=NOW, tax_policy_pct=current_price_tax_policy_pct)

    price_tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=FUTURE, nds_pct=12,
                                                 nsp_pct=0)
    price = create_price(session, price=666, currency_code=currency.char_code, product_id=product.id, hidden=0,
                         dt=FUTURE, tax_policy_pct=price_tax_policy_pct)

    price_object = product.get_price(NOW, currency.char_code, country, resident=1, price_dt=FUTURE)

    check_price_object(
        price_object,
        price=DU(price.price, [currency.char_code], [product.unit.type_rate, 'QTY']),
        type_rate=product.unit.type_rate,
        tax_policy_pct=current_tax_policy_pct,
        price_mapper=price,
        dt=NOW,
        currency=currency.char_code
    )


def test_get_price_future_price_dt_different_tax_policies(session):
    """если после получения цены (налог внутри) и налога в них отличаются налоговые политики,
     пересчитываем цену с полученным налогом"""
    currency = create_currency(session)
    product = create_non_currency_product(session, iso_currency=currency.iso_code)
    country = create_country(session)

    _, _, current_tax_policy_pct = create_valid_tax(session, product, currency, country, NOW, nds_pct=66, nsp_pct=0)
    create_price(session, price=666, currency_code=currency.char_code, product_id=product.id, hidden=0,
                 dt=NOW, tax_policy_pct=current_tax_policy_pct, tax=1)

    _, _, future_tax_policy_pct = create_valid_tax(session, product, currency, country, FUTURE, nds_pct=6, nsp_pct=6)
    future_price = create_price(session, price=666, currency_code=currency.char_code, product_id=product.id, hidden=0,
                                dt=FUTURE, tax_policy_pct=future_tax_policy_pct)

    price_object = product.get_price(NOW, currency.char_code, country, resident=1, price_dt=FUTURE)

    new_price = current_tax_policy_pct.add_to(future_tax_policy_pct.remove_from(future_price.price.as_decimal()))
    check_price_object(
        price_object,
        price=DU(new_price, [currency.char_code], [product.unit.type_rate, 'QTY']),
        type_rate=product.unit.type_rate,
        tax_policy_pct=current_tax_policy_pct,
        price_mapper=future_price,
        dt=NOW,
        currency=currency.char_code
    )


@pytest.mark.parametrize('price_dt', [FUTURE, PAST])
def test_get_price_force_price_dt_same_tax_policy_tax_not_included(session, price_dt):
    """получаем цену без налога и налог на другую дату, добавляем налог к цене"""
    currency = create_currency(session)
    product = create_non_currency_product(session, iso_currency=currency.iso_code)
    country = create_country(session)

    _, tax_policy, current_tax_policy_pct = create_valid_tax(session, product, currency, country, NOW)
    create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=price_dt, nds_pct=12, nsp_pct=0)

    create_price(session, price=42, currency_code=currency.char_code, product_id=product.id, hidden=0,
                 dt=NOW, tax_policy_pct=None)

    non_current_price = create_price(session, price=666, currency_code=currency.char_code, product_id=product.id, hidden=0,
                                     dt=price_dt, tax_policy_pct=None)
    price_object = product.get_price(NOW, currency.char_code, country, resident=1, price_dt=price_dt)

    new_price = current_tax_policy_pct.add_to(non_current_price.price.as_decimal())
    check_price_object(
        price_object,
        price=DU(new_price, [currency.char_code], [product.unit.type_rate, 'QTY']),
        type_rate=product.unit.type_rate,
        tax_policy_pct=current_tax_policy_pct,
        price_mapper=non_current_price,
        dt=NOW,
        currency=currency.char_code
    )


@pytest.mark.parametrize('firm_id', [FirmId.YANDEX_OOO, FirmId.YANDEX_UA, FirmId.KAZNET_MEDIA, FirmId.YANDEX_EU_AG,
                                     FirmId.YANDEX_EU_AG, FirmId.YANDEX_TR])
@pytest.mark.parametrize('wo_tax', [True, False])
def test_tax_by_currency(session, firm_id, wo_tax):
    product = ob.Getter(Product, 1475).build(session).obj
    firm = ob.Getter(Firm, firm_id).build(session).obj
    prices_by_firm = {
        (FirmId.YANDEX_OOO, 'RUR'): (Decimal('30') / Decimal('1.20')),
        (FirmId.YANDEX_UA, 'UAH'): (Decimal('10')),
        (FirmId.KAZNET_MEDIA, 'KZT'): (Decimal('105')),
        (FirmId.YANDEX_EU_AG, 'USD'): (Decimal('0.41')),
        (FirmId.YANDEX_EU_AG, 'EUR'): (Decimal('0.39')),
        (FirmId.YANDEX_TR, 'TRY'): (Decimal('1.07')),
    }
    resident_flags = {tax_policy.resident for tax_policy in firm.country.tax_policies}
    for resident in resident_flags:
        currency = firm.default_currency
        price_obj = product.get_price(NOW, currency, country=firm.country, resident=resident, wo_tax=wo_tax)
        assert price_obj.price_wo_tax_by_piece == DU(prices_by_firm[(firm.id, currency)], [currency], ['QTY'])
        if wo_tax is True:
            assert price_obj.tax_policy_pct is None
        elif wo_tax is False:
            assert price_obj.tax_policy_pct is not None
