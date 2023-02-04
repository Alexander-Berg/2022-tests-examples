# -*- coding: utf-8 -*-

import json

from django.test import TestCase

from events.accounts.helpers import YandexClient
from events.surveyme.factories import SurveyFactory
from events.surveyme.models import AnswerType


class TestParamMaxValidator(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory()
        self.answer_number = AnswerType.objects.get(slug='answer_number')
        self.url = '/admin/api/v2/survey-questions/'
        self.headers = {
            'content_type': 'application/json',
        }

    def test_create_question_with_min_max_params(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_number.pk,
            'label': 'Label',
            'param_min': 1,
            'param_max': 100,
        }
        response = self.client.post(self.url, data=json.dumps(data), **self.headers)
        self.assertEqual(201, response.status_code)

    def test_create_question_without_max_param(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_number.pk,
            'label': 'Label',
            'param_min': 1,
            'param_max': None,
        }
        response = self.client.post(self.url, data=json.dumps(data), **self.headers)
        self.assertEqual(201, response.status_code)

    def test_create_question_totaly_without_max_param(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_number.pk,
            'label': 'Label',
            'param_min': 1,
        }
        response = self.client.post(self.url, data=json.dumps(data), **self.headers)
        self.assertEqual(201, response.status_code)

    def test_create_question_totaly_without_min_param(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_number.pk,
            'label': 'Label',
            'param_max': 100,
        }
        response = self.client.post(self.url, data=json.dumps(data), **self.headers)
        self.assertEqual(201, response.status_code)

    def test_create_question_without_min_param(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_number.pk,
            'label': 'Label',
            'param_min': None,
            'param_max': 100,
        }
        response = self.client.post(self.url, data=json.dumps(data), **self.headers)
        self.assertEqual(201, response.status_code)

    def test_create_question_without_min_max_param(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_number.pk,
            'label': 'Label',
            'param_min': None,
            'param_max': None,
        }
        response = self.client.post(self.url, data=json.dumps(data), **self.headers)
        self.assertEqual(201, response.status_code)

    def test_create_question_totaly_without_min_max_param(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_number.pk,
            'label': 'Label',
        }
        response = self.client.post(self.url, data=json.dumps(data), **self.headers)
        self.assertEqual(201, response.status_code)

    def test_question_shouldnt_be_created(self):
        data = {
            'survey_id': self.survey.pk,
            'answer_type_id': self.answer_number.pk,
            'label': 'Label',
            'param_min': 100,
            'param_max': 1,
        }
        response = self.client.post(self.url, data=json.dumps(data), **self.headers)
        self.assertEqual(400, response.status_code)
