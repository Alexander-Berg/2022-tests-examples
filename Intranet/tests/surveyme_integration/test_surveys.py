# -*- coding: utf-8 -*-
import json

from bson.objectid import ObjectId
from django.test import TestCase, override_settings

from events.accounts.helpers import YandexClient
from events.surveyme.api_admin.v2.serializers import SurveyVariableSerializer
from events.surveyme.factories import SurveyFactory
from events.surveyme_integration.factories import ServiceSurveyHookSubscriptionFactory, SurveyVariableFactory
from events.surveyme_integration.models import SurveyVariable


class TestVariables(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.client.login_yandex(is_superuser=True)
        self.subscription = ServiceSurveyHookSubscriptionFactory()
        self.survey = self.subscription.survey_hook.survey
        self.hook = self.subscription.survey_hook
        self.url = f'/admin/api/v2/surveys/{self.survey.pk}/'

    def get_subscription_from_response(self, response):
        return response.data['hooks'][0]['subscriptions'][0]

    def test_200_for_survey(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)

    def test_api_should_return_variables(self):
        variable_one = SurveyVariableFactory(
            variable_id='575176514507946502000002',
            hook_subscription=self.subscription,
            var='user.staff',
        )
        variable_two = SurveyVariableFactory(
            variable_id='575176514507946501000002',
            hook_subscription=self.subscription,
            var='user.id',
        )
        expected = {
            variable.variable_id: SurveyVariableSerializer(variable).data
            for variable in (variable_one, variable_two, )
        }
        subscription_data = self.get_subscription_from_response(self.client.get(self.url))
        self.assertEqual(subscription_data['variables'], expected)

    def test_api_should_allow_assign_variables_on_create_subscription(self):
        post_data = {
            'hooks': [
                {
                    'id': self.hook.id,
                    'subscriptions': [
                        {
                            'service_type_action': self.subscription.service_type_action_id,
                            'email_to_address': 'user@example.com',
                            'variables': {
                                str(ObjectId()): {
                                    'var': 'forms.id', 'filters': [], 'arguments': {},
                                    '_id': str(ObjectId()),
                                },
                                str(ObjectId()): {
                                    'var': 'staff.meta_name', 'format_name': 'some_data',
                                    'filters': ['md3', 'json', 'test'], '_id': str(ObjectId()),
                                    'arguments': {'some_param': 'test', 'another_one': 'new'},
                                },
                            }
                        }
                    ]
                }
            ]
        }
        self.assertEqual(
            SurveyVariable.objects.filter(hook_subscription=self.hook.id).count(),
            0,
        )
        response = self.client.patch(self.url, data=json.dumps(post_data), content_type='application/json')
        self.assertEqual(response.status_code, 200)
        subscription_data = self.get_subscription_from_response(response)

        self.assertEqual(self.survey.hooks.count(), 1)
        hook = self.survey.hooks.first()
        self.assertEqual(hook.subscriptions.count(), 1)
        subscription = hook.subscriptions.first()
        self.assertNotEqual(subscription.surveyvariable_set.count(), 0)
        variables = SurveyVariable.objects.filter(hook_subscription=subscription_data['id'])
        self.assertEqual(variables.count(), 2)
        staff_variable = variables.get(var='staff.meta_name')
        self.assertEqual(staff_variable.filters, ['md3', 'json', 'test'])
        self.assertEqual(staff_variable.arguments, {'some_param': 'test', 'another_one': 'new'})
        self.assertEqual(staff_variable.format_name, 'some_data')
        self.assertNotEqual(subscription_data['variables'], {})

    def test_api_should_allow_delete_variables(self):
        variable_id = str(ObjectId())
        post_data = {
            'hooks': [
                {
                    'id': self.hook.id,
                    'subscriptions': [
                        {
                            'id': self.subscription.id,
                            'service_type_action': self.subscription.service_type_action_id,
                            'email_to_address': 'user@example.com',
                            'variables': {
                                variable_id: {
                                    'var': 'forms.id', 'filters': [], 'arguments': {},
                                    '_id': variable_id,
                                },
                                str(ObjectId()): {
                                    'var': 'staff.meta_name', 'filters': [], 'arguments': {},
                                    '_id': str(ObjectId()),
                                }
                            },
                            'date_created': str(self.subscription.date_created),
                        }
                    ]
                }
            ]
        }
        response = self.client.patch(self.url, data=json.dumps(post_data), content_type='application/json')
        self.assertEqual(response.status_code, 200)
        subscription_data = self.get_subscription_from_response(response)
        variables = SurveyVariable.objects.filter(hook_subscription=subscription_data['id'])
        self.assertEqual(variables.count(), 2)
        self.assertEqual(set([variable.var for variable in variables]), {'forms.id', 'staff.meta_name'})

        post_data['hooks'][0]['subscriptions'][0]['variables'].pop(variable_id)
        response = self.client.patch(self.url, data=json.dumps(post_data), content_type='application/json')
        self.assertEqual(response.status_code, 200)
        subscription_data = self.get_subscription_from_response(response)
        variables = SurveyVariable.objects.filter(hook_subscription=subscription_data['id'])
        self.assertEqual(variables.count(), 1)
        self.assertEqual(variables.first().var, 'staff.meta_name')

    def test_api_should_allow_update_variables(self):
        variable = SurveyVariable.objects.create(
            hook_subscription=self.subscription,
            variable_id=str(ObjectId()),
            var='staff.meta_name',
            format_name='login',
            filters=['md3', 'json'],
            arguments={'some': 'data', 'another': 'data'},
        )

        post_data = {
            'hooks': [
                {
                    'id': self.hook.id,
                    'subscriptions': [
                        {
                            'id': self.subscription.id,
                            'service_type_action': self.subscription.service_type_action_id,
                            'email_to_address': 'user@example.com',
                            'variables': {
                                variable.variable_id: {
                                    'var': 'staff.meta_name', 'format_name': 'fio',
                                    'arguments': {'new': 'one'}, 'filters': [],
                                    '_id': variable.variable_id,
                                },
                            },
                            'date_created': str(self.subscription.date_created),
                        }
                    ]
                }
            ]
        }
        self.client.patch(self.url, data=json.dumps(post_data), content_type='application/json')
        variable.refresh_from_db()
        self.assertEqual(variable.format_name, 'fio')
        self.assertEqual(variable.arguments, {'new': 'one'})
        self.assertEqual(variable.filters, [])

    def test_api_should_allow_assign_variables_on_update_subscription(self):
        post_data = {
            'hooks': [
                {
                    'id': self.hook.id,
                    'subscriptions': [
                        {
                            'id': self.subscription.id,
                            'service_type_action': self.subscription.service_type_action_id,
                            'email_to_address': 'user@example.com',
                            'variables': {
                                str(ObjectId()): {
                                    'var': 'forms.id', 'filters': [], 'arguments': {},
                                    '_id': str(ObjectId()),
                                },
                                str(ObjectId()): {
                                    'var': 'staff.meta_name', 'format_name': 'some_data',
                                    'filters': ['md3', 'json', 'another_test'],
                                    'arguments': {'some_param': 'test', 'another_one': 'new'},
                                    '_id': str(ObjectId()),
                                }
                            },
                            'date_created': str(self.subscription.date_created),
                        }
                    ]
                }
            ]
        }
        response = self.client.patch(self.url, data=json.dumps(post_data), content_type='application/json')
        self.assertEqual(response.status_code, 200)
        subscription_data = self.get_subscription_from_response(response)
        self.assertNotEqual(subscription_data['variables'], {})
        variables = SurveyVariable.objects.filter(hook_subscription=subscription_data['id'])
        self.assertEqual(variables.count(), 2)
        staff_variable = variables.get(var='staff.meta_name')
        self.assertEqual(staff_variable.filters, ['md3', 'json', 'another_test'])
        self.assertEqual(staff_variable.arguments, {'some_param': 'test', 'another_one': 'new'})
        self.assertEqual(staff_variable.format_name, 'some_data')
        self.assertNotEqual(subscription_data['variables'], {})


class TestEmailFromAddress(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(user=self.profile)

    @override_settings(IS_BUSINESS_SITE=False, APP_TYPE='forms_int')
    def test_should_substitute_default_address_in_forms_int(self):
        data = {
            'hooks': [{
                'triggers': [1, 2],
                'survey': self.survey.pk,
                'subscriptions': [{
                    'service_type_action': 3,
                    'title': 'subject',
                    'email_to_address': 'user@domain.com',
                    'body': 'body',
                }]
            }]
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['hooks']), 1)
        hook = response.data['hooks'][0]
        self.assertEqual(len(hook['subscriptions']), 1)
        subscription = hook['subscriptions'][0]
        self.assertEqual(subscription['email_from_address'], 'devnull@yandex-team.ru')

    @override_settings(IS_BUSINESS_SITE=False, APP_TYPE='forms_int')
    def test_shouldnt_substitute_default_address_in_forms_int(self):
        data = {
            'hooks': [{
                'triggers': [1, 2],
                'survey': self.survey.pk,
                'subscriptions': [{
                    'service_type_action': 3,
                    'email_from_address': 'devnull@domain.com',
                    'title': 'subject',
                    'email_to_address': 'user@domain.com',
                    'body': 'body',
                }]
            }]
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['hooks']), 1)
        hook = response.data['hooks'][0]
        self.assertEqual(len(hook['subscriptions']), 1)
        subscription = hook['subscriptions'][0]
        self.assertEqual(subscription['email_from_address'], 'devnull@domain.com')

    @override_settings(IS_BUSINESS_SITE=False, APP_TYPE='forms_ext_admin')
    def test_should_substitute_default_address_in_forms_ext_admin(self):
        data = {
            'hooks': [{
                'triggers': [1, 2],
                'survey': self.survey.pk,
                'subscriptions': [{
                    'service_type_action': 3,
                    'title': 'subject',
                    'email_to_address': 'user@domain.com',
                    'body': 'body',
                }]
            }]
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['hooks']), 1)
        hook = response.data['hooks'][0]
        self.assertEqual(len(hook['subscriptions']), 1)
        subscription = hook['subscriptions'][0]
        self.assertEqual(subscription['email_from_address'], f'{self.survey.pk}@forms.yandex.ru')

    @override_settings(IS_BUSINESS_SITE=False, APP_TYPE='forms_ext_admin')
    def test_shouldnt_substitute_default_address_in_forms_ext_admin(self):
        data = {
            'hooks': [{
                'triggers': [1, 2],
                'survey': self.survey.pk,
                'subscriptions': [{
                    'service_type_action': 3,
                    'email_from_address': 'devnull@domain.com',
                    'title': 'subject',
                    'email_to_address': 'user@domain.com',
                    'body': 'body',
                }]
            }]
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['hooks']), 1)
        hook = response.data['hooks'][0]
        self.assertEqual(len(hook['subscriptions']), 1)
        subscription = hook['subscriptions'][0]
        self.assertEqual(subscription['email_from_address'], 'devnull@domain.com')

    @override_settings(IS_BUSINESS_SITE=True, APP_TYPE='forms_biz')
    def test_should_substitute_default_address_in_forms_biz(self):
        data = {
            'hooks': [{
                'triggers': [1, 2],
                'survey': self.survey.pk,
                'subscriptions': [{
                    'service_type_action': 3,
                    'title': 'subject',
                    'email_to_address': 'user@domain.com',
                    'body': 'body',
                }]
            }]
        }
        response = self.client.patch(f'/admin/api/v2/surveys/{self.survey.pk}/', data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['hooks']), 1)
        hook = response.data['hooks'][0]
        self.assertEqual(len(hook['subscriptions']), 1)
        subscription = hook['subscriptions'][0]
        self.assertEqual(subscription['email_from_address'], f'{self.survey.pk}@forms-mailer.yaconnect.com')

    @override_settings(IS_BUSINESS_SITE=True, APP_TYPE='forms_biz')
    def test_should_substitute_default_address_in_forms_biz_mailer(self):
        data = {
            'hooks': [{
                'triggers': [1, 2],
                'survey': self.survey.pk,
                'subscriptions': [{
                    'service_type_action': 3,
                    'title': 'subject',
                    'email_to_address': 'user@domain.com',
                    'body': 'body',
                }]
            }]
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['hooks']), 1)
        hook = response.data['hooks'][0]
        self.assertEqual(len(hook['subscriptions']), 1)
        subscription = hook['subscriptions'][0]
        self.assertEqual(subscription['email_from_address'], '{}@forms-mailer.yaconnect.com'.format(self.survey.pk))

    @override_settings(IS_BUSINESS_SITE=True, APP_TYPE='forms_biz')
    def test_shouldnt_substitute_default_address_in_forms_biz(self):
        data = {
            'hooks': [{
                'triggers': [1, 2],
                'survey': self.survey.pk,
                'subscriptions': [{
                    'service_type_action': 3,
                    'email_from_address': 'admin@domain.com',
                    'title': 'subject',
                    'email_to_address': 'user@domain.com',
                    'body': 'body',
                }]
            }]
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['hooks']), 1)
        hook = response.data['hooks'][0]
        self.assertEqual(len(hook['subscriptions']), 1)
        subscription = hook['subscriptions'][0]
        self.assertIsNotNone(subscription['email_from_address'])
        self.assertEqual(subscription['email_from_address'], f'{self.survey.pk}@forms-mailer.yaconnect.com')
