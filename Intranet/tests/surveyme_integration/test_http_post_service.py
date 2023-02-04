# -*- coding: utf-8 -*-
import responses
import os

from django.test import TestCase
from requests import RequestException
from unittest.mock import patch

from events.common_app.testutils import get_content_type_and_boundary, parse_multipart
from events.common_storages.storage import MdsStorage, ReadError
from events.surveyme_integration.services.http.action_processors import HTTPBaseActionProcessor
from events.surveyme_integration.helpers import HTTPServiceBaseTestCaseMixin
from events.arc_compat import read_asset

STATUSES = {
    400, 401, 402, 403, 405, 406, 407,
    408, 409, 410, 411, 412, 413, 414, 415,
    416, 417, 418, 420, 422, 423, 424, 425,
    426, 428, 429, 431, 444, 449, 450, 451,
    494, 495, 496, 497, 499, 500, 501, 502,
    503, 504, 505, 506, 507, 508, 509, 510,
    511, 598, 599,
}


class HTTPServiceTestMixin__post(HTTPServiceBaseTestCaseMixin, TestCase):

    @responses.activate
    def test_should_send_request_to_url_with_multipart_even_if_no_files_sent(self):
        self.register_uri(self.context_data['url'])

        self.do_service_action('post', context=self.context_data)  # BANG!

        self.assertEqual(len(responses.calls), 1)
        content_type, boundary = get_content_type_and_boundary(responses.calls[0].request)
        self.assertEqual(content_type, 'multipart/form-data')

    @responses.activate
    def test_should_send_not_multipart_if_no_files_provided_and_multipart_is_not_forced(self):
        self.register_uri(self.context_data['url'])

        with patch('events.surveyme_integration.services.http.action_processors.PostHTTPActionProcessor.force_multipart', False):
            self.do_service_action('post', context=self.context_data)  # BANG!

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers.get('content-type'), 'application/x-www-form-urlencoded')
        self.assertEqual(responses.calls[0].request.body, 'hello=world')

    @responses.activate
    def test_should_send_headers(self):
        self.context_data['headers'] = {
            'X-HELLO': 'world',
            'X-AMIGO': 'восемь',
            'икс-амиго': 'восемь',
            'X-утф': 'восемь',
            'X-Test-It': ' Key="mytest" ',
        }

        self.register_uri(self.context_data['url'])

        self.do_service_action('post', context=self.context_data)  # BANG!

        self.assertEqual(len(responses.calls), 1)
        expected = {
            'X-HELLO': 'world',
            'X-AMIGO': r'\u0432\u043e\u0441\u0435\u043c\u044c',
            r'\u0438\u043a\u0441-\u0430\u043c\u0438\u0433\u043e': r'\u0432\u043e\u0441\u0435\u043c\u044c',
            r'X-\u0443\u0442\u0444': r'\u0432\u043e\u0441\u0435\u043c\u044c',
            'X-Test-It': 'Key="mytest"',
        }
        result = {
            header: responses.calls[0].request.headers[header]
            for header in expected
            if header in responses.calls[0].request.headers
        }
        self.assertEqual(result, expected)

    @responses.activate
    def test_should_send_data(self):
        self.register_uri(self.context_data['url'])

        self.do_service_action('post', context=self.context_data)  # BANG!

        self.assertEqual(len(responses.calls), 1)
        _, boundary = get_content_type_and_boundary(responses.calls[0].request)
        boundary = boundary.encode()
        parsed_multipart = parse_multipart(boundary, responses.calls[0].request.body)

        self.assertEqual(str(parsed_multipart['hello']['data']), self.context_data['body_data']['hello'])

    @responses.activate
    def test_response(self):
        self.register_uri(self.context_data['url'])

        response = self.do_service_action('post', context=self.context_data)  # BANG!

        self.assertEqual(response['response'].get('headers').get('x-mark'), 'mark')
        self.assertEqual(response['response'].get('content'), 'hello')
        self.assertEqual(response['response'].get('status_code'), 200)

    @responses.activate
    def test_should_follow_redirects(self):
        redirect_to = 'http://redirect.to/'
        redirect_status = 307
        content = 'from redirected endpoint with status %s' % redirect_status
        self.register_uri(redirect_to, content=content)
        self.register_uri(self.context_data['url'], status_code=redirect_status, headers={'location': redirect_to})

        response = self.do_service_action('post', context=self.context_data)  # BANG!
        self.assertEqual(response.get('status'), self.expected_status_after_first_submission)
        self.assertEqual(response.get('response').get('content'), content)

    @responses.activate
    def test_should_raise_not_retriable_exception_for_4xx_error_except_404(self):
        should_raise_requests_error_for_statuses = [404]
        status_codes = [i for i in STATUSES if 399 < i < 500 and i != 409]
        msg = 'Should raise Exception for every 4xx status in response, except {0}'.format(
            ', '.join([str(i) for i in should_raise_requests_error_for_statuses])
        )
        for status_code in status_codes:
            self.register_uri(self.context_data['url'], status_code=status_code)
            try:
                self.do_service_action('post', context=self.context_data)  # BANG!
            except Exception as e:
                if status_code in should_raise_requests_error_for_statuses and not issubclass(type(e), RequestException):
                    self.fail(msg)
                else:
                    pass
            else:
                self.fail(msg)

    @responses.activate
    def test_should_raise_retriable_exception_for_every_5xx(self):
        status_codes = [i for i in STATUSES if i > 499]
        msg = 'Should raise retriable exception for every 5xx status in response'
        for status_code in status_codes:
            self.register_uri(self.context_data['url'], status_code=status_code)
            try:
                self.do_service_action('post', context=self.context_data)  # BANG!
            except Exception:
                pass
            else:
                self.fail(msg)

    @responses.activate
    def test_response_status__if_uri_response_is_200(self):
        self.register_uri(self.context_data['url'])

        response = self.do_service_action('post', context=self.context_data)  # BANG!

        self.assertEqual(response.get('status'), self.expected_status_after_first_submission)

    def _file_return(self, path):
        name = path.split('_')[1]
        return read_asset(os.path.join(self.get_files_path(), name))

    @responses.activate
    def test_should_send_attachments_as_files(self):
        self.context_data['attachments'] = []
        files_for_attachments = [self.files['avatar'], self.files['resume']]

        self.register_uri(self.context_data['url'])

        # add attachments to context
        for file_item in files_for_attachments:
            self.context_data['attachments'].append({
                'path': file_item['path'],
                'filename': file_item['filename'],
                'namespace': 'forms',
                'content_type': file_item['content_type'],
                'headers': {
                    'x-header-filename': file_item['filename'],
                    'x-header-content-type': file_item['content_type'],
                }
            })
        with patch.object(MdsStorage, '_fetch', self._file_return):
            self.do_service_action('post', context=self.context_data)  # BANG!

        _, boundary = get_content_type_and_boundary(responses.calls[0].request)
        boundary = boundary.encode()
        parsed_multipart = parse_multipart(boundary, responses.calls[0].request.body)

        # test sent filename
        self.assertEqual(
            str(parsed_multipart['field_2']['filename']),
            self.context_data['attachments'][0]['filename']
        )
        self.assertEqual(
            str(parsed_multipart['field_3']['filename']),
            self.context_data['attachments'][1]['filename']
        )

        # test sent headers
        self.assertEqual(
            parsed_multipart['field_2']['headers'],
            {
                'x-header-filename': files_for_attachments[0]['filename'],
                'x-header-content-type': files_for_attachments[0]['content_type'],
                'Content-Type': files_for_attachments[0]['content_type'],
                'Content-Disposition': 'form-data',
            }
        )
        self.assertEqual(
            parsed_multipart['field_3']['headers'],
            {
                'x-header-filename': files_for_attachments[1]['filename'],
                'x-header-content-type': files_for_attachments[1]['content_type'],
                'Content-Type': files_for_attachments[1]['content_type'],
                'Content-Disposition': 'form-data',
            }
        )

        # test sent content
        self.assertEqual(
            parsed_multipart['field_2']['data'],
            files_for_attachments[0]['object'].read()
        )
        self.assertEqual(
            parsed_multipart['field_3']['data'].encode(),
            files_for_attachments[1]['object'].read()
        )

    @responses.activate
    def test_should_handle_readerror_from_mds(self):
        self.context_data['attachments'] = []
        files_for_attachments = [self.files['avatar'], self.files['resume']]

        self.register_uri(self.context_data['url'])

        # add attachments to context
        for file_item in files_for_attachments:
            self.context_data['attachments'].append({
                'path': file_item['path'],
                'filename': file_item['filename'],
                'namespace': 'forms',
            })
        with patch.object(MdsStorage, '_fetch') as mock_fetch:
            mock_fetch.side_effect = ReadError
            self.do_service_action('post', context=self.context_data)  # BANG!

        _, boundary = get_content_type_and_boundary(responses.calls[0].request)
        boundary = boundary.encode()
        parsed_multipart = parse_multipart(boundary, responses.calls[0].request.body)

        self.assertTrue('field_2' not in parsed_multipart)
        self.assertTrue('field_3' not in parsed_multipart)


class HTTPServiceTest202__post(HTTPServiceBaseTestCaseMixin, TestCase):

    @responses.activate
    def test_response_status__if_uri_response_is_202__without_content_location_header(self):
        self.register_uri(self.context_data['url'], status_code=202)

        response = self.do_service_action('post', context=self.context_data)  # BANG!

        self.assertEqual(response.get('status'), 'success')

    @responses.activate
    def test_response_status__if_uri_response_is_202__with_content_location_header(self):
        self.register_uri(self.context_data['url'], status_code=202, headers={'Content-Location': 'http://poll.me'})

        response = self.do_service_action('post', context=self.context_data)  # BANG!

        self.assertEqual(response.get('status'), 'success')
        self.assertIsNone(response.get('update_context'))

    @responses.activate
    def test_should_return_next_processing_countdown(self):
        self.register_uri(self.context_data['url'], status_code=202, headers={'Retry-After': '10'})

        response = self.do_service_action('post', context=self.context_data)  # BANG!

        self.assertIsNone(response.get('next_processing_countdown'))

    @responses.activate
    def test_should_return_next_processing_countdown__as_none_if_no_header(self):
        self.register_uri(self.context_data['url'], status_code=202)

        response = self.do_service_action('post', context=self.context_data)  # BANG!

        self.assertIsNone(response.get('next_processing_countdown'))


class TestHTTPBaseActionProcessor(TestCase):
    def setUp(self):
        self.action_processor = HTTPBaseActionProcessor({}, None)

    def test_is_good_for_oauth(self):
        self.assertTrue(self.action_processor.is_good_for_oauth('https://forms.yandex-team.ru/#/'))
        self.assertTrue(self.action_processor.is_good_for_oauth('https://forms.yandex.net/#/'))
        self.assertFalse(self.action_processor.is_good_for_oauth('https://forms.yandex.ru/#/'))


class TestHTTPResponseProcessing(HTTPServiceBaseTestCaseMixin, TestCase):
    @responses.activate
    def test_should_return_success_for_409(self):
        responses.add(
            responses.POST,
            'http://yandex.ru/test_url/',
            body='''
                <html>
                    <head><title>409 Conflict</title></head>
                    <body bgcolor="white">
                        <center><h1>409 Conflict</h1></center>
                        <hr>
                        <center>nginx</center>
                    </body>
                </html>
            ''',
            content_type='text/xml',
            status=409,
        )
        response = self.do_service_action('post', context=self.context_data)  # BANG!
        self.assertEqual(response['status'], 'success')
