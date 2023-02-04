# -*- coding: utf-8 -*-


from unittest.mock import Mock
from django.test import TestCase

from events.common_app.music import MusicStatusClient


class TestMusicStatusClient(TestCase):
    def test_get_status_should_return_valid_status(self):
        client = MusicStatusClient()
        self.assertEqual('not-mobile', client.get_status('123456789'))

    def test_get_status_should_return_none_result(self):
        client = MusicStatusClient()
        client.get_data = Mock(return_value=None)
        self.assertIsNone(client.get_status('123456789'))

    def test_get_status_should_raise_exception(self):
        def _get_data(self, *args, **kwargs):
            raise ValueError
        client = MusicStatusClient()
        client.get_data = _get_data
        with self.assertRaises(ValueError):
            client.get_status('123456789')
