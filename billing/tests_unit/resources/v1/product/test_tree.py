# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
from hamcrest import (
    assert_that,
    equal_to,
    is_not,
    empty,
    has_key,
)

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


@pytest.mark.slow
class TestCaseProductTree(TestCaseApiAppBase):
    BASE_API = '/v1/product/tree'

    def test_get_product_tree(self):
        response = self.test_client.get(self.BASE_API)

        assert_that(response.status_code, equal_to(http.OK), 'Response code must be OK.')
        root_node = response.get_json().get('data')

        assert_that(root_node, has_key('sub_groups'), 'Root node should contain the list of subgroups.')
        assert_that(root_node['sub_groups'], is_not(empty), 'The list of subgroups shouldn\'t be empty.')
