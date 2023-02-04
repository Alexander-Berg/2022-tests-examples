# -*- coding: utf-8 -*-
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.setup import VersionDispatcher

from testutils import TestCase

from hamcrest import (
    assert_that,
    equal_to,
)


class TestVersionDispatcher(TestCase):
    def setUp(self, *args, **kwargs):
        self.dispatcher = VersionDispatcher(app=app)

    def test_parsing_version_and_path(self):
        experiments = [
            ('/v2/resources/', '2'),
            ('resources/', '1'),
            ('/v3/resources/some-path/', '3'),
            ('/resources/some-path', '1'),
        ]

        for experiment_path, exp_result in experiments:
            environ = {'PATH_INFO': experiment_path}
            version = self.dispatcher._get_version(environ)

            assert_that(version, equal_to(exp_result))
