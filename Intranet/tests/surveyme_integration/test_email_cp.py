# -*- coding: utf-8 -*-
from django.test import TestCase

from events.surveyme_integration.services.email.context_processors import EmailContextProcessor
from events.surveyme_integration.helpers import (
    AttachmentsFieldMixin,
    HeadersFieldMixin,
    RenderedCharFieldTestMixin,
)


class TestEmailContextProcessor___headers_field_headers(HeadersFieldMixin, TestCase):
    serializer_class = EmailContextProcessor
    field_name = 'headers'
    fixtures = ['initial_data.json']

    def required_default_headers(self, service_context_instance):
        return {
            'MESSAGE-ID': '<%s>' % self.get_message_id(service_context_instance.notification_unique_id),
            'X-Form-ID': str(service_context_instance.answer.survey_id),
        }


class TestEmailContextProcessor___attachments_field_attachments(AttachmentsFieldMixin, TestCase):
    serializer_class = EmailContextProcessor
    field_name = 'attachments'
    fixtures = ['initial_data.json']
    selective = True


class TestEmailContextProcessor___renderability_for_subject(RenderedCharFieldTestMixin, TestCase):
    serializer_class = EmailContextProcessor
    field_name = 'subject'
    subscription_source = 'title'
    fixtures = ['initial_data.json']


class TestEmailContextProcessor___renderability_for_body(RenderedCharFieldTestMixin, TestCase):
    serializer_class = EmailContextProcessor
    field_name = 'body'
    subscription_source = 'body'
    fixtures = ['initial_data.json']


class TestEmailContextProcessor___renderability_for_from_address(RenderedCharFieldTestMixin, TestCase):
    serializer_class = EmailContextProcessor
    field_name = 'from_address'
    subscription_source = 'email_from_address'
    fixtures = ['initial_data.json']


class TestEmailContextProcessor___renderability_for_to_address(RenderedCharFieldTestMixin, TestCase):
    serializer_class = EmailContextProcessor
    field_name = 'to_address'
    subscription_source = 'email_to_address'
    fixtures = ['initial_data.json']
