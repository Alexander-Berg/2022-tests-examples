# -*- coding: utf-8 -*-

import unittest

from mock import Mock, patch

from butils.tvm.invoker_mixin import mixin_maker
from butils.tvm.exception import (
    TicketParsingException,
    TvmException,
    TvmWrongDestination,
)


namespace = {}


def my_start_response(status, headers, exc_info=None):
    namespace["status"] = status
    namespace["exc_info"] = exc_info


class SomeBaseInvoker(object):
    """
    Not really an invoker
    """

    def __call__(self, environ, start_response):
        namespace["environ"] = environ
        start_response("200 OK", [])
        return ["okay"]


class TestInvokerMixin(unittest.TestCase):
    @classmethod
    def setUpClass(cls):

        # Mock butils.application
        app = Mock()

        def check(ticket):
            fate, src = ticket.split("|")
            if fate == "TvmWrongDestination":
                raise TvmWrongDestination("bad dst")
            if fate == "TicketParsingException":
                raise TicketParsingException("arg1", "arg2", "arg3")
            if fate == "TvmException":
                raise TvmException("xoxoxo")
            ticket_obj = Mock()
            ticket_obj.src = src
            return ticket_obj

        app.tvm._check_service_ticket.side_effect = check
        cls.patcher = patch("butils.application.getApplication", return_value=app)
        cls.patcher.start()

    @classmethod
    def tearDownClass(cls):
        cls.patcher.stop()

    def setUp(self):
        namespace["status"] = None
        namespace["exc_info"] = None
        namespace["environ"] = None

    def test_strict_without_tvm(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=True)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "403 "
        assert exc_info is None
        assert "TVM" in "".join(result)

    def test_lax_without_tvm(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=False)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "200 "
        assert exc_info is None
        assert "okay" in "".join(result)

    def test_strict_with_bad_tvm_destination(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=True)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_YA_SERVICE_TICKET": "TvmWrongDestination|"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "403 "
        assert exc_info is None
        assert "destination" in "".join(result)

    def test_lax_with_bad_tvm_destination(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=False)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_YA_SERVICE_TICKET": "TvmWrongDestination|"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "403 "
        assert exc_info is None
        assert "destination" in "".join(result)

    def test_strict_with_malformed_tvm(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=True)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_YA_SERVICE_TICKET": "TicketParsingException|"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "403 "
        assert exc_info is None
        assert "malformed" in "".join(result)

    def test_lax_with_malformed_tvm(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=False)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_YA_SERVICE_TICKET": "TicketParsingException|"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "403 "
        assert exc_info is None
        assert "malformed" in "".join(result)

    def test_strict_with_tvm_exception(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=True)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_YA_SERVICE_TICKET": "TvmException|"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "403 "
        assert exc_info is None
        assert "TVM" in "".join(result)

    def test_lax_with_tvm_exception(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=False)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_YA_SERVICE_TICKET": "TvmException|"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "403 "
        assert exc_info is None
        assert "TVM" in "".join(result)

    def test_strict_with_unauthorized_source(self):
        def check_requester(requester_app_id):
            return False

        MyTvmMixin = mixin_maker(check_requester, strict=True)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_YA_SERVICE_TICKET": "Ok|123"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "403 "
        assert exc_info is None
        assert "denied for [123]" in "".join(result)

    def test_lax_with_unauthorized_source(self):
        def check_requester(requester_app_id):
            return False

        MyTvmMixin = mixin_maker(check_requester, strict=False)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_YA_SERVICE_TICKET": "Ok|123"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "403 "
        assert exc_info is None
        assert "denied for [123]" in "".join(result)

    def test_strict_with_erroneous_authorization(self):
        def check_requester(requester_app_id):
            1 / 0

        MyTvmMixin = mixin_maker(check_requester, strict=True)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_YA_SERVICE_TICKET": "Ok|123"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "500 "
        assert exc_info[0] == ZeroDivisionError
        assert "troubles" in "".join(result)

    def test_lax_with_erroneous_authorization(self):
        def check_requester(requester_app_id):
            1 / 0

        MyTvmMixin = mixin_maker(check_requester, strict=False)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_YA_SERVICE_TICKET": "Ok|123"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        assert status[:4] == "500 "
        assert exc_info[0] == ZeroDivisionError
        assert "troubles" in "".join(result)

    def test_strict_with_forged_requester_id_header(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=True)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {
            "HTTP_X_YA_SERVICE_TICKET": "Ok|123",
            "HTTP_X_REQUESTER_TVM_ID": "456",
        }
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        environ = namespace["environ"]
        assert status[:4] == "200 "
        assert exc_info is None
        assert environ.get("HTTP_X_REQUESTER_TVM_ID") == "123"
        assert "okay" in "".join(result)

    def test_lax_with_forged_requester_id_header(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=False)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {
            "HTTP_X_YA_SERVICE_TICKET": "Ok|123",
            "HTTP_X_REQUESTER_TVM_ID": "456",
        }
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        environ = namespace["environ"]
        assert status[:4] == "200 "
        assert exc_info is None
        assert environ.get("HTTP_X_REQUESTER_TVM_ID") == "123"
        assert "okay" in "".join(result)

    def test_lax_without_tvm_and_with_forged_requester_id_header(self):
        def check_requester(requester_app_id):
            return True

        MyTvmMixin = mixin_maker(check_requester, strict=False)

        class MyInvoker(MyTvmMixin, SomeBaseInvoker):
            pass

        inv = MyInvoker()
        environ = {"HTTP_X_REQUESTER_TVM_ID": "456"}
        result = inv(environ, my_start_response)
        status = namespace["status"]
        exc_info = namespace["exc_info"]
        environ = namespace["environ"]
        assert status[:4] == "200 "
        assert exc_info is None
        assert environ.get("HTTP_X_REQUESTER_TVM_ID") is None
        assert "okay" in "".join(result)


if __name__ == "__main__":
    unittest.main()


# vim:ts=4:sts=4:sw=4:tw=88:et:
