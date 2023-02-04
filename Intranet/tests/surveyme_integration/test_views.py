# -*- coding: utf-8 -*-
from django.test import TestCase, override_settings

from events.accounts.factories import OrganizationFactory
from events.accounts.factories import OrganizationToGroupFactory
from events.accounts.helpers import YandexClient
from events.surveyme.factories import SurveyFactory, ProfileSurveyAnswerFactory
from events.surveyme_integration.factories import (
    ServiceSurveyHookSubscriptionFactory,
    HookSubscriptionNotificationFactory,
)
from events.surveyme_integration.models import ServiceTypeAction


class TestIntegrationResultsView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex()

        self.survey = SurveyFactory()
        self.answer = ProfileSurveyAnswerFactory(user=self.profile, survey=self.survey)

        self.subscription_wiki = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=ServiceTypeAction.objects.get(service_type__slug='wiki').pk,
            follow_result=True,
        )
        self.notification_wiki = HookSubscriptionNotificationFactory(
            survey=self.survey, user=self.profile,
            answer=self.answer, subscription=self.subscription_wiki,
            context={'supertag': '/users/smosker/1'}
        )

        self.subscription_email = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=ServiceTypeAction.objects.get(service_type__slug='email').pk,
            follow_result=True,
        )
        self.notification_email = HookSubscriptionNotificationFactory(
            survey=self.survey, user=self.profile,
            answer=self.answer, subscription=self.subscription_email,
            context={'to_address': 'smth@yandex.ru'}
        )

        self.subscription_startrek = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=ServiceTypeAction.objects.filter(service_type__slug='startrek').first().pk,
            follow_result=True,
        )
        self.notification_startrek = HookSubscriptionNotificationFactory(
            survey=self.survey, user=self.profile,
            answer=self.answer, subscription=self.subscription_startrek,
            response={'issue': {'key': 'FORMS-12312'}}

        )

        self.subscription_http = ServiceSurveyHookSubscriptionFactory(
            service_type_action_id=ServiceTypeAction.objects.get(slug='post', service_type__slug='http').pk,
            follow_result=True,
        )
        self.notification_http = HookSubscriptionNotificationFactory(
            survey=self.survey, user=self.profile,
            answer=self.answer, subscription=self.subscription_http,
            context={'url': 'https://test.smth.yandex.ru/hello/me'}
        )
        self.subscriptions = [
            self.subscription_http, self.subscription_startrek,
            self.subscription_email, self.subscription_wiki,
        ]

        self.notifications = [
            self.notification_email, self.notification_http,
            self.notification_startrek, self.notification_wiki,
        ]

    def get_data(self, answer_id=None, answer_key=None, status_code=200, **headers):
        params = {}
        if answer_id:
            params['answer_id'] = answer_id
        if answer_key:
            params['answer_key'] = answer_key
        response = self.client.get('/v1/integration-results/', params, **headers)
        self.assertEqual(response.status_code, status_code)
        return response.data

    def test_should_return_data_by_answer_id(self):
        data = self.get_data(answer_id=self.answer.pk)
        self.assertDictEqual(data, {
            'count': 4,
            'next': None,
            'previous': None,
            'results': {
                '1': {
                    'id': '1',
                    'status': 'pending',
                    'integration_type': 'wiki',
                    'resources': {},
                },
                '2': {
                    'id': '2',
                    'status': 'pending',
                    'integration_type': 'email',
                    'resources': {},
                },
                '3': {
                    'id': '3',
                    'status': 'pending',
                    'integration_type': 'startrek',
                    'resources': {},
                },
                '4': {
                    'id': '4',
                    'status': 'pending',
                    'integration_type': 'http',
                    'resources': {},
                },
            },
        })

    def test_should_return_data_by_answer_key(self):
        data = self.get_data(answer_key=self.answer.secret_code)
        self.assertDictEqual(data, {
            'count': 4,
            'next': None,
            'previous': None,
            'results': {
                '1': {
                    'id': '1',
                    'status': 'pending',
                    'integration_type': 'wiki',
                    'resources': {},
                },
                '2': {
                    'id': '2',
                    'status': 'pending',
                    'integration_type': 'email',
                    'resources': {},
                },
                '3': {
                    'id': '3',
                    'status': 'pending',
                    'integration_type': 'startrek',
                    'resources': {},
                },
                '4': {
                    'id': '4',
                    'status': 'pending',
                    'integration_type': 'http',
                    'resources': {},
                },
            },
        })

    def test_should_not_fail_on_bad_request(self):
        response = self.client.get('/v1/integration-results/', {'answer_id': 'hello'})
        self.assertEqual(response.status_code, 400)

        response = self.client.get('/v1/integration-results/', {'answer_id': '99999999999999'})
        self.assertEqual(response.status_code, 200)
        self.assertDictEqual(response.data['results'], {})

        response = self.client.get('/v1/integration-results/', {'answer_key': 'notexistinganswerkey'})
        self.assertEqual(response.status_code, 200)
        self.assertDictEqual(response.data['results'], {})

        response = self.client.get('/v1/integration-results/')
        self.assertEqual(response.status_code, 400)

    def test_should_return_pending(self):
        data = self.get_data(answer_id=self.answer.pk)
        result = data['results']
        self.assertEqual(len(result), 4)
        self.assertEqual(
            set(result),
            {str(subscription.id) for subscription in self.subscriptions}
        )
        self.assertTrue(all(subscription['status'] == 'pending' for subscription in result.values()))

    def test_should_return_follow_only(self):
        self.subscription_email.follow_result = False
        self.subscription_email.save()
        data = self.get_data(answer_id=self.answer.pk)
        result = data['results']
        self.assertEqual(len(result), 3)

    def test_should_return_finished(self):
        for notification in self.notifications:
            notification.status = 'success'
            notification.save()
        data = self.get_data(answer_id=self.answer.pk)
        result = data['results']
        self.assertEqual(len(result), 4)
        self.assertEqual(
            set(result),
            {str(subscription.id) for subscription in self.subscriptions}
        )
        self.assertTrue(all(subscription['status'] == 'success' for subscription in result.values()))
        self.assertEqual(
            result[str(self.subscription_wiki.pk)]['resources'],
            {'wiki_page': '/users/smosker/1', 'link': 'https://wiki.test.yandex-team.ru/users/smosker/1'},
        )
        self.assertEqual(
            result[str(self.subscription_http.pk)]['resources'],
            {'url': 'test.smth.yandex.ru'},
        )
        self.assertEqual(
            result[str(self.subscription_email.pk)]['resources'],
            {'to_address': 'smth@yandex.ru'},
        )
        self.assertEqual(
            result[str(self.subscription_startrek.pk)]['resources'],
            {'link': 'https://st.test.yandex-team.ru/FORMS-12312', 'key': 'FORMS-12312'},
        )

    def test_should_return_mixed(self):
        self.notification_wiki.status = 'success'
        self.notification_wiki.save()
        self.notification_startrek.status = 'error'
        self.notification_startrek.response = None
        self.notification_startrek.save()
        self.notification_http.save()

        data = self.get_data(answer_id=self.answer.pk)
        result = data['results']
        self.assertEqual(len(result), 4)
        self.assertEqual(
            set(result),
            {str(subscription.id) for subscription in self.subscriptions}
        )

        self.assertEqual(
            result[str(self.subscription_wiki.pk)],
            {
                'id': str(self.subscription_wiki.pk), 'status': 'success',
                'resources': {
                    'wiki_page': '/users/smosker/1',
                    'link': 'https://wiki.test.yandex-team.ru/users/smosker/1'
                },
                'integration_type': 'wiki',
            }
        )
        self.assertEqual(
            result[str(self.subscription_http.pk)],
            {
                'id': str(self.subscription_http.pk),
                'status': 'pending', 'resources': {},
                'integration_type': 'http',
            },
        )
        self.assertEqual(
            result[str(self.subscription_email.pk)],
            {
                'id': str(self.subscription_email.pk), 'status': 'pending',
                'resources': {},
                'integration_type': 'email',
            },
        )
        self.assertEqual(
            result[str(self.subscription_startrek.pk)],
            {
                'id': str(self.subscription_startrek.pk), 'status': 'error',
                'resources': {'message': None},
                'integration_type': 'startrek',
            },
        )

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_only_to_same_org(self):
        self.notification_startrek.status = 'success'
        self.notification_startrek.save()
        self.profile = self.client.login_yandex(is_superuser=True)

        self.o2g = OrganizationToGroupFactory()
        self.survey.org = self.o2g.org
        self.survey.save()

        data = self.get_data(answer_id=self.answer.pk, HTTP_X_ORGS='222')
        result = data['results']

        self.assertEqual(
            result[str(self.subscription_startrek.pk)]['resources'],
            {'link': 'https://st.test.yandex-team.ru/FORMS-12312', 'key': 'FORMS-12312'},
        )

        data = self.get_data(answer_id=self.answer.pk, HTTP_X_ORGS='123')
        result = data['results']

        self.assertEqual(
            result[str(self.subscription_startrek.pk)]['resources'],
            {'link': 'https://st.test.yandex-team.ru/FORMS-12312', 'key': 'FORMS-12312'},
        )


class TestVariablesView(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)

    def test_should_return_variables_for_intranet(self):
        response = self.client.get('/admin/api/v2/variables/')
        self.assertEqual(response.status_code, 200)
        connect_only_vars = [
            var
            for var in response.data
            if var.get('connect_only')
        ]
        self.assertTrue(len(connect_only_vars) > 0)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_return_variables_for_b2c(self):
        survey = SurveyFactory()

        response = self.client.get('/admin/api/v2/variables/')
        self.assertEqual(response.status_code, 200)
        connect_only_vars = [
            var
            for var in response.data
            if var.get('connect_only')
        ]
        self.assertTrue(len(connect_only_vars) == 0)

        response = self.client.get(f'/admin/api/v2/variables/?survey={survey.pk}')
        self.assertEqual(response.status_code, 200)
        connect_only_vars = [
            var
            for var in response.data
            if var.get('connect_only')
        ]
        self.assertTrue(len(connect_only_vars) == 0)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_return_variables_for_b2b(self):
        response = self.client.get('/admin/api/v2/variables/')
        self.assertEqual(response.status_code, 200)
        connect_only_vars = [
            var
            for var in response.data
            if var.get('connect_only')
        ]
        self.assertTrue(len(connect_only_vars) == 0)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_variables_for_b2b(self):
        org = OrganizationFactory()
        survey = SurveyFactory(org=org)

        response = self.client.get(f'/admin/api/v2/variables/?survey={survey.pk}')
        self.assertEqual(response.status_code, 200)
        connect_only_vars = [
            var
            for var in response.data
            if var.get('connect_only')
        ]
        self.assertTrue(len(connect_only_vars) > 0)
