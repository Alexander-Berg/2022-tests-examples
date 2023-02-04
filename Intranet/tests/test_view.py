import json
from unittest import TestCase, mock
from asynctest import patch

from django.test import RequestFactory
from django.test.utils import override_settings

from intranet.magiclinks.src.links.dto import Result
from intranet.magiclinks.src.links.dto import String
from intranet.magiclinks.src.links.runner import Runner
from intranet.magiclinks.src.links.views import LinksView
from . import isinstance_checker


class LinksViewTestCase(TestCase):
    @mock.patch('intranet.magiclinks.src.links.views.urls_parser', autospec=True)
    @mock.patch('intranet.magiclinks.src.links.views.workers_manager', autospec=True)
    @patch('intranet.magiclinks.src.links.views.Runner.run')
    @mock.patch('intranet.magiclinks.src.links.views.Runner.__init__', autospec=True)
    def test_links_view(self, init, run, workers_manager, links_parser):
        request = RequestFactory().post('/magiclinks/v1/links/', content_type='application/json',
                                        data=json.dumps({'links': ['url1', 'url2']}))
        init.return_value = None
        result = Result(data={'url1': String(value='result for url1')})
        run.return_value = result
        response = LinksView.as_view()(request)
        self.assertEqual(response.status_code, 200)
        init.assert_called_once_with(isinstance_checker(Runner), request, workers_manager,
                                     links_parser)
        run.assert_called_once_with(['url1', 'url2'])
        response_decoded = json.loads(response.content.decode())
        self.assertEqual(response_decoded['data'],
                         {'url1': {'type': 'string', 'value': 'result for url1'}})
        self.assertFalse(response_decoded['drop_client_cache'])

    @override_settings(DROP_CACHE=True)
    @mock.patch('intranet.magiclinks.src.links.views.urls_parser', autospec=True)
    @mock.patch('intranet.magiclinks.src.links.views.workers_manager', autospec=True)
    @patch('intranet.magiclinks.src.links.views.Runner.run')
    @mock.patch('intranet.magiclinks.src.links.views.Runner.__init__', autospec=True)
    def test_drop_cache_links_view(self, init, run, workers_manager, links_parser):
        request = RequestFactory().post('/magiclinks/v1/links/', content_type='application/json',
                                        data=json.dumps({'links': ['url1', 'url2']}))
        init.return_value = None
        result = Result(data={'url1': String(value='result for url1')})
        run.return_value = result
        response = LinksView.as_view()(request)
        self.assertEqual(response.status_code, 200)
        response_decoded = json.loads(response.content.decode())
        self.assertEqual(response_decoded['data'],
                         {'url1': {'type': 'string', 'value': 'result for url1'}})
        self.assertTrue(response_decoded['drop_client_cache'])

    @override_settings(DROP_CACHE=True)
    @mock.patch('intranet.magiclinks.src.links.views.urls_parser', autospec=True)
    @mock.patch('intranet.magiclinks.src.links.views.workers_manager', autospec=True)
    @patch('intranet.magiclinks.src.links.views.Runner.run')
    @mock.patch('intranet.magiclinks.src.links.views.Runner.__init__', autospec=True)
    def test_links_view_not_completed(self, init, run, workers_manager, links_parser):
        request = RequestFactory().post('/magiclinks/v1/links/', content_type='application/json',
                                        data=json.dumps({'links': ['url1', 'url2']}))
        init.return_value = None
        result = Result(data={'url1': String(value='result for url1')}, completed=False)
        run.return_value = result
        response = LinksView.as_view()(request)
        self.assertEqual(response.status_code, 200)

