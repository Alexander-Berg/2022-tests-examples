# coding: utf-8

import mock
import pytest
import requests
import httpretty

from butils.application import getApplication

from balance import exc, constants as cst
from balance.actions.dcs import dcsaap

from tests import object_builder as ob


@pytest.fixture(autouse=True)
def tvm_client_mock():
    with mock.patch('balance.tvm.get_or_create_tvm_client') as m:
        m.return_value.get_service_ticket_for.return_value = 'ticket'
        yield m


@pytest.fixture
def request_dcsaap_mock():
    with mock.patch('balance.actions.dcs.dcsaap.request_dcsaap') as m:
        yield m


@pytest.fixture
def nirvana_block(session):
    return ob.NirvanaBlockBuilder(). \
        add_input('run', data_type='json'). \
        add_output('run', data_type='json'). \
        build(session).obj


@pytest.fixture
def nirvana_block_download_mock():
    mock_path = 'balance.mapper.nirvana_processor.NirvanaBlock.download'
    return_value = '{"id": 123456}'
    with mock.patch(mock_path, return_value=return_value) as m:
        yield m


@pytest.fixture
def set_run(request_dcsaap_mock):
    def set_run_(new_status, error=None, diffs_count=None):
        run = {'status': int(new_status), 'error': error}
        if diffs_count is not None:
            run['diffs_count'] = diffs_count
        request_dcsaap_mock.return_value.json.return_value = run
    return set_run_


class TestGetCurrentRunId(object):
    """
    Тесты на считывание ID-запуска из операции
    """

    def test_correct_input(self, nirvana_block, nirvana_block_download_mock):
        nirvana_block_download_mock.return_value = '{"id": "123456"}'
        value = dcsaap._get_current_run_id(nirvana_block)
        assert value == 123456

    def test_invalid_input(self, nirvana_block, nirvana_block_download_mock):
        nirvana_block_download_mock.return_value = '{"something": "else"}'
        with pytest.raises(exc.INVALID_PARAM):
            dcsaap._get_current_run_id(nirvana_block)


@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestRequestDCSAAP(object):
    """
    Тесты на вызов API системы сравнения данных
    """

    @staticmethod
    def register_api_path(method, api_path):
        dcsaap_url = getApplication().get_component_cfg('dcsaap')['URL']
        expected_url = '{}/{}'.format(dcsaap_url, api_path)
        httpretty.register_uri(method, expected_url)

    def test_get_request(self):
        api_path = 'api/v1/run/1/'
        self.register_api_path(httpretty.GET, api_path)
        dcsaap.request_dcsaap(api_path)

        request = httpretty.last_request()
        assert request.method == httpretty.GET
        assert request.headers.get(cst.TVM2_SERVICE_TICKET_HEADER) == 'ticket'

    def test_post_request(self):
        api_path = 'api/v1/check/'
        self.register_api_path(httpretty.POST, api_path)

        json_body = {'check_model': 1}
        dcsaap.request_dcsaap(api_path, json_=json_body)

        request = httpretty.last_request()
        assert request.method == httpretty.POST
        assert request.parsed_body == json_body

    def test_path_correction(self):
        expected_api_path = '/api/v1/run/1/'
        self.register_api_path(httpretty.GET, expected_api_path.lstrip('/'))

        api_path = '/api/v1/run/1'
        dcsaap.request_dcsaap(api_path)

        request = httpretty.last_request()
        assert request.path == expected_api_path


class TestRunCheck(object):
    """
    Проверяем запуск сверки в системе сравнения данных
    """

    @pytest.fixture
    def nirvana_block_upload_mock(self):
        with mock.patch('balance.mapper.nirvana_processor.NirvanaBlock.upload') as m:
            yield m

    def test_invalid_output(self, session):
        nirvana_block = ob.NirvanaBlockBuilder(). \
            add_output('output', data_type='text'). \
            build(session).obj

        with pytest.raises(exc.NOT_FOUND):
            dcsaap.run_check(nirvana_block, 1)

    def test_already_run(self, nirvana_block, request_dcsaap_mock):
        already_run_response = requests.Response()
        already_run_response.status_code = 409
        already_run_response.reason = 'conflict'

        request_dcsaap_mock.return_value = already_run_response

        with pytest.raises(requests.HTTPError):
            dcsaap.run_check(nirvana_block, 1)

    def test_check_run(self, nirvana_block, request_dcsaap_mock, nirvana_block_upload_mock):
        response = mock.Mock(status_code=201)
        response.json.return_value = {'id': 123456, 'check_model': 1, 'anything': 'else'}
        request_dcsaap_mock.return_value = response

        dcsaap.run_check(nirvana_block, 1)
        nirvana_block_upload_mock.assert_called_once_with('run', '{"id": 123456}')


@pytest.mark.usefixtures("nirvana_block_download_mock")
class TestCheckStatusAndFinishOrDefer(object):
    """
    Тестируем фукнцию проверки статуса сверки в системе сравнения данных
    """

    def test_started(self, nirvana_block, set_run):
        set_run(new_status=dcsaap.RunStatus.STARTED)
        with pytest.raises(exc.DEFERRED_ERROR):
            dcsaap.check_status_and_finish_or_defer(nirvana_block)
        with pytest.raises(exc.DEFERRED_ERROR) as exc_info:
            dcsaap.check_status_and_finish_or_defer(nirvana_block, retry_delay=600)
        assert 9 < exc_info.value.delay < 11

    def test_error(self, nirvana_block, set_run):
        set_run(new_status=dcsaap.RunStatus.ERROR, error='some error')
        with pytest.raises(exc.CRITICAL_ERROR) as exc_info:
            dcsaap.check_status_and_finish_or_defer(nirvana_block)
        assert 'some error' in str(exc_info.value)

    def test_finished(self, nirvana_block, set_run):
        set_run(new_status=dcsaap.RunStatus.FINISHED)
        dcsaap.check_status_and_finish_or_defer(nirvana_block)


@pytest.mark.usefixtures("nirvana_block_download_mock")
class TestFailIfDiffsThresholdExceeded(object):
    """
    Тестируем функцию проверки количества расхождений
    """

    def test_started(self, nirvana_block, set_run):
        set_run(new_status=dcsaap.RunStatus.STARTED)
        with pytest.raises(AssertionError, match='has non-finished status'):
            dcsaap.fail_if_diffs_threshold_exceeded(nirvana_block)

    def test_has_no_diffs_count(self, nirvana_block, set_run):
        set_run(new_status=dcsaap.RunStatus.FINISHED)
        with pytest.raises(AssertionError, match='has no diffs count field'):
            dcsaap.fail_if_diffs_threshold_exceeded(nirvana_block)

    def test_threshold_exceeded(self, nirvana_block, set_run):
        set_run(new_status=dcsaap.RunStatus.FINISHED, diffs_count=1)
        with pytest.raises(exc.CRITICAL_ERROR):
            dcsaap.fail_if_diffs_threshold_exceeded(nirvana_block)

    def test_threshold_not_exceeded(self, nirvana_block, set_run):
        set_run(new_status=dcsaap.RunStatus.FINISHED, diffs_count=0)
        dcsaap.fail_if_diffs_threshold_exceeded(nirvana_block)
