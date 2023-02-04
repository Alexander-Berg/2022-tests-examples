# -*- coding: utf-8 -*-
import os

from intranet.yandex_directory.src.settings import get_setting

from testutils import TestCase


class Test__get_setting(TestCase):
    def test_get_setting_should_return_python_true_or_false(self):
        experiments = [
            {'setting_value': 'true', 'expected_value': True},
            {'setting_value': 'True', 'expected_value': True},
            {'setting_value': 'false', 'expected_value': False},
            {'setting_value': 'False', 'expected_value': False},
            {'setting_value': 'test_value', 'expected_value': 'test_value'},
        ]
        for experiment in experiments:
            test_setting_name = 'CURRENT_USER'
            os.environ[test_setting_name] = experiment['setting_value']
            value = get_setting(test_setting_name)
            self.assertEqual(value, experiment['expected_value'])
