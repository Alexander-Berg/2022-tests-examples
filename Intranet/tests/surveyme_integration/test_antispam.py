# -*- coding: utf-8 -*-
import responses

from bson.objectid import ObjectId
from django.test import TestCase
from unittest.mock import patch

from events.accounts.factories import UserFactory
from events.common_app.helpers import MockResponse
from events.common_storages.factories import ProxyStorageModelFactory
from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyQuestionFactory,
)
from events.surveyme.models import AnswerType
from events.surveyme_integration.factories import (
    ServiceSurveyHookSubscriptionFactory,
    SurveyVariableFactory,
)
from events.surveyme_integration.services.email.check_form import CheckFormV2


class TestCheckForm(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.user = UserFactory()
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=3,  # email
            body='body',
            title='title',
            email_to_address='user@company.com',
            email_from_address='user@yandex.ru',
        )
        self.survey = self.subscription.survey_hook.survey
        self.survey.user = self.user
        self.survey.save()
        self.questions = [
            SurveyQuestionFactory(
                answer_type=AnswerType.objects.get(slug='answer_short_text'),
            ),
            SurveyQuestionFactory(
                answer_type=AnswerType.objects.get(slug='answer_files'),
            ),
        ]
        self.mds_file = ProxyStorageModelFactory(
            path='/123/45677890_hello.txt',
            original_name='hello.txt',
            file_size=123,
            sha256='123abc',
        )
        self.answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            source_request={
                'url': f'https://localhost/v1/surveys/{self.survey.pk}/form/',
                'request_id': '123',
                'ip': '127.0.0.1',
                'cookies': {'yandexuid': '14881488'},
            },
            data={
                'data': [
                    {
                        'question': self.questions[0].get_answer_info(),
                        'value': 'test text',
                    },
                    {
                        'question': self.questions[1].get_answer_info(),
                        'value': [{
                            'path': self.mds_file.path,
                        }],
                    },
                ],
            },
            user=self.user,
        )
        self.texts = {
            text.slug: text.get_value()
            for text in self.survey.texts.all()
        }

    @responses.activate
    def test_spam_check(self):
        responses.add(
            responses.POST,
            'http://checkform2-test.n.yandex-team.ru/check-json',
            json={'check': {'spam': True, 'ban': 'false', 'id': '*-3ebd-000123AF-160405866900000F'}},
        )

        self.subscription.body = 'XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X'
        self.subscription.title = 'spam message'
        self.subscription.email_from_address = 'devnull@yandex-team.ru'
        self.subscription.save()

        check_form = CheckFormV2(self.answer, self.subscription)
        check_result = check_form.check()

        self.assertTrue(check_result.get('spam'))
        self.assertFalse(check_result.get('ban'))

    @responses.activate
    def test_no_spam_check(self):
        responses.add(
            responses.POST,
            'http://checkform2-test.n.yandex-team.ru/check-json',
            json={'check': {'spam': False, 'ban': 'false', 'id': '*-3ebd-000123AF-160405866900000F'}},
        )

        self.subscription.body = 'Hello world'
        self.subscription.title = 'text message'
        self.subscription.email_from_address = 'devnull@yandex-team.ru'
        self.subscription.save()

        check_form = CheckFormV2(self.answer, self.subscription)
        check_result = check_form.check()

        self.assertFalse(check_result.get('spam'))
        self.assertFalse(check_result.get('ban'))

    def test_undef_spam_check(self):
        self.subscription.body = 'Hello world'
        self.subscription.title = ''
        self.subscription.email_from_address = 'devnull@yandex-team.ru'
        self.subscription.save()

        check_form = CheckFormV2(self.answer, self.subscription)
        with patch('events.surveyme_integration.services.email.check_form.requests_session.post') as mock_post:
            mock_post.return_value = MockResponse({}, 500)
            check_result = check_form.check()

        self.assertIsNone(check_result)

    def test_without_macros(self):
        self.survey.extra = {
            'redirect': {
                'enabled': True,
                'url': 'https://yandex.ru',
            },
        }
        self.survey.save()
        with patch('events.common_app.utils.requests_session.post') as fake_post:
            fake_post.return_value = MockResponse({
                'check': {
                    'spam': False,
                    'ban': 'false',
                },
            })
            check_form = CheckFormV2(self.answer, self.subscription)
            expected = {'spam': False, 'ban': False}
            self.assertDictEqual(check_form.check(), expected)
            params = fake_post.call_args_list[0][1]['json']
            self.assertListEqual(params['attachments'], [{
                'id': self.mds_file.path,
                'sha256': self.mds_file.sha256,
                'source': 'ugc',
            }])
            self.assertEqual(params['body'], self.subscription.body)
            self.assertEqual(params['body_template'], self.subscription.body)
            self.assertEqual(params['capture_type'], 'auto')
            self.assertEqual(params['client_email'], self.answer.user.email)
            self.assertEqual(params['client_ip'], self.answer.source_request['ip'])
            self.assertEqual(params['client_uid'], self.answer.user.uid)
            self.assertEqual(params['form_author'], self.survey.user.uid)
            self.assertEqual(params['form_fields'], {})
            self.assertEqual(params['form_id'], str(self.survey.pk))
            self.assertEqual(params['form_realpath'], self.answer.source_request['url'])
            self.assertEqual(params['form_recipients'], ['user@company.com'])
            self.assertEqual(params['form_type'], 'yandex')
            self.assertEqual(params['form_redir'], 'https://yandex.ru')
            self.assertEqual(params['form_body'], '%s\n%s' % (
                self.texts['successful_submission_title'],
                self.texts['successful_submission'],
            ))
            self.assertEqual(params['from'], self.subscription.email_from_address)
            self.assertEqual(params['from_template'], self.subscription.email_from_address)
            self.assertEqual(params['subject'], self.subscription.title)
            self.assertEqual(params['subject_template'], self.subscription.title)
            self.assertEqual(params['yandexuid'],  '14881488')

    def test_with_macros(self):
        form_id = SurveyVariableFactory(
            variable_id=str(ObjectId()),
            hook_subscription=self.subscription,
            var='form.id',
        )
        self.subscription.body = '{%s}' % form_id.variable_id
        self.subscription.save()

        with patch('events.common_app.utils.requests_session.post') as fake_post:
            fake_post.return_value = MockResponse({
                'check': {
                    'spam': False,
                    'ban': 'false',
                },
            })
            check_form = CheckFormV2(self.answer, self.subscription)
            expected = {'spam': False, 'ban': False}
            self.assertDictEqual(check_form.check(), expected)

        params = fake_post.call_args_list[0][1]['json']
        self.assertEqual(params['body'], str(self.survey.pk))
        self.assertEqual(params['body_template'], self.subscription.body)
        self.assertIsNone(params['form_redir'])

    def test_spam_ban_positive(self):
        with patch('events.common_app.utils.requests_session.post') as fake_post:
            fake_post.return_value = MockResponse({
                'check': {
                    'spam': True,
                    'ban': 'true',
                },
            })
            check_form = CheckFormV2(self.answer, self.subscription)
            expected = {'spam': True, 'ban': True}
            self.assertDictEqual(check_form.check(), expected)

    def test_spam_positive(self):
        with patch('events.common_app.utils.requests_session.post') as fake_post:
            fake_post.return_value = MockResponse({
                'check': {
                    'spam': True,
                    'ban': 'false',
                },
            })
            check_form = CheckFormV2(self.answer, self.subscription)
            expected = {'spam': True, 'ban': False}
            self.assertDictEqual(check_form.check(), expected)
