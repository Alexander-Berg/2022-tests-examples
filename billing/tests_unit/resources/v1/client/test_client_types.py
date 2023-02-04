# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
from hamcrest import (
    assert_that,
    equal_to,
    empty,
    is_not,
)

import http.client as http

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


@pytest.mark.smoke
class TestCaseClientTypes(TestCaseApiAppBase):
    BASE_API = u'/v1/client/types'

    def test_returned_list(self):
        response = self.test_client.get(self.BASE_API)
        assert_that(response.status_code, equal_to(http.OK), u'Response code should be OK')
        data = response.get_json()['data']
        assert_that(data, is_not(empty), u'Empty list of client types')
        assert_that(
            set(data[0]), equal_to({'id', 'name'}),
            u'Fields set in response doesnt match the expectation',
        )
