#!/usr/bin/python
# -*- coding: utf-8 -*-

import os

__author__ = 'spooner'


class Settings():
    root_dir = './test_case_dir/'
    owd = os.getcwd() + '/test_case_dir/'
    root_log_dir = owd + 'var/log/'

    empty_owd = os.getcwd() + '/empty_test_case_dir/'
    empty_root_log_dir = empty_owd + 'var/log/'

    test_non_empty_main_config = './tests/files_for_test/non-empty_push-client-{0}.yaml'
    test_empty_main_config = './tests/files_for_test/empty_push-client-{0}.tpl'

    valid_stdout = [
        {'name': 'var/log/rabota/tests-rabota.log',
         'fakename': '/rabota/tests-rabota.log'},
        {'name': 'var/log/rabota/tests-rabota.shell',
         'fakename': '/rabota/tests-rabota.shell'},
        {'name': 'var/log/review/test1.log',
         'fakename': '/review/test1.log'},
        {'name': 'var/log/review/test1.log.shell',
         'fakename': '/review/test1.log.shell'}
    ]

    test_create_dirs = [
        'var/log/backctld',
        'var/log/graphite-client',
        'var/log/lighttpd',
        'var/log/sysstat',
        'var/log/restarter',
        'var/log/rabota',
        'var/log/qas-backend',
        'var/log/review',
        'var/log/rsync'
    ]

    test_files_to_touch = [
        'var/log/rabota/tests-rabota.log',
        'var/log/rabota/tests-rabota.shell',
        'var/log/graphite-client/test1.log',
        'var/log/review/test1.log',
        'var/log/review/test1.log.shell',
        'var/log/sysstat/test1.log',
        'var/log/restarter/1.log',
        'var/log/rsync/1.shell.log'
    ]


def main():
    pass


if __name__ == '__main__':
    main()
