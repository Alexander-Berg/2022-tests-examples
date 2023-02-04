# -*- coding: utf-8 -*-

from balance.mapper import fias
from tests import object_builder as ob

REGION = 1
AUTONOMOUS_REGION = 2
DISTRICT = 3
CITY = 4
INNER_URBAN_TERRITORY = 5
TOWN = 6
PLANNING_STRUCTURE = 65
STREET = 7
LAND_PARCEL = 75
BUILDING = 8
BUILDING_UNIT = 9

NOT_CENTER = 0
DISTRICT_CENTER = 1
REGION_CENTER = 2
REGION_AND_DISTRICT_CENTER = 3


def create_fias_row(session, short_name, formal_name, parent_fias=None, center_status=NOT_CENTER, **attrs):
    return ob.FiasBuilder(parent_fias=parent_fias, short_name=short_name, formal_name=formal_name,
                          center_status=center_status, **attrs).build(session).obj


def create_fias_city(session, formal_name, short_name=u'г.', obj_level=CITY, **attrs):
    fias_row = create_fias_row(session, short_name=short_name, formal_name=formal_name, obj_level=obj_level,
                               **attrs)
    city = fias.FiasCity(guid=fias_row.guid, short_name=fias_row.short_name, formal_name=fias_row.formal_name)
    session.add(city)
    session.flush()
    return fias_row, city


def create_fias_street(session, city, formal_name, parent_name=u'', **kwargs):
    fias_row = create_fias_row(session, short_name=u'ул.', formal_name=formal_name, parent_guid=city and city.guid,
                               **kwargs)
    street = fias.FiasStreet(guid=fias_row.guid, short_name=fias_row.short_name, formal_name=fias_row.formal_name,
                             parent_guid=city and city.guid, parent_name=parent_name)
    session.add(street)
    session.flush()
    return fias_row, street


def create_person(session, **attrs):
    return ob.PersonBuilder(type='ur', **attrs).build(session).obj
