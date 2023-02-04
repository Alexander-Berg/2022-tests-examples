from maps.automotive.libs.large_tests.lib.http import http_request_json
from maps.automotive.libs.large_tests.lib.fakeenv import get_url


def add_user(user):
    data = {
        "login": user.login,
        "uid": user.uid,
        "phones": [phone.dump() for phone in user.phones]
    }
    http_request_json(
        'POST', get_url() + '/blackbox/adduser',
        params={'oauth': user.oauth}, json=data) >> 200
