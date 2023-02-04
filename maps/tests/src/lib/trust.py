from maps.automotive.libs.large_tests.lib.http import http_request_json
from maps.automotive.libs.large_tests.lib.fakeenv import get_url


def add_payment_methods(user):
    http_request_json(
        'POST', get_url() + '/trust/payment-methods',
        params={'uid': user.uid}, json=[m.dump() for m in user.payment_methods]) >> 200
