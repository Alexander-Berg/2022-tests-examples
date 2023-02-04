#!/usr/bin/python
# -*- coding: utf-8 -*-

__author__ = 'spooner'

import unittest
import sys
import os
import shutil
import socket

sys.path.append('./')

from bin import statbox_append_log as statbox
import settings as test_settings

class BaseTestClass(unittest.TestCase):
    def setUp(self):
        self.hostname = socket.getfqdn().rstrip()
        self.init_test_settings = test_settings.Settings()
        self.testing_root_dir = self.init_test_settings.root_dir
        if not os.path.exists(self.testing_root_dir):
            os.makedirs(self.testing_root_dir)
        for i in self.init_test_settings.test_create_dirs:
            if not os.path.exists(self.testing_root_dir + i):
                os.makedirs(self.testing_root_dir + i)
        for i in self.init_test_settings.test_files_to_touch:
            if not os.path.exists(self.testing_root_dir + i):
                open(self.testing_root_dir + i, 'a').close()

        if not os.path.exists(self.init_test_settings.empty_root_log_dir):
            os.makedirs(self.init_test_settings.empty_root_log_dir)
        if not os.path.exists('./tests/files_for_test'):
            shutil.copytree('./tests/files', './tests/files_for_test')
        self.init_class = statbox.AddLog(default_all_logs_root=self.init_test_settings.root_log_dir,
                                         default_main_config=self.init_test_settings.test_non_empty_main_config,
                                         default_main_config_template=self.init_test_settings.test_empty_main_config)

    def tearDown(self):
        shutil.rmtree(self.testing_root_dir)

    def test_without_config(self):
        try_count = 0
        while try_count != 2:
            valid_stdout = list()
            for i in self.init_test_settings.valid_stdout:
                valid_stdout_config = dict()
                valid_stdout_config['name'] = self.init_test_settings.owd + i['name']
                valid_stdout_config['fakename'] = '/' + self.hostname + i['fakename']
                valid_stdout.append(valid_stdout_config)

            logs_to_append = self.init_class.add_all_files(create_config='False')
            logs_to_append = sorted(logs_to_append, key=lambda k: k['name'])
            valid_stdout = sorted(valid_stdout, key=lambda k: k['name'])
            self.assertListEqual(logs_to_append, valid_stdout)
            try_count += 1

    def test_empty_log_list(self):
        self.init_class = statbox.AddLog(default_all_logs_root=self.init_test_settings.empty_root_log_dir,
                                         default_main_config=self.init_test_settings.test_non_empty_main_config,
                                         default_main_config_template=self.init_test_settings.test_empty_main_config)
        self.init_class.add_all_files(create_config='True')
        assert not os.path.exists(self.init_test_settings.test_non_empty_main_config)



    def test_with_config(self):
        self.init_class.add_all_files(create_config='True')


if __name__ == '__main__':
    unittest.main()
