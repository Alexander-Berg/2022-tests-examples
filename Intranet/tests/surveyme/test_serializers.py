# -*- coding: utf-8 -*-
import pytz
import responses

from datetime import datetime
from django.test import TestCase, override_settings
from unittest.mock import patch

from events.common_app.helpers import override_cache_settings
from events.surveyme_integration.factories import ServiceSurveyHookSubscriptionFactory
from events.surveyme.fields.base.serializers import (
    RedirectSerializer,
    FooterSerializer,
    StatsSerializer,
    TeaserSerializer,
    QuizSerializer,
    DEFAULT_TIMEOUT_DELAY,
    MIN_TIMEOUT_DELAY,
)
from events.surveyme.api_admin.v2.serializers import (
    WikiSubscriptionDataSerializer,
    StartrekSubscriptionDataSerializer,
    ServiceSurveyHookSubscriptionSerializer,
    ExportAnswersSerializer,
)
from events.accounts.factories import UserFactory
from events.media.factories import ImageFactory
from events.surveyme.factories import SurveyFactory
from events.surveyme.models import SurveyQuestion, AnswerType, Survey
from events.surveyme_integration.exceptions import (
    WIKI_NOT_VALID_SUPERTAG_MESSAGE,
    WIKI_PAGE_DOESNT_EXISTS_MESSAGE,
    EMAIL_INCORRECT_VARIABLE_TYPE_MESSAGE,
    EMAIL_INCORRECT_QUESTION_TYPE_MESSAGE,
    EMAIL_INCORRECT_EMAIL_MESSAGE,
    EMAIL_INCORRECT_FROM_TITLE,
    EMPTY_VALUE_EXCEPTION_MESSAGE,
    STARTREK_INCORRECT_QUEUE_MESSAGE,
    STARTREK_INCORRECT_PARENT_MESSAGE,
    STARTREK_QUEUE_AND_PARENT_EMPTY_MESSAGE,
    STARTREK_QUEUE_NOT_EXIST_MESSAGE,
    STARTREK_PARENT_NOT_EXIST_MESSAGE,
    STARTREK_TYPE_EMPTY_MESSAGE,
    STARTREK_TYPE_NOT_EXIST_MESSAGE,
    STARTREK_PRIORITY_EMPTY_MESSAGE,
    STARTREK_PRIORITY_NOT_EXIST_MESSAGE,
    STARTREK_TITLE_EMPTY_MESSAGE,
    NO_SUCH_VARIABLE_MESSAGE,
)


QUEUES_FORMS_RESPONSE = {  # {{{
    'id': 89,
    'key': 'FORMS',
    'name': 'Конструктор форм',
    'description': '',
    'defaultType': {
        'id': '2',
        'key': 'task',
        'display': 'Задача',
    },
    'defaultPriority': {
        'id': '2',
        'key': 'normal',
        'display': 'Средний',
    },
}  # }}}
QUEUES_FORMS_ISSUETYPES_RESPONSE = [  # {{{
    {
        'id': 7,
        'key': 'refactoring',
        'name': 'Рефакторинг',
    },
    {
        'id': 2,
        'key': 'task',
        'name': 'Задача',
        'description': 'A task that needs to be done.',
    },
    {
        'id': 3,
        'key': 'newFeature',
        'name': 'Новая возможность',
    },
    {
        'id': 4,
        'key': 'improvement',
        'name': 'Улучшение',
        'description': 'An improvement or enhancement to an existing feature or task.',
    },
    {
        'id': 1,
        'key': 'bug',
        'name': 'Ошибка',
    },
]  # }}}
PRIORITIES_RESPONSE = [  # {{{
    {
        'id': 5,
        'key': 'blocker',
        'name': 'Блокер',
        'order': 5,
    },
    {
        'id': 4,
        'key': 'critical',
        'name': 'Критичный',
        'order': 4,
    },
    {
        'id': 3,
        'key': 'minor',
        'name': 'Низкий',
        'order': 2,
    },
    {
        'id': 2,
        'key': 'normal',
        'name': 'Средний',
        'order': 3,
    },
    {
        'id': 1,
        'key': 'trivial',
        'name': 'Незначительный',
        'order': 1,
    },
]  # }}}
ISSUES_FORMS_42_RESPONSE = {  # {{{
    'id': '53fc3b6de4b00ed34ab22aac',
    'key': 'FORMS-42',
    'summary': 'Ручки для настройки интеграции из бэкофиса',
    'type': {
        'id': '2',
        'key': 'task',
        'display': 'Задача'
    },
    'priority': {
        'id': '2',
        'key': 'normal',
        'display': 'Средний'
    },
    'assignee': {
        'id': 'web-chib',
        'display': 'Геннадий Чибисов'
    },
    'queue': {
        'id': '89',
        'key': 'FORMS',
        'display': 'Конструктор форм'
    },
    'status': {
        'id': '3',
        'key': 'closed',
        'display': 'Закрыт'
    },
    'parent': {
        'id': '53d9f696e4b0355d283173f6',
        'key': 'FORMS-29',
        'display': 'Рефакторинг интеграции'
    },
}  # }}}
ISSUES_TEST_775_RESPONSE = {  # {{{
    'id': '54fc3b6de4b00ed34ab22aac',
    'key': 'TEST-775',
    'summary': 'test 775',
    'type': {
        'id': '2',
        'key': 'task',
        'display': 'Задача'
    },
    'priority': {
        'id': '2',
        'key': 'normal',
        'display': 'Средний'
    },
    'assignee': {
        'id': 'testit',
        'display': 'Test it'
    },
    'queue': {
        'id': '8',
        'key': 'TEST',
        'display': 'Test'
    },
    'status': {
        'id': '3',
        'key': 'closed',
        'display': 'Закрыт'
    },
}  # }}}
FIELDS_LOCAL_TAGGING = {  # {{{
    'id': '01234--tagging',
    'key': 'tagging',
    'name': 'Tagging',
    'schema': {
        'type': 'string',
    },
    'queue': {
        'key': 'FORMS',
    },
}  # }}}
FIELDS_SYSTEM_TAGS = {  # {{{
    'id': 'tags',
    'key': 'tags',
    'name': 'Tags',
    'schema': {
        'type': 'array',
        'items': 'string',
    },
}  # }}}


class TestWikiSubscriptionDataSerializer(TestCase):
    fixtures = ['initial_data.json']
    serializer = WikiSubscriptionDataSerializer

    def setUp(self):
        self.wiki_subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=9,  # wiki
            title='Title goes here',
            body='Body goes here',
        )

        self.data = {
            'supertag': 'test/me',
            'text': 'some text',
            'id': 3,
            'subscription': self.wiki_subscription.id,

        }

        self.serializer_instance = self.serializer(data=self.data)

    @responses.activate
    def test_wiki_subscription_serializer_success(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/test/me'
        responses.add(responses.HEAD, url, body='')

        self.assertTrue(self.serializer_instance.is_valid())

        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_wiki_subscription_serializer_full_url_success(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/test/me'
        responses.add(responses.HEAD, url, body='')
        self.data['supertag'] = 'https://wiki.test.yandex-team.ru/test/me/'
        self.serializer_instance = self.serializer(data=self.data)

        self.assertTrue(self.serializer_instance.is_valid())

        self.assertEqual(self.serializer_instance.data['supertag'], 'test/me')
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_wiki_subscription_serializer_full_url_with_ancor_success(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/test/me'
        responses.add(responses.HEAD, url, body='')
        self.data['supertag'] = 'https://wiki.test.yandex-team.ru/test/me/#content'
        self.serializer_instance = self.serializer(data=self.data)

        self.assertTrue(self.serializer_instance.is_valid())

        self.assertEqual(self.serializer_instance.data['supertag'], 'test/me#content')
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_wiki_subscription_serializer_fail_not_found(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/test/me'
        responses.add(responses.HEAD, url, body='', status=404)

        self.assertFalse(self.serializer_instance.is_valid())

        expected = {'supertag': [WIKI_PAGE_DOESNT_EXISTS_MESSAGE % self.data['supertag']]}
        self.assertDictEqual(self.serializer_instance.errors, expected)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_wiki_subscription_serializer_fail_no_access(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/test/me'
        responses.add(responses.HEAD, url, body='', status=403)
        user = UserFactory()
        survey = SurveyFactory(user=user)
        self.serializer_instance = self.serializer(data=self.data, context={'survey': survey})

        self.assertFalse(self.serializer_instance.is_valid())

        expected = {'supertag': [WIKI_PAGE_DOESNT_EXISTS_MESSAGE % 'test/me']}
        self.assertDictEqual(self.serializer_instance.errors, expected)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_wiki_subscription_serializer_fail_not_valid_tag(self):
        self.data['supertag'] = '!/test/smth'
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        expected = {'supertag': [WIKI_NOT_VALID_SUPERTAG_MESSAGE % self.data['supertag']]}
        self.assertDictEqual(self.serializer_instance.errors, expected)
        self.assertEqual(len(responses.calls), 0)

    @responses.activate
    def test_wiki_subscription_serializer_fail_empty_value(self):
        self.data['supertag'] = ''
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        expected = {'supertag': [EMPTY_VALUE_EXCEPTION_MESSAGE]}
        self.assertDictEqual(self.serializer_instance.errors, expected)
        self.assertEqual(len(responses.calls), 0)

    @responses.activate
    def test_wiki_subscription_serializer_fail_supertag_not_parse(self):
        self.data['supertag'] = '///'
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        expected = {'supertag': [WIKI_NOT_VALID_SUPERTAG_MESSAGE % self.data['supertag']]}
        self.assertDictEqual(self.serializer_instance.errors, expected)
        self.assertEqual(len(responses.calls), 0)

    @responses.activate
    def test_wiki_subscription_serializer_fail_bad_response(self):
        url = 'https://wiki-api.test.yandex-team.ru/_api/frontend/test/me'
        responses.add(responses.HEAD, url, body='', status=503)

        self.assertFalse(self.serializer_instance.is_valid())

        expected = {'supertag': [WIKI_PAGE_DOESNT_EXISTS_MESSAGE % 'test/me']}
        self.assertDictEqual(self.serializer_instance.errors, expected)
        self.assertEqual(len(responses.calls), 1)


class TestStartrekSubscriptionDataSerializer(TestCase):
    fixtures = ['initial_data.json']
    serializer = StartrekSubscriptionDataSerializer

    def setUp(self):
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=7,  # startrek/create ticket
            title='Title goes here',
            body='Body goes here',
        )

        self.data = {
            'id': 4424,
            'queue': 'FORMS',
            'type': 2,
            'priority': 2,
            'subscription': self.subscription.pk,
        }

        self.serializer_instance = self.serializer(data=self.data)

    def register_uri(self):
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/queues/FORMS/',
            json=QUEUES_FORMS_RESPONSE,
            status=200,
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/issues/FORMS-42/',
            json=ISSUES_FORMS_42_RESPONSE,
            status=200,
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/issues/TEST-775/',
            json=ISSUES_TEST_775_RESPONSE,
            status=200,
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/queues/FORMS/issuetypes/',
            json=QUEUES_FORMS_ISSUETYPES_RESPONSE,
            status=200,
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/priorities/',
            json=PRIORITIES_RESPONSE,
            status=200,
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/fields/tags/',
            json=FIELDS_SYSTEM_TAGS,
            status=200,
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/localFields/01234--tagging/',
            json=FIELDS_LOCAL_TAGGING,
            status=200,
        )

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_serializer_success(self):
        self.register_uri()

        self.assertTrue(self.serializer_instance.is_valid())

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_serializer_parent_success(self):
        self.register_uri()
        self.data['parent'] = 'FORMS-42'

        self.assertTrue(self.serializer_instance.is_valid())

        data = self.serializer_instance.validated_data
        self.assertEqual(data['parent'], 'FORMS-42')
        self.assertEqual(data['queue'], 'FORMS')

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_serializer_parent_with_queue_success(self):
        self.register_uri()
        self.data['parent'] = 'TEST-775'
        self.data['queue'] = 'FORMS'

        self.assertTrue(self.serializer_instance.is_valid())

        data = self.serializer_instance.validated_data
        self.assertEqual(data['parent'], 'TEST-775')
        self.assertEqual(data['queue'], 'FORMS')

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_incorrect_queue(self):
        self.register_uri()
        self.data['queue'] = 'MY QUEUE'
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('queue', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['queue'], [
            STARTREK_INCORRECT_QUEUE_MESSAGE
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_incorrect_parent(self):
        self.register_uri()
        self.data['parent'] = 'FORMS-NUMBER'
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('parent', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['parent'], [
            STARTREK_INCORRECT_PARENT_MESSAGE
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_queue_and_parent_empty(self):
        self.register_uri()
        self.data['queue'] = ''
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('queue', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['queue'], [
            STARTREK_QUEUE_AND_PARENT_EMPTY_MESSAGE
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_queue_not_exist(self):
        self.register_uri()
        self.data['queue'] = 'FORMZ'
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('queue', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['queue'], [
            STARTREK_QUEUE_NOT_EXIST_MESSAGE % self.data['queue']
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_parent_not_exist(self):
        self.register_uri()
        self.data['parent'] = 'FORMS-4200'
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('parent', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['parent'], [
            STARTREK_PARENT_NOT_EXIST_MESSAGE % self.data['parent']
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_type_empty(self):
        self.register_uri()
        self.data['type'] = None
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('type', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['type'], [
            STARTREK_TYPE_EMPTY_MESSAGE
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_type_not_exist(self):
        self.register_uri()
        self.data['type'] = 22
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('type', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['type'], [
            STARTREK_TYPE_NOT_EXIST_MESSAGE
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_priority_empty(self):
        self.register_uri()
        self.data['priority'] = None
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('priority', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['priority'], [
            STARTREK_PRIORITY_EMPTY_MESSAGE
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_priority_not_exist(self):
        self.register_uri()
        self.data['priority'] = 22
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('priority', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['priority'], [
            STARTREK_PRIORITY_NOT_EXIST_MESSAGE
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_type_and_priority_empty(self):
        self.register_uri()
        self.data['type'] = None
        self.data['priority'] = None
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('type', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['type'], [
            STARTREK_TYPE_EMPTY_MESSAGE
        ])
        self.assertIn('priority', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['priority'], [
            STARTREK_PRIORITY_EMPTY_MESSAGE
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_type_and_priority_not_exist(self):
        self.register_uri()
        self.data['type'] = 22
        self.data['priority'] = 22
        self.serializer_instance = self.serializer(data=self.data)

        self.assertFalse(self.serializer_instance.is_valid())

        self.assertIn('type', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['type'], [
            STARTREK_TYPE_NOT_EXIST_MESSAGE
        ])
        self.assertIn('priority', self.serializer_instance.errors)
        self.assertEqual(self.serializer_instance.errors['priority'], [
            STARTREK_PRIORITY_NOT_EXIST_MESSAGE
        ])

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_system_and_local_fields_exist(self):
        self.register_uri()
        self.data['fields'] = [
            {
                'key': {'slug': 'tags'},
                'value': '1',
            },
            {
                'key': {'id': '01234--tagging'},
                'value': '2',
            },
        ]
        self.serializer_instance = self.serializer(data=self.data)
        self.assertTrue(self.serializer_instance.is_valid())
        self.assertEqual(self.serializer_instance.validated_data['fields'][0], {
            'key': {
                'id': 'tags',
                'slug': 'tags',
                'name': 'Tags',
                'type': 'array/string',
            },
            'value': '1',
        })
        self.assertEqual(self.serializer_instance.validated_data['fields'][1], {
            'key': {
                'id': '01234--tagging',
                'slug': 'tagging',
                'name': 'Tagging',
                'type': 'string',
            },
            'value': '2',
        })

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_system_field_not_exist(self):
        self.register_uri()
        field_id = 'tagger'
        responses.add(
            responses.GET,
            f'https://st-api.test.yandex-team.ru/service/fields/{field_id}/',
            status=404,
        )
        self.data['fields'] = [
            {
                'key': {'slug': field_id},
                'value': '1',
            },
        ]
        self.serializer_instance = self.serializer(data=self.data)
        self.assertFalse(self.serializer_instance.is_valid())

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_local_field_not_exist(self):
        self.register_uri()
        field_id = '01234--tagger'
        responses.add(
            responses.GET,
            f'https://st-api.test.yandex-team.ru/v2/localFields/{field_id}/',
            status=404,
        )
        self.data['fields'] = [
            {
                'key': {'id': field_id},
                'value': '1',
            },
        ]
        self.serializer_instance = self.serializer(data=self.data)
        self.assertFalse(self.serializer_instance.is_valid())

    @override_cache_settings()
    @responses.activate
    def test_startrek_subscription_local_field_from_incorrect_queue(self):
        self.register_uri()
        field_id = '01234--tagger'
        response_json = {
            'id': field_id,
            'key': 'tagger',
            'name': 'Tagger',
            'schema': {
                'type': 'string',
            },
            'queue': {
                'key': 'TESTS',
            },
        }
        responses.add(
            responses.GET,
            f'https://st-api.test.yandex-team.ru/v2/localFields/{field_id}/',
            json=response_json,
            status=200,
        )
        self.data['fields'] = [
            {
                'key': {'id': field_id},
                'value': '1',
            },
        ]
        self.serializer_instance = self.serializer(data=self.data)
        self.assertFalse(self.serializer_instance.is_valid())

    @override_cache_settings()
    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_startrek_subscription_field_exist_for_biz(self):
        self.register_uri()
        field_id = 'tagger'
        response_json = {
            'id': field_id,
            'key': 'tagger',
            'name': 'Tagger',
            'schema': {
                'type': 'string',
            },
        }
        responses.add(
            responses.GET,
            f'https://st-api.test.yandex-team.ru/service/fields/{field_id}/',
            json=response_json,
            status=200,
        )
        self.data['fields'] = [
            {
                'key': {'slug': field_id},
                'value': '1',
            },
        ]
        with patch('events.common_app.startrek.client.get_robot_tracker') as mock_robot_tracker:
            mock_robot_tracker.return_value = '321'
            self.serializer_instance = self.serializer(data=self.data)
            self.assertTrue(self.serializer_instance.is_valid())

    @responses.activate
    @override_settings(IS_BUSINESS_SITE=True)
    def test_startrek_subscription_field_not_exist_for_biz(self):
        self.register_uri()
        field_id = 'tagger'
        response_json = {
            'id': field_id,
            'key': 'tagger',
            'name': 'Tagger',
            'schema': {
                'type': 'string',
            },
        }
        responses.add(
            responses.GET,
            f'https://st-api.test.yandex-team.ru/service/fields/{field_id}/',
            json=response_json,
            status=200,
        )
        self.data['fields'] = [
            {
                'key': {'slug': 'taggerNew'},
                'value': '1',
            },
        ]
        with patch('events.common_app.startrek.client.get_robot_tracker') as mock_robot_tracker:
            mock_robot_tracker.return_value = '321'
            self.serializer_instance = self.serializer(data=self.data)
            self.assertFalse(self.serializer_instance.is_valid())


class TestServiceSurveyHookSubscriptionSerializer(TestCase):
    fixtures = ['initial_data.json']
    serializer = ServiceSurveyHookSubscriptionSerializer

    def setUp(self):
        self.subscription = ServiceSurveyHookSubscriptionFactory()
        self.survey = self.subscription.survey_hook.survey

    def register_uri(self):
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/queues/FORMS/',
            json=QUEUES_FORMS_RESPONSE,
            status=200,
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/queues/FORMS/issuetypes/',
            json=QUEUES_FORMS_ISSUETYPES_RESPONSE,
            status=200,
        )
        responses.add(
            responses.GET,
            'https://st-api.test.yandex-team.ru/service/priorities/',
            json=PRIORITIES_RESPONSE,
            status=200,
        )

    @responses.activate
    def test_sshs_serializer_startrek_success(self):
        self.register_uri()
        data = {
            'service_type_action': 7,  # startrek/create ticket
            'title': 'Title goes here',
            'body': 'Body goes here',
            'startrek': {
                'queue': 'FORMS',
                'type': 2,
                'priority': 2,
            },
            'variables': {},
        }
        serializer_instance = self.serializer(data=data, instance=self.subscription, partial=True)

        self.assertTrue(serializer_instance.is_valid())

        self.assertEqual(len(responses.calls), 3)

    @responses.activate
    def test_sshs_serializer_startrek_title_is_empty(self):
        self.register_uri()
        data = {
            'service_type_action': 7,  # startrek/create ticket
            'title': '',
            'body': 'Body goes here',
            'startrek': {
                'queue': 'FORMS',
                'type': 2,
                'priority': 2,
            },
            'variables': {},
        }
        serializer_instance = self.serializer(data=data, instance=self.subscription, partial=True)

        self.assertFalse(serializer_instance.is_valid())

        self.assertEqual(serializer_instance.errors['title'], [
            STARTREK_TITLE_EMPTY_MESSAGE,
        ])

    @responses.activate
    def test_sshs_serializer_startrek_body_can_be_empty(self):
        self.register_uri()
        data = {
            'service_type_action': 7,  # startrek/create ticket
            'title': 'Title goes here',
            'body': '',
            'startrek': {
                'queue': 'FORMS',
                'type': 2,
                'priority': 2,
            },
            'variables': {},
        }
        serializer_instance = self.serializer(data=data, instance=self.subscription, partial=True)

        self.assertTrue(serializer_instance.is_valid())

    def test_sshs_serializer_http_success(self):
        data = {
            'service_type_action': 4,  # http/post
            'title': '',
            'body': '',
            'http_url': 'https://yandex.ru/',
            'variables': {},
        }
        serializer_instance = self.serializer(data=data, instance=self.subscription, partial=True)
        self.assertTrue(serializer_instance.is_valid())

    def test_sshs_serializer_arbitrary_success_intranet(self):
        data = {
            'service_type_action': 11,  # http/arbitrary
            'title': '',
            'body': '',
            'http_url': 'https://yandex.ru/',
            'http_method': 'post',
            'variables': {},
            'headers': [
                {
                    'name': 'X-One',
                    'value': '1',
                    'add_only_with_value': False,
                },
            ],
        }
        serializer_instance = self.serializer(data=data, instance=self.subscription, partial=True)
        self.assertTrue(serializer_instance.is_valid())
        headers = {
            header['name']: header['value']
            for header in serializer_instance.validated_data['headers']
        }
        self.assertTrue('X-One' in headers)
        self.assertEqual(headers['X-One'], '1')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_sshs_serializer_arbitrary_success_business_1(self):
        data = {
            'service_type_action': 11,  # http/arbitrary
            'title': '',
            'body': '',
            'http_url': 'https://yandex.ru/',
            'http_method': 'post',
            'variables': {},
            'headers': [
                {
                    'name': 'X-One',
                    'value': '1',
                    'add_only_with_value': False,
                },
            ],
        }
        serializer_instance = self.serializer(data=data, instance=self.subscription, partial=True)
        self.assertTrue(serializer_instance.is_valid())
        headers = {
            header['name']: header['value']
            for header in serializer_instance.validated_data['headers']
        }
        self.assertTrue('X-One' in headers)
        self.assertEqual(headers['X-One'], '1')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_sshs_serializer_arbitrary_success_business_2(self):
        data = {
            'service_type_action': 11,  # http/arbitrary
            'title': '',
            'body': '',
            'http_url': 'https://api.tracker.yandex.net/',
            'http_method': 'post',
            'variables': {},
            'headers': [
                {
                    'name': 'X-One',
                    'value': '1',
                    'add_only_with_value': False,
                },
            ],
        }
        serializer_instance = self.serializer(data=data, instance=self.subscription, partial=True)
        self.assertTrue(serializer_instance.is_valid())
        headers = {
            header['name']: header['value']
            for header in serializer_instance.validated_data['headers']
        }
        self.assertTrue('X-One' in headers)
        self.assertEqual(headers['X-One'], '1')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_sshs_serializer_arbitrary_failure_business_1(self):
        data = {
            'service_type_action': 11,  # http/arbitrary
            'title': '',
            'body': '',
            'http_url': 'https://api.yandex.net/',
            'http_method': 'post',
            'variables': {},
            'headers': [
                {
                    'name': 'X-One',
                    'value': '1',
                    'add_only_with_value': False,
                },
            ],
        }
        serializer_instance = self.serializer(data=data, instance=self.subscription, partial=True)
        self.assertFalse(serializer_instance.is_valid())

    def test_sshs_serializer_email_success_intranet(self):
        data = {
            'service_type_action': 3,  # email/send
            'title': 'test',
            'body': 'test',
            'email_to_address': 'user@example.com',
            'http_method': 'post',
            'variables': {},
            'headers': [
                {
                    'name': 'X-One',
                    'value': '1',
                    'add_only_with_value': False,
                },
                {
                    'name': 'Reply-To',
                    'value': 'admin@example.com',
                    'add_only_with_value': False,
                },
            ],
        }
        serializer_instance = self.serializer(data=data, instance=self.subscription, partial=True)
        self.assertTrue(serializer_instance.is_valid())
        headers = {
            header['name']: header['value']
            for header in serializer_instance.validated_data['headers']
        }
        self.assertTrue('X-One' in headers)
        self.assertEqual(headers['X-One'], '1')
        self.assertTrue('Reply-To' in headers)
        self.assertEqual(headers['Reply-To'], 'admin@example.com')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_sshs_serializer_email_success_business(self):
        data = {
            'service_type_action': 3,  # email/send
            'title': 'test',
            'body': 'test',
            'email_to_address': 'user@example.com',
            'http_method': 'post',
            'variables': {},
            'headers': [
                {
                    'name': 'X-One',
                    'value': '1',
                    'add_only_with_value': False,
                },
                {
                    'name': 'Reply-To',
                    'value': 'admin@example.com',
                    'add_only_with_value': False,
                },
            ],
        }
        serializer_instance = self.serializer(data=data, instance=self.subscription,
                                              partial=True, context={'survey': self.survey})
        self.assertTrue(serializer_instance.is_valid())
        headers = {
            header['name']: header['value']
            for header in serializer_instance.validated_data['headers']
        }
        self.assertFalse('X-One' in headers)
        self.assertTrue('Reply-To' in headers)
        self.assertEqual(headers['Reply-To'], 'admin@example.com')


class TestEmailSubscriptionDataSerializer(TestCase):
    serializer = ServiceSurveyHookSubscriptionSerializer

    def setUp(self):
        self.survey = SurveyFactory()

    def get_serializer(self):
        return self.serializer(context={'survey': self.survey})

    def test_email_to_addresss_empty(self):
        errors = self.get_serializer()._validate_email({
        })
        self.assertEqual(
            errors,
            {'email_to_address': [EMPTY_VALUE_EXCEPTION_MESSAGE]}
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_email_to_address_incorrect_email_b2b(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'topor@yandex-team.ru;troll@mailru,gleb@gmail.com',
            'surveyvariable_set': [],
        })
        self.assertEqual(
            errors,
            {'email_to_address': [EMAIL_INCORRECT_EMAIL_MESSAGE % {'email': 'troll@mailru'}]}
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_email_to_address_no_such_variable(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'topor@yandex-team.ru,{5aa8fc207de3dc9630b93ba3};{5aa8fc207de3dc9630b93ba5}',
            'surveyvariable_set': [
                {'variable_id': '5aa8fc207de3dc9630b93ba3', 'var': 'user.email'},
            ],
        })
        self.assertEqual(
            errors,
            {'email_to_address': [NO_SUCH_VARIABLE_MESSAGE % {'var_id': '5aa8fc207de3dc9630b93ba5'}]}
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_email_to_address_incorrect_variable_type(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': '{5aa8fc207de3dc9630b93ba3},{5aa8fc207de3dc9630b93ba5}',
            'surveyvariable_set': [
                {'variable_id': '5aa8fc207de3dc9630b93ba3', 'var': 'user.email'},
                {'variable_id': '5aa8fc207de3dc9630b93ba5', 'var': 'user.gender'}
            ],
        })
        self.assertEqual(
            errors,
            {'email_to_address': [EMAIL_INCORRECT_VARIABLE_TYPE_MESSAGE % {'var_id': '5aa8fc207de3dc9630b93ba5'}]}
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_validate_dir_staff_meta_email(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': '{5aa8fc207de3dc9630b93ba3},{5aa8fc207de3dc9630b93ba5}',
            'surveyvariable_set': [
                {'variable_id': '5aa8fc207de3dc9630b93ba3', 'var': 'dir_staff.meta_user', 'format_name': 'dir_staff.email'},
                {'variable_id': '5aa8fc207de3dc9630b93ba5', 'var': 'dir_staff.meta_question', 'format_name': 'dir_staff.email'}
            ],
        })
        self.assertDictEqual(errors, {})

    @override_settings(IS_BUSINESS_SITE=True)
    def test_email_to_address_no_such_question(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': '{5aa8fc207de3dc9630b93ba3},{5aa8fc207de3dc9630b93ba5}',
            'surveyvariable_set': [
                {'variable_id': '5aa8fc207de3dc9630b93ba3',
                 'var': 'form.question_answer', 'arguments': {'question': 42}},
                {'variable_id': '5aa8fc207de3dc9630b93ba5', 'var': 'user.email'}
            ],
        })
        self.assertEqual(
            errors,
            {'email_to_address': ['No such question']}
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_email_to_address_incorrect_question_type(self):
        answer_type = AnswerType.objects.create(slug='answer_long_text', kind='generic')
        survey = Survey.objects.create()
        survey_question = SurveyQuestion.objects.create(answer_type=answer_type, survey=survey)

        errors = self.get_serializer()._validate_email({
            'email_to_address': '{5aa8fc207de3dc9630b93ba3},{5aa8fc207de3dc9630b93ba5}',
            'surveyvariable_set': [
                {'variable_id': '5aa8fc207de3dc9630b93ba3',
                 'var': 'form.question_answer', 'arguments': {'question': survey_question.id}},
                {'variable_id': '5aa8fc207de3dc9630b93ba5', 'var': 'user.email'}
            ],
        })
        self.assertEqual(
            errors,
            {'email_to_address': [EMAIL_INCORRECT_QUESTION_TYPE_MESSAGE % {'var_id': '5aa8fc207de3dc9630b93ba3'}]}
        )

    def test_email_to_address_ok(self):
        answer_type_1 = AnswerType.objects.create(slug='param_subscribed_email', kind='profile')
        survey_1 = Survey.objects.create()
        survey_question_1 = SurveyQuestion.objects.create(answer_type=answer_type_1, survey=survey_1)

        answer_type_2 = AnswerType.objects.create(slug='answer_non_profile_email', kind='profile')
        survey_2 = Survey.objects.create()
        survey_question_2 = SurveyQuestion.objects.create(answer_type=answer_type_2, survey=survey_2)

        errors = self.get_serializer()._validate_email({
            'email_to_address': 'topor@yandex-team.ru;{5aa8fc207de3dc9630b93ba3},nosik@gmail.com;'
                                '{5aa8fc207de3dc9630b93ba5},{5aa8fc207de3dc9630b93ba7},{5aa8fc207de3dc9630b93ba9}',
            'surveyvariable_set': [
                {'variable_id': '5aa8fc207de3dc9630b93ba3',
                 'var': 'form.question_answer', 'arguments': {'question': survey_question_1.id}},
                {'variable_id': '5aa8fc207de3dc9630b93ba5',
                 'var': 'form.question_answer', 'arguments': {'question': survey_question_2.id}},
                {'variable_id': '5aa8fc207de3dc9630b93ba7', 'var': 'user.email'},
                {'variable_id': '5aa8fc207de3dc9630b93ba9', 'var': 'request.query_param'},
            ],
        })
        self.assertEqual(
            errors,
            {}
        )

    def test_email_from_title_correct_field(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'user@domain.com',
            'email_from_title': u'Мое имя',
        })
        self.assertEqual(errors, {})

    def test_email_from_title_correct_field_more_complex(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'user@domain.com',
            'email_from_title': u'He1-1o При_вет',
        })
        self.assertEqual(errors, {})

    def test_email_from_title_empty_field(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'user@domain.com',
            'email_from_title': '',
        })
        self.assertEqual(errors, {})

    def test_email_from_title_null_field(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'user@domain.com',
            'email_from_title': None,
        })
        self.assertEqual(errors, {})

    def test_email_from_title_field_with_variable(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': '{5c3f13d959bad70297cf7ce8}',
            'email_from_title': None,
        })
        self.assertEqual(errors, {})

    def test_email_from_title_incorrect_long_field(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'user@domain.com',
            'email_from_title': 'My Name <name@domain.com>',
        })
        self.assertEqual(errors, {'email_from_title': [EMAIL_INCORRECT_FROM_TITLE]})

    def test_email_from_title_incorrect_short_field(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'user@domain.com',
            'email_from_title': 'name@domain.com',
        })
        self.assertEqual(errors, {'email_from_title': [EMAIL_INCORRECT_FROM_TITLE]})

    def test_email_from_title_incorrect_short_field_with_sharp(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'user@domain.com',
            'email_from_title': 'name#domain',
        })
        self.assertEqual(errors, {'email_from_title': [EMAIL_INCORRECT_FROM_TITLE]})

    def test_email_from_title_incorrect_short_field_with_persent(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'user@domain.com',
            'email_from_title': 'name%domain',
        })
        self.assertEqual(errors, {'email_from_title': [EMAIL_INCORRECT_FROM_TITLE]})

    @override_settings(IS_BUSINESS_SITE=True)
    def test_email_from_address_devnull_b2b(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'topor@yandex-team.ru, gleb@gmail.com',
            'email_from_address': 'devnull@forms-mailer.yaconnect.com',
            'surveyvariable_set': [],
        })
        self.assertEqual(errors, {})

    @override_settings(IS_BUSINESS_SITE=True)
    def test_email_from_address_unknown_b2b(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'topor@yandex-team.ru,gleb@gmail.com',
            'email_from_address': 'borodonog@yahoo.com',
            'surveyvariable_set': [],
        })
        self.assertEqual(errors, {})

    # Заголовок Reply-To валидируется так же, как и email_to_address,
    # не будем повторять все тесты, а только несколько.

    @override_settings(IS_BUSINESS_SITE=True)
    def test_header_reply_to_incorrect_email_b2b(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'topor@yandex-team.ru',
            'headers': [
                {'name': 'Reply-To', 'value': 'topor@yandex-team.ru;gleb@yahoo.gleb'},
                {'name': 'Some-Header', 'value': 'foo'},
                {'name': 'REPLY-TO', 'value': 'razvoz%mail.ru,glupyshka@yandex.ru'},
            ],
            'surveyvariable_set': [],
        })
        self.assertEqual(
            errors,
            {'email_reply_to': [EMAIL_INCORRECT_EMAIL_MESSAGE % {'email': 'razvoz%mail.ru'}]}
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_header_reply_to_incorrect_variable_type(self):
        errors = self.get_serializer()._validate_email({
            'email_to_address': 'topor@yandex-team.ru',
            'surveyvariable_set': [
                {'variable_id': '5aa8fc207de3dc9630b93ba3', 'var': 'user.email'},
                {'variable_id': '5aa8fc207de3dc9630b93ba5', 'var': 'user.gender'},
                {'variable_id': '5aa8fc207de3dc9630b93ba7', 'var': 'request.query_param'},
            ],
            'headers': [
                {'name': 'Reply-To', 'value': '{5aa8fc207de3dc9630b93ba3},{5aa8fc207de3dc9630b93ba5}'},
                {'name': 'Some-Header', 'value': 'foo'},
                {'name': 'REPLY-TO', 'value': '{5aa8fc207de3dc9630b93ba3};{5aa8fc207de3dc9630b93ba7}'},
            ]
        })
        self.assertEqual(
            errors,
            {'email_reply_to': [EMAIL_INCORRECT_VARIABLE_TYPE_MESSAGE % {'var_id': '5aa8fc207de3dc9630b93ba5'}]}
        )

    def test_email_headers_for_intranet(self):
        data = {
            'email_to_address': 'user@example.com',
            'headers': [
                {'name': 'Reply-To', 'value': 'admin@example.com'},
                {'name': 'Some-Header', 'value': 'foo'},
            ]
        }
        errors = self.get_serializer()._validate_email(data)
        self.assertEqual(errors, {})
        self.assertEqual(data['headers'], [
            {'name': 'Reply-To', 'value': 'admin@example.com'},
            {'name': 'Some-Header', 'value': 'foo'},
        ])

    @override_settings(IS_BUSINESS_SITE=True)
    def test_email_headers_for_business(self):
        data = {
            'email_to_address': 'user@example.com',
            'headers': [
                {'name': 'Reply-To', 'value': 'admin@example.com'},
                {'name': 'Some-Header', 'value': 'foo'},
            ]
        }
        errors = self.get_serializer()._validate_email(data)
        self.assertEqual(errors, {})
        self.assertEqual(data['headers'], [
            {'name': 'Reply-To', 'value': 'admin@example.com'},
        ])


class TestExportAnswersSerializer(TestCase):
    def test_validate_empty_data(self):
        data = {}
        serializer = ExportAnswersSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertIn('export_format', serializer.validated_data)
        self.assertEqual(serializer.validated_data['export_format'], 'xlsx')
        self.assertIn('upload', serializer.validated_data)
        self.assertEqual(serializer.validated_data['upload'], 'mds')
        self.assertFalse(serializer.validated_data['upload_files'])

    def test_validate_date_created_utc(self):
        data = {"date_started": "2019-07-11T11:23:30.396455Z"}
        serializer = ExportAnswersSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertEqual(serializer.validated_data['date_started'],
                         datetime(2019, 7, 11, 11, 23, 30, 396455, pytz.UTC))

    def test_validate_date_finished_utc(self):
        data = {"date_finished": "2019-07-11T11:23:30.396455Z"}
        serializer = ExportAnswersSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertEqual(serializer.validated_data['date_finished'],
                         datetime(2019, 7, 11, 11, 23, 30, 396455, pytz.UTC))

    def test_validate_only_pks(self):
        data = {"pks": "1, 3, 5, 7"}
        serializer = ExportAnswersSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertIn('pks', serializer.validated_data)
        expected = [1, 3, 5, 7]
        self.assertEqual(len(serializer.validated_data['pks']), len(expected))
        self.assertListEqual(serializer.validated_data['pks'], expected)

    def test_validate_export_questions(self):
        data = {"export_columns": {"questions": "67623,67624"}}
        serializer = ExportAnswersSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertIn('export_columns', serializer.validated_data)
        export_columns = serializer.validated_data['export_columns']
        self.assertIn('questions', export_columns)
        expected = [67623, 67624]
        self.assertEqual(len(export_columns['questions']), len(expected))
        self.assertListEqual(export_columns['questions'], expected)

    def test_validate_export_user_fields(self):
        data = {"export_columns": {"user_fields": "uid,param_name"}}
        serializer = ExportAnswersSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertIn('export_columns', serializer.validated_data)
        export_columns = serializer.validated_data['export_columns']
        self.assertIn('user_fields', export_columns)
        expected = ['uid', 'param_name']
        self.assertEqual(len(export_columns['user_fields']), len(expected))
        self.assertListEqual(export_columns['user_fields'], expected)

    def test_validate_export_answer_fields(self):
        data = {"export_columns": {"answer_fields": "id,date_created"}}
        serializer = ExportAnswersSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertIn('export_columns', serializer.validated_data)
        export_columns = serializer.validated_data['export_columns']
        self.assertIn('answer_fields', export_columns)
        expected = ['id', 'date_created']
        self.assertEqual(len(export_columns['answer_fields']), len(expected))
        self.assertListEqual(export_columns['answer_fields'], expected)

    def test_validate_export_format(self):
        data = {"export_format": "csv"}
        serializer = ExportAnswersSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertIn('export_format', serializer.validated_data)
        self.assertEqual(serializer.validated_data['export_format'], 'csv')

    def test_validate_upload_format(self):
        data = {"upload": "disk", "upload_files": True}
        serializer = ExportAnswersSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertIn('upload', serializer.validated_data)
        self.assertEqual(serializer.validated_data['upload'], 'disk')
        self.assertTrue(serializer.validated_data['upload_files'])

    def test_validate_only_pks_validation_error(self):
        data = {"pks": "1, 3, five, 7"}
        serializer = ExportAnswersSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('pks', serializer.errors)

    def test_validate_export_questions_validation_error(self):
        data = {"export_columns": {"questions": "67623,answer_short_text_67624"}}
        serializer = ExportAnswersSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('export_columns', serializer.errors)
        self.assertIn('questions', serializer.errors['export_columns'])

    def test_validate_export_format_validation_error(self):
        data = {"export_format": "odt"}
        serializer = ExportAnswersSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('export_format', serializer.errors)


class TestRedirectSerializer(TestCase):
    def test_redirect_should_be_not_enabled(self):
        data = {
            'enabled': False,
        }
        serializer = RedirectSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertFalse(serializer.validated_data['enabled'])
        self.assertEqual(serializer.validated_data['url'], '')
        self.assertFalse(serializer.validated_data['auto_redirect'])
        self.assertFalse(serializer.validated_data['with_delay'])
        self.assertEqual(serializer.validated_data['timeout'], DEFAULT_TIMEOUT_DELAY)

    @override_settings(IS_BUSINESS_SITE=False)
    def test_redirect_should_be_enabled(self):
        data = {
            'enabled': True,
            'url': 'https://yandex.ru/',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': 3000,
        }
        serializer = RedirectSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertTrue(serializer.validated_data['enabled'])
        self.assertEqual(serializer.validated_data['url'], 'https://yandex.ru/')
        self.assertTrue(serializer.validated_data['auto_redirect'])
        self.assertTrue(serializer.validated_data['with_delay'])
        self.assertEqual(serializer.validated_data['timeout'], 3000)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_for_b2b_auto_redirect_should_be_swithced_off(self):
        data = {
            'enabled': True,
            'url': 'https://yandex.ru/',
            'auto_redirect': True,  # should be inverted
            'with_delay': True,
            'timeout': 5000,
        }
        serializer = RedirectSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertTrue(serializer.validated_data['enabled'])
        self.assertEqual(serializer.validated_data['url'], 'https://yandex.ru/')
        self.assertFalse(serializer.validated_data['auto_redirect'])
        self.assertTrue(serializer.validated_data['with_delay'])
        self.assertEqual(serializer.validated_data['timeout'], 5000)

    def test_url_mustnot_be_empty(self):
        data = {
            'enabled': True,
            'url': '',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': 5000,
        }
        serializer = RedirectSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('url', serializer.errors)

    def test_url_must_be_valid(self):
        data = {
            'enabled': True,
            'url': 'not an url',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': 5000,
        }
        serializer = RedirectSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('url', serializer.errors)

    @override_settings(IS_BUSINESS_SITE=False)
    def test_timeout_mast_be_valid(self):
        data = {
            'enabled': True,
            'url': 'https://yandex.ru/',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': None,
        }
        serializer = RedirectSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('timeout', serializer.errors)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_timeout_must_be_greater_then_min(self):
        data = {
            'enabled': True,
            'url': 'https://yandex.ru/',
            'auto_redirect': True,
            'with_delay': True,
            'timeout': 1000,
        }
        serializer = RedirectSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertEqual(serializer.validated_data['timeout'], MIN_TIMEOUT_DELAY)


class TestFooterSerializer(TestCase):
    @override_settings(IS_BUSINESS_SITE=True)
    def test_enabled_always_true_for_b2b(self):
        data = {
            'enabled': False,
        }
        serializer = FooterSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertTrue(serializer.validated_data['enabled'])

        data = {
            'enabled': True,
        }
        serializer = FooterSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertTrue(serializer.validated_data['enabled'])

    def test_shouldnt_validate_data(self):
        data = {
            'enabled': None,
        }
        serializer = FooterSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('enabled', serializer.errors)


class TestStatsSerializer(TestCase):
    def test_should_be_disabled_by_default(self):
        data = {}
        serializer = StatsSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertFalse(serializer.validated_data['enabled'])

    def test_shouldnt_validate_data(self):
        data = {
            'enabled': None,
        }
        serializer = StatsSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('enabled', serializer.errors)


class TestTeaserSerializer(TestCase):
    @override_settings(IS_BUSINESS_SITE=True)
    def test_enabled_always_true_for_b2b(self):
        data = {
            'enabled': False,
        }
        serializer = TeaserSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertTrue(serializer.validated_data['enabled'])

        data = {
            'enabled': True,
        }
        serializer = TeaserSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertTrue(serializer.validated_data['enabled'])

    def test_shouldnt_validate_data(self):
        data = {
            'enabled': None,
        }
        serializer = TeaserSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('enabled', serializer.errors)


class TestQuizSerializer(TestCase):
    def test_should_validate_data(self):
        image = ImageFactory()
        data = {
            'items': [
                {
                    'title': 'Title1',
                    'image_id': None,
                    'description': 'description1'
                },
                {
                    'title': 'Title2',
                    'image_id': image.pk,
                    'description': 'description2'
                },
            ],
            'calc_method': 'range',
            'pass_scores': 12.3,
            'show_correct': True,
            'show_results': True,
        }
        serializer = QuizSerializer(data=data)
        self.assertTrue(serializer.is_valid())
        self.assertEqual(len(serializer.validated_data['items']), 2)
        self.assertDictEqual(serializer.validated_data['items'][0], {
            'title': 'Title1',
            'image_id': None,
            'description': 'description1'
        })
        self.assertDictEqual(serializer.validated_data['items'][1], {
            'title': 'Title2',
            'image_id': image.pk,
            'description': 'description2'
        })
        self.assertEqual(serializer.validated_data['calc_method'], 'range')
        self.assertEqual(serializer.validated_data['pass_scores'], 12.3)
        self.assertTrue(serializer.validated_data['show_correct'])
        self.assertTrue(serializer.validated_data['show_results'])
