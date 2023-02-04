# coding: utf-8

import json

import httpretty
import pytest
from hamcrest import has_entries, assert_that, has_items, equal_to

from balance.application.plugins.geobase_db import GeobaseDb, LaasAPI
from balance.constants import RegionId


MOSCOW_REGION_ID = 213
RUSSIA_REGION_ID = RegionId.RUSSIA
CONTINENT_REGION_TYPE_ID = 1
MOSCOW_DATA = {
    u'id': 213, u'name': u'Москва', u'ename': u'Moscow', u'short_ename': u'MSK',
    u'type': 6, u'parent': 1,
}
CRIMEA_IP = '130.255.134.112'
SEVASTOPOL_DATA = {'name': u'Севастополь', 'parent': 977, 'type': 6,
                   'ename': 'Sevastopol', 'short_ename': 'SVP',
                   'id': 959}
CRIMEA_LAAS_REGION = json.dumps(
    {"region_id": 959, "precision": 2, "latitude": 44.556972, "longitude": 33.526402, "should_update_cookie": False,
     "is_user_choice": False, "suspected_region_id": 959, "city_id": 959, "region_by_ip": 959,
     "suspected_region_city": 959, "location_accuracy": 15000, "location_unixtime": 1565774020,
     "suspected_latitude": 44.556972, "suspected_longitude": 33.526402, "suspected_location_accuracy": 15000,
     "suspected_location_unixtime": 1565774020, "suspected_precision": 2, "region_home": 0,
     "probable_regions_reliability": 1.00, "probable_regions": [], "country_id_by_ip": 225,
     "is_anonymous_vpn": False, "is_public_proxy": False, "is_serp_trusted_net": False, "is_tor": False,
     "is_hosting": False, "is_gdpr": False, "is_mobile": False, "is_yandex_net": False, "is_yandex_staff": False})


def region_as_dict(result):
    dict_ = {attr: getattr(result, attr, '__MISSING_VALUE__') for attr in
             ['id', 'name', 'type', 'parent', 'ename', 'short_ename']}
    dict_['name'] = dict_['name'].decode('utf-8')
    return dict_


@pytest.fixture()
def geobase():
    return GeobaseDb(LaasAPI())


@pytest.mark.parametrize('method', ['region', 'region_by_ip'])
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_region_by_ip(geobase, method):
    httpretty.register_uri(httpretty.GET, geobase.laas_api.region_url, body=CRIMEA_LAAS_REGION)
    result = getattr(geobase, method)(CRIMEA_IP)
    assert_that(region_as_dict(result), has_entries(SEVASTOPOL_DATA))
    headers = dict(httpretty.last_request().headers)
    assert headers['x-forwarded-for-y'] == CRIMEA_IP


@pytest.mark.parametrize('method', ['region_by_id', 'regionById'])
def test_region_by_id(geobase, method):
    result = getattr(geobase, method)(MOSCOW_REGION_ID)
    assert_that(region_as_dict(result), has_entries(MOSCOW_DATA))


def test_parents(geobase):
    assert_that(geobase.parents(MOSCOW_REGION_ID), equal_to([213, 1, 3, 225, 10001, 10000]))


def test_id_is_in_true(geobase):
    assert_that(geobase.id_is_in(MOSCOW_REGION_ID, RUSSIA_REGION_ID), equal_to(True))


def test_id_is_in_false(geobase):
    assert_that(geobase.id_is_in(RUSSIA_REGION_ID, MOSCOW_REGION_ID), equal_to(False))


def test_regions_by_type(geobase):
    result = geobase.regions_by_type(CONTINENT_REGION_TYPE_ID)
    assert_that([region_as_dict(continent) for continent in result],
                has_items(*[has_entries(continent) for continent in [
                    {'ename': 'Europe', 'type': 1, 'id': 111},
                    {'ename': 'Australia and Oceania', 'type': 1, 'id': 138},
                    {'ename': 'Asia', 'type': 1, 'id': 183},
                    {'ename': 'Africa', 'type': 1, 'id': 241},
                    {'ename': 'Arctic and Antarctic', 'type': 1, 'id': 245},
                    {'ename': 'Eurasia', 'type': 1, 'id': 10001},
                    {'ename': 'North America', 'type': 1, 'id': 10002},
                    {'ename': 'South America', 'type': 1, 'id': 10003}]]))


def test_short_ename_absence(geobase):
    region_without_short_ename = 183
    assert_that(geobase.region_by_id(region_without_short_ename).short_ename, equal_to(''))
