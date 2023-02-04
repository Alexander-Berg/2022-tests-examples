# -*- coding: utf-8 -*-
from django.test import TestCase
from django.utils.encoding import force_str
from events.surveyme_integration.services.http.context_processors import (
    HTTPContextProcessor,
)
from events.surveyme_integration.helpers import (
    ContextProcessorMixin,
    RenderedCharFieldTestMixin,
    HeadersFieldMixin,
    HTTPAttachmentsFieldMixin,
    HTTPBodyDataFieldMixin,
)


class TestHTTPContextProcessor(ContextProcessorMixin, TestCase):
    fixtures = ['initial_data.json']
    context_processor_class = HTTPContextProcessor


class TestHTTPContextProcessor__test_renderability_for_url(RenderedCharFieldTestMixin, TestCase):
    fixtures = ['initial_data.json']
    field_name = 'url'
    subscription_source = 'http_url'
    serializer_class = HTTPContextProcessor


class TestHTTPContextProcessor__test_headers_field_headers(HeadersFieldMixin, TestCase):
    serializer_class = HTTPContextProcessor
    field_name = 'headers'
    fixtures = ['initial_data.json']

    def required_default_headers(self, service_context_instance):
        return {
            'X-DELIVERY-ID': service_context_instance.notification_unique_id,
            'X-FORM-ID': force_str(service_context_instance.answer.survey_id),
            'X-FORM-ANSWER-ID': force_str(service_context_instance.answer.id),
        }


class TestHTTPContextProcessor__test_http_attachments_field_attachments(HTTPAttachmentsFieldMixin, TestCase):
    fixtures = ['initial_data.json']
    field_name = 'attachments'
    expected_formats = ['json']
    serializer_class = HTTPContextProcessor
    selective = True


class TestHTTPContextProcessor__test_http_body_data_field_body_data(HTTPBodyDataFieldMixin, TestCase):
    fixtures = ['initial_data.json']
    field_name = 'body_data'
    expected_formats = ['json', 'xml']
    serializer_class = HTTPContextProcessor
    selective = True
