# -*- coding: utf-8 -*-
import responses

from bson.objectid import ObjectId
from django.core.files.base import ContentFile
from django.test import TestCase, override_settings
from requests.exceptions import HTTPError, Timeout as RequestTimeout
from unittest.mock import patch

from events.accounts.helpers import YandexClient
from events.common_app.startrek.client import StartrekClient
from events.common_app.helpers import MockResponse
from events.common_storages.storage import MdsStorage, ReadError
from events.common_storages.factories import ProxyStorageModelFactory
from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyFactory,
    SurveyQuestionFactory,
)
from events.surveyme.models import AnswerType
from events.surveyme_integration.factories import (
    HookSubscriptionNotificationFactory,
    ServiceSurveyHookSubscriptionFactory,
    StartrekSubscriptionDataFactory,
    SurveyHookFactory,
    SurveyVariableFactory,
)
from events.surveyme_integration.models import HookSubscriptionNotification
from events.surveyme_integration.tasks import send_notification
from events.surveyme_integration.helpers import IntegrationTestMixin
from events.yauth_contrib.auth import TvmAuth


class TestStartrekCreateTicketIntegration(IntegrationTestMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()

        self.subscription.service_type_action_id = 7  # startrek. Create ticket
        self.subscription.title = 'Title goes here'
        self.subscription.body = 'Body goes here'
        self.subscription.save()

        self.field = {
            'key': {'slug': 'checkMonth', 'name': 'Check Month', 'type': 'string'},
            'value': 'september',
            'add_only_with_value': True,
        }
        self.subscription_data = StartrekSubscriptionDataFactory(
            subscription=self.subscription,
            queue='FORMS',
            author='riotta',
            assignee='masloval',
            type=2,
            priority=2,
            tags=['hello', 'world'],
            followers=['kdunaev', 'shevnv'],
            fields=[self.field],
        )

    def get_notification(self, response):
        return HookSubscriptionNotification.objects.get(
            answer_id=response.data['answer_id'],
            subscription=self.subscription,
        )

    @responses.activate
    def test_create_ticket(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
            },
            status=201,
        )
        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response = self.post_data()

        notification = self.get_notification(response)
        self.assertEqual(notification.status, 'success')
        issue = notification.response['issue']
        self.assertEqual(issue['queue']['key'], self.subscription.startrek.queue)
        self.assertEqual(issue['createdBy']['id'], self.subscription.startrek.author)
        self.assertEqual(issue['assignee']['id'], self.subscription.startrek.assignee)
        self.assertEqual(issue['summary'], self.subscription.title)
        self.assertEqual(issue['description'], self.subscription.body)
        self.assertEqual(issue['type']['id'], str(self.subscription.startrek.type))
        self.assertEqual(issue['priority']['id'], str(self.subscription.startrek.priority))
        self.assertListEqual(issue['tags'], self.subscription.startrek.tags)
        self.assertListEqual(
            [follower['id'] for follower in issue['followers']],
            self.subscription.startrek.followers,
        )
        self.assertEqual(issue['unique'], 'FORMS/2/1/5feb2315d04802ff849d1a15')
        self.assertEqual(issue[self.field['key']['slug']], self.field['value'])

    @responses.activate
    def test_create_ticket_without_field_if_no_value(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'attachments': None,
            },
            status=201,
        )
        self.field['value'] = ''
        self.field['add_only_with_value'] = False
        self.subscription_data.fields = [self.field]
        self.subscription_data.save()

        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response = self.post_data()

        notification = self.get_notification(response)
        self.assertEqual(notification.status, 'success')
        issue = notification.response['issue']
        self.assertFalse(self.field['key']['slug'] in issue)

    @responses.activate
    def test_apply_filters_correct(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
                'access': [{'id': 'smosker'}, {'id': 'volozh'}],
            },
            status=201,
        )
        variable = SurveyVariableFactory(
            variable_id=str(ObjectId()),
            hook_subscription=self.subscription,
            var='form.question_answer',
            arguments={
                'show_filenames': False,
                'question': self.questions['some_text'].pk,
            }
        )

        self.subscription_data.fields.append({
            'key': {'type': 'array/user', 'slug': 'access', 'name': 'Access'},
            'value': f'{{{variable.variable_id}}}',
            'add_only_with_value': True,
        })
        self.subscription_data.save()

        self.auth_user_client = YandexClient()
        self.auth_user_client.login_yandex()

        self.data[self.questions['some_text'].get_form_field_name()] = 'Колясинский Владимир (smosker), Аркадий Волож (volozh)'

        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response = self.post_data(self.auth_user_client)

        notification = self.get_notification(response)
        self.assertEqual(notification.status, 'success')
        issue = notification.response['issue']
        self.assertListEqual(
            [access['id'] for access in issue['access']],
            ['smosker', 'volozh'],
        )

    @responses.activate
    def test_split_correct(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
                'workCode': ['working_choice', 'studying_choice'],
            },
            status=201,
        )
        variable = SurveyVariableFactory(
            variable_id=str(ObjectId()),
            hook_subscription=self.subscription,
            var='form.question_answer_choice_slug',
            arguments={
                'question': self.questions['career'].pk,
            }
        )
        self.questions['career'].param_is_allow_multiple_choice = True
        self.questions['career'].save()
        self.subscription_data.fields.append({
            'key': {'type': 'array/string', 'slug': 'workCode', 'name': 'Work Code'},
            'value': f'{{{variable.variable_id}}}',
            'add_only_with_value': True,
        })
        self.subscription_data.save()

        self.auth_user_client = YandexClient()
        self.auth_user_client.login_yandex()
        self.data[self.questions['career'].get_form_field_name()] = [
            self.career_choices['working'].pk,
            self.career_choices['studying'].pk,
        ]

        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response = self.post_data(self.auth_user_client)

        notification = self.get_notification(response)
        self.assertEqual(notification.status, 'success')
        issue = notification.response['issue']
        self.assertListEqual(
            issue['workCode'],
            ['working_choice', 'studying_choice'],
        )

    @responses.activate
    def test_create_ticket__without_assignee(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
            },
            status=201,
        )
        self.subscription.startrek.assignee = ''
        self.subscription.startrek.save()

        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response = self.post_data()

        notification = self.get_notification(response)
        self.assertEqual(notification.status, 'success')
        issue = notification.response['issue']
        self.assertIsNotNone(issue)

    @responses.activate
    def test_create_ticket__without_author(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'tech-robot'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
            },
            status=201,
        )
        self.subscription.startrek.author = ''
        self.subscription.startrek.save()

        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response = self.post_data()

        notification = self.get_notification(response)
        self.assertEqual(notification.status, 'success')
        issue = notification.response['issue']
        self.assertIsNotNone(issue)

    @responses.activate
    def test_create_ticket__with_parent(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
                'parent': {
                    'id': '53fc3b6de4b00ed34ab22aac',
                    'key': 'FORMS-42',
                },
            },
            status=201,
        )
        self.subscription.startrek.parent = 'FORMS-42'
        self.subscription.startrek.save()

        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response = self.post_data()

        notification = self.get_notification(response)
        self.assertEqual(notification.status, 'success')
        issue = notification.response['issue']

        self.assertEqual(issue['queue']['key'], 'FORMS')
        self.assertEqual(issue['parent']['key'], 'FORMS-42')
        self.assertTrue(issue['key'].startswith('FORMS'))

    @override_settings(STARTREK_UNIQUE_IN_TESTS=False)
    @responses.activate
    def test_should_return_existing_ticket_on_conflict(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={'errors': {}, 'errorMessages': ['Задача уже существует'], 'statusCode': 409},
            headers={'X-Ticket-Key': 'FORMS-1614'},
            status=409,
        )
        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response = self.post_data()

        notification = self.get_notification(response)
        self.assertEqual(notification.status, 'success')
        issue = notification.response['issue']
        self.assertEqual(issue['key'], 'FORMS-1614')


class TestStartrekUpdateOrCreateTicketIntegration(TestStartrekCreateTicketIntegration):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()

        self.survey.is_allow_answer_editing = True
        self.survey.save()
        self.subscription.service_type_action_id = 8  # startrek. Update or create ticket
        self.subscription.save()
        self.auth_user_client = YandexClient()
        self.auth_user_client.login_yandex()

    def get_notifications(self, response):
        return HookSubscriptionNotification.objects.filter(
            answer_id=response.data['answer_id'],
            subscription=self.subscription,
        )

    @responses.activate
    def test_should_create_many_issues_if_its_not_update_of_one_answer(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
            },
            status=201,
        )
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b3e',
                'key': 'FORMS-1955',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a24',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
            },
            status=201,
        )
        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response1 = self.post_data()
            response2 = self.post_data()

        self.assertNotEqual(response1.data['answer_id'], response2.data['answer_id'])

        notification1 = self.get_notification(response1)
        self.assertEqual(notification1.status, 'success')
        issue1 = notification1.response['issue']

        notification2 = self.get_notification(response2)
        self.assertEqual(notification2.status, 'success')
        issue2 = notification2.response['issue']

        self.assertNotEqual(issue1['key'], issue2['key'])

    @responses.activate
    def test_should_update_existing_issue_for_update_of_one_answer(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
            },
            status=201,
        )
        responses.add(
            responses.PATCH,
            'https://st-api.test.yandex-team.ru/service/issues/FORMS-1954/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
            },
        )
        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response1 = self.post_data(self.auth_user_client)
            response2 = self.post_data(self.auth_user_client)

        self.assertEqual(response1.data['answer_id'], response2.data['answer_id'])

        notifications = self.get_notifications(response1)
        self.assertEqual(notifications[0].status, 'success')
        self.assertEqual(notifications[1].status, 'success')

        issue1 = notifications[0].response['issue']
        issue2 = notifications[1].response['issue']
        self.assertEqual(issue1['key'], issue2['key'])

        self.assertEqual(issue1['summary'], issue2['summary'])
        self.assertEqual(issue1['description'], issue2['description'])

        self.assertEqual(issue2['summary'], self.subscription.title)
        self.assertEqual(issue2['description'], self.subscription.body)

    @responses.activate
    def test_should_update_only_description_and_summary(self):
        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'Title goes here',
                'description': 'Body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
            },
            status=201,
        )
        responses.add(
            responses.PATCH,
            'https://st-api.test.yandex-team.ru/service/issues/FORMS-1954/',
            json={
                'id': '5feb2315e3f8946fccd23b2f',
                'key': 'FORMS-1954',
                'summary': 'New title goes here',
                'description': 'New body goes here',
                'type': {'id': '2'},
                'priority': {'id': '2'},
                'tags': ['hello', 'world'],
                'followers': [{'id': 'kdunaev'}, {'id': 'shevnv'}],
                'createdBy': {'id': 'riotta'},
                'unique': 'FORMS/2/1/5feb2315d04802ff849d1a15',
                'assignee': {'id': 'masloval'},
                'queue': {'key': 'FORMS'},
                'checkMonth': 'september',
                'attachments': None,
            },
        )
        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            response1 = self.post_data(self.auth_user_client)

            self.subscription.title = 'New title goes here'
            self.subscription.body = 'New body goes here'
            self.subscription.save()

            response2 = self.post_data(self.auth_user_client)

        self.assertEqual(response1.data['answer_id'], response2.data['answer_id'])

        notifications = self.get_notifications(response1)
        self.assertEqual(notifications[0].status, 'success')
        self.assertEqual(notifications[1].status, 'success')

        issue1 = notifications[0].response['issue']
        issue2 = notifications[1].response['issue']
        self.assertEqual(issue1['key'], issue2['key'])

        self.assertNotEqual(issue1['summary'], issue2['summary'])
        self.assertNotEqual(issue1['description'], issue2['description'])

        self.assertEqual(issue2['summary'], self.subscription.title)
        self.assertEqual(issue2['description'], self.subscription.body)


class TestStartrekIntegration(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory(is_published_external=True)
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text'),
        )
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.hook,
            service_type_action_id=7,  # create ticket
            title='test title',
            body='test body',
        )
        self.subscription_data = StartrekSubscriptionDataFactory(
            subscription=self.subscription,
            queue='FORMS',
            author='kdunaev',
            assignee='tolec',
            type=2,
            priority=2,
        )

    def get_notification(self, response):
        return HookSubscriptionNotification.objects.get(
            answer_id=response.data['answer_id'],
            subscription=self.subscription,
        )

    def test_shouldnt_create_ticket_on_startrek_error(self):
        data = {
            self.question.param_slug: 'testit',
        }
        startrek_response = MockResponse(status_code=422, json_data={
            'errors': {},
            'errorMessages': ['test error.'],
            'statusCode': 422,
        })
        with patch('events.common_app.startrek.client.StartrekClient.create_issue') as mock_create_issue:
            with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
                mock_create_issue.side_effect = HTTPError(response=startrek_response)
                response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data, format='json')

        self.assertEqual(response.status_code, 200)
        notification = self.get_notification(response)
        self.assertEqual(notification.status, 'error')
        self.assertIn('TrackerConfigError', notification.error['classname'])
        self.assertIn('test error.', notification.error['message'])

    @responses.activate
    def test_should_correct_handle_timeout_error(self):
        data = {
            self.question.param_slug: 'testit',
        }
        with patch('events.common_app.startrek.client.StartrekClient.create_issue') as mock_create_issue:
            with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
                mock_create_issue.side_effect = RequestTimeout()
                response = self.client.post(f'/v1/surveys/{self.survey.pk}/form/', data=data, format='json')

        self.assertEqual(response.status_code, 200)
        notification = self.get_notification(response)
        self.assertEqual(notification.status, 'error')
        self.assertIn('TrackerTimeoutError', notification.error['classname'])

        notification.context['unique'] = 'FORMS/59ef04201c9eab16872c1152'
        notification.status = 'pending'
        notification.save()

        responses.add(
            responses.POST,
            'https://st-api.test.yandex-team.ru/service/issues/',
            json={'errors': {}, 'errorMessages': ['Задача уже существует'], 'statusCode': 409},
            headers={'X-Ticket-Key': 'FORMS-1070'},
            status=409,
        )
        with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
            send_notification(notification.pk)

        notification.refresh_from_db()
        self.assertEqual(notification.status, 'success')
        self.assertIsNotNone(notification.response)
        self.assertEqual(notification.response['issue']['key'], 'FORMS-1070')


class TestStartrekAttachments(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.survey = SurveyFactory()
        self.question = SurveyQuestionFactory(
            survey=self.survey,
            answer_type=AnswerType.objects.get(slug='answer_files'),
        )
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=SurveyHookFactory(survey=self.survey),
            service_type_action_id=7,  # startrek. Create ticket
            title='Title goes here',
            body='Body goes here',
            is_all_questions=True,
        )
        self.startrek = StartrekSubscriptionDataFactory(
            subscription=self.subscription,
            queue='FORMS',
            author='kdunaev',
            type=2,
            priority=2,
        )
        self.meta_data = ProxyStorageModelFactory(
            path='/1/readme.txt',
            sha256='123',
            file_size=10,
            original_name='readme.txt',
        )
        self.answer = ProfileSurveyAnswerFactory(
            survey=self.survey,
            data={
                'data': [{
                    'question': self.question.get_answer_info(),
                    'value': [{
                        'path': self.meta_data.path,
                        'size': self.meta_data.file_size,
                        'name': self.meta_data.original_name,
                    }],
                }],
            },
        )
        self.notification = HookSubscriptionNotificationFactory(
            survey=self.survey,
            answer=self.answer,
            subscription=self.subscription,
            status='pending',
            trigger_slug='create',
        )

    def test_should_attach_file(self):
        content = ContentFile(b'content')
        with patch.object(StartrekClient, 'create_issue') as mock_create_issue:
            with patch.object(StartrekClient, 'create_attachment') as mock_create_attachment:
                with patch.object(MdsStorage, '_open') as mock_open:
                    with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
                        mock_create_issue.return_value = {
                            'key': 'FORMS-123',
                            'summary': self.subscription.title,
                            'description': self.subscription.body,
                        }
                        mock_create_attachment.return_value = {
                            'id': '101',
                        }
                        mock_open.return_value = content
                        send_notification(self.notification.pk)

        self.notification.refresh_from_db()
        self.assertEqual(self.notification.status, 'success')

        mock_open.assert_called_once_with(self.meta_data.path, 'rb')
        mock_create_attachment.assert_called_once_with(
            file_content=content,
            file_name=self.meta_data.original_name,
        )
        mock_create_issue.assert_called_once()
        data = mock_create_issue.call_args[0][0]
        self.assertEqual(data['queue'], self.startrek.queue)
        self.assertDictEqual(data['type'], {'id': str(self.startrek.type)})
        self.assertDictEqual(data['priority'], {'id': str(self.startrek.priority)})
        self.assertEqual(data['author'], self.startrek.author)
        self.assertEqual(data['summary'], self.subscription.title)
        self.assertEqual(data['description'], self.subscription.body)
        self.assertListEqual(data['attachmentIds'], ['101'])

    def test_shouldnt_attach_file(self):
        with patch.object(StartrekClient, 'create_issue') as mock_create_issue:
            with patch.object(StartrekClient, 'create_attachment') as mock_create_attachment:
                with patch.object(MdsStorage, '_open') as mock_open:
                    with patch.object(TvmAuth, '_get_service_ticket', return_value='123'):
                        mock_create_issue.return_value = {
                            'key': 'FORMS-123',
                            'summary': self.subscription.title,
                            'description': self.subscription.body,
                        }
                        mock_create_attachment.return_value = {
                            'id': '101',
                        }
                        mock_open.side_effect = ReadError
                        send_notification(self.notification.pk)

        self.notification.refresh_from_db()
        self.assertEqual(self.notification.status, 'success')

        mock_open.assert_called_once_with(self.meta_data.path, 'rb')
        mock_create_attachment.assert_not_called()
        mock_create_issue.assert_called_once()
        data = mock_create_issue.call_args[0][0]
        self.assertEqual(data['queue'], self.startrek.queue)
        self.assertDictEqual(data['type'], {'id': str(self.startrek.type)})
        self.assertDictEqual(data['priority'], {'id': str(self.startrek.priority)})
        self.assertEqual(data['author'], self.startrek.author)
        self.assertEqual(data['summary'], self.subscription.title)
        self.assertEqual(data['description'], self.subscription.body)
        self.assertListEqual(data['attachmentIds'], [])
