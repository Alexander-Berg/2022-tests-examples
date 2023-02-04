# -*- coding: utf-8 -*-
from django.test import TestCase

from events.surveyme_integration.factories import (
    ServiceSurveyHookSubscriptionFactory,
    SubscriptionHeaderFactory,
    SurveyHookFactory,
    JSONRPCSubscriptionDataFactory,
    JSONRPCSubscriptionParamFactory,
)
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.services.base.context_processors.base import ServiceContextInstance
from events.surveyme_integration.services.json_rpc.context_processors import JSONRPCContextProcessor


class TestJSONRPCContextProcessor(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.answer = ProfileSurveyAnswerFactory()
        self.survey = self.answer.survey
        self.hook = SurveyHookFactory(survey=self.survey)
        self.subscription = ServiceSurveyHookSubscriptionFactory(survey_hook=self.hook)
        self.headers = [
            SubscriptionHeaderFactory(
                subscription=self.subscription,
                name='Authorization',
                value='OAuth test',
            ),
        ]
        self.json_rpc_data = JSONRPCSubscriptionDataFactory(subscription=self.subscription, method='foo')
        self.trigger_data = {}
        self.notification_unique_id = '123'
        self.service_context_instance = ServiceContextInstance(
            subscription=self.subscription,
            answer=self.answer,
            trigger_data=self.trigger_data,
            notification_unique_id=self.notification_unique_id,
        )

    def test_without_json_rpc_params(self):
        exp_data = {
            'notification_unique_id': self.notification_unique_id,
            'survey_id': self.survey.id,
            'answer_id': self.answer.id,
            'subscription_id': self.subscription.id,
            'org_dir_id': None,
            'force_render': False,
            'url': '',
            'ip': '5.255.219.135',
            'tvm2_client_id': None,
            'body_data': {
                'params': {},
                'jsonrpc': '2.0',
                'method': 'foo',
                'id': '123'
            },
            'request_id': '',
        }

        context_processor = JSONRPCContextProcessor(self.service_context_instance)
        response = context_processor.data
        headers = response.pop('headers', {})
        self.assertIn('content-type', headers)
        self.assertEqual(headers['content-type'], 'application/json')
        self.assertIn('Authorization', headers)
        self.assertEqual(headers['Authorization'], 'OAuth test')
        self.assertEqual(response, exp_data)

    def test_with_json_rpc_params(self):
        for param, value in [('param_1', 'value_1'), ('param_2', 'value_2')]:
            JSONRPCSubscriptionParamFactory(
                subscription=self.json_rpc_data,
                name=param,
                value=value,
                add_only_with_value=False,
            )
        exp_data = {
            'notification_unique_id': self.notification_unique_id,
            'survey_id': self.survey.id,
            'answer_id': self.answer.id,
            'subscription_id': self.subscription.id,
            'org_dir_id': None,
            'force_render': False,
            'url': '',
            'ip': '5.255.219.135',
            'tvm2_client_id': None,
            'body_data': {
                'params': {
                    'param_1': 'value_1',
                    'param_2': 'value_2',
                },
                'jsonrpc': '2.0',
                'method': 'foo',
                'id': '123'
            },
            'request_id': '',
        }

        context_processor = JSONRPCContextProcessor(self.service_context_instance)
        response = context_processor.data
        headers = response.pop('headers', {})
        self.assertIn('content-type', headers)
        self.assertEqual(headers['content-type'], 'application/json')
        self.assertIn('Authorization', headers)
        self.assertEqual(headers['Authorization'], 'OAuth test')
        self.assertEqual(response, exp_data)

    def test_with_json_rpc_params_and_only_with_value(self):
        params = [
            ('param_1', 'value_1'),
            ('param_2', 'value_2'),
            ('param_3', 'None'),
            ('param_4', ''),
            ('param_5', ' '),
        ]

        for param, value in params:
            JSONRPCSubscriptionParamFactory(
                subscription=self.json_rpc_data,
                name=param,
                value=value,
                add_only_with_value=True,
            )

        # Должны присутствовать всегда, т.к. add_only_with_value=False
        for param, value in [('param_6', 'None'), ('param_7', '')]:
            JSONRPCSubscriptionParamFactory(
                subscription=self.json_rpc_data,
                name=param,
                value=value,
                add_only_with_value=False,
            )

        exp_data = {
            'notification_unique_id': self.notification_unique_id,
            'survey_id': self.survey.id,
            'answer_id': self.answer.id,
            'subscription_id': self.subscription.id,
            'org_dir_id': None,
            'force_render': False,
            'url': '',
            'ip': '5.255.219.135',
            'tvm2_client_id': None,
            'body_data': {
                'params': {
                    'param_1': 'value_1',
                    'param_2': 'value_2',
                    'param_6': 'None',
                    'param_7': '',
                },
                'jsonrpc': '2.0',
                'method': 'foo',
                'id': '123'
            },
            'request_id': '',
        }

        context_processor = JSONRPCContextProcessor(self.service_context_instance)
        response = context_processor.data
        headers = response.pop('headers', {})
        self.assertIn('content-type', headers)
        self.assertEqual(headers['content-type'], 'application/json')
        self.assertIn('Authorization', headers)
        self.assertEqual(headers['Authorization'], 'OAuth test')
        self.assertEqual(response, exp_data)
