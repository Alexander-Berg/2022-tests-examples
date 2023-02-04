# -*- coding: utf-8 -*-
import base64
import json
import responses
import os

from django.conf import settings
from django.test import TestCase, override_settings
from unittest.mock import patch

from events.common_storages.storage import MdsStorage, ReadError
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.exceptions import (
    EmailSpamError,
    EmailSpamCheckError,
    EmailInternalError,
    EmailValidationError,
    EmailCompaignError,
    EmailMaxSizeExceededError,
)
from events.surveyme_integration.factories import HookSubscriptionNotificationFactory
from events.surveyme_integration.services.email.services import EmailService
from events.surveyme_integration.helpers import ServiceBaseTestCaseMixin
from events.arc_compat import read_asset


@override_settings(HOSTNAME='hostname', APP_TYPE='forms_ext')
class TestEmailService(ServiceBaseTestCaseMixin, TestCase):
    fixtures = ['initial_data.json']
    service_class = EmailService

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()
        self.notification = HookSubscriptionNotificationFactory(answer=self.answer)
        self.simple_context = {
            'subject': 'Hello/Привет',
            'body': 'Thank you for feedback. Спасибо за отзыв.',
            'to_address': 'user@company.com',
            'from_address': 'web-chib@yandex-team.ru',
            'spam_check': False,
            'headers': {
                'X-ID': '1',
                'X-GEOBASE': '2',
            },
            'attachments': None,
            'static_attachments': None,
            'attachment_templates': None,
            'survey_id': str(self.answer.survey.pk),
            'answer_id': str(self.answer.pk),
            'subscription_id': str(self.notification.subscription.pk),
            'notification_unique_id': str(self.notification.pk),
        }
        self.message_id = '%s.%s.%s.%s@%s' % (
            settings.APP_TYPE,
            self.simple_context['survey_id'],
            self.simple_context['answer_id'],
            self.simple_context['notification_unique_id'],
            settings.HOSTNAME,
        )
        super().setUp()

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
    def test_send__should_send_email(self):
        self.register_uri()

        response = self.do_service_action('send', context=self.simple_context)

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertDictEqual(request_json['args'], {
            'subject': self.simple_context['subject'],
            'body': self.simple_context['body'],
        })
        self.assertListEqual(request_json['to'], [{'email': self.simple_context['to_address']}])
        self.assertEqual(request_json['from_email'], self.simple_context['from_address'])
        self.assertEqual(request_json['from_name'], '')
        self.assertTrue(request_json['async'])
        self.assertTrue(request_json['has_ugc'])
        self.assertFalse(request_json['ignore_empty_email'])
        self.assertListEqual(request_json['attachments'], [])
        self.assertDictEqual(request_json['headers'], {
            'X-ID': '1',
            'X-GEOBASE': '2',
            'X-Form-ID': '1',
            'MESSAGE-ID': '<%s>' % self.message_id,
        })
        self.assertIsNotNone(request.headers['X-Sender-Real-User-IP'])

        self.assertEqual(response['status'], 'success')
        response = json.loads(response['response']['content'])
        self.assertEqual(response['result']['status'], 'OK')
        self.assertEqual(response['result']['message_id'], '<123@hostname>')
        self.assertEqual(response['result']['task_id'], '1234')

    @responses.activate
    def test_send__response(self):
        self.register_uri()
        response = self.do_service_action('send', context=self.simple_context)

        self.assertEqual(response['status'], 'success')
        response = json.loads(response['response']['content'])
        self.assertEqual(response['result']['status'], 'OK')
        self.assertIsNotNone(response['result']['message_id'])
        self.assertIsNotNone(response['result']['task_id'])

    @responses.activate
    def test_send__should_send_all_attachments(self):
        self.simple_context['attachments'] = [
            {
                'path': self.files['avatar']['path'],
                'filename': self.files['avatar']['filename'],
                'namespace': 'forms',
            },
        ]
        self.simple_context['static_attachments'] = [
            {
                'path': self.files['resume']['path'],
                'filename': self.files['resume']['filename'],
                'namespace': 'forms',
            },
        ]
        self.simple_context['attachment_templates'] = [
            {
                'type': 'txt',
                'name': 'template.txt',
                'content': '= Заголовок\n\nтекст',
            },
        ]

        self.register_uri()
        with patch.object(MdsStorage, '_fetch', self._fetcher):
            self.do_service_action('send', context=self.simple_context)  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertEqual(len(request_json['attachments']), 3)
        attachments = {
            attachment['filename']: attachment
            for attachment in request_json['attachments']
        }
        self.assertIn('ava.jpeg', attachments)
        self.assertEqual(attachments['ava.jpeg']['mime_type'], 'image/jpeg')

        self.assertIn('resume.txt', attachments)
        self.assertEqual(attachments['resume.txt']['mime_type'], 'text/plain;charset=utf-8')

        self.assertIn('template.txt', attachments)
        self.assertEqual(attachments['template.txt']['mime_type'], 'text/plain;charset=utf-8')
        self.assertEqual(base64.b64decode(attachments['template.txt']['data']).decode(), '= Заголовок\r\n\r\nтекст')

    @responses.activate
    def test_send__response_all_attachments(self):
        self.register_uri()
        self.simple_context['attachments'] = [
            {
                'path': self.files['avatar']['path'],
                'filename': self.files['avatar']['filename'],
                'namespace': 'forms',
            },
        ]
        self.simple_context['static_attachments'] = [
            {
                'path': self.files['resume']['path'],
                'filename': self.files['resume']['filename'],
                'namespace': 'forms',
            },
        ]
        self.simple_context['attachment_templates'] = [
            {
                'type': 'txt',
                'name': 'template.txt',
                'content': '= Заголовок\n\nтекст',
            },
        ]

        with patch.object(MdsStorage, '_fetch', self._fetcher):
            response = self.do_service_action('send', context=self.simple_context)

        self.assertEqual(response['status'], 'success')
        response = json.loads(response['response']['content'])
        self.assertEqual(response['result']['status'], 'OK')
        self.assertIsNotNone(response['result']['message_id'])
        self.assertIsNotNone(response['result']['task_id'])

    @override_settings(MAX_SENDER_ATTACHMENT_SIZE=10)
    @responses.activate
    def test_send__attachment_size_vaidation_error(self):
        self.register_uri()
        self.simple_context['attachment_templates'] = [
            {
                'type': 'txt',
                'name': 'template.txt',
                'content': '= Заголовок\n\nтекст',
            },
        ]

        with self.assertRaises(EmailMaxSizeExceededError):
            self.do_service_action('send', context=self.simple_context)

    def _fetcher(self, name):
        obj_name = name.split('_')[-1]
        return read_asset(os.path.join(self.get_files_path(), obj_name))

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_send__shouldnt_send_system_headers_business(self):
        self.register_uri()

        with patch('events.surveyme_integration.services.email.action_processors.EmailActionProcessor._check_form') as mock_check:
            mock_check.return_value = {
                'spam': False,
                'ban': False,
            }
            self.do_service_action('send', context=self.simple_context)  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertDictEqual(request_json['headers'], {
            'MESSAGE-ID': '<%s>' % self.message_id,
            'X-Form-ID': '1',
            'X-Yandex-CF-Receipt': '',
        })

    @responses.activate
    def test_send__should_raise_internal_sender_error(self):
        self.register_uri(status=500, result={
            'status': 'ERROR',
            'message': 'Internal error',
        })
        with self.assertRaises(EmailInternalError) as e:
            self.do_service_action('send', context=self.simple_context)  # BANG!

        self.assertRequestIsNotEmpty()
        self.assertIn(str(EmailInternalError.help), str(e.exception))

    @responses.activate
    def test_send__should_raise_compaign_not_exist_error(self):
        self.register_uri(status=404, result={
            'status': 'ERROR',
            'message': 'Compaign does not exist',
        })
        with self.assertRaises(EmailCompaignError) as e:
            self.do_service_action('send', context=self.simple_context)  # BANG!

        self.assertRequestIsNotEmpty()
        self.assertIn(str(EmailCompaignError.help), str(e.exception))

    @responses.activate
    def test_send__should_raise_validation_error(self):
        self.register_uri(status=400, result={
            'status': 'ERROR',
            'message': 'Bad request',
        })
        with self.assertRaises(EmailValidationError) as e:
            self.do_service_action('send', context=self.simple_context)  # BANG!

        self.assertRequestIsNotEmpty()
        self.assertIn(str(EmailValidationError.help), str(e.exception))

    @responses.activate
    def test_send__should_raise_email_to_validation_error(self):
        self.register_uri(status=400, result={
            'status': 'ERROR',
            'error': {
                'to': [
                    {'email': ['Invalid value']},
                ],
            },
        })
        with self.assertRaises(EmailValidationError) as e:
            self.do_service_action('send', context=self.simple_context)  # BANG!

        self.assertRequestIsNotEmpty()
        self.assertIn(str(EmailValidationError.help), str(e.exception))

    @responses.activate
    def test_send__should_raise_email_from_validation_error(self):
        self.register_uri(status=400, result={
            'status': 'ERROR',
            'error': {
                'from_email': ['Invalid value'],
            },
        })
        with self.assertRaises(EmailValidationError) as e:
            self.do_service_action('send', context=self.simple_context)  # BANG!

        self.assertRequestIsNotEmpty()
        self.assertIn(str(EmailValidationError.help), str(e.exception))

    @responses.activate
    def test_send__should_raise_field_validation_error(self):
        self.register_uri(status=400, result={
            'status': 'ERROR',
            'error': {
                'some_field': ['Invalid value'],
            },
        })
        with self.assertRaises(EmailValidationError) as e:
            self.do_service_action('send', context=self.simple_context)  # BANG!

        self.assertRequestIsNotEmpty()
        self.assertIn(str(EmailValidationError.help), str(e.exception))

    @responses.activate
    def test_send__should_raise_undefined_validation_error(self):
        self.register_uri(status=400, result={
            'status': 'ERROR',
            'error': {},
        })
        with self.assertRaises(EmailValidationError) as e:
            self.do_service_action('send', context=self.simple_context)  # BANG!

        self.assertRequestIsNotEmpty()
        self.assertIn(str(EmailValidationError.help), str(e.exception))

    @responses.activate
    def test_send__should_raise_spam_error(self):
        self.simple_context['spam_check'] = True
        self.register_uri()
        with patch('events.surveyme_integration.services.email.action_processors.EmailActionProcessor._check_form') as mock_check:
            mock_check.return_value = {
                'spam': True,
                'ban': False,
            }
            with self.assertRaises(EmailSpamError) as e:
                self.do_service_action('send', context=self.simple_context)  # BANG!

        mock_check.assert_called_once()
        self.assertRequestIsEmpty()
        self.assertIn(str(EmailSpamError.help), str(e.exception))

    @responses.activate
    def test_send__should_raise_spam_check_error(self):
        self.simple_context['spam_check'] = True
        self.register_uri()
        with patch('events.surveyme_integration.services.email.check_form.CheckFormV2.check') as mock_check:
            mock_check.return_value = None
            with self.assertRaises(EmailSpamCheckError) as e:
                self.do_service_action('send', context=self.simple_context)  # BANG!

        mock_check.assert_called_once()
        self.assertRequestIsEmpty()
        self.assertIn(str(EmailSpamCheckError.help), str(e.exception))

    @responses.activate
    def test_send__should_handle_readerror_from_mds(self):
        self.simple_context['attachments'] = [
            {
                'path': self.files['avatar']['path'],
                'filename': self.files['avatar']['filename'],
                'namespace': 'forms',
            },
        ]

        self.register_uri()
        with patch.object(MdsStorage, '_fetch') as mock_fetch:
            mock_fetch.side_effect = ReadError
            self.do_service_action('send', context=self.simple_context)  # BANG!

        self.assertRequestIsNotEmpty()
        request = responses.calls[0].request
        request_json = json.loads(request.body.decode())

        self.assertEqual(len(request_json['attachments']), 0)
