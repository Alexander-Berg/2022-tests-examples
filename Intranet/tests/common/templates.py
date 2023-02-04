import logging

from django.template.loader import render_to_string
from django.test import TestCase


logger = logging.getLogger(__name__)


class TemplateTestCase(TestCase):
    template_name = None
    basic_context = {}

    def assemble_context(self, current_context):
        context = self.basic_context.copy()
        context.update(current_context)
        return context

    def assert_template_contains(self, context, strings):
        context = self.assemble_context(context)
        rendered = render_to_string(self.template_name, context)
        for string in strings:
            try:
                self.assertIn(string, rendered)
            except AssertionError:
                logger.debug(string)
                logger.debug(rendered)
                raise

    def assert_template_not_contains(self, context, strings):
        context = self.assemble_context(context)
        rendered = render_to_string(self.template_name, context)
        for string in strings:
            try:
                self.assertNotIn(string, rendered)
            except AssertionError:
                logger.debug(string)
                logger.debug(rendered)
                raise
