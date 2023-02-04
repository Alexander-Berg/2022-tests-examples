# -*- coding: utf-8 -*-
import responses

from unittest.mock import patch
from django.test import TestCase
from events.accounts.helpers import YandexClient
from events.surveyme.factories import (
    SurveyFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
)
from events.surveyme.models import AnswerType, ValidatorType
from events.surveyme.serializers import ValidatorDataSerializer
from events.common_app.helpers import MockResponse


class TestValidatorDataSerializer(TestCase):
    def test_string_value_full_data(self):
        data = {
            'id': 1,
            'slug': None,
            'name': 'Test',
            'questions': [{
                'answer_type': {
                    'slug': 'answer_short_text',
                    'name': 'Answer Short Text',
                },
                'id': 10,
                'label': 'Short Text',
                'slug': 'answer_short_text_10',
                'value': 'test',
            }],
        }
        serializer = ValidatorDataSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_string_value_min_data(self):
        data = {
            'questions': [{
                'slug': 'answer_short_text_10',
                'value': 'test',
            }],
        }
        serializer = ValidatorDataSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_without_questions_data(self):
        data = {
            'questions': [],
        }
        serializer = ValidatorDataSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_number_value(self):
        data = {
            'questions': [{
                'slug': 'answer_number_10',
                'value': 100500,
            }],
        }
        serializer = ValidatorDataSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_boolean_value(self):
        data = {
            'questions': [{
                'slug': 'answer_boolean_10',
                'value': True,
            }],
        }
        serializer = ValidatorDataSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_none_value(self):
        data = {
            'questions': [{
                'slug': 'answer_short_10',
                'value': None,
            }],
        }
        serializer = ValidatorDataSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_choices_list_value(self):
        data = {
            'questions': [{
                'slug': 'answer_choices_10',
                'value': ['one', 'two', None],
            }],
        }
        serializer = ValidatorDataSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_empty_choices_list_value(self):
        data = {
            'questions': [{
                'slug': 'answer_choices_10',
                'value': [],
            }],
        }
        serializer = ValidatorDataSerializer(data=data)
        self.assertTrue(serializer.is_valid())


class TestSubmitForm(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.survey = SurveyFactory(
            is_published_external=True,
            validator_url='/v1/tests/validator/',
        )
        external_validator = ValidatorType.objects.get(slug='external')
        answer_short_text = AnswerType.objects.get(slug='answer_short_text')
        answer_long_text = AnswerType.objects.get(slug='answer_long_text')
        answer_choices = AnswerType.objects.get(slug='answer_choices')
        answer_date = AnswerType.objects.get(slug='answer_date')
        self.questions = [
            SurveyQuestionFactory(
                survey=self.survey,
                label='Short Text',
                answer_type=answer_short_text,
                validator_type=external_validator,
                param_slug='short_text',
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                label='Long Text',
                answer_type=answer_long_text,
                param_slug='long_text',
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                label='Choices',
                answer_type=answer_choices,
                validator_type=external_validator,
                param_slug='choices',
                param_is_allow_multiple_choice=True,
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                label='Single Date',
                answer_type=answer_date,
                param_date_field_type='date',
                param_is_required=False,
                validator_type=external_validator,
                param_slug='single_date',
            ),
            SurveyQuestionFactory(
                survey=self.survey,
                label='Date Range',
                answer_type=answer_date,
                param_date_field_type='daterange',
                param_is_required=False,
                validator_type=external_validator,
                param_slug='date_range',
            ),
        ]
        self.question_choices = self.questions[2]
        self.choices = [
            SurveyQuestionChoiceFactory(
                survey_question=self.question_choices,
                label='41',
            ),
            SurveyQuestionChoiceFactory(
                survey_question=self.question_choices,
                label='42',
            ),
            SurveyQuestionChoiceFactory(
                survey_question=self.question_choices,
                label='43',
            ),
        ]
        self.url = '/v1/surveys/{survey}/form/'.format(survey=self.survey.id)

    def test_submit_form_success(self):
        data = {
            self.questions[0].param_slug: 'short',
            self.questions[1].param_slug: 'long',
            self.questions[2].param_slug: [self.choices[0].pk, self.choices[1].pk],
            self.questions[3].param_slug: '2020-05-22',
            '%s_0' % self.questions[4].param_slug: '2020-05-18',
            '%s_1' % self.questions[4].param_slug: '2020-05-24',
        }
        response_data = MockResponse({
            'status': 'OK',
        })
        with patch('events.surveyme.forms.SurveyForm.post_data', return_value=response_data) as mock_post_data:
            response = self.client.post(self.url, data)

        self.assertEqual(response.status_code, 200)
        mock_post_data.assert_called_once()
        args = mock_post_data.call_args_list[0][0]

        self.assertEqual(args[0], self.survey.validator_url)
        serializer = ValidatorDataSerializer(data=args[1])
        self.assertTrue(serializer.is_valid())

        self.assertEqual(len(serializer.validated_data['questions']), 4)
        questions = serializer.validated_data['questions']

        self.assertEqual(questions[0]['label'], self.questions[0].label)
        self.assertEqual(questions[0]['id'], self.questions[0].pk)
        self.assertEqual(questions[0]['slug'], self.questions[0].param_slug)
        self.assertEqual(questions[0]['value'], 'short')

        self.assertEqual(questions[1]['label'], self.questions[2].label)
        self.assertEqual(questions[1]['id'], self.questions[2].pk)
        self.assertEqual(questions[1]['slug'], self.questions[2].param_slug)
        self.assertListEqual(questions[1]['value'], [self.choices[0].label, self.choices[1].label])

        self.assertEqual(questions[2]['label'], self.questions[3].label)
        self.assertEqual(questions[2]['id'], self.questions[3].pk)
        self.assertEqual(questions[2]['slug'], self.questions[3].param_slug)
        self.assertListEqual(questions[2]['value'], ['2020-05-22', None])

        self.assertEqual(questions[3]['label'], self.questions[4].label)
        self.assertEqual(questions[3]['id'], self.questions[4].pk)
        self.assertEqual(questions[3]['slug'], self.questions[4].param_slug)
        self.assertListEqual(questions[3]['value'], ['2020-05-18', '2020-05-24'])

    def test_submit_form_error(self):
        data = {
            self.questions[0].param_slug: 'short',
            self.questions[1].param_slug: 'long',
            self.questions[2].param_slug: self.choices[0].pk,
            self.questions[3].param_slug: '2020-05-22',
            '%s_0' % self.questions[4].param_slug: '2020-05-18',
            '%s_1' % self.questions[4].param_slug: '2020-05-24',
        }
        response_data = MockResponse({
            'status': 'ERROR',
            'errors': {
                self.questions[0].param_slug: ['short text error'],
                self.questions[3].param_slug: ['single date error'],
                'not_exists': ['incorrect field slug'],
            },
        })
        with patch('events.surveyme.forms.SurveyForm.post_data', return_value=response_data) as mock_post_data:
            response = self.client.post(self.url, data)

        self.assertEqual(response.status_code, 400)
        mock_post_data.assert_called_once()

        fields_data = response.data['fields']
        self.assertListEqual(fields_data[self.questions[0].param_slug]['errors'], ['short text error'])
        self.assertListEqual(fields_data[self.questions[1].param_slug]['errors'], [])
        self.assertListEqual(fields_data[self.questions[2].param_slug]['errors'], [])
        self.assertListEqual(fields_data[self.questions[3].param_slug]['errors'], ['single date error'])

    @responses.activate
    def test_check_form_for_logged_user(self):
        responses.add(responses.POST, 'http://yandex.ru/test', json={"status": "OK"})
        self.survey.validator_url = 'http://yandex.ru/test'
        self.survey.save()

        user = self.client.login_yandex()
        data = {
            self.questions[0].param_slug: 'short',
            self.questions[1].param_slug: 'long',
            self.questions[2].param_slug: [self.choices[0].pk],
        }
        response = self.client.post(self.url, data)
        self.assertEqual(len(responses.calls), 1)
        request = responses.calls[0].request

        self.assertEqual(response.status_code, 200)
        self.assertTrue('X-Uid' in request.headers)
        self.assertEqual(request.headers['X-Uid'], user.uid)
        self.assertTrue('X-Login' in request.headers)
        self.assertEqual(request.headers['X-Login'], user.email)
        self.assertTrue('X-User-Ip' in request.headers)
        self.assertNotEqual(request.headers['X-User-Ip'], '')

    @responses.activate
    def test_check_form_for_not_logged_user(self):
        responses.add(responses.POST, 'http://yandex.ru/test', json={"status": "OK"})
        self.survey.validator_url = 'http://yandex.ru/test'
        self.survey.save()

        data = {
            self.questions[0].param_slug: 'short',
            self.questions[1].param_slug: 'long',
            self.questions[2].param_slug: [self.choices[0].pk],
        }
        response = self.client.post(self.url, data)
        self.assertEqual(len(responses.calls), 1)
        request = responses.calls[0].request

        self.assertEqual(response.status_code, 200)
        self.assertTrue('X-Uid' in request.headers)
        self.assertEqual(request.headers['X-Uid'], '')
        self.assertTrue('X-Login' in request.headers)
        self.assertEqual(request.headers['X-Login'], '')
        self.assertTrue('X-User-Ip' in request.headers)
        self.assertNotEqual(request.headers['X-User-Ip'], '')
