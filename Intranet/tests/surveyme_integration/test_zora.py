# -*- coding: utf-8 -*-
from django.conf import settings
from django.test import TestCase, override_settings
from json import dumps as json_dumps
from requests import Session
from unittest.mock import patch, ANY

from events.accounts.factories import OrganizationFactory
from events.accounts.helpers import YandexClient
from events.surveyme.factories import SurveyFactory
from events.surveyme_integration.factories import (
    SurveyHookFactory,
    ServiceSurveyHookSubscriptionFactory,
)
from events.surveyme_integration.models import ServiceTypeAction
from events.yauth_contrib.auth import TvmAuth, OAuth


class TestHttpIntegration(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.profile = self.client.login_yandex(is_superuser=True)
        self.survey = SurveyFactory(
            user=self.profile,
            org=OrganizationFactory(),
            is_published_external=True,
            is_public=True,
        )
        self.survey_hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(
            survey_hook=self.survey_hook,
            service_type_action_id=ServiceTypeAction.objects.get(slug='arbitrary').pk,
            http_method='post',
            http_url='https://httpbin.com/post',
            body=json_dumps({
                'one': 1,
                'two': 2,
            }),
        )

    def test_should_invoke_https_request_for_not_yandex(self):
        with patch.object(Session, 'request') as mock_make_request:
            response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk)
            self.assertEqual(response.status_code, 200)

        mock_make_request.assert_called_once_with(
            'POST',
            'https://httpbin.com/post',
            auth=ANY,
            headers=ANY,
            data=self.subscription.body.encode(),
            proxies={
                'http': 'http://forms:123@go.zora.yandex.net:1080/',
                'https': 'http://forms:123@go.zora.yandex.net:1080/',
            },
            timeout=settings.GO_ZORA_TIMEOUT,
            cert=None,
            verify=False,
            json=None,
        )
        args, kwargs = mock_make_request.call_args_list[0]

        self.assertEqual(kwargs['headers']['X-Ya-User-Agent'], 'Yandex.Forms/1.0')

        self.assertTrue(isinstance(kwargs['auth'], TvmAuth))
        self.assertEqual(kwargs['auth'].tvm2_client_id, settings.GO_ZORA_TVM2_CLIENT)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_invoke_https_request_for_biz(self):
        with patch.object(Session, 'request') as mock_make_request:
            response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk)
            self.assertEqual(response.status_code, 200)

        mock_make_request.assert_called_once_with(
            'POST',
            'https://httpbin.com/post',
            auth=ANY,
            headers=ANY,
            data=self.subscription.body.encode(),
            proxies={
                'http': 'http://forms:123@go.zora.yandex.net:1080/',
                'https': 'http://forms:123@go.zora.yandex.net:1080/',
            },
            timeout=settings.GO_ZORA_TIMEOUT,
            cert=None,
            verify=False,
            json=None,
        )
        args, kwargs = mock_make_request.call_args_list[0]

        self.assertEqual(kwargs['headers']['X-Ya-User-Agent'], 'Yandex.Forms/1.0')

        self.assertTrue(isinstance(kwargs['auth'], TvmAuth))
        self.assertEqual(kwargs['auth'].tvm2_client_id, settings.GO_ZORA_TVM2_CLIENT)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_invoke_http_request_for_biz(self):
        self.subscription.http_url = 'http://httpbin.com/post'
        self.subscription.save()

        with patch.object(Session, 'request') as mock_make_request:
            response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk)
            self.assertEqual(response.status_code, 200)

        mock_make_request.assert_called_once_with(
            'POST',
            'http://httpbin.com/post',
            auth=ANY,
            headers=ANY,
            data=self.subscription.body.encode(),
            proxies={
                'http': 'http://forms:123@go.zora.yandex.net:1080/',
                'https': 'http://forms:123@go.zora.yandex.net:1080/',
            },
            timeout=settings.GO_ZORA_TIMEOUT,
            cert=None,
            verify=False,
            json=None,
        )
        args, kwargs = mock_make_request.call_args_list[0]

        self.assertEqual(kwargs['headers']['X-Ya-User-Agent'], 'Yandex.Forms/1.0')

        self.assertTrue(isinstance(kwargs['auth'], TvmAuth))
        self.assertEqual(kwargs['auth'].tvm2_client_id, settings.GO_ZORA_TVM2_CLIENT)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_invoke_http_request_for_biz_white_list(self):
        self.subscription.http_url = 'https://api.tracker.yandex.net/v2/issues/'
        self.subscription.save()

        with patch.object(Session, 'request') as mock_make_request:
            response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk)
            self.assertEqual(response.status_code, 200)

        mock_make_request.assert_called_once_with(
            'POST',
            'https://api.tracker.yandex.net/v2/issues/',
            auth=ANY,
            headers=ANY,
            data=self.subscription.body.encode(),
            timeout=settings.DEFAULT_TIMEOUT,
            cert=None,
            verify=settings.YANDEX_ROOT_CERTIFICATE,
            json=None,
        )

    def test_should_invoke_https_request_for_int_external_ru(self):
        self.subscription.http_url = 'https://external.ru/post'
        self.subscription.save()

        with patch.object(Session, 'request') as mock_make_request:
            response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk)
            self.assertEqual(response.status_code, 200)

        mock_make_request.assert_called_once_with(
            'POST',
            self.subscription.http_url,
            auth=ANY,
            headers=ANY,
            data=self.subscription.body.encode(),
            proxies={
                'http': 'http://forms:123@go.zora.yandex.net:1080/',
                'https': 'http://forms:123@go.zora.yandex.net:1080/',
            },
            timeout=settings.GO_ZORA_TIMEOUT,
            cert=None,
            verify=False,
            json=None,
        )
        args, kwargs = mock_make_request.call_args_list[0]

        self.assertEqual(kwargs['headers']['X-Ya-User-Agent'], 'Yandex.Forms/1.0')

        self.assertTrue(isinstance(kwargs['auth'], TvmAuth))
        self.assertEqual(kwargs['auth'].tvm2_client_id, settings.GO_ZORA_TVM2_CLIENT)

    def test_should_invoke_https_request_for_int_with_oauth(self):
        self.subscription.http_url = 'https://any.yandex-team.ru/post'
        self.subscription.save()

        with patch.object(Session, 'request') as mock_make_request:
            response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk)
            self.assertEqual(response.status_code, 200)

        mock_make_request.assert_called_once_with(
            'POST',
            self.subscription.http_url,
            auth=ANY,
            headers=ANY,
            data=self.subscription.body.encode(),
            timeout=settings.DEFAULT_TIMEOUT,
            cert=None,
            verify=settings.YANDEX_ROOT_CERTIFICATE,
            json=None,
        )
        args, kwargs = mock_make_request.call_args_list[0]

        self.assertTrue(isinstance(kwargs['auth'], OAuth))
        self.assertEqual(kwargs['auth'].token, settings.HTTP_INTEGRATION_OAUTH_TOKEN)

    def test_should_invoke_https_request_for_int_with_tvm_auth(self):
        self.subscription.http_url = 'http://any.yandex.net:8099/post'
        self.subscription.tvm2_client_id = '123456'
        self.subscription.save()

        with patch.object(Session, 'request') as mock_make_request:
            with patch.object(TvmAuth, '_get_service_ticket', return_value='123') as mock_get_ticket:
                response = self.client.post('/v1/surveys/%s/form/' % self.survey.pk)
                self.assertEqual(response.status_code, 200)

        mock_get_ticket.assert_called_once_with('123456')
        mock_make_request.assert_called_once_with(
            'POST',
            self.subscription.http_url,
            auth=ANY,
            headers=ANY,
            data=self.subscription.body.encode(),
            timeout=settings.DEFAULT_TIMEOUT,
            cert=None,
            verify=settings.YANDEX_ROOT_CERTIFICATE,
            json=None,
        )
        args, kwargs = mock_make_request.call_args_list[0]

        self.assertTrue(isinstance(kwargs['auth'], TvmAuth))
        self.assertEqual(kwargs['auth'].tvm2_client_id, self.subscription.tvm2_client_id)
