#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Модуль тестирования, агрегирующий все написанные тесты.
"""
import unittest
import sys
import os
wdir = os.path.dirname(os.path.abspath(sys.argv[0]))
wdir = wdir[:wdir.rfind('/')]
os.chdir(wdir)
sys.path.append(".")
from test_depends import *
from test_config import *
from test_finam import *
from test_math import *
from test_updown import *
from test_imports import *
from test_gas import *
from stocks3.share.locker import locker


__author__ = "Bogdanov Evgeniy"
__email__ = "evbogdanov@yandex-team.ru"


if __name__ == "__main__":
    if not locker.try_lock():
        print('PASSIVE-CHECK:{0};{1};{2}'.format('stocks_tests', '1', 'Cannot create lock - second instance is running'))
        sys.exit(1)

    suite = unittest.TestSuite()
    testloader = unittest.TestLoader()
    suite.addTest(testloader.loadTestsFromTestCase(DependsTest))
    suite.addTest(testloader.loadTestsFromTestCase(ConfigTest))
    suite.addTest(testloader.loadTestsFromTestCase(TestMath))
    suite.addTest(testloader.loadTestsFromTestCase(TestUpDown))
    suite.addTest(testloader.loadTestsFromTestCase(GasImportsTest))
    suite.addTest(testloader.loadTestsFromTestCase(TestFinam))
    suite.addTest(testloader.loadTestsFromTestCase(ImportsTest))

    result = unittest.TestResult()
    suite.run(result)
    failure = 0 if len(result.errors) == 0 and len(result.failures) == 0 else 1

    if failure == 1:
        err_messages = []
        for error in result.errors + result.failures:
            try:
                err_messages.append(error[1].replace('\n', ' ').rstrip()[error[1].find('AssertionError:') +
                                            len('AssertionError: ') if error[1].find('AssertionError:') > 0 else 0:][:200])
            except Exception as message:
                print('silly exception ', message)
                err_messages.append(error[1].replace('\n', ' ').rstrip())
        error_description = 'Error: ' + ' | '.join(err_messages)
    else:
        error_description = ''
    message = 'Tests R:{0}, E:{1}/{2}. {3}'.format(result.testsRun, len(result.errors), len(result.failures),
                                                      # error_description[:200])
                                                      error_description)
    print('PASSIVE-CHECK:{0};{1};{2}'.format('stocks_tests', failure, message))

    locker.unlock()
