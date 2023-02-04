# -*- coding: utf-8 -*-

from testutils import SimpleTestCase

from sqlalchemy.exc import OperationalError
from hamcrest import (
    assert_that,
    equal_to
)

from intranet.yandex_directory.src.yandex_directory.common.db import retry_on_close_connect_to_server


class RetryException(OperationalError):
    def __init__(self, message, *args, **kwargs):
        orig = Exception(message)
        super(RetryException, self).__init__('', '', orig)


class TestRetryOnCloseConnectToServer(SimpleTestCase):

    def test_need_retry(self):
        # должны ретраить на ошибки содержащие в сообщении строку "SSL connection has been closed unexpectedly"

        message = 'database options changed SSL connection has been closed unexpectedly'
        assert_that(
            retry_on_close_connect_to_server(RetryException(message)),
            equal_to(True)
        )

    def test_not_retry_(self):
        # не ретраим на произвольные ошибки

        assert_that(
            retry_on_close_connect_to_server(Exception()),
            equal_to(False)
        )

    def test_no_message(self):
        # не ретраим на произвольные ошибки если нет необходимой строки в сообщении

        assert_that(
            retry_on_close_connect_to_server(RetryException('some text')),
            equal_to(False)
        )
