# coding: utf-8

import pytest
import requests
import httpretty

from balance import exc

from tests import object_builder as ob


class TestDownload(object):
    """
    Тесты на получение данных из входов Nirvana-блока
    """

    @pytest.fixture
    def nirvana_block(self, session):
        return ob.NirvanaBlockBuilder(). \
            add_input('text_input', data_type='text'). \
            add_input('json_input', data_type='json'). \
            add_input('complex_input', data_type='text'). \
            add_input('complex_input', data_type='json'). \
            build(session).obj

    def test_invalid_input(self, nirvana_block):
        invalid_input_name = 'unknown_input'
        expected_exception_re = 'has no input {}$'.format(invalid_input_name)
        with pytest.raises(exc.INVALID_PARAM, match=expected_exception_re):
            nirvana_block.download(invalid_input_name)

    def test_invalid_input_index(self, nirvana_block):
        input_name = 'complex_input'
        invalid_index = len(nirvana_block.inputs[input_name]['items'])
        expected_exception_re = 'has no available input index {}$'.format(invalid_index)
        with pytest.raises(exc.INVALID_PARAM, match=expected_exception_re):
            nirvana_block.download(input_name, invalid_index)

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_invalid_url(self, nirvana_block):
        input_name = 'text_input'

        download_url = nirvana_block.inputs[input_name]['items'][0]['downloadURL']
        httpretty.register_uri(httpretty.GET, download_url, status=404)

        with pytest.raises(requests.HTTPError):
            nirvana_block.download(input_name)

    @pytest.mark.usefixtures('httpretty_enabled_fixture')
    def test_successful_download(self, nirvana_block):
        expected_content = 'OK'

        for input_ in nirvana_block.inputs.values():
            for item in input_['items']:
                httpretty.register_uri(httpretty.GET, item['downloadURL'], expected_content)

        for input_name in ('text_input', 'json_input', 'complex_input'):
            content = nirvana_block.download(input_name)
            assert content == expected_content

        content = nirvana_block.download('complex_input', input_index=1)
        assert content == expected_content
