# coding: utf-8

from hamcrest import (
    assert_that,
    has_entries,
)
from unittest.mock import (
    patch,
    ANY,
)

from testutils import (
    TestCase,
    assert_called_once,
)
from intranet.yandex_directory.src.yandex_directory.core.views.check_mail_server import FAKE_CREDENTIALS
from intranet.yandex_directory.src.yandex_directory.core.yarm.exceptions import (
    YarmLoginError,
    YarmConnectionError,
)
from intranet.yandex_directory.src.yandex_directory.core.views.check_mail_server import CHECK_STATUSES


class TestCheckMailServerView(TestCase):
    def setUp(self):
        super(TestCheckMailServerView, self).setUp()
        self.common_params = '?host=test.test&port=999'
        self.url = '/mail-migration/check-server/'

    def test_timeout_error(self):
        # передаем в ручку ssl=1 и protocol=imap
        # делаем вид, что check_server вернул YarmConnectionError
        # проверяем, что check_server вызвался с ssl=True, imap=True
        # провряем, что ручка возвращает status='timeout'
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.check_mail_server.check_server') as check_server:
            check_server.side_effect = YarmConnectionError('test')
            response = self.get_json(
                '{url}{common_params}&ssl=1&protocol=imap'.format(
                    url=self.url,
                    common_params=self.common_params,
                ),
            )
            assert_that(
                response,
                has_entries(
                    status=CHECK_STATUSES.timeout,
                )
            )
            assert_called_once(
                check_server,
                ANY,
                FAKE_CREDENTIALS,
                'test.test',
                999,
                uid=None,
                imap=True,
                ssl=True,
            )

    def test_mail_server_exception(self):
        # передаем в ручку ssl=0, protocol=pop3
        # делаем вид, что check_server вернул какой-то Exception
        # проверяем, что check_server вызвался с ssl=False, imap=False
        # провряем, что ручка возвращает status='checking mail server error'
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.check_mail_server.check_server') as check_server:
            check_server.side_effect = Exception()
            response = self.get_json(
                '{url}{common_params}&ssl=0&protocol=pop3'.format(
                    url=self.url,
                    common_params=self.common_params,
                ),
            )
            assert_that(
                response,
                has_entries(
                    status=CHECK_STATUSES.error,
                )
            )
            assert_called_once(
                check_server,
                ANY,
                FAKE_CREDENTIALS,
                'test.test',
                999,
                uid=None,
                imap=False,
                ssl=False,
            )

    def test_check_mail_server_ok(self):
        # не передаем в ручку ssl. protocol=imap
        # делаем вид, что check_server вернул YarmLoginError
        # проверяем, что check_server вызвался с ssl=True, imap=True
        # провряем, что ручка возвращает status='ok'
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.check_mail_server.check_server') as check_server:
            check_server.side_effect = YarmLoginError('test')
            response = self.get_json(
                '{url}{common_params}&protocol=imap'.format(
                    url=self.url,
                    common_params=self.common_params,
                ),
            )
            assert_that(
                response,
                has_entries(
                    status=CHECK_STATUSES.ok,
                )
            )
            assert_called_once(
                check_server,
                ANY,
                FAKE_CREDENTIALS,
                'test.test',
                999,
                uid=None,
                imap=True,
                ssl=True,
            )

    def test_invalid_protocol(self):
        # передаем в ручку некорректный протокол
        # провряем, что ручка возвращает ошибку 422
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.check_mail_server.check_server') as check_server:
            check_server.side_effect = YarmLoginError('test')
            self.get_json(
                '{url}{common_params}&protocol=some'.format(
                    url=self.url,
                    common_params=self.common_params,
                ),
                expected_code=422,
            )
