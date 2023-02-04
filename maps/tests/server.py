import requests
import urllib.parse


def test_ping(async_backend_url):
    response = requests.get(async_backend_url + "/ping")
    assert response.ok


def test_security_headers(async_backend_url):
    response = requests.get(async_backend_url + "/ping")
    assert response.ok
    assert response.headers['X-Content-Type'] == 'nosniff'

    response = requests.get(async_backend_url + "/result/non-existent")
    assert not response.ok
    assert response.headers['X-Content-Type'] == 'nosniff'


def test_cors(async_backend_url_module_scope):
    for path, expect_successful_get, has_options in [
            ('ping', True, False),
            ('unistat', True, True),
            ('result/XXX', False, True),
            ('cancel/XXX', False, True),
            ('log/request/XXX', False, True),
            ('log/response/XXX', False, True),
            ('stat/task_info?time_interval=' + urllib.parse.quote('2000-01-25T00:00:00Z/2000-01-25T04:00:00Z', safe=''), True, True),
            ('stat/apikeys?time_interval=2020-09-27T00:00:00Z/2020-09-27T02:00:00Z', True, True)]:

        for method in (['OPTIONS', 'GET'] if has_options else ['GET']):

            for origin, expect_extra_headers in [
                    (None, False),
                    ('https://yandex.ru', path.startswith('result/') and method == 'GET'),
                    ('https://xxx.yandex.ru', False),
                    ('s3.mds.yandex.net', False),
                    ('https://s3.mds.yandex.net', True),
                    ('https://s3.mds.yandex.net:123', False)]:

                response = requests.request(
                    method=method,
                    url=async_backend_url_module_scope + '/' + path,
                    headers={'Authorization': 'OAuth TEST_AUTH', 'Origin': origin}
                )

                if method != 'GET' or expect_successful_get:
                    assert response.status_code == requests.codes.ok, response.text

                if expect_extra_headers:
                    assert response.headers.get('Access-Control-Allow-Origin') == origin
                    assert response.headers.get('Access-Control-Allow-Headers') == 'Content-Type, Authorization'
                    assert response.headers.get('Access-Control-Allow-Methods') == 'GET, POST, OPTIONS'
                else:
                    assert response.headers.get('Access-Control-Allow-Origin') is None
