import mock
import pytest
from requests import RequestException

from sepelib.yandex.its import ItsClient, ItsApiRequestException


def test_its_from_config(its_dict):
    i = ItsClient.from_config(its_dict)
    assert i._token == 'test_token'
    assert i._req_timeout == ItsClient._DEFAULT_REQ_TIMEOUT
    assert i._base_url == ItsClient._DEFAULT_BASE_URL
    assert i._attempts == ItsClient._DEFAULT_ATTEMPTS
    assert i._session.name == '/clients/its'


def test_resolve_instance_filter(its_client_mock):
    r = mock.Mock()
    r.json = mock.Mock(return_value={'result': [u'host1:80', u'host2:888']})
    r.status_code = 200
    r.headers = {'Content-Type': 'application/json'}
    with mock.patch('sepelib.http.request.request', return_value=r):
        head_instances = its_client_mock.resolve_instance_filter('some_filter')
        assert head_instances[0].host == 'host1'
        assert head_instances[0].port == '80'
        assert head_instances[1].host == 'host2'
        assert head_instances[1].port == '888'


def test_its_get_controls(its_client_mock):
    r = mock.Mock()
    r.json = mock.Mock(return_value={u'controls': u'testcontrol'})
    r.status_code = 200
    r.headers = {'Content-Type': 'application/json', 'Cache-Control': 'max-age="100"'}
    with mock.patch('sepelib.http.request.request', return_value=r):
        c = its_client_mock.get_controls(['test_tag'])
        assert c.controls == {'controls': 'testcontrol'}
        assert c.etag is None


def test_its_get_controls_invalid_max_age(its_client_mock):
    r = mock.Mock()
    r.json = mock.Mock(return_value={u'controls': u'testcontrol'})
    r.status_code = 200
    r.headers = {'Content-Type': 'application/json', 'Cache-Control': 'max-age="fish"', 'ETag': 'test_etag'}
    with mock.patch('sepelib.http.request.request', return_value=r):
        c = its_client_mock.get_controls(['test_tag'])
        assert c.cache_time is None
        assert c.etag == 'test_etag'


def test_get_ruchka_url(its_client_mock):
    u = its_client_mock._get_ruchka_url('/test/control/')
    assert u == '{}/values/test/control/'.format(its_client_mock._base_url)


def test_its_request(its_client_mock):
    with mock.patch('sepelib.http.request.request', return_value=mock.Mock()):
        its_client_mock._request('get', 'url', headers={'TestHeader': 'TH'})

    with mock.patch('sepelib.http.request.request', side_effect=RequestException):
        with pytest.raises(ItsApiRequestException):
            its_client_mock._request('get', 'url', headers={'TestHeader': 'TH'})


def test_get_control(its_client_mock):
    r = mock.Mock()
    r.json = mock.Mock(return_value={'test': 'test_val'})
    with mock.patch('sepelib.http.request.request', return_value=r):
        c = its_client_mock.get_control_value('path')
        assert c.control['test'] == 'test_val'

    with mock.patch('sepelib.http.request.request', side_effect=RequestException):
        with pytest.raises(ItsApiRequestException):
            its_client_mock.get_control_value('path')
