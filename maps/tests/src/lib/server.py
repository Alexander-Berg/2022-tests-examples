import json
from maps.automotive.libs.large_tests.lib.http import http_request, http_request_json, HttpResponse, decode


ALFRED_SECRET_TO_CHECK='12345'
URL = 'http://127.0.0.1:9003'
HOST = 'auto-navi-carwashes.maps.yandex.net'
HEADERS = {'Host': HOST}
IDM_ROLE='refunder'
IDM_GROUP_SLUG='maps-auto-navi-carwashes'


def get_url():
    return URL


def get_host():
    return HOST


def get_user_phone(user):
    return http_request_json(
        'GET', URL + '/carwashes/1.x/user/phone',
        headers={
            'Authorization': f'OAuth {user.oauth}',
            'Host': HOST,
        })


def post_order(user, order):
    return http_request_json(
        'POST', URL + '/carwashes/1.x/order',
        headers={
            'Authorization': f'OAuth {user.oauth}',
            'Host': HOST,
        },
        json=order.toJson())


def get_active_order(user):
    status, body = http_request(
        'GET', URL + '/carwashes/1.x/order/active',
        headers={
            'Authorization': f'OAuth {user.oauth}',
            'Host': HOST,
        })
    return HttpResponse(
        status,
        decode(json.loads(body)) if status == 200 else body)


def mark_order_used(order_id, secret=ALFRED_SECRET_TO_CHECK):
    return http_request(
        'POST', URL + '/carwashes/1.x/order/used',
        headers={
            'Authorization': f'Bearer {secret}',
            'Host': HOST,
        },
        json={"order_id": order_id})


def post_refund(login, code, phone):
    return http_request(
        'POST', f'{URL}/carwashes/1.x/order/refund',
        headers={
            'Host': HOST,
            'X-User-Name': login
        },
        files={
            "field_1": json.dumps({
                "value": code,
                "question": {
                    "slug": "carwashes_code"
                }
            }),
            "field_2": json.dumps({
                "value": phone.number,
                "question": {
                    "slug": "phone_number"
                }
            }),
            "field_3": "dummy field"
        }
    )


def get_idm_info():
    return http_request_json(
        'GET', f'{URL}/idm/info/',
        headers={
            'Host': HOST
        })


def add_idm_role(login, role=IDM_ROLE, group=IDM_GROUP_SLUG):
    return http_request_json(
        'POST', f'{URL}/idm/add-role/',
        headers={
            'Host': HOST
        },
        params={
            "login": login,
            "role": json.dumps({
                group: role
            }),
        }
    )


def delete_idm_role(login, role=IDM_ROLE, group=IDM_GROUP_SLUG):
    return http_request_json(
        'POST', f'{URL}/idm/remove-role/',
        headers={
            'Host': HOST
        },
        params={
            "login": login,
            "role": json.dumps({
                group: role
            }),
        }
    )


def get_all_idm_roles():
    return http_request_json(
        'GET', f'{URL}/idm/get-all-roles/',
        headers={
            'Host': HOST
        })
