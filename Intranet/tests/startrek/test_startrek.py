# -*- coding: utf-8 -*-
import responses

from datetime import date, datetime
from django.test import TestCase
from django.utils.translation import override
from unittest.mock import Mock, patch

from events.abc.factories import AbcServiceFactory
from events.accounts.helpers import YandexClient
from events.common_app.helpers import override_cache_settings
from events.staff.factories import StaffGroupFactory, StaffOfficeFactory
from events.startrek.fields import (
    ArrayField,
    BaseField,
    BoardField,
    DateField,
    DateTimeField,
    FloatField,
    GroupField,
    IntegerField,
    MoneyField,
    OfficeField,
    ServiceField,
    StringField,
    TextField,
    TimeTrackingField,
    TrackerFieldError,
    UriField,
    UserField,
)


class TestBaseField(TestCase):
    def test_get(self):
        field = BaseField()
        with self.assertRaises(TrackerFieldError):
            field.get(object(), 'something')


class TestStringField(TestCase):
    def test_get(self):
        field = StringField()
        self.assertEqual('foo bar', field.get(object(), 'foo bar'))


class TestTextField(TestCase):
    def test_get(self):
        field = TextField()
        self.assertEqual('bar baz', field.get(object(), 'bar baz'))


class TestArrayStringField(TestCase):
    def test_get(self):
        field = ArrayField(StringField())
        self.assertEqual(['foo', 'bar'], field.get(object(), ['foo', 'bar']))
        self.assertEqual(['foo bar'], field.get(object(), ['foo bar']))
        self.assertEqual(['foo bar'], field.get(object(), 'foo bar'))


class TestArrayUserField(TestCase):
    def test_get(self):
        field = ArrayField(UserField())
        self.assertEqual(['kdunaev', 'chapson'], field.get(object(), ['kdunaev, chapson']))
        self.assertEqual(['kdunaev', 'chapson'], field.get(object(), 'kdunaev, chapson'))
        self.assertEqual(['kdunaev'], field.get(object(), ['kdunaev']))
        self.assertEqual(['kdunaev'], field.get(object(), 'kdunaev'))
        self.assertEqual(['kdunaev', 'chapson'], field.get(object(), ' , kdunaev, chapson'))


class TestArrayBoardField(TestCase):
    def test_get(self):
        field = ArrayField(BoardField())
        self.assertEqual([2531, 2532], field.get(object(), ['2531', '2532']))
        self.assertEqual([2531, 2532], field.get(object(), [2531, 2532]))
        self.assertEqual([2531], field.get(object(), '2531'))
        self.assertEqual([2531], field.get(object(), 2531))


class TestIntegerField(TestCase):
    def test_get(self):
        field = IntegerField()
        self.assertEqual(42, field.get(object(), 42))
        self.assertEqual(15, field.get(object(), 15.95))
        self.assertEqual(42, field.get(object(), '42'))
        with self.assertRaises(TrackerFieldError):
            field.get(object(), '15.85')
        with self.assertRaises(TrackerFieldError):
            field.get(object(), 'fourty two')


class TestFloatField(TestCase):
    def test_get(self):
        field = FloatField()
        self.assertEqual(42, field.get(object(), 42))
        self.assertEqual(15.95, field.get(object(), 15.95))
        self.assertEqual(42.0, field.get(object(), '42'))
        self.assertEqual(15.95, field.get(object(), '15.95'))
        self.assertEqual(15.95, field.get(object(), '15,95'))
        with self.assertRaises(TrackerFieldError):
            field.get(object(), 'fifteen ninty five')


class TestMoneyField(TestCase):
    def test_get(self):
        field = MoneyField()
        self.assertEqual(42, field.get(object(), 42))
        self.assertEqual(15.95, field.get(object(), 15.95))
        self.assertEqual({'amount': 42}, field.get(object(), '42'))
        self.assertEqual({'amount': 15.95}, field.get(object(), '15.95'))
        self.assertEqual({'amount': 42, 'unit': 'USD'}, field.get(object(), '42 USD'))
        self.assertEqual({'amount': 15.95, 'unit': 'USD'}, field.get(object(), '15.95 USD'))
        self.assertEqual({'amount': 42, 'unit': 'TRY'}, field.get(object(), '42 TRY'))
        self.assertEqual({'amount': 15.95, 'unit': 'TRY'}, field.get(object(), '15.95 TRY'))
        with self.assertRaises(TrackerFieldError):
            field.get(object(), 'UAH')


class TestDateField(TestCase):
    def test_get(self):
        field = DateField()
        self.assertEqual(date.today().isoformat(), field.get(object(), date.today()))
        self.assertEqual(date.today().isoformat(), field.get(object(), datetime.now()))
        self.assertEqual('2016-08-29', field.get(object(), '2016-08-29'))
        self.assertEqual('2016-08-29', field.get(object(), '29.08.2016'))
        with self.assertRaises(TrackerFieldError):
            field.get(object(), '28th of august')


class TestDateTimeField(TestCase):
    def test_get(self):
        field = DateTimeField()
        self.assertTrue(isinstance(field.get(object(), datetime.now()), str))
        self.assertTrue(isinstance(field.get(object(), date.today()), str))
        self.assertEqual('2016-08-29', field.get(object(), '2016-08-29'))
        self.assertEqual('2016-08-29T11:39:42', field.get(object(), '2016-08-29T11:39:42'))
        self.assertEqual('2016-08-29T11:39:42.1234', field.get(object(), '2016-08-29T11:39:42.1234'))
        self.assertEqual('2016-08-29T11:39:42+03:00', field.get(object(), '2016-08-29T11:39:42+03:00'))
        self.assertEqual('2016-08-29T11:39:42.1234+03:00', field.get(object(), '2016-08-29T11:39:42.1234+03:00'))
        self.assertEqual('2016-08-29T11:39:42.1234+0300', field.get(object(), '2016-08-29T11:39:42.1234+0300'))
        self.assertEqual('2016-08-29T11:39:42Z', field.get(object(), '2016-08-29T11:39:42Z'))
        self.assertEqual('2016-08-29T11:39:42.1234Z', field.get(object(), '2016-08-29T11:39:42.1234Z'))
        self.assertTrue(field.localize(datetime.now()).tzinfo)
        field.localize = Mock(return_value=datetime.now())
        field.get(object(), datetime.now())
        field.localize.assert_called_once()


class TestTimeTrackingField(TestCase):
    def test_get(self):
        field = TimeTrackingField()
        self.assertEqual('P1W', field.get(object(), '1w'))
        self.assertEqual('P1W', field.get(object(), '1W'))
        self.assertEqual('P1D', field.get(object(), '1d'))
        self.assertEqual('P1D', field.get(object(), '1D'))
        self.assertEqual('PT1H', field.get(object(), '1h'))
        self.assertEqual('PT1H', field.get(object(), '1H'))
        self.assertEqual('PT1M', field.get(object(), '1m'))
        self.assertEqual('PT1M', field.get(object(), '1M'))
        self.assertEqual('PT1S', field.get(object(), '1s'))
        self.assertEqual('PT1S', field.get(object(), '1S'))
        self.assertEqual('P2W6D', field.get(object(), '2w 6d'))
        self.assertEqual('P2W6D', field.get(object(), '2W 6D'))
        self.assertEqual('PT12H30M', field.get(object(), '12h 30m'))
        self.assertEqual('PT12H30M', field.get(object(), '12H 30M'))
        self.assertEqual('P2W6DT12H30M', field.get(object(), '2w 6d 12h 30m'))
        self.assertEqual('P2W6DT12H30M', field.get(object(), '2W 6D 12H 30M'))
        self.assertEqual('PT12H30M', field.get(object(), '12h 30m'))
        self.assertEqual('PT12H30M', field.get(object(), '12H 30M'))
        self.assertEqual('P999W999DT999H999M999S', field.get(object(), '999w 999d 999h 999m 999s'))
        self.assertEqual('P999W999DT999H999M999S', field.get(object(), '999W 999D 999H 999M 999S'))
        self.assertEqual('PT999H999M999S', field.get(object(), '999h 999m 999s'))
        self.assertEqual('PT999H999M999S', field.get(object(), '999H 999M 999S'))
        field.to_startrek = Mock(return_value=None)
        with self.assertRaises(TrackerFieldError):
            field.get(object(), 'not a timetracking range')
        field.to_startrek.assert_not_called()


class TestUriField(TestCase):
    def test_get(self):
        field = UriField()
        self.assertEqual('http://example.com', field.get(object(), 'http://example.com'))
        self.assertEqual('example.com', field.get(object(), 'example.com'))


class TestUserField(TestCase):
    def test_get(self):
        field = UserField()
        self.assertEqual('kdunaev', field.get(object(), 'kdunaev'))
        self.assertEqual(['kdunaev', 'chapson'], field.get(object(), 'kdunaev, chapson'))
        self.assertEqual(['kdunaev', 'chapson'], field.get(object(), ['kdunaev', 'chapson']))


class TestTranslationField(TestCase):
    def test_get(self):
        field = UriField()
        self.assertEqual('translating text', field.get(object(), 'translating text'))


class TestStartrekQueueView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient
    url = '/admin/api/v2/startrek-queues/{queue_name}/'

    def setUp(self):
        self.profile = self.client.login_yandex()
        self.profile.is_staff = True
        self.profile.save()

    def get_queue(self):
        return {
            'id': 1,
            'key': 'FORMS',
            'name': 'FORMS',
            'description': '',
            'defaultPriority': {
                'id': 2, 'key': 'now', 'display': 'Now',
            },
            'defaultType': {
                'id': 3, 'key': 'three', 'display': 'Three',
            },
        }

    def get_issuetypes(self):
        return [
            {
                'id': 1, 'key': 'one', 'name': 'One',
            },
            {
                'id': 2, 'key': 'two', 'name': 'Two',
            },
            {
                'id': 3, 'key': 'three', 'name': 'Three',
            },
        ]

    def get_priorities(self):
        return [
            {
                'id': 1, 'key': 'tomorrow', 'name': 'Tomorrow',
            },
            {
                'id': 2, 'key': 'now', 'name': 'Now',
            },
        ]

    @override_cache_settings()
    def test_retrieve_ok(self):
        url = self.url.format(queue_name='FORMS')
        with patch('events.common_app.startrek.client.StartrekClient.get_queue') as mock_queue:
            with patch('events.common_app.startrek.client.StartrekClient.get_priorities') as mock_priorities:
                with patch('events.common_app.startrek.client.StartrekClient.get_issuetypes') as mock_issuetypes:
                    mock_queue.return_value = self.get_queue()
                    mock_priorities.return_value = self.get_priorities()
                    mock_issuetypes.return_value = self.get_issuetypes()
                    response = self.client.get(url)

        self.assertEqual(200, response.status_code)
        data = response.json()
        self.assertEqual(1, data['id'])
        self.assertEqual('FORMS', data['key'])
        self.assertEqual(2, data['default_priority']['id'])
        self.assertEqual('now', data['default_priority']['key'])
        self.assertEqual(3, data['default_issuetype']['id'])
        self.assertEqual('three', data['default_issuetype']['key'])
        self.assertEqual(2, len(data['priorities']))
        self.assertEqual(3, len(data['issuetypes']))
        self.assertEqual(0, len(data['components']))

    def test_retrieve_error(self):
        url = self.url.format(queue_name='NOT-A-QUEUE')
        with patch('events.common_app.startrek.client.StartrekClient.get_queue') as mock_queue:
            mock_queue.return_value = None
            response = self.client.get(url)

        self.assertEqual(404, response.status_code)


class TestStartrekFieldsSuggest(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    @override_cache_settings()
    def test_should_return_empty_list_without_search_param(self):
        with patch('events.common_app.startrek.client.StartrekClient') as mock_instance:
            response = self.client.get('/admin/api/v2/startrek-fields/suggest/')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data, [])
        mock_instance.assert_not_called()

    def register_uri(self, language='ru'):
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/fields/',
            json=self._get_fields(language),
        )

    def _get_fields(self, language):
        return [
            {
                'id': 'queue',
                'name': 'Queue' if language == 'en' else 'Очередь',
                'key': 'queue',
                'schema': {
                    'type': 'queue',
                    'required': True
                },
            },
            {
                'id': 'tags',
                'name': 'Tags' if language == 'en' else 'Теги',
                'key': 'tags',
                'schema': {
                    'type': 'array',
                    'items': 'string'
                },
            },
            {
                'id': 'technicalLabourCost',
                'name': 'Technical labour cost' if language == 'en' else 'Технические трудозатраты',
                'key': 'technicalLabourCost',
                'schema': {
                    'type': 'timetracking',
                    'required': False
                },
            },
        ]

    @responses.activate
    def test_should_return_list_of_fields(self):
        self.register_uri()
        url = '/admin/api/v2/startrek-fields/suggest/?search=%s'
        response = self.client.get(url % 'tag')
        self.assertEqual(response.status_code, 200)
        self.assertIn('tags', set(it['slug'] for it in response.data))
        self.assertEqual(len(responses.calls), 1)

        response = self.client.get(url % 'тег')
        self.assertEqual(response.status_code, 200)
        self.assertIn('tags', set(it['slug'] for it in response.data))
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_should_return_empty_list_for_not_existing_field(self):
        self.register_uri()
        url = '/admin/api/v2/startrek-fields/suggest/?search=%s'
        response = self.client.get(url % 'notexistingfieldslug')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data, [])
        self.assertEqual(len(responses.calls), 1)

        response = self.client.get(url % 'имянесуществующегополя')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data, [])
        self.assertEqual(len(responses.calls), 1)

    @override_cache_settings()
    @responses.activate
    def test_should_get_default_lang_correct(self):
        self.register_uri()
        url = '/admin/api/v2/startrek-fields/suggest/?search=%s'
        response = self.client.get(url % 'technical')
        self.assertEqual(response.status_code, 200)
        item_data = None
        for item in response.data:
            if item['slug'] == 'technicalLabourCost':
                item_data = item
                break
        self.assertEqual(item_data['name'], 'Технические трудозатраты')

    @override_cache_settings()
    @responses.activate
    def test_should_get_eng_lang_correct(self):
        self.register_uri('en')
        url = '/admin/api/v2/startrek-fields/suggest/?search=%s&lang=en'
        response = self.client.get(url % 'technical')
        self.assertEqual(response.status_code, 200)
        item_data = None
        for item in response.data:
            if item['slug'] == 'technicalLabourCost':
                item_data = item
                break
        self.assertEqual(item_data['name'], 'Technical labour cost')

        url = '/admin/api/v2/startrek-fields/suggest/?search=%s'
        response = self.client.get(url % 'technical', **{'HTTP_ACCEPT_LANGUAGE': 'en'})
        self.assertEqual(response.status_code, 200)
        for item in response.data:
            if item['slug'] == 'technicalLabourCost':
                item_data = item
                break
        self.assertEqual(item_data['name'], 'Technical labour cost')


class TestGroupField(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.groups = [
            StaffGroupFactory(staff_id=1, name='Forms Developers'),
            StaffGroupFactory(staff_id=2, name='Forms Testers'),
        ]

    def test_get(self):
        field = GroupField()
        self.assertEqual({'id': self.groups[0].staff_id}, field.get(object(), 'Forms Developers'))
        self.assertEqual({'id': self.groups[1].staff_id}, field.get(object(), 'Forms Testers'))
        self.assertEqual({'id': 3}, field.get(object(), '3'))
        self.assertIsNone(field.get(object(), 'Not a group'))


class TestServiceField(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.services = [
            AbcServiceFactory(abc_id=111, name='Конструктор форм', translations={
                'name': {
                    'ru': 'Конструктор форм',
                    'en': 'Forms',
                },
            }),
            AbcServiceFactory(abc_id=222, name='Tools and B2b', translations={
                'name': {
                    'ru': 'Tools and B2b',
                },
            }),
            AbcServiceFactory(abc_id=11, name='Конструктор форм', is_deleted=True, translations={
                'name': {
                    'ru': 'Конструктор форм',
                    'en': 'Forms',
                },
            }),
        ]

    def test_get(self):
        field = ServiceField()
        self.assertEqual({'id': self.services[0].abc_id}, field.get(object(), 'Конструктор форм'))
        with override('en'):
            self.assertEqual({'id': self.services[0].abc_id}, field.get(object(), 'Forms'))
        self.assertEqual({'id': self.services[1].abc_id}, field.get(object(), 'Tools and B2b'))
        self.assertEqual({'id': 3}, field.get(object(), '3'))
        self.assertIsNone(field.get(object(), 'Not a service'))


class TestOfficeField(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.morozov = StaffOfficeFactory(
            name='Морозов',
            translations={
                'name': {
                    'ru': 'Морозов',
                    'en': 'Morozov',
                },
            },
        )
        self.passage = StaffOfficeFactory(
            name='Морозовский пассаж',
            translations={
                'name': {
                    'ru': 'Морозовский пассаж',
                },
            },
        )

    def get_data(self, name):
        return OfficeField().get(object(), name)

    def test_get(self):
        with override('ru'):
            self.assertDictEqual(self.get_data('Морозов'), {
                'id': self.morozov.staff_id,
            })
            self.assertDictEqual(self.get_data('Морозовский пассаж'), {
                'id': self.passage.staff_id,
            })
        with override('en'):
            self.assertDictEqual(self.get_data('Morozov'), {
                'id': self.morozov.staff_id,
            })
        self.assertIsNone(self.get_data('Not an office'))
