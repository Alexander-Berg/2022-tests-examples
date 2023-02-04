# -*- coding: utf-8 -*-
import responses

from django.test import TestCase
from django.utils.translation import ugettext as _

from events.common_app.disk.client import DiskClient, DiskUploadError


class TestDiskClient(TestCase):
    def setUp(self):
        self.user_uid = '25871573'
        self.client = DiskClient(self.user_uid)

    def register_uri(self, path, body=None, method=None, status=None):
        url = f'https://intapi.disk.yandex.net:8443/v1/{path}'
        body = body or {}
        method = method or responses.GET
        status = status or 200
        responses.add(method, url, json=body, status=status)

    @responses.activate
    def test_get_resource(self):
        self.register_uri('disk/resources', body={
            'name': 'disk',
            'type': 'dir',
        })
        response = self.client.get_resource('/')

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.headers['X-Uid'], self.user_uid)

    @responses.activate
    def test_isdir(self):
        self.register_uri('disk/resources', body={
            'name': 'Tests',
            'type': 'dir',
        })
        self.assertTrue(self.client.isdir('/Tests'))
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_not_isdir(self):
        self.register_uri('disk/resources', body={
            'name': 'joel.jpg',
            'type': 'file',
        })
        self.assertFalse(self.client.isdir('/Tests/joel.jpg'))
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_isfile(self):
        self.register_uri('disk/resources', body={
            'name': 'joel.jpg',
            'type': 'file',
        })
        self.assertTrue(self.client.isfile('/Tests/joel.jpg'))
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_not_isfile(self):
        self.register_uri('disk/resources', body={
            'name': 'Tests',
            'type': 'dir',
        })
        self.assertFalse(self.client.isfile('/Tests'))
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_shouldnt_make_root_dir(self):
        self.register_uri('disk/resources')
        self.register_uri('disk/resources', method=responses.PUT)

        self.client.mkdir('/')

        self.assertEqual(len(responses.calls), 0)

    @responses.activate
    def test_shouldnt_make_existing_dir(self):
        self.register_uri('disk/resources', body={
            'name': 'Tests',
            'type': 'dir',
        })
        self.register_uri('disk/resources', method=responses.PUT)

        self.client.mkdir('/Tests')

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(responses.calls[0].request.method, responses.GET)

    @responses.activate
    def test_should_make_not_existing_dir(self):
        self.register_uri('disk/resources', status=404)
        self.register_uri('disk/resources', body={
            'name': 'Tests',
            'type': 'dir',
        })
        self.register_uri('disk/resources', method=responses.PUT)

        self.client.mkdir('/Tests/NotExist')

        self.assertEqual(len(responses.calls), 3)
        self.assertEqual(responses.calls[0].request.method, responses.GET)
        self.assertEqual(responses.calls[1].request.method, responses.GET)
        self.assertEqual(responses.calls[2].request.method, responses.PUT)

    @responses.activate
    def test_should_fetch_listdir(self):
        self.register_uri('disk/resources', body={
            'name': 'Tests',
            'type': 'dir',
            '_embedded': {
                'items': [
                    {
                        'name': 'joel.jpg',
                        'type': 'file',
                    },
                    {
                        'name': 'ellie.jpg',
                        'type': 'file',
                    },
                ],
                'total': 2,
            },
        })
        files = list(self.client.listdir('/Tests'))

        self.assertEqual(len(responses.calls), 1)
        self.assertTrue(len(files) >= 2)
        self.assertIn('joel.jpg', files)
        self.assertIn('ellie.jpg', files)
        self.assertFalse('not a file name' in files)

    @responses.activate
    def test_should_return_edit_url(self):
        edit_url = 'file:///Tests/sheet.xlsx'
        self.register_uri('disk/resources/online-editor', body={
            'edit_url': edit_url,
        })

        result = self.client.get_edit_url('/Tests/sheet.xlsx')

        self.assertEqual(result, edit_url)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_shouldnt_return_edit_url(self):
        self.register_uri('disk/resources/online-editor', status=415)

        result = self.client.get_edit_url('/Tests/joel.jpg')

        self.assertIsNone(result)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_should_return_download_url(self):
        download_url = 'file:///Tests/joel.jpg'
        self.register_uri('disk/resources/download', body={
            'href': download_url,
        })

        result = self.client.get_download_url('/Tests/joel.jpg')

        self.assertEqual(result, download_url)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_shouldnt_return_download_url(self):
        self.register_uri('disk/resources/download', status=404)

        result = self.client.get_download_url('/Tests/notexist.txt')

        self.assertIsNone(result)
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_should_change_metadata(self):
        custom_properties = {
            'foo': 'bar',
            'service': 'forms',
        }
        self.register_uri('disk/resources', method=responses.PATCH)
        self.register_uri('disk/resources', body={
            'custom_properties': custom_properties,
        })
        self.assertTrue(self.client.set_metadata('/Tests/joel.jpg', custom_properties))
        self.assertEqual(len(responses.calls), 1)

        result = self.client.get_metadata('/Tests/joel.jpg')
        self.assertEqual(len(responses.calls), 2)
        self.assertDictEqual(result, custom_properties)

    @responses.activate
    def test_should_upload_file(self):
        upload_method = 'PUT'
        upload_url = 'https://disk-api.yandex.net/upload/Tests/textfile.txt'
        self.register_uri('disk/resources/upload', body={
            'method': upload_method,
            'href': upload_url,
        })
        responses.add(upload_method, upload_url)
        content = 'this is a test'

        self.client.upload('/Tests/textfile.txt', content)

        self.assertEqual(len(responses.calls), 2)
        self.assertDictEqual(responses.calls[0].request.params, {
            'path': '/Tests/textfile.txt',
            'overwrite': 'True',
        })
        self.assertEqual(responses.calls[1].request.body, content)

    @responses.activate
    def test_should_throw_upload_error_1(self):
        self.register_uri('disk/resources/upload', status=503, body={
            'message': 'Internal error',
        })
        content = 'this is a test'
        with self.assertRaises(DiskUploadError) as e:
            self.client.upload('/Tests/textfile.txt', content)

        self.assertEqual(e.exception.message, 'Internal error')
        self.assertEqual(len(responses.calls), 1)

    @responses.activate
    def test_should_throw_upload_error_2(self):
        upload_method = 'PUT'
        upload_url = 'https://disk-api.yandex.net/upload/Tests/textfile.txt'
        self.register_uri('disk/resources/upload', body={
            'method': upload_method,
            'href': upload_url,
        })
        responses.add(upload_method, upload_url, status=507, json={
            'message': 'Not enough space',
        })
        content = 'this is a test'
        with self.assertRaises(DiskUploadError) as e:
            self.client.upload('/Tests/textfile.txt', content)

        self.assertEqual(e.exception.message, _('Недостаточно свободного места'))
        self.assertEqual(len(responses.calls), 2)

    @responses.activate
    def test_should_check_file_name(self):
        self.register_uri('disk/resources', status=404)

        file_name, exist = self.client.check_file_name('/Tests/notexist.xlsx')

        self.assertEqual(len(responses.calls), 1)
        self.assertEqual(file_name, '/Tests/notexist.xlsx')
        self.assertFalse(exist)

    @responses.activate
    def test_should_check_file_and_return_new_name(self):
        self.register_uri('disk/resources', body={
            'name': 'sheet.xlsx',
            'type': 'file',
        })
        self.register_uri('disk/resources', body={
            'name': 'Tests',
            'type': 'dir',
            '_embedded': {
                'items': [
                    {
                        'name': 'sheet.xlsx',
                        'type': 'file',
                    },
                    {
                        'name': 'sheet 1.xlsx',
                        'type': 'file',
                    },
                ],
                'total': 2,
            },
        })

        file_name, exist = self.client.check_file_name('/Tests/sheet.xlsx')

        self.assertEqual(len(responses.calls), 2)
        self.assertEqual(file_name, '/Tests/sheet 2.xlsx')
        self.assertTrue(exist)
