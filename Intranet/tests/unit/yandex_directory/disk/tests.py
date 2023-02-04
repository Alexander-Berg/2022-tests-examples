# coding: utf-8
import time

from hamcrest import (
    assert_that,
    equal_to,
    has_key,
)
from unittest.mock import (
    patch,
    Mock,
)

from requests import (
    Response,
    Request
)
from requests.exceptions import (
    HTTPError,
    ReadTimeout,
)

from testutils import (
    TestCase,
    SimpleTestCase,
    override_settings,
    create_inner_uid,
    create_outer_uid,
    assert_not_called,
    assert_called_once,
    mocked_requests,
)

from intranet.yandex_directory.src.yandex_directory.disk import (
    _make_request,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.retry import retry_http_errors
from intranet.yandex_directory.src.yandex_directory.disk.callbacks import activate_user
from intranet.yandex_directory.src.yandex_directory.auth import tvm


mocked_time = Mock(return_value=time.time())


class TestTVMHeaders(TestCase):
    @override_settings(DISK_BASE_URL='disk.yandex')
    def test_tvm_headers_added(self):
        tvm.tickets['disk'] = 'disk-ticket'
        with mocked_requests() as mocked_request:
            _make_request('get', 'handle')
            args, kwargs = mocked_request.get.call_args
            assert_that(kwargs, has_key('headers'))
            assert_that(kwargs['headers'], has_key('X-Ya-Service-Ticket'))


class TestCallback(TestCase):

    def test_new_organization(self):
        # при активации пользователя для новых организаций даем места как всем в Я.Диске
        org_id, uid = 123, create_inner_uid(1)
        with patch('intranet.yandex_directory.src.yandex_directory.disk.ActivateUserDiskTask.delay') as delay_task:
            activate_user(self.main_connection, org_id, {'id': uid, 'user_type': 'user'})

            assert_called_once(
                delay_task,
                org_id=org_id,
                uid=uid,
            )

    def test_portal_user(self):
        # при добавлении портального аккаунта в организацию не активируем b2b Я.Диск
        org_id, uid = 123, create_outer_uid()
        with patch('intranet.yandex_directory.src.yandex_directory.disk.ActivateUserDiskTask.delay') as delay_task:
            activate_user(self.main_connection, org_id, {'id': uid, 'user_type': 'user'})
            assert_not_called(delay_task)

    def test_robot(self):
        # при добавлении робота не активируем b2b Я.Диск
        org_id, uid = 123, create_inner_uid(1)

        with patch('intranet.yandex_directory.src.yandex_directory.disk.ActivateUserDiskTask.delay') as delay_task:
            activate_user(self.main_connection, org_id, {'id': uid, 'user_type': 'robot'})
            assert_not_called(delay_task)


# TODO: перенести
class TestRetryIfRetriableError(SimpleTestCase):

    def setUp(self):
        super(TestRetryIfRetriableError, self).setUp()
        # запрос якобы завершившийся с ошибкой
        self.failed_request = Request(url='http://example.yandex.ru/with-error')

    def test_http_error_exception(self):
        # ретрай нужен для ошибок HTTP 5хх

        # создадим исключение для 500 кода HTTP ответа
        response_5xx = Response()
        response_5xx.status_code = 500
        exc = HTTPError(response=response_5xx, request=self.failed_request)

        assert_that(
            retry_http_errors('disk')(exc),
            equal_to(True)
        )

    def test_read_timeout_exception(self):
        # ретрай нужен для ReadTimeout

        # создадим исключение ReadTimeout
        exc = ReadTimeout(request=self.failed_request)
        assert_that(
            retry_http_errors('disk')(exc),
            equal_to(True)
        )

    def test_some_exception(self):
        # на произвольных исключения ретрай не делаем
        assert_that(
            retry_http_errors('disk')(Exception()),
            equal_to(False)
        )
