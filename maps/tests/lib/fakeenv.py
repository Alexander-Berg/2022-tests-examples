from maps.automotive.libs.large_tests.lib.http import http_request_json
import time

FAKE_ENV = 'http://127.0.0.1:9002'


def reset():
    status, _ = http_request_json('POST', FAKE_ENV + '/reset')
    assert(status == 200)


def add_user(user, confirmTime=None):
    data = {
        "login": user.name,
        "uid": user.oauth,
        "phone": {
            "number": user.phone,
            "confirmTime": str(confirmTime if confirmTime else int(time.time()))
        }
    }
    status, _ = http_request_json(
        'POST', FAKE_ENV + '/blackbox/adduser', params={'oauth': user.oauth}, json=data)
    assert(status == 200)


def set_user_phone(user, phone, confirmTime=None, secured=True):
    confirmTime = confirmTime or int(time.time()) - 1

    status, _ = http_request_json(
        'POST', FAKE_ENV + '/blackbox/user/%s/phone' % user.oauth,
        json={
            "number": phone,
            "confirmTime": str(confirmTime),
            "secured": secured
        })
    assert(status == 200)


def delete_user_phone(user):
    status, _ = http_request_json(
        'DELETE', FAKE_ENV + '/blackbox/user/%s/phone' % user.oauth)
    assert(status == 200)


def delete_user(token):
    status, _ = http_request_json('DELETE', FAKE_ENV + '/blackbox/user/' + token)
    assert(status == 200)
    return token


def read_sms(phone):
    status, phones = http_request_json(
        'GET', FAKE_ENV + '/yasms/readsms', params={'phone': phone})
    assert(status == 200)
    return phones


def get_sms_code(phone):
    sms_list = read_sms(phone)
    assert len(sms_list) == 1, str(sms_list)
    return sms_list[0][-4:]


def set_limit_exceeded(phone, is_exceeded):
    status, phones = http_request_json(
        'POST', FAKE_ENV + '/yasms/limit',
        params={'phone': phone, 'exceeded': ("1" if is_exceeded else "0")})
    assert(status == 200)


def get_datasync_flag(token):
    return http_request_json('GET', FAKE_ENV + '/datasync/flag',
                             params={"user": token})


def get_alice_requests(token):
    return http_request_json('GET', FAKE_ENV + '/alice/discovery/requests/%s' % token)
