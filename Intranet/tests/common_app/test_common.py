# -*- coding: utf-8 -*-
import json

from django.test import TestCase

from events.accounts.helpers import YandexClient
from events.common_app.utils import get_progress
from events.common_app.models import UserMessage


class TestRobotsTxt(TestCase):
    def test_response(self):
        response = self.client.get('/robots.txt', follow=True)

        self.assertEqual(response.status_code, 200)

        expected_response = ('User-agent: *\n'
                             'Disallow: /\n')
        self.assertEqual(response.content.decode(), expected_response)
        self.assertEqual(response['Content-Type'], 'text/plain')


class TestGetProgress(TestCase):
    def test_get_progress(self):
        experiments = [
            {
                'count_all': 6,
                'count_done': 5,
                'expected': 83
            },
            {
                'count_all': 6,
                'count_done': 6,
                'expected': 100
            },
            {
                'count_all': 6,
                'count_done': 0,
                'expected': 0
            }
        ]

        for exp in experiments:
            self.assertEqual(
                get_progress(count_all=exp['count_all'], count_done=exp['count_done']),
                exp['expected']
            )


class TestUserMessageView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex()
        self.profile.is_staff = True
        self.profile.save()

        self.disabled_message = UserMessage.objects.create(
            name='test message',
            link='https://st.yandex-team.ru/FORMS-2678',
            text='Ткни на меня',
            enabled=False,
        )
        self.enabled_message = UserMessage.objects.create(
            name='another test message',
            link='https://smth.test.ru',
            text='Нажми',
            enabled=True,
        )

    def test_show_only_enabled_success(self):
        url = '/admin/api/v2/messages/'
        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = json.loads(response.content.decode('utf-8'))
        self.assertEqual(len(data['results']), 1)
        self.assertEqual(data['results'][0]['name'], self.enabled_message.name)

    def test_show_with_valid_text_success(self):
        url = '/admin/api/v2/messages/'
        self.enabled_message.translations = {
            'text': {'en': 'en_value', 'ru': 'ru_value'}
        }
        self.enabled_message.save()

        response = self.client.get(url)
        self.assertEqual(200, response.status_code)
        data = json.loads(response.content.decode('utf-8'))
        self.assertEqual(len(data['results']), 1)
        self.assertEqual(data['results'][0]['name'], self.enabled_message.name)
        self.assertEqual(data['results'][0]['text'], 'ru_value')

        response = self.client.get(url, HTTP_ACCEPT_LANGUAGE='en')
        self.assertEqual(200, response.status_code)
        data = json.loads(response.content.decode('utf-8'))
        self.assertEqual(len(data['results']), 1)
        self.assertEqual(data['results'][0]['name'], self.enabled_message.name)
        self.assertEqual(data['results'][0]['text'], 'en_value')


class TestLanguageViewSet(TestCase):
    def test_should_return_one_language(self):
        response = self.client.get('/admin/api/v2/languages/en/')
        self.assertEqual(response.status_code, 200)
        self.assertDictEqual(response.data, {
            'id': 'en',
            'name': 'Английский',
        })

    def test_shouldnt_return_one_language(self):
        response = self.client.get('/admin/api/v2/languages/qq/')
        self.assertEqual(response.status_code, 404)

    def test_should_return_list_of_languages(self):
        response = self.client.get('/admin/api/v2/languages/')
        self.assertEqual(response.status_code, 200)
        self.assertTrue(len(response.data) > 0)
        languages = {
            it['id']: it['name']
            for it in response.data
        }
        self.assertTrue('en' in languages)
        self.assertEqual(languages['en'], 'Английский')
