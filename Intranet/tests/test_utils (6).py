import types
from unittest import TestCase, mock
from unittest.mock import MagicMock

from intranet.magiclinks.src.links.dto import UrlObject
from intranet.magiclinks.src.links.utils.match_options import compile_match_options
from intranet.magiclinks.src.links.utils.urls import UrlsParser
from intranet.magiclinks.src.links.utils.workers import (
    WorkersManager,
    import_worker_class,
    UnknownWorkerClass,
)
from intranet.magiclinks.src.links.dto import MatchOptions


class UtilsTestCase(TestCase):
    def test_compile_worker(self):

        worker = MagicMock()
        worker.HOSTNAME_REGEX = {'development': r'this is hostname regex'}
        worker.PATH_REGEX = 'this is path regex'
        worker.FRAGMENT_REGEX = 'this is fragment regex'
        worker.QUERY_REGEX = 'this is query regex'
        match_options = compile_match_options(worker)
        self.assertEqual(match_options.hostname_regex.pattern, r'this is hostname regex')
        self.assertEqual(match_options.path_regex.pattern, r'this is path regex')
        self.assertEqual(match_options.fragment_regex.pattern, r'this is fragment regex')
        self.assertEqual(match_options.query_regex.pattern, r'this is query regex')

    @mock.patch('intranet.magiclinks.src.links.utils.workers.import_module', autospec=True)
    def test_import_worker_class_with_mocked_import_module(self, import_module):
        module = types.ModuleType('disk')
        import_module.return_value = module
        expected_worker_class = MagicMock()
        module.Worker = expected_worker_class
        worker = import_worker_class('disk')
        import_module.assert_called_once_with('intranet.magiclinks.src.links.workers.intranet.disk')
        self.assertEqual(worker, expected_worker_class)

    def test_import_worker_class_with_real_import_module(self):
        import sys
        module = types.ModuleType('test_file')
        sys.modules['intranet.magiclinks.src.links.workers.default.test_file'] = module
        expected_worker_class = MagicMock()
        module.Worker = expected_worker_class
        worker = import_worker_class('test_file')
        self.assertEqual(worker, expected_worker_class)
        del sys.modules['intranet.magiclinks.src.links.workers.default.test_file']


class UrlParserTestCase(TestCase):
    def setUp(self):
        super().setUp()
        self.url_parser = UrlsParser()
        self.compiled_workers = {'test_worker': MatchOptions(r'^example.com$',
                                                             r'^/path/(?P<id>[a-z]+)/?$',
                                                             r'',
                                                             r'',
                                                             )
                                 }

    def test_parse_url_matches(self):
        url_object = self.url_parser.parse_url('http://example.com/path/someid/',
                                               self.compiled_workers)
        self.assertEqual(url_object.url, 'http://example.com/path/someid/')
        self.assertEqual(url_object.worker_class_file, 'test_worker')
        self.assertEqual(url_object.path_match.group('id'), 'someid')

    def test_parse_url_does_not_match(self):
        url_object = self.url_parser.parse_url('http://example.com/path/some-wrong-id/',
                                               self.compiled_workers)
        self.assertIsNone(url_object)

    def test_parse_urls(self):
        urls = ['http://example.com/path/someid/', 'http://example.com/path/some-wrong-id/']

        url_objects = list(self.url_parser.parse_urls(urls, self.compiled_workers))
        self.assertEqual(len(url_objects), 1)
        self.assertEqual(url_objects[0].url, 'http://example.com/path/someid/')
        self.assertEqual(url_objects[0].worker_class_file, 'test_worker')
        self.assertEqual(url_objects[0].path_match.group('id'), 'someid')


class WorkersManagerTestCase(TestCase):
    def setUp(self):
        workers = ['one_worker', 'another_worker']
        self.workers_manager = WorkersManager(workers)
        self.url_parser = UrlsParser()
        self.executor = MagicMock()
        self.session = MagicMock()

    @mock.patch('intranet.magiclinks.src.links.utils.workers.import_worker_class', autospec=True)
    def test_get_worker(self, import_worker):
        expected_worker_class = MagicMock()
        import_worker.return_value = expected_worker_class
        self.workers_manager.workers = ['one_worker']
        workers = self.workers_manager.workers_classes

        import_worker.assert_called_once_with('one_worker')
        self.assertEqual(self.workers_manager.workers_classes['one_worker'],
                         expected_worker_class)
        self.assertEqual(workers, {'one_worker': expected_worker_class, })

    def test_get_unknown_worker(self):
        self.workers_manager.workers = ['wrong_worker']
        with self.assertRaisesRegex(UnknownWorkerClass,
                                    r'^Unknown worker class: wrong_worker$'):
            self.workers_manager.workers_classes

    def test_create_workers(self):
        worker_class1, worker_class2 = MagicMock(), MagicMock()
        worker1, worker2 = MagicMock(), MagicMock()
        worker_class1.return_value, worker_class2.return_value = worker1, worker2
        worker1.url_objects, worker2.url_objects = set(), set()

        worker1.add_url_object.side_effect = worker1.url_objects.add
        worker2.add_url_object.side_effect = worker2.url_objects.add

        self.workers_manager.workers_classes = {
            'one_worker': worker_class1,
            'another_worker': worker_class2,
        }
        url_object1 = UrlObject(url=None, split_result=None, hostname_match=None, path_match=None,
                                worker_class_file='one_worker', fragment_match=None, query_match=None,
                                )
        url_object2 = UrlObject(url=None, split_result=None, hostname_match=None, path_match=None,
                                worker_class_file='another_worker', fragment_match=None, query_match=None,
                                )
        request = MagicMock()
        workers_map = self.workers_manager.create_workers_map(request=request,
                                                              executor=self.executor,
                                                              session=self.session,
                                                              )

        workers = self.workers_manager.populate_workers(workers_map, [url_object1, url_object2])

        worker_class1.assert_called_once_with(
            request=request,
            executor=self.executor,
            session=self.session,
        )
        worker_class2.assert_called_once_with(
            request=request,
            executor=self.executor,
            session=self.session,
        )
        self.assertIn(worker1, workers)
        self.assertIn(worker2, workers)
        self.assertIn(url_object1, worker1.url_objects)
        self.assertIn(url_object2, worker2.url_objects)

    def test_create_workers_skips_empty_workers(self):
        worker_class1, worker_class2 = MagicMock(), MagicMock()
        worker1, worker2 = MagicMock(), MagicMock()
        worker_class1.return_value, worker_class2.return_value = worker1, worker2
        worker1.url_objects, worker2.url_objects = set(), set()

        worker1.add_url_object.side_effect = worker1.url_objects.add
        worker2.add_url_object.side_effect = worker2.url_objects.add

        self.workers_manager.workers_classes = {
            'one_worker': worker_class1,
            'another_worker': worker_class2,
        }
        url_object = UrlObject(url=None, split_result=None, hostname_match=None, path_match=None,
                               worker_class_file='one_worker', fragment_match=None, query_match=None, )
        request = MagicMock()
        workers_map = self.workers_manager.create_workers_map(request=request,
                                                              executor=self.executor,
                                                              session=self.session,
                                                              )

        workers = self.workers_manager.populate_workers(workers_map, [url_object, ])

        worker_class1.assert_called_once_with(
            request=request,
            executor=self.executor,
            session=self.session,
        )
        worker_class2.assert_called_once_with(
            request=request,
            executor=self.executor,
            session=self.session,
        )
        self.assertIn(worker1, workers)
        self.assertNotIn(worker2, workers)
        self.assertIn(url_object, worker1.url_objects)
