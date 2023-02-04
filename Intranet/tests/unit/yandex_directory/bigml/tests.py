# -*- coding: utf-8 -*-

from unittest.mock import (
    patch,
)
from hamcrest import (
    assert_that,
    equal_to,
    calling,
    raises,
)
from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory import bigml


class TestBigML(TestCase):
    maillist_uid = 123

    def test_get_subscribers(self):
        good_result = {
           'status': 'ok',
           'response': [
              {'uid': 1, 'stype': 'inbox'},
              {'uid': 2, 'stype': 'inbox'},
              {'uid': 3, 'stype': 'inbox'},
           ]
        }
        self.mocked_bigml_get.return_value = good_result
        result = bigml.get_maillist_subscribers(self.maillist_uid)

        assert_that(
            result,
            equal_to([1, 2, 3])
        )

    def test_error_in_bigml(self):
        self.mocked_bigml_get.side_effect = bigml.BigmlError('not_found', 'Error message', {})
        with self.assertRaises(bigml.BigmlError):
            bigml.get_maillist_subscribers(self.maillist_uid)
