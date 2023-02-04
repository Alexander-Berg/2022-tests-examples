from maps.automotive.libs.large_tests.lib.http import http_request_json
from maps.automotive.libs.large_tests.lib.fakeenv import get_url


def get_alfred_orders():
    return http_request_json('GET', get_url() + '/alfred/fake/orders') >> 200
