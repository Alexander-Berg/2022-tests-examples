# -*- coding: utf-8 -*-
from django.test import TestCase, override_settings
from events.accounts.helpers import YandexClient


class TestTakeoutView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)

    @override_settings(
        CELERY_TASK_ALWAYS_EAGER=False,
        CELERY_RESULT_BACKEND='django_celery_results.backends:DatabaseBackend',
    )
    def test_invalid_job_id(self):
        data = {
            'job_id': '123456',
        }
        response = self.client.post('/admin/api/v2/takeout/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data['status'], 'no_data')
