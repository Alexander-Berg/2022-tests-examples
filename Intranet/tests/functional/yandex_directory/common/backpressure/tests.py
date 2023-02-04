# -*- coding: utf-8 -*-
from unittest.mock import patch, Mock

from testutils import (
    TestCase,
    override_settings,
)
from intranet.yandex_directory.src.yandex_directory.common.smoke import _smoke_check


BACKPRESSURE_CHECK_WINDOW_SIZE = 3
BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT = 3


class TestTestClosingServiceAfterSmokeErrors(TestCase):
    @override_settings(BACKPRESSURE_CHECK_WINDOW_SIZE=BACKPRESSURE_CHECK_WINDOW_SIZE,
                       BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT=BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT)
    def test_should_not_close_service_after_errors_in_non_vital_services(self):
        # проверяем, что после ошибок в неважных сервисах мы не закроемся
        smoke_tests_results = [
            {'vital': False, 'service': 'some-service', 'status': 'error', 'name': 'service_1', 'message': ''},
            {'vital': True, 'service': 'some-service', 'status': 'ok', 'name': 'service_2', 'message': ''},
        ]
        mocked_smoke_tests_results = Mock(return_value=smoke_tests_results)
        with patch('intranet.yandex_directory.src.yandex_directory.common.smoke._get_smoke_test_results', mocked_smoke_tests_results):
            for _ in range(BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT + 1):
                self.get_json('/ping/')
                self.get_json('/')

    @override_settings(BACKPRESSURE_CHECK_WINDOW_SIZE=BACKPRESSURE_CHECK_WINDOW_SIZE,
                       BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT=BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT)
    def test_should_close_service_after_errors_in_vital_services(self):
        # проверяем, что после ошибок в важных сервисах мы закроемся,
        # а после того, как ошибки пропадут - снова откроем сервис
        smoke_tests_results = [
            {'vital': True, 'service': 'some-service', 'status': 'error', 'name': 'service_1', 'message': ''},
            {'vital': False, 'service': 'some-service', 'status': 'ok', 'name': 'service_2', 'message': ''},
        ]
        smoke_tests_results_without_errors = [
            {'vital': True, 'service': 'some-service', 'status': 'ok', 'name': 'service_1', 'message': ''},
            {'vital': False, 'service': 'some-service', 'status': 'ok', 'name': 'service_2', 'message': ''},
        ]
        mocked_smoke_tests_results = Mock(return_value=smoke_tests_results)
        with patch('intranet.yandex_directory.src.yandex_directory.common.smoke._get_smoke_test_results', mocked_smoke_tests_results):
            for i in range(BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT):
                # сэмулируем проверку
                _smoke_check()

            # после BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT попыток мы должны закрыться на всё кроме пинга
            self.get_json('/', expected_code=503)
            self.get_json('/ping/', expected_code=500)

            mocked_smoke_tests_results.return_value = smoke_tests_results_without_errors
            _smoke_check()

            # теперь ошибок нет и сервис должен открыться после похода в /ping/
            self.get_json('/ping/')
            self.get_json('/')
