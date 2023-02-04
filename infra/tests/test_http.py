import mock
import pytest
import requests

from object_validator import InvalidTypeError
from sepelib.http import request, session


def test_request_error():
    response = mock.Mock()
    response.status_code = 500
    response.text = 'Some error text'
    s = mock.Mock()
    s.request = mock.Mock(return_value=response)
    with pytest.raises(requests.RequestException):
        request.request("mock_method", 'mock_url', [200], session=s)


def test_request():
    response = mock.Mock()
    response.status_code = 200
    r = mock.Mock(return_value=response)
    with mock.patch('requests.request', r):
        request.request("mock_method", 'mock_url', [200])


def test_json_request():
    rsp = mock.Mock()
    rsp.json = mock.Mock(return_value={})
    req = mock.Mock(return_value=rsp)
    parse_mock = mock.Mock(return_value=('application/json', {}))
    with mock.patch('cgi.parse_header', parse_mock):
        with mock.patch('sepelib.http.request.request', return_value=req):
            request.json_request('get', 'some_url')


def test_json_request_error():
    with mock.patch('sepelib.http.request.request', side_effect=requests.RequestException):
        with pytest.raises(requests.RequestException):
            request.json_request('get', 'some_url')


def test_get_json_response_not_ok_status():
    res = mock.Mock()
    res.status_code = 400
    with pytest.raises(requests.RequestException):
        request.get_json_response(res, ok_statuses=[200])


def test_get_json_response_not_json():
    res = mock.Mock()
    res.status_code = 200
    parse_mock = mock.Mock(return_value=('application/notjson', {}))
    with mock.patch('cgi.parse_header', parse_mock):
        with pytest.raises(requests.RequestException):
            request.get_json_response(res, ok_statuses=[200])


def test_get_json_response_wrong_scheme():
    res = mock.Mock()
    res.status_code = 200
    parse_mock = mock.Mock(return_value=('application/json', {}))

    def raise_validation_error(*args):
        raise InvalidTypeError('error')

    with mock.patch('cgi.parse_header', parse_mock):
        with mock.patch('object_validator.validate', side_effect=raise_validation_error):
            with pytest.raises(requests.RequestException):
                request.get_json_response(res, ok_statuses=[200], scheme=mock.Mock())


def test_get_json_response():
    res = mock.Mock()
    res.status_code = 200
    parse_mock = mock.Mock(return_value=('application/json', {}))
    with mock.patch('cgi.parse_header', parse_mock):
        request.get_json_response(res, ok_statuses=[200])


def test_session_status_type():
    s = session.InstrumentedSession
    assert s._get_status_type(200) == '2xx'
    assert s._get_status_type(302) == '3xx'
    assert s._get_status_type(404) == '404'
    assert s._get_status_type(401) == '4xx'
    assert s._get_status_type(500) == '5xx'
    assert s._get_status_type(666) == 'other'


def test_session_request():
    s = session.InstrumentedSession('test_session')
    resp = mock.Mock()
    resp.status_code = 200
    r = mock.Mock(return_value=resp)
    with mock.patch('requests.Session.request', r):
        s.request('put', 'test_url')
    resp.status_code = 400
    with mock.patch('requests.Session.request', r):
        s.request('put', 'test_url')
        s.request('put', 'test_url')
    m = s.get_metrics()
    assert m['total_success']['count'] == 1
    assert m['total_error']['count'] == 2


def test_session_request_exception():
    s = session.InstrumentedSession('test_session')
    with mock.patch('requests.Session.request', side_effect=Exception):
        with pytest.raises(Exception):
            s.request('put', 'test_url')

    m = s.get_metrics()
    assert m['total_exception']['count'] == 1
