# -*- coding: utf-8 -*-
import base64
import json
import responses
import os

from django.conf import settings
from django.core.files.base import ContentFile
from django.test import TestCase, override_settings
from django.utils.translation import ugettext_lazy as _
from unittest.mock import patch, ANY

from events.common_storages.storage import MdsStorage
from events.conditions.factories import ContentTypeAttributeFactory
from events.surveyme.models import ProfileSurveyAnswer, Survey
from events.surveyme_integration.factories import (
    IntegrationFileTemplateFactory,
    SubscriptionAttachmentFactory,
    SurveyHookConditionNodeFactory,
    SurveyHookConditionFactory,
)
from events.surveyme_integration.models import HookSubscriptionNotification
from events.surveyme_integration.services.email.context_processors import EmailBodyField
from events.surveyme_integration.helpers import IntegrationTestMixin
from events.surveyme_integration.utils import encode_string
from events.arc_compat import read_asset


class TestEmailIntegration(IntegrationTestMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()

        self.subscription.title = 'Hello'
        self.subscription.body = 'Thank you for feedback'
        self.subscription.email_to_address = 'user@company.com'
        self.subscription.email_from_address = 'web-chib@yandex-team.ru'
        self.subscription.email_spam_check = False
        self.subscription.save()

    def register_uri(self, status=200, result=None):
        result = result or {
            'message_id': '<123@hostname>',
            'status': 'OK',
            'task_id': '1234',
        }
        body = {
            'result': result,
        }
        url = settings.SENDER_URL.format(
            account=settings.SENDER_ACCOUNT,
            campaign=settings.APP_TYPE,
        )
        responses.add(responses.POST, url, json=body, status=status)

    def assertRequestIsNotEmpty(self):
        self.assertEqual(len(responses.calls), 1)

    def assertRequestIsEmpty(self):
        self.assertEqual(len(responses.calls), 0)

    @responses.activate
    def test_simple_data(self):
        self.register_uri()

        response = self.post_data()  # BANG!

        response_json = response.json()
        answer = ProfileSurveyAnswer.objects.get(survey=self.survey)
        self.assertDictEqual(
            response_json, {
                'answer_id': answer.id,
                'answer_key': answer.secret_code,
                'show_results': None,
                'integrations': [],
                'payment_url': None,
                'messages': []
            }
        )

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertDictEqual(request_json['args'], {
            'subject': self.subscription.title,
            'body': self.subscription.body,
        })
        self.assertListEqual(request_json['to'], [{'email': self.subscription.email_to_address}])
        self.assertEqual(request_json['from_email'], self.subscription.email_from_address)
        self.assertEqual(request_json['from_name'], self.subscription.email_from_title)
        self.assertTrue(request_json['async'])
        self.assertTrue(request_json['has_ugc'])
        self.assertFalse(request_json['ignore_empty_email'])
        self.assertListEqual(request_json['attachments'], [])
        self.assertEqual(request_json['headers']['X-Form-ID'], str(self.survey.pk))
        self.assertEqual(request_json['headers']['MESSAGE-ID'], '<%s>' % self.get_message_id(response))
        self.assertIsNotNone(request.headers['X-Sender-Real-User-IP'])

        notifications = list(HookSubscriptionNotification.objects.filter(subscription=self.subscription))
        self.assertEqual(len(notifications), 1)
        self.assertEqual(notifications[0].status, 'success')
        self.assertTrue(notifications[0].is_visible)

    @responses.activate
    def test_simple_data_with_follow(self):
        self.register_uri()
        self.subscription.follow_result = True
        self.subscription.save()
        response = self.post_data()  # BANG!
        response_json = response.json()
        answer = ProfileSurveyAnswer.objects.get(survey=self.survey)
        self.assertDictEqual(
            response_json, {
                'answer_id': answer.id,
                'answer_key': answer.secret_code,
                'show_results': None,
                'integrations': [{u'id': str(self.subscription.id), u'type': u'email'}],
                'payment_url': None,
                'messages': []
            }
        )

        self.assertRequestIsNotEmpty()

    @responses.activate
    def test_simple_data_with_follow_and_condition_true(self):
        self.register_uri()
        self.subscription.follow_result = True
        self.subscription.save()
        node = SurveyHookConditionNodeFactory(hook=self.subscription.survey_hook)

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='Ответ на вопрос "Короткий ответ"',
            attr='answer_short_text'
        )

        self.condition = SurveyHookConditionFactory(
            condition_node=node,
            content_type_attribute=self.content_type_attribute,
            value=self.data[self.questions['some_text'].get_form_field_name()],
            operator='and',
            condition='eq',
            survey_question=self.questions['some_text'],
        )

        response = self.post_data()  # BANG!
        response_json = response.json()
        answer = ProfileSurveyAnswer.objects.get(survey=self.survey)
        self.assertDictEqual(
            response_json, {
                'answer_id': answer.id,
                'answer_key': answer.secret_code,
                'show_results': None,
                'integrations': [{u'id': str(self.subscription.id), u'type': u'email'}],
                'payment_url': None,
                'messages': []
            }
        )

        self.assertRequestIsNotEmpty()

    @responses.activate
    def test_simple_data_with_follow_and_condition_false(self):
        self.register_uri()
        self.subscription.follow_result = True
        self.subscription.save()
        node = SurveyHookConditionNodeFactory(hook=self.subscription.survey_hook)

        self.content_type_attribute = ContentTypeAttributeFactory(
            title='Ответ на вопрос "Короткий ответ"',
            attr='answer_short_text'
        )

        self.condition = SurveyHookConditionFactory(
            condition_node=node,
            content_type_attribute=self.content_type_attribute,
            value='smth wrong',
            operator='and',
            condition='eq',
            survey_question=self.questions['some_text'],
        )

        response = self.post_data()  # BANG!
        response_json = response.json()
        answer = ProfileSurveyAnswer.objects.get(survey=self.survey)
        self.assertDictEqual(
            response_json, {
                'answer_id': answer.id,
                'answer_key': answer.secret_code,
                'show_results': None,
                'integrations': [],
                'payment_url': None,
                'messages': []
            }
        )

        self.assertRequestIsEmpty()

    @responses.activate
    def test_simple_data_with_specials(self):
        self.register_uri()
        self.subscription.title = 'Hello\n\x07world'
        self.subscription.headers.create(name='Paragraph', value='first line\nsecond line')
        self.subscription.headers.create(name='String', value='one line\x07\x08')
        self.subscription.headers.create(name='Number', value=42)
        self.subscription.headers.create(name='From', value='foo')
        self.subscription.headers.create(name='To', value='bar')
        self.subscription.save()

        self.post_data()

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertDictEqual(request_json['args'], {
            'subject': 'Hello  world',
            'body': self.subscription.body,
        })
        self.assertEqual(request_json['headers']['X-Form-ID'], str(self.survey.pk))
        self.assertEqual(request_json['headers']['Paragraph'], 'first line second line')
        self.assertEqual(request_json['headers']['String'], 'one line  ')
        self.assertEqual(request_json['headers']['Number'], '42')
        self.assertTrue('From' not in request_json['headers'])
        self.assertTrue('To' not in request_json['headers'])

    @responses.activate
    def test_simple_data_from_address_with_extra_quotes(self):
        self.register_uri()
        self.subscription.email_from_address = '"User Name"" <user@example.com>'
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertEqual(request_json['from_email'], 'User Name <user@example.com>')

    @responses.activate
    @override_settings(APP_TYPE='forms_int')
    def test_data_with_variables_int(self):
        self.register_uri()
        self.subscription.surveyvariable_set.add(*list(self.variables.values()))
        self.subscription.title = 'Feedback for {%s}' % self.variables_ids['form_name']
        self.subscription.email_from_address = 'from-{%s}' % self.variables_ids['email_answer_value']
        self.subscription.email_to_address = '{%s}' % self.variables_ids['email_answer_value']
        self.subscription.body = (
            'Hello, {%s}. Your answers:\n{%s}' %
            (self.variables_ids['email_answer_value'], self.variables_ids['form_answers'])
        )
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertDictEqual(request_json['args'], {
            'subject': 'Feedback for %s' % self.variables_result_data['form_name'],
            'body': 'Hello, %s. Your answers:\n%s' % (
                self.variables_result_data['email_answer_value'],
                self.variables_result_data['form_answers']
            ),
        })
        self.assertEqual(request_json['from_email'], 'from-%s' % self.variables_result_data['email_answer_value'])
        self.assertListEqual(request_json['to'], [{'email': self.variables_result_data['email_answer_value']}])

    @responses.activate
    @override_settings(APP_TYPE='forms_ext')
    def test_data_with_variables_ext(self):
        self.register_uri()
        self.subscription.surveyvariable_set.add(*list(self.variables.values()))
        self.subscription.title = 'Feedback for {%s}' % self.variables_ids['form_name']
        self.subscription.email_from_address = 'from-{%s}' % self.variables_ids['email_answer_value']
        self.subscription.email_to_address = '{%s}' % self.variables_ids['email_answer_value']
        self.subscription.body = (
            'Hello, {%s}. Your answers:\n{%s}' %
            (self.variables_ids['email_answer_value'], self.variables_ids['form_answers'])
        )
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertDictEqual(request_json['args'], {
            'subject': 'Feedback for %s' % self.variables_result_data['form_name'],
            'body': 'Hello, %s. Your answers:\n%s\n\n%s' % (
                self.variables_result_data['email_answer_value'],
                self.variables_result_data['form_answers'],
                EmailBodyField.disclaimer,
            ),
        })
        self.assertEqual(request_json['from_email'], 'from-%s' % self.variables_result_data['email_answer_value'])
        self.assertListEqual(request_json['to'], [{'email': self.variables_result_data['email_answer_value']}])

    @responses.activate
    @override_settings(APP_TYPE='forms_ext')
    def test_data_without_variables_ext(self):
        self.register_uri()
        self.subscription.surveyvariable_set.add(*list(self.variables.values()))
        self.subscription.title = 'Feedback for {%s}' % self.variables_ids['form_name']
        self.subscription.email_from_address = 'from-{%s}' % self.variables_ids['email_answer_value']
        self.subscription.email_to_address = '{%s}' % self.variables_ids['email_answer_value']
        self.subscription.body = (
            'Email from form {%s}.' %
            (self.variables_ids['form_name'], )
        )
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertDictEqual(request_json['args'], {
            'subject': 'Feedback for %s' % self.variables_result_data['form_name'],
            'body': 'Email from form %s.' % (
                self.variables_result_data['form_name'],
            ),
        })
        self.assertEqual(request_json['from_email'], 'from-%s' % self.variables_result_data['email_answer_value'])
        self.assertListEqual(request_json['to'], [{'email': self.variables_result_data['email_answer_value']}])

    @responses.activate
    @override_settings(APP_TYPE='forms_int')
    def test_data_with_filters(self):
        self.register_uri()
        self.add_filters_to_variables()
        self.subscription.surveyvariable_set.add(*list(self.variables.values()))
        self.subscription.title = 'Feedback for {%s}' % self.variables_ids['form_name']
        self.subscription.email_from_address = 'from-{%s}' % self.variables_ids['email_answer_value']
        self.subscription.email_to_address = '{%s}' % self.variables_ids['email_answer_value']
        self.subscription.body = (
            'Hello, {%s}. Your answers:\n{%s}' %
            (self.variables_ids['email_answer_value'], self.variables_ids['form_answers'])
        )
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertDictEqual(request_json['args'], {
            'subject': 'Feedback for %s' % self.variables_filtered_result_data['form_name'],
            'body': 'Hello, %s. Your answers:\n%s' % (
                self.variables_filtered_result_data['email_answer_value'],
                self.variables_filtered_result_data['form_answers']
            ),
        })
        self.assertEqual(request_json['from_email'], 'from-%s' % self.variables_filtered_result_data['email_answer_value'])
        self.assertListEqual(request_json['to'], [{'email': self.variables_filtered_result_data['email_answer_value']}])

    @responses.activate
    def test_headers(self):
        self.register_uri()
        self.subscription.headers.create(name='X-GEOBASE', value=1)
        self.subscription.headers.create(name='X-ID', value='2')
        self.subscription.headers.create(name='ONLY-WITH-VALUE-HEADER', value='', add_only_with_value=True)
        self.subscription.headers.create(name='EMPTY-HEADER', value='', add_only_with_value=False)

        response = self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        expected = {
            'MESSAGE-ID': '<%s>' % self.get_message_id(response),
            'X-Form-ID': str(self.survey.pk),
            'X-ID': '2',
            'X-GEOBASE': '1',
            'EMPTY-HEADER': '',
        }
        self.assertDictEqual(request_json['headers'], expected)

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_headers_for_biz(self):
        self.register_uri()
        self.subscription.headers.create(name='X-GEOBASE', value=1)
        self.subscription.headers.create(name='X-ID', value='2')
        self.subscription.headers.create(name='ONLY-WITH-VALUE-HEADER', value='', add_only_with_value=True)
        self.subscription.headers.create(name='EMPTY-HEADER', value='', add_only_with_value=False)

        with patch('events.surveyme_integration.services.email.check_form.CheckFormV2.check') as mock_check:
            mock_check.return_value = {
                'spam': False,
                'ban': False,
            }
            response = self.post_data()  # BANG!

        mock_check.assert_called_once()

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        expected = {
            'MESSAGE-ID': '<%s>' % self.get_message_id(response),
            'X-Form-ID': str(self.survey.pk),
            'X-Yandex-CF-Receipt': '',
        }
        self.assertDictEqual(request_json['headers'], expected)

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_headers_for_biz_without_headers_context(self):
        self.register_uri()
        self.subscription.headers.create(name='X-GEOBASE', value=1)
        self.subscription.headers.create(name='X-ID', value='2')
        self.subscription.headers.create(name='ONLY-WITH-VALUE-HEADER', value='', add_only_with_value=True)
        self.subscription.headers.create(name='EMPTY-HEADER', value='', add_only_with_value=False)

        with patch('events.surveyme_integration.services.email.context_processors.EmailHeadersSerializer.to_representation') as mock_headers:
            with patch('events.surveyme_integration.services.email.check_form.CheckFormV2.check') as mock_check:
                mock_headers.return_value = {}
                mock_check.return_value = {
                    'spam': False,
                    'ban': False,
                    'receipt': '12345',
                }
                response = self.post_data()  # BANG!

        mock_check.assert_called_once()
        mock_headers.assert_called_once_with(ANY)

        self.assertRequestIsNotEmpty()
        expected = {
            'MESSAGE-ID': '<%s>' % self.get_message_id(response),
            'X-Form-ID': str(self.survey.pk),
            'X-Yandex-CF-Receipt': '12345',
        }

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertDictEqual(request_json['headers'], expected)

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

        response = self.post_data()  # BANG!

        expected = {
            'X-%s' % self.variables_result_data['form_name']:
                'value-%s' % self.variables_result_data['email_answer_value'],
            'X-another-%s' % self.variables_result_data['form_name']:
                'value-another-%s' % self.variables_result_data['email_answer_value'],
            'MESSAGE-ID': '<%s>' % self.get_message_id(response),
            'X-Form-ID': str(self.survey.pk),
        }
        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertDictEqual(request_json['headers'], expected)

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

        response = self.post_data()  # BANG!

        expected = {
            'X-%s' % self.variables_filtered_result_data['form_name']:
                'value-%s' % self.variables_filtered_result_data['email_answer_value'],
            'X-another-%s' % self.variables_filtered_result_data['form_name']:
                'value-another-%s' % self.variables_filtered_result_data['email_answer_value'],
            'MESSAGE-ID': '<%s>' % self.get_message_id(response),
            'X-Form-ID': str(self.survey.pk),
        }
        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertDictEqual(request_json['headers'], expected)

    def get_files_path(self):
        return os.path.join(settings.FIXTURES_DIR, 'files')

    def _fetcher(self, name):
        obj_name = name.split('_')[-1]
        return read_asset(os.path.join(self.get_files_path(), obj_name))

    @responses.activate
    def test_should_send_all_attachments(self):
        self.register_uri()
        integration_file_template = IntegrationFileTemplateFactory(
            survey=self.survey,
            name='example',
            type='txt',
            template='текст',
        )
        first_attachment = SubscriptionAttachmentFactory(subscription=self.subscription)
        second_attachment = SubscriptionAttachmentFactory(subscription=self.subscription)
        self.subscription.attachment_templates.add(integration_file_template)

        first_orig_name = 'first.txt'
        second_orig_name = 'second.jpg'

        first_file = read_asset(os.path.join(self.get_files_path(), 'first.txt'))
        second_file = read_asset(os.path.join(self.get_files_path(), 'second.jpg'))

        with patch('events.common_storages.storage.generate_code') as mock_generate:
            mock_generate.return_value = 'b9j2a642b859b330557ab191943c999g'
            with patch.object(MdsStorage, '_save') as mock_save:
                mock_save.return_value = '401/{}_{}'.format('b9j2a642b859b330557ab191943c999g', 'first.txt')
                first_attachment.file = ContentFile(first_file, name=first_orig_name)
                first_attachment.save()

        with patch('events.common_storages.storage.generate_code') as mock_generate:
            mock_generate.return_value = 'b9j2a642b859b330557ab191943c999g'
            with patch.object(MdsStorage, '_save') as mock_save:
                mock_save.return_value = '401/{}_{}'.format('b9j2a642b859b330557ab191943c999g', 'second.jpg')
                second_attachment.file = ContentFile(second_file, name=second_orig_name)
                second_attachment.save()

        with patch.object(MdsStorage, '_fetch', self._fetcher):
            self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())
        self.assertEqual(len(request_json['attachments']), 3)

        # first
        attach = request_json['attachments'][0]
        self.assertEqual(attach['filename'], first_orig_name)
        self.assertEqual(attach['mime_type'], 'text/plain;charset=utf-8')
        self.assertEqual(base64.b64decode(attach['data']), first_file)

        # second
        attach = request_json['attachments'][1]
        self.assertEqual(attach['filename'], second_orig_name)
        self.assertEqual(attach['mime_type'], 'image/jpeg')
        self.assertEqual(base64.b64decode(attach['data']), second_file)

        # third
        attach = request_json['attachments'][2]
        self.assertEqual(attach['filename'], 'example.txt')
        self.assertEqual(attach['mime_type'], 'text/plain;charset=utf-8')
        self.assertEqual(base64.b64decode(attach['data']).decode(), 'текст')

    @responses.activate
    def test_check_notifications_count(self):
        self.register_uri()
        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()

        notifications = list(HookSubscriptionNotification.objects.filter(subscription=self.subscription))
        self.assertEqual(len(notifications), 1)
        self.assertEqual(notifications[0].status, 'success')
        self.assertTrue(notifications[0].is_visible)

        self.client.login_yandex(is_superuser=True)

        response = self.client.get('/admin/api/v2/surveys/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['results'][0]['notifications_count'], 0)

        response = self.client.get('/admin/api/v2/notifications/?is_visible=true&subscription=%s' % notifications[0].id)
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content.decode(response.charset))
        self.assertEqual(len(data['results']), 1)

        data = {'is_visible': False, 'status': 'canceled'}
        response = self.client.patch('/admin/api/v2/notifications/%s/' % notifications[0].id, data)
        self.assertEqual(response.status_code, 200)
        notifications[0].refresh_from_db()
        self.assertEqual(notifications[0].is_visible, False)
        self.assertNotEqual(notifications[0].status, 'canceled')

        response = self.client.get('/admin/api/v2/notifications/?is_visible=true&subscription=%s' % notifications[0].id)
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content.decode(response.charset))
        self.assertEqual(len(data['results']), 0)

        response = self.client.get('/admin/api/v2/surveys/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['results'][0]['notifications_count'], 0)

        response = self.client.get('/admin/api/v2/notifications/?is_visible=false')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content.decode(response.charset))
        self.assertEqual(len(data['results']), 1)

    @responses.activate
    def test_check_notifications_count_should_be_none(self):
        self.register_uri()
        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()

        notifications = list(HookSubscriptionNotification.objects.filter(subscription=self.subscription))
        self.assertEqual(len(notifications), 1)
        self.assertEqual(notifications[0].status, 'success')
        self.assertTrue(notifications[0].is_visible)
        self.subscription.delete()

        survey_notifications = HookSubscriptionNotification.objects.filter(survey=self.survey)
        self.assertEqual(survey_notifications.count(), 1)
        self.assertEqual(survey_notifications.filter(subscription__isnull=True).count(), 1)

        self.client.login_yandex(is_superuser=True)

        response = self.client.get('/admin/api/v2/surveys/')
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data['results'][0]['notifications_count'], 0)

        response = self.client.get('/admin/api/v2/notifications/?survey=%s' % self.survey.pk)
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content.decode(response.charset))
        self.assertEqual(len(data['results']), survey_notifications.count())

        response = self.client.get('/admin/api/v2/notifications/?survey=%s&subscription=%s' % (self.survey.pk, data['results'][0]['id']))
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.content.decode(response.charset))
        self.assertEqual(len(data['results']), 0)

    @responses.activate
    def test_spam_check(self):
        self.register_uri()
        self.survey.captcha_display_mode = 'auto'
        self.survey.captcha_type = 'std'
        self.survey.save()

        self.subscription.email_spam_check = True
        self.subscription.save()

        with patch('events.surveyme_integration.services.email.check_form.CheckFormV2.check') as mock_check:
            mock_check.return_value = {
                'spam': True,
                'ban': True,
            }
            self.post_data()  # BANG!

        self.assertRequestIsEmpty()

        notifications = list(HookSubscriptionNotification.objects.filter(subscription=self.subscription))
        self.assertEqual(len(notifications), 1)
        self.assertEqual(notifications[0].status, 'error')
        self.assertTrue(notifications[0].error['classname'].endswith('EmailSpamError'))

        self.survey.refresh_from_db()
        self.assertFalse(Survey.get_spam_detected(self.survey.pk))
        self.assertEqual(self.survey.captcha_display_mode, 'auto')
        self.assertEqual(self.survey.captcha_type, 'std')

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True, SWITCH_ON_BAN=True)
    def test_spam_check_for_business_always_true(self):
        self.register_uri()
        self.survey.captcha_display_mode = 'always'
        self.survey.captcha_type = 'std'
        self.survey.save()

        self.subscription.email_spam_check = True
        self.subscription.save()

        with patch('events.surveyme.views_api.SurveyFormView._is_show_captcha') as mock_captcha:
            with patch('events.surveyme_integration.services.email.check_form.CheckFormV2.check') as mock_check:
                mock_captcha.return_value = False
                mock_check.return_value = {
                    'spam': True,
                    'ban': True,
                }
                self.post_data()  # BANG!

        self.assertRequestIsEmpty()

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.captcha_display_mode, 'always')
        self.assertTrue(Survey.get_spam_detected(self.survey.pk))

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True, SWITCH_ON_BAN=True)
    def test_spam_check_for_business_always_false(self):
        self.register_uri()
        self.survey.captcha_display_mode = 'always'
        self.survey.captcha_type = 'std'
        self.survey.save()

        with patch('events.surveyme.views_api.SurveyFormView._is_show_captcha') as mock_captcha:
            with patch('events.surveyme_integration.services.email.check_form.CheckFormV2.check') as mock_check:
                mock_captcha.return_value = False
                mock_check.return_value = {
                    'spam': False,
                    'ban': False,
                }
                self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.captcha_display_mode, 'always')
        self.assertFalse(Survey.get_spam_detected(self.survey.pk))

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True, SWITCH_ON_BAN=True)
    def test_spam_check_for_business_always_false_but_spam_already_detected(self):
        self.register_uri()
        self.survey.captcha_display_mode = 'always'
        self.survey.captcha_type = 'std'
        self.survey.is_ban_detected = True
        self.survey.save()
        Survey.set_spam_detected(self.survey.pk, True)

        with patch('events.surveyme.views_api.SurveyFormView._is_show_captcha') as mock_captcha:
            with patch('events.surveyme_integration.services.email.check_form.CheckFormV2.check') as mock_check:
                mock_captcha.return_value = False
                mock_check.return_value = {
                    'spam': False,
                    'ban': False,
                }
                self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()

        self.survey.refresh_from_db()
        self.assertTrue(Survey.get_spam_detected(self.survey.pk))
        self.assertTrue(self.survey.is_ban_detected)

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True, SWITCH_ON_BAN=True)
    def test_spam_check_for_business_auto(self):
        self.register_uri()
        self.survey.captcha_display_mode = 'auto'
        self.survey.captcha_type = 'std'
        self.survey.save()

        with patch('events.surveyme_integration.services.email.check_form.CheckFormV2.check') as mock_check:
            mock_check.return_value = {
                'spam': True,
                'ban': True,
            }
            self.post_data()  # BANG!

        self.assertRequestIsEmpty()

        notifications = list(HookSubscriptionNotification.objects.filter(subscription=self.subscription))
        self.assertEqual(len(notifications), 1)
        self.assertEqual(notifications[0].status, 'error')
        self.assertIn('EmailSpamError', notifications[0].error['classname'])

        self.survey.refresh_from_db()
        self.assertEqual(self.survey.captcha_display_mode, 'auto')
        self.assertTrue(Survey.get_spam_detected(self.survey.pk))
        self.assertEqual(self.survey.captcha_type, 'ocr')

    @responses.activate
    def test_from_title_should_be_empty_if_not_entered(self):
        self.register_uri()
        self.subscription.email_from_title = ''
        self.subscription.email_from_address = 'devnull@domain.com'
        self.subscription.email_to_address = 'user@domain.com'
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertEqual(request_json['from_email'], 'devnull@domain.com')
        self.assertListEqual(request_json['to'], [{'email': 'user@domain.com'}])

    @responses.activate
    def test_from_title_should_be_empty_for_email(self):
        self.register_uri()
        self.subscription.email_from_title = 'admin@domain.com'
        self.subscription.email_from_address = 'devnull@domain.com'
        self.subscription.email_to_address = 'user@domain.com'
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertEqual(request_json['from_email'], 'devnull@domain.com')
        self.assertListEqual(request_json['to'], [{'email': 'user@domain.com'}])

    @responses.activate
    def test_from_title_shouldnt_be_empty_for_text(self):
        self.register_uri()
        self.subscription.email_from_title = 'DevNull'
        self.subscription.email_from_address = 'devnull@domain.com'
        self.subscription.email_to_address = 'user@domain.com'
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertEqual(request_json['from_email'], 'devnull@domain.com')
        self.assertEqual(request_json['from_name'], encode_string('DevNull'))
        self.assertListEqual(request_json['to'], [{'email': 'user@domain.com'}])

    @responses.activate
    def test_from_title_with_header_reply_to(self):
        self.register_uri()
        self.subscription.email_from_title = 'DevNull'
        self.subscription.email_from_address = 'devnull@domain.com'
        self.subscription.email_to_address = 'user@domain.com'
        self.subscription.save()

        self.subscription.headers.create(name='Reply-To', value='Admin <admin@domain.com>')

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertEqual(request_json['from_email'], 'devnull@domain.com')
        self.assertEqual(request_json['from_name'], encode_string('DevNull'))
        self.assertListEqual(request_json['to'], [{'email': 'user@domain.com'}])
        self.assertEqual(request_json['headers']['Reply-To'], '=?utf-8?B?IkFkbWluIg==?= <admin@domain.com>')

    @responses.activate
    def test_punnycode_address_one_email(self):
        self.register_uri()
        self.subscription.email_to_address = 'user@почта.рф'
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertListEqual(request_json['to'], [{'email': 'user@xn--80a1acny.xn--p1ai'}])

    @responses.activate
    def test_punnycode_address_two_emails(self):
        self.register_uri()
        self.subscription.email_to_address = 'user@почта.рф, user@mail.ru'
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertListEqual(request_json['to'], [
            {'email': 'user@xn--80a1acny.xn--p1ai'},
            {'email': 'user@mail.ru'},
        ])

    @responses.activate
    @override_settings(APP_TYPE='forms_int')
    def test_from_email_should_be_set_to_default_if_not_entered_for_int(self):
        self.register_uri()
        self.subscription.email_from_address = ''
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertEqual(request_json['from_email'], 'devnull@yandex-team.ru')

    @responses.activate
    @override_settings(APP_TYPE='forms_int')
    def test_from_email_should_be_set_to_default_if_not_an_email_for_int(self):
        self.register_uri()
        self.subscription.email_from_address = 'not an email'
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertEqual(request_json['from_email'], 'devnull@yandex-team.ru')

    @responses.activate
    @override_settings(APP_TYPE='forms_ext')
    def test_from_email_should_be_set_to_default_if_not_entered_for_ext(self):
        self.register_uri()
        self.subscription.email_from_address = ''
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertEqual(request_json['from_email'], f'{self.survey.pk}@forms.yandex.ru')

    @responses.activate
    @override_settings(APP_TYPE='forms_ext')
    def test_from_email_should_be_set_to_default_if_not_an_email_entered_for_ext(self):
        self.register_uri()
        self.subscription.email_from_address = 'not an email'
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertEqual(request_json['from_email'], f'{self.survey.pk}@forms.yandex.ru')

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_from_email_should_be_set_to_default_if_not_entered_for_biz(self):
        self.register_uri()
        self.subscription.email_from_address = ''
        self.subscription.save()

        with patch('events.surveyme_integration.services.email.check_form.CheckFormV2.check') as mock_check:
            mock_check.return_value = {
                'spam': False,
                'ban': False,
            }
            self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        from_title = encode_string(_('Яндекс.Формы'))
        self.assertEqual(request_json['from_name'], f'{from_title}')
        self.assertEqual(request_json['from_email'], f'{self.survey.pk}@forms-mailer.yaconnect.com')

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_from_email_should_be_set_to_default_if_not_an_email_for_biz(self):
        self.register_uri()
        self.subscription.email_from_address = 'not an email'
        self.subscription.save()

        with patch('events.surveyme_integration.services.email.check_form.CheckFormV2.check') as mock_check:
            mock_check.return_value = {
                'spam': False,
                'ban': False,
            }
            self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        from_title = encode_string(_('Яндекс.Формы'))
        self.assertEqual(request_json['from_name'], f'{from_title}')
        self.assertEqual(request_json['from_email'], f'{self.survey.pk}@forms-mailer.yaconnect.com')

    @responses.activate
    def test_semicolon_separated_address(self):
        self.register_uri()
        self.subscription.email_to_address = 'a@c.com; b@c.com; c@c.com'
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertListEqual(request_json['to'], [
            {'email': 'a@c.com'},
            {'email': 'b@c.com'},
            {'email': 'c@c.com'},
        ])

    @responses.activate
    def test_should_raise_error_if_recipient_address_is_empty(self):
        self.register_uri()
        self.subscription.email_to_address = ''
        self.subscription.save()

        self.post_data()  # BANG!

        self.assertRequestIsEmpty()

        notifications = list(HookSubscriptionNotification.objects.filter(subscription=self.subscription))
        self.assertEqual(len(notifications), 1)
        self.assertEqual(notifications[0].status, 'error')
        self.assertIn('EmailRecipientError', notifications[0].error['classname'])
