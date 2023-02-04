# -*- coding: utf-8 -*-
import json
import responses

from django.test import TestCase

from events.surveyme_integration.factories import JSONRPCSubscriptionDataFactory
from events.surveyme_integration.helpers import IntegrationTestMixin


class TestJSONRPCIntegration(IntegrationTestMixin, TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        super().setUp()

        self.subscription.service_type_action_id = 6  # json-rpc post
        self.subscription.http_url = 'http://yandex.ru/test_url/'
        self.subscription.save()

        JSONRPCSubscriptionDataFactory(
            subscription=self.subscription,
            method='create_referer',
        )

    def get_request_callback(self, status_code):
        def callback(request):
            headers = dict(request.headers)
            headers.update({
                'x-mark': 'mark'
            })
            return status_code, headers, 'hello'
        return callback

    def register_uri(self, status_code=200):
        responses.add_callback(
            responses.POST,
            self.subscription.http_url,
            callback=self.get_request_callback(status_code)
        )

    @responses.activate
    def test_request_content_type(self):
        self.register_uri()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers.get('content-type'), 'application/json')

    @responses.activate
    def test_should_send_json_body(self):
        self.register_uri()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        try:
            body = json.loads(responses.calls[0].request.body)
        except ValueError as e:
            self.fail('Body should contain json data. But errors raised "%s"' % e)
        self.assertTrue('method' in body)
        self.assertTrue('params' in body)
        self.assertTrue('id' in body)
        self.assertTrue('jsonrpc' in body)

    @responses.activate
    def test_should_send_json_rpc_version(self):
        self.register_uri()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(json.loads(responses.calls[0].request.body)['jsonrpc'], '2.0')

    @responses.activate
    def test_should_send_method(self):
        self.register_uri()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(json.loads(responses.calls[0].request.body)['method'], self.subscription.json_rpc.method)

    @responses.activate
    def test_should_send_id(self):
        self.register_uri()

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        notification = self.subscription.get_all_notifications().first()
        self.assertEqual(json.loads(responses.calls[0].request.body)['id'], str(notification.id))

    @responses.activate
    def test_should_send_params(self):
        self.register_uri()

        self.subscription.json_rpc.params.create(
            name="hello",
            value="world"
        )
        self.subscription.json_rpc.params.create(
            name="пока",
            value="луна"
        )

        self.post_data()  # BANG!

        self.assertEqual(len(responses.calls), 1)
        expected = {
            'hello': 'world',
            'пока': 'луна'
        }
        self.assertEqual(json.loads(responses.calls[0].request.body)['params'], expected)
