# -*- coding: utf-8 -*-
import json
import responses

from django.test import TestCase
from unittest.mock import patch

from events.surveyme_integration.helpers import HTTPServiceBaseTestCaseMixin
from events.yauth_contrib.auth import TvmAuth


class HTTPServiceTestMixin__arbitraty_post(HTTPServiceBaseTestCaseMixin, TestCase):

    def setUp(self):
        super().setUp()
        self.context_data = {
            'url': 'http://yandex.ru/test_url/',
            'body_data': '{"hello": "world"}',
            'method': 'post',
            'headers': {'Content-Type': 'application/json'},
        }

    @responses.activate
    def test_should_make_post_requst(self):
        self.register_uri(self.context_data['url'])

        self.do_service_action('arbitrary', context=self.context_data)  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['Content-Type'], 'application/json')
        self.assertEqual(json.loads(responses.calls[0].request.body), {"hello": "world"})
        self.assertEqual(responses.calls[0].request.method, 'POST')

    @responses.activate
    def test_should_send_tvm2_header(self):
        self.context_data['tvm2_client_id'] = '2002680'
        self.context_data['url'] = 'http://smth.yandex-team.ru/test_url/'
        self.register_uri(self.context_data['url'])

        with patch.object(TvmAuth, '_get_service_ticket', return_value='123') as mock_get_ticket:
            self.do_service_action('arbitrary', context=self.context_data)  # BANG!
        mock_get_ticket.assert_called_once_with('2002680')

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['Content-Type'], 'application/json')
        self.assertEqual(json.loads(responses.calls[0].request.body), {"hello": "world"})
        self.assertEqual(responses.calls[0].request.headers['X-Ya-Service-Ticket'], '123')

    @responses.activate
    def test_should_encode_post_request(self):
        self.register_uri(self.context_data['url'])

        self.do_service_action('arbitrary', context=self.context_data)  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertTrue(isinstance(responses.calls[0].request.body, bytes))
        self.assertEqual(responses.calls[0].request.method, 'POST')
