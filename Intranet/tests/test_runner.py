import asyncio
import aiohttp

from unittest import TestCase, mock
from unittest.mock import MagicMock
from asynctest import CoroutineMock

from django.core.handlers.wsgi import WSGIRequest

from intranet.magiclinks.src.links.dto import Result, String, List
from intranet.magiclinks.src.links.dto import UrlObject
from intranet.magiclinks.src.links.runner import Runner
from intranet.magiclinks.src.links.utils.urls import UrlsParser
from intranet.magiclinks.src.links.utils.workers import WorkersManager
from . import isinstance_checker


class DummyPoolExecutor:
    def __init__(self, max_workers):
        self.max_workers = max_workers

    def map(self, fn, workers):
        return map(fn, workers)


class RunnerTestCase(TestCase):
    def setUp(self):
        self.runner = Runner(
            request=mock.create_autospec(WSGIRequest),
            workers_manager=WorkersManager(['one_worker', 'another_worker']),
            url_parser=mock.create_autospec(UrlsParser),
            pool_executor_class=DummyPoolExecutor,
        )
        self.loop = asyncio.get_event_loop()

    def test_runner_run(self):
        urls = ['url1', 'url2']
        url_objects = [
            UrlObject(url='url1', split_result=None, hostname_match=None,
                      path_match=None, worker_class_file='one_worker',
                      fragment_match=None, query_match=None,
                      ),
            UrlObject(url='url2', split_result=None, hostname_match=None,
                      path_match=None, worker_class_file='another_worker',
                      fragment_match=None, query_match=None,
                      )
        ]
        worker1, worker2 = CoroutineMock(), CoroutineMock()
        workers_map = {'one_worker': worker1,
                       'another_worker': worker2,
                       }
        self.runner.url_parser.parse_urls = MagicMock(return_value=url_objects)

        self.runner.workers_manager.create_workers_map = MagicMock(return_value={'one_worker': worker1,
                                                                                 'another_worker': worker2,
                                                                                 })
        self.runner.workers_manager.compiled_workers = MagicMock()
        self.runner.workers_manager.populate_workers = MagicMock(return_value=[worker1, worker2])
        worker1.get = CoroutineMock(return_value=Result(data={'url1': List(value=[String('result for url1')])}, completed=True))
        worker2.get = CoroutineMock(return_value=Result(data={'url2': List(value=[String('result for url2')])}, completed=True))

        result = self.loop.run_until_complete(self.runner.run(urls))
        self.runner.workers_manager.create_workers_map.assert_called_once_with(
            request=self.runner.request,
            executor=isinstance_checker(DummyPoolExecutor),
            session=isinstance_checker(aiohttp.ClientSession),
        )
        self.runner.url_parser.parse_urls.assert_called_with(urls, self.runner.workers_manager.compiled_workers)
        self.runner.workers_manager.populate_workers.assert_called_once_with(workers_map,
                                                                             url_objects,
                                                                             )

        self.assertEqual(result, Result(data={
            'url1': List(value=[String('result for url1')], completed=True),
            'url2': List(value=[String('result for url2')], completed=True),
        }, completed=True))
