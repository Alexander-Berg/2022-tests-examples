# -*- coding: utf-8 -*-
from intranet.yandex_directory.src.yandex_directory.common.backpressure import (
    is_need_to_close_service,
    get_vital_services_errors_count,
    save_smoke_results,
    reset_errors_count,
    last_smoke_test_has_errors,
)

from testutils import (
    TestCase,
    override_settings,
)

from hamcrest import (
    assert_that,
    equal_to,
    less_than,
)

BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT = 3
BACKPRESSURE_CHECK_WINDOW_SIZE = 5


class Test__is_need_to_close_service(TestCase):
    @override_settings(BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT=BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT,
                       BACKPRESSURE_CHECK_WINDOW_SIZE=BACKPRESSURE_CHECK_WINDOW_SIZE)
    def test_is_need_to_close_service_should_return_false(self):
        # проверяем, что функция вернет False если количество ошибок < числа в настройке
        errors_count, _ = get_vital_services_errors_count()
        assert_that(errors_count, less_than(BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT))
        result = is_need_to_close_service()
        assert_that(result, equal_to(False))

    @override_settings(BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT=BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT,
                       BACKPRESSURE_CHECK_WINDOW_SIZE=BACKPRESSURE_CHECK_WINDOW_SIZE)
    def test_is_need_to_close_service_should_return_true_if_errors_count_gte_setting(self):
        # проверяем, что функция вернет True если количество ошибок >= числу в настройке
        experiments = [
            ([True, False, False, False, False], False),
            ([True, True, False, False, False], False),
            ([True, True, True, False, False], True),
            ([True, True, True, True, False], True),
            ([True, True, True, False, True], True),
            ([True, False, True, True], False),  # не должны возвращать True, т.к. число проверок должно равняться 5
        ]

        for errors, exp_result in experiments:
            for error in errors:
                save_smoke_results(has_errors=error)
            assert_that(is_need_to_close_service(), equal_to(exp_result))
            reset_errors_count()


class Test__reset_errors_count(TestCase):
    @override_settings(BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT=BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT,
                       BACKPRESSURE_CHECK_WINDOW_SIZE=BACKPRESSURE_CHECK_WINDOW_SIZE)
    def test_reset_errors_count(self):
        # проверяем, что reset_errors_count сбрасывает ошибки в кэше
        for _ in range(10):
            save_smoke_results(has_errors=True)
        exp_result = (BACKPRESSURE_CHECK_WINDOW_SIZE, BACKPRESSURE_CHECK_WINDOW_SIZE)
        assert_that(get_vital_services_errors_count(), equal_to(exp_result))
        reset_errors_count()
        assert_that(get_vital_services_errors_count(), equal_to((0, 0)))


class Test__last_smoke_test_has_errors(TestCase):
    @override_settings(BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT=BACKPRESSURE_CLOSE_AFTER_ERRORS_COUNT,
                       BACKPRESSURE_CHECK_WINDOW_SIZE=BACKPRESSURE_CHECK_WINDOW_SIZE)
    def test_last_smoke_test_has_errors_should_return_last_status(self):
        experiments = [
            ([], False),
            ([True], True),
            ([True, False], False),
        ]
        for errors, exp_result in experiments:
            for error in errors:
                save_smoke_results(has_errors=error)
            assert_that(last_smoke_test_has_errors(), equal_to(exp_result))
            reset_errors_count()
