from maps.automotive.libs.large_tests.lib.http import http_request_json
from maps.automotive.libs.large_tests.lib.fakeenv import get_url


def set_tariff_price(price):
    return http_request_json(
        'POST', get_url() + '/tariff',
        json={'price': price}) >> 200
