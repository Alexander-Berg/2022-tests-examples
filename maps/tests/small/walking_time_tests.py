# coding=utf-8

import yandex.maps.geolib3 as geolib
from maps.carparks.tools.dap_snippets.lib.common import LocalizedValue
import maps.carparks.tools.dap_snippets.lib.daps_source as daps_source


M = 360.0 / 4e7

# Pedestrian speed is about 1.4 m/s


def test_walking_time_zero():
    assert daps_source.get_walking_time(
        geolib.Point2(0, 0), geolib.Point2(0, 0)) == LocalizedValue(0, u'0 мин')


def test_walking_time_near_one_minute():
    assert daps_source.get_walking_time(
        geolib.Point2(0, 0), geolib.Point2(1 * M, 0)) == \
        LocalizedValue(0, u'1 мин')

    assert daps_source.get_walking_time(
        geolib.Point2(0, 0), geolib.Point2(80 * M, 0)) == \
        LocalizedValue(57, u'1 мин')


def test_walking_time_near_two_minutes():
    assert daps_source.get_walking_time(
        geolib.Point2(0, 0), geolib.Point2(100 * M, 0)) == \
        LocalizedValue(72, u'2 мин')

    assert daps_source.get_walking_time(
        geolib.Point2(0, 0), geolib.Point2(160 * M, 0)) == \
        LocalizedValue(115, u'2 мин')
