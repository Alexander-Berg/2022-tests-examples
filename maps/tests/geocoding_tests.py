# coding=utf-8

import unittest
import pytest
from yandex.maps.geolib3 import Point2

import maps.carparks.libs.geocoding.geocoding as geocoding
from maps.carparks.libs.geocoding.geocoding import GeocoderResult

M = 360.0 / 4e7


class GeocoderMock(geocoding.Geocoder):
    def __init__(self, result_dict):
        self.result_dict = result_dict

    def resolve(self, address):
        return self.result_dict.get(address)


def get_ids(results):
    return sorted([result.id for result in results])


class GoodGeocoderIdsTests(unittest.TestCase):
    def test_geocodable_address(self):
        assert get_ids(geocoding.get_good_geocoder_results(
            ['A'],
            Point2(0, 0),
            geocoder=GeocoderMock({
                'A': [GeocoderResult('a', Point2(0, 0),
                                     address='A')]
            }))) == ['a']

    def test_geocodable_addresses(self):
        assert get_ids(geocoding.get_good_geocoder_results(
            ['A', 'B'],
            Point2(0, 0),
            geocoder=GeocoderMock({
                'A': [GeocoderResult('a', Point2(0, 0), address='A')],
                'B': [GeocoderResult('b', Point2(0, 0), address='B')],
            }))) == ['a', 'b']

    # TODO : check logic of this test
    def test_geocodable_addresses_with_same_ids(self):
        assert get_ids(geocoding.get_good_geocoder_results(
            ['A', 'AA'],
            Point2(0, 0),
            geocoder=GeocoderMock({
                'A': [GeocoderResult('a', Point2(0, 0), address='A')],
                'AA': [GeocoderResult('a', Point2(0, 0), address='AA')],
            }))) == ['a']

    def test_non_geocodable_address(self):
        class GeocoderMock(geocoding.Geocoder):
            def resolve(self, address):
                raise geocoding.GeocoderException()

        with pytest.raises(geocoding.GeocoderException):
            geocoding.get_good_geocoder_results(['A'],
                                                Point2(0, 0),
                                                geocoder=GeocoderMock(''))

    def test_geocodable_and_non_geocodable_addresses(self):
        class GeocoderMock(geocoding.Geocoder):
            def resolve(self, address):
                if address == 'Good':
                    return [GeocoderResult('good', Point2(0, 0),
                                           address='Good')]
                raise geocoding.GeocoderException()

        assert (get_ids(geocoding.get_good_geocoder_results(
            ['Good', 'Bad'],
            Point2(0, 0),
            geocoder=GeocoderMock(''))) == ['good'])
        assert (get_ids(geocoding.get_good_geocoder_results(
            ['Bad', 'Good'],
            Point2(0, 0),
            geocoder=GeocoderMock(''))) == ['good'])

    def test_far_address(self):
        with pytest.raises(geocoding.GeocoderException):
            geocoding.get_good_geocoder_results(
                ['Far'],
                Point2(0, 0),
                geocoder=GeocoderMock({
                    'Far': [GeocoderResult('far', Point2(0, 400 * M),
                                           address='Far')]
                }))

    def test_near_and_far_addresses(self):
        assert (get_ids(geocoding.get_good_geocoder_results(
            ['Far', 'Near'], Point2(0, 0),
            geocoder=GeocoderMock({
                'Near': [GeocoderResult('near', Point2(0, 10 * M),
                                        address='Near')],
                'Far': [GeocoderResult('far', Point2(0, 400 * M),
                                       address='Far')]
            }))) == ['near'])

    def test_first_result_is_selected(self):
        assert get_ids(geocoding.get_good_geocoder_results(
            ['A'],
            Point2(0, 0),
            geocoder=GeocoderMock({
                'A': [GeocoderResult('a', Point2(0, 0), address='A'),
                      GeocoderResult('not_selected', Point2(0, 0),
                                     address='A')]
            }))) == ['a']
