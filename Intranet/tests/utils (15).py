# coding: utf-8

import Cookie


class MockResponse(object):
    def __init__(self, body):
        self.body = body
        self.request_time = 1.0


class MockRequest(object):
    def __init__(self, headers=None, cookies=None, host="test.yandex-team.ru"):
        if headers is None:
            headers = {}
        if cookies is None:
            cookies = {}

        self.host = host
        self.headers = headers
        cookies = {str(key): str(value) for key, value in cookies.items()}
        self.cookies = Cookie.SimpleCookie(cookies)
