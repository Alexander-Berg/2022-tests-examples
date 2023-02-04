#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Тестируем боевую конфигурацию :)
"""
import unittest
import sys
import urllib.request, urllib.error, urllib.parse
import ssl
import threading

sys.path.append(".")
import stocks3
from stocks3.core.exception import S3Exception
import time
from stocks3.share.curl import get_http_code_retry
from stocks3.core.source import SourceLoader
from stocks3.export.exporter import make_exporter


class Profiler(object):
    def __init__(self, message):
        self.message = message

    def __enter__(self):
        self._startTime = time.time()

    def __exit__(self, type_, value, traceback):
        print("Elapsed time: {0:.3f} sec in section {1}".format(time.time() - self._startTime, self.message))


SOURCES_QUANTITY = 31
disabled_checkers_checks = [40003, 40068, 13, 17, 2506, 12]
disables_checkers = ['TimeFilterChecker']


class Worker(threading.Thread):
    def __init__(self, source_name, source):
        threading.Thread.__init__(self)
        self._source_name = source_name
        self._source = source
        self._stop_event = threading.Event()
        self.new_failed_prices = []
        self.failed_prices = []
        self.message = ''
        self.res = True

    def stop(self):
        self._stop_event.set()

    def stopped(self):
        return self._stop_event.isSet()

    def get_source(self):
        return self._source_name

    def run(self):
        self.res = True
        self.failed_prices = []
        try:
            try:
                self.failed_prices = self._source.transfer(test=True)
                for price in self.failed_prices:
                    if price.quote.quote_id not in disabled_checkers_checks:
                        self.new_failed_prices.append(price)
                self.failed_prices = self.new_failed_prices
            except S3Exception as msg:
                self.res = False
                self.message = 'Src: ' + self._source_name + ' Msg:' + str(msg)
            except KeyboardInterrupt:
                sys.exit(-1)
            except Exception as msg:
                self.res = False
                self.message = 'Src: ' + self._source_name + ' Msg:' + str(msg)
            finally:
                self._source.clean()
        except Exception as e:
            self.message = "CLEANERROR: %s: %s\n" % (self._source_name, e)
            self.res = False
        finally:
            self.stop()


class ImportsTest(unittest.TestCase):
    def setUp(self):
        stocks3.load_modules()
        loader = SourceLoader()
        self.sources = list(loader.get_active_sources())
        self.exporter = make_exporter(self.sources)

    def test_urls(self):
        # Тестируем урлы
        failed_urls = []
        sources_list = list(self.sources)
        for sourceName, source in sources_list:
            for transport in source.transports:
                url = transport.url if 'url' in transport.__dict__ else None
                if url is not None:
                    http_code, http_message = get_http_code_retry(url)
                    if http_code not in [200, 401, 403]:
                        failed_urls.append('Url {0} is not responding right. Return code is {1} ({2})'.format(
                            url, http_code, http_message))
                    self.assertTrue(http_code in [200, 401, 403],
                                    'Url {0} is not responding right. Return code is {1}({2})'.format(
                                        url, http_code, http_message))
        # self.assertTrue(len(failed_urls) == 0, failed_urls)

        self.assertEqual(len(sources_list), SOURCES_QUANTITY,
                         'Not matched sources count! (%d|%d)' % (len(sources_list), SOURCES_QUANTITY))
        self.assertTrue(len(sources_list) < 100, 'Too much sources')

    def test_data(self):
        # Тестируем source
        res = True
        message = 'Some strange happens while import'
        workers = []
        try:
            for sourcce_name, source in self.sources:
                w = Worker(sourcce_name, source)
                workers.append(w)
                w.start()
            for worker in workers:
                while not worker.stopped():
                    time.sleep(1)
                if not worker.res:
                    res = False
                    message = worker.message
                self.assertEqual(len(worker.failed_prices), 0, 'Failed checks ' +
                                 ' % '.join(['{0}/{1}'.format(pr.quote.quote_id, worker._source_name)
                                             for pr in worker.failed_prices[0:3]]))
        except Exception as msg:
            res = False
            message = str(msg)
        self.assertTrue(res, message)


if __name__ == "__main__":
    unittest.main()
    # suite = unittest.TestLoader().loadTestsFromTestCase(ImportsTest)
    # result = unittest.TestResult()
    # suite.run(result)
    # failure = 0 if len(result.errors) == 0 and len(result.failures) == 0 else 2
    # message = 'Tests run: {0}, Errors: {1}, Failures: {2}'.format(
    #               result.testsRun, len(result.errors), len(result.failures))
    # print('PASSIVE-CHECK:{0};{1};{2}'.format('StocksImportTests', failure, message))
