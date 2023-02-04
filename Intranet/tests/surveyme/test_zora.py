# -*- coding: utf-8 -*-
from django.test import TestCase, override_settings
from json import dumps as json_dumps

from events.accounts.factories import OrganizationFactory
from events.accounts.helpers import YandexClient
from events.surveyme.factories import SurveyFactory
from events.surveyme.utils import check_if_internal_host
from events.surveyme_integration.models import ServiceTypeAction


class TestCheckInternalHost(TestCase):
    def test_check_if_internal_host(self):
        self.assertTrue(check_if_internal_host('yandex.net'))
        self.assertTrue(check_if_internal_host('yandex-team.ru'))
        self.assertTrue(check_if_internal_host('test.yandex.ru'))
        self.assertTrue(check_if_internal_host('any.yandex.net'))
        self.assertTrue(check_if_internal_host('any.yandex-team.ru'))
        self.assertTrue(check_if_internal_host('any.test.yandex.ru'))
        self.assertTrue(check_if_internal_host('forms-any-api.yandex.ru'))

    def test_check_if_not_internal_host(self):
        self.assertFalse(check_if_internal_host('any.yandex.ru'))
        self.assertFalse(check_if_internal_host('yandex.ru'))
        self.assertFalse(check_if_internal_host('httpbin.com'))


class TestHttpIntegration(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(user=self.profile, org=OrganizationFactory())
        self.http_action = ServiceTypeAction.objects.get(slug='arbitrary')

    def test_should_modify_http_interation_for_int(self):
        self.survey.org = None
        self.survey.save()
        data = {
            'hooks': [
                {
                    'triggers': [1, 2],
                    'subscriptions': [{
                        'service_type_action': self.http_action.pk,
                        'http_method': 'post',
                        'http_url': 'https://yandex.ru/test',
                        'body': json_dumps({
                            'one': 1,
                            'two': 2,
                        }),
                    }],
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_modify_http_interation_for_biz(self):
        data = {
            'hooks': [
                {
                    'triggers': [1, 2],
                    'subscriptions': [{
                        'service_type_action': self.http_action.pk,
                        'http_method': 'post',
                        'http_url': 'https://httpbin.com/post',
                        'body': json_dumps({
                            'one': 1,
                            'two': 2,
                        }),
                    }],
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_modify_http_interation_if_not_in_organization(self):
        self.survey.org = None
        self.survey.save()
        data = {
            'hooks': [
                {
                    'triggers': [1, 2],
                    'subscriptions': [{
                        'service_type_action': self.http_action.pk,
                        'http_method': 'post',
                        'http_url': 'https://httpbin.com/post',
                        'body': json_dumps({
                            'one': 1,
                            'two': 2,
                        }),
                    }],
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 200)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_modify_http_interation_if_not_supported_method(self):
        data = {
            'hooks': [
                {
                    'triggers': [1, 2],
                    'subscriptions': [{
                        'service_type_action': self.http_action.pk,
                        'http_method': 'patch',
                        'http_url': 'https://httpbin.com/post',
                        'body': json_dumps({
                            'one': 1,
                            'two': 2,
                        }),
                    }],
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_modify_http_interation_if_not_supported_scheme(self):
        data = {
            'hooks': [
                {
                    'triggers': [1, 2],
                    'subscriptions': [{
                        'service_type_action': self.http_action.pk,
                        'http_method': 'post',
                        'http_url': 'ftp://httpbin.com/ftp',
                        'body': json_dumps({
                            'one': 1,
                            'two': 2,
                        }),
                    }],
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_modify_http_interation_if_internal_host(self):
        data = {
            'hooks': [
                {
                    'triggers': [1, 2],
                    'subscriptions': [{
                        'service_type_action': self.http_action.pk,
                        'http_method': 'post',
                        'http_url': 'https://any.yandex.net/post',
                        'body': json_dumps({
                            'one': 1,
                            'two': 2,
                        }),
                    }],
                },
            ],
        }
        response = self.client.patch('/admin/api/v2/surveys/%s/' % self.survey.pk, data=data, format='json')
        self.assertEqual(response.status_code, 400)
