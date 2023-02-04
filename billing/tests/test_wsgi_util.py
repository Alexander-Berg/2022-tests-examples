# -*- coding: utf-8 -*-

import unittest

from butils.wsgi_util import response_500_on_exception


namespace = {}


def my_start_response(status, headers, exc_info=None):
    namespace["status"] = status
    namespace["exc_info"] = exc_info


class TestResponse500OnException(unittest.TestCase):
    def setUp(self):
        namespace["status"] = None
        namespace["exc_info"] = None

    def test_function_app_ok(self):
        @response_500_on_exception
        def my_app(environ, start_response):
            start_response("200 Perfect", [])
            return ["okay"]

        result = my_app({}, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "200 "
        assert exc_info is None
        assert "".join(result) == "okay"

    def test_function_app_exc(self):
        @response_500_on_exception
        def my_app(environ, start_response):
            1 / 0

        result = my_app({}, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "500 "
        assert exc_info[0] == ZeroDivisionError
        assert "".join(result) != "okay"

    def test_method_app_ok(self):
        class MyApp(object):
            @response_500_on_exception
            def __call__(self, environ, start_response):
                start_response("200 Perfect", [])
                return ["okay"]

        my_app = MyApp()
        result = my_app({}, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "200 "
        assert exc_info is None
        assert "".join(result) == "okay"

    def test_method_app_err(self):
        class MyApp(object):
            @response_500_on_exception
            def __call__(self, environ, start_response):
                1 / 0

        my_app = MyApp()
        result = my_app({}, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "500 "
        assert exc_info[0] == ZeroDivisionError
        assert "".join(result) != "okay"


if __name__ == "__main__":
    unittest.main()


# vim:ts=4:sts=4:sw=4:tw=88:et:
