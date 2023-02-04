# coding: utf-8
from __future__ import unicode_literals

from unittest import TestCase

from static_api.dehydrators import Dehydrator


class TestDehydrator(Dehydrator):
    fields = [
        'type',
        'name.en',
        'uid',
        'missing'
    ]

    def get_uid(self):
        return self.get_raw('user_uid')


class DehydratorTest(TestCase):
    def setUp(self):
        self.data = TestDehydrator({'type': 'user',
                                    'name': {'en': 'Alex',
                                             'ru': u'Александр'},
                                    'user_uid': 1234}).as_dict()

    def test_get_simple(self):
        self.assertEqual(self.data['type'], 'user')

    def test_get_nested(self):
        self.assertEqual(self.data['name']['en'], 'Alex')

    def test_get_method(self):
        self.assertEqual(self.data['uid']['user_uid'], 1234)

    def test_missing(self):
        self.assertTrue('missing' not in self.data)
