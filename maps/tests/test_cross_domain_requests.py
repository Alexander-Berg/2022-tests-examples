import requests
from maps.b2bgeo.ya_courier.backend.test_lib.util import request
from maps.b2bgeo.libs.py_http.headers import is_yandex_origin


def test_yandex_origin_recognition(system_env_with_db):
    for origin, expected_is_yandex in [
            (None, False),
            ('http://yandex.ru', False),
            ('http://yandex.com.tr', False),
            ('https://yandex.ru', True),
            ('https://zyandex.ru', False),
            ('https://.yandex.ru', False),
            ('https://m.yandex.ru', True),
            ('https://a.b.yandex.ru', True),
            ('https://yandex.ru:', False),
            ('https://yandex.ru:123', True),
            ('https://my.yandex.ru:1', True),
            ('https://zandex.ru:123', False),
            ('https://yandex.com.tr', True),
            ('https://m.yandex.com.tr', True),
            ('https://yandex.com.tr:123', True),
            ('https://yandex.com', True),
            ('https://m.yandex.com', True),
            ('https://yandex.com:41', True),
            ('https://yandex.com.fr', False),
            ('https://yandex.tr', False),
            ('https://zyandex.com.tr', False),
            ('https://localhost.msup.yandex.ru', True),
            ]:
        assert is_yandex_origin(origin) == expected_is_yandex


def test_response_headers(system_env_with_db):
    env = system_env_with_db
    for origin, expected_extra_headers in [
            (None, False),
            ('https://yandex.ru:123', True),
            ('https://my.yandex.ru', True),
            ('https://zyandex.ru', False),
            ('https://zyandex.ru:123', False),
            ('https://yandex.com.tr', True),
            ('https://my.yandex.com.tr', True),
            ('https://fyandex.com.tr', False),
            ('https://a.b.yandex.com.tr', True),
            ]:
        response = request(
            method='get',
            url="{}/api/v1/test".format(env.url),
            headers={**env.get_headers(env.auth_header_super), 'Origin': origin},
            verify=env.verify_ssl
        )
        assert response.status_code == requests.codes.ok
        assert response.json() == {'message': 'OK'}
        if expected_extra_headers:
            assert response.headers.get('Access-Control-Allow-Origin') == origin
            assert response.headers.get('Access-Control-Allow-Headers') == 'Content-Type, X-Requested-With, X-CSRF-Token'
            assert response.headers.get('Access-Control-Allow-Credentials') == 'true'
            assert response.headers.get('Access-Control-Allow-Methods') == 'POST, GET, PUT, DELETE, PATCH'
        else:
            assert response.headers.get('Access-Control-Allow-Origin') is None
