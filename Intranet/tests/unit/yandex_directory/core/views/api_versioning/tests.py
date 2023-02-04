# -*- coding: utf-8 -*-
from intranet.yandex_directory.src.yandex_directory.core.views.api_versioning import (
    get_methods_map_for_view,
    _get_api_version_from_method_name,
)
from intranet.yandex_directory.src.yandex_directory.core.views.base import View

from hamcrest import (
    assert_that,
    equal_to,
)

from testutils import (
    TestCase,
    override_settings,
)


class TestVersioningView(View):
    def get(self):
        pass

    def get_3(self):
        pass

    def get_7(self):
        pass

    def get_9(self):
        pass

    def patch(self):
        pass

    def post_15(self):
        pass



SUPPORTED_API_VERSIONS = [1, 7, 8, 9, 10, 11]
ONE_SUPPORTED_API_VERSION = [15]


class Test___get_all_methods_and_versions(TestCase):
    @override_settings(SUPPORTED_API_VERSIONS=SUPPORTED_API_VERSIONS)
    def test_creating_methods_map(self):
        methods_map = get_methods_map_for_view(TestVersioningView)

        exp_methods_map = {
            ('get', 1): 'get',
            ('get', 7): 'get_7',
            ('get', 8): 'get_7',
            ('get', 9): 'get_9',
            ('get', 10): 'get_9',
            ('get', 11): 'get_9',
            ('patch', 1): 'patch',
            ('patch', 7): 'patch',
            ('patch', 8): 'patch',
            ('patch', 9): 'patch',
            ('patch', 10): 'patch',
            ('patch', 11): 'patch',
        }

        assert_that(methods_map, equal_to(exp_methods_map))

    @override_settings(SUPPORTED_API_VERSIONS=ONE_SUPPORTED_API_VERSION)
    def test_creating_methods_map_with_only_last_version(self):
        methods_map = get_methods_map_for_view(TestVersioningView)

        exp_methods_map = {
            ('post', 15): 'post_15',
            ('patch', 15): 'patch',
            ('patch', 2): 'patch',
        }

        assert_that(methods_map, equal_to(exp_methods_map))

    @override_settings(SUPPORTED_API_VERSIONS=ONE_SUPPORTED_API_VERSION)
    def test_creating_methods_map_with_only_last_version(self):
        methods_map = get_methods_map_for_view(TestVersioningView)

        exp_methods_map = {
            ('get', 15): 'get_9',
            ('patch', 15): 'patch',
        }

        assert_that(methods_map, equal_to(exp_methods_map))


class Test___get_api_version_from_method_name(TestCase):
    def test_should_return_api_version(self):
        experiments = [
            ('get', 1),
            ('get_1', 1),
            ('get_12', 12),
            ('patch_123', 123),
        ]
        for method, exp_result in experiments:
            assert_that(_get_api_version_from_method_name(method), equal_to(exp_result))

    def test_should_return_first_version_for_special_methods_without_versioning(self):
        experiments = [
            ('get', 1),
            ('get_1', 1),
            ('get_12', 12),
            ('patch_123', 123),
        ]
        for method, exp_result in experiments:
            assert_that(_get_api_version_from_method_name(method), equal_to(exp_result))

    def test_should_raise_error_if_there_is_no_version(self):
        with self.assertRaises(RuntimeError):
            _get_api_version_from_method_name('get_some_value')
