# -*- coding: utf-8 -*-
import pytest
import datetime

from balance import exc
from balance.mapper.products import TaxPolicyPct, TaxPolicy

from tests.balance_tests.price.price_common import create_tax_policy_pct, create_tax_policy, create_country, create_product, create_currency, \
    create_tax

NOW = datetime.datetime.now().replace(microsecond=0)

NDS_PCT = 45
NSP_PCT = 12


def test_pct_by_date_no_tax_policy_pct(session):
    """выкидываем исключение, если ни одной подходящей налоговой ставки по налоговой политике на нашлось"""
    tax_policy = create_tax_policy(session)
    with pytest.raises(exc.NO_PRODUCT_TAX_ON_DATE) as exc_info:
        tax_policy.pct_by_date(dt=NOW)
    error_msg = 'Tax for tax_policy {} is not defined on date {}, {}, {}'.format(tax_policy.id, NOW, None, None)
    assert exc_info.value.msg == error_msg


def test_pct_by_date(session):
    """возвращаем подходящую по налоговой политике налоговую ставку"""
    tax_policy = create_tax_policy(session)
    tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=NDS_PCT)
    result = tax_policy.pct_by_date(dt=NOW)
    assert result is tax_policy_pct


def test_pct_by_date_hidden(session):
    """скрытые налоговые ставки не участвуют в подборе ставки по политике"""
    tax_policy = create_tax_policy(session)
    create_tax_policy_pct(session, policy=tax_policy, hidden=1, dt=NOW, nds_pct=NDS_PCT)
    with pytest.raises(exc.NO_PRODUCT_TAX_ON_DATE) as exc_info:
        tax_policy.pct_by_date(dt=NOW)
    error_msg = 'Tax for tax_policy {} is not defined on date {}, {}, {}'.format(tax_policy.id, NOW, None, None)
    assert exc_info.value.msg == error_msg


def test_pct_by_date_in_future(session):
    """налоговые ставки с датой действия в будущем не участвуют в подборе ставки по политике"""
    tax_policy = create_tax_policy(session)
    create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW + datetime.timedelta(hours=1), nds_pct=NDS_PCT)
    with pytest.raises(exc.NO_PRODUCT_TAX_ON_DATE) as exc_info:
        tax_policy.pct_by_date(dt=NOW)
    error_msg = 'Tax for tax_policy {} is not defined on date {}, {}, {}'.format(tax_policy.id, NOW, None, None)
    assert exc_info.value.msg == error_msg


def test_pct_by_date_several_taxes(session):
    """из двух действующих налоговых ставок выбираем более позднюю"""
    tax_policy = create_tax_policy(session)
    dates = [NOW - datetime.timedelta(hours=2), NOW - datetime.timedelta(hours=1)]
    policies = [create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=date, nds_pct=NDS_PCT) for date in dates]
    result = tax_policy.pct_by_date(dt=NOW)
    assert result is policies[1]


def test_from_country_resident_empty_response(session):
    """возвращаем пустой список, если определить налоговую политику по стране невозможно"""
    country = create_country(session)
    result = TaxPolicy.from_country_resident(session, country=country)
    assert result == []


def test_from_country_resident(session):
    """определяем налоговую политику по стране"""
    country = create_country(session)
    tax_policy = create_tax_policy(session, country=country, hidden=0)
    result = TaxPolicy.from_country_resident(session, country=country)
    assert result == [tax_policy]


def test_from_country_resident_hidden(session):
    """скрытая налоговая политика не участвует в подборе по стране"""
    country = create_country(session)
    create_tax_policy(session, country=country, hidden=1)
    result = TaxPolicy.from_country_resident(session, country=country)
    assert result == []


def test_from_country_resident_default_tax_by_default(session):
    """по умолчанию при подборе налоговой политики по стране, подбираются только дефолтные политики"""
    country = create_country(session)
    tax_policy = create_tax_policy(session, country=country, default_tax=1)
    create_tax_policy(session, country=country, default_tax=0)
    result = TaxPolicy.from_country_resident(session, country=country)
    assert result == [tax_policy]


def test_from_country_resident_default_tax_false(session):
    """с параметром default_tax_only=False, подобранные налоговые политики не фильтруются по признаку default_tax"""
    country = create_country(session)
    default_tax_policy = create_tax_policy(session, country=country, default_tax=1)
    non_default_tax_policy = create_tax_policy(session, country=country, default_tax=0)
    result = TaxPolicy.from_country_resident(session, country=country, default_tax_only=False)
    assert set(result) == {default_tax_policy, non_default_tax_policy}


def test_from_country_resident_filter_by_resident(session):
    """при подборе по стране признак резидентской налоговой политики можно указать явно"""
    country = create_country(session)
    create_tax_policy(session, country=country, default_tax=1, resident=1)
    tax_policy_1 = create_tax_policy(session, country=country, default_tax=1, resident=0)
    result = TaxPolicy.from_country_resident(session, country=country, resident=0)
    assert result == [tax_policy_1]


def test_from_taxes(session):
    """подбираем налоговую ставку по проценту"""
    tax_policy = create_tax_policy(session)
    tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=NDS_PCT, nsp_pct=0)
    result = TaxPolicyPct.from_taxes(session, nds_pct=tax_policy_pct.nds_pct, nsp_pct=tax_policy_pct.nsp_pct, dt=NOW)
    assert result is tax_policy_pct


def test_from_taxes_hidden_tax_policy_pct(session):
    """скрытые налоговые ставки не участвуют в подборе ставки по проценту"""
    tax_policy = create_tax_policy(session)
    tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=1, dt=NOW, nds_pct=NDS_PCT, nsp_pct=0)
    result = TaxPolicyPct.from_taxes(session, nds_pct=tax_policy_pct.nds_pct, nsp_pct=tax_policy_pct.nsp_pct, dt=NOW)
    assert result is None


def test_from_taxes_hidden_tax_policy(session):
    """налоговые ставки со скрытой налоговой политикой не участвуют в подборе ставки по проценту"""
    tax_policy = create_tax_policy(session, hidden=1)
    tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=NDS_PCT, nsp_pct=0)
    result = TaxPolicyPct.from_taxes(session, nds_pct=tax_policy_pct.nds_pct, nsp_pct=tax_policy_pct.nsp_pct,
                                     dt=NOW)
    assert result is None


def test_from_taxes_tax_policy_pct_in_future(session):
    """налоговые ставки с датой действия в будущем не участвуют в подборе ставки по проценту"""
    tax_policy = create_tax_policy(session)
    tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW + datetime.timedelta(hours=1),
                                           nds_pct=NDS_PCT, nsp_pct=0)
    result = TaxPolicyPct.from_taxes(session, nds_pct=tax_policy_pct.nds_pct, nsp_pct=tax_policy_pct.nsp_pct, dt=NOW)
    assert result is None


def test_from_taxes_several_tax_policy_pct(session):
    """из двух действующих налоговых ставок выбираем более позднюю"""
    tax_policy = create_tax_policy(session)
    dates = [NOW - datetime.timedelta(hours=2), NOW - datetime.timedelta(hours=1)]
    policies = [create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=date, nds_pct=NDS_PCT, nsp_pct=0) for
                date in dates]
    result = TaxPolicyPct.from_taxes(session, nds_pct=NDS_PCT, nsp_pct=0, dt=NOW)
    assert result is policies[-1]


def test_from_taxes_filter_by_country(session):
    """подбираем налоговую ставку по проценту c учетом страны налоговой политики, если была передана"""
    policy_pcts = []
    for pct_dt in [NOW - datetime.timedelta(hours=2), NOW - datetime.timedelta(hours=1)]:
        tax_policy = create_tax_policy(session, country=create_country(session))
        tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=pct_dt, nds_pct=NDS_PCT,
                                               nsp_pct=0)
        policy_pcts.append(tax_policy_pct)
    result = TaxPolicyPct.from_taxes(session, nds_pct=NDS_PCT, nsp_pct=0, dt=NOW, country=policy_pcts[0].policy.country)
    assert result is policy_pcts[0]


def test_from_taxes_filter_by_resident(session):
    """подбираем налоговую ставку по проценту c учетом признака резиденства налоговой политики, если был передан"""
    policy_pcts = []
    for resident, pct_dt in [(1, NOW - datetime.timedelta(hours=2)),
                             (0, NOW - datetime.timedelta(hours=1))]:
        tax_policy = create_tax_policy(session, resident=resident)
        tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=pct_dt, nds_pct=NDS_PCT,
                                               nsp_pct=0)
        policy_pcts.append(tax_policy_pct)
    result = TaxPolicyPct.from_taxes(session, nds_pct=NDS_PCT, nsp_pct=0, dt=NOW, resident=1)
    assert result is policy_pcts[0]


def test_from_taxes_filter_by_default_tax(session):
    """при подборе налоговой ставки по проценту доступные ставки сортируются по признаку default_tax налоговой
    политики и времени ставки"""
    policy_pcts = []
    for default_tax, pct_dt in [(1, NOW - datetime.timedelta(hours=2)),
                                (0, NOW - datetime.timedelta(hours=1))]:
        tax_policy = create_tax_policy(session, default_tax=default_tax)
        tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=pct_dt, nds_pct=NDS_PCT,
                                               nsp_pct=0)
        policy_pcts.append(tax_policy_pct)
    result = TaxPolicyPct.from_taxes(session, nds_pct=NDS_PCT, nsp_pct=0, dt=NOW)
    assert result is policy_pcts[0]


def test_tax_by_date_no_taxes(session):
    """если налог определить не удалось, выбрасываем исключение"""
    country = create_country(session)
    product = create_product(session)
    with pytest.raises(exc.NO_PRODUCT_TAX_ON_DATE) as exc_info:
        product.tax_by_date(dt=NOW, country=country)
    error_msg = 'Tax for product {} is not defined on date {}, {}, None'.format(product.id, NOW, country)
    assert exc_info.value.msg == error_msg


def test_tax_by_date_no_taxes_dont_raise(session):
    """подавляем исключение с флагом dont_raise"""
    country = create_country(session)
    product = create_product(session)
    result = product.tax_by_date(dt=NOW, country=country, dont_raise=True)
    assert result is None


def test_tax_by_date_filter_by_resident(session):
    """подбираем налоговую ставку налога через налоговую политику, с учетом признака резиденства"""
    currency = create_currency(session)
    product = create_product(session)
    country = create_country(session)
    tax_policy_pcts = []
    for resident, tax_dt in [(1, NOW - datetime.timedelta(seconds=1)),
                             (0, NOW)]:
        tax_policy = create_tax_policy(session, resident=resident, country=country)
        create_tax(session, currency=currency.char_code, currency_id=currency.char_code, product_id=product.id,
                   hidden=0, dt=tax_dt, nds_pct=None, nsp_pct=None, policy=tax_policy)
        tax_policy_pcts.append(create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=NDS_PCT,
                                                     nsp_pct=NSP_PCT))
    result = product.tax_by_date(dt=NOW, country=country, resident=1)
    assert result is tax_policy_pcts[0]


@pytest.mark.parametrize('resident', [0, 1])
def test_tax_by_date_resident_new_style_tax_non_defined_resident(session, resident):
    """если признак резиденства не указан явно, возвращаем налоговую ставку с налоговой ставкой без учета resident"""
    product = create_product(session)
    currency = create_currency(session)
    country = create_country(session, default_currency=currency.char_code)
    tax_policy = create_tax_policy(session, resident=resident, country=country)
    create_tax(session, currency=currency.char_code, currency_id=currency.char_code, product_id=product.id,
               hidden=0, dt=NOW, policy=tax_policy, iso_currency=currency.iso_code)
    tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=NDS_PCT,
                                           nsp_pct=NSP_PCT)

    result = product.tax_by_date(dt=NOW, country=country, resident=None)
    assert result is tax_policy_pct


def test_tax_by_date_resident_new_style_tax_hidden_tax(session):
    """скрытый налог не участвует в подборе налога для продукта"""
    product = create_product(session)
    currency = create_currency(session)
    country = create_country(session)
    tax_policy = create_tax_policy(session, resident=1, country=country)
    create_tax(session, currency=currency.char_code, currency_id=currency.char_code, product_id=product.id,
               hidden=1, dt=NOW, policy=tax_policy, iso_currency=currency.iso_code)
    create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=NDS_PCT, nsp_pct=NSP_PCT)
    with pytest.raises(exc.NO_PRODUCT_TAX_ON_DATE) as exc_info:
        product.tax_by_date(dt=NOW, country=country, resident=1)
    error_msg = 'Tax for product {} is not defined on date {}, {}, {}'.format(product.id, NOW, country,
                                                                              tax_policy.resident)
    assert exc_info.value.msg == error_msg


def test_tax_by_date_resident_new_style_tax_in_future(session):
    """налог с датой начала в будущем не участвует в подборе налога для продукта"""
    product = create_product(session)
    currency = create_currency(session)
    country = create_country(session)
    tax_policy = create_tax_policy(session, resident=1, country=country)
    create_tax(session, currency=currency.char_code, currency_id=currency.char_code, product_id=product.id,
               hidden=0, dt=NOW + datetime.timedelta(hours=1), policy=tax_policy,
               iso_currency=currency.iso_code)
    create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=NDS_PCT, nsp_pct=NSP_PCT)
    with pytest.raises(exc.NO_PRODUCT_TAX_ON_DATE) as exc_info:
        product.tax_by_date(dt=NOW, country=country, resident=1)
    error_msg = 'Tax for product {} is not defined on date {}, {}, {}'.format(product.id, NOW, country,
                                                                              tax_policy.resident)
    assert exc_info.value.msg == error_msg


def test_tax_by_date_resident_new_style_tax_hidden_tax_policy(session):
    """налог со скрытой налоговой политикой  не участвует в подборе налога для продукта"""
    product = create_product(session)
    currency = create_currency(session)
    country = create_country(session)
    tax_policy = create_tax_policy(session, resident=1, country=country, hidden=1)
    create_tax(session, currency=currency.char_code, currency_id=currency.char_code, product_id=product.id,
               hidden=0, dt=NOW, policy=tax_policy, iso_currency=currency.iso_code)
    create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=NDS_PCT, nsp_pct=NSP_PCT)
    with pytest.raises(exc.NO_PRODUCT_TAX_ON_DATE) as exc_info:
        product.tax_by_date(dt=NOW, country=country, resident=1)
    error_msg = 'Tax for product {} is not defined on date {}, {}, {}'.format(product.id, NOW, country,
                                                                              tax_policy.resident)
    assert exc_info.value.msg == error_msg


def test_tax_by_date_with_default_tax(session):
    """если налог подобрать не удалось, с флагом default_tax пытаемся подобрать дефолтный налог для страны"""
    product = create_product(session)
    country = create_country(session)
    tax_policy = create_tax_policy(session, resident=1, country=country)
    tax_policy_pct = create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=NOW, nds_pct=NDS_PCT,
                                           nsp_pct=NSP_PCT)
    result = product.tax_by_date(dt=NOW, country=country, resident=None, default_tax=True)
    assert result is tax_policy_pct


def test_tax_by_date_resident_with_default_tax(session):
    """подбираем дефолтный налог с учетом резиденства"""
    product = create_product(session)
    country = create_country(session)
    tax_policy_pcts = []
    for resident, tax_policy_pct_dt in [(1, NOW - datetime.timedelta(seconds=1)),
                                        (0, NOW)]:
        tax_policy = create_tax_policy(session, resident=resident, country=country)
        tax_policy_pcts.append(create_tax_policy_pct(session, policy=tax_policy, hidden=0, dt=tax_policy_pct_dt,
                                                     nds_pct=NDS_PCT, nsp_pct=NSP_PCT))
    result = product.tax_by_date(dt=NOW, country=country, resident=1, default_tax=True)
    assert result is tax_policy_pcts[0]
