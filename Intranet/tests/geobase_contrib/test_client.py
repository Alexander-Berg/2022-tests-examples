# -*- coding: utf-8 -*-
import responses

from django.test import TestCase
from requests.exceptions import HTTPError

from events.geobase_contrib.client import GeobaseClient, GEOBASE_URL


class TestGeobaseClient(TestCase):
    def register_uri(self, path, data, status=200):
        responses.add(responses.GET, f'{GEOBASE_URL}{path}', json=data, status=status)

    @responses.activate
    def test_should_return_region_by_id(self):
        self.register_uri('/region_by_id', {
            'id': 213,
            'type': 6,
            'parent_id': 1,
            'name': 'Москва',
            'en_name': 'Moscow',
            'population': 12000000,
        })
        client = GeobaseClient()
        region = client.get_region_by_id(213)
        self.assertEqual(region['id'], 213)
        self.assertEqual(region['type'], 6)
        self.assertEqual(region['parent_id'], 1)
        self.assertEqual(region['name'], 'Москва')
        self.assertEqual(region['en_name'], 'Moscow')
        self.assertEqual(region['population'], 12000000)

    @responses.activate
    def test_shouldnt_return_region_by_id(self):
        self.register_uri('/region_by_id', {}, status=404)
        client = GeobaseClient()
        with self.assertRaises(HTTPError):
            client.get_region_by_id(213)

    @responses.activate
    def test_should_return_region_by_ip(self):
        self.register_uri('/region_by_ip', {
            'id': 213,
            'type': 6,
            'parent_id': 1,
            'name': 'Москва',
            'en_name': 'Moscow',
            'population': 12000000,
        })
        client = GeobaseClient()
        region = client.get_region_by_ip('5.45.203.152')
        self.assertEqual(region['id'], 213)
        self.assertEqual(region['type'], 6)
        self.assertEqual(region['parent_id'], 1)
        self.assertEqual(region['name'], 'Москва')
        self.assertEqual(region['en_name'], 'Moscow')
        self.assertEqual(region['population'], 12000000)

    @responses.activate
    def test_shouldnt_return_region_by_ip(self):
        self.register_uri('/region_by_ip', {}, status=404)
        client = GeobaseClient()
        with self.assertRaises(HTTPError):
            client.get_region_by_ip('127.0.0.1')

    @responses.activate
    def test_should_return_regions_by_type(self):
        self.register_uri('/regions_by_type', [{
            'id': 213,
            'type': 6,
            'parent_id': 1,
            'name': 'Москва',
            'en_name': 'Moscow',
            'population': 12000000,
        }, {
            'id': 2,
            'type': 6,
            'parent_id': 10174,
            'name': 'Санкт-Перербург',
            'en_name': 'Saint Petersburg',
            'population': 5000000,
        }])
        client = GeobaseClient()
        regions = client.get_regions_by_type(6)
        self.assertEqual(len(regions), 2)
        self.assertEqual(regions[0]['id'], 213)
        self.assertEqual(regions[0]['type'], 6)
        self.assertEqual(regions[0]['parent_id'], 1)
        self.assertEqual(regions[0]['name'], 'Москва')
        self.assertEqual(regions[0]['en_name'], 'Moscow')
        self.assertEqual(regions[0]['population'], 12000000)
        self.assertEqual(regions[1]['id'], 2)
        self.assertEqual(regions[1]['type'], 6)
        self.assertEqual(regions[1]['parent_id'], 10174)
        self.assertEqual(regions[1]['name'], 'Санкт-Перербург')
        self.assertEqual(regions[1]['en_name'], 'Saint Petersburg')
        self.assertEqual(regions[1]['population'], 5000000)

    @responses.activate
    def test_shouldnt_return_regions_by_type(self):
        self.register_uri('/regions_by_type', {}, status=404)
        client = GeobaseClient()
        with self.assertRaises(HTTPError):
            client.get_regions_by_type(6)

    @responses.activate
    def test_should_return_geobase_data(self):
        self.register_uri('/regions_by_type', [{
            'id': 213,
            'type': 6,
            'parent_id': '1',
            'name': 'Москва',
            'en_name': 'Moscow',
            'population': 12000000,
        }, {
            'id': 2,
            'type': 6,
            'parent_id': '10174',
            'name': 'Санкт-Перербург',
            'en_name': 'Saint Petersburg',
            'population': 5000000,
        }])
        self.register_uri('/regions_by_type', [{
            'id': 225,
            'type': 3,
            'parent_id': '10001',
            'name': 'Россия',
            'en_name': 'Russia',
        }])
        client = GeobaseClient()
        geobase = client.get_geobase_data()
        self.assertEqual(len(geobase), 3)
        self.assertTrue('213' in geobase)
        self.assertTrue('2' in geobase)
        self.assertTrue('225' in geobase)

    def test_should_return_cities(self):
        geodata = {  # {{{
            '213': {
                'id': '213',
                'type': 6,
                'parent_id': '1',
                'name': 'Москва',
                'en_name': 'Moscow',
                'population': 12000000,
            },
            '1': {
                'id': '1',
                'type': 5,
                'parent_id': '3',
                'name': 'Москва и Московская область',
                'en_name': 'Moscow and Moscow Oblast',
            },
            '3': {
                'id': '3',
                'type': 4,
                'parent_id': '225',
                'name': 'Центральный федеральный округ',
                'en_name': 'Central Federal District',
            },
            '2': {
                'id': '2',
                'type': 6,
                'parent_id': '10174',
                'name': 'Санкт-Перербург',
                'en_name': 'Saint Petersburg',
                'population': 5000000,
            },
            '10174': {
                'id': '10174',
                'type': 5,
                'parent_id': '17',
                'name': 'Санкт-Петербург и Ленинградская область',
                'en_name': 'Saint-Petersburg and Leningrad Oblast',
            },
            '17': {
                'id': '17',
                'type': 4,
                'parent_id': '225',
                'name': 'Северо-Западный федеральный округ',
                'en_name': 'Northwestern Federal District',
            },
            '225': {
                'id': '225',
                'type': 3,
                'parent_id': 10001,
                'name': 'Россия',
                'en_name': 'Russia',
            },
        }  # }}}
        client = GeobaseClient()
        cities = list(client.get_cities(geodata))
        self.assertEqual(len(cities), 2)
        city_dict = {
            it['id']: it
            for it in cities
        }
        self.assertSetEqual(set(city_dict.keys()), set(['213', '2']))

        msk = city_dict['213']
        self.assertEqual(msk['id'], '213')
        self.assertEqual(msk['name'], 'Москва')
        self.assertEqual(msk['full_name'], 'Москва, Москва и Московская область, Россия')
        self.assertDictEqual(msk['translations'], {
            'name': {
                'ru': 'Москва',
                'en': 'Moscow',
            },
            'full_name': {
                'ru': 'Москва, Москва и Московская область, Россия',
                'en': 'Moscow, Moscow and Moscow Oblast, Russia',
            },
        })
        self.assertEqual(msk['country']['id'], '225')

        spb = city_dict['2']
        self.assertEqual(spb['id'], '2')
        self.assertEqual(spb['name'], 'Санкт-Перербург')
        self.assertEqual(spb['full_name'], 'Санкт-Перербург, Санкт-Петербург и Ленинградская область, Россия')
        self.assertDictEqual(spb['translations'], {
            'name': {
                'ru': 'Санкт-Перербург',
                'en': 'Saint Petersburg',
            },
            'full_name': {
                'ru': 'Санкт-Перербург, Санкт-Петербург и Ленинградская область, Россия',
                'en': 'Saint Petersburg, Saint-Petersburg and Leningrad Oblast, Russia',
            },
        })
        self.assertEqual(spb['country']['id'], '225')

    def test_should_return_countries(self):
        geodata = {  # {{{
            '213': {
                'id': '213',
                'type': 6,
                'parent_id': '1',
                'name': 'Москва',
                'en_name': 'Moscow',
                'population': 12000000,
            },
            '1': {
                'id': '1',
                'type': 5,
                'parent_id': '3',
                'name': 'Москва и Московская область',
                'en_name': 'Moscow and Moscow Oblast',
            },
            '3': {
                'id': '3',
                'type': 4,
                'parent_id': '225',
                'name': 'Центральный федеральный округ',
                'en_name': 'Central Federal District',
            },
            '2': {
                'id': '2',
                'type': 6,
                'parent_id': '10174',
                'name': 'Санкт-Перербург',
                'en_name': 'Saint Petersburg',
                'population': 5000000,
            },
            '10174': {
                'id': '10174',
                'type': 5,
                'parent_id': '17',
                'name': 'Санкт-Петербург и Ленинградская область',
                'en_name': 'Saint-Petersburg and Leningrad Oblast',
            },
            '17': {
                'id': '17',
                'type': 4,
                'parent_id': '225',
                'name': 'Северо-Западный федеральный округ',
                'en_name': 'Northwestern Federal District',
            },
            '225': {
                'id': '225',
                'type': 3,
                'parent_id': 10001,
                'name': 'Россия',
                'en_name': 'Russia',
            },
        }  # }}}
        client = GeobaseClient()
        cities = list(client.get_cities(geodata))
        countries = list(client.get_countries(cities))
        self.assertEqual(len(countries), 1)
        self.assertEqual(countries[0]['id'], '225')
        self.assertEqual(countries[0]['name'], 'Россия')
        self.assertEqual(countries[0]['full_name'], 'Россия')
        self.assertDictEqual(countries[0]['translations'], {
            'name': {
                'ru': 'Россия',
                'en': 'Russia',
            },
            'full_name': {
                'ru': 'Россия',
                'en': 'Russia',
            },
        })
