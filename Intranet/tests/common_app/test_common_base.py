# -*- coding: utf-8 -*-
import datetime
from django.test import TestCase


def toggle_indexer(aClass):
    """Декоратор класса, короый отключает на время testcase индексатор"""
    # todo: test me
    pass


class TestDateAsserts(TestCase):
    def assert_equals_dates(self, date1, date2, msg=''):
        date1 = self.get_date_withot_ms(date1)
        date2 = self.get_date_withot_ms(date2)
        self.assertEqual(date1, date2, msg=msg)

    def get_date_withot_ms(self, date):
        """Removes microseconds from date

        @type date: datetime.datetime
        @rtype: datetime.datetime

        """
        return datetime.datetime(*tuple(date.timetuple())[:6])


class TestUtilsAsserts(TestCase):
    def assertIsMarkdownRenderOfField(self, model_instance, expected_markdown_rendered_field, field_with_markdown, msg_prefix=''):
        setattr(model_instance, field_with_markdown, '*привет*')
        msg = msg_prefix + 'поле "{field_1}" не возвращает отрендеренный markdown из поля "{field_2}"'.format(
            field_1=expected_markdown_rendered_field,
            field_2=field_with_markdown,
        )
        self.assertEqual(getattr(model_instance, expected_markdown_rendered_field, None), '<p><em>привет</em></p>', msg=msg)
