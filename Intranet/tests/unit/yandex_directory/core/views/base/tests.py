# -*- coding: utf-8 -*-
import json
from unittest.mock import Mock, patch

from flask import g, Response, request
from hamcrest import (
    assert_that,
    has_entries,
    has_properties,
)

from sqlalchemy.exc import OperationalError

from intranet.yandex_directory.src.yandex_directory.auth.user import User
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.views.base import View as BaseView
from intranet.yandex_directory.src.yandex_directory.common.exceptions import ImmediateReturn, ReadonlyModeError
from intranet.yandex_directory.src.yandex_directory.auth.decorators import no_permission_required
from intranet.yandex_directory.src.yandex_directory.auth.service import Service

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.core.views.api_versioning import get_methods_map_for_view


class BaseTestCase(TestCase):
    def setUp(self):
        super(BaseTestCase, self).setUp()
        class View(BaseView):
            def get(self, *args, **kwargs):
                return 'ok'
        View._methods_map = get_methods_map_for_view(View)
        self.View = View

# имитируем ошибку от pgbouncer
class RetryException(OperationalError):
    def __init__(self, *args, **kwargs):
        orig = Exception('database options changed SSL connection has been closed unexpectedly')
        super(RetryException, self).__init__('', '', orig)


class TestViewCase__get_ordering_fields(BaseTestCase):
    def setUp(self):
        super(TestViewCase__get_ordering_fields, self).setUp()

        self.allowed_fields = ['name', 'id', 'some_field']
        self.view = self.View()
        self.view.allowed_ordering_fields = self.allowed_fields

    def test_get_ordering_fields_without_ordering_arg(self):
        with app.test_request_context('/?some-param=1'):
            fields = self.view._get_ordering_fields()
        # при None в модели будет использован порядок сортировки по умолчанию
        self.assertIsNone(fields)

    def test_get_ordering_fields_with_one_ordering_arg(self):
        with app.test_request_context('/?ordering=id'):
            fields = self.view._get_ordering_fields()
        self.assertEqual(fields, ['id'])

    def test_get_ordering_fields_with_many_ordering_arg(self):
        with app.test_request_context('/?ordering=id,name'):
            fields = self.view._get_ordering_fields()
        self.assertEqual(fields, ['id', 'name'])

    def test_get_ordering_fields_should_raise_error_if_field_is_not_allowed(self):
        with app.test_request_context('/?ordering=name,my_param'):
            request.api_version = 5

            with self.assertRaises(ImmediateReturn):
                self.view._get_ordering_fields()


class TestView__view(BaseTestCase):
    def test_retry(self):
        # ретраим выполнение запроса при разрыве соединения
        with patch.object(self.View, 'get', side_effect=RetryException) as mc:
            view = self.View()
            try:
                with app.test_request_context('/'):
                    view.dispatch()
            except RetryException:
                pass

            # зафиксировано три попытки вызвать get_shard
            self.assertEqual(mc.call_count, 3)

    def test_ro_mode(self):
        # отдаем 503 в ro режиме
        view = self.View()
        with patch.object(self.View, 'get', side_effect=ReadonlyModeError):
            with app.test_request_context('/'):
                response = view.dispatch()
        assert_that(
            response,
            has_properties(
                status_code=503,
            )
        )
        assert_that(
            json.loads(response.data),
            has_entries(
                code='read_only_mode',
            )
        )

    def test_default_error_handler_returns_location(self):
        # При редиректе должен отдаваться заголовок Location
        # Этот тест нужен, потому что как-то раз в результате рефакторинга
        # мы поломали то, как возвращаются заголовки в ответах с редиректом.
        #
        # Редирект может происходить двумя способами.
        # Либо быть возвращён из вьюхи как response,
        # либо кинут как исключение RequestRedirect.
        #
        # Именно так поступает flask, если ему надо средиректить
        # на роут оканчивающийся на обратный слэш.
        # В таком случае срабатывает обработчик исключений, который у нас
        # особенный, так как должен возвращать сообщения об ошибке, как json.
        # И как раз это мы здесь и проверяем.
        from intranet.yandex_directory.src.yandex_directory.common.web import handle_exception
        from werkzeug.routing import RequestRedirect

        exc = RequestRedirect('/users/')

        # Исключения такого типа обработчик должен пропускать
        # через себя без изменений.
        response = handle_exception(exc)
        assert response is exc
