# -*- coding: utf-8 -*-
import pytest

from tests.balance_tests.fias.fias_common import *


@pytest.mark.parametrize('letters_start_with', [u'несущесТвую', u'Г. несУществую'])
def test_cities(session, letters_start_with):
    """ищем по имени, по имени и короткому имени без учета регистра"""
    _, city = create_fias_city(session, formal_name=u'Несуществующий_город')
    result = fias.FiasCity.cities(session, name=letters_start_with)
    assert result == ([city], 1)


def test_cities_sort_by_formal_name(session):
    _, city = create_fias_city(session, formal_name=u'Несуществующий_город')
    _, city_2 = create_fias_city(session, formal_name=u'Несуществующий_городок')
    result = fias.FiasCity.cities(session, name=u'г. несуществую')
    assert result == ([city, city_2], 2)


def test_fias_city_parents(session):
    """записываем в родители фиасного города все родительские структуры больше улицы"""
    fias_objects = []
    parent_fias = None
    for object_level in [REGION, AUTONOMOUS_REGION, DISTRICT, CITY, INNER_URBAN_TERRITORY, TOWN, PLANNING_STRUCTURE]:
        fias_row = create_fias_row(session, formal_name=u'Фиас', short_name=u'р.', obj_level=object_level,
                                   center_status=NOT_CENTER, parent_fias=parent_fias)
        fias_objects.append(fias_row)
        parent_fias = fias_row
    assert fias_objects[-1].city_parents == fias_objects[::-1]


@pytest.mark.parametrize('city_center_status', [DISTRICT_CENTER, REGION_CENTER, REGION_AND_DISTRICT_CENTER, NOT_CENTER])
def test_fias_city_parents_district_depend_on_city_center_status(session, city_center_status):
    """записываем район в родители города, только если город не является центром района, автономии записываем всегда"""
    autonomous_region = create_fias_row(session, formal_name=u'Автономия', short_name=u'р.',
                                        obj_level=AUTONOMOUS_REGION, center_status=NOT_CENTER)
    district = create_fias_row(session, formal_name=u'Район', short_name=u'р.', obj_level=DISTRICT,
                               center_status=NOT_CENTER, parent_fias=autonomous_region)
    city = create_fias_row(session, formal_name=u'Город', short_name=u'г.', obj_level=CITY,
                           center_status=city_center_status, parent_fias=district)
    if city_center_status in (DISTRICT_CENTER, REGION_AND_DISTRICT_CENTER):
        assert city.city_parents == [city, autonomous_region]
    else:
        assert city.city_parents == [city, district, autonomous_region]


@pytest.mark.parametrize('city_center_status', [DISTRICT_CENTER, REGION_CENTER, REGION_AND_DISTRICT_CENTER, NOT_CENTER])
def test_fias_city_parents_region_depend_on_city_center_status(session, city_center_status):
    """записываем регион в родители города, только если город не является центром региона, автономии
    записываем всегда"""
    region = create_fias_row(session, formal_name=u'Регион', short_name=u'р.', obj_level=REGION,
                             center_status=NOT_CENTER)
    autonomous_region = create_fias_row(session, formal_name=u'Автономия', short_name=u'р.',
                                        obj_level=AUTONOMOUS_REGION, center_status=NOT_CENTER, parent_fias=region)
    city = create_fias_row(session, formal_name=u'Город', short_name=u'г.', obj_level=CITY,
                           center_status=city_center_status, parent_fias=autonomous_region)
    if city_center_status in (REGION_CENTER, REGION_AND_DISTRICT_CENTER):
        assert city.city_parents == [city, autonomous_region]
    else:
        assert city.city_parents == [city, autonomous_region, region]


def test_cities_by_limit(session):
    """При передаче лимита cities вернет общее кол-во записей, а выдачу городов ограничит лимитом-1
    (строчка на "показать все")"""
    _, city = create_fias_city(session, formal_name=u'Несуществующий_город')
    _, city2 = create_fias_city(session, formal_name=u'Несуществующий_городок')
    _, city3 = create_fias_city(session, formal_name=u'Несуществующий_городочек')
    result = fias.FiasCity.cities(session, name=u'г. несуществую', limit=2)
    assert result == ([city], 3)


def test_city_chain(session):
    district = create_fias_row(session, formal_name=u'Район', short_name=u'р.', obj_level=DISTRICT,
                               center_status=NOT_CENTER)
    city, _ = create_fias_city(session, formal_name=u'Город', short_name=u'г.', obj_level=CITY, parent_fias=district)
    assert city.city_chain == [u'г.', u'Город', u'Район', u'р.']


def test_oebs_chain(session):
    district = create_fias_row(session, formal_name=u'Район', short_name=u'р.', obj_level=DISTRICT,
                               center_status=NOT_CENTER)
    city, _ = create_fias_city(session, formal_name=u'Город', short_name=u'г.', obj_level=CITY, parent_fias=district)
    assert city.oebs_chain == [u'Район', u'р.', u'г.', u'Город']


def test_check_city_fias(session, muzzle_logic):
    region = create_fias_row(session, formal_name=u'Регион', short_name=u'р.', obj_level=REGION,
                             center_status=NOT_CENTER)
    district = create_fias_row(session, formal_name=u'Район', short_name=u'р.', obj_level=DISTRICT,
                               center_status=NOT_CENTER, parent_fias=region)
    city, _ = create_fias_city(session, formal_name=u'Несуществующий_город', parent_fias=district)
    cities = muzzle_logic.check_city_fias(session, city=u'г. несуществую', limit=25)
    assert cities.tag == 'cities'
    assert len(cities) == 1
    assert cities[0].tag == 'city'
    assert cities[0].text == u'г. Несуществующий_город|Район р. Регион р.|{}\n'.format(city.guid)


def test_check_city_fias_count_more_than_limit(session, muzzle_logic):
    _, city = create_fias_city(session, formal_name=u'Несуществующий_город')
    _, city2 = create_fias_city(session, formal_name=u'Несуществующий_городок')
    cities = muzzle_logic.check_city_fias(session, city=u'несущ'.encode('utf-8'), limit=1)
    assert cities.tag == 'cities'
    assert len(cities) == 1
    assert cities[0].tag == 'city'
    assert cities[0].text == u'Показать все|несущ|-2'


def test_check_city_fias_empty_result(session, muzzle_logic):
    cities = muzzle_logic.check_city_fias(session, city=u'г. несуществую', limit=25)
    assert cities.tag == 'empty'
    assert len(cities) == 0
