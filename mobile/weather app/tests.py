# coding=utf-8
import json
import mock
import requests_mock
from django.test import TestCase
from rest_framework import status

from yaphone.advisor.advisor.tests.views import BasicAdvisorViewTest
from yaphone.advisor.common.android_tz import AndroidTimezone
from yaphone.advisor.common.mocks.geobase import LookupMock
from yaphone.advisor.launcher.tests.base import StatelessViewMixin
from yaphone.advisor.weather.base import WeatherClientBase

WEATHER_ANSWER = u'''
{
    "info": {
        "def_pressure_pa": 997,
        "f": true,
        "url": "https://yandex.ru/pogoda/moscow?lat=55.7343247&lon=37.5883512",
        "def_pressure_mm": 748,
        "lon": 37.5883512,
        "n": true,
        "p": true,
        "tzinfo": {
            "dst": false,
            "name": "Europe/Moscow",
            "abbr": "MSK",
            "offset": 10800
        },
        "_h": false,
        "lat": 55.7343247,
        "slug": "moscow"
    },
    "geo_object": {
        "locality": {
            "id": 213,
            "name": "Москва"
        }
    },
    "yesterday": {
        "temp": 3
    },
    "now_dt": "2018-04-02T12:56:22.333Z",
    "forecasts": [
        {
            "week": 15,
            "moon_text": "decreasing-moon",
            "date_ts": 1522616400,
            "hours": [
                {
                    "wind_speed": 5.6,
                    "uv_index": 0,
                    "soil_temp": 0,
                    "hour": "0",
                    "temp": 1,
                    "prec_prob": 0,
                    "hour_ts": 1522616400,
                    "soil_moisture": 0.41,
                    "pressure_pa": 990,
                    "wind_gust": 10.8,
                    "pressure_mm": 742,
                    "wind_dir": "se",
                    "prec_period": 60,
                    "humidity": 90,
                    "prec_mm": 0,
                    "feels_like": -5,
                    "condition": "overcast",
                    "icon": "ovc"
                }
            ],
            "parts": {
                "night": {
                    "prec_mm": 0,
                    "prec_prob": 0,
                    "soil_temp": 0,
                    "_fallback_temp": false,
                    "temp_avg": 2,
                    "pressure_pa": 988,
                    "temp_min": 1,
                    "uv_index": 0,
                    "_source": "0,1,2,3,4,5",
                    "temp_max": 2,
                    "_fallback_prec": false,
                    "soil_moisture": 0.41,
                    "daytime": "n",
                    "condition": "overcast",
                    "polar": false,
                    "pressure_mm": 741,
                    "wind_dir": "se",
                    "icon": "ovc",
                    "wind_speed": 6.4,
                    "wind_gust": 11.4,
                    "humidity": 90,
                    "feels_like": -4,
                    "prec_period": 360
                }
            },
            "sunset": "19:09",
            "biomet": {
                "index": 0,
                "condition": "magnetic-field_0"
            },
            "date": "2018-04-02",
            "moon_code": 1,
            "sunrise": "05:58"
        }
    ],
    "now": 1522673782,
    "fact": {
        "wind_speed": 2,
        "uv_index": 0,
        "obs_time": 1522673292,
        "soil_temp": 0,
        "wind_gust": 11.6,
        "temp": 4,
        "polar": false,
        "season": "spring",
        "soil_moisture": 0.44,
        "daytime": "d",
        "pressure_pa": 983,
        "humidity": 66,
        "source": "station",
        "pressure_mm": 737,
        "wind_dir": "e",
        "accum_prec": {
            "1": 9.1,
            "3": 9.5,
            "7": 13.3
        },
        "feels_like": 0,
        "condition": "overcast-and-light-rain",
        "icon": "ovc_-ra"
    }
}
'''


# noinspection PyPep8Naming
class WeatherBase(object):
    def setUp(self):
        super(WeatherBase, self).setUp()
        self.weather_mock_adapter = requests_mock.Adapter()
        WeatherClientBase.http.mount('mock', self.weather_mock_adapter)
        self.weather_mock_adapter.register_uri(url='mock://weather', method='GET', text=WEATHER_ANSWER)


class WeatherForecastTest(WeatherBase, BasicAdvisorViewTest, TestCase):
    endpoint = '/api/v1/weather_forecast/'
    default_params = {
        'lat': 55.759284,
        'lon': 37.619706,
        'locale': 'ru_RU',
    }

    def test_missing_lat_lon_with_lbs(self):
        self.assertParameterNotRequired('lat')
        self.assertParameterNotRequired('lon')

    def test_missing_lat_lon_without_lbs(self):
        self.client_model.profile.lbs_info.location = None
        self.client_model.profile.save()
        params = self.default_params.copy()
        del params['lat']
        del params['lon']
        self.assertEqual(self.get(params).status_code, status.HTTP_404_NOT_FOUND)

    def test_missing_locale(self):
        self.assertParameterRequired('locale')

    def test_wrong_location_with_lbs(self):
        params = self.default_params.copy()
        params['lat'] = params['lon'] = '********'
        self.assertEqual(self.get(params).status_code, status.HTTP_200_OK)

    def test_wrong_location_without_lbs(self):
        self.client_model.profile.lbs_info.location = None
        self.client_model.profile.save()
        params = self.default_params.copy()
        params['lat'] = params['lon'] = '********'
        self.assertEqual(self.get(params).status_code, status.HTTP_404_NOT_FOUND)

    def test_valid_timezones(self):
        params = self.default_params.copy()
        valid_timezones = {
            'GMT+3': 'GMT+03:00', 'GMT 3': 'GMT+03:00', 'GMT-3': 'GMT-03:00', 'GMT-03': 'GMT-03:00',
            'GMT+03': 'GMT+03:00', 'GMT 03': 'GMT+03:00', 'GMT-03:00': 'GMT-03:00', 'GMT+0300': 'GMT+03:00',
            'GMT-300': 'GMT-03:00', 'GMT-3:00': 'GMT-03:00',
            'GMT-23:59': 'GMT-23:59', 'GMT+23:59': 'GMT+23:59', 'GMT 23:59': 'GMT+23:59',
        }
        for passing_timezone, expected_result in valid_timezones.iteritems():
            params['timezone'] = passing_timezone
            result = self.get(params)
            msg = 'testing timezone: "%s", expected result: "%s"' % (passing_timezone, expected_result)
            self.assertEqual(result.status_code, status.HTTP_200_OK, msg)
            self.assertEqual(str(AndroidTimezone(passing_timezone)), expected_result, msg)

    def test_invalid_timezones(self):
        params = self.default_params.copy()
        invalid_timezones = [
            'GMT+:00', 'GMT-:00', 'GMT :00', 'GMT+24', 'GMT-24:00', 'GMT3:00', 'GMT+3:0', 'GMT +10', 'GMT  10:00',
        ]
        for passing_timezone in invalid_timezones:
            params['timezone'] = passing_timezone
            msg = 'testing invalid timezone: "%s"' % passing_timezone
            self.assertEqual(self.get(params).status_code, status.HTTP_400_BAD_REQUEST, msg)


@mock.patch('yaphone.utils.geo.geobase_lookuper', LookupMock())
class WeatherForecastV2Test(WeatherBase, StatelessViewMixin, TestCase):
    endpoint = '/api/v2/weather_forecast/'
    default_params = {
        'lat': 55.759284,
        'lon': 37.619706,
    }

    def test_missing_lat_lon(self):
        self.assertParameterRequired('lat')
        self.assertParameterRequired('lon')

    def test_wrong_location(self):
        params = self.default_params.copy()
        params['lat'] = params['lon'] = '********'
        self.assertEqual(self.get(params).status_code, status.HTTP_400_BAD_REQUEST)

    def test_post_with_location_ok(self):
        data = {
            "location": {
                "latitude": 55.123456,
                "longitude": 37.123456
            }
        }
        response = self.client.post(self.endpoint, data=json.dumps(data), content_type='application/json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_empty_post(self):
        response = self.client.post(self.endpoint, data="{}", content_type='application/json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    @mock.patch('yaphone.utils.geo.LbsLocator.locate',
                lambda *args, **kwargs: {'type': 'cells', 'latitude': 55.1234, 'longitude': 37.5566,
                                         'precision': 100.0})
    def test_post_with_cells_ok(self):
        data = {
            "cells": [{
                "country_code": 1,
                "operator_id": 2,
                "cell_id": 3,
                "lac": 4,
                "signal_strength": 5,
                "age": 6,
            }]
        }
        response = self.client.post(self.endpoint, data=json.dumps(data), content_type='application/json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    @mock.patch('yaphone.utils.geo.LbsLocator.locate',
                lambda *args, **kwargs: {'type': 'networks', 'latitude': 55.1234, 'longitude': 37.5566,
                                         'precision': 100.0})
    def test_post_with_wifi_ok(self):
        data = {
            "wifi_networks": [{
                "mac": "12-34-56-78-9A-BC",
                "signal_strength": -2,
                "age": 3,
            }]
        }
        response = self.client.post(self.endpoint, data=json.dumps(data), content_type='application/json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    @mock.patch('yaphone.utils.geo.LbsLocator.locate',
                lambda *args, **kwargs: {'type': 'ip', 'latitude': 55.1234, 'longitude': 37.5566, 'precision': 100.0})
    def test_post_with_ip_404(self):
        data = {
            "cells": [{
                "country_code": 1,
                "operator_id": 2,
                "cell_id": 3,
                "lac": 4,
                "signal_strength": 5,
                "age": 6,
            }]
        }
        response = self.client.post(self.endpoint, data=json.dumps(data), content_type='application/json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    @mock.patch('yaphone.utils.geo.LbsLocator.locate', lambda *args, **kwargs: None)
    def test_post_location_not_found(self):
        data = {
            "cells": [{
                "country_code": 1,
                "operator_id": 2,
                "cell_id": 3,
                "lac": 4,
                "signal_strength": 5,
                "age": 6,
            }]
        }
        response = self.client.post(self.endpoint, data=json.dumps(data), content_type='application/json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
