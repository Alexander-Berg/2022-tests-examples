# -*- coding: utf-8 -*-
from django.test import TestCase
from freezegun import freeze_time

from events.accounts.helpers import YandexClient
from events.common_app.factories import UserMessageFactory
from events.media.factories import ImageFactory
from events.media.utils import get_image_links


@freeze_time('2020-08-15 09:18')
class TestUserMessageView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex()

    def test_should_return_active_messages(self):
        image = ImageFactory()
        translations = {
            'label': {
                'ru': 'ru_label',
                'en': 'en_label',
            }
        }
        message = UserMessageFactory(
            enabled=True,
            slug='test1',
            label='label',
            show_count=7,
            image=image,
            translations=translations,
        )

        response = self.client.get('/admin/api/v2/messages/')

        self.assertEqual(response.status_code, 200)
        results = response.data['results']
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]['id'], message.pk)
        self.assertEqual(results[0]['slug'], 'test1')
        self.assertEqual(results[0]['label'], 'ru_label')
        self.assertEqual(results[0]['show_count'], 7)
        self.assertIsNotNone(results[0]['image'])
        self.assertDictEqual(results[0]['image']['links'], get_image_links(image))

    def test_should_return_active_messages__date_started(self):
        message01 = UserMessageFactory(enabled=True, date_started='2020-08-01')
        UserMessageFactory(enabled=True, date_started='2020-08-30')

        response = self.client.get('/admin/api/v2/messages/')

        self.assertEqual(response.status_code, 200)
        results = response.data['results']
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]['id'], message01.pk)

    def test_should_return_active_messages__date_finished(self):
        UserMessageFactory(enabled=True, date_finished='2020-08-01')
        message30 = UserMessageFactory(enabled=True, date_finished='2020-08-30')

        response = self.client.get('/admin/api/v2/messages/')

        self.assertEqual(response.status_code, 200)
        results = response.data['results']
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]['id'], message30.pk)

    def test_should_return_active_messages__date_started__date_finished(self):
        message01_20 = UserMessageFactory(enabled=True, date_started='2020-08-01', date_finished='2020-08-20')
        UserMessageFactory(enabled=True, date_started='2020-08-20', date_finished='2020-08-30')

        response = self.client.get('/admin/api/v2/messages/')

        self.assertEqual(response.status_code, 200)
        results = response.data['results']
        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]['id'], message01_20.pk)

    def test_shouldnt_return_any_messages(self):
        UserMessageFactory(enabled=False)
        UserMessageFactory(enabled=False, date_started='2020-08-15')
        UserMessageFactory(enabled=False, date_finished='2020-08-15')
        UserMessageFactory(enabled=False, date_started='2020-08-01', date_finished='2020-08-30')

        response = self.client.get('/admin/api/v2/messages/')

        self.assertEqual(response.status_code, 200)
        results = response.data['results']
        self.assertEqual(len(results), 0)
