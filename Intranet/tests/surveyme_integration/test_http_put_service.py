# -*- coding: utf-8 -*-
import responses

from django.test import TestCase

from events.common_app.testutils import get_content_type_and_boundary, parse_multipart
from events.surveyme_integration.services.http.services import PutHTTPActionProcessor
from events.surveyme_integration.helpers import HTTPServiceBaseTestCaseMixin


class HTTPServiceTest__put(HTTPServiceBaseTestCaseMixin, TestCase):

    @responses.activate
    def test_should_send_data(self):
        self.register_uri(self.context_data['url'], method=responses.PUT)

        self.do_service_action('put', context=self.context_data)  # BANG!

        self.assertEqual(len(responses.calls), 1)
        _, boundary = get_content_type_and_boundary(responses.calls[0].request)
        boundary = boundary.encode()
        parsed_multipart = parse_multipart(boundary, responses.calls[0].request.body)
        self.assertEqual(parsed_multipart['hello']['data'], self.context_data['body_data']['hello'])
        self.assertEqual(self.service_instance.default_action_processor_class, PutHTTPActionProcessor)
