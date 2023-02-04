from maps.automotive.libs.large_tests.lib.http import http_request_json

FAKE_ENV = 'http://127.0.0.1:9002'


def get_url():
    return FAKE_ENV


def reset():
    status, _ = http_request_json('POST', get_url() + '/reset')
    assert(status == 200)
