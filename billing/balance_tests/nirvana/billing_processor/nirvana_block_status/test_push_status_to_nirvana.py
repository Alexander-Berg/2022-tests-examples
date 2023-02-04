import json
import logging

import httpretty
import mock
import pytest
from requests.exceptions import HTTPError, ConnectionError

from balance.actions.nirvana import status


@pytest.fixture()
def nirvana_block_status():
    return {'some': 'json'}


@pytest.fixture()
def get_block_status_mock(nirvana_block_status):
    with mock.patch('balance.actions.nirvana.status.get_block_status') as m:
        m.return_value = nirvana_block_status
        yield m


@pytest.fixture()
def mock_nirvana_push_status_uri_ok(nirvana_block, nirvana_block_status):
    def request_callback(request, uri, response_headers):
        assert json.loads(request.body) == nirvana_block_status
        assert request.headers.get('Content-Type') == 'application/json'
        return 200, response_headers, ''

    httpretty.register_uri(
        httpretty.PUT, nirvana_block.change_status_callback_url,
        body=request_callback,
    )
    yield


@pytest.fixture()
def mock_nirvana_push_status_uri_500(nirvana_block):
    httpretty.register_uri(
        httpretty.PUT, nirvana_block.change_status_callback_url,
        status=500, body='Some message',
    )
    yield


@pytest.fixture(name='log')
def mock_logger():
    with mock.patch.object(status.log, 'makeRecord') as log:
        yield log


@pytest.mark.usefixtures('mock_nirvana_push_status_uri_ok')
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.usefixtures('get_block_status_mock')
def test_push_status_ok(nirvana_block, log):
    status.push_status_to_nirvana(nirvana_block)
    assert log.call_count == 0


def assert_logger_call_params(call_args, log_level, msg, exc):
    assert call_args[1] == log_level
    assert call_args[4] == msg
    assert call_args[6][0] == exc

@pytest.mark.usefixtures('mock_nirvana_push_status_uri_500')
@pytest.mark.usefixtures('httpretty_enabled_fixture')
@pytest.mark.usefixtures('get_block_status_mock')
def test_push_status_returns_500(nirvana_block, log):
    status.push_status_to_nirvana(nirvana_block)
    assert log.call_count == 1
    assert_logger_call_params(log.call_args[0], logging.ERROR, 'Failed to push status to Nirvana.', HTTPError)


@pytest.mark.usefixtures('get_block_status_mock')
def test_push_status_no_connection(nirvana_block, log):
    status.push_status_to_nirvana(nirvana_block)
    assert log.call_count == 1
    assert_logger_call_params(log.call_args[0], logging.ERROR, 'Failed to push status to Nirvana.', ConnectionError)
