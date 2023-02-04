from maps.automotive.libs.large_tests.lib.http import http_request_json
from maps.automotive.libs.large_tests.lib.fakeenv import get_url


def set_order_pay_state(order_id, state):
    return http_request_json('PUT', get_url() + f'/payment/fake/order/{order_id}/pay_status/{state}') >> 200


def set_order_refund_state(refund_id, state):
    return http_request_json('PUT', get_url() + f'/payment/fake/order/{refund_id}/refund_status/{state}') >> 200


def get_order(order_id):
    return http_request_json('GET', get_url() + f'/payment/v1/internal/order/911/{order_id}') >> 200


def get_last_order():
    return http_request_json('GET', get_url() + '/payment/fake/order/last') >> 200
