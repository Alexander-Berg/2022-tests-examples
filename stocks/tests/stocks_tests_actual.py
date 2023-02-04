#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тестируем всё
"""
import unittest
import sys
import os
wdir = os.path.dirname(os.path.abspath(sys.argv[0]))
wdir = wdir[:wdir.rfind('/')]
os.chdir(wdir)
from test_actual import ActualTest
from stocks3.share.locker import locker


__author__ = "Bogdanov Evgeniy"
__email__ = "evbogdanov@yandex-team.ru"

if __name__ == "__main__":
    if not locker.try_lock():
        print(
            'PASSIVE-CHECK:{0};{1};{2}'.format('stocks_tests_actual', '1', 'Cannot create lock - second instance is running'))
        sys.exit(1)

    suite = unittest.TestSuite()
    testloader = unittest.TestLoader()
    suite.addTest(testloader.loadTestsFromTestCase(ActualTest))

    result = unittest.TestResult()
    suite.run(result)
    failure = 0 if len(result.errors) == 0 and len(result.failures) == 0 else 1

    if failure == 1:
        err_messages = []
        for error in result.errors + result.failures:
            try:
                err_messages.append(error[1].replace('\n', ' ').rstrip()[error[1].find('AssertionError:') +
                                                                         len('AssertionError: ') if error[1].find(
                    'AssertionError:') > 0 else 0:][:200])
            except Exception as message:
                print('silly exception ', message)
                err_messages.append(error[1].replace('\n', ' ').rstrip())
        error_description = 'Error: ' + ' | '.join(err_messages)
        error_description = error_description.replace(' %', ',')
    else:
        error_description = ''
    message = 'Tests R:{0}, E:{1}/{2}. {3}'.format(result.testsRun, len(result.errors), len(result.failures),
                                                   # error_description[:200])
                                                   error_description)
    print('PASSIVE-CHECK:{0};{1};{2}'.format('stocks_tests_actual', failure, message))

    locker.unlock()
