from maps.automotive.libs.large_tests.lib.http import http_request

POLLER_URL = "http://127.0.0.1:9004"
HOST = 'auto-pandora-poller.maps.yandex.net'
HEADERS = {'Host': HOST}


def get_url():
    return POLLER_URL


def get_host():
    return HOST


def stop():
    http_request("POST", POLLER_URL + "/__stop", headers=HEADERS) >> 200


def start():
    http_request("POST", POLLER_URL + "/__start", headers=HEADERS) >> 200
