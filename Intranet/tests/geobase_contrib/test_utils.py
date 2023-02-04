# -*- coding: utf-8 -*-
from django.test import TestCase

from events.geobase_contrib.factories import CityFactory, CountryFactory
from events.geobase_contrib.models import City, Country
from events.geobase_contrib.utils import GeobaseSync


class TestGeobaseSync(TestCase):
    def test_should_sync_countries(self):
        CountryFactory(geobase_id=225, name='russia')
        CountryFactory(geobase_id=84, name='usa')

        countries = [{
            'id': '84',
            'name': 'США',
            'full_name': 'США',
            'translations': {
                'name': {
                    'ru': 'США',
                    'en': 'USA',
                },
                'full_name': {
                    'ru': 'США',
                    'en': 'USA',
                },
            },
        }, {
            'id': '93',
            'name': 'Аргентина',
            'full_name': 'Аргентина',
            'translations': {
                'name': {
                    'ru': 'Аргентина',
                    'en': 'Argentina',
                },
                'full_name': {
                    'ru': 'Аргентина',
                    'en': 'Argentina',
                },
            },
        }]

        sync = GeobaseSync()
        insert_count, update_count = sync.sync_countries(countries)
        self.assertEqual(insert_count, 1)
        self.assertEqual(update_count, 1)

        insert_count, update_count = sync.sync_countries(countries)
        self.assertEqual(insert_count, 0)
        self.assertEqual(update_count, 0)

        db_countries = {
            it.geobase_id: it
            for it in Country.objects.all()
        }
        self.assertEqual(len(db_countries), 3)

        russia = db_countries[225]
        self.assertEqual(russia.name, 'russia')
        self.assertEqual(russia.full_name, '')

        usa = db_countries[84]
        self.assertEqual(usa.name, 'США')
        self.assertEqual(usa.full_name, 'США')

        argentina = db_countries[93]
        self.assertEqual(argentina.name, 'Аргентина')
        self.assertEqual(argentina.full_name, 'Аргентина')

    def test_should_sync_cities(self):
        russia = CountryFactory(geobase_id=225, name='russia')
        CityFactory(geobase_id='213', name='moscow', country=russia)
        CityFactory(geobase_id='2', name='spb', country=russia)

        cities = [{
            'id': '2',
            'name': 'Санкт-Петербург',
            'full_name': 'Санкт-Петербург, Россия',
            'country': {
                'id': '225',
            },
            'translations': {
                'name': {
                    'ru': 'Санкт-Петербург',
                    'en': 'Saint Petersburg',
                },
                'full_name': {
                    'ru': 'Санкт-Петербург, Россия',
                    'en': 'Saint Petersburg, Russia',
                },
            },
            'population': 5000000,
        }, {
            'id': '22',
            'name': 'Калининград',
            'full_name': 'Калининград, Россия',
            'country': {
                'id': '225',
            },
            'translations': {
                'name': {
                    'ru': 'Калининград',
                    'en': 'Kaliningrad',
                },
                'full_name': {
                    'ru': 'Калининград, Россия',
                    'en': 'Kaliningrad, Russia',
                },
            },
            'population': 1000000,
        }]

        sync = GeobaseSync()
        insert_count, update_count = sync.sync_cities(cities)
        self.assertEqual(insert_count, 1)
        self.assertEqual(update_count, 1)

        insert_count, update_count = sync.sync_cities(cities)
        self.assertEqual(insert_count, 0)
        self.assertEqual(update_count, 0)

        db_cities = {
            it.geobase_id: it
            for it in City.objects.all()
        }
        self.assertEqual(len(db_cities), 3)

        moscow = db_cities[213]
        self.assertEqual(moscow.name, 'moscow')
        self.assertEqual(moscow.full_name, '')
        self.assertEqual(moscow.country_id, russia.pk)
        self.assertEqual(moscow.profiles_count, 0)

        spb = db_cities[2]
        self.assertEqual(spb.name, 'Санкт-Петербург')
        self.assertEqual(spb.full_name, 'Санкт-Петербург, Россия')
        self.assertEqual(spb.country_id, russia.pk)
        self.assertEqual(spb.profiles_count, 5000000)

        kaliningrad = db_cities[22]
        self.assertEqual(kaliningrad.name, 'Калининград')
        self.assertEqual(kaliningrad.full_name, 'Калининград, Россия')
        self.assertEqual(kaliningrad.country_id, russia.pk)
        self.assertEqual(kaliningrad.profiles_count, 1000000)
