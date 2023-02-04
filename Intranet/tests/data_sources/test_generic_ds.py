# -*- coding: utf-8 -*-
import responses

from django.test import TestCase
from django.utils.translation import override

from events.accounts.helpers import YandexClient
from events.data_sources.sources import CountryDataSource, CityDataSource, AddressDataSource
from events.geobase_contrib.factories import CountryFactory, CityFactory
from events.common_app.helpers import MockRequest


class TestCountryDataSource(TestCase):
    def setUp(self):
        self.countries = {
            'russia': CountryFactory(
                name='Россия',
                full_name='Россия, Евразия',
                translations={
                    'name': {
                        'ru': 'Россия',
                        'en': 'Russia',
                    },
                    'full_name': {
                        'ru': 'Россия, Евразия',
                        'en': 'Russia, Eurasia',
                    },
                },
            ),
            'usa': CountryFactory(
                name='США',
                full_name='США, Северная Америка',
                translations={
                    'name': {
                        'ru': 'США',
                        'en': 'USA',
                    },
                    'full_name': {
                        'ru': 'США, Северная Америка',
                        'en': 'USA, North America',
                    },
                },
            ),
            'china': CountryFactory(
                name='Китай',
                full_name='Китай, Азия',
                translations={
                    'name': {
                        'ru': 'Китай',
                        'en': 'China',
                    },
                    'full_name': {
                        'ru': 'Китай, Азия',
                        'en': 'China, Asia',
                    },
                },
            ),
        }

    def test_queryset_should_return_all_countries(self):
        self.assertEqual(CountryDataSource().get_filtered_queryset().count(), 0)

    def test_suggest_filter_ru(self):
        filter_data = {'suggest': 'Рос'}
        with override('ru'):
            response = CountryDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.countries['russia'].pk)

    def test_suggest_filter_en(self):
        filter_data = {'suggest': 'US'}
        with override('en'):
            response = CountryDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.countries['usa'].pk)

    def test_suggest_filter_de(self):
        filter_data = {'suggest': 'Chi'}
        with override('de'):
            response = CountryDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.countries['china'].pk)

    def test_should_filter_by_ids(self):
        filter_data = {'id': [self.countries['russia'].id]}
        response = CountryDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.countries['russia'])

    def test_should_filter_by_text(self):
        filter_data = {'text': self.countries['russia'].full_name}
        response = CountryDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.countries['russia'])

    def test_serializer(self):
        countries = [
            self.countries['china'],
            self.countries['usa'],
        ]
        serializer = CountryDataSource.serializer_class(countries, many=True)
        expected = [
            {
                'id': str(countries[0].pk),
                'text': countries[0].full_name,
            },
            {
                'id': str(countries[1].pk),
                'text': countries[1].full_name,
            },
        ]
        self.assertListEqual(serializer.data, expected)


class TestCityDataSource(TestCase):
    def setUp(self):
        self.countries = {
            'russia': CountryFactory(
                name='Россия',
                full_name='Россия, Евразия',
                translations={
                    'name': {
                        'ru': 'Россия',
                        'en': 'Russia',
                    },
                    'full_name': {
                        'ru': 'Россия, Евразия',
                        'en': 'Russia, Eurasia',
                    },
                },
            ),
            'usa': CountryFactory(
                name='США',
                full_name='США, Северная Америка',
                translations={
                    'name': {
                        'ru': 'США',
                        'en': 'USA',
                    },
                    'full_name': {
                        'ru': 'США, Северная Америка',
                        'en': 'USA, North America',
                    },
                },
            ),
            'china': CountryFactory(
                name='Китай',
                full_name='Китай, Азия',
                translations={
                    'name': {
                        'ru': 'Китай',
                        'en': 'China',
                    },
                    'full_name': {
                        'ru': 'Китай, Азия',
                        'en': 'China, Asia',
                    },
                },
            ),
        }

        self.cities = {
            'moscow': CityFactory(
                name='Москва',
                full_name='Москва, Москва и Московская область, Россия',
                translations={
                    'name': {
                        'ru': 'Москва',
                        'en': 'Moscow',
                    },
                    'full_name': {
                        'ru': 'Москва, Москва и Московская область, Россия',
                        'en': 'Moscow, Moscow and Moscow oblast, Russia',
                    },
                },
                country=self.countries['russia'],
            ),
            'washington': CityFactory(
                name='Вашингтон',
                full_name='Вашингтон, Округ Колумбия, США',
                translations={
                    'name': {
                        'ru': 'Вашингтон',
                        'en': 'Washington',
                    },
                    'full_name': {
                        'ru': 'Вашингтон, Округ Колумбия, США',
                        'en': 'Washington, District of Columbia, United States',
                    },
                },
                country=self.countries['usa'],
            ),
            'beijing': CityFactory(
                name='Пекин',
                full_name='Пекин, Китай',
                translations={
                    'name': {
                        'ru': 'Пекин',
                        'en': 'Beijing',
                    },
                    'full_name': {
                        'ru': 'Пекин, Китай',
                        'en': 'Beijing, China',
                    },
                },
                country=self.countries['china'],

            ),
        }

    def test_queryset_should_return_no_cities_without_suggest(self):
        self.assertEqual(CityDataSource().get_filtered_queryset().count(), 0)

    def test_suggest_filter_ru(self):
        filter_data = {'suggest': 'Мос'}
        with override('ru'):
            response = CityDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.cities['moscow'].pk)

    def test_suggest_filter_en(self):
        filter_data = {'suggest': 'Bei'}
        with override('en'):
            response = CityDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.cities['beijing'].pk)

    def test_suggest_filter_de(self):
        filter_data = {'suggest': 'Was'}
        with override('de'):
            response = CityDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.cities['washington'].pk)

    def test_country_filter(self):
        filter_data={'country': str(self.countries['russia'].pk)}
        response = CityDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0].pk, self.cities['moscow'].pk)

    def test_should_filter_by_ids(self):
        filter_data = {'id': [self.cities['moscow'].id]}
        response = CityDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.cities['moscow'])

    def test_should_filter_by_text_ru(self):
        filter_data = {'text': 'Москва, Москва и Московская область, Россия'}
        with override('ru'):
            response = CityDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.cities['moscow'])

    def test_should_filter_by_text_en(self):
        filter_data = {'text': 'Washington, District of Columbia, United States'}
        with override('en'):
            response = CityDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.cities['washington'])

    def test_should_filter_by_text_de(self):
        filter_data = {'text': 'Beijing, China'}
        with override('en'):
            response = CityDataSource().get_filtered_queryset(filter_data)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.cities['beijing'])

    def test_serializer(self):
        cities = [
            self.cities['washington'],
            self.cities['moscow'],
        ]
        serializer = CityDataSource.serializer_class(cities, many=True)
        expected = [
            {
                'id': str(cities[0].pk),
                'text': cities[0].full_name,
            },
            {
                'id': str(cities[1].pk),
                'text': cities[1].full_name,
            },
        ]
        self.assertListEqual(serializer.data, expected)


class TestAddressSource(TestCase):

    def setUp(self):
        self.filter_data = {
            'suggest': u'академика капицы 26',
        }
        self.result = [
            u'улица Академика Капицы, 26к1, Москва, Россия',
            u'улица Академика Капицы, 26к2, Москва, Россия',
            u'улица Академика Капицы, 26к2с1, Москва, Россия',
            u'улица Академика Капицы, 26к3, Москва, Россия',
            u'улица Академика Капицы, 26к3с1, Москва, Россия',
        ]
        self.request = MockRequest()
        self.request.query_params.update(self.filter_data)

    def register_uri(self, data=None, status=200):
        responses.add(
            responses.GET,
            'https://suggest-maps.yandex.ru/suggest-geo-mobile',
            body=data or '',
            content_type='text/xml',
            status=status,
        )

    def _get_data(self):
        return '''
            <suggest>
                <item type="address" lat="55.6260" lon="37.5212">
                    <display>
                        <t>улица Академика Капицы, 26к1, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.6266" lon="37.5219">
                    <display>
                        <t>улица Академика Капицы, 26к2, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.6268" lon="37.5214">
                    <display>
                        <t>улица Академика Капицы, 26к2с1, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.6274" lon="37.5220">
                    <display>
                        <t>улица Академика Капицы, 26к3, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.6269" lon="37.5229">
                    <display>
                        <t>улица Академика Капицы, 26к3с1, Москва, Россия</t>
                    </display>
                </item>
            </suggest>
        '''

    @responses.activate
    def test_get_data_success(self):
        self.register_uri(data=self._get_data())
        data_source = AddressDataSource(request=self.request)
        result = data_source.get_filtered_queryset(filter_data=self.filter_data)
        expected = [
            {'id': item, 'name': item}
            for item in self.result
        ]
        self.assertEqual(result, expected)

    @responses.activate
    def test_get_data_fail(self):
        self.register_uri(status=400)
        data_source = AddressDataSource()
        result = data_source.get_filtered_queryset(filter_data=self.filter_data)
        self.assertEqual(result, [])


class TestAddressDataSourceViewSet(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        super().setUp()
        self.profile = self.client.login_yandex(is_superuser=True)
        self.base_url = '/v1/data-source/address/?suggest=фрунзе'

    def register_uri(self, data=None, status=200):
        responses.add(
            responses.GET,
            'https://suggest-maps.yandex.ru/suggest-geo-mobile',
            body=data or '',
            content_type='text/xml',
            status=status,
        )

    def _get_data(self):
        return '''
            <suggest>
                <item type="address" lat="55.7236" lon="37.5879">
                    <display>
                        <t>Фрунзенская набережная, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.7343" lon="37.5893">
                    <display>
                        <t>улица Тимура Фрунзе, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.7344" lon="37.5895">
                    <display>
                        <t>улица Тимура Фрунзе, 16, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.7217" lon="37.5770">
                    <display>
                        <t>3-я Фрунзенская улица, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.7246" lon="37.5847">
                    <display>
                        <t>Фрунзенская набережная, 30, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.7231" lon="37.5822">
                    <display>
                        <t>2-я Фрунзенская улица, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.0366" lon="82.9167"> <display>
                        <t>улица Фрунзе, 5, Новосибирск, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.7212" lon="37.5768">
                    <display>
                        <t>3-я Фрунзенская улица, 9, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.7233" lon="37.5796">
                    <display>
                        <t>2-я Фрунзенская улица, 9, Москва, Россия</t>
                    </display>
                </item>
                <item type="address" lat="55.0384" lon="82.9386">
                    <display>
                        <t>улица Фрунзе, Новосибирск, Россия</t>
                    </display>
                </item>
            </suggest>
        '''

    @responses.activate
    def test_without_filters_should_return_number_of_items(self):
        self.register_uri(data=self._get_data())
        response = self.client.get(self.base_url)
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        self.assertEqual(len(data), 10)
        for text in data.values():
            self.assertIn('фрунзе', text.lower())

    @responses.activate
    def test_with_filter_in_should_return_number_one_item(self):
        self.register_uri(data=self._get_data())
        response = self.client.get('%s&id=Фрунзенская набережная, Москва, Россия' % self.base_url)
        self.assertEqual(response.status_code, 200)
        data = {
            it['id']: it['text']
            for it in response.data['results']
        }
        expected = {
            'Фрунзенская набережная, Москва, Россия': 'Фрунзенская набережная, Москва, Россия',
        }
        self.assertEqual(len(data), len(expected))
        self.assertDictEqual(data, expected)
