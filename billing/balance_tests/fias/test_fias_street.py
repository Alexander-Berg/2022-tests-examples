# -*- coding: utf-8 -*-


from tests.balance_tests.fias.fias_common import create_fias_city, create_fias_street, PLANNING_STRUCTURE
from balance.mapper import fias


def test_drop_street(session):
    structure, _ = create_fias_city(session, formal_name=u'Регион', short_name=u'р.', obj_level=PLANNING_STRUCTURE)
    street, _ = create_fias_street(session, formal_name=u'Советская', city=structure)
    assert street.drop_street == structure


def test_drop_street_from_city(session):
    structure, _ = create_fias_city(session, formal_name=u'Регион', short_name=u'р.', obj_level=PLANNING_STRUCTURE)
    assert structure.drop_street == structure


def test_streets_empty(session):
    _, city = create_fias_city(session, formal_name=u'Москва')
    result = fias.FiasStreet.streets(fias=city, street=u'ул. центра')
    assert result == []


def test_streets_get(session):
    """Возвращаем улицы, начинающиеся с букв, вне зависимости от регистра"""
    _, city = create_fias_city(session, formal_name=u'Москва')
    _, street = create_fias_street(session, city=city, formal_name=u'Центральная')
    result = fias.FiasStreet.streets(fias=city, street=u'уЛ. ценТРа')
    assert set(result) == {street}


def test_streets_with_limit(session):
    _, city = create_fias_city(session, formal_name=u'Москва')
    [create_fias_street(session, city=city, formal_name=u'центральная') for _ in range(2)]
    result = fias.FiasStreet.streets(fias=city, street=u'ул. центра', limit=1)
    assert len(result) == 1


def test_streets_by_parent_name(session):
    """ищем улицу по полному имени до города - parent_name"""
    _, city = create_fias_city(session, formal_name=u'Москва')
    create_fias_street(session, city=city, formal_name=u'центральная', parent_name=u'зеленый район')
    _, street = create_fias_street(session, city=city, formal_name=u'Центральная', parent_name=u'тИхий район')
    result = fias.FiasStreet.streets(fias=city, street=u'центральная ул., тиХ')
    assert set(result) == {street}
