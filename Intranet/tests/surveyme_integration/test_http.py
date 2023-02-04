# -*- coding: utf-8 -*-
import json
import responses

from django.test import TestCase

from events.common_app.testutils import parse_multipart
from events.surveyme.api_admin.v2.serializers import AnswerTypeSerializer, SurveyQuestionChoiceSerializer
from events.surveyme.factories import SurveyQuestionFactory, SurveyQuestionChoiceFactory
from events.surveyme.models import AnswerType
from events.surveyme_integration.factories import SubscriptionHeaderFactory
from events.surveyme_integration.models import HookSubscriptionNotification
from events.surveyme_integration.services.http.action_processors import HTTPBaseActionProcessor
from events.surveyme_integration.helpers import IntegrationTestMixin


class TestHTTPIntegration(IntegrationTestMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()

        self.subscription.service_type_action_id = 4  # post to http
        self.subscription.http_url = 'http://yandex.ru/test_url/'
        self.subscription.save()

    def get_request_callback(self, status_code):
        def callback(request):
            headers = dict(request.headers)
            headers.update({
                'x-mark': 'mark'
            })
            return status_code, headers, 'hello'
        return callback

    def register_uri(self, status_code=200):
        responses.add_callback(
            responses.POST,
            self.subscription.http_url,
            callback=self.get_request_callback(status_code)
        )

    @responses.activate
    def test_simple_data(self):
        self.register_uri()
        self.post_data()  # BANG!
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_data_with_variables(self):
        self.register_uri()
        self.subscription.surveyvariable_set.add(*list(self.variables.values()))
        self.subscription.http_url = '%s?email={%s}' % (
            self.subscription.http_url,
            self.variables_ids['email_answer_value']
        )
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        expected = {
            'email': self.variables_result_data['email_answer_value'],
        }
        self.assertEqual(responses.calls[0].request.params, expected)

    @responses.activate
    def test_data_with_filters(self):
        self.register_uri()
        self.add_filters_to_variables()
        self.subscription.surveyvariable_set.add(*list(self.variables.values()))
        self.subscription.http_url = '%s?email={%s}' % (
            self.subscription.http_url,
            self.variables_ids['email_answer_value']
        )
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        expected = {
            'email': self.variables_filtered_result_data['email_answer_value'],

        }
        self.assertEqual(responses.calls[0].request.params, expected)

    @responses.activate
    def test_headers(self):
        self.register_uri()
        self.subscription.headers.create(name='X-GEOBASE', value=1)
        self.subscription.headers.create(name='X-ID', value='2')

        self.post_data()  # BANG!

        expected = {
            'X-ID': '2',
            'X-GEOBASE': '1',
            'X-DELIVERY-ID': str(self.subscription.get_all_notifications().first().id)
        }
        for key, value in list(expected.items()):
            self.assertEqual(responses.calls[0].request.headers.get(key), value)

    @responses.activate
    def test_headers_with_variables(self):
        self.register_uri()
        self.subscription.surveyvariable_set.add(*list(self.variables.values()))

        self.subscription.headers.create(
            name='X-{%s}' % self.variables_ids['form_name'],
            value='value-{%s}' % self.variables_ids['email_answer_value']
        )
        self.subscription.headers.create(
            name='X-another-{%s}' % self.variables_ids['form_name'],
            value='value-another-{%s}' % self.variables_ids['email_answer_value']
        )

        self.post_data()  # BANG!

        expected = {
            'x-%s' % self.variables_result_data['form_name']:
                'value-%s' % self.variables_result_data['email_answer_value'],
            'x-another-%s' % self.variables_result_data['form_name']:
                'value-another-%s' % self.variables_result_data['email_answer_value'],
            'x-delivery-id': str(self.subscription.get_all_notifications().first().id)
        }
        for key, value in list(expected.items()):
            self.assertEqual(responses.calls[0].request.headers.get(key), value)

    @responses.activate
    def test_headers_with_variables_and_filters(self):
        self.register_uri()
        self.subscription.surveyvariable_set.add(*list(self.variables.values()))
        self.add_filters_to_variables()

        self.subscription.headers.create(
            name='X-{%s}' % self.variables_ids['form_name'],
            value='value-{%s}' % self.variables_ids['email_answer_value']
        )
        self.subscription.headers.create(
            name='X-another-{%s}' % self.variables_ids['form_name'],
            value='value-another-{%s}' % self.variables_ids['email_answer_value']
        )

        self.post_data()  # BANG!

        expected = {
            'X-%s' % self.variables_filtered_result_data['form_name']:
                'value-%s' % self.variables_filtered_result_data['email_answer_value'],
            'X-another-%s' % self.variables_filtered_result_data['form_name']:
                'value-another-%s' % self.variables_filtered_result_data['email_answer_value'],
            'x-delivery-id': str(self.subscription.get_all_notifications().first().id)
        }
        for key, value in list(expected.items()):
            self.assertEqual(responses.calls[0].request.headers.get(key), value)

    @responses.activate
    def test_should_send_all_questions_in_body_if_is_all_questions(self):
        self.register_uri()

        self.subscription.is_all_questions = True
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        content_type, boundary = responses.calls[0].request.headers.get('content-type').split(';')
        self.assertEqual(content_type, 'multipart/form-data')

        boundary = boundary.split('=')[1].encode()
        body = responses.calls[0].request.body
        parsed_body = parse_multipart(boundary, body)

        # test name
        key = 'field_1'
        self.assertEqual(
            json.loads(parsed_body[key]['data']),
            {
                'value': self.data[self.questions['name'].get_form_field_name()],
                'question': {
                    'id': self.questions['name'].id,
                    'slug': self.questions['name'].param_slug,
                    'label': {
                        'ru': self.questions['name'].label
                    },
                    'type': AnswerTypeSerializer(self.questions['name'].answer_type).data,
                    'group_id': None,
                }
            }
        )

        # test career
        key = 'field_3'
        self.assertEqual(
            json.loads(parsed_body[key]['data']),
            {
                'value': 'working',
                'question': {
                    'id': self.questions['career'].id,
                    'slug': self.questions['career'].param_slug,
                    'label': {
                        'ru': self.questions['career'].label
                    },
                    'type': AnswerTypeSerializer(self.questions['career'].answer_type).data,
                    'group_id': None,
                    'choices': {
                        choice['label']: choice for choice in
                        SurveyQuestionChoiceSerializer(
                            self.questions['career'].surveyquestionchoice_set.all(), many=True
                        ).data
                    },
                },
                'choice_id': self.career_choices['working'].id,
            }
        )

        # test email
        key = 'field_%s' % self.questions['email'].id
        self.assertEqual(
            json.loads(parsed_body[key]['data']),
            {
                'value': self.data[self.questions['email'].get_form_field_name()],
                'question': {
                    'id': self.questions['email'].id,
                    'slug': self.questions['email'].param_slug,
                    'label': {
                        'ru': self.questions['email'].label
                    },
                    'type': AnswerTypeSerializer(self.questions['email'].answer_type).data,
                    'group_id': None,
                }
            }
        )

    @responses.activate
    def test_should_send_group_questions_correct(self):
        group_question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_group'),
        )
        question = SurveyQuestionFactory(
            label='group_text',
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
            param_is_required=False,
            group=group_question,
        )
        question_choice = SurveyQuestionFactory(
            label='group_choice',
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_choices'),
            param_is_required=False,
            group=group_question,
            param_is_allow_multiple_choice=True,
        )
        choices = {
            'first': SurveyQuestionChoiceFactory(survey_question=question_choice, label='first_choice'),
            'second': SurveyQuestionChoiceFactory(survey_question=question_choice, label='second_choice'),
        }

        self.data[question.get_form_field_name(group_counter=1)] = 'test 1 group'
        self.data[question.get_form_field_name(group_counter=2)] = 'test 2 group'
        self.data[question.get_form_field_name(group_counter=5)] = 'test 5 group'
        self.data[question_choice.get_form_field_name(group_counter=5)] = [choices['first'].id, choices['second'].id]
        self.data[question_choice.get_form_field_name(group_counter=2)] = [choices['second'].id]
        self.data[question_choice.get_form_field_name(group_counter=3)] = [choices['first'].id]

        self.register_uri()

        self.subscription.is_all_questions = True
        self.subscription.save()

        self.post_data()  # BANG!
        self.assertEqual(len(responses.calls), 1)
        content_type, boundary = responses.calls[0].request.headers.get('content-type').split(';')
        self.assertEqual(content_type, 'multipart/form-data')

        boundary = boundary.split('=')[1].encode()
        body = responses.calls[0].request.body
        parsed_body = parse_multipart(boundary, body)

        key = 'field_{}'.format(question_choice.id)
        data = json.loads(parsed_body[key]['data'])
        value = data.pop('value')
        expected = {
            'question': {
                'id': question_choice.id,
                'slug': question_choice.param_slug,
                'label': {
                    'ru': question_choice.label
                },
                'type': AnswerTypeSerializer(question_choice.answer_type).data,
                'group_id': group_question.id,
                'choices': {
                    choice['label']: choice for choice in SurveyQuestionChoiceSerializer(
                        question_choice.surveyquestionchoice_set.all(), many=True
                    ).data
                },
            },
            'choice_id': {
                question_choice.get_form_field_name(group_counter=1): str(choices['second'].id),
                question_choice.get_form_field_name(group_counter=2): str(choices['first'].id),
                question_choice.get_form_field_name(group_counter=3): "{},{}".format(choices['first'].id, choices['second'].id)
            },
        }
        self.assertDictEqual(data, expected)

        expected_value = {
            question_choice.get_form_field_name(group_counter=1): choices['second'].label,
            question_choice.get_form_field_name(group_counter=2): choices['first'].label,
            question_choice.get_form_field_name(group_counter=3): "{}, {}".format(choices['first'].label, choices['second'].label),
        }
        self.assertDictEqual(value, expected_value)

        key = 'field_{}'.format(question.id)
        data = json.loads(parsed_body[key]['data'])
        value = data.pop('value')
        self.assertDictEqual(
            data,
            {
                'question': {
                    'id': question.id,
                    'slug': question.param_slug,
                    'label': {
                        'ru': question.label
                    },
                    'type': AnswerTypeSerializer(question.answer_type).data,
                    'group_id': group_question.id,
                }
            }
        )
        expected_value = {
            question.get_form_field_name(group_counter=0): "test 1 group",
            question.get_form_field_name(group_counter=1): "test 2 group",
            question.get_form_field_name(group_counter=3): "test 5 group",
        }
        self.assertDictEqual(value, expected_value)

    @responses.activate
    def test_should_send_questions_from_list_if_not_all(self):
        self.register_uri()

        self.subscription.is_all_questions = False
        self.subscription.questions.add(self.questions['name'])
        self.subscription.questions.add(self.questions['email'])
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        content_type, boundary = responses.calls[0].request.headers.get('content-type').split(';')
        self.assertEqual(content_type, 'multipart/form-data')

        boundary = boundary.split('=')[1].encode()
        body = responses.calls[0].request.body
        parsed_body = parse_multipart(boundary, body)

        # test name
        key = 'field_1'
        self.assertEqual(
            json.loads(parsed_body[key]['data']),
            {
                'value': self.data[self.questions['name'].get_form_field_name()],
                'question': {
                    'id': self.questions['name'].id,
                    'slug': self.questions['name'].param_slug,
                    'label': {
                        'ru': self.questions['name'].label
                    },
                    'type': AnswerTypeSerializer(self.questions['name'].answer_type).data,
                    'group_id': None,
                }
            }
        )

        # test email
        key = 'field_2'
        self.assertEqual(
            json.loads(parsed_body[key]['data']),
            {
                'value': self.data[self.questions['email'].get_form_field_name()],
                'question': {
                    'id': self.questions['email'].id,
                    'slug': self.questions['email'].param_slug,
                    'label': {
                        'ru': self.questions['email'].label
                    },
                    'type': AnswerTypeSerializer(self.questions['email'].answer_type).data,
                    'group_id': None,
                }
            }
        )

        # test career
        key = 'field_3'
        self.assertFalse(key in parsed_body)

    @responses.activate
    def test_should_save_response(self):
        self.register_uri()

        self.post_data()  # BANG!

        notification = self.subscription.get_all_notifications().first()
        self.assertEqual(notification.status, 'success')
        self.assertIsNotNone(notification.date_finished)
        self.assertEqual(notification.response.get('headers').get('x-mark'), 'mark')
        self.assertEqual(notification.response.get('content'), 'hello')


class TestArbitraryHTTPIntegration(IntegrationTestMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()

        self.subscription.service_type_action_id = 11
        self.subscription.http_url = 'http://yandex.ru/test_url/'
        self.subscription.http_method = 'post'
        self.subscription.save()

        self.method = responses.POST

    def register_uri(self, status_code=200):
        responses.add_callback(
            self.method,
            self.subscription.http_url,
            callback=self.get_request_callback(status_code)
        )

    def get_request_callback(self, status_code):
        def callback(request):
            headers = dict(request.headers)
            headers.update({
                'x-mark': 'mark'
            })
            return status_code, headers, 'hello'
        return callback

    @responses.activate
    def test_simple_data_post(self):
        self.register_uri()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.method, 'POST')

    @responses.activate
    def test_simple_data_get(self):
        self.method = responses.GET
        self.subscription.http_method = 'get'
        self.subscription.save()

        self.register_uri()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.method, 'GET')

    @responses.activate
    def test_simple_data_put(self):
        self.method = responses.PUT
        self.subscription.http_method = 'put'
        self.subscription.save()

        self.register_uri()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.method, 'PUT')

    @responses.activate
    def test_simple_data_delete(self):
        self.method = responses.DELETE
        self.subscription.http_method = 'delete'
        self.subscription.save()

        self.register_uri()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.method, 'DELETE')

    @responses.activate
    def test_data_post_with_body(self):
        SubscriptionHeaderFactory(
            subscription=self.subscription,
            name='Content-Type',
            value='application/json',
        )

        body = '{"some": "data"}'
        self.subscription.body = body
        self.subscription.save()

        self.register_uri()

        self.post_data()  # BANG!

        self.assertEqual(responses.calls[0].request.body, body.encode())
        self.assertEqual(responses.calls[0].request.method, 'POST')
        self.assertEqual(responses.calls[0].request.headers['Content-Type'], 'application/json')

    @responses.activate
    def test_data_with_variables(self):
        self.register_uri()
        self.subscription.surveyvariable_set.add(*list(self.variables.values()))
        self.subscription.http_url = '%s?email={%s}' % (
            self.subscription.http_url,
            self.variables_ids['email_answer_value']
        )
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        expected = {
            'email': self.variables_result_data['email_answer_value'],
        }
        self.assertEqual(responses.calls[0].request.params, expected)


class TestLongHttpResponse(IntegrationTestMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()

        self.subscription.service_type_action_id = 4
        self.subscription.http_url = 'http://yandex.ru/test_url/'
        self.subscription.save()

        self.max_content_size = HTTPBaseActionProcessor.max_content_size

    def register_uri(self, status_code=200):
        responses.add(
            responses.POST,
            self.subscription.http_url,
            body='?' * self.max_content_size * 2,
            status=status_code,
        )

    @responses.activate
    def test_simple_data_post(self):
        self.register_uri(500)

        response = self.post_data()  # BANG!

        notification = (
            HookSubscriptionNotification.objects.all()
            .filter(
                answer_id=response.data['answer_id'],
                subscription=self.subscription,
            )
            .first()
        )
        self.assertIsNotNone(notification)
        self.assertEqual(notification.status, 'error')
        self.assertIn('RemoteServiceError', notification.error['classname'])
        self.assertIn('500 Server Error', notification.error['message'])
