# -*- coding: utf-8 -*-
import json
import responses

from django.test import TestCase

from events.surveyme_integration.helpers import HTTPServiceBaseTestCaseMixin


class HTTPServiceTestMixin__arbitraty_put(HTTPServiceBaseTestCaseMixin, TestCase):

    def setUp(self):
        super().setUp()
        self.context_data = {
            'url': 'http://yandex.ru/test_url/',
            'body_data': '{"hello": "world"}',
            'method': 'put',
            'headers': {'Content-Type': 'application/json'},
        }

    @responses.activate
    def test_should_make_post_requst(self):
        self.register_uri(self.context_data['url'], method=responses.PUT)

        self.do_service_action('arbitrary', context=self.context_data)  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['Content-Type'], 'application/json')
        self.assertEqual(json.loads(responses.calls[0].request.body), {"hello": "world"})
        self.assertEqual(responses.calls[0].request.method, 'PUT')
