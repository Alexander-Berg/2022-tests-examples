import os
import yatest
import asyncio
import aiohttp
import pytest
import vcr
from concurrent.futures import ThreadPoolExecutor

from asynctest import CoroutineMock
from unittest import TestCase, mock
from unittest.mock import Mock, patch
from django.test import RequestFactory
from django.conf import settings

from intranet.magiclinks.src.links.utils.urls import UrlsParser
from intranet.magiclinks.src.links.utils.workers import WorkersManager
from intranet.magiclinks.src.links.dto import List, String, Image


def source_path(path_: str) -> str:
    try:
        return yatest.common.source_path(path_)
    except NotImplementedError:  # only for local pycharm tests
        root = os.environ['Y_PYTHON_SOURCE_ROOT']
        return os.path.join(root, path_)


path = source_path('intranet/magiclinks/vcr_cassettes')
test_vcr = vcr.VCR(cassette_library_dir=path)


class BaseWorkerTestCase(TestCase):
    loop = asyncio.get_event_loop()
    worker_class = None
    worker_class_file = None

    def setUp(self):
        self.factory = RequestFactory()
        self.session = aiohttp.ClientSession(
            connector=aiohttp.TCPConnector(
                verify_ssl=settings.SSL_VERIFY,
            ),
        )
        with patch.object(WorkersManager, 'workers_classes',
                          new_callable=mock.PropertyMock) as workers_classes:
            workers_classes.return_value = {self.worker_class_file: self.worker_class}

            self.workers_manager = WorkersManager((self.worker_class_file, ))
            self.executor = ThreadPoolExecutor()
            self.workers = self.workers_manager.create_workers_map(self.get_request(),
                                                                   executor=self.executor,
                                                                   session=self.session,
                                                                   )
            self.compiled_workers = self.workers_manager.compiled_workers
        self.url_parser = UrlsParser()

    def tearDown(self) -> None:
        task = self.loop.create_task(self.session.close())
        self.loop.run_until_complete(task)

    def parse_url(self, url, hostname_match='', path_data=None, fragment_data=None,
                  query_data=None, should_parse=True,
                  ):
        fragment_data = fragment_data if fragment_data else {}
        query_data = query_data if query_data else {}
        path_data = path_data if path_data else {}
        parsed_url = self.url_parser.parse_url(url, self.compiled_workers)
        if not should_parse:
            self.assertIsNone(parsed_url)
            return
        self.assertEqual(parsed_url.url, url)
        self.assertEqual(parsed_url.worker_class_file, self.worker_class_file)
        self.assertEqual(parsed_url.hostname_match.group(), hostname_match)

        for group_name, path_match in path_data.items():
            self.assertEqual(parsed_url.path_match.group(group_name), path_match)

        for fragment_name, fragment_match in fragment_data.items():
            self.assertEqual(parsed_url.fragment_match.group(fragment_name), fragment_match)

        for query_name, query_match in query_data.items():
            self.assertEqual(parsed_url.query_match.group(query_name), query_match)

    @pytest.mark.asyncio
    async def response_check(self, *urls, expected_data, cassette_name=None):
        worker = self.get_worker()
        token_value = 'test'
        worker.get_token = CoroutineMock(return_value=token_value)
        for url in urls:
            parsed_url = self.url_parser.parse_url(url, self.compiled_workers)
            worker.add_url_object(parsed_url)

        if cassette_name:
            folder = cassette_name.split('_')[0]
            path_to_cassette = os.path.join(folder, cassette_name)
            with test_vcr.use_cassette(path_to_cassette):
                result = await worker.get()
        else:
            result = await worker.get()
        self.assertEqual(expected_data, result.data)

    @pytest.mark.asyncio
    async def fail_response_check(self, url, cassette_name=None, completed=False):
        expected_data = {
            url: List(
                ttl=60,
                value=[
                    Image(
                        src=self.worker_class.FAVICON,
                        text=self.worker_class.FAVICON_TEXT,
                    ),
                    String(value=url),
                ],
                completed=completed,
            )}
        await self.response_check(url,
                                  expected_data=expected_data,
                                  cassette_name=cassette_name,
                                  )

    def get_request(self):
        request = self.factory.post('/magiclinks/v1/links/')
        request.META['HTTP_AUTHORIZATION'] = 'OAuth some-token'
        yauser = Mock()
        yauser.uid = '123'
        yauser.is_external = None
        yauser.login = 'smosker'
        request.yauser = yauser
        request.user_ip = '192.168.1.1'
        return request

    def get_worker(self):
        return self.worker_class(request=self.get_request(),
                                 executor=self.executor,
                                 session=self.session,
                                 )
