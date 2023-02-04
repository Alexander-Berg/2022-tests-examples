# -*- coding: utf-8 -*-
from django.test import TestCase

from events.data_sources.utils import get_table_name, get_table_id


class TestUtils(TestCase):
    def test_get_table_name(self):
        self.assertEqual(
            get_table_name('https://yt.yandex-team.ru/hahn/navigate/?path=//home/forms/answers'),
            ('hahn', '//home/forms/answers'),
        )
        self.assertEqual(
            get_table_name('https://yandex.ru/test?folder=//home/forms/answers'),
            ('test', None),
        )
        self.assertEqual(
            get_table_name('https://yandex.ru'),
            (None, None),
        )

    def test_get_table_id(self):
        self.assertEqual(get_table_id('hahn', '//home/forms/answers'), '//home/forms/answers_hahn')
