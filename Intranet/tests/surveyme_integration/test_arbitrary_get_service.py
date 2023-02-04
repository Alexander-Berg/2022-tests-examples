# -*- coding: utf-8 -*-
import responses

from django.test import TestCase

from events.surveyme_integration.helpers import HTTPServiceBaseTestCaseMixin


class HTTPServiceTest___arbitraty_get(HTTPServiceBaseTestCaseMixin, TestCase):

    def setUp(self):
        super().setUp()
        self.context_data = {
            'url': 'http://yandex.ru/test_url/',
            'method': 'get',
            'headers': None,
        }

    @responses.activate
    def test_should_make_get_request(self):
        self.register_uri(self.context_data['url'], method=responses.GET)

        self.do_service_action('arbitrary', context=self.context_data)  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.method, 'GET')
