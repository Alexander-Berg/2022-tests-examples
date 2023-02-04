# -*- coding: utf-8 -*-

import pytest
import datetime

from butils.decimal_unit import DecimalUnit as DU
from balance.constants import TAX_POLICY_PCT_ID_18_RUSSIA
from balance.mapper.products import PriceObject
from balance.mapper import TaxPolicyPct
from balance import exc

from tests.balance_tests.price.price_common import create_non_currency_product, create_currency, create_currency_product, create_price, \
    create_tax_policy_pct

NOW = datetime.datetime.now().replace(microsecond=0)


def test_product_currency_is_none(session):
    """Валюта продукта может быть не указана"""
    product = create_non_currency_product(session)
    assert product.product_currency is None


def test_price_by_date_currency_product(session):
    """у валютного продукта цена всегда равно 1 единице валюты за штуку"""
    currency = create_currency(session)
    product = create_currency_product(session, iso_currency=currency.iso_code)
    result = product.price_by_date(dt=NOW, currency=None)
    assert result == PriceObject(price=DU(1, [product.unit.iso_currency], [1, 'QTY']),
                                 type_rate=product.unit.type_rate,
                                 tax_policy_pct=None,
                                 price_mapper=None,
                                 dt=NOW,
                                 currency=product.unit.iso_currency)


def test_price_by_date_currency_product_other_currency(session):
    """у валютного продукта цена всегда равно 1 единице валюты продукта за штуку, даже если в price_by_date
    передали другую валюту"""
    currency = create_currency(session)
    product = create_currency_product(session, iso_currency=currency.iso_code)
    result = product.price_by_date(dt=NOW, currency=create_currency(session).char_code)
    assert result == PriceObject(price=DU(1, [product.unit.iso_currency], [1, 'QTY']),
                                 type_rate=product.unit.type_rate,
                                 tax_policy_pct=None,
                                 price_mapper=None,
                                 dt=NOW,
                                 currency=product.unit.iso_currency)


def test_price_by_date_rur_product(session):
    """правим rub на rur при определении цены валютного продукта"""
    product = create_currency_product(session, iso_currency='RUB')
    result = product.price_by_date(dt=NOW, currency=None)
    assert result == PriceObject(price=DU(1, ['RUR'], [1, 'QTY']),
                                 type_rate=product.unit.type_rate,
                                 tax_policy_pct=None,
                                 price_mapper=None,
                                 dt=NOW,
                                 currency='RUR')


def test_price_by_date_non_currency_product_wo_reference_price_currency(session):
    """подбираем цену продукту по переданной дате и валюте"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                         tax_policy_pct=create_tax_policy_pct(session))
    result = product.price_by_date(dt=NOW, currency=currency.char_code)
    assert result.price == DU(price.price, [currency.char_code], [product.unit.type_rate, 'QTY'])
    assert result.type_rate == product.unit.type_rate
    assert result.tax_policy_pct == price.tax_policy_pct
    assert result.dt == NOW
    assert result.currency == currency.char_code


def test_price_by_date_non_currency_product_w_reference_price_currency(session):
    """если по переданной валюте определить цену не удалось, определяем цену по reference_price_currency"""
    product = create_non_currency_product(session)
    reference_currency = create_currency(session)
    product.reference_price_currency = reference_currency.char_code
    price = create_price(session, currency_code=reference_currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                         tax_policy_pct=create_tax_policy_pct(session))
    currency = create_currency(session)
    result = product.price_by_date(dt=NOW, currency=currency.char_code)

    assert result.price == DU(price.price, [reference_currency.char_code], [product.unit.type_rate, 'QTY'])
    assert result.type_rate == product.unit.type_rate
    assert result.tax_policy_pct == price.tax_policy_pct
    assert result.dt == NOW
    assert result.currency == reference_currency.char_code


def test_price_by_date_non_currency_product_w_reference_price_currency_w_price_in_currency(session):
    """если есть цена и в переданной валюте, и в reference_price_currency, возвращаем цену в переданной валюте"""
    product = create_non_currency_product(session)
    reference_currency = create_currency(session)
    product.reference_price_currency = reference_currency.char_code
    price_currency = create_currency(session)
    for currency in (reference_currency, price_currency):
        price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=NOW,
                             tax_policy_pct=create_tax_policy_pct(session))

    result = product.price_by_date(dt=NOW, currency=price_currency.char_code)

    assert result.price == DU(price.price, [price_currency.char_code], [product.unit.type_rate, 'QTY'])
    assert result.type_rate == product.unit.type_rate
    assert result.tax_policy_pct == price.tax_policy_pct
    assert result.dt == NOW
    assert result.currency == price_currency.char_code


def test_price_by_date_non_currency_product_several_prices(session):
    """возвращаем самую позднюю цену, но не позднее переданной даты"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    prices = []
    for price_dt in (NOW - datetime.timedelta(seconds=1), NOW):
        prices.append(create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0, dt=price_dt,
                                   tax_policy_pct=create_tax_policy_pct(session)))
    result = product.price_by_date(dt=NOW, currency=currency.char_code)

    assert result.price == DU(prices[-1].price, [currency.char_code], [product.unit.type_rate, 'QTY'])
    assert result.type_rate == product.unit.type_rate
    assert result.tax_policy_pct == prices[-1].tax_policy_pct
    assert result.dt == NOW
    assert result.currency == currency.char_code


def test_price_by_date_non_currency_product_no_price(session):
    """выбрасываем исключение, если цена продукта не задана"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    with pytest.raises(exc.NO_PRODUCT_PRICE_ON_DATE) as exc_info:
        product.price_by_date(dt=NOW, currency=currency.char_code)
    error_msg = 'Price of product {} for currency {} is not defined on date {}'.format(
        product.id, currency.char_code, NOW)
    assert exc_info.value.msg == error_msg


def test_price_by_date_non_currency_product_hidden_price(session):
    """отбрасываем скрытые цены при подборе"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=1, dt=NOW,
                 tax_policy_pct=create_tax_policy_pct(session))
    with pytest.raises(exc.NO_PRODUCT_PRICE_ON_DATE) as exc_info:
        product.price_by_date(dt=NOW, currency=currency.char_code)
    error_msg = 'Price of product {} for currency {} is not defined on date {}'.format(
        product.id, currency.char_code, NOW)
    assert exc_info.value.msg == error_msg


def test_price_by_date_non_currency_product_future_price(session):
    """отбрасываем цены с датой действия в будущем при подборе"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0,
                 dt=NOW + datetime.timedelta(seconds=1), tax_policy_pct=create_tax_policy_pct(session))
    with pytest.raises(exc.NO_PRODUCT_PRICE_ON_DATE) as exc_info:
        product.price_by_date(dt=NOW, currency=currency.char_code)
    error_msg = 'Price of product {} for currency {} is not defined on date {}'.format(
        product.id, currency.char_code, NOW)
    assert exc_info.value.msg == error_msg


def test_price_by_date_non_currency_product_wrong_currency(session):
    """цену подбираем по переданной валюте"""
    product = create_non_currency_product(session)
    create_price(session, currency_code=create_currency(session).char_code, product_id=product.id, hidden=0,
                 dt=NOW, tax_policy_pct=create_tax_policy_pct(session))
    currency = create_currency(session)
    with pytest.raises(exc.NO_PRODUCT_PRICE_ON_DATE) as exc_info:
        product.price_by_date(dt=NOW, currency=currency.char_code)
    error_msg = 'Price of product {} for currency {} is not defined on date {}'.format(
        product.id, currency.char_code, NOW)
    assert exc_info.value.msg == error_msg


def test_price_by_date_non_currency_product_internal_price(session):
    """флаг use_internal позволяет не использовать внутренние цены"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0,
                 dt=NOW, tax_policy_pct=create_tax_policy_pct(session), internal=1)

    with pytest.raises(exc.NO_PRODUCT_PRICE_ON_DATE) as exc_info:
        product.price_by_date(dt=NOW, currency=currency.char_code, use_internal=False)
    error_msg = 'Price of product {} for currency {} is not defined on date {}'.format(product.id, currency.char_code,
                                                                                       NOW)
    assert exc_info.value.msg == error_msg


def test_price_by_date_non_currency_product_use_internal_price_by_default(session):
    """по умолчанию используем внутренние цены"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0,
                         dt=NOW, tax_policy_pct=create_tax_policy_pct(session), internal=1)
    result = product.price_by_date(dt=NOW, currency=currency.char_code)
    assert result.price == DU(price.price, [currency.char_code], [product.unit.type_rate, 'QTY'])
    assert result.type_rate == product.unit.type_rate
    assert result.tax_policy_pct == price.tax_policy_pct
    assert result.dt == NOW
    assert result.currency == currency.char_code


def test_price_by_date_non_currency_product_no_price_value(session):
    """цену без величины считаем недействительной"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0,
                 dt=NOW, tax_policy_pct=create_tax_policy_pct(session), internal=0, price=None)
    with pytest.raises(exc.NO_PRODUCT_PRICE_ON_DATE) as exc_info:
        product.price_by_date(dt=NOW, currency=currency.char_code)
    error_msg = 'Price of product {} for currency {} is not defined on date {}'.format(product.id, currency.char_code,
                                                                                       NOW)
    assert exc_info.value.msg == error_msg


def test_price_by_date_non_currency_product_zero_price_value(session):
    """цену с 0 считаем действительной"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0,
                         dt=NOW, tax_policy_pct=create_tax_policy_pct(session), internal=0, price=0)

    result = product.price_by_date(dt=NOW, currency=currency.char_code)
    assert result.price == DU(price.price, [currency.char_code], [product.unit.type_rate, 'QTY'])
    assert result.type_rate == product.unit.type_rate
    assert result.tax_policy_pct == price.tax_policy_pct
    assert result.dt == NOW
    assert result.currency == currency.char_code


def test_price_by_date_non_currency_product_old_style_tax(session):
    """если налог включен в цену, а налоговая политика не указана, вызываем исключение"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0,
                 dt=NOW, tax_policy_pct=None, internal=0, price=100, tax=1)
    with pytest.raises(exc.OLD_STYLE_TAX) as exc_info:
        product.price_by_date(dt=NOW, currency=currency.char_code)
    error_msg = 'Price for product {} on date {} for currency u\'{}\' has OLD-style tax'.format(
        product.id, NOW, currency.char_code)
    assert exc_info.value.msg == error_msg


def test_price_by_date_non_currency_product_allow_old_style_tax(session):
    """если налог включен в цену, а налоговая политика не указана, вызываем исключение"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0,
                         dt=NOW, tax_policy_pct=None, internal=0, price=100, tax=1)
    result = product.price_by_date(dt=NOW, currency=currency.char_code, allow_old_tax=True)
    assert result.price == DU(price.price, [currency.char_code], [product.unit.type_rate, 'QTY'])
    assert result.type_rate == product.unit.type_rate
    assert result.tax_policy_pct == session.query(TaxPolicyPct).get(TAX_POLICY_PCT_ID_18_RUSSIA)
    assert result.dt == NOW
    assert result.currency == currency.char_code


def test_price_by_date_non_currency_product_tax_not_included(session):
    """если налог включен в цену и налоговая политика не указана, налоговая политика может быть не указана"""
    product = create_non_currency_product(session)
    currency = create_currency(session)
    price = create_price(session, currency_code=currency.char_code, product_id=product.id, hidden=0,
                         dt=NOW, tax_policy_pct=None, internal=0, price=100)
    result = product.price_by_date(dt=NOW, currency=currency.char_code)
    assert result.price == DU(price.price, [currency.char_code], [product.unit.type_rate, 'QTY'])
    assert result.type_rate == product.unit.type_rate
    assert result.tax_policy_pct == price.tax_policy_pct
    assert result.dt == NOW
    assert result.currency == currency.char_code
