# coding: utf-8

from testutils import (
    TestCase,
)


class TestDecoratorIDMView(TestCase):

    def test_apply_decorators_service_not_found_success(self):
        self.get_json(
            '/idm/smth/info',
            expected_code=404,
        )
