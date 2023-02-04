# coding: utf-8
import json
import sys

from flask import current_app
from hamcrest import *
from unittest.mock import patch

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.setup import versioned_blueprint
from intranet.yandex_directory.src.yandex_directory.common.utils import json_response
from intranet.yandex_directory.src.yandex_directory.auth.decorators import no_permission_required
from intranet.yandex_directory.src.yandex_directory.core.views.base import View
from intranet.yandex_directory.src.yandex_directory.auth.middlewares import (
    SIMPLE_PING_URL,
)
from intranet.yandex_directory.src.yandex_directory.common.db import not_closing
from intranet.yandex_directory.src.yandex_directory.directory_logging.logger import error_log


from testutils import (
    TestCase,
    get_auth_headers,
    override_settings,
)


class TestSimplePing(TestCase):

    def test_simple_ping_does_not_hit_database(self):
        # Ручка "/simple-ping/" не должна работать с базой данных
        client = app.test_client()

        with patch('intranet.yandex_directory.src.yandex_directory.common.db.get_meta_connection') as get_meta_connection, \
             patch('intranet.yandex_directory.src.yandex_directory.common.db.get_main_connection') as get_main_connection:
            response = client.get(SIMPLE_PING_URL)
            self.assertEqual(response.status_code, 200)
            data = json.loads(response.data)
            self.assertDictEqual(data, {'status': 'ok'})

            # проверим, что для запроса не запрашивались коннекты к базам
            self.assertEqual(get_meta_connection.call_count, 0)
            self.assertEqual(get_main_connection.call_count, 0)


class TestBaseView(TestCase):

    def test_if_x_database_given_go_to_master(self):
        # Проверяем, что если передан заголовок x-database,
        # то все обращения будут идти к мастер базам

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.base.get_meta_connection') \
                 as get_meta_connection, \
             patch('intranet.yandex_directory.src.yandex_directory.core.views.base.get_main_connection') \
                 as get_main_connection:
            get_meta_connection.return_value = not_closing(self.meta_connection)
            get_main_connection.return_value = not_closing(self.main_connection)

            headers = get_auth_headers()
            headers['X-Database'] = 'master'
            self.get_json('/organizations/', headers=headers)

            # проверим, что для коннекты к базам устанавливались с
            # параметром for_write=True
            get_meta_connection.assert_called_once_with(
                for_write=True
            )

            get_main_connection.assert_called_once_with(
                self.shard,
                for_write=True,
            )


class TestException(TestCase):
    def test_raise_exception(self):
        def logger_side_effect(*args, **kwargs):
            logger_side_effect.exc_info = sys.exc_info()

        logger_side_effect.exc_info = None

        # Проверяем, что кастомная ошибка залогирована обработчиком по умолчанию
        with patch.object(
                error_log,
                'exception',
                side_effect=logger_side_effect
        ) as exc_func:
            response = app.test_client().get('/exception/')

            assert_that(
                response,
                has_properties(
                    status_code=500,
                    json=has_entries(
                        code='unhandled_exception',
                        message='unhandled_exception',
                    ),
                )
            )

            exc_func.assert_called_once_with(
                'Request failed with unhandled exception.'
            )
            assert_that(
                logger_side_effect.exc_info,
                all_of(
                    is_(tuple),
                    has_item(
                        all_of(
                            is_(Exception),
                        )
                    )
                )
            )


class TestViewForApiVersioning(View):
    @no_permission_required
    def get(self, meta_connection, main_connection):
        return json_response({'version': 'old'})

    @no_permission_required
    def get_2(self, meta_connection, main_connection):
        return json_response({'version': '2'})

    @no_permission_required
    def get_6(self, meta_connection, main_connection):
        return json_response({'version': '6'})

    @no_permission_required
    def post_6(self, meta_connection, main_connection, data):
        return json_response({'version': '6'})


class TestViewPingWithoutVersion(View):
    @no_permission_required
    def get(self, meta_connection, main_connection):
        return json_response({'version': '1'})


TEST_API_VIEW_WITH_VERSION = '/test-api-version/'
TEST_API_VIEW_WITHOUT_VERSION = '/test-ping/'


with versioned_blueprint(app, 'version_tests', __name__) as bp:
    bp.add_url_rule(TEST_API_VIEW_WITH_VERSION, view_func=TestViewForApiVersioning.as_view('TestViewForApiVersioning'))
    bp.add_url_rule(TEST_API_VIEW_WITHOUT_VERSION, view_func=TestViewPingWithoutVersion.as_view('TestViewPingWithoutVersion'))


SUPPORTED_API_VERSIONS = [1, 2, 3, 4, 6, 1232131233]


class TestApiVersioning(TestCase):
    def test_should_get_v2_version_of_view(self):
        response = self.get_json('/v2%s' % TEST_API_VIEW_WITH_VERSION, expected_code=200)
        assert_that(response, equal_to({'version': '2'}))

    def test_should_get_original_version_of_view(self):
        response = self.get_json(TEST_API_VIEW_WITH_VERSION, expected_code=200)
        assert_that(response, equal_to({'version': 'old'}))

    def test_should_raise_an_error_if_required_api_version_is_unknown(self):
        response = self.get_json('/v2222222%s' % TEST_API_VIEW_WITH_VERSION, expected_code=400)
        exp_response = {
            'code': 'unsupported_api_version',
            'message': 'Supported API versions: %s' % app.config['SUPPORTED_API_VERSIONS'],
        }
        assert_that(response, equal_to(exp_response))

    @override_settings(SUPPORTED_API_VERSIONS=[6])
    def test_should_raise_an_error_if_required_api_version_is_depricated(self):
        response = self.get_json('/v2%s' % TEST_API_VIEW_WITH_VERSION, expected_code=400)
        exp_response = {
            'code': 'unsupported_api_version',
            'message': 'Supported API versions: %s' % app.config['SUPPORTED_API_VERSIONS'],
        }
        assert_that(response, equal_to(exp_response))

        response = self.get_json('%s' % TEST_API_VIEW_WITH_VERSION, expected_code=400)
        exp_response = {
            'code': 'unsupported_api_version',
            'message': 'Supported API versions: %s' % app.config['SUPPORTED_API_VERSIONS'],
        }
        assert_that(response, equal_to(exp_response))

        self.get_json('/v6%s' % TEST_API_VIEW_WITH_VERSION, expected_code=200)

    @override_settings(SUPPORTED_API_VERSIONS=[6], VIEW_CLASSES_WITHOUT_VERSION=['TestViewPingWithoutVersion'])
    def test_should_raise_an_error_for_incorrect_version(self):
        response = self.get_json('%s' % TEST_API_VIEW_WITHOUT_VERSION, expected_code=200)
        assert_that(response, equal_to({'version': '1'}))

        exp_response = {
            'code': 'not_found',
            'message': 'Not found',
        }
        response = self.get_json('/v2%s' % TEST_API_VIEW_WITHOUT_VERSION, expected_code=404)
        assert_that(response, equal_to(exp_response))

        response = self.get_json('/v6%s' % TEST_API_VIEW_WITHOUT_VERSION, expected_code=404)
        assert_that(response, equal_to(exp_response))

    @override_settings(SUPPORTED_API_VERSIONS=[6])
    def test_should_return_error_instead_of_minimal_version_too(self):
        response = self.post_json('%s' % TEST_API_VIEW_WITH_VERSION, data={}, expected_code=400)
        exp_response = {
            'code': 'unsupported_api_version',
            'message': 'Supported API versions: %s' % app.config['SUPPORTED_API_VERSIONS'],
        }
        assert_that(response, equal_to(exp_response))
