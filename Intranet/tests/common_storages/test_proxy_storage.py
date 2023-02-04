# -*- coding: utf-8 -*-
import hashlib

from unittest.mock import patch

from django.conf import settings
from django.test import TestCase
from django.test.utils import override_settings
from django.core.files.base import ContentFile
from django.utils.timezone import now

from events.common_storages.models import ProxyStorageModel
from events.common_storages.proxy_storages import ProxyStorage
from events.common_storages.storage import MdsClient


class TestProxyStorage(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        self.proxy_storage = ProxyStorage()

    def test_url(self):
        response = self.proxy_storage.url('/hello/world.txt')
        expected = 'https://{domain}/files?path=%2Fhello%2Fworld.txt'.format(
            domain=settings.DEFAULT_FRONTEND_DOMAIN
        )
        self.assertEqual(response, expected)

    @override_settings(FILE_PATH_PREFIX='/u')
    def test_url_file_path_prefix(self):
        response = self.proxy_storage.url('/hello/world.txt')
        expected = 'https://{domain}/u/files?path=%2Fhello%2Fworld.txt'.format(
            domain=settings.DEFAULT_FRONTEND_DOMAIN
        )
        self.assertEqual(response, expected)

    def test_should_add_date_created_to_meta_backend_info(self):
        original_name = 'hello.txt'
        with patch('events.common_storages.storage.generate_code') as mock_generate:
            mock_generate.return_value = 'b9j2a642b859b370557ab890943c999y'
            with patch.object(MdsClient, 'upload') as mock_save:
                mock_save.return_value = '401/{}_{}'.format('b9j2a642b859b370557ab890943c999y', 'hello')
                path = self.proxy_storage.save(content=ContentFile('hello', name=original_name), name=original_name)
                meta_backend_obj = self._get_meta_backend_obj(path)
        now_date = now()
        self.assertEqual(meta_backend_obj.date_created.minute, now_date.minute)
        self.assertEqual(meta_backend_obj.date_created.hour, now_date.hour)
        self.assertEqual(meta_backend_obj.date_created.day, now_date.day)
        self.assertEqual(meta_backend_obj.date_created.month, now_date.month)
        self.assertEqual(meta_backend_obj.date_created.year, now_date.year)

    def test_should_add_original_name_to_meta_backend_info(self):
        original_name = 'hello.txt'
        with patch('events.common_storages.storage.generate_code') as mock_generate:
            mock_generate.return_value = 'b9j2a642b859b370557rf890943c999z'
            with patch.object(MdsClient, 'upload') as mock_save:
                mock_save.return_value = '401/{}_{}'.format('b9j2a642b859b370557rf890943c999z', 'hello')
                path = self.proxy_storage.save(content=ContentFile('hello', name=original_name), name=original_name)
                meta_backend_obj = self._get_meta_backend_obj(path)
        self.assertEqual(meta_backend_obj.original_name, original_name)

    def test_should_add_sha256_to_meta_backend_info(self):
        original_name = 'hello.txt'
        file_content = ContentFile('hello')
        file_content.name = original_name
        with patch('events.common_storages.storage.generate_code') as mock_generate:
            mock_generate.return_value = 'b9j2a642b859b370557uj890943c999k'
            with patch.object(MdsClient, 'upload') as mock_save:
                mock_save.return_value = '401/{}_{}'.format('b9j2a642b859b370557uj890943c999k', 'hello')
                path = self.proxy_storage.save(content=file_content, name=original_name)
                meta_backend_obj = self._get_meta_backend_obj(path)
        m = hashlib.sha256()
        m.update(file_content.read().encode('utf-8'))
        m.update(original_name.encode('utf-8'))
        self.assertEqual(meta_backend_obj.sha256, m.hexdigest())

    def _get_meta_backend_obj(self, path):
        return ProxyStorageModel.objects.get(path=path)
