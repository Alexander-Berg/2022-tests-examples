# -*- coding: utf-8 -*-

import copy
import json

import mock
import pytest
from django.test import Client

from smarttv.alice.tv_proxy.proxy.indexer import Cleaner
from smarttv.alice.tv_proxy.proxy.search_proxy import FoundDocument, build_found_document
from smarttv.alice.tv_proxy.tests import mocks

http_client = Client()

valid_channels_data = {
    'channels': [
        {
            'uri': 'live-tv://android.media.tv/channel/12345',
            'number': 1,
            'name': 'Тестовый канал'
        },
        {
            'uri': 'live-tv://android.media.tv/channel/123456',
            'number': 2,
            'name': 'One more test channel'
        }
    ],
    'timestamp': 1594908176
}

saas_stored_documents = [
    {'url': 'a', 'properties': {'i_ts': 100, 's_device_id': 'module'}},
    {'url': 'a2', 'properties': {'i_ts': 150, 's_device_id': 'module'}},
    {'url': 'b', 'properties': {'i_ts': 200, 's_device_id': 'module'}},
    {'url': 'c', 'properties': {'i_ts': 300, 's_device_id': 'module'}},
    {'url': 'd', 'properties': {'i_ts': 400, 's_device_id': 'station'}},  # другой девайс
    # записи с непонятным device_id или таймштампом не трогаем
    {'url': 'e', 'properties': {'i_ts': 0, 's_device_id': 'module'}},
    {'url': 'f', 'properties': {'s_device_id': 'module'}},
]


@mock.patch('smarttv.alice.tv_proxy.proxy.indexer.saas_search_client', mocks.saas_search_client)
class TestWriter(object):
    endpoint = '/index-cable-channels'
    required_headers = {'HTTP_X_QUASARDEVICEID': 'd68c1bca4efa403313837b12f5cdcd26'}

    def make_req_and_check(self, data, headers, checking_code):
        response = http_client.post(
            path=self.endpoint,
            data=json.dumps(data),
            content_type='application/json',
            **headers
        )
        assert response.status_code == checking_code, response.content

    @mock.patch('smarttv.alice.tv_proxy.proxy.indexer.message_writer', mocks.always_successful_writer)
    def test_valid_request(self):
        self.make_req_and_check(valid_channels_data, self.required_headers, 200)

    @mock.patch('smarttv.alice.tv_proxy.proxy.indexer.message_writer', mocks.fatal_error_writer)
    def test_indexing_error(self):
        self.make_req_and_check(valid_channels_data, self.required_headers, 503)

    @mock.patch('smarttv.alice.tv_proxy.proxy.indexer.message_writer', mocks.always_successful_writer)
    def test_header_validation(self):
        self.make_req_and_check(valid_channels_data, {}, 400)

    @mock.patch('smarttv.alice.tv_proxy.proxy.indexer.message_writer', mocks.always_successful_writer)
    def test_body_validation(self):
        data = copy.deepcopy(valid_channels_data)
        del data['channels'][0]['uri']

        self.make_req_and_check(data, self.required_headers, 400)


class TestCleaner:
    """
    Кейсы класса Cleaner на удаление старых версий каналов из сааса
    """
    @pytest.fixture
    def cleaner(self, mocker):
        cleaner = Cleaner('module')
        mocker.patch.object(cleaner, 'delete_urls')
        return cleaner

    @pytest.fixture
    def found_document(self):
        def builder(url):
            return FoundDocument(url, ts=None, device_id=None)  # only url matters for test
        return builder

    @pytest.fixture
    def saas(self, mocker):
        yield mocker.patch('smarttv.alice.tv_proxy.proxy.indexer.saas_search_client')

    def test_rm_docs_by(self, cleaner, saas, found_document, mocker):
        saas.search.return_value = set([
            found_document('url1'), found_document('url2')
        ])
        # поскольку вызов delete_urls статический, приходится мокать класс
        Cleaner = mocker.patch('smarttv.alice.tv_proxy.proxy.indexer.Cleaner')

        cleaner.rm_docs_by('somequery')

        Cleaner.delete_urls.assert_called_once()
        call_args = Cleaner.delete_urls.call_args.args[0]
        assert sorted(call_args) == ['url1', 'url2']

    def test_rm_docs_earlier_than(self, saas, cleaner):
        """
        Проверим, что rm_docs_earlier_than не удаляет лишних документов
        """
        found_docs = []
        for item in saas_stored_documents:
            found_docs.append(build_found_document(item))
        saas.search.return_value = found_docs
        cleaner.rm_docs_earlier_than(200)

        cleaner.delete_urls.assert_called_with(['a', 'a2'])

    def test_exclude_works(self, saas, cleaner):
        found_docs = []
        for item in saas_stored_documents:
            found_docs.append(build_found_document(item))
        saas.search.return_value = found_docs
        cleaner.rm_docs_earlier_than(200, exclude=set(['a']))

        cleaner.delete_urls.assert_called_with(['a2'])


class TestCleanerInjection:
    """
    Проверка на то, что Cleaner валидирует пользовательский ввод в device_id
    """
    def test_in_call(self):
        cleaner = Cleaner('')
        cleaner.device_id = '* && some'
        with pytest.raises(ValueError):
            cleaner.rm_docs_earlier_than(100)


class TestFoundDocumentBuilder:
    @pytest.fixture
    def saas_doc(self):
        return {
            'properties': {
                'i_ts': '1642061934',
                's_device_id': 'module',
            },
            'url': 'content://url',
        }

    def test_all_properties_correct(self, saas_doc):
        expected = FoundDocument(url='content://url', ts=1642061934, device_id='module')
        assert build_found_document(saas_doc) == expected

    def test_without_timestamp(self, saas_doc):
        del saas_doc['properties']['i_ts']
        expected = FoundDocument(url='content://url', ts=None, device_id='module')
        assert build_found_document(saas_doc) == expected

    def test_without_device_id(self, saas_doc):
        del saas_doc['properties']['s_device_id']
        expected = FoundDocument(url='content://url', ts=1642061934, device_id=None)
        assert build_found_document(saas_doc) == expected
