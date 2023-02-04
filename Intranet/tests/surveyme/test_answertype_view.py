# -*- coding: utf-8 -*-
from django.test import TestCase
from events.accounts.helpers import YandexClient


class TestAnswerTypes_forms_ext(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)

    def test_answer_types(self):
        response = self.client.get('/admin/api/v2/answer-types/?page_size=max')
        self.assertEqual(response.status_code, 200)

        answer_types = {
            at['slug']: at
            for at in response.data['results']
        }
        # внеш кф не поддерживает поле Оплата
        self.assertNotIn('answer_payment', answer_types.keys())
        # внеш кф поддерживает профильные поля
        self.assertTrue(any(at['kind'] == 'profile' for at in answer_types.values()))
