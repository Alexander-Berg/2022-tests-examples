# -*- coding: utf-8 -*-
from django.test import TestCase

from events.surveyme_integration.services.email.context_processors import EmailContextProcessor
from events.surveyme_integration.services.base.context_processors.fields import RenderedCharField
from events.surveyme_integration.services.base.context_processors.base import ContextProcessorBase
from events.surveyme_integration.helpers import (
    PrepareTextAndFileQuestionsWithAnswersMixin,
    RenderedCharFieldTestMixin,
)


class TestSerializer(ContextProcessorBase):
    title = RenderedCharField(source='subscription.title')

    class Meta(ContextProcessorBase.Meta):
        fields = ContextProcessorBase.Meta.fields + ['title']


class TestGroupFilesCase(PrepareTextAndFileQuestionsWithAnswersMixin, TestCase):
    fixtures = ['initial_data.json']

    def test_show_group_files_correct(self):
        self.init_files('avatar', 'resume', 'photos')
        serializer = EmailContextProcessor(self.service_context_instance)
        results = {
            attachment['filename']
            for attachment in serializer.data['attachments']
        }
        expected = {'ava.jpeg', 'resume.txt', 'photo_1.jpeg', 'photo_2.jpeg'}
        self.assertSetEqual(results, expected)


class TestTestSerializer___renderability_for_title(RenderedCharFieldTestMixin, TestCase):
    fixtures = ['initial_data.json']
    field_name = 'title'
    subscription_source = 'title'
    serializer_class = TestSerializer
